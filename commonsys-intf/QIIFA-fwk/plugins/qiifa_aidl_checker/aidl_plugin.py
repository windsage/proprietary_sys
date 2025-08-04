#!/usr/bin/python
# -*- coding: utf-8 -*-
#Copyright (c) 2021 Qualcomm Technologies, Inc.
#All Rights Reserved.
#Confidential and Proprietary - Qualcomm Technologies, Inc.

try:
#    Import standard python modules
    import sys,os,json,shutil,traceback
#   Import local utilities
    from qiifa_util.util import UtilFunctions, Variables, Constants, Logger
#   Import plugin_interface base module
    from plugin_interface import plugin_interface
    import plugins.qiifa_hidl_checker.xmltodict as xmltodict
    from collections import OrderedDict
    from datetime import datetime
    import copy
    from zipfile import ZipFile
    import glob

except Exception as e:
    traceback.print_exc()
    Logger.logStdout.error(e)
    Logger.logStdout.error("Please check the Import library. Please contact qiifa-squad")
    sys.exit(1)


sys.dont_write_bytecode = True

LOG_TAG = "aidl_plugin"
MODULE_INFO_DICT ={}
AIDL_METADATA_DICT = {}
AIDL_SKIPPED_LST = {}
PLUGIN_STATE_WARNING = False
FCM_GLOBAL_DICT={"device":"device"}
FCM_XML_DICT = {}
FCM_MAX_VAL = 0
FCM_MIN_VAL = 100
PFV_MIN_VAL = 0
FCM_MAX_KEY = "0"
FCM_VENDOR_TL = 0
FCM_MAX_VENDOR_TL_LST = []
QSSI_AIDL_NOTFND = []
DEVICES_FCM_MAX_KEY = ""
XML_HAL_NOTFND = []
MD_HAL_NOTFND = {}
VNDR_HAL_NOTFND = {}
QIIFA_AIDL_CHIPSET_DVLPMNT_ENBL = []

def CreateRootDir(OPDirPath):
    '''
    Generate the path by creating directories till the end of the path is reached.
    '''
    if not (os.path.isdir(OPDirPath)):
        os.mkdir(OPDirPath)

def plugin_disabled():
    if (Constants.aidl_plugin_state == "disabled"):
        return True

def check_plugin_state():
     global PLUGIN_STATE_WARNING
     supported_values = ["disabled","enforced","warning"]
     supported = False
     if(Constants.aidl_plugin_state=="warning"):
         PLUGIN_STATE_WARNING = True
     for value in supported_values:
        if(Constants.aidl_plugin_state == value):
             supported = True
             break
     return supported

def JSONLoadtoDict(path):
    '''
    Parse the JSON file given in the path into a list of dictionaries.
    '''
    info_dict = [] #The json file would always be a list of dictionaries
    filename = os.path.basename(path)
    try:
        info_file_handle = open(path, "r")
        info_dict = json.load(info_file_handle)
        info_file_handle.close()
    except Exception as e:
        traceback.print_exc()
        Logger.logStdout.error(e)
        reason = "AIDL file is not present in "+path
        if PLUGIN_STATE_WARNING:
            UtilFunctions.print_violations_on_stdout(LOG_TAG,filename,JSONLoadtoDict.__name__,reason,False)
        else:
            UtilFunctions.print_violations_on_stdout(LOG_TAG,filename,JSONLoadtoDict.__name__,reason)
            return
    return info_dict

def chkDupinDict(dictlst,path):
    '''
    Find if the list of dictionaries have any duplicates
    and stop running if any
    '''
    if len(dictlst) >0:
        compdictlst=[]
        for dict in dictlst:
            if dict not in compdictlst:
                found_dupl=False
                if len(compdictlst) > 0:
                    for comdict in compdictlst:
                        if comdict[u'name'] == dict[u'name']:
                            found_dupl=True
                if not found_dupl:
                    compdictlst.append(dict)
            else:
                reason="Duplicate interface have been found in the file present in this path "+path+". Please remove duplicates before proceeding"
                if PLUGIN_STATE_WARNING:
                    UtilFunctions.print_violations_on_stdout(LOG_TAG,dict[u'name'],chkDupinDict.__name__,reason,False)
                else:
                    UtilFunctions.print_violations_on_stdout(LOG_TAG,dict[u'name'],chkDupinDict.__name__,reason)
                    sys.exit(1)


def CheckIntfinMtDt(metadatadict_lst, intf_name):
    intffound=False
    founddict={}
    for metadata_dict in metadatadict_lst:
        if str(metadata_dict[u'name']) == intf_name:
            intffound=True
            founddict = metadata_dict
    return intffound,founddict

def mapHashVerAIDLMtdt(metadata_dict,module_dict, arg_intf_name=None):
    '''
    Correlating the version and hash information from
    AIDL metadata JSON file and manually verifing if the frozen hashes
    are present in the corresponding AIDL interface(aidl_api) folders using the
    path from the modules info json file. The information is then updated as a
    dictionary in the hashes and the development hashes node in the
    aidl meta data dict which is compiled and saved for QIIFA CMD
    '''
    global MD_HAL_NOTFND
    intf_path = ""
    intf_name = ""
    hal_found = False
    try:
        for hal in metadata_dict:
            if arg_intf_name != None and arg_intf_name == hal[u'name']:
                hal_found = True
            MD_HAL_NOTFND[hal[u'name']] = hal[u'name']
            hash_dict={}
            hash_max_val=0
            intf_name = hal["name"]
            if u'hashes' in hal.keys():
                if len(hal[u'hashes']) > 0 and type(hal[u'hashes']) == list and u'path' in hal.keys() and len(hal[u'path']) > 0:
                    intf_path = hal["path"][0]
                    intf_path = os.path.join(Constants.croot,intf_path)
                    for root,dirs,files in os.walk(intf_path):
                        for file in files:
                            if file == ".hash":
                                split_root = root.split("/")
                                hash_fd = open(os.path.join(root,file),'r')
                                finalhash = hash_fd.readlines()
                                finalhash = [hashes.replace("\n","") for hashes in finalhash]
                                finalhash = finalhash[0]
                                if finalhash in hal[u'hashes']:
                                    hash_dict[split_root[-1]] = finalhash
                                    hash_max_val = max(int(split_root[-1]),hash_max_val)
                                    break
                    hal[u'hashes'] = hash_dict
            else:
                reason = "Hashes is not present in AIDL Metadata JSON. Please compile again or contact qiifa-squad for more information"
                if PLUGIN_STATE_WARNING:
                    UtilFunctions.print_violations_on_stdout(LOG_TAG,hal["name"],mapHashVerAIDLMtdt.__name__,reason,False)
                else:
                    UtilFunctions.print_violations_on_stdout(LOG_TAG,hal["name"],mapHashVerAIDLMtdt.__name__,reason)
                    sys.exit(1)

        if arg_intf_name != None and not hal_found:
            reason = "Interface information / Hashes not present in METADATA. Please compile again or contact qiifa-squad for more information"
            if PLUGIN_STATE_WARNING:
                UtilFunctions.print_violations_on_stdout(LOG_TAG,hal["name"],mapHashVerAIDLMtdt.__name__,reason,False)
            else:
                UtilFunctions.print_violations_on_stdout(LOG_TAG,hal["name"],mapHashVerAIDLMtdt.__name__,reason)
                return

    except Exception as e:
        traceback.print_exc()
        Logger.logStdout.error(e)
        reason = "Hashes is not present in AIDL Metadata JSON. Please compile again or contact qiifa-squad for more information"
        if PLUGIN_STATE_WARNING:
            UtilFunctions.print_violations_on_stdout(LOG_TAG,intf_name,mapHashVerAIDLMtdt.__name__,reason,False)
        else:
            UtilFunctions.print_violations_on_stdout(LOG_TAG,intf_name,mapHashVerAIDLMtdt.__name__,reason)
        sys.exit(0)

def mapPathtoAIDLmtdt(metadata_dict, module_dict, intf_name=None):
    '''
    Identifying if the path for all the interfaces is same in
    the module info file and then collating the path and adding
    the path to aidl metadata.
    '''
    found_intf = False
    for aidl_intf_metadata in metadata_dict:
        if intf_name != None and (aidl_intf_metadata[u'name'] == intf_name or aidl_intf_metadata[u'types'][0].startswith(intf_name)):
            found_intf = True
        if intf_name == None or found_intf:
            path_set = set()
            path_aidl_exist=False
            for moduleinfo_key in module_dict.keys():
                if str(moduleinfo_key).startswith(str(aidl_intf_metadata[u'name'])+"-") and not path_aidl_exist:
                    if u'path' in (module_dict[moduleinfo_key]).keys() and len(module_dict[moduleinfo_key][u'path']) > 0 and type (module_dict[moduleinfo_key][u'path']) is list:
                        for path in module_dict[moduleinfo_key][u'path']:
                            if ("interface" in path.lower() or "intf" in  path.lower() or "aidl" in path.lower()) and "hidl" not in path.lower():
                                for root,dirs,files in os.walk(os.path.join(Constants.croot,path)):
                                    for file in files:
                                        if file.endswith(".aidl"):
                                            path_aidl_exist = True
                                            break

                                if path_aidl_exist:
                                    path_set.add(str(path))
                                    break
            aidl_intf_metadata[u'path'] = list(path_set)

def mapPlatformVerMtdtVer(metadata_dict, intf_name=None):
    found_intf = False
    goldencmd_dict = JSONLoadtoDict(Constants.aidl_qiifa_cmd_device)
    platform_version = Constants.platform_version
    try:
        if not platform_version.isdigit():
            current_year = datetime.now().year
            vintfkey = int(str(current_year)+"04")
            platform_version = Constants.VINTF_PLATVER_MAP[vintfkey]
        if type(platform_version) != int and platform_version.isdigit():
            platform_version = int(platform_version)
        if platform_version < 15:
            platform_version = 15
        for aidl_intf_metadata in metadata_dict:
            glden_cmd_intf = False
            if intf_name != None and aidl_intf_metadata[u'name'] == intf_name:
                found_intf = True
            else:
                found_intf = False
            if intf_name == None or found_intf:
                version_platformver_dict = {}
                if len(goldencmd_dict)>0:
                    for golden_cmd in goldencmd_dict:
                        if aidl_intf_metadata[u'name'] == golden_cmd[u'name']:
                            if "version_pfv_map" in golden_cmd.keys():
                                for version in aidl_intf_metadata[u'versions']:
                                    if str(version) not in golden_cmd["version_pfv_map"].keys():
                                        version_platformver_dict[version]=platform_version
                                    else:
                                        version_platformver_dict[version]=golden_cmd["version_pfv_map"][str(version)]
                            else:
                                for version in aidl_intf_metadata[u'versions']:
                                    version_platformver_dict[version]=platform_version
                            aidl_intf_metadata["version_pfv_map"] = version_platformver_dict
                            glden_cmd_intf = True
                    if not glden_cmd_intf:
                        if u'versions' in aidl_intf_metadata.keys() and len(aidl_intf_metadata) > 0:
                            for version in aidl_intf_metadata[u'versions']:
                                version_platformver_dict[version]=platform_version
                            aidl_intf_metadata["version_pfv_map"] = version_platformver_dict
                else:
                    if u'versions' in aidl_intf_metadata.keys() and len(aidl_intf_metadata) > 0:
                        for version in aidl_intf_metadata[u'versions']:
                            version_platformver_dict[version]=platform_version
                        aidl_intf_metadata["version_pfv_map"] = version_platformver_dict

                chkVerDep(aidl_intf_metadata,platform_version,found_intf)
            if found_intf:
                break
    except Exception as e:
        traceback.print_exc()
        Logger.logStdout.error(e)
        reason = "Issues with Platform version with map function. Please contact qiifa-squad for more information"
        if PLUGIN_STATE_WARNING:
            UtilFunctions.print_violations_on_stdout(LOG_TAG,intf_name,mapPlatformVerMtdtVer.__name__,reason,False)
        else:
            UtilFunctions.print_violations_on_stdout(LOG_TAG,intf_name,mapPlatformVerMtdtVer.__name__,reason)
        sys.exit(0)

def mapDeviceMatrixMtdtType(metadata_dict):
    for directory in Constants.paths_of_xml_files:
        for root,dirs,files in os.walk(directory):
            for xml_file in files:
                if xml_file.startswith("compatibility_matrix.device"):
                    xml_fd = open(os.path.join(root,xml_file))
                    xml_files_dict = xmltodict.parse(xml_fd.read())
                    for matrix_key in xml_files_dict.keys():
                        if u'hal' in xml_files_dict[matrix_key].keys():
                            for xmldevice_dict in xml_files_dict[matrix_key][u'hal']:
                                for mtdt_dict in metadata_dict:
                                    if mtdt_dict[u'name'].startswith("vendor.") and mtdt_dict[u'name'] != xmldevice_dict[u'name'] and (mtdt_dict[u'types'][0]).startswith(xmldevice_dict[u'name']) and xmldevice_dict[u'@format'] == 'aidl':
                                        mtdt_dict[u'name'] = xmldevice_dict[u'name']
                                        break

def initAIDLGlobalPluginVars(intf_name=None):
    '''
    This function is meant to initialize global metadata
    which will be used in enforcement.
    Primarily planning to load information from :
    1. aidl_metadata.json (Contains the list of HAL;s)
    2. module_info.json (Module <-> Project mapping)
    3. AIDL Skipped List Dictionary: Getting a list of modules
       which will be skipped by AIDL
    4. Correlate the corresponding hash value and version and
       update the aidl metadata dictionary
    5. Append the path information for each hash to the metadata information.
    '''
    global AIDL_METADATA_DICT, MODULE_INFO_DICT, AIDL_SKIPPED_LST, QIIFA_AIDL_CHIPSET_DVLPMNT_ENBL
    AIDL_METADATA_DICT = JSONLoadtoDict(Constants.aidl_metadata_file_path)
    MODULE_INFO_DICT   = JSONLoadtoDict(Constants.module_info_file_path)
    AIDL_SKIPPED_LST   = JSONLoadtoDict(Constants.aidl_skipped_intf)
    if Constants.qiifa_out_path_target_value != "qssi" and UtilFunctions.pathExists(Constants.qiifa_aidl_dvlpmnt_config_notif) == True:
        QIIFA_AIDL_CHIPSET_DVLPMNT_ENBL = JSONLoadtoDict(Constants.qiifa_aidl_dvlpmnt_config_notif)
        QIIFA_AIDL_CHIPSET_DVLPMNT_ENBL = QIIFA_AIDL_CHIPSET_DVLPMNT_ENBL[u'CHIPSET_NAME']
    mapPathtoAIDLmtdt(AIDL_METADATA_DICT,MODULE_INFO_DICT,intf_name)
    mapDeviceMatrixMtdtType(AIDL_METADATA_DICT)
    mapHashVerAIDLMtdt(AIDL_METADATA_DICT,MODULE_INFO_DICT,intf_name)
    mapPlatformVerMtdtVer(AIDL_METADATA_DICT,intf_name)  
    
