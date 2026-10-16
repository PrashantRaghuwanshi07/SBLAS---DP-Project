package project;
public class StandardLoanProcessing extends LoanProcessingTemplate {
    private final CreditAssessmentStrategy strategy;
    public StandardLoanProcessing(CreditAssessmentStrategy strategy){this.strategy=strategy;}
    protected CreditAssessment assessCredit(Customer c,double amount){return strategy.assess(c,amount);}
}
