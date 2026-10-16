package project;
import java.sql.*;
public final class DatabaseSetup {
    private DatabaseSetup(){}
    public static void initialize() throws SQLException {
        try(Connection c=DatabaseConnection.getConnection(); Statement s=c.createStatement()) {
            s.execute("PRAGMA foreign_keys=ON");
            s.execute("CREATE TABLE IF NOT EXISTS Customer(CustomerID TEXT PRIMARY KEY, Name TEXT NOT NULL, CustomerType TEXT NOT NULL, KYCVerified INTEGER NOT NULL, CreditScore INTEGER NOT NULL, MonthlyIncome REAL NOT NULL, ExistingMonthlyDebt REAL NOT NULL, CreatedAt TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            s.execute("CREATE TABLE IF NOT EXISTS LoanRequest(LoanID INTEGER PRIMARY KEY AUTOINCREMENT, CustomerID TEXT NOT NULL REFERENCES Customer(CustomerID), LoanType TEXT NOT NULL, Amount REAL NOT NULL CHECK(Amount>0), Status TEXT NOT NULL, ApprovedBy TEXT, CreditRisk TEXT NOT NULL, FraudFlag INTEGER NOT NULL DEFAULT 0, Channel TEXT NOT NULL DEFAULT 'APP', CreatedAt TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, ApprovalDueAt TEXT NOT NULL, UpdatedAt TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, Version INTEGER NOT NULL DEFAULT 0)");
            s.execute("CREATE TABLE IF NOT EXISTS AuditLog(AuditID INTEGER PRIMARY KEY AUTOINCREMENT, Actor TEXT NOT NULL, Action TEXT NOT NULL, LoanID INTEGER, Details TEXT NOT NULL, CreatedAt TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            s.execute("CREATE TABLE IF NOT EXISTS LoginAttempts(AttemptID INTEGER PRIMARY KEY AUTOINCREMENT, Username TEXT NOT NULL, Success INTEGER NOT NULL, CreatedAt TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            s.execute("CREATE TABLE IF NOT EXISTS ApplicationSnapshot(SnapshotID INTEGER PRIMARY KEY AUTOINCREMENT, LoanID INTEGER NOT NULL REFERENCES LoanRequest(LoanID), Snapshot TEXT NOT NULL, CreatedAt TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            // Migrate an older SBLAS.db in place; existing Customer/LoanRequest rows are preserved.
            ensureColumn(c, "Customer", "CreditScore", "INTEGER NOT NULL DEFAULT 700");
            ensureColumn(c, "Customer", "MonthlyIncome", "REAL NOT NULL DEFAULT 1");
            ensureColumn(c, "Customer", "ExistingMonthlyDebt", "REAL NOT NULL DEFAULT 0");
            ensureColumn(c, "Customer", "CreatedAt", "TEXT NOT NULL DEFAULT 'legacy'");
            ensureColumn(c, "LoanRequest", "CreditRisk", "TEXT NOT NULL DEFAULT 'MEDIUM'");
            ensureColumn(c, "LoanRequest", "FraudFlag", "INTEGER NOT NULL DEFAULT 0");
            ensureColumn(c, "LoanRequest", "CreatedAt", "TEXT NOT NULL DEFAULT 'legacy'");
            ensureColumn(c, "LoanRequest", "ApprovalDueAt", "TEXT NOT NULL DEFAULT '2099-01-01T00:00:00Z'");
            ensureColumn(c, "LoanRequest", "UpdatedAt", "TEXT NOT NULL DEFAULT 'legacy'");
            ensureColumn(c, "LoanRequest", "Version", "INTEGER NOT NULL DEFAULT 0");
            ensureColumn(c, "LoanRequest", "Channel", "TEXT NOT NULL DEFAULT 'APP'");
            s.execute("UPDATE LoanRequest SET ApprovedBy='Branch Manager' WHERE Status='PENDING' AND (ApprovedBy IS NULL OR ApprovedBy='')");
            s.execute("UPDATE LoanRequest SET ApprovalDueAt=strftime('%Y-%m-%dT%H:%M:%fZ','now','+1 day') WHERE Status='PENDING' AND ApprovalDueAt='2099-01-01T00:00:00Z'");
            s.execute("CREATE INDEX IF NOT EXISTS idx_loan_status_due ON LoanRequest(Status,ApprovalDueAt)");
        }
    }
    private static void ensureColumn(Connection c,String table,String column,String definition)throws SQLException {
        boolean found=false;
        try(Statement st=c.createStatement();ResultSet rs=st.executeQuery("PRAGMA table_info("+table+")")) {
            while(rs.next()) if(column.equalsIgnoreCase(rs.getString("name"))) {found=true;break;}
        }
        if(!found) try(Statement st=c.createStatement()) { st.execute("ALTER TABLE "+table+" ADD COLUMN "+column+" "+definition); }
    }
}
