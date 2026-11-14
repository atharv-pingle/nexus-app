import os
import json
import httpx
from typing import Optional, Dict, Any, List
from sqlalchemy.orm import Session
from app.models import ServerSetting

DEFAULT_OPENROUTER_MODEL = "deepseek/deepseek-r1"
OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions"

def get_setting(db: Session, key: str, default: str = "") -> str:
    setting = db.query(ServerSetting).filter(ServerSetting.key == key).first()
    if setting and setting.value:
        return setting.value
    return os.getenv(key.upper(), default)

def set_setting(db: Session, key: str, value: str):
    setting = db.query(ServerSetting).filter(ServerSetting.key == key).first()
    if not setting:
        setting = ServerSetting(key=key, value=value)
        db.add(setting)
    else:
        setting.value = value
    db.commit()

async def test_openrouter_key(api_key: str) -> Dict[str, Any]:
    if not api_key:
        return {"success": False, "error": "API Key is empty"}
    headers = {
        "Authorization": f"Bearer {api_key.strip()}",
        "HTTP-Referer": "https://nexus-app.internal",
        "X-Title": "Nexus Admin"
    }
    payload = {
        "model": "deepseek/deepseek-r1",
        "messages": [{"role": "user", "content": "ping"}],
        "max_tokens": 5
    }
    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            resp = await client.post(OPENROUTER_URL, headers=headers, json=payload)
            if resp.status_code == 200:
                return {"success": True, "message": "OpenRouter Key is valid!"}
            else:
                return {"success": False, "error": f"HTTP {resp.status_code}: {resp.text}"}
    except Exception as e:
        return {"success": False, "error": str(e)}

async def execute_chat(
    db: Session,
    system_prompt: str,
    user_prompt: str,
    model: Optional[str] = None,
    json_mode: bool = False
) -> Dict[str, Any]:
    api_key = get_setting(db, "openrouter_api_key")
    if not api_key:
        return {"success": False, "error": "OpenRouter API Key is not configured on the server."}
    
    selected_model = model or get_setting(db, "default_model", DEFAULT_OPENROUTER_MODEL)
    
    headers = {
        "Authorization": f"Bearer {api_key.strip()}",
        "Content-Type": "application/json",
        "HTTP-Referer": "https://nexus-planner.app",
        "X-Title": "Nexus Autonomous Agent"
    }
    
    messages = [
        {"role": "system", "content": system_prompt},
        {"role": "user", "content": user_prompt}
    ]
    
    body: Dict[str, Any] = {
        "model": selected_model,
        "messages": messages,
        "temperature": 0.2
    }
    
    if json_mode:
        body["response_format"] = {"type": "json_object"}
        
    try:
        async with httpx.AsyncClient(timeout=60.0) as client:
            resp = await client.post(OPENROUTER_URL, headers=headers, json=body)
            if resp.status_code != 200:
                return {"success": False, "error": f"OpenRouter Error {resp.status_code}: {resp.text}"}
            
            data = resp.json()
            content = data["choices"][0]["message"]["content"]
            return {"success": True, "content": content}
    except Exception as e:
        return {"success": False, "error": str(e)}
