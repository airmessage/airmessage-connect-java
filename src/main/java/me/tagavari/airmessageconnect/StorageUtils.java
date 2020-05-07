package me.tagavari.airmessageconnect;

import com.google.cloud.firestore.*;
import me.tagavari.airmessageconnect.document.DocumentUser;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class StorageUtils {
	//Singleton instance
	private static final StorageUtils storageUtils = new StorageUtils();
	
	//Database structure
	private static final String fieldUsersServerInstallationID = "server_id";
	private static final String fieldUsersRelayID = "relay_id";
	//private static final String fieldUsersIsSubscribed = "is_subscribed";
	//private static final String fieldUsersSubscriptionSource = "subscription_source";
	
	//Database references
	private Firestore db;
	private CollectionReference collectionUsers;
	
	/**
	 * Gets this singleton instance
	 * @return The instance of StorageUtils
	 */
	public static StorageUtils instance() {
		return storageUtils;
	}
	
	/**
	 * Initializes this instance and hooks it up to Firebase
	 */
	public void initialize() {
		//Initializing the database
		 db = FirestoreOptions.getDefaultInstance().getService();
		 collectionUsers = db.collection("users");
	}
	
	/**
	 * Get user information for a particular user
	 * @param userUID The UID of the user to check
	 * @return The document containing user information
	 */
	public DocumentUser getDocumentUser(String userUID) throws ExecutionException, InterruptedException {
		//Retrieving the user's document
		DocumentSnapshot documentSnapshot = collectionUsers.document(userUID).get().get();
		
		//Returning if there is no document
		if(!documentSnapshot.exists()) return null;
		
		//Returning the value
		/* return new DocumentUser(documentSnapshot.getString(fieldUsersRelayID),
				documentSnapshot.getString(fieldUsersServerInstallationID),
				unboxBoolean(documentSnapshot.getBoolean(fieldUsersIsSubscribed)),
				unboxInt(documentSnapshot.get(fieldUsersSubscriptionSource), 0)
		); */
		return new DocumentUser(documentSnapshot.getString(fieldUsersRelayID),
				documentSnapshot.getString(fieldUsersServerInstallationID));
	}
	
	/**
	 * Update the specified user's relay point ID and active server installation ID
	 * @param userUID The UID of the user to update
	 * @param relayID The relay ID to apply
	 * @param installationID The installation ID to apply
	 */
	public void updateRegisteredServerRelayInstallationID(String userUID, String relayID, String installationID) throws ExecutionException, InterruptedException {
		//Creating the update data
		Map<String, Object> update = new HashMap<>();
		update.put(fieldUsersRelayID, relayID);
		update.put(fieldUsersServerInstallationID, installationID);
		
		//Updating the user data
		collectionUsers.document(userUID).set(update, SetOptions.merge()).get();
	}
	
	/**
	 * Update the specified user's relay point ID
	 * @param userUID The UID of the user to update
	 * @param relayID The relay ID to apply
	 */
	public void updateRegisteredServerRelayID(String userUID, String relayID) throws ExecutionException, InterruptedException {
		//Creating the update data
		Map<String, Object> update = new HashMap<>();
		update.put(fieldUsersRelayID, relayID);
		
		//Updating the user data
		collectionUsers.document(userUID).set(update, SetOptions.merge()).get();
	}
	
	/**
	 * Checks if this user has an active subscription and can be provided service
	 * @param userID The UID of the user to check
	 * @return TRUE if this user has a subscription
	 */
	public boolean checkSubscription(String userID) throws ExecutionException, InterruptedException {
		return true;
	}
	
	private static boolean unboxBoolean(Boolean value) {
		return value == null ? false : value;
	}
	
	private static int unboxInt(Object value, int defaultValue) {
		return value == null ? defaultValue : ((Number) value).intValue();
	}
}