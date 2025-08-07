package org.bbagisix.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.bbagisix.exception.BusinessException;
import org.bbagisix.exception.ErrorCode;
import org.bbagisix.user.domain.UserVO;
import org.bbagisix.user.dto.CustomOAuth2User;
import org.bbagisix.user.dto.SignUpRequest;
import org.bbagisix.user.mapper.UserMapper;
import org.bbagisix.user.service.EmailService;
import org.bbagisix.user.service.UserService;
import org.bbagisix.user.service.VerificationStorageService;
import org.bbagisix.user.util.CookieUtil;
import org.bbagisix.user.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 테스트 - 일반 회원가입/로그인")
public class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private UserService userService;

    private UserVO testUser;
    private SignUpRequest signUpRequest;

    @BeforeEach
    void setUp() {
        testUser = UserVO.builder()
                .userId(1L)
                .name("테스트사용자")
                .email("test@example.com")
                .password("encodedPassword")
                .nickname("테스트닉네임")
                .role("USER")
                .emailVerified(true)
                .assetConnected(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        signUpRequest = SignUpRequest.builder()
                .name("테스트사용자")
                .email("test@example.com")
                .password("password123")
                .nickname("테스트닉네임")
                .build();
    }

    @Test
    @DisplayName("중복되지 않은 이메일 체크")
    void isEmailDuplicate_NotDuplicate() {
        // Given
        String email = "new@example.com";
        when(userMapper.countByEmail(email)).thenReturn(0);

        // When
        boolean result = userService.isEmailDuplicate(email);

        // Then
        assertFalse(result);
        verify(userMapper).countByEmail(email);
    }

    @Test
    @DisplayName("중복된 이메일 체크")
    void isEmailDuplicate_Duplicate() {
        // Given
        String email = "existing@example.com";
        when(userMapper.countByEmail(email)).thenReturn(1);

        // When
        boolean result = userService.isEmailDuplicate(email);

        // Then
        assertTrue(result);
        verify(userMapper).countByEmail(email);
    }

    @Test
    @DisplayName("이미 존재하는 이메일로 회원가입 시도")
    void signUp_EmailDuplicate() {
        // Given
        when(userMapper.countByEmail(signUpRequest.getEmail())).thenReturn(1);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, 
                () -> userService.signUp(signUpRequest, response));
        
        assertEquals(ErrorCode.EMAIL_ALREADY_EXISTS, exception.getErrorCode());
        verify(userMapper).countByEmail(signUpRequest.getEmail());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userMapper, never()).insertUser(any(UserVO.class));
    }

    @Test
    @DisplayName("정상적인 회원가입")
    void signUp_Success() {
        // Given
        when(userMapper.countByEmail(signUpRequest.getEmail())).thenReturn(0);
        when(passwordEncoder.encode(signUpRequest.getPassword())).thenReturn("encodedPassword");
        when(userMapper.insertUser(any(UserVO.class))).thenReturn(1);
        // 실제 호출될 인수에 맞춰 Mock 설정
        when(jwtUtil.createToken(
                eq("test@example.com"), 
                eq("USER"), 
                eq("테스트사용자"), 
                eq("테스트닉네임"), 
                isNull(), // userId는 null로 전달됨
                eq(24 * 60 * 60 * 1000L)))
                .thenReturn("test.jwt.token");

        try (MockedStatic<CookieUtil> mockedCookieUtil = mockStatic(CookieUtil.class)) {
            // When
            String result = userService.signUp(signUpRequest, response);

            // Then
            assertEquals("회원가입 완료.", result);
            verify(userMapper).countByEmail(signUpRequest.getEmail());
            verify(passwordEncoder).encode(signUpRequest.getPassword());
            verify(userMapper).insertUser(any(UserVO.class));
            mockedCookieUtil.verify(() -> CookieUtil.addJwtCookie(response, "test.jwt.token"));
        }
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 로그인 시도")
    void login_UserNotFound() {
        // Given
        String email = "nonexistent@example.com";
        String password = "password123";
        
        when(userMapper.findByEmail(email)).thenReturn(null);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, 
                () -> userService.login(email, password, response));
        
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(userMapper).findByEmail(email);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("잘못된 비밀번호로 로그인 시도")
    void login_WrongPassword() {
        // Given
        String email = "test@example.com";
        String password = "wrongPassword";
        
        when(userMapper.findByEmail(email)).thenReturn(testUser);
        when(passwordEncoder.matches(password, testUser.getPassword())).thenReturn(false);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, 
                () -> userService.login(email, password, response));
        
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(userMapper).findByEmail(email);
        verify(passwordEncoder).matches(password, testUser.getPassword());
    }

    @Test
    @DisplayName("계좌 연동 여부 업데이트")
    void updateAssetConnected() {
        // Given
        Long userId = 1L;
        boolean assetConnected = true;

        Authentication authentication = mock(Authentication.class);
        CustomOAuth2User customOAuth2User = mock(CustomOAuth2User.class);

        when(authentication.getPrincipal()).thenReturn(customOAuth2User);
        when(customOAuth2User.getUserId()).thenReturn(userId);

        // When
        String result = userService.updateAssetConnected(assetConnected, authentication);

        // Then
        assertEquals("계좌 연동 여부 업데이트 완료: " + assetConnected, result);
        verify(userMapper).updateAssetConnected(userId, assetConnected);
    }

    @Test
    @DisplayName("정상적인 로그인")
    void login_Success() {
        // Given
        String email = "test@example.com";
        String password = "password123";
        
        when(userMapper.findByEmail(email)).thenReturn(testUser);
        when(passwordEncoder.matches(password, testUser.getPassword())).thenReturn(true);
        // 실제 호출될 인수에 맞춰 Mock 설정
        when(jwtUtil.createToken(
                eq("test@example.com"),
                eq("USER"),
                eq("테스트사용자"),
                eq("테스트닉네임"),
                eq(1L), // testUser의 userId
                eq(24 * 60 * 60 * 1000L)))
                .thenReturn("test.jwt.token");

        try (MockedStatic<CookieUtil> mockedCookieUtil = mockStatic(CookieUtil.class)) {
            // When
            String result = userService.login(email, password, response);

            // Then
            assertEquals("로그인 성공.", result);
            verify(userMapper).findByEmail(email);
            verify(passwordEncoder).matches(password, testUser.getPassword());
            mockedCookieUtil.verify(() -> CookieUtil.addJwtCookie(response, "test.jwt.token"));
        }
    }
}
