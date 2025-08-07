package org.bbagisix.finproduct.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.bbagisix.finproduct.domain.SavingBaseVO;
import org.bbagisix.finproduct.domain.SavingOptionVO;

@Mapper
public interface FinProductMapper {

    void insertOrUpdateBase(SavingBaseVO baseVO);

    void insertOrUpdateOption(SavingOptionVO optionVO);

    // 복합 키(fin_co_no, fin_prdt_cd, dcls_month)를 기준으로 saving_base 테이블의 PK를 조회
    // 옵션 정보를 saving_option 테이블에 저장할 때, 방금 알아낸 `saving_base_id` 값을 `saving_base_id` 컬럼에 넣음
    Long findSavingBaseId(SavingBaseVO baseVO);
}
