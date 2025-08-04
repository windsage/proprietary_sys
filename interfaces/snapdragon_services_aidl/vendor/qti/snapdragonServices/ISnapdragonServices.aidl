/*====================================================================
*  Copyright (c)  Qualcomm Technologies, Inc.
*  All Rights Reserved.
*  Confidential and Proprietary - Qualcomm Technologies, Inc.
*====================================================================
*/

package vendor.qti.snapdragonServices;

/*
 * Interface: ISnapdragonServices
 * Description: This interface will be the managed pipe for passing binder
 *              objects from Vendor services to System clients.
 */
@VintfStability
interface ISnapdragonServices{
    /**
     * service: registerService
     * Description: Registers Vendor services with Snapdragon Service
     * Input: binder_obj -> The binder pbject of the vendor service
     *        service_descriptor -> The unique name of the vendor service
     */
    void registerService(in IBinder binder_obj, in String service_descriptor);

    /**
     * service: connectService
     * Description: Gets clients the binder object of the Vendor service they
     *              want.
     * Input: service_descriptor -> The unique name of the vendor service
     */
    IBinder connectService(in String service_descriptor);

    /**
     * service: getRegisteredServices
     * Description: Give a list of all currently registered services
     */
    List<String> getRegisteredServices();
}
