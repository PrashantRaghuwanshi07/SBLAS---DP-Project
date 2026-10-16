package project;

public class Customer {
    private final String customerId, name, customerType;
    private final boolean kycVerified;
    private final int creditScore;
    private final double monthlyIncome, existingMonthlyDebt;

    public Customer(String customerId, String name, String customerType, boolean kycVerified,
                    int creditScore, double monthlyIncome, double existingMonthlyDebt) {
        this.customerId = customerId; this.name = name; this.customerType = customerType;
        this.kycVerified = kycVerified; this.creditScore = creditScore;
        this.monthlyIncome = monthlyIncome; this.existingMonthlyDebt = existingMonthlyDebt;
    }
    public String getCustomerId(){ return customerId; }
    public String getName(){ return name; }
    public String getCustomerType(){ return customerType; }
    public boolean isKycVerified(){ return kycVerified; }
    public int getCreditScore(){ return creditScore; }
    public double getMonthlyIncome(){ return monthlyIncome; }
    public double getExistingMonthlyDebt(){ return existingMonthlyDebt; }
}
