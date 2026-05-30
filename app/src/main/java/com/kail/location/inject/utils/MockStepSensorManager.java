package com.lerist.inject.utils;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;
import java.text.SimpleDateFormat;

public class MockStepSensorManager {
    private static float baseStepCount = 0.0f;
    private static float stepCountOffset = 0.0f;
    private static float stepSpeed = 1.0f;
    private static boolean mocking = false;
    private static boolean monitorStarted = false;
    private static long mockStartTimeMillis = 0;
    private static long lastStepUpdateTimeMillis = 0;
    private static boolean dataInjectionHookEnabled = false;
    private static boolean sensorFeatureEnabled = false;
    static SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd");
    static int updateIntervalMillis = 50;

    static class StepSensorMonitor implements Runnable {
        class SensorListener implements SensorEventListener {
            SensorListener() {
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }

            @Override
            public void onSensorChanged(SensorEvent event) {
            }
        }

        StepSensorMonitor() {
        }

        @Override
        public void run() {
            throw new UnsupportedOperationException("Method not decompiled: com.lerist.inject.utils.MockStepSensorManager.StepSensorMonitor.run():void");
        }
    }

    public static boolean hook_nativeIsDataInjectionEnabled(long nativePtr) {
        if (!mocking || !dataInjectionHookEnabled) {
            return hook_nativeIsDataInjectionEnabled_bak(nativePtr);
        }
        hook_nativeIsDataInjectionEnabled_bak(nativePtr);
        return true;
    }

    public static boolean hook_nativeIsDataInjectionEnabled_bak(long nativePtr) {
        try {
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append("#");
            stringBuffer.append("#");
            stringBuffer.append("#");
            stringBuffer.append("#");
            stringBuffer.append("#");
            stringBuffer.append("#");
            stringBuffer.toString();
            for (int i = 0; i < 100; i = i + 1 + 1) {
            }
            for (int i2 = 0; i2 < 100; i2 = i2 + 1 + 1) {
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean hook_nativeIsDataInjectionEnabled_copy(long nativePtr) {
        return false;
    }

    public static long getMockStepCount() {
        return (long) (baseStepCount + stepCountOffset);
    }

    public static float getStepSpeed() {
        return stepSpeed;
    }

    private static void startMonitorThread() {
        Log.e("MSU", "init...");
        mockStartTimeMillis = System.currentTimeMillis();
        new Thread(new StepSensorMonitor()).start();
        monitorStarted = true;
    }

    public static boolean isSensorFeatureEnabled() {
        return sensorFeatureEnabled;
    }

    public static boolean isStepSensorMocking() {
        try {
            return LStepSensor.isMocking();
        } catch (Throwable unused) {
            return false;
        }
    }

    static boolean isSameDay(long timestampMillis) {
        return dayFormat.format(Long.valueOf(System.currentTimeMillis())).equals(dayFormat.format(Long.valueOf(timestampMillis)));
    }

    public static void setSensorFeatureEnabled(boolean enabled) {
        sensorFeatureEnabled = enabled;
    }

    public static void setBaseStepCount(long stepCount) {
        baseStepCount = stepCount;
    }

    public static void setStepCountOffset(long stepCount) {
        stepCountOffset = stepCount;
        baseStepCount = 0.0f;
    }

    public static void setStepSpeed(float speed) {
        mockStartTimeMillis = System.currentTimeMillis();
        stepSpeed = speed;
    }

    public static boolean loadStepSensorHook(byte[] dexBytes, String targetProcessName) {
        return LStepSensor.loadAndHook(dexBytes, targetProcessName);
    }

    public static void startStepSensorMock() {
        mockStartTimeMillis = System.currentTimeMillis();
        mocking = true;
        if (!monitorStarted) {
            startMonitorThread();
        }
        try {
            loadStepSensorHook(LicenseStateManager.getCurrentLicenseBytes(), LicenseStateManager.getCurrentDeviceId());
            LStepSensor.setMocking(mocking);
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    public static void stopStepSensorMock() {
        mocking = false;
        try {
            LStepSensor.setMocking(false);
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }
}
