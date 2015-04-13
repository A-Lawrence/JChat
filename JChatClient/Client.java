package JChatClient;

// Web Service stuff!
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceRef;
import javax.xml.namespace.QName;
import java.net.SocketException;
import java.net.URL;
import JChatServer.ServerInterface;

// Gui stuff!
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

// Misc!
import java.util.UUID;

public class Client extends Thread {
    // Server & Service variables
    private boolean fullyInitialized = false; // True once client is up and running!
    private URL svr_url;
    private QName svr_qualName;
    private Service svr_service;
    private ServerInterface svr_endPoint;
    private int reconnectAttempts = 0;

    /* CLIENT INFORMATION */
    private String clientID = "";
    private String clientName = "";
    private String clientPass = "";
    private String clientAuth = "U";

    /* GUI Related variables */
    // Main frame/container
    protected JFrame frame; // The JFrame
    protected Container container; // The JFrame container.

    // Message area
    protected JScrollPane msgScrollPane; // The scroll pane for the text area.
    protected JTextPane msgTextPane; // The message text pane.
    protected StyledDocument msgStyledDoc; // The message styled document.
    protected Style msgTextStyle; // The styles for the message area.
    //protected JTextArea msgTextArea; // The text area for messages.

    // Input area
    protected JScrollPane inputScrollPane; // The scoll pane for the input message.
    protected JTextArea inputTextArea; // The text area to input message.

    // The user list
    protected JScrollPane userScrollPane; // The scroll pane for the users.
    protected JPanel userPanel; // The panel for the user list (to stop line wrap).
    protected JTextPane userTextPane; // The user text pane.
    protected StyledDocument userStyledDoc; // The text area for the users.
    protected Style userTextStyle; // The styles for the user list.

