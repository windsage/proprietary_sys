
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
        CWD = os.path.dirname(os.path.abspath(__file__))
        command = self.base_cmd + " --iic enforce --cr actual_report" + " --wroot "+CWD
        if os.path.isdir("build"):
            shutil.rmtree("build")
        os.mkdir("build")
        subprocess.check_output("cmake ..",shell=True,cwd="build")
        subprocess.check_output("make -C build",shell=True)
        try:
            self.actual_result=subprocess.check_output(command,stderr=subprocess.STDOUT, shell=True)
        except subprocess.CalledProcessError as e:
            self.actual_result=str(e.output)

    def test(self):
        self.assertGreater(self.actual_result.find("File not accessible/present"), -1,self.actual_result)


if __name__ == '__main__':
    if len(sys.argv) > 1:
        
        QiifaTest.base_cmd = sys.argv.pop()
    unittest.main()
