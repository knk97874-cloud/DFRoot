#!/usr/bin/env python3
"""
Static Source-Contract Test Suite for DFRoot Single-App KernelSU Integration

CRITICAL SCOPE & COVERAGE LIMITATION NOTICE:
- These tests are STATIC SOURCE-CONTRACT checks executed via Python.
- They inspect and enforce invariants, logic gates, and diagnostic strings
  directly from actual Java sources (KernelSuManager.java, MainActivity.java,
  ExploitRunner.java, AndroidManifest.xml, build.gradle.kts).
- They do NOT execute compiled Android DEX bytecode on ART/Android OS runtime.
  True runtime validation requires an on-device instrumentation runner
  (connectedAndroidTest) or Robolectric JVM test harness with Android framework stubs.
"""

import os
import re
import unittest

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))


def read_repo_file(rel_path: str) -> str:
    path = os.path.join(REPO_ROOT, rel_path)
    with open(path, "r", encoding="utf-8") as f:
        return f.read()


def sanitize_platform(raw_val: str) -> str:
    """Normalize custom/personal kernel flavor tags to a neutral 'generic' identifier."""
    return re.sub(r"^(Linux-\d+\.\d+(?:\.\d+)?)-.+?-(aarch64.*|x86_64.*|arm.*)$", r"\1-generic-\2", raw_val)


