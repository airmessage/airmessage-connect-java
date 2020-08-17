package me.tagavari.airmessageconnect;

import me.tagavari.airmessageconnect.communicate.Protocol;
import me.tagavari.airmessageconnect.structure.ConnectionGroup;

public class ClientData {
	private final boolean isServer;
	private Type type;
	private final Protocol protocol;
	private ConnectionGroup connectionGroup;
	private int connectionID;
	private boolean disableCleanup = false;
	
	public ClientData(boolean isServer, Type type, Protocol protocol) {
		this.isServer = isServer;
		this.type = type;
		this.protocol = protocol;
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
	
	public void setType(Type type) {
		this.type = type;
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
		private final int closeCode;
		
		public Type(String groupID, String fcmToken) {
			this.groupID = groupID;
			this.fcmToken = fcmToken;
			this.closeCode = -1;
		}
		
		public Type(int closeCode) {
			this.groupID = null;
			this.fcmToken = null;
			this.closeCode = closeCode;
		}
		
		public String getGroupID() {
			return groupID;
		}
		
		public String getFCMToken() {
			return fcmToken;
		}
		
		public int getCloseCode() {
			return closeCode;
		}
	}
}