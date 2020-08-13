package me.tagavari.airmessageconnect;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.logging.Level;

//https://stackoverflow.com/a/12514888
public class SecurityUtils {
	private static final String keystorePassword = "m4KH6UteeOA0w95lF0bX";
	
	public static SSLContext loadJKS(File file) {
		try {
			KeyStore ks = KeyStore.getInstance("JKS");
			ks.load(new FileInputStream(file), keystorePassword.toCharArray());
			
			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(ks, keystorePassword.toCharArray());
			TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
			tmf.init(ks);
			
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
			return sslContext;
		} catch(IOException | CertificateException | UnrecoverableKeyException | NoSuchAlgorithmException | KeyManagementException | KeyStoreException exception) {
			Main.getLogger().log(Level.SEVERE, exception.getMessage(), exception);
			return null;
		}
	}
	
	public static SSLContext loadPEM(File file) {
		String fileData;
		try {
			fileData = new String(Files.readAllBytes(file.toPath())).replace("\n", "");
		} catch(IOException exception) {
			Main.getLogger().log(Level.SEVERE, exception.getMessage(), exception);
			return null;
		}
		
		Certificate[] certificateArray;
		try {
			certificateArray = Arrays.stream(extractStringSection(fileData, "-----BEGIN CERTIFICATE-----", "-----END CERTIFICATE-----"))
					.map(certString -> {
						try {
							return generateCertificateFromDER(Base64.getDecoder().decode(certString));
						} catch(CertificateException exception) {
							throw new RuntimeException(exception);
						}
					})
					.toArray(Certificate[]::new);
		} catch(RuntimeException exception) {
			Main.getLogger().log(Level.SEVERE, exception.getMessage(), exception);
			return null;
		}
		
		PrivateKey privateKey;
		try {
			privateKey = generatePrivateKeyFromDER(Base64.getDecoder().decode(extractStringSection(fileData, "-----BEGIN PRIVATE KEY-----", "-----END PRIVATE KEY-----")[0]));
		} catch(InvalidKeySpecException | NoSuchAlgorithmException exception) {
			Main.getLogger().log(Level.SEVERE, exception.getMessage(), exception);
			return null;
		}
		
		try {
			KeyStore keystore = KeyStore.getInstance("JKS");
			keystore.load(null);
			for(int i = 0; i < certificateArray.length; i++) keystore.setCertificateEntry("cert-alias-" + i, certificateArray[i]);
			keystore.setKeyEntry("key-alias", privateKey, keystorePassword.toCharArray(), certificateArray);
			
			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(keystore, keystorePassword.toCharArray());
			
			KeyManager[] km = kmf.getKeyManagers();
			
			SSLContext context = SSLContext.getInstance("TLS");
			context.init(km, null, null);
			return context;
		} catch(KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | UnrecoverableKeyException | KeyManagementException exception) {
			Main.getLogger().log(Level.SEVERE, exception.getMessage(), exception);
			return null;
		}
	}
	
	private static String[] extractStringSection(String input, String beginDelimiter, String endDelimiter) {
		return Arrays.stream(input.split(beginDelimiter))
				.skip(1)
				.map(section -> section.split(endDelimiter)[0])
				.toArray(String[]::new);
	}
	
	private static byte[] parseDERFromPEM(byte[] pem, String beginDelimiter, String endDelimiter) {
		String data = new String(pem);
		String[] tokens = data.split(beginDelimiter);
		tokens = tokens[1].split(endDelimiter);
		return Base64.getDecoder().decode(tokens[0]);
	}
	
	private static X509Certificate generateCertificateFromDER(byte[] certBytes) throws CertificateException {
		CertificateFactory factory = CertificateFactory.getInstance("X.509");
		return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(certBytes));
	}
	
	private static RSAPrivateKey generatePrivateKeyFromDER(byte[] keyBytes) throws InvalidKeySpecException, NoSuchAlgorithmException {
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
		KeyFactory factory = KeyFactory.getInstance("RSA");
		return (RSAPrivateKey) factory.generatePrivate(spec);
	}
}