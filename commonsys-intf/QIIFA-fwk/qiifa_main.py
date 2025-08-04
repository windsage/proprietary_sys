#!/usr/bin/python
# -*- coding: utf-8 -*-
#Copyright (c) 2019-2020 Qualcomm Technologies, Inc.
#All Rights Reserved.
#Confidential and Proprietary - Qualcomm Technologies, Inc.

'''
Import standard python modules
'''
import time,sys,os,errno,subprocess,argparse,tarfile,importlib

'''
Import plugin_interface base module
'''
import plugin_interface

'''
Import local utilities, constans, logger, arguement modules
'''
from qiifa_util.util import UtilFunctions, Variables, Constants, Logger, Arguments

def load_plugins():
    '''
    Description: dl_load defination dynamically loads modules from "/plugins/" folder
                 plugin filename MUST be _plugin.py
    Return     : List of modules
    '''
    pl_list = []
    root = os.path.dirname(os.path.realpath(__file__)) + "/plugins/"
    for dummy, dir, files in os.walk(root):
        head_tail = os.path.split(dummy)
        for ffile in files:
            if ffile.endswith('_plugin.py'):
                modulename = ffile.strip('.py')
                Logger.logInternal.info("calling for " + modulename)
                pl = None
                try:
                    pl = importlib.import_module('plugins.' + head_tail[1] + '.' + modulename)
                except (NameError, SyntaxError) as ex:
                    Logger.logStdout.info('Module %s cannot be loaded!', modulename)
                    Logger.logStdout.info(ex)
                    pl = None
                    sys.exit()
                pl_list.append(pl)
    return pl_list

def zip_artifacts():
    Logger.logInternal.info ("Creating tar file")
    UtilFunctions.makeTarFile(Constants.qiifa_cmd_tarfile, Constants.qiifa_current_cmd_dir)
    if Constants.qiifa_out_path_target_value == "qssi":
        UtilFunctions.zip_dir(Constants.qiifa_code_path,Constants.qiifa_out_dir+"/QIIFA-fwk.zip")
    Logger.logInternal.info ("Done tar file")

def refresh_artifacts():
    if not UtilFunctions.dirExists(Constants.qiifa_out_dir):
        UtilFunctions.create_dir(Constants.qiifa_out_dir)
    UtilFunctions.rmdir(Constants.qiifa_current_cmd_dir)
    UtilFunctions.create_dir(Constants.qiifa_current_cmd_dir)

def get_plugin_name_from_module(module):
    return str(module.__name__).split(".")[-1]

