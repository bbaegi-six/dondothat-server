package org.bbagisix.asset.service;

import org.bbagisix.asset.domain.AssetVO;
import org.bbagisix.asset.dto.AssetDTO;
import org.bbagisix.asset.mapper.AssetMapper;
import org.bbagisix.codef.dto.CodefTransactionResDTO;
import org.bbagisix.codef.service.CodefApiServiceImpl;
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
public class AssetServiceImpl implements AssetService {

	@Autowired
	private AssetMapper assetMapper;

	@Autowired
	private CodefApiServiceImpl codefApiService;

	@Autowired
	private ExpenseMapper expenseMapper;

	private static final int MONTH = 3; // 처음 3개월 소비내역 조회
	private static final Long ETC = 3L; // 📄 카테고리 id : 기타
	private static final Long INCOME = 99L; // 📄 카테고리 id : 수입


	// 1 계좌 연동 + 3개월 소비내역 저장
	// POST /api/assets/accounts/connect
	@Override
	@Transactional
	public void connectAsset(Long userId, AssetDTO assetDTO){
		try {
			String connectedId = codefApiService.getConnectedId(assetDTO);
			if(connectedId == null){
				// 💥 에러 메시지 : 계좌 연동 실패 connectedId 가져올 수 없음
				return;
			}

			LocalDate today = LocalDate.now();
			LocalDate startMonth = today.minusMonths(MONTH);
			LocalDate start = startMonth.withDayOfMonth(1);

			String todayStr = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
			String startStr = start.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

			CodefTransactionResDTO reqDTO = codefApiService.getTransactionList(assetDTO,connectedId,startStr,todayStr);

			System.out.println("1️⃣" + reqDTO.toString());
			if(reqDTO == null) {
				// 💥 에러 메시지 : 거래내역 조회 실패
				return;
			}

			AssetVO assetVO = createUserAssetVO(userId,assetDTO,connectedId, reqDTO);
			System.out.println("1️⃣" + assetVO.toString());
			Long assetId = insertUserAsset(assetVO);
			System.out.println("1️⃣" + assetId);

			saveTransactionHistory(assetId,userId,reqDTO);
			System.out.println("1️⃣ end");

		} catch (Exception e) {
			// 💥 에러 메시지 : connectedAsset 실행 중 오류 발생
			return;
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
			// 💥 에러 메시지 : 계좌 정보 저장 실패: 생성된 계좌를 찾을 수 없습니다
		}
		return insertedAsset.getAssetId();
	}

	private void saveTransactionHistory(Long assetId, Long userId, CodefTransactionResDTO resDTO){
		List<ExpenseVO> expenseVOList = toExpenseVOList(assetId,userId,resDTO);

		if(!expenseVOList.isEmpty()){
			int insertedCount = assetMapper.insertExpenses(expenseVOList);
			if (insertedCount != expenseVOList.size()) {
				// 💥 에러 메시지 : 거래내역 저장 중 일부 실패
			}

		} else{
			//💥 에러 메시지 : 저장할 거래 내역이 없습니다
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
					expenseVO.setCategoryId(ETC);
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