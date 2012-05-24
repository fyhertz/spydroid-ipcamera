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

package net.majorkernelpanic.libstreaming;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import android.media.MediaRecorder;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;

/**
 * 
 *   RtspServer (RFC 2326)
 *   One client handled at a time only
 * 
 */
public class RtspServer implements Runnable {
	
	private final static String TAG = "RTSPServer";

	// Message types for UI thread
	public static final int MESSAGE_LOG = 2;
	public static final int MESSAGE_START = 3;
	public static final int MESSAGE_STOP = 4;
	
	// The RTSP server his just a remote interface for controlling a streamingManager
	public final StreamingManager streamingManager;
	
	private ServerSocket server = null; 
	private Socket client = null;
	private Handler handler = null;
	private int port;
	private VideoQuality defaultVideoQuality = null;
	private SurfaceHolder surfaceHolder = null;
	private boolean running = false, defaultSoundEnabled = true;
	private OutputStream output = null;
	
	public RtspServer(StreamingManager streamingManager, int port, Handler handler) {
		this.port = port;
		this.handler = handler;
		this.streamingManager = streamingManager;
	}

	/** */
	public void setDefaultVideoQuality(VideoQuality quality) {
		defaultVideoQuality = quality;
	}
	
	/** */
	public void setSurfaceHolder(SurfaceHolder sh) {
		surfaceHolder = sh;
	}
	
	/** */
	public void setDefaultSoundOption(boolean enable) {
		defaultSoundEnabled = enable;
	}
	
	public void start() {
		if (!running) {
			try {
				server = new ServerSocket(port);
				new Thread(this).start();
				running = true;
			} catch (IOException e) {
				log(e.getMessage());
			}
		}
	}
	
	public void stop() {
		if (running) {
			running = false;
			try {
				if (client != null) client.close();
				server.close();
				streamingManager.flush();
			} catch (IOException ignore) {}
		}
	}
	
	public void run() {
		
		BufferedReader input = null;
		Request request;
		Response response;
		
		Log.i(TAG,"RTSP Server started");
		
		while (running) {
			
			try {
				client = server.accept();
				input = new BufferedReader(new InputStreamReader(client.getInputStream()));
				output = client.getOutputStream();
			} catch (SocketException e) {
				break;
			} catch (IOException e) {
				log(e.getMessage());
				continue;
			}
			
			streamingManager.startNewSession();
			streamingManager.setDestination(getClientAddress());
			
			log("Connection from "+getClientAddress().getHostAddress());
			
			while (true) {
				
				try {
					// Parse the request
					request = Request.parseRequest(input);
					// Do something accordingly
					response = processRequest(request);
					// Send response
					response.send(output);
				} catch (SocketException e) {
					// Client disconnected, we can now wait for another client to connect
					break;
				} catch (IllegalStateException e) {
					// Invalid request
					Log.e(TAG,"Bad request");
					continue;
				} catch (IOException e) {
					// Invalid request
					Log.e(TAG,"Bad request");
					continue;
				} 
				
			}
			
			// Streaming stop when client disconnect
			streamingManager.stopAll();
			// Inform the UI Thread that streaming has stopped
			handler.obtainMessage(MESSAGE_STOP).sendToTarget();
			
			try {
				client.close();
			} catch (IOException ignore) {
				
			} finally {
				log("Client disconnected");
			}
			
		}
		Log.i(TAG,"RTSP Server stopped");
		
	}
	
