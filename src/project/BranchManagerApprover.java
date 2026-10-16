package project;
public class BranchManagerApprover extends LoanApprover { public String authorityName(){return "Branch Manager";} protected double maximumAmount(){return 1_000_000;} }
