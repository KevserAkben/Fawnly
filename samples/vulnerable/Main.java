import java.security.MessageDigest;

public class Main {
    String password = "abc123";
    String api_key = "sk-live-12345";

    public void weakHash() throws Exception {
        MessageDigest.getInstance("MD5");
    }

    public void commandInjection(String userInput) throws Exception {
        Runtime.getRuntime().exec("ls " + userInput);
    }
}
