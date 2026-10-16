package project;
import java.time.Instant;
import java.util.concurrent.*;
public class EscalationService implements AutoCloseable {
    private final LoanDAO dao=new LoanDAO(); private final ApprovalChain chain=new ApprovalChain();
    private final ScheduledExecutorService executor=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"sblas-escalation");t.setDaemon(true);return t;});
    public void start(){long poll=Long.getLong("sblas.escalation.poll.seconds",15L);executor.scheduleWithFixedDelay(this::check, poll,poll,TimeUnit.SECONDS);}
    private void check(){try{for(var row:dao.pendingExpired(Instant.now().toString())){int id=(Integer)row.get("LoanID");int version=(Integer)row.get("Version");String current=(String)row.get("ApprovedBy");String next=chain.nextAuthority(current);if(next==null){dao.audit("ESCALATION_SERVICE","ESCALATION_LIMIT_REACHED",id,"Final oversight deadline passed; remains pending for manual governance review.");continue;}if(dao.escalate(id,version,next,"No decision before configured deadline")){System.out.println("Loan #"+id+" automatically escalated to "+next);}}}catch(Exception e){System.err.println("Escalation check failed: "+e.getMessage());}}
    public void close(){executor.shutdownNow();}
}
