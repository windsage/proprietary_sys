# Copyright (c) 2024 Qualcomm Technologies, Inc.
# All Rights Reserved.
# Confidential and Proprietary - Qualcomm Technologies, Inc.

import os
import subprocess

SANDBOX_STATUS_KEY = "ENABLE_SANDBOX"
FRAMEWORK_BASH_CONFIGS_FILEPATH = "qiifa_bash_configs"

LINUX_ANDROID = "LA"
LINUX_EMBEDDED = "LE"

class BashUtils:
    @staticmethod
    def read_bash_variable(file_path,key):
        CMD = 'echo $(source %s; echo $%s)' % (file_path,key)
        byte_stream = subprocess.Popen(CMD, stdout=subprocess.PIPE, shell=True, executable='/bin/bash')
        byte_data = byte_stream.stdout.readlines()[0].strip()
        return byte_data.decode('utf-8')

class PlatformUtils:

    _config_file_suffix = "qiifa_bash_configs"

    @staticmethod
    def get_platform_type():
        platform_version = os.getenv("PLATFORM_VERSION")
        if platform_version == None:
            return LINUX_EMBEDDED
        else:
            return LINUX_ANDROID

    @staticmethod
    def is_qssi_build():
        qcpath = os.getenv("QCPATH")
        return not os.path.isdir(os.path.join(qcpath,'QIIFA-cmd-vendor'))

    @staticmethod
    def get_target_bash_config_filepath():
        qcpath = os.getenv("QCPATH")
        return os.path.join(qcpath,'QIIFA-cmd-vendor',PlatformUtils._config_file_suffix)

    @staticmethod
    def get_framework_bash_config_filepath():
        current_file_directory = os.path.dirname(os.path.abspath(__file__))
        return os.path.join(current_file_directory,PlatformUtils._config_file_suffix)

class ConfigReader:

    _is_sandbox_enabled = False

    def is_sandbox_enabled(self):
        return self._is_sandbox_enabled

    def __init__(self,platform_type):
        self._is_sandbox_enabled = self._get_sandbox_status(platform_type)

    def _get_sandbox_status(self,platform_type):
        framework_sandbox_status = self._get_framework_sandbox_status()
        if platform_type == LINUX_EMBEDDED:
            return framework_sandbox_status
        else:
            if PlatformUtils.is_qssi_build():
                return framework_sandbox_status
            target_sandbox_status = self._get_target_sandbox_status()
            return framework_sandbox_status and target_sandbox_status

    def _get_framework_sandbox_status(self):
        framework_bash_config_file_path = PlatformUtils.get_framework_bash_config_filepath()
        framework_sandbox_status = BashUtils.read_bash_variable(framework_bash_config_file_path,SANDBOX_STATUS_KEY)
        return framework_sandbox_status == '1'

    def _get_target_sandbox_status(self):
        target_config_filepath = PlatformUtils.get_target_bash_config_filepath()
        if not os.path.exists(target_config_filepath):
            return '1'
        target_sandbox_status = BashUtils.read_bash_variable(target_config_filepath,SANDBOX_STATUS_KEY)
        return target_sandbox_status == '1'
