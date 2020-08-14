package me.tagavari.airmessageconnect;

import me.tagavari.airmessageconnect.communicate.Communications;
import me.tagavari.airmessageconnect.communicate.Protocol;
import me.tagavari.airmessageconnect.structure.ConnectionCollection;
import me.tagavari.airmessageconnect.structure.ConnectionGroup;
import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.exceptions.InvalidDataException;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshakeBuilder;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Server extends WebSocketServer {
	//Creating the state values
	private final ConnectionCollection connectionCollection = new ConnectionCollection();
	
	public Server(InetSocketAddress address) {
		super(address);
		
		setConnectionLostTimeout(10 * 60); //Every 10 mins
	}
	
	@Override
	public void onStart() {
		Main.getLogger().log(Level.INFO, "WebSocket server started");
	}
	
	@Override
	public ServerHandshakeBuilder onWebsocketHandshakeReceivedAsServer(WebSocket conn, Draft draft, ClientHandshake request) throws InvalidDataException {
		ServerHandshakeBuilder builder = super.onWebsocketHandshakeReceivedAsServer(conn, draft, request);
		
		//Logging the event
		Main.getLogger().log(Level.FINE, "Responding to handshake from client " + Main.connectionToString(conn));
		
		//Checking for cookie header
		if(!request.hasFieldValue("Cookie")) {
			Main.getLogger().log(Level.FINE, "Rejecting handshake (no cookie) from client " + Main.connectionToString(conn));
			throw new InvalidDataException(CloseFrame.PROTOCOL_ERROR);
		}
		
		//Getting the cookies
		Map<String, String> cookieMap = Stream.of(request.getFieldValue("Cookie").split("; *"))
				.map(str -> str.split("="))
				.collect(Collectors.toMap(str -> str[0], str -> str[1]));
		
		//Reading parameter data
		int commVer;
		try {
			commVer = Integer.parseInt(cookieMap.get("communications"));
		} catch(NumberFormatException exception) {
			Main.getLogger().log(Level.FINE, "Rejecting handshake (bad communications string - " + cookieMap.get("communications") + ") from client " + Main.connectionToString(conn));
			Main.getLogger().log(Level.WARNING, exception.getMessage(), exception);
			throw new InvalidDataException(CloseFrame.PROTOCOL_ERROR);
		}
		
		//Finding an appropriate protocol version
		Protocol protocol = Communications.getProtocol(commVer);
		if(protocol == null) {
			Main.getLogger().log(Level.FINE, "Rejecting handshake (bad communications version - " + commVer + ") from client " + Main.connectionToString(conn));
			throw new InvalidDataException(CloseFrame.POLICY_VALIDATION, "Bad communications version " + commVer);
		}
		
		//Running handshake validation through the protocol
		protocol.handleHandshake(conn, draft, request, cookieMap);
		
		//Accepting the request
		return builder;
	}
	
	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		//Getting the connection data
		ClientData clientData = conn.getAttachment();
		ClientData.Type type = clientData.getType();
		clientData.clearType();
		
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
	}
	
	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		//Getting the client data
		ClientData clientData = conn.getAttachment();
		ConnectionGroup group = clientData.getConnectionGroup();
		
		String logSuffix = "(" + code + " / " + reason + " / " + remote + ")";
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
	}
	
	@Override
	public void onMessage(WebSocket conn, ByteBuffer message) {
		//Forwarding the message for the protocol to handle
		ClientData clientData = conn.getAttachment();
		conn.<ClientData>getAttachment().getProtocol().receive(conn, clientData, message);
	}
	
	@Override
	public void onMessage(WebSocket conn, String message) {
	
	}
	
	@Override
	public void onError(WebSocket conn, Exception exception) {
		//Logging the exception
		Main.getLogger().log(Level.WARNING, exception.getMessage(), exception);
	}
}