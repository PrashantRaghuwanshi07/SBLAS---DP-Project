package project;
// Pattern 10: Memento. Contains only a safe, non-secret draft snapshot.
public record LoanApplicationMemento(String customerId,String customerName,String customerType,String loanType,double amount,int creditScore,double income,double debt,String capturedAt) {}
