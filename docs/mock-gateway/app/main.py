from fastapi import FastAPI, HTTPException, Depends, Header, Request
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field
from typing import Optional, List, Dict, Any
from datetime import datetime, timedelta
import uuid
import json
import hashlib
import time
import asyncio
import redis.asyncio as redis
from contextlib import asynccontextmanager

import os

# Configuration
API_KEY = "dev-key-change-in-prod"
CACHE_TTL = 3600
GUARDRAIL_ENABLED = True
TRACE_ENABLED = True
REDIS_URL = os.getenv("REDIS_URL", "redis://redis:6379")

# Redis client
redis_client: Optional[redis.Redis] = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global redis_client
    redis_client = redis.from_url(REDIS_URL, decode_responses=True, socket_timeout=5, socket_connect_timeout=5, retry_on_timeout=True)
    
    # Start background task to connect to Redis
    async def connect_redis():
        for attempt in range(30):
            try:
                await redis_client.ping()
                print("Redis connected successfully")
                return
            except Exception as e:
                print(f"Redis connection attempt {attempt + 1} failed: {e}")
                if attempt == 29:
                    print("Failed to connect to Redis after 30 attempts - continuing without cache")
                    return
                await asyncio.sleep(2)
    
    # Start connection in background, don't block startup
    asyncio.create_task(connect_redis())
    
    yield
    await redis_client.close()


app = FastAPI(
    title="Mock AI Gateway",
    description="EngAI Gateway pattern mock for resume-ai-android",
    version="1.0.0",
    lifespan=lifespan
)


# ==================== MODELS ====================

class HealthResponse(BaseModel):
    status: str = "healthy"
    timestamp: str
    version: str = "1.0.0"
    services: Dict[str, str] = {}


class ChatRequest(BaseModel):
    messages: List[Dict[str, str]] = Field(..., min_items=1)
    model: str = "gpt-4o-mini"
    temperature: float = 0.7
    max_tokens: int = 2048
    stream: bool = False
    metadata: Optional[Dict[str, Any]] = None


class ChatResponse(BaseModel):
    id: str
    object: str = "chat.completion"
    created: int
    model: str
    choices: List[Dict[str, Any]]
    usage: Dict[str, int]
    trace_id: Optional[str] = None
    cached: bool = False


class EmbeddingRequest(BaseModel):
    input: List[str] = Field(..., min_items=1)
    model: str = "text-embedding-3-small"


class EmbeddingResponse(BaseModel):
    object: str = "list"
    data: List[Dict[str, Any]]
    model: str
    usage: Dict[str, int]
    trace_id: Optional[str] = None
    cached: bool = False


class GuardrailRequest(BaseModel):
    content: str
    context: Optional[str] = None


class GuardrailResponse(BaseModel):
    allowed: bool
    reason: Optional[str] = None
    categories: List[str] = []


class TraceEntry(BaseModel):
    trace_id: str
    timestamp: str
    request_type: str
    model: str
    input_hash: str
    output_hash: str
    tokens_in: int
    tokens_out: int
    latency_ms: int
    cost_usd: float
    cached: bool
    guardrail_passed: bool
    metadata: Optional[Dict[str, Any]] = None


# ==================== AUTH ====================

async def verify_api_key(x_api_key: str = Header(...)):
    if x_api_key != API_KEY:
        raise HTTPException(status_code=401, detail="Invalid API key")
    return x_api_key


# ==================== UTILITIES ====================

def generate_trace_id() -> str:
    return f"trace_{uuid.uuid4().hex[:16]}"


def hash_content(content: str) -> str:
    return hashlib.sha256(content.encode()).hexdigest()[:16]


def estimate_tokens(text: str) -> int:
    return len(text.split()) * 1.3


def estimate_cost(tokens_in: int, tokens_out: int, model: str) -> float:
    rates = {
        "gpt-4o-mini": (0.00015, 0.0006),
        "gpt-4o": (0.005, 0.015),
        "text-embedding-3-small": (0.00002, 0),
    }
    in_rate, out_rate = rates.get(model, (0.001, 0.002))
    return (tokens_in * in_rate + tokens_out * out_rate) / 1000


