from .judge import load_audience_embeddings
import asyncio

async def runem():
    embeddings, meta = await load_audience_embeddings()
    print(type(embeddings), type(meta))

asyncio.run(runem())