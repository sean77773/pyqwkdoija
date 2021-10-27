package testserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;


public class VerySimpleHttpServer {
    private static final int PORT = 8000;
	private HttpServer server;


	
	public VerySimpleHttpServer() throws Exception {
		this.server = HttpServer.create(new InetSocketAddress(this.PORT),100);
        this.server.createContext("/", new RequestHandler());
        this.server.setExecutor(null); 
        this.server.start();
	}
	
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
	
	public void closeServer() {
		this.server.stop(0);
	}
	// this takes the requests and handles which response to give by delegating to helper methods.
	class RequestHandler implements HttpHandler {
		
		@Override
		public void handle(HttpExchange t) throws IOException {
			 String response = "";
			 if(t.getRequestMethod().equals("GET")) {
				 response = "This is a simple response to GET request.";
			 } else if(t.getRequestMethod().equals("POST")) {
				 String requestBody = getRequestBody(t);
				 response = "This is a simple response to POST request containing body: " + requestBody;
			 }
			
	          t.sendResponseHeaders(200, response.length());
	          OutputStream os = t.getResponseBody();
	          os.write(response.getBytes());
	          os.close();
			 }
		}
}

	