	public Response processRequest(Request request) throws IllegalStateException, IOException{
		Response response = new Response(request);
		
		/* ********************************************************************************** */
		/* ********************************* Method DESCRIBE ******************************** */
		/* ********************************************************************************** */
		if (request.method.toUpperCase().equals("DESCRIBE")) {
			
			if (surfaceHolder==null) {
				throw new IllegalStateException("setSurfaceHolder() should be called before a client connects");
			}
			
			// Here we parse the requested URI
			List<NameValuePair> params = URLEncodedUtils.parse(URI.create(request.uri),"UTF-8");
			if (params.size()>0) {
				for (Iterator<NameValuePair> it = params.iterator();it.hasNext();) {
					NameValuePair param = it.next();
					
					// H264
					if (param.getName().equals("h264")) {
						VideoQuality quality = defaultVideoQuality.clone();
						String[] config = param.getValue().split("-");
						try {
							quality.bitRate = Integer.parseInt(config[0])*1000; // conversion to bit/s
							quality.frameRate = Integer.parseInt(config[1]);
							quality.resX = Integer.parseInt(config[2]);
							quality.resY = Integer.parseInt(config[3]);
						}
						catch (IndexOutOfBoundsException ignore) {}
						log("H264: "+quality.resX+"x"+quality.resY+", "+quality.frameRate+" fps, "+quality.bitRate+" bps");
						streamingManager.addH264Track(MediaRecorder.VideoSource.CAMERA, 5006, quality, surfaceHolder);
					}
					
					// AMRNB
					else if (param.getName().equals("amrnb")) {
						log("ARMNB");
						streamingManager.addAMRNBTrack(5004);
					}
					
					// FLASH ON/OFF
					else if (param.getName().equals("flash")) {
						if (param.getValue().equals("on")) {
							streamingManager.setFlashState(true);
						} 
						else {
							streamingManager.setFlashState(false);
						}
					}
					
					// ROTATION
					else if (param.getName().equals("rotation")) {
						defaultVideoQuality.orientation = Integer.parseInt(param.getValue());
					}
					
				}
			} 
			// Uri has no parameters: the default behaviour is to add one h264 track and one amrnb track
			else {
				streamingManager.addH264Track(MediaRecorder.VideoSource.CAMERA, 5006, defaultVideoQuality, surfaceHolder);
				if (defaultSoundEnabled) {
					streamingManager.addAMRNBTrack(5004);
				}
			}
			
			String requestContent = streamingManager.getSessionDescriptor();
			String requestAttributes = "Content-Base: "+getServerAddress()+":"+port+"/\r\n" +
					"Content-Type: application/sdp\r\n";
			
			streamingManager.startAll();
			
			handler.obtainMessage(MESSAGE_START).sendToTarget();
			
			response.status = Response.STATUS_OK;
			response.attributes = requestAttributes;
			response.content = requestContent;
			
		}
		
		/* ********************************************************************************** */
		/* ********************************* Method OPTIONS ********************************* */
		/* ********************************************************************************** */
		else if (request.method.toUpperCase().equals("OPTIONS")) {
			response.status = Response.STATUS_OK;
			response.attributes = "Public: DESCRIBE,SETUP,TEARDOWN,PLAY,PAUSE\r\n";
		}

		/* ********************************************************************************** */
		/* ********************************** Method SETUP ********************************** */
		/* ********************************************************************************** */
		else if (request.method.toUpperCase().equals("SETUP")) {
			String p2,p1;
			Pattern p; Matcher m;
			int ssrc, trackId;
			
			p = Pattern.compile("trackID=(\\w+)",Pattern.CASE_INSENSITIVE);
			m = p.matcher(request.uri);
			
			if (!m.find()) {
				response.status = Response.STATUS_BAD_REQUEST;
				return response;
			} 
			
			trackId = Integer.parseInt(m.group(1));
			
			if (!streamingManager.trackExists(trackId)) {
				response.status = Response.STATUS_NOT_FOUND;
				return response;
			}
			
			p = Pattern.compile("client_port=(\\d+)-(\\d+)",Pattern.CASE_INSENSITIVE);
			m = p.matcher(request.headers.get("Transport"));
			
			if (!m.find()) {
				int port = streamingManager.getTrackPort(trackId);
				p1 = String.valueOf(port);
				p2 = String.valueOf(port+1);
			}
			else {
				p1 = m.group(1); p2 = m.group(2);
			}
			
			ssrc = streamingManager.getTrackSSRC(trackId);
			
			String attributes = "Transport: RTP/AVP/UDP;unicast;client_port="+p1+"-"+p2+";server_port=54782-54783;ssrc="+Integer.toHexString(ssrc)+";mode=play\r\n" +
								"Session: "+ "1185d20035702ca" + "\r\n" +
								"Cache-Control: no-cache\r\n";
			
			response.status = Response.STATUS_OK;
			response.attributes = attributes;
		}

		/* ********************************************************************************** */
		/* ********************************** Method PLAY *********************************** */
		/* ********************************************************************************** */
		else if (request.method.toUpperCase().equals("PLAY")) {
			String requestAttributes = "RTP-Info: ";
			requestAttributes += "url=rtsp://"+getServerAddress()+":"+port+"/trackID="+0+";seq=0;rtptime=0,";
			requestAttributes = requestAttributes.substring(0, requestAttributes.length()-1) + "\r\nSession: 1185d20035702ca\r\n";
			
			response.status = Response.STATUS_OK;
			response.attributes = requestAttributes;
		}


		/* ********************************************************************************** */
		/* ********************************** Method PAUSE ********************************** */
		/* ********************************************************************************** */
		else if (request.method.toUpperCase().equals("PAUSE")) {
			response.status = Response.STATUS_OK;
		}

		/* ********************************************************************************** */
		/* ********************************* Method TEARDOWN ******************************** */
		/* ********************************************************************************** */
		else if (request.method.toUpperCase().equals("TEARDOWN")) {
			response.status = Response.STATUS_OK;
		}
		
		/* Method Unknown */
		else {
			Log.e(TAG,"Command unknown: "+request);
			response.status = Response.STATUS_BAD_REQUEST;
		}
		
		return response;
		
	}
	
