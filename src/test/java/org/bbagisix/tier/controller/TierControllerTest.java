package org.bbagisix.tier.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.bbagisix.tier.dto.TierDTO;
import org.bbagisix.tier.service.TierService;
import org.bbagisix.user.dto.CustomOAuth2User;
import org.bbagisix.user.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class TierControllerTest {

	private MockMvc mockMvc;

	@Mock
	private TierService tierService;

	@InjectMocks
	private TierController tierController;

	private ObjectMapper objectMapper = new ObjectMapper();
	private TierDTO bronzeTier;
	private TierDTO silverTier;
	private TierDTO goldTier;
	private CustomOAuth2User mockUser;
	private Authentication mockAuthentication;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(tierController).build();

		// 테스트 데이터 준비
		bronzeTier = TierDTO.builder()
			.tierId(1L)
			.name("브론즈")
			.build();

		silverTier = TierDTO.builder()
			.tierId(2L)
			.name("실버")
			.build();

		goldTier = TierDTO.builder()
			.tierId(3L)
			.name("골드")
			.build();

		// Mock 사용자 생성
		UserResponse userResponse = UserResponse.builder()
			.userId(1L)
			.name("테스트유저")
			.email("test@test.com")
			.nickname("테스트닉네임")
			.role("ROLE_USER")
			.build();

		mockUser = new CustomOAuth2User(userResponse);
		mockAuthentication = new UsernamePasswordAuthenticationToken(
			mockUser, null, mockUser.getAuthorities());
	}

	@Test
	@DisplayName("사용자 현재 티어 조회 테스트 - 성공")
	void getUserCurrentTier_Success() throws Exception {
		// given
		when(tierService.getUserCurrentTier(1L)).thenReturn(silverTier);

		// when
		MvcResult result = mockMvc.perform(get("/api/user/me/tiers")
				.principal(mockAuthentication)
				.characterEncoding("UTF-8"))
			.andExpect(status().isOk())
			.andReturn();

		// then - JSON 응답을 UTF-8로 읽기
		String jsonResponse = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
		TierDTO responseDto = objectMapper.readValue(jsonResponse, TierDTO.class);

		assertEquals(2L, responseDto.getTierId());
		assertEquals("실버", responseDto.getName());

		verify(tierService, times(1)).getUserCurrentTier(1L);
	}

	@Test
	@DisplayName("사용자 현재 티어 조회 테스트 - 티어 없음")
	void getUserCurrentTier_NoTier() throws Exception {
		// given
		when(tierService.getUserCurrentTier(1L)).thenReturn(null);

		// when
		MvcResult result = mockMvc.perform(get("/api/user/me/tiers")
				.principal(mockAuthentication)
				.characterEncoding("UTF-8"))
			.andExpect(status().isOk())
			.andReturn();

		// then
		String content = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
		assertTrue(content.isEmpty() || content.equals("null"));

		verify(tierService, times(1)).getUserCurrentTier(1L);
	}

	@Test
	@DisplayName("전체 티어 목록 조회 테스트 - 성공")
	void getAllTiers_Success() throws Exception {
		// given
		List<TierDTO> allTiers = Arrays.asList(bronzeTier, silverTier, goldTier);
		when(tierService.getAllTiers()).thenReturn(allTiers);

		// when
		MvcResult result = mockMvc.perform(get("/api/tiers/all")
				.principal(mockAuthentication)
				.characterEncoding("UTF-8"))
			.andExpect(status().isOk())
			.andReturn();

		// then - JSON 배열을 UTF-8로 읽기
		String jsonResponse = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
		TierDTO[] responseArray = objectMapper.readValue(jsonResponse, TierDTO[].class);

		assertEquals(3, responseArray.length);
		assertEquals(1L, responseArray[0].getTierId());
		assertEquals("브론즈", responseArray[0].getName());
		assertEquals(2L, responseArray[1].getTierId());
		assertEquals("실버", responseArray[1].getName());
		assertEquals(3L, responseArray[2].getTierId());
		assertEquals("골드", responseArray[2].getName());

		verify(tierService, times(1)).getAllTiers();
	}

	@Test
	@DisplayName("컨트롤러 직접 호출 테스트 - getUserCurrentTier")
	void directCall_getUserCurrentTier() {
		// given
		when(tierService.getUserCurrentTier(1L)).thenReturn(silverTier);

		// when
		ResponseEntity<TierDTO> response = tierController.getUserCurrentTier(mockAuthentication);

		// then
		assertEquals(200, response.getStatusCodeValue());
		assertEquals(2L, response.getBody().getTierId());
		assertEquals("실버", response.getBody().getName());

		verify(tierService, times(1)).getUserCurrentTier(1L);
	}

	@Test
	@DisplayName("컨트롤러 직접 호출 테스트 - getAllTiers")
	void directCall_getAllTiers() {
		// given
		List<TierDTO> allTiers = Arrays.asList(bronzeTier, silverTier, goldTier);
		when(tierService.getAllTiers()).thenReturn(allTiers);

		// when
		ResponseEntity<List<TierDTO>> response = tierController.getAllTiers();

		// then
		assertEquals(200, response.getStatusCodeValue());
		assertEquals(3, response.getBody().size());
		assertEquals("브론즈", response.getBody().get(0).getName());
		assertEquals("실버", response.getBody().get(1).getName());
		assertEquals("골드", response.getBody().get(2).getName());

		verify(tierService, times(1)).getAllTiers();
	}
}