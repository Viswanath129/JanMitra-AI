import os

class Settings:
    # App Settings
    APP_NAME: str = "JanMitra AI FastAPI Backend"
    DEBUG: bool = os.getenv("DEBUG", "False").lower() in ("true", "1", "t")
    PORT: int = int(os.getenv("PORT", "8000"))
    
    # Security Configurations
    JWT_SECRET_KEY: str = os.getenv("JWT_SECRET_KEY", "b99be4f16954a7f6cdcf4e69b59695669b6dbfae9d3b1458")
    JWT_ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 60
    REFRESH_TOKEN_EXPIRE_DAYS: int = 30
    
    # Database Configurations
    DATABASE_URL: str = os.getenv(
        "DATABASE_URL", 
        "postgresql://postgres:postgres@db:5432/janmitra"
    )
    
    # Gemini AI Configuration
    GEMINI_API_KEY: str = os.getenv("GEMINI_API_KEY", "MY_GEMINI_API_KEY")
    
    # Firebase Setup
    FIREBASE_PROJECT_ID: str = os.getenv("FIREBASE_PROJECT_ID", "janmitra-ai")
    FIREBASE_STORAGE_BUCKET: str = os.getenv("FIREBASE_STORAGE_BUCKET", "janmitra-ai.appspot.com")
    
    # CORS Configuration
    CORS_ORIGINS: list = ["*"]

settings = Settings()
