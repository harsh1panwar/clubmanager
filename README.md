# ClubPass — Campus Club & Event Manager

A Spring Boot web app for college clubs: organizers create events, students register, attendance is taken by scanning a **rotating QR code**, and verified attendees download a **PDF certificate**.

The core problem it solves is *proxy attendance*. A static QR code (or a printed sheet) can be screenshotted and forwarded to a friend who never showed up. ClubPass shows a QR code on the organizer's screen that changes every 30 seconds, and each student scans it from their own logged-in account — so a forwarded screenshot is both expired and useless for marking anyone else present.

## Features

**Organizers**
- Create clubs and events (venue, date, capacity)
- Open a full-screen live check-in pass with a rotating QR code and countdown
- Watch check-ins arrive in real time on the attendance roster

**Students**
- Browse upcoming events and register (capacity-limited)
- Check in by scanning the live QR with the phone camera (photo upload and paste-the-code fallbacks)
- Download a personalised participation certificate (PDF) once attendance is verified

## How the rotating QR works

The check-in token is a stateless HMAC over the event ID and the current 30-second time window — the same idea behind TOTP apps like Google Authenticator:

```
token = Base64Url( HMAC-SHA256(secret, eventId + ":" + floor(epochSeconds / 30)) )
```

- **Nothing is stored** in the database for tokens; the server recomputes the expected value.
- Validation accepts the current and previous window only (≈30–60 s), compared in constant time.
- The attendance row is tied to the **authenticated user** making the request, not to anything inside the QR, so a shared code cannot mark someone else present.

## Tech stack

| Layer | Technology |
|---|---|
| Backend | Java 17+, Spring Boot 3.5, Spring Web, Spring Data JPA, Spring Security |
| Auth | Stateless JWT (JJWT, HS512) with role-based access (ORGANIZER / ATTENDEE), BCrypt password hashing |
| Database | PostgreSQL, Hibernate |
| QR & PDF | ZXing (QR images), Apache PDFBox (certificates) |
| Frontend | Static HTML/CSS/JS served by Spring Boot, html5-qrcode for camera scanning |

Architecture is a classic layered design: `controller → service → repository → entity`, with DTOs at the API boundary and a global exception handler that returns consistent JSON errors.

## API

| Method | Endpoint | Role | Description |
|---|---|---|---|
| POST | `/api/auth/register` | public | Create an account, returns JWT |
| POST | `/api/auth/login` | public | Log in, returns JWT |
| POST | `/api/clubs` | Organizer | Create a club |
| GET | `/api/clubs` / `/api/clubs/mine` | any / Organizer | List clubs |
| POST | `/api/events` | Organizer | Create an event for a club you own |
| GET | `/api/events` / `/api/events/{id}` | any | Browse events with live seat counts |
| GET | `/api/events/mine` | Organizer | Events you organize |
| GET | `/api/events/{id}/live-qr` | Organizer (owner) | Current token + seconds until rotation (JSON) |
| GET | `/api/events/{id}/live-qr-image` | Organizer (owner) | Current token as a PNG QR code |
| GET | `/api/events/{id}/attendees` | Organizer (owner) | Attendance sheet |
| POST | `/api/events/{id}/register` | Attendee | Register for an event |
| POST | `/api/events/{id}/check-in` | Attendee | Submit scanned token `{ "token": "..." }` |
| GET | `/api/events/{id}/certificate` | Attendee (checked in) | Download PDF certificate |
| GET | `/api/registrations/me` | Attendee | Your registrations and attendance status |

Errors always look like `{ "status": 409, "error": "Event is full", "timestamp": "..." }`.

## Running locally

1. Install PostgreSQL and create a database:
   ```sql
   CREATE DATABASE clubmanager_db;
   ```
2. Set environment variables (IntelliJ: Run → Edit Configurations → Environment variables):
   ```
   DB_PASSWORD=<your postgres password>
   APP_JWT_SECRET=<random string, at least 64 characters>
   ```
   Optional: `DB_URL`, `DB_USERNAME`, `TOTP_SECRET` (defaults to the JWT secret), `PORT`, `SHOW_SQL=true`.

   Never commit passwords or secrets to the repository.
3. Run `ClubmanagerApplication`, or:
   ```
   ./mvnw spring-boot:run
   ```
4. Open http://localhost:8080 — create one **Club organizer** account and one **Student** account.

**Testing the camera scan from a phone:** browsers only allow camera access over HTTPS or `localhost`. On a phone over plain `http://<your-laptop-ip>:8080`, use "Scan a photo" or "Paste the code", or deploy the app (Render/Railway give you HTTPS).

## Tests

`TotpServiceTest` covers token acceptance, cross-event rejection, forged-secret rejection and blank input:

```
./mvnw test -Dtest=TotpServiceTest
```

## Design decisions worth discussing

- **Rotating server-displayed QR instead of a per-student QR** — closes the screenshot-sharing loophole of static tickets.
- **Stateless HMAC tokens** — no token table, no cleanup job, horizontally scalable.
- **N+1 query fixes** — `@EntityGraph` for event/club/organizer joins and a single grouped `COUNT` query for registration counts across all events.
- **Certificates generated on demand** — no file storage needed; a PDF is rendered from the database only when an attended registration requests it.
- **DTOs everywhere** — entities (and password hashes) never leave the API.
