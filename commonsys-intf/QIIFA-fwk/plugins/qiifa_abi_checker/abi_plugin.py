#!/usr/bin/python
# -*- coding: utf-8 -*-
#Copyright (c) 2019-2020 Qualcomm Technologies, Inc.
#All Rights Reserved.
#Confidential and Proprietary - Qualcomm Technologies, Inc.
import sys,os,subprocess,time,json,re,threading,multiprocessing
import xml.etree.ElementTree as ET
'''Import local utilities'''
from qiifa_util.util import UtilFunctions, Constants, Logger
'''
Import plugin_interface base module
'''
from plugins.qiifa_abi_checker.cd_wrapper import read_techpackdata_from_CD_XML
from plugin_interface import plugin_interface
#Globals
module_info_dict ={}
lsdump_path_info_dict = {}
LOG_TAG = "abi_plugin"

def createStringForIncludes(
    library_inc_dict):
    include_string =""
    for includes in library_inc_dict:
      if(includes == "-isystem" or "bionic" in includes):
        continue
      else:
        include_string += "-I" + Constants.croot + "/" + includes + " "
    return include_string

def createStringForSystemIncludeHeaders():
    system_include_headers = ""
    for include_headers in Constants.sdump_system_include_headers:
      system_include_headers += " -isystem " + Constants.croot + "/" +include_headers
    return system_include_headers

def createStringForExportHeaders(export_headers):
    export_headers_string = ""
    for header in export_headers:
      export_headers_string += "-I" + header + " "
    return export_headers_string

def createStringForList(list_):
    converted_string = ""
    for item in list_:
      converted_string += item  + " "
    return converted_string

def createDirectory(path):
    if not os.path.exists(path):
      os.makedirs(path)
      Logger.logInternal.info("Directory " +  path + " Created ")
    else:
      Logger.logInternal.info("Directory " + path +  " already exists")

def loadModuleInfoDictFromJSONFile():
    module_info_file_handle = open(Constants.module_info_file_path, "r")
    module_info_dict = {}
    ##TODO Break build if any parsing error once QIIFA is enforced
    try:
     module_info_dict = json.load(module_info_file_handle)
    except Exception as e:
      sys.exit(1)
    return module_info_dict

def checkIf32and64ArchVersionExists(library):
    ## Optimize this logic , not a good one
    arch_map = {Constants.arch32:False,Constants.arch64:False}
    if "/lib64/" in library :
      if os.path.exists(Constants.out_path + library):
        arch_map[Constants.arch64] = True
      if os.path.exists(Constants.out_path + library.replace("/lib64/","/lib/")):
        arch_map[Constants.arch32] = True
    elif "/lib/" in library :
      if os.path.exists(Constants.out_path + library):
        arch_map[Constants.arch32] = True
      if os.path.exists(Constants.out_path + library.replace("/lib/","/lib64/")):
        arch_map[Constants.arch64] = True
    return arch_map

def createDirectoryStructureForAbiDumps(path):
    createDirectory(path + "/abidumps/" + Constants.arch64)
    createDirectory(path + "/abidumps/" + Constants.arch32)

def generateLsDumpForLibrary(arch_type, library_dict):
    library_src_dict = library_dict["srcs"]
    library_inc_dict = library_dict["incs"]
    library_cflags   = library_dict["cflags"]
    source_dir_path  = library_dict["path"][0]
    include_headers  = createStringForIncludes(library_inc_dict)
    system_headers   = createStringForSystemIncludeHeaders()
    library_cflags   = createStringForCflags(library_cflags)

def generateLsdump(lib_lsdump_info, library_export_headers):
    lsdump_out_path = lib_lsdump_info["lsdump_dir"]
    library_name = lib_lsdump_info["name"]
    sdump_dir = lib_lsdump_info["sdump_dir"]
    lsdump_command_string = ""
    lsdump_db = ""
    if(Constants.qiifa_golden_abi_dump_folder in lsdump_out_path):
      lsdump_db = Constants.golden_db
    elif(Constants.qiifa_current_abi_dump_folder in lsdump_out_path):
      lsdump_db = Constants.current_db
    lsdump_command_string += Constants.header_abi_linker + " " + library_export_headers + " -o " + lsdump_out_path + "/" + library_name + ".lsdump" + " "
    for file in os.listdir(sdump_dir):
      lsdump_command_string += sdump_dir + "/" + file + " "
    arch_arm64_flags = " -arch " + Constants.arch64
    arch_arm32_flags = " -arch " + Constants.arch32

    if lib_lsdump_info["arch"] == Constants.arch64:
      lsdump_command_string += " -so " + Constants.out_path + lib_lsdump_info["lib_path"]
      lsdump_command_string += arch_arm64_flags
    if lib_lsdump_info["arch"] == Constants.arch32:
      lib_path = str(lib_lsdump_info["lib_path"])
      lib_path = lib_path.replace("lib64","lib")
      lsdump_command_string += " -so " + Constants.out_path + lib_path
      lsdump_command_string += arch_arm32_flags
    try:
      command_run = subprocess.call(lsdump_command_string, shell=True)
      if command_run == 0:
          log_info = "Lsdump has been successfully generated for " + lib_lsdump_info["arch"] + " library " + library_name
          UtilFunctions.print_info_on_stdout(LOG_TAG, lsdump_db + " " + log_info)
      else:
          print("LSdump Generation failed with command : " + lsdump_command_string)
          sys.exit(-1)
    except subprocess.CalledProcessError as e:
      Logger.logInternal.warning(e.output)

def generateSdump(source_file_path, output_file_path, library_sdump_info,arch):
    success_bit = True
    sdump_gen_rule = [ Constants.header_abi_dumper,
                       "--suppress-errors",
                       "-o",
                       output_file_path,
                       source_file_path,
                       library_sdump_info["export"],
                       "--",
                       library_sdump_info["includes"],
                       Constants.sdump_system_cflags,
                       library_sdump_info["system_headers"],
                       library_sdump_info["cflags"]]

    sdump_command_string = ""
    for subrule in sdump_gen_rule:
      sdump_command_string += subrule + " "

    if arch == Constants.arch64:
      sdump_command_string += Constants.sdump_64_bit_target + " -B " + Constants.sdump_64_bit_gcc + " -isystem " + Constants.sdump_system_asm_64_includes
    if arch == Constants.arch32:
      sdump_command_string += Constants.sdump_32_bit_target + " -B " + Constants.sdump_32_bit_gcc + " -isystem " + Constants.sdump_system_asm_32_includes
    try:
      command_run = subprocess.call(sdump_command_string, shell=True)
      if not command_run == 0:
          print("Sdump Generation failed with command : " + sdump_command_string)
          sys.exit(-1)
    except subprocess.CalledProcessError as e:
      Logger.logInternal.warning("QIIFA : FAILED")
      success_bit = False
    return success_bit

