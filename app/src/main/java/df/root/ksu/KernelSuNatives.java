package df.root.ksu;

import android.util.Log;

public class KernelSuNatives {

    private static final String TAG = "KernelSuNatives";
    private static boolean sLibraryLoaded = false;

    static {
        try {
            System.loadLibrary("ksu_jni");
            sLibraryLoaded = true;
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "libksu_jni.so not loaded, native IOCTL fallback mode active");
            sLibraryLoaded = false;
        }
    }

    public static boolean isLibraryLoaded() {
        return sLibraryLoaded;
    }

    // KernelSU Supercall IOCTL commands and flags
    public static final int KSU_GET_INFO_FLAG_LKM = (1 << 0);
    public static final int KSU_GET_INFO_FLAG_MANAGER = (1 << 1);
    public static final int KSU_GET_INFO_FLAG_BUNDLED = (1 << 2);
    public static final int KSU_GET_INFO_FLAG_LATE_LOAD = (1 << 3);

    public static class DriverInfo {
        public final int version;
        public final int flags;
        public final int features;
        public final int uapiVersion;
        public final boolean isManager;
        public final boolean isLateLoaded;

        public DriverInfo(int version, int flags, int features, int uapiVersion) {
            this.version = version;
            this.flags = flags;
            this.features = features;
            this.uapiVersion = uapiVersion;
            this.isManager = (flags & KSU_GET_INFO_FLAG_MANAGER) != 0;
            this.isLateLoaded = (flags & KSU_GET_INFO_FLAG_LATE_LOAD) != 0;
        }

        @Override
        public String toString() {
            return "DriverInfo{version=" + version + ", flags=" + flags +
                    ", isManager=" + isManager + ", isLateLoaded=" + isLateLoaded +
                    ", uapiVersion=" + uapiVersion + "}";
        }
    }

    public static native int nativeGetDriverVersion();
    public static native int nativeGetDriverFlags();
    public static native int nativeGetDriverFeatures();
    public static native int nativeGetUapiVersion();
    public static native boolean nativeIsManager();
    public static native int nativeGrantRoot(int uid);
}
