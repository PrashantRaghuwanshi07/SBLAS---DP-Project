package project;
// Pattern 6: Builder
public class LoanApplicationBuilder {
    private String customerId,name,customerType,loanType,channel="APP";
    private boolean kycVerified;
    private int creditScore=700;
    private double income,debt,amount;
    public LoanApplicationBuilder customerId(String v){customerId=v;return this;}
    public LoanApplicationBuilder name(String v){name=v;return this;}
    public LoanApplicationBuilder customerType(String v){customerType=v;return this;}
    public LoanApplicationBuilder loanType(String v){loanType=v;return this;}
    public LoanApplicationBuilder channel(String v){channel=v;return this;}
    public LoanApplicationBuilder kycVerified(boolean v){kycVerified=v;return this;}
    public LoanApplicationBuilder creditScore(int v){creditScore=v;return this;}
    public LoanApplicationBuilder income(double v){income=v;return this;}
    public LoanApplicationBuilder debt(double v){debt=v;return this;}
    public LoanApplicationBuilder amount(double v){amount=v;return this;}
    public Customer buildCustomer(){
        if(blank(customerId)||blank(name)||blank(customerType))throw new IllegalArgumentException("Customer ID, name and type are required.");
        if(!customerType.equals("Salaried")&&!customerType.equals("Self-Employed"))throw new IllegalArgumentException("Invalid customer type.");
        if(amount<=0||amount>10000000)throw new IllegalArgumentException("Loan amount must be greater than zero and no more than ₹1 crore.");
        return new Customer(customerId.trim(),name.trim(),customerType,kycVerified,creditScore,income,debt);
    }
    public LoanStatusNotifier buildNotifier(){return new LoanStatusNotifier(switch(channel){case "EMAIL"->new EmailChannel();case "SMS"->new SMSChannel();default->new AppChannel();});}
    private boolean blank(String s){return s==null||s.trim().isEmpty();}
    public String getLoanType(){return loanType;}
}
