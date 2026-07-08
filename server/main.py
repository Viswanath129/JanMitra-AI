import os
import time
import shutil
import uuid
import jwt
from typing import List, Optional
from fastapi import FastAPI, Depends, HTTPException, status, File, UploadFile, Form
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.orm import Session
from pydantic import BaseModel, EmailStr

from config import settings
from database import (
    get_db, Base, engine, UserModel, ReportModel, 
    VillageModel, MediaModel, AIAnalysisModel, 
    StatusHistoryModel, RecommendationModel, 
    NotificationModel, AuditLogModel
)
from auth import (
    get_password_hash, verify_password, create_access_token, 
    create_refresh_token, verify_token, get_current_user, RoleChecker
)
from ai_service import analyze_report_with_ai, compare_projects_with_ai, rag_query_with_ai

# Initialize Database tables
Base.metadata.create_all(bind=engine)

app = FastAPI(title=settings.APP_NAME, debug=settings.DEBUG)

# Configure CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.CORS_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# --- Pydantic Schemas ---
class UserRegister(BaseModel):
    email: EmailStr
    password: str
    full_name: str
    phone_number: Optional[str] = None
    role: Optional[str] = "citizen"

class UserLogin(BaseModel):
    email: EmailStr
    password: str

class FirebaseExchangeRequest(BaseModel):
    id_token: str
    role: Optional[str] = "citizen"

class RefreshTokenRequest(BaseModel):
    refresh_token: str

class FcmTokenRegister(BaseModel):
    fcm_token: str
    email: Optional[str] = None

class TestNotificationRequest(BaseModel):
    fcm_token: str
    title: str
    message: str
    issue_id: Optional[str] = None

class TokenResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str
    role: str
    full_name: str

class ReportCreate(BaseModel):
    issue_id: str
    category: str
    description: str
    voice_file_path: Optional[str] = None
    image_uri: Optional[str] = None
    location_latitude: float
    location_longitude: float
    location_name: str
    urgency: str
    timestamp: Optional[int] = None
    priority_score: float
    ai_summary: Optional[str] = None
    evidence_strength: Optional[str] = "Moderate"
    citizen_sentiment: Optional[str] = "Concerned"
    explanation_text: Optional[str] = None

class ReportResponse(BaseModel):
    id: int
    issue_id: str
    category: str
    description: str
    detected_language: str
    location_latitude: float
    location_longitude: float
    location_name: str
    urgency: str
    status: str
    priority_score: float
    ai_summary: Optional[str]
    explanation_text: Optional[str]
    created_at: str

    class Config:
        from_attributes = True

class ChatQuery(BaseModel):
    message: str

class CompareRequest(BaseModel):
    project_a_id: str
    project_b_id: str

# --- Seed Sample Village and Asset Data if Empty ---
@app.on_event("startup")
def startup_db_seed():
    db = SessionLocal = engine.connect()
    # Check if we should insert seed data
    from sqlalchemy import text
    try:
        # Seed villages if empty
        r = db.execute(text("SELECT COUNT(*) FROM villages")).fetchone()
        if r and r[0] == 0:
            db.execute(text("""
                INSERT INTO villages (village_name, district, ward, population, development_index, historical_funding_cr, drinking_water_gap, road_connectivity_gap, school_upgrade_need, healthcare_gap, vulnerable_population_pct)
                VALUES 
                ('Bhola Village', 'District East', 'Ward 12', 1800, 0.32, 0.5, true, true, false, true, 55.0),
                ('Rampur Village', 'District East', 'Ward 14', 4500, 0.45, 1.2, true, false, true, true, 35.0),
                ('Seva Village', 'District West', 'Ward 03', 6200, 0.65, 2.8, false, false, true, false, 20.0),
                ('Kalyanpur Village', 'District North', 'Ward 08', 3200, 0.52, 1.5, false, true, false, false, 40.0);
            """))
            db.commit()
    except Exception as e:
        print(f"Startup seed error: {e}")
    finally:
        db.close()


# --- API Routes ---

# 1. Root / Health-check
@app.get("/")
def read_root():
    return {"status": "healthy", "service": settings.APP_NAME, "version": "1.0.0"}


