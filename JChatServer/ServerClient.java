package JChatServer;

public class ServerClient {
    // The client information
    private String clientID = "";
    private String clientPass = "";
    private String clientName = "";
    private String clientAuth = "";
    private int lastMessageID = 0;
    private long lastActive = 0;

    // Create the client with the ID and passphrase.
    public ServerClient(String clientID, String clientPass){
        this.clientID = clientID;
        this.clientPass = clientPass;
        this.setClientAuth("USER");
    }

    // Get the ID
    public String getClientID(){
        return this.clientID;
    }

    // Get the passphrase
    public String getClientPass(){
        return this.clientPass;
    }

    // Set the client name
    public void setClientName(String name){
        this.clientName = name;
    }

    // Get the client name
    public String getClientName(){
        return this.clientName;
    }

    // Set the client auth level
    public void setClientAuth(String auth){
        this.clientAuth = auth;
    }

    // Get the client auth level
    public String getClientAuth(){
        return this.clientAuth;
    }

    // Set the last (read) message ID.
    public void setLastMessageID(int id){
        this.lastMessageID = id;
    }

    // Get the last (read) message ID.
    public int getLastMessageID(){
        return this.lastMessageID;
    }

    // Set the last active timestamp
    public void setLastActive(long la){
        this.lastActive = la;
    }

    // Get the last active timestamp
    public long getLastActive(){
        return this.lastActive;
    }
}