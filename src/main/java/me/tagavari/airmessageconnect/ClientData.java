package me.tagavari.airmessageconnect;

import me.tagavari.airmessageconnect.communicate.Protocol;
import me.tagavari.airmessageconnect.structure.ConnectionGroup;

public class ClientData {
	private final boolean isServer;
	private Type type;
	private final Protocol protocol;
	private ConnectionGroup connectionGroup;
	private int connectionID;
	
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
	
	public static class Type {
		private final String groupID;
		
		public Type(String groupID) {
			this.groupID = groupID;
		}
		
		public String getGroupID() {
			return groupID;
		}
	}
}