package project;
public class SelfEmployedCustomerLoanFactory implements LoanFactory {
    public Loan createLoan(String type, Customer c, double a, LoanStatusNotifier n) {
        return switch(type) { case "Business Loan" -> new SelfEmployedBusinessLoan(c,a,n); case "Working Capital Loan" -> new SelfEmployedWorkingCapitalLoan(c,a,n); case "Home Loan" -> new SelfEmployedHomeLoan(c,a,n); default -> null; };
    }
}
