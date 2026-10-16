package project;
public class LoanStatusNotifier {
    private final NotificationChannel channel;
    public LoanStatusNotifier(NotificationChannel channel){this.channel=channel;}
    public String getChannelName(){if(channel instanceof EmailChannel)return "EMAIL";if(channel instanceof SMSChannel)return "SMS";return "APP";}
    public void notifyStatus(Loan loan,String status){channel.send("Loan #"+loan.getLoanId()+" for "+loan.getCustomer().getName()+" is "+status+". Type: "+loan.getLoanType()+", amount: INR "+String.format("%.2f",loan.getAmount()));}
}
