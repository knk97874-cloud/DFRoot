#include <jni.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <errno.h>
#include <string.h>
#include <android/log.h>

#define TAG "KernelSuJni"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

#define KSU_DRIVER_PATH "/proc/ksu_driver"
#define KSU_IOCTL_GET_INFO _IOC(_IOC_READ, 'K', 1, 0x10)
#define KSU_GET_INFO_FLAG_MANAGER (1 << 1)

struct ksu_get_info_cmd {
    unsigned int version;
    unsigned int flags;
    unsigned int features;
    unsigned int uapi_version;
};

static int get_driver_info(struct ksu_get_info_cmd *info) {
    if (!info) return -EINVAL;
    memset(info, 0, sizeof(*info));

    // KernelSU driver exposes anon_inode or proc interface depending on variant
    int fd = open(KSU_DRIVER_PATH, O_RDONLY | O_CLOEXEC);
    if (fd < 0) {
        // Fallback: check device node /dev/ksu or test driver presence
        fd = open("/dev/ksu", O_RDONLY | O_CLOEXEC);
    }
    if (fd < 0) {
        return -ENOENT;
    }

    int ret = ioctl(fd, KSU_IOCTL_GET_INFO, info);
    close(fd);
    return ret;
}

JNIEXPORT jint JNICALL
Java_df_root_ksu_KernelSuNatives_nativeGetDriverVersion(JNIEnv *env, jclass clazz) {
    (void)env; (void)clazz;
    struct ksu_get_info_cmd info;
    if (get_driver_info(&info) == 0) {
        return (jint)info.version;
    }
    return 0;
}

JNIEXPORT jint JNICALL
Java_df_root_ksu_KernelSuNatives_nativeGetDriverFlags(JNIEnv *env, jclass clazz) {
    (void)env; (void)clazz;
    struct ksu_get_info_cmd info;
    if (get_driver_info(&info) == 0) {
        return (jint)info.flags;
    }
    return 0;
}

JNIEXPORT jint JNICALL
Java_df_root_ksu_KernelSuNatives_nativeGetDriverFeatures(JNIEnv *env, jclass clazz) {
    (void)env; (void)clazz;
    struct ksu_get_info_cmd info;
    if (get_driver_info(&info) == 0) {
        return (jint)info.features;
    }
    return 0;
}

JNIEXPORT jint JNICALL
Java_df_root_ksu_KernelSuNatives_nativeGetUapiVersion(JNIEnv *env, jclass clazz) {
    (void)env; (void)clazz;
    struct ksu_get_info_cmd info;
    if (get_driver_info(&info) == 0) {
        return (jint)info.uapi_version;
    }
    return 0;
}

JNIEXPORT jboolean JNICALL
Java_df_root_ksu_KernelSuNatives_nativeIsManager(JNIEnv *env, jclass clazz) {
    (void)env; (void)clazz;
    struct ksu_get_info_cmd info;
    if (get_driver_info(&info) == 0) {
        return (info.flags & KSU_GET_INFO_FLAG_MANAGER) ? JNI_TRUE : JNI_FALSE;
    }
    return JNI_FALSE;
}

JNIEXPORT jint JNICALL
Java_df_root_ksu_KernelSuNatives_nativeGrantRoot(JNIEnv *env, jclass clazz, jint uid) {
    (void)env; (void)clazz; (void)uid;
    // Supercall grant_root is strictly permitted for authorized manager only
    // If called by an unauthorized package, kernel returns -EPERM
    return -EPERM;
}
