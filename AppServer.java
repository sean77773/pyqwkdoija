package testserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class AppServer {
    private static final int PORT = 8000;
	private HttpServer server;
	private Game game;
	private HashMap<String, String> addrUsernameMap = new HashMap<String, String>();
	private HashMap<String, String> addrPlayerMap = new HashMap<String, String>();
	private HashMap<String, Boolean> playerTurnMap = new HashMap<String, Boolean>();
	private HashMap<String, Integer> addrNumReqsMap = new HashMap<String, Integer>();
	private String movedLast;
	private HashMap<String, Integer> inputMoveMap = new HashMap<String, Integer>();
	private static final String USERNAME_PROMPT = "Please Enter a Username between 2 and 10 characters long.";
	private static final String WAITING_MSG = "Waiting for second player..";
	private int lastPlayer1Reqs;
	private int lastPlayer2Reqs;
	
	// These two variables are involved in helping the losing player receive a response with the board state
	// and winner declared after a winning move is made. Without it, the losing player doesn't get notified
	// and gets a message with a blank board and who's turn it is (which should come after the winning notification)
	// this issue occurs because when playerX makes a winning move, a response is sent back containing the board state and 
	// the fact they won and then straight after the isWon variable is set back to false as a new game has started,
	// meaning playerY's update GET requests never pass through the "if(isWon){winner message} part of the control flow
	private String lastWonBoard;
	private HashMap<String, Boolean> addrWinNotifyMap = new HashMap<String, Boolean>();
	// true if game that just finished finished via a win, false otherwise (i.e. board became full and no winner)
	private boolean finishedByWin;


	

	
	public AppServer() throws Exception {
		this.server = HttpServer.create(new InetSocketAddress(this.PORT),100);
		// set player's turns
		this.playerTurnMap.put("playerone", true);
		this.playerTurnMap.put("playertwo", false);
		// Map user input to integers (1 less to deal with zero indexing)
		this.inputMoveMap.put("1", 0);
		this.inputMoveMap.put("2", 1);
		this.inputMoveMap.put("3", 2);
		this.inputMoveMap.put("4", 3);
		this.inputMoveMap.put("5", 4);
		this.inputMoveMap.put("6", 5);
		this.inputMoveMap.put("7", 6);
		this.inputMoveMap.put("8", 7);
		this.inputMoveMap.put("9", 8);
		this.game = new Game();
        this.server.createContext("/", new PlayerHandler());
        this.server.setExecutor(null); 
        this.server.start();
	}
	
	
	// this takes the requests and handles which response to give by delegating to helper methods.
    class PlayerHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange t) throws IOException {
            String addr = t.getRequestHeaders().get("X-Forwarded-For").get(0);
        	String response = "";
        	
        	// Handle all In-game requests (i.e. 2 players playing)
        	if(addrUsernameMap.size() == 2) {
        		if(!addrUsernameMap.containsKey(addr)) {
        		    response = "Game is full.";    
           // Client is a player, send appropriate response to GET and POST requests.
        		}else {
        			//Handle if request is GET and use GET response method for ingame requests
        			if(t.getRequestMethod().equals("GET")) {
        				response = ingameResponseGET(t);
        			}
        			// Handle if request is POST
        	        // ingame POST Requests (after 2 players joined) can only be to try make moves (i.e. no username entry as that is done at start)
        	        // So request body must be 1-9
        			else{
        			    String playNum = addrPlayerMap.get(addr);
        			    // if it is requesting player's turn trying to make move, then -
        			    if(playerTurnMap.get(playNum)) {
        			    	String requestBody = getRequestBody(t);
        			    	// if input is NOT 1-9 then just use ingameResponseGET to prompt them to enter 1-9 again
        			    	if(!inputMoveMap.containsKey(requestBody)) {
        			    		response = ingameResponseGET(t);
        			    	}else {
        			    	// if input IS valid then use handleMove to make move.
        			    		response = handleMove(requestBody, t);

        			    		
        			    	}
        			    // If its not requesting players turn then just use ingameResponseGET to prompt them to wait
        			    }else {
        			    	response = ingameResponseGET(t);
        			    }
        			}
        		}
        	}
        	// Handle all pre-game requests (i.e. when less than 2 players have joined)
        	else{
        		// if get request => do generateGetResponse
        		// else if post request => do addUsername (check that post body is 2-8 characters long)
        		//                --- if requester's address is already in addrUNameMap then return generateGetResponse
        		if(t.getRequestMethod().equals("GET")) {
    				response = pregameResponseGET(addr);
    			}else{
    				String requestBody = getRequestBody(t);
    				// If user has not already entered their username then call handleUsername
    				if(requestBody.length() > 2 && requestBody.length() <= 10 && !addrUsernameMap.containsKey(addr)) {
    					response = handleUsername(addr, requestBody);
    				// In any other case, send them the appropriate response using pregameResponseGET
    				}else {
    					response = pregameResponseGET(addr);
    				}
    			}
        	}
        
          // increment total number of requests related to this IP address
          if(addrNumReqsMap.containsKey(addr)){
        	  int newReqVal = addrNumReqsMap.get(addr) + 1;
        	  addrNumReqsMap.put(addr, newReqVal);
          }
          // Send response
          t.sendResponseHeaders(200, response.length());
          OutputStream os = t.getResponseBody();
          os.write(response.getBytes());
          os.close();

        }
    }
    
    // Simple method which takes a HttpExchange object and formats and returns its body as a string
    public String getRequestBody(HttpExchange t) throws IOException{
    	InputStreamReader inputStream =  new InputStreamReader(t.getRequestBody(),"utf-8");
    	BufferedReader bufferedRead = new BufferedReader(inputStream);
    	int b;
    	StringBuilder buf = new StringBuilder(512);
    	while ((b = bufferedRead.read()) != -1) {
    	    buf.append((char) b);
    	}
    	bufferedRead.close();
    	inputStream.close();
    	String requestBody = buf.toString();
    	return requestBody;
    	
    }
    
    
    // Generate pregame GET responses for when there is less than 2 clients joined
    public String pregameResponseGET(String addr) {
    	if(addrUsernameMap.containsKey(addr)) {
    		return WAITING_MSG;
    	}else {
    		return USERNAME_PROMPT;
    	}
    }
    
    //Generates GET responses for when 2 clients are connected
    public String ingameResponseGET(HttpExchange t) {
    	// First thing is get the IP address from X-forwarded-for header
    	String addr = t.getRequestHeaders().get("X-Forwarded-For").get(0);
    	// Every response starts with the board being printed followed by an instruction
    	// the instruction, e.g. "please enter column 1-9" is appended based on the game state
    	// and who's turn it currently is and who sent the request
    	// this is done using the HashMaps addrUsernameMap, addrPlayerMap, and playerTurnMap
    	StringBuilder response = new StringBuilder();
    	response.append(game.toString());
    	response.append(System.getProperty("line.separator"));
        // First check if game has just finished and new one started, notify losing player of win
    	// as otherwise they don't get notified and jump straight into a new game
    	if(this.addrWinNotifyMap.get(addr) == true) {
    		response = new StringBuilder();
    		response.append(this.lastWonBoard);
    		response.append(System.getProperty("line.separator"));
    		if(this.finishedByWin == true) {
    			response.append(this.movedLast + " has won the game.");	
    		}else {
    			response.append("Game is over. Nobody won.");
    		}
    		// Set back to default values 
    		this.addrWinNotifyMap.put(addr, false);
    		
    	}
    	// Check if game is over and append appropriate response to board string
    	else if(game.getIsOver()) {
    		if(game.getIsWon()) {
    			response.append(this.movedLast + " has won the game.");
    		}else {
    			response.append("Game is over. Nobody won.");
    		}
    	}
    	// If it is player one's turn and the request is from player one, prompt them to make a move.
    	else if(game.getPlayerOneTurn() && addrPlayerMap.get(addr).equals("playerone")) {
    		response.append("Its your turn " + addrUsernameMap.get(addr) + ", please enter column (1-9):" );
    	}
    	// If it is player two's turn and the request is from player two, prompt them to make a move.
    	else if(!game.getPlayerOneTurn() && addrPlayerMap.get(addr).equals("playertwo")){
    		response.append("Its your turn " + addrUsernameMap.get(addr) + ", please enter column (1-9):" );
    	}
    	// if it is a request from a player and its not their turn, tell them they are waiting for other player to make move.
    	else {
    		String otherPlayer;
    		for (String playerAddress : addrUsernameMap.keySet()) {
    		    if(!playerAddress.equals(addr)) {
    		    	otherPlayer = this.addrUsernameMap.get(playerAddress);
    		    	response.append("Waiting on " + otherPlayer + " to make their move.");
    		    }
    		}
    	}
        // return Board + instruction response
    	return response.toString();
    	
    }
    
    //
    public String handleUsername(String addr, String username) {
    	String player = "playerone";
    	// for some reason this line doesn't always get executed when a username enters their username (although not frequently)
    	// (adding address + number of requests to a dictionary, resulting in empty dictionary)
    	// meaning the game can't handle disconnects. Its not very common but very odd behaviour, I'm unsure of the cause.
    	this.addrNumReqsMap.put(addr, 1);
    	if(addrUsernameMap.size() == 1 && addrPlayerMap.containsValue("playerone")) {
    		player = "playertwo";
    		// Set variable for keeping track of number of requests for player 1 that updates every 3 seconds
    		this.lastPlayer2Reqs = 1;
    		// Checking for disconnect called here as clients aren't considered playing the game until they enter their username
    		this.checkDC(addr, 2);
    	}else {
    		this.lastPlayer1Reqs = 1;
    		this.checkDC(addr, 1);

    	}
    	// Handle the case where user enters same name as opponent
    	// "user1" becomes "user1 (1)"
    	if(addrUsernameMap.containsValue(username)) {
    		String suffix = " (1)";
    		username = username.concat(suffix);
    	}
    	this.addrUsernameMap.put(addr, username);
    	this.addrPlayerMap.put(addr, player);
    	this.addrWinNotifyMap.put(addr, false);
 
    	String playNum;
    	if(player.equals("playerone")) {
    		playNum = "Player One.";
    	}else {
    		playNum = "Player Two.";
    	}
    	String response = "Welcome " + username + ". You are " + playNum;
    	return response;
    }
    
    // Method for handling moved i.e. input of 1-9
    public String handleMove(String col, HttpExchange t) {
    	String addr = t.getRequestHeaders().get("X-Forwarded-For").get(0);
    	int intCol = this.inputMoveMap.get(col);
    	String response = "";
    	// First check if user can make move, if column is full then respond to user as such
    	if(this.game.checkColFull(intCol)) {
    		response = "Column is full. Please Enter a different column.";
    	}else {
    	// Move is valid
    		// make turn and update board
    		this.game.takeTurn(intCol);
            // change players' turns
    		this.playerTurnMap.put("playerone", !playerTurnMap.get("playerone"));
    		this.playerTurnMap.put("playertwo", !playerTurnMap.get("playertwo"));
    		// movedLast used for when printing username with winning message
    		this.movedLast = addrUsernameMap.get(addr);
    		response = this.ingameResponseGET(t);
    	}
    	// Start a new game if game is finished.
        if(this.game.getIsOver()) {
          //needed to notify other player of the win
          this.lastWonBoard = game.toString();
          this.addrWinNotifyMap.put(addr, true);
          // check if over by win or board full
          // to give proper notification message to losing player
          if(this.game.getIsWon() == false) {
        	  this.finishedByWin = false;
          }else {
        	  this.finishedByWin = true;
          }
          // Get other player's address and change value for them in hashmap which stores addresses which need notified of win
          // this is used in the controlflow of generating a GET response
          for (String address : this.addrUsernameMap.keySet()) {
        	    if(!address.equals(addr)) {
        	    	this.addrWinNotifyMap.put(address, true);
        	    }
        	}
          // create new game
      	  this.game = new Game();
      	  this.playerTurnMap.put("playerone", true);
      	  this.playerTurnMap.put("playertwo", false);
        }
       return response;
    }
    
   
    // This method checks for clients disconnecting. If the total number of requests this session from the client hasn't changed
    // in 3 seconds then the client is considered disconnected as they should automatically send requests every 40ms.
    // This is a thread which starts upon a player entering their username (i.e. properly connecting to the game)
    // There is a hashmap called addrNumReqsMap whose key/values are IP-address/Numberof requests.
    // Number of requests increments each request. 
    // If a player disconnects the game immediately ends and the server goes back to being a pregame lobby
    // Other user can stay on server and wait for second player to connect then the game can start again
	public void checkDC(String addr, int playerNum){
        Runnable dcPing = new Runnable() {
            public void run() { 
            	int reqCurVal = addrNumReqsMap.get(addr); 
            	if(playerNum == 1) {
            		if(reqCurVal == lastPlayer1Reqs) {
            		    addrUsernameMap.remove(addr);
            			addrPlayerMap.remove(addr);
            			addrNumReqsMap.remove(addr);
            			addrWinNotifyMap.remove(addr);
            			game = new Game();
            			playerTurnMap.put("playerone", true);
            			playerTurnMap.put("playertwo", false);
            			lastPlayer1Reqs = 1;
            		}else {
            			lastPlayer1Reqs = reqCurVal;
            		}	
            	}else if(playerNum == 2){
            		if(reqCurVal == lastPlayer2Reqs) {
            		    addrUsernameMap.remove(addr);
            			addrPlayerMap.remove(addr);
            			addrNumReqsMap.remove(addr);
            			addrWinNotifyMap.remove(addr);
            			game = new Game();
            			playerTurnMap.put("playerone", true);
            			playerTurnMap.put("playertwo", false);
            			lastPlayer2Reqs = 1;
            		}else {
            			lastPlayer2Reqs = reqCurVal;
            		}	
            	}
            }
          };

		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		executor.scheduleAtFixedRate(dcPing, 0, 100, TimeUnit.MILLISECONDS);
	}
    
    // Create server object
    public static void main(String[] args) throws Exception {
    	AppServer appServer = new AppServer();
    }
    
    
}

