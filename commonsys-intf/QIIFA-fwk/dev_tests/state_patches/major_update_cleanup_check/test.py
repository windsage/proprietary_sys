
# Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.
# Confidential and Proprietary - Qualcomm Technologies, Inc.

import unittest
import sys
import os
import subprocess
import shutil
sys.path.append("../../")
from util import QiifaTestUtils

class QiifaTest(unittest.TestCase):

    base_cmd = ""
    expected_result = "expected_result"
    actual_result = "actual_result"
    qiifa_dir = ""

    def setUp(self):
        CWD = os.path.dirname(os.path.abspath(__file__))
        QiifaTestUtils.cleanup_workspace(CWD)
        build_dir = os.path.join(CWD,"build")

        QiifaTestUtils.find_and_replace_cd(CWD,"3.0.0","2.5.0")
        subprocess.check_output("cmake ..",shell=True,cwd=build_dir)
        subprocess.check_output("make ",shell=True,cwd=build_dir)
        # generate first GRD in 2.5.0
        command = self.base_cmd + " --golden_ref all" + " --wroot "+CWD
        subprocess.check_output(command,shell=True)

        
        QiifaTestUtils.find_and_replace_cd(CWD,"2.5.0","3.0.0")
        # subprocess.check_output(command,shell=True)
        # Update version 3.0.0
        command = self.base_cmd + " --iic enforce --cr actual_report" + " --wroot "+CWD

        try:
            subprocess.check_output(command,stderr=subprocess.STDOUT, shell=True)
        except subprocess.CalledProcessError as e:
            self.actual_result=str(e.output)

    def test(self):
        self.assertTrue(str(self.actual_result).__contains__("Major update detected"),self.actual_result)

if __name__ == '__main__':
    if len(sys.argv) > 1:
        
        QiifaTest.base_cmd = sys.argv.pop()
    unittest.main()
