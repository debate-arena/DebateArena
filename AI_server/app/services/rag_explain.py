import os
import httpx
from dotenv import load_dotenv
load_dotenv()

GMS_API_KEY = os.getenv("GMS_API_KEY")
SUMMARIZE_API_URL = os.getenv("SUMMARIZE_API_URL")

async def generate_audience_explanation(summary_text: dict, voted_detail: list, audience_metadata: list, top_k: int = 3):
    explanations = []
    
    for detail in voted_detail[:top_k]:
        juror_id = detail["juror"]