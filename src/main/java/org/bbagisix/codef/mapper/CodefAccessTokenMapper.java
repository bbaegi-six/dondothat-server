package org.bbagisix.codef.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.bbagisix.codef.domain.CodefAccessTokenVO;

@Mapper
public interface CodefAccessTokenMapper {
	CodefAccessTokenVO getCurrentToken();

	int insertToken(CodefAccessTokenVO tokenVO);

	int updateToken(CodefAccessTokenVO tokenVO);
}
