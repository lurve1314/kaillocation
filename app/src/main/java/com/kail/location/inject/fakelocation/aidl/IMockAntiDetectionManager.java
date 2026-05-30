package com.lerist.inject.fakelocation.aidl;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import java.util.List;

public interface IMockAntiDetectionManager extends IInterface {

    abstract class Stub extends Binder implements IMockAntiDetectionManager {

        private static class Proxy implements IMockAntiDetectionManager {
            public static IMockAntiDetectionManager defaultImpl;

            private IBinder remoteBinder;

            Proxy(IBinder remoteBinder) {
                this.remoteBinder = remoteBinder;
            }

            @Override
            public IBinder asBinder() {
                return this.remoteBinder;
            }

            @Override
            public List<String> getScopedPackageRules() {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken("com.lerist.aidl.fakelocation.IMockAntiDetectionManager");
                    if (!this.remoteBinder.transact(14, data, reply, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().getScopedPackageRules();
                    }
                    reply.readException();
                    return reply.createStringArrayList();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public List<String> getTargetPackages() {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken("com.lerist.aidl.fakelocation.IMockAntiDetectionManager");
                    if (!this.remoteBinder.transact(12, data, reply, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().getTargetPackages();
                    }
                    reply.readException();
                    return reply.createStringArrayList();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public boolean isPackageFilterEnabled() {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken("com.lerist.aidl.fakelocation.IMockAntiDetectionManager");
                    if (!this.remoteBinder.transact(6, data, reply, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().isPackageFilterEnabled();
                    }
                    reply.readException();
                    return reply.readInt() != 0;
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public boolean isPackageVisibilityFilteringEnabled() {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken("com.lerist.aidl.fakelocation.IMockAntiDetectionManager");
                    if (!this.remoteBinder.transact(8, data, reply, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().isPackageVisibilityFilteringEnabled();
                    }
                    reply.readException();
                    return reply.readInt() != 0;
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public boolean isPackageManagerHookEnabled() {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken("com.lerist.aidl.fakelocation.IMockAntiDetectionManager");
                    if (!this.remoteBinder.transact(4, data, reply, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().isPackageManagerHookEnabled();
                    }
                    reply.readException();
                    return reply.readInt() != 0;
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public List<String> getDetectedPackages() {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken("com.lerist.aidl.fakelocation.IMockAntiDetectionManager");
                    if (!this.remoteBinder.transact(10, data, reply, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().getDetectedPackages();
                    }
                    reply.readException();
                    return reply.createStringArrayList();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, "com.lerist.aidl.fakelocation.IMockAntiDetectionManager");
        }

        public static IMockAntiDetectionManager asInterface(IBinder binder) {
            if (binder == null) {
                return null;
            }
            IInterface localInterface = binder.queryLocalInterface("com.lerist.aidl.fakelocation.IMockAntiDetectionManager");
            return (localInterface == null || !(localInterface instanceof IMockAntiDetectionManager)) ? new Proxy(binder) : (IMockAntiDetectionManager) localInterface;
        }

        public static IMockAntiDetectionManager getDefaultImpl() {
            return Proxy.defaultImpl;
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
            if (code == 1598968902) {
                reply.writeString("com.lerist.aidl.fakelocation.IMockAntiDetectionManager");
                return true;
            }
            switch (code) {
                case 1:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockAntiDetectionManager");
                    updateLicenseState(data.readString(), data.readString());
                    reply.writeNoException();
                    return true;
                case 2:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockAntiDetectionManager");
                    refreshPackageManagerHookEnabled();
                    reply.writeNoException();
                    return true;
                case 3:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockAntiDetectionManager");
                    disablePackageManagerHook();
                    reply.writeNoException();
                    return true;
                case 4:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockAntiDetectionManager");
                    boolean packageManagerHookEnabled = isPackageManagerHookEnabled();
                    reply.writeNoException();
                    reply.writeInt(packageManagerHookEnabled ? 1 : 0);
                    return true;
                case 5:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockAntiDetectionManager");
                    setPackageFilterEnabled(data.readInt() != 0);
                    reply.writeNoException();
                    return true;
                case 6:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockAntiDetectionManager");
                    boolean packageFilterEnabled = isPackageFilterEnabled();
                    reply.writeNoException();
                    reply.writeInt(packageFilterEnabled ? 1 : 0);
                    return true;
                case 7:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockAntiDetectionManager");
                    setPackageVisibilityFilterEnabled(data.readInt() != 0);
                    reply.writeNoException();
                    return true;
                case 8:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockAntiDetectionManager");
                    boolean packageVisibilityFilteringEnabled = isPackageVisibilityFilteringEnabled();
                    reply.writeNoException();
                    reply.writeInt(packageVisibilityFilteringEnabled ? 1 : 0);
                    return true;
                case 9:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockAntiDetectionManager");
                    setDetectedPackages(data.createStringArrayList());
                    reply.writeNoException();
                    return true;
                case 10:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockAntiDetectionManager");
                    List<String> detectedPackages = getDetectedPackages();
                    reply.writeNoException();
                    reply.writeStringList(detectedPackages);
                    return true;
                case 11:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockAntiDetectionManager");
                    setTargetPackages(data.createStringArrayList());
                    reply.writeNoException();
                    return true;
                case 12:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockAntiDetectionManager");
                    List<String> targetPackages = getTargetPackages();
                    reply.writeNoException();
                    reply.writeStringList(targetPackages);
                    return true;
                case 13:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockAntiDetectionManager");
                    setScopedPackageRules(data.createStringArrayList());
                    reply.writeNoException();
                    return true;
                case 14:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockAntiDetectionManager");
                    List<String> scopedPackageRules = getScopedPackageRules();
                    reply.writeNoException();
                    reply.writeStringList(scopedPackageRules);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }
    }

    List<String> getScopedPackageRules();

    void updateLicenseState(String licenseToken, String deviceId);

    void setScopedPackageRules(List<String> scopedPackageRules);

    void setPackageFilterEnabled(boolean enabled);

    List<String> getTargetPackages();

    void setTargetPackages(List<String> targetPackages);

    boolean isPackageFilterEnabled();

    boolean isPackageVisibilityFilteringEnabled();

    void disablePackageManagerHook();

    boolean isPackageManagerHookEnabled();

    List<String> getDetectedPackages();

    void refreshPackageManagerHookEnabled();

    void setPackageVisibilityFilterEnabled(boolean enabled);

    void setDetectedPackages(List<String> detectedPackages);
}