class TestKernelSuManagerStaticContract(unittest.TestCase):
    """
    Validates architectural, security, and UI state contracts
    directly against the actual Android Java source files.
    """

    @classmethod
    def setUpClass(cls):
        cls.ksu_manager_java = read_repo_file("app/src/main/java/df/root/ksu/KernelSuManager.java")
        cls.main_activity_java = read_repo_file("app/src/main/java/df/root/MainActivity.java")
        cls.exploit_runner_java = read_repo_file("app/src/main/java/df/root/ExploitRunner.java")
        cls.manifest_xml = read_repo_file("app/src/main/AndroidManifest.xml")
        cls.build_gradle_kts = read_repo_file("app/build.gradle.kts")
        cls.ksu_jni_c = read_repo_file("app/src/main/jni/ksu_jni.c")

    def test_01_privacy_sanitizer_and_no_build_host_leak(self):
        """
        Verify platform tag sanitizer contract and ensure KernelSuManager.java
        does not trust or display raw /proc/version build-host strings as driver proof.
        """
        raw_mock = "Linux-6.6.21-diabl0w-mybuildhost-2026-aarch64-unknown"
        sanitized = sanitize_platform(raw_mock)
        self.assertNotIn("diabl0w", sanitized, "Platform sanitizer must strip personal tags")
        self.assertNotIn("mybuildhost", sanitized, "Platform sanitizer must strip host names")
        self.assertTrue(sanitized.startswith("Linux-6.6.21-generic-"), "Must normalize to generic identifier")

        self.assertNotIn("/proc/version", self.ksu_manager_java,
                         "KernelSuManager must not parse /proc/version to claim driver active")

    def test_02_supercall_ioctl_interface_semantics(self):
        """
        Verify that active driver status requires authoritative IOCTL supercall query,
        and distinguishes authorized manager status from unauthorized driver presence.
        """
        self.assertIn("nativeGetDriverVersion", self.ksu_manager_java,
                      "KernelSuManager must declare/call nativeGetDriverVersion")
        self.assertIn("nativeIsManager", self.ksu_manager_java,
                      "KernelSuManager must declare/call nativeIsManager")
        self.assertIn("ACTIVE_UNAUTHORIZED", self.ksu_manager_java,
                      "Status enum must include ACTIVE_UNAUTHORIZED")
        self.assertIn("ACTIVE_AUTHORIZED", self.ksu_manager_java,
                      "Status enum must include ACTIVE_AUTHORIZED")

    def test_03_honest_blocker_reporting(self):
        """
        Verify that diagnostic output explicitly explains the root-cause blockers:
        - Upstream expects me.weishu.kernelsu package name
        - Upstream verifies APK release signature
        - Supercall interface is denied for unverified packages
        """
        self.assertIn("me.weishu.kernelsu", self.ksu_manager_java)
        self.assertIn("APK signature", self.ksu_manager_java)
        self.assertIn("Supercall IOCTL interface denied", self.ksu_manager_java)

    def test_04_no_auto_select_placeholder_label(self):
        """
        Verify that when no valid external manager is detected,
        the UI displays 'No SU Manager installed' or 'Select a SU Manager'
        instead of falsely claiming standard or samsung is active.
        """
        self.assertIn("Select a SU Manager", self.main_activity_java,
                      "MainActivity must use neutral selection placeholder")
        self.assertIn("No SU Manager installed", self.main_activity_java,
                      "MainActivity must show 'No SU Manager installed' when none detected")

    def test_05_known_manager_packages_contract(self):
        """
        Verify that KernelSuManager explicitly declares known upstream and fork package names.
        """
        self.assertIn("me.weishu.kernelsu", self.ksu_manager_java)
        self.assertIn("io.github.a13e300.ksu", self.ksu_manager_java)
        self.assertIn("me.bmax.apatch", self.ksu_manager_java)

    def test_06_manifest_queries_element_declared(self):
        """
        Verify that AndroidManifest.xml contains explicit <queries> declarations
        for known SU Manager packages to satisfy Android 11+ package visibility rules.
        """
        self.assertIn("<queries>", self.manifest_xml)
        self.assertIn('android:name="me.weishu.kernelsu"', self.manifest_xml)
        self.assertIn('android:name="io.github.a13e300.ksu"', self.manifest_xml)
        self.assertIn('android:name="me.bmax.apatch"', self.manifest_xml)

    def test_07_defensive_null_checks_and_lifecycle_guards(self):
        """
        Verify defensive programming contracts in Activity and Receiver classes:
        - isFinishing() / isDestroyed() guards before UI operations
        - Null check on getActionBar() in MainActivity and SettingsActivity
        - Defensive PowerManager null checks and WakeLock timeout in BootReceiver
        """
        boot_receiver_java = read_repo_file("app/src/main/java/df/root/BootReceiver.java")
        settings_activity_java = read_repo_file("app/src/main/java/df/root/SettingsActivity.java")

        self.assertIn("isFinishing()", self.main_activity_java)
        self.assertIn("isDestroyed()", self.main_activity_java)
        self.assertIn("getActionBar() != null", self.main_activity_java)
        self.assertIn("getActionBar() != null", settings_activity_java)
        self.assertIn("pm != null", boot_receiver_java)
        self.assertIn("wl.acquire(10 * 60 * 1000L);", boot_receiver_java)

    def test_08_package_visibility_and_prerequisite_reporting(self):
        """
        Verify:
        1. AndroidManifest.xml queries declaration satisfies package visibility.
        2. KernelSuManager diagnostic report distinguishes between external manager presence
           and missing prerequisites.
        3. Launch Root button enablement conditions are clearly logged to UI.
        """
        self.assertIn("hasSelectableSuManager", self.ksu_manager_java)
        self.assertIn("Prerequisites missing:", self.ksu_manager_java)
        self.assertIn("Launch Root Button:", self.ksu_manager_java)

    def test_09_dev_df_marker_and_missing_prerequisite_isolation(self):
        """
        Verify:
        1. /dev/df marker is qualified as a prior attempt/unverified filesystem marker,
           NOT authoritatively proving loaded kernel code.
        2. KernelSuManager reports prerequisite items independently.
        3. Method alignment exists between MainActivity and KernelSuManager.
        """
        # 1. Honest marker reporting
        self.assertNotIn("Marker: /dev/df indicates active driver", self.ksu_manager_java,
                         "KernelSuManager must not claim /dev/df proves active kernel hooks")

        # 2. Unverified marker qualification
        self.assertIn("Marker: /dev/df present (prior hook attempt or unverified filesystem marker",
                      self.ksu_manager_java)

        # 3. Method alignment with MainActivity
        self.assertIn("hasSelectableSuManager", self.ksu_manager_java)

        # 4. Independent prerequisite checking
        self.assertIn("if (!hasSelectableManager && !hasBundled)", self.ksu_manager_java)

    def test_10_four_state_manager_and_binary_distinction(self):
        """
        Inspect MainActivity.java and KernelSuManager.java to verify precise distinction
        across all four states:
        1. A known manager package is installed and visible with libksud.so (Select a SU Manager).
        2. A manager package is installed and visible, but lacks libksud.so (Manager installed (lacks libksud.so)).
        3. No supported manager package is installed (No SU Manager installed).
        4. Bundled binaries exist but runtime integration is incomplete.
        """
        # 1. State 2 distinct placeholder in MainActivity
        self.assertIn('placeholder = "Manager installed (lacks libksud.so)";', self.main_activity_java)
        self.assertIn('placeholder = "No SU Manager installed";', self.main_activity_java)
        self.assertIn('placeholder = "Select a SU Manager";', self.main_activity_java)

        # 2. Package discovery method in KernelSuManager
        self.assertIn("getInstalledExternalManagerPackage", self.ksu_manager_java)

        # 3. Diagnostic reporting differentiates package presence vs binary availability
        self.assertIn('External SU Manager is NOT installed', self.ksu_manager_java)
        self.assertIn('External SU Manager (" + installedManagerPkg + ") is installed but lacks extracted libksud.so',
                      self.ksu_manager_java)

        # 4. State 4: Bundled binaries distinction (packaged vs incomplete runtime integration)
        self.assertIn("hasBundledStandardKsud", self.ksu_manager_java)
        self.assertIn("hasBundledSamsungKsud", self.ksu_manager_java)
        self.assertIn("Runtime Integration: INCOMPLETE", self.ksu_manager_java)

    def test_11_bundled_variant_ui_selection_and_button_enablement(self):
        """
        Verify Option A contract:
        1. KernelSuManager defines bundled variant package IDs.
        2. MainActivity populates entries with bundled variants and auto-selects based on Build.MANUFACTURER.
        3. ExploitRunner safely handles bundled: prefix by verifying binary accessibility in nativeLibraryDir.
        """
        # 1. Package IDs in KernelSuManager
        self.assertIn('public static final String ID_BUNDLED_STANDARD = "bundled:standard";', self.ksu_manager_java)
        self.assertIn('public static final String ID_BUNDLED_SAMSUNG = "bundled:samsung";', self.ksu_manager_java)

        # 2. Bundled entries in MainActivity
        self.assertIn('ID_BUNDLED_STANDARD', self.main_activity_java)
        self.assertIn('ID_BUNDLED_SAMSUNG', self.main_activity_java)
        self.assertIn('Build.MANUFACTURER.equalsIgnoreCase("samsung")', self.main_activity_java)

        # 3. ExploitRunner safe decoupled backend guard
        self.assertIn('suManagerPackage.startsWith("bundled:")', self.exploit_runner_java)
        self.assertIn('BUNDLED_KSUD_SAMSUNG_LIB', self.exploit_runner_java)
        self.assertIn('BUNDLED_KSUD_STANDARD_LIB', self.exploit_runner_java)
        self.assertIn('Root exploitation decoupled for safety', self.exploit_runner_java)

    def test_12_bundled_staging_and_accurate_return_code_semantics(self):
        """
        Verify:
        1. ExploitRunner defines explicit, documented return codes.
        2. Staging is performed for bundled variant into app executable path.
        3. Staging and decoupled testing returns RC_STAGED_DECOUPLED (4), NEVER RC_SUCCESS (0).
        4. Staging failure returns RC_STAGE_ERR (3) and missing binary returns RC_GENERAL_ERR (2).
        5. MainActivity maps RC_STAGED_DECOUPLED to distinct toast without claiming root success.
        6. Background manager sync is strictly gated on genuine RC_SUCCESS.
        """
        # 1. Return code constants in ExploitRunner
        self.assertIn('public static final int RC_SUCCESS          = 0;', self.exploit_runner_java)
        self.assertIn('public static final int RC_KSUD_ERR         = 1;', self.exploit_runner_java)
        self.assertIn('public static final int RC_GENERAL_ERR      = 2;', self.exploit_runner_java)
        self.assertIn('public static final int RC_STAGE_ERR        = 3;', self.exploit_runner_java)
        self.assertIn('public static final int RC_STAGED_DECOUPLED = 4;', self.exploit_runner_java)

        # 2. Staging execution in ExploitRunner
        self.assertIn('stageBin(ctx, bundledSrc, "ksud");', self.exploit_runner_java)
        self.assertIn('return RC_STAGED_DECOUPLED;', self.exploit_runner_java)
        self.assertIn('return RC_STAGE_ERR;', self.exploit_runner_java)
        self.assertIn('return RC_GENERAL_ERR;', self.exploit_runner_java)

        # 3. Decoupled test mode must never return RC_SUCCESS
        bundled_block = self.exploit_runner_java.split('suManagerPackage.startsWith("bundled:")')[1].split('else if (!suManagerPackage.equals')[0]
        self.assertNotIn('return RC_SUCCESS;', bundled_block)
        self.assertNotIn('return 0;', bundled_block)

        # 4. MainActivity UI mapping
        self.assertIn('rc == ExploitRunner.RC_STAGED_DECOUPLED ? "DFRoot: Staged (Exploitation Decoupled)"', self.main_activity_java)
        self.assertIn('if (rc == ExploitRunner.RC_SUCCESS)', self.main_activity_java)

    def test_13_self_contained_single_app_zero_external_apk_contract(self):
        """
        Verify:
        1. ExploitRunner sets PREF_SU_MANAGER to ctx.getPackageName() (df.root) on bundled staging,
           guaranteeing zero external APK requirement.
        2. ExploitRunner logs Self-Contained All-in-One Mode.
        3. KernelSuManager defines isSelfManaged helper.
        4. MainActivity provides 'Self-Contained KernelSU Engine' dropdown placeholder.
        """
        self.assertIn("prefs.edit().putString(PREF_SU_MANAGER, ctx.getPackageName()).apply();",
                      self.exploit_runner_java)
        self.assertIn("Self-Contained All-in-One Mode", self.exploit_runner_java)
        self.assertIn("public static boolean isSelfManaged(Context ctx)", self.ksu_manager_java)
        self.assertIn("Self-Contained KernelSU Engine", self.main_activity_java)

    def test_14_gradle_automatic_ksud_download_contract(self):
        """
        Verify:
        1. build.gradle.kts defines ensureDualKsudBinaries task.
        2. Expected SHA-256 hashes are specified for both variants.
        3. preBuild dependsOn ensureDualKsudBinaries to guarantee self-sufficient local compilation.
        """
        self.assertIn('tasks.register("ensureDualKsudBinaries")', self.build_gradle_kts)
        self.assertIn("049491e2b4cc0a6c913d3920cb90b8c94c9e8ca96c0ee92818293304973bfb1b", self.build_gradle_kts)
        self.assertIn("3934878b5d01ab9f1dfdadbbb67505c7524090de1574719308211b26f0988f72", self.build_gradle_kts)
        self.assertIn('tasks.named("preBuild").configure {', self.build_gradle_kts)
        self.assertIn('dependsOn("ensureDualKsudBinaries")', self.build_gradle_kts)

    def test_15_stage_assets_bundled_variant_resolution_contract(self):
        """
        Verify:
        1. ExploitRunner.stageAssets handles bundled: prefix without failing or assuming external app.
        2. ExploitRunner.stageAssets resolves self-package (df.root) across bundled standard, samsung, or plain ksud.
        """
        self.assertIn('if (suManagerPackage != null && suManagerPackage.startsWith("bundled:"))', self.exploit_runner_java)
        self.assertIn('File std = new File(ctx.getApplicationInfo().nativeLibraryDir, df.root.ksu.KernelSuManager.BUNDLED_KSUD_STANDARD_LIB);', self.exploit_runner_java)
        self.assertIn('File sam = new File(ctx.getApplicationInfo().nativeLibraryDir, df.root.ksu.KernelSuManager.BUNDLED_KSUD_SAMSUNG_LIB);', self.exploit_runner_java)

    def test_16_native_grant_root_ioctl_bridge_contract(self):
        """
        Verify:
        1. ksu_jni.c defines KSU_IOCTL_GRANT_ROOT command.
        2. nativeGrantRoot communicates directly with the driver via ioctl instead of hardcoded -EPERM.
        """
        self.assertIn('KSU_IOCTL_GRANT_ROOT', self.ksu_jni_c)
        self.assertIn('ioctl(fd, KSU_IOCTL_GRANT_ROOT, &target_uid)', self.ksu_jni_c)


if __name__ == "__main__":
    unittest.main()
