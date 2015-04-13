package JChatServer;

// Web Services stuff!
import javax.jws.WebService;
import javax.xml.ws.Endpoint;

// Misc!
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

@WebService(endpointInterface = "JChatServer.ServerInterface")
public class Server extends Thread implements ServerInterface {
    // The hash table of clients
    Hashtable<String, ServerClient> clients = new Hashtable<String, ServerClient>();

    // The array of messages
    ArrayList<Message> messages = new ArrayList<Message>();

    // The arrayList of help for the commands!
    ArrayList<String> helpInfo = new ArrayList<String>();

    // The hashTable of server information!
    Hashtable<String, String> serverInfo = new Hashtable<String, String>();

    // Constructor - sets the necessary info for the server!
    public Server(){
        synchronized(this.clients){
            // Add "Server" as a client.
            ServerClient server = new ServerClient("Server", "Server");
            server.setClientName("Server");
            server.setClientAuth("ADMIN");
            server.setLastActive(System.currentTimeMillis()/1000);
            this.clients.put("Server", server);
        }

        // Add the default server info
        this.serverInfo.put("name", "ChatterBox Chat Server");

        // Add the info for the commands.
        this.helpInfo.add("/msg <username> <msg> - Sends <msg> privately to <username>.");
        this.helpInfo.add("/me <action> - sends a message in third person.");
        this.helpInfo.add("/nick <requested name> - attempts to change username to <requested name>");
        this.helpInfo.add("/auth <password> authenticate as admin using <password>.");
        this.helpInfo.add("/auth <password> Q - authenticate as admin using <password> without announcing this to the channel!");
        this.helpInfo.add("/deauth - deauthorises yourself as administrator.");
        this.helpInfo.add("/deauth <username> - deauthorises <username> as administrator.");
        this.helpInfo.add("/silence <username> - Silences <username>.");
        this.helpInfo.add("/unsilence <username> - Allows <username> to speak again.");
        this.helpInfo.add("/warn <username> <reason> - Wanrs <username> with <reason> whilst admin.");
        this.helpInfo.add("/kick <username> <reason> - Kicks <username> from the server with <reason> whilst admin.");
        this.helpInfo.add("/setserver <key> <value> - Sets server <key> to <value> as admin.");
    }

    // The threaded actions
    public void run(){
        while(true){
            // Try and sleep.
            try {
                Thread.sleep(2000);
            } catch(Exception e){
                System.out.println("Error: " + e.getMessage());
                System.out.println("System closing.");
                System.exit(1);
                break;
            }

            // Get all clients and see which ones haven't been active in over 5 minutes!
            synchronized(this.clients){
                // Update the server so it doesn't time out!
                ServerClient server = this.clients.get("Server");
                server.setLastActive((System.currentTimeMillis()/1000)+300);
                this.clients.put("Server", server);

                // Get the keyset of client IDs.
                Set<String> keySet = this.clients.keySet();

                // Iterate through each client.
                Iterator<String> itr = keySet.iterator();
                while(itr.hasNext()){
                    String str = itr.next();

                    // Get the stored client
                    ServerClient client = this.clients.get(str);

                    // Have they been inactive for over 2 minutes?
                    if(client.getLastActive() <= (System.currentTimeMillis()/1000)-300){
                        // Time them out
                        this.pushMessage("Server", "User '" + client.getClientName() + "' timed out.");
                        this.clients.remove(str);
                    }
                }
            }
        }
    }

    // Security check! Ensure that the client exists and has given the correct pass phrase!
    private boolean security(String clientID, String clientPass){
        // Does this ID exist?
        synchronized(this.clients){
            if(!this.clients.containsKey(clientID)){
                return false;
            }
        }

        ServerClient client;
        synchronized(this.clients){
            // Get the client
            client = this.clients.get(clientID);
        }

        // Does the pass phrase match?
        if(!client.getClientPass().equals(clientPass)){
            return false;
        }

        // If we're here, it's a valid check!
        return true;
    }

