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

class ExpsBatch(BaseModel):
    exps:List[Exp]

class ExpForAnalytics(BaseModel):
    category_id: int
    amount: int

class ExpsAnalytics(BaseModel):
    exps:List[ExpForAnalytics]

def classify_category(desc):
    prompt = f'''
    소비내역: {desc}
    소비내역을 아래 카테고리 번호 중 하나로 분류하세요. 설명없이 카테고리 번호만 출력하세요.
    (카페/간식:1, 편의점:2, 식비:3, 택시:5, 쇼핑:6,
    술/유흥:7, 문화(영화관, 티켓, 공연, 스포츠):8,
    의료(병원/약국):9, 생활(마트/생활/주거):10, 기타:11, 대중교통:12)
    우아한형제들과 요기요만 4로 분류하세요.:
    '''
    response = client.chat.completions.create(
        model="gpt-4.1-nano",
        messages=[{"role": "user", "content": prompt}],
        temperature=0
    )
    return response.choices[0].message.content.strip()

async def classify_category_async(desc):
    loop = asyncio.get_event_loop()
    # OpenAI 동기 호출을 스레드 풀로 감싸서 비동기처럼 동작
    return await loop.run_in_executor(None, lambda: classify_category(desc))

# 단일 분류 API
@app.post("/classify")
async def classify(exp: Exp):
    category = classify_category(exp.description)
    return {"expenditure_id": exp.expenditure_id, "category_id": int(category)}

# 여러 개 한 번에 분류 (배치)
@app.post("/classify_batch")
async def classify_batch(batch: ExpsBatch):
    tasks = [classify_category_async(e.description) for e in batch.exps]
    categories = await asyncio.gather(*tasks)
    results = [
        {"expenditure_id": e.expenditure_id, "category_id": int(c)}
        for e, c in zip(batch.exps, categories)
    ]
    return {"results": results}

@app.post("/analysis")
async def analysis(batch: ExpsAnalytics):
    simplified = [{"category_id": e.category_id, "amount": e.amount} for e in batch.exps]
    prompt = f'''
    소비내역:{simplified}
    아래 조건을 기준으로 '과소비' 카테고리 상위 3개를 선정하세요:
    1. 전체 소비 중 비율이 높은 카테고리.
    2. 카페/간식(1), 편의점(2), 배달음식(4), 택시(5), 쇼핑(6), 술/유흥(7), 문화(8) 등 사치성 소비에 가중치를 둠.
    3. 절대 지출 금액이 높을 경우 우선 고려.
    설명 없이 과소비 카테고리 번호만 쉼표로 구분하여 출력하세요. 
    '''
    response = client.chat.completions.create(
        model="gpt-4o",
        messages=[{"role": "user", "content": prompt}],
        temperature=0
    )
    res = response.choices[0].message.content.strip()
    result_list = [int(x.strip()) for x in res.split(",") if x.strip()]
    
    return {"results": result_list}
    
