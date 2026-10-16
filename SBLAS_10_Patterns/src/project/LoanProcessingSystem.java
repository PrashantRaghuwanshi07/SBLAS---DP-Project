package project;
// Pattern 2: Singleton
public final class LoanProcessingSystem {
    private static final LoanProcessingSystem INSTANCE=new LoanProcessingSystem();
    private final LoanDAO dao=new LoanDAO();
    private final LoanWorkflowMediator mediator=new LoanWorkflowMediator(dao,new StandardLoanProcessing(new StandardCreditAssessmentStrategy()));
    private LoanProcessingSystem(){}
    public static LoanProcessingSystem getInstance(){return INSTANCE;}
    public int processLoan(Customer customer,Loan loan,boolean fraudFlag)throws Exception{return mediator.submit(customer,loan,fraudFlag);}
}