    // Allow a client to join and take their details.
    public String[] join(String clientPass){

        // Generate a universally unique ID and store it!
        UUID randomID = UUID.randomUUID();
        String clientID = randomID.toString();

        // Now, create a new client instance
        ServerClient client = new ServerClient(clientID, clientPass);
        client.setLastMessageID(this.messages.size()-1);
        client.setLastActive(System.currentTimeMillis()/1000);

        // Add the client to the hashtable
        synchronized(this.clients){
            this.clients.put(clientID, client);
        }

        // Set their name to "Guest"
        String uName = "Guest";
        try {
            uName = this.setDisplayName(clientID, clientPass, "Guest");
        } catch(Exception e){
            // There shouldn't ever be an exception here!
            System.out.println("There was a catastophic error!");
            System.exit(0);
        }

        // Add a joining message!
        //this.messages.add("Server: " + uName + " has joined the chat!\n");
        this.pushMessage("Server", uName + " has joined the chat :~)");

        // Return the client ID and client name in an array.
        String[] getInfo = new String[2];
        getInfo[0] = clientID;
        getInfo[1] = uName;
        return getInfo;
    }

    // Send a message to all connected clients.
    public boolean talk(String clientID, String clientPass, String message) throws Exception {
        // Security
        if(!security(clientID, clientPass)){
            throw new Exception("INVAL_SECURITY");
        }

        // Update last active for the client
        this.updateLastActive(clientID);

        // Get client info
        ServerClient requestingClient = this.clients.get(clientID);

        // The requesting client should not be a guest - otherwise, they can only listen.
        if(requestingClient.getClientAuth().equalsIgnoreCase("GUEST")){
            this.pushMessage("Server", "You are only a guest; guests cannot speak.", clientID);
            return false;
        }

        // Strip all new lines & tabs from the message
        message = message.replace("\n", "");
        message = message.replace("\t", " ");
        message = message.replaceAll(" +", " ");

        // Now we need to handle the message!  There are certain types of
        // message that could be sent.
        // If the first character is a backspace, it's a special message.
        if(message.substring(0, 1).equals("/")){
            // Now, get the message split by spaces.
            String[] msgSplit = message.split(" ");

            /****** Match the code *********/
            // Help
            if((msgSplit[0].equals("/help"))){
                // Create the help message
                String helpMsg = "The following commands are available on this server:";
                Iterator<String> it = this.helpInfo.iterator();
                while(it.hasNext()){
                    String command = it.next();
                    helpMsg = helpMsg + "\r\n" + command;
                }
                this.pushMessage("Server", helpMsg, clientID, 'P');
            }

            // Private message
            if((msgSplit[0].equalsIgnoreCase("/msg") || msgSplit[0].equalsIgnoreCase("/m") || msgSplit[0].equalsIgnoreCase("/pm")) && msgSplit.length >= 3){
                // Part 2 should be a name of someone that's here!
                // Let's check if they exist
                if(!this.clientNameExists(msgSplit[1])){
                    // They don't, so let's send the client a message.
                    this.pushMessage("Server", "User '" + msgSplit[1] + "' isn't on this server.", clientID, 'P');
                    return false;
                }

                // Get the toClientID
                String toClientID = this.getClientIDByName(msgSplit[1]);

                // If they've tried to send a message to "Server", send them a nice response!
                if(toClientID.equalsIgnoreCase("Server")){
                    this.pushMessage("Server", "I do not interact with humans. >.<", clientID, 'P');
                    return false;
                }

                // If the toClientID and fromClientID are the same - send them a PM from the server!
                if(clientID.equals(toClientID)){
                    this.pushMessage("Server", "Messaging yourself is just pointless...", clientID, 'P');
                    return false;
                }

                // Stick all the message together minus 0 and 1
                msgSplit[0] = "";
                msgSplit[1] = "";
                String msg = "";
                for(int i=0; i<msgSplit.length; i++){
                    msg = msg + msgSplit[i];
                    if(!msgSplit[i].equals("")){
                        msg = msg + " ";
                    }
                }

                // Now we have a client ID, let's send the message
                this.pushMessage(clientID, msg, toClientID, 'P');
            }

            // 3rd Person (slash-me)
            if((msgSplit[0].equalsIgnoreCase("/me")) && msgSplit.length >= 2){
                // Stick all the message together except 0
                msgSplit[0] = "";
                String msg = "";
                for(int i=0; i<msgSplit.length; i++){
                    msg = msg + msgSplit[i];
                    if(!msgSplit[i].equals("")){
                        msg = msg + " ";
                    }
                }

                // Now send the 3rd person message
                this.pushMessage(clientID, msg, '3');
            }

            // Change nickname
            if((msgSplit[0].equalsIgnoreCase("/nick")) && msgSplit.length >= 2){
                // Change the username
                this.setDisplayName(clientID, clientPass, msgSplit[1]);
                return true;
            }

            // Authenticate as admin
            if((msgSplit[0].equalsIgnoreCase("/auth")) && msgSplit.length >= 2){
                // Is the password correct?
                if(!msgSplit[1].equalsIgnoreCase("samaria")){
                    // Nope - so we'll reject their request!
                    this.pushMessage("Server", "Cannot authenticate as admin - invalid credentials.", clientID, 'P');
                    return false;
                }

                // Since it was true, authenticate!
                ServerClient client;
                synchronized(this.clients){
                    client = this.clients.get(clientID);
                    client.setClientAuth("ADMIN");
                    this.clients.put(clientID, client);
                }

                // Now let everyone know (if there isn't a special flag saying otherwise)
                if(msgSplit.length >= 3){
                    // If part 2 is "Q", we won't announce it to the chat.
                    // Otherwise, we will!
                    if(msgSplit[2].equalsIgnoreCase("Q")){
                        this.pushMessage("Server", "You are now an admin.", clientID, 'P');
                        return true;
                    } else {
                        this.pushMessage("Server", "User '" + client.getClientName() + "' is now an admin.");
                        return true;
                    }
                } else {
                    this.pushMessage("Server", "User '" + client.getClientName() + "' is now an admin.");
                    return true;
                }
            }

            // De-authenticate as an admin
            if(msgSplit[0].equalsIgnoreCase("/deauth")){
                // The requesting user should be an admin!
                if(!requestingClient.getClientAuth().equalsIgnoreCase("ADMIN")){
                    // Tell them!
                    this.pushMessage("Server", "You do not have sufficient rights to do this.", clientID, 'P');
                    return false;
                }

                // If there is a username given, we're going to deauth that user!
                ServerClient client;
                if(msgSplit.length >= 2){
                    // If they've specified the server, say NO!
                    if(msgSplit[1].equalsIgnoreCase("Server")){
                        this.pushMessage("Server", "You cannot demote me! I am angry that you tried! :-@", clientID, 'P');
                        return false;
                    }

                    // Does the user exist?
                    if(!this.clientNameExists(msgSplit[1])){
                        // Tell them it doesn't exist!
                        this.pushMessage("Server", "User '" + msgSplit[1] + "' isn't on this server.", clientID, 'P');
                        return false;
                    }

                    // Get the client ID
                    String deAuthCID = this.getClientIDByName(msgSplit[1]);

                    // De auth the client
                    synchronized(this.clients){
                        client = this.clients.get(deAuthCID);

                        // If they aren't admin, ignore.
                        if(!client.getClientAuth().equalsIgnoreCase("ADMIN")){
                            return false;
                        }

                        client.setClientAuth("USER");
                        this.clients.put(clientID, client);
                    }
                } else { // Deauth THIS user.
                    synchronized(this.clients){
                        client = this.clients.get(clientID);

                        // If they aren't admin, ignore.
                        if(!client.getClientAuth().equalsIgnoreCase("ADMIN")){
                            return false;
                        }

                        client.setClientAuth("USER");
                        this.clients.put(clientID, client);
                    }
                }

                // Announce to the server
                this.pushMessage("Server", "User '" + client.getClientName() + "' is no longer an admin.");
                return true;
            }

            // Silence a user
            if((msgSplit[0].equalsIgnoreCase("/silence")) && msgSplit.length >= 2){
                // The requesting user should be an admin!
                if(!requestingClient.getClientAuth().equalsIgnoreCase("ADMIN")){
                    // Tell them!
                    this.pushMessage("Server", "You do not have sufficient rights to do this.", clientID, 'P');
                    return false;
                }

                // If they've specified the server, say NO!
                if(msgSplit[1].equalsIgnoreCase("Server")){
                    this.pushMessage("Server", "Trying to silence me is a criminal offence! :-/", clientID, 'P');
                    return false;
                }

                // Does the requested user exist?
                if(!this.clientNameExists(msgSplit[1])){
                    // Tell them it doesn't exist!
                    this.pushMessage("Server", "User '" + msgSplit[1] + "' isn't on this server.", clientID, 'P');
                    return false;
                }

                // Get the user info so we can silence them
                String silenceClientID = this.getClientIDByName(msgSplit[1]);
                ServerClient client;
                synchronized(this.clients){
                    client = this.clients.get(silenceClientID);

                    // If they're already a guest, do nothing.
                    if(client.getClientAuth().equalsIgnoreCase("GUEST")){
                        this.pushMessage("Server", "User '" + msgSplit[1] + "' is already silenced.", clientID, 'P');
                        return false;
                    }

                    client.setClientAuth("GUEST");
                    this.clients.put(silenceClientID, client);
                }

                // Announce to the world!
                this.pushMessage("Server", "User '" + msgSplit[1] + "' has been silenced.");
            }

            // Unsilence a user
            if((msgSplit[0].equalsIgnoreCase("/unsilence")) && msgSplit.length >= 2){
                // The requesting user should be an admin!
                if(!requestingClient.getClientAuth().equalsIgnoreCase("ADMIN")){
                    // Tell them!
                    this.pushMessage("Server", "You do not have sufficient rights to do this.", clientID, 'P');
                    return false;
                }

                // If they've specified the server, say NO!
                if(msgSplit[1].equalsIgnoreCase("Server")){
                    this.pushMessage("Server", "I am happy that you tried to save me, but I'm too powerful to be silenced in the first place.", clientID, 'P');
                    return false;
                }

                // Does the requested user exist?
                if(!this.clientNameExists(msgSplit[1])){
                    // Tell them it doesn't exist!
                    this.pushMessage("Server", "User '" + msgSplit[1] + "' isn't on this server.", clientID, 'P');
                    return false;
                }

                // Get the user info so we can silence them
                String silenceClientID = this.getClientIDByName(msgSplit[1]);
                ServerClient client;
                synchronized(this.clients){
                    client = this.clients.get(silenceClientID);

                    // If they're not a guest, do nothing.
                    if(!client.getClientAuth().equalsIgnoreCase("GUEST")){
                        this.pushMessage("Server", "User '" + msgSplit[1] + "' is not silenced.", clientID, 'P');
                        return false;
                    }

                    client.setClientAuth("USER");
                    this.clients.put(silenceClientID, client);
                }

                // Announce to the world!
                this.pushMessage("Server", "User '" + msgSplit[1] + "' is allowed to talk again.");
            }

            // Warn a user
            if((msgSplit[0].equalsIgnoreCase("/warn")) && msgSplit.length >= 3){
                // The requesting user should be an admin!
                if(!requestingClient.getClientAuth().equalsIgnoreCase("ADMIN")){
                    // Tell them!
                    this.pushMessage("Server", "You do not have sufficient rights to do this.", clientID, 'P');
                    return false;
                }

                // If they've specified the server, say NO!
                if(msgSplit[1].equalsIgnoreCase("Server")){
                    this.pushMessage("Server", "I am the one in charge - you cannot warn me!", clientID, 'P');
                    return false;
                }

                // Part 2 should be a name of someone that's here!
                // Let's check if they exist
                if(this.restrictedClientName(msgSplit[1]) || !this.clientNameExists(msgSplit[1])){
                    // They don't, so let's send the client a message.
                    this.pushMessage("Server", "User '" + msgSplit[1] + "' isn't on this server.", clientID, 'P');
                    return false;
                }

                // Get the toClientID
                String toClientID = this.getClientIDByName(msgSplit[1]);

                // Stick all the message together minus 0 and 1
                msgSplit[0] = "";
                msgSplit[1] = "";
                String msg = "";
                for(int i=0; i<msgSplit.length; i++){
                    msg = msg + msgSplit[i];
                    if(!msgSplit[i].equals("")){
                        msg = msg + " ";
                    }
                }

                // Now we have a client ID, let's send the warning message!
                this.pushMessage(clientID, msg, toClientID, 'W');
            }

            // Kick/boot a user
            if((msgSplit[0].equalsIgnoreCase("/kick") || msgSplit[0].equalsIgnoreCase("/boot")) && msgSplit.length >= 3){
                // The requesting user should be an admin!
                if(!requestingClient.getClientAuth().equalsIgnoreCase("ADMIN")){
                    // Tell them!
                    this.pushMessage("Server", "You do not have sufficient rights to do this.", clientID, 'P');
                    return false;
                }

                // If they've specified the server, say NO!
                if(msgSplit[1].equalsIgnoreCase("Server")){
                    this.pushMessage("Server", "Kicking me would cause this enitre thing to fail.", clientID, 'P');
                    return false;
                }

                // Part 2 should be a name of someone that's here!
                // Let's check if they exist
                if(this.restrictedClientName(msgSplit[1]) || !this.clientNameExists(msgSplit[1])){
                    // They don't, so let's send the client a message.
                    this.pushMessage("Server", "User '" + msgSplit[1] + "' isn't on this server.", clientID);
                    return false;
                }

                // Get the toClientID
                String toClientID = this.getClientIDByName(msgSplit[1]);

                // Stick all the message together minus 0 and 1
                msgSplit[0] = "";
                msgSplit[1] = "";
                String msg = "";
                for(int i=0; i<msgSplit.length; i++){
                    msg = msg + msgSplit[i];
                    if(!msgSplit[i].equals("")){
                        msg = msg + " ";
                    }
                }

                // Get the client info
                ServerClient client = this.clients.get(toClientID);

                // Now we have a client ID, let's send the kick message!
                this.pushMessage(clientID, msg, toClientID, 'K');

                // Send a public message
                this.pushMessage("Server", client.getClientName() + " has been kicked by " + requestingClient.getClientName());
            }

            // Set server info
            if((msgSplit[0].equalsIgnoreCase("/setserver")) && msgSplit.length >= 3){
                // The requesting user should be an admin!
                if(!requestingClient.getClientAuth().equalsIgnoreCase("ADMIN")){
                    // Tell them!
                    this.pushMessage("Server", "You do not have sufficient rights to do this.", clientID, 'P');
                    return false;
                }

                // Set the key, so it's not overwritten.
                String key = msgSplit[1];

                // Stick all the message back together (except 0 and 1)
                msgSplit[0] = "";
                msgSplit[1] = "";
                String msg = "";
                for(int i=0; i<msgSplit.length; i++){
                    msg = msg + msgSplit[i];
                    if(!msgSplit[i].equals("")){
                        msg = msg + " ";
                    }
                }

                // Set the info
                synchronized(this.serverInfo){
                    this.serverInfo.put(key.toLowerCase(), msg);
                }

                // PM to say successful.
                this.pushMessage("Server", "Server " + key + " set to '" + msg + "'.", clientID, 'P');
                return false;
            }
        } else {
            // Add the message to the messages hashtable.
            //this.messages.add(client.getClientName() + ": " + message);
            this.pushMessage(clientID, message);
        }

        // Return true
        return true;
    }

