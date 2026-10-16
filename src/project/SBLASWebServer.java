package project;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.*;

public class SBLASWebServer {
    private static final LoanDAO DAO=new LoanDAO();
    public static void main(String[] args)throws Exception {
        DatabaseSetup.initialize();
        EscalationService escalation=new EscalationService();escalation.start();
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",8080),0);
        server.createContext("/",x->{
            String path=x.getRequestURI().getPath();
            switch(path){
                case "/", "/index.html" -> serveStatic(x,"index.html","text/html; charset=utf-8");
                case "/customer", "/customer.html" -> serveStatic(x,"customer.html","text/html; charset=utf-8");
                case "/staff", "/staff.html" -> serveStatic(x,"staff.html","text/html; charset=utf-8");
                default -> send(x,404,"Page not found","text/plain; charset=utf-8");
            }
        });
        server.createContext("/style.css",x->serveStatic(x,"style.css","text/css; charset=utf-8"));
        server.createContext("/script.js",x->serveStatic(x,"script.js","application/javascript; charset=utf-8"));
        server.createContext("/customer.js",x->serveStatic(x,"customer.js","application/javascript; charset=utf-8"));
        server.createContext("/staff.js",x->serveStatic(x,"staff.js","application/javascript; charset=utf-8"));
        server.createContext("/api/apply",SBLASWebServer::apply);
        server.createContext("/api/loans",x->protectedList(x,false));
        server.createContext("/api/audit",x->protectedList(x,true));
        server.createContext("/api/decision",SBLASWebServer::decision);
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(8));server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(()->{server.stop(1);escalation.close();}));
        System.out.println("SBLAS running at http://127.0.0.1:8080");
        System.out.println("DEMO customer: customer / Demo@1234 | branch: branch / Branch@1234 | regional: regional / Regional@1234 | committee: committee / Committee@1234 | riskdirector: riskdirector / RiskDirector@1234");
        System.out.println("Escalation deadline seconds="+Long.getLong("sblas.deadline.seconds",604800L)+" (override with -Dsblas.deadline.seconds=60 for a demo)");
    }
    private static void serveStatic(HttpExchange x,String file,String type)throws IOException{
        try{if(!x.getRequestMethod().equals("GET")){send(x,405,"Method not allowed", "text/plain");return;}Path p=Path.of("Frontend",file);if(!Files.exists(p))p=Path.of("frontend",file);if(!Files.exists(p)){send(x,404,"Frontend file not found: "+file,"text/plain");return;}sendBytes(x,200,Files.readAllBytes(p),type);}finally{x.close();}
    }
    private static void apply(HttpExchange x)throws IOException{
        try{
            if(!x.getRequestMethod().equals("POST")){sendJson(x,405,Map.of("success",false,"message","POST required"));return;}
            Map<String,String>d=parseForm(new String(x.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
            String user=d.get("username"),pass=d.get("password");
            String type=required(d,"customerType"),loanType=required(d,"loanType"),channel=d.getOrDefault("channel","APP").toUpperCase(Locale.ROOT);
            boolean kyc="true".equalsIgnoreCase(d.getOrDefault("kycVerified","false"));
            LoanApplicationBuilder b=new LoanApplicationBuilder().customerId(required(d,"customerId")).name(required(d,"name")).customerType(type).loanType(loanType).channel(channel).kycVerified(kyc).creditScore(integer(d,"creditScore")).income(number(d,"income")).debt(number(d,"debt")).amount(number(d,"amount"));
            Customer c=b.buildCustomer();LoanStatusNotifier notifier=b.buildNotifier();LoanFactory factory=type.equals("Salaried")?new SalariedCustomerLoanFactory():new SelfEmployedCustomerLoanFactory();Loan loan=factory.createLoan(loanType,c,number(d,"amount"),notifier);
            if(loan==null){sendJson(x,400,Map.of("success",false,"message","That loan type is not available for this customer category."));return;}
            CreditAssessment assessment=new StandardCreditAssessmentStrategy().assess(c,loan.getAmount());
            boolean fraudFlag=assessment.riskLevel().equals("HIGH")||number(d,"amount")>c.getMonthlyIncome()*36;
            int id=new LoanPortalProxy().submitLoan(user,pass,c,loan,fraudFlag);
            loan.getNotifier().notifyStatus(loan,"PENDING REVIEW");
            sendJson(x,200,Map.of("success",true,"message","Application submitted. It is pending human approval.","loanId",id,"risk",assessment.riskLevel(),"debtToIncomePercent",assessment.debtToIncome(),"fraudFlag",fraudFlag,"approvalDueAt",java.time.Instant.now().plusSeconds(Long.getLong("sblas.deadline.seconds",604800L)).toString()));
        }catch(SecurityException e){sendJson(x,401,Map.of("success",false,"message",e.getMessage()));}
        catch(IllegalArgumentException e){sendJson(x,400,Map.of("success",false,"message",e.getMessage()));}
        catch(Exception e){e.printStackTrace();sendJson(x,500,Map.of("success",false,"message","Application could not be processed. Check server console."));}
        finally{x.close();}
    }
    private static void protectedList(HttpExchange x,boolean audit)throws IOException{
        try{
            String u=x.getRequestHeaders().getFirst("X-Username"),p=x.getRequestHeaders().getFirst("X-Password");String role=AuthService.role(u,p);
            if(role==null||role.equals("CUSTOMER")){sendJson(x,401,Map.of("success",false,"message","Valid staff demo credentials required."));return;}
            Object data=audit?DAO.auditLog():DAO.listLoans();sendJson(x,200,Map.of("success",true,"data",data));
        }catch(Exception e){sendJson(x,500,Map.of("success",false,"message","Could not load records."));}finally{x.close();}
    }
    private static void decision(HttpExchange x)throws IOException{
        try{
            if(!x.getRequestMethod().equals("POST")){sendJson(x,405,Map.of("success",false,"message","POST required"));return;}
            Map<String,String>d=parseForm(new String(x.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));String user=d.get("username"),pass=d.get("password"),role=AuthService.role(user,pass);
            if(role==null||role.equals("CUSTOMER")){DAO.logLogin(user,false);sendJson(x,401,Map.of("success",false,"message","Staff authentication failed."));return;}
            DAO.logLogin(user,true);int id=Integer.parseInt(required(d,"loanId"));String status=required(d,"status").toUpperCase(Locale.ROOT);if(!Set.of("APPROVED","REJECTED").contains(status))throw new IllegalArgumentException("Invalid decision.");
            Map<String,Object> loan=null;for(Map<String,Object> row:DAO.listLoans())if(((Number)row.get("LoanID")).intValue()==id){loan=row;break;}
            if(loan==null){sendJson(x,404,Map.of("success",false,"message","Loan not found."));return;}
            String assigned=String.valueOf(loan.get("ApprovedBy"));if(!assigned.equals(role)){sendJson(x,403,Map.of("success",false,"message","This loan is assigned to "+assigned+", not your role."));return;}
            boolean changed=DAO.decide(id,status,role,"Decision submitted by authenticated authority");
            if(!changed){sendJson(x,409,Map.of("success",false,"message","Loan was already decided or updated by another user."));return;}
            Customer customer=new Customer(String.valueOf(loan.get("CustomerID")),String.valueOf(loan.get("Name")),String.valueOf(loan.get("CustomerType")),((Number)loan.get("KYCVerified")).intValue()==1,((Number)loan.get("CreditScore")).intValue(),((Number)loan.get("MonthlyIncome")).doubleValue(),((Number)loan.get("ExistingMonthlyDebt")).doubleValue());
            String channelName=String.valueOf(loan.get("Channel"));NotificationChannel channel=switch(channelName){case "EMAIL"->new EmailChannel();case "SMS"->new SMSChannel();default->new AppChannel();};
            LoanStatusNotifier notifier=new LoanStatusNotifier(channel);String loanType=String.valueOf(loan.get("LoanType"));double amount=((Number)loan.get("Amount")).doubleValue();
            Loan notificationLoan=new Loan(customer,amount,notifier){public String getLoanType(){return loanType;}};notificationLoan.setLoanId(id);notifier.notifyStatus(notificationLoan,status);
            sendJson(x,200,Map.of("success",true,"message","Loan #"+id+" marked "+status+". A demo notification was printed to the server console."));
        }catch(IllegalArgumentException e){sendJson(x,400,Map.of("success",false,"message",e.getMessage()==null?"Invalid request":e.getMessage()));}
        catch(Exception e){e.printStackTrace();sendJson(x,500,Map.of("success",false,"message","Could not save decision."));}finally{x.close();}
    }
    private static Map<String,String> parseForm(String s){Map<String,String> m=new HashMap<>();for(String pair:s.split("&")){if(pair.isEmpty())continue;String[]kv=pair.split("=",2);m.put(decode(kv[0]),kv.length>1?decode(kv[1]):"");}return m;}
    private static String decode(String s){return URLDecoder.decode(s,StandardCharsets.UTF_8);}
    private static String required(Map<String,String>m,String k){String v=m.get(k);if(v==null||v.trim().isEmpty())throw new IllegalArgumentException("Missing field: "+k);return v.trim();}
    private static int integer(Map<String,String>m,String k){return Integer.parseInt(required(m,k));}
    private static double number(Map<String,String>m,String k){double v=Double.parseDouble(required(m,k));if(!Double.isFinite(v))throw new IllegalArgumentException("Invalid numeric value: "+k);return v;}
    private static void sendJson(HttpExchange x,int status,Object obj)throws IOException{send(x,status,toJson(obj),"application/json; charset=utf-8");}
    private static void send(HttpExchange x,int status,String s,String type)throws IOException{sendBytes(x,status,s.getBytes(StandardCharsets.UTF_8),type);}
    private static void sendBytes(HttpExchange x,int status,byte[]b,String type)throws IOException{x.getResponseHeaders().set("Content-Type",type);x.getResponseHeaders().set("X-Content-Type-Options","nosniff");x.getResponseHeaders().set("Cache-Control","no-store");x.getResponseHeaders().set("Content-Security-Policy","default-src 'self'; style-src 'self' 'unsafe-inline'; script-src 'self'; connect-src 'self'; object-src 'none'; frame-ancestors 'none'");x.sendResponseHeaders(status,b.length);try(OutputStream out=x.getResponseBody()){out.write(b);}}
    @SuppressWarnings("unchecked") private static String toJson(Object o){if(o==null)return "null";if(o instanceof String s)return "\""+escape(s)+"\"";if(o instanceof Number||o instanceof Boolean)return o.toString();if(o instanceof Map<?,?> m){StringJoiner j=new StringJoiner(",","{","}");m.forEach((k,v)->j.add(toJson(String.valueOf(k))+":"+toJson(v)));return j.toString();}if(o instanceof Iterable<?> it){StringJoiner j=new StringJoiner(",","[","]");for(Object v:it)j.add(toJson(v));return j.toString();}return toJson(o.toString());}
    private static String escape(String s){return s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r").replace("\t","\\t");}
}
