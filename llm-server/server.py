from fastapi import FastAPI
from pydantic import BaseModel
from openai import OpenAI
import asyncio
from dotenv import load_dotenv
from typing import List, Dict, Any
from datetime import datetime
import os
import re
import json

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

# classify 키워드
category_keyword_map = {
    1: ["우아한형제들", "요기요", "배달의민족", "배민", "쿠팡이츠"],
    2: ["스타벅스", "투썸", "컴포즈", "매머드", "커피", "카페", "베이커리","파리바게뜨","공차",
        "이디야", "빽다방","쥬씨","배스킨라빈스","요거트","던킨","노티드","설빙","젤라또"],
    4: ["카카오T", "택시", "우버","T블루"],
    5: ["CU", "씨유", "GS25", "지에스25", "세븐일레븐", "이마트24", "미니스톱"],
    6: ["CGV", "메가박스", "시네마", "예스24", "인터파크", "도서", "공연", "문화", "티켓", "문고", "알라딘","책방","씨어터","미술관"],
    7: ["맥주", "소주", "술집", "포차", "와인", "호프","펍","주막"],
    8: ["버스", "지하철", "교통카드", "티머니", "T머니", "코레일", "KTX", "공항철도"],
    9: ["약국", "병원", "의원", "치과", "내과","외과", "이비인후과","안과", "피부과","정형외과"],
    10: ["이마트", "홈플러스","마트", "다이소", "코스트코", "마켓컬리", "로켓프레시","안경","전기","수도","가스"],
    11: ["김밥", "분식", "식당", "라멘", "파스타", "카츠", "맥도날드","KFC" ,"우동", "텐동","써브웨이","샤브","해화로",
         "칼국수", "버거", "스시", "포케","롯데리아","맘스터치" ,"떡볶이", "도시락", "순대","육회","세종원","카레",
         "치킨","통닭", "피자","해장","국수","국밥","토스트","감자탕", "갈비","브런치" ,"고기","한우","삼겹","프레퍼스",
         "다이닝","타코","비스트로","레스토랑","반점","키친","그릴","정성","할머니","숯불","리필","원조","전통","샹츠마라"],
    3: ["백화점", "아울렛","면세점","올리브영", "무신사", "에이블리","지그재그" ,"쿠팡", "11번가", "G마켓", 
        "SSG", "롯데온", "네이버","나이스페이먼츠","ALIPAY","ARS","KCP"],
}

# 키워드 필터링 함수
def keyword_filtering(desc: str) -> int:
    for id, keywords in category_keyword_map.items():
        for k in keywords:
            if k in desc:
                return id 
    return -1

# 한 번에 여러 건을 분류하는 LLM 함수
def classify_batch_llm(items: List[Dict]) -> Dict[int, int]:

    system_prompt = (
        "너는 영수증의 상호명을 정확히 하나의 카테고리로 분류하는 분류기다. "
        "반드시 제공된 ENUM(키) 중 하나만 선택한다. "
        "입력은 [expenditure_id:int, description:str]의 JSON 배열이다. "
        '반드시 JSON 객체 하나로만 응답하고, "results" 키 아래에 '
        "[expenditure_id:int, category_id:int] 쌍의 배열만 담아라. "
        '예: {"results": [[1, 11], [2, 2], ...]}'
    )

    # LLM에 보내는 inputs를 [id, desc]의 compact 배열로 구성
    compact_inputs = [[it["expenditure_id"], it["description"]] for it in items]

    user_payload = {
        "inputs": compact_inputs,
        "enum": {
            "2": "카페/간식/디저트",
            "3": "쇼핑",
            "5": "편의점",
            "6": "문화(영화,공연,도서,전시)",
            "7": "술/유흥",
            "8": "대중교통",
            "9": "의료",
            "10": "생활(마트,주거,통신,이발)",
            "11": "식비(식당,음식점)",
            "12": "기타"
        },
        "rules": [
            "브랜드/장소가 명확하면 해당 카테고리 우선(예: 스타벅스=2).",
            "애매하면 문자열 주요 키워드만 분석하여, 대한민국 기준으로 가장 가능성 높은 카테고리로 추정.",
            "전혀 예측할 수 없는 경우에만 불가피하게 12로 지정.(최대한 피한다.)"
        ],
        "output_schema": {"results": [["int", "int"]]}
    }

    response = client.chat.completions.create(
        model="gpt-4o",
        temperature=0,
        top_p=0,
        response_format={"type": "json_object"},
        messages=[
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": json.dumps(user_payload, ensure_ascii=False)},
        ]
    )

    raw = response.choices[0].message.content
    try:
        data = json.loads(raw)
        pairs = data.get("results", [])
    except Exception:
        # 비정상 출력 시 [id, 12] 쌍 배열로 방어
        pairs = [[it["expenditure_id"], 12] for it in items]

    # CHANGED: [id, cat] → {id: cat} 매핑으로 변환 (반환 타입은 기존과 동일)
    out: Dict[int, int] = {}
    for p in pairs:
        try:
            eid, cid = int(p[0]), int(p[1])
        except Exception:
            # 방어적 캐스팅
            try:
                eid = int(p[0])
            except Exception:
                continue
            cid = 12
        out[eid] = cid

    return out

