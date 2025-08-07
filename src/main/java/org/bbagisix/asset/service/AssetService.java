package org.bbagisix.asset.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bbagisix.asset.domain.AssetVO;
import org.bbagisix.asset.dto.AssetDTO;
import org.bbagisix.asset.mapper.AssetMapper;
import org.bbagisix.classify.service.ClassifyService;
import org.bbagisix.codef.dto.CodefTransactionResDTO;
import org.bbagisix.codef.service.CodefApiService;
import org.bbagisix.exception.BusinessException;
import org.bbagisix.exception.ErrorCode;
import org.bbagisix.expense.domain.ExpenseVO;
import org.bbagisix.expense.mapper.ExpenseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Transactional
@RequiredArgsConstructor
@Log4j2
public class AssetService {

	private final AssetMapper assetMapper;

	private final CodefApiService codefApiService;
	private final ClassifyService classifyService;

	private final ExpenseMapper expenseMapper;

	private static final int MONTH = 3; // 처음 3개월 소비내역 조회
	private static final Long TBC = 14L; // 📄 카테고리 id : TBC 미지정
	private static final Long INCOME = 13L; // 📄 카테고리 id : 수입

	// 1. 계좌 연동 + 3개월 소비내역 저장
	// POST /api/assets/connect
	@Transactional
	public String connectMainAsset(Long userId, AssetDTO assetDTO) {
		// 정보 누락
		if (assetDTO.getBankpw() == null || assetDTO.getBankId() == null || assetDTO.getBankAccount() == null) {
			throw new BusinessException(ErrorCode.ASSET_FAIL, "필수 계좌 정보가 누락되었습니다.");
		}

		// 이미 연결된 계좌 확인
		AssetVO existingAsset = assetMapper.selectAssetByUserIdAndStatus(userId, "main");
		if (existingAsset != null) {
			throw new BusinessException(ErrorCode.ASSET_ALREADY_EXISTS);
		}

		// codef api 통한 연결 ID 생성
		String connectedId = codefApiService.getConnectedId(assetDTO);
		if (connectedId == null) {
			throw new BusinessException(ErrorCode.ASSET_FAIL, "외부 은행 API 연결 ID 생성에 실패했습니다.");
		}

		// 조회 기간 설정 (3개월)
		LocalDate today = LocalDate.now();
		LocalDate startMonth = today.minusMonths(MONTH);
		LocalDate start = startMonth.withDayOfMonth(1);

		String todayStr = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		String startStr = start.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

		// 거래 내역 조회
		CodefTransactionResDTO reqDTO = codefApiService.getTransactionList(assetDTO, connectedId, startStr, todayStr,
			true);
		if (reqDTO == null) {
			log.error("❌ Codef API 응답이 null - 사용자ID: {}", userId);
			throw new BusinessException(ErrorCode.TRANSACTION_FAIL, "외부 은행 API에서 거래내역을 조회하지 못했습니다.");
		}

		// db 저장
		AssetVO assetVO = createUserAssetVO(userId, assetDTO, connectedId, reqDTO, "main");
		Long assetId = insertUserAsset(assetVO, "main");

		saveTransactionHistory(assetId, userId, reqDTO);
		String accountName = reqDTO.getResAccountName();
		return accountName;
	}

	// 1-2. 서브 계좌 입력
	// 이건 그냥 db에 저장하는 것임
	public void connectSubAsset(Long userId, AssetDTO assetDTO) {
		// 정보 누락
		if (assetDTO.getBankpw() == null || assetDTO.getBankId() == null || assetDTO.getBankAccount() == null) {
			throw new BusinessException(ErrorCode.ASSET_FAIL, "필수 계좌 정보가 누락되었습니다.");
		}

		// 이미 연결된 계좌 확인
		AssetVO existingAsset = assetMapper.selectAssetByUserIdAndStatus(userId, "sub");
		if (existingAsset != null) {
			throw new BusinessException(ErrorCode.ASSET_ALREADY_EXISTS);
		}

		// db 저장
		AssetVO assetVO = createUserAssetVO(userId, assetDTO, null, null, "sub");
		Long assetId = insertUserAsset(assetVO, "sub");
	}

