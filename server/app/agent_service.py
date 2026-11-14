import datetime
import json
from typing import List, Dict, Any, Optional
from sqlalchemy.orm import Session
from app.models import User, ScheduleBlock, ChatMessage, Habit
from app.openrouter import execute_chat

DEFAULT_SCHEDULE_TEMPLATE = [
    {"title": "WFH Focus Block (Ultradian 1)", "block_type": "FOCUS", "start_time": "08:00", "end_time": "09:30", "duration_minutes": 90},
    {"title": "Physical Reset: 10 Pushups & Hydrate", "block_type": "FITNESS_NUDGE", "start_time": "09:30", "end_time": "09:40", "duration_minutes": 10},
    {"title": "WFH Execution Block (Ultradian 2)", "block_type": "FOCUS", "start_time": "10:00", "end_time": "11:30", "duration_minutes": 90},
    {"title": "Lunch & Sunlight Walk", "block_type": "TRANSITION", "start_time": "11:30", "end_time": "12:30", "duration_minutes": 60},
    {"title": "Core Work & Communication Sprint", "block_type": "FOCUS", "start_time": "13:00", "end_time": "15:00", "duration_minutes": 120},
    {"title": "15:30 Power Nap / Rest (NSDR)", "block_type": "TRANSITION", "start_time": "15:30", "end_time": "15:55", "duration_minutes": 25},
    {"title": "Dumbbell & Bench Workout", "block_type": "WORKOUT", "start_time": "16:00", "end_time": "16:45", "duration_minutes": 45},
    {"title": "SaaS Income Engine Deep Work", "block_type": "SAAS", "start_time": "17:15", "end_time": "19:00", "duration_minutes": 105},
    {"title": "Physical Reset: 10 Pushups & Stretch", "block_type": "FITNESS_NUDGE", "start_time": "19:00", "end_time": "19:15", "duration_minutes": 15},
    {"title": "Dinner & Personal Downtime", "block_type": "TRANSITION", "start_time": "19:30", "end_time": "21:00", "duration_minutes": 90},
    {"title": "Nightly Schedule Review & Wind Down", "block_type": "WIND_DOWN", "start_time": "21:30", "end_time": "22:30", "duration_minutes": 60},
    {"title": "Sleep & Deep Recovery", "block_type": "SLEEP", "start_time": "23:00", "end_time": "07:30", "duration_minutes": 510},
]

def ensure_full_day_schedule(db: Session, user: User, target_date: Optional[str] = None) -> List[ScheduleBlock]:
    """Ensures user has a continuous full-day schedule fitting every hour until sleep."""
    if not target_date:
        target_date = datetime.date.today().isoformat()
        
    blocks = db.query(ScheduleBlock).filter(
        ScheduleBlock.user_id == user.id,
        ScheduleBlock.scheduled_date == target_date
    ).order_by(ScheduleBlock.start_time.asc()).all()
    
    if not blocks:
        new_blocks = []
        for item in DEFAULT_SCHEDULE_TEMPLATE:
            b = ScheduleBlock(
                user_id=user.id,
                title=item["title"],
                block_type=item["block_type"],
                start_time=item["start_time"],
                end_time=item["end_time"],
                duration_minutes=item["duration_minutes"],
                scheduled_date=target_date,
                status="UPCOMING",
                is_completed=False,
                delay_count=0
            )
            new_blocks.append(b)
        db.add_all(new_blocks)
        db.commit()
        blocks = db.query(ScheduleBlock).filter(
            ScheduleBlock.user_id == user.id,
            ScheduleBlock.scheduled_date == target_date
        ).order_by(ScheduleBlock.start_time.asc()).all()
        
    return blocks

def parse_time_to_minutes(time_str: str) -> int:
    try:
        parts = time_str.split(":")
        return int(parts[0]) * 60 + int(parts[1])
    except Exception:
        return 0

