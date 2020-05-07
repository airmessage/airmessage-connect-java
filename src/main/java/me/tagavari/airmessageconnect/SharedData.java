package me.tagavari.airmessageconnect;

public class SharedData {
	public static final int closeCodeNoGroup = 4000; //There is no active group with a matching ID
	public static final int closeCodeNoCapacity = 4001; //The client's group is at capacity
	public static final int closeCodeAccountValidation = 4002; //This account couldn't be validated
	public static final int closeCodeServerTokenRefresh = 4003; //The server's provided installation ID is out of date; log in again to re-link this device
	public static final int closeCodeNoSubscription = 4004; //This user does not have an active subscription
	public static final int closeCodeOtherLocation = 4005; //Logged in from another location
}