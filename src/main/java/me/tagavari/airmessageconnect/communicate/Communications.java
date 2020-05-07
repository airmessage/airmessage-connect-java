package me.tagavari.airmessageconnect.communicate;

import me.tagavari.airmessageconnect.communicate.protocol1.Protocol1;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Communications {
	private static final Map<Integer, Protocol> protocols;
	static {
		Map<Integer, Protocol> map = new HashMap<>();
		map.put(1, new Protocol1());
		protocols = Collections.unmodifiableMap(map);
	}
	
	public static Protocol getProtocol(int commVer) {
		return protocols.get(commVer);
	}
}