    // Poll for all recent messages for a client.
    public String[][] talkPoll(String clientID, String clientPass) throws Exception {
        // Security
        if(!security(clientID, clientPass)){
            throw new Exception("INVAL_SECURITY");
        }

        // Update last active for the client
        this.updateLastActive(clientID);

        // Get the client info
        ServerClient client;
        synchronized(this.clients){
            client = this.clients.get(clientID);
        }

        // Get all recent messages
        String[][] recent = new String[this.messages.size()-(client.getLastMessageID()+1)][4];
        for(int i=client.getLastMessageID()+1; i<this.messages.size(); i++){
            // Get message details!
            Message msg = this.messages.get(i);

            // If the message is private, is it this clients private message?
            if(!msg.getToClientID().equals("")){
                // Is it this one?
                if(!msg.getToClientID().equals(clientID)){
                    continue; // Skip this message and go to the next one.
                }
            }

            // Add message details to the recent list.
            recent[i-(client.getLastMessageID()+1)][0] = msg.getFromClientName(); // Client from name
            recent[i-(client.getLastMessageID()+1)][1] = msg.getFromClientID(); // Client from ID
            recent[i-(client.getLastMessageID()+1)][2] = msg.getMessage(); // Message
            recent[i-(client.getLastMessageID()+1)][3] = Character.toString(msg.getMessageType()); // Type
        }

        // Set the latest message ID for this client
        client.setLastMessageID(this.messages.size()-1);

        // Save the client
        synchronized(this.clients){
            this.clients.put(clientID, client);
        }

        // Return the recent messages.
        return recent;
    }