	// 2. 계좌 삭제
	public void deleteAsset(Long userId, String status) {
		AssetVO asset = assetMapper.selectAssetByUserIdAndStatus(userId, status);
		if (asset == null) {
			throw new BusinessException(ErrorCode.ASSET_NOT_FOUND);
		}

		// 1 main 계좌인 경우에만 Codef API 연결 해제
		if ("main".equals(status)) {

			// Codef API 연결해제
			boolean codefDeleted = codefApiService.deleteConnectedId(userId);
			if (!codefDeleted) {
				throw new BusinessException(ErrorCode.CODEF_FAIL, "Codef API 연결 해제에 실패했습니다.");
			}

			// main 계좌의 거래내역 삭제
			int deletedExpenses = assetMapper.deleteExpensesByUserId(userId);
			log.info("삭제된 거래내역 수: {}", deletedExpenses);
		}

		// 2 계좌 정보 삭제
		int deletedAssets = assetMapper.deleteUserAssetByUserIdAndStatus(userId, status); // status param
		if (deletedAssets == 0) {
			throw new BusinessException(ErrorCode.ASSET_FAIL, "계좌 정보 삭제에 실패했습니다.");
		}

	}

	// AssetVO 생성
	private AssetVO createUserAssetVO(Long userId, AssetDTO assetDTO, String connectedId, CodefTransactionResDTO reqDTO,
		String status) {
		AssetVO assetVO = new AssetVO();
		assetVO.setUserId(userId);
		if (reqDTO == null) {
			assetVO.setBalance(0L);
			String assetName = assetDTO.getBankName() + " 계좌";
			assetVO.setAssetName(assetName);
			assetVO.setConnectedId(null);
		} else {
			assetVO.setAssetName(reqDTO.getResAccountName());
			assetVO.setConnectedId(connectedId);
			if (reqDTO.getResAccountBalance() != null) {
				Long balance = amountToLong(reqDTO.getResAccountBalance());
				assetVO.setBalance(balance);
			} else {
				assetVO.setBalance(0L);
			}

		}
		assetVO.setBankName(assetDTO.getBankName());
		assetVO.setBankAccount(assetDTO.getBankAccount());
		assetVO.setBankId(assetDTO.getBankId());
		String encryptedPassword = codefApiService.encryptPw(assetDTO.getBankpw());
		assetVO.setBankPw(encryptedPassword);
		assetVO.setStatus(status);

		return assetVO;
	}

	// 계좌 정보 DB 저장
	private Long insertUserAsset(AssetVO assetVO, String status) {
		// 들어가기 전에 암호화!
		assetMapper.insertUserAsset(assetVO);

		AssetVO insertedAsset = assetMapper.selectAssetByUserIdAndStatus(assetVO.getUserId(), status);
		if (insertedAsset == null || insertedAsset.getAssetId() == null) {
			throw new BusinessException(ErrorCode.ASSET_FAIL, "계좌 정보 저장에 실패했습니다.");
		}
		return insertedAsset.getAssetId();
	}

	// 거래 내역 저장
	private void saveTransactionHistory(Long assetId, Long userId, CodefTransactionResDTO resDTO) {
		List<ExpenseVO> expenseVOList = toExpenseVOList(assetId, userId, resDTO);

		if (!expenseVOList.isEmpty()) {
			// log.info("llm start.." + expenseVOList.stream().toList());
			expenseVOList = classifyService.classify(expenseVOList);
			// log.info("llm end.." + expenseVOList.stream().toList());
			int insertedCount = assetMapper.insertExpenses(expenseVOList);
			if (insertedCount != expenseVOList.size()) {
				throw new BusinessException(ErrorCode.TRANSACTION_FAIL,
					"일부 거래내역 저장에 실패했습니다. 예상: " + expenseVOList.size() + ", 실제: " + insertedCount);
			}

		} else {
			throw new BusinessException(ErrorCode.ASSET_FAIL);
		}
	}