def GenVndrTarLvlPlatVersMap(vendorxml_path):
    global FCM_GLOBAL_DICT,FCM_MAX_KEY,FCM_VENDOR_TL, FCM_MAX_VENDOR_TL_LST
    filename = ""
    try:
        for root,dirs,files in os.walk(vendorxml_path):
            for file in files:
                if file.endswith(".xml"):
                    file_path = os.path.join(root,file)
                    xml_fd = open(file_path)
                    filename = file
                    try:
                        xml_files_dict = xmltodict.parse(xml_fd.read())
                        if len(xml_files_dict)>0:
                            if type(xml_files_dict) is OrderedDict or type(xml_files_dict) is dict:
                                for key in xml_files_dict.keys():
                                    if type(xml_files_dict[key]) is OrderedDict or type(xml_files_dict[key]) is dict:
                                        for key_xml in xml_files_dict[key].keys():
                                            if key_xml == u'@target-level':
                                                FCM_GLOBAL_DICT[str(xml_files_dict[key][u'@target-level'])]=int(xml_files_dict[key][u'@target-level'])
                                                FCM_MAX_KEY = str(xml_files_dict[key][u'@target-level'])
                                                FCM_VENDOR_TL = FCM_MAX_KEY
                                                break
                    except Exception as e:
                        traceback.print_exc()
                        reason = "Something is wrong with the xml file "+file_path
                        Logger.logStdout.error(e)
                        if PLUGIN_STATE_WARNING:
                            UtilFunctions.print_violations_on_stdout(LOG_TAG,filename,GenVndrTarLvlPlatVersMap.__name__,reason,False)
                        else:
                            UtilFunctions.print_violations_on_stdout(LOG_TAG,filename,GenVndrTarLvlPlatVersMap.__name__,reason)
                            sys.exit(1)
        for root,dirs,files in os.walk(Constants.qiifa_aidl_db_root):
            for file in files:
                filename = file
                if file.startswith("qiifa_aidl_cmd_"):
                    fname_replace = (file.replace(".json","")).replace("qiifa_aidl_cmd_","qiifa_aidl_cmd.")
                    fnam_split = fname_replace.split(".")
                    if fnam_split[-1].isdigit() and FCM_MAX_KEY.isdigit() and int(fnam_split[-1]) >= int(FCM_MAX_KEY):
                        if int(fnam_split[-1]) == int(FCM_MAX_KEY):
                            FCM_VENDOR_TL = int(fnam_split[-1])

        for root,dirs,files in os.walk(Constants.qiifa_vndr_cmptmat_path):
            for file in files:
                if file.startswith("compatibility_matrix.") and file.endswith("xml"):
                    filename = (file.split("."))[1]
                    if filename.isdigit() and int(filename) > int(FCM_VENDOR_TL):
                        FCM_MAX_VENDOR_TL_LST.append(int(filename))

        removeIncompatFCMVer(FCM_GLOBAL_DICT,FCM_MAX_KEY)
        FindMinMaxFCMDict(FCM_GLOBAL_DICT)
    except Exception as e:
        reason = "Something is wrong with the xml file "+file_path
        traceback.print_exc()
        Logger.logStdout.error(e)
        if PLUGIN_STATE_WARNING:
            UtilFunctions.print_violations_on_stdout(LOG_TAG,filename,GenVndrTarLvlPlatVersMap.__name__,reason,False)
        else:
            UtilFunctions.print_violations_on_stdout(LOG_TAG,filename,GenVndrTarLvlPlatVersMap.__name__,reason)
            sys.exit(1)

def GenQSSIMaxFCMPlatVersMap(file_path):
    global FCM_GLOBAL_DICT,FCM_MAX_KEY,DEVICES_FCM_MAX_KEY,PFV_MIN_VAL
    root,dirs,files = next(os.walk(file_path))
    for xml_file in files:
        if xml_file.startswith("compatibility_matrix."):
            split_name = xml_file.split('.')
            if split_name[1].isdigit():
               if int(split_name[1]) > int(FCM_MAX_KEY):
                   FCM_MAX_KEY = split_name[1]
               if int(split_name[1]) >0 and int(split_name[1]) >= Constants.AIDL_MIN_SUPPORT_VERSION:
                   FCM_GLOBAL_DICT[split_name[1]]= int(split_name[1])
    removeIncompatFCMVer(FCM_GLOBAL_DICT,FCM_MAX_KEY)
    FindMinMaxFCMDict(FCM_GLOBAL_DICT)
    for pfv_key in Constants.VINTF_PLATVER_MAP.keys():
        if FCM_MAX_VAL == pfv_key:
            PFV_MIN_VAL = Constants.VINTF_PLATVER_MAP[pfv_key]
            break
    if PFV_MIN_VAL < Constants.AIDL_MIN_PLATFORM_VERSION:
        PFV_MIN_VAL = Constants.AIDL_MIN_PLATFORM_VERSION

def removeIncompatFCMVer(FCM_GLOBAL_DICT,FCM_MAX_KEY):
    try:
        allowedminver = Constants.VINTF_PLATVER_MAP[int(FCM_MAX_KEY)] - Constants.AIDL_NO_OF_PREV_VERS_SUPP
        for key in Constants.VINTF_PLATVER_MAP.keys():
            if Constants.VINTF_PLATVER_MAP[key] <= allowedminver and str(key) in FCM_GLOBAL_DICT.keys():
                del FCM_GLOBAL_DICT[str(key)]
    except Exception as e:
        traceback.print_exc()
        Logger.logStdout.error(e)
        reason = "The QIIFA CMD or the compatibility matrix is not present. Please check or contact qiifa-squad Please check go/qiifa for more information"
        if PLUGIN_STATE_WARNING:
            UtilFunctions.print_violations_on_stdout(LOG_TAG,"aild metadata",removeIncompatFCMVer.__name__,reason,False)
        else:
            UtilFunctions.print_violations_on_stdout(LOG_TAG,"aidl_metadata",removeIncompatFCMVer.__name__,reason)
def FindMinMaxFCMDict(fcm_dict):
    global FCM_MAX_VAL,FCM_MIN_VAL
    try:
        for key in fcm_dict:
            if not key.startswith("device"):
                FCM_MAX_VAL = max(fcm_dict[key],FCM_MAX_VAL)
                FCM_MIN_VAL = min(fcm_dict[key],FCM_MIN_VAL)
    except Exception as e:
        traceback.print_exc()
        Logger.logStdout.error(e)
        reason = "The QIIFA CMD or the compatibility matrix is not present. Please check or contact qiifa-squad Please check go/qiifa for more information"
        if PLUGIN_STATE_WARNING:
            UtilFunctions.print_violations_on_stdout(LOG_TAG,"aild metadata",removeIncompatFCMVer.__name__,reason,False)
        else:
            UtilFunctions.print_violations_on_stdout(LOG_TAG,"aidl_metadata",removeIncompatFCMVer.__name__,reason)

def check_xml_dup_auth_cnfm(hal,duplicate_hal):
    skip_same_keys = [u'version',u'interface']
    if hal.keys() == duplicate_hal.keys():
        for key in hal.keys():
            if key not in skip_same_keys and hal[key] != duplicate_hal[key]:
                reason = "Please verify the XML tag "+key+", For the Same XML interface "+hal['name']+" version and instance name can be different and all the other tags should be the same"
                UtilFunctions.print_violations_on_stdout("AIDL Plugin",hal['name'],check_xml_dup_auth_cnfm.__name__,reason)
                sys.exit(1)
    else:
        reason = "Please verify the XML File, For the Same XML interface "+hal['name']+" version and instance name can be different and all the other tags should be the same"
        UtilFunctions.print_violations_on_stdout("AIDL Plugin",hal['name'],check_xml_dup_auth_cnfm.__name__,reason)
        sys.exit(1)

def UpdXmltoDict(FCM_MASTER_DICT,FCM_ROOT_PATH,XML_INTF_INFO=None,TARGET_LEVEL=0):
    '''
    Read the XML entries and add them to dictionaries
    '''
    global FCM_XML_DICT
    if u'manifest' not in FCM_XML_DICT.keys():
        FCM_XML_DICT["manifest"]=[]
    for root,dirs,files in os.walk(FCM_ROOT_PATH):
        try:
            for xml_file in files:
                xml_file_path = os.path.join(root, xml_file)
                split_xml_fn = xml_file.split(".xml")
                split_xml_fn = split_xml_fn[0]
                xml_hal_lst_dict = {}
                if xml_file.startswith("compatibility_matrix."):
                    split_name = xml_file.split('.')
                    if split_name[1] in FCM_MASTER_DICT:
                        fd = open(xml_file_path)
                        xml_files_dict = xmltodict.parse(fd.read())
                        FCM_XML_DICT[xml_file_path] = []
                        if type(xml_files_dict) is OrderedDict or type(xml_files_dict) is dict:
                            if 'compatibility-matrix' in xml_files_dict.keys():
                                if type(xml_files_dict['compatibility-matrix']['hal']) is OrderedDict or type(xml_files_dict['compatibility-matrix']['hal']) is dict:
                                    if '@format' in xml_files_dict['compatibility-matrix']['hal'].keys() and xml_files_dict['compatibility-matrix']['hal']['@format'] == 'aidl' and XML_INTF_INFO==None:
                                        if xml_files_dict['compatibility-matrix']['hal'][u'name'] not in xml_hal_lst_dict.keys():
                                            FCM_XML_DICT[xml_file_path].append(xml_files_dict['compatibility-matrix']['hal'])
                                            xml_hal_lst_dict[xml_files_dict['compatibility-matrix']['hal'][u'name']] = xml_files_dict['compatibility-matrix']['hal']
                                        else:
                                            check_xml_dup_auth_cnfm(xml_files_dict['compatibility-matrix']['hal'],xml_hal_lst_dict[xml_files_dict['compatibility-matrix']['hal'][u'name']])
                                    elif '@format' in xml_files_dict['compatibility-matrix']['hal'].keys() and xml_files_dict['compatibility-matrix']['hal']['@format'] == 'aidl' and XML_INTF_INFO==xml_files_dict['compatibility-matrix']['hal'][u'name']:
                                        FCM_XML_DICT[xml_file_path].append(xml_files_dict['compatibility-matrix']['hal'])
                                elif type(xml_files_dict['compatibility-matrix']['hal']) is list:
                                    for hal_list in xml_files_dict['compatibility-matrix']['hal']:
                                        if '@format' in hal_list.keys() and hal_list['@format'] == 'aidl' and XML_INTF_INFO==None:
                                            if hal_list[u'name'] not in xml_hal_lst_dict.keys():
                                                FCM_XML_DICT[xml_file_path].append(hal_list)
                                                xml_hal_lst_dict[hal_list[u'name']] = hal_list
                                            else:
                                                check_xml_dup_auth_cnfm(hal_list,xml_hal_lst_dict[hal_list[u'name']])
                                        elif '@format' in hal_list.keys() and hal_list['@format'] == 'aidl' and hal_list['name']==XML_INTF_INFO:
                                            FCM_XML_DICT[xml_file_path] = hal_list
                        if split_name[1].isdigit() and TARGET_LEVEL > 0 and TARGET_LEVEL > int(split_name[1]):
                            del FCM_XML_DICT[xml_file_path]
                        elif len(FCM_XML_DICT[xml_file_path]) == 0:
                            del FCM_XML_DICT[xml_file_path]
                        fd.close()
                else:
                    fd = open(xml_file_path)
                    xml_files_dict = xmltodict.parse(fd.read())
                    if type(xml_files_dict) is OrderedDict or type(xml_files_dict) is dict:
                        if 'manifest' in xml_files_dict.keys():
                            if (type(xml_files_dict['manifest']) is OrderedDict or type(xml_files_dict['manifest']) is dict) and u'hal' in xml_files_dict['manifest'].keys():
                                if type(xml_files_dict['manifest']['hal']) is OrderedDict or type(xml_files_dict['manifest']['hal']) is dict:
                                    if '@format' in xml_files_dict['manifest']['hal'].keys() and xml_files_dict['manifest']['hal']['@format'] == 'aidl' and XML_INTF_INFO==None:
                                        FCM_XML_DICT["manifest"].append(xml_files_dict['manifest']['hal'])
                                    elif '@format' in xml_files_dict['manifest']['hal'].keys() and xml_files_dict['manifest']['hal']['@format'] == 'aidl' and XML_INTF_INFO==xml_files_dict['manifest']['hal']['name']:
                                        FCM_XML_DICT["manifest"].append(xml_files_dict['manifest']['hal'])
                                elif type(xml_files_dict['manifest']['hal']) is list:
                                    for hal_list in xml_files_dict['manifest']['hal']:
                                        if '@format' in hal_list.keys() and hal_list['@format'] == 'aidl' and XML_INTF_INFO==None:
                                            FCM_XML_DICT["manifest"].append(hal_list)
                                        elif '@format' in hal_list.keys() and hal_list['@format'] == 'aidl' and XML_INTF_INFO==hal_list['name']:
                                            FCM_XML_DICT["manifest"].append(hal_list)
                    fd.close()
        except Exception as e:
            traceback.print_exc()
            reason = "Please verify the XML File, invalid syntax has been used."
            Logger.logStdout.error(e)
            UtilFunctions.print_violations_on_stdout("AIDL Plugin",xml_file,UpdXmltoDict.__name__,reason)
            sys.exit(1)
    if len(FCM_XML_DICT["manifest"]) == 0:
        del FCM_XML_DICT["manifest"]

