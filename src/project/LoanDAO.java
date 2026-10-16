package project;
import java.sql.*;
import java.time.Instant;
import java.util.*;
public class LoanDAO {
    public int saveApplication(Customer customer, Loan loan, CreditAssessment assessment, boolean fraudFlag, Instant dueAt) throws SQLException {
        try(Connection c=DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                try(PreparedStatement p=c.prepareStatement("INSERT INTO Customer(CustomerID,Name,CustomerType,KYCVerified,CreditScore,MonthlyIncome,ExistingMonthlyDebt) VALUES(?,?,?,?,?,?,?) ON CONFLICT(CustomerID) DO UPDATE SET Name=excluded.Name,CustomerType=excluded.CustomerType,KYCVerified=excluded.KYCVerified,CreditScore=excluded.CreditScore,MonthlyIncome=excluded.MonthlyIncome,ExistingMonthlyDebt=excluded.ExistingMonthlyDebt")) {
                    p.setString(1,customer.getCustomerId());p.setString(2,customer.getName());p.setString(3,customer.getCustomerType());p.setInt(4,customer.isKycVerified()?1:0);p.setInt(5,customer.getCreditScore());p.setDouble(6,customer.getMonthlyIncome());p.setDouble(7,customer.getExistingMonthlyDebt());p.executeUpdate();
                }
                int id;
                try(PreparedStatement p=c.prepareStatement("INSERT INTO LoanRequest(CustomerID,LoanType,Amount,Status,ApprovedBy,CreditRisk,FraudFlag,ApprovalDueAt,Channel) VALUES(?,?,?,'PENDING','Branch Manager',?,?,?,?)",Statement.RETURN_GENERATED_KEYS)) {
                    p.setString(1,customer.getCustomerId());p.setString(2,loan.getLoanType());p.setDouble(3,loan.getAmount());p.setString(4,assessment.riskLevel());p.setInt(5,fraudFlag?1:0);p.setString(6,dueAt.toString());p.setString(7,loan.getNotifier().getChannelName());p.executeUpdate();
                    try(ResultSet r=p.getGeneratedKeys()){if(!r.next())throw new SQLException("No loan ID returned");id=r.getInt(1);}
                }
                audit(c,"SYSTEM","APPLICATION_CREATED",id,"risk="+assessment.riskLevel()+", fraudFlag="+fraudFlag);
                c.commit();loan.setLoanId(id);return id;
            } catch(Exception e){c.rollback();if(e instanceof SQLException se)throw se;throw new SQLException(e);}
        }
    }
    public boolean decide(int id,String status,String approver,String note) throws SQLException {
        if(!Set.of("APPROVED","REJECTED","REFERRED").contains(status))throw new IllegalArgumentException("Invalid status");
        try(Connection c=DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try(PreparedStatement p=c.prepareStatement("UPDATE LoanRequest SET Status=?,ApprovedBy=?,UpdatedAt=CURRENT_TIMESTAMP,Version=Version+1 WHERE LoanID=? AND Status='PENDING' AND ApprovedBy=?")) {
                p.setString(1,status);p.setString(2,approver);p.setInt(3,id);p.setString(4,approver);int n=p.executeUpdate();
                if(n==1)audit(c,approver,"DECISION_"+status,id,note);
                c.commit();return n==1;
            }catch(Exception e){c.rollback();if(e instanceof SQLException se)throw se;throw new SQLException(e);}
        }
    }
    public List<Map<String,Object>> listLoans() throws SQLException {
        String q="SELECT l.LoanID,l.CustomerID,c.Name,c.CustomerType,l.LoanType,l.Amount,l.Status,COALESCE(l.ApprovedBy,'Unassigned') ApprovedBy,l.CreditRisk,l.FraudFlag,l.CreatedAt,l.ApprovalDueAt,l.Channel,c.KYCVerified,c.CreditScore,c.MonthlyIncome,c.ExistingMonthlyDebt FROM LoanRequest l JOIN Customer c ON c.CustomerID=l.CustomerID ORDER BY l.LoanID DESC";
        List<Map<String,Object>> out=new ArrayList<>();try(Connection c=DatabaseConnection.getConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery(q)){
            while(r.next()){Map<String,Object> m=new LinkedHashMap<>();for(String k:List.of("LoanID","CustomerID","Name","CustomerType","LoanType","Amount","Status","ApprovedBy","CreditRisk","FraudFlag","CreatedAt","ApprovalDueAt","Channel","KYCVerified","CreditScore","MonthlyIncome","ExistingMonthlyDebt"))m.put(k,r.getObject(k));out.add(m);}
        }return out;
    }
    public List<Map<String,Object>> pendingExpired(String now) throws SQLException {
        List<Map<String,Object>> out=new ArrayList<>();try(Connection c=DatabaseConnection.getConnection();PreparedStatement p=c.prepareStatement("SELECT LoanID,ApprovedBy,Version FROM LoanRequest WHERE Status='PENDING' AND ApprovalDueAt<=?")){p.setString(1,now);try(ResultSet r=p.executeQuery()){while(r.next()){Map<String,Object> m=new HashMap<>();m.put("LoanID",r.getInt(1));m.put("ApprovedBy",r.getString(2));m.put("Version",r.getInt(3));out.add(m);}}}return out;
    }
    public boolean escalate(int id,int expectedVersion,String nextAuthority,String reason) throws SQLException {
        try(Connection c=DatabaseConnection.getConnection()) {c.setAutoCommit(false);try(PreparedStatement p=c.prepareStatement("UPDATE LoanRequest SET ApprovedBy=?,ApprovalDueAt=?,UpdatedAt=CURRENT_TIMESTAMP,Version=Version+1 WHERE LoanID=? AND Status='PENDING' AND Version=?")){
            p.setString(1,nextAuthority);p.setString(2,Instant.now().plusSeconds(Long.getLong("sblas.deadline.seconds",604800L)).toString());p.setInt(3,id);p.setInt(4,expectedVersion);int n=p.executeUpdate();if(n==1)audit(c,"ESCALATION_SERVICE","AUTO_ESCALATED",id,reason+"; next="+nextAuthority);c.commit();return n==1;
        }catch(Exception e){c.rollback();if(e instanceof SQLException se)throw se;throw new SQLException(e);}}
    }
    public void saveSnapshot(int loanId,String snapshot) throws SQLException {try(Connection c=DatabaseConnection.getConnection();PreparedStatement p=c.prepareStatement("INSERT INTO ApplicationSnapshot(LoanID,Snapshot) VALUES(?,?)")){p.setInt(1,loanId);p.setString(2,snapshot);p.executeUpdate();}}
    public List<Map<String,Object>> auditLog() throws SQLException {List<Map<String,Object>> out=new ArrayList<>();try(Connection c=DatabaseConnection.getConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT AuditID,Actor,Action,LoanID,Details,CreatedAt FROM AuditLog ORDER BY AuditID DESC LIMIT 100")){while(r.next()){Map<String,Object> m=new LinkedHashMap<>();for(String k:List.of("AuditID","Actor","Action","LoanID","Details","CreatedAt"))m.put(k,r.getObject(k));out.add(m);}}return out;}
    public boolean isLoginLocked(String user) { if(user==null)return false; try(Connection c=DatabaseConnection.getConnection();PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM LoginAttempts WHERE Username=? AND Success=0 AND CreatedAt>=datetime('now','-10 minutes') AND AttemptID>COALESCE((SELECT MAX(AttemptID) FROM LoginAttempts WHERE Username=? AND Success=1),0)")){p.setString(1,user);p.setString(2,user);try(ResultSet r=p.executeQuery()){return r.next()&&r.getInt(1)>=5;}}catch(SQLException e){return false;} }
    public void logLogin(String user,boolean success) {try(Connection c=DatabaseConnection.getConnection();PreparedStatement p=c.prepareStatement("INSERT INTO LoginAttempts(Username,Success) VALUES(?,?)")){p.setString(1,user==null?"unknown":user.substring(0,Math.min(80,user.length())));p.setInt(2,success?1:0);p.executeUpdate();}catch(SQLException e){System.err.println("Could not record login attempt: "+e.getMessage());}}
    public void assignAuthority(int id,String authority) throws SQLException { try(Connection c=DatabaseConnection.getConnection();PreparedStatement p=c.prepareStatement("UPDATE LoanRequest SET ApprovedBy=? WHERE LoanID=? AND Status='PENDING'")){p.setString(1,authority);p.setInt(2,id);p.executeUpdate();} }
    public void audit(String actor,String action,Integer id,String details) throws SQLException { try(Connection c=DatabaseConnection.getConnection()){audit(c,actor,action,id,details);} }
    private static void audit(Connection c,String actor,String action,Integer id,String details)throws SQLException{try(PreparedStatement p=c.prepareStatement("INSERT INTO AuditLog(Actor,Action,LoanID,Details) VALUES(?,?,?,?)")){p.setString(1,actor);p.setString(2,action);if(id==null)p.setNull(3,Types.INTEGER);else p.setInt(3,id);p.setString(4,details==null?"":details.substring(0,Math.min(500,details.length())));p.executeUpdate();}}
}
