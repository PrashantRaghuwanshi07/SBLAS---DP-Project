package project;
// Pattern 4: Chain of Responsibility. Each node handles amounts within its authority limit or forwards onward.
public abstract class LoanApprover {
    protected LoanApprover nextApprover;
    public LoanApprover setNextApprover(LoanApprover next){this.nextApprover=next;return next;}
    public abstract String authorityName();
    protected abstract double maximumAmount();
    public String routeAmount(double amount){if(amount<=maximumAmount())return authorityName();if(nextApprover!=null)return nextApprover.routeAmount(amount);throw new IllegalArgumentException("No approval authority for this amount.");}
}