def generateAbiDumpForLibrary(library_info,path):
    success_bit = True
    abidump_path           = path
    library_name           = library_info["name"]
    library_dict           = library_info["dict"]
    library_arch_type      = library_info["arch_type"]
    library_static_dep     = library_info["staticdep"]
    library_srcs           = library_dict["srcs"]
    library_incs           = library_dict["incs"]
    library_cflags         = library_dict["cflags"]
    source_dir_path        = library_dict["path"][0]
    include_headers        = createStringForIncludes(library_incs)
    system_headers         = createStringForSystemIncludeHeaders()
    library_cflags         = createStringForList(library_cflags)
    library_export_headers = createStringForExportHeaders(library_dict["export"])
    library_sdump_info     = {"includes"       : include_headers,
                              "export"         : library_export_headers,
                              "system_headers" : system_headers,
                              "cflags"         : library_cflags}
    library_dir          = os.path.join(abidump_path,library_name)
    library_sdump_path   = os.path.join(library_dir,library_name + "-sdump")
    library_lsdump_path  = os.path.join(library_dir,library_name + "-lsdump")
    UtilFunctions.create_dir(library_dir)
    UtilFunctions.create_dir(library_sdump_path)
    UtilFunctions.create_dir(library_lsdump_path)

    ##TODO : Handle Case of File name Conflicts
    for source in library_srcs:
      source_file_path = os.path.join(Constants.croot, source_dir_path, source)
      output_file_path = os.path.join(library_sdump_path,os.path.splitext(source.split("/")[-1])[0] + ".sdump")
      generateSdump(source_file_path, output_file_path, library_sdump_info,library_arch_type)

    ## Handle Case where library has static dependencies
    ##TODO : Handle Case of File name Conflicts
    if library_static_dep :
      for static_dep in library_static_dep :
        source_dir_path = static_dep["dict"]["path"][0]
        library_sdump_info     = {"includes"       : static_dep["incs"],
                                  "export"         : library_export_headers,
                                  "system_headers" : system_headers,
                                  "cflags"         : static_dep["cflags"]}
        for source in static_dep["srcs"]:
           source_file_path = os.path.join(Constants.croot, source_dir_path, source)
           output_file_path = os.path.join(library_sdump_path, os.path.splitext(source.split("/")[-1])[0] + ".sdump")
           generateSdump(source_file_path, output_file_path, library_sdump_info,Constants.arch64)

    library_lsdump_info = {"name"       : library_name ,
                           "sdump_dir"  : library_sdump_path,
                           "lsdump_dir" : library_lsdump_path,
                           "arch"       : library_arch_type,
                           "lib_path"   : library_info["lib_path"]}
    generateLsdump(library_lsdump_info, library_export_headers)

#TODO Refaactor code to make it generic so it can be used (Remove path creation logic)
def generateABIdiffFilesForArch(new_lsdump_path,golden_lsdump_path,arch_type,output_abi_diff_path):
    #TODO Check for condition that lsdump doesn;t exist in new-abi but present in Golden
    #Check if lsdump folder exists
    createDirectory(output_abi_diff_path)
    if os.path.exists(golden_lsdump_path) and os.path.exists(new_lsdump_path):
      for lsdump_dir in os.listdir(new_lsdump_path):
        createDirectory(output_abi_diff_path)
        ## Create Path for new lsdump file
        new_lsdump_file_path = new_lsdump_path + "/" + lsdump_dir + "/" + lsdump_dir + "-lsdump" + "/" + lsdump_dir + ".lsdump"
        golden_lsdump_file_path = golden_lsdump_path + "/" + lsdump_dir + "/" + lsdump_dir + "-lsdump" + "/" + lsdump_dir + ".lsdump"
        if os.path.exists(new_lsdump_file_path) and os.path.exists(golden_lsdump_file_path):
          output_abidiff_file_path = output_abi_diff_path + "/" +  lsdump_dir + ".abidiff"
          ## Both File exists , we are good to start header-abi-diff command.
          ## Create command for header-abi-diff with all arguments.
          header_abi_diff_command = [ Constants.header_abi_diff,
                                      "-old",
                                      golden_lsdump_file_path,
                                      "-new",
                                      new_lsdump_file_path,
                                      "-arch",
                                      arch_type,
                                      "-o",
                                      output_abidiff_file_path,
                                      "-lib",
                                      lsdump_dir]
          header_abi_diff_serialized_command = ""
          for instruction in header_abi_diff_command:
            header_abi_diff_serialized_command += instruction + " "
          try:
              command_run = subprocess.call(header_abi_diff_serialized_command, shell=True)
              if not command_run == 0:
                  print(" Header Abi Diff Error !! Reason : (Incompatibility/Error executing diff!!)")
                  ##sys.exit(-1)
          except subprocess.CalledProcessError as e:
              Logger.logInternal.warning("QIIFA : FAILED")
        else:
            Logger.logInternal.warning("lsdump File doesnt't exist for generating diff")
            print("Lsdump files not found for generating diff!! Exiting Script")
            sys.exit(-1)
    else:
      print("Goldent/Current Lsdump path not found. Exiting")
      print("Golden : " + golden_lsdump_path)
      print("Current : " + new_lsdump_path)
      sys.exit(-1)

def generateABIdiffFiles(new_abi_dumps_path, golden_abi_dumps_path, output_abi_diff_path):
   UtilFunctions.create_dir(os.path.join(output_abi_diff_path,Constants.abidiff_string))
   ## Expected Input Directory Format for new_abi_dumps_path or golden_abi_dumps_path
   ## QIIFA
   ##  -abidumps
   ##    -<module>{Module : sp-hal , commonsys-intf}
   ##   -- arm32
   ##   |     --- lsdump_test
   ##   |         -- lsdump_test-lsdump
   ##   |          | -- lsdump_test.lsdump
   ##   |         -- lsdump_test-sdump
   ##   |              --- qiifa-static_dep.sdump
   ##   |              --- qiifa-static.sdump
   ##   |              --- qiifa.sdump
   ##   -- arm64
   ##         --- lsdump_test
   ##             -- lsdump_test-lsdump
   ##              | -- lsdump_test.lsdump
   ##             -- lsdump_test-sdump
   ##                  --- qiifa-static_dep.sdump
   ##                  --- qiifa-static.sdump
   ##                  --- qiifa.sdump
   abidump_metadata = Constants.abidump_metadata
   abidump_modules = abidump_metadata["modules"]
   for module in abidump_modules:
     UtilFunctions.create_dir(os.path.join(output_abi_diff_path,Constants.abidiff_string,module))
     UtilFunctions.create_dir(os.path.join(output_abi_diff_path,Constants.abidiff_string,module,Constants.arch64))
     UtilFunctions.create_dir(os.path.join(output_abi_diff_path,Constants.abidiff_string,module,Constants.arch32))
     golden_lsdump_path_32_bit = os.path.join(golden_abi_dumps_path,Constants.abidumps_string,module,Constants.arch32)
     golden_lsdump_path_64_bit = os.path.join(golden_abi_dumps_path,Constants.abidumps_string,module,Constants.arch64)
     new_lsdump_path_32_bit    = os.path.join(new_abi_dumps_path,Constants.abidumps_string,module,Constants.arch32)
     new_lsdump_path_64_bit    = os.path.join(new_abi_dumps_path,Constants.abidumps_string,module,Constants.arch64)
     generateABIdiffFilesForArch(new_lsdump_path_32_bit,golden_lsdump_path_32_bit,Constants.arch32,os.path.join(Constants.arch32,output_abi_diff_path,Constants.abidiff_string,module,Constants.arch32))
     generateABIdiffFilesForArch(new_lsdump_path_64_bit,golden_lsdump_path_64_bit,Constants.arch64, os.path.join(Constants.arch64,output_abi_diff_path,Constants.abidiff_string,module,Constants.arch64))

def get_dict_from_module_info(module_name):
  module ={}
  try:
    module = module_info_dict[module_name]
  except KeyError:
    Logger.logInternal.error("Module not present in module_info.json : " + module_name)
  return module


def run_pre_verify_check_for_lsdump(library_list,path):
  pre_verify_check = True
  ## Check if Library List is Empty
  if not library_list:
    Logger.logInternal.error("Library list is empty.")
    pre_verify_check = False
    return pre_verify_check
  ## Check if Export Header field is present in module_info_dict
  library_name = os.path.splitext(os.path.basename(str(library_list[0])))[0]
  library_dict = get_dict_from_module_info(library_name)
  if library_dict and library_dict["abi_checker"]:
    try:
      export_headers_list = library_dict["export"]
    except KeyError:
      Logger.logInternal.error("Export Header Information is not defined in Module Info Json File. \
      Exiting QIIFA-ABI Script.")
      pre_verify_check = False
      return pre_verify_check
  if not os.path.exists(path):
    Logger.logInternal.error("Path is not accessible : " + path)
    pre_verify_check = False
  return pre_verify_check

