/*
 * Copyright (C) 2011-2013 GUIGUI Simon, fyhertz@gmail.com
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

package net.majorkernelpanic.spydroid.api;

import static net.majorkernelpanic.streaming.SessionBuilder.AUDIO_NONE;
import static net.majorkernelpanic.streaming.SessionBuilder.VIDEO_NONE;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;

import net.majorkernelpanic.http.TinyHttpServer;
import net.majorkernelpanic.spydroid.SpydroidApplication;
import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.rtsp.UriParser;

import org.apache.http.HttpEntityEnclosingRequest;
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
import org.apache.http.util.EntityUtils;

import android.util.Log;

/**
 * 
 * HTTP server of Spydroid.
 * 
 * Its document root is assets/www, it contains a little user-friendly website to control spydroid from a browser.
 * 
 * Some commands can be sent to it by sending POST request to "/request.json".
 * See {@link RequestHandler} to find out what kind of commands can be sent.
 * 
 * Streams can also be started/stopped by sending GET request to "/spydroid.sdp".
 * The HTTP server then responds with a proper Session Description (SDP).
 * All supported options are described in {@link UriParser}
 *
 */
public class CustomHttpServer extends TinyHttpServer {

	/** A stream failed to start. */
	public final static int ERROR_START_FAILED = 0xFE;

	/** Streaming started. */
	public final static int MESSAGE_STREAMING_STARTED = 0X00;

	/** Streaming stopped. */
	public final static int MESSAGE_STREAMING_STOPPED = 0X01;

	/** Maximal number of streams that you can start from the HTTP server. **/
	protected static final int MAX_STREAM_NUM = 2;

	private DescriptionRequestHandler mDescriptionRequestHandler;
	private WeakHashMap<Session,Object> mSessions = new WeakHashMap<Session,Object>(2);

	public CustomHttpServer() {

		// The common name that appears in the CA of the HTTPS server of Spydroid
		mCACommonName = "Spydroid CA";

		// If at some point a stream cannot start the exception is stored so that
		// it can be fetched in the HTTP interface to display an appropriate message
		addCallbackListener(mListener);

		// HTTP is used by default for now
		mHttpEnabled = true;
		mHttpsEnabled = false;

	}

	private CallbackListener mListener = new CallbackListener() {
		@Override
		public void onError(TinyHttpServer server, Exception e, int error) {
			if (error==ERROR_START_FAILED) {
				SpydroidApplication.getInstance().lastCaughtException = e;
			}
		}
		@Override
		public void onMessage(TinyHttpServer server, int message) {}
	};

	@Override
	public void onCreate() {
		super.onCreate();
		mDescriptionRequestHandler = new DescriptionRequestHandler();
		addRequestHandler("/spydroid.sdp*", mDescriptionRequestHandler);
		addRequestHandler("/request.json*", new CustomRequestHandler());
	}

	@Override
	public void stop() {
		super.stop();
		// If user has started a session with the HTTP Server, we need to stop it
		for (int i=0;i<mDescriptionRequestHandler.mSessionList.length;i++) {
			if (mDescriptionRequestHandler.mSessionList[i].session != null) {
				boolean streaming = isStreaming();
				mDescriptionRequestHandler.mSessionList[i].session.stop();
				if (streaming && !isStreaming()) {
					postMessage(MESSAGE_STREAMING_STOPPED);
				}
				mDescriptionRequestHandler.mSessionList[i].session.release();
				mDescriptionRequestHandler.mSessionList[i].session = null;
			}
		}

	}

	public boolean isStreaming() {
		for ( Session session : mSessions.keySet() ) {
		    if ( session != null ) {
		    	if (session.isStreaming()) return true;
		    } 
		}
		return false;
	}

	public long getBitrate() {
		long bitrate = 0;
		for ( Session session : mSessions.keySet() ) {
		    if ( session != null ) {
		    	if (session.isStreaming()) bitrate += session.getBitrate();
		    } 
		}
		return bitrate;
	}
	
	class CustomRequestHandler implements HttpRequestHandler {

		public CustomRequestHandler() {}