    // Update the last active time of a client
    private void updateLastActive(String clientID){
        synchronized(this.clients){
            ServerClient client = this.clients.get(clientID);
            client.setLastActive(System.currentTimeMillis()/1000);
            this.clients.put(clientID, client);
        }
    }

    // Get the requested client information for "this" client.
    public String getInfo(String clientID, String clientPass, String getInfo) throws Exception {
        // Security
        if(!security(clientID, clientPass)){
            throw new Exception("INVAL_SECURITY");
        }

        // Update last active for the client
        this.updateLastActive(clientID);

        // Get this client's info
        ServerClient client;
        synchronized(this.clients){
            client = this.clients.get(clientID);
        }

        // Let's get the information they've requested.
        if(getInfo.equalsIgnoreCase("clientName")){ // Client name
            return client.getClientName();
        } else if(getInfo.equalsIgnoreCase("clientAuth")){ // Client auth level
            return client.getClientAuth();
        } else if(getInfo.equalsIgnoreCase("connectedClients")){
            // Get all clients
            String[] clients;
            ArrayList<String> admins = new ArrayList<String>();
            ArrayList<String> users = new ArrayList<String>();
            ArrayList<String> guests = new ArrayList<String>();
            synchronized(this.clients){
                // Create the clients array.
                clients = new String[this.clients.size()];

                // To get the names, we need to get the key set from the hashtable
                Set<String> keySet = this.clients.keySet();

                // Iterate through each client and add them to the string.
                Iterator<String> itr = keySet.iterator();
                int arrayCount = 0;
                while(itr.hasNext()){
                    // Get the client ID
                    String str = itr.next();

                    // Get the client
                    ServerClient cli = this.clients.get(str);

                    // Add them to the right array
                    if(cli.getClientAuth().equalsIgnoreCase("ADMIN")){
                        admins.add(cli.getClientName() + "," + cli.getClientAuth());
                    } else if(cli.getClientAuth().equalsIgnoreCase("USER")){
                        users.add(cli.getClientName() + "," + cli.getClientAuth());
                    } else if(cli.getClientAuth().equalsIgnoreCase("GUEST")){
                        guests.add(cli.getClientName() + "," + cli.getClientAuth());
                    }

                    // Add client info to the string
                    //clients[arrayCount] = cli.getClientName() + "," + cli.getClientAuth();
                    //clients[arrayCount][1] = cli.getClientAuth();
                    //arrayCount++;
                    //clients = clients + ";" + cli.getClientName() + "," + cli.getClientAuth();
                }
            }

            // Sort alphabetically
            //Arrays.sort(clients, String.CASE_INSENSITIVE_ORDER);
            Collections.sort(admins, String.CASE_INSENSITIVE_ORDER);
            Collections.sort(users, String.CASE_INSENSITIVE_ORDER);
            Collections.sort(guests, String.CASE_INSENSITIVE_ORDER);

            // Make a string
            String clientsStr = "";
            for(int i=0; i<admins.size(); i++){
                clientsStr = clientsStr + ";" + admins.get(i);
            }
            for(int i=0; i<users.size(); i++){
                clientsStr = clientsStr + ";" + users.get(i);
            }
            for(int i=0; i<guests.size(); i++){
                clientsStr = clientsStr + ";" + guests.get(i);
            }

            // Return the clients
            return clientsStr;
        } else if(getInfo.substring(0, 11).equalsIgnoreCase("ServerInfo_")){
            // Get the key
            String key = getInfo.substring(11);

            // Does this key exist?
            if(this.serverInfo.containsKey(key)){
                // Return the info
                return this.serverInfo.get(key);
            } else {
                return "";
            }
        }

        // Default case
        return "";
    }