# 동시 배치 수 제한을 위한 세마포어
_SEMAPHORE = asyncio.Semaphore(16)  # 병렬 갯수
_BATCH_SIZE = 16                    # 배치 크기

# 배치 호출을 비동기로 감싸기 (블로킹 SDK 호출을 스레드로 우회)
async def _classify_batch_llm_async(items: List[Dict]) -> Dict[int, int]:
    async with _SEMAPHORE:
        loop = asyncio.get_event_loop()
        return await loop.run_in_executor(None, lambda: classify_batch_llm(items))

# 분류 API
@app.post("/classify")
async def classify(list: ExpsList):
    # 1) 키워드 필터로 즉시 결정 가능한 것 선별
    decided: Dict[int, int] = {}
    undecided: List[Dict] = []

    for e in list.exps:
        k = keyword_filtering(e.description or "")
        if k != -1:
            decided[e.expenditure_id] = k
        else:
            undecided.append({"expenditure_id": e.expenditure_id, "description": e.description})

    # 2) 필터링 후 남은 것들
        
    # 같은 description은 한 번만 LLM에 보내게끔 설정
    by_desc: Dict[str, List[int]] = {}
    for it in undecided:
        d = it["description"]
        by_desc.setdefault(d, []).append(it["expenditure_id"])

    # 대표 id 하나만 들고 가서 LLM 호출
    uniq_items = [{"expenditure_id": ids[0], "description": desc}
                  for desc, ids in by_desc.items()]
    
    # 배치 LLM 호출(세마포어로 동시성 제한)
    tasks = []
    for i in range(0, len(uniq_items), _BATCH_SIZE):
        batch = uniq_items[i:i + _BATCH_SIZE]
        tasks.append(_classify_batch_llm_async(batch))

    results_llm: Dict[int, int] = {}
    if tasks:
        for chunk in await asyncio.gather(*tasks):
            results_llm.update(chunk)

    # 대표 id의 예측을 동일 description의 나머지 id에 전파
    for desc, ids in by_desc.items():
        rep_id = ids[0]
        pred = int(results_llm.get(rep_id, 12))
        for eid in ids: # 대표 포함 전체에 동일 적용
            results_llm[eid] = pred

    # 3) 결합 결과 생성
    final = []
    for e in list.exps:
        cid = decided.get(e.expenditure_id) or results_llm.get(e.expenditure_id) or 12  # 방어적 기본값
        final.append({"expenditure_id": e.expenditure_id, "category_id": int(cid)})

    return {"results": final}

# 분석 API
@app.post("/analysis")
async def analysis(list: ExpsAnalytics):
    
    # compact 형태로 변환 ([category_id, amount, date])
    simplified = [
        [e.category_id, e.amount, e.expenditure_date.strftime("%Y-%m-%d")]  # CHANGED
        for e in list.exps
    ]
    
    SYS = (
        "당신은 개인 금융 분석가. "
        "user가 준 rules만 근거로 판단하고, 정수 배열로만 출력하라. "
        "입력 데이터는 [category_id:int, amount:int, date:str] 배열 형식이다."
    )
        
    USER = {
        "task":"과소비 카테고리 상위 3개 선정",
        "rules":[
            "최근 60~30일의 지출 대비 최근 30일의 지출 증가율을 최우선 고려.",
            "증가율이 같다면 사치성 소비를 우선. ",
            "절대 지출 금액 우선. ",
            "최근 30일에 새로 등장한 카테고리는 가산점.",
        ],
        "categories":{
            1: "배달음식", 2: "카페/간식", 3: "쇼핑", 4: "택시",
            5: "편의점", 6: "문화", 7: "술/유흥",
        },
        "data":simplified,
        "output": "중복없는 3개의 정수 배열(예: [1,3,7])"
    }
    
    response = client.chat.completions.create(
        model="gpt-4o", 
        messages=[
            {"role": "system", "content": SYS},
            {"role": "user", "content": json.dumps(USER, ensure_ascii=False)},
        ],
        response_format={
            "type": "json_schema",
            "json_schema": {
                "name": "top3",
                "strict": True,
                "schema": {
                    "type": "object",  # 루트는 object
                    "properties": {
                        "results": {
                            "type": "array",
                            "items": {"type": "integer", "minimum": 1, "maximum": 7},
                            "minItems": 3,
                            "maxItems": 3,
                        }
                    },
                    "required": ["results"],
                    "additionalProperties": False
                }
            }
        },
        temperature=0,
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
