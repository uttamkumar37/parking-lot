# ParkSmart - Parking Lot Management System

ParkSmart is a full-stack parking lot management application with a Spring Boot backend and React frontend. The current implementation supports user authentication, parking lot discovery, live slot availability, cinema-style visual slot selection, vehicle parking and exit flow, billing, Stripe checkout session creation, admin metrics, Redis caching, Kafka-backed booking events, and email/SMS notification hooks.

## Current Stack

| Layer | Technology |
| --- | --- |
| Backend | Java 21, Spring Boot 3.2, Maven |
| API/security | Spring Web, Spring Security, JWT |
| Persistence | Spring Data JPA, PostgreSQL, Flyway |
| Cache/events | Redis cache, Kafka |
| Payments/notifications | Stripe, Spring Mail, Twilio |
| Frontend | React 18, Vite, Tailwind CSS, Zustand, Axios, React Router |
| Infra | Docker Compose, Nginx frontend container, GitHub Actions |

## Implemented Features

- Register and login with JWT access tokens.
- Role-based authorization for admin APIs.
- Public parking lot listing, slot availability, and slot-level floor maps.
- Redis-cached slot availability with cache eviction on park/exit.
- Vehicle parking with visual selected-slot booking and fallback slot allocation when no slot is selected.
- Premium React parking picker with lot cards, floor tabs/dropdown, slot-type filters, drive-lane map layout, slot legend, detail panel, and confirmation modal.
- Active booking prevention for already parked vehicles.
- Exit flow that frees the slot, calculates duration, creates a bill, and marks the booking pending payment.
- Pricing strategies: hourly, dynamic surge, and weekend premium.
- Stripe checkout session creation at `/api/v1/payments/{bookingId}/checkout`.
- Local/dev mock checkout links when `STRIPE_SECRET_KEY` is blank or `sk_test_placeholder`.
- Stripe webhook handling for completed and expired checkout sessions.
- Kafka booking events on topic `parking.booking.events`.
- Email/SMS notification hooks for booking, bill, and payment events.
- Admin dashboard, booking listing, revenue summary, lot creation/toggle, and user listing APIs.
- React pages for login, registration, dashboard, cinema-style parking slot selection, booking list/detail, payment success, and admin dashboard.
- Docker Compose services for PostgreSQL, Redis, Kafka/Zookeeper, backend, and frontend.
- Backend unit/controller tests for auth, booking, payment, parking lot slot maps, Kafka config, security ownership, booking/payment flow, and event serialization.

## Architecture

```text
Browser
  |
  | HTTP
  v
React + Vite frontend
  |
  | /api/v1 REST requests
  v
Spring Boot backend
  |-- Controllers
  |-- Services
  |-- Repositories
  |-- Security/JWT filter
  |
  |-- PostgreSQL via JPA/Flyway
  |-- Redis for cache
  |-- Kafka for booking events
  |-- Stripe for checkout
  |-- Mail/Twilio for notifications
```

## API Reference

All backend endpoints are served under `/api/v1`.

### Authentication

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
```

### Parking Lots

```text
GET /api/v1/parking/lots
GET /api/v1/parking/lots/{lotId}/availability
GET /api/v1/parking/lots/{lotId}/slot-map
```

`/slot-map` returns floor, section, and slot-level availability for the visual picker. Sections are derived from slot order when the database has no explicit section table.

Example shape:

```json
{
  "lotId": "uuid",
  "lotName": "ParkSmart Downtown",
  "address": "123 Main Street",
  "city": "San Francisco",
  "totalFloors": 3,
  "active": true,
  "floors": [
    {
      "floorId": "uuid",
      "floorNumber": 1,
      "floorName": "Ground Floor",
      "totalSlots": 31,
      "availableSlots": 30,
      "occupiedSlots": 1,
      "reservedSlots": 0,
      "disabledSlots": 0,
      "sections": [
        {
          "sectionName": "A",
          "slots": [
            {
              "id": "uuid",
              "code": "A01",
              "slotNumber": "G-S01",
              "type": "SMALL",
              "status": "AVAILABLE",
              "evSupported": false,
              "handicapAccessible": false,
              "hourlyRate": 2.00
            }
          ]
        }
      ]
    }
  ]
}
```

### Bookings

```text
POST /api/v1/bookings/park
POST /api/v1/bookings/{bookingId}/exit
GET  /api/v1/bookings/my
GET  /api/v1/bookings/{bookingId}
```

`POST /api/v1/bookings/park` accepts an optional `slotId`. When provided, the backend validates that the selected slot belongs to the lot, is compatible with the vehicle type, and is still available before atomically occupying it. When omitted, the existing automatic allocation/fallback path is used.

```json
{
  "lotId": "uuid",
  "slotId": "uuid",
  "licensePlate": "ABC123",
  "vehicleType": "CAR",
  "brand": "Toyota",
  "model": "Camry",
  "color": "White"
}
```

### Payments

```text
POST /api/v1/payments/{bookingId}/checkout
GET  /api/v1/payments/{bookingId}/status
POST /api/v1/payments/webhook
```

### Vehicles

```text
GET /api/v1/vehicles
```

### Admin

```text
GET   /api/v1/admin/dashboard
GET   /api/v1/admin/bookings
GET   /api/v1/admin/revenue?from=YYYY-MM-DD&to=YYYY-MM-DD
POST  /api/v1/admin/lots
PATCH /api/v1/admin/lots/{lotId}/toggle
GET   /api/v1/admin/users
```

## Database

Flyway migrations create these main tables:

```text
users
vehicles
parking_lots
parking_floors
parking_slots
bookings
bills
payments
```

The seed data creates:

- Default admin user: `admin@parksmart.com` / `Admin@123`
- Sample lot: `ParkSmart Downtown`
- Three floors with small, medium, large, EV, and oversized slots

## Parking Slot Selection UX

The `/park` page provides the main slot selection experience:

- Step 1: choose a parking lot from cards showing name, address, city, floor count, open status, and available slots.
- Step 2: choose a floor with desktop tabs or a mobile dropdown.
- Step 3: filter the map by slot type: small, medium, large, oversized, or EV.
- Step 4: pick an available rectangle from the cinema-style slot map. Occupied, reserved, and maintenance slots are disabled.
- Step 5: review the selected slot in a desktop side panel or mobile bottom sheet.
- Step 6: confirm license plate and vehicle details in a modal.
- Step 7: the frontend calls `POST /api/v1/bookings/park` with the selected `slotId` and navigates to the booking detail page on success.

The dashboard still keeps the high-level summary cards and adds a `Choose Parking Slot` entry point into the visual picker.

## Local Development

### Prerequisites

- Java 21
- Maven 3.9 or the included Maven wrapper
- Node.js 18+
- Docker and Docker Compose

### Backend

```bash
cd backend
./mvnw test
./mvnw spring-boot:run
```

The backend runs on `http://localhost:8080/api/v1` by default.

