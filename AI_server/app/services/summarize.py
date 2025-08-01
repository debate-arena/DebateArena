from transformers import pipeline
from app.models.schemas import STTRequest, RequestInput, ResponseOutput, LastInput, LastOutput, AttackdefenseInput
import os
import torch
import httpx
import json
import pandas as pd
import numpy as np
from typing import List
from sklearn.metrics.pairwise import cosine_similarity
from dotenv import load_dotenv
load_dotenv()

GMS_API_KEY = os.getenv("GMS_API_KEY")
SUMMARIZE_API_URL = os.getenv("SUMMARIZE_API_URL")
    
# 처음으로 들어온 텍스트 요약. 공방 이전에 개인의 논리 주장 시 사용하는 함수.
async def summarize_first_half(input_data: STTRequest) -> str:
    user_id = input_data.user_id
    topic = input_data.topic
    text = input_data.text
    position = input_data.position
    max_text_len = round(len(text)/2)
    
    headers = {
        "Content-Type": "application/json",
        "Authorization" : f"Bearer {GMS_API_KEY}"
    }
    
    prompt = f"""
    {topic}에서 {position}의 역할인 사람이 말한 내용은 다음과 같습니다: {text}.
    
    이 발언의 핵심 주장을 한국어로 반드시 최소 150자 이상, 반드시 최대 {max_text_len}자 이내로 요약해 주세요.
    150자를 채우기 위해 없는 내용을 추가하지 말고 최대한 주어진 텍스트 내에서 가공하여 내용의 핵심을 요약해 주세요.
    """
    
    payload = {
        "model": "gpt-4.1-mini",
        "messages": [
            {"role": "system", "content": "한국어로 대답해주세요. 간략하고 핵심적인 요약을 해주세요. 앞뒤로 불필요한 내용은 넣지 말고 들어온 텍스트 내에서만 말해주세요."},
            {"role": "user", "content": prompt}
        ],
        "max_tokens": 2048,
        "temperature": 0.3
    }
    
    # 5초간 대답이 없으면 에러로 간주. 
    async with httpx.AsyncClient(verify=False, timeout=10.0) as client:
        response = await client.post(SUMMARIZE_API_URL, headers=headers, json=payload)
        response.raise_for_status()  # 에러 발생 시 예외 던짐
        result = response.json()     # JSON 파싱

    # 응답 결과에서 요약 추출
    return result["choices"][0]["message"]["content"].strip()
    

# 공방전에서 들어온 텍스트 요약
async def summarize_second_half(input_data: AttackdefenseInput):
    topic = input_data.topic
    target = input_data.target
    key = input_data.key
    
    attack_id = key["attack"]["user_id"]
    attack_position = key["attack"]["position"]
    attack_text = key["attack"]["text"]
    
    defense_id = key["defense"]["user_id"]
    defense_position = key["defense"]["position"]
    defense_text = key["defense"]["text"]

    headers = {
        "Content-Type": "application/json",
        "Authorization" : f"Bearer {GMS_API_KEY}"
    }
    
    
    prompt = f"""
    {topic}에 대한 공방전에서 {attack_position}을 주장하는 사람이 {defense_position}을 주장하는 사람의 
    {defense_text}에 대해 공격한 발언 내용은 다음과 같습니다: {attack_text}.
    
    이 발언의 핵심 주장을 한국어로 반드시 최소 150자 이상, 반드시 최대 500자 이내로 요약해 주세요.
    150자를 채우기 위해 없는 내용을 추가하지 말고 최대한 주어진 텍스트 내에서 가공하여 내용의 핵심을 요약해 주세요.
    
    반환되는 텍스트는 아래의 양식을 적용해서 답변해주세요.
    
    공격 내용 : 공격 내용 요약
    방어 내용 : 방어 내용 요약
    
    """
    
    payload = {
        "model": "gpt-4.1-mini",
        "messages": [
            {"role": "system", "content": "한국어로 대답해주세요. 간략하고 핵심적인 요약을 해주세요. 앞뒤로 불필요한 내용은 넣지 말고 들어온 텍스트 내에서만 말해주세요."},
            {"role": "user", "content": prompt}
        ],
        "max_tokens": 2048,
        "temperature": 0.3
    }
    
    async with httpx.AsyncClient(verify=False, timeout=10.0) as client:
        response = await client.post(SUMMARIZE_API_URL, headers=headers, json=payload)
        response.raise_for_status()
        result = response.json()
    
    combined_summarize = result["choices"][0]["message"]["content"].strip()
    
    content = {
        "attack_id": attack_id,
        "defense_id": defense_id,
        "text": combined_summarize
    }

    return {"result": content}


# 토론이 끝나면 모든 요약본을 받아서 전체 요약을 받아올 함수.
async def summarize_result_text(input_data: LastInput):
    topic = input_data.topic
    entire = input_data.entire
    
    summaries = {}
    
    headers = {
        "Content-Type": "application/json",
        "Authorization" : f"Bearer {GMS_API_KEY}"
    }
    
    for key, val in entire.items():
        position = val["position"]
        text = val["text"]
    
        prompt = f"""
        당신은 전문적인 토론 분석가입니다. 아래는 {topic}에 대해 {position}을 주장하는 한 명 이상의 인물들이 참여한 토론의 주장 요약입니다.

        이 토론을 다음 기준에 따라 정리해 주세요:

        요약 기준:
        1. 각 인물의 입장(찬성/반대/중립)을 구분해 명시합니다.
        2. 각 입장에서 제시한 주요 주장(논거)를 항목별로 요약합니다.
        3. 결론적으로 해당 인물들이 주장하고자 하는 바를 반드시 최소 300자 이상 요약합니다.

        형식:
        **{topic}에 대한 요약을 해드리겠습니다.**
        - `참여자 및 입장:`  
        - `주요 주장:`  
        - `결론/종합 요약:`  

        불필요한 잡담, 반복, 감정 표현은 제거하고 핵심 주장과 논리적 구조 중심으로 작성해 주세요.

        다음은 요약할 토론 내용입니다: {text} 
        """
        
        payload = {
            "model": "gpt-4.1",
            "messages": [
                {"role": "developer", "content": "한국어로 대답해주세요."},
                {"role": "user", "content": prompt}
            ],
            "max_tokens": 6000,
            "temperature": 0.3
        }
        
        async with httpx.AsyncClient(verify=False, timeout=30.0) as client:
            print("summarize쪽에서 에러터진거 아님!!!!!!!!!!!!!!!!!!!!!!!!")
            response = await client.post(SUMMARIZE_API_URL, headers=headers, json=payload)
            response.raise_for_status()  # 에러 발생 시 예외 던짐
            result = response.json()     # JSON 파싱
        
        summaries[key] = result["choices"][0]["message"]["content"].strip()
    
    return summaries