package org.bbagisix.finproduct.service;

import org.bbagisix.exception.BusinessException;
import org.bbagisix.exception.ErrorCode;
import org.bbagisix.finproduct.domain.SavingBaseVO;
import org.bbagisix.finproduct.domain.SavingOptionVO;
import org.bbagisix.finproduct.dto.FssApiResponseDTO;
import org.bbagisix.finproduct.dto.ProductBaseDTO;
import org.bbagisix.finproduct.dto.ProductOptionDTO;
import org.bbagisix.finproduct.mapper.FinProductMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

// 데이터 저장 서비스
@Log4j2
@Service
@RequiredArgsConstructor
public class FinProductService {
    
    private final FssApiService fssApiService;
    private final FinProductMapper finProductMapper;
    
    // 금감원 API 데이터를 동기화하여 DB에 저장
    @Scheduled(cron = "0 0 4 ? * MON") // 매주 월요일 오전 4시에 실행
    @Transactional
    public void syncFinProductData() {
        try {
            log.info("금융상품 데이터 동기화 시작");
            
            // 1. 금감원 API에서 데이터 조회
            FssApiResponseDTO fssResponse = fssApiService.getSavingProductsFromFss();
            
            // 2. 조회된 데이터를 DB에 저장
            saveFinProductData(fssResponse);
            
            log.info("금융상품 데이터 동기화 완료");
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("금융상품 데이터 동기화 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.FINPRODUCT_DATA_SAVE_FAILED, e);
        }
    }
    
    // FSS API 응답 데이터를 DB에 저장
    @Transactional
    public void saveFinProductData(FssApiResponseDTO fssResponse) {
        try {
            if (fssResponse == null || fssResponse.getResult() == null) {
                throw new BusinessException(ErrorCode.FINPRODUCT_DATA_SAVE_FAILED, "저장할 데이터가 없습니다");
            }
            
            // 1. Base 데이터 저장
            int baseSavedCount = saveBaseData(fssResponse.getResult().getBaseList());
            
            // 2. Option 데이터 저장  
            int optionSavedCount = saveOptionData(fssResponse.getResult().getOptionList());
            
            log.info("데이터 저장 완료 - Base: {}건, Option: {}건", baseSavedCount, optionSavedCount);
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("FSS API 데이터 저장 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.FINPRODUCT_DATA_SAVE_FAILED, e);
        }
    }
    
    // Base 데이터 저장
    private int saveBaseData(java.util.List<ProductBaseDTO> baseList) {
        if (baseList == null || baseList.isEmpty()) {
            log.warn("저장할 Base 데이터가 없습니다");
            return 0;
        }
        
        int savedCount = 0;
        for (ProductBaseDTO baseDTO : baseList) {
            try {
                SavingBaseVO baseVO = convertToBaseVO(baseDTO);
                finProductMapper.insertOrUpdateBase(baseVO);
                savedCount++;
                
            } catch (Exception e) {
                log.error("Base 데이터 저장 실패 - 상품코드: {}, 오류: {}", 
                        baseDTO.getFinPrdtCd(), e.getMessage());
                // 개별 실패는 로그만 남기고 계속 진행
            }
        }
        
        return savedCount;
    }
    
    // Option 데이터 저장
    private int saveOptionData(java.util.List<ProductOptionDTO> optionList) {
        if (optionList == null || optionList.isEmpty()) {
            log.warn("저장할 Option 데이터가 없습니다");
            return 0;
        }
        
        int savedCount = 0;
        for (ProductOptionDTO optionDTO : optionList) {
            try {
                // 1. 같은 상품코드의 Base 데이터의 PK 조회
                SavingBaseVO baseVO = SavingBaseVO.builder()
                        .finCoNo(optionDTO.getFinCoNo())
                        .finPrdtCd(optionDTO.getFinPrdtCd()) // 같은 상품 코드
                        .dclsMonth(optionDTO.getDclsMonth())
                        .build();
                
                Long savingBaseId = finProductMapper.findSavingBaseId(baseVO);
                if (savingBaseId == null) {
                    log.warn("Option 저장 실패 - 연관된 Base 데이터를 찾을 수 없음: {}", optionDTO.getFinPrdtCd());
                    continue;
                }
                
                // 2. 각 Option을 개별 저장 (여러 번 호출)
                SavingOptionVO optionVO = convertToOptionVO(optionDTO, savingBaseId);
                finProductMapper.insertOrUpdateOption(optionVO); // 기간별로 여러 번 호출
                savedCount++;
                
            } catch (Exception e) {
                log.error("Option 데이터 저장 실패 - 상품코드: {}, 오류: {}", 
                        optionDTO.getFinPrdtCd(), e.getMessage());
                // 개별 실패는 로그만 남기고 계속 진행
            }
        }
        
        return savedCount;
    }
    
    // ProductBaseDTO를 SavingBaseVO로 변환
    private SavingBaseVO convertToBaseVO(ProductBaseDTO dto) {
        if (dto == null) return null;
        
        return SavingBaseVO.builder()
                .dclsMonth(dto.getDclsMonth())
                .finCoNo(dto.getFinCoNo())
                .finPrdtCd(dto.getFinPrdtCd())
                .korCoNm(dto.getKorCoNm())
                .finPrdtNm(dto.getFinPrdtNm())
                .joinWay(dto.getJoinWay())
                .mtrtInt(dto.getMtrtInt())
                .spclCnd(dto.getSpclCnd())
                .joinDeny(dto.getJoinDeny())
                .joinMember(dto.getJoinMember())
                .etcNote(dto.getEtcNote())
                .maxLimit(dto.getMaxLimit())
                .dclsStrtDay(dto.getDclsStrtDay())
                .dclsEndDay(dto.getDclsEndDay())
                .finCoSubmDay(dto.getFinCoSubmDay())
                .build();
    }
    
    // ProductOptionDTO를 SavingOptionVO로 변환
    private SavingOptionVO convertToOptionVO(ProductOptionDTO dto, Long savingBaseId) {
        if (dto == null) return null;
        
        return SavingOptionVO.builder()
                .savingBaseId(savingBaseId)
                .dclsMonth(dto.getDclsMonth())
                .finCoNo(dto.getFinCoNo())
                .finPrdtCd(dto.getFinPrdtCd())
                .intrRateType(dto.getIntrRateType())
                .intrRateTypeNm(dto.getIntrRateTypeNm())
                .rsrvType(dto.getRsrvType())
                .rsrvTypeNm(dto.getRsrvTypeNm())
                .saveTrm(dto.getSaveTrm())
                .intrRate(dto.getIntrRate())
                .intrRate2(dto.getIntrRate2())
                .build();
    }
}