package JChatServer;

public class Message {
    private char type = 'G'; // G for general message, P for private, 3 for 3rd person (\me)
    private String fromClientID = "";
    private String fromClientName = "";
    private String message = "";
    private String toClientID = "";
    private String toClientName = "";

    // Constructor
    public Message(String msg){
        this.message = msg;
        setMessageType('G');
    }

    // Set the message type
    public void setMessageType(char type){
        this.type = type;
    }

    // Get the message type
    public char getMessageType(){
        return this.type;
    }

    // Set the client from, information
    public void setFromClient(String clientID, String clientName){
        this.fromClientID = clientID;
        this.fromClientName = clientName;
    }

    // Get the client ID
    public String getFromClientID(){
        return this.fromClientID;
    }

    // Get the client from, name
    public String getFromClientName(){
        return this.fromClientName;
    }

    // Get the message
    public String getMessage(){
        return this.message;
    }

    // Set the client to, name (in the case of private messages)
    public void setToClient(String clientID, String clientName){
        this.toClientID = clientID;
        this.toClientName = clientName;
    }

    // Get the client to, ID
    public String getToClientID(){
        return this.toClientID;
    }

    // Get the client to, name
    public String getToClientName(){
        return this.toClientName;
    }
}
