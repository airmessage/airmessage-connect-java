package me.tagavari.airmessageconnect.structure;

import me.tagavari.airmessageconnect.ClientData;
import me.tagavari.airmessageconnect.Main;
import me.tagavari.airmessageconnect.SharedData;
import org.java_websocket.WebSocket;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Represents a total collection of servers and their client connections
 */
public class ConnectionCollection {
	//Amount of clients allowed per group
	private static final int groupCapacity = 3;
	
	private final Map<String, ConnectionGroup> connectionMap = Collections.synchronizedMap(new HashMap<>());
	
	/**
	 * Registers a new server WebSocket with its group
	 *
	 * This method will create a new group if there isn't currently one,
	 * and will replace an existing one otherwise
	 * @param connection The server WebSocket connection
	 * @param groupID The connection's group ID
	 */
	public void addServer(WebSocket connection, String groupID) {
		//Destroying a group if it already exists
		ConnectionGroup existingGroup = connectionMap.get(groupID);
		if(existingGroup != null) existingGroup.closeAll(SharedData.closeCodeOtherLocation);
		
		//Creating a new group
		ConnectionGroup newGroup = new ConnectionGroup(connection, groupID);
		connectionMap.put(groupID, newGroup);
		
		//Setting the connection's group
		connection.<ClientData>getAttachment().setConnectionGroup(newGroup);
	}
	
	/**
	 * Registers a new client WebSocket with its group
	 *
	 * This method will disconnect the client automatically
	 * if it cannot be added to a group
	 *
	 * @param connection The client WebSocket client
	 * @param groupID The connection's group ID
	 * @return TRUE if the operation was successful
	 */
	public boolean addClient(WebSocket connection, String groupID) {
		//Getting the group
		ConnectionGroup group = connectionMap.get(groupID);
		
		//Checking if the group was not found
		if(group == null) {
			//Closing the connection
			Main.getLogger().log(Level.FINE, "Rejecting connection (no group - " + groupID + ") from client " + Main.connectionToString(connection));
			connection.close(SharedData.closeCodeNoGroup);
			return false;
		}
		
		//Checking if the group is at capacity
		if(group.getCount() >= groupCapacity) {
			//Closing the connection
			Main.getLogger().log(Level.FINE, "Rejecting connection (no capacity - " + groupID + ") from client " + Main.connectionToString(connection));
			connection.close(SharedData.closeCodeNoCapacity);
			return false;
		}
		
		//Generating a new connection ID
		int connectionID = group.nextConnectionID();
		
		//Adding the client connection
		group.addClient(connectionID, connection);
		
		//Setting the connection's group
		ClientData clientData = connection.getAttachment();
		clientData.setConnectionGroup(group);
		clientData.setConnectionID(connectionID);
		
		//Operation successful!
		return true;
	}
	
	public void removeGroup(String groupID) {
		connectionMap.remove(groupID);
	}
}