def check_for_Export_Headers(library_dict,library_name):
  export_check_flag = True
  export_header_message = "Skipping lsdump for Library as Export Headers \
  are not defined for " + library_name + ". \nPlese define export header \
  Information for " + library_name +  " (LOCAL_EXPORT_C_INCLUDES). \
  \n For detailed information please visit go/QIIFA."
  if not library_dict["export"]:
     Logger.logInternal.error(export_header_message)
     export_check_flag = False
  return export_check_flag

def create_metadata_for_lsdump(library_dict,arch_type,library,library_name):
  # Check for Static Dependencies
  library_static_dep_list = library_dict["static"]
  library_whole_static_dep_list = library_dict["wstatic"]
  serialzed_static_dep_list = []
  #TODO:Disabling Static Library Dependency Inclusin in Sdump generation.
  #Including static deps for all libraries significantly increases sdump generation
  #time which is overkill for system. Need to come up with a opt in mechanism.
  #if len(library_static_dep_list)> 0 or len(library_whole_static_dep_list) > 0:
  #  handleStaticDependencies(library_dict,module_info_dict,serialzed_static_dep_list)
  library_info  = {"dict"       : library_dict,
                   "arch_type"  : arch_type ,
                   "name"       : library_name,
                   "lib_path"   : library,
                   "staticdep"  : serialzed_static_dep_list}
  return library_info


def generateAbiDumpForTechPackageLibrary(libraryInfo, abidump_path):
    if(not os.path.exists(Constants.lsdump_paths_file_path)):
      Logger.logInternal.info("lsdump_paths.txt is not present at %s ",Constants.lsdump_paths_file_path)
      return
    library_name = libraryInfo["name"]
    library_dir          = os.path.join(abidump_path,library_name)
    library_lsdump_path  = os.path.join(library_dir,library_name + "-lsdump")
    UtilFunctions.create_dir(library_dir)
    UtilFunctions.create_dir(library_lsdump_path)
    with open(Constants.lsdump_paths_file_path, 'r') as lsdump_paths_file:
        for line in lsdump_paths_file:
            tag, path = (x.strip() for x in line.split(':', 1))
            if(libraryInfo["name"] in path):
                if(libraryInfo["arch_type"] == Constants.arch64 and Constants.arm64_armv8_a in path):
                     full_path = os.path.join(Constants.croot, path)
                     output_content = read_output_content(full_path, Constants.croot)
                     reference_dump_path = os.path.join(library_lsdump_path , os.path.basename(full_path).replace(".so",""))
                     with open(reference_dump_path, 'wb') as f:
                         f.write(bytes(output_content.encode('utf-8')))
                elif(libraryInfo["arch_type"] == Constants.arch32 and Constants.arm_armv7_a_neon in path):
                     full_path = os.path.join(Constants.croot, path)
                     output_content = read_output_content(full_path, Constants.croot)
                     reference_dump_path = os.path.join(library_lsdump_path, os.path.basename(full_path).replace(".so",""))
                     with open(reference_dump_path, 'wb') as f:
                         f.write(bytes(output_content.encode('utf-8')))

def is_soong_library(library_dict):
    if not library_dict["abi_checker"]:
        return True
    return False

def identify_installed_path_for_mklibs(libraryList):
    installed_library_list =[]
    for library in libraryList:
        try:
            library_dict = module_info_dict[library]
            installed_path = os.path.join(Constants.croot,library_dict["installed"][0])
            installed_path = installed_path.split(Constants.out_path)[1]
            installed_library_list.append(installed_path)
        except KeyError:
            print("Values not found")
    return installed_library_list

def generateAbiDumpForLibraryList(libraryList, path):
    abi_dump_flag = True
    target_64 = False
    libraryList = identify_installed_path_for_mklibs(libraryList)
    pre_verify_check = run_pre_verify_check_for_lsdump(libraryList, path)
    if not pre_verify_check:
        abi_dump_flag = False
        print("Pre-Verification failed for Lsdump")
        Logger.logInternal.error("Pre-Verification failed for Lsdump")
        return abi_dump_flag

    ## Check if target is 32 or 64 bit
    if os.path.exists(os.path.join(Constants.out_path,"vendor/lib64")):
         target_64 = True
    for library in libraryList:
        arch_map = checkIf32and64ArchVersionExists(library)
        if not arch_map[Constants.arch64] and not arch_map[Constants.arch32]:
            Logger.logInternal.info("Skipping Lsdump Generation. Library not compiled : " + library)
            print("Skipping Lsdump Generation. Library not compiled : " + library)
            continue
        # Format :: /vendor/lib64/libllvm-qgl.so
        library_name  = str(os.path.splitext(os.path.basename(library))[0].rstrip())

        ##Handling for 64 Bit Library
        if arch_map[Constants.arch64]:
            library_dict = get_dict_from_module_info(library_name)
            if check_for_Export_Headers(library_dict,library_name):
                library_info = create_metadata_for_lsdump(library_dict,Constants.arch64,library,library_name)
                library_abidump_path = os.path.join(path,Constants.arch64)
                UtilFunctions.create_dir(library_abidump_path)
                generateAbiDumpForLibrary(library_info,library_abidump_path)
            else:
                Logger.logInternal.info("64 bit variant is not compiled for : " + library_name)

        ##Handling for 32 Bit Library
        if arch_map[Constants.arch32]:
            library_dict = {}
            if not target_64:
                library_dict = get_dict_from_module_info(library_name)
            else:
                library_dict = get_dict_from_module_info(library_name + "_32")
            if check_for_Export_Headers(library_dict,library_name):
                library_info = create_metadata_for_lsdump(library_dict,Constants.arch32,library,library_name)
                library_abidump_path = os.path.join(path,Constants.arch32)
                UtilFunctions.create_dir(library_abidump_path)
                generateAbiDumpForLibrary(library_info,library_abidump_path)
            else:
                Logger.logInternal.info("32 bit variant is not compiled for : " + library_name)
    return abi_dump_flag

def getStaticLibraryInformation(module_info_dict, static_lib_name):
    ## Find out static lib details from module_info
    static_lib_dict     = module_info_dict[static_lib_name]
    static_lib_srcs     = static_lib_dict["srcs"]
    static_lib_incs     = static_lib_dict["incs"]
    static_lib_cflags   = static_lib_dict["cflags"]
    static_lib_info     =  { "dict"   : static_lib_dict,
                             "name"   : static_lib_name,
                             "srcs"   : static_lib_srcs,
                             "incs"   : createStringForIncludes(static_lib_incs),
                             "cflags" : createStringForList(static_lib_cflags)}
    return static_lib_info

def handleStaticDependencies(library_dict, module_info_dict,serialzed_static_dep_list):
  static_lib_info = {}
  whole_static_lib_info = {}
  for static_lib_name in library_dict["static"]:
    ## Find out static lib details from module_info
    static_lib_info = getStaticLibraryInformation(module_info_dict, static_lib_name)
    serialzed_static_dep_list.append(static_lib_info)

  for static_lib_name in library_dict["wstatic"]:
    whole_static_lib_info = getStaticLibraryInformation(module_info_dict, static_lib_name)
    serialzed_static_dep_list.append(whole_static_lib_info)

  ## Check for recursive static dependency if found then
  ## enter into recrusion.
  if static_lib_info:
    static_lib_dict = static_lib_info["dict"]
    if len(static_lib_dict["static"]) > 0 or len(static_lib_dict["wstatic"]) > 0 :
      handleStaticDependencies(static_lib_dict,module_info_dict,serialzed_static_dep_list)
  if whole_static_lib_info:
    whole_static_lib_dict = whole_static_lib_info["dict"]
    if len(whole_static_lib_dict["static"]) > 0 or len(whole_static_lib_dict["wstatic"]) > 0 :
      handleStaticDependencies(whole_static_lib_dict,module_info_dict,serialzed_static_dep_list)
  return serialzed_static_dep_list

