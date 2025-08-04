/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.listen;

import java.nio.ByteBuffer;

/**
 * Listen Type defines all the data structures that are used by
 * ListenEngineAPIs
 */
public class ListenTypes  {

    /*  Types of event notifications sent to the App.
     *  All events are sent to set ListenReceiver callback
     *  function except where noted. */

    /** Event notifying that Listen feature is disabled  */
    public static final int LISTEN_FEATURE_DISABLE_EVENT = 1;
    /** Event notifying that Listen feature is enabled  */
    public static final int LISTEN_FEATURE_ENABLE_EVENT = 2;

    /** Event notifying that VoiceWakeuo feature is disabled  */
    public static final int VOICEWAKEUP_FEATURE_DISABLE_EVENT = 3;
    /** Event notifying that VoiceWakeup feature is enabled  */
    public static final int VOICEWAKEUP_FEATURE_ENABLE_EVENT = 4;

    /** Event notifying detection was successful -
     *     minimum keyword and user confidence levels were met.
     *  <p> Sent only to VoiceWakeup session(s) that had registered
     *     for specific Keyword that was detected. */
    public static final int DETECT_SUCCESS_EVENT = 5;
    /** Event notifying that detection was started but failed.
     *  <p> This event is returned only when Special notify-all
     *     mode is enabled.
     *  <p> Sent only to session(s) that are registered. */
    public static final int DETECT_FAILED_EVENT = 6;

    /** Event notifying that SoundModel deregistered implicitly
     *  by ListenEngine.
     *  <p> Sent only to session(s) that are registered. */
    public static final int DEREGISTERED_EVENT = 7;

    /** Event notifying that Listen detection is now active  */
    public static final int LISTEN_RUNNING_EVENT = 8;
    /** Event notifying that Listen detection is not active
     * <p>Listen is temporarily stopped when microphone is used
     *     during phone call or audio recording */
    public static final int LISTEN_STOPPED_EVENT = 9;

    /** Event notifying that catastrophic error occurred and
     *  Listen Service must be restarted
     *  <p>All previously created Listen objects are stale
     *  and must be recreated
     *  <p>Any previously registered SoundModels must be
     *  re-registered. */
    public static final int LISTEN_ENGINE_DIED_EVENT = 10;

    /* The following constants define various status types */

    /** Success status return when method completed without error */
    public static final int STATUS_SUCCESS = 0;
    /** Generic failure-unknown reason */
    public static final int STATUS_EFAILURE = -1;
    /** Failure - Bad parameters */
    public static final int STATUS_EBAD_PARAM = -2;
    /** Failure - SoundModel is not registered */
    public static final int STATUS_ESOUNDMODEL_NOT_REGISTERED = -3;
    /** Failure - Previously registered SoundModel should be
     *  deregistered before registering a new SoundModel */
    public static final int STATUS_ESOUNDMODEL_ALREADY_REGISTERED = -4;
    /** Failure - either Listen or VoiceWakeup Feature not
     *  enabled */
    public static final int STATUS_EFEATURE_NOT_ENABLED = -5;
    /** Failure - requested session/masterControl object can not be
     *  instanced */
    public static final int STATUS_ERESOURCE_NOT_AVAILABLE = -6;
    /** Failure - callback not set for session/masterControl object */
    public static final int STATUS_ECALLBACK_NOT_SET = -7;

    /** Failure - buffering is not enabled for this session */
    public static final int STATUS_EBUFFERING_NOT_ENABLED = -8;
    /** Failure - currently not doing buffering */
    public static final int STATUS_ENOT_BUFFERING = -9;

