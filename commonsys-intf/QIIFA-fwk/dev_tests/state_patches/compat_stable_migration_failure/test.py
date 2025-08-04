
# Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.
# Confidential and Proprietary - Qualcomm Technologies, Inc.

import unittest
import sys
import subprocess
import os
import shutil
sys.path.append("../../")
from util import QiifaTestUtils

class QiifaTest(unittest.TestCase):
    base_cmd = ""
    actual_result = "actual_result"
    qiifa_dir = ""

    def setUp(self):
        CWD = os.path.dirname(os.path.abspath(__file__))
        QiifaTestUtils.cleanup_workspace(CWD)
        build_dir = os.path.join(CWD,"build")

        QiifaTestUtils.find_and_replace_api_metadata(CWD,"class2","class1") # switches back class2 => class1
        QiifaTestUtils.find_and_replace_cd(CWD,"stable","compatible")
        subprocess.check_output("cmake ..",shell=True,cwd=build_dir)
        subprocess.check_output("make ",shell=True,cwd=build_dir)
        command = self.base_cmd + " --golden_ref all" + " --wroot "+CWD
        subprocess.check_output(command,shell=True)

        QiifaTestUtils.find_and_replace_api_metadata(CWD,"class1","class2") # switches class1 => class2
        QiifaTestUtils.find_and_replace_cd(CWD,"compatible","stable")
        subprocess.check_output("cmake ..",shell=True,cwd=build_dir)
        subprocess.check_output("make ",shell=True,cwd=build_dir)
        command = self.base_cmd + " --golden_ref all" + " --wroot "+CWD
        try:
            subprocess.check_output(command,stderr=subprocess.STDOUT, shell=True)
        except subprocess.CalledProcessError as e:
            self.actual_result=str(e.output)

    def test(self):
        self.assertGreater(self.actual_result.find("new state is incompatible"), -1,self.actual_result)

if __name__ == '__main__':
    if len(sys.argv) > 1:
        
        QiifaTest.base_cmd = sys.argv.pop()
    unittest.main()
