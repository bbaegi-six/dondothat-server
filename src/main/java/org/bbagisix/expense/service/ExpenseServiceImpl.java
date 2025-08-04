package org.bbagisix.expense.service;

import java.util.List;
import java.util.stream.Collectors;

import org.bbagisix.category.domain.CategoryVO;
import org.bbagisix.category.mapper.CategoryMapper;
import org.bbagisix.expense.domain.ExpenseVO;
import org.bbagisix.expense.dto.ExpenseDTO;
import org.bbagisix.expense.mapper.ExpenseMapper;
import org.bbagisix.exception.BusinessException;
import org.bbagisix.exception.ErrorCode;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
public class ExpenseServiceImpl implements ExpenseService {
	private final ExpenseMapper expenseMapper;
	private final CategoryMapper categoryMapper;
	// asset 패키지 완성 시 연동
	// private final AssetMapper assetMapper;

	@Override
	public ExpenseDTO createExpense(ExpenseDTO expenseDTO) {
		try {
			ExpenseVO vo = dtoToVo(expenseDTO);
			expenseMapper.insert(vo);
			if (vo.getExpenditureId() == null) {
				throw new BusinessException(ErrorCode.EXPENSE_CREATE_FAILED);
			}
			return voToDto(vo);
		} catch (BusinessException e) {
			throw e;
		} catch (Exception e) {
			log.error("소비내역 생성 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
			throw new BusinessException(ErrorCode.EXPENSE_CREATE_FAILED, e);
		}
	}

	@Override
	public ExpenseDTO getExpenseById(Long expenditureId, Long userId) {
		try {
			ExpenseVO vo = expenseMapper.findById(expenditureId);
			if (vo == null) {
				throw new BusinessException(ErrorCode.EXPENSE_NOT_FOUND);
			}
			if (!vo.getUserId().equals(userId)) {
				throw new BusinessException(ErrorCode.EXPENSE_ACCESS_DENIED);
			}
			return voToDto(vo);
		} catch (BusinessException e) {
			throw e;
		} catch (Exception e) {
			log.error("소비내역 조회 중 예상치 못한 오류 발생: expenditureId={}, userId={}, error={}", expenditureId, userId, e.getMessage(), e);
			throw new BusinessException(ErrorCode.DATA_ACCESS_ERROR, e);
		}
	}

	@Override
	public List<ExpenseDTO> getExpensesByUserId(Long userId) {
		try {
			return expenseMapper.findAllByUserId(userId).stream().map(this::voToDto).collect(Collectors.toList());
		} catch (Exception e) {
			log.error("사용자 소비내역 목록 조회 중 오류 발생: userId={}, error={}", userId, e.getMessage(), e);
			throw new BusinessException(ErrorCode.DATA_ACCESS_ERROR, e);
		}
	}

	@Override
	public ExpenseDTO updateExpense(Long expenditureId, ExpenseDTO expenseDTO, Long userId) {
		try {
			ExpenseVO vo = expenseMapper.findById(expenditureId);
			if (vo == null) {
				throw new BusinessException(ErrorCode.EXPENSE_NOT_FOUND);
			}
			if (!vo.getUserId().equals(userId)) {
				throw new BusinessException(ErrorCode.EXPENSE_ACCESS_DENIED);
			}

			vo.setCategoryId(expenseDTO.getCategoryId());
			vo.setAssetId(expenseDTO.getAssetId());
			vo.setAmount(expenseDTO.getAmount());
			vo.setDescription(expenseDTO.getDescription());
			vo.setExpenditureDate(expenseDTO.getExpenditureDate());

			int result = expenseMapper.update(vo);
			if (result != 1) {
				throw new BusinessException(ErrorCode.EXPENSE_UPDATE_FAILED, 
					"예상 업데이트 수: 1, 실제: " + result);
			}
			return voToDto(expenseMapper.findById(expenditureId));
		} catch (BusinessException e) {
			throw e;
		} catch (Exception e) {
			log.error("소비내역 수정 중 예상치 못한 오류 발생: expenditureId={}, userId={}, error={}", expenditureId, userId, e.getMessage(), e);
			throw new BusinessException(ErrorCode.EXPENSE_UPDATE_FAILED, e);
		}
	}

	@Override
	public void deleteExpense(Long expenditureId, Long userId) {
		try {
			ExpenseVO vo = expenseMapper.findById(expenditureId);
			if (vo == null) {
				throw new BusinessException(ErrorCode.EXPENSE_NOT_FOUND);
			}
			if (!vo.getUserId().equals(userId)) {
				throw new BusinessException(ErrorCode.EXPENSE_ACCESS_DENIED);
			}
			int result = expenseMapper.delete(expenditureId, userId);
			if (result != 1) {
				throw new BusinessException(ErrorCode.EXPENSE_DELETE_FAILED, 
					"예상 삭제 수: 1, 실제: " + result);
			}
		} catch (BusinessException e) {
			throw e;
		} catch (Exception e) {
			log.error("소비내역 삭제 중 예상치 못한 오류 발생: expenditureId={}, userId={}, error={}", expenditureId, userId, e.getMessage(), e);
			throw new BusinessException(ErrorCode.EXPENSE_DELETE_FAILED, e);
		}
	}

	@Override
	public List<ExpenseVO> getRecentExpenses(Long userId) {
		try {
			return expenseMapper.getRecentExpenses(userId);
		} catch (Exception e) {
			log.error("최근 소비내역 조회 중 오류 발생: userId={}, error={}", userId, e.getMessage(), e);
			throw new BusinessException(ErrorCode.DATA_ACCESS_ERROR, e);
		}
	}

	@Override
	public ExpenseDTO getExpenseByIdInternal(Long expenditureId) {
		try {
			ExpenseVO vo = expenseMapper.findById(expenditureId);
			if (vo == null) {
				throw new BusinessException(ErrorCode.EXPENSE_NOT_FOUND);
			}
			return voToDto(vo);
		} catch (BusinessException e) {
			throw e;
		} catch (Exception e) {
			log.error("소비내역 내부 조회 중 오류 발생: expenditureId={}, error={}", expenditureId, e.getMessage(), e);
			throw new BusinessException(ErrorCode.DATA_ACCESS_ERROR, e);
		}
	}

	@Override
	public ExpenseDTO updateExpenseInternal(Long expenditureId, ExpenseDTO expenseDTO) {
		try {
			ExpenseVO vo = expenseMapper.findById(expenditureId);
			if (vo == null) {
				throw new BusinessException(ErrorCode.EXPENSE_NOT_FOUND);
			}

			vo.setCategoryId(expenseDTO.getCategoryId());
			vo.setAssetId(expenseDTO.getAssetId());
			vo.setAmount(expenseDTO.getAmount());
			vo.setDescription(expenseDTO.getDescription());
			vo.setExpenditureDate(expenseDTO.getExpenditureDate());

			int result = expenseMapper.update(vo);
			if (result != 1) {
				throw new BusinessException(ErrorCode.EXPENSE_UPDATE_FAILED, 
					"예상 업데이트 수: 1, 실제: " + result);
			}
			return voToDto(expenseMapper.findById(expenditureId));
		} catch (BusinessException e) {
			throw e;
		} catch (Exception e) {
			log.error("소비내역 내부 업데이트 중 오류 발생: expenditureId={}, error={}", expenditureId, e.getMessage(), e);
			throw new BusinessException(ErrorCode.EXPENSE_UPDATE_FAILED, e);
		}
	}

	private ExpenseVO dtoToVo(ExpenseDTO dto) {
		if (dto == null)
			return null;
		return ExpenseVO.builder()
			.userId(dto.getUserId())
			.categoryId(dto.getCategoryId())
			.assetId(dto.getAssetId())
			.amount(dto.getAmount())
			.description(dto.getDescription())
			.expenditureDate(dto.getExpenditureDate())
			.build();
	}

	private ExpenseDTO voToDto(ExpenseVO vo) {
		if (vo == null)
			return null;

		ExpenseDTO dto = ExpenseDTO.builder()
			.expenditureId(vo.getExpenditureId())
			.userId(vo.getUserId())
			.categoryId(vo.getCategoryId())
			.assetId(vo.getAssetId())
			.amount(vo.getAmount())
			.description(vo.getDescription())
			.expenditureDate(vo.getExpenditureDate())
			.createdAt(vo.getCreatedAt())
			.updatedAt(vo.getUpdatedAt())
			.build();

		if (vo.getCategoryId() != null) {
			CategoryVO categoryVO = categoryMapper.findById(vo.getCategoryId());
			if (categoryVO != null) {
				dto.setCategoryName(categoryVO.getName());
				dto.setCategoryIcon(categoryVO.getIcon());
			}
		}

		/* asset 패키지 완성 시 연동
		* if (vo.getAssetId() != null) {
			AssetVO assetVO = assetMapper.findById(vo.getAssetId());
			if (assetVO != null) {
				dto.setAssetName(assetVO.getAssetName());
				dto.setBankName(assetVO.getBankName());
			}
		}
		* */
		return dto;
	}
}
