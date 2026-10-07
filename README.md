# ClubManager

A Spring Boot-based event and attendance management system with JWT authentication, role-based access control, PostgreSQL persistence, and TOTP/QR-based attendance verification.

## Overview

ClubManager supports two user roles:

- **ORGANIZER** — creates clubs/events and displays a rotating live QR code for attendance.
- **ATTENDEE** — browses and registers for events and can check in using the event QR flow.

The project is designed as a REST API backend with a lightweight HTML/JavaScript frontend.

## Features

- JWT-based authentication and authorization
- BCrypt password hashing
- Role-based access control
- Club and event management
- Event registration
- TOTP-based time-sensitive QR generation
- QR-based attendance check-in
- PostgreSQL database persistence
- HTML/JavaScript frontend served by Spring Boot

## Technology Stack

- Java 17
- Spring Boot
- Spring Security
- Spring Data JPA / Hibernate
- JWT (JJWT)
- PostgreSQL
- ZXing
- HTML
- JavaScript
- Maven

## Architecture

```text
HTML / JavaScript Client
          |
          v
Spring Boot REST API
          |
   Spring Security
      + JWT
          |
          v
 Service Layer
          |
          v
 Spring Data JPA
          |
          v
      PostgreSQL
```

## Main REST API

### Authentication
- `POST /api/auth/register`
- `POST /api/auth/login`

### Clubs
- `POST /api/clubs`
- `GET /api/clubs`

### Events
- `POST /api/events`
- `GET /api/events`
- `GET /api/events/{id}`
- `POST /api/events/{id}/register`
- `GET /api/events/{id}/live-qr`
- `GET /api/events/{id}/live-qr-image`
- `POST /api/events/{id}/check-in`

## Data Model

Core entities:

- **User**
- **Club**
- **Event**
- **Registration**

Relationships connect users with clubs, events, and registrations while registration records store attendance status and timestamp information.

## QR Attendance Flow

1. An organizer requests the live QR for an event.
2. The backend generates a time-based TOTP value.
3. The TOTP value is encoded into a QR image.
4. The organizer screen refreshes the QR periodically.
5. The attendee scans the QR and submits the token to the check-in endpoint.
6. The backend validates the token before marking attendance.

## Running Locally

### Prerequisites

- Java 17+
- Maven
- PostgreSQL

### 1. Create the database

Create a PostgreSQL database named:

```text
clubmanager_db
```

### 2. Configure environment variables

Set the following environment variables:

```text
DB_PASSWORD=<your-postgres-password>
APP_JWT_SECRET=<your-random-secret>
```

Do not commit passwords or application secrets to the repository.

### 3. Start the application

```bash
./mvnw spring-boot:run
```

On Windows:

```powershell
mvnw.cmd spring-boot:run
```

The application runs on port 8080 by default.

## Project Structure

```text
src/
├── main/
│   ├── java/          # Controllers, services, security, entities, repositories
│   └── resources/
│       ├── static/    # HTML/JavaScript frontend
│       └── application.properties
└── test/
```

## Status

The core authentication, event-management, organizer QR workflow, and attendance API are implemented. The attendee-side scanning UI is still being expanded.
