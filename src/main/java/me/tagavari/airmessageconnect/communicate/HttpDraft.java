package me.tagavari.airmessageconnect.communicate;

import org.java_websocket.WebSocketImpl;
import org.java_websocket.drafts.Draft;
import org.java_websocket.enums.CloseHandshakeType;
import org.java_websocket.enums.HandshakeState;
import org.java_websocket.exceptions.InvalidDataException;
import org.java_websocket.exceptions.InvalidHandshakeException;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.*;
import org.java_websocket.util.Charsetfunctions;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

public class HttpDraft extends Draft {
	public static boolean isHTTP(ClientHandshake clientHandshake) {
		String upgrade = clientHandshake.getFieldValue("Upgrade");
		return upgrade == null || upgrade.isEmpty();
	}
	
	@Override
	public List<ByteBuffer> createHandshake(Handshakedata handshakedata) {
		return Collections.singletonList(ByteBuffer.wrap(Charsetfunctions.asciiBytes(
			"HTTP/1.0 200 OK\r\n" +
			"Connection: close\r\n" +
			"\r\n"
		)));
	}
	
	@Override
	public List<ByteBuffer> createHandshake(Handshakedata handshakedata, boolean withcontent) {
		return Collections.singletonList(ByteBuffer.wrap(Charsetfunctions.asciiBytes(
			"HTTP/1.0 200 OK\r\n" +
			"Connection: close\r\n" +
			"\r\n"
		)));
	}
	
	@Override
	public HandshakeState acceptHandshakeAsClient(ClientHandshake request, ServerHandshake response) throws InvalidHandshakeException {
		throw new RuntimeException("This draft can't be used on a client");
	}
	
	@Override
	public HandshakeState acceptHandshakeAsServer(ClientHandshake handshakedata) throws InvalidHandshakeException {
		if(isHTTP(handshakedata) && (handshakedata.getResourceDescriptor().startsWith("/health"))) {
			return HandshakeState.MATCHED;
		} else {
			return HandshakeState.NOT_MATCHED;
		}
	}
	
	@Override
	public ByteBuffer createBinaryFrame(Framedata framedata) {
		throw new RuntimeException("This draft doesn't work with frames");
	}
	
	@Override
	public List<Framedata> createFrames(ByteBuffer binary, boolean mask) {
		throw new RuntimeException("This draft doesn't work with frames");
	}
	
	@Override
	public List<Framedata> createFrames(String text, boolean mask) {
		throw new RuntimeException("This draft doesn't work with frames");
	}
	
	@Override
	public void processFrame(WebSocketImpl webSocketImpl, Framedata frame) throws InvalidDataException {
		throw new RuntimeException("This draft doesn't work with frames");
	}
	
	@Override
	public void reset() {
	
	}
	
	@Override
	public ClientHandshakeBuilder postProcessHandshakeRequestAsClient(ClientHandshakeBuilder request) throws InvalidHandshakeException {
		throw new RuntimeException("This draft can't be used on a client");
	}
	
	@Override
	public HandshakeBuilder postProcessHandshakeResponseAsServer(ClientHandshake request, ServerHandshakeBuilder response) throws InvalidHandshakeException {
		return response;
	}
	
	@Override
	public List<Framedata> translateFrame(ByteBuffer buffer) throws InvalidDataException {
		throw new RuntimeException("This draft doesn't work with frames");
	}
	
	@Override
	public CloseHandshakeType getCloseHandshakeType() {
		return CloseHandshakeType.NONE;
	}
	
	@Override
	public Draft copyInstance() {
		return this;
	}
}