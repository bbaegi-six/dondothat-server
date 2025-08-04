package org.bbagisix.expense.service;

import java.util.List;
import java.util.stream.Collectors;

import org.bbagisix.category.domain.CategoryVO;
import org.bbagisix.category.mapper.CategoryMapper;
import org.bbagisix.expense.domain.ExpenseVO;
import org.bbagisix.expense.dto.ExpenseDTO;
import org.bbagisix.expense.mapper.ExpenseMapper;
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
		ExpenseVO vo = dtoToVo(expenseDTO);
		expenseMapper.insert(vo);
		return voToDto(vo);
	}

	@Override
	public ExpenseDTO getExpenseById(Long expenditureId, Long userId) {
		ExpenseVO vo = expenseMapper.findById(expenditureId);
		if (vo == null || !vo.getUserId().equals(userId)) {
			throw new RuntimeException("소비 내역을 찾을 수 없거나 접근 권한이 없습니다. id=" + expenditureId);
		}
		return voToDto(vo);
	}

	@Override
	public List<ExpenseDTO> getExpensesByUserId(Long userId) {
		return expenseMapper.findAllByUserId(userId).stream().map(this::voToDto).collect(Collectors.toList());
	}

	@Override
	public ExpenseDTO updateExpense(Long expenditureId, ExpenseDTO expenseDTO, Long userId) {
		ExpenseVO vo = expenseMapper.findById(expenditureId);
		if (vo == null || !vo.getUserId().equals(userId)) {
			throw new RuntimeException("수정할 소비 내역을 찾을 수 없거나 접근 권한이 없습니다. id=" + expenditureId);
		}

		vo.setCategoryId(expenseDTO.getCategoryId());
		vo.setAssetId(expenseDTO.getAssetId());
		vo.setAmount(expenseDTO.getAmount());
		vo.setDescription(expenseDTO.getDescription());
		vo.setExpenditureDate(expenseDTO.getExpenditureDate());

		expenseMapper.update(vo);
		return voToDto(expenseMapper.findById(expenditureId));
	}

	@Override
	public void deleteExpense(Long expenditureId, Long userId) {
		ExpenseVO vo = expenseMapper.findById(expenditureId);
		if (vo == null || !vo.getUserId().equals(userId)) {
			throw new RuntimeException("삭제할 소비 내역을 찾을 수 없거나 접근 권한이 없습니다. id=" + expenditureId);
		}
		expenseMapper.delete(expenditureId);
	}

	@Override
	public List<ExpenseVO> getRecentExpenses(Long userId) {
		return expenseMapper.getRecentExpenses(userId);
	}

	@Override
	public ExpenseDTO getExpenseByIdInternal(Long expenditureId) {
		ExpenseVO vo = expenseMapper.findById(expenditureId);
		return voToDto(vo);
	}

	@Override
	public ExpenseDTO updateExpenseInternal(Long expenditureId, ExpenseDTO expenseDTO) {
		ExpenseVO vo = expenseMapper.findById(expenditureId);
		if (vo == null) {
			throw new RuntimeException("수정할 소비 내역이 없습니다. id=" + expenditureId);
		}

		vo.setCategoryId(expenseDTO.getCategoryId());
		vo.setAssetId(expenseDTO.getAssetId());
		vo.setAmount(expenseDTO.getAmount());
		vo.setDescription(expenseDTO.getDescription());
		vo.setExpenditureDate(expenseDTO.getExpenditureDate());

		expenseMapper.update(vo);
		return voToDto(expenseMapper.findById(expenditureId));
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
