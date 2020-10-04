package me.tagavari.airmessageconnect.communicate;

import me.tagavari.airmessageconnect.ClientData;
import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.exceptions.InvalidDataException;
import org.java_websocket.handshake.ClientHandshake;

import java.nio.ByteBuffer;
import java.util.Map;

public interface Protocol {
	/**
	 * Get the version number of this protocol
	 * @return The version number of this protocol
	 */
	int getVersion();
	
	/**
	 * Process incoming data
	 * @param conn The connection of this event
	 * @param clientData The data of this client
	 * @param data The data that was received
	 */
	void receive(WebSocket conn, ClientData clientData, ByteBuffer data);
	
	/**
	 * Handle a handshake as the server
	 * @param conn The client connection that initiated this handshake
	 * @param draft The draft of the current connection
	 * @param request Client request data
	 * @param paramMap A map of parameter keys to data (helper)
	 * @return The client data to attach to this connection
	 * @throws InvalidDataException If this request is to be rejected
	 */
	ClientData handleHandshake(WebSocket conn, Draft draft, ClientHandshake request, Map<String, String> paramMap) throws InvalidDataException;
	
	/**
	 * Sends a message signaling the client that the connection is OK to use
	 * @return The data to send
	 */
	byte[] sendSharedConnectionOK();
	
	/**
	 * Forwards a message from a server to a client
	 * @param payload The data to send
	 * @return The data to send
	 */
	byte[] sendClientProxy(byte[] payload);
	
	/**
	 * Forwards a message from a client to a server
	 * @param connectionID The connection ID of the sending client
	 * @param payload The data to send
	 * @return The data to send
	 */
	byte[] sendServerProxy(int connectionID, byte[] payload);
	
	/**
	 * Sends a message to the server, notifying it about the connection of a client
	 * @param connectionID The client's connection ID
	 * @return The data to send
	 */
	byte[] sendServerConnection(int connectionID);
	
	/**
	 * Sends a message to the server, notifying it about the disconnection of a client
	 * @param connectionID The client's connection ID
	 * @return The data to send
	 */
	byte[] sendServerDisconnection(int connectionID);
}