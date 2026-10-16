package project;
public class StandardCreditAssessmentStrategy implements CreditAssessmentStrategy {
    public CreditAssessment assess(Customer c,double amount){
        double estimatedEmi=amount*0.015; // illustrative monthly estimate for lab demo, not a bank quote
        double dti=(c.getExistingMonthlyDebt()+estimatedEmi)/Math.max(1,c.getMonthlyIncome());
        String risk;String why;
        if(c.getCreditScore()<600||dti>0.65){risk="HIGH";why="Low demo credit score or high debt-to-income ratio; manual review required.";}
        else if(c.getCreditScore()<700||dti>0.40){risk="MEDIUM";why="Additional affordability review recommended.";}
        else {risk="LOW";why="Score and illustrative affordability checks are within demo policy.";}
        return new CreditAssessment(risk,c.getCreditScore(),Math.round(dti*10000.0)/100.0,why);
    }
}
