package org.bbagisix.codef.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.bbagisix.asset.domain.AssetVO;
import org.bbagisix.asset.dto.AssetDTO;
import org.bbagisix.asset.mapper.AssetMapper;
import org.bbagisix.asset.service.AssetService;
import org.bbagisix.classify.service.ClassifyService;
import org.bbagisix.codef.dto.CodefTransactionResDTO;
import org.bbagisix.expense.domain.ExpenseVO;
import org.bbagisix.expense.mapper.ExpenseMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class CodefSchedulerService {

	private final AssetMapper assetMapper;

	private final ExpenseMapper expenseMapper;

	private final CodefApiService codefApiService;

	private static final Long TBC = 14L; // 카테고리 id : TBC 미지정
	private static final Long INCOME = 13L; // 카테고리 id : 수입
	private final AssetService assetService;
	private final ClassifyService classifyService;

	// 10분마다 실행 (cron: 초 분 시 일 월 요일)
	// @Scheduled(cron = "0 */10 * * * *")
	// 자정(00:00)에 한번 실행
	@Scheduled(cron = "0 0 0 * * *")
	@Transactional
	public void syncAllMainAssetsTransactions() {
		LocalDateTime now = LocalDateTime.now();

		log.info("✅ Scheduler start" + now);
		// 모든 main 계좌 조회
		List<AssetVO> mainAssets = assetMapper.selectAllMainAssets();

		if (mainAssets.isEmpty()) {
			log.info("동기화할 main 계좌가 없습니다.");
			return;
		}

		int successCount = 0;
		int failCount = 0;

		// 각 계좌별로 거래내역 동기화
		for (AssetVO asset : mainAssets) {
			try {
				syncAssetTransactions(asset);
				successCount++;
			} catch (Exception e) {
				failCount++;
			}
		}
		log.info("Scheduler finish - success: {}, fail: {}", successCount, failCount);
	}

	// 단일 계좌의 거래내역 동기화
	private void syncAssetTransactions(AssetVO asset) {
		// AssetDTO 생성
		AssetDTO assetDTO = createAssetDTO(asset);

		// 조회 기간 설정 (어제~오늘)
		LocalDate today = LocalDate.now();
		LocalDate yesterday = today.minusDays(1);

		String todayStr = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		String yesterdayStr = yesterday.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

		// 거래내역 조회
		log.info(" 👉 [ user ID : {} ] Codef API start...", asset.getUserId());
		CodefTransactionResDTO transactionResDTO = codefApiService.getTransactionList(assetDTO, asset.getConnectedId(),
			yesterdayStr, todayStr, false);

		if (transactionResDTO == null || transactionResDTO.getResTrHistoryList() == null) {
			log.warn("API 응답이 null입니다");
			return;
		}
		// 계좌 잔액 업데이트
		updateAssetBalance(asset, transactionResDTO);
		log.info(" 👉 [ user ID : {} ] update new balance... ", asset.getUserId());

		// 새로운 거래내역만 필터링하여 저장
		List<ExpenseVO> newTransactions = filterNewTransactions(asset, transactionResDTO);

		if (!newTransactions.isEmpty()) {
			newTransactions = classifyService.classify(newTransactions);
			int insertedCount = expenseMapper.insertExpenses(newTransactions);
			log.info(" 👉 [ user ID : {} ] update new transactions... : {} ", asset.getUserId(), newTransactions.size());
		}
	}

	// AssetVO를 AssetDTO로 변환
	private AssetDTO createAssetDTO(AssetVO asset) {
		AssetDTO dto = new AssetDTO();
		dto.setBankName(asset.getBankName());
		dto.setBankId(asset.getBankId());
		dto.setBankpw(asset.getBankPw());
		dto.setBankAccount(asset.getBankAccount());
		return dto;
	}

	// 계좌 잔액 업데이트
	private void updateAssetBalance(AssetVO asset, CodefTransactionResDTO transactionResDTO) {
		if (transactionResDTO.getResAccountBalance() != null) {
			Long newBalance = assetService.amountToLong(transactionResDTO.getResAccountBalance());
			if (!newBalance.equals(asset.getBalance())) {
				assetMapper.updateAssetBalance(asset.getAssetId(), newBalance);
			}
		}

	}

	// 중복되지 않은 새로운 거래내역만 필터링
	private List<ExpenseVO> filterNewTransactions(AssetVO asset, CodefTransactionResDTO transactionResDTO) {
		List<ExpenseVO> newTransactions = new ArrayList<>();

		for (CodefTransactionResDTO.HistoryItem item : transactionResDTO.getResTrHistoryList()) {
			List<ExpenseVO> expenseVOList = assetService.toExpenseVOList(asset.getAssetId(), asset.getUserId(),
				transactionResDTO);

			for (ExpenseVO expenseVO : expenseVOList) {
				if (!isDuplicateTransaction(expenseVO)) {
					newTransactions.add(expenseVO);
				}
			}
			break; // 전체 리스트 한번에 처리
		}
		return newTransactions;
	}

	// 중복 거래 내역 체크
	private boolean isDuplicateTransaction(ExpenseVO expenseVO) {
		try {
			int count = assetMapper.countDuplicateTransaction(
				expenseVO.getUserId(),
				expenseVO.getAssetId(),
				expenseVO.getAmount(),
				expenseVO.getDescription(),
				expenseVO.getExpenditureDate()
			);
			return count > 0;
		} catch (Exception err) {
			return false; // 오류 시 중복이 아닌 것으로 간주하여 저장
		}
	}
}
