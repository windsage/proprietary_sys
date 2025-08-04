
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

        QiifaTestUtils.find_and_replace_cd(CWD,"stable","development")
        QiifaTestUtils.find_and_replace_api_metadata(CWD,"class2","class1") # switches back class2 => class1
        subprocess.check_output("cmake ..",shell=True,cwd=build_dir)
        subprocess.check_output("make ",shell=True,cwd=build_dir)
        # generate first GRD in Dev
        command = self.base_cmd + " --golden_ref all" + " --wroot "+CWD
        subprocess.check_output(command,shell=True)
        QiifaTestUtils.find_and_replace_cd(CWD,"development","stable")
        # Update GRD to compatible state
        subprocess.check_output(command,shell=True)

        # Add Incompatible change
        QiifaTestUtils.find_and_replace_api_metadata(CWD,"class1","class2") # switches class1 => class2
        subprocess.check_output("cmake ..",shell=True,cwd=build_dir)
        subprocess.check_output("make ",shell=True,cwd=build_dir)
        command = self.base_cmd + " --iic enforce --cr actual_report" + " --wroot "+CWD

        try:
            subprocess.check_output(command,stderr=subprocess.STDOUT, shell=True)
        except subprocess.CalledProcessError as e:
            self.actual_result=str(e.output)

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