    /** Failure - data in buffer is not complete; overflow occurred */
    public static final int STATUS_EBUFFERING_DATA_INCOMPLETE = -10;
    /** Failure - keyword specified as input parameter not in SoundModel */
    public static final int STATUS_EKEYWORD_NOT_IN_SOUNDMODEL = -11;
    /** Failure - user specified as input parameter not in SoundModel */
    public static final int STATUS_EUSER_NOT_IN_SOUNDMODEL = -12;
    /** Failure - user+keyword pair specified as input parameter not in SoundModel */
    public static final int STATUS_EKEYWORD_USER_PAIR_NOT_IN_SOUNDMODEL = -13;
    /** Failure - feature not supported with given SoundModel type */
    public static final int STATUS_ENOT_SUPPORTED_FOR_SOUNDMODEL_VERSION = -14;
    /** Failure - type or version of SoundModel not support by algorithm  */
    public static final int STATUS_EUNSUPPORTED_SOUNDMODEL = -15;
    /** Failure - user specified as input parameter already used or reserved */
    public static final int STATUS_EUSER_NAME_CANNOT_BE_USED = -16;
    /** Failure - memory could not be allocated */
    public static final int STATUS_ENO_MEMORY = -17;
    /** Failure - detection not running */
    public static final int STATUS_EDETECTION_NOT_RUNNING = -18;
    /** Failure - keyword-user pair to be trained already in SoundModel */
    public static final int STATUS_EUSER_KEYWORD_PAIRING_ALREADY_PRESENT = -19;
    /** Failure - soundmodels being merged can not contain the same keyword */
    public static final int STATUS_ESOUNDMODELS_WITH_SAME_KEYWORD_CANNOT_BE_MERGED = -20;
    /** Failure - soundmodels being merged can not contain the same user-keyword pair */
    public static final int STATUS_ESOUNDMODELS_WITH_SAME_USER_KEYWORD_PAIR_CANNOT_BE_MERGED = -21;
    /** Failure - merge would result in soundmodel with number of keywords exceeding capacity */
    public static final int STATUS_EMAX_KEYWORDS_EXCEEDED = -22;
    /** Failure - merge would result in soundmodel with number of users exceeding capacity */
    public static final int STATUS_EMAX_USERS_EXCEEDED = -23;
    /** Failure - can not delete keyword from soundmodel if it is the only one */
    public static final int STATUS_ECANNOT_DELETE_LAST_KEYWORD = -24;
    /** Failure - no speach detected within recording */
    public static final int STATUS_ENO_SPEACH_IN_RECORDING = -25;
    /** Failure - level of noise in recording to high */
    public static final int STATUS_ETOO_MUCH_NOISE_IN_RECORDING = -26;
    /** Failure - Recording is too short */
    public static final int STATUS_ERECORDING_TOO_SHORT = -27;
    /** Failure - Recording is too long */
    public static final int STATUS_ERECORDING_TOO_LONG = -28;
    /** Failure - Recording has chopped speech/keyword */
    public static final int STATUS_ECHOPPED_SAMPLE = -29;
    /** Failure - Recording has clipped speech/keyword */
    public static final int STATUS_ECLIPPED_SAMPLE = -30;
    /** Failure - Function not implemented */
    public static final int STATUS_EFUNCTION_NOT_IMPLEMENTED = -31;
    /** Failure - Input string has small syllables */
    public static final int STATUS_ETOO_SMALL_SYLLABLES = -32;
    /** Failure - Input string has many syllables */
    public static final int STATUS_ETOO_MANY_SYLLABLES = -33;
    /** Failure - Input string has local mismatch */
    public static final int STATUS_ELOCALE_MISMATCH = -34;
    /** Failure - Recording quality for UDK training is not proper w.r.t text input */
    public static final int STATUS_BAD_UDK_RECORDING_QUALITY = -35;

    /* The following constants defined are error codes returned from LSM server side */
    /** TODO: Add all error codes, which can be returned from server side here */

    /* The following constants define the detection modes of VoiceWakeup */

    /** Defines Keyword-only detection mode */
    public static final int KEYWORD_ONLY_DETECTION_MODE = 1;
    /** Defines Keyword detection and user verification mode */
    public static final int USER_KEYWORD_DETECTION_MODE = 2;

    /* The following constants define the parameters for Set/GetParam function */

    /** Defines a string for Listen feature parameter type */
    public static final String LISTEN_FEATURE = "ListenFeature";
    /** Defines a string for VoiceWakup feature parameter type */
    public static final String VOICEWAKEUP_FEATURE = "VoiceWakeupFeature";
    /** Defines a string for Enable parameter value */
    public static final String ENABLE = "enable";
    /** Defines a string for Disable parameter value */
    public static final String DISABLE = "disable";

    /*The following constants are used for UDK config quality check results */
    public static final int UdkTextInputQualityPass = 0x00000000;
    public static final int UdkTextInputTooSmallSyllables = 0x00020001;
    public static final int UdkTextInputTooManySyllables = 0x00020002;
    public static final int UdkTextInputFail = 0x00020003;


    /* The following constants define the parameters for DetectionData type field */

