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
    public static final String BUNDLED_KSUD_STANDARD_LIB = "libksud_standard.so";
    public static final String BUNDLED_KSUD_SAMSUNG_LIB = "libksud_samsung.so";
    public static final String ID_BUNDLED_STANDARD = "bundled:standard";
    public static final String ID_BUNDLED_SAMSUNG = "bundled:samsung";
    public static final String[] KNOWN_MANAGER_PKGS = {
        UPSTREAM_MANAGER_PKG,
        "io.github.a13e300.ksu",
        "me.bmax.apatch"
    };

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
     * Checks if DFRoot APK has bundled libksud_standard.so in its native library directory.
     */
    public static boolean hasBundledStandardKsud(Context ctx) {
        if (ctx == null) return false;
        ApplicationInfo ai = ctx.getApplicationInfo();
        if (ai == null || ai.nativeLibraryDir == null) return false;
        return new File(ai.nativeLibraryDir, BUNDLED_KSUD_STANDARD_LIB).exists();
    }

    /**
     * Checks if DFRoot APK has bundled libksud_samsung.so in its native library directory.
     */
    public static boolean hasBundledSamsungKsud(Context ctx) {
        if (ctx == null) return false;
        ApplicationInfo ai = ctx.getApplicationInfo();
        if (ai == null || ai.nativeLibraryDir == null) return false;
        return new File(ai.nativeLibraryDir, BUNDLED_KSUD_SAMSUNG_LIB).exists();
    }

    /**
     * Checks if DFRoot can operate in self-contained manager mode using bundled daemons.
     */
    public static boolean isSelfManaged(Context ctx) {
        return hasBundledStandardKsud(ctx) || hasBundledSamsungKsud(ctx) || hasBundledKsud(ctx);
    }

    /**
     * Diagnostic-only reporting for packaged KernelSU CLI binaries.
     * Clearly distinguishes:
     * 1. Binary packaged status
     * 2. Compatibility status
     * 3. Runtime integration status
     */
    public void reportBundledDiagnostics(Context ctx, IReporter reporter) {
        if (reporter == null) return;
        boolean hasStandard = hasBundledStandardKsud(ctx);
        boolean hasSamsung = hasBundledSamsungKsud(ctx);
        reporter.report("--- Bundled KernelSU Diagnostics ---\n");
        reporter.report("* Standard Variant (libksud_standard.so): " + (hasStandard ? "PACKAGED (PRESENT)" : "ABSENT") + "\n");
        reporter.report("  - Compatibility: UNKNOWN (requires authoritative kernel driver verification)\n");
        reporter.report("* Samsung Variant (libksud_samsung.so): " + (hasSamsung ? "PACKAGED (PRESENT)" : "ABSENT") + "\n");
        reporter.report("  - Compatibility: UNKNOWN (requires authoritative kernel driver verification)\n");
        reporter.report("* Runtime Integration: INCOMPLETE (selection & staging decoupled pending driver identification)\n");
        reporter.report("* Status: Diagnostic-only mode. Neither binary is staged or selected.\n");

        boolean hasSelectableManager = hasSelectableSuManager(ctx);
        String installedManagerPkg = getInstalledExternalManagerPackage(ctx);
        boolean hasBundled = hasBundledKsud(ctx) || hasStandard || hasSamsung;
        boolean isHooked = new File("/dev/df").exists();

        reporter.report("* Launch Root Button: " + ((hasSelectableManager || hasBundled) && !isHooked ? "ENABLED" : "DISABLED") + "\n");
        if (isHooked) {
            reporter.report("  - Marker: /dev/df present (prior hook attempt or unverified filesystem marker; reboot required to clear)\n");
        }
        if (!hasSelectableManager && !hasBundled) {
            reporter.report("  - Prerequisites missing:\n");
            if (installedManagerPkg == null) {
                reporter.report("    1. External SU Manager is NOT installed (checked me.weishu.kernelsu, io.github.a13e300.ksu, me.bmax.apatch)\n");
            } else {
                reporter.report("    1. External SU Manager (" + installedManagerPkg + ") is installed but lacks extracted libksud.so in nativeLibraryDir\n");
            }
            reporter.report("    2. Bundled binaries are for diagnostic detection only, not staged into runtime path\n");
            reporter.report("  - Action Required: Install an external SU Manager APK with extracted libksud.so or stage a verified binary\n");
        }
        reporter.report("\n");
    }

    /**
     * Checks if any installed non-system app contains libksud.so in its nativeLibraryDir.
     * Matches the exact criteria used by MainActivity and ExploitRunner.
     */
    public boolean hasSelectableSuManager(Context ctx) {
        if (ctx == null) return false;
        PackageManager pm = ctx.getPackageManager();
        if (pm == null) return false;
        for (ApplicationInfo ai : pm.getInstalledApplications(0)) {
            if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;
            if (ai.nativeLibraryDir != null && new File(ai.nativeLibraryDir, BUNDLED_KSUD_LIB).exists()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the package name of the first installed and visible external SU manager,
     * or null if no supported manager package is installed.
     */
    public static String getInstalledExternalManagerPackage(Context ctx) {
        if (ctx == null) return null;
        PackageManager pm = ctx.getPackageManager();
        if (pm == null) return null;
        for (String pkg : KNOWN_MANAGER_PKGS) {
            try {
                pm.getApplicationInfo(pkg, 0);
                return pkg;
            } catch (PackageManager.NameNotFoundException ignored) {}
        }
        return null;
    }

    /**
     * Checks if a supported external KernelSU Manager app is installed.
     */
    public boolean isExternalManagerInstalled(Context ctx) {
        return getInstalledExternalManagerPackage(ctx) != null;
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

        String installedPkg = getInstalledExternalManagerPackage(ctx);
        if (installedPkg != null) {
            reporter.report("* Upstream Manager: " + installedPkg + " detected on device\n");
        } else {
            reporter.report("* Upstream Manager: none detected (checked " + UPSTREAM_MANAGER_PKG + ", io.github.a13e300.ksu, me.bmax.apatch)\n");
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
                if (installedPkg != null) {
                    reporter.report("  -> Recommendation: Launch " + installedPkg + " for policy & module management.\n");
                } else {
                    reporter.report("  -> Recommendation: Install official me.weishu.kernelsu to grant root to apps.\n");
                }
                break;

            case ERROR:
            default:
                reporter.report("* KernelSU Driver: Query error or interface exception\n");
                break;
        }
    }
}
