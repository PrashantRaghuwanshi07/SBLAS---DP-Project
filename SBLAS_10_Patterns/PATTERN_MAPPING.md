# SBLAS — pattern-to-code map

| # | Pattern | Main code | Role in SBLAS |
|---:|---|---|---|
| 1 | Proxy | `LoanPortalProxy` | Authenticates demo customer, records failed login attempts, applies KYC and input gates before submission. |
| 2 | Singleton | `LoanProcessingSystem` | Provides one central application-processing service. |
| 3 | Abstract Factory | `LoanFactory`, `SalariedCustomerLoanFactory`, `SelfEmployedCustomerLoanFactory` | Creates only loan types supported by the customer category. |
| 4 | Chain of Responsibility | `LoanApprover`, `BranchManagerApprover`, `RegionalManagerApprover`, `CreditCommitteeApprover`, `ApprovalChain` | Routes amounts to the first suitable authority and advances pending applications after missed deadlines. |
| 5 | Bridge | `LoanStatusNotifier`, `NotificationChannel`, `EmailChannel`, `SMSChannel`, `AppChannel` | Keeps notification logic independent from the delivery channel. |
| 6 | Builder | `LoanApplicationBuilder` | Builds customer/application details with chained setters and basic validation. |
| 7 | Strategy | `CreditAssessmentStrategy`, `StandardCreditAssessmentStrategy` | Isolates credit-risk policy from workflow code. |
| 8 | Mediator | `LoanWorkflowMediator` | Coordinates assessment, persistence, authority assignment, fraud flags and memento snapshotting. |
| 9 | Template Method | `LoanProcessingTemplate`, `StandardLoanProcessing` | Defines a fixed sequence of validation, KYC, credit assessment and fraud checks. |
| 10 | Memento | `LoanApplicationMemento`, `LoanApplicationHistory` | Captures safe application-state snapshots; persisted snapshots support draft-history demonstration. |

## Approval limits and escalation
- Branch Manager: up to ₹10,00,000.
- Regional Manager: up to ₹50,00,000.
- Credit Committee: up to ₹1,00,00,000.
- Risk Director: escalation oversight after the Credit Committee deadline.
- Default deadline: 7 days (168 hours); for class demonstration, use 60 seconds with the provided demo launcher.

A deadline miss changes the assigned authority and records an audit event. It does not make a decision on the customer's behalf.