def checkCompatiblityInABIdiffFiles(abidiff_folder):
  compatiblity_flag = True
  print("---------------------------------------")
  if UtilFunctions.dirExists(abidiff_folder):
    for abidiff_file in os.listdir(abidiff_folder):
        ## Check if file is directoiry , if Yes then enter into recursion.
        file_path = os.path.join(abidiff_folder,abidiff_file)
        if os.path.isdir(file_path):
          compatiblity_flag = checkCompatiblityInABIdiffFiles(file_path)
        else:
          if abidiff_file.endswith('.abidiff'):
            file_handle = open(file_path,"r")
            ## We are intrested in reading last line of abidiff file to check status
            try:
              last_line = subprocess.check_output(['tail', '-1', file_path])
              if re.search(r'\b' + "COMPATIBLE" + r'\b',last_line.decode()):
                print("QIIFA : Library " + abidiff_file  + " is COMPATIBLE")
              else:
                print("QIIFA : Library " + abidiff_file  +
                      " is not COMPATIBLE. Check " + file_path + " for more Information.")
                compatiblity_flag = False
            except subprocess.CalledProcessError as e:
              Logger.logInternal.warning(e.output)
          else:
            continue
  print("---------------------------------------")
  return compatiblity_flag

def generate_abi_dump_for_module(metadata,library_list,module_path):
    UtilFunctions.create_dir(module_path)
    return generateAbiDumpForLibraryList(library_list,module_path)

def check_for_lsdump_compatibility(golden_path,current_path,output_abi_diff_path):
    golden_path_32_bit   = os.path.join(golden_path,Constants.arch32)
    golden_path_64_bit   = os.path.join(golden_path,Constants.arch64)
    current_path_32_bit  = os.path.join(current_path,Constants.arch32)
    current_path_64_bit  = os.path.join(current_path,Constants.arch64)
    output_abi_diff_path_32 = os.path.join(output_abi_diff_path,Constants.arch32)
    output_abi_diff_path_64 = os.path.join(output_abi_diff_path,Constants.arch64)
    generateABIdiffFilesForArch(current_path_32_bit,golden_path_32_bit,Constants.arch32,output_abi_diff_path_32)
    generateABIdiffFilesForArch(current_path_64_bit,golden_path_64_bit,Constants.arch64,output_abi_diff_path_64)
    return checkCompatiblityInABIdiffFiles(output_abi_diff_path)

def generate_abi_dump_for_techpackage(config,golden_ref=False):
    techpack_info_file = os.path.join(os.path.dirname(Constants.qiifa_build_xml_path),"techpack_info.txt")
    enabled_techpack_list=[]
    if os.path.exists(techpack_info_file):
        file_handle = open(techpack_info_file, "r")
        techpack_info = file_handle.readline()
        enabled_techpack_list = techpack_info.split(",")
    else:
        print("techpack_info.txt file missing. Exiting!!")
        sys.exit(-1)
    tech_package_list = load_tech_package_list(config)
    for tech_package in tech_package_list:
      tech_json = read_qiifa_techpackage(tech_package)
      tech_json = read_techpackdata_from_CD_XML(tech_json)
      lsdump_dir_path = None
      tech_pack_name  = tech_json["techpackage_name"]
      if tech_pack_name in enabled_techpack_list:
          if golden_ref:
              lsdump_dir_path = os.path.join(Constants.croot,tech_json["lsdump_golden_path"],Constants.abidumps_string)
          else:
              lsdump_dir_path = Constants.qiifa_current_abi_dump_folder
          createDirectory(lsdump_dir_path)
          lsdump_dir_path = os.path.join(lsdump_dir_path,Constants.techpackage_string)
          createDirectory(lsdump_dir_path)
          techpackage_library_list = tech_json["library_list"]
          techpackage_library_list_split = split_modules_as_per_build_type(techpackage_library_list)
          lsdump_dir_path = os.path.join(lsdump_dir_path,tech_pack_name)
          createDirectory(lsdump_dir_path)
          find_and_copy_lsdump_files_for_bp_libs(techpackage_library_list_split["bplibs_dict"],lsdump_dir_path)
          generate_abi_dump_for_module(tech_pack_name,techpackage_library_list_split["mklibs_dict"],lsdump_dir_path)
          verify_lsdump_files(lsdump_dir_path,techpackage_library_list)
          if not golden_ref:
              techpack_golden_path = os.path.join(Constants.croot,tech_json["lsdump_golden_path"],
                                     Constants.abidumps_string,Constants.techpackage_string,tech_pack_name)
              output_abi_diff_path = os.path.join(Constants.croot,Constants.qiifa_current_cmd_dir,
                                     Constants.abidiff_string,Constants.techpackage_string,tech_pack_name)
              compatiblity_flag = check_for_lsdump_compatibility(techpack_golden_path,lsdump_dir_path,tech_pack_name)
              if not compatiblity_flag:
                  print("Techpackage : " + tech_pack_name + " not Compatible!! Exiting build!!")
                  sys.exit(-1)

def func_generateGoldenAbiDumpsforTechPackages(self):
    abi_config_dict = read_abi_config()
    if abi_config_dict:
        initialize()
        for config in abi_config_dict:
            if abi_config_checker(config,"techpackage"):
                generate_abi_dump_for_techpackage(config,True)

def arg_in_path(new_arg, path):
    for arg in new_arg:
      if(arg in path):
        return True
    return False

def func_generateGoldenAbiDumpsforVndk(self, arg_lst):
    new_arg_lst = arg_lst.split(',')
    libs_processed = 0
    total_libs  = 0
    lsdump_paths_file_path = Constants.lsdump_paths_file_path
    ref_dump_path = Constants.vndk_abi_golden_dumps
    vndk_version = UtilFunctions.get_vndk_version()
    if vndk_version == None:
      print("Failed to get vndk_version info, platform version is "+str(UtilFunctions.get_platform_version()))
      sys.exit()
    with open(lsdump_paths_file_path, 'r') as lsdump_paths_file:
      unprocessed_libs_list = ""
      for line in lsdump_paths_file:
        tag, path = (x.strip() for x in line.split(':', 1))
        if(tag == Constants.vndk_core or tag == Constants.vndk_sp) and "android_vendor" in path and arg_in_path(new_arg_lst, path):
              total_libs += 1
              full_path = os.path.join(Constants.croot, path)
              if not (os.path.exists(full_path)):
                unprocessed_libs_list += (full_path + "\n")
                continue
              libs_processed += 1
              reference_dump_path  = os.path.join(ref_dump_path, get_tag_string(tag), vndk_version, Constants.binder_bitness, get_arch(path), "source-based", os.path.basename(full_path))
              if not os.path.exists((os.path.dirname(reference_dump_path))):
                os.makedirs(os.path.dirname(reference_dump_path))
              output_content = read_output_content(full_path, Constants.croot)
              with open(reference_dump_path, 'wb+') as f:
                f.write(bytes(output_content.encode('utf-8')))
      if(unprocessed_libs_list):
        reason = "Following lsdump paths don't exists \n: " + unprocessed_libs_list
        UtilFunctions.print_warning_on_stdout(reason)
    info = "\nTotal libraries : " + str(total_libs) + "\nProcessed libraries(lsdump generated) : " + str(libs_processed) + "\nUnprocessed libraries(lsdump not generated) : " + str(total_libs - libs_processed)
    UtilFunctions.print_info_on_stdout(LOG_TAG, info)

def split_modules_as_per_build_type(module_list):
    mklibs_list= {}
    bplibs_list= {}
    if not module_info_dict:
        initialize()
    for lib in module_list:
        try:
            lib_dict = module_info_dict[str(lib)]
            if is_soong_library(lib_dict):
                bplibs_list[str(lib)] = lib_dict
            else:
                mklibs_list[str(lib)] = lib_dict
        except KeyError:
            print("Library Not found in module_info.json : " + lib + ".Exiting!!")
            sys.exit(1)
    return {"mklibs_dict":mklibs_list , "bplibs_dict":bplibs_list}