def format_minutes_to_time(minutes: int) -> str:
    h = (minutes // 60) % 24
    m = minutes % 60
    return f"{h:02d}:{m:02d}"

def shift_remaining_blocks(db: Session, user_id: int, date_str: str, from_block_id: int, delay_mins: int):
    """Shifts all upcoming incomplete blocks forward by delay_mins."""
    upcoming = db.query(ScheduleBlock).filter(
        ScheduleBlock.user_id == user_id,
        ScheduleBlock.scheduled_date == date_str,
        ScheduleBlock.id >= from_block_id,
        ScheduleBlock.is_completed == False,
        ScheduleBlock.block_type != "SLEEP"
    ).all()
    
    for b in upcoming:
        start_m = parse_time_to_minutes(b.start_time) + delay_mins
        end_m = parse_time_to_minutes(b.end_time) + delay_mins
        b.start_time = format_minutes_to_time(start_m)
        b.end_time = format_minutes_to_time(end_m)
        b.delay_count += 1
        b.status = "DELAYED"
    db.commit()

async def check_proactive_interventions(db: Session, user: User, now_time_str: Optional[str] = None) -> Optional[ChatMessage]:
    """
    Evaluates current time vs schedule and user memory to proactively message the user.
    Nudges for:
    - 10 pushups / physical reset between work blocks
    - Checking in on overdue or completed tasks
    - Evening wind-down before sleep
    """
    today_str = datetime.date.today().isoformat()
    if not now_time_str:
        now_dt = datetime.datetime.now()
        now_time_str = f"{now_dt.hour:02d}:{now_dt.minute:02d}"
        
    now_mins = parse_time_to_minutes(now_time_str)
    blocks = ensure_full_day_schedule(db, user, today_str)
    
    # Check last message timestamp to avoid spamming (minimum 15 mins between proactive nudges)
    last_msg = db.query(ChatMessage).filter(
        ChatMessage.user_id == user.id,
        ChatMessage.sender == "agent"
    ).order_by(ChatMessage.created_at.desc()).first()
    
    if last_msg:
        diff_secs = (datetime.datetime.utcnow() - last_msg.created_at).total_seconds()
        if diff_secs < 120:  # Allow 2 min cooldown in testing/demo, or check content
            return None

    # Find currently active block
    active_block = None
    next_block = None
    for i, b in enumerate(blocks):
        sm = parse_time_to_minutes(b.start_time)
        em = parse_time_to_minutes(b.end_time)
        if sm <= now_mins <= em:
            active_block = b
            if i + 1 < len(blocks):
                next_block = blocks[i + 1]
            break
        elif now_mins < sm and not next_block:
            next_block = b

    # Case 1: In a fitness nudge block or transition between work blocks
    if active_block and active_block.block_type == "FITNESS_NUDGE" and not active_block.is_completed:
        actions = [
            {"id": "pushup_done", "label": "✓ Did 10 Pushups", "action": "COMPLETE_BLOCK", "block_id": active_block.id},
            {"id": "push_10m", "label": "+10 min", "action": "DELAY_SCHEDULE", "mins": 10}
        ]
        msg = ChatMessage(
            user_id=user.id,
            sender="agent",
            content=f"Time for a quick physical reset! Step away from the screen and knock out 10 pushups + drink a full glass of water.",
            message_type="FITNESS_NUDGE",
            actions_json=json.dumps(actions)
        )
        db.add(msg)
        db.commit()
        db.refresh(msg)
        return msg

    # Case 2: In a focus block that has gone past its scheduled end time
    overdue_block = None
    for b in blocks:
        em = parse_time_to_minutes(b.end_time)
        if now_mins > em and not b.is_completed and b.block_type in ("FOCUS", "SAAS", "WORKOUT"):
            overdue_block = b
            break
            
    if overdue_block:
        actions = [
            {"id": "done_task", "label": "✓ Completed", "action": "COMPLETE_BLOCK", "block_id": overdue_block.id},
            {"id": "push_20m", "label": "+20m Push", "action": "DELAY_SCHEDULE", "mins": 20},
            {"id": "pushup_break", "label": "Add 10 Pushups Break", "action": "INJECT_PUSHUPS"}
        ]
        msg = ChatMessage(
            user_id=user.id,
            sender="agent",
            content=f"Checking in on '{overdue_block.title}'. Still working on it, or should I mark it finished and shift the rest of your day?",
            message_type="CHECKIN",
            actions_json=json.dumps(actions)
        )
        db.add(msg)
        db.commit()
        db.refresh(msg)
        return msg

    # Case 3: Approaching bedtime / wind-down
    bed_mins = parse_time_to_minutes(user.bedtime or "23:30")
    if bed_mins - 60 <= now_mins <= bed_mins:
        actions = [
            {"id": "review_ready", "label": "Review Tomorrow", "action": "REVIEW_TOMORROW"},
            {"id": "sleep_ready", "label": "Goodnight Nexus", "action": "GOODNIGHT"}
        ]
        msg = ChatMessage(
            user_id=user.id,
            sender="agent",
            content="We're approaching your bedtime wind-down. Screen brightness down, wrap up any open tabs, and let's lock in your recovery for tomorrow.",
            message_type="CHECKIN",
            actions_json=json.dumps(actions)
        )
        db.add(msg)
        db.commit()
        db.refresh(msg)
        return msg

    return None

async def handle_agent_dialogue(db: Session, user: User, user_text: str) -> Dict[str, Any]:
    """
    Main conversational agent brain:
    Processes user input, accesses user memory & live schedule,
    applies schedule/fitness adjustments, and returns an executive assistant response.
    """
    today_str = datetime.date.today().isoformat()
    blocks = ensure_full_day_schedule(db, user, today_str)
    
    # Store user message
    user_msg = ChatMessage(
        user_id=user.id,
        sender="user",
        content=user_text,
        message_type="TEXT"
    )
    db.add(user_msg)
    db.commit()

    # Build context for LLM
    sched_summary = []
    for b in blocks:
        st = "✓" if b.is_completed else ("⏱️" if b.status == "DELAYED" else "○")
        sched_summary.append(f"{st} [{b.id}] {b.start_time}-{b.end_time}: {b.title} ({b.block_type})")
        
    sched_text = "\n".join(sched_summary)
    
    sys_prompt = f"""You are NEXUS, an autonomous, highly capable personal executive assistant.
You actively manage Atharv's time, focus, and physical fitness throughout the entire day until sleep ({user.bedtime or '23:30'}).
Key responsibilities:
1. Always be direct, supportive, and action-oriented. No corporate fluff, no gaming jargon (no "quests" or "XP").
2. Proactively inject physical fitness habits (e.g. "10 pushups", hydration, sunlight walks) to maintain peak energy.
3. If Atharv says a meeting ran over, or he's tired, or asks for an update, adapt the schedule smoothly with zero guilt.
4. Always maintain a continuous plan until bedtime.

CURRENT SCHEDULE TODAY:
{sched_text}

Respond in JSON format:
{{
  "reply": "Concise natural response speaking directly to Atharv.",
  "action": "NONE" | "COMPLETE_TASK" | "DELAY_SCHEDULE" | "ADD_PUSHUPS" | "REORGANIZE_DAY",
  "target_block_id": null or integer block id,
  "delay_minutes": 0 or integer,
  "quick_actions": [
     {{"label": "Label", "action": "ACTION_TYPE"}}
  ]
}}"""

    res = await execute_chat(db, sys_prompt, f"Atharv: {user_text}", json_mode=True)
    reply_text = "I've noted that and updated your schedule."
    action = "NONE"
    target_id = None
    delay_mins = 0
    quick_actions = []

    if res.get("success"):
        try:
            parsed = json.loads(res.get("content", "{}"))
            reply_text = parsed.get("reply", reply_text)
            action = parsed.get("action", "NONE")
            target_id = parsed.get("target_block_id")
            delay_mins = parsed.get("delay_minutes", 0)
            quick_actions = parsed.get("quick_actions", [])
        except Exception as e:
            print("Error parsing LLM JSON:", e)

    # Fallback keyword checks if LLM is concise or in case of format anomaly
    lower = user_text.lower()
    if "pushup" in lower and ("did" in lower or "done" in lower or "finished" in lower):
        action = "COMPLETE_TASK"
        # Find fitness block
        for b in blocks:
            if b.block_type == "FITNESS_NUDGE" and not b.is_completed:
                target_id = b.id
                break
        reply_text = "Great job knocking out those 10 pushups! Bloodflow restored. Ready for the next focus block."
    elif "delay" in lower or "push" in lower or "ran over" in lower or "late" in lower:
        if delay_mins == 0:
            delay_mins = 20
        action = "DELAY_SCHEDULE"

    # Execute actions
    if action == "COMPLETE_TASK" and target_id:
        b = db.query(ScheduleBlock).filter(ScheduleBlock.id == target_id, ScheduleBlock.user_id == user.id).first()
        if b:
            b.is_completed = True
            b.status = "COMPLETED"
            db.commit()
    elif action == "DELAY_SCHEDULE" and delay_mins > 0:
        # Shift incomplete blocks
        first_inc = next((b for b in blocks if not b.is_completed), None)
        if first_inc:
            shift_remaining_blocks(db, user.id, today_str, first_inc.id, delay_mins)
    elif action == "ADD_PUSHUPS":
        # Inject 10 pushups block before next focus block
        first_inc = next((b for b in blocks if not b.is_completed), None)
        if first_inc:
            nudge = ScheduleBlock(
                user_id=user.id,
                title="Physical Reset: 10 Pushups & Hydrate",
                block_type="FITNESS_NUDGE",
                start_time=first_inc.start_time,
                end_time=format_minutes_to_time(parse_time_to_minutes(first_inc.start_time) + 10),
                duration_minutes=10,
                scheduled_date=today_str,
                status="UPCOMING"
            )
            db.add(nudge)
            db.commit()
            shift_remaining_blocks(db, user.id, today_str, first_inc.id, 10)

    # Save agent response to chat history
    agent_msg = ChatMessage(
        user_id=user.id,
        sender="agent",
        content=reply_text,
        message_type="TEXT",
        actions_json=json.dumps(quick_actions) if quick_actions else None
    )
    db.add(agent_msg)
    db.commit()
    db.refresh(agent_msg)

    # Reload fresh schedule
    updated_blocks = db.query(ScheduleBlock).filter(
        ScheduleBlock.user_id == user.id,
        ScheduleBlock.scheduled_date == today_str
    ).order_by(ScheduleBlock.start_time.asc()).all()

    return {
        "reply": reply_text,
        "message_id": agent_msg.id,
        "action_taken": action,
        "schedule": [
            {
                "id": b.id,
                "title": b.title,
                "block_type": b.block_type,
                "start_time": b.start_time,
                "end_time": b.end_time,
                "duration_minutes": b.duration_minutes,
                "is_completed": b.is_completed,
                "status": b.status,
                "delay_count": b.delay_count,
                "scheduled_date": b.scheduled_date
            } for b in updated_blocks
        ],
        "quick_actions": quick_actions
    }