    // Check whether a client's display name is restricted.
    private boolean restrictedClientName(String name){
        // Firstly, we've got a list of disallowed names!
        ArrayList<String> disallowed = new ArrayList<String>();
        disallowed.add("admin");
        disallowed.add("root");
        disallowed.add("r00t");
        disallowed.add("r0ot");
        disallowed.add("ro0t");
        disallowed.add("server");
        disallowed.add("moderator");

        // Check the name doesn't match any of these!
        Iterator<String> dis = disallowed.iterator();
        while(dis.hasNext()){
            // Ge tthe string
            String d = dis.next();

            // Matches?
            if(name.equalsIgnoreCase(d)){
                return true;
            }

            // Check if it contains any of the disallowed names!
            if(name.contains(d)){
                return true;
            }
        }

        // Return false!
        return false;
    }

    // Check whether a given name exists on the server.
    private boolean clientNameExists(String name){
        boolean result = false;

        synchronized(this.clients){
            // To check the names, we need to get the key set from the hashtable
            Set<String> keySet = this.clients.keySet();

            // Iterate through each client and see if this name exists!
            Iterator<String> itr = keySet.iterator();
            while(itr.hasNext()){
                String str = itr.next();

                // Get the stored client
                ServerClient client = this.clients.get(str);

                // Does it match?
                // If so, set the result.
                if(client.getClientName().equalsIgnoreCase(name)){
                    result = true;
                    break;
                }
            }
        }

        // Return the result.
        return result;
    }

