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
import java.math.BigInteger;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.security.auth.x500.X500Principal;

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.DERSequence;
import org.spongycastle.asn1.x500.X500NameBuilder;
import org.spongycastle.asn1.x500.style.BCStyle;
import org.spongycastle.asn1.x509.BasicConstraints;
import org.spongycastle.asn1.x509.ExtendedKeyUsage;
import org.spongycastle.asn1.x509.KeyPurposeId;
import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.cert.X509v3CertificateBuilder;
import org.spongycastle.cert.jcajce.JcaX509CertificateConverter;
import org.spongycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.spongycastle.jce.X509KeyUsage;
import org.spongycastle.jce.provider.JDKKeyStore;
import org.spongycastle.operator.ContentSigner;
import org.spongycastle.operator.OperatorCreationException;
import org.spongycastle.operator.jcajce.JcaContentSignerBuilder;

import android.util.Log;

/**
 * 
 * Contains two classes, one for generating certificates for the HTTPS server,
 * and the other is a KeyStore that is handed to the SSLContext before creating a SSLSocket.
 * 
 * If you do not need SSL support (you only need an HTTP server) delete this class and the Songy Castle library.
 *
 */
public final class ModSSL {

	public final static class X509KeyManager implements javax.net.ssl.X509KeyManager {

		public final static String TAG = "X509KeyManager";

		private char[] mPassword;
		private final JDKKeyStore.BouncyCastleStore mKeyStore;

		static {
			// Adds the the Spongy Castle security provider
			// If you have another lib using Spoongy Castle in your project, 
			// check that the provider is not added more than once
			Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());
		}

		public X509KeyManager(char[] password, String CN) throws Exception {
			mPassword = password;
			mKeyStore = new JDKKeyStore.BouncyCastleStore();
			try {
				Log.d(TAG, "Generation of CA certificate...");
				KeyPair keys = CertificateFactory.generateRSAKeyPair(CertificateFactory.DEFAULT_KEY_SIZE);
				X509Certificate rootCertificate = CertificateFactory.generateRootCertificate(keys,CN);
				mKeyStore.engineSetKeyEntry("root", keys.getPrivate(), mPassword, new Certificate[]{rootCertificate});
			} catch (Exception e) {
				Log.e(TAG, "Failed to generate certificate !");
				e.printStackTrace();
				throw e;
			}
		}

		private X509KeyManager() {
			mKeyStore = new JDKKeyStore.BouncyCastleStore();
		}

		/** This method will not be called. Client authentication has not been implemented. */
		@Override
		public synchronized String chooseClientAlias(String[] arg0, Principal[] arg1, Socket arg2) {
			// Will not be used in our case
			Log.d(TAG, "chooseClientAlias");
			return null;
		}

		@Override
		public synchronized String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
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
						Log.e(TAG, "Failed to generate certificate for CN: "+localAddress);
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
		public synchronized X509Certificate[] getCertificateChain(String alias) {
			Certificate caCertificate = mKeyStore.engineGetCertificate("root");
			Certificate leafCertificate = mKeyStore.engineGetCertificate(alias); 		
			return new X509Certificate[] {(X509Certificate) leafCertificate, (X509Certificate) caCertificate};
		}

		/** This method will not be called. Client authentication has not been implemented. */
		@Override
		public synchronized String[] getClientAliases(String arg0, Principal[] arg1) {
			// Will not be used in our case
			Log.d(TAG, "getClientAliases");
			return null;
		}

		/** 
		 * Returns the private key of the certificate corresponding to the alias.
		 * @param alias The alias
		 * @return The private key 
		 */
		@Override
		public synchronized PrivateKey getPrivateKey(String alias) {
			try {
				return (PrivateKey) mKeyStore.engineGetKey(alias, mPassword);
			} catch (Exception e) {
				Log.d(TAG, "Alias: \""+alias+"\" not found in the keystore !");
				return null;
			}
		}

		@Override
		public synchronized String[] getServerAliases(String keyType, Principal[] issuers) {
			Log.d(TAG, "getServersAliases");
			if (keyType.equals("RSA")) {
				int i = 0;
				Enumeration<String> aliases = mKeyStore.engineAliases();
				String[] list = new String[mKeyStore.engineSize()];
				while (aliases.hasMoreElements()) {
					list[i++] = aliases.nextElement();
				}
				return list;
			} else return null;
		}

