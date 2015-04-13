package JChatServer;

// Web Service things
import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.Style;

// Misc!
import java.util.ArrayList;

@WebService(name="ChatterBox")
@SOAPBinding(style = Style.RPC)
public interface ServerInterface {
    @WebMethod public String[] join(String clientPass);
    @WebMethod public String setDisplayName(String clientID, String clientPass, String clientName) throws Exception;
    @WebMethod public boolean talk(String clientID, String clientPass, String message) throws Exception;
    @WebMethod public String[][] talkPoll(String clientID, String clientPass) throws Exception;
    @WebMethod public String getInfo(String clientID, String clientPass, String getInfo) throws Exception;
    @WebMethod public void quit(String clientID, String clientPass) throws Exception;
}