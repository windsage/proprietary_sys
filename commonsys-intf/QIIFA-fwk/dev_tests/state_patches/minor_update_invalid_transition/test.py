
# Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.
# Confidential and Proprietary - Qualcomm Technologies, Inc.

import unittest
import sys
import subprocess
import os
import shutil

class QiifaTest(unittest.TestCase):
    base_cmd = ""
    actual_result = "actual_result"
    qiifa_dir = ""

    def setUp(self):
        command = self.base_cmd + " --iic enforce --cr "+self.actual_result
        if os.path.isdir("build"):
            shutil.rmtree("build")
        os.mkdir("build")
        subprocess.check_output("cmake ..",shell=True,cwd="build")
        subprocess.check_output("make -C build",shell=True)
        try:
            subprocess.check_output(command,stderr=subprocess.STDOUT, shell=True)
        except subprocess.CalledProcessError as e:
            error_msg = e.output
            print(error_msg.decode())
            self.actual_result=error_msg.decode()

    def test(self):
        self.assertGreater(self.actual_result.find("Minor revision only allowed in stable state"), -1,self.actual_result)

if __name__ == '__main__':
    if len(sys.argv) > 1:
        
        QiifaTest.base_cmd = sys.argv.pop()
    unittest.main()