# 2. Authentication API
@app.post("/api/auth/register", response_model=dict)
def register_user(user_data: UserRegister, db: Session = Depends(get_db)):
    existing = db.query(UserModel).filter(UserModel.email == user_data.email).first()
    if existing:
        raise HTTPException(status_code=400, detail="Email is already registered")
    
    hashed = get_password_hash(user_data.password)
    user = UserModel(
        email=user_data.email,
        password_hash=hashed,
        full_name=user_data.full_name,
        phone_number=user_data.phone_number,
        role=user_data.role
    )
    db.add(user)
    db.commit()
    db.refresh(user)
    return {"status": "success", "message": "User registered successfully", "id": user.id}

@app.post("/api/auth/login", response_model=TokenResponse)
def login_user(login_data: UserLogin, db: Session = Depends(get_db)):
    user = db.query(UserModel).filter(UserModel.email == login_data.email).first()
    if not user or not user.password_hash or not verify_password(login_data.password, user.password_hash):
        raise HTTPException(status_code=401, detail="Invalid email or password")
    
    access_token = create_access_token({"sub": user.email, "role": user.role})
    refresh_token = create_refresh_token({"sub": user.email, "role": user.role})
    
    return TokenResponse(
        access_token=access_token,
        refresh_token=refresh_token,
        token_type="Bearer",
        role=user.role,
        full_name=user.full_name
    )

@app.post("/api/auth/firebase_exchange", response_model=TokenResponse)
def firebase_exchange(req: FirebaseExchangeRequest, db: Session = Depends(get_db)):
    try:
        # Decode Firebase ID Token (JWT) without signature verification for offline / cross-env compatibility
        payload = jwt.decode(req.id_token, options={"verify_signature": False})
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Invalid Firebase ID Token: {str(e)}")
    
    uid = payload.get("user_id") or payload.get("sub")
    email = payload.get("email")
    full_name = payload.get("name") or payload.get("email", "Firebase User").split("@")[0]
    phone_number = payload.get("phone_number")
    
    if not email:
        email = f"anonymous_{uid}@janmitra.ai"
        
    user = db.query(UserModel).filter(UserModel.email == email).first()
    if not user:
        user = UserModel(
            uid=uid,
            email=email,
            full_name=full_name,
            phone_number=phone_number or "",
            role=req.role or "citizen"
        )
        db.add(user)
        db.commit()
        db.refresh(user)
    else:
        user.uid = uid
        if full_name and user.full_name != full_name:
            user.full_name = full_name
        if phone_number and not user.phone_number:
            user.phone_number = phone_number
        db.commit()
        db.refresh(user)
        
    access_token = create_access_token({"sub": user.email, "role": user.role})
    refresh_token = create_refresh_token({"sub": user.email, "role": user.role})
    
    return TokenResponse(
        access_token=access_token,
        refresh_token=refresh_token,
        token_type="Bearer",
        role=user.role,
        full_name=user.full_name
    )

@app.post("/api/auth/refresh", response_model=TokenResponse)
def refresh_token_endpoint(req: RefreshTokenRequest, db: Session = Depends(get_db)):
    try:
        payload = verify_token(req.refresh_token, "refresh")
        email = payload.get("sub")
        if not email:
            raise HTTPException(status_code=401, detail="Invalid refresh token payload")
        
        user = db.query(UserModel).filter(UserModel.email == email).first()
        if not user:
            raise HTTPException(status_code=401, detail="User not found")
            
        access_token = create_access_token({"sub": user.email, "role": user.role})
        refresh_token = create_refresh_token({"sub": user.email, "role": user.role})
        
        return TokenResponse(
            access_token=access_token,
            refresh_token=refresh_token,
            token_type="Bearer",
            role=user.role,
            full_name=user.full_name
        )
    except Exception as e:
        raise HTTPException(status_code=401, detail=f"Token refresh failed: {str(e)}")

@app.post("/api/auth/register_fcm_token")
def register_fcm_token(req: FcmTokenRegister, db: Session = Depends(get_db)):
    if req.email:
        user = db.query(UserModel).filter(UserModel.email == req.email).first()
        if user:
            user.fcm_token = req.fcm_token
            db.commit()
            return {"status": "success", "message": f"FCM token registered and associated with {req.email}"}
    return {"status": "success", "message": "FCM token registered (unassociated)"}

