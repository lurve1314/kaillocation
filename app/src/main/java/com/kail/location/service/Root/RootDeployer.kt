package com.kail.location.service.Root

import android.content.Context
import com.kail.location.utils.KailLog
import com.kail.location.utils.ShellUtils
import java.io.File
import java.util.zip.ZipFile

/**
 * Helper for the "root" run mode.
 *
 * What this does on every service start:
 *   - Creates the kail staging directories with permissive SELinux labels.
 *   - Copies libkail_native_hook.so out of the APK into /data/local/kail-lib/
 *     so the in-app NativeSensorHook can dlopen it for step-counter mocking.
 *   - Grants the host package the AppOps `android:mock_location` permission
 *     via `appops set` so [com.kail.location.service.Developer.MockLocationProvider]
 *     can register a test provider without the user manually flipping
 *     "select mock location app" in Developer Settings.
 *
 * What this deliberately does NOT do:
 *   - Run kail_inject against system_server. Ptrace-injecting system_server
 *     is extremely fragile on production ROMs: a single mismatch between the
 *     loader's expected ART layout and the running framework version freezes
 *     the entire phone (system_server hangs in ptrace_stop, every UI thread
 *     blocks on it). The kail FakeLocation injection framework lives under
 *     [FAKELOC_DIR] and the injector binary lives at [STAGING_DIR]/kail_inject
 *     for operators who want to run it manually after vetting it on their
 *     specific ROM, but the controller app does not auto-run them.
 *
 * The opt-in helpers [stageInjectionPayloads] and [bootstrapInjection] are
 * provided so a developer can wire them to a manual button in the UI later.
 */
object RootDeployer {
    private const val TAG = "RootDeployer"

    const val STAGING_DIR = "/data/local/kail-lib"
    const val FAKELOC_DIR = "/data/kail-loc"
    const val NATIVE_HOOK_SO = "libkail_native_hook.so"
    const val INJECTOR_BIN = "kail_inject"

    /** FakeLocation loader/hook libraries packaged in the APK under lib/<abi>/. */
    private val FAKELOC_LIBS = listOf(
        "libfakeloc_init.so",
        "libfakeloc_initzygote.so",
        "libfakeloc_apphook.so",
        "liblhooker.so",
        "libStepSensor.so",
        "libantidetect.so"
    )

    /**
     * Idempotent setup that the service runs at every start.
     *
     * Stages the FakeLocation toolchain on disk, grants the AppOps mock
     * location permission, and runs `kail_inject` against system_server to
     * register the service_mock_* binders (matching the original FakeLocation
     * behaviour). The injector now has a 5-second watchdog (see
     * cpp/root/inject{,64}.cpp) so a hung remote dlopen detaches the tracee
     * cleanly instead of leaving system_server in ptrace_stop.
     */
    fun ensureBaseline(context: Context): Boolean {
        if (!ShellUtils.hasRoot()) {
            KailLog.w(null, TAG, "ensureBaseline: no root; skipping")
            return false
        }
        prepareDirs()
        deployNativeHookLib(context)
        deployInjectorBin(context)
        deployFakelocLibs(context)
        deployDexPayload(context)
        grantMockLocationAppOps(context)
        // Best-effort inject. If the watchdog trips we'll just log the
        // injector's "Inject fail" message; the service still functions
        // through the test-provider path.
        runCatching { bootstrapInjection() }
            .onFailure { KailLog.w(null, TAG, "bootstrapInjection: ${it.message}") }
        return true
    }

    /**
     * Run kail_inject against system_server to register the FakeLocation
     * service_mock_* binders.
     *
     * The injector has a 5-second watchdog (see cpp/root/inject{,64}.cpp) that
     * trips PTRACE_DETACH if a remote function hangs — typically when the
     * remote dlopen blocks on a linker mutex held by a sibling thread. With
     * the watchdog, a hung inject leaves system_server runnable instead of
     * permafrozen, at the cost of an "Inject fail" return.
     */
    fun bootstrapInjection(): Boolean {
        if (!ShellUtils.hasRoot()) return false
        val injector = File(STAGING_DIR, INJECTOR_BIN)
        val initLoader = File(FAKELOC_DIR, "libfakeloc_init.so")
        if (!injector.exists() || !initLoader.exists()) {
            KailLog.e(null, TAG, "bootstrapInjection: injector or loader missing; call stageInjectionPayloads first")
            return false
        }
        val cmd = "${injector.absolutePath} -P system_server -l ${initLoader.absolutePath} -n com.kail.location"
        val out = ShellUtils.executeCommand(cmd)
        KailLog.i(null, TAG, "kail_inject -> $out")
        return out.contains("Inject ok")
    }

