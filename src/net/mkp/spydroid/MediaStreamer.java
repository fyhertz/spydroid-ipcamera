package net.mkp.spydroid;

import java.io.IOException;
import java.io.InputStream;

import android.media.MediaRecorder;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;

/* 
 *  Just a MediaRecorder that writes in an OutputStream
 * 
 */

public class MediaStreamer extends MediaRecorder{

	private static int id = 0;
	
	private LocalServerSocket lss = null;
	private LocalSocket receiver, sender = null;
	
	public void prepare() throws IllegalStateException,IOException {
		
		receiver = new LocalSocket();
		try {
			lss = new LocalServerSocket("Spydroid"+id);
			receiver.connect(new LocalSocketAddress("Spydroid"+id));
			receiver.setReceiveBufferSize(500000);
			receiver.setSendBufferSize(500000);
			sender = lss.accept();
			sender.setReceiveBufferSize(500000);
			sender.setSendBufferSize(500000);
			id++;
		} catch (IOException e1) {
			Log.e(SpydroidActivity.LOG_TAG, "What ? It cannot be !!");
			return;
		}
		
		setOutputFile(sender.getFileDescriptor());
		
		try {
			super.prepare();
		} catch (IllegalStateException e) {
			closeSockets();
			throw e;
		} catch (IOException e) {
			closeSockets();
			throw e;
		}
		
	}
	
	public InputStream getOutputStream() {
		
		InputStream out = null;
		
		try {
			out = receiver.getInputStream();
		} catch (IOException e) {
		}

		return out;
		
	}

	
	public void stop() {
		super.stop();
		closeSockets();
	}
	
	private void closeSockets() {
		try {
			lss.close();
			sender.close();
			receiver.close();
		}
		catch (IOException e) {
			Log.e(SpydroidActivity.LOG_TAG,"Error while attempting to close local sockets");
		}
		lss = null; sender = null; receiver = null;
	}
	
}