    // Get a client ID by their display name.
    private String getClientIDByName(String name){
        synchronized(this.clients){
            // Get the key set for the hashtable.
            Set<String> keySet = this.clients.keySet();

            // Iterate through all the users until we find ours!
            Iterator<String> itr = keySet.iterator();
            while(itr.hasNext()){
                String str = itr.next();

                // Get the stored client
                ServerClient c = this.clients.get(str);

                // Does their username match?
                if(c.getClientName().equalsIgnoreCase(name)){
                    return c.getClientID();
                }
            }
        }

        // We haven't found it if we've got this far, so return an empty string
        return "";
    }

    // Attempt to set a client's display name.
    public String setDisplayName(String clientID, String clientPass, String clientName) throws Exception {
        // Security
        if(!security(clientID, clientPass)){
            throw new Exception("INVAL_SECURITY");
        }

        // Strip all rubbish from the names!
        clientName = clientName.replaceAll("[^A-Za-z0-9_]", "");

        // If this name is restricted, start this all again with "Guest"
        String newName = "";
        if(this.restrictedClientName(clientName)){
            // Change back to a guest
            this.pushMessage("Server", "Restricted username chosen.", clientID);
            return this.setDisplayName(clientID, clientPass, "Guest");
        } else {

            // Check whether this display name is allowed.
            boolean allowed = !this.clientNameExists(clientName);

            if(!allowed){
                this.pushMessage("Server", "Invalid client name - forcing change.", clientID);
            }

            // If it's not allowed, try appending numbers until it is!
            int number = 0;
            while(!allowed){
                // Create new name
                newName = clientName;
                newName = newName + "_" + Integer.toString(number);

                // Check...
                allowed = !this.clientNameExists(newName);

                // If this is allowed, set it as the new name!
                if(allowed){
                    clientName = newName;
                } else { // Else increment the number
                    number++;
                }
            }
        }

        String oldName;
        synchronized(this.clients){
            // Update the client
            ServerClient client = this.clients.get(clientID);
            oldName = client.getClientName();
            client.setClientName(clientName);
            this.clients.put(clientID, client);
        }

        // Add a message to let everyone know!
        if(!oldName.equals("")){
            //this.messages.add(oldName + " has changed his/her display name to " + clientName + "\n");
            this.pushMessage("Server", oldName + " has changed"
                    + " his/her display name to " + clientName + ".");
        }

        // Return the name we've agreed on!
        return clientName;
    }

