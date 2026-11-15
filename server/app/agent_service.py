import datetime
import json
import re
from typing import List, Dict, Any, Optional
from sqlalchemy.orm import Session
from app.models import User, ScheduleBlock, ChatMessage, Habit, ServerSetting
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
    """Returns schedule blocks for the target date without injecting pre-filled dummy data."""
    if not target_date:
        target_date = datetime.date.today().isoformat()
        
    blocks = db.query(ScheduleBlock).filter(
        ScheduleBlock.user_id == user.id,
        ScheduleBlock.scheduled_date == target_date
    ).order_by(ScheduleBlock.start_time.asc()).all()
    
    return blocks

def rollover_incomplete_tasks(db: Session, user: User) -> List[ScheduleBlock]:
    """Core brain: automatically carries over incomplete tasks from today into tomorrow's queue."""
    today_str = datetime.date.today().isoformat()
    tomorrow_str = (datetime.date.today() + datetime.timedelta(days=1)).isoformat()
    
    incomplete = db.query(ScheduleBlock).filter(
        ScheduleBlock.user_id == user.id,
        ScheduleBlock.scheduled_date == today_str,
        ScheduleBlock.is_completed == False,
        ScheduleBlock.block_type != "SLEEP"
    ).all()
    
    carried = []
    for inc in incomplete:
        exists = db.query(ScheduleBlock).filter(
            ScheduleBlock.user_id == user.id,
            ScheduleBlock.scheduled_date == tomorrow_str,
            ScheduleBlock.title == inc.title
        ).first()
        if not exists:
            b = ScheduleBlock(
                user_id=user.id,
                title=inc.title,
                block_type=inc.block_type,
                start_time=inc.start_time,
                end_time=inc.end_time,
                duration_minutes=inc.duration_minutes,
                scheduled_date=tomorrow_str,
                status="CARRIED_OVER",
                is_completed=False,
                delay_count=0
            )
            db.add(b)
            carried.append(b)
    if carried:
        db.commit()
    return carried

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
    
    # Check last message timestamp to avoid spamming (minimum cooldown)
    last_msg = db.query(ChatMessage).filter(
        ChatMessage.user_id == user.id,
        ChatMessage.sender == "agent"
    ).order_by(ChatMessage.created_at.desc()).first()
    
    if last_msg:
        diff_secs = (datetime.datetime.utcnow() - last_msg.created_at).total_seconds()
        if diff_secs < 120:
            return None

    # Find currently active block
    active_block = None
    for b in blocks:
        sm = parse_time_to_minutes(b.start_time)
        em = parse_time_to_minutes(b.end_time)
        if sm <= now_mins <= em:
            active_block = b
            break

    # Case 1: In a fitness nudge block or transition between work blocks
    if active_block and active_block.block_type == "FITNESS_NUDGE" and not active_block.is_completed:
        actions = [
            {"label": "💪 Did 10 Pushups!", "action": "COMPLETE_BLOCK", "block_id": active_block.id},
            {"label": "⏱️ +10m", "action": "DELAY_SCHEDULE", "mins": 10}
        ]
        msg = ChatMessage(
            user_id=user.id,
            sender="agent",
            content="Time for a quick physical reset! Drop and knock out 10 pushups + drink water right now 💥.",
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
            {"label": "✓ Yes, Done!", "action": "COMPLETE_BLOCK", "block_id": overdue_block.id},
            {"label": "⏱️ +15m Shift", "action": "DELAY_SCHEDULE", "mins": 15},
            {"label": "💪 10 Pushups Break", "action": "INJECT_PUSHUPS"}
        ]
        msg = ChatMessage(
            user_id=user.id,
            sender="agent",
            content=f"Checking in on '{overdue_block.title}' 🎯. Are you finished, or should I shift your schedule by +15m?",
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
            {"label": "🌙 Goodnight Nexus", "action": "GOODNIGHT"}
        ]
        msg = ChatMessage(
            user_id=user.id,
            sender="agent",
            content="We're approaching your 23:30 bedtime wind-down 🌙. Dim screens, wrap up any tabs, and let's lock in your recovery for tomorrow.",
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
    Processes user input with multi-turn memory & two-day schedule awareness (Today & Tomorrow),
    executes dynamic task operations (ADD, MOVE, UPDATE, COMPLETE, DELAY), and returns a sleek executive assistant response.
    """
    today_str = datetime.date.today().isoformat()
    tomorrow_str = (datetime.date.today() + datetime.timedelta(days=1)).isoformat()

    # Retrieve past conversation turns for rolling memory
    past_msgs = db.query(ChatMessage).filter(
        ChatMessage.user_id == user.id
    ).order_by(ChatMessage.created_at.desc()).limit(8).all()
    past_msgs.reverse()

    conv_history = []
    for m in past_msgs:
        conv_history.append({
            "role": "user" if m.sender == "user" else "assistant",
            "content": m.content
        })

    # Store incoming user message
    user_msg = ChatMessage(
        user_id=user.id,
        sender="user",
        content=user_text,
        message_type="TEXT"
    )
    db.add(user_msg)
    db.commit()

    # Fetch live blocks for Today and Tomorrow
    today_blocks = ensure_full_day_schedule(db, user, today_str)
    tomorrow_blocks = ensure_full_day_schedule(db, user, tomorrow_str)

    today_summary = [f"{'✓' if b.is_completed else '○'} [ID {b.id}] {b.start_time}-{b.end_time}: {b.title} ({b.block_type})" for b in today_blocks]
    tomorrow_summary = [f"{'✓' if b.is_completed else '○'} [ID {b.id}] {b.start_time}-{b.end_time}: {b.title} ({b.block_type})" for b in tomorrow_blocks]

    today_sched_text = "\n".join(today_summary) if today_summary else "No tasks scheduled for today."
    tomorrow_sched_text = "\n".join(tomorrow_summary) if tomorrow_summary else "No tasks scheduled for tomorrow."

    sys_prompt = f"""You are NEXUS, an autonomous, highly capable personal executive assistant.
You actively manage Atharv's time, focus, and schedule across Today and Tomorrow until sleep ({user.bedtime or '23:30'}).
Key responsibilities:
1. Always be direct, supportive, and action-oriented with motivating contextual emojis (🎯, 💪, ⚡, 🔋, 🌙, 🚀, 💥, 👋, ⏱️).
2. If Atharv asks to move or reschedule a task (e.g. "move tomorrow gym task to today" or "move it to today at 6 PM"), set action="MOVE_TASK", specify target_block_id (if identifiable) or task_title, target_date ("today" or "tomorrow"), and start_time (HH:MM 24h format).
3. If Atharv asks to add a task, set action="ADD_TASK", specify task_title, target_date ("today" or "tomorrow"), start_time (HH:MM 24h), and duration_minutes (default 45).
4. If Atharv completed a task or did pushups, set action="COMPLETE_TASK".
5. If Atharv says a meeting ran over or asks to delay, set action="DELAY_SCHEDULE" and specify delay_minutes.
6. Never output raw markdown asterisks (**) in your reply. Use clean typography.

TODAY'S SCHEDULE ({today_str}):
{today_sched_text}

TOMORROW'S SCHEDULE ({tomorrow_str}):
{tomorrow_sched_text}

Respond strictly in JSON format:
{{
  "reply": "Conversational reply speaking directly to Atharv with contextual emojis (no asterisks).",
  "action": "NONE" | "ADD_TASK" | "MOVE_TASK" | "UPDATE_TASK" | "COMPLETE_TASK" | "DELAY_SCHEDULE" | "ADD_PUSHUPS",
  "task_title": null or string,
  "target_date": "today" | "tomorrow",
  "start_time": null or "HH:MM",
  "duration_minutes": 45,
  "block_type": "FOCUS" | "FITNESS_NUDGE" | "WORKOUT" | "TRANSITION" | "SAAS",
  "target_block_id": null or integer block id,
  "delay_minutes": 0 or integer,
  "show_schedule_widget": false,
  "quick_actions": [
     {{"label": "Label with emoji", "action": "ACTION_TYPE"}}
  ]
}}"""

    res = await execute_chat(db, sys_prompt, f"Atharv: {user_text}", json_mode=True, conversation_history=conv_history)
    reply_text = "Got it, Atharv! 🚀 Synced with your memory and day timeline."
    action = "NONE"
    task_title = None
    target_date = "today"
    start_time = None
    duration_mins = 45
    block_type = "FOCUS"
    target_id = None
    delay_mins = 0
    show_schedule_widget = False
    quick_actions = []

    if res.get("success"):
        try:
            parsed = json.loads(res.get("content", "{}"))
            reply_text = parsed.get("reply", reply_text).replace("**", "")
            action = parsed.get("action", "NONE")
            task_title = parsed.get("task_title")
            target_date = parsed.get("target_date", "today")
            start_time = parsed.get("start_time")
            duration_mins = parsed.get("duration_minutes", 45)
            block_type = parsed.get("block_type", "FOCUS")
            target_id = parsed.get("target_block_id")
            delay_mins = parsed.get("delay_minutes", 0)
            show_schedule_widget = parsed.get("show_schedule_widget", False)
            quick_actions = parsed.get("quick_actions", [])
        except Exception as e:
            print("Error parsing LLM JSON:", e)

    # Heuristic intent detection
    lower = user_text.lower()
    is_for_tomorrow = ("tomorrow" in lower) or (target_date == "tomorrow")
    target_date_str = tomorrow_str if is_for_tomorrow else today_str

    if ("move" in lower or "reschedule" in lower or "instead of" in lower) and action == "NONE":
        action = "MOVE_TASK"
        if "gym" in lower:
            task_title = "Gym"

    if ("add " in lower or "schedule " in lower or "new task" in lower) and not task_title and action != "MOVE_TASK":
        action = "ADD_TASK"
        clean_title = re.sub(r'^(please\s+)?(add|schedule|create)(\s+a)?(\s+task)?(\s+to)?\s+', '', user_text, flags=re.IGNORECASE).strip()
        time_match = re.search(r'at\s+(\d{1,2}(?::\d{2})?\s*(?:am|pm)?)', clean_title, flags=re.IGNORECASE)
        if time_match:
            time_str = time_match.group(1).lower().strip()
            clean_title = clean_title.replace(time_match.group(0), '').strip()
            try:
                if "pm" in time_str:
                    h = int(re.sub(r'[^\d]', '', time_str.split(':')[0]))
                    if h < 12: h += 12
                    start_time = f"{h:02d}:00"
                elif "am" in time_str:
                    h = int(re.sub(r'[^\d]', '', time_str.split(':')[0]))
                    start_time = f"{h:02d}:00"
                else:
                    parts = time_str.split(':')
                    start_time = f"{int(parts[0]):02d}:{int(parts[1]) if len(parts) > 1 else 0:02d}"
            except Exception:
                start_time = "10:00"
        task_title = clean_title.strip() or "Focused Session"

    if "pushup" in lower and ("did" in lower or "done" in lower or "finished" in lower or "crushed" in lower):
        action = "COMPLETE_TASK"
        for b in today_blocks:
            if b.block_type == "FITNESS_NUDGE" and not b.is_completed:
                target_id = b.id
                break
        reply_text = "Boom! 💥 10 Pushups logged into your telemetry. Bloodflow restored and momentum locked! 🚀"

    elif "delay" in lower or "push" in lower or "ran over" in lower or "late" in lower:
        if delay_mins == 0:
            delay_mins = 15
        action = "DELAY_SCHEDULE"

    if "show" in lower or "what is my schedule" in lower or "what's next" in lower or "my plan" in lower:
        show_schedule_widget = True

    # ---------------- EXECUTE AGENT ACTIONS ----------------
    if action == "MOVE_TASK":
        # Directional destination resolution
        dest_to_tomorrow = bool(
            re.search(r'\b(to|for|on|onto)\s+tomorrow\b', lower) or
            (target_date == "tomorrow" and not re.search(r'\b(to|for|on|onto)\s+today\b', lower)) or
            ("tomorrow" in lower and "today" not in lower)
        )
        dest_to_today = bool(
            re.search(r'\b(to|for|on|onto)\s+today\b', lower) or
            (target_date == "today" and not re.search(r'\b(to|for|on|onto)\s+tomorrow\b', lower)) or
            ("today" in lower and "tomorrow" not in lower)
        )

        if dest_to_tomorrow:
            dest_date = tomorrow_str
            src_date = today_str
        elif dest_to_today:
            dest_date = today_str
            src_date = tomorrow_str
        else:
            if target_date == "tomorrow":
                dest_date = tomorrow_str
                src_date = today_str
            else:
                dest_date = today_str
                src_date = tomorrow_str

        # Fallback task keywords if task_title was empty
        if not task_title:
            if "gym" in lower:
                task_title = "Gym"
            elif "workout" in lower:
                task_title = "Workout"
            elif "pushup" in lower:
                task_title = "Pushup"

        block_to_move = None
        if target_id:
            block_to_move = db.query(ScheduleBlock).filter(
                ScheduleBlock.id == target_id,
                ScheduleBlock.user_id == user.id
            ).first()

        if not block_to_move and task_title:
            # 1. Look on source date first
            block_to_move = db.query(ScheduleBlock).filter(
                ScheduleBlock.user_id == user.id,
                ScheduleBlock.scheduled_date == src_date,
                ScheduleBlock.title.ilike(f"%{task_title}%")
            ).order_by(ScheduleBlock.id.asc()).first()

            # 2. If not found on src_date, look on any date
            if not block_to_move:
                block_to_move = db.query(ScheduleBlock).filter(
                    ScheduleBlock.user_id == user.id,
                    ScheduleBlock.title.ilike(f"%{task_title}%")
                ).order_by(ScheduleBlock.id.desc()).first()

        if not block_to_move:
            # Look for any uncompleted non-sleep block on source date
            block_to_move = db.query(ScheduleBlock).filter(
                ScheduleBlock.user_id == user.id,
                ScheduleBlock.scheduled_date == src_date,
                ScheduleBlock.is_completed == False,
                ScheduleBlock.block_type != "SLEEP"
            ).order_by(ScheduleBlock.start_time.asc()).first()

        if block_to_move:
            block_to_move.scheduled_date = dest_date
            block_to_move.status = "UPCOMING"
            block_to_move.is_completed = False

            # Extract start time if specified (e.g. "at 6 PM", "at 18:00")
            time_match = re.search(r'at\s+(\d{1,2}(?::\d{2})?\s*(?:am|pm)?)', lower)
            if time_match and not start_time:
                time_str = time_match.group(1).lower().strip()
                try:
                    if "pm" in time_str:
                        h = int(re.sub(r'[^\d]', '', time_str.split(':')[0]))
                        if h < 12: h += 12
                        start_time = f"{h:02d}:00"
                    elif "am" in time_str:
                        h = int(re.sub(r'[^\d]', '', time_str.split(':')[0]))
                        start_time = f"{h:02d}:00"
                    else:
                        parts = time_str.split(':')
                        start_time = f"{int(parts[0]):02d}:{int(parts[1]) if len(parts) > 1 else 0:02d}"
                except Exception:
                    pass

            if start_time:
                start_m = parse_time_to_minutes(start_time)
                block_to_move.start_time = start_time
                block_to_move.end_time = format_minutes_to_time(start_m + block_to_move.duration_minutes)

            # Clean up redundant duplicate uncompleted task on destination date
            db.query(ScheduleBlock).filter(
                ScheduleBlock.user_id == user.id,
                ScheduleBlock.scheduled_date == dest_date,
                ScheduleBlock.id != block_to_move.id,
                ScheduleBlock.title.ilike(f"%{block_to_move.title}%"),
                ScheduleBlock.is_completed == False
            ).delete(synchronize_session=False)

            db.commit()
            dest_label = "tomorrow" if dest_date == tomorrow_str else "today"
            reply_text = f"Done, Atharv! 🎯 Moved '{block_to_move.title}' to {dest_label} at {block_to_move.start_time}–{block_to_move.end_time}. Your schedule has been auto-rebalanced! ⚡"
        else:
            reply_text = f"I couldn't find a matching task to move. Check your tasks tab and let me know the title or time! 🎯"

    elif action == "ADD_TASK" and task_title:
        if not start_time:
            first_upcoming = next((b for b in today_blocks if not b.is_completed and b.block_type != "SLEEP"), None)
            start_time = first_upcoming.start_time if first_upcoming else "10:00"

        start_m = parse_time_to_minutes(start_time)
        end_time = format_minutes_to_time(start_m + duration_mins)

        new_block = ScheduleBlock(
            user_id=user.id,
            title=task_title,
            block_type=block_type,
            start_time=start_time,
            end_time=end_time,
            duration_minutes=duration_mins,
            scheduled_date=target_date_str,
            status="UPCOMING",
            is_completed=False,
            delay_count=0
        )
        db.add(new_block)
        db.commit()

        day_label = "tomorrow" if is_for_tomorrow else "today"
        reply_text = f"Got it, Atharv! 🎯 Added '{task_title}' for {day_label} at {start_time}–{end_time}. Check your tasks tab to view it!"

    elif (action == "CANCEL_TASK" or (("cancel" in lower or "delete" in lower or "remove" in lower) and ("session" in lower or "task" in lower or "gym" in lower))):
        target_to_cancel = None
        if target_id:
            target_to_cancel = db.query(ScheduleBlock).filter(
                ScheduleBlock.id == target_id,
                ScheduleBlock.user_id == user.id
            ).first()
        if not target_to_cancel:
            search_title = task_title or ("gym" if "gym" in lower else None)
            if search_title:
                target_to_cancel = db.query(ScheduleBlock).filter(
                    ScheduleBlock.user_id == user.id,
                    ScheduleBlock.scheduled_date == today_str,
                    ScheduleBlock.title.ilike(f"%{search_title}%")
                ).first()
            if not target_to_cancel:
                target_to_cancel = db.query(ScheduleBlock).filter(
                    ScheduleBlock.user_id == user.id,
                    ScheduleBlock.scheduled_date == today_str,
                    ScheduleBlock.is_completed == False,
                    ScheduleBlock.block_type != "SLEEP"
                ).order_by(ScheduleBlock.start_time.asc()).first()
        if target_to_cancel:
            task_title_canceled = target_to_cancel.title
            db.delete(target_to_cancel)
            db.commit()
            reply_text = f"Cancelled '{task_title_canceled}'. Your schedule is freed up for focused execution! 🎯"
        else:
            reply_text = "I couldn't find an active task to cancel today."

    elif action == "COMPLETE_TASK" and target_id:
        b = db.query(ScheduleBlock).filter(ScheduleBlock.id == target_id, ScheduleBlock.user_id == user.id).first()
        if b:
            b.is_completed = True
            b.status = "COMPLETED"
            db.commit()

    elif action == "DELAY_SCHEDULE" and delay_mins > 0:
        first_inc = next((b for b in today_blocks if not b.is_completed), None)
        if first_inc:
            shift_remaining_blocks(db, user.id, today_str, first_inc.id, delay_mins)
            reply_text = f"Understood! ⏱️ Shifted your remaining schedule by +{delay_mins} mins without guilt."

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

    # Reload fresh schedules for both today and tomorrow
    today_updated = db.query(ScheduleBlock).filter(
        ScheduleBlock.user_id == user.id,
        ScheduleBlock.scheduled_date == today_str
    ).order_by(ScheduleBlock.start_time.asc()).all()

    tomorrow_updated = db.query(ScheduleBlock).filter(
        ScheduleBlock.user_id == user.id,
        ScheduleBlock.scheduled_date == tomorrow_str
    ).order_by(ScheduleBlock.start_time.asc()).all()

    def serialize_block(b):
        return {
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
        }

    return {
        "reply": reply_text,
        "message_id": agent_msg.id,
        "action_taken": action,
        "show_schedule_widget": show_schedule_widget,
        "schedule": [serialize_block(b) for b in today_updated],
        "tomorrow_schedule": [serialize_block(b) for b in tomorrow_updated],
        "quick_actions": quick_actions
    }

async def run_auto_plan(db: Session, user: User) -> Dict[str, Any]:
    """
    Agentic Auto-Plan Core Brain:
    1. Backs up existing manual baseline for Today & Tomorrow if not already backed up.
    2. Gathers missed/incomplete tasks from yesterday and today.
    3. Gathers recent conversation context to understand Atharv's current focus, commitments, and habits.
    4. Rebalances schedules for Today & Tomorrow (ensuring no time conflicts, placing missed tasks into optimal slots).
    5. Tags rebalanced tasks with status='AI_SCHEDULED' or 'CARRIED_OVER'.
    6. Returns rebalanced schedules and an executive summary.
    """
    today_str = datetime.date.today().isoformat()
    yesterday_str = (datetime.date.today() - datetime.timedelta(days=1)).isoformat()
    tomorrow_str = (datetime.date.today() + datetime.timedelta(days=1)).isoformat()

    # Step 1: Save baseline snapshot if not already present
    baseline_setting = db.query(ServerSetting).filter(
        ServerSetting.key == f"baseline_schedule_{user.id}"
    ).first()

    current_blocks = db.query(ScheduleBlock).filter(
        ScheduleBlock.user_id == user.id,
        ScheduleBlock.scheduled_date.in_([today_str, tomorrow_str])
    ).all()

    if not baseline_setting:
        snapshot = [
            {
                "title": b.title,
                "block_type": b.block_type,
                "start_time": b.start_time,
                "end_time": b.end_time,
                "duration_minutes": b.duration_minutes,
                "is_completed": b.is_completed,
                "status": b.status,
                "delay_count": b.delay_count,
                "scheduled_date": b.scheduled_date
            }
            for b in current_blocks
        ]
        baseline_setting = ServerSetting(
            key=f"baseline_schedule_{user.id}",
            value=json.dumps(snapshot)
        )
        db.add(baseline_setting)
        db.commit()

    # Step 2: Fetch missed tasks from yesterday
    yesterday_missed = db.query(ScheduleBlock).filter(
        ScheduleBlock.user_id == user.id,
        ScheduleBlock.scheduled_date == yesterday_str,
        ScheduleBlock.is_completed == False,
        ScheduleBlock.block_type != "SLEEP"
    ).all()

    # Step 3: Fetch recent chat messages for priorities & intent
    recent_msgs = db.query(ChatMessage).filter(
        ChatMessage.user_id == user.id
    ).order_by(ChatMessage.created_at.desc()).limit(15).all()
    recent_msgs.reverse()
    chat_summary = "\n".join([f"{m.sender}: {m.content}" for m in recent_msgs])

    today_blocks = [b for b in current_blocks if b.scheduled_date == today_str]
    tomorrow_blocks = [b for b in current_blocks if b.scheduled_date == tomorrow_str]

    # Step 4: AI Rebalance prompt
    ai_prompt = f"""You are the NEXUS Executive Schedule Intelligence Engine.
Atharv has engaged Agentic Auto-Plan. Your job is to analyze his recent messages, missed tasks from yesterday, and today's/tomorrow's schedule to create an optimal, high-performance rebalanced schedule for Today ({today_str}) and Tomorrow ({tomorrow_str}).

CONVERSATION CONTEXT & PRIORITIES:
{chat_summary or 'Standard executive productivity routine.'}

MISSED INCOMPLETE TASKS FROM YESTERDAY ({yesterday_str}):
{chr(10).join([f"- {b.title} ({b.block_type}, {b.duration_minutes}m)" for b in yesterday_missed]) if yesterday_missed else 'None'}

EXISTING TASKS TODAY ({today_str}):
{chr(10).join([f"- [ID {b.id}] {b.start_time}-{b.end_time}: {b.title} ({b.block_type}, completed={b.is_completed})" for b in today_blocks]) if today_blocks else 'None'}

EXISTING TASKS TOMORROW ({tomorrow_str}):
{chr(10).join([f"- [ID {b.id}] {b.start_time}-{b.end_time}: {b.title} ({b.block_type}, completed={b.is_completed})" for b in tomorrow_blocks]) if tomorrow_blocks else 'None'}

RULES:
1. Carry over any missed tasks from yesterday (especially Gym, Work, or critical focus blocks) into Today or Tomorrow if not already completed or present.
2. Rebalance tasks into realistic non-overlapping time slots respecting circadian rhythm:
   - Morning Workout / Gym (07:00-08:00 or morning)
   - Morning Focus / Execution Blocks (09:00-11:30)
   - Lunch / Walk Transition (12:00-13:00)
   - Afternoon Deep Work (13:30-15:30)
   - NSDR Power Nap / Rest (15:30-15:55)
   - SaaS / Late Sprint (16:30-18:30)
   - Evening Wind-down / Personal Downtime (19:30-21:30)
   - Sleep lock ({user.bedtime or '23:30'})
3. Retain completed tasks as completed with their recorded times.
4. Mark newly added, carried over, or rescheduled uncompleted tasks with status "AI_SCHEDULED" or "CARRIED_OVER".
5. Provide a crisp, professional 1-sentence executive summary (no asterisks).

Respond strictly in JSON format:
{{
  "summary": "Crisp executive summary of schedule optimization and rebalancing",
  "today_schedule": [
     {{"title": "Task Title", "block_type": "FOCUS", "start_time": "HH:MM", "end_time": "HH:MM", "duration_minutes": 60, "status": "AI_SCHEDULED", "is_completed": false}}
  ],
  "tomorrow_schedule": [
     {{"title": "Task Title", "block_type": "FOCUS", "start_time": "HH:MM", "end_time": "HH:MM", "duration_minutes": 60, "status": "AI_SCHEDULED", "is_completed": false}}
  ]
}}"""

    llm_res = await execute_chat(db, ai_prompt, "Rebalance schedule now.", json_mode=True)
    summary_text = "Schedule rebalanced and optimized across focus windows."
    plan_applied = False

    if llm_res.get("success"):
        try:
            parsed = json.loads(llm_res.get("content", "{}"))
            llm_today = parsed.get("today_schedule", [])
            llm_tomorrow = parsed.get("tomorrow_schedule", [])
            summary_text = parsed.get("summary", summary_text).replace("**", "")

            if llm_today or llm_tomorrow:
                # Remove uncompleted blocks for today and tomorrow to replace with optimized ones, keeping completed blocks intact!
                db.query(ScheduleBlock).filter(
                    ScheduleBlock.user_id == user.id,
                    ScheduleBlock.scheduled_date.in_([today_str, tomorrow_str]),
                    ScheduleBlock.is_completed == False
                ).delete(synchronize_session=False)

                for item in llm_today:
                    if item.get("is_completed"): continue
                    b = ScheduleBlock(
                        user_id=user.id,
                        title=item.get("title", "Focus Session"),
                        block_type=item.get("block_type", "FOCUS"),
                        start_time=item.get("start_time", "10:00"),
                        end_time=item.get("end_time", "11:00"),
                        duration_minutes=item.get("duration_minutes", 60),
                        scheduled_date=today_str,
                        status=item.get("status", "AI_SCHEDULED"),
                        is_completed=False,
                        delay_count=0
                    )
                    db.add(b)

                for item in llm_tomorrow:
                    if item.get("is_completed"): continue
                    b = ScheduleBlock(
                        user_id=user.id,
                        title=item.get("title", "Focus Session"),
                        block_type=item.get("block_type", "FOCUS"),
                        start_time=item.get("start_time", "10:00"),
                        end_time=item.get("end_time", "11:00"),
                        duration_minutes=item.get("duration_minutes", 60),
                        scheduled_date=tomorrow_str,
                        status=item.get("status", "AI_SCHEDULED"),
                        is_completed=False,
                        delay_count=0
                    )
                    db.add(b)

                db.commit()
                plan_applied = True
        except Exception as e:
            print("Auto-plan LLM parsing error, falling back to algorithmic rebalancer:", e)

    # Fallback algorithmic rebalancer if LLM was unavailable or returned empty
    if not plan_applied:
        # Check missed tasks from yesterday
        carried_count = 0
        for ym in yesterday_missed:
            exists = db.query(ScheduleBlock).filter(
                ScheduleBlock.user_id == user.id,
                ScheduleBlock.scheduled_date == today_str,
                ScheduleBlock.title == ym.title
            ).first()
            if not exists:
                new_block = ScheduleBlock(
                    user_id=user.id,
                    title=ym.title,
                    block_type=ym.block_type,
                    start_time=ym.start_time,
                    end_time=ym.end_time,
                    duration_minutes=ym.duration_minutes,
                    scheduled_date=today_str,
                    status="CARRIED_OVER",
                    is_completed=False,
                    delay_count=0
                )
                db.add(new_block)
                carried_count += 1
        db.commit()
        summary_text = f"Carried over {carried_count} missed task(s) and rebalanced your timeline."

    # Fetch fresh blocks
    today_res = db.query(ScheduleBlock).filter(
        ScheduleBlock.user_id == user.id,
        ScheduleBlock.scheduled_date == today_str
    ).order_by(ScheduleBlock.start_time.asc()).all()

    tomorrow_res = db.query(ScheduleBlock).filter(
        ScheduleBlock.user_id == user.id,
        ScheduleBlock.scheduled_date == tomorrow_str
    ).order_by(ScheduleBlock.start_time.asc()).all()

    def serialize_block(b):
        return {
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
        }

    return {
        "success": True,
        "summary": summary_text,
        "today_schedule": [serialize_block(b) for b in today_res],
        "tomorrow_schedule": [serialize_block(b) for b in tomorrow_res]
    }

def revert_auto_plan(db: Session, user: User) -> Dict[str, Any]:
    """
    Reverts Today and Tomorrow schedule to the baseline snapshot taken before Auto-Plan.
    """
    today_str = datetime.date.today().isoformat()
    tomorrow_str = (datetime.date.today() + datetime.timedelta(days=1)).isoformat()

    baseline_setting = db.query(ServerSetting).filter(
        ServerSetting.key == f"baseline_schedule_{user.id}"
    ).first()

    if baseline_setting:
        try:
            snapshot = json.loads(baseline_setting.value)
            # Delete current uncompleted blocks on today and tomorrow
            db.query(ScheduleBlock).filter(
                ScheduleBlock.user_id == user.id,
                ScheduleBlock.scheduled_date.in_([today_str, tomorrow_str]),
                ScheduleBlock.is_completed == False
            ).delete(synchronize_session=False)

            # Re-insert baseline blocks
            for item in snapshot:
                b = ScheduleBlock(
                    user_id=user.id,
                    title=item["title"],
                    block_type=item.get("block_type", "FOCUS"),
                    start_time=item["start_time"],
                    end_time=item["end_time"],
                    duration_minutes=item.get("duration_minutes", 60),
                    scheduled_date=item["scheduled_date"],
                    status=item.get("status", "UPCOMING"),
                    is_completed=item.get("is_completed", False),
                    delay_count=item.get("delay_count", 0)
                )
                db.add(b)
            db.delete(baseline_setting)
            db.commit()
        except Exception as e:
            print("Error reverting auto-plan baseline:", e)

    today_res = db.query(ScheduleBlock).filter(
        ScheduleBlock.user_id == user.id,
        ScheduleBlock.scheduled_date == today_str
    ).order_by(ScheduleBlock.start_time.asc()).all()

    tomorrow_res = db.query(ScheduleBlock).filter(
        ScheduleBlock.user_id == user.id,
        ScheduleBlock.scheduled_date == tomorrow_str
    ).order_by(ScheduleBlock.start_time.asc()).all()

    def serialize_block(b):
        return {
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
        }

    return {
        "success": True,
        "summary": "Reverted to manual schedule.",
        "today_schedule": [serialize_block(b) for b in today_res],
        "tomorrow_schedule": [serialize_block(b) for b in tomorrow_res]
    }
