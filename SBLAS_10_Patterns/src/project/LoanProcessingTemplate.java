package project;
// Pattern 9: Template Method
public abstract class LoanProcessingTemplate {
    public final CreditAssessment process(Customer customer,double amount){
        validate(customer,amount);
        verifyKyc(customer);
        CreditAssessment result=assessCredit(customer,amount);
        runFraudChecks(customer,amount);
        return result;
    }
    protected void validate(Customer c,double amount){if(c==null||amount<=0)throw new IllegalArgumentException("Invalid application.");}
    protected void verifyKyc(Customer c){if(!c.isKycVerified())throw new SecurityException("KYC verification is required.");}
    protected abstract CreditAssessment assessCredit(Customer customer,double amount);
    protected void runFraudChecks(Customer c,double amount){if(amount>c.getMonthlyIncome()*60)throw new IllegalArgumentException("Application flagged: amount is unusually high relative to income. Manual review is required.");}
}
