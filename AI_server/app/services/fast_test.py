from transformers import pipeline
import whisper
import os
import torch
from faster_whisper import WhisperModel

faster_whisper_model = WhisperModel("small", device = "cuda", compute_type="float16")

segments, info = faster_whisper_model.transcribe("./test_1.wav", beam_size=5)

print("Detected language:", info.language)

for segment in segments:
    print(f"[{segment.start:.2f} - {segment.end:.2f}] {segment.text}")