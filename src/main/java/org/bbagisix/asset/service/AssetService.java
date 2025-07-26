package org.bbagisix.asset.service;

import org.bbagisix.asset.domain.AssetVO;
import org.bbagisix.asset.dto.AssetDTO;
import org.bbagisix.asset.mapper.AssetMapper;
import org.bbagisix.codef.dto.CodefTransactionResDTO;
import org.bbagisix.codef.service.CodefApiService;
import org.bbagisix.exception.BusinessException;
import org.bbagisix.exception.ErrorCode;
import org.bbagisix.expense.domain.ExpenseVO;
import org.bbagisix.expense.mapper.ExpenseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class AssetService {

	@Autowired
	private AssetMapper assetMapper;

	@Autowired
	private CodefApiService codefApiService;

	@Autowired
	private ExpenseMapper expenseMapper;

	private static final int MONTH = 3; // 처음 3개월 소비내역 조회
	private static final Long TBC = 3L; // 📄 카테고리 id : TBC 미지정
	private static final Long INCOME = 99L; // 📄 카테고리 id : 수입


	// 1 계좌 연동 + 3개월 소비내역 저장
	// POST /api/assets/connect
	@Transactional
	public void connectAsset(Long userId, AssetDTO assetDTO){

		try {
			// 기존 연결된 계좌 확인
			AssetVO existingAsset = assetMapper.selectAssetByUserId(userId);
			if (existingAsset != null) {
				throw new BusinessException(ErrorCode.ASSET_ALREADY_CONNECTED);
			}

			// Codef API를 통한 연결 ID 생성
			String connectedId = codefApiService.getConnectedId(assetDTO);
			if (connectedId == null) {
				throw new BusinessException(ErrorCode.CODEF_CONNECTED_ID_NOT_FOUND);
			}

			// 거래내역 조회 기간 설정
			LocalDate today = LocalDate.now();
			LocalDate startMonth = today.minusMonths(MONTH);
			LocalDate start = startMonth.withDayOfMonth(1);

			String todayStr = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
			String startStr = start.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

			// 거래 내역 조회
			CodefTransactionResDTO reqDTO = codefApiService.getTransactionList(assetDTO, connectedId, startStr, todayStr);
			if (reqDTO == null) {
				throw new BusinessException(ErrorCode.TRANSACTION_FETCH_FAILED);
			}

			// 계좌 정보 저장
			AssetVO assetVO = createUserAssetVO(userId, assetDTO, connectedId, reqDTO);
			Long assetId = insertUserAsset(assetVO);

			// 거래 내역 저장
			saveTransactionHistory(assetId, userId, reqDTO);
		} catch (Exception e){
			throw new BusinessException(ErrorCode.ASSET_CONNECTION_FAILED);
		}

	}

	public void deleteAsset(Long userId) {
		try {

			AssetVO asset = assetMapper.selectAssetByUserId(userId);
			if (asset == null) {
				throw new BusinessException(ErrorCode.ASSET_NOT_FOUND);
			}

			// 1. Codef API 연결 해제
			boolean codefDeleted = codefApiService.deleteConnectedId(userId);
			if (!codefDeleted) {
				throw new BusinessException(ErrorCode.ASSET_DISCONNECTION_FAILED, "Codef API 연결 해제에 실패했습니다.");
			}

			// 2. 관련 거래내역 삭제
			int deletedExpenses = assetMapper.deleteExpensesByUserId(userId);

			// 3. 계좌 정보 삭제
			int deletedAssets = assetMapper.deleteUserAssetByUserId(userId);
			if (deletedAssets == 0) {
				throw new BusinessException(ErrorCode.ASSET_DELETE_FAILED, "계좌 정보 삭제에 실패했습니다.");
			}
		} catch (Exception e){
			throw new BusinessException(ErrorCode.ASSET_DELETE_FAILED);
		}
	}

	private AssetVO createUserAssetVO(Long userId, AssetDTO assetDTO, String connectedId, CodefTransactionResDTO reqDTO){
		AssetVO assetVO = new AssetVO();
		assetVO.setUserId(userId);
		assetVO.setAssetName(reqDTO.getResAccountName());
		assetVO.setBankName(assetDTO.getBankName());
		assetVO.setBankAccount(assetDTO.getBankAccount());
		assetVO.setBankId(assetDTO.getBankId());
		assetVO.setBankPw(assetDTO.getBankpw());
		assetVO.setConnectedId(connectedId);

		if (reqDTO.getResAccountBalance() != null) {
			Long balance = amountToLong(reqDTO.getResAccountBalance());
			assetVO.setBalance(balance);
		} else {
			assetVO.setBalance(0L);
		}

		return assetVO;
	}

	private Long insertUserAsset(AssetVO assetVO) {
		assetMapper.insertUserAsset(assetVO);

		AssetVO insertedAsset = assetMapper.selectAssetByUserId(assetVO.getUserId());
		if (insertedAsset == null || insertedAsset.getAssetId() == null) {
			throw new BusinessException(ErrorCode.ASSET_SAVE_FAILED, "계좌 정보 저장에 실패했습니다.");
		}
		return insertedAsset.getAssetId();
	}

	private void saveTransactionHistory(Long assetId, Long userId, CodefTransactionResDTO resDTO){
		List<ExpenseVO> expenseVOList = toExpenseVOList(assetId,userId,resDTO);

		if(!expenseVOList.isEmpty()){
			int insertedCount = assetMapper.insertExpenses(expenseVOList);
			if (insertedCount != expenseVOList.size()) {
				throw new BusinessException(ErrorCode.TRANSACTION_SAVE_FAILED,
					"일부 거래내역 저장에 실패했습니다. 예상: " + expenseVOList.size() + ", 실제: " + insertedCount);
			}

		} else{
			throw new BusinessException(ErrorCode.TRANSACTION_SAVE_FAILED);
		}
	}

	private List<ExpenseVO> toExpenseVOList(Long assetId, Long userId, CodefTransactionResDTO responseDTO) {
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

				Timestamp expenditureTimestamp = parseTransactionDateTime(
					item.getResAccountTrDate(),
					item.getResAccountTrTime()
				);
				expenseVO.setExpenditureDate(expenditureTimestamp);

				expenses.add(expenseVO);
			}
		}
		return expenses;
	}

	private Long amountToLong(String amountStr){
		if(amountStr == null || amountStr.trim().isEmpty()){
			return 0L;
		}
		try {
			String numericStr = amountStr.replaceAll("[^0-9]", "");
			return numericStr.isEmpty() ? 0L : Long.parseLong(numericStr);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}


	private Timestamp parseTransactionDateTime(String dateStr, String timeStr) {
		try {
			LocalDate date = parseTransactionDate(dateStr);
			if (date == null) {
				// 💥 에러 메시지
				return null;
			}

			LocalTime time = parseTransactionTime(timeStr);

			LocalDateTime dateTime = LocalDateTime.of(date, time);

			return Timestamp.valueOf(dateTime);

		} catch (Exception e) {
			// 💥 에러 메시지
			return null;
		}
	}

	private LocalDate parseTransactionDate(String dateStr) {
		try {
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
		} catch (Exception e) {
			// 💥 에러 메시지 : 날짜 변환 실패
		}
		return null;
	}

	private LocalTime parseTransactionTime(String timeStr) {
		try {
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
		} catch (Exception e) {
			// 💥 에러 메시지 : 시간 변환 실패
		}
		return LocalTime.of(0, 0, 0);
	}





}