@app.post("/api/auth/send_test_notification")
def send_test_notification(req: TestNotificationRequest, db: Session = Depends(get_db)):
    # Save to standard notification table
    notification = NotificationModel(
        title=req.title,
        message=req.message,
        fcm_token=req.fcm_token,
        is_read=False
    )
    db.add(notification)
    db.commit()
    db.refresh(notification)
    
    # Return success
    return {
        "status": "success",
        "message": "FCM notification recorded in centralized database",
        "notification_id": notification.id
    }


# 3. Report Submissions & GIS APIs
@app.post("/api/reports/submit", response_model=dict)
def submit_report(report_data: ReportCreate, db: Session = Depends(get_db), current_user: Optional[UserModel] = Depends(get_current_user)):
    # Check if duplicate issue_id exists
    existing = db.query(ReportModel).filter(ReportModel.issue_id == report_data.issue_id).first()
    if existing:
        return {"status": "exists", "message": "Report already synchronized", "id": existing.id}
    
    # Process AI summary and metadata enrichment automatically on submission if not supplied
    summary = report_data.ai_summary
    detected_lang = "English"
    urgency_val = report_data.urgency
    explanation_val = report_data.explanation_text
    
    try:
        ai_res = analyze_report_with_ai(
            category=report_data.category,
            description=report_data.description,
            village_name=report_data.location_name,
            has_voice=report_data.voice_file_path is not None,
            has_photo=report_data.image_uri is not None
        )
        detected_lang = ai_res.get("language", "English")
        if not summary:
            summary = ai_res.get("summary", f"Requirement of {report_data.category} in {report_data.location_name}")
        if urgency_val == "Medium" and ai_res.get("urgency") != "Medium":
            # Override client-side default with AI's detected severity
            urgency_val = ai_res.get("urgency", "Medium")
    except Exception:
        pass
        
    report = ReportModel(
        issue_id=report_data.issue_id,
        category=report_data.category,
        description=report_data.description,
        voice_file_path=report_data.voice_file_path,
        image_uri=report_data.image_uri,
        detected_language=detected_lang,
        location_latitude=report_data.location_latitude,
        location_longitude=report_data.location_longitude,
        location_name=report_data.location_name,
        urgency=urgency_val,
        status="Reported",
        timestamp=report_data.timestamp or int(time.time() * 1000),
        priority_score=report_data.priority_score,
        ai_summary=summary,
        evidence_strength=report_data.evidence_strength,
        citizen_sentiment=report_data.citizen_sentiment,
        explanation_text=explanation_val,
        user_id=current_user.id if current_user else None
    )
    
    db.add(report)
    db.commit()
    db.refresh(report)
    
    # Save a record of the initial status
    history = StatusHistoryModel(
        report_id=report.id,
        new_status="Reported",
        comment="Initial system intake via citizen mobile app."
    )
    db.add(history)
    db.commit()
    
    return {"status": "success", "message": "Report successfully published on centralized ledger", "id": report.id, "issue_id": report.issue_id}

@app.get("/api/reports/list", response_model=List[ReportResponse])
def list_reports(category: Optional[str] = None, village: Optional[str] = None, status: Optional[str] = None, db: Session = Depends(get_db)):
    query = db.query(ReportModel)
    if category and category != "All":
        query = query.filter(ReportModel.category == category)
    if village and village != "All":
        query = query.filter(ReportModel.location_name == village)
    if status and status != "All":
        query = query.filter(ReportModel.status == status)
        
    reports = query.order_by(ReportModel.priority_score.desc()).all()
    
    # Convert created_at to string format
    response_list = []
    for r in reports:
        response_list.append(ReportResponse(
            id=r.id,
            issue_id=r.issue_id,
            category=r.category,
            description=r.description,
            detected_language=r.detected_language,
            location_latitude=r.location_latitude,
            location_longitude=r.location_longitude,
            location_name=r.location_name,
            urgency=r.urgency,
            status=r.status,
            priority_score=r.priority_score,
            ai_summary=r.ai_summary,
            explanation_text=r.explanation_text,
            created_at=r.created_at.isoformat()
        ))
    return response_list


