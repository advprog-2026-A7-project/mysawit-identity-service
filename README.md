# mysawit-identity-service

Spring Boot (Java + Gradle) microservice for MySawit.

## Run (local)
```bash
./gradlew bootRun
```

Runs at: http://localhost:8081

## Health
- GET /actuator/health

## Auth Register/Login
- `POST /api/auth/register`
- `POST /api/auth/login`

### Register Payload
`role` is optional. If omitted, it defaults to `BURUH`.

Example `BURUH`:
```json
{
  "username": "buruh01",
  "email": "buruh01@mail.com",
  "password": "secret123",
  "role": "BURUH",
  "mandorId": "optional-mandor-id"
}
```

Example `MANDOR`:
```json
{
  "username": "mandor01",
  "email": "mandor01@mail.com",
  "password": "secret123",
  "role": "MANDOR",
  "certificationNumber": "CERT-001"
}
```

Example `SUPIR`:
```json
{
  "username": "supir01",
  "email": "supir01@mail.com",
  "password": "secret123",
  "role": "SUPIR",
  "kebunId": "kebun-001"
}
```

Rules:
- `ADMIN` cannot self-register from `/api/auth/register`.
- `certificationNumber` is required when `role=MANDOR`.
- `mandorId` for `BURUH` is optional; if provided it must point to an existing `MANDOR`.

### Login Payload
Primary contract uses `email`:
```json
{
  "email": "user@mail.com",
  "password": "secret123"
}
```

Backward compatibility:
- `username` is temporarily accepted as alias for `email`.

## ADMIN Login-Only via SQL
Use SQL template at `src/main/resources/sql/admin-seed.sql` to provision ADMIN account directly in database.

Notes:
- You must store password as BCrypt hash.
- For JPA JOINED inheritance, insert into both `users` and `admins` tables using the same `id`.

## Error Response Format
All errors are standardized:
```json
{
  "timestamp": "2026-03-05T15:17:30.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Certification number is required for MANDOR",
  "path": "/api/auth/register"
}
```
