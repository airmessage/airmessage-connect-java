package me.tagavari.airmessageconnect;

public class SharedData {
	public static final int closeCodeIncompatibleProtocol = 4000; //No protocol version matching the one requested
	public static final int closeCodeNoGroup = 4001; //There is no active group with a matching ID
	public static final int closeCodeNoCapacity = 4002; //The client's group is at capacity
	public static final int closeCodeAccountValidation = 4003; //This account couldn't be validated
	public static final int closeCodeServerTokenRefresh = 4004; //The server's provided installation ID is out of date; log in again to re-link this device
	public static final int closeCodeNoSubscription = 4005; //This user does not have an active subscription
	public static final int closeCodeOtherLocation = 4006; //Logged in from another location
}