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
 * 
 * Based on that: http://hc.apache.org/httpcomponents-core-ga/examples.html.
 * 
 */

package net.majorkernelpanic.http;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.util.Date;
import java.util.Locale;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509KeyManager;

import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpServerConnection;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.impl.cookie.DateParseException;
import org.apache.http.impl.cookie.DateUtils;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.apache.http.util.EntityUtils;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * HTTP server based on this one: http://hc.apache.org/httpcomponents-core-ga/examples.html.
 * You may add some logic to this server with {@link #addRequestHandler(String, HttpRequestHandler)}.
 * By default it serves files from /assets/www.
 */
public class TinyHttpServer extends Service {

	public static final String TAG = "HttpServer";

	/** Default port for HTTP. */
	public final static int DEFAULT_HTTP_PORT = 8080;

	/** Default port for HTTPS. */
	public final static int DEFAULT_HTTPS_PORT = 8443;

	/** The list of MIME Media Types supported by the server. */
	protected static String[] mimeMediaTypes = new String[] {
		"htm",	"text/html", 
		"html",	"text/html", 
		"gif",	"image/gif",
		"jpg",	"image/jpeg",
		"png",	"image/png", 
		"js",	"text/javascript",
		"json",	"text/json",
		"css",	"text/css"
	};

	/** Contains the date corresponding to the last update time the app. was updated. */
	protected static Date mLastModified;

	private static final Object sLock = new Object();

	private ModifiedHttpRequestHandlerRegistry mRegistry;
	private Context mContext;
	private BasicHttpProcessor mHttpProcessor;
	private HttpParams mParams; 

	private HttpRequestListener mHttpRequestListener = null;
	private HttpsRequestListener mHttpsRequestListener = null;

	private int mHttpPort = DEFAULT_HTTP_PORT;
	private int mHttpsPort = DEFAULT_HTTPS_PORT;

	private boolean mHttpEnabled = true, mHttpUpdate = false;
	private boolean mHttpsEnabled = false, mHttpsUpdate = false;

	@Override
	public void onCreate() {

		super.onCreate();

		mContext = getApplicationContext();
		mRegistry = new ModifiedHttpRequestHandlerRegistry();

		mParams = new BasicHttpParams();
		mParams
		.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 5000)
		.setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024)
		.setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
		.setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)
		.setParameter(CoreProtocolPNames.ORIGIN_SERVER, "MajorKernelPanic HTTP Server");

		// Set up the HTTP protocol processor
		mHttpProcessor = new BasicHttpProcessor();
		mHttpProcessor.addInterceptor(new ResponseDate());
		mHttpProcessor.addInterceptor(new ResponseServer());
		mHttpProcessor.addInterceptor(new ResponseContent());
		mHttpProcessor.addInterceptor(new ResponseConnControl());

		// Default request handler: serves file in assets/www
		addRequestHandler("*", new HttpFileHandler(getAssets()));

		// Will be used in the "Last-Modifed" entity-header field
		try {
			String packageName = mContext.getPackageName();
			mLastModified = new Date(mContext.getPackageManager().getPackageInfo(packageName, 0).lastUpdateTime);
		} catch (NameNotFoundException e) {
			mLastModified = new Date(0);
		}
		
		// Let's restore the state of the service 
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		mHttpPort = Integer.parseInt(settings.getString("http_port", String.valueOf(mHttpPort)));
		mHttpsPort = Integer.parseInt(settings.getString("https_port", String.valueOf(mHttpsPort)));
		mHttpEnabled = settings.getBoolean("http_enabled", mHttpEnabled);
		mHttpsEnabled = settings.getBoolean("https_enabled", mHttpsEnabled);
		
		// Starts the HTTP server
		start();
		
		// If the configuration is modified, the server will adjust
		settings.registerOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);

	}

	private OnSharedPreferenceChangeListener mOnSharedPreferenceChangeListener = new OnSharedPreferenceChangeListener() {
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

			if (key.equals("http_port")) {
				mHttpPort = Integer.parseInt(sharedPreferences.getString("http_port", String.valueOf(mHttpPort)));
				setHttpPort(mHttpPort);
				start();
			}
			
			else if (key.equals("https_port")) {
				mHttpsPort = Integer.parseInt(sharedPreferences.getString("https_port", String.valueOf(mHttpsPort)));
				setHttpsPort(mHttpPort);
				start();
			}

			else if (key.equals("https_enabled")) {
				mHttpsEnabled = sharedPreferences.getBoolean("https_enabled", true);
				setHttpsEnabled(mHttpsEnabled);
				start();
			}			

			else if (key.equals("http_enabled")) {
				mHttpEnabled = sharedPreferences.getBoolean("http_enabled", true);
				setHttpEnabled(mHttpEnabled);
				start();
			}
		}
	};
	
	private void setHttpPort(int port) {
		if (port != mHttpPort) mHttpUpdate = true;
		mHttpPort = port;
	}

	private void setHttpsPort(int port) {
		if (port != mHttpsPort) mHttpsUpdate = true;
		mHttpsPort = port;
	}

	private void setHttpEnabled(boolean enable) {
		mHttpEnabled = enable;
	}

	private void setHttpsEnabled(boolean enable) {
		mHttpsEnabled = enable;
	}	
	
	@Override
	public int onStartCommand (Intent intent, int flags, int startId) {
		//Log.d(TAG,"TinyServerHttp started !");
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		stop();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		settings.unregisterOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
	}

	public void start() {
		// Stops the HTTP server if it has been disabled or if it needs to be restarted
		if ((!mHttpEnabled || mHttpUpdate) && mHttpRequestListener != null) {
			mHttpRequestListener.kill();
			mHttpRequestListener = null;
		}
		// Stops the HTTPS server if it has been disabled or if it needs to be restarted
		if ((!mHttpsEnabled || mHttpsUpdate) && mHttpsRequestListener != null) {
			mHttpsRequestListener.kill();
			mHttpsRequestListener = null;
		}
		// Starts the HTTP server if needed
		if (mHttpEnabled && mHttpRequestListener == null) {
			try {
				mHttpRequestListener = new HttpRequestListener(mHttpPort);
			} catch (IOException e) {
				if (mListener != null) mListener.onError(this, e);
				mHttpRequestListener = null;
			}
		}
		// Starts the HTTPS server if needed
		if (mHttpsEnabled && mHttpsRequestListener == null) {
			try {
				mHttpsRequestListener = new HttpsRequestListener(mHttpsPort);
			} catch (IOException e) {
				if (mListener != null) mListener.onError(this, e);
				mHttpsRequestListener = null;
			}
		}

		mHttpUpdate = false;
		mHttpsUpdate = false;

	}

	public void stop() {
		if (mHttpRequestListener != null) {
			// Stops the HTTP server
			mHttpRequestListener.kill();
			mHttpRequestListener = null;
		}
		if (mHttpsRequestListener != null) {
			// Stops the HTTPS server
			mHttpsRequestListener.kill();
			mHttpsRequestListener = null;
		}
	}

	/** Be careful: those callbacks won't necessarily be called from the ui thread ! */
	public interface CallbackListener {

		/** Called when an error occurs. */
		void onError(TinyHttpServer server, Exception e);

	}

	protected CallbackListener mListener = null;

	/**
	 * See {@link TinyHttpServer.CallbackListener} to check out what events will be fired once you set up a listener.
	 * @param listener The listener
	 */
	public void setCallbackListener(CallbackListener listener) { 
		mListener = listener;
	}

	/** The Binder you obtain when a connection with the Service is established. */
	public class LocalBinder extends Binder {
		public TinyHttpServer getService() {
			return TinyHttpServer.this;
		}
	}

	/** See {@link TinyHttpServer.LocalBinder}. */
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	private final IBinder mBinder = new LocalBinder();

	/** 
	 * You may add some HttpRequestHandlers before calling start()
	 * All HttpRequestHandlers added after start() will be ignored
	 * @param pattern Patterns may have three formats: * or *<uri> or <uri>*
	 * @param handler A HttpRequestHandler
	 */ 
	protected void addRequestHandler(String pattern, HttpRequestHandler handler) {
		mRegistry.register(pattern, handler);
	}

	private class HttpRequestListener extends RequestListener {

		public HttpRequestListener(final int port) throws IOException {
			ServerSocket serverSocket = new ServerSocket(port);
			construct(serverSocket);
			Log.i(TAG,"HTTP server listening on port " + serverSocket.getLocalPort());
		}

		protected void kill() {
			super.kill();
			Log.i(TAG,"HTTP server stopped !");
		}

	}

	private class HttpsRequestListener extends RequestListener {

		private X509KeyManager mKeyManager;
		private char[] mPassword;
		private boolean mNotSupported = false;

		private final static String FILE_NAME = "keystore.jks";
		private final static String CLASSPATH = "net.majorkernelpanic.http.X509KeyManager";

		public HttpsRequestListener(final int port) throws IOException {

			mPassword = "COUCOU".toCharArray();

			// We create the X509KeyManager through reflexion so that SSL support can easily be removed if not needed
			try {
				Class<?> X509KeyManager = Class.forName(CLASSPATH);
				Method loadFromKeyStore = X509KeyManager.getDeclaredMethod("loadFromKeyStore", InputStream.class, char[].class);

				try {
					InputStream is = mContext.openFileInput(FILE_NAME);
					mKeyManager = (X509KeyManager) loadFromKeyStore.invoke(null, is, mPassword);
				} catch (FileNotFoundException e) {
					Constructor<?> constructor = X509KeyManager.getConstructor(new Class[]{char[].class});
					mKeyManager = (javax.net.ssl.X509KeyManager) constructor.newInstance(mPassword);
				}
			} catch (Exception e1) {
				// HTTPS support disabled !
				Log.e(TAG,"HTTPS not supported !");
				if (e1 instanceof IOException) throw (IOException)e1;
				else throw new IOException("HTTPS not supported !");
			}

			try {
				SSLContext sslContext = SSLContext.getInstance("SSL");
				sslContext.init(new KeyManager[] {mKeyManager}, null, null);
				ServerSocket serverSocket = sslContext.getServerSocketFactory().createServerSocket(port);
				construct(serverSocket);
				Log.i(TAG,"HTTPS server listening on port " + serverSocket.getLocalPort());
			} catch (Exception e1) {
				Log.e(TAG,"HTTPS server crashed !");
				if (e1 instanceof IOException) throw (IOException)e1;
				else throw new IOException("HTTPS server crashed !");
			}
		}

		protected void kill() {
			if (!mNotSupported) {
				super.kill();
				// Saves all the certificates generated by the our KeyManager in a keystore
				// Again we use reflexion
				try {
					Method saveToKeyStore = Class.forName(CLASSPATH).getDeclaredMethod("saveToKeyStore", OutputStream.class, char[].class);
					synchronized (sLock) {
						// Prevents concurrent write operation in the keystore  
						OutputStream os = mContext.openFileOutput(FILE_NAME, Context.MODE_PRIVATE); 
						saveToKeyStore.invoke(mKeyManager, os, mPassword);
					}
				} catch (Exception e) {
					System.out.println("An error occured while saving the KeyStore");
					e.printStackTrace();
				}
				Log.i(TAG,"HTTPS server stopped !");
			}
		}

	}

	private class RequestListener extends Thread {

		private ServerSocket mServerSocket;
		private final org.apache.http.protocol.HttpService mHttpService;

		protected RequestListener() throws IOException {

			mHttpService = new org.apache.http.protocol.HttpService(
					mHttpProcessor, 
					new DefaultConnectionReuseStrategy(), 
					new DefaultHttpResponseFactory());
			mHttpService.setHandlerResolver(mRegistry);
			mHttpService.setParams(mParams);

		}

		protected void construct(ServerSocket serverSocket) {
			mServerSocket = serverSocket;
			start();
		}

		protected void kill() {
			try {
				mServerSocket.close();
			} catch (IOException ignore) {}
			try {
				this.join();
			} catch (InterruptedException ignore) {}
		}

		public void run() {
			while (!Thread.interrupted()) {
				try {
					// Set up HTTP connection
					Socket socket = this.mServerSocket.accept();
					DefaultHttpServerConnection conn = new DefaultHttpServerConnection();
					Log.d(TAG,"Incoming connection from " + socket.getInetAddress());
					conn.bind(socket, mParams);

					// Start worker thread
					Thread t = new WorkerThread(this.mHttpService, conn, socket);
					t.setDaemon(true);
					t.start();
				} catch (SocketException e) {
					break;
				} catch (InterruptedIOException ex) {
					Log.e(TAG,"Interrupted !");
					break;
				} catch (IOException e) {
					Log.d(TAG,"I/O error initialising connection thread: " 
							+ e.getMessage());
					break;
				}
			}
		}
	}

	private class HttpFileHandler implements HttpRequestHandler  {

		private final AssetManager assetManager;

		public HttpFileHandler(final AssetManager assetManager) {
			super();
			this.assetManager = assetManager;
		}

		public void handle(
				final HttpRequest request, 
				final HttpResponse response,
				final HttpContext context) throws HttpException, IOException {
			AbstractHttpEntity body = null;

			final String method = request.getRequestLine().getMethod().toUpperCase(Locale.ENGLISH);
			if (!method.equals("GET") && !method.equals("HEAD") && !method.equals("POST")) {
				throw new MethodNotSupportedException(method + " method not supported"); 
			}

			final String url = URLDecoder.decode(request.getRequestLine().getUri());
			if (request instanceof HttpEntityEnclosingRequest) {
				HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
				byte[] entityContent = EntityUtils.toByteArray(entity);
				Log.d(TAG,"Incoming entity content (bytes): " + entityContent.length);
			}

			final String location = "www"+(url.equals("/")?"/index.htm":url);
			response.setStatusCode(HttpStatus.SC_OK);

			try {
				Log.i(TAG,"Requested: \""+url+"\"");

				// Compares the Last-Modified date header (if present) with the If-Modified-Since date
				if (request.containsHeader("If-Modified-Since")) {
					try {
						Date date = DateUtils.parseDate(request.getHeaders("If-Modified-Since")[0].getValue());
						if (date.compareTo(mLastModified)<=0) {
							// The file has not been modified
							response.setStatusCode(HttpStatus.SC_NOT_MODIFIED);
							return;
						}
					} catch (DateParseException e) {
						e.printStackTrace();
					}
				}

				// We determine if the asset is compressed
				try {
					AssetFileDescriptor afd = assetManager.openFd(location);

					// The asset is not compressed
					FileInputStream fis = new FileInputStream(afd.getFileDescriptor());
					fis.skip(afd.getStartOffset());
					body = new InputStreamEntity(fis, afd.getDeclaredLength());

					Log.d(TAG,"Serving uncompressed file " + "www" + url);

				} catch (FileNotFoundException e) {

					// The asset may be compressed
					// AAPT compresses assets so first we need to uncompress them to determine their length
					InputStream stream =  assetManager.open(location,AssetManager.ACCESS_STREAMING);
					ByteArrayOutputStream buffer = new ByteArrayOutputStream(64000);
					byte[] tmp = new byte[4096]; int length = 0;
					while ((length = stream.read(tmp)) != -1) buffer.write(tmp, 0, length);
					body = new InputStreamEntity(new ByteArrayInputStream(buffer.toByteArray()), buffer.size());
					stream.close();

					Log.d(TAG,"Serving compressed file " + "www" + url);

				}

				body.setContentType(getMimeMediaType(url)+"; charset=UTF-8");
				response.addHeader("Last-Modified", DateUtils.formatDate(mLastModified));

			} catch (IOException e) {
				// File does not exist
				response.setStatusCode(HttpStatus.SC_NOT_FOUND);
				body = new EntityTemplate(new ContentProducer() {
					public void writeTo(final OutputStream outstream) throws IOException {
						OutputStreamWriter writer = new OutputStreamWriter(outstream, "UTF-8"); 
						writer.write("<html><body><h1>");
						writer.write("File ");
						writer.write("www"+url);
						writer.write(" not found");
						writer.write("</h1></body></html>");
						writer.flush();
					}
				});
				Log.d(TAG,"File " + "www" + url + " not found");
				body.setContentType("text/html; charset=UTF-8");
			}

			response.setEntity(body);

		}

		private String getMimeMediaType(String fileName) {
			String extension = fileName.substring(fileName.lastIndexOf(".")+1, fileName.length());
			for (int i=0;i<mimeMediaTypes.length;i+=2) {
				if (mimeMediaTypes[i].equals(extension)) 
					return mimeMediaTypes[i+1];
			}
			return mimeMediaTypes[0];
		}

	}

	static class WorkerThread extends Thread {

		private final org.apache.http.protocol.HttpService httpservice;
		private final HttpServerConnection conn;
		private final Socket socket;

		public WorkerThread(
				final org.apache.http.protocol.HttpService httpservice, 
				final HttpServerConnection conn,
				final Socket socket) {
			super();
			this.httpservice = httpservice;
			this.conn = conn;
			this.socket = socket;
		}

		public void run() {
			Log.d(TAG,"New connection thread");
			HttpContext context = new ModifiedHttpContext(socket);
			try {
				while (!Thread.interrupted() && this.conn.isOpen()) {
					try {
						this.httpservice.handleRequest(this.conn, context);
					} catch (UnsupportedOperationException e) {
						e.printStackTrace();
						// shutdownOutput is not implemented by SSLSocket, and it is called in the implementation
						// of org.apache.http.impl.SocketHttpServerConnection.close().
					}
				}
			} catch (ConnectionClosedException e) {
				Log.d(TAG,"Client closed connection");
				e.printStackTrace();
			} catch (SocketTimeoutException e) {
				Log.d(TAG,"Socket timeout");
			} catch (IOException e) {
				Log.e(TAG,"I/O error: " + e.getMessage());
			} catch (HttpException e) {
				Log.e(TAG,"Unrecoverable HTTP protocol violation: " + e.getMessage());
			} finally {
				/*try {
					socket.close();
				} catch (IOException e) {}*/
				try {
					this.conn.shutdown();
				} catch (Exception ignore) {}
			}
		}
	}

}