/*==============================================================================
  FILE:         QmsSdkNtnSatellite.h

  OVERVIEW:     -

        Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
        All rights reserved.
        Confidential and Proprietary - Qualcomm Technologies, Inc.
==============================================================================*/

#pragma once

#include <stdlib.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef enum {
    /* Invalid Status */
    INVALID = -1,
    /* No error in the request */
    SUCCESS = 0,
    /* Invalid Parameter[s] */
    INVALID_ARG = 1,
    /* Unexpected error occurred during processing */
    INTERNAL_ERR = 2,
    /* Unsupported command */
    NOT_SUPPORTED = 3,
    /* Request is issued on unexpected nDDS */
    INVALID_OPERATION = 4,
    /* Response to client register when the service is up */
    SERVICE_UP = 5,
    /* Response to client register when the service is down */
    SERVICE_DOWN = 6,
    /* No sim available on which the parameters are configured */
    INVALID_SLOT = 7,
    /* License needs to be registered again */
    LICENSE_RESET = 8
} status_e;

typedef enum {
    /* Invalid Status */
    NTN_SATELLITE_STATUS_INVALID = -1,
    /* No error in the request */
    NTN_SATELLITE_STATUS_SUCCESS = 0,
    /* Invalid Parameter[s] */
    NTN_SATELLITE_STATUS_INVALID_ARG = 1,
    /* Unexpected error occurred during processing */
    NTN_SATELLITE_STATUS_INTERNAL_ERR = 2,
    /* Unsupported command */
    NTN_SATELLITE_STATUS_NOT_SUPPORTED = 3,
    /* Request is issued on invalid modem state */
    NTN_SATELLITE_STATUS_INVALID_STATE = 4,
    /* Request is issue when resouces are not availble */
    NTN_SATELLITE_STATUS_NO_RESOURCES = 5,
    /* Response to client register satellite modem is not available */
    NTN_SATELLITE_STATUS_RADIO_NOT_AVAILABLE = 6,
    /* Network Access is barred for the requested service */
    NTN_SATELLITE_STATUS_ACCESS_BARRED = 7,
     /* Request Aborted */
    NTN_SATELLITE_STATUS_REQUEST_ABORTED = 8,
     /* Network is not reachable */
    NTN_SATELLITE_STATUS_NOT_REACHABLE= 9,
     /* Request Timeout on the network */
    NTN_SATELLITE_STATUS_NETWORK_TIMEOUT= 10,
    /* Request failed due to network error */
    NTN_SATELLITE_STATUS_NETWORK_ERROR = 11,
    /* Request not authorised */
    NTN_SATELLITE_STATUS_NOT_AUTHORIZED = 12,

} satellite_status_e;


typedef enum {
    /* Invalid event */
    NTN_SATELLITE_MESSAGE_INVALID = -1,
    /* Incoming message received event */
    NTN_SATELLITE_MESSAGE_RECEIVED = 1,
    /* satellite position update event */
    NTN_SATELLITE_POSITION_UPDATE = 2,
    /* satellite modem state update event */
    NTN_SATELLITE_MODEM_STATE_UPDATE = 3,
    /* satellite signal strength update event */
    NTN_SATELLITE_SIGNAL_STRENGTH_UPDATE = 4,
    /* Satellite capabilities update event */
    NTN_SATELLITE_CAPABILITIES_UPDATE = 5,
    /* Satellite support update event */
    NTN_SATELLITE_SUPPORT_UPDATE = 6,
    /* Satellite activation failure */
    NTN_SATELLITE_ACTIVATION_FAILURE = 7,
    /* Cellular coverage update event */
    NTN_SATELLITE_CELLULAR_COVERAGE_UPDATE = 8,

} satellite_event_e;


typedef enum {
    /* Invalid Status */
    NTN_SATELLITE_STATE_INVALID = -1,
    /* Device does not support Satellite modem capability */
    NTN_SATELLITE_STATE_UNAVAILABLE = 1,
    /* Device supports Satellite modem however it is not enabled */
    NTN_SATELLITE_STATE_POWER_OFF = 2,
    /* Cellular scanning is enabled. This state is not used right now as it tracks cellular state */
    NTN_SATELLITE_STATE_IDLE = 3,
    /* Rx or Tx message transfer is in progress */
    NTN_SATELLITE_STATE_TRANSFERRING = 4,
   /* Device is powered on but not registered to the satellite cell */
    NTN_SATELLITE_STATE_NOT_CONNECTED = 5,
    /* Device is registered to the satellite Cell */
    NTN_SATELLITE_STATE_CONNECTED = 6,

} satellite_state_e;


