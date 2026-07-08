from sqlalchemy import create_engine, Column, Integer, String, Text, Boolean, Float, ForeignKey, DateTime, BigInt, JSON
from sqlalchemy.orm import declarative_base, sessionmaker, relationship
from sqlalchemy.sql import func
import datetime
from config import settings

Base = declarative_base()

# 1. User Model
class UserModel(Base):
    __tablename__ = "users"
    
    id = Column(Integer, primary_key=True, index=True)
    uid = Column(String(128), unique=True, index=True, nullable=True) # Firebase or SSO uid
    email = Column(String(255), unique=True, index=True)
    password_hash = Column(String(255), nullable=True)
    role = Column(String(50), default="citizen") # citizen, officer, admin
    full_name = Column(String(255))
    phone_number = Column(String(50))
    anonymous_id = Column(String(128))
    fcm_token = Column(String(255), nullable=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    
    reports = relationship("ReportModel", back_populates="user")
    notifications = relationship("NotificationModel", back_populates="user")

# 2. Village Model
class VillageModel(Base):
    __tablename__ = "villages"
    
    id = Column(Integer, primary_key=True, index=True)
    village_name = Column(String(100), unique=True, index=True, nullable=False)
    district = Column(String(100), nullable=False)
    ward = Column(String(100), nullable=False)
    population = Column(Integer, nullable=False)
    development_index = Column(Float, nullable=False)
    historical_funding_cr = Column(Float, nullable=False)
    drinking_water_gap = Column(Boolean, default=False)
    road_connectivity_gap = Column(Boolean, default=False)
    school_upgrade_need = Column(Boolean, default=False)
    healthcare_gap = Column(Boolean, default=False)
    vulnerable_population_pct = Column(Float, nullable=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now())

# 3. Report Model
class ReportModel(Base):
    __tablename__ = "reports"
    
    id = Column(Integer, primary_key=True, index=True)
    issue_id = Column(String(50), unique=True, index=True, nullable=False)
    category = Column(String(100), nullable=False)
    description = Column(Text, nullable=False)
    voice_file_path = Column(String(512))
    image_uri = Column(String(512))
    detected_language = Column(String(50), default="English")
    location_latitude = Column(Float, nullable=False)
    location_longitude = Column(Float, nullable=False)
    location_name = Column(String(255), nullable=False)
    urgency = Column(String(50), nullable=False)
    status = Column(String(50), default="Reported")
    timestamp = Column(BigInt, nullable=False)
    priority_score = Column(Float, nullable=False)
    ai_summary = Column(Text)
    evidence_strength = Column(String(50))
    citizen_sentiment = Column(String(100))
    citizen_demand_score = Column(Float)
    infra_gap_score = Column(Float)
    population_impact_score = Column(Float)
    distance_to_service_score = Column(Float)
    safety_risk_score = Column(Float)
    edu_health_score = Column(Float)
    budget_feasibility_score = Column(Float)
    historical_neglect_score = Column(Float)
    explanation_text = Column(Text)
    user_id = Column(Integer, ForeignKey("users.id", ondelete="SET NULL"))
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    
    user = relationship("UserModel", back_populates="reports")
    media_attachments = relationship("MediaModel", back_populates="report", cascade="all, delete-orphan")
    ai_analysis = relationship("AIAnalysisModel", back_populates="report", uselist=False, cascade="all, delete-orphan")
    status_history = relationship("StatusHistoryModel", back_populates="report", cascade="all, delete-orphan")

# 4. Media Model
class MediaModel(Base):
    __tablename__ = "media"
    
    id = Column(Integer, primary_key=True, index=True)
    report_id = Column(Integer, ForeignKey("reports.id", ondelete="CASCADE"))
    media_type = Column(String(50), nullable=False) # photo, video, audio
    url = Column(String(512), nullable=False)
    thumbnail_url = Column(String(512))
    file_size = Column(BigInt)
    duration = Column(Integer)
    local_uri = Column(String(512))
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    
    report = relationship("ReportModel", back_populates="media_attachments")

# 5. AI Analysis Model
class AIAnalysisModel(Base):
    __tablename__ = "ai_analysis"
    
    id = Column(Integer, primary_key=True, index=True)
    report_id = Column(Integer, ForeignKey("reports.id", ondelete="CASCADE"), unique=True)
    language = Column(String(100))
    urgency = Column(String(50))
    summary = Column(Text)
    duplicate_alert = Column(Text)
    raw_payload = Column(JSON)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    
    report = relationship("ReportModel", back_populates="ai_analysis")

# 6. Status History Model
class StatusHistoryModel(Base):
    __tablename__ = "status_history"
    
    id = Column(Integer, primary_key=True, index=True)
    report_id = Column(Integer, ForeignKey("reports.id", ondelete="CASCADE"))
    old_status = Column(String(50))
    new_status = Column(String(50), nullable=False)
    changed_by = Column(Integer, ForeignKey("users.id", ondelete="SET NULL"))
    comment = Column(Text)
    changed_at = Column(DateTime(timezone=True), server_default=func.now())
    
    report = relationship("ReportModel", back_populates="status_history")

# 7. Recommendations Model
class RecommendationModel(Base):
    __tablename__ = "recommendations"
    
    id = Column(Integer, primary_key=True, index=True)
    village_name = Column(String(255))
    department = Column(String(100), nullable=False)
    recommended_work = Column(String(255), nullable=False)
    estimated_cost = Column(Float)
    rationale = Column(Text)
    priority_rank = Column(Integer)
    created_at = Column(DateTime(timezone=True), server_default=func.now())

# 8. Notification Model
class NotificationModel(Base):
    __tablename__ = "notifications"
    
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"))
    title = Column(String(255), nullable=False)
    message = Column(Text, nullable=False)
    is_read = Column(Boolean, default=False)
    fcm_token = Column(String(255))
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    
    user = relationship("UserModel", back_populates="notifications")

# 9. Audit Log Model
class AuditLogModel(Base):
    __tablename__ = "audit_logs"
    
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id", ondelete="SET NULL"))
    action = Column(String(255), nullable=False)
    ip_address = Column(String(100))
    details = Column(Text)
    timestamp = Column(DateTime(timezone=True), server_default=func.now())

# Create engine and SessionLocal
engine = create_engine(settings.DATABASE_URL)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
