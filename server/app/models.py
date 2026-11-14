import datetime
from sqlalchemy import Column, Integer, String, Boolean, DateTime, ForeignKey, Text
from sqlalchemy.orm import relationship
from app.database import Base

class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    username = Column(String(50), unique=True, index=True, nullable=False)
    email = Column(String(120), unique=True, index=True, nullable=False)
    hashed_password = Column(String(255), nullable=False)
    role = Column(String(20), default="user", nullable=False)  # "admin" or "user"
    is_active = Column(Boolean, default=True, nullable=False)
    created_at = Column(DateTime, default=datetime.datetime.utcnow)

    schedules = relationship("ScheduleBlock", back_populates="user", cascade="all, delete-orphan")
    habits = relationship("Habit", back_populates="user", cascade="all, delete-orphan")
    friction_logs = relationship("FrictionLog", back_populates="user", cascade="all, delete-orphan")

class ScheduleBlock(Base):
    __tablename__ = "schedule_blocks"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    title = Column(String(200), nullable=False)
    block_type = Column(String(50), default="FOCUS")  # FOCUS, WORKOUT, TRANSITION, SAAS, WIND_DOWN
    start_time = Column(String(10), nullable=False)   # "08:00"
    end_time = Column(String(10), nullable=False)     # "09:30"
    duration_minutes = Column(Integer, default=90)
    is_completed = Column(Boolean, default=False)
    status = Column(String(20), default="UPCOMING")    # UPCOMING, IN_PROGRESS, COMPLETED, SKIPPED, DELAYED
    delay_count = Column(Integer, default=0)
    scheduled_date = Column(String(20), nullable=False) # "2026-11-14"
    created_at = Column(DateTime, default=datetime.datetime.utcnow)

    user = relationship("User", back_populates="schedules")

class Habit(Base):
    __tablename__ = "habits"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    title = Column(String(100), nullable=False)
    category = Column(String(50), default="STRENGTH")  # STRENGTH, FOCUS, SLEEP, MINDFULNESS
    target_days = Column(Integer, default=66)          # UCL Phillippa Lally 66-day automaticity model
    current_day = Column(Integer, default=1)
    streak = Column(Integer, default=0)
    last_completed_date = Column(String(20), nullable=True) # "2026-11-14"
    created_at = Column(DateTime, default=datetime.datetime.utcnow)

    user = relationship("User", back_populates="habits")

class FrictionLog(Base):
    __tablename__ = "friction_logs"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    block_id = Column(Integer, nullable=True)
    friction_reason = Column(String(255), nullable=False)
    energy_level = Column(String(20), default="MEDIUM")
    voice_transcript = Column(Text, nullable=True)
    timestamp = Column(DateTime, default=datetime.datetime.utcnow)

    user = relationship("User", back_populates="friction_logs")

class ServerSetting(Base):
    __tablename__ = "server_settings"

    id = Column(Integer, primary_key=True, index=True)
    key = Column(String(100), unique=True, index=True, nullable=False)
    value = Column(Text, nullable=False)
    updated_at = Column(DateTime, default=datetime.datetime.utcnow, onupdate=datetime.datetime.utcnow)