typedef enum {
    /* Non-terrestrial network signal strength is unavailable */
    NTN_SATELLITE_SIGNAL_STRENGTH_NONE = -1,
    /* Non-terrestrial network signal strength is poor */
    NTN_SATELLITE_SIGNAL_STRENGTH_POOR = 1,
    /* Non-terrestrial network signal strength is moderate */
    NTN_SATELLITE_SIGNAL_STRENGTH_MODERATE = 2,
     /* Non-terrestrial network signal strength is good */
    NTN_SATELLITE_SIGNAL_STRENGTH_GOOD = 3,
    /* Non-terrestrial network signal strength is great */
    NTN_SATELLITE_SIGNAL_STRENGTH_GREAT = 4,

} satellite_signal_strength_e;


typedef enum {
    /* Request Priority unknown */
    NTN_SATELLITE_PRIORITY_TYPE_UNKNOWN            = 0,
    /* Request Priority is for Emergency Service */
    NTN_SATELLITE_PRIORITY_TYPE_EMERGENCY          = 1,
    /* Request Priority is for non-Emergency Service */
    NTN_SATELLITE_PRIORITY_TYPE_NON_EMERGENCY      = 2,

} satellite_priority_type_e;


typedef enum {
    /* Non-terrestrial Unknown RAT */
    NTN_SATELLITE_RADIO_TECHNOLOGY_UNKNOWN     = 0,
    /* Non-terrestrial NB IOT RAT */
    NTN_SATELLITE_RADIO_TECHNOLOGY_NB_IOT  = 1,
    /* Non-terrestrial New Radio 5G RAT */
    NTN_SATELLITE_RADIO_TECHNOLOGY_NR         = 2,
    /* Non-terrestrial Enhanced Machine Type Communication RAT */
    NTN_SATELLITE_RADIO_TECHNOLOGY_EMTC      = 3,
    /* Non-terrestrial proprietary RAT */
    NTN_SATELLITE_RADIO_TECHNOLOGY_PROPRIETARY = 4,

} satellite_radio_technology_e;


typedef enum {
    /* Suggested device hold position unknown */
    NTN_SATELLITE_DEVICE_HOLD_POSITION_UNKNOWN         = 0,
    /* Suggestion to hold device in Portrait mode*/
    NTN_SATELLITE_DEVICE_POSITION_PORTRAIT        = 1,
    /* Suggestion to hold device in Landscape mode using left hand */
    NTN_SATELLITE_DEVICE_HOLD_POSITION_LANDSCAPE_LEFT  = 2,
    /* Suggestion to hold device in Landscape mode using right hand */
    NTN_SATELLITE_DEVICE_HOLD_POSITION_LANDSCAPE_RIGHT = 3,

} satellite_device_hold_position_e;


typedef enum {
    /* Display mode unknown */
    NTN_SATELLITE_DEVICE_DISPLAY_MODE_UNKNOWN         = 0,
    /* Display mode for non-foldable phones */
    NTN_SATELLITE_DEVICE_DISPLAY_MODE_FIXED        = 1,
    /* Display mode for foldable phones when device is opened */
    NTN_SATELLITE_DEVICE_DISPLAY_MODE_OPENED  = 2,
    /* Display mode for foldable phones when device is closed */
    NTN_SATELLITE_DEVICE_DISPLAY_MODE_CLOSED = 3,

} satellite_device_display_mode_e;


typedef enum {
    /* Action unknown */
    NTN_SATELLITE_ACTION_UNKNOWN         = 0,
    /* Start the suggested action */
    NTN_SATELLITE_ACTION_START_SENDING    = 1,
    /* Stop the suggested action */
    NTN_SATELLITE_ACTION_STOP_SENDING    = 2,

} satellite_action_e;

/* Message structure to hold Satellite antenna position */
typedef struct {
    /* Suggested hold position of a device */
    satellite_device_hold_position_e holdPosition;
    /* Device display hold mode */
    satellite_device_display_mode_e  displayMode;
    /* x-axis coordinates of device antenna */
    float                            xAxis;
    /* y-axis coordinates of device antenna */
    float                            yAxis;
    /* z-axis coordinates of device antenna */
    float                            zAxis;

} satellite_antenna_position;


