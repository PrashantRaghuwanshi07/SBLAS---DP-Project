package project;
import java.sql.Connection;
import java.sql.DriverManager;
import java.nio.file.Files;
import java.nio.file.Path;
public final class DatabaseConnection {
    private DatabaseConnection(){}
    public static Connection getConnection() throws java.sql.SQLException {
        String configured=System.getProperty("sblas.db", "data/SBLAS.db");
        Path path=Path.of(configured).toAbsolutePath();
        try { Files.createDirectories(path.getParent()); } catch(Exception e){throw new java.sql.SQLException("Cannot create database directory",e);}
        return DriverManager.getConnection("jdbc:sqlite:"+path);
    }
}