def ConcatXMLAIDLMtDt(xml_hal_lst,metadata_lst,vintfnumber):
    global XML_HAL_NOTFND,MD_HAL_NOTFND
    json_lst = []
    hal_added_lst = []
    try:
        if type(xml_hal_lst) is list:
            for xml_hal in xml_hal_lst:
                xml_hal_found = False
                for md_hal in metadata_lst:
                    if xml_hal[u'name'] == md_hal[u'name'] and xml_hal[u'name'] not in hal_added_lst:
                        if md_hal[u'name'] in MD_HAL_NOTFND.keys():
                            del MD_HAL_NOTFND[md_hal[u'name']]
                        hal_added_lst.append(xml_hal[u'name'])
                        xml_hal_found = True
                        json_hal = md_hal.copy()
                        if u'stability' in json_hal.keys():
                            if json_hal[u'stability'] != "vintf":
                                reason = "QIIFA Checks only Stable interfaces. Please update the vendor HAL to be a stable interface. Please contact qiifa-squad for more info"
                                if PLUGIN_STATE_WARNING:
                                    UtilFunctions.print_violations_on_stdout("AIDL Plugin",xml_hal[u'name'],ConcatXMLAIDLMtDt.__name__,reason,False)
                                else:
                                    UtilFunctions.print_violations_on_stdout("AIDL Plugin",xml_hal[u'name'],ConcatXMLAIDLMtDt.__name__,reason)
                                    return
                        #has development needs to be updated only for new Hals not for old hals
                        if u'has_development' in json_hal.keys():
                            if json_hal[u'has_development'] == True:
                                if vintfnumber.isdigit() and int(vintfnumber) < FCM_MAX_VAL:
                                    json_hal[u'has_development'] = False
                                elif vintfnumber =="device":
                                    Logger.logStdout.error("This interface "+str(xml_hal[u'name'])+" needs to be frozen. There should not be any development once the interface is frozen")
                        min_hash_ind = 1
                        max_hash_ind = 1
                        if u'version' in xml_hal.keys():
                            xml_version = xml_hal[u'version']
                            if xml_version.isdigit():
                                min_hash_ind = int(xml_version)
                                max_hash_ind = int(xml_version)
                            else:
                                try:
                                    version_split = xml_version.split("-")
                                    min_hash_ind = int(version_split[0])
                                    max_hash_ind = int(version_split[1])
                                except Exception as e:
                                    traceback.print_exc()
                                    reason = "Please verify the XML File(compatibility.matrix."+vintfnumber+"), invalid Version syntax has been used."
                                    Logger.logStdout.error(e)
                                    if PLUGIN_STATE_WARNING:
                                        UtilFunctions.print_violations_on_stdout("AIDL Plugin",xml_hal[u'name'],ConcatXMLAIDLMtDt.__name__,reason,False)
                                    else:
                                        UtilFunctions.print_violations_on_stdout("AIDL Plugin",xml_hal[u'name'],ConcatXMLAIDLMtDt.__name__,reason)
                        elif u'version' not in xml_hal.keys() and u'versions' in json_hal.keys() and json_hal[u'has_development'] == False:
                            Logger.logInternal.warning("AIDL interface "+json_hal["name"]+" in (compatibility.matrix."+vintfnumber+").xml file does not contain version and Hence version being taken as default (1)")
                            min_hash_ind = 1
                            max_hash_ind = 1
                        else:
                            if ((vintfnumber.isdigit() and int(vintfnumber) >= FCM_MAX_VAL) or vintfnumber =="device"):
                                Logger.logInternal.warning("The version needs to be updated for the following hal info in the compatibility matrix(compatibility.matrix."+vintfnumber+") for "+str(xml_hal[u'name'])+"")
                            else:
                                Logger.logInternal.warning("The version needs to be updated for the following hal info in the compatibility matrix(compatibility.matrix."+vintfnumber+") for "+str(xml_hal[u'name'])+" ")
                        if u'versions' not in json_hal.keys():
                            reason = "Version is not present in AIDL Metadata JSON. Please compile again or contact qiifa-squad for more information"
                            if PLUGIN_STATE_WARNING:
                                UtilFunctions.print_violations_on_stdout("AIDL Plugin",xml_hal[u'name'],ConcatXMLAIDLMtDt.__name__,reason,False)
                            else:
                                UtilFunctions.print_violations_on_stdout("AIDL Plugin",xml_hal[u'name'],ConcatXMLAIDLMtDt.__name__,reason)
                        if sys.version_info < (3,):
                            json_hal[u'versions'] = range(min_hash_ind,max_hash_ind+1)
                        else:
                            json_hal[u'versions'] = list(range(min_hash_ind,max_hash_ind+1))
                        if u'hashes' in json_hal.keys() and type(json_hal[u'hashes']) is dict:
                            json_hal_hashes = json_hal[u'hashes'].copy()
                            for hash_key in json_hal[u'hashes'].keys():
                                if int(hash_key) < min_hash_ind or int(hash_key) > max_hash_ind:
                                    del json_hal_hashes[hash_key]
                            json_hal[u'hashes'] = json_hal_hashes
                        if u'version_pfv_map' in json_hal.keys() and type(json_hal[u'version_pfv_map']) is dict:
                            json_hal_hashes = json_hal[u'version_pfv_map'].copy()
                            for hash_key in json_hal[u'version_pfv_map'].keys():
                                if int(hash_key) < min_hash_ind or int(hash_key) > max_hash_ind:
                                    del json_hal_hashes[hash_key]
                            json_hal[u'version_pfv_map'] = json_hal_hashes
                        json_lst.append(json_hal)
            if not xml_hal_found:
                if xml_hal[u'name'] not in XML_HAL_NOTFND:
                    XML_HAL_NOTFND.append(xml_hal[u'name'])
        elif type(xml_hal_lst) is OrderedDict:
            xml_hal_found = False
            for md_hal in metadata_lst:
                if xml_hal_lst[u'name'] == md_hal[u'name'] and xml_hal_lst[u'name'] not in hal_added_lst:
                    if md_hal[u'name'] in MD_HAL_NOTFND.keys():
                        del MD_HAL_NOTFND[md_hal[u'name']]
                    hal_added_lst.append(xml_hal_lst[u'name'])
                    xml_hal_found = True
                    json_hal = md_hal.copy()
                    if u'stability' in json_hal.keys():
                        if json_hal[u'stability'] != "vintf":
                            reason = "QIIFA Checks only Stable interfaces. Please update the vendor HAL to be a stable interface. Please contact qiifa-squad for more info"
                            if PLUGIN_STATE_WARNING:
                                UtilFunctions.print_violations_on_stdout("AIDL Plugin",xml_hal_lst[u'name'],ConcatXMLAIDLMtDt.__name__,reason,False)
                            else:
                                UtilFunctions.print_violations_on_stdout("AIDL Plugin",xml_hal_lst[u'name'],ConcatXMLAIDLMtDt.__name__,reason)
                                return
                    #has development needs to be updated only for new Hals not for old hals
                    if u'has_development' in json_hal.keys():
                        if json_hal[u'has_development'] == True:
                            if vintfnumber.isdigit() and int(vintfnumber) < FCM_MAX_VAL:
                                json_hal[u'has_development'] = False
                            elif vintfnumber =="device":
                                Logger.logStdout.error("This interface "+str(xml_hal_lst[u'name'])+" needs to be frozen. There should not be any development once the interface is frozen")
                            elif vintfnumber.isdigit():
                                Logger.logStdout.info("This interface "+str(xml_hal_lst[u'name'])+" needs to be frozen. There should not be any development once the interface is frozen")
                    min_hash_ind = 1
                    max_hash_ind = 1
                    if u'version' in xml_hal_lst.keys():
                        xml_version = xml_hal_lst[u'version']
                        if xml_version.isdigit():
                            min_hash_ind = int(xml_version)
                            max_hash_ind = int(xml_version)
                        else:
                            try:
                                version_split = xml_version.split("-")
                                min_hash_ind = int(version_split[0])
                                max_hash_ind = int(version_split[1])
                            except Exception as e:
                                traceback.print_exc()
                                reason = "Please verify the XML File(compatibility.matrix."+vintfnumber+"), invalid Version syntax has been used."
                                Logger.logStdout.error(e)
                                if PLUGIN_STATE_WARNING:
                                    UtilFunctions.print_violations_on_stdout("AIDL Plugin",xml_hal_lst[u'name'],ConcatXMLAIDLMtDt.__name__,reason,False)
                                else:
                                    UtilFunctions.print_violations_on_stdout("AIDL Plugin",xml_hal_lst[u'name'],ConcatXMLAIDLMtDt.__name__,reason)
                    elif u'version' not in xml_hal_lst.keys() and u'versions' in json_hal.keys() and json_hal[u'has_development'] == False:
                        Logger.logInternal.warning("AIDL interface "+json_hal["name"]+" in (compatibility.matrix."+vintfnumber+").xml file does not contain version and Hence version being taken as default (1)")
                        min_hash_ind = 1
                        max_hash_ind = 1
                    else:
                        if (vintfnumber.isdigit() and int(vintfnumber) >= FCM_MAX_VAL) or vintfnumber =="device":
                            Logger.logInternal.warning("The version needs to be updated for the following hal info in the compatibility matrix(compatibility.matrix."+vintfnumber+") for "+str(xml_hal_lst[u'name'])+" ")
                        else:
                            Logger.logInternal.warning("The version needs to be updated for the following hal info in the compatibility matrix(compatibility.matrix."+vintfnumber+") for "+str(xml_hal_lst[u'name'])+"")
                    if sys.version_info < (3,):
                        json_hal[u'versions'] = range(min_hash_ind,max_hash_ind+1)
                    else:
                        json_hal[u'versions'] = list(range(min_hash_ind,max_hash_ind+1))
                    if type(json_hal[u'hashes']) is dict:
                        json_hal_hashes = json_hal[u'hashes'].copy()
                        for hash_key in json_hal[u'hashes'].keys():
                            if int(hash_key) < min_hash_ind or int(hash_key) > max_hash_ind:
                                del json_hal_hashes[hash_key]
                        json_hal[u'hashes'] = json_hal_hashes
            json_lst.append(json_hal)
            if not xml_hal_found:
                if xml_hal_lst[u'name'] not in XML_HAL_NOTFND:
                    XML_HAL_NOTFND.append(xml_hal_lst[u'name'])
    except Exception as e:
        traceback.print_exc()
        reason = "Please verify the XML File, invalid syntax has been used."
        Logger.logStdout.error(e)
        UtilFunctions.print_violations_on_stdout("AIDL Plugin","XML_FILE",ConcatXMLAIDLMtDt.__name__,reason)
        sys.exit(1)
    return json_lst


