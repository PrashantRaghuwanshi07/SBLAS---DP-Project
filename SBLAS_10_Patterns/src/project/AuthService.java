package project;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
public final class AuthService {
    private AuthService(){}
    private record Account(String role,String salt,String hash){}
    // Lab demo accounts only. Replace with a proper identity provider and per-user stored salts in production.
    private static final Map<String,Account> ACCOUNTS=Map.of(
      "customer",new Account("CUSTOMER","U0JMQVMtbGFiLWN1c3RvbWVy","pkb6F9FHBDtutQ5inlJrZ0ZA3oa1GGDDC3nZ7cuUlpc="),
      "branch",new Account("Branch Manager","U0JMQVMtbGFiLWJyYW5jaA==","QLGSjcNSwimbJZb5EJmZdUrwUBqAF4CvtkAeB8pbJSw="),
      "regional",new Account("Regional Manager","U0JMQVMtbGFiLXJlZ2lvbmFs","3k7SdtfBTtHyMeA12zJ5yUZ58wfnylrYH7N6C9PGpR4="),
      "committee",new Account("Credit Committee","U0JMQVMtbGFiLWNvbW1pdHRlZQ==","meHe4JMM9CyHj1TVmrSkirbGNspZXYH3wnkz8U8KK7k="),
      "riskdirector",new Account("Risk Director","U0JMQVMtbGFiLXJpc2tkaXJlY3Rvcg==","n0Bvk0st34lS+ztKT8zITl7q8xHgC9hxDKzFex0/J3Y=")
    );
    public static boolean authenticate(String user,String pass){return role(user,pass)!=null;}
    public static String role(String user,String pass){
        if(user==null||pass==null)return null; Account a=ACCOUNTS.get(user.trim().toLowerCase());if(a==null)return null;
        try{PBEKeySpec spec=new PBEKeySpec(pass.toCharArray(),Base64.getDecoder().decode(a.salt()),120000,256);byte[] derived=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();spec.clearPassword();byte[] expected=Base64.getDecoder().decode(a.hash());return MessageDigest.isEqual(derived,expected)?a.role():null;}catch(Exception e){throw new IllegalStateException("Password verification unavailable",e);}
    }
}
