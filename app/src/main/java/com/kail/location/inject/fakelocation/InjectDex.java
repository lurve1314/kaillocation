package com.kail.location.inject.fakelocation;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Process;
import com.kail.location.inject.fakelocation.service.AntiDetectionManagerService;
import com.kail.location.inject.fakelocation.service.HideRootManagerService;
import com.kail.location.inject.fakelocation.service.MockLocationManagerService;
import com.kail.location.inject.fakelocation.service.MockWifiManagerService;
import com.kail.location.inject.fakelocation.service.NativeCatchManagerService;
import com.kail.location.inject.utils.HiddenApiBypass;
import com.kail.location.inject.utils.PackageSignatureVerifier;
import com.kail.location.inject.utils.ServiceManagerBridge;
import com.kail.location.lib.lhooker.LHooker;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import com.kail.location.inject.fakelocation.hook.phone.PhoneInterfaceManagerHook;
import com.kail.location.inject.fakelocation.hook.app.AppProcessHook;

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
                            LHooker.loadHookLibrary("/data/kail-loc/liblhooker64.so");
                        } else {
                            LHooker.loadHookLibrary("/data/kail-loc/liblhooker.so");
                        }
                    }
                    LHooker.loadHookLibrary("/data/kail-loc/liblhookerx.so");
                }
                LHooker.loadHookLibrary("/data/kail-loc/liblhookerx64.so");
            } else {
                String nativeLibraryAbi = new File("" + ((Context) contextObject).getPackageManager().getApplicationInfo(packageName.split(":")[0], 0).nativeLibraryDir).getName();
                log("App abi: " + nativeLibraryAbi);
                if (LHooker.isX86_64Abi(nativeLibraryAbi)) {
                    LHooker.loadHookLibrary("/data/kail-loc/liblhookerx64.so");
                } else if (LHooker.isX86Abi(nativeLibraryAbi)) {
                    LHooker.loadHookLibrary("/data/kail-loc/liblhookerx.so");
                } else {
                    if (LHooker.isArm64Abi(nativeLibraryAbi)) {
                        LHooker.loadHookLibrary("/data/kail-loc/liblhooker64.so");
                    }
                    LHooker.loadHookLibrary("/data/kail-loc/liblhooker.so");
                }
            }
            if (!LHooker.initialized) {
                com.kail.location.inject.utils.InjectLog.e("InjectDex", "hookApplication aborted: LHooker not initialized");
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
            com.kail.location.inject.utils.InjectLog.e("InjectDex", "hookApplication error", th);
            return null;
        }
    }

    public static Object[] init(Object contextObject) {
        applicationContext = (Context) contextObject;
        com.kail.location.inject.utils.InjectLog.i("InjectDex", "init: " + contextObject);
        try {
            initializeMainThread((Context) contextObject);
            HiddenApiBypass.bypassHiddenApiRestrictions();
            LHooker.loadHookLibrary(LHooker.isDeviceX86_64() ? "/data/kail-loc/liblhookerx64.so" : LHooker.isDeviceX86() ? "/data/kail-loc/liblhookerx.so" : LHooker.isDeviceArm64() ? "/data/kail-loc/liblhooker64.so" : "/data/kail-loc/liblhooker.so");
            PackageSignatureVerifier.verifyPackageSignature((Context) contextObject, "com.kail.location", "oem_manager");
            ServiceManagerBridge.addService(((Context) contextObject).getClassLoader(), "oem_location", new MockLocationManagerService());
            ServiceManagerBridge.addService(((Context) contextObject).getClassLoader(), "oem_wifi", new MockWifiManagerService());
            ServiceManagerBridge.addService(((Context) contextObject).getClassLoader(), "oem_security", new AntiDetectionManagerService());
            ServiceManagerBridge.addService(((Context) contextObject).getClassLoader(), "oem_integrity", new HideRootManagerService());
            ServiceManagerBridge.addService(((Context) contextObject).getClassLoader(), "oem_native", new NativeCatchManagerService());
            PackageSignatureVerifier.verifyPackageSignature((Context) contextObject, "com.kail.location", "oem_bluetooth");
            if (!LHooker.initialized) {
                com.kail.location.inject.utils.InjectLog.e("InjectDex", "init aborted: LHooker not initialized");
                return null;
            }
            LHooker.suspendAll();
            com.kail.location.inject.utils.InjectLog.i("InjectDex", "init finished, all services registered");
            return null;
        } catch (RuntimeException unused) {
            return null;
        } catch (Throwable th) {
            th.printStackTrace();
            com.kail.location.inject.utils.InjectLog.e("InjectDex", "init error", th);
            return null;
        }
    }

    public static Object[] initZygote(Object startupParam) {
        String libraryPath;
        String processLibraryPath;
        com.kail.location.inject.utils.InjectLog.i("InjectDex", "initZygote: " + startupParam);
        HiddenApiBypass.bypassHiddenApiRestrictions();
        if (LHooker.isDeviceX86_64() || LHooker.isDeviceX86()) {
            libraryPath = "/data/kail-loc/liblhookerx.so";
            if (Build.VERSION.SDK_INT >= 23 && Process.is64Bit()) {
                processLibraryPath = "/data/kail-loc/liblhookerx64.so";
                LHooker.loadHookLibrary(processLibraryPath);
            }
            LHooker.loadHookLibrary(libraryPath);
        } else {
            libraryPath = "/data/kail-loc/liblhooker.so";
            if (Build.VERSION.SDK_INT >= 23 && Process.is64Bit()) {
                processLibraryPath = "/data/kail-loc/liblhooker64.so";
                LHooker.loadHookLibrary(processLibraryPath);
            }
            LHooker.loadHookLibrary(libraryPath);
        }
        AppProcessHook.hook(ClassLoader.getSystemClassLoader());
        com.kail.location.inject.utils.InjectLog.i("InjectDex", "initZygote finished, initialized=" + LHooker.initialized);
        return null;
    }

    public static Context getApplicationContext() {
        return applicationContext;
    }

    static void log(String message) {
        com.kail.location.inject.utils.InjectLog.d("InjectDex", message);
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