    /** Defines a string that identifies the VoiceWakeupDetectionEventData
     *  event data structure (version 1.0)
     *  <p>This is allocated if VoiceWakeup detection event is parsed by
     *  ListenSoundModel::parseDetectionEventData()
     */
    public static final String VWU_EVENT_0100 = "VoiceWakeup_DetectionData_v0100";

    /** Defines a string that identifies the VoiceWakeupDetectionEventDataV2
     *  event data structure (version 2.0)
     *  <p>This is allocated if VoiceWakeup detection event is parsed by
     *  ListenSoundModel::parseDetectionEventData()
     */
    public static final String VWU_EVENT_0200 = "VoiceWakeup_DetectionData_v0200";

    /** The following constants define possibly supported Application Types.
     *  Used as input to ListenVoiceWakeupSession.createInstance()  */
    /** Application Type defined for Snapdragon Voice Activation */
    public static final int SVA_APP_TYPE = 1;

    /** Event Data Type returned to client when SVA 1.0 detects keyword.
     * <p> This describes which set of fields are passed to processEvent(). When
     * type = EVENT_DATA_TYPE, only size and payload[] EventData fields are filled.
     */
    public static final int EVENT_DATA_TYPE = 1;
    /** Event Data Type returned to client when SVA 2.0 (or higher) detects keyword.
     * <p> This describes which set of fields are passed to processEvent(). When
     * type = EVENT_DATA_TYPE_V2 all EventData fields are filled.
     */
    public static final int EVENT_DATA_TYPE_V2 = 2;

    /** This struct is returned to the
     *  IListenEventProcessor::processEvent() by the SVA 1.0 Listen Engine
     */
    public static class EventData  {
        /** type of EventData: EVENT_DATA_TYPE, EVENT_DATA_TYPE_V2 */
        public int   type;
        /** size of array */
        public int   size;
        /** event data returned as payload */
        public byte  payload[];
      /** phrase of highest scoring detected keyword.<p>
       *  Set non-null for EVENT_DATA_TYPE_V2*/
        public String      keyword;
      /** name of highest scoring verified user.<p>
       *  Set non-null for EVENT_DATA_TYPE_V2 only if user was verified. */
        public String      user;
    }

    /** This struct contains 'identifiers' for keywords in a
     *  SVA 2.0 VoiceWakeup sound model
     */
    public static class VWUKeywordConfLevel
    {
     /** keyword phrase */
        public String      keyword;
       /** confidence level for this keyword */
        public short       confLevel;
    }

    /** This struct contains 'identifiers' for user plus keyword pairs in a
     *  SVA 2.0 VoiceWakeup sound model
     */
    public static class VWUUserKeywordPairConfLevel
    {
     /** keyword phrase */
        public String      keyword;
     /** user name */
        public String      user;
       /** confidence level for this pair */
        public short       confLevel;
    }

    /**
     *  This struct is filled by the application and passed into
     *  ListenVoiceWakeupSession::registerSoundModel(SoundModelParams)
     *  when registering a SoundModel for SVA 1.0
     *  <p>This SVA 1.0 form of detection parameters is obsolete
     *  @deprecated use RegisterParams instead.
     */
@Deprecated
    public static class SoundModelParams
    {
        /** Buffer containing SoundModel */
        public ByteBuffer           soundModelData;
        /** Type of detection to perform:
         *  KEYWORD_ONLY_DETECTION_MODE, USER_KEYWORD_DETECTION_MODE  */
        public int                  detectionMode;
        /** Minimum percent (0-100) confidence level in keyword match
         *  that will trigger DETECT_SUCCESS_EVENT event being sent to the client */
        public short                minKeywordConfidence;
        /** Minimum percent (0-100) confidence level in user match
         *  that will trigger DETECT_SUCCESS_EVENT event being sent to the client  */
        public short                minUserConfidence;
        /** Turns on special notify-all detection mode
         *  <p>Requests that ListenEngine return a DETECT_FAILED_EVENT when minimum
         *  Keyword and User confidence levels are not met */
        public boolean              bFailureNotification;
    }

    /**
     *  This struct is filled by the application and passed into
     *  ListenVoiceWakeupSession::registerSoundModel(RegisterParams)
     */
    public static class RegisterParams
    {
        /** Buffer containing SoundModel */
        public ByteBuffer                     soundModelData;
        /** Type of detection to perform:
         *  KEYWORD_ONLY_DETECTION_MODE, USER_KEYWORD_DETECTION_MODE  */
        public int                           detectionMode;
        /** Turns on special notify-all detection mode
         *  <p>Requests that ListenEngine return a DETECT_FAILED_EVENT when minimum
         *  Keyword and User confidence levels are not met */
        public boolean                       bFailureNotification;

