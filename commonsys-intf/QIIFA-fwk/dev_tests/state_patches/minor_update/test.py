
# Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.
# Confidential and Proprietary - Qualcomm Technologies, Inc.

import unittest
import sys
import os
import subprocess
import shutil

class QiifaTest(unittest.TestCase):
    base_cmd = ""
    expected_result = "expected_result"
    actual_result = "actual_result"
    qiifa_dir = ""

    def setUp(self):
        command = self.base_cmd + " --iic enforce --cr actual_report"
        if os.path.isdir("build"):
            shutil.rmtree("build")
        os.mkdir("build")
        subprocess.check_output("cmake ..",shell=True,cwd="build")
        subprocess.check_output("make -C build",shell=True)
        subprocess.check_output(command,shell=True)
        file_handler = open("expected_report","r")
        self.expected_result = file_handler.read()
        file_handler.close()
        file_handler = open("actual_report","r")
        self.actual_result = file_handler.read()
        file_handler.close()

    def test(self):
        self.assertEqual(self.actual_result, self.expected_result)

if __name__ == '__main__':
    if len(sys.argv) > 1:
        
        QiifaTest.base_cmd = sys.argv.pop()
    unittest.main()
