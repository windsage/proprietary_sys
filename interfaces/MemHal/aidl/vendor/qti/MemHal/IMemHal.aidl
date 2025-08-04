/*
* Copyright (c) 2023 Qualcomm Technologies, Inc.
* All Rights Reserved.
* Confidential and Proprietary - Qualcomm Technologies, Inc.
*/

package vendor.qti.MemHal;
@VintfStability
interface IMemHal {

/* This API will be used to retrieve the value of properties defined in configstore.
*  It will take three arguments :
*     propname   : String, name of the property whose value is required by the user.
*     defaultVal : String, defaultVal to return .
*     req_details: int vector, contains pid and tid of calling process. Can be expanded on need.
*
*  Return type :  String. Returns the value of the requested property.
*/

  String MemHal_GetProp(in String propname, in String defaultVal, in int[] req_details);

/* This API will be used to set the value of the property requested by user to a custom value.
*  It will take three arguments:
*     propname: String , name of property whose value need to be set to a custom value.
*     NewVal : New Value for the property.
*     req_details: int vector, contains pid and tid of calling process. Can be expanded on need.
*
*  Return type: int , Return request handle on success else failure.
*/
  int MemHal_SetProp(in String propname, in String NewVal, in int[] req_details);

/* This API will work on request type. Different request types can be provided to perform various memory related tasks.
*  Currently we are supporting three requests:
*     VENDOR_SET_MEMORY_NODE_Request :Will be used to set value of different memory related node. We need to provide opcode for node and the value we need to set.
*     VENDOR_FREE_MEMORY_REQUEST :Will be used to free the requested type of memory. We need to provide opcode for memory type and amount of memory to be freed.
*     VENDOR_BOOST_MODE_REQUEST :Will be used to set device in boost mode. We will limit the concurrency to maintain more free memory.
*
*  It will take three arguments:
*     in_list: int vector, first index will have request type(defined above), followed by the arguments corresponding to request.
*     req_details: int vector, contains pid and tid of calling process. Can be expanded on need.
*
*  Return type: int , Return request handle on success else failure.
*/

  int MemHal_SubmitRequest(in int[] in_list, in int[] req_details);
}

