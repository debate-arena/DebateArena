from fastapi import APIRouter, UploadFile, File
from fastapi.responses import JSONResponse
from app.models.schemas import RequestInput, ResponseOutput
from app.services.inference import run_inference, run_whisper_inference
import shutil
import os
import torch
import logging

router = APIRouter()

@router.post("/predict", response_model=ResponseOutput)
async def predict(input_data: RequestInput):
    result = run_inference(input_data)
    return {"result": result}

logger = logging.getLogger("uvicorn")

@router.post("/whisper", response_model=ResponseOutput)
async def whisper(file: UploadFile = File(...)):
    save_path = os.path.join("voice", file.filename)
    
    with open(save_path, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)
    
    transcription = run_whisper_inference(save_path)
    
    logger.info(f"CUDA available: {torch.cuda.is_available()}")
    
    return {"result": transcription}