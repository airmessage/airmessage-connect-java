package me.tagavari.airmessageconnect.communicate.protocol1;

class NHT {
	//Shared het header types
	/*
	 * The connected device has been connected successfully
	 */
	static final int nhtConnectionOK = 0;
	
	//Client-only net header types
	
	/*
	 * Proxy the message to the server (client -> connect)
	 *
	 * payload - data
	 */
	static final int nhtClientProxy = 100;
	
	/*
	 * Add an item to the list of FCM tokens (client -> connect)
	 *
	 * string - registration token
	 */
	static final int nhtClientAddFCMToken = 110;
	
	/*
	 * Remove an item from the list of FCM tokens (client -> connect)
	 *
	 * string - registration token
	 */
	static final int nhtClientRemoveFCMToken = 111;
	
	//Server-only net header types
	
	/*
	 * Notify a new client connection (connect -> server)
	 *
	 * int - connection ID
	 */
	static final int nhtServerOpen = 200;
	
	/*
	 * Close a connected client (server -> connect)
	 * Notify a closed connection (connect -> server)
	 *
	 * int - connection ID
	 */
	static final int nhtServerClose = 201;
	
	/*
	 * Proxy the message to the client (server -> connect)
	 * Receive data from a connected client (connect -> server)
	 *
	 * int - connection ID
	 * payload - data
	 */
	static final int nhtServerProxy = 210;
	
	/*
	 * Proxy the message to all connected clients (server -> connect)
	 *
	 * payload - data
	 */
	static final int nhtServerProxyBroadcast = 211;
	
	/**
	 * Notify offline clients of a new message
	 */
	static final int nhtServerNotifyPush = 212;
}