def GenHIDLAIDLListCSV(xml_file_root,target_level = None):
    hidl_dict={}
    aidl_dict={}
    hidl_lst=[]
    aidl_lst=[]
    total_hal_lst=[]
    try:
        if target_level == None:
            target_level = FCM_MAX_VAL
        for root,dirs,files in os.walk(xml_file_root):
            for file in files:
                if file.startswith("compatibility_matrix."):
                    file_path = os.path.join(root,file)
                    fname_split = ((file.replace(".xml","")).split("."))[-1]
                    if (fname_split.isdigit() and int(fname_split)>=target_level) or (fname_split=='device'):
                        xml_fd = open(file_path)
                        try:
                            xml_files_dict = xmltodict.parse(xml_fd.read())
                            if len(xml_files_dict)>0:
                                if type(xml_files_dict) is OrderedDict or type(xml_files_dict) is dict:
                                    if type(xml_files_dict[u'compatibility-matrix']) is OrderedDict or type(xml_files_dict[u'compatibility-matrix']) is dict:
                                        if type(xml_files_dict[u'compatibility-matrix'][u'hal']) is OrderedDict or type(xml_files_dict[u'compatibility-matrix'][u'hal']) is dict:
                                            if '@format' in xml_files_dict['compatibility-matrix']['hal'].keys():
                                                if xml_files_dict['compatibility-matrix']['hal'][u'name'] not in total_hal_lst:
                                                    total_hal_lst.append(xml_files_dict['compatibility-matrix']['hal'][u'name'])
                                                if xml_files_dict['compatibility-matrix']['hal'][u'@format'] == 'aidl' and xml_files_dict['compatibility-matrix']['hal'][u'name'] not in aidl_dict.keys():
                                                    if u'version' in xml_files_dict['compatibility-matrix']['hal'].keys():
                                                        aidl_dict[xml_files_dict['compatibility-matrix']['hal'][u'name']]={u'version':str(xml_files_dict['compatibility-matrix']['hal'][u'version'])}
                                                    else:
                                                        aidl_dict[xml_files_dict['compatibility-matrix']['hal'][u'name']]={u'version':"1"}
                                                elif xml_files_dict['compatibility-matrix']['hal'][u'@format'] == 'hidl' and xml_files_dict['compatibility-matrix']['hal'][u'name'] not in hidl_dict.keys():
                                                    hidl_dict[xml_files_dict['compatibility-matrix']['hal'][u'name']]={}
                                        elif type(xml_files_dict[u'compatibility-matrix'][u'hal']) is list:
                                            for hal_list in xml_files_dict[u'compatibility-matrix'][u'hal']:
                                                if hal_list[u'name'] not in total_hal_lst:
                                                    total_hal_lst.append(hal_list[u'name'])
                                                if hal_list[u'@format'] == 'aidl' and hal_list[u'name'] not in aidl_dict.keys():
                                                    if u'version' in hal_list.keys():
                                                        aidl_dict[hal_list[u'name']]={u'version':str(hal_list[u'version'])}
                                                    else:
                                                        aidl_dict[hal_list[u'name']] = {u'version':"1"}
                                                elif hal_list[u'@format'] == 'hidl' and hal_list[u'name'] not in hidl_dict.keys():
                                                    hidl_dict[hal_list[u'name']] = {}
                        except Exception as e:
                            traceback.print_exc()
                            reason = "Please verify the XML File, invalid syntax has been used."
                            Logger.logStdout.error(e)
                            UtilFunctions.print_violations_on_stdout("AIDL Plugin",fname_split,GenHIDLAIDLListCSV.__name__,reason)
                        xml_fd.close()
                else:
                    file_path = os.path.join(root,file)
                    xml_fd = open(file_path)
                    try:
                        xml_files_dict = xmltodict.parse(xml_fd.read())
                        if type(xml_files_dict) is OrderedDict or type(xml_files_dict) is dict:
                            if u'manifest' in xml_files_dict.keys():
                                if u'hal' in xml_files_dict['manifest'].keys():
                                    if type(xml_files_dict['manifest']['hal']) is OrderedDict or type(xml_files_dict['manifest']['hal']) is dict:
                                        if u'@format' in xml_files_dict['manifest']['hal'].keys():
                                            if xml_files_dict['manifest']['hal'][u'name'] not in total_hal_lst:
                                                total_hal_lst.append(xml_files_dict['manifest']['hal'][u'name'])
                                            if xml_files_dict['manifest']['hal'][u'@format'] == 'aidl' and xml_files_dict['manifest']['hal'][u'name'] not in aidl_dict.keys():
                                                if u'version' in xml_files_dict['manifest']['hal'].keys():
                                                    aidl_dict[xml_files_dict['manifest']['hal'][u'name']]={u'version':str(xml_files_dict['manifest']['hal'][u'version'])}
                                                else:
                                                    aidl_dict[xml_files_dict['manifest']['hal'][u'name']]={u'version':"1"}
                                            elif xml_files_dict['manifest']['hal'][u'@format'] == 'hidl' and xml_files_dict['manifest']['hal'][u'name'] not in hidl_dict.keys():
                                                hidl_dict[xml_files_dict['manifest']['hal'][u'name']]={}
                                    elif type(xml_files_dict['manifest']['hal']) is list:
                                        for hal_list in xml_files_dict[u'manifest'][u'hal']:
                                            if u'@format' in hal_list.keys():
                                                if hal_list[u'name'] not in total_hal_lst:
                                                    total_hal_lst.append(hal_list[u'name'])
                                                if hal_list[u'@format'] == 'aidl' and hal_list[u'name'] not in aidl_dict.keys():
                                                    if u'version' in hal_list.keys():
                                                        aidl_dict[hal_list[u'name']]={u'version':str(hal_list[u'version'])}
                                                    else:
                                                        aidl_dict[hal_list[u'name']]={u'verion':"1"}
                                                elif hal_list[u'@format'] == 'hidl' and hal_list[u'name'] not in hidl_dict.keys():
                                                    hidl_dict[hal_list[u'name']]={}
                    except Exception as e:
                        traceback.print_exc()
                        reason = "Please verify the XML File, invalid syntax has been used."
                        Logger.logStdout.error(e)
                        UtilFunctions.print_violations_on_stdout("AIDL Plugin",file,GenHIDLAIDLListCSV.__name__,reason)
                    xml_fd.close()
        if len(AIDL_METADATA_DICT) > 0 and type(AIDL_METADATA_DICT) is list:
            for meta_dict in AIDL_METADATA_DICT:
                if str(meta_dict[u'name']) not in total_hal_lst and str(meta_dict[u'name']).startswith("vendor.") and str(meta_dict[u'name']) not in AIDL_SKIPPED_LST[u'UNSTABLE_INTFS']:
                    total_hal_lst.append(meta_dict[u'name'])
                    version_info=""
                    if u'versions' in meta_dict.keys():
                        if len(meta_dict[u'versions']) == 1:
                            version_info = min(meta_dict[u'versions'])
                        else:
                            version_info = str(min(meta_dict[u'versions']))+"-"+str(max(meta_dict[u'versions']))
                    aidl_dict[meta_dict[u'name']]={u'version':version_info}
                if str(meta_dict[u'name']) in total_hal_lst and meta_dict[u'name'] in aidl_dict.keys() and u'has_development' in meta_dict.keys():
                    aidl_dict[meta_dict[u'name']][u'has_development'] = meta_dict[u'has_development']

        file1 = open(Constants.qiifa_aidl_db_root + "qiifa_aidl_hidl_lst.csv", "w")
        intflstnew = ["HAL_NAME, HAL_OWNER, HIDL, AIDL, AIDL_VERSION, FROZEN\n"]
        for hal in total_hal_lst:
            new_str=str(hal)
            if "android" in str(hal).lower():
                new_str=new_str+",google"
            else:
                new_str=new_str+",qti"
            if hal in hidl_dict.keys():
                new_str=new_str+",x"
            else:
                new_str=new_str+","
            if hal in aidl_dict.keys():
                new_str=new_str+",x"
                if len(aidl_dict[hal])>0:
                    if u'version' in aidl_dict[hal].keys():
                        new_str=new_str+",\"=\"\""+str(aidl_dict[hal][u'version'])+"\"\"\""
                    else:
                        new_str=new_str+","
                    if u'has_development' in aidl_dict[hal].keys():
                        if aidl_dict[hal][u'has_development']:
                            new_str=new_str+",false"
                        else:
                            new_str=new_str+",true"
                    else:
                        new_str=new_str+",unknown"
                else:
                     new_str=new_str+","
            else:
                new_str=new_str+",,"
            new_str= new_str+ " \n"
            intflstnew.append(new_str)
        file1.writelines(intflstnew)
        file1.close()
    except Exception as e:
        traceback.print_exc()
        reason = "Please verify the XML File, invalid syntax has been used."
        Logger.logStdout.error(e)
        UtilFunctions.print_violations_on_stdout("AIDL Plugin",fname_split,GenHIDLAIDLListCSV.__name__,reason)

def chkVerDep(mtdt_dict,platform_version,found_intf=False):
    dep_ver = []
    no_dep_ver = []
    try:
        if u'version_pfv_map' in mtdt_dict.keys() and len(mtdt_dict[u'version_pfv_map']) > 0:
            for version_key in mtdt_dict[u'version_pfv_map'].keys():
                if mtdt_dict[u'version_pfv_map'][version_key] < (platform_version - Constants.AIDL_NO_OF_PREV_VERS_SUPP):
                    dep_ver.append(version_key)
                else:
                    no_dep_ver.append(version_key)
        if len(dep_ver) > 0:
            Logger.logStdout.info("In the following interface "+mtdt_dict[u'name']+", the following version ("+', '.join([str(version) for version in dep_ver])+") can be deprecated")
        elif len(dep_ver) == 0 and found_intf:
            Logger.logStdout.info("In the following interface "+mtdt_dict[u'name']+", the following version ("+', '.join([str(version) for version in no_dep_ver])+") cannot be deprecated")
    except:
        traceback.print_exc()
        reason = "Please check the version and platform version mapping. Please contact QIIFA-Squad for more info."
        Logger.logStdout.error(e)
        UtilFunctions.print_violations_on_stdout("AIDL Plugin",mtdt_dict[u'name'],chkVerDep.__name__,reason)



def aidlCreateMain(self,
                             flag,
                             arg_create_type,
                             arg_intf_name=None):
    Logger.logStdout.info("Running qiifa golden db generator... \n")
    if not check_plugin_state():
        Logger.logStdout.warning("AIDL Plugin state doen't match supported values. Exiting now.")
        return
    if plugin_disabled():
        Logger.logStdout.warning("AIDL Plugin is disabled. Exiting now.")
        return
    if Constants.qiifa_out_path_target_value != "qssi":
        reason = "QIIFA golden db generator not supported on vendor Side. Please use only on System Side"
        if PLUGIN_STATE_WARNING:
            UtilFunctions.print_violations_on_stdout(LOG_TAG,"QIIFA golden db generator",aidlCreateMain.__name__,reason,False)
        else:
            UtilFunctions.print_violations_on_stdout(LOG_TAG,"QIIFA golden db generator",aidlCreateMain.__name__,reason)
        return

    initAIDLGlobalPluginVars(arg_intf_name)
    if len(AIDL_METADATA_DICT) == 0:
        reason="Error while parsing txt files"
        if PLUGIN_STATE_WARNING:
            UtilFunctions.print_violations_on_stdout(LOG_TAG,"QIIFA golden db generator",aidlCreateMain.__name__,reason,False)
        else:
            UtilFunctions.print_violations_on_stdout(LOG_TAG,"QIIFA golden db generator",aidlCreateMain.__name__,reason)
            return

    if arg_create_type != "aidl_lst_gen":
        for directory in Constants.paths_of_xml_files:
            if UtilFunctions.pathExists(directory):
                GenQSSIMaxFCMPlatVersMap(directory)
                if (arg_intf_name != None):
                    UpdXmltoDict(FCM_GLOBAL_DICT,directory,arg_intf_name,FCM_MAX_VAL)
                else:
                    UpdXmltoDict(FCM_GLOBAL_DICT,directory)
        '''
            Generating the /QIIFA_cmd/aidl if the folder does not exist
        '''
        if len(FCM_XML_DICT) == 0 :
            reason="Interface name is not found in the compatibility matrix. Please add it to the corresponding compatibility matrix"
            if PLUGIN_STATE_WARNING:
                UtilFunctions.print_violations_on_stdout(LOG_TAG,arg_intf_name,aidlCreateMain.__name__,reason,False)
            else:
                UtilFunctions.print_violations_on_stdout(LOG_TAG,arg_intf_name,aidlCreateMain.__name__,reason)
                sys.exit(1)
        while not os.path.isdir(Constants.qiifa_aidl_db_root):
            CreateRootDir(Constants.qiifa_aidl_db_root)
        #the case where user wants to generate the full cmd
        #not recommended :)

        if arg_intf_name == None and arg_create_type == Constants.AIDL_SUPPORTED_CREATE_ARGS[0]:
            Logger.logStdout.info("Running full aidl cmd generation.. \n")
            Logger.logStdout.warning("Not a recommended operation..\n")
            createGoldenCmd(Constants.qiifa_aidl_db_root, FCM_XML_DICT, AIDL_METADATA_DICT, "all")
        #at this point it must be a single intf cmd generation
        else:
            Logger.logStdout.info("Running aidl cmd generation with option: " + arg_create_type +", intf name: " + arg_intf_name+"\n")
            createGoldenCmd(Constants.qiifa_aidl_db_root, FCM_XML_DICT, AIDL_METADATA_DICT, arg_create_type, arg_intf_name)
        Logger.logInternal.info ("Success")
    elif arg_create_type == "aidl_lst_gen":
        for directory in Constants.paths_of_xml_files:
            if UtilFunctions.pathExists(directory):
                if "system/etc/vintf" in directory:
                    GenQSSIMaxFCMPlatVersMap(directory)
                    GenHIDLAIDLListCSV(directory,FCM_MAX_VAL)
        Logger.logInternal.info ("Success")