/* Message structure to hold Satellite capabilities */
typedef struct {
    /* Supported RAT */
    satellite_radio_technology_e ratTechnology;
    /* If Satellite requires pointing */
    bool                         isPointingRequired;
    /* Maximum size of message exchange */
    int64_t                      maxSupportedMessageBytes;
    /* Antenna capabilities */
    satellite_antenna_position   *antennaPosition;
    /* Num of antenna positions carried by antennaPosition array */
    int64_t                      antennaPositionCount;

} satellite_capabilities;

/* Message structure to hold the satellite position from the device*/
typedef struct
{
    /* Position of satellite in hoizontal plane from the device  */
    float satelliteAzimuthDegrees;
    /* Position of satellite in vertical plane from the device  */
    float satelliteElevationDegrees;

} satellite_position;


/* Message structure to hold the message content*/
typedef struct
{
    /* Message content */
    uint8_t* messageBytes;
    /* Length of the message */
    uint32_t length;

} satellite_message;

/* Message structure to notify satellite events and associated data */
typedef struct {
  /* Event Indication received from the service */
  satellite_event_e event;

  union {
    /* Received Message. Used with event NTN_SATELLITE_MESSAGE_RECEIVED */
    satellite_message             messageData;
    /* Received position. Used with event NTN_SATELLITE_POSITION_UPDATE */
    satellite_position            satellitePosition;
    /* Satellite capabilities update. Used with event NTN_SATELLITE_CAPABILITIES_UPDATE */
    satellite_capabilities        capabilities;
    /* Receievd Satellite Signal strength. Used with event NTN_SATELLITE_SIGNAL_STRENGTH_UPDATE */
    satellite_signal_strength_e   signalstrength;
    /* State update for the Modem . Used with event NTN_SATELLITE_MODEM_STATE_UPDATE  */
    satellite_state_e             satelliteState;
    /* Satellite support update. Used with event NTN_SATELLITE_SUPPORT_UPDATE */
    bool                          isSatelliteSupported;
    /* Satellite activation error code. used with event NTN_SATELLITE_ACTIVATION_FAILURE */
    uint64_t                      errorCode;
    /* Cellular coverage availability update due to background scan. Used with event NTN_SATELLITE_CELLULAR_COVERAGE_UPDATE */
    bool                          isCellularCoverageAvailable;

  } eventData;

} satellite_event_info;

/* Message structure to hold the gnss position fix */
typedef struct {
    /* Latitude of the position fix  */
    float latitude;
    /* Longitude of the position fix  */
    float longitude;
    /* Altitude of the position fix  */
    float altitude;
    /* Clock offset of the position fix  */
    float clockOffset;
} satellite_receiver_position_fix;

/* Message structure to hold iccid of the SIM */
typedef struct {
  /* ICCID */
  uint8_t*  iccid;
  /* Lenth of the ICCID field */
  uint32_t  length;

} satellite_sim_iccid;

/*EARFCN and band information */
typedef struct {
  /*network PLMN associated with Channel */
  uint8_t*  mccmnc;

  /* Lenth of MCC MNC field */
  uint32_t  plmnLength;

  /* num of supported Satellite bands */
  uint32_t  numOfBands;

/* list of Satellite bands */
  uint64_t* satelliteBands;

/* num of supported radio channels */
  uint32_t  numOfRadioChannels;

/* list of E-UTRAN absolute radio frequency channels */
  uint64_t* satelliteEarfcs;

} satellite_system_selection_specifier;

/*===========================================================================
  FUNCTION:  service_status_ind
===========================================================================*/
/** @ingroup service_status_ind

    Indication function to send the service status updates to the client.

    @param[in] status, service status (up / down).

    @return none

    @dependencies This API will be triggered when there is a change in service status.
*/
/*=========================================================================*/
typedef void (*service_status_ind)(status_e status);

/*===========================================================================
  FUNCTION:  satellite_event_ind
===========================================================================*/
/** @ingroup satellite_event_ind

    Unsolicited event indications to the client providing various
    information about the satellite modem

    @param[in] eventData, events and associated data


    @return none

    @dependencies

*/
/*=========================================================================*/
typedef void (*satellite_event_ind)(satellite_event_info eventData);

/*===========================================================================
  FUNCTION:  satellite_result_cb
===========================================================================*/
/** @ingroup satellite_result_cb

    Callback function to send the result of invocation of satellite service APIs

    @param[in] transaction id of the request as returned by the given API
    @param[in] status of the request

    @return none

    @dependencies
    This API will be triggered as result of set_satellite_mode API invocation
*/
/*=========================================================================*/
typedef void (*satellite_result_cb)(uint64_t transaction_id, satellite_status_e status);

