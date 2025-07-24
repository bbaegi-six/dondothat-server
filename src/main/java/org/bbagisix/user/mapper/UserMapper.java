package org.bbagisix.user.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.bbagisix.user.domain.UserVO;

@Mapper
public interface UserMapper {

	// 회원가입
	int insertUser(UserVO user);

	// 조회 메서드
	UserVO selectUserByEmail(@Param("email") String email);
	UserVO selectUserById(@Param("id") Long id);

	// 이메일 중복 체크
	int countByEmail(@Param("email") String email);
}
