package me.tagavari.airmessageconnect;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.java_websocket.WebSocket;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.logging.*;

public class Main {
	private static final int port = 1259;
	
	private static Logger logger;
	
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
		
		//Starting the server
		WebSocketServer server = new Server(new InetSocketAddress(port));
		server.run();
	}
	
	public static Logger getLogger() {
		return logger;
	}
	
	public static String getRelayID() {
		return "relay-dev";
	}
	
	private static Formatter getLoggerFormatter() {
		return new Formatter() {
			private final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
			
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
}