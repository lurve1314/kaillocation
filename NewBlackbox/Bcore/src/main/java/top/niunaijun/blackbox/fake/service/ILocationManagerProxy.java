package top.niunaijun.blackbox.fake.service;

import android.content.Context;
import android.location.LocationManager;
import android.os.IInterface;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

import black.android.location.BRILocationListener;
import black.android.location.BRILocationManagerStub;
import black.android.location.provider.BRProviderProperties;
import black.android.location.provider.ProviderProperties;
import black.android.os.BRServiceManager;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.entity.location.BLocation;
import top.niunaijun.blackbox.fake.frameworks.BLocationManager;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.MethodParameterUtils;


public class ILocationManagerProxy extends BinderInvocationStub {
    public static final String TAG = "ILocationManagerProxy";

    public ILocationManagerProxy() {
        super(BRServiceManager.get().getService(Context.LOCATION_SERVICE));
    }

    @Override
    protected Object getWho() {
        return BRILocationManagerStub.get().asInterface(BRServiceManager.get().getService(Context.LOCATION_SERVICE));
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        MethodParameterUtils.replaceFirstAppPkg(args);
        
        String pkg = BActivityThread.getAppPackageName();
        int uid = BActivityThread.getUserId();
        Log.d(TAG, "method=" + method.getName() + " pkg=" + pkg + " userId=" + uid);
        
        if (pkg != null && pkg.equals("com.google.android.gms")) {
            
            if (method.getName().equals("getLastLocation") || 
                method.getName().equals("getLastKnownLocation") ||
                method.getName().equals("requestLocationUpdates")) {
                Log.w(TAG, "Blocking location request from Google Play Services to prevent crash");
                return null;
            }
        }
        
        return super.invoke(proxy, method, args);
    }

    @ProxyMethod("registerGnssStatusCallback")
    public static class RegisterGnssStatusCallback extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            
            return true;
        }
    }

    @ProxyMethod("addGnssMeasurementsListener")
    public static class AddGnssMeasurementsListener extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (BLocationManager.isFakeLocationEnable()) {
                Log.d(TAG, "Blocking addGnssMeasurementsListener for " + BActivityThread.getAppPackageName());
                return false;
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("registerGnssNmeaCallback")
    public static class RegisterGnssNmeaCallback extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (BLocationManager.isFakeLocationEnable()) {
                Log.d(TAG, "Blocking registerGnssNmeaCallback for " + BActivityThread.getAppPackageName());
                return false;
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("unregisterLocationListener")
    public static class UnregisterLocationListener extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            IInterface listener = MethodParameterUtils.getFirstParam(args, IInterface.class);
            if (listener != null) {
                BLocationManager.get().removeUpdates(listener.asBinder());
            }
            return 0;
        }
    }

    @ProxyMethod("getLastLocation")
    public static class GetLastLocation extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (BLocationManager.isFakeLocationEnable()) {
                Log.d(TAG, "GetLastLocation returning fake location for " + BActivityThread.getAppPackageName());
                return BLocationManager.get().getLocation(BActivityThread.getUserId(), BActivityThread.getAppPackageName()).convert2SystemLocation();
            }
            Log.d(TAG, "GetLastLocation fake disabled, passing through");
            
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                if (e.getCause() instanceof SecurityException) {
                    Log.w(TAG, "Location permission denied, returning null for getLastLocation");
                    return null;
                }
                throw e;
            }
        }
    }

    @ProxyMethod("getLastKnownLocation")
    public static class GetLastKnownLocation extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (BLocationManager.isFakeLocationEnable()) {
                Log.d(TAG, "GetLastKnownLocation returning fake location for " + BActivityThread.getAppPackageName());
                return BLocationManager.get().getLocation(BActivityThread.getUserId(), BActivityThread.getAppPackageName()).convert2SystemLocation();
            }
            Log.d(TAG, "GetLastKnownLocation fake disabled, passing through");
            
            
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                if (e.getCause() instanceof SecurityException) {
                    Log.w(TAG, "Location permission denied, returning null for getLastKnownLocation");
                    return null;
                }
                throw e;
            }
        }
    }

    @ProxyMethod("registerLocationListener")
    public static class RegisterLocationListener extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            IInterface listener = MethodParameterUtils.getFirstParam(args, IInterface.class);
            if (listener != null) {
                Log.d(TAG, "RegisterLocationListener intercepted for " + BActivityThread.getAppPackageName());
                BLocationManager.get().requestLocationUpdates(listener.asBinder());
                return 0;
            }
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                if (e.getCause() instanceof SecurityException) {
                    Log.w(TAG, "Location permission denied for registerLocationListener");
                    return 0;
                }
                throw e;
            }
        }
    }

    @ProxyMethod("requestLocationUpdates")
    public static class RequestLocationUpdates extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (BLocationManager.isFakeLocationEnable()) {
                IInterface listener = MethodParameterUtils.getFirstParam(args, IInterface.class);
                if (listener != null) {
                    Log.d(TAG, "RequestLocationUpdates intercepted for " + BActivityThread.getAppPackageName());
                    BLocationManager.get().requestLocationUpdates(listener.asBinder());
                    return 0;
                }
                Log.w(TAG, "RequestLocationUpdates: no IInterface found in args (API mismatch?)");
            }
            
            
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                if (e.getCause() instanceof SecurityException) {
                    Log.w(TAG, "Location permission denied for requestLocationUpdates, returning 0");
                    return 0;
                }
                throw e;
            }
        }
    }

    @ProxyMethod("removeUpdates")
    public static class RemoveUpdates extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            IInterface listener = MethodParameterUtils.getFirstParam(args, IInterface.class);
            if (listener != null) {
                BLocationManager.get().removeUpdates(listener.asBinder());
                return 0;
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getProviderProperties")
    public static class GetProviderProperties extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            Object providerProperties = method.invoke(who, args);
            if (BLocationManager.isFakeLocationEnable()) {
                BRProviderProperties.get(providerProperties)._set_mHasNetworkRequirement(false);
                if (BLocationManager.get().getCell(BActivityThread.getUserId(), BActivityThread.getAppPackageName()) == null) {
                    BRProviderProperties.get(providerProperties)._set_mHasCellRequirement(false);
                }
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("removeGpsStatusListener")
    public static class RemoveGpsStatusListener extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            
            return 0;
        }
    }

    @ProxyMethod("getBestProvider")
    public static class GetBestProvider extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            if (BLocationManager.isFakeLocationEnable()) {
                return LocationManager.GPS_PROVIDER;
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("getAllProviders")
    public static class GetAllProviders extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return Arrays.asList(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER);
        }
    }

    @ProxyMethod("isProviderEnabledForUser")
    public static class isProviderEnabledForUser extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            String provider = (String) args[0];
            return Objects.equals(provider, LocationManager.GPS_PROVIDER);
        }
    }

    @ProxyMethod("setExtraLocationControllerPackageEnabled")
    public static class setExtraLocationControllerPackageEnabled extends MethodHook {

        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return 0;
        }
    }
}
