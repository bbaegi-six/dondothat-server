from fastapi import FastAPI
from pydantic import BaseModel
from openai import OpenAI
import asyncio
from dotenv import load_dotenv
from typing import List
from datetime import datetime
import os

load_dotenv()
client = OpenAI(api_key=os.getenv("API_KEY"))
app=FastAPI()

class Exp(BaseModel):
    expenditure_id: int
    description: str

class ExpsList(BaseModel):
    exps:List[Exp]

class ExpForAnalytics(BaseModel):
    category_id: int
    amount: int
    expenditure_date:datetime

class ExpsAnalytics(BaseModel):
    exps:List[ExpForAnalytics]

category_keyword_map = {
    1: ["우아한형제들", "요기요"],
    2: ["스타벅스", "투썸", "컴포즈", "매머드", "커피", "카페", "베이커리"],
    3: ["신세계백화점", "현대백화점", "롯데백화점", "올리브영", "무신사"],
    4: ["카카오T", "택시"],
    5: ["CU", "씨유", "GS", "지에스", "세븐일레븐", "이마트24", "편의점"],
    6: ["CGV", "메가박스", "롯데시네마", "예스24", "인터파크", "도서", "공연", "문화"],
    7: ["맥주", "소주", "술집", "포차"],
    8: ["버스", "지하철", "교통카드", "티머니"],
    9: ["약국", "병원", "의원", "한의원"],
    11: ["김밥", "분식", "식당", "라멘", "파스타", "카츠", "맥도날드", "우동", "써브웨이", "칼국수", "버거", "스시"],
    10: ["이마트", "홈플러스", "마트", "다이소"],
}

def keyword_filtering(desc:str) -> int:
    for id,keywords in category_keyword_map.items():
        for k in keywords:
            if k in desc:
                return id 
    return -1

def classify_category(desc):

    filtered= keyword_filtering(desc)
    if filtered!=-1:
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
    # OpenAI 동기 호출을 스레드 풀로 감싸서 비동기처럼 동작
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
        "expenditure_date":e.expenditure_date.strftime("%Y-%m-%d")
        } for e in list.exps]
    prompt = f'''
    소비내역: {simplified}
    아래 조건으로 '과소비' 카테고리 상위 3개를 선정하세요:
    1. 지난 60~30일 대비 최근 30일 지출 급증한 카테고리.
    2. 배달음식(1), 카페/간식(2), 쇼핑(3), 택시(4), 편의점(5), 문화(6), 술/유흥(7) 등 사치성 소비에 가중치를 둠.
    3. 절대 금액이 큰 카테고리 우선.
    4. 이전 지출이 없던 새로운 카테고리도 고려.
    상위 3개 카테고리 번호만 쉼표로 구분하여 출력:
    '''
    response = client.chat.completions.create(
        model="gpt-4o",
        messages=[{"role": "user", "content": prompt}],
        temperature=0
    )
    res = response.choices[0].message.content.strip()
    result_list = [int(x.strip()) for x in res.split(",") if x.strip()]
    
    return {"results": result_list}