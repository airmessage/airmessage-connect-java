package me.tagavari.airmessageconnect.structure;

import me.tagavari.airmessageconnect.ClientData;
import org.java_websocket.WebSocket;

import java.util.*;

/**
 * Represents a group of connections centered around a single server with any number of clients
 */
public class ConnectionGroup {
	private static final int fcmTokenListLimit = 8;
	
	//Server and client connections
	private final WebSocket serverConnection;
	private final Map<Integer, WebSocket> clientConnections = Collections.synchronizedMap(new HashMap<>());
	
	//The list of FCM tokens for this account
	private final List<String> clientFCMTokenList;
	private boolean isClientFCMTokenListModified = false;
	
	//The group ID of this connection group
	private final String groupID;
	
	//Used to assign IDs to clients
	private int connectionID = 0;
	
	/**
	 * Creates a new ConnectionGroup
	 * @param serverConnection The server WebSocket connection
	 * @param groupID The ID of this connection group
	 */
	public ConnectionGroup(WebSocket serverConnection, String groupID, List<String> clientFCMTokenList) {
		this.groupID = groupID;
		this.serverConnection = serverConnection;
		if(clientFCMTokenList != null) this.clientFCMTokenList = clientFCMTokenList;
		else this.clientFCMTokenList = new ArrayList<>(fcmTokenListLimit);
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
		for(WebSocket clientConnection : clientConnections.values()) {
			clientConnection.<ClientData>getAttachment().setConnectionGroup(null);
			clientConnection.close(code);
		}
		clientConnections.clear();
		
		//Closing the server connection
		serverConnection.close(code);
		serverConnection.<ClientData>getAttachment().setConnectionGroup(null);
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
	
	/**
	 * Returns a list representing the FCM tokens of all registered clients
	 * @return The list
	 */
	public List<String> getClientFCMTokenList() {
		return clientFCMTokenList;
	}
	
	/**
	 * Add a client FCM token to this group
	 * This function is safe to call at any time -
	 * if the token is already in the list, it will be brought to the top
	 * if the token is not in the list, it will be added to the top, discarding the oldest item
	 * @param token The token to add
	 */
	public void addClientFCMToken(String token) {
		//Checking if the token already exists in the list
		int index = clientFCMTokenList.indexOf(token);
		if(index != -1) {
			//Checking if the token isn't first in the list
			if(index != 0) {
				//Moving the token to the top of the list
				clientFCMTokenList.remove(token);
				clientFCMTokenList.add(0, token);
				setClientFCMTokenListModified();
			}
		} else {
			//Checking if the list is at capacity
			if(clientFCMTokenList.size() >= fcmTokenListLimit) {
				//Removing the oldest item
				clientFCMTokenList.remove(fcmTokenListLimit - 1);
			}
			
			//Adding the item to the top of the list
			clientFCMTokenList.add(0, token);
			setClientFCMTokenListModified();
		}
	}
	
	/**
	 * Remove a client FCM token to this group
	 * @param token The token to remove
	 */
	public void removeClientFCMToken(String token) {
		boolean containedToken = clientFCMTokenList.remove(token);
		if(containedToken) setClientFCMTokenListModified();
	}
	
	/**
	 * Called to check if the FCM token list has been modified
	 * since it was loaded into this connection group
	 * @return TRUE if the FCM token list has been modified
	 */
	public boolean isClientFCMTokenListModified() {
		return isClientFCMTokenListModified;
	}
	
	/**
	 * Marks the FCM token list as modified, to be saved
	 * when this group is finished
	 */
	public void setClientFCMTokenListModified() {
		isClientFCMTokenListModified = true;
	}
}