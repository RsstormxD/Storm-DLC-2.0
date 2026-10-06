package com.top1.client;
/** The integrated port is released as part of Storm DLC 2.0. */
public final class VersionCheck {
    public static final String VERSION = dev.stormdlc.update.ClientUpdates.currentVersion();
    public static boolean isOutdated() { return dev.stormdlc.update.ClientUpdates.isOutdated(); }
}
