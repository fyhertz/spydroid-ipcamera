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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.util.Enumeration;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.spongycastle.jce.provider.JDKKeyStore;

import android.util.Log;

public final class X509KeyManager implements javax.net.ssl.X509KeyManager {

	public final String TAG = "X509KeyManager";

	private char[] mPassword;
	private final JDKKeyStore.BouncyCastleStore mKeyStore;

	static {
		// Adds the the Spongy Castle security provider
		// If you have another lib using Spoongy Castle in your project, 
		// check that the provider is not added more than once
		Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());
	}

	public X509KeyManager(char[] password) {
		mPassword = password;
		mKeyStore = new JDKKeyStore.BouncyCastleStore();
		try {
			Log.i(TAG, "Generation of CA certificate...");
			KeyPair keys = CertificateFactory.generateRSAKeyPair(CertificateFactory.DEFAULT_KEY_SIZE);
			X509Certificate rootCertificate = CertificateFactory.generateRootCertificate(keys);
			mKeyStore.engineSetKeyEntry("root", keys.getPrivate(), mPassword, new Certificate[]{rootCertificate});
		} catch (Exception e) {
			Log.e(TAG, "Failed to generate certificate !");
			e.printStackTrace();
		}
	}

	private X509KeyManager() {
		mKeyStore = new JDKKeyStore.BouncyCastleStore();
	}

	/** This method will not be called. Client authentication has not been implemented. */
	@Override
	public String chooseClientAlias(String[] arg0, Principal[] arg1, Socket arg2) {
		// Will not be used in our case
		return null;
	}

	@Override
	public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
		String localAddress = socket!=null ? socket.getLocalAddress().getHostAddress() : "0.0.0.0";

		if (keyType.equals("RSA")) {

			// If no certificate have been generated for this address so far, we generate one
			if (!mKeyStore.engineContainsAlias(localAddress)) {
				try {

					// We get the CA certificate and private key 
					X509Certificate caCertificate = (X509Certificate) mKeyStore.engineGetCertificate("root");
					PrivateKey caPrivateKey = (PrivateKey) mKeyStore.engineGetKey("root",mPassword);

					// Generates the pair of keys for the new certificate
					KeyPair keys = CertificateFactory.generateRSAKeyPair(CertificateFactory.DEFAULT_KEY_SIZE);

					// We use the localAddress for the CN of the certificate
					Certificate certificate = CertificateFactory.generateSignedCertificate(caCertificate, caPrivateKey, keys.getPublic(), localAddress);

					// Adds the new certificate in the KeyStore
					mKeyStore.engineSetKeyEntry(localAddress, keys.getPrivate(), mPassword, new Certificate[]{certificate});

				} catch (Exception e) {
					// The certificate could not be generated for some reason
					e.printStackTrace();
					return null;
				}	
			}

			// We use the address the socket is locally bound to as the alias in the KeyManager 
			return localAddress;

		}
		return null;
	}

	@Override
	public X509Certificate[] getCertificateChain(String alias) {
		Certificate caCertificate = mKeyStore.engineGetCertificate("root");
		Certificate leafCertificate = mKeyStore.engineGetCertificate(alias); 		
		return new X509Certificate[] {(X509Certificate) leafCertificate, (X509Certificate) caCertificate};
	}

	/** This method will not be called. Client authentication has not been implemented. */
	@Override
	public synchronized String[] getClientAliases(String arg0, Principal[] arg1) {
		// Will not be used in our case
		return null;
	}

	/** 
	 * Returns the private key of the certificate corresponding to the alias.
	 * @param alias The alias
	 * @return The private key 
	 */
	@Override
	public PrivateKey getPrivateKey(String alias) {
		try {
			return (PrivateKey) mKeyStore.engineGetKey(alias, mPassword);
		} catch (Exception e) {
			Log.d(TAG, "Alias: \""+alias+"\" not found in the keystore !");
			return null;
		}
	}

	@Override
	public String[] getServerAliases(String keyType, Principal[] issuers) {
		if (keyType=="RSA") {
			int i = 0;
			Enumeration<String> aliases = mKeyStore.engineAliases();
			String[] list = new String[mKeyStore.engineSize()];
			while (aliases.hasMoreElements()) {
				list[i++] = (String) aliases.nextElement();
			}
			return list;
		} else return null;
	}

	public synchronized static X509KeyManager loadFromKeyStore(InputStream is, char[] password) throws IOException {
		X509KeyManager manager = new X509KeyManager();
		manager.mKeyStore.engineLoad(is, password);
		manager.mPassword = password;
		return manager;
	}

	/**
	 * Saves all the certificates generated and their private key in a JKS file
	 * @param file The file where the certificate will be saved 
	 * @param password The password to access the private key
	 */
	public synchronized void saveToKeyStore(OutputStream os, char[] password) 
			throws InvalidKeyException, 
			NoSuchAlgorithmException, 
			NoSuchPaddingException, 
			InvalidParameterSpecException, 
			InvalidKeySpecException, 
			InvalidAlgorithmParameterException, 
			IllegalBlockSizeException, 
			BadPaddingException, 
			KeyStoreException, 
			CertificateException, 
			IOException {

		mKeyStore.engineStore(os, password);

	}



}
