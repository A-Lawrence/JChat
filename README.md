# JChat System

This is a chat system in Java which I created for a third year University module.  It makes use of Java WebService libraries.

NB: This implementation of a chat server does **_not_** use Glassfish.
Only the command line is required to compile and run the program.

## Running the Server
1. Open a command prompt.
2. Navigate to the directory containing **“JChatServer”** and **“JChatClient”**.
3. Compile the server with **javac JChatServer/*.java**.
4. Run the server with **java JChatServer.Server**
   * You may run this in the background using **JChatServer.Server &**.

## Running the Client
1. Open a command prompt.
2. Navigate to the directory containing **JChatServer** and **JChatClient** directories.
3. Compile the client with **javac JChatClient/*.java**.
4. Run the client with **java JChatClient.Client**
   * You may run this in the background, using **java JChatClient.Client &**.

## Features
* Three user levels: GUEST, USER and ADMIN.
  *GUEST only allows read access.
  *USER allows read and write access.
  *ADMIN allows read and write access along with admin commands.
* Automatic termination of idle clients (if a client is idle for 2 minutes).
* Security token for clients!
  * Upon connection, clients give the server a unique pass-phrase and the server gives the client a unique ID. 
Both pieces of information are required to ensure that the client is who we expect for any future interactions with the server.
* Endless list of client commands.
* Ability to change nickname on the fly.
* Ability to send private messages.
* Automatic reconnect of client when server “goes away”.
* Colour coded user list for easy identification of user groups.
* Colour coded messages for easier reading.  


## / Commands
The following commands can be executed by clients.
**NB**: Parameters surrounded by **<> are required**; Parameters surrounded by *[] are optional*.

#### /nick *<username>*
Change *your* username to <username>.  If it’s not available, _x (where x is an integer) will be appended to the end of the username.

###### Example
/nick Anthony
Server: Guest has changed his/her display name to Anthony.

#### /msg *<username> <message>*
Send a private message to <username>.  If the user doesn’t exist, an error is given.
Aliases of this command are: /m /pm

###### Example
/pm TestDummy This is a test message.
Private message from Anthony: This is a test message [Only seen by TestDummy]

#### /me *<action>*
Sends an action message to the chat room!

###### Example
/me is writing the chat room documentation.
* Anthony is writing the chat room documentation.

#### /auth *<password> *[flags]*
Authenticates *you* as an admin on the server - password is samaria.

##### Optional Flags
Q – Doesn’t announce to the channel that you have been upgraded to admin.

###### Example 1
/auth samaria
Server: User 'Anthony' is now an admin.

###### Example 2
/auth samaria Q
Private message from Server: You are now an admin.

#### /deauth *[username]*
If no username is given, de-authenticates *you* as an administrator of the server.

##### Optional username
When a username is given, that user is de-authenticated as an admin of the server.

###### Example 1
/deauth
Server: User 'Anthony' is no longer an admin.

###### Example 2
/deauth TestDummy
Server: User ‘TestDummy’ is no longer an admin.

#### /warn *<username> <reason>*
Provides a warning to a user, with any reason you specify.

###### Example
/warn TestDummy We are testing.
*** Anthony gave you a warning: We are testing. [Only seen by TestDummy]


#### /kick *<username> <reason>*
Forcefully removes the given user from the server, with a reason.

###### Example
/kick TestDummy We warned you.
Server: TestDummy has been kicked by Anthony
*** Anthony has kicked you because: We warned you! [Only seen by TestDummy]
Server: TestDummy has quit :(

#### /silence *<username>*
Silence a user by demoting them to GUEST status.

###### Example
/silence TestDummy
Server: User 'TestDummy' has been silenced.
Server: You are only a guest; guests cannot speak. [Only seen by TestDummy when he tries to send a message]

#### /unsilence *<username>*
Allow a user to speak by promoting them to USER status.

###### Example
/unsilence TestDummy
Server: User 'TestDummy' is allowed to talk again.

#### /setserver *<key> <value>*
Set a server setting (key) to the given value.

###### Example
/setserver name This is my server!
Private message from Server: Server name set to 'This is my server!'. [All clients will then be updated]