/*===========================================================================
  FUNCTION:  satellite_supported_cb
===========================================================================*/
/** @ingroup satellite_supported_cb

    Callback function to update the result of request_is_satellite_supported
    API

    @param[in] API result status
    @param[in] Satellite Capabilities

    @return none

    @dependencies
    This API will be triggered as result of request_is_satellite_supported API
    invocation
*/
/*=========================================================================*/
typedef void (*satellite_supported_cb)(satellite_status_e status, bool isSupported);

/*===========================================================================
  FUNCTION:  satellite_capabilities_cb
===========================================================================*/
/** @ingroup satellite_capabilities_cb

    Callback function to update the result of request_satellite_capabilities API

    @param[in] API result status
    @param[in] Satellite Capabilities

    @return none

    @dependencies
    This API will be triggered as result of request_satellite_capabilities API
    invocation
*/
/*=========================================================================*/
typedef void (*satellite_capabilities_cb)(satellite_status_e status,
                                          satellite_capabilities capabilities);

/*===========================================================================
  FUNCTION:  satellite_signal_strength_cb
===========================================================================*/
/** @ingroup satellite_signal_strength_cb

    Callback function to update the result of request_satellite_signal_strength API

    @param[in] API result status
    @param[in] satellite signal strength

    @return none

    @dependencies
    This API will be triggered as result of request_satellite_signal_strength API
    invocation
*/
/*=========================================================================*/
typedef void (*satellite_signal_strength_cb)(satellite_status_e status,
                                             satellite_signal_strength_e satelliteSignalStrength);

/*===========================================================================
  FUNCTION:  satellite_state_request_cb
===========================================================================*/
/** @ingroup satellite_state_request_cb

    Callback function to update the result of request_satellite_state API

    @param[in] API result status
    @param[in] satellite modem state

    @return none

    @dependencies
    This API will be triggered as result of request_satellite_state API
    invocation
*/
/*=========================================================================*/
typedef void (*satellite_state_request_cb)(satellite_status_e status,
                                           satellite_state_e satelliteModemState);

/*===========================================================================
  FUNCTION:  qms_ntn_get_satellite_service_status
===========================================================================*/
/** @ingroup qms_ntn_get_satellite_service_status

    This API is used to register for satellite service status. This API should
    be invoked first to know the status of service before client registers itself
    to the service

    @param[in] status_cb_fn, callback function over which service indication be
    received on.

    @return none

    @dependencies SERVICE UP / DOWN will be received in the status of the
    status_cb_fn depending on the server / Modem status.
*/
/*=========================================================================*/
extern void qms_ntn_get_satellite_service_status(service_status_ind   status_cb_fn);

/*===========================================================================
   FUNCTION:  qms_ntn_register_satellite_client
 ===========================================================================*/
 /** @ingroup qms_ntn_register_satellite_client

    This API registers the client with the service. Client should register
    it before invoking any other API

    @param[in] stats_ind_fn, indication function over which sync/async stats updates will be
    received on.
    @param[in] satellite_result_cb_fn function over which request response will be received

     @return transaction id of the transaction

    @dependencies SERVICE UP / DOWN will be received in the status of the
    status_cb_fn depending on the server / Modem status.
    Client must register the client first before triggering the other APIs.
 */
 /*=========================================================================*/
extern uint64_t qms_ntn_register_satellite_client(satellite_event_ind  satellite_event_ind_fn,
                                                 satellite_result_cb satellite_result_cb_fn);

/*===========================================================================
   FUNCTION:  qms_ntn_update_satellite_system_selection_specifiers
 ===========================================================================*/
 /** @ingroup qms_ntn_update_satellite_system_selection_specifiers

    This API updates the service with the bands and radio channels to scan for each PLMN

    @param[in] listSpecifiers, List of PLMN wise system selection specifers.
    @param[in] numOfSpecifiers, inum of system selction specifiers
    received on.
    @param[in] satellite_result_cb_fn function over which request response will be received

     @return transaction id of the transaction

    @dependencies SERVICE UP / DOWN will be received in the status of the
    status_cb_fn depending on the server / Modem status.
    Client must register the client first before triggering the other APIs.
 */
 /*=========================================================================*/
