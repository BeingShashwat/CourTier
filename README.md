# CourTier

> Track Indian court cases automatically using CNR numbers without checking eCourts manually again and again.

CourTier is a backend-focused project built to simplify court case tracking for lawyers, interns, litigants, and anyone who regularly checks case updates on the Indian eCourts portal.

The idea came from seeing how repetitive and frustrating it is to manually open the eCourts website every time just to check whether a hearing date or case status changed.

This project automates that workflow.

---

## What Problem Does It Solve?

The eCourts portal is useful, but:

* checking updates manually is repetitive
* tracking multiple cases becomes messy
* there is no simple personal tracking system
* hearing updates can easily be missed

CourTier tries to solve this by allowing users to:

* save cases using CNR numbers
* track multiple cases in one place
* fetch latest case details automatically
* avoid repeatedly visiting the eCourts portal

---

## Features

* JWT Authentication with refresh token support
* User Registration & Login
* Secure Spring Security setup
* Add cases using CNR number
* Track multiple cases per user
* eCourts scraping integration (district courts + Allahabad HC)
* Captcha handling flow
* Kafka-based diff detection pipeline
* Transactional Outbox Pattern for reliable event publishing
* Redis caching for fast read APIs
* Rate limiting via Bucket4j
* Circuit Breaker with Resilience4j
* Background polling scheduler
* Soft delete tracked cases
* Centralized error handling
* REST APIs with structured responses

---

## Tech Stack

### Backend

* Java 21
* Spring Boot
* Spring Security
* Spring Data JPA
* Maven

### Database

* PostgreSQL

### Caching & Messaging

* Redis
* Apache Kafka

### Reliability

* Resilience4j (Circuit Breaker)
* Transactional Outbox Pattern
* Bucket4j (Rate Limiting)

### Authentication

* JWT Tokens (Access + Refresh)

### Infrastructure

* AWS EC2
* Nginx (Reverse Proxy)

### Other Libraries / Tools

* Jsoup
* Lombok
* Hibernate

---

## Project Structure

```bash
src/main/java/com/courtier/courtier
│
├── common
│   ├── config
│   ├── exception
│   └── security
│
├── user
│   ├── controller
│   ├── dto
│   ├── entity
│   ├── repository
│   └── service
│
├── case_
│   ├── controller
│   ├── dto
│   ├── entity
│   ├── repository
│   └── service
│
└── scraper
```

---

## API Flow

### Authentication

```http
POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh
```

### Case Tracking

```http
POST   /api/cases
GET    /api/cases
GET    /api/cases/{cnr}
POST   /api/cases/{cnr}/poll
DELETE /api/cases/{cnr}
```

---

## How It Works

1. User registers and logs in — receives a JWT access + refresh token pair
2. User adds a case using its CNR number
3. Backend scrapes the eCourts portal and parses case details
4. Case data is stored in PostgreSQL; updates are published via Kafka using the Outbox pattern
5. Redis caches frequently read case data for fast subsequent lookups
6. User can manually poll a case to trigger a fresh scrape and diff check
7. Circuit Breaker protects against scraper failures; rate limiting prevents abuse

---

## Performance Benchmarks

Benchmarked with all components active: Redis ON, Kafka ON, Circuit Breaker ON, Outbox ON.  
EC2 numbers include public internet round-trip through Nginx.

| Endpoint | Localhost | EC2 (Public IP) |
|---|---|---|
| `POST /api/auth/login` | 89.67 ms | 231.89 ms |
| `POST /api/auth/refresh` | 6.97 ms | 169.53 ms |
| `GET /api/cases` | 10.50 ms | 158.41 ms |
| `GET /api/cases/{cnr}` | 5.63 ms | 157.92 ms |
| `POST /api/cases` (cold scrape) | ~969 ms | ~1.00 s |
| `POST /api/cases/{cnr}/poll` | ~729 ms | ~786 ms |
| `DELETE /api/cases/{cnr}` | 24 ms | 178 ms |

**Key observations:**

* Auth endpoints show the largest EC2 overhead due to network latency, Nginx traversal, and bcrypt hashing.
* Cached read APIs (`GET /api/cases`, `GET /api/cases/{cnr}`) stay well under 200 ms even over the public internet — Redis is doing its job.
* Cold scrape and poll latencies are nearly identical between localhost and EC2. The bottleneck is the external eCourts website, not the infrastructure.
* Delete is fast in both environments; the 154 ms difference on EC2 is purely network round-trip.

---

## Running Locally

### Clone Repository

```bash
git clone https://github.com/BeingShashwat/CourTier.git
cd CourTier
```

### Configure Database

Update `application.yml`

```yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/courtier
    username: postgres
    password: your_password
```

### Run Application

```bash
./mvnw spring-boot:run
```

---

## Future Improvements

* email/SMS notifications on case updates
* advocate dashboard with multi-client case management
* case hearing reminders
* frontend integration
* Docker deployment

---

## Why I Built This

I wanted to build something that solves an actual workflow problem instead of another generic CRUD project.

This project helped me learn:

* Spring Security and JWT flows
* Kafka producer/consumer architecture
* Redis caching and rate limiting
* Resilience patterns (Circuit Breaker, Outbox)
* Scraping workflows and real-world edge cases
* Backend architecture and database modeling
* Deploying and benchmarking on AWS EC2

---

## Status

Deployed on AWS EC2 with Nginx as reverse proxy. All backend components active.

Currently focused on improving scraper reliability and exploring notification integrations.

---

## Author

Shashwat  
3rd Year CSE Student  
Chandigarh University