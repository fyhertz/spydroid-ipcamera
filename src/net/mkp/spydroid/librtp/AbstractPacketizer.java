package net.mkp.spydroid.librtp;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.SocketException;

abstract public class AbstractPacketizer extends Thread implements Runnable{

	protected SmallRtpSocket rsock = null;
	protected InputStream fis = null;
	protected boolean running = false;
	
	protected byte[] buffer = new byte[4096];	
	
	protected final int rtphl = 12; 				// Rtp header length
	
	
	public AbstractPacketizer(InputStream fis, InetAddress dest, int port) throws SocketException {
		
		this.fis = fis;
		this.rsock = new SmallRtpSocket(dest, port, buffer);
		
	}
	
	
	public void startStreaming() {
		running = true;
		start();
	}

	public void stopStreaming() {
		running = false;
	}
	
	abstract public void run();
	
	
	
}
