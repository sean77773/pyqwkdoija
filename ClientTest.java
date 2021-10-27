package testserver;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ClientTest {
	
	public ClientTest() throws Exception {
		
	}
	// Simple server class created to test http request/response functionality
	// Responds with a simple message to get requests
	@Test
	void testGETState() throws Exception{
		Client testClient = new Client();
		VerySimpleHttpServer testServer = new VerySimpleHttpServer();
		String response = testClient.getState();
        String expectedResponse = "This is a simple response to GET request.";
		assertEquals(expectedResponse, response);
		testServer.closeServer();
		Thread.sleep(50);
	}
	
	@Test
	// Simple Server responds to requests with the request body
	void testMakeMove() throws Exception{
		Client testClient = new Client();
		VerySimpleHttpServer testServer = new VerySimpleHttpServer();
		String requestBody = "2";
		String response = testClient.makeMove(requestBody);
        String expectedResponse = "This is a simple response to POST request containing body: " + requestBody;
		assertEquals(expectedResponse, response);
		testServer.closeServer();
		Thread.sleep(50);
	}
	@Test
	void testEnterUsername() throws Exception {
		Client testClient = new Client();
		VerySimpleHttpServer testServer = new VerySimpleHttpServer();
		String requestBody = "GIMLI";
		String response = testClient.makeMove(requestBody);
        String expectedResponse = "This is a simple response to POST request containing body: " + requestBody;
		assertEquals(expectedResponse, response);
		testServer.closeServer();
		Thread.sleep(50);
	}
	

}
