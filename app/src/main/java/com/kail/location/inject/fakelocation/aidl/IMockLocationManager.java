package com.lerist.inject.fakelocation.aidl;

import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.telephony.SubscriptionInfo;
import java.util.List;
import com.lerist.inject.fakelocation.model.CellTowerInfo;

public interface IMockLocationManager extends IInterface {

    abstract class Stub extends Binder implements IMockLocationManager {

        private static class Proxy implements IMockLocationManager {
            public static IMockLocationManager defaultImpl;

            private IBinder remoteBinder;

            Proxy(IBinder remoteBinder) {
                this.remoteBinder = remoteBinder;
            }

            @Override
            public IBinder asBinder() {
                return this.remoteBinder;
            }

            @Override
            public List<String> getSafeApps() {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken("com.lerist.aidl.fakelocation.IMockLocationManager");
                    if (!this.remoteBinder.transact(33, data, reply, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().getSafeApps();
                    }
                    reply.readException();
                    return reply.createStringArrayList();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public List<String> getAllowMockPackages() {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken("com.lerist.aidl.fakelocation.IMockLocationManager");
                    if (!this.remoteBinder.transact(10, data, reply, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().getAllowMockPackages();
                    }
                    reply.readException();
                    return reply.createStringArrayList();
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public boolean isMocking() {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken("com.lerist.aidl.fakelocation.IMockLocationManager");
                    if (!this.remoteBinder.transact(4, data, reply, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().isMocking();
                    }
                    reply.readException();
                    return reply.readInt() != 0;
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public Location getMockLocation() {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken("com.lerist.aidl.fakelocation.IMockLocationManager");
                    if (!this.remoteBinder.transact(6, data, reply, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().getMockLocation();
                    }
                    reply.readException();
                    return reply.readInt() != 0 ? (Location) Location.CREATOR.createFromParcel(reply) : null;
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public boolean isMockSubscriptionInfoEnabled() {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken("com.lerist.aidl.fakelocation.IMockLocationManager");
                    if (!this.remoteBinder.transact(31, data, reply, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().isMockSubscriptionInfoEnabled();
                    }
                    reply.readException();
                    return reply.readInt() != 0;
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public List<CellTowerInfo> getMockCells() {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken("com.lerist.aidl.fakelocation.IMockLocationManager");
                    if (!this.remoteBinder.transact(21, data, reply, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().getMockCells();
                    }
                    reply.readException();
                    return reply.createTypedArrayList(CellTowerInfo.CREATOR);
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }

            @Override
            public List<SubscriptionInfo> getMockSubscriptionInfo() {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                try {
                    data.writeInterfaceToken("com.lerist.aidl.fakelocation.IMockLocationManager");
                    if (!this.remoteBinder.transact(29, data, reply, 0) && Stub.getDefaultImpl() != null) {
                        return Stub.getDefaultImpl().getMockSubscriptionInfo();
                    }
                    reply.readException();
                    return reply.createTypedArrayList(SubscriptionInfo.CREATOR);
                } finally {
                    reply.recycle();
                    data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, "com.lerist.aidl.fakelocation.IMockLocationManager");
        }

        public static IMockLocationManager asInterface(IBinder binder) {
            if (binder == null) {
                return null;
            }
            IInterface localInterface = binder.queryLocalInterface("com.lerist.aidl.fakelocation.IMockLocationManager");
            return (localInterface == null || !(localInterface instanceof IMockLocationManager)) ? new Proxy(binder) : (IMockLocationManager) localInterface;
        }

        public static IMockLocationManager getDefaultImpl() {
            return Proxy.defaultImpl;
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
            if (code == 1598968902) {
                reply.writeString("com.lerist.aidl.fakelocation.IMockLocationManager");
                return true;
            }
            switch (code) {
                case 1:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockLocationManager");
                    updateLicenseState(data.readString(), data.readString());
                    reply.writeNoException();
                    return true;
                case 2:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockLocationManager");
                    startMockLocation();
                    reply.writeNoException();
                    return true;
                case 3:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockLocationManager");
                    stopMockLocation();
                    reply.writeNoException();
                    return true;
                case 4:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockLocationManager");
                    boolean mocking = isMocking();
                    reply.writeNoException();
                    reply.writeInt(mocking ? 1 : 0);
                    return true;
                case 5:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockLocationManager");
                    setMockLocation(data.readInt() != 0 ? (Location) Location.CREATOR.createFromParcel(data) : null);
                    reply.writeNoException();
                    return true;
                case 6:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockLocationManager");
                    Location mockLocation = getMockLocation();
                    reply.writeNoException();
                    if (mockLocation != null) {
                        reply.writeInt(1);
                        mockLocation.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                case 7:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockLocationManager");
                    setIntervalTimeout(data.readLong());
                    reply.writeNoException();
                    return true;
                case 8:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockLocationManager");
                    long intervalTimeout = getIntervalTimeout();
                    reply.writeNoException();
                    reply.writeLong(intervalTimeout);
                    return true;
                case 9:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockLocationManager");
                    setAllowMockPackages(data.createStringArrayList());
                    reply.writeNoException();
                    return true;
                case 10:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockLocationManager");
                    List<String> allowMockPackages = getAllowMockPackages();
                    reply.writeNoException();
                    reply.writeStringList(allowMockPackages);
                    return true;
                case 11:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockLocationManager");
                    startStepSensorMock();
                    reply.writeNoException();
                    return true;
                case 12:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockLocationManager");
                    stopStepSensorMock();
                    reply.writeNoException();
                    return true;
                case 13:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockLocationManager");
                    boolean stepSensorMocking = isStepSensorMocking();
                    reply.writeNoException();
                    reply.writeInt(stepSensorMocking ? 1 : 0);
                    return true;
                case 14:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockLocationManager");
                    setStepSpeed(data.readFloat());
                    reply.writeNoException();
                    return true;
                case 15:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockLocationManager");
                    float stepSpeed = getStepSpeed();
                    reply.writeNoException();
                    reply.writeFloat(stepSpeed);
                    return true;
                case 16:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockLocationManager");
                    setStepCountOffset(data.readLong());
                    reply.writeNoException();
                    return true;
                case 17:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockLocationManager");
                    long mockStepCount = getMockStepCount();
                    reply.writeNoException();
                    reply.writeLong(mockStepCount);
                    return true;
                case 18:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockLocationManager");
                    addOnMockListener(data.readStrongBinder());
                    reply.writeNoException();
                    return true;
                case 19:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockLocationManager");
                    removeOnMockListener(data.readStrongBinder());
                    reply.writeNoException();
                    return true;
                case 20:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockLocationManager");
                    setMockCells(data.createTypedArrayList(CellTowerInfo.CREATOR));
                    reply.writeNoException();
                    return true;
                case 21:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockLocationManager");
                    List<CellTowerInfo> mockCells = getMockCells();
                    reply.writeNoException();
                    reply.writeTypedList(mockCells);
                    return true;
                case 22:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockLocationManager");
                    boolean mockGpsStatus = isMockGpsStatus();
                    reply.writeNoException();
                    reply.writeInt(mockGpsStatus ? 1 : 0);
                    return true;
                case 23:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockLocationManager");
                    setMockGpsStatus(data.readInt() != 0);
                    reply.writeNoException();
                    return true;
                case 24:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockLocationManager");
                    setMockSourceTagEnabled(data.readInt() != 0);
                    reply.writeNoException();
                    return true;
                case 25:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockLocationManager");
                    boolean mockSourceTagEnabled = isMockSourceTagEnabled();
                    reply.writeNoException();
                    reply.writeInt(mockSourceTagEnabled ? 1 : 0);
                    return true;
                case 26:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockLocationManager");
                    boolean sensorFeatureEnabled = isSensorFeatureEnabled();
                    reply.writeNoException();
                    reply.writeInt(sensorFeatureEnabled ? 1 : 0);
                    return true;
                case 27:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockLocationManager");
                    setSensorFeatureEnabled(data.readInt() != 0);
                    reply.writeNoException();
                    return true;
                case 28:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockLocationManager");
                    setMockSubscriptionInfo(data.createTypedArrayList(SubscriptionInfo.CREATOR));
                    reply.writeNoException();
                    return true;
                case 29:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockLocationManager");
                    List<SubscriptionInfo> mockSubscriptionInfo = getMockSubscriptionInfo();
                    reply.writeNoException();
                    reply.writeTypedList(mockSubscriptionInfo);
                    return true;
                case 30:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockLocationManager");
                    setMockSubscriptionInfoEnabled(data.readInt() != 0);
                    reply.writeNoException();
                    return true;
                case 31:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockLocationManager");
                    boolean mockSubscriptionInfoEnabled = isMockSubscriptionInfoEnabled();
                    reply.writeNoException();
                    reply.writeInt(mockSubscriptionInfoEnabled ? 1 : 0);
                    return true;
                case 32:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockLocationManager");
                    setSafeApps(data.createStringArrayList());
                    reply.writeNoException();
                    return true;
                case 33:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockLocationManager");
                    List<String> safeApps = getSafeApps();
                    reply.writeNoException();
                    reply.writeStringList(safeApps);
                    return true;
                case 34:
                    data.enforceInterface("com.lerist.aidl.fakelocation.IMockLocationManager");
                    setBaseStepCount(data.readLong());
                    reply.writeNoException();
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }
    }

    List<String> getSafeApps();

    void updateLicenseState(String licenseToken, String deviceId);

    void setSafeApps(List<String> safeApps);

    void setMockSubscriptionInfoEnabled(boolean enabled);

    List<String> getAllowMockPackages();

    void setAllowMockPackages(List<String> packages);

    boolean isMocking();

    boolean isMockGpsStatus();

    Location getMockLocation();

    boolean isStepSensorMocking();

    void setMockCells(List<CellTowerInfo> cells);

    void removeOnMockListener(IBinder listenerBinder);

    void setSensorFeatureEnabled(boolean enabled);

    void setIntervalTimeout(long intervalMillis);

    void setMockLocation(Location location);

    void setMockSourceTagEnabled(boolean enabled);

    boolean isMockSubscriptionInfoEnabled();

    List<CellTowerInfo> getMockCells();

    void setStepCountOffset(long stepCount);

    void stopMockLocation();

    long getMockStepCount();

    void setMockSubscriptionInfo(List<SubscriptionInfo> subscriptionInfo);

    void setMockGpsStatus(boolean enabled);

    void addOnMockListener(IBinder listenerBinder);

    void startStepSensorMock();

    long getIntervalTimeout();

    boolean isSensorFeatureEnabled();

    void setBaseStepCount(long stepCount);

    void setStepSpeed(float speed);

    boolean isMockSourceTagEnabled();

    void stopStepSensorMock();

    List<SubscriptionInfo> getMockSubscriptionInfo();

    float getStepSpeed();

    void startMockLocation();
}
