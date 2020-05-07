package me.tagavari.airmessageconnect.structure;

import org.java_websocket.WebSocket;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a group of connections centered around a single server with any number of clients
 */
public class ConnectionGroup {
	//Server and client connections
	private final WebSocket serverConnection;
	private final Map<Integer, WebSocket> clientConnections = Collections.synchronizedMap(new HashMap<>());
	
	//The group ID of this connection group
	private final String groupID;
	
	//Used to assign IDs to clients
	private int connectionID = 0;
	
	/**
	 * Creates a new ConnectionGroup
	 * @param serverConnection The server WebSocket connection
	 * @param groupID The ID of this connection group
	 */
	public ConnectionGroup(WebSocket serverConnection, String groupID) {
		this.groupID = groupID;
		this.serverConnection = serverConnection;
	}
	
	/**
	 * Gets this connection group's unique identifier
	 * @return The group ID
	 */
	public String getGroupID() {
		return groupID;
	}
	
	/**
	 * Closes and cleans up the connection of a client with the specified connection ID
	 * If there is no connected client with a matching connection ID, this method does nothing
	 * @param connectionID The connection ID of the client to disconnect
	 * @param code The closing code
	 */
	public void closeClient(int connectionID, int code) {
		WebSocket connection = clientConnections.remove(connectionID);
		if(connection != null) connection.close(code);
	}
	
	/**
	 * Closes all connections related to this group, rendering this group useless
	 */
	public void closeAll(int code) {
		//Closing existing connections
		for(WebSocket connectionPair : clientConnections.values()) connectionPair.close(code);
		clientConnections.clear();
		
		//Closing the approach connection
		serverConnection.close(code);
	}
	
	/**
	 * Returns the number of active client connections for this connection group
	 * @return The number of active client connections
	 */
	public int getCount() {
		return clientConnections.size();
	}
	
	/**
	 * Registers a new client connection
	 * @param connectionID The connection ID of this client
	 * @param connection The connection to register
	 */
	public void addClient(int connectionID, WebSocket connection) {
		clientConnections.put(connectionID, connection);
	}
	
	/**
	 * Unregisters a client connection, usually after the connection has disconnected
	 * @param connectionID The connection ID of the client to unregister
	 */
	public void removeClient(int connectionID) {
		clientConnections.remove(connectionID);
	}
	
	/**
	 * Gets the active server connection
	 * @return The server connection
	 */
	public WebSocket getConnectionServer() {
		return serverConnection;
	}
	
	/**
	 * Gets the specified client connection
	 * @param connectionID The connection ID of the client
	 * @return The client connection, or NULL if none is available
	 */
	public WebSocket getConnectionClient(int connectionID) {
		return clientConnections.get(connectionID);
	}
	
	/**
	 * Get all connected client connections
	 * @return A collection of all currently connected clients
	 */
	public Collection<WebSocket> getAllConnectionsClient() {
		return clientConnections.values();
	}
	
	/**
	 * Returns an integer representing a value that is safe to use to identify connections
	 * @return The ID
	 */
	public int nextConnectionID() {
		return ++connectionID;
	}
}