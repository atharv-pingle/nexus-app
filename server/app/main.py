import os
import datetime
from contextlib import asynccontextmanager
from typing import List, Optional
from fastapi import FastAPI, Depends, HTTPException, status, Form, Request
from fastapi.responses import HTMLResponse, RedirectResponse
from fastapi.templating import Jinja2Templates
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from sqlalchemy.orm import Session

from app.database import engine, Base, get_db
from app.models import User, ScheduleBlock, Habit, FrictionLog, ServerSetting
from app.auth import (
    verify_password,
    get_password_hash,
    create_access_token,
    get_current_user,
    get_current_admin_user,
    decode_token
)
from app.openrouter import (
    test_openrouter_key,
    get_setting,
    set_setting,
    execute_chat,
    DEFAULT_OPENROUTER_MODEL
)

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Initialize database tables
    Base.metadata.create_all(bind=engine)
    
    # Auto-bootstrap default admin user if none exists
    db = next(get_db())
    try:
        admin = db.query(User).filter(User.username == "admin").first()
        if not admin:
            admin_user = User(
                username="admin",
                email="admin@nexus.internal",
                hashed_password=get_password_hash("nexusadmin123"),
                role="admin",
                is_active=True
            )
            db.add(admin_user)
            db.commit()
            print(">>> Bootstrapped default admin user: 'admin' / 'nexusadmin123'")
            
        # Ensure default model setting exists
        if not get_setting(db, "default_model"):
            set_setting(db, "default_model", DEFAULT_OPENROUTER_MODEL)
    finally:
        db.close()
    yield

app = FastAPI(title="Nexus Server", version="1.0.0", lifespan=lifespan)

# Enable CORS for local/remote mobile app access
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Setup Templates
templates_dir = os.path.join(os.path.dirname(__file__), "templates")
templates = Jinja2Templates(directory=templates_dir)

# ----------------- SCHEMAS -----------------

class UserCreate(BaseModel):
    username: str
    email: str
    password: str
    role: str = "user"

class UserResponse(BaseModel):
    id: int
    username: str
    email: str
    role: str
    is_active: bool
    created_at: datetime.datetime

    class Config:
        from_attributes = True

class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user: UserResponse

class ScheduleBlockSchema(BaseModel):
    id: Optional[int] = None
    title: str
    block_type: str = "FOCUS"
    start_time: str
    end_time: str
    duration_minutes: int = 90
    is_completed: bool = False
    status: str = "UPCOMING"
    delay_count: int = 0
    scheduled_date: Optional[str] = None

class HabitSchema(BaseModel):
    id: Optional[int] = None
    title: str
    category: str = "STRENGTH"
    target_days: int = 66
    current_day: int = 1
    streak: int = 0
    last_completed_date: Optional[str] = None

class FrictionLogSchema(BaseModel):
    block_id: Optional[int] = None
    friction_reason: str
    energy_level: str = "MEDIUM"
    voice_transcript: Optional[str] = None

class SettingsUpdate(BaseModel):
    openrouter_api_key: Optional[str] = None
    default_model: Optional[str] = None

class KeyTestRequest(BaseModel):
    key: str

class VoiceCommandRequest(BaseModel):
    transcript: str

class VoiceCommandResponse(BaseModel):
    action_taken: str
    voice_response: str
    updated_schedule: List[ScheduleBlockSchema]

# ----------------- WEB UI ROUTES -----------------

@app.get("/", response_class=HTMLResponse)
async def root(request: Request):
    return templates.TemplateResponse("dashboard.html", {"request": request})

@app.get("/login", response_class=HTMLResponse)
async def login_page(request: Request):
    return templates.TemplateResponse("login.html", {"request": request})

@app.get("/dashboard", response_class=HTMLResponse)
async def dashboard_page(request: Request):
    return templates.TemplateResponse("dashboard.html", {"request": request})

# ----------------- AUTH API -----------------

@app.post("/api/v1/auth/login", response_model=TokenResponse)
async def login(
    username: str = Form(...),
    password: str = Form(...),
    db: Session = Depends(get_db)
):
    # Support login with either username or email
    user = db.query(User).filter((User.username == username) | (User.email == username)).first()
    if not user or not verify_password(password, user.hashed_password):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect username or password"
        )
    if not user.is_active:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Account is disabled")
    
    token = create_access_token(data={"sub": user.username, "role": user.role, "id": user.id})
    return TokenResponse(
        access_token=token,
        user=UserResponse.from_orm(user)
    )

