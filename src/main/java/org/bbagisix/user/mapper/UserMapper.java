package org.bbagisix.user.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.bbagisix.user.domain.UserVO;

@Mapper
public interface UserMapper {

	// 회원가입
	int insertUser(UserVO user);

	// 조회 메서드
	UserVO findByUserId(@Param("userId") Long userId);
	UserVO selectUserByEmail(@Param("email") String email);
	UserVO selectUserById(@Param("userId") Long userId);
	UserVO findByName(@Param("name") String name);
	UserVO findBySocialId(@Param("socialId") String socialId);
	UserVO findByEmail(@Param("email") String email);
	String getNameByUserId(@Param("userId") Long userId);

	// 이메일 중복 체크
	int countByEmail(@Param("email") String email);

	// 닉네임 업데이트
	int updateNickname(@Param("userId") Long userId, @Param("nickname") String nickname);

	// 계좌 연동 여부 업데이트
	void updateAssetConnected(@Param("userId") Long userId, @Param("assetConnected") Boolean assetConnected);
}
