package com.kail.location.service.Root

import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Message
import android.os.Process
import android.os.SystemClock
import android.provider.Settings
import androidx.preference.PreferenceManager
import com.kail.location.R
import com.kail.location.geo.GeoPredict
import com.kail.location.inject.fakelocation.aidl.IMockLocationManager
import com.kail.location.inject.utils.ServiceManagerBridge
import com.kail.location.root.NativeSensorHook
import com.kail.location.service.Developer.MockLocationProvider
import com.kail.location.utils.GoUtils
import com.kail.location.utils.KailLog
import com.kail.location.utils.MapUtils
import com.kail.location.utils.ShellUtils
import com.kail.location.utils.service.RouteEngine
import com.kail.location.utils.service.ServiceConstants
import com.kail.location.utils.service.ServiceNotificationHelper
import com.kail.location.viewmodels.JoystickViewModel
import com.kail.location.views.joystick.JoystickWindowManager
import com.kail.location.views.locationpicker.LocationPickerActivity

/**
 * Foreground service for the "root" run mode.
 *
 * Mocks location safely without ptrace-injecting system_server. Concretely:
 *
 *   1. [RootDeployer.ensureBaseline] stages every FakeLocation loader/hook
 *      .so (libfakeloc_init.so / libfakeloc_initzygote.so /
 *      libfakeloc_apphook.so / liblhooker.so / libStepSensor.so /
 *      libantidetect.so) into /data/kail-loc/, drops the kail_inject ptrace
 *      injector + libkail_native_hook.so into /data/local/kail-lib/, and
 *      copies the host APK to /data/kail-loc/libfakeloc.so as the dex
 *      payload. The full FakeLocation toolchain is therefore present on the
 *      device for an operator who wants to bootstrap it manually via
 *      [RootDeployer.bootstrapInjection], but the service does NOT run that
 *      step automatically — ptrace-injecting a production system_server
 *      regularly deadlocks the dynamic-linker lock and freezes the phone.
 *   2. The service grants the host package the AppOps `mock_location`
 *      permission via `appops set` so the standard Android test-provider
 *      mechanism works without the user toggling Developer Settings.
 *   3. Location updates are pushed via [MockLocationProvider] (the same code
 *      Developer mode uses) which calls
 *      `LocationManager.setTestProviderLocation` for both GPS and NETWORK
 *      providers. This is exactly the mechanism Developer mode would use,
 *      but root mode handles the AppOps grant for you.
 *   4. Sensor/step-cadence simulation runs through the in-app
 *      [NativeSensorHook] binding to libkail_native_hook.so. The full hook
 *      on system_server still requires the FakeLocation injection chain
 *      above to be online, so step mocking degrades gracefully when only
 *      ensureBaseline ran.
 *
 * The route engine, joystick, foreground notification, and control-action
 * surface mirror [com.kail.location.service.Xposed.ServiceGoXposed] so the
 * existing UI plugs in unchanged.
 */
class ServiceGoRoot : Service() {

    private var mCurLat = ServiceConstants.DEFAULT_LAT
    private var mCurLng = ServiceConstants.DEFAULT_LNG
    private var mCurAlt = ServiceConstants.DEFAULT_ALT
    private var mCurBea = ServiceConstants.DEFAULT_BEA
    private var mSpeed = 1.2

    private lateinit var mLocManager: LocationManager
    private lateinit var mLocHandlerThread: HandlerThread
    private lateinit var mLocHandler: Handler
    private var isStop = false

    private lateinit var mJoystickManager: JoystickWindowManager
    private lateinit var mJoystickViewModel: JoystickViewModel

    private val mBinder = ServiceGoRootBinder()
    private val mRouteEngine = RouteEngine()
    private val mNotificationHelper by lazy {
        ServiceNotificationHelper(
            service = this,
            channelId = "SERVICE_GO_ROOT_NOTE",
            channelName = "SERVICE_GO_ROOT_NOTE",
            noteId = SERVICE_GO_NOTE_ID,
            onShowJoystick = { mJoystickManager.show() },
            onHideJoystick = { mJoystickManager.hide() }
        )
    }

