/******************************************************************************
  Copyright (c) 2023 Qualcomm Technologies, Inc.
  All Rights Reserved.
  Confidential and Proprietary - Qualcomm Technologies, Inc.
******************************************************************************/

package vendor.qti.ims.uceaidlservice;

import vendor.qti.ims.uceaidlservice.IOptionsListener;
import vendor.qti.ims.uceaidlservice.IOptionsService;
import vendor.qti.ims.uceaidlservice.IPresenceListener;
import vendor.qti.ims.uceaidlservice.IPresenceService;
import vendor.qti.ims.uceaidlservice.IUceListener;
import vendor.qti.ims.uceaidlservice.UceStatus;
import vendor.qti.ims.uceaidlservice.IOptionsListener;
import vendor.qti.ims.uceaidlservice.IOptionsService;
import vendor.qti.ims.uceaidlservice.IPresenceListener;
import vendor.qti.ims.uceaidlservice.IPresenceService;
import vendor.qti.ims.uceaidlservice.IOptionsListener;
import vendor.qti.ims.uceaidlservice.IOptionsService;
import vendor.qti.ims.uceaidlservice.IPresenceService;
import vendor.qti.ims.uceaidlservice.IOptionsListener;
import vendor.qti.ims.uceaidlservice.IOptionsService;
import vendor.qti.ims.uceaidlservice.IPresenceListener;
import vendor.qti.ims.uceaidlservice.IPresenceService;

@VintfStability
interface IUceService {
    /**
     * Add a IUceListener to the service
     *
     * @param uceListener IUceListener object
     * @return
     *
     */
    UceStatus addUceListener(in IUceListener listener);

    /**
     * Places a request to create Options Service.
     * upon successful, the application should expect to receive
     * IOptionsListener.OnOptionsCreated() callback
     *
     * Note: Customer needs to create unique listener for each subscription/iccid
     *
     * @param listener        IOptionsListener object
     * @param clientHandle    a token from the client
     * @param iccid           ICCID of the subscription to work on.
     * @return
     */
    UceStatus createOptionsService(in IOptionsListener listener, in long clientHandle,
        in String iccid);

    /**
     * Places a request to create Presence Service.
     * upon successful, the application should expect to receive
     * IPresenceListener.OnPresenceCreated() callback
     *
     * Note: Customer needs to create unique listener for each subscription/Iccid
     *
     * @param listener        IPresenceListener object
     * @param clientHandle    a token from the client
     * @param iccid           ICCID of the subscription to work on.
     * @return
     */
    UceStatus createPresenceService(in IPresenceListener listener, in long clientHandle,
        in String iccid);

    /**
     * Delete the instance of Option Service
     * @param serviceHandle     received in IOptionsListener.OnOptionsCreated() callback
     * @param out none
     */
    void destroyOptionsService(in long serviceHandle);

    /**
     * Delete the instance of Presence Service
     * @param serviceHandle     received in IPresenceListener.OnPresenceCreated() callback
     * @param out none
     */
    void destroyPresenceService(in long serviceHandle);

    /**
     * Get the Options Service Instance
     * @param serviceHandle    received in IOptionsListener.OnOptionsCreated()
     * @return                 Instance of IOptionsService
     */
    IOptionsService getOptionsService(in long serviceHandle);

    /**
     * Get the Presence Service Instance
     *
     * @param serviceHandle    received in IPresenceListener.OnPresenceCreated()
     * @return                 Instance of PresenceService
     */
    IPresenceService getPresenceService(in long serviceHandle);
}
