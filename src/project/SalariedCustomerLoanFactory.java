package project;
public class SalariedCustomerLoanFactory implements LoanFactory {
    public Loan createLoan(String type, Customer c, double a, LoanStatusNotifier n) {
        return switch(type) { case "Personal Loan" -> new SalariedPersonalLoan(c,a,n); case "Home Loan" -> new SalariedHomeLoan(c,a,n); case "Car Loan" -> new SalariedCarLoan(c,a,n); default -> null; };
    }
}
