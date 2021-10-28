- Compile Game.java.
- Compile and run AppServer.java.
- Compile and run Client.java (per player) and follow onscreen instructions
- All files are part of testserver package

I wrote and tested this using eclipse, and partially tested in CLI.

User Instructions:
- User input can be "q" (for query) to send GET request, 1-9 to make move(POST(9)), or a String between 2-10 characters long to enter username(POST(username)).


Functionality Overview:
- Server starts, allows maximum of two clients to join.
- Client is considered "joined" after they enter their username. IP is logged.
- If another client tries joining while 2 players joined, they are met with "Game is full." response. As soon as a player leaves, the waiting player will get access and a username prompt
- When One user leaves while playing, the other user stays in the lobby and doesn't need to re enter their username or password and stays as playerone or playertwo.
- Joining player becomes whichever player number is free (player 1 or player 2)
- When the game finishes, another one automatically begins
- Users can manually send GET request with 'q' however after username entry any input will result in a GET request apart from 1-9 if it is their turn
- Server is persistent and robust to various scenarios such as disconnects, starting new games
- Client is the same, as soon as there is space in the game they will join and if launched before server starts, they will automatically join when it goes live

Files -> AppServer.java - this is the application server
      -> Client.java - this is the clientside program
      -> Game.java  - This is an object which handles all the game logic

TestFiles -> GameTest.java - unit tests Game.java
              -> ClientTest.java - unit tests Client.java
              -> VerySimpleHttpServer.java - helper class for ClientTest.java that has simple GET and POST responses
              -> TestServer.java - Essentially a copy of AppServer used for testing, with some slight modifications to allow for testing of methods which take HttpExchange as argument
              -> TestServerTest.java - unit tests TestServer
Overview:

- 3 classes: AppServer, Client, Game

- Client program handles input and has a GET method to be called if input = "q", a POST method if input in 1-9, and another POST method if input is greater than 1 character.
  The first POST method handles attempts to make a move and the second handles attempts to enter a username.

- A HTTPRequest is generated from these and sent to the HTTPServer using HTTPExchange. 

- The application server is in charge of all the logic of sending an appropriate response to the client, based on the game state and the client's identifier (IP address).

- Game() initialised in application server

- This Game only really works when using the special Client program, server does not have much error handling for non-standard input (i.e. input not allowed by client)

Thought Process:

I wasn't sure whether to use something like jetty or nginx but I initially felt that the problem description was simple 
enough to warrant using the JDK's built in HTTPClient, HTTPExchange, and HTTPServer. I also toyed with the idea of using TCP
sockets and just having hard coded HTTPResponses and Requests but that felt like cheating and not strictly adhering to HTTP.
The upside of this socket approach (I think) would have been getting more control over the sockets which would have made it 
easier to handle things like disconnects. If I was given a choice on which protocol to use I think websocket would be more 
suitable to handle bidirectional communication than http.

The main issues I identified in the planning phase were having the server push to the clients unprompted when appropriate, identifying users,
and detecting disconnects. For server push I implemented it as the client sending GET requests every 40ms (arbitrarily chosen)
to the server and if there is any change since the last printed state then print that. This is not a very elegant approach.
A better way to do it would have been longpolling where by the client sends an asynchronous request and the server responds
when a change has occured. I felt implementing this would have taken too long and that the constant pinging was servicable.
The issue with the constant requests is that the server is sending back a response (usually the string representation
of the board) and the checking for change is done client-side. This is quite inefficient sending over a reponse every 40ms. 
With longpolling the change is detected server-side so there is no unneccessary requests/responses sent.

I decided to use this constant request sending to handle disconnects as well. Every three seconds ,the application server checks
if the total number of requests from each player has changed since the last check. If it hasn't, the user is considered disconnected
and the game ends as the number should have increased due to the automatic requests. Another player is free to join and the game will start again.

For player identification I decided to use spoofed IP addresses. The client generates a random IP and sends it in the x-forwarded-for header.
I originally used the HttpRequest.getRemoteAddress() method but the IP returned would change based on the method the client input invoked due to
(I think) built in proxies. I realise now that there is better ways for client identification, namely cookies and that IP would not be suitable for a 
proper web deployment due to spoofing and dynamic addresses.

While there is those things I would change if doing it again, I think its a good implementation that smoothly handles things like disconnects and the game ending,
allowing for persistent play.

Bugs/Issues:

Some issues that I'm sure could be fixed relatively easily but unfortunately I don't have enough time.

- Application Server cannot handle pregame disconnects (i.e. first player joins by entering username and disconnects before second player joins by entering username)
- When game is won only the winner sees the "usernameX has won." response. This is an issue caused by having the game restart
  immediately after it is won. To revert to the game ending (and requiring server to restart and clients to rejoin) when someone wins - comment out lines 261 - 264 of the code.
- Overall its not very robust to handle non Client object requests, with more time proper error handling could be put in place.
- Not particularly secure, could be improved upon using things like https and authorisation headers

Testing:

Unit Testing done for all 3 classes with majority of methods tested (excluding getter/setters etc.) as well as manual integration testing.
I know that unit testing should be testing methods of classes independently without having dependencies on other methods/classes but with the way this architecture is set up
a lot of methods take HttpExchange as an argument which is an onobtainable intermediary between HttpRequests and HttpResponses. This can't be instantiated in isolation as far as 
I'm aware so a client and server will have to to send requests/responses to each other to test these methods. 

For unit testing the Client class, a very bare bones helper Server class was made which had responses for GET and POST to properly test the client's request methods.

- Simple server is called VerySimpleServer.java

Unit Testing the Game class was quite straight forward.


For the AppServer class, a test version of the class was created. It is essentially a copy/paste except with more getters/setters to simulate a game being played within the 
methods being tested. A separate class was chosen because of the lack of control HttpExchange offers. Because of this lack of control, any method being tested that took HttpExchange
as an argument required its own server context and corresponding unique HttpHandler as seen on lines 61 - 97 of TestServer.java.

- The test version of the class is called TestServer.java
- and the unit testing class is called TestServerTest.java

Thoughts before submitting:

If doing this again I would probably try to implement longpolling or server side events as a cleaner solution to faking server pushes. I would probably use jetty as well to make things easier.
In AppServer.java, the control flow is a little convoluted when processing requests in the inner PlayerHandler class' handle() method. I would probably refactor this and make it more modular.
There is also portions of code in some files where there is copy/paste repeated code which probably could be consolidated but this would be refactored if given more time. In the Game class,
the methods for testing if there is a 5 in a row are a little brute-forcish as they can check unneccesary squares (i.e. cells that couldn't be involved in a win with the last place piece). For example
when checking if there is a horizontal 5 in a row at the last played piece, the entire row is scanned for a 5 in a row of that piece. This isn't really an issue due to the size of the board but there's 
definitely more elegant solutions to check for a win. 

Please play around with it (try break it) and see how it handles the following cases:

- client launching with no server launched (client gets "Failed to connect to server.") 
- client launching with no server launched followed by a server launch (you will get "Failed to connect to server." then as soon as the server starts you will get username prompt)
- various combinations of clients entering username(i.e. joining) and then shutting down the client (i.e. disconnecting) at various stages of the game
- fill up lobby with 2 players and launch other clients and then have client programs which are playing shut down


Looking forward to getting feedback on this! 

Sean








