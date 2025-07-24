package org.bbagisix.expense.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.bbagisix.category.dto.CategoryDTO;
import org.bbagisix.config.RootConfig;
import org.bbagisix.config.ServletConfig;
import org.bbagisix.expense.dto.ExpenseDTO;
import org.bbagisix.expense.service.ExpenseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.log4j.Log4j2;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = { RootConfig.class, ServletConfig.class })
@Transactional
@Log4j2
class ExpenseControllerTest {

	@Autowired
	private WebApplicationContext ctx;

	@Autowired
	private ExpenseService expenseService;

	private MockMvc mockMvc;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void setUp() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(ctx)
			.addFilter(new CharacterEncodingFilter("UTF-8", true))
			.build();
	}

	private ExpenseDTO createTestExpenseDTO(String description, Long amount) {
		return ExpenseDTO.builder()
			.userId(1L)
			.categoryId(1L)
			.assetId(1L)
			.amount(amount)
			.description(description)
			.expenditureDate(new java.util.Date())
			.build();
	}

	@Test
	@DisplayName("POST /api/expenses - 지출 내역 생성 요청")
	void createExpense() throws Exception {
		// given
		ExpenseDTO newExpense = createTestExpenseDTO("컨트롤러 테스트", 120000L);
		String jsonContent = objectMapper.writeValueAsString(newExpense);

		// when
		MvcResult result = mockMvc.perform(post("/api/expenses")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonContent))
			.andExpect(status().isCreated())
			.andDo(print())
			.andReturn();

		// then
		String responseBody = new String(result.getResponse().getContentAsByteArray(), "UTF-8");
		ExpenseDTO responseDTO = objectMapper.readValue(responseBody, ExpenseDTO.class);

		assertNotNull(responseDTO);
		assertNotNull(responseDTO.getExpenditureId());
		assertEquals("컨트롤러 테스트", responseDTO.getDescription());
	}

	@Test
	@DisplayName("GET /api/expenses/{id} - 지출 내역 상세 조회")
	void getExpenseById() throws Exception {
		// given
		ExpenseDTO createdExpense = expenseService.createExpense(createTestExpenseDTO("컨트롤러 테스트", 120000L));
		Long expenseId = createdExpense.getExpenditureId();

		// when
		MvcResult result = mockMvc.perform(get("/api/expenses/" + expenseId)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andDo(print())
			.andReturn();

		// then
		String responseBody = new String(result.getResponse().getContentAsByteArray(), "UTF-8");
		ExpenseDTO responseDTO = objectMapper.readValue(responseBody, ExpenseDTO.class);
		assertEquals(expenseId, responseDTO.getExpenditureId());
		assertEquals("컨트롤러 테스트", responseDTO.getDescription());
	}

	@Test
	@DisplayName("PUT /api/expenses/{id} - 지출 내역 수정")
	void updateExpense() throws Exception {
		// given
		ExpenseDTO createdExpense = expenseService.createExpense(createTestExpenseDTO("원본 데이터", 120000L));
		Long expenseId = createdExpense.getExpenditureId();

		ExpenseDTO updatedDto = ExpenseDTO.builder()
			.userId(1L)
			.categoryId(2L)
			.assetId(1L)
			.amount(9999L)
			.description("수정된 데이터")
			.expenditureDate(new java.util.Date())
			.build();
		String jsonContent = objectMapper.writeValueAsString(updatedDto);

		// when
		MvcResult result = mockMvc.perform(put("/api/expenses/" + expenseId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonContent))
			.andExpect(status().isOk())
			.andDo(print())
			.andReturn();

		// then
		String responseBody = new String(result.getResponse().getContentAsByteArray(), "UTF-8");
		ExpenseDTO responseDTO = objectMapper.readValue(responseBody, ExpenseDTO.class);
		assertEquals("수정된 데이터", responseDTO.getDescription());
		assertEquals(9999L, responseDTO.getAmount());
	}

	@Test
	@DisplayName("DELETE /api/expenses/{id} - 지출 내역 삭제")
	void deleteExpense() throws Exception {
		// given
		ExpenseDTO createdExpense = expenseService.createExpense(createTestExpenseDTO("삭제될 데이터", 120000L));
		Long expenseId = createdExpense.getExpenditureId();

		// when & then
		mockMvc.perform(delete("/api/expenses/" + expenseId))
			.andExpect(status().isNoContent())
			.andDo(print());

		// verify
		assertNull(expenseService.getExpenseById(expenseId));
	}

	@Test
	@DisplayName("GET /api/expenses/categories - 카테고리 목록 조회")
	void getAllCategories() throws Exception {
		// when
		MvcResult result = mockMvc.perform(get("/api/expenses/categories"))
			.andExpect(status().isOk())
			.andDo(print())
			.andReturn();

		// then
		String responseBody = new String(result.getResponse().getContentAsByteArray(), "UTF-8");
		List<CategoryDTO> categories = objectMapper.readValue(responseBody, new TypeReference<List<CategoryDTO>>() {});
		assertNotNull(categories);
	}
}
