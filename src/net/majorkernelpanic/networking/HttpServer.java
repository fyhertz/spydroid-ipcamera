/*
 * Copyright (C) 2011 GUIGUI Simon, fyhertz@gmail.com
 * 
 * This file is part of Spydroid (http://code.google.com/p/spydroid-ipcamera/)
 * 
 * Spydroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */


package net.majorkernelpanic.networking;

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
import net.majorkernelpanic.spydroid.R;
import net.majorkernelpanic.spydroid.SpydroidActivity;
import net.majorkernelpanic.spydroid.R.raw;

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
	} 
	
	public void stop() {
		super.stop();
		// If user has started a session with the HTTP Server, we need to stop it
		if (DescriptorRequestHandler.session != null) {
			DescriptorRequestHandler.session.stopAll();
			DescriptorRequestHandler.session.flush();
		}
	}
	
	protected static Context context;
	
	/** Allow user to start streams (a session contains one or more streams) from the HTTP server by requesting 
	 * this URL: http://ip/spydroid.sdp (the RTSP server is not needed here) 
	 **/
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

