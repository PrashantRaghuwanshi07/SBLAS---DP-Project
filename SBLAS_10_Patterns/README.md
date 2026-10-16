# Smart Bank Loan Approval System (SBLAS) — 10 Design Patterns

Academic Java + SQLite web application. This is a demonstration project, **not production banking software**.

## Design patterns implemented

Existing five:
1. **Proxy** — `LoanPortalProxy`: authenticates demo user, applies the KYC gate and validates application fields.
2. **Singleton** — `LoanProcessingSystem`: one central processing service.
3. **Abstract Factory** — `LoanFactory`, `SalariedCustomerLoanFactory`, `SelfEmployedCustomerLoanFactory`: compatible loan creation by customer category.
4. **Chain of Responsibility** — `ApprovalChain` and `LoanApprover` nodes route a loan to the first authority whose amount limit covers it.
5. **Bridge** — `LoanStatusNotifier` + `NotificationChannel` + email/SMS/app implementations.

Five additions:
6. **Builder** — `LoanApplicationBuilder` builds and validates application details.
7. **Strategy** — `CreditAssessmentStrategy` and `StandardCreditAssessmentStrategy` isolate risk policy.
8. **Mediator** — `LoanWorkflowMediator` coordinates assessment, persistence, assignment and snapshotting.
9. **Template Method** — `LoanProcessingTemplate` defines the ordered validation/KYC/risk/fraud workflow.
10. **Memento** — `LoanApplicationMemento` and `LoanApplicationHistory` capture safe draft snapshots; snapshots are stored in `ApplicationSnapshot`.

## Requirements
- JDK 17 or later
- Maven 3.9+
- Internet on first run so Maven can download SQLite JDBC dependencies

## Run in IntelliJ
1. Extract this ZIP.
2. Open the `SBLAS_10_Patterns` folder in IntelliJ as a Maven project (open `pom.xml`).
3. Wait for Maven dependency sync.
4. Run `project.SBLASWebServer` from `src/project/SBLASWebServer.java`, or run `mvn clean compile exec:java` from the project terminal.
5. Open **http://127.0.0.1:8080**.

The database is `data/SBLAS.db`. `DatabaseSetup` creates missing tables and adds new columns to an older project database without deleting existing rows. Stop the server before manually editing the DB.

## Demo credentials

| Role | Username | Password |
|---|---|---|
| Customer application | `customer` | `Demo@1234` |
| Branch Manager | `branch` | `Branch@1234` |
| Regional Manager | `regional` | `Regional@1234` |
| Credit Committee | `committee` | `Committee@1234` |
| Risk Director (last escalation tier) | `riskdirector` | `RiskDirector@1234` |

These are intentionally published lab credentials. Do not use them for real accounts or deploy this demo to a public network.

## Test timed escalation

Default approval deadline is 7 days (168 hours). To demonstrate quickly, run the `run-demo-escalation-windows.bat` file or start with JVM options:

```bash
MAVEN_OPTS="-Dsblas.deadline.seconds=60 -Dsblas.escalation.poll.seconds=5" mvn clean compile exec:java
```

Submit an application and do not decide it. In the demo launcher, after roughly 60 seconds, it should be reassigned from Branch Manager to Regional Manager (or to the next authority based on the original amount). After each escalation, a fresh deadline starts. The final escalation tier remains pending for governance review if it misses its deadline; the system never auto-approves or auto-rejects a loan.

## How to test the main flow
1. Submit a Salaried / Personal Loan application with KYC checked and customer demo credentials.
2. For example, use amount `500000`, monthly income `60000`, debt `0`, and credit score `720`.
3. Open the staff workspace. Log in as `branch` / `Branch@1234` to review applications assigned to Branch Manager.
4. Approve/reject an assigned pending request. A decision is only accepted from the currently assigned role, only while status is `PENDING`, and only once.
5. Load the audit log to see application creation, decisions, fraud flags and escalation events.
6. Open `data/SBLAS.db` in DB Browser for SQLite and inspect `Customer`, `LoanRequest`, `AuditLog`, `LoginAttempts`, and `ApplicationSnapshot`.

## Security and realism features in this lab build
- PBKDF2-HMAC-SHA256 password verification for demo accounts.
- Five failed customer login attempts within a 10-minute window trigger a lockout.
- Role-specific staff authentication; server verifies that a decision-maker matches the current assigned authority.
- KYC gate, input validation, prepared SQL statements, foreign keys, transactions and optimistic version checks for escalation.
- Rule-based credit score/debt-to-income assessment; high-risk cases receive a manual-review flag.
- Audit log, duplicate-decision protection, content-security headers, no-store responses and loopback-only binding (`127.0.0.1`).
- Scheduled escalation on missed deadlines.

## Important limitations
- The KYC checkbox is a **simulation** and is not identity verification. A real system must obtain KYC status from a trusted backend provider and must never trust a browser checkbox.
- Credit score/risk thresholds and EMI estimate are illustrative rules only. There is no CIBIL/credit-bureau API connection and no actual credit report is fetched.
- Email/SMS/App channels print demonstration notifications to the server console; they do not send real messages.
- The app is bound to localhost. Do not expose it to the internet. Real banking requires a reviewed identity provider, per-user password salts, MFA, CSRF protection, secrets management, TLS, formal authorization, rate limiting across every endpoint, encrypted backups, monitoring, penetration testing and applicable regulatory compliance.
- `Memento` snapshots store draft fields for recovery/audit demonstration. They are not allowed to roll back final approval decisions.