async def check_semantic_cache(cache_key: str) -> Optional[Dict]:
    if not redis_client:
        return None
    cached = await redis_client.get(f"cache:{cache_key}")
    if cached:
        return json.loads(cached)
    return None


async def store_semantic_cache(cache_key: str, response: Dict):
    if redis_client:
        await redis_client.setex(f"cache:{cache_key}", CACHE_TTL, json.dumps(response))


async def check_guardrail(content: str) -> tuple[bool, List[str]]:
    if not GUARDRAIL_ENABLED:
        return True, []
    
    # Simple keyword-based guardrail (mock)
    blocked_patterns = [
        "ignore previous instructions",
        "system prompt",
        "reveal your instructions",
        "bypass",
        "jailbreak",
        "dan mode",
    ]
    
    content_lower = content.lower()
    violations = [p for p in blocked_patterns if p in content_lower]
    
    return len(violations) == 0, violations


async def store_trace(trace: TraceEntry):
    if not TRACE_ENABLED or not redis_client:
        return
    await redis_client.lpush("traces:recent", trace.model_dump_json())
    await redis_client.ltrim("traces:recent", 0, 999)


# ==================== ENDPOINTS ====================

@app.get("/health", response_model=HealthResponse)
async def health(api_key: str = Depends(verify_api_key)):
    redis_status = "healthy"
    try:
        if redis_client:
            await redis_client.ping()
        else:
            redis_status = "disconnected"
    except:
        redis_status = "unhealthy"
    
    return HealthResponse(
        timestamp=datetime.utcnow().isoformat(),
        services={"redis": redis_status}
    )


@app.post("/v1/chat/completions", response_model=ChatResponse)
async def chat_completions(
    request: ChatRequest,
    api_key: str = Depends(verify_api_key),
    x_trace_id: Optional[str] = Header(None)
):
    start_time = time.time()
    trace_id = x_trace_id or generate_trace_id()
    
    # Prepare input for caching
    input_text = json.dumps([m.get("content", "") for m in request.messages], sort_keys=True)
    cache_key = hash_content(f"{request.model}:{input_text}:{request.temperature}:{request.max_tokens}")
    
    # Check semantic cache
    cached_response = await check_semantic_cache(cache_key)
    if cached_response:
        cached_response["trace_id"] = trace_id
        cached_response["cached"] = True
        return ChatResponse(**cached_response)
    
    # Guardrail check
    all_content = " ".join([m.get("content", "") for m in request.messages])
    allowed, violations = await check_guardrail(all_content)
    
    if not allowed:
        raise HTTPException(
            status_code=400,
            detail={"error": "Content blocked by guardrail", "violations": violations}
        )
    
    # Mock response generation
    last_user_msg = next((m["content"] for m in reversed(request.messages) if m.get("role") == "user"), "")
    
    # Generate contextual mock response based on keywords
    if "resume" in last_user_msg.lower() or "cv" in last_user_msg.lower():
        mock_content = "Here's an improved version of your resume section: [AI-enhanced content with action verbs, quantified achievements, and ATS keywords]"
    elif "ats" in last_user_msg.lower():
        mock_content = "ATS Score: 85/100. Key improvements: add more keywords from job description, quantify achievements, use standard section headers."
    elif "template" in last_user_msg.lower():
        mock_content = "Recommended template: Modern Professional - clean layout, ATS-friendly, works well for tech roles."
    else:
        mock_content = "I've analyzed your request and generated a tailored response. [Mock AI response]"
    
    tokens_in = int(estimate_tokens(input_text))
    tokens_out = int(estimate_tokens(mock_content))
    latency_ms = int((time.time() - start_time) * 1000)
    cost = estimate_cost(tokens_in, tokens_out, request.model)
    
    response = {
        "id": f"chatcmpl-{uuid.uuid4().hex[:12]}",
        "object": "chat.completion",
        "created": int(time.time()),
        "model": request.model,
        "choices": [{
            "index": 0,
            "message": {
                "role": "assistant",
                "content": mock_content
            },
            "finish_reason": "stop"
        }],
        "usage": {
            "prompt_tokens": tokens_in,
            "completion_tokens": tokens_out,
            "total_tokens": tokens_in + tokens_out
        },
        "trace_id": trace_id,
        "cached": False
    }
    
    # Store in cache
    await store_semantic_cache(cache_key, response)
    
    # Store trace
    trace = TraceEntry(
        trace_id=trace_id,
        timestamp=datetime.utcnow().isoformat(),
        request_type="chat",
        model=request.model,
        input_hash=hash_content(input_text),
        output_hash=hash_content(mock_content),
        tokens_in=tokens_in,
        tokens_out=tokens_out,
        latency_ms=latency_ms,
        cost_usd=cost,
        cached=False,
        guardrail_passed=allowed,
        metadata=request.metadata
    )
    await store_trace(trace)
    
    return ChatResponse(**response)


