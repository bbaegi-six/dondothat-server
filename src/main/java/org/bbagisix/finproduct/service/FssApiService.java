package org.bbagisix.finproduct.service;

import org.bbagisix.exception.BusinessException;
import org.bbagisix.exception.ErrorCode;
import org.bbagisix.finproduct.dto.FssApiResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

// 금감원 API 호출 서비스
@Log4j2
@Service
@RequiredArgsConstructor
public class FssApiService {
    
    // 상수 정의
    private static final String BANK_SECTOR_CODE = "020000"; // 은행권
    private static final String SUCCESS_CODE = "000"; // 정상 응답 코드
    private static final int MAX_RETRY_COUNT = 3; // 최대 재시도 횟수
    private static final long RETRY_DELAY_MS = 1000L; // 재시도 간격 (1초)

    private final RestTemplate restTemplate;
    
    @Value("${FSS_API_KEY}")
    private String apiKey;
    
    @Value("${FSS_API_URL}")
    private String apiUrl;
    

    // 금감원 적금상품 API에서 은행권 데이터를 수집
    public FssApiResponseDTO getSavingProductsFromFss() {
        try {
            log.info("금감원 API 호출 시작 - 은행권 적금상품 데이터 수집");
            
            FssApiResponseDTO response = callFssApi();
            if (response == null || !isSuccessResponse(response)) {
                throw new BusinessException(ErrorCode.FSS_API_CALL_FAILED);
            }
            
            int baseCount = response.getResult().getBaseList() != null ? response.getResult().getBaseList().size() : 0;
            int optionCount = response.getResult().getOptionList() != null ? response.getResult().getOptionList().size() : 0;
            
            log.info("금감원 API 호출 완료 - Base: {}건, Option: {}건", baseCount, optionCount);
            
            return response;
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("금감원 API 호출 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.FSS_API_RESPONSE_ERROR, e);
        }
    }
    
    // 금감원 API 호출
    private FssApiResponseDTO callFssApi() {
        String url = buildApiUrl();
        
        for (int attempt = 1; attempt <= MAX_RETRY_COUNT; attempt++) {
            try {
                log.debug("API 호출 시도 {}/{}", attempt, MAX_RETRY_COUNT);
                
                FssApiResponseDTO response = restTemplate.getForObject(url, FssApiResponseDTO.class);
                
                if (response != null && isSuccessResponse(response)) {
                    log.debug("API 호출 성공");
                    return response;
                } else if (response != null) {
                    log.warn("API 응답 에러 - 코드: {}, 메시지: {}", 
                            response.getResult().getErrCd(), response.getResult().getErrMsg());
                } else {
                    log.warn("API 응답이 null");
                }
                
            } catch (Exception e) {
                log.warn("API 호출 실패 (시도 {}/{}): {}", attempt, MAX_RETRY_COUNT, e.getMessage());
            }
            
            // 마지막 시도가 아니면 잠시 대기
            if (attempt < MAX_RETRY_COUNT) {
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        log.error("API 호출 최종 실패 - 최대 재시도 횟수 초과");
        return null;
    }
    
    // API URL 생성
    private String buildApiUrl() {
        return UriComponentsBuilder.fromHttpUrl(apiUrl)
                .queryParam("auth", apiKey)
                .queryParam("topFinGrpNo", BANK_SECTOR_CODE)
                .queryParam("pageNo", 1)
                .build()
                .toUriString();
    }
    
    // API 응답이 성공인지 확인
    private boolean isSuccessResponse(FssApiResponseDTO response) {
        return response != null 
                && response.getResult() != null 
                && SUCCESS_CODE.equals(response.getResult().getErrCd());
    }
}