    /**
     * Inject the FakeLocation app-hook loader into an arbitrary running
     * process by name. Used to bring the cell-tower pull APIs
     * (TelephonyManager.getAllCellInfo / getCellLocation, served by
     * com.android.phone's PhoneInterfaceManager) under
     * [com.kail.location.inject.fakelocation.hook.phone.PhoneInterfaceManagerHook].
     *
     * The system_server inject (registering the service_mock_* binders) is a
     * prerequisite — the app-hook InjectDex.hookApplication path reads mock
     * state through those binders. Safe to call repeatedly; injecting an
     * already-hooked process just re-runs hookApplication which no-ops the
     * already-installed hooks.
     */
    fun injectAppProcess(processName: String): Boolean {
        if (!ShellUtils.hasRoot()) return false
        val injector = File(STAGING_DIR, INJECTOR_BIN)
        // libfakeloc_apphook.so -> InjectDex.hookApplication (installs the
        // per-process hooks, including PhoneInterfaceManagerHook for phone).
        val appLoader = File(FAKELOC_DIR, "libfakeloc_apphook.so")
        if (!injector.exists() || !appLoader.exists()) {
            KailLog.e(null, TAG, "injectAppProcess: injector or apphook loader missing")
            return false
        }
        val cmd = "${injector.absolutePath} -P $processName -l ${appLoader.absolutePath} -n com.kail.location"
        val out = ShellUtils.executeCommand(cmd)
        KailLog.i(null, TAG, "kail_inject ($processName) -> $out")
        return out.contains("Inject ok")
    }

    /**
     * Side-effect free check for whether the ptrace-injection prerequisites
     * have already been staged on disk.
     */
    fun isInjectionStaged(): Boolean {
        if (!File(FAKELOC_DIR, "libfakeloc.so").exists()) return false
        if (!File(FAKELOC_DIR, "libfakeloc_init.so").exists()) return false
        if (!File(STAGING_DIR, INJECTOR_BIN).exists()) return false
        return true
    }

    // ------------------------------------------------------------------
    // Building blocks
    // ------------------------------------------------------------------

    fun deployNativeHookLib(context: Context): Boolean {
        val src = File(context.applicationInfo.nativeLibraryDir, NATIVE_HOOK_SO)
        val dst = File(STAGING_DIR, NATIVE_HOOK_SO)
        return copyAndChmod(context, src, "lib/${preferredAbi()}/$NATIVE_HOOK_SO", dst)
    }

    fun deployInjectorBin(context: Context): Boolean {
        val abi = preferredAbi()
        val src = File(context.applicationInfo.nativeLibraryDir, "libkail_inject.so")
        val dst = File(STAGING_DIR, INJECTOR_BIN)
        val ok = copyAndChmod(context, src, "lib/$abi/libkail_inject.so", dst)
        if (ok) ShellUtils.executeCommand("chmod 755 ${dst.absolutePath}")
        return ok
    }

    fun deployFakelocLibs(context: Context): Boolean {
        var initLoader = false
        val abi = preferredAbi()
        val isArm64 = abi == "arm64-v8a"
        for (name in FAKELOC_LIBS) {
            val src = File(context.applicationInfo.nativeLibraryDir, name)
            val dst = File(FAKELOC_DIR, name)
            val ok = copyAndChmod(context, src, "lib/$abi/$name", dst)
            if (ok && name == "libfakeloc_init.so") initLoader = true

            // InjectDex.java probes both `<name>.so` (arm) and `<name>64.so`
            // (arm64) without checking which side actually exists. Mirror the
            // file under the matching suffix for the active ABI so the lookup
            // succeeds regardless of which path it picks first.
            if (ok && isArm64 && !name.contains("64.so")) {
                val sixtyFour = name.replace(".so", "64.so")
                val mirror = File(FAKELOC_DIR, sixtyFour)
                ShellUtils.executeCommand("cp -f ${dst.absolutePath} ${mirror.absolutePath}")
                ShellUtils.executeCommand("chmod 777 ${mirror.absolutePath}")
                ShellUtils.executeCommand("chcon u:object_r:system_file:s0 ${mirror.absolutePath}")
            }
        }
        return initLoader
    }

