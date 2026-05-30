package com.lerist.inject.fakelocation.aidl;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import java.util.List;

public interface IHideRootManager extends IInterface {

    abstract class Stub extends Binder implements IHideRootManager {

        private static class Proxy implements IHideRootManager {
            public static IHideRootManager defaultImpl;

            private IBinder remoteBinder;

            Proxy(IBinder remoteBinder) {
                this.remoteBinder = remoteBinder;
            }

            @Override
            public IBinder asBinder() {
                return this.remoteBinder;
            }

            @Override
            public List<String> getHiddenProcesses() {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken("com.lerist.aidl.fakelocation.IHideRootManager");
                    if (!this.remoteBinder.transact(10, data, reply, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().getHiddenProcesses();
                    }
                    reply.readException();
                    return reply.createStringArrayList();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public boolean isHideAppListEnabled() {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken("com.lerist.aidl.fakelocation.IHideRootManager");
                    if (!this.remoteBinder.transact(6, data, reply, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().isHideAppListEnabled();
                    }
                    reply.readException();
                    return reply.readInt() != 0;
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public List<String> getHiddenPackages() {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken("com.lerist.aidl.fakelocation.IHideRootManager");
                    if (!this.remoteBinder.transact(8, data, reply, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().getHiddenPackages();
                    }
                    reply.readException();
                    return reply.createStringArrayList();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public void disableHideRoot() {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken("com.lerist.aidl.fakelocation.IHideRootManager");
                    if (this.remoteBinder.transact(3, data, reply, 0) || Stub.getDefaultImpl() == null) {
                        reply.readException();
                    } else {
                        Stub.getDefaultImpl().disableHideRoot();
                    }
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public boolean isHideRootEnabled() {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken("com.lerist.aidl.fakelocation.IHideRootManager");
                    if (!this.remoteBinder.transact(4, data, reply, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().isHideRootEnabled();
                    }
                    reply.readException();
                    return reply.readInt() != 0;
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, "com.lerist.aidl.fakelocation.IHideRootManager");
        }

        public static IHideRootManager asInterface(IBinder binder) {
            if (binder == null) {
                return null;
            }
            IInterface localInterface = binder.queryLocalInterface("com.lerist.aidl.fakelocation.IHideRootManager");
            return (localInterface == null || !(localInterface instanceof IHideRootManager)) ? new Proxy(binder) : (IHideRootManager) localInterface;
        }

        public static IHideRootManager getDefaultImpl() {
            return Proxy.defaultImpl;
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
            if (code == 1598968902) {
                reply.writeString("com.lerist.aidl.fakelocation.IHideRootManager");
                return true;
            }
            switch (code) {
                case 1:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IHideRootManager");
                    updateLicenseState(data.readString(), data.readString());
                    reply.writeNoException();
                    return true;
                case 2:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IHideRootManager");
                    refreshHideRootEnabled();
                    reply.writeNoException();
                    return true;
                case 3:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IHideRootManager");
                    disableHideRoot();
                    reply.writeNoException();
                    return true;
                case 4:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IHideRootManager");
                    boolean hideRootEnabled = isHideRootEnabled();
                    reply.writeNoException();
                    reply.writeInt(hideRootEnabled ? 1 : 0);
                    return true;
                case 5:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IHideRootManager");
                    setHideAppListEnabled(data.readInt() != 0);
                    reply.writeNoException();
                    return true;
                case 6:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IHideRootManager");
                    boolean hideAppListEnabled = isHideAppListEnabled();
                    reply.writeNoException();
                    reply.writeInt(hideAppListEnabled ? 1 : 0);
                    return true;
                case 7:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IHideRootManager");
                    setHiddenPackages(data.createStringArrayList());
                    reply.writeNoException();
                    return true;
                case 8:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IHideRootManager");
                    List<String> hiddenPackages = getHiddenPackages();
                    reply.writeNoException();
                    reply.writeStringList(hiddenPackages);
                    return true;
                case 9:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IHideRootManager");
                    setHiddenProcesses(data.createStringArrayList());
                    reply.writeNoException();
                    return true;
                case 10:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IHideRootManager");
                    List<String> hiddenProcesses = getHiddenProcesses();
                    reply.writeNoException();
                    reply.writeStringList(hiddenProcesses);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }
    }

    List<String> getHiddenProcesses();

    void updateLicenseState(String licenseToken, String deviceId);

    void setHiddenProcesses(List<String> processes);

    void setHideAppListEnabled(boolean enabled);

    boolean isHideAppListEnabled();

    void setHiddenPackages(List<String> packages);

    List<String> getHiddenPackages();

    void disableHideRoot();

    void refreshHideRootEnabled();

    boolean isHideRootEnabled();
}
