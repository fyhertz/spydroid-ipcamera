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

import net.majorkernelpanic.libstreaming.video.VideoQuality;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import android.hardware.Camera.CameraInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Implementation of a subset of the RTSP protocol (RFC 2326)
 * This allow remote control of an android device cameras & microphone
 * One client at a time only
 */
public class RtspServer implements Runnable {
	
	private final static String TAG = "RTSPServer";

	// Message types for UI thread
	public static final int MESSAGE_LOG = 2;
	public static final int MESSAGE_START = 3;
	public static final int MESSAGE_STOP = 4;
	
	// The RTSP server is just an interface that drives a streamManager
	public final StreamManager streamManager;
	
	private Socket client = null;
	private Handler handler = null;
	private OutputStream output = null;
	private int port;
	private boolean running = false;
	private ServerSocket server = null; 

	
	/**
	 * Constructor
	 * @param context The context of the app
	 * @param port The port the RtspServer will listen on
	 * @param handler Handler of the UI Thread, it will be used to send messages to the ui
	 */
	public RtspServer(StreamManager streamManager, int port, Handler handler) {
		this.handler = handler;
		this.port = port;
		this.streamManager = streamManager;
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
				streamManager.flush();
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
			
			streamManager.startNewSession();
			streamManager.setDestination(getClientAddress());
			
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
					Log.e(TAG,"Bad request or something wrong with a MediaStream");
					if (e.getMessage()!=null) Log.e(TAG,e.getMessage());
					continue;
				} catch (IOException e) {
					// Invalid request
					Log.e(TAG,"Bad request or something wrong with a MediaStream");
					if (e.getMessage()!=null) Log.e(TAG,e.getMessage());
					continue;
				} 
				
			}
			
			// Streaming stop when client disconnect
			streamManager.stopAll();
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
			
			// Here we parse the requested URI
			List<NameValuePair> params = URLEncodedUtils.parse(URI.create(request.uri),"UTF-8");
			if (params.size()>0) {
				for (Iterator<NameValuePair> it = params.iterator();it.hasNext();) {
					NameValuePair param = it.next();
					
					// H264
					if (param.getName().equals("h264")) {
						VideoQuality quality = VideoQuality.parseQuality(param.getValue());
						streamManager.addVideoTrack(StreamManager.VIDEO_H264, CameraInfo.CAMERA_FACING_BACK, 5006, quality);
					}
					
					// H263
					else if (param.getName().equals("h263")) {
						VideoQuality quality = VideoQuality.parseQuality(param.getValue());
						streamManager.addVideoTrack(StreamManager.VIDEO_H263, CameraInfo.CAMERA_FACING_BACK, 5006, quality);
					}
					
					// AMRNB
					else if (param.getName().equals("amrnb")) {
						streamManager.addAudioTrack(StreamManager.AUDIO_AMRNB, 5004);
					}
					
					// AMR -> just for convenience: does the same as AMRNB
					else if (param.getName().equals("amr")) {
						streamManager.addAudioTrack(StreamManager.AUDIO_AMRNB, 5004);
					}
					
					// AAC -> experimental
					else if (param.getName().equals("aac")) {
						streamManager.addAudioTrack(StreamManager.AUDIO_AAC, 5004);
					}
					
					// Generic Audio Stream -> make use of api level 12
					// TODO: Doesn't work :/
					else if (param.getName().equals("testnewapi")) {
						streamManager.addAudioTrack(StreamManager.AUDIO_ANDROID_AMR, 5004);
					}
					
					// FLASH ON/OFF
					else if (param.getName().equals("flash")) {
						if (param.getValue().equals("on")) {
							streamManager.setFlashState(true);
						} 
						else {
							streamManager.setFlashState(false);
						}
					}
					
					// ROTATION
					else if (param.getName().equals("rotation")) {
						streamManager.defaultVideoQuality.orientation = Integer.parseInt(param.getValue());
					}
					
				}
			} 
			// Uri has no parameters: the default behavior is to add one h264 track and one amrnb track
			else {
				streamManager.addVideoTrack(5006);
				streamManager.addAudioTrack(5004);
			}
			
			String requestContent = streamManager.getSessionDescriptor();
			String requestAttributes = "Content-Base: "+getServerAddress()+":"+port+"/\r\n" +
					"Content-Type: application/sdp\r\n";
			
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
			Pattern p; Matcher m;
			int p2, p1, ssrc, trackId, src;
			
			p = Pattern.compile("trackID=(\\w+)",Pattern.CASE_INSENSITIVE);
			m = p.matcher(request.uri);
			
			if (!m.find()) {
				response.status = Response.STATUS_BAD_REQUEST;
				return response;
			} 
			
			trackId = Integer.parseInt(m.group(1));
			
			if (!streamManager.trackExists(trackId)) {
				response.status = Response.STATUS_NOT_FOUND;
				return response;
			}
			
			p = Pattern.compile("client_port=(\\d+)-(\\d+)",Pattern.CASE_INSENSITIVE);
			m = p.matcher(request.headers.get("Transport"));
			
			if (!m.find()) {
				int port = streamManager.getTrackDestinationPort(trackId);
				p1 = port;
				p2 = port+1;
			}
			else {
				p1 = Integer.parseInt(m.group(1)); 
				p2 = Integer.parseInt(m.group(2));
			}
			
			ssrc = streamManager.getTrackSSRC(trackId);
			src = streamManager.getTrackLocalPort(trackId);
			streamManager.setTrackDestinationPort(trackId, p1);
			
			try {
				streamManager.start(trackId);
				response.attributes = "Transport: RTP/AVP/UDP;unicast;client_port="+p1+"-"+p2+";server_port="+src+"-"+(src+1)+";ssrc="+Integer.toHexString(ssrc)+";mode=play\r\n" +
						"Session: "+ "1185d20035702ca" + "\r\n" +
						"Cache-Control: no-cache\r\n";
				response.status = Response.STATUS_OK;
			} catch (RuntimeException e) {
				log("Could not start stream, configuration probably not supported by phone");
				response.status = Response.STATUS_INTERNAL_SERVER_ERROR;
			}
			
		}

		/* ********************************************************************************** */
		/* ********************************** Method PLAY *********************************** */
		/* ********************************************************************************** */
		else if (request.method.toUpperCase().equals("PLAY")) {
			String requestAttributes = "RTP-Info: ";
			if (streamManager.trackExists(0)) requestAttributes += "url=rtsp://"+getServerAddress()+":"+port+"/trackID="+0+";seq=0;rtptime=0,";
			if (streamManager.trackExists(1)) requestAttributes += "url=rtsp://"+getServerAddress()+":"+port+"/trackID="+1+";seq=0;rtptime=0,";
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
		public static final String STATUS_INTERNAL_SERVER_ERROR = "Internal Server Error";
		
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
					"Server: MajorKernelPanic RTSP Server !\r\n" +
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
