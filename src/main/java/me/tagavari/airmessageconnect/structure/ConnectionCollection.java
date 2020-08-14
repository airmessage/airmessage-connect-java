package me.tagavari.airmessageconnect.structure;

import me.tagavari.airmessageconnect.ClientData;
import me.tagavari.airmessageconnect.Main;
import me.tagavari.airmessageconnect.SharedData;
import me.tagavari.airmessageconnect.StorageUtils;
import org.java_websocket.WebSocket;
import org.java_websocket.framing.CloseFrame;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

/**
 * Represents a total collection of servers and their client connections
 */
public class ConnectionCollection {
	//Amount of clients allowed per group
	private static final int groupCapacity = 4;
	
	private final Map<String, ConnectionGroup> connectionMap = Collections.synchronizedMap(new HashMap<>());
	
	/**
	 * Registers a new server WebSocket with its group
	 *
	 * This method will create a new group if there isn't currently one,
	 * and will replace an existing one otherwise
	 * @param connection The server WebSocket connection
	 * @param groupID The connection's group ID
	 * @return TRUE if the operation was successful
	 */
	public boolean addServer(WebSocket connection, String groupID) {
		List<String> fcmTokenList;
		boolean fcmTokenListModified;
		
		//Checking if a group already exists
		ConnectionGroup existingGroup = connectionMap.get(groupID);
		if(existingGroup != null) {
			//Closing the group
			existingGroup.closeAll(SharedData.closeCodeOtherLocation);
			
			//Copying the FCM token list from the previous group
			fcmTokenList = existingGroup.getClientFCMTokenList();
			fcmTokenListModified = existingGroup.isClientFCMTokenListModified();
		} else {
			if(!Main.isUnlinked()) {
				//Reading the FCM token list from the database
				try {
					List<String> dataFCMTokens = StorageUtils.instance().getFCMTokens(groupID);
					if(dataFCMTokens == null) fcmTokenList = null;
					else fcmTokenList = new ArrayList<>(dataFCMTokens);
				} catch(ExecutionException | InterruptedException exception) {
					//Logging the exception
					Main.getLogger().log(Level.SEVERE, exception.getMessage(), exception);
					
					//Closing the connection
					connection.close(CloseFrame.TRY_AGAIN_LATER);
					
					//Returning false
					return false;
				}
			} else {
				fcmTokenList = null;
			}
			
			//Fresh copy - no modifications
			fcmTokenListModified = false;
		}
		
		//Creating a new group
		ConnectionGroup newGroup = new ConnectionGroup(connection, groupID, fcmTokenList);
		if(fcmTokenListModified) newGroup.setClientFCMTokenListModified();
		connectionMap.put(groupID, newGroup);
		
		//Setting the connection's group
		connection.<ClientData>getAttachment().setConnectionGroup(newGroup);
		
		//Returning true
		return true;
	}
	
	/**
	 * Registers a new client WebSocket with its group
	 *
	 * This method will disconnect the client automatically
	 * if it cannot be added to a group
	 *
	 * @param connection The client WebSocket client
	 * @param groupID The connection's group ID
	 * @param fcmToken The client's FCM token (or NULL if none is available)
	 * @return TRUE if the operation was successful
	 */
	public boolean addClient(WebSocket connection, String groupID, String fcmToken) {
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
		
		//Registering the client's FCM token
		if(fcmToken != null) group.addClientFCMToken(fcmToken);
		
		//Operation successful!
		return true;
	}
	
	public void removeGroup(String groupID) {
		connectionMap.remove(groupID);
	}
}