def createGoldenCmd(json_db_file_root,fcm_xml_dict, metadatadict_lst, arg_type, for_intf_name=None):
    # root,dirs,files = next(os.walk(json_db_file_root))
    json_create_dict={}
    dbdict_lst=[]
    json_cmd_all_lst=[]
    json_db_file_path = os.path.join(json_db_file_root,"qiifa_aidl_cmd_device.json")
    if arg_type != "all":
        if (UtilFunctions.pathExists(json_db_file_path) == True):
            dbdict_lst = JSONLoadtoDict(json_db_file_path)
        if len(dbdict_lst) == 0:
            reason="Please run python qiifa_main.py --create aidl before running this option "+ str(arg_type)
            if PLUGIN_STATE_WARNING:
                UtilFunctions.print_violations_on_stdout(LOG_TAG,"QIIFA golden db generator for "+str(arg_type),createGoldenCmd.__name__,reason,False)
            else:
                UtilFunctions.print_violations_on_stdout(LOG_TAG,"QIIFA golden db generator for "+str(arg_type),createGoldenCmd.__name__,reason)
                sys.exit(1)
    for key in fcm_xml_dict.keys():
        if "device" in key:
            json_fname = "qiifa_aidl_cmd_"+str(FCM_GLOBAL_DICT[((str(os.path.basename(key)).replace(".xml","").split("."))[-1])])
            json_create_dict[json_fname] = ConcatXMLAIDLMtDt(fcm_xml_dict[key],metadatadict_lst,str(FCM_GLOBAL_DICT[((str(os.path.basename(key)).replace(".xml","").split("."))[-1])]))
        elif "manifest" in key:
            json_fname = "qiifa_aidl_cmd_manifest"
            json_create_dict[json_fname] = ConcatXMLAIDLMtDt(fcm_xml_dict[key],metadatadict_lst,"device")
        if "device" in key or "manifest" in key:
            if arg_type == 'all':
                Logger.logStdout.info("Running aidl create command with option all for : "+str(key)+" \n")
                for meta_dict in json_create_dict[json_fname]:
                    if u'path' in meta_dict.keys():
                        del meta_dict[u'path']
                    if meta_dict[u'name'].startswith("vendor.") or "device" in key:
                        if meta_dict not in dbdict_lst and meta_dict[u'name'] not in json_cmd_all_lst:
                            dbdict_lst.append(meta_dict)
                            json_cmd_all_lst.append(meta_dict[u'name'])
                        elif meta_dict not in dbdict_lst and meta_dict[u'name'] in json_cmd_all_lst:
                            for goldencmd_dict in dbdict_lst:
                                if meta_dict[u'name'] == goldencmd_dict[u'name']:
                                    for mtdt_version in meta_dict[u'versions']:
                                        if mtdt_version not in goldencmd_dict[u'versions']:
                                            goldencmd_dict[u'versions'].append(mtdtver)
                                            goldencmd_dict[u'hashes'][str(mtdt_version)] = meta_dict[u'hashes'][str(mtdt_version)]
                                            goldencmd_dict[u'version_pfv_map'][str(mtdt_version)] = meta_dict[u'version_pfv_map'][str(mtdt_version)]
            else:
                Logger.logStdout.info("Running aidl create command with option "+ str(arg_type)+" and for interface "+str(for_intf_name)+"\n")
                chkDupinDict(dbdict_lst,json_db_file_path)
                intffound = False
                founddict = {}
                fcmintffound = False
                fcmintfdict = {}
                for goldencmd in dbdict_lst:
                    if goldencmd[u'name'] == for_intf_name:
                        intffound = True
                        founddict = goldencmd
                for fcmdict in json_create_dict[json_fname]:
                    if fcmdict[u'name'] == for_intf_name:
                        fcmintffound = True
                        fcmintfdict = fcmdict
                if fcmintffound:
                    if arg_type == Constants.AIDL_SUPPORTED_CREATE_ARGS[1]:
                        if not intffound:
                            reason="Interface name is not found in the aidl file path present in "+os.path.basename(json_db_file_path)
                            if PLUGIN_STATE_WARNING:
                                UtilFunctions.print_violations_on_stdout(LOG_TAG,for_intf_name,createGoldenCmd.__name__,reason,False)
                            else:
                                UtilFunctions.print_violations_on_stdout(LOG_TAG,for_intf_name,createGoldenCmd.__name__,reason)
                                sys.exit(1)
                        missing_param = set()
                        for goldencmd_dict in dbdict_lst:
                            if goldencmd_dict[u'name'] == for_intf_name:
                                for mtdt_dict in AIDL_METADATA_DICT:
                                    if u'path' in mtdt_dict.keys():
                                        del mtdt_dict[u'path']
                                    if mtdt_dict[u'name'] == goldencmd_dict[u'name'] and goldencmd_dict != mtdt_dict:
                                        mtdtdict_keys = set(mtdt_dict.keys())
                                        goldencmd_keys = set(goldencmd_dict.keys())
                                        missing_param = set(goldencmd_keys ^ mtdtdict_keys)
                                        same_para = set(goldencmd_keys & mtdtdict_keys)
                                        metamissing_param = list(missing_param & mtdtdict_keys)
                                        goldenmissing_param = list(missing_param & goldencmd_keys)
                                        new_parame = list(missing_param & same_para)
                                        if len(metamissing_param) > 0:
                                            Logger.logStdout.info("The following parameters for the interface are being added ")
                                            Logger.logStdout.info(metamissing_param)
                                            for addkey in metamissing_param:
                                                goldencmd_dict[addkey] = mtdt_dict[addkey]
                                        if len(goldenmissing_param) > 0:
                                            Logger.logStdout.info("The following parameters for the interface are being removed ")
                                            Logger.logStdout.info(goldenmissing_param)
                                            for removalkey in goldenmissing_param:
                                                del goldencmd_dict[removalkey]
                                        for mtdtver in mtdt_dict[u'versions']:
                                            if mtdtver not in goldencmd_dict[u'versions']:
                                                goldencmd_dict[u'versions'].append(mtdtver)
                                            if str(mtdtver) not in goldencmd_dict[u'hashes'].keys():
                                                goldencmd_dict[u'hashes'][str(mtdtver)] = mtdt_dict[u'hashes'][str(mtdtver)]
                                            if str(mtdtver) not in goldencmd_dict[u'version_pfv_map'].keys():
                                                goldencmd_dict[u'version_pfv_map'][str(mtdtver)] = mtdt_dict[u'version_pfv_map'][mtdtver]
                                    elif mtdt_dict[u'name'] == goldencmd_dict[u'name'] and goldencmd_dict == mtdt_dict:
                                        reason="Interface ("+for_intf_name+") does not have changes than what is present in the file "+os.path.basename(json_db_file_path)
                                        if PLUGIN_STATE_WARNING:
                                            UtilFunctions.print_violations_on_stdout(LOG_TAG,for_intf_name,createGoldenCmd.__name__,reason,False)
                                        else:
                                            UtilFunctions.print_violations_on_stdout(LOG_TAG,for_intf_name,createGoldenCmd.__name__,reason)
                                            sys.exit(1)
                    elif arg_type == Constants.AIDL_SUPPORTED_CREATE_ARGS[2]:
                        if intffound:
                            reason="Interface name is found in the aidl file path present in "+os.path.basename(json_db_file_path)+" \n \t\t\t\t\t\t Please use aidl_one to update existing interface information. Please visit go/qiifa for more info"
                            if PLUGIN_STATE_WARNING:
                                UtilFunctions.print_violations_on_stdout(LOG_TAG,for_intf_name,createGoldenCmd.__name__,reason,False)
                            else:
                                UtilFunctions.print_violations_on_stdout(LOG_TAG,for_intf_name,createGoldenCmd.__name__,reason)
                                sys.exit(1)
                        for mtdt_dict in AIDL_METADATA_DICT:
                            if mtdt_dict[u'name'] == for_intf_name and not intffound:
                                if u'path' in mtdt_dict.keys():
                                    del mtdt_dict[u'path']
                                dbdict_lst.append(mtdt_dict)
                    break
                else:
                    reason="Interface name is not found in the Compatibility Matrix Please add it to the FCM"
                    if PLUGIN_STATE_WARNING:
                        UtilFunctions.print_violations_on_stdout(LOG_TAG,for_intf_name,createGoldenCmd.__name__,reason,False)
                    else:
                        UtilFunctions.print_violations_on_stdout(LOG_TAG,for_intf_name,createGoldenCmd.__name__,reason)
                        sys.exit(1)
    if len(dbdict_lst) > 0:
        dbdict_lst = sorted(dbdict_lst, key=lambda d: d[u'name'])
        with open(json_db_file_path,'w') as jf:
            json.dump(dbdict_lst, jf, separators=(",", ": "), indent=4,sort_keys=True)
    if len(XML_HAL_NOTFND)>0:
        for name in XML_HAL_NOTFND:
            Logger.logStdout.warning("Please check these XML hal manifest since there is compatbility matrix entry for this hal ("+name+") but there is no aidl_interface defined")
    if len(MD_HAL_NOTFND) >0 and arg_type == "all":
        for key in MD_HAL_NOTFND.keys():
            if key.startswith("vendor."):
                Logger.logStdout.warning("Please check these Metadata hal entries since there is  no compatbility matrix entry for this hal ("+key+") but there is an aidl_interface defined")
            else:
                Logger.logInternal.warning("Please check these Metadata hal entries since there is  no compatbility matrix entry for this hal ("+key+") but there is an aidl_interface defined")

def enforceVNDRProps(metadata_dict_list):
    try:
        if type(metadata_dict_list) is list:
            for metadata_dict in metadata_dict_list:
                if metadata_dict[u'name'] in AIDL_SKIPPED_LST[u'ALL'] or metadata_dict[u'name'] in AIDL_SKIPPED_LST[u'UNSTABLE_INTFS']:
                    Logger.logStdout.warning("AIDL Skipped INTF from Skipped List " + metadata_dict[u'name'])
                elif metadata_dict[u'name'].startswith(Constants.AIDL_ALLWED_INTF_PREF[0]):
                    if (u'stability' in metadata_dict.keys() and metadata_dict[u'stability'] != "vintf") or u'stability' not in metadata_dict.keys():
                        reason="Stability tag needs to be set to VINTF for this interface name: "+metadata_dict[u'name']
                        if PLUGIN_STATE_WARNING:
                            UtilFunctions.print_violations_on_stdout(LOG_TAG,metadata_dict[u'name'],enforceVNDRProps.__name__,reason,False)
                        else:
                            UtilFunctions.print_violations_on_stdout(LOG_TAG,metadata_dict[u'name'],enforceVNDRProps.__name__,reason)
                            return
                    if u'path'  in metadata_dict.keys():
                        path_found = False
                        for paths in Constants.qiifa_aidl_allowed_path:
                            if metadata_dict[u'path'][0].startswith(paths):
                                path_found = True
                                break
                        if not path_found:
                            reason="Incorrect path Location. Please check the path for this interface name: "+metadata_dict[u'name']
                            if PLUGIN_STATE_WARNING:
                                UtilFunctions.print_violations_on_stdout(LOG_TAG,metadata_dict[u'name'],enforceVNDRProps.__name__,reason,False)
                            else:
                                UtilFunctions.print_violations_on_stdout(LOG_TAG,metadata_dict[u'name'],enforceVNDRProps.__name__,reason)
                                return
    except Exception as e:
        traceback.print_exc()
        Logger.logStdout.error(e)
        reason = "The Vendor interface properties like Stability, Path of the interface are set incorrectly. Please check go/qiifa for more information"
        if PLUGIN_STATE_WARNING:
            UtilFunctions.print_violations_on_stdout(LOG_TAG,"Enforce Vendor Properties",enforceVNDRProps.__name__,reason,False)
        else:
            UtilFunctions.print_violations_on_stdout(LOG_TAG,"Enforce Vendor Properties",enforceVNDRProps.__name__,reason)
            return

def readQIIFACMDSource(dbcmd_root_path,TARGET_LEVEL=0):
    db_dict={}
    for root,dirs,files in os.walk(Constants.qiifa_aidl_db_root):
        for file in files:
            if file.startswith("qiifa_aidl_cmd_"):
                jsonfname = ((file.replace(".json","")).split("_"))[-1]
                qiifa_aidl_db_path = os.path.join(root,file)
                if TARGET_LEVEL == 0:
                    goldencmddict_lst = JSONLoadtoDict(qiifa_aidl_db_path)
                    if len(goldencmddict_lst)<=0:
                        reason="JSON File has not been generated yet. Please run --create option before running IIC checker"
                        if PLUGIN_STATE_WARNING:
                            UtilFunctions.print_violations_on_stdout(LOG_TAG,"aidlIICCheckStart",readQIIFACMDSource.__name__,reason,False)
                            return
                        else:
                            UtilFunctions.print_violations_on_stdout(LOG_TAG,"aidlIICCheckStart",readQIIFACMDSource.__name__,reason)
                            return
                    chkDupinDict(goldencmddict_lst,qiifa_aidl_db_path)
                    db_dict[qiifa_aidl_db_path] = goldencmddict_lst
                elif jsonfname == 'device' or (TARGET_LEVEL > 0 and jsonfname.isdigit() and int(jsonfname)==TARGET_LEVEL):
                    if jsonfname.isdigit(): # make it point to the Android or the Platform version
                        Logger.logStdout.info("Reading the following file : qiifa_aidl_cmd_"+str(TARGET_LEVEL))
                    elif jsonfname == 'device':
                        Logger.logStdout.info("Reading the following file : qiifa_aidl_cmd_device")
                    goldencmddict_lst = JSONLoadtoDict(qiifa_aidl_db_path)
                    if len(goldencmddict_lst)<=0:
                        reason="JSON File has not been generated yet. Please run --create option before running IIC checker"
                        if PLUGIN_STATE_WARNING:
                            UtilFunctions.print_violations_on_stdout(LOG_TAG,"aidlIICCheckStart",readQIIFACMDSource.__name__,reason,False)
                            return
                        else:
                            UtilFunctions.print_violations_on_stdout(LOG_TAG,"aidlIICCheckStart",readQIIFACMDSource.__name__,reason)
                            sys.exit(1)
                    chkDupinDict(goldencmddict_lst,qiifa_aidl_db_path)
                    db_dict[qiifa_aidl_db_path] = goldencmddict_lst
    return db_dict

def aidlIICMain(self,
                    flag,
                    iic_check_type,
                    intf_name=None,
                    qssi_path=None,
                    target_path=None,
                    arg_test_source=None,
                    arg_tp_build=None,):
    '''
    Enforcements for AIDL:
       1. All package name should start from vendor.qti prefix
       2. If AIDL is defined as vendor_available then stability tag
          should be set to vintf.
    '''
    ### We intend to check AIDL interfaces which are part of commonsys-intf
    ### for qssi and vendor compatibility. All commonsys-intf projects are
    ### part of qssi SI, therefore add a check to run aidl checker while
    ### building QSSI lunch.
    if not check_plugin_state():
        Logger.logStdout.warning("AIDL Plugin state doen't match supported values. Exiting now.")
        return
    if plugin_disabled():
        Logger.logStdout.warning("AIDL Plugin is disabled. Exiting now.")
        return
    if qssi_path != None and target_path != None:
        aidlIICCheckStart(flag,iic_check_type,intf_name,qssi_path,target_path)
    else:
        initAIDLGlobalPluginVars(intf_name)
        if Constants.qiifa_out_path_target_value == "qssi":
            enforceVNDRProps(AIDL_METADATA_DICT)
            aidlIICCheckStart(flag,iic_check_type,intf_name,qssi_path,target_path)
        else:
            aidlIICCheckStart(flag,iic_check_type,intf_name,qssi_path,target_path)
        if not Variables.is_violator:
            if not Constants.platform_version.isdigit() and Constants.qiifa_out_path_target_value == "qssi":
                for directory in Constants.paths_of_xml_files:
                    if UtilFunctions.pathExists(directory):
                        GenHIDLAIDLListCSV(directory,FCM_MAX_VAL)

def aidlIICCheckStart(flag,
                    iic_check_type=None,
                    intf_name=None,
                    qssi_path=None,
                    target_path=None):
    if flag == "check":
        if qssi_path != None and target_path != None:
            Logger.logStdout.info("Running AIDL image to image compatibility check.....")
            aidlSuperImgChk(qssi_path, target_path)
        else:
            if iic_check_type in Constants.AIDL_SUPPORTED_CREATE_ARGS:# or iic_check_type == Constants.AIDL_SUPPORTED_CREATE_ARGS[4]:
                if (UtilFunctions.pathExists(Constants.aidl_metadata_file_path) == False):
                    reason="JSON file is not found at"+Constants.aidl_metadata_file_path
                    if PLUGIN_STATE_WARNING:
                        UtilFunctions.print_violations_on_stdout(LOG_TAG,"aidlIICCheckStart",aidlIICCheckStart.__name__,reason,False)
                    else:
                        UtilFunctions.print_violations_on_stdout(LOG_TAG,"aidlIICCheckStart",aidlIICCheckStart.__name__,reason)
                        sys.exit(1)
                json_iic_dict={}
                if Constants.qiifa_out_path_target_value == "qssi":
                    Logger.logStdout.info("Running QSSI only checker.....")
                else:
                    Logger.logStdout.info("Running Target only checker.....")
                for directory in Constants.paths_of_xml_files:
                    if UtilFunctions.pathExists(directory):
                        if Constants.qiifa_out_path_target_value == "qssi":
                            GenQSSIMaxFCMPlatVersMap(directory)
                        else:
                            GenVndrTarLvlPlatVersMap(directory)
                        if (intf_name != None):
                            UpdXmltoDict(FCM_GLOBAL_DICT,directory,intf_name,FCM_MAX_VAL)
                        else:
                            UpdXmltoDict(FCM_GLOBAL_DICT,directory)
                if len(FCM_XML_DICT) == 0:
                    reason="No compatiblity Matrix found. This is not right. This case should not be present. Please check"
                    if PLUGIN_STATE_WARNING:
                        UtilFunctions.print_violations_on_stdout(LOG_TAG,"aidlIICCheckStart",aidlIICCheckStart.__name__,reason,False)
                    else:
                        UtilFunctions.print_violations_on_stdout(LOG_TAG,"aidlIICCheckStart",aidlIICCheckStart.__name__,reason)
                        sys.exit(1)
                for key in FCM_XML_DICT.keys():
                    if "manifest" not in key:
                        json_fname = "qiifa_aidl_cmd_"+str(FCM_GLOBAL_DICT[((str(os.path.basename(key)).replace(".xml","").split("."))[-1])])
                        json_iic_dict[json_fname] = ConcatXMLAIDLMtDt(FCM_XML_DICT[key],AIDL_METADATA_DICT,str(FCM_GLOBAL_DICT[((str(os.path.basename(key)).replace(".xml","").split("."))[-1])]))
                    elif "manifest" in key:
                        json_fname = "manifest"
                        json_iic_dict[json_fname] = ConcatXMLAIDLMtDt(FCM_XML_DICT[key],AIDL_METADATA_DICT,"manifest")
                if len(XML_HAL_NOTFND)>0:
                    for name in XML_HAL_NOTFND:
                        if iic_check_type == Constants.AIDL_SUPPORTED_CREATE_ARGS[0]:
                            Logger.logStdout.warning("Please check these XML hal manifest since there is compatbility matrix entry for this hal ("+name+") but there is no aidl_interface defined")
                if len(MD_HAL_NOTFND) >0:
                    for key in MD_HAL_NOTFND.keys():
                        if key not in AIDL_SKIPPED_LST[u'SUBINTF'] and iic_check_type == Constants.AIDL_SUPPORTED_CREATE_ARGS[0]:
                            if key.startswith("vendor."):
                                Logger.logStdout.warning("Please check these Metadata hal entries since there is  no compatbility matrix entry for this hal ("+key+") but there is an aidl_interface defined")
                            else:
                                Logger.logInternal.warning("Please check these Metadata hal entries since there is  no compatbility matrix entry for this hal ("+key+") but there is an aidl_interface defined")
                if Constants.qiifa_out_path_target_value == "qssi" and (iic_check_type == Constants.AIDL_SUPPORTED_CREATE_ARGS[0] or iic_check_type == Constants.AIDL_SUPPORTED_CREATE_ARGS[4]):
                    goldencmd_dict = readQIIFACMDSource(Constants.qiifa_aidl_db_root,FCM_VENDOR_TL)
                    CompareXMLQIIFACmd(goldencmd_dict,json_iic_dict,FCM_VENDOR_TL,intf_name)
                    if len(FCM_MAX_VENDOR_TL_LST) > 0:
                        for FCM_TL in FCM_MAX_VENDOR_TL_LST:
                            goldencmd_dict = readQIIFACMDSource(Constants.qiifa_aidl_db_root,FCM_TL)
                            CompareXMLQIIFACmd(goldencmd_dict,json_iic_dict,FCM_TL,intf_name)
                elif Constants.qiifa_out_path_target_value == "qssi" and iic_check_type == Constants.AIDL_SUPPORTED_CREATE_ARGS[5]:
                    return
        if len(VNDR_HAL_NOTFND) > 0:
            for key in VNDR_HAL_NOTFND.keys():
                if PLUGIN_STATE_WARNING:
                    UtilFunctions.print_violations_on_stdout(LOG_TAG,key,aidlIICCheckStart.__name__,VNDR_HAL_NOTFND[key][1],False)
                else:
                    UtilFunctions.print_violations_on_stdout(LOG_TAG,key,aidlIICCheckStart.__name__,VNDR_HAL_NOTFND[key][1])
    else:
        Logger.logStdout.error("Unexpected aidl checker flag!")
        sys.exit(1)


