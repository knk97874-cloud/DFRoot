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

    def test_01_privacy_sanitizer_and_no_build_host_leak(self):
        """
        Verify platform tag sanitizer contract and ensure KernelSuManager.java
        does not trust or display raw /proc/version build-host strings as driver proof.
        """
        sample_raw = "Linux-6.6.102-custom-build-aarch64-with-glibc2.43"
        sanitized = sanitize_platform(sample_raw)
        self.assertNotIn("custom-build", sanitized)
        self.assertIn("generic", sanitized)

        # Enforce that KernelSuManager does not use /proc/version to claim compatibility
        self.assertNotIn("/proc/version", self.ksu_manager_java,
                         "KernelSuManager must not parse /proc/version build host strings as compatibility proof")

    def test_02_kernel_trust_model_rejection_contract(self):
        """
        Verify that AndroidManifest.xml and KernelSuManager.java maintain df.root
        package identity and do not claim to be the upstream me.weishu.kernelsu manager.
        """
        # Manifest application package / namespace check
        self.assertIn('namespace = "df.root"', read_repo_file("app/build.gradle.kts"))
        self.assertIn('UPSTREAM_MANAGER_PKG = "me.weishu.kernelsu"', self.ksu_manager_java)
        
        # Verify throne tracker separation: df.root must not impersonate upstream
        self.assertNotIn('android:sharedUserId="me.weishu.kernelsu"', self.manifest_xml)

    def test_03_diagnostic_reporting_three_part_distinction(self):
        """
        Inspect actual KernelSuManager.java source code to verify that reportBundledDiagnostics
        explicitly asserts all three required diagnostic categories:
        1. 'PACKAGED (PRESENT)'
        2. 'Compatibility: UNKNOWN'
        3. 'Runtime Integration: INCOMPLETE'
        """
        self.assertIn("reportBundledDiagnostics", self.ksu_manager_java,
                      "KernelSuManager.java must define reportBundledDiagnostics")

        # 1. Binary packaged distinction
        self.assertIn('* Standard Variant (libksud_standard.so): " + (hasStandard ? "PACKAGED (PRESENT)" : "ABSENT")',
                      self.ksu_manager_java)
        self.assertIn('* Samsung Variant (libksud_samsung.so): " + (hasSamsung ? "PACKAGED (PRESENT)" : "ABSENT")',
                      self.ksu_manager_java)

        # 2. Compatibility unknown distinction
        self.assertIn("Compatibility: UNKNOWN (requires authoritative kernel driver verification)",
                      self.ksu_manager_java)

        # 3. Runtime integration incomplete distinction
        self.assertIn("Runtime Integration: INCOMPLETE (selection & staging decoupled pending driver identification)",
                      self.ksu_manager_java)
        self.assertIn("Neither binary is staged or selected", self.ksu_manager_java)

    def test_04_manager_list_empty_state_and_duplicate_suppression(self):
        """
        Inspect actual MainActivity.java source code to verify:
        1. Spinner is explicitly disabled when entries.size() <= 1 (suppressing duplicate prompt/popup)
        2. DropDownView suppresses position 0 placeholder (View.GONE)
        """
        self.assertIn("entries.size() <= 1", self.main_activity_java)
        self.assertIn("spinnerSuManager.setEnabled(false);", self.main_activity_java)
        self.assertIn('No SU Manager installed', self.main_activity_java)

        # Check DropDownView hiding placeholder item 0
        self.assertIn("if (e.packageName == null) {", self.main_activity_java)
        self.assertIn("emptyView.setVisibility(View.GONE);", self.main_activity_java)

    def test_05_button_state_logic_isolation(self):
        """
        Inspect actual MainActivity.java updateRunButton() method:
        Verify that canRun strictly checks (mValidSuManagerPos >= 1 || KernelSuManager.hasBundledKsud(this))
        and is NOT prematurely enabled by hasBundledStandardKsud or hasBundledSamsungKsud.
        """
        # Ensure updateRunButton exists
        self.assertIn("private void updateRunButton()", self.main_activity_java)

        # Verify canRun logic formula in Java
        self.assertIn("boolean canRun = (mValidSuManagerPos >= 1 || KernelSuManager.hasBundledKsud(this));",
                      self.main_activity_java)
        self.assertIn("btnRun.setEnabled(canRun && !new File(\"/dev/df\").exists());",
                      self.main_activity_java)

        # Ensure dual variants are NOT hooked into canRun
        self.assertNotIn("hasBundledStandardKsud", self.main_activity_java,
                         "MainActivity.updateRunButton must not enable btnRun based on bundled standard variant")
        self.assertNotIn("hasBundledSamsungKsud", self.main_activity_java,
                         "MainActivity.updateRunButton must not enable btnRun based on bundled samsung variant")

    def test_06_missing_binary_and_unselected_manager_error_handling(self):
        """
        Inspect actual ExploitRunner.java run() method:
        Verify that when suManagerPackage is null and libksud.so is absent:
        1. It reports 'no SU manager selected'
        2. It immediately halts and returns error code 2
        """
        self.assertIn("if (suManagerPackage == null) {", self.exploit_runner_java)
        self.assertIn('reporter.report("no SU manager selected\\n");', self.exploit_runner_java)
        self.assertIn("return 2;", self.exploit_runner_java)

    def test_07_unauthorized_manager_supercall_rejection(self):
        """
        Inspect KernelSuManager.java to ensure it does not attempt privileged
        KernelSU supercalls (which require only_manager / KSU throne authorization)
        during unverified diagnostic runs.
        """
        # Verify no unauthenticated supercall or ioctl invocations in KernelSuManager
        self.assertNotIn("ioctl", self.ksu_manager_java)
        self.assertNotIn("prctl", self.ksu_manager_java)


if __name__ == "__main__":
    unittest.main()
