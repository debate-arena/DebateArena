from pydantic import BaseModel

class RequestInput(BaseModel):
    topic: str
    text: str

class ResponseOutput(BaseModel):
    result: str

class STTRequest(BaseModel):
    user_id: str
    topic: str
    position: str
    text: str

# 공방전에서 사용하는 모델들
class AttackdefenseInput(BaseModel):
    topic: str
    target: str
    key: dict

class AttackDefenseOutput(BaseModel):
    result: dict

# 최종 결과에 사용하는 3가지 모델
# 백엔드 쪽에서 들어오는 자료들 구조
class LastInput(BaseModel):
    topic: str
    draw: str
    entire: dict

# 내가 백엔드 쪽으로 보내는 결과들
class LastOutput(BaseModel):
    result: dict

# 전체 요약본을 임베딩 할 모델
class EmbeddingInput(BaseModel):
    position: str
    text: str
    
class EmbeddingOutput(BaseModel):
    result: list[float]
    