extern uint64_t qms_ntn_update_satellite_system_selection_specifiers(satellite_system_selection_specifier* listSpecifiers,
                                                                     uint32_t                              numOfSpecifiers,
                                                                     satellite_result_cb                   satellite_result_cb_fn);

/*===========================================================================
  FUNCTION:  qms_ntn_request_is_satellite_supported
===========================================================================*/
/** @ingroup qms_ntn_request_is_satellite_supported

    This API is used to query if device supports Satellite Modem Capability

    @param[in] callback function over which device support response would
    be received

    @return none

    @dependencies SERVICE UP / DOWN will be received in the status of the
    status_cb_fn depending on the server / Modem status.
    Client must register the client first before triggering the other APIs.
*/
/*=========================================================================*/
extern void qms_ntn_request_is_satellite_supported(satellite_supported_cb satellite_support_cb_fn);

/*===========================================================================
  FUNCTION:  qms_ntn_set_up_satellite_messaging
===========================================================================*/
/** @ingroup qms_ntn_set_up_satellite_messaging

    This API is used to enable or disable the satellite mode

    @param[in] enable or disable satellite mode
    @param[in] priority of satellite attach
    @param[in] enable or disable demo mode
    @param[in] iccid, integrated circuit identification number of Subscriber Identity Module (SIM).
    @param[in] callback function over which request response will be received

    @return transaction id of the transaction

    @dependencies SERVICE UP / DOWN will be received in the status of the
    status_cb_fn depending on the server / Modem status.
    Client must register the client first before triggering the other APIs.
*/
/*=========================================================================*/
extern uint64_t qms_ntn_set_up_satellite_messaging(bool enable,
                                                   satellite_priority_type_e priorityType,
                                                   bool demoMode,
                                                   satellite_sim_iccid iccid,
                                                   satellite_result_cb satellite_result_cb_fn);

/*===========================================================================
  FUNCTION:  qms_ntn_enable_cellular_scan_and_attach_in_satellite_enabled_mode
===========================================================================*/
/** @ingroup qms_ntn_enable_cellular_scan_and_attach_in_satellite_enabled_mode

    This API is used to enable\disable cellular scan while satellite mode
    is enabled

    @param[in] enable or disable cellular scan mode
    @param[in] callback function over which request response will be received

    @return transaction id of the transaction

    @dependencies SERVICE UP / DOWN will be received in the status of the
    status_cb_fn depending on the server / Modem status.
    Client must register the client first before triggering the other APIs.
    Satellite mode should be enabled before invocation of this API
*/
/*=========================================================================*/
extern uint64_t qms_ntn_enable_cellular_scan_and_attach_in_satellite_enabled_mode(bool enable,
                                                                                  satellite_result_cb satellite_result_cb_fn);

/*===========================================================================
  FUNCTION:  request_satellite_capabilities
===========================================================================*/
/** @ingroup request_satellite_capabilities

    This API is used to query the capabilities of the Satellite modem

    @param[in] callback function over which request response will be received

    @return none

    @dependencies SERVICE UP / DOWN will be received in the status of the
    status_cb_fn depending on the server / Modem status.
    Client must register the client first before triggering the other APIs.
*/
/*=========================================================================*/
extern void qms_ntn_request_satellite_capabilities(satellite_capabilities_cb satellite_cap_cb_fn);

/*===========================================================================
  FUNCTION:  qms_ntn_notify_satellite_pointing_info
===========================================================================*/
/** @ingroup qms_ntn_notify_satellite_pointing_info

    Request to start or stop sending the pointing info of satellite.
    When started, this API would result in asynchronous satellite_event_ind
    which would carry pointing information as and when there's an update.

    @param[in] action to start or stop sending pointing info
    @param[in] callback function over which request response will be received

    @return transaction id of the transaction

    @dependencies SERVICE UP / DOWN will be received in the status of the
    status_cb_fn depending on the server / Modem status.
    Client must register the client first before triggering the other APIs.
*/
/*=========================================================================*/
extern uint64_t qms_ntn_notify_satellite_pointing_info(satellite_action_e action,
                                                       satellite_result_cb satellite_result_cb_fn);

/*===========================================================================
  FUNCTION:  qms_ntn_notify_satellite_signal_strength
===========================================================================*/
/** @ingroup qms_ntn_notify_satellite_signal_strength

    Request to start or stop sending the signal strength of satellite.
    When started, this API would result in asynchronous satellite_event_ind
    which would carry signal strength information as and when there's an update

    @param[in] action to start or stop sending signal strength
    @param[in] callback function over which request response will be received

    @return transaction id of the transaction

    @dependencies SERVICE UP / DOWN will be received in the status of the
    status_cb_fn depending on the server / Modem status.
    Client must register the client first before triggering the other APIs.
*/
/*=========================================================================*/
extern uint64_t qms_ntn_notify_satellite_signal_strength(satellite_action_e action,
                                                    satellite_result_cb satellite_result_cb_fn);