    // Handle a client leaving the chat server.
    public void quit(String clientID, String clientPass) throws Exception {
        // Security
        if(!security(clientID, clientPass)){
            throw new Exception("INVAL_SECURITY");
        }

        ServerClient client;
        synchronized(this.clients){
            // Get the client
            client = this.clients.get(clientID);

            // Delete the client from the client list.
            this.clients.remove(clientID);
        }

        // Now, let's tell everyone!
        //this.messages.add("Server: " + client.getClientName() + " has quit :(\n");
        this.pushMessage("Server", client.getClientName() + " has quit :(");
    }

    // Push a message to the system
    private void pushMessage(String fromClientID, String msg, String toClientID, char type){
        // Form a new message
        Message message = new Message(msg + "\n");

        // Get the fromClient info
        ServerClient fromClient;
        synchronized(this.clients){
            fromClient = this.clients.get(fromClientID);
        }

        // Set the fromClient info
        message.setFromClient(fromClient.getClientID(),
                              fromClient.getClientName());
        // If the toClientID isn't null or empty - add it!
        if(this.clients.containsKey(toClientID)){
            // Get the toClient info
            ServerClient toClient;
            synchronized(this.clients){
                toClient = this.clients.get(toClientID);
            }

            // Set the toClient info
            message.setToClient(toClient.getClientID(),
                                toClient.getClientName());
        }

        // If the message has a type, set it - must previously have been general message!
        if(type != ' ' && message.getMessageType() == 'G'){
            message.setMessageType(type);
        }

        // Store it
        this.messages.add(message);
    }
    // Overloading!
    private void pushMessage(String fromClientID, String msg, String toClientID){
        pushMessage(fromClientID, msg, toClientID, ' ');
    }
    private void pushMessage(String fromClientID, String msg, char type){
        pushMessage(fromClientID, msg, "", type);
    }
    private void pushMessage(String fromClientID, String msg){
        pushMessage(fromClientID, msg, "", ' ');
    }

    public static void main(String[] args){
        // Create a new server instance.
        Server server = new Server();

        // Publish it
        Endpoint.publish("http://localhost:8081/ChatterBox", server);

        // Now start the thread
        Thread sThread = new Thread(server);
        sThread.start();
    }
}