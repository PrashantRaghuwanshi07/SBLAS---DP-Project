package project;
public class ApprovalChain {
    private final LoanApprover branch=new BranchManagerApprover();
    public ApprovalChain(){branch.setNextApprover(new RegionalManagerApprover()).setNextApprover(new CreditCommitteeApprover());}
    public String initialAuthority(double amount){return branch.routeAmount(amount);}
    public String nextAuthority(String current){return switch(current==null?"":current){case "Branch Manager"->"Regional Manager";case "Regional Manager"->"Credit Committee";case "Credit Committee"->"Risk Director";default->null;};}
}
