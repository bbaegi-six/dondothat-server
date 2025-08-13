from fastapi import FastAPI
from pydantic import BaseModel
from openai import OpenAI
import asyncio
from dotenv import load_dotenv
from typing import List, Dict, Any
from datetime import datetime
import os
import re

load_dotenv()
client = OpenAI(api_key=os.getenv("API_KEY"))
app = FastAPI()

class Exp(BaseModel):
    expenditure_id: int
    description: str

class ExpsList(BaseModel):
    exps: List[Exp]

class ExpForAnalytics(BaseModel):
    category_id: int
    amount: int
    expenditure_date: datetime

class ExpsAnalytics(BaseModel):
    exps: List[ExpForAnalytics]

class SavingProduct(BaseModel):
    finPrdtCd: str
    korCoNm: str
    finPrdtNm: str
    spclCnd: str
    joinMember: str
    intrRate: float
    intrRate2: float

class SavingRecommendRequest(BaseModel):
    savings: List[SavingProduct]
    userAge: int
    userRole: str
    mainBankName: str = None

category_keyword_map = {
    1: ["우아한형제들", "요기요", "배달의민족", "배민", "쿠팡이츠"],
    2: ["스타벅스", "투썸", "컴포즈", "매머드", "커피", "카페", "베이커리", "메가커피", "이디야", "폴바셋"],
    3: ["신세계백화점", "현대백화점", "롯데백화점", "올리브영", "무신사", "쿠팡", "11번가", "G마켓", "SSG.COM", "롯데온", "네이버스마트스토어"],
    4: ["카카오T", "택시", "우버", "마카롱", "T 블루"],
    5: ["CU", "씨유", "GS", "지에스", "세븐일레븐", "이마트24", "편의점", "미니스톱"],
    6: ["CGV", "메가박스", "롯데시네마", "예스24", "인터파크", "도서", "공연", "문화", "티켓", "교보문고"],
    7: ["맥주", "소주", "술집", "포차", "홈술", "와인", "역전할머니", "더부스"],
    8: ["버스", "지하철", "교통카드", "티머니", "T머니", "코레일", "KTX", "공항철도", "카카오모빌리티"],
    9: ["약국", "병원", "의원", "한의원", "치과", "안과", "피부과"],
    10: ["이마트", "홈플러스", "마트", "다이소", "코스트코", "마켓컬리", "쿠팡 로켓프레시"],
    11: ["김밥", "분식", "식당", "라멘", "파스타", "카츠", "맥도날드", "우동", "써브웨이", "칼국수", "버거", "스시", 
         "롯데리아", "버거킹", "신전떡볶이", "백향목분식", "본도시락", "BHC", "교촌치킨", "미분당", "홍콩반점"]
}

def keyword_filtering(desc: str) -> int:
    for id, keywords in category_keyword_map.items():
        for k in keywords:
            if k in desc:
                return id 
    return -1

def classify_category(desc):
    filtered = keyword_filtering(desc)
    if filtered != -1:
        return filtered

    prompt = f'''
    소비내역: {desc}
    소비내역을 아래 카테고리 번호 중 하나로 분류하세요. 설명없이 카테고리 번호만 출력하세요.
    (카페/간식:2, 쇼핑:3, 택시:4, 편의점:5, 문화(영화관, 티켓, 공연, 스포츠):6,
    술/유흥:7, 대중교통:8, 의료(병원/약국):9, 생활(마트/생활/주거):10, 식비:11, 기타:12)
    우아한형제들과 요기요만 1로 분류하세요.:
    '''
    response = client.chat.completions.create(
        model="gpt-4o",
        messages=[{"role": "user", "content": prompt}],
        temperature=0
    )
    return response.choices[0].message.content.strip()

async def classify_category_async(desc):
    loop = asyncio.get_event_loop()
    return await loop.run_in_executor(None, lambda: classify_category(desc))

# 분류 API
@app.post("/classify")
async def classify(list: ExpsList):
    tasks = [classify_category_async(e.description) for e in list.exps]
    categories = await asyncio.gather(*tasks)
    results = [
        {"expenditure_id": e.expenditure_id, "category_id": int(c)}
        for e, c in zip(list.exps, categories)
    ]
    return {"results": results}

# 분석 API
@app.post("/analysis")
async def analysis(list: ExpsAnalytics):
    simplified = [
        {"category_id": e.category_id, 
        "amount": e.amount, 
        "expenditure_date": e.expenditure_date.strftime("%Y-%m-%d")
        } for e in list.exps]
    
    messages = [
        {
            "role": "system",
            "content": """당신은 개인 금융 관리 전문가입니다. 사용자의 소비 패턴을 분석하여 과소비가 심한 카테고리를 찾아내는 것이 당신의 역할입니다.

분석 기준:
1. 최근 30일 vs 이전 30일 지출 증가율
2. 사치성 소비 카테고리 가중치 (배달음식, 카페, 쇼핑, 택시, 편의점, 문화, 술/유흥)
3. 절대 지출 금액의 크기
4. 새롭게 등장한 소비 카테고리

출력 형식: 카테고리 번호 3개를 쉼표로 구분 (예: 1,3,7)
1~7 범위의 숫자만 사용하세요."""
        },
        {
            "role": "user",
            "content": f"""다음 소비 데이터를 분석하여 과소비가 심한 카테고리 상위 3개를 선정하세요:

소비내역: {simplified}

카테고리 설명:
1: 배달음식, 2: 카페/간식, 3: 쇼핑, 4: 택시, 5: 편의점
6: 문화, 7: 술/유흥

분석 조건:
- 지난 60~30일 대비 최근 30일 지출이 급증한 카테고리
- 배달음식(1), 카페(2), 쇼핑(3), 택시(4), 편의점(5), 문화(6), 술/유흥(7) 사치성 소비 우선
- 절대 금액이 큰 카테고리 우선
- 새로운 소비 패턴도 고려

상위 3개 카테고리 번호만 쉼표로 구분하여 출력:"""
        }
    ]
    
    response = client.chat.completions.create(
        model="gpt-4o", 
        messages=messages,
        temperature=0,
        max_tokens=20
    )
    
    res = response.choices[0].message.content.strip()
    
    # 응답에서 숫자 추출 및 유효성 검사
    numbers = re.findall(r'\b([1-7])\b', res)  # 1~7만 매치
    result_list = [int(x) for x in numbers[:3]]  # 최대 3개만
    
    # 결과가 없으면 기본값 반환
    if not result_list:
        result_list = [1, 3, 7]  # 기본 과소비 카테고리
        
    return {"results": result_list}

