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

* JWT Authentication
* User Registration & Login
* Secure Spring Security setup
* Add cases using CNR number
* Track multiple cases per user
* eCourts scraping integration
* Captcha handling flow
* Poll/update endpoints
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

### Authentication

* JWT Tokens

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
POST /auth/register
POST /auth/login
```

### Case Tracking

```http
POST /cases/add
GET /cases
DELETE /cases/{id}
POST /cases/poll
```

---

## How It Works

1. User registers/login
2. JWT token gets generated
3. User adds a case using CNR number
4. Backend interacts with eCourts flow
5. Case details are parsed and stored
6. User can poll for latest updates anytime

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

* email notifications
* scheduled background polling
* advocate dashboard
* case hearing reminders
* frontend integration
* analytics for tracked cases
* Docker deployment
* Redis caching

---

## Why I Built This

I wanted to build something that solves an actual workflow problem instead of another generic CRUD project.

This project helped me learn:

* Spring Security
* JWT Authentication
* API design
* scraping workflows
* backend architecture
* database modeling
* handling real-world edge cases

---

## Status

Backend MVP completed.

Currently focused on improving:

* scraper reliability
* polling optimization
* deployment setup

---

## Author

Shashwat
3rd Year CSE Student
Chandigarh University
