package me.tagavari.airmessageconnect.communicate.protocol1;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import me.tagavari.airmessageconnect.ClientData;
import me.tagavari.airmessageconnect.Main;
import me.tagavari.airmessageconnect.SharedData;
import me.tagavari.airmessageconnect.StorageUtils;
import me.tagavari.airmessageconnect.communicate.Protocol;
import me.tagavari.airmessageconnect.document.DocumentUser;
import me.tagavari.airmessageconnect.structure.ConnectionGroup;
import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.exceptions.InvalidDataException;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

public class Protocol1 implements Protocol {
	@Override
	public void receive(WebSocket conn, ClientData clientData, ByteBuffer bytes) {
		try {
			//Unpacking the message
			int type = bytes.getInt();
			
			switch(type) {
				case NHT.nhtClientProxy: {
					//Client-only
					if(clientData.isServer()) break;
					
					//Reading the data
					byte[] data = new byte[bytes.remaining()];
					bytes.get(data);
					
					//Getting the server's connection
					WebSocket socket = clientData.getConnectionGroup().getConnectionServer();
					
					//Sending the data to the server
					socket.send(socket.<ClientData>getAttachment().getProtocol().sendServerProxy(clientData.getConnectionID(), data));
					
					break;
				}
				case NHT.nhtClientAddFCMToken: {
					//Client-only
					if(clientData.isServer()) break;
					
					//Reading the token
					byte[] data = new byte[bytes.remaining()];
					bytes.get(data);
					String token = new String(data, StandardCharsets.ISO_8859_1);
					
					//Adding the token
					clientData.getConnectionGroup().addClientFCMToken(token);
					
					break;
				}
				case NHT.nhtClientRemoveFCMToken: {
					//Client-only
					if(clientData.isServer()) break;
					
					//Reading the token
					byte[] data = new byte[bytes.remaining()];
					bytes.get(data);
					String token = new String(data, StandardCharsets.ISO_8859_1);
					
					//Removing the token
					clientData.getConnectionGroup().removeClientFCMToken(token);
					
					break;
				}
				case NHT.nhtServerClose: {
					//Server-only
					if(!clientData.isServer()) break;
					
					//Reading the data
					int connectionID = bytes.getInt();
					
					//Removing the connection
					clientData.getConnectionGroup().closeClient(connectionID, CloseFrame.NORMAL);
					
					break;
				}
				case NHT.nhtServerProxy: {
					//Server-only
					if(!clientData.isServer()) break;
					
					//Reading the data
					int connectionID = bytes.getInt();
					byte[] data = new byte[bytes.remaining()];
					bytes.get(data);
					
					//Getting the specified client's connection
					WebSocket socket = clientData.getConnectionGroup().getConnectionClient(connectionID);
					
					//Checking if the connection wasn't found
					if(socket == null) {
						//Notifying the server that this connection is disconnected
						sendServerDisconnection(connectionID);
					} else {
						//Sending the data to the client
						socket.send(socket.<ClientData>getAttachment().getProtocol().sendClientProxy(data));
					}
					
					break;
				}
				case NHT.nhtServerProxyBroadcast: {
					//Server-only
					if(!clientData.isServer()) break;
					
					//Reading the data
					byte[] data = new byte[bytes.remaining()];
					bytes.get(data);
					
					//Getting the server' connection group
					ConnectionGroup connectionGroup = clientData.getConnectionGroup();
					
					//Sending the data to all clients
					for(WebSocket socket : connectionGroup.getAllConnectionsClient()) {
						socket.send(socket.<ClientData>getAttachment().getProtocol().sendClientProxy(data));
					}
					
					break;
				}
				case NHT.nhtServerNotifyPush: {
					//Server-only
					if(!clientData.isServer()) break;
					
					//Getting the server' connection group
					ConnectionGroup connectionGroup = clientData.getConnectionGroup();
					
					//Sending a firebase message
					List<String> tokens = new ArrayList<>(connectionGroup.getClientFCMTokenList());
					if(tokens.isEmpty()) break;
					MulticastMessage message = MulticastMessage.builder()
							.addAllTokens(tokens)
							.build();
					ApiFuture<BatchResponse> responseFuture = FirebaseMessaging.getInstance().sendMulticastAsync(message);
					ApiFutures.addCallback(responseFuture, new ApiFutureCallback<BatchResponse>() {
						@Override
						public void onFailure(Throwable throwable) {
							throwable.printStackTrace();
						}
						
						@Override
						public void onSuccess(BatchResponse response) {
							if(response.getFailureCount() > 0) {
								//Finding failed responses
								List<SendResponse> responseList = response.getResponses();
								for(int i = 0; i < responseList.size(); i++) {
									if(responseList.get(i).isSuccessful()) continue;
									
									//The order of responses corresponds to the order of the registration tokens
									String failedToken = tokens.get(i);
									
									//Removing failed tokens
									connectionGroup.removeClientFCMToken(failedToken);
								}
							}
						}
					}, MoreExecutors.directExecutor());
					
					break;
				}
			}
		} catch(BufferUnderflowException exception) {
			Main.getLogger().log(Level.WARNING, exception.getMessage(), exception);
		}
	}
	