def find_and_copy_lsdump_files_for_bp_libs(bplibs_dict,destination_path):
    arch64_path = os.path.join(destination_path,Constants.arch64)
    arch32_path = os.path.join(destination_path,Constants.arch32)
    createDirectory(arch64_path)
    createDirectory(arch32_path)
    for bplib in bplibs_dict:
        try:
            ## Handling for 64 bit
            lsdump_path_64 = None
            if (bplib + "_64") in lsdump_path_info_dict:
                lsdump_path_64 = lsdump_path_info_dict[bplib + "_64"]
            if lsdump_path_64 and os.path.exists(lsdump_path_64):
                lsdump_content = read_output_content(lsdump_path_64, Constants.croot)
                reference_dump_path = os.path.join(arch64_path,str(bplib))
                createDirectory(reference_dump_path)
                reference_dump_path = os.path.join(reference_dump_path,str(bplib) + "-lsdump")
                createDirectory(reference_dump_path)
                reference_dump_path = os.path.join(reference_dump_path,str(bplib) + ".lsdump")
                with open(reference_dump_path, 'wb') as f:
                    f.write(bytes(lsdump_content.encode('utf-8')))
            else:
                print("Lsdump Information not found for .bp Library: " + str(bplib)+ " Exiting Script Now.")
                sys.exit(-1)

            lsdump_path_32 = None
            if (bplib + "_32") in lsdump_path_info_dict:
                lsdump_path_32 = lsdump_path_info_dict[bplib + "_32"]
            if lsdump_path_32 and os.path.exists(lsdump_path_32):
                lsdump_content = read_output_content(lsdump_path_32, Constants.croot)
                reference_dump_path = os.path.join(arch32_path,str(bplib))
                createDirectory(reference_dump_path)
                reference_dump_path = os.path.join(reference_dump_path,str(bplib) + "-lsdump")
                createDirectory(reference_dump_path)
                reference_dump_path = os.path.join(reference_dump_path,str(bplib) + ".lsdump")
                lsdump_content = read_output_content(lsdump_path_32, Constants.croot)
                with open(reference_dump_path, 'wb') as f:
                    f.write(bytes(lsdump_content.encode('utf-8')))
            else:
                print("Warning : Lsdump Information not found for .bp Library (32 bit variant ): " + str(bplib))
        except KeyError:
            print("Copy Lsdump Information Failed for Library : " + bplib)
            sys.exit(-1)

def verify_lsdump_files(lsdump_dir_path,library_list):
    ## We need to verify if lsdump files are generated
    ## for provided library list in given path,
    ## if lsdump is not available
    ## Let's throw error and break build.
    file_extension = ".lsdump"
    lsdump_file_list = []
    for path, dirc, files in os.walk(lsdump_dir_path):
        for name in files:
            if name.endswith(file_extension):
                lsdump_file_list.append(name)

    for library in library_list:
        lsdump_value = library + file_extension
        if lsdump_value in lsdump_file_list:
            continue
        else:
            print("LSdump not found for Library : " + library)
            sys.exit(-1)

def generate_abidumps_for_sphal(abi_config_dict,golden=False):
    ## Check if sphal plugin configuration is enabled.
    ## If Yes then get sphal library list.
    for config in abi_config_dict:
        if config["abi_type"] == "sphal" and config["enabled"] == "true":
            ## Complete list of libraries (Android.bp and Android.mk)
            sp_hal_library_list = config["sp_hal_library_list"]
            if not sp_hal_library_list:
                return
            ## Check if abi-dumps folder exist, if not create directory
            ## structure.
            abidump_metadata = Constants.abidump_metadata
            sphal_lsdump_dir_path=""
            if golden:
                sphal_lsdump_dir_path = Constants.sp_hal_golden_abi_dump_path_noship
            else:
                sphal_lsdump_dir_path =  Constants.sp_hal_current_abi_dump_path
                ## Let;s check if golden lsdump exists ,if not then give
                ## warning and proceed ahead. Do not break build if top
                ## level sp-hal golden directory is missing.
                if not os.path.exists(Constants.sp_hal_golden_abi_dump_path_noship):
                    print("Warning : Golden Reference dumps not found for sp-hal!!")
                    return
            createDirectory(sphal_lsdump_dir_path)
            ## Divide library list into two parts
            ## 1.Libraries compiled through Android.bp mechanism(Lsdump is gen by Soong)
            ## 2.Libraries compiled through Android.mk nechanism
            sphal_library_dict = split_modules_as_per_build_type(sp_hal_library_list)
            find_and_copy_lsdump_files_for_bp_libs(sphal_library_dict["bplibs_dict"],sphal_lsdump_dir_path)
            generate_abi_dump_for_module(Constants.sp_hal_string,sphal_library_dict["mklibs_dict"],sphal_lsdump_dir_path)
            verify_lsdump_files(sphal_lsdump_dir_path,sp_hal_library_list)
            if not golden:
                sphal_golden_path = Constants.sp_hal_golden_abi_dump_path_noship
                output_abi_diff_path = os.path.join(Constants.croot,Constants.qiifa_current_cmd_dir,
                           Constants.abidiff_string,Constants.sp_hal_string)
                compatiblity_flag = check_for_lsdump_compatibility(sphal_golden_path,sphal_lsdump_dir_path,output_abi_diff_path)
                if not compatiblity_flag:
                    print("Sphal not Compatible!! Exiting build!!")
                    sys.exit(-1)
            break

def func_generate_golden_abidumps_for_sphal(self):
    abi_config_dict = read_abi_config()
    golden_dumps = True
    generate_abidumps_for_sphal(abi_config_dict,golden_dumps)


def abi_config_checker(config,type):
  if config["abi_type"] == type and config["enabled"] == "true":
    return True
  else:
    return False

def get_techpackage_list():
    tree = ET.parse(Constants.qiifa_build_xml_path)
    root = tree.getroot()
    tech_package_list = []
    for child in root:
        enable = False
        module_name = ""
        for subchild in child:
            if(subchild.tag=="techpackagename"):
                enable = True
                module_name = subchild.text
            if(enable and subchild.tag == "enable" and subchild.text == "enabled"):
                tech_package_list.append(module_name)
    return tech_package_list

def load_tech_package_list(config):
  return config["techpacakages_json_file_list"]

def difference_in_sp_hal_files():
    fp_golden  = open(Constants.sp_hal_gen_golden_path,"r")
    fp_current = open(Constants.sp_hal_gen_current_path,"r")
    diff_library_list = []
    for line1 in fp_current:
      flag = False
      for line2 in fp_golden:
        if (line1 == line2):
          flag = True
          break
      fp_golden.seek(0,0)
      if flag == False:
        diff_library_list.append(line1)
    fp_golden.close()
    fp_current.close()
    return diff_library_list

def check_for_violations():
    sp_hal_diff_list = difference_in_sp_hal_files()
    if(sp_hal_diff_list):
      library_list = ""
      for ele in sp_hal_diff_list:
        library_list += ele
      reason = "Extra libraries which causing the issue are: \n" + library_list
      UtilFunctions.print_violations_on_stdout(LOG_TAG, Constants.sp_hal_string, check_for_violations.__name__, reason)
    else:
      Logger.logInternal.info("sphal check ran and no violations found")

def abi_checker_for_sp_hal():
    golden_sp_hal_generated_file_flag = True
    if not UtilFunctions.pathExists(Constants.sp_hal_gen_golden_path):
      Logger.logInternal.error("Skipping, Golden sp_hal_generated_file not found at %s", Constants.sp_hal_gen_golden_path)
      golden_sp_hal_generated_file_flag = False
    if(golden_sp_hal_generated_file_flag):
      abidump_metadata = Constants.abidump_metadata
      sp_hal_library_list = prepare_data_and_directory_structure(abidump_metadata,abidump_metadata["current_path"],Constants.sp_hal_string)
      check_for_violations()

