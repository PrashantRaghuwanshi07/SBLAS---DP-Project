package project;
public interface LoanFactory {
    Loan createLoan(String type, Customer customer, double amount, LoanStatusNotifier notifier);
}
