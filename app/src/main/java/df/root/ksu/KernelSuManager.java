package df.root.ksu;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.File;

import df.root.IReporter;

public class KernelSuManager {

    private static final String TAG = "KernelSuManager";
    public static final String UPSTREAM_MANAGER_PKG = "me.weishu.kernelsu";
    public static final String BUNDLED_KSUD_LIB = "libksud.so";

    public enum Status {
        NOT_ROOTED,
        DRIVER_NOT_ACTIVE,
        ACTIVE_UNAUTHORIZED,
        ACTIVE_AUTHORIZED,
        ERROR
    }

    private static volatile KernelSuManager sInstance;

    public static KernelSuManager getInstance() {
        if (sInstance == null) {
            synchronized (KernelSuManager.class) {
                if (sInstance == null) {
                    sInstance = new KernelSuManager();
                }
            }
        }
        return sInstance;
    }

    private KernelSuManager() {}

    /**
     * Checks if DFRoot APK has bundled libksud.so in its native library directory.
     */
    public static boolean hasBundledKsud(Context ctx) {
        if (ctx == null) return false;
        ApplicationInfo ai = ctx.getApplicationInfo();
        if (ai == null || ai.nativeLibraryDir == null) return false;
        return new File(ai.nativeLibraryDir, BUNDLED_KSUD_LIB).exists();
    }

    /**
     * Checks if the official external KernelSU Manager app is installed.
     */
    public boolean isExternalManagerInstalled(Context ctx) {
        try {
            ctx.getPackageManager().getApplicationInfo(UPSTREAM_MANAGER_PKG, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * Evaluates current KernelSU driver and manager status.
     */
    public Status evaluateStatus(Context ctx) {
        if (!new File("/dev/df").exists()) {
            return Status.NOT_ROOTED;
        }

        if (!KernelSuNatives.isLibraryLoaded()) {
            // Check if su binary or ksud state directory exists
            if (new File("/data/adb/ksud").exists() || new File("/system/bin/su").exists()) {
                return Status.ACTIVE_UNAUTHORIZED;
            }
            return Status.DRIVER_NOT_ACTIVE;
        }

        try {
            int version = KernelSuNatives.nativeGetDriverVersion();
            if (version <= 0) {
                return Status.DRIVER_NOT_ACTIVE;
            }
            boolean isManager = KernelSuNatives.nativeIsManager();
            return isManager ? Status.ACTIVE_AUTHORIZED : Status.ACTIVE_UNAUTHORIZED;
        } catch (Throwable t) {
            Log.e(TAG, "Error querying native KernelSU driver status", t);
            return Status.ERROR;
        }
    }

    /**
     * Executes background KernelSU manager synchronization and diagnostics.
     * Runs completely in the background without modifying UI components.
     */
    public void executeBackgroundManagerSync(Context ctx, IReporter reporter) {
        reporter.report("\n--- KernelSU Background Manager Sync ---\n");

        boolean externalInstalled = isExternalManagerInstalled(ctx);
        if (externalInstalled) {
            reporter.report("* Upstream Manager: me.weishu.kernelsu detected on device\n");
        } else {
            reporter.report("* Upstream Manager: me.weishu.kernelsu not installed\n");
        }

        Status status = evaluateStatus(ctx);
        switch (status) {
            case NOT_ROOTED:
                reporter.report("* Hook Status: /dev/df not found (unrooted state)\n");
                break;

            case DRIVER_NOT_ACTIVE:
                reporter.report("* KernelSU Driver: Inactive or not loaded\n");
                break;

            case ACTIVE_AUTHORIZED:
                reporter.report("* KernelSU Driver: ACTIVE and AUTHORIZED\n");
                reporter.report("* Manager Status: df.root is recognized as crowned manager\n");
                break;

            case ACTIVE_UNAUTHORIZED:
                reporter.report("* KernelSU Driver: ACTIVE (late-load executed)\n");
                reporter.report("* Manager Status: UNAUTHORIZED (kernel trust check rejected df.root)\n");
                reporter.report("* Blocker Details:\n");
                reporter.report("  - Upstream driver expects KSU_PACKAGE_NAME = me.weishu.kernelsu\n");
                reporter.report("  - Driver verifies APK signature matching release keystore\n");
                reporter.report("  - Supercall IOCTL interface denied for package: " + ctx.getPackageName() + "\n");
                if (externalInstalled) {
                    reporter.report("  -> Recommendation: Launch me.weishu.kernelsu for policy & module management.\n");
                } else {
                    reporter.report("  -> Recommendation: Install official me.weishu.kernelsu to grant root to apps.\n");
                }
                break;

            case ERROR:
            default:
                reporter.report("* KernelSU Driver: Query error or interface exception\n");
                break;
        }
        reporter.report("----------------------------------------\n\n");
    }
}