def abi_checker_for_commonsys_intf():
    abidump_metadata = Constants.abidump_metadata
    commonsys_intf_library_list = prepare_data_and_directory_structure(abidump_metadata,abidump_metadata["current_path"],Constants.commonsys_intf_string)
    golden_abi_dump_path_flag = True
    if not UtilFunctions.pathExists(Constants.commonsys_intf_golden_abi_dump_path):
      Logger.logInternal.error("Skipping, Golden Abidump path  not found at %s", Constants.commonsys_intf_golden_abi_dump_path)
      golden_abi_dump_path_flag = False
    if(golden_abi_dump_path_flag):
      if not module_info_dict:
        initialize()
      commonsys_intf_metadata = abidump_metadata["modules"][Constants.commonsys_intf_string]
      flag = generate_abi_dump_for_module(commonsys_intf_metadata,commonsys_intf_library_list,commonsys_intf_metadata["current_path"])
      return flag
    return False

def remove_current_vndk_dumps():
    if(os.path.exists(Constants.vndk_abi_current_dumps)):
      UtilFunctions.rmdir(Constants.vndk_abi_current_dumps)
    if(os.path.exists(Constants.vndk_abi_diff_path)):
      UtilFunctions.rmdir(Constants.vndk_abi_diff_path)

def abi_checker_for_vndk_libs():
    # Let;s figure out on which platform version qiifa code is running.
    platform_version_suffix = 0
    platform_version_suffix = UtilFunctions.get_platform_version()
    if platform_version_suffix == None:
      print("Failed to get platform version info ")
      return
    vndk_abidump_version = "vndk-" + str(platform_version_suffix)
    vndk_golden_abi_dump_path = os.path.join(Constants.qiifa_golden_directory_path_noship,Constants.abidumps_string,Constants.vndk_version_string,vndk_abidump_version,Constants.vndk_abi_string)
    if not UtilFunctions.pathExists(vndk_golden_abi_dump_path):
      Logger.logInternal.error("Skipping, Golden Abidump path  not found at %s", vndk_golden_abi_dump_path)
      return
    if not UtilFunctions.pathExists(Constants.lsdump_paths_file_path):
      Logger.logInternal.error("Skipping, lsdump_paths.txt not found at %s", Constants.lsdump_paths_file_path)
      return
    ref_dump_path = Constants.vndk_abi_current_dumps
    copy_vndk_lsdump(Constants.lsdump_paths_file_path, ref_dump_path)
    UtilFunctions.create_dir(Constants.current_abidiff_path)
    UtilFunctions.create_dir(Constants.vndk_abi_diff_path)
    generate_abi_diff_for_vndk_libs(vndk_golden_abi_dump_path,Constants.vndk_abi_current_dumps,Constants.vndk_abi_diff_path)
    generate_report_for_vndk_libs(vndk_golden_abi_dump_path,Constants.vndk_abi_diff_path)
    remove_current_vndk_dumps()

def module_present_in_build_xml(module):
    if(os.path.exists(Constants.qiifa_build_xml_path)):
        build_config_packages = get_techpackage_list()
        for tech_package in build_config_packages:
            if(module == tech_package):
                return True
        return False
    else:
        Logger.logInternal.info("build_config.xml does not exists at %s", Constants.qiifa_build_xml_path)
    return False

def get_tag_string(tag):
    if tag.startswith('VNDK'):
        return 'vndk'
    raise ValueError(tag + 'is not a known tag.')

def read_output_content(output_path, replace_str):
    with open(output_path, 'r') as f:
        return f.read().replace(replace_str, '')

def get_arch(path):
    if Constants.arm64_armv8_a in path:
      return Constants.arm64_armv8_a
    elif Constants.arm_armv7_a_neon in path:
      return Constants.arm_armv7_a_neon
    elif Constants.arm_armv8_2a in path:
      return Constants.arm_armv8_2a
    else:
      print("New lib architecture type detected "+str(path))
      sys.exit(1)

def copy_vndk_lsdump(lsdump_paths_file_path, ref_dump_path):
    libs_processed = 0
    total_libs  = 0
    vndk_version = UtilFunctions.get_vndk_version()
    if vndk_version == None:
      print("Failed to get vndk_version info, platform version is "+str(UtilFunctions.get_platform_version()))
      sys.exit(1)
    with open(lsdump_paths_file_path, 'r') as lsdump_paths_file:
      unprocessed_libs_list = ""
      for line in lsdump_paths_file:
        tag, path = (x.strip() for x in line.split(':', 1))
        if(tag == Constants.vndk_core or tag == Constants.vndk_sp) and "android_vendor" in path:
          total_libs += 1
          full_path = os.path.join(Constants.croot, path)
          if not (os.path.exists(full_path)):
            unprocessed_libs_list += (full_path + "\n")
            continue
          libs_processed += 1
          reference_dump_path  = os.path.join(ref_dump_path, get_tag_string(tag), vndk_version, Constants.binder_bitness, get_arch(path), "source-based", os.path.basename(full_path))
          if not os.path.exists((os.path.dirname(reference_dump_path))):
            os.makedirs(os.path.dirname(reference_dump_path))
          output_content = read_output_content(full_path, Constants.croot)
          with open(reference_dump_path, 'wb') as f:
            f.write(bytes(output_content.encode('utf-8')))
      if(unprocessed_libs_list):
        reason = "Following lsdump paths don't exists \n: " + unprocessed_libs_list
        UtilFunctions.print_warning_on_stdout(reason)
    info = "\nTotal libraries : " + str(total_libs) + "\nProcessed libraries(lsdump generated) : " + str(libs_processed) + "\nUnprocessed libraries(lsdump not generated) : " + str(total_libs - libs_processed)
    UtilFunctions.print_info_on_stdout(LOG_TAG, info)

def func_start_qiifa_abi_checker(self):
    abi_config_dict = read_abi_config()
    abidump_metadata = Constants.abidump_metadata
    if abi_config_dict:
      abi_dump_success_flag = False
      for config in abi_config_dict:
        if abi_config_checker(config,"sphal") and not ((Constants.qiifa_out_path_target_value).startswith("qssi")):
          abi_checker_for_sp_hal()
          generate_abidumps_for_sphal(abi_config_dict)
        if abi_config_checker(config,"commonsysintf"):
          flag = abi_checker_for_commonsys_intf()
          if(flag):
            abi_dump_success_flag = flag
        if abi_config_checker(config,"techpackage"):
          generate_abi_dump_for_techpackage(config)
        if abi_config_checker(config,"vndk") and Constants.qiifa_out_path_target_value == "qssi":
          abi_checker_for_vndk_libs()
      else :
        Logger.logInternal.info("ABI Dump Generation Skipped!!")
    else:
      Logger.logInternal.warning("Abichecker : Configuration is empty. Exiting!!")


def get_recursive_list_of_files(folder,file_list):
  if UtilFunctions.dirExists(folder):
    for file in os.listdir(folder):
      file_path = os.path.join(folder,file)
      if os.path.isdir(file_path):
        get_recursive_list_of_files(file_path,file_list)
      else:
        file_list.append(file_path)

def optimised_abi_diff_for_vndk_libs(golden_folder,chunk_of_paths, vndk_abidiff_arm64_path, vndk_abidiff_arm32_path):
  for golden_file_path in chunk_of_paths:
    relative_golden_file_path = golden_file_path.replace(golden_folder + "/","")
    current_file_path = os.path.join(Constants.vndk_abi_current_dumps,relative_golden_file_path)
    if UtilFunctions.pathExists(current_file_path):
      arch_type = Constants.arch32
      if Constants.arch64 in golden_file_path:
        arch_type = Constants.arch64
      output_abidiff_file_path = ""
      library_name = os.path.splitext(os.path.basename(relative_golden_file_path))[0] \
                     + "." + Constants.abidiff_string
      if Constants.arm64_armv8_a in current_file_path:
        output_abidiff_file_path = os.path.join(vndk_abidiff_arm64_path,library_name)
      elif Constants.arm_armv7_a_neon in current_file_path or Constants.arm_armv8_2a in current_file_path:
        output_abidiff_file_path = os.path.join(vndk_abidiff_arm32_path,library_name)
      header_abi_diff_command = [ Constants.header_abi_diff,
                                    "-old",
                                    golden_file_path,
                                    "-new",
                                    current_file_path,
                                    "-arch",
                                    arch_type,
                                    "-o",
                                    output_abidiff_file_path,
                                    "-lib",
                                    library_name]
      header_abi_diff_serialized_command = ""
      for instruction in header_abi_diff_command:
        header_abi_diff_serialized_command += instruction + " "
      try:
        subprocess.check_output(header_abi_diff_serialized_command, shell=True)
      except subprocess.CalledProcessError as e:
        Logger.logInternal.warning("QIIFA : Header abi diff generation FAILED")

