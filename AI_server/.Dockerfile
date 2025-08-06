# 1. Python 베이스 이미지 선택 (3.10 버전 사용)
FROM python:3.10-slim

# 2. 시스템 패키지 업데이트 및 필요한 패키지 설치
RUN apt-get update && apt-get install -y \
    gcc \
    g++ \
    build-essential \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 3. 작업 디렉토리 설정
WORKDIR /app

# 4. Python 환경 변수 설정
ENV PYTHONUNBUFFERED=1
ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONPATH=/app

# 5. 종속성 파일 복사 및 설치
COPY requirements.txt .
RUN pip install --upgrade pip && \
    pip install --no-cache-dir -r requirements.txt

# 6. 전체 소스 복사
COPY . .

# chroma_jurors 생성
RUN python data/process.py

# 7. 포트 노출 (FastAPI 기본 포트)
EXPOSE 8000

# 8. 비root 사용자 생성 (보안 강화)
RUN adduser --disabled-password --gecos '' appuser && \
    chown -R appuser:appuser /app
USER appuser

# 9. FastAPI 서버 실행
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
