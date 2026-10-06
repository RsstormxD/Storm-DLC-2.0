package xyz.angames.astolfoclient.client.util;

import java.util.Set;

/** Compatibility entry point for existing commands and cosmetic modules. */
public final class FriendManager {
    private FriendManager() {}
    public static void addFriend(String name) { FriendsManager.addFriend(name); }
    public static void removeFriend(String name) { FriendsManager.removeFriend(name); }
    public static boolean isFriend(String name) { return FriendsManager.isFriend(name); }
    public static void clearFriends() { FriendsManager.clearFriends(); }
    public static Set<String> getFriends() { return FriendsManager.getFriends(); }
}