        /** The number of keywords contained in sound model */
        public short                         numKeywords;
        /** The number of keyword-specific user data contained in sound model*/
        public short                         numKeywordUserPairs;
        /** Array of minimum percent (0-100) confidence level in keyword match
         *  that will trigger DETECT_SUCCESS_EVENT event being sent to the client
         * <p>Keyword confidence levels can be input in any order
         * <p>This param can be set to null when registering a sound model
         *  for which minimum confidence levels are not used/needed.
         * <p>When registering SVA sound model, array size must match [numKeywords, numKeywordUserPairs]
         */
        public VWUKeywordConfLevel           keywordMinConfLevels[];
        /** Array of minimum confidence levels for each active user+keyword pair
         *  that will trigger DETECT_SUCCESS_EVENT event during user verification
         * <p>Pair confidence levels can be input in any order
         * <p>This param can be set to null when registering a non-SVA sound model
         *  for which minimum confidence levels are not used/needed.
         * <p>When registering SVA sound model, array size must match [numKeywords, numKeywordUserPairs]
         */
        public VWUUserKeywordPairConfLevel   userKWPairMinConfLevels[];
        /** When non-null Look-Ahead Buffering parameters are set as specified. */
        public LookAheadBufferParams         bufferParams;
    }

    /**
     *  This struct used to passed parameters for Look-Ahead Buffering
     *  during calls to ListenVoiceWakeupSession::registerSoundModel(RegisterParams)
     */
    public static class LookAheadBufferParams
  {
        /** Maximum length (in ms) audio buffered after end of keyword */
        public boolean                       enableBuffering;
    }

    /**
     *  This struct filled and returned by ListenVoiceWakeupSession::readBuffer()
     */
    public static class ReadResults
    {
        /** status from read
         *         <br> STATUS_SUCCESS
         *         <br> STATUS_ESOUNDMODEL_NOT_REGISTERED
         *         <br> STATUS_EBUFFERING_NOT_ENABLED
         *         <br> STATUS_ENOT_BUFFERING
         *         <br> STATUS_EBUFFER_OVERFLOW_OCCURRED
         *         <br> STATUS_EBAD_PARAM
         */
        public int               status;
        /** number of samples (shorts) written to destination buffer */
        public int               writeSize;
    }

// -----------------------------------------------------------------------
// SoundModel Classes
// -----------------------------------------------------------------------


    /**
     *  This struct is an abstract class inherited by particular types
     *  of Detection event Data
     */
    public static abstract class DetectionData
    {
        /** Status of the parseDetectionEventData() */
        public int             status;
        /**type of Detection Data - includes type number */
        public String          type;
    }
    // SVA 1.0
    /**
     *  This struct is filled by
     *  ListenSoundModel::parseDetectionEventData()
     *  when event data is from VoiceWakeup detection event
     */
    public static class VoiceWakeupDetectionData extends DetectionData
    {
        /** Keyword phrase whose detected confidence level is the greatest */
        public String          keyword;
        /** percent (0-100) confidence that speech matches keyword */
        public short           keywordConfidenceLevel;
        /** percent (0-100) confidence that keyword is spoken by user
         *  who trained sound model; zero if user verification not performed */
        public short           userConfidenceLevel;
    }
    // SVA 2.0
    /**
     *  This struct is filled by
     *  ListenSoundModel::parseDetectionEventData()
     *  when event data is from VoiceWakeup detection V2 algorithm
     *  <p>Inherited fields take on special meaning when used in SVA 2.0:
     *  <br>    keyword - string of keyword with the highest returned confidence level
     *  <br>    keywordConfidenceLevel - percent value of highest returned keyword confidence level
     *  <br>    userConfidenceLevel - percent value of highest returned user confidence level
     *  <p>New fields returned for SVA 2.0:
     *  <br>    nonzeroKWConfLevels[] - array of keyword confidence levels
     *  <br>    nonzeroUserKWPairConfLevels[] - array of confidence levels for active user+keyword pairs
     */
    public static class VoiceWakeupDetectionDataV2 extends DetectionData
    {
        /** Array of confidence level for each keyword that was detected
         *  for DETECTION_SUCCESS, or all keywords for DETECTION_FAILED */
        public VWUKeywordConfLevel  nonzeroKWConfLevels[];