# 적금 상품 추천 API
@app.post("/recommend-savings")
async def recommend_savings(request: SavingRecommendRequest):
    # 사용자 정보와 적금 상품 리스트를 기반으로 추천
    user_info = {
        "age": request.userAge,
        "role": request.userRole,
        "mainBank": request.mainBankName or "없음"
    }
    
    savings_data = [
        {
            "상품코드": s.finPrdtCd,
            "은행명": s.korCoNm,
            "상품명": s.finPrdtNm,
            "특별조건": s.spclCnd,
            "가입대상": s.joinMember,
            "기본금리": f"{s.intrRate}%",
            "우대금리": f"{s.intrRate2}%"
        } for s in request.savings
    ]
    
    messages = [
        {
            "role": "system",
            "content": """당신은 금융 상품 추천 전문가입니다. 사용자의 상황에 가장 적합한 적금 상품 3개를 추천하는 것이 당신의 역할입니다.

추천 기준:
1. 사용자 나이와 가입대상 적합성
2. 사용자 직업과 상품 특성 매칭
3. 금리 경쟁력 (기본금리 + 우대금리)
4. 특별조건의 달성 가능성
5. 은행 다양성 (가능한 다른 은행 상품 포함)

추천 원칙:
- 주거래은행 상품이 있고 조건이 좋다면 2개까지 포함 가능
- 나머지는 다른 은행의 경쟁력 있는 상품으로 구성
- 최대한 다양한 은행에서 선택하여 포트폴리오 다양화

출력 형식: 상품코드 3개를 쉼표로 구분하여 출력 (예: 00266451,00123456,00789012)
반드시 제공된 상품 목록에서만 선택하세요."""
        },
        {
            "role": "user", 
            "content": f"""다음 사용자 정보를 바탕으로 가장 적합한 적금 상품 3개를 추천하세요:

사용자 정보:
- 나이: {user_info['age']}세
- 직업: {user_info['role']}
- 주거래은행: {user_info['mainBank']}

추천 대상 적금 상품 목록:
{savings_data}

추천 조건:
- 사용자가 실제 가입 가능한 상품만 선택
- 금리가 높고 조건이 유리한 상품 우선
- 주거래은행 상품은 최대 2개까지 포함 (다른 은행 상품도 반드시 포함)
- 사용자 직업/연령대에 맞는 상품 우선
- 서로 다른 은행 상품으로 구성하여 다양성 확보

상위 3개 추천 상품의 상품코드만 쉼표로 구분하여 출력:"""
        }
    ]
    
    response = client.chat.completions.create(
        model="gpt-4o",
        messages=messages, 
        temperature=0,
        max_tokens=50
    )
    
    res = response.choices[0].message.content.strip()
    
    # 응답에서 상품코드 추출
    product_codes = [code.strip() for code in res.split(',')]
    
    # 유효한 상품코드만 필터링
    valid_codes = []
    available_codes = [s.finPrdtCd for s in request.savings]
    
    for code in product_codes:
        if code in available_codes and len(valid_codes) < 3:
            valid_codes.append(code)
    
    # 추천된 상품 정보 반환
    recommended_products = []
    for code in valid_codes:
        for saving in request.savings:
            if saving.finPrdtCd == code:
                recommended_products.append({
                    "finPrdtCd": saving.finPrdtCd,
                    "korCoNm": saving.korCoNm,
                    "finPrdtNm": saving.finPrdtNm,
                    "spclCnd": saving.spclCnd,
                    "joinMember": saving.joinMember,
                    "intrRate": saving.intrRate,
                    "intrRate2": saving.intrRate2
                })
                break
    
    # 추천 결과가 부족하면 상위 상품으로 채우기
    if len(recommended_products) < 3:
        remaining_needed = 3 - len(recommended_products)
        recommended_codes = [p["finPrdtCd"] for p in recommended_products]
        
        for saving in request.savings:
            if len(recommended_products) >= 3:
                break
            if saving.finPrdtCd not in recommended_codes:
                recommended_products.append({
                    "finPrdtCd": saving.finPrdtCd,
                    "korCoNm": saving.korCoNm,
                    "finPrdtNm": saving.finPrdtNm,
                    "spclCnd": saving.spclCnd,
                    "joinMember": saving.joinMember,
                    "intrRate": saving.intrRate,
                    "intrRate2": saving.intrRate2
                })
    
    return {"recommendations": recommended_products}