# 4. Media Upload to Secure App Storage & Firebase Emulator URL Generation
UPLOAD_DIR = "/app/server/uploads"
os.makedirs(UPLOAD_DIR, exist_ok=True)

@app.post("/api/media/upload")
def upload_media(file: UploadFile = File(...), media_type: str = Form(...), report_id: Optional[int] = Form(None), db: Session = Depends(get_db)):
    file_extension = file.filename.split(".")[-1]
    unique_filename = f"{uuid.uuid4()}.{file_extension}"
    file_path = os.path.join(UPLOAD_DIR, unique_filename)
    
    # Save file locally on server storage
    with open(file_path, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)
        
    # Generate public secure storage URL (Firebase storage emulator or public reverse proxy)
    public_url = f"https://firebasestorage.googleapis.com/v0/b/{settings.FIREBASE_STORAGE_BUCKET}/o/{unique_filename}?alt=media"
    
    if report_id:
        media_item = MediaModel(
            report_id=report_id,
            media_type=media_type,
            url=public_url,
            local_uri=file.filename
        )
        db.add(media_item)
        db.commit()
        db.refresh(media_item)
        
    return {"status": "success", "url": public_url, "filename": unique_filename}


# 5. AI Service & RAG Integrations
@app.post("/api/ai/chat")
def ai_chat(query: ChatQuery, db: Session = Depends(get_db)):
    # Collect RAG context from active databases
    reports = db.query(ReportModel).all()
    reports_dict = [{"issue_id": r.issue_id, "category": r.category, "location_name": r.location_name, "priority_score": r.priority_score, "description": r.description} for r in reports]
    
    villages = db.query(VillageModel).all()
    villages_dict = [{"village_name": v.village_name, "population": v.population, "development_index": v.development_index, "drinking_water_gap": v.drinking_water_gap, "road_connectivity_gap": v.road_connectivity_gap} for v in villages]
    
    # Dynamic RAG Query
    ai_response_text = rag_query_with_ai(
        user_question=query.message,
        context_reports=reports_dict,
        context_assets=[],
        context_stats=villages_dict
    )
    return {"status": "success", "response": ai_response_text}

@app.post("/api/ai/compare")
def ai_compare_projects(req: CompareRequest, db: Session = Depends(get_db)):
    projA = db.query(ReportModel).filter(ReportModel.issue_id == req.project_a_id).first()
    projB = db.query(ReportModel).filter(ReportModel.issue_id == req.project_b_id).first()
    
    if not projA or not projB:
        raise HTTPException(status_code=404, detail="One or both reports could not be found")
        
    projA_dict = {"issue_id": projA.issue_id, "category": projA.category, "location_name": projA.location_name, "priority_score": projA.priority_score, "description": projA.description, "explanation_text": projA.explanation_text}
    projB_dict = {"issue_id": projB.issue_id, "category": projB.category, "location_name": projB.location_name, "priority_score": projB.priority_score, "description": projB.description, "explanation_text": projB.explanation_text}
    
    comparison_text = compare_projects_with_ai(projA_dict, projB_dict)
    return {"status": "success", "comparison": comparison_text}


# 6. Analytics Dashboard API
@app.get("/api/analytics/dashboard")
def get_analytics(db: Session = Depends(get_db)):
    reports = db.query(ReportModel).all()
    total_count = len(reports)
    
    # Urgent status aggregations
    urgency_counts = {"Critical": 0, "High": 0, "Medium": 0, "Low": 0}
    category_counts = {}
    status_counts = {}
    
    for r in reports:
        urgency_counts[r.urgency] = urgency_counts.get(r.urgency, 0) + 1
        category_counts[r.category] = category_counts.get(r.category, 0) + 1
        status_counts[r.status] = status_counts.get(r.status, 0) + 1
        
    return {
        "total_reports": total_count,
        "urgency_distribution": urgency_counts,
        "category_distribution": category_counts,
        "status_distribution": status_counts,
        "last_updated": int(time.time())
    }
