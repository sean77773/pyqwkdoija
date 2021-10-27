package testserver;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import com.sun.net.httpserver.HttpServer;



import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.util.HashMap;
import java.util.Random;
class TestServerTest {
	
	private HashMap<String, String> addrUsernameMap = new HashMap<String, String>();
	private HashMap<String, String> addrPlayerMap = new HashMap<String, String>();
	private static final String USERNAME_PROMPT = "Please Enter a Username between 2 and 10 characters long.";
	private static final String WAITING_MSG = "Waiting for second player..";

		

	
	// generate GET Request
	public HttpRequest genGETRequest(String IP, String context) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(context))
                .setHeader("X-Forwarded-For", IP)
                .GET()
                .build();
        return request;
	}

	// generate POST request containing String
	public HttpRequest genPOSTRequest(String IP, String body, String context) {
		 HttpRequest request = HttpRequest.newBuilder()
		          .uri(URI.create(context))
		          .setHeader("X-Forwarded-For", IP)
		          .POST(BodyPublishers.ofString(body))
		          .build();
		 return request;
	}
	
	// helper method to generate random IP
	public String generateIP() {
	    Random ip = new Random();
	    return ip.nextInt(256) + "." + ip.nextInt(256) + "." + ip.nextInt(256) + "." + ip.nextInt(256);
	}
	

	
	
	@Test
	// Check if getRequestBody() can decode body message from HttpExchange intermediary, sent with HttpRequest from HttpClient
	// do this by creating custom HttpHandler in TestServer.java which exclusively responds with the return value of getRequestBody(HttpRequest.body())
	// so the Response.body() should always be equal to request.body()
	void testGetRequestBody() throws Exception{
		TestServer testServer = new TestServer(7070);
		HttpClient testClient = HttpClient.newHttpClient();
		String IP = generateIP();
		String body = "HELLO THERE";
		String context = "http://localhost:7070/getRequestBody";
		HttpRequest request = genPOSTRequest(IP, body, context);
		HttpResponse<String> response = testClient.send(request, HttpResponse.BodyHandlers.ofString());
		assertEquals(body, response.body());
	    
		// Let's reuse generateIP to generate some random Strings and test that response.body() == request.body()
		// An arbitrary number of times
		int counter = 0;
		while(counter < 10) {
			counter++;
			body = generateIP();
			request = genPOSTRequest(IP, body, context);
			response = testClient.send(request, HttpResponse.BodyHandlers.ofString());
			assertEquals(body, response.body());
			
		}
		// Test against false positives
		body = "RESPOND WITH ME";
		String notBody = "I am not the body";
		request = genPOSTRequest(IP, body, context);
		response = testClient.send(request, HttpResponse.BodyHandlers.ofString());
		assertEquals(body, response.body());
		assertNotEquals(notBody, response.body());
		testServer.close();
	}
	
	
	// Test pregame GET response
	// If senders address is in address 
	@Test
	void testPregameResponseGET() throws Exception{
		TestServer testServer = new TestServer(7080);
		// No players in game, should be username prompt
		// IP not present in addrUsernameMap which means player hasnt entered username/connected
		String IP = generateIP();
		assertEquals(USERNAME_PROMPT, testServer.pregameResponseGET(IP));
		// Add user to dictionary
        String username = "Gimli";
		testServer.addToAddrUsernameMap(IP, username);
		assertEquals(WAITING_MSG, testServer.pregameResponseGET(IP));
		// Remove user from dictionary
		testServer.removeFromAddrUsernameMap(IP);
		assertEquals(USERNAME_PROMPT, testServer.pregameResponseGET(IP));
		testServer.close();
	}
	
	public StringBuilder board(Game game) {
		StringBuilder response = new StringBuilder();
    	response.append(game.toString());
    	response.append(System.getProperty("line.separator"));
    	return response;
		
	}
	
	@Test
	// Tests for when two players are connected (ingame)
	void testIngameResponseGET() throws Exception{
		TestServer testServer = new TestServer(7090);
		HttpClient testClient = HttpClient.newHttpClient();
		String playerOneIP = generateIP();
		String playerTwoIP = generateIP();
		String usernameOne = "Gimli";
		String usernameTwo = "Arteta";
		String context = "http://localhost:7090/inGameResponseGET";
		// Add player1 and player2 to the two main hashmaps
		testServer.addToAddrUsernameMap(playerOneIP, usernameOne);
		testServer.addToAddrUsernameMap(playerTwoIP, usernameTwo);
		
		testServer.addToAddrPlayerMap(playerOneIP, "playerone");
		testServer.addToAddrPlayerMap(playerTwoIP, "playertwo");
        // Check we get the "make a move" prompt if it is player who made request's turn
		HttpRequest request = genGETRequest(playerOneIP, context);
		HttpResponse<String> response = testClient.send(request, HttpResponse.BodyHandlers.ofString());
		StringBuilder board = board(testServer.getGame());
		String expectedMsg = "Its your turn Gimli, please enter column (1-9):";
		board.append(expectedMsg);
		expectedMsg = board.toString();
		assertEquals(expectedMsg, response.body());
		// Else check we get the "waiting on other player" message if its not player who made request's turn
		board = board(testServer.getGame());
		request = genGETRequest(playerTwoIP, context);
		board.append("Waiting on Gimli to make their move.");
		expectedMsg = board.toString();
		response = testClient.send(request, HttpResponse.BodyHandlers.ofString());
		assertEquals(expectedMsg, response.body());
		// Change turn and see reponse
		testServer.changeTurns();
		board = board(testServer.getGame());
		request = genGETRequest(playerTwoIP, context);
		board.append("Its your turn Arteta, please enter column (1-9):");
		expectedMsg = board.toString();
		response = testClient.send(request, HttpResponse.BodyHandlers.ofString());
		assertEquals(expectedMsg, response.body());
		// Simulate Board being full of pieces with no winner and assert proper response body
		testServer.changeIsOver();
		board = board(testServer.getGame());
		request = genGETRequest(playerTwoIP, context);
		board.append("Game is over. Nobody won.");
		expectedMsg = board.toString();
		response = testClient.send(request, HttpResponse.BodyHandlers.ofString());
		assertEquals(expectedMsg, response.body());
		// Simulate Gimli Winning and appropriate response message is correct
		testServer.setMovedLast("Gimli");
		testServer.changeIsWon();
		board = board(testServer.getGame());
		request = genGETRequest(playerTwoIP, context);
		board.append("Gimli has won the game.");
		expectedMsg = board.toString();
		response = testClient.send(request, HttpResponse.BodyHandlers.ofString());
		assertEquals(expectedMsg, response.body());
		testServer.close();
	
	}
	
	@Test
	// Tests for handling username (i.e. POST request greater than 1 character)
	// and subsequent player number assigning, particularly when a player disconnects
	// and the method must allocate him the available username
	public void testHandleUsername() throws Exception{
		// Handle 0 players connected, should welcome player 1 (Gimli)
		TestServer testServer = new TestServer(7050);
		String playerOneIP = generateIP();
		String playerTwoIP = generateIP();
		String usernameOne = "Gimli";
		String usernameTwo = "Arteta";
		String response = testServer.handleUsername(playerOneIP, usernameOne);
		String expectedMsg = "Welcome Gimli. You are Player One.";
		assertEquals(expectedMsg, response);
		// Now get 2nd player (Arteta) to POST username
		response = testServer.handleUsername(playerTwoIP, usernameTwo);
		expectedMsg = "Welcome Arteta. You are Player Two.";
		assertEquals(expectedMsg, response);
		// Simulate Player 1 leaving the game, a new player joining and 
		// ensuring that he gets assigned as player 1 after entering username
		// also simulatenously checking that it changes his name to Arteta (1) if he enters same name
		// as other player 
		addrUsernameMap.remove(playerOneIP);
		addrPlayerMap.remove(playerOneIP);
		String playerThreeIP = generateIP();
		String usernameThree = "Arteta";
		response = testServer.handleUsername(playerThreeIP, usernameThree);
		expectedMsg = "Welcome Arteta (1). You are Player One.";
		testServer.close();
	}
	
	@Test
	// Tests for handling making moves
	public void testHandleMove() throws Exception{
		TestServer testServer = new TestServer(7040);
		String playerOneIP = generateIP();
		String playerTwoIP = generateIP();
		String usernameOne = "Gimli";
		String usernameTwo = "Arteta";
		//  [ ][ ][ ][ ][ ][ ][ ][ ][o]
		//	[ ][ ][ ][ ][ ][ ][ ][ ][x]
		//	[ ][ ][ ][ ][ ][ ][ ][ ][o]
		//	[ ][ ][ ][ ][ ][ ][ ][ ][x]
		//	[ ][ ][ ][ ][ ][ ][ ][ ][o]
		//	[ ][ ][ ][ ][ ][ ][ ][ ][x]
		// First handleMove(8) six times to get above board
		// assert it equals String representation of above
		int num_rows = 6;
		int num_cols = 9;
		StringBuilder res = new StringBuilder();
		for(int row = 0; row < num_rows; row++) {
			StringBuilder line = new StringBuilder();
			for(int col = 0; col < num_cols; col++) {
				if(col == num_cols - 1) {
					if(row % 2 == 0) {
						line.append("[o]");
					}else {
						line.append("[x]");
					}
				} else {
				     line.append("[ ]");
				  }
			}
			res.append(line);
			res.append(System.getProperty("line.separator"));
		}
		res.append(System.getProperty("line.separator"));
		res.append("Its your turn Gimli, please enter column (1-9):");
		// This is the string rep. of above commented board
		String expectedBoard = res.toString();
		String context = "http://localhost:7040/handleMove";
		int row = num_rows -1;
		HttpClient testClient = HttpClient.newHttpClient();
		// Add users to 2 main hashmaps
		testServer.addToAddrUsernameMap(playerOneIP, usernameOne);
		testServer.addToAddrUsernameMap(playerTwoIP, usernameTwo);
		
		testServer.addToAddrPlayerMap(playerOneIP, "playerone");
		testServer.addToAddrPlayerMap(playerTwoIP, "playertwo");
		// doesn't matter what request body is here (representing what column to add to)
		// as the value is hardcoded as 8 in the Testserver class
		// This is because getting access to HttpExchange is tricky and I
		// can't add more arguments (body/col) to the HttpHandler method that takes in a HttpExchange as argument
		// I know this is bad practice but not enough time to figure out how to fix above
		String body = "2";
		HttpRequest request = genPOSTRequest(playerOneIP, body, context);
		HttpResponse<String> response = testClient.send(request, HttpResponse.BodyHandlers.ofString());
		while(row > 0) {
			row--;
			response = testClient.send(request, HttpResponse.BodyHandlers.ofString());
		}
		String result = response.body();
		
		assertEquals(expectedBoard, result);
		
		// Next Ensure it returns Column Full message to user if they try to add to that column again
		response = testClient.send(request, HttpResponse.BodyHandlers.ofString());
		assertEquals("Column is full. Please Enter a different column.", response.body());
        
		// Next we need to ensure that upon doing a winning move, the game will start again
		testServer.close();
		testServer = new TestServer(7040);
		testServer.addToAddrUsernameMap(playerOneIP, usernameOne);
		testServer.addToAddrUsernameMap(playerTwoIP, usernameTwo);
		
		testServer.addToAddrPlayerMap(playerOneIP, "playerone");
		testServer.addToAddrPlayerMap(playerTwoIP, "playertwo");
		// Populate column with 5 x's in a row
		row = num_rows -1;
		int counter = 0;
		int numToWin = 5;
		while(counter < numToWin) {
			counter++;
			response = testClient.send(request, HttpResponse.BodyHandlers.ofString());
		    testServer.changeTurns();
		}
		
		// Ensure that all cells in bottom row are empty to asser that a new game ahs started and a new board has been made
		int col = 0;
		while(col < num_cols) {
			assertTrue(testServer.getCellValue(row, col) == ' ');
			col++;
		}
		testServer.close();
	}

}