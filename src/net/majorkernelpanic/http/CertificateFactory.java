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
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.util.Calendar;
import java.util.Date;
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
import org.spongycastle.operator.ContentSigner;
import org.spongycastle.operator.OperatorCreationException;
import org.spongycastle.operator.jcajce.JcaContentSignerBuilder;

/**
 *  
 *  Contains some methods to create v3 X509 certificates for the HTTPS server.
 *  It uses the lib Spongy Castle, which actually is the lib Bouncy Castle repackaged
 *  for Android. 
 *  
 *  All certificates generated here uses RSA for the key pair and SHA-1/SHA256 for
 *  checksums.
 *  
 *  If you don't need HTTPS support, you can remove this class and 
 *  {@link X509KeyManager}.
 *
 */
public final class CertificateFactory {

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

	public static X509Certificate generateRootCertificate(KeyPair keys) 
			throws NoSuchAlgorithmException, OperatorCreationException, CertificateException, 
			KeyStoreException, UnrecoverableKeyException, IOException, 
			InvalidKeyException, NoSuchPaddingException, InvalidParameterSpecException, 
			InvalidKeySpecException, InvalidAlgorithmParameterException, IllegalBlockSizeException, 
			BadPaddingException {

		X500NameBuilder builder = new X500NameBuilder(BCStyle.INSTANCE);

		builder.addRDN(BCStyle.CN, "AwesomeHttpServer CA");

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
