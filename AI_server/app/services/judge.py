from transformers import pipeline
from dotenv import load_dotenv
load_dotenv()
import os
import torch
import httpx
import json
import pandas as pd
import numpy as np
from typing import List
from sklearn.metrics.pairwise import cosine_similarity
from chromadb import PersistentClient

GMS_API_KEY = os.getenv("GMS_API_KEY")
EMBEDDING_API_URL = os.getenv("EMBEDDING_API_URL")


# AI 청중을 통한 판정을 위해 전체 요약 텍스트를 임베딩하는 함수. 일단은 GPT-text-embedding-3-small 모델 사용.
# 크레딧을 생각보다 많이 잡아먹지 않아서 그냥 large 모델로 변경. 1536 차원 -> 3072차원
# 속도가 너무 느리면 다시 변경하거나 구글 004 모델로의 변경도 고려 중. -> 속도 괜찮아서 large 모델 유지
async def result_embedding(input_data: dict):
    text = input_data["text"]
    
    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {GMS_API_KEY}"
    }
    

    payload = {
        "model": "text-embedding-3-large",
        "input": text,
    }
    
    async with httpx.AsyncClient(verify=False, timeout=60.0) as client:
        response = await client.post(EMBEDDING_API_URL, headers=headers, json=payload)
        response.raise_for_status()
        result = response.json()
        output = result["data"][0]["embedding"]
    
    return output


async def load_audience_embeddings():
    # 현재 파일의 위치를 기준으로 상대 경로 계산
    current_dir = os.path.dirname(os.path.abspath(__file__))
    chroma_path = os.path.join(current_dir, "..", "..", "data", "chroma_jurors")
    
    client = PersistentClient(path=chroma_path)
    collection = client.get_or_create_collection("audience")
    audience = collection.get(include=["embeddings", "metadatas"])
    
    embeddings = audience["embeddings"]
    metadata = audience["metadatas"]
    
    return embeddings, metadata
    
#토론이 끝나면 summarize쪽에서 전체 요약을 받고 판정을 내릴 함수.
async def judging(summary_texts: dict):
    num1_text = summary_texts.get("num1", "")
    num2_text = summary_texts.get("num2", "")
    
    # 2. 요약 임베딩
    num1_embedding = np.array(await result_embedding({"text": num1_text}))
    num2_embedding = np.array(await result_embedding({"text": num2_text}))
    
    # 3. 청중 임베딩 불러오기
    audience_embeddings, audience_metadata = await load_audience_embeddings()
    sorted_data = sorted(zip(audience_embeddings, audience_metadata), key=lambda x: x[1].get("id", 0))
    audience_embeddings = np.array([x[0] for x in sorted_data])
    
    # 청중 임베딩이 비어있는 경우 에러 처리
    if audience_embeddings is None or len(audience_embeddings) == 0:
        raise ValueError("청중 임베딩 데이터가 없습니다. process.py를 실행하여 데이터를 준비해주세요.")
    
    # 4. 유사도 계산
    num1_similarities = cosine_similarity([num1_embedding], audience_embeddings)[0]
    num2_similarities = cosine_similarity([num2_embedding], audience_embeddings)[0]
    
     # 5. 개별 청중의 투표
    votes = {"num1": 0, "num2": 0}
    voted_details = []  # 누가 누구를 선택했는지

    for i in range(len(audience_embeddings)):
        sim1 = num1_similarities[i]
        sim2 = num2_similarities[i]
        if sim1 > sim2:
            votes["num1"] += 1
            voted_details.append({"juror": i, "vote": "num1", "diff": sim1 - sim2})
        else:
            votes["num2"] += 1
            voted_details.append({"juror": i, "vote": "num2", "diff": sim2 - sim1})
    
    # 6. 최종 결과 구성
    winner = "무승부"
    if votes["num1"] > votes["num2"]:
        winner = "num1"
    elif votes["num2"] > votes["num1"]:
        winner = "num2"
    
    return {
        "winner": winner,
        "votes": votes,
        "details": voted_details  # 선택적으로 리턴
    }
    