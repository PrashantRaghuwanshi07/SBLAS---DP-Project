package project;
public class CreditCommitteeApprover extends LoanApprover { public String authorityName(){return "Credit Committee";} protected double maximumAmount(){return 10_000_000;} }
