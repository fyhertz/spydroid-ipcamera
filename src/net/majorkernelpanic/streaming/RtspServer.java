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

package net.majorkernelpanic.streaming;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.media.MediaRecorder;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;

/**
 *   RtspServer (RFC 2326)
 *   One client at a time only
 * 
 */
public class RtspServer  extends Thread implements Runnable {
	
	private final String TAG = "RTSPServer";

	// Status code definitions
	private static final String STATUS_OK = "200 OK";
	private static final String STATUS_BAD_REQUEST = "400 Bad Request";
	private static final String STATUS_NOT_FOUND = "404 Not Found";
	
	// Message types sent to UI Thread
	public static final int MESSAGE_H264_TEST = 1;
	public static final int MESSAGE_LOG = 2;
	
	private ServerSocket server = null; 
	private Socket client = null;
	private InputStream is = null;
	private OutputStream os = null;
	private Handler handler = null;
	private String request, response;
	private byte[] buffer = new byte[4096];
	private int port, seqid = 1;	
	private boolean running = false;
	
	public StreamingService streamingService = new StreamingService();
	
	public RtspServer(int port, Handler handler) {
		this.port = port;
		this.handler = handler;
	}

	public void start() {
		super.start();
		log("RTSP Server running !");
	}
	
	public void run() {
		
		try {
			server = new ServerSocket(port);
		} catch (IOException e) {
			Log.e(TAG,e.getMessage());
			return;
		}
		
		while (handleClient());
		
	}
	
	public boolean handleClient() {
		
		int len = 0;
		
		try {
			client = server.accept();
			is = client.getInputStream();
			os = client.getOutputStream();
		} catch (IOException e) {
			Log.e(TAG,e.getMessage());
			return true;
		}
		
		streamingService.setDestination(getClientAddress());
		streamingService.flush();
		
		log("Connection from "+getClientAddress().getHostAddress());
		
		while (true) {
			
			try {
				len = is.read(buffer,0,buffer.length);
			} catch (IOException e) {
				break;
			}
			if (len<=0) break;
			
			request = new String(buffer,0,len);
			
			/* Command Describe */
			if (request.startsWith("DESCRIBE")) commandDescribe();
			/* Command Options */
			else if (request.startsWith("OPTIONS")) commandOptions();
			/* Command Setup */
			else if (request.startsWith("SETUP")) commandSetup();
			/* Command Play */
			else if (request.startsWith("PLAY")) commandPlay();
			/* Command Teardown */
			else if (request.startsWith("TEARDOWN")) {commandTeardown();break;}
			/* Command Unknown */
			else commandUnknown();
			
		}
		
		log("Streaming stopped !");
		streamingService.stopAll();
		
		try {
			client.close();
			log("Client disconnected");
		} catch (IOException e) {
			return true;
		}
		
		return true;
		
	}
	
	/* ********************************************************************************** */
	/* ******************************** Command DESCRIBE ******************************** */
	/* ********************************************************************************** */
	private void commandDescribe() {
		
		// Can't run H264Test from this thread because it has no Looper
		// UI Thread then adds H264 Track
		handler.obtainMessage(MESSAGE_H264_TEST).sendToTarget();
		
	}
	
	public void h264TestResult(VideoQuality videoQuality, String[] params, SurfaceHolder holder) {
		streamingService.addH264Track(MediaRecorder.VideoSource.CAMERA, 5006, params, videoQuality, holder);
		respondDescribe();
		
	}
	
	private void respondDescribe() {
		
		String requestContent = streamingService.getSessionDescriptor();
		String requestAttributes = "Content-Base: "+getServerAddress()+":"+port+"/\r\n" +
							"Content-Type: application/sdp\r\n";
		
		writeHeader(STATUS_OK,requestContent.length(),requestAttributes);
		writeContent(requestContent);
		
		streamingService.prepareAll();
		
	}
			

	/* ********************************************************************************** */
	/* ******************************** Command OPTIONS ********************************* */
	/* ********************************************************************************** */
	private void commandOptions() {
		writeHeader(STATUS_OK,0,"Public: DESCRIBE,SETUP,TEARDOWN,PLAY\r\n");
		writeContent("");
	}
		
