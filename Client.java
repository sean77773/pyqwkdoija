package testserver;
import java.util.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.net.http.HttpRequest.BodyPublishers;


public class Client {
    private HashMap<String, Integer> digits = new HashMap<String, Integer>();
    private HttpClient client = HttpClient.newHttpClient();
    private String IP;
    // switcher to handle connection error messages so "failed to contact server" isn't printed repeatedly while
    // client waits for server to start
    private boolean handledConnectFail = false;
    // This is a variable which keeps track of the last changed state
    // the client automatically sends GET requests to server every 40 milliseconds
    // and if any change is made then it is printed to the user
    private String lastState;
    private static final String CONN_ERROR = "Failed to contact server.";
   
    public Client () {
    	this.digits.put("q", 0);
        this.digits.put("1", 1);
        this.digits.put("2", 2);
        this.digits.put("3", 3);
        this.digits.put("4", 4);
        this.digits.put("5", 5);
        this.digits.put("6", 6);
        this.digits.put("7", 7);
        this.digits.put("8", 8);
        this.digits.put("9", 9);
        this.IP = generateIP();
    }
    
	// Generates a fake IP for identification as testing on local machine
	// This is sent in the x-forwarded-for header
	// I realise now a better idea would be to use something else for ID such as cookies
	public String generateIP() {
	    Random ip = new Random();
	    return ip.nextInt(256) + "." + ip.nextInt(256) + "." + ip.nextInt(256) + "." + ip.nextInt(256);
	}
	
	public void connectError() {
		if(!this.handledConnectFail) {
            System.out.println(CONN_ERROR);
            this.handledConnectFail = true;
		}
	}
    
	// Master input handler which uses helper methods to handle user input
	public void handleInput(String s) throws IOException, InterruptedException{
		// if equals "q" (for query) then send GET request to server
		if(s.equals("q")) {
			String state = this.getState();
			System.out.println(state);
		// if 1-9 then use make move POST request to server
		}else if(this.digits.containsKey(s)){
			this.makeMove(s);
		// if none of the above then try send POST request of with username
		}else {
			this.enterUsername(s);
		}
	}
    

    // If input is greater than 1 character client assumes they are trying to enter a username
    // which will be sent as request to server to be processed and responded to with appropriate response.
	public void enterUsername(String uname) throws IOException, InterruptedException {
		try {
		 HttpRequest request = HttpRequest.newBuilder()
		          .uri(URI.create("http://localhost:8000/"))
		          .setHeader("X-Forwarded-For", this.IP)
		          .POST(BodyPublishers.ofString(uname))
		          .build();
          
		 HttpResponse<String> response = this.client.send(request, HttpResponse.BodyHandlers.ofString());
		 this.lastState = response.body();
		 System.out.println(response.body());
		 // successful contact
		 this.handledConnectFail = false;
		}catch(Exception e) {
			this.connectError();
		}
	}
	
	
	// simple get request sent to server that returns response body as string
	// This is used when the user enters "q" (for query) as input manually
	// and is also automatically called every 40ms in a seperate thread
	// to mimic server push functionality to notify each player when the other completes turn
	// if I had more time I would look into longpolling as it's a better solution 
	// using asynchronous send requests
	// as sending a string over TCP every 40ms to compare to detect changes is inferior to doing that server side and only sending changes
	public String getState() throws IOException, InterruptedException {
		try {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8000/"))
                .setHeader("X-Forwarded-For", this.IP)
                .GET()
                .build();

        HttpResponse<String> response = this.client.send(request,HttpResponse.BodyHandlers.ofString());
		// successful contact
        this.handledConnectFail = false;
        return response.body();
		} catch(Exception e) {
			this.connectError();
			String dummyResponse = "";
			return dummyResponse;
		}

	}
	
	// If user enters 1-9 then this POST request is sent to the server with the move as the body
	public String makeMove(String col) throws IOException, InterruptedException {
		try {
		 HttpRequest request = HttpRequest.newBuilder()
		          .uri(URI.create("http://localhost:8000/"))
		          .setHeader("X-Forwarded-For", this.IP)
		          .POST(BodyPublishers.ofString(col))
		          .build();

		    HttpResponse<String> response = this.client.send(request, HttpResponse.BodyHandlers.ofString());
		    this.lastState = response.body();
		    System.out.println(response.body());
			// successful contact
		    this.handledConnectFail = false;
		    return response.body();
		}catch(Exception e) {
			this.connectError();
			String dummyResponse = "";
			return dummyResponse;
		}

		
	}
	
	// last state used to keep track of changes with the automatic GET requests
	public void setLastState(String s) {
		this.lastState = s;
	}
	public String getLastState() {
		return this.lastState;
	}


	
	// allows user to enter input
	public void play() throws IOException, InterruptedException, Exception{	
		this.checkUpdates();
        while(true) { 
        	Scanner scanner = new Scanner(System.in);
        	String input = scanner.nextLine();
        	this.handleInput(input);
        }
	}
	
	public void setHandledConnectFail() {
		this.handledConnectFail = true;
	}
	
	// check for updates every 40 milliseconds with GET request
	// by comparing newest GET response with last GET request. If they are different then update
	public void checkUpdates() throws Exception{
		this.lastState = this.getState();
		System.out.println(this.lastState);
		// Separate thread to send GET requests every 40ms and print response if different from last
		Runnable updateRunnable = new Runnable(){
		    public void run() {
		    	try {
		    		String newState = getState();
		    		// getState returns empty string if there is an exception
		    		if(newState.equals("")){
		    			connectError();
		    			return;
		    		}
		    		else if(!newState.equals(lastState)) {
		        	    System.out.println(newState);
		        	    lastState = newState;
		        }
		    	handledConnectFail = false;	
		        }catch (Exception e) {
		            return;
		        }
		    }
		};

		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		executor.scheduleAtFixedRate(updateRunnable, 0, 40, TimeUnit.MILLISECONDS);
	}
	
	
	
	
    public static void main(String[] args) throws IOException, InterruptedException, Exception {
    	Client player = new Client();
    	player.play();
    }
}


