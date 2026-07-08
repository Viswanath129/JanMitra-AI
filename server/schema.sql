-- Production PostgreSQL with PostGIS & Vector Search Schema for JanMitra AI

-- Enable necessary extensions
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Users table
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    uid VARCHAR(128) UNIQUE, -- Firebase UID or internal ID
    email VARCHAR(255) UNIQUE,
    password_hash VARCHAR(255),
    role VARCHAR(50) NOT NULL DEFAULT 'citizen', -- 'citizen', 'officer', 'admin'
    full_name VARCHAR(255),
    phone_number VARCHAR(50),
    anonymous_id VARCHAR(128),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Village Statistics (including Geospatial representation)
CREATE TABLE IF NOT EXISTS villages (
    id SERIAL PRIMARY KEY,
    village_name VARCHAR(100) UNIQUE NOT NULL,
    district VARCHAR(100) NOT NULL,
    ward VARCHAR(100) NOT NULL,
    population INT NOT NULL,
    development_index DOUBLE PRECISION NOT NULL,
    historical_funding_cr DOUBLE PRECISION NOT NULL,
    drinking_water_gap BOOLEAN DEFAULT FALSE,
    road_connectivity_gap BOOLEAN DEFAULT FALSE,
    school_upgrade_need BOOLEAN DEFAULT FALSE,
    healthcare_gap BOOLEAN DEFAULT FALSE,
    vulnerable_population_pct DOUBLE PRECISION NOT NULL,
    geom GEOMETRY(Point, 4326), -- PostGIS spatial point
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. Citizen Reports table
CREATE TABLE IF NOT EXISTS reports (
    id SERIAL PRIMARY KEY,
    issue_id VARCHAR(50) UNIQUE NOT NULL,
    category VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    voice_file_path VARCHAR(512),
    image_uri VARCHAR(512),
    detected_language VARCHAR(50) DEFAULT 'English',
    location_latitude DOUBLE PRECISION NOT NULL,
    location_longitude DOUBLE PRECISION NOT NULL,
    location_name VARCHAR(255) NOT NULL,
    urgency VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'Reported',
    timestamp BIGINT NOT NULL,
    priority_score DOUBLE PRECISION NOT NULL,
    ai_summary TEXT,
    evidence_strength VARCHAR(50),
    citizen_sentiment VARCHAR(100),
    citizen_demand_score DOUBLE PRECISION,
    infra_gap_score DOUBLE PRECISION,
    population_impact_score DOUBLE PRECISION,
    distance_to_service_score DOUBLE PRECISION,
    safety_risk_score DOUBLE PRECISION,
    edu_health_score DOUBLE PRECISION,
    budget_feasibility_score DOUBLE PRECISION,
    historical_neglect_score DOUBLE PRECISION,
    explanation_text TEXT,
    user_id INT REFERENCES users(id) ON DELETE SET NULL,
    geom GEOMETRY(Point, 4326), -- Spatial report location
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 4. Media attachments table (for GCS/Firebase URLs)
CREATE TABLE IF NOT EXISTS media (
    id SERIAL PRIMARY KEY,
    report_id INT REFERENCES reports(id) ON DELETE CASCADE,
    media_type VARCHAR(50) NOT NULL, -- 'photo', 'video', 'audio'
    url VARCHAR(512) NOT NULL,
    thumbnail_url VARCHAR(512),
    file_size BIGINT,
    duration INT, -- in seconds for audio/video
    local_uri VARCHAR(512), -- Local cache reference
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 5. AI Analysis Table
CREATE TABLE IF NOT EXISTS ai_analysis (
    id SERIAL PRIMARY KEY,
    report_id INT UNIQUE REFERENCES reports(id) ON DELETE CASCADE,
    language VARCHAR(100),
    urgency VARCHAR(50),
    summary TEXT,
    duplicate_alert TEXT,
    raw_payload JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 6. Status change audit trail
CREATE TABLE IF NOT EXISTS status_history (
    id SERIAL PRIMARY KEY,
    report_id INT REFERENCES reports(id) ON DELETE CASCADE,
    old_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    changed_by INT REFERENCES users(id) ON DELETE SET NULL,
    comment TEXT,
    changed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 7. MP recommendations table
CREATE TABLE IF NOT EXISTS recommendations (
    id SERIAL PRIMARY KEY,
    village_name VARCHAR(255),
    department VARCHAR(100) NOT NULL,
    recommended_work VARCHAR(255) NOT NULL,
    estimated_cost DOUBLE PRECISION,
    rationale TEXT,
    priority_rank INT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 8. Notifications table
CREATE TABLE IF NOT EXISTS notifications (
    id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    fcm_token VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 9. Audit Logs (Compliance & Security)
CREATE TABLE IF NOT EXISTS audit_logs (
    id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(255) NOT NULL,
    ip_address VARCHAR(100),
    details TEXT,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Spatial Spatial index for performance GIS queries
CREATE INDEX IF NOT EXISTS idx_reports_geom ON reports USING gist(geom);
CREATE INDEX IF NOT EXISTS idx_villages_geom ON villages USING gist(geom);
