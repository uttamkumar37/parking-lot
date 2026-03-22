# 🚗 ParkSmart — Production-Ready Parking Lot Management System

[![Java](https://img.shields.io/badge/Java-21_LTS-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue.svg)](https://reactjs.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-blue.svg)](https://docs.docker.com/compose/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

---

## 📌 Resume-Ready Description

> **ParkSmart** is a cloud-native, microservice-ready Parking Lot Management System built with a **Java 21 (LTS) + Spring Boot 3** backend and **React 18 + Tailwind CSS** frontend. It supports multi-floor parking lots, dynamic pricing strategies, JWT-based authentication, real-time slot availability, Stripe payment integration, email/SMS notifications, Redis caching, Kafka async event processing, and full CI/CD deployment on AWS via GitHub Actions. Designed using SOLID principles, Gang-of-Four design patterns (Strategy, Factory, Observer, Builder, Singleton), and tested with JUnit 5 + Mockito.

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          CLIENT TIER                                     │
│  React 18 + Redux Toolkit + Tailwind CSS + Axios                        │
│  Pages: Login | Dashboard | Park Vehicle | Bookings | Payment            │
└─────────────────────────┬───────────────────────────────────────────────┘
                          │ HTTPS (Nginx Reverse Proxy)
┌─────────────────────────▼───────────────────────────────────────────────┐
│                       API GATEWAY / NGINX                                │
│  Rate Limiting | SSL Termination | Load Balancing                        │
└─────────────────────────┬───────────────────────────────────────────────┘
                          │ REST / JSON
┌─────────────────────────▼───────────────────────────────────────────────┐
│                    SPRING BOOT 3 APPLICATION                             │
│  ┌───────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐              │
│  │Controllers│  │ Services │  │   Repos  │  │ Security │              │
│  └───────────┘  └──────────┘  └──────────┘  └──────────┘              │
│  Design Patterns: Strategy | Factory | Observer | Builder | Singleton   │
└──────┬──────────────────┬──────────────────┬────────────────────────────┘
       │                  │                  │
┌──────▼──────┐  ┌────────▼────┐  ┌─────────▼──────┐
│ PostgreSQL  │  │    Redis    │  │     Kafka       │
│ (Primary DB)│  │  (Cache)    │  │ (Event Queue)   │
└─────────────┘  └─────────────┘  └────────────────┘
                          │
          ┌───────────────┼───────────────┐
   ┌──────▼──────┐ ┌──────▼──────┐ ┌─────▼─────┐
   │   Stripe    │ │ SendGrid    │ │  Twilio   │
   │  Payments   │ │   Email     │ │    SMS    │
   └─────────────┘ └─────────────┘ └───────────┘
```

---

## 📐 High Level Design (HLD)

### Core Services
| Service | Responsibility |
|---------|---------------|
| **Auth Service** | JWT auth, user registration, role management |
| **Parking Service** | Slot allocation, availability checks, floor management |
| **Booking Service** | Booking lifecycle: park → active → exit → billed |
| **Pricing Service** | Hourly/dynamic/weekend pricing strategies |
| **Payment Service** | Stripe integration, payment link generation |
| **Notification Service** | Email via SendGrid, SMS via Twilio |
| **Cache Service** | Redis-backed slot availability, rate limiting |

### Key Design Decisions
- **Strategy Pattern** for pluggable pricing (Hourly, Dynamic, Weekend, Premium)
- **Factory Pattern** for vehicle type resolution and pricing strategy selection
- **Observer/Events** for decoupled notifications on booking state changes
- **Repository Pattern** via Spring Data JPA for data access abstraction
- **Builder Pattern** for complex DTO construction
- **CQRS-lite** — separate read (cached) and write paths for slot availability

---

## 📊 Database Schema

```sql
-- Users
users (id, name, email, password_hash, phone, role, created_at)

-- Parking infrastructure
parking_lots (id, name, address, total_floors, created_at)
parking_floors (id, lot_id, floor_number, total_slots)
parking_slots (id, floor_id, slot_number, slot_type, status, created_at)

-- Vehicles
vehicles (id, user_id, license_plate, vehicle_type, brand, model)

-- Bookings
bookings (id, user_id, vehicle_id, slot_id, entry_time, exit_time,
          duration_minutes, status, created_at)

-- Billing & Payments
bills (id, booking_id, base_amount, tax_amount, total_amount,
       pricing_strategy, created_at)
payments (id, bill_id, amount, currency, status, stripe_session_id,
          stripe_payment_intent, payment_link, paid_at)
```

---

## 🔐 API Reference

### Authentication
```
POST /api/v1/auth/register     — Register new user
POST /api/v1/auth/login        — Login & receive JWT
POST /api/v1/auth/refresh      — Refresh JWT token
```

### Parking Management
```
GET  /api/v1/parking/lots               — List all lots
GET  /api/v1/parking/lots/{id}/availability — Real-time slot availability
POST /api/v1/parking/lots               — Create lot (ADMIN)
POST /api/v1/parking/lots/{id}/floors   — Add floor (ADMIN)
POST /api/v1/parking/slots              — Add slots (ADMIN)
```

### Bookings
```
POST /api/v1/bookings/park     — Park vehicle (creates booking)
POST /api/v1/bookings/{id}/exit — Exit and generate bill
GET  /api/v1/bookings/active   — Active bookings for user
GET  /api/v1/bookings/history  — Booking history for user
GET  /api/v1/bookings/{id}     — Get booking details
```

### Payments
```
POST /api/v1/payments/{bookingId}/create-session — Create Stripe checkout
GET  /api/v1/payments/{bookingId}/status         — Payment status
POST /api/v1/payments/webhook                    — Stripe webhook handler
```

### Admin
```
GET  /api/v1/admin/dashboard           — System stats
GET  /api/v1/admin/bookings            — All bookings (paginated)
PUT  /api/v1/admin/slots/{id}/status   — Update slot status
```

---

## 🚀 Quick Start

### Prerequisites
- Java 17+, Maven 3.9+
- Node.js 18+, npm 9+
- Docker & Docker Compose
- PostgreSQL 15+ (or use Docker)
- Redis 7+ (or use Docker)

### 1. Clone & Configure

```bash
git clone https://github.com/your-username/parksmart.git
cd parksmart

# Copy and edit environment variables
cp backend/src/main/resources/application-example.yml \
   backend/src/main/resources/application-local.yml
```

### 2. Start with Docker Compose (Recommended)

```bash
docker-compose up -d
# Backend at http://localhost:8080
# Frontend at http://localhost:3000
# PgAdmin at http://localhost:5050
# Kafka UI at http://localhost:8090
```

### 3. Manual Backend Start

```bash
cd backend
mvn clean install -DskipTests
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### 4. Manual Frontend Start

```bash
cd frontend
npm install
npm start
```

---

## 🧪 Testing

```bash
# Backend unit + integration tests
cd backend
mvn test

# Coverage report (generated at target/site/jacoco/)
mvn verify jacoco:report

# Frontend tests
cd frontend
npm test
```

---

## 🐳 Docker

```bash
# Build images
docker build -t parksmart-backend ./backend
docker build -t parksmart-frontend ./frontend

# Full stack
docker-compose up --build
```

---

## ☁️ AWS Deployment

See [infra/aws/README.md](infra/aws/README.md) for full instructions.

**Architecture:**
- **EC2** (t3.small) — Spring Boot backend
- **RDS** (PostgreSQL) — Primary database
- **ElastiCache** (Redis) — Caching layer
- **S3** — Static frontend + file storage
- **CloudFront** — CDN for frontend
- **ALB** — Application Load Balancer
- **MSK** — Managed Kafka

---

## 🔧 Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `DB_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/parksmart` |
| `DB_USERNAME` | DB username | `parking_user` |
| `DB_PASSWORD` | DB password | `changeme` |
| `JWT_SECRET` | 256-bit JWT signing secret | `your-secret-key-min-32-chars` |
| `JWT_EXPIRY` | Token expiry in ms | `86400000` |
| `STRIPE_SECRET_KEY` | Stripe API secret | `sk_live_...` |
| `STRIPE_WEBHOOK_SECRET` | Stripe webhook signing secret | `whsec_...` |
| `SENDGRID_API_KEY` | SendGrid API key | `SG.xxx` |
| `TWILIO_ACCOUNT_SID` | Twilio Account SID | `ACxxx` |
| `TWILIO_AUTH_TOKEN` | Twilio Auth Token | `xxx` |
| `REDIS_HOST` | Redis hostname | `localhost` |
| `KAFKA_BOOTSTRAP` | Kafka broker address | `localhost:9092` |

---

## 📁 Project Structure

```
parking-lot/
├── backend/                        # Spring Boot 3 application
│   ├── src/main/java/com/parksmart/
│   │   ├── config/                 # Spring, Security, Kafka, Redis configs
│   │   ├── controller/             # REST controllers
│   │   ├── service/                # Business logic
│   │   ├── repository/             # JPA repositories
│   │   ├── entity/                 # JPA entities
│   │   ├── dto/                    # Request/Response DTOs
│   │   ├── exception/              # Custom exceptions + global handler
│   │   ├── security/               # JWT filter, UserDetailsService
│   │   ├── strategy/               # Pricing strategies
│   │   ├── factory/                # Vehicle & Pricing factories
│   │   ├── event/                  # Kafka events & listeners
│   │   └── util/                   # Helpers
│   ├── src/test/                   # Unit + integration tests
│   ├── pom.xml
│   └── Dockerfile
├── frontend/                       # React 18 application
│   ├── src/
│   │   ├── components/             # Reusable UI components
│   │   ├── pages/                  # Route-level pages
│   │   ├── store/                  # Redux Toolkit slices
│   │   ├── services/               # Axios API services
│   │   └── hooks/                  # Custom React hooks
│   ├── package.json
│   ├── tailwind.config.js
│   └── Dockerfile
├── infra/
│   ├── aws/                        # AWS CloudFormation / setup scripts
│   ├── nginx/                      # Nginx config
│   └── k8s/                        # Kubernetes manifests (optional)
├── docker/
│   └── docker-compose.yml
├── .github/
│   └── workflows/
│       └── ci-cd.yml               # GitHub Actions pipeline
└── README.md
```

---

## 🧩 Design Patterns Used

| Pattern | Where | Why |
|---------|-------|-----|
| **Strategy** | `PricingStrategy` implementations | Swap pricing algorithms at runtime |
| **Factory** | `PricingStrategyFactory`, `VehicleFactory` | Decouple object creation |
| **Observer/Events** | `BookingEvent`, Kafka listeners | Decoupled notifications |
| **Builder** | DTOs, `BookingResponse` | Readable complex object construction |
| **Singleton** | Spring beans (default) | Single instance per context |
| **Repository** | Spring Data JPA | Data access abstraction |
| **Chain of Responsibility** | Slot allocation algorithm | Try slot types in priority order |
| **Decorator** | Logging/Caching wrappers | Add cross-cutting concerns |
| **Template Method** | `AbstractNotificationService` | Shared notification skeleton |

---

## 📈 Scalability Considerations

- **Redis Cache**: Slot availability cached with 30s TTL; cache invalidated on booking events
- **Kafka**: Booking events published async; notification/email consumers scale independently
- **Connection Pool**: HikariCP with tuned min/max pool size
- **Pagination**: All list endpoints paginated (default 20, max 100)
- **Horizontal Scaling**: Stateless Spring Boot + Redis session → scale behind ALB
- **Database**: Read replicas for analytics/history queries; write to primary
- **Rate Limiting**: Nginx + Redis-backed token bucket on payment endpoints

---

## 🤖 AI Features (Bonus)

- **Demand-based Pricing**: Pricing service queries booking density for past 30 days; applies surge multiplier when >80% occupancy
- **Slot Recommendation**: ML-lite scoring (floor proximity, EV charging) in `SlotRecommendationService`
- **Availability Prediction**: Simple time-series regression on historical data to predict occupancy for next 4 hours

---

## 📄 License

MIT © 2024 — Built for learning, production-hardened.
