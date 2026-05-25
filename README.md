# Employee Management System - Backend & Workforce Engine

An enterprise-grade RESTful API developed using Java 17 and Spring Boot, specifically tailored for managing shift-based, overtime-heavy blue-collar workforces in the construction industry. This system tracks real-time site attendance, calculates tiered overtime wages under a monthly cap, processes atomic payroll settlements, and incorporates robust fault-tolerant backend architectures.

---
## Table of Contents

- [Info](#info)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Setup Instructions](#setup-instructions)
- [Environment Variables](#environment-variables)
- [API Endpoints](#api-endpoints)




## 🛠️ Recruitment Assignment Metadata

### 1. Forked Repository Context
- **HRMS Forked:** [employee-management-system-backend)](https://github.com/harsh-1806/employee-management-system-backend)
- **Why this repo:** It provided a clean, lightweight Spring Boot + JPA configuration with basic worker entities, already built on Java 17 and Spring Boot 3.x 

### 2. AI Tool Utilization Transparency
- **Gemini:** Used to generate entities, services, DTOs, controllers.
- **ChatGPT:** Used to create configs, sql queries for database, bug fixing.

### 3. Core Architectural Decisions & Trade-offs
- **Schema Constraints vs. App-Layer Validation:** Rather than relying solely on Java validation, strict constraints like unique structural composite indexes and database-level checks (`clock_out_time > clock_in_time`) were enforced. This ensures data integrity even if parallel threads attempt race-condition clock-ins.
- **Redis Hash-Map Structure for Real-Time State:** Active workers are stored in Redis as individual hashes prefixed by `active_worker:`. This provides $O(1)$ fast lookups for site supervisors standing on-site, completely isolating high-frequency dashboard traffic from hitting the core Supabase PostgreSQL instance.
- **Asynchronous & Decoupled Side Effects:** To comply with Ticket LF-204, notification tracking is completely abstracted away from the database transaction lifecycle. SMS notifications are emitted as decoupled internal Spring Events, bound to a transaction lifecycle, ensuring an employee never receives a premature settlement notification on a rolled-back execution path.
- **Future Improvements with More Time:** Given more time, I would replace synchronous REST calls to external government systems with asynchronous reactive pooling (`WebClient` with custom thread isolations) and implement a scheduled dead-letter queue (DLQ) batch processor to automatically flag and eject active worker instances lingering in Redis past the 16-hour system threshold.

---

## 🚀 Local Setup Instructions

This application is built with Java 17+, Spring Boot, Supabase (PostgreSQL), and Redis.

### Prerequisites
- **Java 17** or later installed
- **Maven 3.x** installed
- **Redis Server** (Running locally on `localhost:6379` or an accessible remote instance)
- A free **Supabase Account** ([supabase.com](https://supabase.com))

### 1. Database Setup (Supabase Integration)
1. Log into your **Supabase Dashboard** and create a new project.
2. Navigate to **Project Settings -> Database**.
3. Under the **Connection String** section, copy the **URI** string (ensure it uses port `6543` for connection pooling/PgBouncer to prevent connection exhaustion as requested in LF-205).
4. Keep your project database password ready for environment mapping.

### 2. System Environment Variable Setup
To prevent security leaks, database credentials and cache profiles must not be hardcoded inside `application.yml`. Configure your local terminal environment with these exact keys before launching the app:

#### On Linux/macOS:
```bash
export DB_USERNAME="postgres.your-supabase-project-id"
export DB_PASSWORD="your-secure-supabase-password"
export DB_HOST="hostname"
export DB_PORT="Port number"

#### On Windows:

$env:DB_USERNAME="postgres.your-supabase-project-id"
$env:DB_PASSWORD="your-secure-supabase-password"
$env:DB_HOST="hostname"
$env:DB_PORT="Port number"

### 3. Run the Application
Compile the binaries and execute the Spring Boot application profile using the Maven wrapper:

Bash
mvn clean install
mvn spring-boot:run

### 4. 🧭 Core REST API Reference
All requests must pass standard payload validation schemas. Faulty requests or domain violations return structured JSON errors.

Part 1: Worker Attendance Tracker
1. Clock-In Worker
Endpoint: POST /api/attendance/clock-in

Payload:

JSON
{
  "workerId": 1,
  "siteId": 101
}
Response (200 OK):

JSON
{ "message": "Clock-in successful." }
2. Clock-Out Worker
Endpoint: POST /api/attendance/clock-out

Payload:

JSON
{ "workerId": 1 }
Response (200 OK):

JSON
{ "message": "Clock-out registered successfully." }
3. Real-Time Active Worker Cache (Served Exclusively from Redis)
Endpoint: GET /api/attendance/active

Response (200 OK):

JSON
[
  {
    "workerId": 1,
    "workerName": "Ramesh Kumar",
    "siteId": 101,
    "siteName": "Greenfield Phase 2",
    "clockInTime": "2026-05-26T08:00:00"
  }
]
4. Historical Paginated Logs
Endpoint: GET /api/attendance/log?workerId=1&from=2026-05-01T00:00:00&to=2026-05-31T23:59:59&page=0&size=10

Response (200 OK):

JSON
{
  "content": [
    {
      "attendanceId": 452,
      "workerName": "Ramesh Kumar",
      "siteName": "Greenfield Phase 2",
      "clockInTime": "2026-05-25T08:00:00",
      "clockOutTime": "2026-05-25T18:00:00",
      "totalHours": 10.00,
      "overtimeHours": 2.00,
      "isFlagged": false
    }
  ],
  "currentPage": 0,
  "totalElements": 1,
  "totalPages": 1
}
Part 2: Overtime Calculation & Settlements
5. Fetch Monthly Payroll Summary
Endpoint: GET /api/overtime/summary/1?month=2026-04

Response (200 OK):

JSON
{
  "workerId": 1,
  "month": "2026-04",
  "totalOvertimeHours": 12.50,
  "totalPayoutAmount": 3450.00,
  "breakdown": [
    {
      "date": "2026-04-12",
      "hours": 2.50,
      "rateApplied": 150.00,
      "amount": 525.00,
      "status": "PENDING"
    }
  ]
}
6. Execute Monthly Settlement (Atomic Transaction Block)
Endpoint: POST /api/overtime/settle/1?month=2026-04

Response (200 OK):

JSON
{
  "message": "Overtime settled successfully.",
  "totalSettledAmount": 3450.00,
  "entriesSettled": 5
}
🛑 Global Structured Error Response Matrix
When business rules or data constraints are breached, endpoints gracefully return descriptive JSON schemas instead of structural trace stack logs:

JSON
{
  "error": "INVALID_SETTLEMENT",
  "message": "Cannot settle current or future months.",
  "timestamp": "2026-05-26T12:05:00Z"
}
📄