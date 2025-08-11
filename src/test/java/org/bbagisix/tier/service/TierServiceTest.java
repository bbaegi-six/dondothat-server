package org.bbagisix.tier.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;

import org.bbagisix.tier.domain.TierVO;
import org.bbagisix.tier.dto.TierDTO;
import org.bbagisix.tier.mapper.TierMapper;
import org.bbagisix.user.domain.UserVO;
import org.bbagisix.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TierServiceTest {

	@Mock
	private TierMapper tierMapper;

	@Mock
	private UserMapper userMapper;

	@InjectMocks
	private TierService tierService;

	private TierVO bronzeTier;
	private TierVO silverTier;
	private TierVO goldTier;
	private UserVO testUser;

	@BeforeEach
	void setUp() {
		// 테스트 데이터 준비
		bronzeTier = TierVO.builder()
			.tierId(1L)
			.name("브론즈")
			.build();

		silverTier = TierVO.builder()
			.tierId(2L)
			.name("실버")
			.build();

		goldTier = TierVO.builder()
			.tierId(3L)
			.name("골드")
			.build();

		testUser = UserVO.builder()
			.userId(1L)
			.name("테스트유저")
			.email("test@test.com")
			.tierId(1L) // 브론즈 티어
			.build();
	}

	@Test
	@DisplayName("전체 티어 목록 조회 테스트")
	void getAllTiers() {
		// given
		List<TierVO> mockTiers = Arrays.asList(bronzeTier, silverTier, goldTier);
		when(tierMapper.findAllTiers()).thenReturn(mockTiers);

		// when
		List<TierDTO> result = tierService.getAllTiers();

		// then
		assertEquals(3, result.size());
		assertEquals("브론즈", result.get(0).getName());
		assertEquals("실버", result.get(1).getName());
		assertEquals("골드", result.get(2).getName());

		verify(tierMapper, times(1)).findAllTiers();
	}

	@Test
	@DisplayName("사용자 현재 티어 조회 테스트 - 정상")
	void getUserCurrentTier_Success() {
		// given
		when(userMapper.findByUserId(1L)).thenReturn(testUser);
		when(tierMapper.findById(1L)).thenReturn(bronzeTier);

		// when
		TierDTO result = tierService.getUserCurrentTier(1L);

		// then
		assertNotNull(result);
		assertEquals(1L, result.getTierId());
		assertEquals("브론즈", result.getName());

		verify(userMapper, times(1)).findByUserId(1L);
		verify(tierMapper, times(1)).findById(1L);
	}

	@Test
	@DisplayName("사용자 현재 티어 조회 테스트 - tier_id가 null")
	void getUserCurrentTier_TierIdNull() {
		// given
		UserVO userWithoutTier = UserVO.builder()
			.userId(1L)
			.name("신규유저")
			.tierId(null) // 티어 없음
			.build();

		when(userMapper.findByUserId(1L)).thenReturn(userWithoutTier);

		// when
		TierDTO result = tierService.getUserCurrentTier(1L);

		// then
		assertNull(result);
		verify(userMapper, times(1)).findByUserId(1L);
		verify(tierMapper, never()).findById(any());
	}

	@Test
	@DisplayName("티어 승급 테스트 - 첫 승급 (null → 1)")
	void promoteUserTier_FirstPromotion() {
		// given
		UserVO newUser = UserVO.builder()
			.userId(1L)
			.tierId(null) // 아직 티어 없음
			.build();

		when(userMapper.findByUserId(1L)).thenReturn(newUser);
		when(tierMapper.findById(1L)).thenReturn(bronzeTier);

		// when
		tierService.promoteUserTier(1L);

		// then
		verify(userMapper, times(1)).findByUserId(1L);
		verify(tierMapper, times(1)).findById(1L);
		verify(userMapper, times(1)).updateUserTier(1L, 1L);
	}

	@Test
	@DisplayName("티어 승급 테스트 - 일반 승급 (1 → 2)")
	void promoteUserTier_NormalPromotion() {
		// given
		when(userMapper.findByUserId(1L)).thenReturn(testUser);
		when(tierMapper.findById(2L)).thenReturn(silverTier);

		// when
		tierService.promoteUserTier(1L);

		// then
		verify(userMapper, times(1)).findByUserId(1L);
		verify(tierMapper, times(1)).findById(2L);
		verify(userMapper, times(1)).updateUserTier(1L, 2L);
	}

	@Test
	@DisplayName("티어 승급 테스트 - 최고 티어 (승급 안됨)")
	void promoteUserTier_MaxTier() {
		// given
		UserVO maxTierUser = UserVO.builder()
			.userId(1L)
			.tierId(3L) // 골드 티어 (최고)
			.build();

		when(userMapper.findByUserId(1L)).thenReturn(maxTierUser);
		when(tierMapper.findById(4L)).thenReturn(null); // 다음 티어 없음

		// when
		tierService.promoteUserTier(1L);

		// then
		verify(userMapper, times(1)).findByUserId(1L);
		verify(tierMapper, times(1)).findById(4L);
		verify(userMapper, never()).updateUserTier(any(), any()); // 승급 안됨
	}
}