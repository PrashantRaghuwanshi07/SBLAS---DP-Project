package project;

public abstract class Loan {
    private final Customer customer;
    private final double amount;
    private final LoanStatusNotifier notifier;
    private int loanId;
    protected Loan(Customer customer, double amount, LoanStatusNotifier notifier) {
        this.customer=customer; this.amount=amount; this.notifier=notifier;
    }
    public abstract String getLoanType();
    public Customer getCustomer(){return customer;}
    public double getAmount(){return amount;}
    public LoanStatusNotifier getNotifier(){return notifier;}
    public int getLoanId(){return loanId;}
    public void setLoanId(int id){loanId=id;}
}