def CompareXMLQIIFACmd(goldencmd_dict, xmlconcat_dict,TARGET_LEVEL=0,intf_name=None,enforcement=True):
    for xml_key in xmlconcat_dict.keys():
        for gcd_key in goldencmd_dict.keys():
            if xml_key in os.path.basename(gcd_key) and "device" in os.path.basename(gcd_key):
                Logger.logStdout.info("Running Comparison of Compatibility matrix and QIIFA AIDL CMD for :"+os.path.basename(gcd_key))
                sort_check = chkcmdSorted(goldencmd_dict[gcd_key],os.path.basename(gcd_key))
                if sort_check:
                    return
                if xmlconcat_dict[xml_key] != goldencmd_dict[gcd_key]:
                    CompareMtdtQIIFACMDDict(xml_key,os.path.basename(gcd_key), xmlconcat_dict[xml_key],goldencmd_dict[gcd_key],intf_name,enforcement)
            elif xml_key == "manifest" and int(TARGET_LEVEL) > 0:
                if str(TARGET_LEVEL) in os.path.basename(gcd_key):
                    CompareMtdtQIIFACMDDict(xml_key,os.path.basename(gcd_key), xmlconcat_dict[xml_key],goldencmd_dict[gcd_key],intf_name,enforcement)
                elif "device" in os.path.basename(gcd_key) and int(TARGET_LEVEL) <= int(FCM_VENDOR_TL):
                    CompareMtdtQIIFACMDDict(xml_key,os.path.basename(gcd_key), xmlconcat_dict[xml_key],goldencmd_dict[gcd_key],intf_name,enforcement)

def CompareMtdtQIIFACMDDict(filename,gcd_key, dict_list_1,dict_list_2,intf_name,enforcement):
    global PLUGIN_STATE_WARNING, VNDR_HAL_NOTFND,QIIFA_AIDL_CHIPSET_DVLPMNT_ENBL
    hal_name_dict1_no_dict2 = {}
    hal_name_dict2_no_dict1 = {}
    prefix_id = gcd_key.replace(".json","")
    prefix_id = prefix_id.split("_")
    prefix_id = prefix_id[-1]
    if not enforcement:
        PLUGIN_STATE_WARNING = True
    elif (Constants.aidl_plugin_state=="warning"):
        PLUGIN_STATE_WARNING = True
    elif Constants.platform_version.isdigit() and prefix_id.isdigit() and Constants.target_board_platform in QIIFA_AIDL_CHIPSET_DVLPMNT_ENBL:
        Logger.logStdout.info("Development Chipset found "+Constants.target_board_platform+". If this is incorrect, please contact QIIFA-squad")
        PLUGIN_STATE_WARNING = True
    elif enforcement:
        PLUGIN_STATE_WARNING = False
    if not PLUGIN_STATE_WARNING:
        warning_mode_enabled = True
    else:
        warning_mode_enabled = False

    for dict_hal_1 in dict_list_1:
        if dict_hal_1[u'name'] not in hal_name_dict1_no_dict2.keys():
            hal_name_dict1_no_dict2[dict_hal_1[u'name']] = dict_hal_1[u'name']
        if Constants.qiifa_out_path_target_value != "qssi" and prefix_id.isdigit() and dict_hal_1[u'name'].startswith("vendor."):
            del hal_name_dict1_no_dict2[dict_hal_1[u'name']]
        elif Constants.qiifa_out_path_target_value != "qssi" and not prefix_id.isdigit() and dict_hal_1[u'name'].startswith("android."):
            del hal_name_dict1_no_dict2[dict_hal_1[u'name']]
    for dict_hal_2 in dict_list_2:
        if dict_hal_2[u'name'] not in hal_name_dict2_no_dict1.keys():
            hal_name_dict2_no_dict1[dict_hal_2[u'name']] = dict_hal_2[u'name']
        if Constants.qiifa_out_path_target_value != "qssi" and prefix_id.isdigit() and dict_hal_2[u'name'].startswith("android."):
            del hal_name_dict2_no_dict1[dict_hal_2[u'name']]
        elif Constants.qiifa_out_path_target_value != "qssi" and not prefix_id.isdigit() and dict_hal_2[u'name'].startswith("vendor."):
            del hal_name_dict2_no_dict1[dict_hal_2[u'name']]
    for dict_hal_1 in dict_list_1:
        for dict_hal_2 in dict_list_2:
            if dict_hal_1[u'name'] == dict_hal_2[u'name']:
                if dict_hal_1[u'name'] in hal_name_dict1_no_dict2.keys():
                    del hal_name_dict1_no_dict2[dict_hal_1[u'name']]
                if dict_hal_2[u'name'] in hal_name_dict2_no_dict1.keys():
                    del hal_name_dict2_no_dict1[dict_hal_2[u'name']]
                if dict_hal_1[u'name'] in AIDL_SKIPPED_LST[u'ALL'] or dict_hal_1[u'name'] in AIDL_SKIPPED_LST[u'UNSTABLE_INTFS']:
                    Logger.logStdout.warning("AIDL Skipped INTF from Skipped List " + dict_hal_1[u'name'])
                else:
                    if warning_mode_enabled:
                        if not dict_hal_1[u'name'].startswith("vendor."):
                            PLUGIN_STATE_WARNING = True
                        else:
                            PLUGIN_STATE_WARNING = False
                    if Constants.qiifa_out_path_target_value == "qssi":
                        CmpEachMtdtDictParam(dict_hal_1,dict_hal_2)
    if len(hal_name_dict1_no_dict2) > 0:
        for name in hal_name_dict1_no_dict2:
            if name in AIDL_SKIPPED_LST[u'ALL'] or name in Constants.qiifa_aidl_fw_intfs or name in AIDL_SKIPPED_LST[u'UNSTABLE_INTFS']:
                Logger.logStdout.warning("AIDL Skipped INTF from Skipped List " + name)
            else:
                reason="Please check the AIDL Interface, QIIFA CMD does not have this information. Please run --create option before running IIC."
                if PLUGIN_STATE_WARNING:
                    UtilFunctions.print_violations_on_stdout(LOG_TAG,name,CompareMtdtQIIFACMDDict.__name__,reason,False)
                else:
                    UtilFunctions.print_violations_on_stdout(LOG_TAG,name,CompareMtdtQIIFACMDDict.__name__,reason)
                return

def CmpEachMtdtDictParam(meta_dict,goldencmd_dictrlst,superimgpath_enable = False):
    cmpMtdtQIIFACMDIntfHash(meta_dict,goldencmd_dictrlst,Constants.qiifa_out_path_target_value, superimgpath_enable)
    cmpMtdtQIIFACMDIntfVer(meta_dict,goldencmd_dictrlst,Constants.qiifa_out_path_target_value, superimgpath_enable)
    if u'versions' in meta_dict.keys() and u'versions' in goldencmd_dictrlst.keys():
        cmpMtdtQIIFACMDIntfVerPFMap(meta_dict,goldencmd_dictrlst,Constants.qiifa_out_path_target_value, superimgpath_enable)

def cmpMtdtQIIFACMDIntfHash(meta_dict,goldencmd_dictrlst,image_type, superimgpath_enable):
    if superimgpath_enable:
        return
    try:
        hashnotfound = False
        if len(AIDL_SKIPPED_LST) > 0 and meta_dict[u'name'] in AIDL_SKIPPED_LST[u'HASH_CHECK']:
            Logger.logStdout.warning("AIDL Hash Check Skipped for "+meta_dict[u'name'])
        elif u'hashes' not in meta_dict.keys() or u'hashes' not in goldencmd_dictrlst.keys():
            hashmetaerror = False
            if "hashes" in meta_dict.keys() and "hashes" not in goldencmd_dictrlst.keys():
                hashmetaerror = True
                reason="AIDL interface's hashes is present in META data but not present in QIIFA CMD. Please run --create option before running IIC."
            elif "hashes" not in meta_dict.keys() and "hashes" in goldencmd_dictrlst.keys() :
                if len(goldencmd_dictrlst[u'hashes'].values()) > 1:
                    reason="AIDL interface's hashes is not present in META data but present in QIIFA CMD. Please run --create option before running IIC."
                    hashmetaerror = True
            else:
                reason="AIDL interface's hashes is not present in META data and in QIIFA CMD. Please run --create option before running IIC."
                hashmetaerror = True

            if meta_dict[u'name'] in AIDL_SKIPPED_LST[u'HASH_CHECK']:
                Logger.logStdout.warning("AIDL Hash Check Skipped for "+meta_dict[u'name'])
            elif hashmetaerror:
                if PLUGIN_STATE_WARNING:
                    UtilFunctions.print_violations_on_stdout(LOG_TAG,meta_dict[u'name'],cmpMtdtQIIFACMDIntfHash.__name__,reason,False)
                else:
                    UtilFunctions.print_violations_on_stdout(LOG_TAG,meta_dict[u'name'],cmpMtdtQIIFACMDIntfHash.__name__,reason)
                return
        elif meta_dict[u'hashes'] != goldencmd_dictrlst[u'hashes']:
            diffinmetavalue=[]
            diffingoldcmdvalue=[]
            metahashkeyset = set()
            metahashvalueset = set()
            goldencmdhashkeyset = set()
            goldencmdhashvalueset = set()
            samevaluesets = set()
            if len(meta_dict[u'hashes']) > 0:
                if type(meta_dict[u'hashes']) is OrderedDict or type(meta_dict[u'hashes']) is dict:
                    metahashvalueset = set(meta_dict[u'hashes'].values())
                    metahashkeyset = set(meta_dict[u'hashes'].keys())
                elif type(cmpMtdtQIIFACMDIntfHash) is list:
                    metahashvalueset = set(meta_dict[u'hashes'])
                    metahashkeyset = set(meta_dict[u'hashes'])
            if len(goldencmd_dictrlst[u'hashes']) > 0:
                if type(goldencmd_dictrlst[u'hashes']) is OrderedDict or type(goldencmd_dictrlst[u'hashes']) is dict:
                    goldencmdhashvalueset = set(goldencmd_dictrlst[u'hashes'].values())
                    goldencmdhashkeyset = set(goldencmd_dictrlst[u'hashes'].keys())
                elif type(cmpMtdtQIIFACMDIntfHash) is list:
                    goldencmdhashvalueset = set(goldencmd_dictrlst[u'hashes'])
                    goldencmdhashkeyset = set(goldencmd_dictrlst[u'hashes'])
            samekeysets = metahashkeyset & goldencmdhashkeyset
            diffinmetavaluekey = list(samekeysets ^ metahashkeyset)
            diffingoldcmdvaluekey = list(samekeysets ^ goldencmdhashkeyset)
            samevaluesets = metahashvalueset & goldencmdhashvalueset
            diffinmetavalue = list(samevaluesets ^ metahashvalueset)
            diffingoldcmdvalue = list(samevaluesets ^ goldencmdhashvalueset)
            if image_type == "qssi":
                if len(diffinmetavaluekey) > 0 or len(diffinmetavalue) > 0:
                    hashnotfound = True
            else:
                hashnotfound = True
                if len(goldencmd_dictrlst[u'hashes']) > 0:
                    for key in goldencmd_dictrlst[u'hashes'].keys():
                        if len(meta_dict[u'hashes']) > 0 and key in meta_dict[u'hashes'].keys():
                            if meta_dict[u'hashes'][key] == goldencmd_dictrlst[u'hashes'][key]:
                                hashnotfound = False
        if hashnotfound:
            if (len(diffinmetavalue) > 0):
                Logger.logStdout.error("These are the hashses which are different on the META Side")
                Logger.logStdout.error(diffinmetavalue)
            if (len(diffingoldcmdvalue) > 0):
                Logger.logStdout.error("These are the hashses which are different on the QIIFA CMD Side")
                Logger.logStdout.error(diffingoldcmdvalue)
            reason="AIDL interface's hashes has been modified. Please run --create option before running IIC."
            if PLUGIN_STATE_WARNING:
                UtilFunctions.print_violations_on_stdout(LOG_TAG,meta_dict[u'name'],cmpMtdtQIIFACMDIntfHash.__name__,reason,False)
            else:
                UtilFunctions.print_violations_on_stdout(LOG_TAG,meta_dict[u'name'],cmpMtdtQIIFACMDIntfHash.__name__,reason)
            return
    except Exception as e:
        traceback.print_exc()
        Logger.logStdout.error(e)
        hashmetaerror = False
        if "hashes" in meta_dict.keys() and "hashes" not in goldencmd_dictrlst.keys():
            hashmetaerror = True
            reason="AIDL interface's hashes is present in META data but not present in QIIFA CMD. Please run --create option before running IIC."
        elif "hashes" not in meta_dict.keys() and "hashes" in goldencmd_dictrlst.keys() :
            if len(goldencmd_dictrlst[u'hashes'].values()) > 1:
                reason="AIDL interface's hashes is not present in META data but present in QIIFA CMD. Please run --create option before running IIC."
                hashmetaerror = True
        else:
            reason="AIDL interface's hashes is not present in META data but not present in QIIFA CMD. Please run --create option before running IIC."
            hashmetaerror = True
        if meta_dict[u'name'] in AIDL_SKIPPED_LST[u'HASH_CHECK']:
            Logger.logStdout.warning("AIDL Hash Check Skipped for "+meta_dict[u'name'])
        elif hashmetaerror:
            if PLUGIN_STATE_WARNING:
                UtilFunctions.print_violations_on_stdout(LOG_TAG,meta_dict[u'name'],cmpMtdtQIIFACMDIntfHash.__name__,reason,False)
            else:
                UtilFunctions.print_violations_on_stdout(LOG_TAG,meta_dict[u'name'],cmpMtdtQIIFACMDIntfHash.__name__,reason)
            return

