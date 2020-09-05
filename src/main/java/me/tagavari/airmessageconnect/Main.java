package me.tagavari.airmessageconnect;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.java_websocket.WebSocket;
import org.java_websocket.server.CustomSSLWebSocketServerFactory;
import org.java_websocket.server.WebSocketServer;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

public class Main {
	private static final int port = 1259;
	
	private static final File logFile = new File("logs", "latest.log");
	
	private static Logger logger;
	
	private static final String argUnlinked = "unlinked";
	private static final String argInsecure = "insecure";
	private static boolean isUnlinked = false;
	private static boolean isInsecure = false;
	
	public static void main(String[] args) {
		//Initializing the logger
		logger = Logger.getGlobal();
		logger.setLevel(Level.FINEST);
		for(Handler handler : logger.getParent().getHandlers()) logger.getParent().removeHandler(handler);
		{
			ConsoleHandler handler = new ConsoleHandler();
			handler.setLevel(Level.FINEST);
			handler.setFormatter(getLoggerFormatter());
			logger.addHandler(handler);
		}
		try {
			if(!logFile.getParentFile().exists()) logFile.getParentFile().mkdir();
			else if(logFile.exists()) Files.move(logFile.toPath(), FileHelper.findFreeFile(logFile.getParentFile(), new SimpleDateFormat("YYYY-MM-dd").format(new Date()) + ".log", "-", 1).toPath());
			
			FileHandler handler = new FileHandler(logFile.getPath());
			handler.setLevel(Level.FINEST);
			handler.setFormatter(getLoggerFormatter());
			logger.addHandler(handler);
		} catch(IOException exception) {
			Main.getLogger().log(Level.SEVERE, "Failed to initialize log file - continuing without saving logs to disk");
		}
		
		//Reading the arguments
		for(String argument : args) {
			if(argUnlinked.equals(argument)) {
				if(isUnlinked) continue;
				isUnlinked = true;
				Main.getLogger().log(Level.INFO, "Server is running in UNLINKED MODE. Accounts will not be verified. This functionality cannot be used in production.");
			} else if(argInsecure.equals(argument)) {
				if(isInsecure) continue;
				isInsecure = true;
				Main.getLogger().log(Level.INFO, "Server is running in INSECURE MODE. Traffic will not be encrypted. This functionality cannot be used in production.");
			} else {
				Main.getLogger().log(Level.INFO, "Unknown argument provided: " + argument);
			}
		}
		
		if(!isUnlinked()) {
			//Initializing Firebase
			try {
				FirebaseOptions options = new FirebaseOptions.Builder()
						.setCredentials(GoogleCredentials.getApplicationDefault())
						.build();
				
				FirebaseApp.initializeApp(options);
			} catch(IOException exception) {
				Main.getLogger().log(Level.SEVERE, exception.getMessage(), exception);
				return;
			}
			
			//Initializing data utils
			StorageUtils.instance().initialize();
		}
		
		//Creating the server
		WebSocketServer server = new Server(new InetSocketAddress(port));
		
		if(!Main.isInsecure()) {
			//Loading the SSL context
			SSLContext sslContext = SecurityUtils.loadPEM(new File("certificate.pem"));
			if(sslContext == null) {
				return;
			} else {
				SSLEngine engine = sslContext.createSSLEngine();
				String[] ciphers = engine.getEnabledCipherSuites();
				server.setWebSocketFactory(new CustomSSLWebSocketServerFactory(sslContext, new String[]{"TLSv1.2"}, ciphers));
			}
		}
		
		//Starting the server
		server.start();
	}
	
	public static Logger getLogger() {
		return logger;
	}
	
	public static String getRelayID() {
		return "relay-dev";
	}
	
	private static Formatter getLoggerFormatter() {
		return new Formatter() {
			private final DateFormat dateFormat = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
			
			@Override
			public String format(LogRecord record) {
				String stackTrace = "";
				if(record.getThrown() != null) {
					StringWriter errors = new StringWriter();
					record.getThrown().printStackTrace(new PrintWriter(errors));
					stackTrace = errors.toString();
				}
				return dateFormat.format(record.getMillis()) + ' ' + '[' + record.getLevel().toString() + ']' + ' ' + formatMessage(record) + '\n' + stackTrace;
			}
		};
	}
	
	public static String connectionToString(WebSocket connection) {
		InetSocketAddress remoteSocketAddress = connection.getRemoteSocketAddress();
		if(remoteSocketAddress == null) return "unknown";
		InetAddress address = remoteSocketAddress.getAddress();
		if(address == null) return "unknown";
		return address.getHostAddress() + " (" + address.getHostName() + ")";
	}
	
	public static boolean isUnlinked() {
		return isUnlinked;
	}
	
	public static boolean isInsecure() {
		return isInsecure;
	}
}