def main():
    '''
    Description: main function
    '''
    start = time.time()

    argsObj = Arguments()
    args = argsObj.parser.parse_args()

    pl_list = load_plugins()

    '''
    For argument type: --type
    '''
    arg_type = args.type
    arg_create = args.create
    arg_target = args.target
    arg_qssi = args.qssi
    arg_enforced = args.enforced
    arg_test_source = args.test_source
    arg_tp_build = args.techpack_build
    arg_tp_list = args.techpack_names
    _scan_all_component = args.scan_all_component

    if not UtilFunctions.validateArgs(arg_type, arg_create, arg_qssi, arg_target,pl_list,arg_tp_build,arg_tp_list):
        Logger.logStdout.info ("python qiifa_main.py -h\n")
        sys.exit(-1)

    #Identifying if current techpack list contains graphics_tp
    try:
        if ((not (UtilFunctions.pathExists((Constants.paths_of_xml_files)[1]))) or (UtilFunctions.pathExists((Constants.paths_of_xml_files)[1]))) and (len(os.listdir((Constants.paths_of_xml_files)[1])) == 0) and arg_tp_build and (type(arg_tp_list) == str):
            if  "graphics_tp" in arg_tp_list:
                Logger.logStdout.info("paths_of_xml directory was empty for given techpackbuild  %s",(Constants.paths_of_xml_files)[1])
                Logger.logStdout.info("Graphics Techpack (graphics_tp) Detected")
                Constants.graphics_current_techpack = True
            else:
                Logger.logStdout.warning("paths_of_xml  was not found or directory was empty for given techpackbuild  %s",(Constants.paths_of_xml_files)[1])
    except OSError:
        if arg_tp_build and (type(arg_tp_list) == str) and ("graphics_tp" in arg_tp_list):
            Logger.logStdout.warning("paths_of_xml directory does not exist for given techpackbuild  %s",(Constants.paths_of_xml_files)[1])
            Logger.logStdout.info("Graphics Techpack (graphics_tp) Detected")
            Constants.graphics_current_techpack = True
        else:
            Logger.logStdout.warning("OS path not found for techpack only build check : %s",(Constants.paths_of_xml_files)[1])

    if Constants.qiifa_out_dir != "" and arg_type != None:
        refresh_artifacts()

    if arg_enforced == "1":
        Variables.is_enforced = True
    else:
        Logger.logInternal.info("Not Enforced")
        Variables.is_enforced = False
    Variables.is_enforced = True

    Logger.logInternal.info("Checking for argument --type ")
    if arg_type != None and  arg_type[0] == "all" and arg_tp_build:
        for plugin in pl_list:
            if plugin != None:
                plugin_name = get_plugin_name_from_module(plugin)
                plugin.MyPlugin().register()
                plugin.MyPlugin().config()
                if plugin_name == "qiifa_api_management_plugin":
                    plugin.MyPlugin().IIC(arg_test_source = arg_test_source, arg_tp_build=arg_tp_build, scan_all_component=_scan_all_component)
                elif plugin_name != "hidl_plugin":
                     plugin.MyPlugin().IIC(arg_test_source = arg_test_source, arg_tp_build=arg_tp_build)
    elif arg_type != None and  arg_type[0] == "all":
        for plugin in pl_list:
            if plugin != None:
               plugin_name = get_plugin_name_from_module(plugin)
               plugin.MyPlugin().register()
               plugin.MyPlugin().config()
               if plugin_name == "qiifa_api_management_plugin":
                  plugin.MyPlugin().IIC(arg_test_source = arg_test_source, arg_tp_build=arg_tp_build, scan_all_component=_scan_all_component)
               elif plugin_name != "hidl_plugin":
                  plugin.MyPlugin().IIC(arg_test_source = arg_test_source, arg_tp_build=arg_tp_build)
    elif arg_type != None:
        for plugin in pl_list:
            if plugin != None and arg_type != None:
                plugin_name = get_plugin_name_from_module(plugin)
                if arg_type[0] in plugin.MyPlugin().register():
                    plugin.MyPlugin().config()
                    if plugin_name == "qiifa_api_management_plugin":
                        plugin.MyPlugin().IIC(arg_test_source = arg_test_source, arg_tp_build=arg_tp_build, scan_all_component=_scan_all_component)
                    elif plugin_name != "hidl_plugin":
                        kwargs={}
                        kwargs["arg_test_source"]=arg_test_source
                        kwargs["arg_tp_build"]=arg_tp_build
                        if plugin_name == "aidl_plugin":
                            kwargs["iic_args_list"]=arg_type
                        plugin.MyPlugin().IIC(**kwargs)
    if arg_type != None:
        zip_artifacts()

    '''
    For argument type: --create
    '''
    Logger.logInternal.info("Checking for argument --create")
    if arg_create != None and arg_create[0] == "all":
        for plugin in pl_list:
            if plugin != None:
                plugin.MyPlugin().register()
                plugin.MyPlugin().config()
                plugin.MyPlugin().generateGoldenCMD()
    elif arg_create != None:
        created = False
        for plugin in pl_list:
            if plugin != None and arg_create != None:
                ''' only validate the first in arg_create,
                    send the rest down the pipe for further validation'''
                if arg_create[0] in plugin.MyPlugin().register():
                    plugin.MyPlugin().config()
                    kwargs = {"create_args_lst":arg_create}
                    plugin.MyPlugin().generateGoldenCMD(**kwargs)
                    created = True
        if not created:
            Logger.logStdout.info ("python qiifa_main.py -h\n")
            sys.exit(-1)

    '''
    For argument type: --target
    '''
    if arg_qssi is not None and arg_target is not None:
        if not os.path.isfile(arg_target):
            Logger.logStdout.error("target path " + arg_target + " doesn't exist")
            sys.exit(-1)
        elif not os.path.isfile(arg_qssi):
            Logger.logStdout.error("qssi path " + arg_qssi + " doesn't exist")
            sys.exit(-1)
        for plugin in pl_list:
            if plugin != None:
                if "aidl" in plugin.MyPlugin().register() :
                    kwargs = {"qssi_path":arg_qssi, "target_path":arg_target}
                    plugin.MyPlugin().IIC(**kwargs)

    '''
    end of main, exit with sucess
    '''
    if (Variables.is_violator == True):
        Logger.logStdout.error("\tFailure: There is/are interface integrity QIIFA violaters\n")
        Logger.logStdout.error("\tplease look at qiifa_violaters.txt file in out folder\n")
        Logger.logStdout.error("\tfor QSSI only, out/target/product/qssi/QIIFA_QSSI/qiifa_violaters.txt\n")
        Logger.logStdout.error("\tfor target," + Constants.logfileViolators.replace(Constants.croot + "/",'') + "\n")
    else:
        Logger.logStdout.info("\tThere are no QIIFA violators\n")
        Logger.logStdout.info("\tQIIFA: Success\n")

    if (Variables.is_enforced == True and Variables.is_violator == True):
        Logger.logStdout.error("\tQIIFA is in enforcing mode, returning failure to caller\n")
        sys.exit(-1)
    elif (Variables.is_enforced == False and Variables.is_violator == True):
        Logger.logStdout.info("\tThere are QIIFA violaters but QIIFA is not in enforcing mode, returning success to caller\n")

    sys.exit(0)

if __name__ == '__main__':
    main()
