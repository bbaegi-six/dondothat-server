package org.bbagisix.common.codef.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.bbagisix.common.codef.domain.CodefAccessTokenVO;

@Mapper
public interface CodefAccessTokenMapper {
	CodefAccessTokenVO getCurrentToken();

	int insertToken(CodefAccessTokenVO tokenVO);

	int updateToken(CodefAccessTokenVO tokenVO);
}
