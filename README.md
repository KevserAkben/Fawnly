# Fawnly — Static Application Security Testing Platform

Fawnly, Java projelerini Semgrep ile tarayan full-stack bir SAST platformudur.

## Mimari

| Katman     | Teknoloji                                      |
|------------|------------------------------------------------|
| Backend    | Java 17, Spring Boot 3, Spring Security, JWT   |
| Frontend   | React 18, Vite, Tailwind CSS                   |
| Veritabanı | PostgreSQL + Flyway migration                  |
| Analiz     | Semgrep CLI (ProcessBuilder)                   |
| E-posta    | JavaMailSender → Gmail SMTP (OTP)              |

## Proje Yapısı

```
fawnly/
├── backend/                          # Spring Boot API
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/fawnly/
│       │   ├── FawnlyApplication.java
│       │   ├── config/               # Security, CORS, Async, OpenAPI
│       │   ├── controller/           # REST endpoint'ler
│       │   ├── dto/                  # Request/Response modelleri
│       │   ├── entity/               # JPA entity'ler
│       │   ├── exception/            # Global exception handler
│       │   ├── repository/           # Spring Data JPA
│       │   ├── security/             # JWT, rate limiting
│       │   ├── service/              # İş mantığı
│       │   └── util/                 # Sanitize, OTP, validation
│       └── resources/
│           ├── application.yml
│           └── db/migration/
│               ├── V1__init_schema.sql
│               └── V2__audit_log.sql
├── frontend/                         # React SPA
│   ├── package.json
│   ├── vite.config.js
│   ├── tailwind.config.js
│   └── src/
│       ├── api/                      # Axios client & API modülleri
│       ├── components/               # UI bileşenleri
│       ├── pages/                    # Ekranlar
│       └── utils/                    # Auth, validation
├── rules/
│   └── owasp_java.yaml               # Semgrep kuralları (3 adet)
├── samples/
│   └── vulnerable/Main.java          # Test örneği
├── docker-compose.yml                # PostgreSQL
└── README.md
```

## Gereksinimler

