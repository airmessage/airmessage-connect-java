package me.tagavari.airmessageconnect;

import io.sentry.ScopeCallback;
import io.sentry.Sentry;
import io.sentry.protocol.User;
import me.tagavari.airmessageconnect.communicate.Communications;
import me.tagavari.airmessageconnect.communicate.HttpDraft;
import me.tagavari.airmessageconnect.communicate.Protocol;
import me.tagavari.airmessageconnect.structure.ConnectionCollection;
import me.tagavari.airmessageconnect.structure.ConnectionGroup;
import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.exceptions.InvalidDataException;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshakeBuilder;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Server extends WebSocketServer {
	//Creating the constants
	//Any airmessage.org domain, "localhost", or "app"
	private static final Pattern originRegex = Pattern.compile("^(?:https://(?:.+\\.)?.airmessage\\.org)|(?:https?://localhost(?::\\d+)?|(?:app))$");
	
	//Creating the state values
	private final ConnectionCollection connectionCollection = new ConnectionCollection();
	
	public Server(InetSocketAddress address) {
		super(address, Arrays.asList(new HttpDraft(), new Draft_6455()));
		
		setConnectionLostTimeout(10 * 60); //Every 10 mins
	}
	
	@Override
	public void onStart() {
		Main.getLogger().log(Level.INFO, "WebSocket server started");
	}
	
	@Override
	public ServerHandshakeBuilder onWebsocketHandshakeReceivedAsServer(WebSocket conn, Draft draft, ClientHandshake request) throws InvalidDataException {
		if(HttpDraft.isHTTP(request)) {
			return super.onWebsocketHandshakeReceivedAsServer(conn, draft, request);
		}
		
		//Updating Sentry with the current scope
		Sentry.configureScope(scope -> {
			User user = new User();
			user.setIpAddress(Main.getIP(conn));
			scope.setUser(user);
		});
		
		//Calling the super method
		ServerHandshakeBuilder builder = super.onWebsocketHandshakeReceivedAsServer(conn, draft, request);
		
		//Logging the event
		Main.getLogger().log(Level.FINE, "Responding to handshake from client " + Main.connectionToString(conn));
		
		//Checking for an origin header header
		if(!request.hasFieldValue("Origin")) {
			Main.getLogger().log(Level.FINE, "Rejecting handshake (no origin) from client " + Main.connectionToString(conn));
			throw new InvalidDataException(CloseFrame.PROTOCOL_ERROR);
		}
		
		//Validating the origin
		if(!originRegex.matcher(request.getFieldValue("Origin")).matches()) {
			Main.getLogger().log(Level.FINE, "Rejecting handshake (bad origin - " + request.getFieldValue("Origin") + ") from client " + Main.connectionToString(conn));
			throw new InvalidDataException(CloseFrame.PROTOCOL_ERROR);
		}
		
		Map<String, String> queryParams;
		{
			//Checking for a resource descriptor
			String resourceDescriptor = request.getResourceDescriptor();
			if(resourceDescriptor.isEmpty()) {
				Main.getLogger().log(Level.FINE, "Rejecting handshake (no resource descriptor) from client " + Main.connectionToString(conn));
				throw new InvalidDataException(CloseFrame.PROTOCOL_ERROR);
			}
			
			//Extracting the query from the string
			int queryIndex = resourceDescriptor.lastIndexOf("?");
			if(queryIndex == -1) {
				Main.getLogger().log(Level.FINE, "Rejecting handshake (no query params - " + resourceDescriptor + ") from client " + Main.connectionToString(conn));
				throw new InvalidDataException(CloseFrame.PROTOCOL_ERROR);
			}
			String queryString = resourceDescriptor.substring(queryIndex + 1);
			
			//Getting the query parameters
			try {
				queryParams = Stream.of(queryString.split("&"))
						.map(str -> {
							//Split the string into key-value pair
							String[] keyValue = str.split("=");
							if(keyValue.length != 2) throw new IllegalStateException("Invalid query key-value: " + str);
							
							//Decode the value side
							keyValue[1] = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
							
							return keyValue;
						})
						.collect(Collectors.toMap(str -> str[0], str -> str[1]));
			} catch(IllegalStateException exception) {
				Main.getLogger().log(Level.FINE, "Rejecting handshake (bad query formatting - " + exception.getMessage() + ") from client " + Main.connectionToString(conn));
				throw new InvalidDataException(CloseFrame.PROTOCOL_ERROR);
			}
		}
		
		//Reading parameter data
		int commVer;
		try {
			commVer = Integer.parseInt(queryParams.get("communications"));
		} catch(NumberFormatException exception) {
			Main.getLogger().log(Level.WARNING, "Rejecting handshake (bad communications string - " + queryParams.get("communications") + ") from client " + Main.connectionToString(conn) + ": " + exception.getMessage(), exception);
			throw new InvalidDataException(CloseFrame.PROTOCOL_ERROR);
		}
		
		//Finding an appropriate protocol version
		Protocol protocol = Communications.getProtocol(commVer);
		if(protocol == null) {
			Main.getLogger().log(Level.FINE, "Rejecting handshake (bad communications version - " + commVer + ") from client " + Main.connectionToString(conn));
			
			//Disconnect the client with our custom close code
			conn.setAttachment(new ClientData(SharedData.closeCodeIncompatibleProtocol));
			return builder;
		}
		
		try {
			//Running handshake validation through the protocol
			ClientData clientData = protocol.handleHandshake(conn, draft, request, queryParams);
			conn.setAttachment(clientData);
			
			//Accepting the request
			return builder;
		} catch(InvalidDataException exception) {
			//Disconnect the client later if we're rejecting them with a custom close code
			if(exception.getCloseCode() >= 4000 && exception.getCloseCode() < 5000) {
				conn.setAttachment(new ClientData(exception.getCloseCode()));
				return builder;
			} else {
				//Re-throw the exception and reject the handshake normally
				throw exception;
			}
		}
	}
	
	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		if(HttpDraft.isHTTP(handshake)) {
			conn.close();
			return;
		}
		
		//Updating Sentry with the current scope
		withSentryScope(scope -> {
			User user = new User();
			user.setIpAddress(Main.getIP(conn));
			scope.setUser(user);
		}, () -> {
			//Getting the connection data
			ClientData clientData = conn.getAttachment();
			
			//Checking if the client is to be disconnected
			if(clientData.isRejected()) {
				Main.getLogger().log(Level.FINE, "Disconnecting rejected connection from " + Main.connectionToString(conn) + " (" + clientData.getCloseCode() + ")");
				conn.close(clientData.getCloseCode());
				return;
			}
			
			//Getting the client type
			ClientData.Type type = clientData.getType();
			clientData.clearType();
			
			//Updating the user
			if(Sentry.isEnabled()) {
				Sentry.configureScope(scope -> {
					User user = new User();
					user.setIpAddress(Main.getIP(conn));
					user.setId(type.getGroupID());
					user.setOthers(Map.of(
						"client_id", clientData.isServer() ? "server" : Integer.toString(clientData.getConnectionID()),
						"protocol_version", Integer.toString(clientData.getProtocol().getVersion())
					));
					scope.setUser(user);
				});
			}
			
			if(clientData.isServer()) {
				//Adding a new collection for the server
				connectionCollection.addServer(conn, type.getGroupID());
			} else {
				//Adding the client to a group
				boolean result = connectionCollection.addClient(conn, type.getGroupID(), type.getFCMToken());
				
				//Notifying the server of the addition
				if(result) {
					WebSocket serverSocket = clientData.getConnectionGroup().getConnectionServer();
					serverSocket.send(serverSocket.<ClientData>getAttachment().getProtocol().sendServerConnection(clientData.getConnectionID()));
				} else {
					return;
				}
			}
			
			//Sending the connection OK message
			conn.send(clientData.getProtocol().sendSharedConnectionOK());
			
			//Logging the event
			if(clientData.isServer()) {
				Main.getLogger().log(Level.FINE, "Server of group " + clientData.getConnectionGroup().getGroupID() + " connected from " + Main.connectionToString(conn));
			} else {
				Main.getLogger().log(Level.FINE, "Client " + clientData.getConnectionID() + " of " + clientData.getConnectionGroup().getGroupID() + " connected from " + Main.connectionToString(conn));
			}
		});
	}
	
	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		String logSuffix = "(" + code + " / " + reason + " / " + remote + ")";
		
		//Getting the client data
		ClientData clientData = conn.getAttachment();
		if(clientData == null) return;
		
		//Logging disconnections of rejected clients
		if(clientData.isRejected()) {
			Main.getLogger().log(Level.FINE, "Rejected client disconnected from " + Main.connectionToString(conn) + " " + logSuffix);
			return;
		}
		
		//Updating Sentry with the current scope
		withSentryScopeGenerated(conn, clientData, () -> {
			ConnectionGroup group = clientData.getConnectionGroup();
			
			if(group == null) {
				//No group, nothing to do
				//Just log the event
				if(clientData.isServer()) {
					Main.getLogger().log(Level.FINE, "Server disconnected from " + Main.connectionToString(conn) + " " + logSuffix);
				} else {
					Main.getLogger().log(Level.FINE, "Client disconnected from " + Main.connectionToString(conn) + " " + logSuffix);
				}
			} else {
				//Log the event and clean up
				if(clientData.isServer()) {
					if(!clientData.getDisableCleanup()) {
						//Unregistering the group and disconnecting all clients
						group.closeAll(SharedData.closeCodeNoGroup);
						connectionCollection.removeGroup(group.getGroupID());
						
						//Writing the group's client FCM tokens to the database (if modifications were made)
						if(group.isClientFCMTokenListModified()) {
							if(!Main.isUnlinked()) {
								try {
									StorageUtils.instance().updateFCMTokens(group.getGroupID(), group.getClientFCMTokenList());
								} catch(ExecutionException | InterruptedException exception) {
									Main.getLogger().log(Level.SEVERE, exception.getMessage(), exception);
								}
							}
						}
					}
					
					//Logging the event
					Main.getLogger().log(Level.FINE, "Server of group " + group.getGroupID() + " disconnected from " + Main.connectionToString(conn) + " " + logSuffix);
				} else {
					//Getting the disconnected client's connection ID
					int connectionID = clientData.getConnectionID();
					
					if(!clientData.getDisableCleanup()) {
						//Unregistering the connection if the connection is a client
						group.removeClient(connectionID);
						
						//Notifying the server of the disconnection
						WebSocket serverSocket = clientData.getConnectionGroup().getConnectionServer();
						serverSocket.send(serverSocket.<ClientData>getAttachment().getProtocol().sendServerDisconnection(connectionID));
					}
					
					//Logging the event
					Main.getLogger().log(Level.FINE, "Client " + clientData.getConnectionID() + " of " + group.getGroupID() + " disconnected from " + Main.connectionToString(conn) + " " + logSuffix);
				}
			}
		});
	}
	
	@Override
	public void onMessage(WebSocket conn, ByteBuffer message) {
		//Getting the client's data
		ClientData clientData = conn.getAttachment();
		if(clientData == null) return;
		
		//Ignoring if this client is rejected
		if(clientData.isRejected()) return;
		
		//Updating Sentry with the current scope
		withSentryScopeGenerated(conn, clientData, () -> {
			//Forwarding the message for the protocol to handle
			conn.<ClientData>getAttachment().getProtocol().receive(conn, clientData, message);
		});
	}
	
	@Override
	public void onMessage(WebSocket conn, String message) {
	
	}
	
	@Override
	public void onError(WebSocket conn, Exception exception) {
		if(conn != null) {
			ClientData clientData = conn.getAttachment();
			if(clientData != null && !clientData.isRejected()) {
				withSentryScopeGenerated(conn, clientData, () -> Main.getLogger().log(Level.WARNING, exception.getMessage(), exception));
			}
		}
		
		Main.getLogger().log(Level.WARNING, exception.getMessage(), exception);
	}
	
	private static void withSentryScope(ScopeCallback scope, Runnable runnable) {
		if(Sentry.isEnabled()) {
			try {
				Sentry.pushScope();
				Sentry.configureScope(scope);
				runnable.run();
			} finally {
				Sentry.popScope();
			}
		} else {
			runnable.run();
		}
	}
	
	private static void withSentryScopeGenerated(WebSocket conn, ClientData clientData, Runnable callback) {
		//Updating Sentry with the current scope
		withSentryScope(scope -> {
			User user = new User();
			user.setIpAddress(Main.getIP(conn));
			user.setId(clientData.getConnectionGroup().getGroupID());
			user.setOthers(Map.of(
				"client_id", clientData.isServer() ? "server" : Integer.toString(clientData.getConnectionID()),
				"protocol_version", Integer.toString(clientData.getProtocol().getVersion())
			));
			scope.setUser(user);
		}, callback);
	}
}