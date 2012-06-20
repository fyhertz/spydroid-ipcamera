package net.majorkernelpanic.spydroid;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.URLDecoder;

import net.majorkernelpanic.http.BasicHttpServer;
import net.majorkernelpanic.http.ModifiedHttpContext;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import android.content.res.AssetManager;

/**
 * This is an HTTP interface for spydroid
 * For example: "http://xxx.xxx.xxx.xxx:8080/spydroid.sdp?h264" 
 * would start a video stream from the phone's camera to some remote client
 * and return an appropriate sdp file
 */
public class HttpServer extends BasicHttpServer{

	public HttpServer(int port, AssetManager assetManager) {
		super(port, assetManager);
		addRequestHandler("/spydroid.sdp*", new DescriptorRequestHandler());
	}
	
	static class DescriptorRequestHandler implements HttpRequestHandler {

		private Session session;
		
		public synchronized void handle(HttpRequest request, HttpResponse response,
				HttpContext context) throws HttpException, IOException {
			Socket socket = ((ModifiedHttpContext)context).getSocket();
			
			// Stop all streams if a Session already exists
			if (session != null) {
				session.stopAll();
				session.flush();
			}
			
			// Create new Session
			session = new Session(socket.getInetAddress());
			
			// Parse URI and configure the Session accordingly 
			final String uri = URLDecoder.decode(request.getRequestLine().getUri());
			UriParser.parse(uri, session);
			
			final String sessionDescriptor = 
					"v=0\r\n" +
					"o=- 15143872582342435176 15143872582342435176 IN IP4 "+socket.getLocalAddress().getHostName()+"\r\n"+
					"s=Unnamed\r\n"+
					"i=N/A\r\n"+
					"c=IN IP4 "+socket.getLocalAddress().getHostAddress()+"\r\n"+
					"t=0 0\r\n"+
					"a=tool:spydroid\r\n"+
					"a=recvonly\r\n"+
					"a=type:broadcast\r\n"+
					"a=charset:UTF-8\r\n"+
					session.getSessionDescriptor();
			
        	response.setStatusCode(HttpStatus.SC_OK);
        	EntityTemplate body = new EntityTemplate(new ContentProducer() {
        		public void writeTo(final OutputStream outstream) throws IOException {
        			OutputStreamWriter writer = new OutputStreamWriter(outstream, "UTF-8"); 
        			writer.write(sessionDescriptor);
        			writer.flush();
        		}
        	});
        	body.setContentType("text/plain; charset=UTF-8");
        	response.setEntity(body);
			
			// Start all streams associated to the Session
			session.startAll();
			
		}
		
	}
	
}