def get_number_of_threads():
  x=multiprocessing.cpu_count()
  return x if x < 16 else 16

def get_chunk_of_paths_list(golden_file_list):
  num_of_threads = get_number_of_threads()
  num_of_task_for_each_thread = len(golden_file_list)/num_of_threads
  unassigned_tasks = len(golden_file_list)-(num_of_task_for_each_thread*num_of_threads)
  chunk_of_paths_list = []
  chunk_of_paths = []
  num_of_paths = 0
  modified_num_of_tasks_for_some_threads = num_of_task_for_each_thread
  for golden_file_path in golden_file_list:
    chunk_of_paths.append(golden_file_path)
    num_of_paths  += 1
    if(unassigned_tasks > 0 and num_of_paths == num_of_task_for_each_thread):
      modified_num_of_tasks_for_some_threads = num_of_task_for_each_thread + 1
      unassigned_tasks -= 1
    if(num_of_paths == modified_num_of_tasks_for_some_threads):
      num_of_paths = 0
      modified_num_of_tasks_for_some_threads  = num_of_task_for_each_thread
      chunk_of_paths_list.append(chunk_of_paths)
      chunk_of_paths = []
  return chunk_of_paths_list

def generate_abi_diff_for_vndk_libs(golden_folder,current_folder,output_folder):
  golden_file_list = []
  golden_library_removed_list = []
  vndk_abidiff_path = os.path.join(output_folder,"vndk")
  vndk_abidiff_arm64_path = os.path.join(vndk_abidiff_path, Constants.arm64_armv8_a)
  vndk_abidiff_arm32_path = os.path.join(vndk_abidiff_path, Constants.arm_armv7_a_neon)
  UtilFunctions.create_dir(vndk_abidiff_path)
  UtilFunctions.create_dir(vndk_abidiff_arm64_path)
  UtilFunctions.create_dir(vndk_abidiff_arm32_path)
  get_recursive_list_of_files(golden_folder,golden_file_list)
  chunk_of_paths_list = get_chunk_of_paths_list(golden_file_list)
  UtilFunctions.print_info_on_stdout(LOG_TAG, "Generating abidiff for vndk libraries...")
  num_of_threads = 1
  thread_names = {}
  for chunk_of_paths in chunk_of_paths_list:
    thread_names[num_of_threads]  = threading.Thread(target=optimised_abi_diff_for_vndk_libs, args=(golden_folder,chunk_of_paths,vndk_abidiff_arm64_path,vndk_abidiff_arm32_path,))
    num_of_threads += 1
  for x in range (1,num_of_threads):
    thread_names[x].start()
  for x in range (1,num_of_threads):
    thread_names[x].join()
  UtilFunctions.print_info_on_stdout(LOG_TAG, "abidiff for vndk libraries done...!")

def generate_report_for_vndk_libs(golden_path , abi_diff_path):
  compatible_libs = []
  incompatible_libs = []
  extension_libs = []
  unknown_status_libs = []
  abidiff_files =[]
  violation_reason = ""
  violation_flag = False
  get_recursive_list_of_files(abi_diff_path, abidiff_files)
  UtilFunctions.print_info_on_stdout(LOG_TAG, "Generating report for vndk libraries")
  for abidiff_file in abidiff_files:
    file_handle = open(abidiff_file,"r")
    ## We are intrested in reading last line of abidiff file to check status
    try:
      last_line = subprocess.check_output(['tail', '-1', abidiff_file])
      if re.search(r'\b' + "COMPATIBLE" + r'\b',last_line):
        compatible_libs.append(abidiff_file)
      elif re.search(r'\b' + "EXTENSION" + r'\b',last_line):
        extension_libs.append(abidiff_file)
      elif re.search(r'\b' + "INCOMPATIBLE" + r'\b',last_line):
        incompatible_libs.append(abidiff_file)
      else:
        unknown_status_libs.append(abidiff_file)
    except subprocess.CalledProcessError as e:
      Logger.logInternal.warning(e.output)
  vndk_report_file_handler = open(Constants.vndk_abi_compatiblity_report,"w+")
  if compatible_libs:
    vndk_report_file_handler.write("#####Compatible Libs#####\n")
    for compatible_lib in compatible_libs:
      vndk_report_file_handler.write(compatible_lib + " is Compatible\n")
  if incompatible_libs:
    vndk_report_file_handler.write("######InCompatible Libs######\n")
    violation_reason += "######InCompatible Libs######\n"
    violation_flag = True
    for incompatible_lib in incompatible_libs:
      vndk_report_file_handler.write(incompatible_lib + " is InCompatible\n")
      violation_reason += (incompatible_lib + " is InCompatible\n")
  if extension_libs:
    vndk_report_file_handler.write("#####Extension Libs#######\n")
    for extension_lib in extension_libs:
      vndk_report_file_handler.write(extension_lib + " is Extension\n")
  if unknown_status_libs:
    vndk_report_file_handler.write("######Unknown Status Libs#####\n")
    violation_reason += "######Unknown Status Libs#####\n"
    violation_flag = True
    for unknown_status_lib in unknown_status_libs:
      vndk_report_file_handler.write(unknown_status_lib + " status is unknown\n")
      violation_reason += (unknown_status_lib + " status is unknown\n")
  golden_abi_library_list = []
  curent_abi_library_list = []
  get_recursive_list_of_files(golden_path,golden_abi_library_list)
  get_recursive_list_of_files(Constants.vndk_abi_current_dumps,curent_abi_library_list)
  golden_abi_library_list_relative = [lib.replace(golden_path, "") for lib in golden_abi_library_list]
  curent_abi_library_list_relative = [lib.replace(Constants.vndk_abi_current_dumps, "") for lib in curent_abi_library_list]
  golden_set = set(golden_abi_library_list_relative)
  current_set = set(curent_abi_library_list_relative)
  golden_extra_set = golden_set.difference(current_set)
  current_extra_set = current_set.difference(golden_set)
  if golden_extra_set:
    vndk_report_file_handler.write("######Libraries Removed######\n")
    violation_reason += ("######Libraries Removed######\n")
    violation_flag = True
    for lib in golden_extra_set:
      vndk_report_file_handler.write(lib + " has been removed.\n")
      violation_reason += (lib + " has been removed.\n")
  if current_extra_set:
    vndk_report_file_handler.write("######Libraries Added#######\n")
    violation_reason += ("######Libraries Added######\n")
    violation_flag = True
    for lib in current_extra_set:
      ##Ignore Platform Libraries folder
      if not "/platform/" in lib:
        vndk_report_file_handler.write(lib + " has been added.\n")
        violation_reason += (lib + " has been added.\n")
  vndk_report_file_handler.close()
  UtilFunctions.print_info_on_stdout(LOG_TAG, "report done for vndk libs")
  if violation_flag:
    ##TODO:Temporarily log vndk violations in non-enforcment mode & do not break build
    enforced = False
    UtilFunctions.print_violations_on_stdout(LOG_TAG, Constants.vndk_abi_string, generate_report_for_vndk_libs.__name__, violation_reason,enforced)

