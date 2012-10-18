package net.majorkernelpanic.spydroid;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.net.Socket;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.List;

import net.majorkernelpanic.http.BasicHttpServer;
import net.majorkernelpanic.http.ModifiedHttpContext;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.os.Handler;
import android.util.Log;

/**
 * This is an HTTP interface for spydroid
 * For example: "http://xxx.xxx.xxx.xxx:8080/spydroid.sdp?h264" 
 * would start a video stream from the phone's camera to some remote client
 * and return an appropriate sdp file
 */
public class HttpServer extends BasicHttpServer{

	public HttpServer(int port, Context context, Handler handler) {
		super(port, context.getAssets());
		this.context = context;
		addRequestHandler("/spydroid.sdp*", new DescriptorRequestHandler(handler));
		addRequestHandler("/sound.htm*", new SoundRequestHandler(context, handler));
		addRequestHandler("/js/params.js", new SoundsListRequestHandler(handler));
	} 
	
	public void stop() {
		super.stop();
		// If user has started a session with the HTTP Server, we need to stop it
		if (DescriptorRequestHandler.session != null) {
			DescriptorRequestHandler.session.stopAll();
			DescriptorRequestHandler.session.flush();
		}
	}
	
	private static boolean screenState = true;
	private static Context context;
	
	public void setScreenState(boolean state) {
		screenState  = state;
	}
	
	/** Send an array with all available sounds, and the screenState boolean that indicates if the app is on the foreground */
	static class SoundsListRequestHandler implements HttpRequestHandler {

		private Handler handler;
		private Field[] raws = R.raw.class.getFields();
		
		public SoundsListRequestHandler(Handler handler) {
			this.handler = handler;
		}
		
		public void handle(HttpRequest request, HttpResponse response, HttpContext arg2) throws HttpException, IOException {
			EntityTemplate body = new EntityTemplate(new ContentProducer() {
				public void writeTo(final OutputStream outstream) throws IOException {
					OutputStreamWriter writer = new OutputStreamWriter(outstream, "UTF-8"); 
					writer.write("var sounds = [");
					for(int i=0; i < raws.length-1; i++) {
						writer.write("'"+raws[i].getName() + "',");
					}
					writer.write("'"+raws[raws.length-1].getName() + "'];");
					writer.write("var screenState = "+(screenState?"1":"0")+";");
					writer.flush();
				}
			});

			response.setStatusCode(HttpStatus.SC_OK);
        	body.setContentType("application/json; charset=UTF-8");
        	response.setEntity(body);
        	
        	if (!screenState) {
    			Intent i = new Intent(context,SpydroidActivity.class);
    			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    			context.startActivity(i);
        	}
        	
		}
	}
	
	/**	Play a sound on the phone based on the uri */
	static class SoundRequestHandler implements HttpRequestHandler {

		private Handler handler;
		private Context context;
		private SoundPool soundPool = new SoundPool(4,AudioManager.STREAM_MUSIC,0);
		private Field[] raws = R.raw.class.getFields();
		
		public SoundRequestHandler(Context context, Handler handler) {
			this.handler = handler;
			this.context = context;
			
			soundPool.setOnLoadCompleteListener(new OnLoadCompleteListener() {
				public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
					soundPool.play(sampleId, 0.99f, 0.99f, 1, 0, 1);
				}
			});
		}
		
		public void handle(HttpRequest request, HttpResponse response, HttpContext arg2)
				throws HttpException, IOException {
			
			final String uri = URLDecoder.decode(request.getRequestLine().getUri());
			final List<NameValuePair> params = URLEncodedUtils.parse(URI.create(uri),"UTF-8");
			final String[] content = {"Error"};
			int soundID;
			
			response.setStatusCode(HttpStatus.SC_NOT_FOUND);

			if (params.size()>0) {
	        	try {
	        		for (Iterator<NameValuePair> it = params.iterator();it.hasNext();) {
	        			NameValuePair param = it.next();
	        			// Load sound with appropriate name
	        			if (param.getName().equals("name")) {
	        				for(int i=0; i < raws.length; i++) {
	        					if (raws[i].getName().equals(param.getValue())) {
	        						soundID = soundPool.load(context, raws[i].getInt(null), 0);
	        						response.setStatusCode(HttpStatus.SC_OK);
	        						content[0] = "OK";
	        					}
	        				}
	        			}
	        		}
				} catch (IllegalArgumentException e) {

				} catch (IllegalAccessException e) {

				}
			}
			
        	EntityTemplate body = new EntityTemplate(new ContentProducer() {
        		public void writeTo(final OutputStream outstream) throws IOException {
        			OutputStreamWriter writer = new OutputStreamWriter(outstream, "UTF-8"); 
        			writer.write(content[0]);
        			writer.flush();
        		}
        	});
        	body.setContentType("text/plain; charset=UTF-8");
        	response.setEntity(body);
			
			
		}
		
		
	}
	
	/* Allow user to start streams (a session contains one or more streams) from the HTTP server by requesting 
	 * this URL: http://ip/spydroid.sdp (the RTSP server is not needed here) */
	static class DescriptorRequestHandler implements HttpRequestHandler {

		private static Session session;
		private Handler handler;
		
		public DescriptorRequestHandler(Handler handler) {
			this.handler = handler;
		}
		
		public synchronized void handle(HttpRequest request, HttpResponse response,
				HttpContext context) throws HttpException, IOException {
			Socket socket = ((ModifiedHttpContext)context).getSocket();
			
			// Stop all streams if a Session already exists
			if (session != null) {
				session.stopAll();
				session.flush();
			}
			
			// Create new Session
			session = new Session(socket.getInetAddress(),handler);
			
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

