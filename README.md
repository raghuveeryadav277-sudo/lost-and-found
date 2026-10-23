# Lost & Found — Smart Campus Asset Recovery Platform

A web platform that replaces the scattered, manual campus lost-and-found
process (notice boards, chat groups, security-desk registers) with one
structured flow:

**Report → Search → Match → Claim → Verify → Return**

Built by **Team Four Founders**, Department of Information Technology,
G. L. Bajaj Institute of Technology and Management, Greater Noida.

| Member | Role |
|---|---|
| Ravyanshi Omar | Team Leader — Frontend (HTML, CSS, JavaScript) |
| Rajeshwari Agrawal | Frontend (HTML, CSS, JavaScript) |
| Pushkar Singh Bharadwaj | Java Development (Core Java) |
| Raghuveer Kumar | Backend & Database (Spring Boot, SQL) |

---

## How to run (demo)

You only need **Java 17 or newer** installed — nothing else, no database
server, no setup.

- **Windows:** double-click **`START.bat`**
- **Linux/Mac:** run `./start.sh`
- Or directly: `java -jar lost-and-found.jar`

Then open **http://localhost:8080** in a browser.
Data is stored in a local file database (folder `data/` next to the jar)
and uploaded photos in `uploads/`, so reports survive restarts.

### Demo logins (seeded on first run)

| Account | Email | Password | Role |
|---|---|---|---|
| Admin | `admin@campus.edu` | `admin123` | ADMIN |
| Student | `aarav@campus.edu` | `student123` | USER |
| Student | `meera@campus.edu` | `student123` | USER |

Six sample reports are also seeded — Aarav's **lost** black wallet and
Meera's **found** black wallet match each other, so the complete
claim → approve → QR verify → returned flow can be demoed immediately:

1. Login as **Aarav**, open the found *Black Wallet*, raise a claim with a proof note.
2. Login as **Meera**, go to **My Claims → Claims on my items → Approve**. A QR code is generated.
3. Still as Meera, open **Verify Handover**, enter the verification code (or scan the QR) → the item becomes **RETURNED** and both users get notifications.
4. Login as **Admin** to see the whole case in the audit lists.

---

## Features (mapped to the project PPT / report)

| PPT / report point | Where it is in the app |
|---|---|
| Report, Search, Reconnect | Report Item page; Browse & Search with category / type / keyword / location filters |
| AI Image Matching (innovation highlight) | Every uploaded photo gets a perceptual fingerprint (64-bit average hash). Item pages show **Match score** and **Image match score** for suggested items; strong matches trigger notifications |
| QR Verification | Approving a claim generates a one-time token + QR code (ZXing). The Verify page confirms handover and closes the case |
| Notifications | Alerts on match found, claim raised, claim approved/rejected and item returned; unread badge in the navbar |
| Smart Campus Map | Schematic campus map (SVG) with pins for open reports at Library, Canteen, Main Gate, Lab Block, Hostel, Playground, etc. |
| Secure Authentication | Registration + form login, BCrypt-hashed passwords (Spring Security) |
| Verified Claims | Ownership proof note; only the reporter (or admin) can approve/reject |
| Role-Based Access | USER and ADMIN roles; Admin page (all cases, moderation, audit) is admin-only |
| Database design (4 tables) | `users`, `items`, `claims`, `notifications` — exactly the tables in the report |
| End-to-end journey | Create account → Report → Search → Verify → Return → Track (My Reports / My Claims) |

## Tech stack

- **Frontend:** HTML, CSS, JavaScript, Bootstrap 5 (static portal served by the app; calls REST APIs with `fetch`)
- **Backend:** Java 21, Spring Boot 3 (Web, Data JPA, Security, Validation)
- **Database:** SQL — H2 relational database in file mode (genuine SQL schema with PK/FK; chosen so the demo runs anywhere with zero DB installation)
- **QR codes:** ZXing (core + javase)
- **Build:** Maven — `mvn -DskipTests package` produces the single runnable jar

## Project structure

```
lost-and-found/
├── pom.xml                     Maven build (Spring Boot 3.3, Java 21)
├── START.bat / start.sh        One-click launchers
├── lost-and-found.jar          The runnable app (after build)
└── src/main/
    ├── java/com/campus/lostfound/
    │   ├── LostFoundApplication.java
    │   ├── config/     Security config, web config, first-run seed data
    │   ├── model/      User, Item, Claim, Notification (the 4 tables)
    │   ├── repository/ Spring Data JPA repositories
    │   ├── service/    Matching engine, image hashing, QR, notifications
    │   └── controller/ REST APIs: auth, items, claims, notifications, admin
    └── resources/
        ├── application.properties
        └── static/     Portal pages (HTML/CSS/JS + Bootstrap)
```

## Honest notes for the demo

- **Image matching** is a prototype implementation of the PPT's "AI Image
  Matching": a classic perceptual-hash comparison (fast, offline, and
  explainable), not a cloud AI model.
- **Campus map** is a schematic diagram of the campus, not Google Maps.
- The embedded H2 SQL database can be swapped for MySQL/PostgreSQL by
  changing only `application.properties` and adding the driver — the JPA
  entities and SQL schema stay the same.
- CSRF protection is disabled in this prototype for the same-origin
  fetch-based forms; a production deployment should enable it.
