import java.util.Properties;

public class MyIdentity {

    public static void setIdentity(Properties prop) {
      prop.setProperty("database", "csci3901");
      prop.setProperty("user", "root");  
      prop.setProperty("password", "19970101"); 
    }
}
