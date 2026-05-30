package com.lerist.inject.fakelocation.aidl;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;

public interface INativeCatchManager extends IInterface {

    abstract class Stub extends Binder implements INativeCatchManager {
        public Stub() {
            attachInterface(this, "com.lerist.aidl.fakelocation.INativeCatchManager");
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
            int status;
            if (code == 1) {
                data.enforceInterface("com.lerist.aidl.fakelocation.INativeCatchManager");
                status = getNativeCatchInitStatus();
            } else {
                if (code != 2) {
                    if (code != 3) {
                        if (code != 1598968902) {
                            return super.onTransact(code, data, reply, flags);
                        }
                        reply.writeString("com.lerist.aidl.fakelocation.INativeCatchManager");
                        return true;
                    }
                    data.enforceInterface("com.lerist.aidl.fakelocation.INativeCatchManager");
                    boolean nativeCatchEnabled = isNativeCatchEnabled();
                    reply.writeNoException();
                    reply.writeInt(nativeCatchEnabled ? 1 : 0);
                    return true;
                }
                data.enforceInterface("com.lerist.aidl.fakelocation.INativeCatchManager");
                status = getNativeCatchHookStatus();
            }
            reply.writeNoException();
            reply.writeInt(status);
            return true;
        }
    }

    int getNativeCatchInitStatus();

    boolean isNativeCatchEnabled();

    int getNativeCatchHookStatus();
}