def prepare_data_and_directory_structure(abidump_object,abidump_path,abi_type):
    abidump_metadata = Constants.abidump_metadata
    UtilFunctions.create_dir(abidump_path)
    ##Generate SP-HAL List
    if(abi_type == Constants.sp_hal_string):
      return get_sphal_list(abidump_path)
    ## Get List fo Commonsys-Intf Libraries
    elif(abi_type == Constants.commonsys_intf_string):
      return get_commonsys_intf_library_list()

def stripSourceFilePrefixInLsdump(lsdump_dir):
    if(UtilFunctions.dirExists(lsdump_dir)):
      for file in os.listdir(lsdump_dir):
        ## Check if file is directory , if Yes then enter into recursion.
        file_path = lsdump_dir + "/" + file
        if os.path.isdir(file_path):
          stripSourceFilePrefixInLsdump(file_path)
        else :
          if file.endswith('.lsdump'):
            read_file_handle = open(file_path, "r")
            file_content = read_file_handle.read()
            file_content = file_content.replace(Constants.croot,"")
            read_file_handle.close()
            write_file_handle = open(file_path, "w")
            write_file_handle.write(file_content)
            write_file_handle.close()
    else:
      Logger.logInternal.error("Abidump Directory Not Found for Stripping Source File Information.")

def get_sphal_list(path):
    ## Check if sp_hal file is already generated, if present then return
    libraryList = []
    UtilFunctions.create_dir(path)
    path = os.path.join(path, Constants.sp_hal_string)
    UtilFunctions.create_dir(path)
    sp_hal_gen_path = os.path.join(path, Constants.sp_hal_gen_file)
    if(UtilFunctions.pathExists(sp_hal_gen_path)):
      sp_hal_file_handler = open(sp_hal_gen_path ,"r")
      libraryList = []
      for lib_path in sp_hal_file_handler:
        libraryList.append(lib_path.rstrip())
      return libraryList
    else:
      file_handle = open(Constants.selinux_file_path, "r")
      sp_hal_gen_file = open(sp_hal_gen_path,"w+")
      for entry in file_handle:
        selinux_file_context = entry.split(" ")[-1].rstrip()
        sp_hal_library = entry.split(" ")[0].rstrip()
        if selinux_file_context == 'u:object_r:same_process_hal_file:s0':
          sp_hal_library_path_itr = filter(lambda i: i not in Constants.selinux_context_sp_chars, sp_hal_library)
          sp_hal_library_path = ''.join(sp_hal_library_path_itr)
          library_path = sp_hal_library_path.replace('/vendorsystem','')
          #TODO Temporary Ignore vm-system files from check list
          if not "vm-system" in library_path:
            sp_hal_gen_file.write(library_path + "\n")
            libraryList.append(library_path)
      sp_hal_gen_file.close()
      return libraryList

def get_commonsys_intf_library_list():
    library_list = []
    if(UtilFunctions.pathExists(Constants.commonsys_intf_lib_file_path)):
      file_handler = open(Constants.commonsys_intf_lib_file_path ,"r")
      for lib_path in file_handler:
        library_list.append(lib_path.rstrip())
      return library_list
    else:
      tmp_lib_list =[]
      commonsys_intf_file = open(Constants.commonsys_intf_lib_file_path, "w+")
      for module in module_info_dict:
        install_paths = module_info_dict[module]['installed']
        project_paths = module_info_dict[module]['path']
        classes = module_info_dict[module]['class']
        for project_path in project_paths:
          commonsys_intf_path = Constants.commonsys_intf_path.replace(Constants.croot + "/",'')
          if project_path.startswith(commonsys_intf_path):
            for class_type in classes:
              if str(class_type) == Constants.shared_libs:
                for install_path in install_paths:
                  so_file_name = str(install_path).split("/")[-1]
                  # Ignore host libraries and recovery libraries.
                  # Also, ensure the lib exists before returning the list since not every
                  # entry in module-info.json really gets built ultimately.
                  # Ignore HIDL Libraries, they are guaranteed to be backward compatible
                  # through HIDL mechanism.
                  if not str(install_path).startswith(Constants.HOST_PATH) \
                        and Constants.recovery_path not in str(install_path) \
                        and str(install_path).endswith(".so") \
                        and os.path.exists(os.path.join(Constants.croot + "/" + str(install_path))) \
                        and not ((so_file_name.startswith("vendor.qti") or so_file_name.startswith("com.")) and "@" in so_file_name) \
                        and not so_file_name in tmp_lib_list:
                    library_path = install_path.replace(Constants.out_path,'')
                    tmp_lib_list.append(so_file_name)
                    library_list.append(library_path)
                    commonsys_intf_file.write(library_path + "\n")
                  else:
                    pass
      commonsys_intf_file.close()
    return library_list

def read_abi_config():
    abi_config_dict = {}
    if UtilFunctions.pathExists(Constants.abi_config_file):
      abi_config_file_handler = open(Constants.abi_config_file,'r')
      abi_config_dict = json.load(abi_config_file_handler)
    else:
      Logger.logInternal.warning("Abichecker : Abichecker config file not present.")
    return abi_config_dict

def read_qiifa_techpackage(path):
    qiifa_techpackage_dict = {}
    tech_package_json_file_path=os.path.join(Constants.croot,path)
    if UtilFunctions.pathExists(tech_package_json_file_path):
      tech_package_json_file_handler = open(tech_package_json_file_path,'r')
      qiifa_techpackage_dict = json.load(tech_package_json_file_handler)
    else:
      Logger.logInternal.warning("Abichecker : Abichecker tech_packagejson file not present.")
    return qiifa_techpackage_dict

def load_lsdump_path_information():
    lsdump_path_dict = {}
    if not os.path.exists(Constants.lsdump_paths_file_path) and not os.path.getsize(Constants.lsdump_paths_file_path) > 0 :
       Logger.logInternal.info("lsdump_paths.txt is not present/empty at %s ",Constants.lsdump_paths_file_path)
       return lsdump_path_dict
    with open(Constants.lsdump_paths_file_path, 'r') as lsdump_paths_file:
        for line in lsdump_paths_file:
            if not ":" in line:
                continue
            tag, path = (x.strip() for x in line.split(':', 1))
            ## Add only PLATFORM LIBRARIES and only vendor variant
            if tag == "PLATFORM" and "android_vendor" in path:
                ## Find out library name form provide path
                library_name = (path.split("/")[-1]).replace(".so.lsdump","")
                if "arm64_armv8" in path:
                    lsdump_path_dict[library_name + "_64"] = os.path.join(Constants.croot,path)
                elif "arm_" in path:
                    lsdump_path_dict[library_name + "_32"] = os.path.join(Constants.croot,path)
    return lsdump_path_dict

def initialize():
    global module_info_dict
    global lsdump_path_info_dict
    module_info_dict = loadModuleInfoDictFromJSONFile()
    lsdump_path_info_dict = load_lsdump_path_information()

class qiifa_abi:
    def __init__(self):
      pass

'''
plugin class implementation
plugin class is derived from plugin_interface class
Name of plugin class MUST be MyPlugin
'''
class MyPlugin(plugin_interface):
    def __init__(self):
        pass

    def register(self):
        return Constants.ABI_SUPPORTED_CREATE_ARGS

    def config(self, QIIFA_type=None, libsPath=None, CMDPath=None):
        pass

    def generateGoldenCMD(self, libsPath=None, storagePath=None, create_args_lst=None):
        if(create_args_lst[0] == "abi_vndk" and len(create_args_lst) == 2):
          func_generateGoldenAbiDumpsforVndk(self,create_args_lst[1])
        elif(create_args_lst[0]=="techpackage"):
          func_generateGoldenAbiDumpsforTechPackages(self)
        elif(create_args_lst[0]=="sphal"):
          func_generate_golden_abidumps_for_sphal(self)
        else:
          func_generateGoldenAbiDumpsforTechPackages(self)
          func_generate_golden_abidumps_for_sphal(self)

    def IIC(self, **kwargs):
        func_start_qiifa_abi_checker(self)