def cmpMtdtQIIFACMDIntfVer(meta_dict,goldencmd_dictrlst,image_type, superimgpath_enable):
    versionnotfound = False
    try:
        if len(AIDL_SKIPPED_LST) > 0 and meta_dict[u'name'] in AIDL_SKIPPED_LST[u'VERSION_CHECK']:
            Logger.logStdout.warning("AIDL Version Check Skipped for "+meta_dict[u'name'])
        elif u'versions' not in meta_dict.keys() or u'versions' not in goldencmd_dictrlst.keys():
            versionerror = False
            if "versions" in meta_dict.keys() and "versions" not in goldencmd_dictrlst.keys():
                versionerror = True
                reason="AIDL interface's versions is present in META data but not present in QIIFA CMD. Please run --create option before running IIC."
            elif "versions" not in meta_dict.keys() and "versions" in goldencmd_dictrlst.keys() :
                if len(goldencmd_dictrlst[u'versions']) > 1:
                    reason="AIDL interface's versions is not present in META data but present in QIIFA CMD. Please run --create option before running IIC."
                    versionerror = True
            else:
                reason="AIDL interface's versions is not present in META data but not present in QIIFA CMD. Please run --create option before running IIC."
                versionerror = True

            if versionerror:
                if PLUGIN_STATE_WARNING:
                    Logger.logInternal.info("ABI VERSION TAG NOT PRESENT FOR " + meta_dict[u'name'])
                else:
                    UtilFunctions.print_violations_on_stdout(LOG_TAG,meta_dict[u'name'],cmpMtdtQIIFACMDIntfVer.__name__,reason)
                return
        elif meta_dict[u'versions'] != goldencmd_dictrlst[u'versions']:
            metahashvalueset = set()
            goldencmdhashvalueset = set()
            samevaluesets = set()
            diffinmetavalue = []
            diffingoldcmdvalue = []
            metahashvalueset = set(meta_dict[u'versions'])
            goldencmdhashvalueset = set(goldencmd_dictrlst[u'versions'])
            samevaluesets = metahashvalueset & goldencmdhashvalueset
            diffinmetavalue = list(samevaluesets ^ metahashvalueset)
            diffingoldcmdvalue = list(samevaluesets ^ goldencmdhashvalueset)
            if image_type == "qssi":
                if len(diffinmetavalue) > 0:
                    versionnotfound = True
            else:
                versionnotfound = True
                if len(goldencmd_dictrlst[u'versions']) > 0 and len(meta_dict[u'versions']) > 0:
                    for golden_version in goldencmd_dictrlst[u'versions']:
                        for meta_version in meta_dict[u'versions']:
                            if golden_version == meta_version:
                                versionnotfound = False
        if versionnotfound:
            if (len(diffinmetavalue) > 0):
                Logger.logStdout.error("These are the version which are different on the META Side")
                Logger.logStdout.error(diffinmetavalue)
            if (len(diffingoldcmdvalue) > 0):
                Logger.logStdout.error("These are the version which are different on the QIIFA CMD Side")
                Logger.logStdout.error(diffingoldcmdvalue)
            reason="AIDL interface's versions has been modified. Please run --create option before running IIC."
            if PLUGIN_STATE_WARNING:
                UtilFunctions.print_violations_on_stdout(LOG_TAG,meta_dict[u'name'],cmpMtdtQIIFACMDIntfVer.__name__,reason,False)
            else:
                UtilFunctions.print_violations_on_stdout(LOG_TAG,meta_dict[u'name'],cmpMtdtQIIFACMDIntfVer.__name__,reason)
            return
    except Exception as e:
        traceback.print_exc()
        Logger.logStdout.error(e)
        versionerror = False
        if "versions" in meta_dict.keys() and "versions" not in goldencmd_dictrlst.keys():
            versionerror = True
            reason="AIDL interface's versions is present in META data but not present in QIIFA CMD. Please run --create option before running IIC."
        elif "versions" not in meta_dict.keys() and "versions" in goldencmd_dictrlst.keys() :
            if len(goldencmd_dictrlst[u'versions']) > 1:
                reason="AIDL interface's versions is not present in META data but present in QIIFA CMD. Please run --create option before running IIC."
                versionerror = True
        else:
            reason="AIDL interface's versions is not present in META data but not present in QIIFA CMD. Please run --create option before running IIC."
            versionerror = True
        if versionerror:
            if PLUGIN_STATE_WARNING:
                Logger.logInternal.info("ABI VERSION TAG NOT PRESENT FOR " + meta_dict[u'name'])
            else:
                UtilFunctions.print_violations_on_stdout(LOG_TAG,meta_dict[u'name'],cmpMtdtQIIFACMDIntfVer.__name__,reason)
            return

def cmpMtdtQIIFACMDIntfVerPFMap(meta_dict,goldencmd_dictrlst,image_type, superimgpath_enable):
    if superimgpath_enable:
        return
    versionpfvmapnotfound = False
    try:
        if len(AIDL_SKIPPED_LST) > 0 and meta_dict[u'name'] in AIDL_SKIPPED_LST[u'VERSIONPFMAP_CHECK']:
            Logger.logStdout.warning("AIDL Version Check Skipped for "+meta_dict[u'name'])
        elif u'version_pfv_map' not in meta_dict.keys() or u'version_pfv_map' not in goldencmd_dictrlst.keys():
            versionerror = False
            if "version_pfv_map" in meta_dict.keys() and "version_pfv_map" not in goldencmd_dictrlst.keys():
                versionerror = True
                reason="AIDL interface's Version and platform version map is present in META data but not present in QIIFA CMD. Please run --create option before running IIC."
            elif "version_pfv_map" not in meta_dict.keys() and "version_pfv_map" in goldencmd_dictrlst.keys() :
                if len(goldencmd_dictrlst[u'version_pfv_map']) > 1:
                    reason="AIDL interface's Version and platform version map is not present in META data but present in QIIFA CMD. Please run --create option before running IIC."
                    versionerror = True
            else:
                reason="AIDL interface's Version and platform version map is not present in META data but not present in QIIFA CMD. Please run --create option before running IIC."
                versionerror = True
            if versionerror:
                if PLUGIN_STATE_WARNING:
                    Logger.logInternal.info("ABI VERSION TAG NOT PRESENT FOR " + meta_dict[u'name'])
                else:
                    UtilFunctions.print_violations_on_stdout(LOG_TAG,meta_dict[u'name'],cmpMtdtQIIFACMDIntfVerPFMap.__name__,reason)
                return
        elif meta_dict[u'version_pfv_map'] != goldencmd_dictrlst[u'version_pfv_map']:
            versionpfvmapnotfound = False
            metaversionpfvmapkeylst = []
            goldencmdversionpfvmapkeylst = []
            diffinmetaversion = []
            diffingoldencmdversion = []
            for key in meta_dict[u'version_pfv_map'].keys():
                metaversionpfvmapkeylst.append(int(key))
            for key in goldencmd_dictrlst[u'version_pfv_map'].keys():
                goldencmdversionpfvmapkeylst.append(int(key))
            if meta_dict[u'versions'] != metaversionpfvmapkeylst:
                for version in meta_dict[u'versions']:
                    if version not in diffinmetaversion and version not in metaversionpfvmapkeylst:
                        diffinmetaversion.append(int(version))
            if goldencmd_dictrlst[u'versions'] != goldencmdversionpfvmapkeylst:
                for version in meta_dict[u'versions']:
                    if version not in diffingoldencmdversion and version not in goldencmdversionpfvmapkeylst:
                        diffingoldencmdversion.append(int(version))
            min_platform_version = 99999999
            max_platform_version = 0
            diffverpfvmapdict = {}
            old_pfv = 0
            old_version = 0
            count = 0
            if len(diffingoldencmdversion) == 0:
                for versionkey in goldencmd_dictrlst[u'versions']:
                    min_platform_version = min(int(goldencmd_dictrlst[u'version_pfv_map'][str(versionkey)]),min_platform_version)
                    max_platform_version = min(int(goldencmd_dictrlst[u'version_pfv_map'][str(versionkey)]),max_platform_version)
                    if old_pfv > int(goldencmd_dictrlst[u'version_pfv_map'][str(versionkey)]):
                        diffverpfvmapdict[int(old_version)] = int(goldencmd_dictrlst[u'version_pfv_map'][str(old_version)])
                    if count == 0 and int(goldencmd_dictrlst[u'version_pfv_map'][str(versionkey)]) > PFV_MIN_VAL:
                        diffverpfvmapdict[int(versionkey)] = int(goldencmd_dictrlst[u'version_pfv_map'][str(versionkey)])
                    if min_platform_version < Constants.AIDL_MIN_PLATFORM_VERSION:
                        diffverpfvmapdict[int(versionkey)] = int(goldencmd_dictrlst[u'version_pfv_map'][str(versionkey)])
                    old_pfv = int(goldencmd_dictrlst[u'version_pfv_map'][str(versionkey)])
                    old_version = int(versionkey)
                    count = count + 1
            if image_type == "qssi":
                if len(diffinmetaversion) > 0 or len(diffingoldencmdversion) > 0 or len(diffverpfvmapdict) > 0:
                    versionpfvmapnotfound = True
        if versionpfvmapnotfound:
            if (len(diffinmetaversion) > 0):
                Logger.logStdout.error("These are the versions in versions with platform version map which are different on the META Side")
                Logger.logStdout.error(diffinmetaversion)
            if (len(diffingoldencmdversion) > 0):
                Logger.logStdout.error("These are the versions in versions with platform version map which are different on the QIIFA CMD Side")
                Logger.logStdout.error(diffingoldencmdversion)
            if (len(diffverpfvmapdict) > 0):
                Logger.logStdout.error("These are the version with platform version map modified. Please check")
                Logger.logStdout.error(diffverpfvmapdict)
            reason="AIDL interface's Version with platform version map has been modified. Please run --create option before running IIC."
            if PLUGIN_STATE_WARNING:
                UtilFunctions.print_violations_on_stdout(LOG_TAG,meta_dict[u'name'],cmpMtdtQIIFACMDIntfVerPFMap.__name__,reason,False)
            else:
                UtilFunctions.print_violations_on_stdout(LOG_TAG,meta_dict[u'name'],cmpMtdtQIIFACMDIntfVerPFMap.__name__,reason)
            return
    except Exception as e:
        traceback.print_exc()
        Logger.logStdout.error(e)
        versionerror = False
        if "version_pfv_map" in meta_dict.keys() and "version_pfv_map" not in goldencmd_dictrlst.keys():
            versionerror = True
            reason="AIDL interface's Version with platform version map is present in META data but not present in QIIFA CMD. Please run --create option before running IIC."
        elif "version_pfv_map" not in meta_dict.keys() and "version_pfv_map" in goldencmd_dictrlst.keys() :
            if len(goldencmd_dictrlst[u'version_pfv_map']) > 1:
                reason="AIDL interface's Version with platform version map is not present in META data but present in QIIFA CMD. Please run --create option before running IIC."
                versionerror = True
        else:
            reason="AIDL interface's Version with platform version map is not present in META data but not present in QIIFA CMD. Please run --create option before running IIC."
            versionerror = True
        if versionerror:
            if PLUGIN_STATE_WARNING:
                Logger.logInternal.info("ABI VERSION TAG NOT PRESENT FOR " + meta_dict[u'name'])
            else:
                UtilFunctions.print_violations_on_stdout(LOG_TAG,meta_dict[u'name'],cmpMtdtQIIFACMDIntfVerPFMap.__name__,reason)
            return
def chkcmdSorted(goldencmddict_list,filename):
    filename = filename+".json"
    try:
        sorteddict_list = sorted(goldencmddict_list, key=lambda d: d[u'name'])
        listcount = len(goldencmddict_list)
        for key in range(listcount):
            if goldencmddict_list[key] != sorteddict_list[key]:
                reason = "The following QIIFA CMD is not sorted. Please check go/qiifa to get instructions for adding interface. Please contact qiifa-squad for more information."
                if PLUGIN_STATE_WARNING:
                    UtilFunctions.print_violations_on_stdout(LOG_TAG,filename,chkcmdSorted.__name__,reason,False)
                else:
                    UtilFunctions.print_violations_on_stdout(LOG_TAG,filename,chkcmdSorted.__name__,reason)
                return True
    except Exception as e:
        traceback.print_exc()
        Logger.logStdout.error(e)
        reason = "Error in the QIIFA CMD check sort function. Please contact qiifa-squad for more information"
        if PLUGIN_STATE_WARNING:
            UtilFunctions.print_violations_on_stdout(LOG_TAG,filename,chkcmdSorted.__name__,reason,False)
        else:
            UtilFunctions.print_violations_on_stdout(LOG_TAG,filename,chkcmdSorted.__name__,reason)
        return True
    return False