/*===========================================================================
  FUNCTION:  qms_ntn_request_satellite_state
===========================================================================*/
/** @ingroup qms_ntn_request_satellite_state

    This API is used to query state of satellite modem

     @param[in] callback function over which request response will be received

    @return none

    @dependencies SERVICE UP / DOWN will be received in the status of the
    status_cb_fn depending on the server / Modem status.
    Client must register the client first before triggering the other APIs.
*/
/*=========================================================================*/
extern void qms_ntn_request_satellite_state(satellite_state_request_cb satellite_state_cb_fn);

/*===========================================================================
  FUNCTION:  qms_ntn_request_satellite_signal_strength
===========================================================================*/
/** @ingroup qms_ntn_request_satellite_signal_strength

    This API is used to query signal strength of NTN satellite

     @param[in] callback function over which request response will be received

    @return none

    @dependencies SERVICE UP / DOWN will be received in the status of the
    status_cb_fn depending on the server / Modem status.
    Client must register the client first before triggering the other APIs.
*/
/*=========================================================================*/
extern void qms_ntn_request_satellite_signal_strength(satellite_signal_strength_cb satellite_signal_strength_cb_fn);

/*===========================================================================
  FUNCTION:  qms_ntn_send_satellite_message
===========================================================================*/
/** @ingroup qms_ntn_send_satellite_message

    This API is used to send message on the satellite network

     @param[in] message message content to be sent
     @param[in] priorityType priority of the message
     @param[in] callback function over which request response will be received

    @return transaction id of the transaction

    @dependencies SERVICE UP / DOWN will be received in the status of the
    cb_fn depending on the server / Modem status.
    Client must register the client first before triggering the other APIs.
*/
/*=========================================================================*/
extern uint64_t qms_ntn_send_satellite_message(satellite_message message,
                                               satellite_priority_type_e priorityType,
                                               satellite_result_cb satellite_result_cb_fn);

/*===========================================================================
  FUNCTION:  qms_ntn_send_satellite_message
===========================================================================*/
/** @ingroup qms_ntn_abort_satellite_message

    This API is used to abort previous message on the satellite network

     @param[in] callback function over which request response will be received

    @return transaction id of the transaction

    @dependencies SERVICE UP / DOWN will be received in the status of the
    status_cb_fn depending on the server / Modem status.
    Client must register the client first before triggering the other APIs.
    Client should send a message before aborting it.
*/
/*=========================================================================*/
extern uint64_t qms_ntn_abort_satellite_message(satellite_result_cb satellite_result_cb_fn);


/*===========================================================================
  FUNCTION:  qms_ntn_set_satellite_receiver_position_fix
===========================================================================*/
/** @ingroup qms_ntn_set_satellite_receiver_position_fix

    This API is used to update receivers GNSS position


     @param[in] positionFix postion fix of the receiver
     @param[in] callback function over which request response will be received

    @return transaction id of the transaction

    @dependencies SERVICE UP / DOWN will be received in the status of the
    cb_fn depending on the server / Modem status.
    Client must register the client first before triggering the other APIs.
*/
/*=========================================================================*/

extern uint64_t qms_ntn_set_satellite_receiver_position_fix(satellite_receiver_position_fix positionFix,
                                                            satellite_result_cb satellite_result_cb_fn);

/*===========================================================================
  FUNCTION:  qms_ntn_deregister_satellite_client
===========================================================================*/
/** @ingroup qms_ntn_deregister_satellite_client

    This API is used to deregister the client with the service

    @param[in] stats_ind_fn, indication function over which async stats updates
    were received
    @param[in] callback function over which request response will be received

    @return none

    @dependencies SERVICE UP / DOWN will be received in the status of the
    status_cb_fn depending on the server / Modem status.
    Client must register the client first before triggering the other APIs.
*/
/*=========================================================================*/
extern uint64_t qms_ntn_deregister_satellite_client(satellite_event_ind satellite_event_ind_fn,
                                                   satellite_result_cb satellite_result_cb_fn);

#ifdef __cplusplus
} // Extern "C"
#endif