### Frontend

```bash
cd frontend
npm ci
npm run dev
```

The frontend dev server runs on `http://127.0.0.1:3000` and proxies `/api` to the local backend.

Set `VITE_API_URL` if you need a different API base URL:

```bash
VITE_API_URL=http://localhost:8080/api/v1 npm run dev
```

### Docker Compose

```bash
docker compose up --build
```

Default service URLs:

- Frontend: `http://localhost:3000`
- Backend: `http://localhost:8080/api/v1`
- Backend health: `http://localhost:8080/api/v1/actuator/health`

Inside Docker, the frontend Nginx container proxies `/api` to the backend service.

If a default host port is already in use, override it:

```bash
BACKEND_PORT=18080 FRONTEND_PORT=13000 docker compose up -d
```

## Tests and Builds

```bash
cd backend
./mvnw test
```

```bash
cd frontend
npm ci
npm run build
```

GitHub Actions workflow:

```text
.github/workflows/ci-cd.yml
```

## Project Structure

```text
parking-lot/
├── .github/workflows/ci-cd.yml
├── backend/
│   ├── .mvn/wrapper/
│   ├── mvnw
│   ├── mvnw.cmd
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/parksmart/
│       │   ├── config/
│       │   ├── controller/
│       │   ├── dto/
│       │   ├── entity/
│       │   ├── event/
│       │   ├── exception/
│       │   ├── factory/
│       │   ├── repository/
│       │   ├── security/
│       │   ├── service/
│       │   └── strategy/
│       └── test/java/com/parksmart/
├── frontend/
│   ├── package.json
│   ├── nginx.conf
│   ├── vite.config.js
│   └── src/
│       ├── api/
│       ├── components/
│       │   └── parking/
│       ├── pages/
│       └── store/
├── docker-compose.yml
└── README.md
```

## Design Patterns Used

| Pattern | Where |
| --- | --- |
| Strategy | `PricingStrategy`, `HourlyPricingStrategy`, `DynamicPricingStrategy`, `WeekendPricingStrategy` |
| Factory | `PricingStrategyFactory`, `VehicleSlotFactory` |
| Observer/event | `BookingEvent`, Kafka publisher/listener |
| Builder | Lombok builders and `BookingEvent.Builder` |
| Repository | Spring Data JPA repositories |

## Configuration

Common environment variables:

| Variable | Purpose | Default |
| --- | --- | --- |
| `DB_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/parksmart` |
| `DB_USERNAME` | Database user | `parking_user` |
| `DB_PASSWORD` | Database password | `changeme` |
| `JWT_SECRET` | JWT signing secret | development fallback in `application.yml` |
| `JWT_EXPIRY` | Access token expiry in ms | `86400000` |
| `REDIS_HOST` | Redis host | `localhost` |
| `REDIS_PORT` | Redis port | `6379` |
| `KAFKA_BOOTSTRAP` | Kafka bootstrap servers | `localhost:9092` |
| `STRIPE_SECRET_KEY` | Stripe secret key | `sk_test_placeholder` |
| `STRIPE_WEBHOOK_SECRET` | Stripe webhook secret | `whsec_placeholder` |
| `STRIPE_SUCCESS_URL` | Stripe success redirect | `http://localhost:3000/payment/success` |
| `STRIPE_CANCEL_URL` | Stripe cancel redirect | `http://localhost:3000/payment/cancel` |
| `MAIL_HOST` | SMTP host | `smtp.sendgrid.net` |
| `MAIL_USERNAME` | SMTP username | `apikey` |
| `MAIL_PASSWORD` | SMTP password | empty |
| `TWILIO_ACCOUNT_SID` | Twilio account SID | empty |
| `TWILIO_AUTH_TOKEN` | Twilio auth token | empty |
| `TWILIO_FROM_NUMBER` | Twilio sender number | empty |
| `VITE_API_URL` | Frontend API base URL | `/api/v1` |
| `BACKEND_PORT` | Docker Compose backend host port | `8080` |
| `FRONTEND_PORT` | Docker Compose frontend host port | `3000` |

## Roadmap / Planned Features

These items are not implemented in the current codebase:

- Refresh token endpoint and refresh token persistence.
- Redux Toolkit migration; the current frontend uses Zustand.
- Legacy `/payments/{bookingId}/create-session` alias; the implemented endpoint is `/checkout`.
- AWS infrastructure folder and deployment scripts.
- Slot recommendation service.
- Availability prediction.
- AI/time-series demand forecasting.
- Redis-backed rate limiting.
- PgAdmin and Kafka UI services in Docker Compose.

## License

MIT
