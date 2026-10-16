package project;
public class LoanPortalProxy {
    private final LoanProcessingSystem realSystem=LoanProcessingSystem.getInstance();
    private final LoanDAO dao=new LoanDAO();
    public int submitLoan(String username,String password,Customer customer,Loan loan,boolean fraudFlag)throws Exception {
        if(dao.isLoginLocked(username))throw new SecurityException("Too many failed attempts. Try again after 10 minutes.");
        if(!AuthService.authenticate(username,password)){dao.logLogin(username,false);throw new SecurityException("Invalid demo credentials.");}
        dao.logLogin(username,true);
        if(!"CUSTOMER".equals(AuthService.role(username,password)))throw new SecurityException("Only a customer account can submit an application.");
        if(!customer.isKycVerified())throw new SecurityException("KYC is not verified. Application blocked.");
        if(!customer.getCustomerId().matches("[A-Za-z0-9_-]{4,32}"))throw new IllegalArgumentException("Customer ID must be 4–32 letters, numbers, _ or -.");
        if(customer.getCreditScore()<300||customer.getCreditScore()>900)throw new IllegalArgumentException("Credit score must be between 300 and 900.");
        if(customer.getMonthlyIncome()<=0||customer.getExistingMonthlyDebt()<0)throw new IllegalArgumentException("Income/debt values are invalid.");
        return realSystem.processLoan(customer,loan,fraudFlag);
    }
}