		public void handle(HttpRequest request, HttpResponse response, HttpContext arg2) throws HttpException, IOException {

			if (request.getRequestLine().getMethod().equals("POST")) {

				// Retrieve the POST content
				HttpEntityEnclosingRequest post = (HttpEntityEnclosingRequest) request;
				byte[] entityContent = EntityUtils.toByteArray(post.getEntity());
				String content = new String(entityContent, Charset.forName("UTF-8"));

				// Execute the request
				final String json = RequestHandler.handle(content);

				// Return the response
				EntityTemplate body = new EntityTemplate(new ContentProducer() {
					public void writeTo(final OutputStream outstream) throws IOException {
						OutputStreamWriter writer = new OutputStreamWriter(outstream, "UTF-8");
						writer.write(json);
						writer.flush();
					}
				});
				response.setStatusCode(HttpStatus.SC_OK);
				body.setContentType("application/json; charset=UTF-8");
				response.setEntity(body);
			}

		}
	}

	/** 
	 * Allows to start streams (a session contains one or more streams) from the HTTP server by requesting 
	 * this URL: http://ip/spydroid.sdp (the RTSP server is not needed here). 
	 **/
	class DescriptionRequestHandler implements HttpRequestHandler {

		private final SessionInfo[] mSessionList = new SessionInfo[MAX_STREAM_NUM];

		private class SessionInfo {
			public Session session;
			public String uri;
			public String description;
		}

		public DescriptionRequestHandler() {
			for (int i=0;i<MAX_STREAM_NUM;i++) {
				mSessionList[i] = new SessionInfo();
			}
		}

		public synchronized void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException {
			Socket socket = ((TinyHttpServer.MHttpContext)context).getSocket();
			String uri = request.getRequestLine().getUri();
			int id = 0;
			boolean stop = false;

			try {

				// A stream id can be specified in the URI, this id is associated to a session
				List<NameValuePair> params = URLEncodedUtils.parse(URI.create(uri),"UTF-8");
				uri = "";
				if (params.size()>0) {
					for (Iterator<NameValuePair> it = params.iterator();it.hasNext();) {
						NameValuePair param = it.next();
						if (param.getName().equalsIgnoreCase("id")) {
							try {	
								id = Integer.parseInt(param.getValue());
							} catch (Exception ignore) {}
						}
						else if (param.getName().equalsIgnoreCase("stop")) {
							stop = true;
						}
					}	
				}

				params.remove("id");
				uri = "http://c?" + URLEncodedUtils.format(params, "UTF-8");

				if (!uri.equals(mSessionList[id].uri)) {

					mSessionList[id].uri = uri;

					// Stops all streams if a Session already exists
					if (mSessionList[id].session != null) {
						boolean streaming = isStreaming();
						mSessionList[id].session.syncStop();
						if (streaming && !isStreaming()) {
							postMessage(MESSAGE_STREAMING_STOPPED);
						}
						mSessionList[id].session.release();
						mSessionList[id].session = null;
					}

					if (!stop) {
						
						boolean b = false;
						if (mSessionList[id].session != null) {
							InetAddress dest = InetAddress.getByName(mSessionList[id].session.getDestination());
							if (!dest.isMulticastAddress()) {
								b = true;
							}
						}
						if (mSessionList[id].session == null || b) {
							// Parses URI and creates the Session
							mSessionList[id].session = UriParser.parse(uri);
							mSessions.put(mSessionList[id].session, null);
						} 

						// Sets proper origin & dest
						mSessionList[id].session.setOrigin(socket.getLocalAddress().getHostAddress());
						if (mSessionList[id].session.getDestination()==null) {
							mSessionList[id].session.setDestination(socket.getInetAddress().getHostAddress());
						}
						
						// Starts all streams associated to the Session
						boolean streaming = isStreaming();
						mSessionList[id].session.syncStart();
						if (!streaming && isStreaming()) {
							postMessage(MESSAGE_STREAMING_STARTED);
						}

						mSessionList[id].description = mSessionList[id].session.getSessionDescription().replace("Unnamed", "Stream-"+id);
						Log.v(TAG, mSessionList[id].description);
						
					}
				}

				final int fid = id; final boolean fstop = stop;
				response.setStatusCode(HttpStatus.SC_OK);
				EntityTemplate body = new EntityTemplate(new ContentProducer() {
					public void writeTo(final OutputStream outstream) throws IOException {
						OutputStreamWriter writer = new OutputStreamWriter(outstream, "UTF-8");
						if (!fstop) {
							writer.write(mSessionList[fid].description);
						} else {
							writer.write("STOPPED");
						}
						writer.flush();
					}
				});
				body.setContentType("application/sdp; charset=UTF-8");
				response.setEntity(body);

			} catch (Exception e) {
				mSessionList[id].uri = "";
				response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
				Log.e(TAG,e.getMessage()!=null?e.getMessage():"An unknown error occurred");
				e.printStackTrace();
				postError(e,ERROR_START_FAILED);
			}

		}

	}

}