@app.post("/v1/embeddings", response_model=EmbeddingResponse)
async def embeddings(
    request: EmbeddingRequest,
    api_key: str = Depends(verify_api_key),
    x_trace_id: Optional[str] = Header(None)
):
    start_time = time.time()
    trace_id = x_trace_id or generate_trace_id()
    
    input_text = json.dumps(request.input, sort_keys=True)
    cache_key = hash_content(f"emb:{request.model}:{input_text}")
    
    cached = await check_semantic_cache(cache_key)
    if cached:
        cached["trace_id"] = trace_id
        cached["cached"] = True
        return EmbeddingResponse(**cached)
    
    # Guardrail
    all_content = " ".join(request.input)
    allowed, violations = await check_guardrail(all_content)
    if not allowed:
        raise HTTPException(status_code=400, detail={"error": "Content blocked", "violations": violations})
    
    # Mock embeddings
    embeddings_data = []
    total_tokens = 0
    for i, text in enumerate(request.input):
        tokens = int(estimate_tokens(text))
        total_tokens += tokens
        # Mock 1536-dim vector
        embedding = [0.01 * (hash(text + str(j)) % 100) for j in range(1536)]
        embeddings_data.append({
            "object": "embedding",
            "index": i,
            "embedding": embedding
        })
    
    latency_ms = int((time.time() - start_time) * 1000)
    cost = estimate_cost(total_tokens, 0, request.model)
    
    response = {
        "object": "list",
        "data": embeddings_data,
        "model": request.model,
        "usage": {
            "prompt_tokens": total_tokens,
            "total_tokens": total_tokens
        },
        "trace_id": trace_id
    }
    
    await store_semantic_cache(cache_key, response)
    
    trace = TraceEntry(
        trace_id=trace_id,
        timestamp=datetime.utcnow().isoformat(),
        request_type="embedding",
        model=request.model,
        input_hash=hash_content(input_text),
        output_hash="embedding_vector",
        tokens_in=total_tokens,
        tokens_out=0,
        latency_ms=latency_ms,
        cost_usd=cost,
        cached=False,
        guardrail_passed=allowed
    )
    await store_trace(trace)
    
    return EmbeddingResponse(**response)


@app.post("/v1/guardrail/check", response_model=GuardrailResponse)
async def guardrail_check(
    request: GuardrailRequest,
    api_key: str = Depends(verify_api_key)
):
    allowed, violations = await check_guardrail(request.content)
    return GuardrailResponse(
        allowed=allowed,
        reason="Content passes guardrail checks" if allowed else "Content blocked by guardrail",
        categories=violations
    )


@app.get("/v1/traces")
async def get_traces(
    limit: int = 100,
    api_key: str = Depends(verify_api_key)
):
    if not redis_client:
        return {"traces": [], "error": "Redis not available"}
    
    traces_json = await redis_client.lrange("traces:recent", 0, limit - 1)
    traces = [json.loads(t) for t in traces_json]
    return {"traces": traces, "count": len(traces)}


@app.get("/v1/cache/stats")
async def cache_stats(api_key: str = Depends(verify_api_key)):
    if not redis_client:
        return {"error": "Redis not available"}
    
    keys = await redis_client.keys("cache:*")
    return {
        "cache_entries": len(keys),
        "ttl_seconds": CACHE_TTL
    }


@app.delete("/v1/cache")
async def clear_cache(api_key: str = Depends(verify_api_key)):
    if not redis_client:
        return {"error": "Redis not available"}
    
    keys = await redis_client.keys("cache:*")
    if keys:
        await redis_client.delete(*keys)
    return {"cleared": len(keys)}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8080)