        /** Array of confidence level for each user+keyword pair detected
         *  for DETECTION_SUCCESS, or all active pairs for DETECTION_FAILED */
        public VWUUserKeywordPairConfLevel   nonzeroUserKWPairConfLevels[];
    }

    /**
     *  This struct is a base class inherited by application specific SoundModel
     *  data classes
     */
    public static class SoundModelInfo
    {
        /** SoundModel type current values:
         *  SVA_SOUNDMODEL_TYPE, UNKNOWN_SOUNDMODEL_TYPE */
        public int     type;
        /** SoundModel version (if known).
         * 2-bytes major number, 2-bytes minor number: 0x0100, 0x0200, ... */
        public int     version;
        /** total size of the model */
        public int     size;
    }


    /** This struct contains fields that describe the content of a
     *  SVA Sound Model
     */
    public static class SVASoundModelInfo
                               extends SoundModelInfo
    {
     /** number of keyword, users and userKeyword pairs in sound model */
        public KeywordUserCounts counts;

       /** info about each keyword in sound model */
        public KeywordInfo       keywordInfo[];

     /** names of users with some user data in SM*/
        public String            userNames[];  // could be null if SVA 1.0 SM
    }

    /** This struct contains 'identifiers' for user plus keyword pairs in a
     *  SVA 2.0 sound model
     */
    public static class KeywordInfo
    {
     /** keyword phrase */
        public String  keywordPhrase;

     /** user name */
        public String  activeUsers[];
    }

    /** This struct contains that describe the number of
     *  keywords and users in a VoiceWakeup SoundModel
     */
    public static class KeywordUserCounts
    {
       /** number of keywords VoiceWakeup SoundModel can contain*/
        public short  numKeywords;
       /** number of users VoiceWakeup SoundModel can contain*/
        public short  numUsers;
       /** number of active Keywords plus User+Keyword pairs */
        public short  numUserKWPairs;
    }

    /** This struct is returned to as output parameter from
     *  ListenSoundModel::extend() and ListenSoundModel::createUdkSm().
     */
    public static class ConfidenceData
    {
       /** userMatch  - percent (0-100)
         *        confidence level in user for all recordings is the
         *        same
         */
        public int  userMatch;
    }

    /** This struct is returned as output parameter from
     *  ListenSoundModel::verifyUserRecordingQuaility()
     */
    public static class QualityCheckInfo
    {
        /** Batch EPD output segments */
        public boolean isEpdFilteredSegmentSet;
        /** batch EPD segment has low SNR than SNR threshold */
        public boolean isLowSnrSet;
        /** SNR of EPD segment */
        public float epdSnr;
        /** estimated start index of EPD segment  including guard frame */
        public int epdStart;
        /** estimated end index of EPD segment including guard frame */
        public int epdEnd;
        /** estimated start index of EPD segment without guard frame */
        public int exactEpdStart;
        /** estimated end index of EPD segment without guard frame */
        public int exactEpdEnd;
        /** 1st stage SVA score with given EPD segment  */
        public short keywordConfidenceLevel;
        /** peak level of EPD segment  */
        public float epdPeakLevel;
        /** RMS level of EPD segment */
        public float epdRmsLevel;
        /** number of clipping samples of EPD segment  */
        public int n_epdSamplesClipping;
        /** estimated start index of 1st stage model */
        public int keywordStart;
        /** estimated end index of 1st stage model  */
        public int keywordEnd;
    }
    /** This struct is returned as output parameter from
     *  ListenSoundModel::getSMLVersion()
     */
    public static class SMLVersion {
        /** Sound model Library (SML) version */
        public long version;
    }

       /** This struct is the input to ListenSoundModel::initEPD() */
    public static class ListenEPDParams {
        /** The minimum snr of frame that speech segment starts */
        public float minSnrOnset;
        /** The minimum snr of frame that speech segment ends */
        public float minSnrLeave;
        /** The minimum snr value assumed in the end point detector */
        public float snrFloor;
        /** The minimum snr value of speech to verify */
        public float snrThresholds;
        /** The forgetting factor used for noise estimation */
        public float forgettingFactorNoise;
        /**
         * the number of frames in the beginning that are used
         * for noise estimate(valid only for online mode)
         */
        public int numFrameTransientFrame;
        /**
         * the number of frame are used for noise estimation equals
         * to minenergyFrameRatio * number of input frames
         * (valid only for batch mode)
         */
        public float minEnergyFrameRatio;
        public float minNoiseEnergy;
        /** the minimum nubmer of samples for a speech phrase (targetted speech) */
        public int numMinFramesInPhrase;
        /** the minimum number of samples for a speech interval */
        public int numMinFramesInSpeech;
        /** the maximum allowable number of samples for a speech gap */
        public int numMaxFrameInSpeechGap;
        /** the speech head */
        public int numFramesInHead;
        /** the speech tail */
        public int numFramesInTail;
        /** feature */
        public int preEmphasize;
        /** corner case protection */
        public int numMaxFrames;
        /** for 1st stage EPD */
        public int keyword_threshold;
        /* Do we even need this thing */
        /* TODO: RESOLVE BEFORE RELEASE */
        // public int[] reserved[4];
    }