    private var locationLoopStarted: Boolean = false
    private var speedFluctuation: Boolean = false
    private var stepEnabled: Boolean = false
    private var stepCadence: Float = 120f
    private var stepMode: Int = 0
    private var stepScheme: Int = 0

    private var nativeHookReady: Boolean = false
    private var nativeHookAttempted: Boolean = false

    /** Drives Android's standard test-provider mechanism. Same code Developer mode uses. */
    private val mMockLocationProvider by lazy { MockLocationProvider(this, mLocManager) }

    /**
     * Cached binder into the FakeLocation injection layer. Resolved after
     * RootDeployer.ensureBaseline runs kail_inject on system_server. When
     * non-null, location updates go through it; otherwise the service falls
     * back to [mMockLocationProvider].
     */
    private var mockLocService: IMockLocationManager? = null

    /**
     * Set true only when the start intent flagged WIFI_ONLY/CELL_ONLY — those
     * modes intentionally do not run the location loop.
     */
    private var modeWifiOnly: Boolean = false
    private var modeCellOnly: Boolean = false

    companion object {
        const val DEFAULT_LAT = ServiceConstants.DEFAULT_LAT
        const val DEFAULT_LNG = ServiceConstants.DEFAULT_LNG
        const val DEFAULT_ALT = ServiceConstants.DEFAULT_ALT
        const val DEFAULT_BEA = ServiceConstants.DEFAULT_BEA

        private const val TAG = "ServiceGoRoot"
        private const val HANDLER_MSG_ID = 0
        private const val DEFAULT_LOCATION_UPDATE_INTERVAL_MS = 200L
        private const val SERVICE_GO_HANDLER_NAME = "ServiceGoRootLocation"
        private const val SERVICE_GO_NOTE_ID = 3

        const val SERVICE_GO_NOTE_ACTION_JOYSTICK_SHOW = ServiceNotificationHelper.ACTION_JOYSTICK_SHOW
        const val SERVICE_GO_NOTE_ACTION_JOYSTICK_HIDE = ServiceNotificationHelper.ACTION_JOYSTICK_HIDE

        const val EXTRA_ROUTE_POINTS = ServiceConstants.EXTRA_ROUTE_POINTS
        const val EXTRA_ROUTE_LOOP = ServiceConstants.EXTRA_ROUTE_LOOP
        const val EXTRA_JOYSTICK_ENABLED = ServiceConstants.EXTRA_JOYSTICK_ENABLED
        const val EXTRA_ROUTE_SPEED = ServiceConstants.EXTRA_ROUTE_SPEED
        const val EXTRA_COORD_TYPE = ServiceConstants.EXTRA_COORD_TYPE
        const val EXTRA_CONTROL_ACTION = ServiceConstants.EXTRA_CONTROL_ACTION
        const val EXTRA_SPEED_FLUCTUATION = ServiceConstants.EXTRA_SPEED_FLUCTUATION
        const val EXTRA_SEEK_RATIO = ServiceConstants.EXTRA_SEEK_RATIO

        const val EXTRA_STEP_ENABLED = "EXTRA_STEP_ENABLED"
        const val EXTRA_STEP_FREQ = "EXTRA_STEP_FREQ"
        const val EXTRA_STEP_MODE = "EXTRA_STEP_MODE"
        const val EXTRA_STEP_SCHEME = "EXTRA_STEP_SCHEME"
        const val EXTRA_WIFI_ONLY = "EXTRA_WIFI_ONLY"
        const val EXTRA_CELL_ONLY = "EXTRA_CELL_ONLY"

        const val CONTROL_PAUSE = ServiceConstants.CONTROL_PAUSE
        const val CONTROL_RESUME = ServiceConstants.CONTROL_RESUME
        const val CONTROL_STOP = ServiceConstants.CONTROL_STOP
        const val CONTROL_SEEK = ServiceConstants.CONTROL_SEEK
        const val CONTROL_SET_SPEED = ServiceConstants.CONTROL_SET_SPEED
        const val CONTROL_SET_SPEED_FLUCTUATION = ServiceConstants.CONTROL_SET_SPEED_FLUCTUATION
        const val CONTROL_SET_STEP = "set_step"

        const val COORD_WGS84 = ServiceConstants.COORD_WGS84
        const val COORD_BD09 = ServiceConstants.COORD_BD09
        const val COORD_GCJ02 = ServiceConstants.COORD_GCJ02

        const val ACTION_STATUS_CHANGED = ServiceConstants.ACTION_STATUS_CHANGED
        const val EXTRA_IS_SIMULATING = ServiceConstants.EXTRA_IS_SIMULATING
        const val EXTRA_IS_PAUSED = ServiceConstants.EXTRA_IS_PAUSED
    }

