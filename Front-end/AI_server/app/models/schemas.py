from pydantic import BaseModel

class RequestInput(BaseModel):
    text: str

class ResponseOutput(BaseModel):
    result: str
