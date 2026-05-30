package com.lerist.inject.fakelocation.service;

import com.lerist.lib.lhooker.LHooker;
import com.lerist.inject.fakelocation.aidl.INativeCatchManager;

public class NativeCatchManagerService extends INativeCatchManager.Stub {

    @Override
    public int getNativeCatchInitStatus() {
        return !LHooker.initialized ? -1 : 0;
    }

    @Override
    public boolean isNativeCatchEnabled() {
        boolean initialized = LHooker.initialized;
        return false;
    }

    @Override
    public int getNativeCatchHookStatus() {
        return !LHooker.initialized ? -1 : 0;
    }
}
