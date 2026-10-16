package project;
import java.sql.SQLException;
import java.time.Instant;
// Pattern 8: Mediator coordinates services without making each service call all others.
public class LoanWorkflowMediator {
    private final LoanDAO dao;
    private final LoanProcessingTemplate template;
    public LoanWorkflowMediator(LoanDAO dao,LoanProcessingTemplate template){this.dao=dao;this.template=template;}
    public int submit(Customer customer,Loan loan,boolean fraudFlag)throws Exception {
        CreditAssessment assessment=template.process(customer,loan.getAmount());
        if(assessment.riskLevel().equals("HIGH")) fraudFlag=true;
        String initialAuthority=new ApprovalChain().initialAuthority(loan.getAmount());
        int id=dao.saveApplication(customer,loan,assessment,fraudFlag,Instant.now().plusSeconds(Long.getLong("sblas.deadline.seconds",604800L)));
        dao.assignAuthority(id,initialAuthority);
        LoanApplicationMemento memento=new LoanApplicationMemento(customer.getCustomerId(),customer.getName(),customer.getCustomerType(),loan.getLoanType(),loan.getAmount(),customer.getCreditScore(),customer.getMonthlyIncome(),customer.getExistingMonthlyDebt(),Instant.now().toString());
        LoanApplicationHistory history=new LoanApplicationHistory();history.save(memento);dao.saveSnapshot(id,memento.toString()+"; risk="+assessment.riskLevel()+"; assigned="+initialAuthority);
        if(fraudFlag)dao.audit("SYSTEM","FRAUD_REVIEW_FLAGGED",id,"Rule-based flag: unusual amount/risk; manual review required.");
        return id;
    }
}
