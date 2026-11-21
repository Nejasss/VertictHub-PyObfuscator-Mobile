import unittest
from vh.android_adapter.compatibility import MANDATORY_CORE_MODULES, audit_core_modules


class CoreInventoryTests(unittest.TestCase):
    def test_all_34_modules_present_and_importable(self):
        report = audit_core_modules()
        self.assertEqual(report["total_expected"], 34)
        self.assertEqual(report["modules_present"], 34)
        self.assertEqual(report["modules_loaded"], 34)
        self.assertEqual(report["modules_verified"], 34)
        for mod in report["modules"]:
            self.assertTrue(mod["file_present"], mod["filename"])
            self.assertTrue(mod["sha256_verified"], mod["filename"])
            self.assertTrue(mod["import_ok"], f"{mod['filename']}: {mod['import_error']}")
            self.assertTrue(mod["apis_ok"], f"{mod['filename']}: missing {mod['missing_apis']}")
        self.assertEqual(len(MANDATORY_CORE_MODULES), 34)