    // Displays the main client GUI.
    private void display(){
        // Create the frame and disable the close operation
        this.frame = new JFrame();
        this.frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        // Now make up our custom on close function!
        this.frame.addWindowListener(new WindowAdapter(){
            public void windowClosing(WindowEvent e){
                quit();
            }
        });

        /* MAIN FRAME INFO */
        this.container = this.frame.getContentPane();
        this.frame.setLayout(null);
        this.frame.setVisible(true);
        this.frame.setLocationRelativeTo(this.frame.getRootPane());
        this.frame.setSize(FRAME_WIDTH, FRAME_HEIGHT);
        this.frame.setResizable(false);

        /* CHAT MESSAGES */
        // Sort out the text pane and the styled document.
        this.msgTextPane = new JTextPane();
        this.msgStyledDoc = this.msgTextPane.getStyledDocument();
        //this.msgTextArea = new JTextArea();
        this.msgTextPane.setEditable(false);
        //this.msgTextArea.setEditable(false);
        //this.msgTextArea.setLineWrap(true);
        //this.msgTextArea.setWrapStyleWord(true);

        // Add the styles to the document.
        this.msgTextStyle = this.msgTextPane.addStyle("default", null);
        StyleConstants.setForeground(this.msgTextStyle, Color.gray);

        this.msgTextStyle = this.msgTextPane.addStyle("PrivateMessage", null);
        StyleConstants.setForeground(this.msgTextStyle, Color.ORANGE);
        StyleConstants.setItalic(this.msgTextStyle, true);

        this.msgTextStyle = this.msgTextPane.addStyle("ServerMessage", null);
        StyleConstants.setForeground(this.msgTextStyle, Color.RED);
        StyleConstants.setBold(this.msgTextStyle, true);

        this.msgTextStyle = this.msgTextPane.addStyle("ThirdPersonMessage", null);
        StyleConstants.setForeground(this.msgTextStyle, Color.MAGENTA);
        StyleConstants.setBold(this.msgTextStyle, true);
        StyleConstants.setItalic(this.msgTextStyle, true);

        this.msgTextStyle = this.msgTextPane.addStyle("WarningMessage", null);
        StyleConstants.setForeground(this.msgTextStyle, Color.RED);
        StyleConstants.setBold(this.msgTextStyle, true);

        this.msgTextStyle = this.msgTextPane.addStyle("KickMessage", null);
        StyleConstants.setForeground(this.msgTextStyle, Color.RED);
        StyleConstants.setBold(this.msgTextStyle, true);

        this.msgTextStyle = this.msgTextPane.addStyle("OwnMessage", null);
        StyleConstants.setForeground(this.msgTextStyle, Color.DARK_GRAY);
        StyleConstants.setBold(this.msgTextStyle, true);
        StyleConstants.setItalic(this.msgTextStyle, true);

        this.msgTextStyle = this.msgTextPane.addStyle("OtherMessage", null);
        StyleConstants.setForeground(this.msgTextStyle, Color.BLUE);
        StyleConstants.setBold(this.msgTextStyle, true);
        StyleConstants.setItalic(this.msgTextStyle, true);

        // Add the text pane to the scroll pane.
        this.msgScrollPane = new JScrollPane(this.msgTextPane,
                                          JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                          JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        this.msgScrollPane.setPreferredSize(
                            new Dimension(MESSAGE_WIDTH, // Width
                                          MESSAGE_HEIGHT)); // Height
        this.msgScrollPane.setBounds(PADDING, // Left (X)
                                  PADDING, // Top (Y)
                                  MESSAGE_WIDTH, // Width
                                  MESSAGE_HEIGHT); // Height
        this.container.add(this.msgScrollPane);

        /* USER LIST */
        // Sort out the main Pane and styled document
        this.userTextPane = new JTextPane();
        this.userStyledDoc = this.userTextPane.getStyledDocument();
        this.userTextPane.setEditable(false);

        // Add the styles to the document
        this.userTextStyle = this.userTextPane.addStyle("title", null);
        StyleConstants.setBold(this.userTextStyle, true);

        this.userTextStyle = this.userTextPane.addStyle("ADMIN", null);
        StyleConstants.setForeground(this.userTextStyle, Color.red);
        StyleConstants.setBold(this.userTextStyle, true);

        this.userTextStyle = this.userTextPane.addStyle("USER", null);
        StyleConstants.setForeground(this.userTextStyle, Color.blue);

        this.userTextStyle = this.userTextPane.addStyle("GUEST", null);
        StyleConstants.setForeground(this.userTextStyle, Color.magenta);

        // Create the pane and add the textPane.
        this.userPanel = new JPanel(new BorderLayout());
        this.userPanel.add(this.userTextPane);

        // Now add it all to a scroll pane.
        this.userScrollPane = new JScrollPane(this.userPanel,
                                              JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                              JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        this.userScrollPane.setPreferredSize(
                            new Dimension(FRAME_WIDTH-MESSAGE_WIDTH-(PADDING*5), // Width
                                          MESSAGE_HEIGHT));
        this.userScrollPane.setBounds((PADDING*2) + MESSAGE_WIDTH, // Left (X)
                                      PADDING, // Top (Y)
                                      FRAME_WIDTH-MESSAGE_WIDTH-(PADDING*5), // Width
                                      MESSAGE_HEIGHT); // Height
        this.container.add(this.userScrollPane);

        // Add the text area and scroll pane for the input message.
        this.inputTextArea = new JTextArea("Chat is loading...");
        this.inputTextArea.setEditable(false);
        this.inputTextArea.setLineWrap(true);
        this.inputTextArea.setWrapStyleWord(true);
        this.inputTextArea.addKeyListener(new KeyAdapter(){
            public void keyTyped(KeyEvent evt) {
                sendMessage(evt);
            }
        });
        this.inputScrollPane = new JScrollPane(this.inputTextArea,
                                               JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                               JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        this.inputScrollPane.setPreferredSize(
                            new Dimension(FRAME_WIDTH-(PADDING*4), // Width
                                          40)); // Height
        this.inputScrollPane.setBounds(PADDING, // Left (X)
                                       MESSAGE_HEIGHT+(PADDING*2), // Top (Y)
                                       FRAME_WIDTH-(PADDING*4), // Width
                                       40); // Height
        this.container.add(this.inputScrollPane);

        // Repaint the GUI
        this.container.repaint();
        this.frame.repaint();

        // Try and get the focus for the text area!
        this.inputScrollPane.requestFocusInWindow();
        this.inputTextArea.requestFocusInWindow();

        // Fully initialized
        this.fullyInitialized = true;
    }

    // The main method runs the actual client since we don't want it to be static!
    public static void main(String[] args) {
        try {
            // Start client
            Client client = new Client();
            Thread cli = new Thread(client);
            client.start();
            cli.start();
        } catch(Exception e){
            String msg = "There was an error starting the client - please contact support.";
            JOptionPane.showMessageDialog(null, msg);
            System.exit(1);
        }
    }

    // Connect to the server.
    private void connect(){
        //this.msgTextArea.append("*** DISCONNECTED: ATTEMPTING TO RECONNECT ***\n");
        this.frame.setTitle("*** DISCONNECTED: ATTEMPTING TO RECONNECT ***");
        // Try and connect to the server.
        try {
            // Set the URL and QName
            this.svr_url = new URL("http://localhost:8081/ChatterBox?wsdl");
            this.svr_qualName = new QName("http://JChatServer/", "ServerService");

            // Get the service port/endpoint.
            this.svr_service = Service.create(this.svr_url, this.svr_qualName);
            this.svr_endPoint = this.svr_service.getPort(new QName("http://JChatServer/", "ServerPort"),
                                                         JChatServer.ServerInterface.class);
            reconnectAttempts = 0;
            //this.msgTextArea.append("*** CONNECTED TO SERVER ***\n");
            this.frame.setTitle("*** CONNECTED TO SERVER ***");
        } catch(Exception e){
            this.reconnectAttempts++;
            //this.msgTextArea.append("*** COULD NOT CONNECT TO SERVER ***\n");
            this.frame.setTitle("*** COULD NOT CONNECT TO SERVER ***");
        }
    }

    // Start the client by sorting out the server and loading the GUI.
    public void start() {
        // Display the client so they don't think it's not doing anything!
        // Only do this is not already started!
        if(this.frame == null){
            this.display();
        }

        // If the endpoint is invalid, connect
        if(this.svr_endPoint == null){
            this.connect();
        }

        // Generate the client passPhrase - this is for security!
        // By generating a unique ID, we can ensure that nobody can pretend to be
        // us, although with a bit of tinkering and reverse engineering,
        // it is, technically, possible.
        UUID pp = UUID.randomUUID();
        this.clientPass = pp.toString();

        // Now we need to connect to the chat server and get our ID and name.
        try {
            String[] getInfo = this.svr_endPoint.join(this.clientPass);
            this.clientID = getInfo[0];
            this.clientName = getInfo[1];
        } catch(Exception e){

        }

        // Right, let's start!
        this.inputTextArea.setText("");
        this.inputTextArea.setEditable(true);
    }

    // Quit the chat
    private void quit(boolean dontClose){
        try {
            this.svr_endPoint.quit(this.clientID, this.clientPass);
            this.clientID = "";
        } catch(Exception e){
            // We don't actually care here because we're quitting anyway!
        }

        // Only close if dontClose is false!
        if(!dontClose){
            System.exit(0);
        }
    }
    // Overload!
    private void quit(){
        this.quit(false);
    }

    // Send the user's message to the chat server.
    private void sendMessage(){
        // Firstly, make the text area read only and get the contents
        this.inputTextArea.setEditable(false);
        String message = this.inputTextArea.getText();

        // Send the message to the server
        try {
            this.svr_endPoint.talk(this.clientID, this.clientPass, message);
            message = "";
        } catch(Exception e){
            // If it's an invalid security token, let's reset the client ID, reconnect and try again.
            if(e.getMessage().equalsIgnoreCase("INVAL_SECURITY")){
                this.clientID = null;
                this.start();
                this.sendMessage();
                return;
            } else { // Unknown error
                String msg = e.getMessage();
                msg = "Oops! An error occured:\n" + msg;
                JOptionPane.showMessageDialog(null, msg);
            }
        }

        // Make the text area empty and editable again.
        this.inputTextArea.setText(message);
        this.inputTextArea.setEditable(true);
    }

    // Overload so we can accept a key event - only send on new line character.
    private void sendMessage(KeyEvent evt){
        // Get the character
        char c = evt.getKeyChar();

        // If it's a new line, call the original method.
        if(c == '\n'){
            sendMessage();
        }
    }

    // The run method for the thread - continually polls for messages!
    public void run(){
        // Let's poll the server for things.
        while(true){
            // If we're not a client, or it's not finished loading, wait.
            if(this.clientID.equals("") && !this.fullyInitialized){
                continue;
            }

            boolean disconnect = false;
            try {
                /*** MESSAGES ***/
                // Get
                String[][] recent = this.svr_endPoint.talkPoll(this.clientID, this.clientPass);

                // Display
                for(int i=0; i<recent.length; i++){
                    // If either the message or the user are empty, skip
                    if(recent[i][0].equals("") || recent[i][2].equals("")){
                        continue;
                    }

                    // Default variables
                    String m = "";
                    String style = "default";
                    int formatStart = this.msgStyledDoc.getLength();
                    int formatEnd = formatStart;

                    // Format the message!
                    if(recent[i][3].equalsIgnoreCase("P")){
                        // Format the message
                        m = "Private message from " + recent[i][0] + ": " + recent[i][2];

                        // Sort out the text position/formatting info
                        formatEnd += m.length() - recent[i][2].length() - 1;
                        style = "PrivateMessage";

                        // RED FOR SERVER
                        if(recent[i][0].equalsIgnoreCase("SERVER")){
                            style = "ServerMessage";
                        }
                    } else if(recent[i][3].equalsIgnoreCase("3")){ // 3rd person?
                        // Format the message
                        m = " * " + recent[i][0]+ " " + recent[i][2];

                        // Sort out the text position/formatting info
                        formatEnd += m.length();
                        style = "ThirdPersonMessage";
                    } else if(recent[i][3].equalsIgnoreCase("W")){ // Warning
                        // Format the message
                        m = " *** " + recent[i][0] + " gave you a warning: " + recent[i][2];

                        // Sort out the text position/formatting info
                        formatEnd += m.length() - recent[i][2].length() - 1;
                        style = "WarningMessage";
                    } else if(recent[i][3].equalsIgnoreCase("K")){ // Kick
                        // Format the message
                        m = " *** " + recent[i][0] + " has kicked you because: " + recent[i][2];
                        disconnect = true;

                        // Sort out the text position/formatting info
                        formatEnd += m.length() - recent[i][2].length() - 1;
                        style = "KickMessage";
                    } else {
                        // Format the message
                        m = recent[i][0] + ": " + recent[i][2];

                        // Sort out the text position/formatting info
                        formatEnd += recent[i][0].length();

                        // Different user types
                        if(recent[i][0].equalsIgnoreCase("SERVER")){
                            style = "ServerMessage";
                        } else if(recent[i][1].equalsIgnoreCase(this.clientID)){
                            style = "OwnMessage";
                        } else {
                            style = "OtherMessage";
                        }
                    }

                    // Append to text area and format it
                    //this.msgTextArea.append(m);
                    this.msgStyledDoc.insertString(this.msgStyledDoc.getLength(),
                                                   m,
                                                   null);
                    this.msgStyledDoc.setCharacterAttributes(
                                                formatStart, // Start
                                                formatEnd, // Finish
                                                this.msgTextPane.getStyle(style), // Style
                                                true);
                    this.msgStyledDoc.setCharacterAttributes(
                                                formatEnd, // Start
                                                this.msgStyledDoc.getLength(), // Finish
                                                this.msgTextPane.getStyle("default"), // Style
                                                true);
                }

                /*** CLIENT INFORMATION ***/
                this.clientName = this.svr_endPoint.getInfo(this.clientID, this.clientPass, "clientName");
                this.clientAuth = this.svr_endPoint.getInfo(this.clientID, this.clientPass, "clientAuth");

                /*** UPDATE THE GUI WHERE REQUIRED ***/
                // Frame title
                String serverName = this.svr_endPoint.getInfo(this.clientID, this.clientPass, "ServerInfo_name");
                this.frame.setTitle(serverName + " :: Currently logged in as " + this.clientName + " (" + this.clientAuth + ")");

                // Auto scroll to bottom of messages
                /*this.msgTextArea.selectAll();
                int x = this.msgTextArea.getSelectionEnd();
                this.msgTextArea.select(x,x);*/
                this.msgTextPane.selectAll();
                int x = this.msgTextPane.getSelectionEnd();
                this.msgTextPane.select(x,x);

                /*** Get a list of all connected clients ***/
                // Get all connected clients and split them by ":"
                String clients = this.svr_endPoint.getInfo(this.clientID, this.clientPass, "connectedClients");
                String[] clientsSplit = clients.split(";");

                // Now loop through and add them to the users list
                //this.userStyledDoc.setText("-----Client List-----\n");
                this.userStyledDoc.remove(0, this.userStyledDoc.getLength());
                this.userStyledDoc.insertString(0, "--CLIENT LIST--\n", null);
                this.userStyledDoc.setCharacterAttributes(
                                            0, // Start
                                            this.userStyledDoc.getLength(), // Finish
                                            this.userTextPane.getStyle("title"), // Style
                                            true);
                for(int i=0; i<clientsSplit.length; i++){
                    // If it's empty or null, ignore it
                    if(clientsSplit[i].equals("") || clientsSplit[i].equals("null") || clientsSplit[i] == null){
                        continue;
                    }

                    // Split the client info up by _
                    String[] cli = clientsSplit[i].split(",");

                    // If it's an empty name, skip.
                    if(cli[0].equals("")){
                        continue;
                    }

                    // Get the length of the textPane so we can style stuff
                    int textStart = this.userStyledDoc.getLength();
                    int textEnd = this.userStyledDoc.getLength();

                    // Construct the name
                    String name = cli[0];
                    if(cli[1].equalsIgnoreCase("ADMIN")){
                        // Format the name
                        name = "*" + name + "*";

                        // Sort out the pointers for formatting
                        textEnd += name.length()+2;
                    } else if(cli[1].equalsIgnoreCase("GUEST")){
                        //Format the name
                        name = "-" + name + "-";

                        // Sort out the pointers for formatting
                        textEnd += name.length()+2;
                    } else {
                        // Format the name
                        name = cli[0];

                        // Sort out the pointers for formatting
                        textEnd += name.length();
                    }

                    // Now, add them to the list and style them!
                    this.userStyledDoc.insertString(textStart,
                                                   name + "\n", null);
                    this.userStyledDoc.setCharacterAttributes(
                                                textStart, // Start
                                                textEnd, // Finish
                                                this.userTextPane.getStyle(cli[1].toUpperCase()), // Style
                                                true);
                }
                // Reset the reconnect attempts.
                this.reconnectAttempts = 0;
            } catch(Exception e){
                e.printStackTrace();
                System.exit(1);

                // Since we're not connected, do something!
                if(this.clientID.equals("") || e.getMessage().equalsIgnoreCase("INVAL_SECURITY")){ // Never been connected/server error.
                    this.start();
                } else { // Just disconnected.
                    this.connect();
                }
                this.reconnectAttempts++;

                // If there have been more than 10 attempts, just close!
                if(this.reconnectAttempts >= 10){
                    //String msg = "The server has gone away - client closing.";
                    //JOptionPane.showMessageDialog(null, msg);
                    //System.exit(1);
                }
            }

            // Try and sleep
            try {
                // Disconnect?
                if(disconnect){
                    // Warn of the disconnect and prevent typing.
                    this.frame.setTitle("DISCONNECTING");
                    this.inputTextArea.setEditable(false);
                    this.inputTextArea.setEnabled(false);
                    for(int i=10; i>0; i--){
                        //this.msgTextArea.append("You will be disconnected in " + i + " seconds.\n");
                        Thread.sleep(1000);
                    }
                    //this.msgTextArea.append("You are now disconnected.");

                    // Actually disconnect
                    this.frame.setTitle("DISCONNECTED");
                    this.quit(true);
                    break;
                }

                // Sleep for .25 of a second.
                Thread.sleep(250);
            }  catch(Exception e){
                System.out.println(e.getMessage());
            }
        }
    }

    // All of the constants for the client.
    private static final boolean DEBUG = true;
    private static final int FRAME_WIDTH = 600;
    private static final int FRAME_HEIGHT = 500;
    private static final int PADDING = 10;
    private static final int MESSAGE_WIDTH = 450;
    private static final int MESSAGE_HEIGHT = 400;
}