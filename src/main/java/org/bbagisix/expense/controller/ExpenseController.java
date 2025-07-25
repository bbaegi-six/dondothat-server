package org.bbagisix.expense.controller;

import java.util.List;

import org.bbagisix.category.dto.CategoryDTO;
import org.bbagisix.category.service.CategoryService;
import org.bbagisix.expense.dto.ExpenseDTO;
import org.bbagisix.expense.service.ExpenseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@Log4j2
public class ExpenseController {
	private final ExpenseService expenseService;
	private final CategoryService categoryService;

	@GetMapping
	public ResponseEntity<List<ExpenseDTO>> getAllExpenses(@RequestParam Long userId) {
		List<ExpenseDTO> expenses = expenseService.getExpensesByUserId(userId);
		return ResponseEntity.ok(expenses);
	}

	@PostMapping
	public ResponseEntity<ExpenseDTO> createExpense(@RequestBody ExpenseDTO expenseDTO) {
		// user 패키지 구현 시 수정
		expenseDTO.setUserId(1L);
		ExpenseDTO createdExpense = expenseService.createExpense(expenseDTO);
		return new ResponseEntity<>(createdExpense, HttpStatus.CREATED);
	}

	@GetMapping("/{expenditureId}")
	public ResponseEntity<ExpenseDTO> getExpenseById(@PathVariable Long expenditureId) {
		ExpenseDTO expense = expenseService.getExpenseById(expenditureId);
		return ResponseEntity.ok(expense);
	}

	@PutMapping("/{expenditureId}")
	public ResponseEntity<ExpenseDTO> updateExpense(@PathVariable Long expenditureId,
		@RequestBody ExpenseDTO expenseDTO) {
		ExpenseDTO updatedExpense = expenseService.updateExpense(expenditureId, expenseDTO);
		return ResponseEntity.ok(updatedExpense);
	}

	@DeleteMapping("/{expenditureId}")
	public ResponseEntity<ExpenseDTO> deleteExpense(@PathVariable Long expenditureId) {
		expenseService.deleteExpense(expenditureId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/categories")
	public ResponseEntity<List<CategoryDTO>> getAllCategories() {
		return ResponseEntity.ok(categoryService.getAllCategories());
	}
}
