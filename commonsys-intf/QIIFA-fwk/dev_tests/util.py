# Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries. All rights reserved.
# Confidential and Proprietary - Qualcomm Technologies, Inc.

import os
import shutil

CD_ROOT = "qc/loremipsum/cd/"
API_METADATA_FILE_SUFFIX = os.path.join(CD_ROOT,"api_metadata.xml")
CD_FILE_SUFFIX = os.path.join(CD_ROOT,"cd.xml")
GRD_DIR_SUFFIX = os.path.join(CD_ROOT,"golden_reference_dumps")

class QiifaTestUtils():

    @staticmethod
    def remove_artifact(artifact_path):
        if os.path.isdir(artifact_path):
            shutil.rmtree(artifact_path)
        if os.path.isfile(artifact_path):
            os.remove(artifact_path)

    @staticmethod
    def tear_down_artifacts(artifacts_list):
        for artifact in artifacts_list:
            QiifaTestUtils.remove_artifact(artifact)
    
    @staticmethod
    def cleanup_workspace(cwd):
        build_dir = os.path.join(cwd,"build")
        actual_report_path = os.path.join(cwd,"actual_report")
        grd_directory_path = os.path.join(cwd,GRD_DIR_SUFFIX)
        workspace_tmp_artifacts = [build_dir,actual_report_path,grd_directory_path]
        QiifaTestUtils.tear_down_artifacts(workspace_tmp_artifacts)
        os.mkdir(build_dir)

    def cleanup_grd(cwd):
        grd_directory_path = os.path.join(cwd,GRD_DIR_SUFFIX)
        QiifaTestUtils.remove_artifact(grd_directory_path)

    @staticmethod
    def find_and_replace_file_contents(search_text,replace_text,file_path):
        with open(file_path, 'r') as file:
            data = file.read()
            if search_text in data:
                data = data.replace(search_text, replace_text)
            else:
                return
        with open(file_path, 'w') as file:
            file.write(data)

    @staticmethod
    def find_and_replace_api_metadata(root_dir,search_text,replace_text):
        api_metadata_filepath = os.path.join(root_dir,API_METADATA_FILE_SUFFIX)
        QiifaTestUtils.find_and_replace_file_contents(search_text,replace_text,api_metadata_filepath)
    
    @staticmethod
    def find_and_replace_cd(root_dir,search_text,replace_text):
        api_metadata_filepath = os.path.join(root_dir,CD_FILE_SUFFIX)
        QiifaTestUtils.find_and_replace_file_contents(search_text,replace_text,api_metadata_filepath)
      
    @staticmethod
    def find_and_replace_cd_component(root_dir,search_text,replace_text):
        cd_file = os.path.join(root_dir,"qc/display-kernel.lnx/cd/cd.xml")
        QiifaTestUtils.find_and_replace_file_contents(search_text,replace_text,cd_file)

    @staticmethod
    def find_and_replace_apimetatdata_component(root_dir,search_text,replace_text):
        api_metadata_filepath = os.path.join(root_dir,"qc/display-kernel.lnx/cd/api_metadata.xml")
        QiifaTestUtils.find_and_replace_file_contents(search_text,replace_text,api_metadata_filepath)

    def check_directory_contains_subdir(directory_path, subdir_name):
        for dirpath, dirnames, _ in os.walk(directory_path):
            if subdir_name in dirnames:
                return True
        return False
    
    def is_platform_LE(root_dir):
        return check_directory_contains_subdir(root_dir,"rootfs")