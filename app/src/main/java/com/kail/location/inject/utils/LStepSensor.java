package com.lerist.inject.utils;

public class LStepSensor {
    private static native int doHook(byte[] dexBytes, String targetProcessName);

    public static native boolean isMocking();

    public static native void setMocking(boolean enabled);

    public static native void setSensorValues(int sensorType, int accuracy, float[] values);

    public static boolean loadAndHook(byte[] dexBytes, String targetProcessName) {
        if (dexBytes != null && targetProcessName != null) {
            try {
                System.load("/data/fakeloc/libStepSensor.so");
            } catch (Throwable th) {
                th.printStackTrace();
            }
            try {
                return doHook(dexBytes, targetProcessName) == 0;
            } catch (Throwable th2) {
                th2.printStackTrace();
            }
        }
        return false;
    }
}
