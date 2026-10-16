package project;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
// Memento caretaker: snapshots are for draft recovery, not to reverse final audit/approval decisions.
public class LoanApplicationHistory {
    private final Deque<LoanApplicationMemento> snapshots=new ArrayDeque<>();
    public void save(LoanApplicationMemento m){if(snapshots.size()==10)snapshots.removeFirst();snapshots.addLast(m);}
    public LoanApplicationMemento latest(){return snapshots.peekLast();}
    public static LoanApplicationMemento snapshot(LoanApplicationBuilder b,String id,String name,String type,double amount,int score,double income,double debt){return new LoanApplicationMemento(id,name,type,b.getLoanType(),amount,score,income,debt,Instant.now().toString());}
}