    override fun onBind(intent: Intent): IBinder = mBinder

    override fun onCreate() {
        super.onCreate()
        KailLog.i(this, TAG, "onCreate started")
        runCatching { mNotificationHelper.initAndStartForeground() }
            .onFailure { KailLog.e(this, TAG, "initNotification: ${it.message}") }
        runCatching { mLocManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager }
            .onFailure { KailLog.e(this, TAG, "LocationManager init: ${it.message}") }
        runCatching { initGoLocation() }
            .onFailure { KailLog.e(this, TAG, "initGoLocation: ${it.message}") }
        runCatching {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val joystickEnabledPref = prefs.getBoolean("setting_joystick_enabled", false)
            initJoyStick()
            if (joystickEnabledPref) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)) {
                    mJoystickManager.show()
                }
            } else {
                mJoystickManager.hide()
            }
        }.onFailure {
            KailLog.e(this, TAG, "init joystick: ${it.message}")
            GoUtils.DisplayToast(applicationContext, getString(R.string.service_overlay_failed, it.message))
        }
        broadcastStatus()
        KailLog.i(this, TAG, "onCreate finished")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val ctrl = intent.getStringExtra(EXTRA_CONTROL_ACTION)
            if (!ctrl.isNullOrBlank()) {
                handleControlAction(ctrl, intent)
                return super.onStartCommand(intent, flags, startId)
            }
            speedFluctuation = intent.getBooleanExtra(EXTRA_SPEED_FLUCTUATION, false)
        }

        mNotificationHelper.startForegroundIfReady()

        if (intent != null) {
            modeWifiOnly = intent.getBooleanExtra(EXTRA_WIFI_ONLY, false)
            modeCellOnly = intent.getBooleanExtra(EXTRA_CELL_ONLY, false)

            val coordType = intent.getStringExtra(EXTRA_COORD_TYPE) ?: COORD_BD09
            mCurLng = intent.getDoubleExtra(LocationPickerActivity.LNG_MSG_ID, DEFAULT_LNG)
            mCurLat = intent.getDoubleExtra(LocationPickerActivity.LAT_MSG_ID, DEFAULT_LAT)
            runCatching {
                when (coordType) {
                    COORD_WGS84 -> { /* keep */ }
                    COORD_GCJ02 -> {
                        val wgs = MapUtils.gcj02towgs84(mCurLng, mCurLat)
                        mCurLng = wgs[0]; mCurLat = wgs[1]
                    }
                    else -> {
                        val wgs = MapUtils.bd2wgs(mCurLng, mCurLat)
                        mCurLng = wgs[0]; mCurLat = wgs[1]
                    }
                }
            }
            mCurAlt = intent.getDoubleExtra(LocationPickerActivity.ALT_MSG_ID, DEFAULT_ALT)
            val joystickEnabled = intent.getBooleanExtra(EXTRA_JOYSTICK_ENABLED, false)
            mSpeed = intent.getFloatExtra(EXTRA_ROUTE_SPEED, mSpeed.toFloat()).toDouble() / 3.6

            val routeArray = intent.getDoubleArrayExtra(EXTRA_ROUTE_POINTS)
            if (routeArray != null && routeArray.size >= 2) {
                mRouteEngine.setupFromArray(routeArray, coordType)
                mRouteEngine.setLoop(intent.getBooleanExtra(EXTRA_ROUTE_LOOP, false))
            }

            stepEnabled = intent.getBooleanExtra(EXTRA_STEP_ENABLED, stepEnabled)
            stepCadence = intent.getFloatExtra(EXTRA_STEP_FREQ, stepCadence)
            stepMode = intent.getIntExtra(EXTRA_STEP_MODE, stepMode)
            stepScheme = intent.getIntExtra(EXTRA_STEP_SCHEME, stepScheme)

            KailLog.i(this, TAG, "onStartCommand lat=$mCurLat lng=$mCurLng wifiOnly=$modeWifiOnly cellOnly=$modeCellOnly step=$stepEnabled spm=$stepCadence")

            // Bring up the in-app sensor hook + the FakeLocation injection
            // chain. The injection step shells out to su + kail_inject, which
            // can take a couple of seconds, so run it off the main thread.
            ensureNativeHookOnce()
            applyStepSimulation()
            Thread({ startMockLocationOnInjection() }, "ServiceGoRootBootstrap").start()

            if (!modeWifiOnly && !modeCellOnly) {
                startLocationLoop()
            }

            runCatching {
                mJoystickViewModel.setCurrentPosition(mCurLng, mCurLat, mCurAlt)
                if (joystickEnabled) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)) {
                        if (mRouteEngine.isActive) mJoystickManager.showRouteControl(mSpeed * 3.6)
                        else mJoystickManager.show()
                    } else {
                        GoUtils.DisplayToast(applicationContext, getString(R.string.service_grant_overlay))
                    }
                } else {
                    mJoystickManager.hide()
                }
            }.onFailure { KailLog.e(this, TAG, "joystick show: ${it.message}") }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        KailLog.i(this, TAG, "onDestroy started")
        runCatching {
            broadcastStatusStopped()
            isStop = true
            locationLoopStarted = false
            if (this::mLocHandler.isInitialized) mLocHandler.removeCallbacksAndMessages(null)
            if (this::mLocHandlerThread.isInitialized) mLocHandlerThread.quitSafely()
            if (this::mJoystickManager.isInitialized) mJoystickManager.destroy()

            // Tell the injected layer to stop, if present.
            stopMockLocationOnInjection()

            // Tell the in-app native hook to wind down.
            if (nativeHookReady) {
                runCatching { NativeSensorHook.nativeSetMocking(0) }
                runCatching { NativeSensorHook.nativeSetStepSimEnabled(false) }
                runCatching { NativeSensorHook.nativeSetRouteSimulation(false, 120f, 0) }
                runCatching { NativeSensorHook.nativeReset() }
            }

            mNotificationHelper.stopForeground()
        }.onFailure { KailLog.e(this, TAG, "onDestroy: ${it.message}") }
        super.onDestroy()
        KailLog.i(this, TAG, "onDestroy finished")
    }

    // ------------------------------------------------------------------
    // Control intent dispatcher
    // ------------------------------------------------------------------

    private fun handleControlAction(ctrl: String, intent: Intent) {
        when (ctrl) {
            CONTROL_PAUSE -> runCatching {
                isStop = true
                if (this::mJoystickManager.isInitialized) mJoystickManager.setRoutePauseState(true)
                broadcastStatus()
                KailLog.log(this, TAG, "Paused (isStop=true)", isHighFrequency = false)
            }.onFailure { KailLog.e(this, TAG, "Pause: ${it.message}") }

            CONTROL_RESUME -> runCatching {
                isStop = false
                if (this::mJoystickManager.isInitialized) mJoystickManager.setRoutePauseState(false)
                if (locationLoopStarted && this::mLocHandler.isInitialized && !mLocHandler.hasMessages(HANDLER_MSG_ID)) {
                    mLocHandler.sendEmptyMessage(HANDLER_MSG_ID)
                }
                applyStepSimulation()
                broadcastStatus()
                KailLog.log(this, TAG, "Resumed (isStop=false)", isHighFrequency = false)
            }.onFailure { KailLog.e(this, TAG, "Resume: ${it.message}") }

            CONTROL_STOP -> runCatching {
                stopSelf()
                broadcastStatus()
            }.onFailure { KailLog.e(this, TAG, "stop: ${it.message}") }

            CONTROL_SEEK -> {
                val ratio = intent.getFloatExtra(EXTRA_SEEK_RATIO, 0f).coerceIn(0f, 1f)
                mRouteEngine.seekToRatio(ratio)
                mCurLng = mRouteEngine.currentLng
                mCurLat = mRouteEngine.currentLat
                mCurBea = mRouteEngine.currentBea
                updateJoystickStatus()
            }

            CONTROL_SET_SPEED -> {
                val kmh = intent.getFloatExtra(EXTRA_ROUTE_SPEED, (mSpeed * 3.6).toFloat())
                mSpeed = kmh.toDouble() / 3.6
            }

            CONTROL_SET_SPEED_FLUCTUATION -> {
                speedFluctuation = intent.getBooleanExtra(EXTRA_SPEED_FLUCTUATION, speedFluctuation)
            }

            CONTROL_SET_STEP -> {
                stepEnabled = intent.getBooleanExtra(EXTRA_STEP_ENABLED, stepEnabled)
                stepCadence = intent.getFloatExtra(EXTRA_STEP_FREQ, stepCadence)
                stepMode = intent.getIntExtra(EXTRA_STEP_MODE, stepMode)
                stepScheme = intent.getIntExtra(EXTRA_STEP_SCHEME, stepScheme)
                ensureNativeHookOnce()
                applyStepSimulation()
            }
        }
    }

    // ------------------------------------------------------------------
    // Native (in-app) sensor hook bridge
    // ------------------------------------------------------------------

    /**
     * One-shot best-effort initialization of the in-app NativeSensorHook
     * bindings against [RootDeployer.NATIVE_HOOK_SO].
     *
     * IMPORTANT — actual sensor hook installation is a no-op when run from
     * the controller app process. The `Dobby` hook installer in
     * native_hook/hook.cpp scans `/proc/self/maps` for libsensor.so /
     * libsensorservice.so and only patches them when they are mapped in the
     * caller. Those libraries live in system_server (and individual apps
     * that consume sensors), not in com.kail.location.
     *
     * For the hook to fire, the SO needs to be loaded into the target
     * process by an external loader — the Xposed companion already does
     * this from inside system_server (see [com.kail.locationxposed]); the
     * FakeLocation injection chain (libfakeloc_init.so / libStepSensor.so)
     * would do it post-injection but we never auto-run kail_inject because
     * it freezes system_server on production ROMs.
     *
     * This method therefore initializes the JNI side so step parameters can
     * be plumbed in, but logs a clear notice that no hook is active from
     * this process. The bool it returns is "JNI bindings loaded", not "hook
     * installed". When the SO is loaded in another process via Xposed/
     * Zygisk, that process picks up its own copy and the hook does install.
     */
    private fun ensureNativeHookOnce(): Boolean {
        if (nativeHookReady) return true
        if (nativeHookAttempted) return false
        nativeHookAttempted = true

        if (!ShellUtils.hasRoot()) {
            KailLog.w(this, TAG, "no root; skipping native hook deploy")
            return false
        }

        runCatching { RootDeployer.deployNativeHookLib(this) }
            .onFailure { KailLog.e(this, TAG, "deployNativeHookLib: ${it.message}") }

        // Probe sensor offsets from libsensor.so / libsensorservice.so on
        // disk so a future loader can pick them up via the JNI setters.
        val (writeOffset, convertOffset) = getOffsetsFromSystem()

        val ok = runCatching {
            NativeSensorHook.nativeSetWriteOffset(parseHexOffset(writeOffset))
            NativeSensorHook.nativeSetConvertOffset(parseHexOffset(convertOffset))
            NativeSensorHook.nativeInitHook()
            true
        }.getOrElse {
            KailLog.e(this, TAG, "NativeSensorHook init: ${it.message}")
            false
        }

        nativeHookReady = ok
        if (ok) {
            KailLog.i(
                this,
                TAG,
                "NativeSensorHook JNI initialized in app process; sensor hook on system_server " +
                    "is NOT installed from here — needs Xposed/Zygisk loader. offsets=$writeOffset/$convertOffset"
            )
        } else {
            KailLog.w(this, TAG, "NativeSensorHook JNI init failed; offsets=$writeOffset/$convertOffset")
        }
        return ok
    }

    private fun applyStepSimulation() {
        if (!nativeHookReady) return
        runCatching {
            NativeSensorHook.nativeSetGaitParams(stepCadence, stepMode, stepScheme, stepEnabled)
            NativeSensorHook.nativeSetStepSimEnabled(stepEnabled)
            if (stepEnabled) {
                NativeSensorHook.nativeSetRouteSimulation(true, stepCadence, stepMode)
                NativeSensorHook.nativeSetMocking(1)
            } else {
                NativeSensorHook.nativeSetRouteSimulation(false, stepCadence, stepMode)
                NativeSensorHook.nativeSetMocking(0)
            }
        }.onFailure { KailLog.e(this, TAG, "applyStepSimulation: ${it.message}") }
    }

    // ------------------------------------------------------------------
    // Mock-location bridge
    //
    // Two backends are tried in order:
    //  1. The FakeLocation injection layer (preferred) — only available
    //     after RootDeployer.ensureBaseline has run kail_inject on
    //     system_server and the binder service "service_mock_location" is
    //     registered.  This is the original FakeLocation hook path.
    //  2. Standard Android test-provider via [MockLocationProvider] — same
    //     code Developer mode uses.  Always works once the AppOps grant
    //     lands; harmless if the inject path also runs.
    // ------------------------------------------------------------------

    private fun resolveMockLocService(): IMockLocationManager? {
        mockLocService?.let { return it }
        val binder = runCatching {
            ServiceManagerBridge.getService(ClassLoader.getSystemClassLoader(), "service_mock_location")
        }.getOrNull() ?: return null
        return runCatching { IMockLocationManager.Stub.asInterface(binder) }.getOrNull()?.also {
            mockLocService = it
            KailLog.i(this, TAG, "FakeLocation mock-location binder online")
        }
    }

    private fun startMockLocationOnInjection() {
        // Stage the FakeLocation toolchain on disk, run kail_inject (with the
        // 5s watchdog), and grant mock_location AppOps.  RootDeployer is
        // idempotent and never re-runs the inject if the binder is already
        // registered.
        runCatching { RootDeployer.ensureBaseline(this) }
            .onFailure { KailLog.e(this, TAG, "RootDeployer.ensureBaseline: ${it.message}") }

        // Prefer the FakeLocation binder if the inject brought it up.
        val svc = resolveMockLocService()
        if (svc != null) {
            runCatching {
                svc.startMockLocation()
                svc.setIntervalTimeout(currentLocationUpdateIntervalMs())
                pushLocationToInjection()
                KailLog.i(this, TAG, "FakeLocation mock-location active lat=$mCurLat lng=$mCurLng")
            }.onFailure { KailLog.e(this, TAG, "startMockLocation (binder): ${it.message}") }
            return
        }

        // Fallback: register GPS + NETWORK test providers and push an initial fix.
        runCatching {
            mMockLocationProvider.ensureProviders()
            pushLocationToInjection()
            KailLog.i(this, TAG, "Test-provider mock-location active lat=$mCurLat lng=$mCurLng")
        }.onFailure { KailLog.e(this, TAG, "ensureProviders: ${it.message}") }
    }

    private fun stopMockLocationOnInjection() {
        runCatching { mockLocService?.stopMockLocation() }
        fakelocStartCalled = false
        runCatching { mMockLocationProvider.cleanup() }
            .onFailure { KailLog.e(this, TAG, "cleanup providers: ${it.message}") }
    }

    private var fakelocStartCalled: Boolean = false

    private fun pushLocationToInjection() {
        // Path 1: FakeLocation binder.
        // Re-resolve every push so that updates start flowing through the
        // FakeLocation injection layer as soon as kail_inject finishes,
        // even if the location loop began before the binder was online.
        val svc = mockLocService ?: resolveMockLocService()
        if (svc != null) {
            // First time we got the binder, tell it to start mocking.
            if (!fakelocStartCalled) {
                fakelocStartCalled = true
                runCatching {
                    svc.startMockLocation()
                    svc.setIntervalTimeout(currentLocationUpdateIntervalMs())
                    KailLog.i(this, TAG, "FakeLocation startMockLocation invoked")
                }.onFailure { KailLog.e(this, TAG, "startMockLocation (binder): ${it.message}") }
            }
            runCatching {
                val loc = Location(LocationManager.GPS_PROVIDER).apply {
                    latitude = mCurLat
                    longitude = mCurLng
                    altitude = mCurAlt
                    bearing = mCurBea
                    speed = mSpeed.toFloat()
                    accuracy = 1.0f
                    time = System.currentTimeMillis()
                    elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                    extras = Bundle().apply { putString("from", "rocker") }
                }
                svc.setMockLocation(loc)
            }.onFailure { KailLog.e(this, TAG, "setMockLocation (binder): ${it.message}") }
            return
        }

        // Path 2: test-provider fallback.
        runCatching {
            mMockLocationProvider.setLocation(mCurLat, mCurLng, mCurAlt, mCurBea, mSpeed, isStop)
        }.onFailure { KailLog.e(this, TAG, "setLocation (provider): ${it.message}") }
    }

    // ------------------------------------------------------------------
    // Location loop / status broadcasts
    // ------------------------------------------------------------------

    private fun broadcastStatus() {
        val intent = Intent(ACTION_STATUS_CHANGED).apply {
            putExtra(EXTRA_IS_SIMULATING, locationLoopStarted && !isStop)
            putExtra(EXTRA_IS_PAUSED, isStop)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    private fun broadcastStatusStopped() {
        val intent = Intent(ACTION_STATUS_CHANGED).apply {
            putExtra(EXTRA_IS_SIMULATING, false)
            putExtra(EXTRA_IS_PAUSED, false)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    private fun initGoLocation() {
        mLocHandlerThread = HandlerThread(SERVICE_GO_HANDLER_NAME, Process.THREAD_PRIORITY_BACKGROUND)
        mLocHandlerThread.start()
        mLocHandler = object : Handler(mLocHandlerThread.looper) {
            override fun handleMessage(msg: Message) {
                try {
                    if (!isStop) {
                        if (mRouteEngine.isActive) {
                            val speedForStep = if (speedFluctuation) {
                                GeoPredict.randomInRangeWithMean(mSpeed * 0.5, mSpeed * 1.5, mSpeed)
                            } else {
                                mSpeed
                            }
                            val intervalMs = currentLocationUpdateIntervalMs()
                            mRouteEngine.advance(speedForStep * (intervalMs / 1000.0))
                            mCurLng = mRouteEngine.currentLng
                            mCurLat = mRouteEngine.currentLat
                            mCurBea = mRouteEngine.currentBea
                            updateJoystickStatus()
                        }
                        pushLocationToInjection()
                    }
                    if (!isStop) {
                        sendEmptyMessageDelayed(HANDLER_MSG_ID, currentLocationUpdateIntervalMs())
                    }
                } catch (e: InterruptedException) {
                    KailLog.e(this@ServiceGoRoot, TAG, "loop interrupted: ${e.message}")
                    Thread.currentThread().interrupt()
                } catch (e: Exception) {
                    KailLog.e(this@ServiceGoRoot, TAG, "loop: ${e.message}")
                    if (!isStop) sendEmptyMessageDelayed(HANDLER_MSG_ID, currentLocationUpdateIntervalMs())
                }
            }
        }
    }

    private fun currentLocationUpdateIntervalMs(): Long {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        return (prefs.getString("setting_report_interval", DEFAULT_LOCATION_UPDATE_INTERVAL_MS.toString())
            ?.toLongOrNull() ?: DEFAULT_LOCATION_UPDATE_INTERVAL_MS).coerceAtLeast(0L)
    }

    private fun startLocationLoop() {
        if (!this::mLocHandler.isInitialized) return
        isStop = false
        if (locationLoopStarted) return
        locationLoopStarted = true
        mLocHandler.sendEmptyMessage(HANDLER_MSG_ID)
        broadcastStatus()
    }

    private fun updateJoystickStatus() {
        if (this::mJoystickManager.isInitialized && mRouteEngine.isActive) {
            val status = mRouteEngine.buildStatusString()
            if (status != null) {
                mJoystickManager.updateRouteStatus(mRouteEngine.progressRatio, status.first, status.second)
            }
        }
    }

    // ------------------------------------------------------------------
    // Joystick wiring
    // ------------------------------------------------------------------

    private fun initJoyStick() {
        mJoystickViewModel = JoystickViewModel(application)
        mJoystickManager = JoystickWindowManager(this, mJoystickViewModel, object : JoystickViewModel.ActionListener {
            override fun onMoveInfo(speed: Double, disLng: Double, disLat: Double, angle: Double) {
                mSpeed = speed
                val next = GeoPredict.nextByDisplacementKm(mCurLng, mCurLat, disLng, disLat)
                mCurLng = next.first
                mCurLat = next.second
                mCurBea = angle.toFloat()
            }

            override fun onPositionInfo(lng: Double, lat: Double, alt: Double) {
                mCurLng = lng
                mCurLat = lat
                mCurAlt = alt
            }

            override fun onRouteControl(action: String) {
                val intent = Intent(this@ServiceGoRoot, ServiceGoRoot::class.java)
                intent.putExtra(EXTRA_CONTROL_ACTION, action)
                startService(intent)
            }

            override fun onRouteSeek(progress: Float) {
                val intent = Intent(this@ServiceGoRoot, ServiceGoRoot::class.java)
                intent.putExtra(EXTRA_CONTROL_ACTION, CONTROL_SEEK)
                intent.putExtra(EXTRA_SEEK_RATIO, progress)
                startService(intent)
            }

            override fun onRouteSpeedChange(speed: Double) {
                mSpeed = speed / 3.6
            }
        })
    }

    // ------------------------------------------------------------------
    // Sensor offset probing (mirrors ServiceGoXposed)
    // ------------------------------------------------------------------

    private fun runSuCommand(cmd: String): String =
        runCatching {
            val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            val out = proc.inputStream.bufferedReader().readText().trim()
            proc.waitFor()
            out
        }.getOrDefault("")

    private fun getOffsetsFromSystem(): Pair<String, String> {
        val candidates = listOf("toybox readelf", "readelf", "/system/bin/toybox readelf")
        for (cmd in candidates) {
            try {
                val test = runSuCommand("$cmd 2>&1")
                if (test.contains("not found") || (test.isEmpty() && !cmd.startsWith("/"))) continue

                val sensorOut = runSuCommand("$cmd -Ws /system/lib64/libsensor.so 2>/dev/null | grep _ZN7android7BitTube11sendObjects")
                val sensorServiceOut = runSuCommand("$cmd -Ws /system/lib64/libsensorservice.so 2>/dev/null | grep '_ZN7android8hardware7sensors14implementation20convertToSensorEvent[^4V1]'")
                val sensorServiceV1Out = runSuCommand("$cmd -Ws /system/lib64/libsensorservice.so 2>/dev/null | grep '_ZN7android8hardware7sensors4V1_014implementation20convertToSensorEvent'")

                if (sensorOut.isNotEmpty()) {
                    val sensorOffset = parseReadelfOffset(sensorOut)
                    val sensorServiceOffset = when {
                        sensorServiceOut.isNotEmpty() -> parseReadelfOffset(sensorServiceOut)
                        sensorServiceV1Out.isNotEmpty() -> parseReadelfOffset(sensorServiceV1Out)
                        else -> ""
                    }
                    if (sensorOffset.isNotEmpty() && sensorServiceOffset.isNotEmpty()) {
                        return Pair(
                            if (sensorOffset.startsWith("0x")) sensorOffset else "0x$sensorOffset",
                            if (sensorServiceOffset.startsWith("0x")) sensorServiceOffset else "0x$sensorServiceOffset"
                        )
                    }
                }
            } catch (_: Exception) { /* try next candidate */ }
        }
        KailLog.w(this, TAG, "readelf unavailable; sensor offsets unknown")
        return Pair("0x0", "0x0")
    }

    private fun parseReadelfOffset(output: String): String {
        val trimmed = output.trim().lines().firstOrNull()?.trim() ?: return ""
        val parts = trimmed.split(Regex("\\s+"))
        return parts.firstOrNull { it.matches(Regex("^[0-9a-fA-F]{8,16}$")) } ?: ""
    }

    private fun parseHexOffset(s: String): Long {
        val v = s.removePrefix("0x").removePrefix("0X")
        return v.toLongOrNull(16) ?: 0L
    }

    inner class ServiceGoRootBinder : Binder() {
        fun getService(): ServiceGoRoot = this@ServiceGoRoot
    }
}
