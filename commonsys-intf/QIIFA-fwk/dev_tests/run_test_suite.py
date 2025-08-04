# Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.
# Confidential and Proprietary - Qualcomm Technologies, Inc.

import os
import sys
import json
import subprocess

API_MANAGEMENT_PATH = "/plugins/qiifa_api_management/qiifa_api_management.py"
cmd_data = None
TEST_DIR = os.path.dirname(os.path.abspath(__file__))
QIIFA_ROOT = os.path.dirname(TEST_DIR)
CMD_ARGS_FILE_PATH = os.path.join(TEST_DIR,"cmd_args.json")

def init_commad_params():
    file_handler = open(CMD_ARGS_FILE_PATH,"r")
    file_data = file_handler.read()
    file_handler.close()
    global BASE_CMD
    global cmd_data
    cmd_data = json.loads(file_data)
    BASE_CMD = '"'+cmd_data["python-dir"]+' '
    BASE_CMD += QIIFA_ROOT + API_MANAGEMENT_PATH
    BASE_CMD += " --qiifa_tools "+cmd_data["qiifa_tools"]


def test_all():
    init_commad_params()
    dir_list = os.listdir(os.path.join(TEST_DIR,"state_patches"))
    skip_list = ["dev_stable_migration_failure","compat_stable_migration_failure","dlkm_brk","major_update","major_update_cleanup_check","minor_update","minor_update_invalid_transition"]
    success_list = []
    failed_list = []
    for dir in dir_list:
        if 1 and str(dir) not in skip_list:
            test_dir = os.path.join(TEST_DIR,"state_patches",str(dir))
            test_script_path =  os.path.join(test_dir,"test.py")
            symbol_path = os.path.join(test_dir,cmd_data["symbol"])
            test_command = cmd_data["python-dir"] +' '+test_script_path+' '+BASE_CMD
            if str(dir).find("LA") > -1: 
                test_command += " --platform  LA"
                test_command += " --symbol "+symbol_path+'"'
            elif str(dir).find("LE") > -1:
                test_command += " --platform  LE"
                test_command += " --rootfs "+cmd_data["rootfs"]+'"'
            else:
                test_command += " --platform  LA"
                test_command += " --symbol "+symbol_path+'"'
            try:
                print("Testing ::"+str(dir))
                output = subprocess.check_output(test_command,shell=True,cwd=test_dir)
                success_list.append(str(dir))
            except subprocess.CalledProcessError as e:
                print("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx Failed for test : "+str(dir)+" xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
                print("Test Command : ",test_command)
                print(str(e.output))
                failed_list.append(str(dir))
        else:
            print("Skipping ::"+str(dir))
    if len(failed_list) == 0:
        print("All test passed")
    else:
        print("********************************************************************************************************************")
        print("----------------------------------------------- PASSED tests -------------------------------------------------------")
        print("********************************************************************************************************************")
        for test in success_list:
            print(test)
        print("********************************************************************************************************************")
        print("----------------------------------------------- FAILED tests -------------------------------------------------------")
        print("********************************************************************************************************************")
        for test in failed_list:
            print(test)
        print("********************************************************************************************************************")
        sys.exit(1)

def main():
    test_all()

if __name__ == "__main__":
    main()