- Java 17+
- Maven 3.8+
- Node.js 18+ & npm
- PostgreSQL 14+
- Semgrep CLI (`pip install semgrep` veya [resmi kurulum](https://semgrep.dev/docs/getting-started/))
- Git (GitHub taraması için)
- Gmail hesabı + App Password (OTP e-postası için)

## Ortam Değişkenleri

### Backend

| Değişken            | Açıklama                          | Varsayılan                          |
|---------------------|-----------------------------------|-------------------------------------|
| `DB_URL`            | PostgreSQL JDBC URL               | `jdbc:postgresql://localhost:5432/fawnly` |
| `DB_USERNAME`       | DB kullanıcı adı                  | `fawnly`                            |
| `DB_PASSWORD`       | DB şifresi                        | `changeme`                          |
| `JWT_SECRET`        | JWT imza anahtarı (min 32 karakter) | `change-this-to-a-256-bit-secret...` |
| `GMAIL_USERNAME`    | Gmail adresi                      | —                                   |
| `GMAIL_APP_PASSWORD`| Gmail App Password                | —                                   |
| `CORS_ORIGINS`      | İzin verilen origin'ler           | `http://localhost:5173`             |
| `SEMGREP_PATH`      | Semgrep binary yolu               | `semgrep`                           |
| `SEMGREP_RULES_PATH`| Kural dosyası yolu                | `rules/owasp_java.yaml`             |
| `SCAN_TEMP_DIR`     | Tarama geçici dizini              | `/tmp/sast-scans`                   |
| `SERVER_PORT`       | API portu                         | `8080`                              |

### Frontend

| Değişken        | Açıklama     | Varsayılan              |
|-----------------|--------------|-------------------------|
| `VITE_API_URL`  | Backend URL  | `http://localhost:8080` |

## Kurulum

### 1. PostgreSQL

```bash
docker compose up -d
```

### 2. Backend

```bash
cd backend

export DB_URL=jdbc:postgresql://localhost:5432/fawnly
export DB_USERNAME=fawnly
export DB_PASSWORD=changeme
export JWT_SECRET=your-super-secret-key-at-least-32-chars-long
export GMAIL_USERNAME=your-email@gmail.com
export GMAIL_APP_PASSWORD=your-gmail-app-password
export CORS_ORIGINS=http://localhost:5173
export SEMGREP_RULES_PATH=../rules/owasp_java.yaml

mvn spring-boot:run
```

API: `http://localhost:8080`  
Swagger UI: `http://localhost:8080/swagger-ui.html`  
Health: `http://localhost:8080/actuator/health`

### 3. Frontend

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

Uygulama: `http://localhost:5173`

### 4. Semgrep Kurulumu

```bash
pip install semgrep
semgrep --version
```

## Semgrep Kuralları

`rules/owasp_java.yaml` dosyasında 3 OWASP kuralı tanımlıdır:

| Kural | OWASP | CWE    | Severity | Açıklama                    |
|-------|-------|--------|----------|-----------------------------|
| A02   | A02   | CWE-327| HIGH     | Zayıf hash (MD5, SHA-1)     |
| A03   | A03   | CWE-78 | HIGH     | Command injection (exec)    |
| A07   | A07   | CWE-798| MEDIUM   | Hardcoded secrets           |

Test örneği: `samples/vulnerable/Main.java`

## API Endpoint'leri

| Method | Endpoint                                    | Açıklama                |
|--------|---------------------------------------------|-------------------------|
| POST   | `/api/auth/register`                        | Kayıt                   |
| POST   | `/api/auth/verify-otp`                      | OTP doğrulama           |
| POST   | `/api/auth/resend-otp`                      | OTP yeniden gönder      |
| POST   | `/api/auth/login`                           | Giriş                   |
| POST   | `/api/auth/refresh`                         | Token yenileme          |
| POST   | `/api/auth/logout`                          | Çıkış                   |
| GET    | `/api/dashboard`                            | Dashboard özeti         |
| POST   | `/api/scans/git`                            | Git tarama başlat (202) |
| POST   | `/api/scans/zip`                            | ZIP tarama başlat (202) |
| GET    | `/api/scans/{id}/status`                    | Tarama durumu (polling) |
| GET    | `/api/scans`                                | Tarama geçmişi          |
| DELETE | `/api/scans/{id}`                           | Tarama sil              |
| GET    | `/api/scans/{id}/findings`                  | Bulgular + özet         |
| PATCH  | `/api/scans/{id}/findings/{fid}/triage`     | Triage güncelle         |
| PATCH  | `/api/scans/{id}/findings/{fid}/note`       | Not kaydet              |
| PUT    | `/api/settings/username`                    | Kullanıcı adı değiştir  |
| POST   | `/api/settings/password/request-otp`        | Şifre OTP iste          |
| PUT    | `/api/settings/password`                    | Şifre değiştir          |
| GET    | `/api/settings/sessions`                    | Aktif oturumlar         |
| DELETE | `/api/settings/sessions/{id}`               | Oturum kapat            |

Tüm `/api/*` endpoint'lerinde rate limiting: **30 istek/dk/kullanıcı**.

## Tarama Akışı

1. Kullanıcı GitHub URL veya ZIP yükler → `202 Accepted`
2. Scan kaydı `queued` olarak DB'ye yazılır
3. `@Async` thread pool arka planda:
   - Git clone veya ZIP unzip → `/tmp/sast-scans/{id}/`
   - `semgrep --config rules/owasp_java.yaml --json`
   - Sonuçlar `findings` tablosuna yazılır
   - Dizin temizlenir (GC)
4. Frontend 3 sn'de bir `/api/scans/{id}/status` polling yapar
5. `done` → Results sayfasına yönlendirme

## Tema Renkleri

```css
--toz-pembe : #FFC0CB
--mint      : #98FF98
--pembe-koyu: #f9a8b8
--mint-koyu : #5ecf8a
```

## Güvenlik

- JWT access token (24 saat) + refresh token
- BCrypt şifre hash
- OWASP Encoder ile XSS sanitizasyonu
- CORS whitelist
- Bucket4j rate limiting
- Audit log (hassas işlemler)
- Spring Boot Actuator health endpoint

## Lisans

MIT