@app.get("/api/v1/auth/me", response_model=UserResponse)
async def get_me(current_user: User = Depends(get_current_user)):
    return UserResponse.from_orm(current_user)

# ----------------- ADMIN API -----------------

@app.get("/api/v1/admin/users", response_model=List[UserResponse])
async def list_users(
    admin: User = Depends(get_current_admin_user),
    db: Session = Depends(get_db)
):
    users = db.query(User).order_by(User.id.asc()).all()
    return [UserResponse.from_orm(u) for u in users]

@app.post("/api/v1/admin/users", response_model=UserResponse)
async def create_user(
    user_in: UserCreate,
    admin: User = Depends(get_current_admin_user),
    db: Session = Depends(get_db)
):
    if db.query(User).filter(User.username == user_in.username).first():
        raise HTTPException(status_code=400, detail="Username already exists")
    if db.query(User).filter(User.email == user_in.email).first():
        raise HTTPException(status_code=400, detail="Email already registered")
    
    new_user = User(
        username=user_in.username,
        email=user_in.email,
        hashed_password=get_password_hash(user_in.password),
        role=user_in.role,
        is_active=True
    )
    db.add(new_user)
    db.commit()
    db.refresh(new_user)
    
    # Seed default initial schedule & habits for the new user
    today_str = datetime.date.today().isoformat()
    defaults = [
        ScheduleBlock(user_id=new_user.id, title="WFH Focus Block (Ultradian 1)", block_type="FOCUS", start_time="08:00", end_time="09:30", duration_minutes=90, scheduled_date=today_str, status="UPCOMING"),
        ScheduleBlock(user_id=new_user.id, title="WFH Execution Block (Ultradian 2)", block_type="FOCUS", start_time="10:00", end_time="11:30", duration_minutes=90, scheduled_date=today_str, status="UPCOMING"),
        ScheduleBlock(user_id=new_user.id, title="15:30 Interceptor & Power Nap (NSDR)", block_type="TRANSITION", start_time="15:30", end_time="15:55", duration_minutes=25, scheduled_date=today_str, status="UPCOMING"),
        ScheduleBlock(user_id=new_user.id, title="Dumbbell & Bench Strength Session", block_type="WORKOUT", start_time="16:00", end_time="16:45", duration_minutes=45, scheduled_date=today_str, status="UPCOMING"),
        ScheduleBlock(user_id=new_user.id, title="SaaS Income Engine Deep Work", block_type="SAAS", start_time="17:00", end_time="18:30", duration_minutes=90, scheduled_date=today_str, status="UPCOMING"),
    ]
    db.add_all(defaults)
    
    default_habits = [
        Habit(user_id=new_user.id, title="Dumbbell & Bench Strength", category="STRENGTH", current_day=1, streak=0),
        Habit(user_id=new_user.id, title="15:30 Work-to-Life Transition Interceptor", category="SLEEP", current_day=1, streak=0),
        Habit(user_id=new_user.id, title="Nightly Schedule Review", category="FOCUS", current_day=1, streak=0),
    ]
    db.add_all(default_habits)
    db.commit()

    return UserResponse.from_orm(new_user)

@app.put("/api/v1/admin/users/{user_id}/status")
async def toggle_user_status(
    user_id: int,
    active: bool,
    admin: User = Depends(get_current_admin_user),
    db: Session = Depends(get_db)
):
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    if user.id == admin.id:
        raise HTTPException(status_code=400, detail="Cannot disable own admin account")
    user.is_active = active
    db.commit()
    return {"status": "ok", "user_id": user_id, "is_active": active}

@app.get("/api/v1/admin/users/{user_id}/data")
async def get_user_data(
    user_id: int,
    admin: User = Depends(get_current_admin_user),
    db: Session = Depends(get_db)
):
    today_str = datetime.date.today().isoformat()
    schedules = db.query(ScheduleBlock).filter(
        ScheduleBlock.user_id == user_id,
        ScheduleBlock.scheduled_date == today_str
    ).order_by(ScheduleBlock.start_time.asc()).all()
    
    habits = db.query(Habit).filter(Habit.user_id == user_id).all()
    friction = db.query(FrictionLog).filter(FrictionLog.user_id == user_id).order_by(FrictionLog.timestamp.desc()).limit(10).all()

    return {
        "schedules": schedules,
        "habits": habits,
        "friction_logs": friction
    }

@app.get("/api/v1/admin/settings")
async def get_admin_settings(
    admin: User = Depends(get_current_admin_user),
    db: Session = Depends(get_db)
):
    key = get_setting(db, "openrouter_api_key")
    model = get_setting(db, "default_model", DEFAULT_OPENROUTER_MODEL)
    return {
        "openrouter_api_key": key,
        "default_model": model
    }

