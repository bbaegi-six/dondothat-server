package org.bbagisix.codef.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.bbagisix.codef.domain.CodefAccessTokenVO;

@Mapper
public interface CodefAccessTokenMapper {
	CodefAccessTokenVO getCurrentToken();

	void insertToken(CodefAccessTokenVO tokenVO);

	void updateToken(CodefAccessTokenVO tokenVO);
}
