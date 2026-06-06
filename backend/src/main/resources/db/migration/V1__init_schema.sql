-- Fawnly initial schema

CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    is_active   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE email_verif (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code        VARCHAR(6)   NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    used        BOOLEAN      NOT NULL DEFAULT FALSE,
    purpose     VARCHAR(30)  NOT NULL DEFAULT 'REGISTRATION'
);

CREATE INDEX idx_email_verif_user_id ON email_verif(user_id);
CREATE INDEX idx_email_verif_code ON email_verif(code);

CREATE TABLE scans (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    project_name VARCHAR(100) NOT NULL,
    source_type  VARCHAR(10)  NOT NULL CHECK (source_type IN ('git', 'zip')),
    source_ref   TEXT         NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'queued'
                 CHECK (status IN ('queued', 'running', 'done', 'failed')),
    started_at   TIMESTAMPTZ,
    finished_at  TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    error_message TEXT
);

CREATE INDEX idx_scans_user_id ON scans(user_id);
CREATE INDEX idx_scans_status ON scans(status);

CREATE TABLE findings (
    id            BIGSERIAL PRIMARY KEY,
    scan_id       BIGINT       NOT NULL REFERENCES scans(id) ON DELETE CASCADE,
    rule_id       VARCHAR(100) NOT NULL,
    severity      VARCHAR(10)  NOT NULL CHECK (severity IN ('HIGH', 'MEDIUM', 'LOW')),
    file_path     TEXT         NOT NULL,
    line_no       INTEGER      NOT NULL,
    owasp_code    VARCHAR(20),
    cwe           VARCHAR(20),
    message       TEXT         NOT NULL,
    triage_status VARCHAR(30)  NOT NULL DEFAULT 'Needs Review'
                  CHECK (triage_status IN ('True Positive', 'False Positive', 'Not Exploitable', 'Needs Review')),
    note          TEXT,
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_findings_scan_id ON findings(scan_id);
CREATE INDEX idx_findings_severity ON findings(severity);
CREATE INDEX idx_findings_triage_status ON findings(triage_status);

CREATE TABLE sessions (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    refresh_token VARCHAR(512) NOT NULL UNIQUE,
    device_info   VARCHAR(500),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at    TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_sessions_user_id ON sessions(user_id);
CREATE INDEX idx_sessions_refresh_token ON sessions(refresh_token);
