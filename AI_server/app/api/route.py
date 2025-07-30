from fastapi import APIRouter, UploadFile, File
from fastapi.responses import JSONResponse
from app.models.schemas import RequestInput, ResponseOutput, STTRequest, EmbeddingOutput, AttackdefenseInput, AttackDefenseOutput,LastOutput, LastInput
from app.services.summarize import summarize_first_half, summarize_result_text, summarize_second_half
from app.services.judge import judging
import shutil
import os
import torch
import logging

router = APIRouter()

# 1차로 의견 주장하며 받아온 텍스트를 요약본으로 바꾸는 곳
@router.post("/summaries/opinion", response_model=ResponseOutput)
async def summarize_opinion(input_data: STTRequest):
    result = await summarize_first_half(input_data)
    return {"result": result}

# 공방전을 하며 나온 공격/방어를 받아서 요약본으로 바꿔주는 곳
@router.post("/summaries/seigedefense", response_model=AttackDefenseOutput)
async def summarize_seige_defense(input_data: AttackdefenseInput):
    result = await summarize_second_half(input_data)
    return result

# 최종으로 모든 요약을 받아서 결과를 도출하는 곳
@router.post("/summaries/result", response_model=LastOutput)
async def last(input_data: LastInput):
    # 1. 요약 텍스트 생성
    summary_texts = await summarize_result_text(input_data)
    
    # 2. judge.py의 judging 함수 호출하여 최종 판정
    result = await judging(summary_texts)
    
    return {"result": result, "full_summarize": summary_texts}