		public synchronized static X509KeyManager loadFromKeyStore(InputStream is, char[] password) throws IOException {
			Log.d(TAG,"Loading certificates from file...");
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

	/**
	 *  
	 *  Contains some methods to create v3 X509 certificates for the HTTPS server.
	 *  It uses the lib Spongy Castle, which actually is the lib Bouncy Castle repackaged
	 *  for Android. 
	 *  
	 *  All certificates generated here uses RSA for the key pair and SHA-1/SHA256 for
	 *  checksums.
	 *  
	 *  If you don't need HTTPS support, you can remove the class {@link ModSSL}
	 *
	 */
	public final static class CertificateFactory {

		/** The default length of RSA keys */
		public final static int DEFAULT_KEY_SIZE = 1024;

		private final static String BC = org.spongycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;

		public static KeyPair generateRSAKeyPair(int keySize) throws NoSuchAlgorithmException {
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
			keyGen.initialize(keySize);
			KeyPair keyPair = keyGen.genKeyPair();
			return keyPair;
		}

		public static X509Certificate generateSignedCertificate(X509Certificate caCertificate, PrivateKey caPrivateKey, PublicKey publicKey, String CN) 
				throws NoSuchAlgorithmException, OperatorCreationException, CertificateException, 
				KeyStoreException, UnrecoverableKeyException, IOException, 
				InvalidKeyException, NoSuchPaddingException, InvalidParameterSpecException, 
				InvalidKeySpecException, InvalidAlgorithmParameterException, IllegalBlockSizeException, 
				BadPaddingException {

			X500NameBuilder builder = new X500NameBuilder(BCStyle.INSTANCE);

			builder.addRDN(BCStyle.CN, CN);

			// We want this root certificate to be valid for one year
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.YEAR, 1);

			ContentSigner sigGen = new JcaContentSignerBuilder("SHA1WithRSAEncryption").setProvider(BC).build(caPrivateKey);
			X509v3CertificateBuilder certGen = new JcaX509v3CertificateBuilder(
					caCertificate, 
					new BigInteger(80, new Random()), 
					new Date(System.currentTimeMillis() - 50000),
					calendar.getTime(),
					new X500Principal(builder.build().getEncoded()),
					publicKey);

			// Those are the extensions needed for the certificate to be a leaf certificate that authenticates a SSL server
			certGen.addExtension(new ASN1ObjectIdentifier("2.5.29.15"), true, new X509KeyUsage(X509KeyUsage.keyEncipherment));
			certGen.addExtension(new ASN1ObjectIdentifier("2.5.29.37"), true, new DERSequence(KeyPurposeId.id_kp_serverAuth));

			X509CertificateHolder certificateHolder = certGen.build(sigGen);
			X509Certificate certificate = new JcaX509CertificateConverter().setProvider(BC).getCertificate(certificateHolder);

			return certificate;

		}

		public static X509Certificate generateRootCertificate(KeyPair keys, String CN) 
				throws NoSuchAlgorithmException, OperatorCreationException, CertificateException, 
				KeyStoreException, UnrecoverableKeyException, IOException, 
				InvalidKeyException, NoSuchPaddingException, InvalidParameterSpecException, 
				InvalidKeySpecException, InvalidAlgorithmParameterException, IllegalBlockSizeException, 
				BadPaddingException {

			X500NameBuilder builder = new X500NameBuilder(BCStyle.INSTANCE);

			builder.addRDN(BCStyle.CN, CN);

			// We want this root certificate to be valid for one year 
			Calendar calendar = Calendar.getInstance();
			calendar.add( Calendar.YEAR, 1 );

			ContentSigner sigGen = new JcaContentSignerBuilder("SHA1WithRSAEncryption").setProvider(BC).build(keys.getPrivate());
			X509v3CertificateBuilder certGen = new JcaX509v3CertificateBuilder(
					builder.build(), 
					new BigInteger(80, new Random()), 
					new Date(System.currentTimeMillis() - 50000),
					calendar.getTime(),
					builder.build(),
					keys.getPublic());

			// Those are the extensions needed for a CA certificate
			certGen.addExtension(new ASN1ObjectIdentifier("2.5.29.19"), true, new BasicConstraints(true));
			certGen.addExtension(new ASN1ObjectIdentifier("2.5.29.15"), true, new X509KeyUsage(X509KeyUsage.digitalSignature));
			certGen.addExtension(new ASN1ObjectIdentifier("2.5.29.37"), true, new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth));

			X509CertificateHolder certificateHolder = certGen.build(sigGen);

			X509Certificate certificate = new JcaX509CertificateConverter().setProvider(BC).getCertificate(certificateHolder);

			return certificate;

		}	

	}

}