	// 거래 내역을 ExpenseVO 리스트로 변환
	public List<ExpenseVO> toExpenseVOList(Long assetId, Long userId, CodefTransactionResDTO responseDTO) {
		List<ExpenseVO> expenses = new ArrayList<>();

		if (responseDTO.getResTrHistoryList() != null) {
			for (CodefTransactionResDTO.HistoryItem item : responseDTO.getResTrHistoryList()) {
				ExpenseVO expenseVO = new ExpenseVO();
				expenseVO.setUserId(userId);
				expenseVO.setAssetId(assetId);

				Long withdrawAmount = amountToLong(item.getResAccountOut());
				Long depositAmount = amountToLong(item.getResAccountIn());

				if (withdrawAmount > 0) {
					expenseVO.setCategoryId(TBC);
					expenseVO.setAmount(withdrawAmount);
				} else if (depositAmount > 0) {
					expenseVO.setCategoryId(INCOME);
					expenseVO.setAmount(depositAmount);
				} else {
					continue; // 금액이 0인 경우 스킵
				}

				expenseVO.setDescription(item.getResAccountDesc3());

				Date expenditureDate = parseTransactionDateTime(
					item.getResAccountTrDate(),
					item.getResAccountTrTime()
				);
				expenseVO.setExpenditureDate(expenditureDate);

				expenses.add(expenseVO);
			}
		}
		return expenses;
	}

	// 금액 문자열을 Long으로 변환
	public Long amountToLong(String amountStr) {
		if (amountStr == null || amountStr.trim().isEmpty()) {
			return 0L;
		}
		try {
			String numericStr = amountStr.replaceAll("[^0-9]", "");
			return numericStr.isEmpty() ? 0L : Long.parseLong(numericStr);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	// 거래 일시 파싱
	private Date parseTransactionDateTime(String dateStr, String timeStr) {

		LocalDate date = parseTransactionDate(dateStr);
		if (date == null) {
			throw new BusinessException(ErrorCode.TRANSACTION_FAIL, "거래 날짜 파싱에 실패했습니다");
		}

		LocalTime time = parseTransactionTime(timeStr);

		LocalDateTime dateTime = LocalDateTime.of(date, time);

		return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
	}

	// 거래 날짜 파싱
	private LocalDate parseTransactionDate(String dateStr) {
		if (dateStr != null && dateStr.length() >= 8) {
			String year = dateStr.substring(0, 4);
			String month = dateStr.substring(4, 6);
			String day = dateStr.substring(6, 8);

			return LocalDate.of(
				Integer.parseInt(year),
				Integer.parseInt(month),
				Integer.parseInt(day)
			);
		}
		return null;
	}

	// 거래 시간 파싱
	private LocalTime parseTransactionTime(String timeStr) {
		if (timeStr != null && !timeStr.trim().isEmpty()) {
			String cleanTimeStr = timeStr.replaceAll("[^0-9]", "");

			if (cleanTimeStr.length() >= 6) {
				String hour = cleanTimeStr.substring(0, 2);
				String minute = cleanTimeStr.substring(2, 4);
				String second = cleanTimeStr.substring(4, 6);

				return LocalTime.of(
					Integer.parseInt(hour),
					Integer.parseInt(minute),
					Integer.parseInt(second)
				);
			} else if (cleanTimeStr.length() >= 4) {
				// HHMM 형식
				String hour = cleanTimeStr.substring(0, 2);
				String minute = cleanTimeStr.substring(2, 4);

				return LocalTime.of(
					Integer.parseInt(hour),
					Integer.parseInt(minute),
					0
				);
			}
		}
		return LocalTime.of(0, 0, 0);
	}

}