	@Override
	public void handleHandshake(WebSocket conn, Draft draft, ClientHandshake request, Map<String, String> cookieMap) throws InvalidDataException {
		//Reading parameter data
		boolean isServer;
		String installationID, idToken, userID, fcmToken;
		try {
			isServer = Boolean.parseBoolean(cookieMap.get("isServer"));
			installationID = cookieMap.get("installationID");
			idToken = cookieMap.get("idToken");
			userID = cookieMap.get("userID");
			if(userID != null) userID = URLDecoder.decode(userID, "UTF-8");
			fcmToken = cookieMap.get("fcmToken");
			if(fcmToken != null) fcmToken = URLDecoder.decode(fcmToken, "UTF-8");
		} catch(NumberFormatException | UnsupportedEncodingException exception) {
			Main.getLogger().log(Level.WARNING, "Rejecting handshake (bad request format) from client " + Main.connectionToString(conn));
			Main.getLogger().log(Level.WARNING, exception.getMessage(), exception);
			throw new InvalidDataException(CloseFrame.PROTOCOL_ERROR);
		}
		
		try {
			//Checking if this is a server request
			if(isServer) {
				//Failing if there is no installation ID or the installation ID is invalid ("/" prevents injection attacks)
				if(installationID == null || installationID.isEmpty() || installationID.contains("/")) {
					throw new InvalidDataException(CloseFrame.PROTOCOL_ERROR);
				}
				
				//Checking if this is a first-time connection
				if(idToken != null) {
					//Failing if a user ID was provided
					if(userID != null) {
						Main.getLogger().log(Level.WARNING, "Rejecting handshake (user ID provided - " + userID + ") from client " + Main.connectionToString(conn));
						throw new InvalidDataException(CloseFrame.PROTOCOL_ERROR);
					}
					
					//Validating the ID token
					try {
						FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
						userID = decodedToken.getUid();
					} catch(IllegalArgumentException exception) {
						Main.getLogger().log(Level.WARNING, "Rejecting handshake (illegal Firebase state) from client " + Main.connectionToString(conn));
						Main.getLogger().log(Level.WARNING, exception.getMessage(), exception);
						throw new InvalidDataException(CloseFrame.PROTOCOL_ERROR);
					} catch(FirebaseAuthException exception) {
						Main.getLogger().log(Level.WARNING, "Rejecting handshake (token validation error) from client " + Main.connectionToString(conn));
						Main.getLogger().log(Level.WARNING, exception.getMessage(), exception);
						throw new InvalidDataException(SharedData.closeCodeAccountValidation);
					}
					
					//Rejecting if this user doesn't have a subscription
					if(!StorageUtils.instance().checkSubscription(userID)) {
						Main.getLogger().log(Level.WARNING, "Rejecting handshake (no subscription) from client " + Main.connectionToString(conn));
						throw new InvalidDataException(SharedData.closeCodeNoSubscription);
					}
					
					//Updating the installation ID and relay ID for this user
					StorageUtils.instance().updateRegisteredServerRelayInstallationID(userID, Main.getRelayID(), installationID);
				} else {
					//Failing if there is no user ID, or the user ID is invalid ("/" prevents injection attacks)
					if(userID == null || userID.isEmpty() || userID.contains("/")) {
						Main.getLogger().log(Level.WARNING, "Rejecting handshake (bad user ID - " + userID + ") from client " + Main.connectionToString(conn));
						throw new InvalidDataException(CloseFrame.PROTOCOL_ERROR);
					}
					
					//Rejecting if this user doesn't have a subscription
					if(!StorageUtils.instance().checkSubscription(userID)) {
						Main.getLogger().log(Level.WARNING, "Rejecting handshake (no subscription) from client " + Main.connectionToString(conn));
						throw new InvalidDataException(SharedData.closeCodeNoSubscription);
					}
					
					//Fetching user details
					DocumentUser documentUser = StorageUtils.instance().getDocumentUser(userID);
					
					//Rejecting if this is installation ID out-of-date
					if(documentUser == null || !installationID.equals(documentUser.installationID)) {
						Main.getLogger().log(Level.WARNING, "Rejecting handshake (token refresh) from client " + Main.connectionToString(conn));
						throw new InvalidDataException(SharedData.closeCodeServerTokenRefresh);
					}
					
					//Updating the relay ID for this user (if necessary)
					String thisRelayID = Main.getRelayID();
					if(!thisRelayID.equals(documentUser.relayID)) StorageUtils.instance().updateRegisteredServerRelayID(userID, thisRelayID);
				}
			} else {
				//Failing if a user ID was provided
				if(userID != null) {
					Main.getLogger().log(Level.WARNING, "Rejecting handshake (user ID provided - " + userID + ") from client " + Main.connectionToString(conn));
					throw new InvalidDataException(CloseFrame.PROTOCOL_ERROR);
				}
				
				//Validating the ID token (and failing if a user UID was provided)
				if(idToken == null) {
					Main.getLogger().log(Level.WARNING, "Rejecting handshake (no ID token provided) from client " + Main.connectionToString(conn));
					throw new InvalidDataException(CloseFrame.PROTOCOL_ERROR);
				}
				
				try {
					FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
					userID = decodedToken.getUid();
				} catch(IllegalArgumentException exception) {
					Main.getLogger().log(Level.WARNING, "Rejecting handshake (illegal Firebase state) from client " + Main.connectionToString(conn));
					Main.getLogger().log(Level.WARNING, exception.getMessage(), exception);
					throw new InvalidDataException(CloseFrame.PROTOCOL_ERROR);
				} catch(FirebaseAuthException exception) {
					Main.getLogger().log(Level.WARNING, "Rejecting handshake (token validation error) from client " + Main.connectionToString(conn));
					Main.getLogger().log(Level.WARNING, exception.getMessage(), exception);
					throw new InvalidDataException(SharedData.closeCodeAccountValidation);
				}
				
				//Rejecting if this user doesn't have a subscription
				if(!StorageUtils.instance().checkSubscription(userID)) {
					Main.getLogger().log(Level.WARNING, "Rejecting handshake (no subscription) from client " + Main.connectionToString(conn));
					throw new InvalidDataException(SharedData.closeCodeNoSubscription);
				}
			}
		} catch(ExecutionException | InterruptedException exception) {
			Main.getLogger().log(Level.WARNING, "Rejecting handshake (internal exception) from client " + Main.connectionToString(conn));
			Main.getLogger().log(Level.WARNING, exception.getMessage(), exception);
			
			//Internal error
			throw new InvalidDataException(CloseFrame.TRY_AGAIN_LATER);
		}
		
		//Tagging the client with its type information and communication version
		conn.setAttachment(new ClientData(isServer, new ClientData.Type(userID, fcmToken), this));
	}
	