	private String getServerAddress() {
		return client.getLocalAddress().getHostAddress();
	}
	
	private InetAddress getClientAddress() {
		return client.getInetAddress();
	}
	 
	private void log(String msg) {
		handler.obtainMessage(MESSAGE_LOG, msg).sendToTarget();
		Log.i(TAG,msg);
	}
	
	private static class Request {
		
		// Parse method & uri
		public static final Pattern regexMethod = Pattern.compile("(\\w+) (\\S+) RTSP",Pattern.CASE_INSENSITIVE);
		// Parse a request header
		public static final Pattern rexegHeader = Pattern.compile("(\\S+):(.+)",Pattern.CASE_INSENSITIVE);
		
		public String method;
		public String uri;
		public HashMap<String,String> headers = new HashMap<String,String>();
		
		/** Parse the method, uri & headers of a RTSP request */
		public static Request parseRequest(BufferedReader input) throws IOException, IllegalStateException, SocketException {
			Request request = new Request();
			String line;
			Matcher matcher;

			// Parsing request method & uri
			if ((line = input.readLine())==null) throw new SocketException();
			matcher = regexMethod.matcher(line);
			matcher.find();
			request.method = matcher.group(1);
			request.uri = matcher.group(2);

			// Parsing headers of the request
			while ( (line = input.readLine()) != null && line.length()>3 ) {
				matcher = rexegHeader.matcher(line);
				matcher.find();
				request.headers.put(matcher.group(1),matcher.group(2));
			}
			if (line==null) throw new SocketException();
			
			Log.e(TAG,request.method+" "+request.uri);
			
			return request;
		}
	}
	
	private static class Response {
		
		// Status code definitions
		public static final String STATUS_OK = "200 OK";
		public static final String STATUS_BAD_REQUEST = "400 Bad Request";
		public static final String STATUS_NOT_FOUND = "404 Not Found";
		
		public String status = STATUS_OK;
		public String content = "";
		public String attributes = "";
		private final Request request;
		
		public Response(Request request) {
			this.request = request;
		}
		
		public void send(OutputStream output) throws IOException {
			int seqid = -1;
			
			try {
				seqid = Integer.parseInt(request.headers.get("Cseq"));
			} catch (Exception ignore) {}
			
			String response = 	"RTSP/1.0 "+status+"\r\n" +
					(seqid>=0?("Cseq: " + seqid + "\r\n"):"") +
					"Content-Length: " + content.length() + "\r\n" +
					attributes +
					"\r\n" + 
					content;
			
			Log.d(TAG,response);
			
			output.write(response.getBytes());
		}
	}
		
	
	
}
