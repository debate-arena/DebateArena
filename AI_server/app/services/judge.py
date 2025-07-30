from transformers import pipeline
from app.models.schemas import LastInput, EmbeddingInput, RequestInput
from app.services.summarize import summarize_result_text
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
    
    async with httpx.AsyncClient(verify=False, timeout=10.0) as client:
        response = await client.post(EMBEDDING_API_URL, headers=headers, json=payload)
        response.raise_for_status()
        result = response.json()
        output = result["data"][0]["embedding"]
    
    return output


async def load_audience_embeddings():
    client = PersistentClient(path="data/chroma_jurors")
    collection = client.get_or_create_collection("audience")
    audience = collection.get(include=["embeddings", "metadatas"])
    
    embeddings = audience["embeddings"]
    metadata = audience["metadatas"]
    
    return embeddings, metadata
    
#토론이 끝나면 summarize쪽에서 전체 요약을 받고 판정을 내릴 함수.
async def judging(input_data: LastInput, summary_texts: dict):
    num1_text = summary_texts.get("num1", "")
    num2_text = summary_texts.get("num2", "")
    
    # 2. 요약 임베딩
    num1_embedding = np.array(await result_embedding({"text": num1_text}))
    num2_embedding = np.array(await result_embedding({"text": num2_text}))
    
    # 3. 청중 임베딩 불러오기
    audience_embeddings, audience_metadata = await load_audience_embeddings()
    audience_embeddings = np.array(audience_embeddings)

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