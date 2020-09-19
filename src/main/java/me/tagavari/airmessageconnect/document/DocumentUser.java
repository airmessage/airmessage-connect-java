package me.tagavari.airmessageconnect.document;

import java.util.List;

public class DocumentUser {
	public final String relayID;
	public final String installationID;
	public final boolean isActivated;
	//public final boolean isSubscribed;
	//public final int subscriptionSource;
	
	public DocumentUser(String relayID, String installationID, boolean isActivated) {
		this.relayID = relayID;
		this.installationID = installationID;
		this.isActivated = isActivated;
	}
}