def aidlSuperImgChk(qssi_path, target_path):
    '''
    Description: This defination runs image to image checker
    Type       : Internal defination
    '''
    target_dir = "target"
    if not os.path.exists(target_dir):
        os.makedirs(target_dir)

    qssi_dir = "qssi"
    if not os.path.exists(qssi_dir):
        os.makedirs(qssi_dir)

    #Fetch Manifests
    files = glob.glob(target_path)
    for file in files:
        with ZipFile(file, 'r') as zipObj:
            for file_info in zipObj.infolist():
                if file_info.filename.startswith('VENDOR/etc/vintf'):
                    zipObj.extract(file_info, target_dir)

    files = glob.glob(qssi_path)
    for file in files:
        with ZipFile(file, 'r') as zipObj:
            for file_info in zipObj.infolist():
                if file_info.filename.startswith(('SYSTEM/etc/vintf', 'SYSTEM_EXT/etc/vintf', 'PRODUCT/etc/vintf')):
                    zipObj.extract(file_info, qssi_dir)

    qssi_path = qssi_dir
    target_path = target_dir
    global FCM_XML_DICT,FCM_VENDOR_TL,FCM_GLOBAL_DICT,FCM_MAX_KEY
    qssi_lst=[]
    target_lst=[]
    qssi_paths_of_xml_files = [os.path.join(qssi_path,"SYSTEM/etc/vintf"),os.path.join(qssi_path,"SYSTEM_EXT/etc/vintf"),os.path.join(qssi_path,"PRODUCT/etc/vintf")]
    vendor_paths_of_xml_files = [os.path.join(target_path,"VENDOR/etc/vintf")]
    for directory in qssi_paths_of_xml_files:
        if UtilFunctions.pathExists(directory):
            GenQSSIMaxFCMPlatVersMap(directory)
            UpdXmltoDict(FCM_GLOBAL_DICT,directory)
    QSSI_FCM_MAX = FCM_MAX_KEY
    qssi_mn_dict = FCM_XML_DICT.copy()

    FCM_XML_DICT = {}
    FCM_GLOBAL_DICT = {}
    for directory in vendor_paths_of_xml_files:
        if UtilFunctions.pathExists(directory):
            GenVndrTarLvlPlatVersMap(directory)
            UpdXmltoDict(FCM_GLOBAL_DICT,directory)
    target_mn_dict = FCM_XML_DICT.copy()

    if qssi_mn_dict and target_mn_dict:
        try:
            qssi_lst = []
            target_lst=[]
            for target_key in target_mn_dict.keys():
                if "manifest" in target_key:
                    if type(target_mn_dict[target_key]) is list and len(target_mn_dict[target_key]) > 0:
                        count=0
                        for target_dict in target_mn_dict[target_key]:
                            if (type(target_dict) is OrderedDict or type(target_dict) is dict) and u'@format' in target_dict.keys() and target_dict[u'@format']=="aidl":
                                if count==0:
                                    count = count+1
                                    Logger.logStdout.info("Currently reading the following FCM : compatibility_matrix."+str(FCM_VENDOR_TL)+".xml")
                                for qssi_dict in qssi_mn_dict[qssi_path+"/SYSTEM/etc/vintf/compatibility_matrix."+str(FCM_VENDOR_TL)+".xml"]:
                                    if qssi_dict[u'@format']=="aidl" and qssi_dict[u'name'] == target_dict[u'name']:
                                        compareSuperImgVer(qssi_dict,target_dict)
                                        break

                            if (type(target_dict) is OrderedDict or type(target_dict) is dict) and u'@format' in target_dict.keys() and target_dict[u'@format']=="aidl" and int(QSSI_FCM_MAX) > int(FCM_VENDOR_TL):
                                if count==1:
                                    count = count+1
                                    Logger.logStdout.info("Currently reading the following FCM : compatibility_matrix."+str(QSSI_FCM_MAX)+".xml")
                                for qssi_dict in qssi_mn_dict[qssi_path+"/SYSTEM/etc/vintf/compatibility_matrix."+str(QSSI_FCM_MAX)+".xml"]:
                                    if qssi_dict[u'@format']=="aidl" and qssi_dict[u'name'] == target_dict[u'name']:
                                        compareSuperImgVer(qssi_dict,target_dict)
                                        break
                compatibility_matrix = qssi_path+"/SYSTEM/etc/vintf/compatibility_matrix.device.xml"
                if "manifest" in target_key and compatibility_matrix in qssi_mn_dict.keys():
                    qssi_lst = qssi_mn_dict[compatibility_matrix]
                    target_lst = target_mn_dict[target_key]
                    if len(qssi_lst)<= 0 or len(target_lst)<=0:
                        reason="Either compatibilty_matrix.device.xml on QSSI side or Manifests on target side are not present"
                        if PLUGIN_STATE_WARNING:
                            Logger.logStdout.error(reason)
                            UtilFunctions.print_violations_on_stdout(LOG_TAG,"Manifest are not present for QSSI / Target",aidlSuperImgChk.__name__,reason,False)
                        else:
                            UtilFunctions.print_violations_on_stdout(LOG_TAG,"Manifest are not present for QSSI / Target",aidlSuperImgChk.__name__,reason)
                            return
                    else:
                        for target_dict in target_lst:
                            if(target_dict[u'name'].startswith("vendor.") and target_dict[u'@format']=="aidl"):
                                interface_found = False
                                for qssi_dict in qssi_lst:
                                    if target_dict[u'name'] == qssi_dict[u'name'] and target_dict!=qssi_dict:
                                        interface_found = True
                                        compareSuperImgVer(qssi_dict, target_dict, True)
                                        break
                                if not interface_found:
                                    QSSI_AIDL_NOTFND.append(target_dict[u'name'])
                        if QSSI_AIDL_NOTFND:
                            Logger.logStdout.error("Interfaces are not present or there is a version mismatch on the QSSI side : %s",QSSI_AIDL_NOTFND)
                            Logger.logStdout.error("Vendor is ahead of QSSI. Please make sure use latest QSSI AU or tip of QSSI")
                            sys.exit(1)
        except Exception as e:
            traceback.print_exc()
            Logger.logStdout.error(e)
            sys.exit(0)
    else:
        Logger.logStdout.error("Manifests on qssi or target side doesn't exist.")
        sys.exit(1)
    pass

def compareSuperImgVer(qssi_dict,vendor_dict,compareverison = False):
    vendorINTFVer = 1
    qssiINTFVer = 1
    PLUGIN_STATE_WARNING = True
    if u'version' in vendor_dict.keys():
        vendorINTFVer = vendor_dict[u'version']
    if u'version' in qssi_dict.keys():
        qssiINTFVer = qssi_dict[u'version']
    if not type(qssiINTFVer) is int and not qssiINTFVer.isdigit() and "-" in qssiINTFVer:
        qssiINTFVer = qssiINTFVer.split("-")
        qssiINTFVer = range(int(qssiINTFVer[0]),int(qssiINTFVer[1])+1)
    elif not type(qssiINTFVer) is int and qssiINTFVer.isdigit():
        qssiINTFVer = int(qssiINTFVer)
    if not type(vendorINTFVer) is int and not vendorINTFVer.isdigit() and "-" in vendorINTFVer:
        vendorINTFVer = vendorINTFVer.split("-")
        vendorINTFVer = range(int(vendorINTFVer[0]),int(vendorINTFVer[1])+1)
    elif not type(vendorINTFVer) is int and vendorINTFVer.isdigit():
        vendorINTFVer = int(vendorINTFVer)
    reason = None
    if type(qssiINTFVer) is int and type(vendorINTFVer) is int and vendorINTFVer > qssiINTFVer :
        if compareverison:
            QSSI_AIDL_NOTFND.append(vendor_dict[u'name'])
        reason = "The interface manifest version ("+str(vendorINTFVer)+")  is greater than the corresponding FCM interface version ("+str(qssiINTFVer)+")"
    elif type(qssiINTFVer) is int and type(vendorINTFVer) is int and vendorINTFVer < qssiINTFVer:
        reason = "The interface manifest version ("+str(vendorINTFVer)+")  is less than the corresponding FCM interface version ("+str(qssiINTFVer)+")"
    elif type(qssiINTFVer) is list and not type(vendorINTFVer) is list:
        if int(vendorINTFVer) not in qssiINTFVer:
            if compareverison:
                QSSI_AIDL_NOTFND.append(vendor_dict[u'name'])
            qssiINTFVer = ' '.join(map(str, qssiINTFVer))
            reason = "The interface manifest version ("+str(vendorINTFVer)+")  is not matching the available FCM interface versions ("+str(qssiINTFVer)+")"
    elif not type(qssiINTFVer) is list and type(vendorINTFVer) is list:
        if int(qssiINTFVer) not in vendorINTFVer:
            if compareverison:
                QSSI_AIDL_NOTFND.append(vendor_dict[u'name'])
            vendorINTFVer = ' '.join(map(str, vendorINTFVer))
            reason = "The interface mainfest versions ("+str(vendorINTFVer)+")  is not matching the FCM interface version ("+str(qssiINTFVer)+")"
    if reason != None and not compareverison:
        if PLUGIN_STATE_WARNING:
            UtilFunctions.print_violations_on_stdout(LOG_TAG,qssi_dict[u'name'],compareSuperImgVer.__name__,reason,False)
        else:
            UtilFunctions.print_violations_on_stdout(LOG_TAG,qssi_dict[u'name'],compareSuperImgVer.__name__,reason)


def intf_not_found_lst(main_lst,compare_lst):
    '''Compare the list of dictionaries and if the
        name present in main list is not present in
        compare list. we collect the name of interfaces not
        found and return it as a list  '''
    found_name=False
    comapre_lst_not_found=[]
    for main_dict in main_lst:
        found_name=False
        for compare_dict in compare_lst:
            if compare_dict[u'name'] == main_dict[u'name']:
                found_name=True
        if not found_name:
            comapre_lst_not_found.append(str(main_dict[u'name']))
    return comapre_lst_not_found


def is_intf_modified(meta_dict, goldencmd_dict):
    '''
    Compare the meta dictionary and the golden golden cmd dictionary
    and return true if something has changed
    '''
    if goldencmd_dict != meta_dict:
        return True
    return False

def check_dup_intf_name(goldenlist, intf_name, find_dup):
    dup_intf_lst=[]
    for goldendict in goldenlist:
        if goldendict[u'name'] == intf_name:
            if find_dup:
                dup_intf_lst.append(goldendict)
            else:
                return goldendict
    '''
    Check if Interface is present
    '''
    if len(dup_intf_lst) == 0:
        return None
    elif len(dup_intf_lst) == 1:
        return dup_intf_lst[0]
    return dup_intf_lst

def skipintf_chk(intf_name):
    string_concatenate = ''
    for words in intf_name.split('.'):
        string_concatenate += words
        if string_concatenate in Constants.AIDL_ALLWED_INTF_PREF:
            return False
        string_concatenate += '.'
    return True

class qiifa_aidl:
    def __init__(self):
         pass
    start_qiifa_aidl_checker = aidlIICMain
'''
plugin class implementation
plugin class is derived from plugin_interface class
Name of plugin class MUST be MyPlugin
'''
class MyPlugin(plugin_interface):
    def __init__(self):
        pass

    def register(self):
        return Constants.AIDL_SUPPORTED_CREATE_ARGS

    def config(self, QIIFA_type=None, libsPath=None, CMDPath=None):
        pass

    def generateGoldenCMD(self, libsPath=None, storagePath=None, create_args_lst=None):
        '''
        the assumption here is that if create_args_lst is empty, then this was called under the
        circumstance where --create arg was called with "all" option; so it should behave as if
        --create was called with "aidl" option.
        '''
        if create_args_lst is None or create_args_lst[0] == Constants.AIDL_SUPPORTED_CREATE_ARGS[0]:
            aidlCreateMain(self, "golden", Constants.AIDL_SUPPORTED_CREATE_ARGS[0])
        #This is used to create a list of Interfaces currently available in the max available compatibility matrix
        elif create_args_lst[0] == Constants.AIDL_SUPPORTED_CREATE_ARGS[3]:
            aidlCreateMain(self, "listgen", Constants.AIDL_SUPPORTED_CREATE_ARGS[3])
        #In this case create_args_lst[1] will have the particular intf name
        elif create_args_lst[0] in Constants.AIDL_SUPPORTED_CREATE_ARGS[1:3]:
            if len(create_args_lst) != 2:
                reason = "Interface Name not provided. Please check the create command help. Check python qiifa_main.py -h for more info"
                UtilFunctions.print_violations_on_stdout(LOG_TAG,"generateGoldenCMD",MyPlugin.generateGoldenCMD.__name__,reason)
                sys.exit(1)
            aidlCreateMain(self, "golden", create_args_lst[0], create_args_lst[1])
        else:
            Logger.logStdout.info("Invalid --create argument options")
            Logger.logStdout.info("python qiifa_main.py -h")
            sys.exit()

    def IIC(self,iic_args_list = None, **kwargs):
        if Constants.IS_CUSTOMER_VARIANT or not os.path.isdir(Constants.qiifa_tools_path):
            print("Skipping QIIFA AIDL for Customer Build")
            return
        if iic_args_list is None or iic_args_list[0] == Constants.AIDL_SUPPORTED_CREATE_ARGS[0]:
            if "target_path" not in kwargs.keys():
                aidlIICMain(self,"check",Constants.AIDL_SUPPORTED_CREATE_ARGS[0])
            elif "target_path" in kwargs.keys():
                aidlIICMain(self,"check",Constants.AIDL_SUPPORTED_CREATE_ARGS[0],None,kwargs[u'qssi_path'],kwargs[u'target_path'])
        elif iic_args_list[0] in Constants.AIDL_SUPPORTED_CREATE_ARGS[4:6]:
            if len(iic_args_list) != 2:
                reason = "Interface Name not provided. Please check the Type command help. Check python qiifa_main.py -h for more info"
                UtilFunctions.print_violations_on_stdout(LOG_TAG,"IIC",MyPlugin.IIC.__name__,reason)
                sys.exit(1)
            aidlIICMain(self, "check", iic_args_list[0], iic_args_list[1])
        else:
            Logger.logStdout.info("Invalid --type argument options")
            Logger.logStdout.info("python qiifa_main.py -h")
            sys.exit()
