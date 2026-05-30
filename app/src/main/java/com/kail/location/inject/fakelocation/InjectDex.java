package com.lerist.inject.fakelocation;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Process;
import android.util.Log;
import com.lerist.inject.fakelocation.service.AntiDetectionManagerService;
import com.lerist.inject.fakelocation.service.HideRootManagerService;
import com.lerist.inject.fakelocation.service.MockLocationManagerService;
import com.lerist.inject.fakelocation.service.MockWifiManagerService;
import com.lerist.inject.fakelocation.service.NativeCatchManagerService;
import com.lerist.inject.utils.HiddenApiBypass;
import com.lerist.inject.utils.PackageSignatureVerifier;
import com.lerist.inject.utils.ServiceManagerBridge;
import com.lerist.lib.lhooker.LHooker;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import com.lerist.inject.fakelocation.hook.phone.PhoneInterfaceManagerHook;
import com.lerist.inject.fakelocation.hook.app.AppProcessHook;

public class InjectDex {

    public static List<?> activeHooks = Collections.synchronizedList(new ArrayList());

    static List<InitializationCallback> initializationCallbacks = Collections.synchronizedList(new ArrayList());

    private static Handler mainHandler;

    private static Context applicationContext;

    public interface InitializationCallback {
        void onInitialized();
    }

    public static Object[] hookApplication(Object contextObject) {
        String message;
        applicationContext = (Context) contextObject;
        log("App: " + contextObject);
        try {
            HiddenApiBypass.bypassHiddenApiRestrictions();
            String packageName = ((Context) contextObject).getPackageName();
            if (packageName.equals("com.android.phone")) {
                if (!LHooker.isDeviceX86_64()) {
                    if (!LHooker.isDeviceX86()) {
                        if (LHooker.isDeviceArm64()) {
                            LHooker.loadHookLibrary("/data/fakeloc/liblhooker64.so");
                        } else {
                            LHooker.loadHookLibrary("/data/fakeloc/liblhooker.so");
                        }
                    }
                    LHooker.loadHookLibrary("/data/fakeloc/liblhookerx.so");
                }
                LHooker.loadHookLibrary("/data/fakeloc/liblhookerx64.so");
            } else {
                String nativeLibraryAbi = new File("" + ((Context) contextObject).getPackageManager().getApplicationInfo(packageName.split(":")[0], 0).nativeLibraryDir).getName();
                log("App abi: " + nativeLibraryAbi);
                if (LHooker.isX86_64Abi(nativeLibraryAbi)) {
                    LHooker.loadHookLibrary("/data/fakeloc/liblhookerx64.so");
                } else if (LHooker.isX86Abi(nativeLibraryAbi)) {
                    LHooker.loadHookLibrary("/data/fakeloc/liblhookerx.so");
                } else {
                    if (LHooker.isArm64Abi(nativeLibraryAbi)) {
                        LHooker.loadHookLibrary("/data/fakeloc/liblhooker64.so");
                    }
                    LHooker.loadHookLibrary("/data/fakeloc/liblhooker.so");
                }
            }
            if (!LHooker.initialized) {
                Log.e("InjectDex", "App unfinished.");
                return null;
            }
            LHooker.suspendAll();
            if (packageName.equals("com.android.phone")) {
                PhoneInterfaceManagerHook.hook(((Context) contextObject).getClassLoader());
                message = "App finished.";
            } else {
                AppProcessHook.applyHookToApp((Context) contextObject, packageName);
                message = "App[" + packageName + "] finished.";
            }
            log(message);
            return null;
        } catch (Throwable th) {
            th.printStackTrace();
            Log.e("InjectDex", "App err:" + th.getMessage());
            return null;
        }
    }

    public static Object[] init(Object contextObject) {
        applicationContext = (Context) contextObject;
        Log.d("InjectDex", "init." + contextObject);
        try {
            initializeMainThread((Context) contextObject);
            HiddenApiBypass.bypassHiddenApiRestrictions();
            LHooker.loadHookLibrary(LHooker.isDeviceX86_64() ? "/data/fakeloc/liblhookerx64.so" : LHooker.isDeviceX86() ? "/data/fakeloc/liblhookerx.so" : LHooker.isDeviceArm64() ? "/data/fakeloc/liblhooker64.so" : "/data/fakeloc/liblhooker.so");
            PackageSignatureVerifier.verifyPackageSignature((Context) contextObject, "com.lerist.fakelocation", "service_mock_manager");
            ServiceManagerBridge.addService(((Context) contextObject).getClassLoader(), "service_mock_location", new MockLocationManagerService());
            ServiceManagerBridge.addService(((Context) contextObject).getClassLoader(), "service_mock_wifi", new MockWifiManagerService());
            ServiceManagerBridge.addService(((Context) contextObject).getClassLoader(), "service_mock_antidetection", new AntiDetectionManagerService());
            ServiceManagerBridge.addService(((Context) contextObject).getClassLoader(), "service_hide_root", new HideRootManagerService());
            ServiceManagerBridge.addService(((Context) contextObject).getClassLoader(), "service_nativecatch", new NativeCatchManagerService());
            PackageSignatureVerifier.verifyPackageSignature((Context) contextObject, "com.lerist.fakelocation", "service_mock_bluetooth");
            if (!LHooker.initialized) {
                Log.e("InjectDex", "Init unfinished.");
                return null;
            }
            LHooker.suspendAll();
            Log.d("InjectDex", "Init finish.");
            return null;
        } catch (RuntimeException unused) {
            return null;
        } catch (Throwable th) {
            th.printStackTrace();
            Log.e("InjectDex", "Init error.");
            return null;
        }
    }

    public static Object[] initZygote(Object startupParam) {
        String libraryPath;
        String processLibraryPath;
        Log.d("InjectDex", "initZygote." + startupParam);
        HiddenApiBypass.bypassHiddenApiRestrictions();
        if (LHooker.isDeviceX86_64() || LHooker.isDeviceX86()) {
            libraryPath = "/data/fakeloc/liblhookerx.so";
            if (Build.VERSION.SDK_INT >= 23 && Process.is64Bit()) {
                processLibraryPath = "/data/fakeloc/liblhookerx64.so";
                LHooker.loadHookLibrary(processLibraryPath);
            }
            LHooker.loadHookLibrary(libraryPath);
        } else {
            libraryPath = "/data/fakeloc/liblhooker.so";
            if (Build.VERSION.SDK_INT >= 23 && Process.is64Bit()) {
                processLibraryPath = "/data/fakeloc/liblhooker64.so";
                LHooker.loadHookLibrary(processLibraryPath);
            }
            LHooker.loadHookLibrary(libraryPath);
        }
        AppProcessHook.hook(ClassLoader.getSystemClassLoader());
        Log.d("InjectDex", "InitZygote finish." + LHooker.initialized);
        return null;
    }

    public static Context getApplicationContext() {
        return applicationContext;
    }

    static void log(String message) {
        Log.d("InjectDex", message);
    }

    private static void initializeMainThread(Context context) {
        mainHandler = new Handler(context.getMainLooper());
        Iterator<InitializationCallback> iterator = initializationCallbacks.iterator();
        while (iterator.hasNext()) {
            try {
                iterator.next().onInitialized();
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }
    }
}
