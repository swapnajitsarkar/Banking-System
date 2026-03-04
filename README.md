# 🏦 Digital Banking System

A production-grade REST API built with **Spring Boot 3**, **Spring Security (JWT + RBAC)**, **Spring Data JPA**, and **Docker**.

---

## Architecture

```
Controller → Service → Repository → Database
     ↑           ↑
  JWT Filter   @Transactional
  (Security)   (ACID)
```

| Layer | Responsibility |
|-------|---------------|
| `controller/` | HTTP routing, input validation, response shaping |
| `service/` | Business logic, transaction management |
| `repository/` | JPA data access, pessimistic locking |
| `security/` | JWT generation/validation, UserDetailsService |
| `exception/` | GlobalExceptionHandler (@ControllerAdvice) |

---

## Quick Start

### Prerequisites
- Docker & Docker Compose
- JDK 17+ (for local dev)

### Run with Docker (Recommended)
```bash
# Clone and start
git clone <repo>
cd digital-banking-system

# Copy and configure env
cp .env.example .env  # Set JWT_SECRET, DB passwords

# Build and launch both containers
docker compose up --build

# API is live at: http://localhost:8080
```

### Run Locally
```bash
# Start MySQL only
docker compose up db

# Run the app
mvn spring-boot:run
```

---

## API Reference

### Authentication
| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/api/auth/register` | Register new user | Public |
| POST | `/api/auth/login` | Get JWT token | Public |

### Accounts
| Method | Endpoint | Description | Role |
|--------|----------|-------------|------|
| POST | `/api/accounts` | Create account | USER |
| GET | `/api/accounts` | List my accounts | USER |
| GET | `/api/accounts/{number}` | Get account details | USER (owner) |
| DELETE | `/api/accounts/{number}` | Deactivate account | ADMIN |

### Transactions
| Method | Endpoint | Description | Role |
|--------|----------|-------------|------|
| POST | `/api/transactions/deposit` | Deposit funds | USER |
| POST | `/api/transactions/withdraw` | Withdraw funds | USER |
| POST | `/api/transactions/transfer` | Transfer between accounts | USER (owner) |
| GET | `/api/transactions/history/{number}` | Account statement | USER (owner) |
| GET | `/api/transactions/admin/all` | All transactions | ADMIN |

---

## Example Requests

### Register
```json
POST /api/auth/register
{
  "fullName": "Alice Smith",
  "email": "alice@example.com",
  "password": "SecurePass123"
}
```

### Login → returns JWT
```json
POST /api/auth/login
{
  "email": "alice@example.com",
  "password": "SecurePass123"
}
```

### Transfer (requires Authorization header)
```
Authorization: Bearer <jwt_token>
```
```json
POST /api/transactions/transfer
{
  "sourceAccountNumber": "ACC482910374821",
  "destinationAccountNumber": "ACC123456789012",
  "amount": 250.00,
  "description": "Rent payment"
}
```

---

## Error Responses

All errors return structured JSON:
```json
{
  "success": false,
  "message": "Insufficient funds in account: ACC482910374821",
  "timestamp": "2024-01-15T10:30:00"
}
```

| Status | Meaning |
|--------|---------|
| 400 | Validation error / bad request |
| 401 | Not authenticated |
| 402 | Insufficient funds |
| 403 | Forbidden (wrong role or not account owner) |
| 404 | Account / user not found |
| 409 | Duplicate email |
| 422 | Account inactive |

---

## Key Design Decisions

### ACID Transactions
- `@Transactional(isolation = REPEATABLE_READ)` on all fund movements
- Pessimistic write locks (`SELECT ... FOR UPDATE`) prevent concurrent balance races
- `BigDecimal` used for all monetary values (never `double`)

### JWT Authentication
- Stateless — no server-side session storage
- Token signed with HS512
- 24-hour expiry (configurable via `app.jwt.expiration-ms`)

### RBAC
- `ROLE_USER`: own accounts + own transactions only
- `ROLE_ADMIN`: read all transactions, deactivate accounts
- Double enforcement: SecurityConfig URL rules + `@PreAuthorize` on services

### Docker
- Multi-stage build (Maven builder → Alpine JRE runtime image)
- App waits for DB `healthcheck` before starting
- App connects to `db:3306` (Docker internal DNS), not `localhost`
- Non-root user inside container

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_USERNAME` | `bankinguser` | MySQL username |
| `DB_PASSWORD` | `bankingpass` | MySQL password |
| `JWT_SECRET` | (default) | **Change in production!** |
| `MYSQL_ROOT_PASSWORD` | `rootpassword` | MySQL root password |
