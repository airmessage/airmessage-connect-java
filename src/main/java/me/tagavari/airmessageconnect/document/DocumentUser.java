package me.tagavari.airmessageconnect.document;

public class DocumentUser {
	public final String relayID;
	public final String installationID;
	//public final boolean isSubscribed;
	//public final int subscriptionSource;
	
	public DocumentUser(String relayID, String installationID) {
		this.relayID = relayID;
		this.installationID = installationID;
	}
}