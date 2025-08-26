from transformers import pipeline
import whisper
import os
import torch
from faster_whisper import WhisperModel
# HuggingFace pipeline 로드 (최초 1회만 다운로드)
classifier = pipeline("sentiment-analysis")

# 들어온 텍스트의 감정을 분석하는 모델.
def run_inference(input_data):
    text = input_data.text
    result = classifier(text)[0]  # label과 score 반환
    return f"{result['label']} ({result['score']:.2f})"

# Whisper 모델 로드 (최초 1회만 다운로드)
whisper_model = whisper.load_model("small")

# 음성 파일을 텍스트로 변환하는 모델.
def run_whisper_inference(file_path):
    if not os.path.exists(file_path):
        raise FileNotFoundError(f"File {file_path} does not exist.")
    
    result = whisper_model.transcribe(file_path)
    return result['text']  # 변환된 텍스트 반환