@app.post("/api/v1/admin/settings")
async def save_admin_settings(
    settings: SettingsUpdate,
    admin: User = Depends(get_current_admin_user),
    db: Session = Depends(get_db)
):
    if settings.openrouter_api_key is not None:
        set_setting(db, "openrouter_api_key", settings.openrouter_api_key)
    if settings.default_model is not None:
        set_setting(db, "default_model", settings.default_model)
    return {"status": "ok"}

@app.post("/api/v1/admin/test-ai-key")
async def test_key(
    req: KeyTestRequest,
    admin: User = Depends(get_current_admin_user)
):
    res = await test_openrouter_key(req.key)
    return res

# ----------------- SCHEDULE APIS (FOR USER & MOBILE APP) -----------------

@app.get("/api/v1/schedule/today", response_model=List[ScheduleBlockSchema])
async def get_today_schedule(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    today_str = datetime.date.today().isoformat()
    blocks = db.query(ScheduleBlock).filter(
        ScheduleBlock.user_id == current_user.id,
        ScheduleBlock.scheduled_date == today_str
    ).order_by(ScheduleBlock.start_time.asc()).all()
    
    if not blocks:
        # Seed initial template if first time today
        defaults = [
            ScheduleBlock(user_id=current_user.id, title="WFH Focus Block (Ultradian 1)", block_type="FOCUS", start_time="08:00", end_time="09:30", duration_minutes=90, scheduled_date=today_str, status="UPCOMING"),
            ScheduleBlock(user_id=current_user.id, title="WFH Execution Block (Ultradian 2)", block_type="FOCUS", start_time="10:00", end_time="11:30", duration_minutes=90, scheduled_date=today_str, status="UPCOMING"),
            ScheduleBlock(user_id=current_user.id, title="15:30 Interceptor & Power Nap (NSDR)", block_type="TRANSITION", start_time="15:30", end_time="15:55", duration_minutes=25, scheduled_date=today_str, status="UPCOMING"),
            ScheduleBlock(user_id=current_user.id, title="Dumbbell & Bench Strength Session", block_type="WORKOUT", start_time="16:00", end_time="16:45", duration_minutes=45, scheduled_date=today_str, status="UPCOMING"),
            ScheduleBlock(user_id=current_user.id, title="SaaS Income Engine Deep Work", block_type="SAAS", start_time="17:00", end_time="18:30", duration_minutes=90, scheduled_date=today_str, status="UPCOMING"),
        ]
        db.add_all(defaults)
        db.commit()
        blocks = defaults
        
    return [
        ScheduleBlockSchema(
            id=b.id,
            title=b.title,
            block_type=b.block_type,
            start_time=b.start_time,
            end_time=b.end_time,
            duration_minutes=b.duration_minutes,
            is_completed=b.is_completed,
            status=b.status,
            delay_count=b.delay_count,
            scheduled_date=b.scheduled_date
        ) for b in blocks
    ]

@app.post("/api/v1/schedule/rebalance")
async def rebalance_schedule(
    block_id: int,
    action: str,  # "DELAY_15", "DELAY_30", "SKIP"
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    block = db.query(ScheduleBlock).filter(
        ScheduleBlock.id == block_id,
        ScheduleBlock.user_id == current_user.id
    ).first()
    if not block:
        raise HTTPException(status_code=404, detail="Block not found")
        
    if action == "SKIP":
        block.status = "SKIPPED"
        db.commit()
    elif action in ("DELAY_15", "DELAY_30"):
        delay_mins = 15 if action == "DELAY_15" else 30
        block.delay_count += 1
        block.status = "DELAYED"
        
        # Calculate shifted times
        def add_mins(time_str: str, mins: int) -> str:
            parts = time_str.split(":")
            h, m = int(parts[0]), int(parts[1])
            tot = h * 60 + m + mins
            return f"{((tot // 60) % 24):02d}:{tot % 60:02d}"
            
        block.start_time = add_mins(block.start_time, delay_mins)
        block.end_time = add_mins(block.end_time, delay_mins)
        
        # Ripple forward upcoming blocks
        upcoming = db.query(ScheduleBlock).filter(
            ScheduleBlock.user_id == current_user.id,
            ScheduleBlock.scheduled_date == block.scheduled_date,
            ScheduleBlock.id > block.id,
            ScheduleBlock.is_completed == False
        ).all()
        for ub in upcoming:
            ub.start_time = add_mins(ub.start_time, delay_mins)
            ub.end_time = add_mins(ub.end_time, delay_mins)
        db.commit()

    return {"status": "ok", "action": action}

@app.put("/api/v1/schedule/blocks/{block_id}/complete")
async def complete_block(
    block_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    block = db.query(ScheduleBlock).filter(
        ScheduleBlock.id == block_id,
        ScheduleBlock.user_id == current_user.id
    ).first()
    if not block:
        raise HTTPException(status_code=404, detail="Block not found")
    block.is_completed = True
    block.status = "COMPLETED"
    db.commit()
    return {"status": "ok", "block_id": block_id}

# ----------------- HABIT APIS (66-DAY AUTOMATICITY) -----------------

@app.get("/api/v1/habits", response_model=List[HabitSchema])
async def get_habits(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    habits = db.query(Habit).filter(Habit.user_id == current_user.id).all()
    return [
        HabitSchema(
            id=h.id,
            title=h.title,
            category=h.category,
            target_days=h.target_days,
            current_day=h.current_day,
            streak=h.streak,
            last_completed_date=h.last_completed_date
        ) for h in habits
    ]

@app.post("/api/v1/habits/{habit_id}/toggle")
async def toggle_habit(
    habit_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    habit = db.query(Habit).filter(
        Habit.id == habit_id,
        Habit.user_id == current_user.id
    ).first()
    if not habit:
        raise HTTPException(status_code=404, detail="Habit not found")
        
    today_str = datetime.date.today().isoformat()
    if habit.last_completed_date == today_str:
        # Already completed today: undo
        habit.last_completed_date = None
        habit.streak = max(0, habit.streak - 1)
        habit.current_day = max(1, habit.current_day - 1)
    else:
        habit.last_completed_date = today_str
        habit.streak += 1
        habit.current_day = min(66, habit.current_day + 1)
        
    db.commit()
    return {"status": "ok", "streak": habit.streak, "current_day": habit.current_day}

# ----------------- FRICTION & VOICE AGENT APIS -----------------

@app.post("/api/v1/friction/log")
async def log_friction(
    friction: FrictionLogSchema,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    log = FrictionLog(
        user_id=current_user.id,
        block_id=friction.block_id,
        friction_reason=friction.friction_reason,
        energy_level=friction.energy_level,
        voice_transcript=friction.voice_transcript
    )
    db.add(log)
    db.commit()
    return {"status": "ok", "id": log.id}

@app.post("/api/v1/agent/voice", response_model=VoiceCommandResponse)
async def process_voice(
    req: VoiceCommandRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    today_str = datetime.date.today().isoformat()
    blocks = db.query(ScheduleBlock).filter(
        ScheduleBlock.user_id == current_user.id,
        ScheduleBlock.scheduled_date == today_str
    ).order_by(ScheduleBlock.start_time.asc()).all()

    sys_prompt = """You are NEXUS Voice Orchestrator. The user speaks hands-free commands.
Respond with concise JSON matching:
{
  "action": "DELAY_15" | "DELAY_30" | "COMPLETE" | "LOG_FRICTION" | "NONE",
  "voice_response": "Short 1-sentence confirmation for Text-To-Speech audio output."
}"""

    res = await execute_chat(db, sys_prompt, f"User said: '{req.transcript}'", json_mode=True)
    voice_resp_text = "I've noted that for your schedule."
    action = "NONE"
    
    if res.get("success"):
        try:
            import json as pyjson
            parsed = pyjson.loads(res.get("content", "{}"))
            action = parsed.get("action", "NONE")
            voice_resp_text = parsed.get("voice_response", voice_resp_text)
        except Exception:
            pass

    # Apply action to current active block if needed
    if action in ("DELAY_15", "DELAY_30") and blocks:
        active = next((b for b in blocks if not b.is_completed), blocks[0])
        delay = 15 if action == "DELAY_15" else 30
        active.delay_count += 1
        active.status = "DELAYED"
        db.commit()

    return VoiceCommandResponse(
        action_taken=action,
        voice_response=voice_resp_text,
        updated_schedule=[
            ScheduleBlockSchema(
                id=b.id,
                title=b.title,
                block_type=b.block_type,
                start_time=b.start_time,
                end_time=b.end_time,
                duration_minutes=b.duration_minutes,
                is_completed=b.is_completed,
                status=b.status,
                delay_count=b.delay_count,
                scheduled_date=b.scheduled_date
            ) for b in blocks
        ]
    )
