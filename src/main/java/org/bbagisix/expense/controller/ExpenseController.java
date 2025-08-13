package org.bbagisix.expense.controller;

import java.util.List;
import java.util.Map;

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
import org.springframework.security.core.Authentication;
import org.bbagisix.user.dto.CustomOAuth2User;

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
	public ResponseEntity<List<ExpenseDTO>> getAllExpenses(Authentication authentication) {
		CustomOAuth2User currentUser = (CustomOAuth2User) authentication.getPrincipal();
		List<ExpenseDTO> expenses = expenseService.getExpensesByUserId(currentUser.getUserId());
		return ResponseEntity.ok(expenses);
	}

	@PostMapping
	public ResponseEntity<ExpenseDTO> createExpense(@RequestBody ExpenseDTO expenseDTO, Authentication authentication) {
		CustomOAuth2User currentUser = (CustomOAuth2User) authentication.getPrincipal();
		expenseDTO.setUserId(currentUser.getUserId());
		ExpenseDTO createdExpense = expenseService.createExpense(expenseDTO);
		return new ResponseEntity<>(createdExpense, HttpStatus.CREATED);
	}

	@GetMapping("/{expenditureId}")
	public ResponseEntity<ExpenseDTO> getExpenseById(@PathVariable Long expenditureId, Authentication authentication) {
		CustomOAuth2User currentUser = (CustomOAuth2User) authentication.getPrincipal();
		ExpenseDTO expense = expenseService.getExpenseById(expenditureId, currentUser.getUserId());
		return ResponseEntity.ok(expense);
	}

	@PutMapping("/{expenditureId}")
	public ResponseEntity<ExpenseDTO> updateExpense(@PathVariable Long expenditureId,
		@RequestBody ExpenseDTO expenseDTO, Authentication authentication) {
		CustomOAuth2User currentUser = (CustomOAuth2User) authentication.getPrincipal();
		ExpenseDTO updatedExpense = expenseService.updateExpense(expenditureId, expenseDTO, currentUser.getUserId());
		return ResponseEntity.ok(updatedExpense);
	}

	@DeleteMapping("/{expenditureId}")
	public ResponseEntity<ExpenseDTO> deleteExpense(@PathVariable Long expenditureId, Authentication authentication) {
		CustomOAuth2User currentUser = (CustomOAuth2User) authentication.getPrincipal();
		expenseService.deleteExpense(expenditureId, currentUser.getUserId());
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/categories")
	public ResponseEntity<List<CategoryDTO>> getAllCategories() {
		return ResponseEntity.ok(categoryService.getAllCategories());
	}

	@PostMapping("/refresh")
	public ResponseEntity<String> refreshExpensesFromCodef(Authentication authentication) {
		CustomOAuth2User currentUser = (CustomOAuth2User) authentication.getPrincipal();
		expenseService.refreshFromCodef(currentUser.getUserId());
		return ResponseEntity.ok("거래내역 새로고침이 완료되었습니다.");
	}

	@GetMapping("/current-month-summary")
	public ResponseEntity<Map<String, Long>> getCurrentMonthSummary(Authentication authentication) {
		CustomOAuth2User currentUser = (CustomOAuth2User) authentication.getPrincipal();
		Map<String, Long> summary = expenseService.getCurrentMonthSummary(currentUser.getUserId());
		return ResponseEntity.ok(summary);
	}
}
