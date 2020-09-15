package me.tagavari.airmessageconnect;

import me.tagavari.airmessageconnect.communicate.Protocol;
import me.tagavari.airmessageconnect.structure.ConnectionGroup;

/**
 * Represents data associated with a client
 */
public class ClientData {
	private final int closeCode;
	private final boolean isServer;
	private Type type;
	private final Protocol protocol;
	private ConnectionGroup connectionGroup;
	private int connectionID;
	private boolean disableCleanup = false;
	
	//For failed clients
	public ClientData(int closeCode) {
		this.closeCode = closeCode;
		this.isServer = false;
		this.type = null;
		this.protocol = null;
	}
	
	/**
	 * For valid clients
	 * @param isServer TRUE if this client is a server
	 * @param type Connection classification data for this client
	 * @param protocol The protocol that this client is using
	 */
	public ClientData(boolean isServer, Type type, Protocol protocol) {
		this.closeCode = -1;
		this.isServer = isServer;
		this.type = type;
		this.protocol = protocol;
	}
	
	public boolean isRejected() {
		return closeCode != -1;
	}
	
	public int getCloseCode() {
		return closeCode;
	}
	
	public boolean isServer() {
		return isServer;
	}
	
	public Type getType() {
		return type;
	}
	
	public void clearType() {
		type = null;
	}
	
	public Protocol getProtocol() {
		return protocol;
	}
	
	public ConnectionGroup getConnectionGroup() {
		return connectionGroup;
	}
	
	public void setConnectionGroup(ConnectionGroup connectionGroup) {
		this.connectionGroup = connectionGroup;
	}
	
	public int getConnectionID() {
		return connectionID;
	}
	
	public void setConnectionID(int connectionID) {
		this.connectionID = connectionID;
	}
	
	public boolean getDisableCleanup() {
		return disableCleanup;
	}
	
	public void setDisableCleanup(boolean disableCleanup) {
		this.disableCleanup = disableCleanup;
	}
	
	public static class Type {
		private final String groupID;
		private final String fcmToken;
		
		public Type(String groupID, String fcmToken) {
			this.groupID = groupID;
			this.fcmToken = fcmToken;
		}
		
		public String getGroupID() {
			return groupID;
		}
		
		public String getFCMToken() {
			return fcmToken;
		}
	}
}