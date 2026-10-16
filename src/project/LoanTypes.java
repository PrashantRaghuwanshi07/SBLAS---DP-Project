package project;

class SalariedPersonalLoan extends Loan { SalariedPersonalLoan(Customer c,double a,LoanStatusNotifier n){super(c,a,n);} public String getLoanType(){return "Personal Loan";} }
class SalariedHomeLoan extends Loan { SalariedHomeLoan(Customer c,double a,LoanStatusNotifier n){super(c,a,n);} public String getLoanType(){return "Home Loan";} }
class SalariedCarLoan extends Loan { SalariedCarLoan(Customer c,double a,LoanStatusNotifier n){super(c,a,n);} public String getLoanType(){return "Car Loan";} }
class SelfEmployedBusinessLoan extends Loan { SelfEmployedBusinessLoan(Customer c,double a,LoanStatusNotifier n){super(c,a,n);} public String getLoanType(){return "Business Loan";} }
class SelfEmployedWorkingCapitalLoan extends Loan { SelfEmployedWorkingCapitalLoan(Customer c,double a,LoanStatusNotifier n){super(c,a,n);} public String getLoanType(){return "Working Capital Loan";} }
class SelfEmployedHomeLoan extends Loan { SelfEmployedHomeLoan(Customer c,double a,LoanStatusNotifier n){super(c,a,n);} public String getLoanType(){return "Home Loan";} }