    fun deployDexPayload(context: Context): Boolean {
        val dst = File(FAKELOC_DIR, "libfakeloc.so")
        // Prefer the slim inject.dex we ship in assets — it contains only the
        // FakeLocation bootstrap classes (com.kail.location.inject.* +
        // com.kail.location.lib.lhooker.*), about 1-2 MB compared to the full
        // 33 MB APK. system_server's DexClassLoader can verify that small dex
        // in well under our 60 s ptrace watchdog window. The full APK path is
        // only used as a fallback if assets/inject.dex is missing (older builds
        // without the slim-dex Gradle task).
        val slim = runCatching {
            val out = File(context.cacheDir, "inject.dex")
            context.assets.open("inject.dex").use { input ->
                out.outputStream().use { input.copyTo(it) }
            }
            out
        }.getOrNull()

        return runCatching {
            if (slim != null && slim.exists() && slim.length() > 0) {
                ShellUtils.executeCommand("cp -f ${slim.absolutePath} ${dst.absolutePath}")
                KailLog.i(null, TAG, "deployDexPayload: using slim inject.dex (${slim.length()} bytes)")
            } else {
                val apkPath = context.applicationInfo.sourceDir ?: return@runCatching false
                ShellUtils.executeCommand("cp -f $apkPath ${dst.absolutePath}")
                KailLog.w(null, TAG, "deployDexPayload: assets/inject.dex missing; falling back to full APK ($apkPath)")
            }
            ShellUtils.executeCommand("chmod 644 ${dst.absolutePath}")
            ShellUtils.executeCommand("chcon u:object_r:system_file:s0 ${dst.absolutePath}")
            dst.exists() && dst.length() > 0
        }.getOrElse {
            KailLog.e(null, TAG, "deployDexPayload: ${it.message}")
            false
        }
    }

    /**
     * Silently grant the host package the `android:mock_location` AppOps so
     * `LocationManager.addTestProvider` works without the user toggling
     * "select mock location app" in Developer Settings.
     */
    fun grantMockLocationAppOps(context: Context): Boolean {
        val pkg = context.packageName
        return runCatching {
            ShellUtils.executeCommand("appops set $pkg android:mock_location allow")
            // Some ROMs accept a numeric op id alias; harmless when it does not exist.
            ShellUtils.executeCommand("appops set $pkg 58 allow")
            true
        }.getOrElse {
            KailLog.e(null, TAG, "grantMockLocationAppOps: ${it.message}")
            false
        }
    }

    /** Convenience: revoke the AppOps grant when leaving root mode. */
    fun revokeMockLocationAppOps(context: Context): Boolean {
        val pkg = context.packageName
        return runCatching {
            ShellUtils.executeCommand("appops set $pkg android:mock_location ignore")
            true
        }.getOrElse { false }
    }

    private fun prepareDirs() {
        runCatching {
            ShellUtils.executeCommand("setenforce 0")
            for (d in listOf(STAGING_DIR, FAKELOC_DIR)) {
                ShellUtils.executeCommand("mkdir -p $d")
                ShellUtils.executeCommand("chmod 777 $d")
                ShellUtils.executeCommand("chcon u:object_r:system_file:s0 $d")
            }
            // libfakeloc_init.cpp uses /data/kail-loc/system_dex as the
            // DexClassLoader optimization output dir. If it doesn't exist
            // before we inject, ART falls back to compiling the 33MB APK in
            // an in-process buffer which can take >10s on cold cache and
            // sometimes never finishes (system_server gets killed by its
            // own watchdog). Pre-create it with permissive SELinux labels.
            for (d in listOf("$FAKELOC_DIR/system_dex", "$FAKELOC_DIR/oat")) {
                ShellUtils.executeCommand("mkdir -p $d")
                ShellUtils.executeCommand("chmod 777 $d")
                ShellUtils.executeCommand("chcon u:object_r:system_file:s0 $d")
            }
        }.onFailure { KailLog.e(null, TAG, "prepareDirs: ${it.message}") }
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    private fun preferredAbi(): String =
        android.os.Build.SUPPORTED_ABIS.firstOrNull { it == "arm64-v8a" }
            ?: android.os.Build.SUPPORTED_ABIS.firstOrNull()
            ?: "arm64-v8a"

    private fun copyAndChmod(context: Context, src: File, zipEntry: String, dst: File): Boolean {
        return runCatching {
            if (src.exists() && src.length() > 0) {
                ShellUtils.executeCommand("cp -f ${src.absolutePath} ${dst.absolutePath}")
            } else {
                extractFromApk(context, zipEntry, dst)
            }
            ShellUtils.executeCommand("chmod 777 ${dst.absolutePath}")
            ShellUtils.executeCommand("chcon u:object_r:system_file:s0 ${dst.absolutePath}")
            dst.exists() && dst.length() > 0
        }.getOrElse {
            KailLog.e(null, TAG, "copyAndChmod ${dst.name}: ${it.message}")
            false
        }
    }

    private fun extractFromApk(context: Context, zipEntry: String, dst: File) {
        val apkPath = context.applicationInfo.sourceDir ?: return
        ZipFile(apkPath).use { zip ->
            val entry = zip.getEntry(zipEntry) ?: return
            zip.getInputStream(entry).use { input ->
                dst.outputStream().use { out -> input.copyTo(out) }
            }
        }
    }
}