	@Override
	public byte[] sendSharedConnectionOK() {
		ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.BYTES);
		byteBuffer.putInt(NHT.nhtConnectionOK);
		
		return byteBuffer.array();
	}
	
	@Override
	public byte[] sendClientProxy(byte[] payload) {
		ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.BYTES + payload.length);
		byteBuffer.putInt(NHT.nhtClientProxy);
		byteBuffer.put(payload);
		
		return byteBuffer.array();
	}
	
	@Override
	public byte[] sendServerConnection(int connectionID) {
		ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.BYTES * 2);
		byteBuffer.putInt(NHT.nhtServerOpen);
		byteBuffer.putInt(connectionID);
		
		return byteBuffer.array();
	}
	
	@Override
	public byte[] sendServerDisconnection(int connectionID) {
		ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.BYTES * 2);
		byteBuffer.putInt(NHT.nhtServerClose);
		byteBuffer.putInt(connectionID);
		
		return byteBuffer.array();
	}
	
	@Override
	public byte[] sendServerProxy(int connectionID, byte[] payload) {
		ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.BYTES * 2 + payload.length);
		byteBuffer.putInt(NHT.nhtServerProxy);
		byteBuffer.putInt(connectionID);
		byteBuffer.put(payload);
		
		return byteBuffer.array();
	}
}