	/* ********************************************************************************** */
	/* ********************************** Command SETUP ********************************* */
	/* ********************************************************************************** */
	private void commandSetup() {
			
		String p2,p1;
		Pattern p; Matcher m;
		int ssrc, trackId;
		
		p = Pattern.compile("client_port=(\\d+)-(\\d+)",Pattern.CASE_INSENSITIVE);
		m = p.matcher(request);
		
		if (!m.find()) {
			writeHeader(STATUS_BAD_REQUEST,0,"");
			writeContent("");
			return;
		}
		
		p1 = m.group(1); p2 = m.group(2);
		
		p = Pattern.compile("trackID=(\\w+)",Pattern.CASE_INSENSITIVE);
		m = p.matcher(request);
		
		if (!m.find()) {
			writeHeader(STATUS_BAD_REQUEST,0,"");
			writeContent("");
			return;
		}
		
		trackId = Integer.parseInt(m.group(1));
		
		if (!streamingService.trackExists(trackId)) {
			writeHeader(STATUS_NOT_FOUND,0,"");
			writeContent("");
			return;
		}
		
		ssrc = streamingService.getTrackSSRC(trackId);
		
		String attributes = "Transport: RTP/AVP/UDP;unicast;client_port="+p1+"-"+p2+";server_port=54782-54783;ssrc="+Integer.toHexString(ssrc)+";mode=play\r\n" +
							"Session: "+ "1185d20035702ca" + "\r\n" +
							"Cache-Control: no-cache\r\n";
		
		writeHeader(STATUS_OK,0,attributes);
		writeContent("");

		log("Streaming started !");
		streamingService.startAll();
		
	}
		
	/* ********************************************************************************** */
	/* ********************************** Command PLAY ********************************** */
	/* ********************************************************************************** */
	private void commandPlay() {
		
		String requestAttributes = "RTP-Info: ";
		requestAttributes += "url=rtsp://"+getServerAddress()+":"+port+"/trackID="+0+";seq=0;rtptime=0,";
		requestAttributes = requestAttributes.substring(0, requestAttributes.length()-1) + "\r\nSession: 1185d20035702ca\r\n";
		
		writeHeader(STATUS_OK,0,requestAttributes);
		writeContent("");
		
	}
	
	/* ********************************************************************************** */
	/* ******************************** Command TEARDOWN ******************************** */
	/* ********************************************************************************** */
	private void commandTeardown() {
		
		writeHeader(STATUS_OK,0,"");
		writeContent("");
		
	}
	
	/* ********************************************************************************** */
	/* ******************************* Command Unknown !? ******************************* */
	/* ********************************************************************************** */	
	private void commandUnknown() {
		writeHeader(STATUS_BAD_REQUEST,0,"");
		writeContent("");
	}

	private void writeHeader(String status, int length,String attributes) {
		
		boolean match;
		Pattern p = Pattern.compile("CSeq: (\\d+)",Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(request); 
		
		match = m.find();
		if (match) seqid = Integer.parseInt(m.group(1));
		
		response = 	"RTSP/1.0 "+status+"\r\n" +
					(match?("Cseq: " + seqid + "\r\n"):"") +
					"Content-Length: " + length + "\r\n" +
					attributes +
					"\r\n";
		
	}
	
	private void writeContent(String content) {
		
		response += content;
		
		try {
			os.write(response.getBytes(),0, response.length());
		} catch (IOException e) {

		}
		
	}
	
	/**
	 * 
	 * @return Returns local address
	 */
	private String getServerAddress() {
		return client.getLocalAddress().getHostAddress();
	}
	
	/**
	 * 
	 * @return Returns client address
	 */
	private InetAddress getClientAddress() {
		return client.getInetAddress();
	}
	 
	/**
	  * 
	  * @param msg String to send to the UI Thread
	  */
	private void log(String msg) {
		handler.obtainMessage(MESSAGE_LOG, msg).sendToTarget();
	}
	
	
}