    /** This struct is the ouput result from ListenSoundModel::processEPD() */
    public static class EPDResult {
        /**
         * Value 1 indicates keyword end is detected,
         * Value 0 indicates keyword is not detected.
         */
        public int is_detected;
        /**
         * keyword start position in the order of 10ms
         * from the beginning of input recording samples
         */
        public int start_index;
        /**
         * keyword end position in the order of 10ms
         * from the beginning of input recording samples
         */
        public int end_index;
        /**
         * SNR with given realtime audio input computed
         * with estimated start-end voice activity indices
         */
        public float snr;
    }

    /**
     * This struct contains the opaque handle that needs to be
     * used by the application layer during EPD operation
     */
    public static class EPDHandle {
        /** Opaque buffer that stores the EPD state information */
        public ByteBuffer epd_data;
    }

    public static class SoundModelHeader {
        int  numKeywords;               /* total number of keywords  */
        int  numUsers;                  /* total number of users  */
        int  numActiveUserKeywordPairs; /* total number of active user+keyword pairs in SM */
        boolean isStripped;             /* if corresponding keyword is stripped or not */
        int  langPerKw[];               /* Language code of each keyword */
        int  numUsersSetPerKw[];        /* number active Users per keyword - included as convenience */
        boolean isUserDefinedKeyword[];
        /* Ordered 'truth' table of all possible pairs of users for each keyword.
        * Active entries marked with 1, inactive 0.keywordPhrase
        * 16-bit short (rather than boolean) is used to match SM model data size */
        int  userKeywordPairFlags[][];
        int  model_indicator;           /* for SM 3.0, indicate which models were combined */
    }

    public static class RawData {
        byte[] data;
        int size;
    }

    /** This struct is returned as output parameter from
     *  ListenSoundModel::modelComptibleCheck()
     */
    public static class SMLMatchResult {
        public int fSvaMatchResult;
        public int fSvaInternalMatchResult;
        public int sPdkMatchResult;
        public int sUdkMatchResult;
        public int sRnnMatchResult;
        public int sUvMatchResult;
        public int sUvEaiMatchResult;
    }

    /** This struct is to convert LSM Server
     *  return status to LSM client return status.
     */
    public static class LSMServerStatus {
        public static final int kSucess = 0;
        public static final int kFailed = 1;
        public static final int kBadParam = 2;
        public static final int kKeywordNotFound = 3;
        public static final int kUserNotFound = 4;
        public static final int kUserKwPairNotActive = 5;
        public static final int kSMVersionUnsupported = 6;
        public static final int kUserDataForKwAlreadyPresent = 7;
        public static final int kDuplicateKeyword = 8;
        public static final int kDuplicateUserKeywordPair = 9;
        public static final int kMaxKeywordsExceeded = 10;
        public static final int kMaxUsersExceeded = 11;
        public static final int kEventStructUnsupported = 12;
        public static final int kLastKeyword = 13;
        public static final int kNoSignal = 14;
        public static final int kLowSnr = 15;
        public static final int kRecordingTooShort = 16;
        public static final int kRecordingTooLong = 17;
        public static final int kNeedRetrain = 18;
        public static final int kUserUDKPairNotRemoved = 19;
        public static final int kCannotCreateUserUDK = 20;
        public static final int kOutputArrayTooSmall = 21;
        public static final int kTooManyAbnormalUserScores = 22;
        public static final int kWrongModel = 23;
        public static final int kWrongModelAndIndicator = 24;
        public static final int kDuplicateModel = 25;
        public static final int kChoppedSample = 26;
        public static final int kSecondStageKeywordNotFound = 27;
        public static final int kClippedSample = 28;
        public static final int kBadRecordingQualitiy = 29;
    }
}
