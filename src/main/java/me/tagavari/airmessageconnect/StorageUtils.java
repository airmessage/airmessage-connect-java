package me.tagavari.airmessageconnect;

import com.google.cloud.firestore.*;
import me.tagavari.airmessageconnect.document.DocumentUser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

public class StorageUtils {
	//Singleton instance
	private static final StorageUtils storageUtils = new StorageUtils();
	
	//Database structure
	private static final String fieldUsersRelayID = "relayID";
	private static final String fieldUsersServerInstallationID = "severID";
	private static final String fieldUsersIsActivated = "isActivated";
	//private static final String fieldUsersIsSubscribed = "isSubscribed";
	//private static final String fieldUsersSubscriptionSource = "subscriptionSource";
	
	private static final String collectionUsersData = "data";
	private static final String documentDataFCM = "fcm";
	private static final String fieldFCMList = "fcmTokenList";
	
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
	 * @return The document containing user information, or NULL if none was found
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
		try {
			return new DocumentUser(documentSnapshot.getString(fieldUsersRelayID), documentSnapshot.getString(fieldUsersServerInstallationID), unboxBoolean(documentSnapshot.getBoolean(fieldUsersIsActivated)));
		} catch(RuntimeException exception) {
			Main.getLogger().log(Level.SEVERE, exception.getMessage(), exception);
			return null;
		}
	}
	
	public List<String> getFCMTokens(String userUID) throws ExecutionException, InterruptedException {
		//Retrieving the user's document
		DocumentSnapshot documentSnapshot = collectionUsers.document(userUID + '/' + collectionUsersData + '/' + documentDataFCM).get().get();
		
		//Returning if there is no document
		if(!documentSnapshot.exists()) return null;
		
		//Returning the token list
		try {
			return (List<String>) documentSnapshot.get(fieldFCMList);
		} catch(RuntimeException exception) {
			Main.getLogger().log(Level.SEVERE, exception.getMessage(), exception);
			return null;
		}
	}
	
	public void updateFCMTokens(String userUID, List<String> list) throws ExecutionException, InterruptedException {
		//Creating the update data
		Map<String, Object> update = new HashMap<>();
		update.put(fieldFCMList, list);
		
		//Updating the user data
		collectionUsers.document(userUID + '/' + collectionUsersData + '/' + documentDataFCM).set(update, SetOptions.merge()).get();
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