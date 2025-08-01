package org.bbagisix.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.bbagisix.user.dto.CustomOAuth2User;
import org.bbagisix.user.dto.UserDTO;
import org.bbagisix.user.dto.GoogleResponse;
import org.bbagisix.user.dto.NaverResponse;
import org.bbagisix.user.dto.OAuth2Response;
import org.bbagisix.user.domain.UserVO;
import org.bbagisix.user.mapper.UserMapper;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserMapper userMapper;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        log.info("OAuth2 로그인, DB 조회 및 가입 처리 시작");
        
        // 기본 OAuth2UserService로 사용자 정보 가져오기
        OAuth2User oAuth2User = super.loadUser(userRequest);
        System.out.println(oAuth2User);
        
        // OAuth2 제공자 정보 (google, naver 등)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2Response oAuth2Response = null;

        if (registrationId.equals("naver")) {
            oAuth2Response = new NaverResponse(oAuth2User.getAttributes());
        } else if (registrationId.equals("google")) {
            oAuth2Response = new GoogleResponse(oAuth2User.getAttributes());
        } else {
            log.error("지원하지 않는 소셜 로그인입니다.");
            return null;
        }
        String socialId = oAuth2Response.getProvider() + "_" + oAuth2Response.getProviderId();
        String email = oAuth2Response.getEmail();
        UserVO userVO = userMapper.findByEmail(email);

        if (userVO == null) {
            log.info("신규 사용자입니다. DB에 저장합니다.");
            userVO = UserVO.builder()
                .name(oAuth2Response.getName())
                .socialId(socialId)
                .email(email)
                .nickname("기본 닉네임")
                .socialId(socialId)
                .role("ROLE_USER")
                .build();

            userMapper.insertUser(userVO);
        } else {
            log.info("기존 사용자입니다.");
        }

        UserDTO userDTO = new UserDTO();
        userDTO.setName(userVO.getName());
        userDTO.setNickname(userVO.getNickname());
        userDTO.setRole(userVO.getRole());
        userDTO.setEmail(userVO.getEmail());

        return new CustomOAuth2User(userDTO);
    }
}
