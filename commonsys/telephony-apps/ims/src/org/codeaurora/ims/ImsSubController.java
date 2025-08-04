/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package org.codeaurora.ims;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Looper;
import android.os.Handler;
import android.os.Message;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import androidx.annotation.VisibleForTesting;
import com.qualcomm.ims.utils.Log;
import org.codeaurora.telephony.utils.AsyncResult;
import org.codeaurora.telephony.utils.Preconditions;
import java.lang.Boolean;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.HashMap;

/**
 * The responsibility of this class to is to control which subscription
 * can support IMS for 7+7 Reduced scope architectures.
 * The inputs to this class are
 *    1) PolicyManager Decision - 7 + 7 mode Reduced scope
 * The outputs from this class are
 *    1) Which subsciption(s) IMS is enabled on
 */
public class ImsSubController {
    private final Context mContext;
    private List<ImsStateListener> mListeners = new CopyOnWriteArrayList<>();
    private List<ImsStackConfigListener> mStackConfigListeners = new CopyOnWriteArrayList<>();
    private List<OnMultiSimConfigChanged> mOnMultiSimConfigChangedListeners =
            new CopyOnWriteArrayList<>();
    private Handler mHandler;
    private static final int EVENT_SUB_CONFIG_CHANGED = 1;
    private static final int EVENT_GET_SUB_CONFIG = 2;
    private static final int EVENT_IMS_SERVICE_UP = 3;
    private static final int EVENT_IMS_SERVICE_DOWN = 4;
    private static final int EVENT_RADIO_AVAILABLE = 5;
    private static final int EVENT_RADIO_NOT_AVAILABLE = 6;
    private static final int EVENT_MSIM_VOICE_CAPABILITY_CHANGED = 7;
    private static final int EVENT_QUERY_MSIM_VOICE_CAPABILITY = 8;

    private static final int INVALID_PHONE_ID = -1;
    private static final int DEFAULT_PHONE_ID = 0;
    private List<ImsSenderRxr> mSenderRxrs;
    private List<ImsServiceSub> mServiceSubs;
    private int mModemSimultStackCount = 0; // Both SUBs have the same stack count.
    // Store modem IMS stack status per slot even both are the same to avoid any race issue.
    private HashMap<Integer, List<Boolean>> mModemStackStatus = new HashMap<>();
    // Modem provides stack status for 6 slots.
    // NOTE: Currently, we expect only 2 stacks to be active.
    private final static int MAX_VALID_STACK_STATUS_COUNT = 6;
    private boolean mActiveStacks[] = new boolean[MAX_VALID_STACK_STATUS_COUNT];
    /*
     * DSDV (Dual Sim Dual Volte) i.e. 7+7
     * TRUE -- Dsdv is supported
     * FALSE -- Dsdv is not supported and can be inferred when
     *          lower layers inform that sub config request is
     *          not supported via ImsErrorCode.REQUEST_NOT_SUPPORTED
     */
    private boolean mIsDsdv = true;
    private TelephonyManager mTm = null;

    private static final String ACTION_MSIM_VOICE_CAPABILITY =
            "org.codeaurora.intent.action.MSIM_VOICE_CAPABILITY";
    private static final String PERMISSION_MSIM_VOICE_CAPABILITY =
            "com.qti.permission.RECEIVE_MSIM_VOICE_CAPABILITY";
    private static final String EXTRAS_MSIM_VOICE_CAPABILITY = "MsimVoiceCapability";
    private static final String EXTRAS_DSDS_TRANSITION_SUPPORTED = "DsdsTransitionSupported";
    private boolean mIsDsdsTransitionFeatureSupported = false;

    public interface ImsStateListener {
        public void onActivateIms(int phoneId);
        public void onDeactivateIms(int phoneId);
    }

    public interface OnMultiSimConfigChanged {
        /**
         * Method that reports multi-sim configuration change
         * @param prevModemCount int value representing the previous number of active modem(s)
         * @param activeSimCount int value representing the current number of active modem(s)
         */
        public void onMultiSimConfigChanged(int prevModemCount, int activeModemCount);
    }

    public interface ImsStackConfigListener {
        /**
         * Method that reports the active/inactive status of each
         * IMS-capable stack.
         * @param activeStacks Array corresponding to IMS stacks (subscriptions).
         *        True and False values correspond to active and inactive respectively.
         * @param phoneId the serviceSub instance id that needs to act on this update
         */
        public void onStackConfigChanged(boolean[] activeStacks, int phoneId);
    }

    /**
     * Registers a stackConfigListener.
     * @param stackConfigListener Listener to be registered.
     * @param phoneId the serviceSub instance id that is registering for stack config updates
     * @see ImsSubController#ImsStackConfigListener
     * @throws IllegalArgumentException Will throw an error if stackConfigListener is null.
     */
    public void registerListener(ImsStackConfigListener stackConfigListener, int phoneId) {
        if (isDisposed()) {
            Log.d(this, "returning as ImsSubController is disposed");
            return;
        }
        if (stackConfigListener == null) {
            throw new IllegalArgumentException("stackConfigListener is null!");
        }
        if (!mStackConfigListeners.contains(stackConfigListener)) {
            mStackConfigListeners.add(stackConfigListener);
        } else {
            Log.w(this, "registerListener :: duplicate stackConfigListener!");
        }
        notifyStackConfigChanged(mActiveStacks, phoneId);
    }

    /**
     * Unregisters a stackConfigListener.
     * @param stackConfigListener Listener to unregister
     * @see ImsSubController#ImsStackConfigListener
     * @throws IllegalArgumentException Will throw an error if listener is null.
     * @return true of listener is removed, false otherwise.
     */
    public boolean unregisterListener(ImsStackConfigListener stackConfigListener) {
        if (isDisposed()) {
            Log.d(this, "returning as ImsSubController is disposed");
            return false;
        }
        if (stackConfigListener == null) {
            throw new IllegalArgumentException("stackConfigListener is null");
        }
        return mStackConfigListeners.remove(stackConfigListener);
    }

    /**
     * Registers a OnMultiSimConfigChanged listener.
     * @param listener Listener to be registered.
     * @see ImsSubController#OnMultiSimConfigChanged
     * @throws IllegalArgumentException Will throw an error if simConfigChangedListener is null.
     */
    public void registerListener(OnMultiSimConfigChanged listener) {
        if (isDisposed()) {
            Log.d(this, "returning as ImsSubController is disposed");
            return;
        }
        if (listener == null) {
            throw new IllegalArgumentException("simConfigChangedListener is null!");
        }
        if (!mOnMultiSimConfigChangedListeners.contains(listener)) {
            mOnMultiSimConfigChangedListeners.add(listener);
        } else {
            Log.w(this, "registerListener :: duplicate OnMultiSimConfigChanged listener!");
        }
    }

    /**
     * Unregisters a OnMultiSimConfigChanged listener.
     * @param listener Listener to unregister
     * @see ImsSubController#OnMultiSimConfigChanged
     * @throws IllegalArgumentException Will throw an error if listener is null.
     * @return true of listener is removed, false otherwise.
     */
    public boolean unregisterListener(OnMultiSimConfigChanged listener) {
        if (isDisposed()) {
            Log.d(this, "returning as ImsSubController is disposed");
            return false;
        }
        if (listener == null) {
            throw new IllegalArgumentException("simConfigChangedListener");
        }
        return mOnMultiSimConfigChangedListeners.remove(listener);
    }

    public ImsSubController(Context context) {
        this(context, Looper.getMainLooper());
    }

    public ImsSubController(Context context, Looper looper) {
        this(context, new CopyOnWriteArrayList<ImsSenderRxr>(),
                new CopyOnWriteArrayList<ImsServiceSub>(), looper);
        int activeModemCount = getActiveModemCount();
        for (int i = 0; i < activeModemCount; i++) {
            createImsSenderRxr(context, i);
            createImsServiceSub(context, i, mSenderRxrs.get(i));
        }
        mIsDsdsTransitionFeatureSupported = mSenderRxrs != null &&
                mSenderRxrs.get(getDefaultPhoneId()).isFeatureSupported(Feature.DSDS_TRANSITION);
    }

    @VisibleForTesting
    public ImsSubController(Context context, List<ImsSenderRxr> senderRxrs,
            List<ImsServiceSub> serviceSubs, Looper looper) {
        mContext = context;
        mTm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        mContext.registerReceiver(mMultiSimConfigChangedReceiver,
                new IntentFilter(TelephonyManager.
                        ACTION_MULTI_SIM_CONFIG_CHANGED));
        mSenderRxrs = senderRxrs;
        mServiceSubs = serviceSubs;
        mHandler = new ImsSubControllerHandler(looper);
    }

    public boolean isMultiSimEnabled() {
        return getActiveModemCount() > 1;
    }

    public int getActiveModemCount() {
        return mTm.getActiveModemCount();
    }

    private void createImsSenderRxr(Context context, int phoneId) {
        ImsSenderRxr senderRxr = new ImsSenderRxr(mContext, phoneId);
        senderRxr.registerForAvailable(mHandler, EVENT_RADIO_AVAILABLE, phoneId);
        senderRxr.registerForNotAvailable(mHandler, EVENT_RADIO_NOT_AVAILABLE, phoneId);
        senderRxr.registerForImsServiceUp(mHandler, EVENT_IMS_SERVICE_UP, phoneId);
        senderRxr.registerForImsServiceDown(mHandler, EVENT_IMS_SERVICE_DOWN, phoneId);
        mSenderRxrs.add(senderRxr);
    }

    private void createImsServiceSub(Context context, int phoneId, ImsSenderRxr senderRxr) {
        ImsServiceSub serviceSub = new ImsServiceSub(context, phoneId, senderRxr, this);
        mServiceSubs.add(serviceSub);
    }

    @VisibleForTesting
    public void setIsDsdv(boolean isDsdv) {
        mIsDsdv = isDsdv;
    }

    @VisibleForTesting
    public Handler getHandler() {
        return mHandler;
    }

    /**
     * Registers listener.
     * @param listener Listener to be registered
     * @see ImsSubController#Listener
     * @throws IllegalArgumentException Will throw an error if listener is null.
     */
    public void registerListener(ImsStateListener listener) {
        if (isDisposed()) {
            Log.d(this, "registerListener, returning as isDisposed");
            return;
        }
        if (listener == null) {
            throw new IllegalArgumentException("listener is null");
        }
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        } else {
            Log.w(this, "Duplicate listener " + listener);
        }
    }

    /**
     * Unregisters listener.
     * @param listener Listener to unregister
     * @see ImsSubContrller#Listener
     * @throws IllegalArgumentException Will throw an error if listener is null.
     * @return true of listener is removed, false otherwise.
     */
    public boolean unregisterListener(ImsStateListener listener) {
        if (isDisposed()) {
            Log.d(this, "unregisterListener, returning as isDisposed");
            return false;
        }
        if (listener == null) {
            throw new IllegalArgumentException("listener is null");
        }
        return mListeners.remove(listener);
    }

    public static int getDefaultPhoneId() {
        return DEFAULT_PHONE_ID;
    }

    public boolean isDsdv() {
        return mIsDsdv;
    }

    private void notifyStackConfigChanged(boolean[] activeStacks, int phoneId) {
        Log.v(this, "notifyStackConfigChanged: activeStacks:" + Arrays.toString(activeStacks)
                + " phoneId: " + phoneId);
        for (ImsStackConfigListener listener : mStackConfigListeners) {
            listener.onStackConfigChanged(activeStacks, phoneId);
        }
    }

    private void notifyOnMultiSimConfigChanged(int prevModemCount, int activeModemCount) {
        Log.v(this, "notifyOnMultiSimConfigChanged: prevModemCount: " + prevModemCount
                + " activeModemCount: " + activeModemCount);
        if (prevModemCount == activeModemCount) {
            return;
        }
        for (OnMultiSimConfigChanged listener : mOnMultiSimConfigChangedListeners) {
            listener.onMultiSimConfigChanged(prevModemCount, activeModemCount);
        }
    }

    private void handleSubConfigException(Throwable exception) {
        Preconditions.checkArgument(exception != null);
        final int errorCode = ((ImsRilException)exception).getErrorCode();
        Log.i(this, "handleSubConfigException error : " + errorCode);
        if (errorCode == ImsErrorCode.REQUEST_NOT_SUPPORTED) {
            handleModemImsStackNotSupported();
        } else {
            Log.w (this, "Unhandled error code : " + errorCode);
        }
    }

    private void handleSubConfigChanged(AsyncResult ar, boolean ignoreStackCount) {
        if (!isMultiSimEnabled()) {
            Log.v(this, "handleSubConfigChanged: Single SIM mode");
            return;
        }
        if (ar.exception != null) {
            handleSubConfigException(ar.exception);
        } else if(ar.result != null) {
            ImsSubConfigDetails config = (ImsSubConfigDetails) ar.result;
            if (!ignoreStackCount) {
                mModemSimultStackCount = config.getSimultStackCount();
            }
            if (ar.userObj == null) {
                Log.e(this, "handleSubConfigChanged ar.userObj is null");
                return;
            }

            List<Boolean> stackStatus = config.getImsStackEnabledList();
            mModemStackStatus.put((int)ar.userObj, stackStatus);
            boolean[] activeStacks = new boolean[MAX_VALID_STACK_STATUS_COUNT];

            for(int i = 0; i < mModemSimultStackCount; ++i) {
                activeStacks[i] = stackStatus.get(i);
            }

            notifyStackConfigChanged(activeStacks, (int)ar.userObj);
        } else {
            Log.e(this, "ar.result and ar.exception are null");
        }
    }

    private void handleMultiSimVoiceCapability(AsyncResult ar) {
        if (ar.exception != null) {
            final int errorCode = ((ImsRilException)ar.exception).getErrorCode();
            Log.e(this, "handleMultiSimVoiceCapability errorCode: " + errorCode);
            return;
        }

        if (ar.result == null) {
            Log.e(this, "handleMultiSimVoiceCapability ar.result is null");
            return;
        }

        broadcastConcurrentCallsIntent((int)ar.result);
    }

    private void broadcastConcurrentCallsIntent(int voiceCapability) {
        Intent intent = new Intent(ACTION_MSIM_VOICE_CAPABILITY);
        intent.putExtra(EXTRAS_MSIM_VOICE_CAPABILITY, voiceCapability);
        Log.d(this, "is dsds transition feature supported: " +
                mIsDsdsTransitionFeatureSupported);
        intent.putExtra(EXTRAS_DSDS_TRANSITION_SUPPORTED,
                mIsDsdsTransitionFeatureSupported);
        mContext.sendBroadcast(intent, PERMISSION_MSIM_VOICE_CAPABILITY);
    }

    private boolean isAidlReorderingSupported(int phoneId) {
        if (mSenderRxrs == null || mSenderRxrs.isEmpty() ||
                mSenderRxrs.get(phoneId) == null) {
            return false;
        }
        return mSenderRxrs.get(phoneId).isAidlReorderingSupported();
    }

    private class ImsSubControllerHandler extends Handler {
        public ImsSubControllerHandler() {
            super();
        }

        public ImsSubControllerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.i(this, "Message received: what = " + msg.what);
            if (isDisposed()) {
                Log.d(this, "handleMessage, returning as isDisposed");
                return;
            }
            AsyncResult ar = (AsyncResult)msg.obj;
            int phoneId = (int)ar.userObj;
            try {
                switch (msg.what) {
                    case EVENT_SUB_CONFIG_CHANGED:
                        Log.i(this, "Received EVENT_SUB_CONFIG_CHANGED phoneId = " + phoneId);
                        if (isAidlReorderingSupported(phoneId)) {
                            // In RIL, the indication relies on the stack count information
                            // received from the EVENT_GET_SUB_CONFIG resp.
                            // The resp information, which includes the stack count
                            // is sent through the indication, so don't ignore the stack count
                            Log.d(this, "Relying on indication is supported, " +
                                    " don't ignore stack count");
                            handleSubConfigChanged(ar, false /* ignoreStackCount */);
                        } else {
                            handleSubConfigChanged(ar, true /* ignoreStackCount */);
                        }
                        break;
                    case EVENT_GET_SUB_CONFIG:
                        if (isAidlReorderingSupported(phoneId)) {
                            Log.d(this, "Received EVENT_GET_SUB_CONFIG phoneId = " + phoneId +
                                    "ignoring as relying on indication is supported");
                            break;
                        }
                        Log.i(this, "Received EVENT_GET_SUB_CONFIG phoneId = " + phoneId);
                        handleSubConfigChanged(ar, false /* ignoreStackCount */);
                        break;
                    case EVENT_IMS_SERVICE_UP:
                        Log.i(this, "Received EVENT_IMS_SERVICE_UP phoneId = " + phoneId);
                        registerForRadioEvents(phoneId);
                        break;
                    case EVENT_IMS_SERVICE_DOWN:
                        Log.i(this, "Received EVENT_IMS_SERVICE_DOWN phoneId = " + phoneId);
                        deRegisterFromRadioEvents(phoneId);
                        updateStackConfig(phoneId, false);
                        break;

                    case EVENT_RADIO_NOT_AVAILABLE:
                        Log.i(this, "Received EVENT_RADIO_NOT_AVAILABLE phoneId = " + phoneId);
                        updateStackConfig(phoneId, false);
                        break;

                    case EVENT_RADIO_AVAILABLE:
                        Log.i(this, "Received EVENT_RADIO_AVAILABLE phoneId = " + phoneId);
                        handleRadioAvailable(phoneId);
                        break;
                    case EVENT_QUERY_MSIM_VOICE_CAPABILITY:
                        if (isAidlReorderingSupported(phoneId)) {
                            Log.d(this, "Received EVENT_QUERY_MSIM_VOICE_CAPABILITY phoneId = " +
                                    phoneId + "ignoring as relying on indication is supported");
                            break;
                        }
                    case EVENT_MSIM_VOICE_CAPABILITY_CHANGED:
                        Log.i(this, "Received multi sim voice capability phoneId = " + phoneId);
                        handleMultiSimVoiceCapability(ar);
                        break;

                    default:
                        Log.w(this, "Unknown message = " + msg.what);
                        break;
                }
            } catch (IndexOutOfBoundsException exc) {
                Log.e(this, "handleMessage :: Invalid phoneId " + phoneId);
            }
        }
    }

    private void handleRadioAvailable(int phoneId) {
        if (maybeInitDefaultSubscriptionStatus()) {
            Log.v(this, "handleRadioAvailable: Single SIM mode direct initialization");
            return;
        }

        // Query stack configuration and multi sim voice capability when radio is available
        // to update the latest status from modem.
        // This is required to handle modem SSR use cases.
        ImsSenderRxr ci = mSenderRxrs.get(phoneId);
        if (ci == null) {
            Log.e(this, "handleRadioAvailable: ImsSenderRxr is null");
            return;
        }

        ci.getImsSubConfig(mHandler.obtainMessage(EVENT_GET_SUB_CONFIG, phoneId));
        if (phoneId == DEFAULT_PHONE_ID) {
            ci.queryMultiSimVoiceCapability(mHandler.obtainMessage(
                    EVENT_QUERY_MSIM_VOICE_CAPABILITY, phoneId));
        }
    }

    private void registerForRadioEvents(int phoneId) {
        if (maybeInitDefaultSubscriptionStatus()) {
            Log.v(this, "registerForRadioEvents: Single SIM mode direct initialization");
            return;
        }

        ImsSenderRxr ci = mSenderRxrs.get(phoneId);
        if (ci == null) {
            Log.e(this, "registerForRadioEvents: ImsSenderRxr is null");
            return;
        }

        final boolean isPrimarySubscription = phoneId == DEFAULT_PHONE_ID;
        final boolean isRadioAvailable = ci.getRadioState() != null &&
                ci.getRadioState().isAvailable();
        ci.registerForImsSubConfigChanged(mHandler, EVENT_SUB_CONFIG_CHANGED, phoneId);
        if (isRadioAvailable) {
            ci.getImsSubConfig(mHandler.obtainMessage(EVENT_GET_SUB_CONFIG, phoneId));
        }

        // Some events will be triggered on primary subscription only. Register for those events if
        // this is primary subscription.
        if (!isPrimarySubscription ) {
            Log.v(this, "registerForRadioEvents: phoneId: " + phoneId +
                    " is not primary subscription.");
            return;
        }

        ci.registerForMultiSimVoiceCapabilityChanged(mHandler, EVENT_MSIM_VOICE_CAPABILITY_CHANGED,
                phoneId);
        if (isRadioAvailable) {
            ci.queryMultiSimVoiceCapability(mHandler.obtainMessage(
                    EVENT_QUERY_MSIM_VOICE_CAPABILITY, phoneId));
        }
    }

    private void deRegisterFromRadioEvents(int phoneId) {
        if (mTm == null || mTm.getActiveModemCount() <= 1) {
            Log.v(this, "deRegisterFromRadioEvents: Single SIM mode");
            return;
        }

        ImsSenderRxr ci = mSenderRxrs.get(phoneId);
        if (ci == null) {
            Log.e(this, "deRegisterFromRadioEvents: ImsSenderRxr is null");
            return;
        }

        ci.unregisterForImsSubConfigChanged(mHandler);

        // Some events will be triggered on primary subscription only. Deregister for those events
        // if this is deafult subscription.
        final boolean isPrimarySubscription = phoneId == DEFAULT_PHONE_ID;
        if (!isPrimarySubscription ) {
            Log.v(this, "deRegisterFromRadioEvents: phoneId: " + phoneId +
                    " is not primary subscription.");
            return;
        }

        ci.unregisterForMultiSimVoiceCapabilityChanged(mHandler);
    }

    private void updateStackConfig(int phoneId, boolean isEnabled) {
        boolean[] activeStacks;
        List<Boolean> stackStatus = mModemStackStatus.get(phoneId);
        Log.v(this, "updateStackConfig phoneId: " + phoneId + " isEnabled: " + isEnabled
                 + " mIsDsdv : " + mIsDsdv);

        if (mIsDsdv) {
            if (stackStatus == null) {
                Log.w(this, "updateStackConfig Stacks are not yet initialized");
                return;
            }

            if (stackStatus.get(phoneId) == isEnabled) {
                Log.w(this, "updateStackConfig nothing to update");
                return;
            }

            activeStacks = new boolean[MAX_VALID_STACK_STATUS_COUNT];
            stackStatus.set(phoneId, isEnabled);
            for(int i = 0; i < mModemSimultStackCount; ++i) {
                activeStacks[i] = stackStatus.get(i);
            }
        } else {
            if (mActiveStacks[phoneId] == isEnabled) {
                Log.w(this, "updateStackConfig nothing to update");
                return;
            }

            mActiveStacks[phoneId] = isEnabled;
            activeStacks = mActiveStacks;
        }
        notifyStackConfigChanged(activeStacks, phoneId);
    }

    /* Method to initialize IMS stack for default Phone */
    private boolean maybeInitDefaultSubscriptionStatus() {
        mModemSimultStackCount = 0;
        mModemStackStatus.clear();
        if (mTm == null || mTm.getActiveModemCount() <= 1) {
            /* If not multi-sim, a change in socket communication is not required */
            Log.i(this, "maybeInitDefaultSubscriptionStatus: Not multi-sim.");
            mIsDsdv = false;
            updateActiveImsStackForPhoneId(DEFAULT_PHONE_ID);
            return true;
        }

        return false;
    }

    private void handleModemImsStackNotSupported() {
        Log.i(this, "handleModemImsStackNotSupported");
        mModemSimultStackCount = 0;
        mModemStackStatus.clear();
        mIsDsdv = false;
        // Now without cross mapping supported, assume default phone has IMS stack support only.
        // This is used to address an exception case where lower layer has a problem where the
        // unexpected value is sent to Telephony, so with this handling, make sure at least
        // default phone has IMS stack brought up.
        updateActiveImsStackForPhoneId(DEFAULT_PHONE_ID);
    }

    /* Method to re-initialize the Ims Phone instances.
       This API will only be used for single IMS stack. */
    private void updateActiveImsStackForPhoneId(int phoneId) {
        if (isDisposed()) {
            Log.d(this, "updateActiveImsStackForPhoneId return as ImsSubController is disposed");
            return;
        }

        if (phoneId == SubscriptionManager.INVALID_SIM_SLOT_INDEX) {
            Log.e(this, "switchImsPhone: Invalid phoneId: " + phoneId);
            return;
        }

        for (int i = 0; i < mActiveStacks.length; ++i) {
            if (i == phoneId) {
                mActiveStacks[i] = true;
            } else {
                mActiveStacks[i] = false;
            }
        }

        notifyStackConfigChanged(mActiveStacks, phoneId);
    }

    private boolean isDisposed() {
        return mHandler == null;
    }

    public List<ImsServiceSub> getServiceSubs() {
        return mServiceSubs;
    }

    public ImsServiceSub getServiceSub(int phoneId) {
        if (phoneId > INVALID_PHONE_ID && phoneId < mServiceSubs.size()) {
            return mServiceSubs.get(phoneId);
        }
        return null;
    }

    private BroadcastReceiver mMultiSimConfigChangedReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (isDisposed()) {
                Log.d(this, "onReceive, returning as isDisposed");
                return;
            }
            String action = intent.getAction();
            if (action.equals(TelephonyManager.ACTION_MULTI_SIM_CONFIG_CHANGED)) {
                int activeModemCount = intent.getIntExtra(
                        TelephonyManager.EXTRA_ACTIVE_SIM_SUPPORTED_COUNT, 1);
                handleOnMultiSimConfigChanged(activeModemCount);
            }
        }
    };

    // Helper function that handles the multi sim configuration change
    private void handleOnMultiSimConfigChanged(int activeModemCount) {
        if (isDisposed()) {
            Log.d(this, "handleOnMultiSimConfigChanged: already disposed.Ignore.");
            return;
        }
        int prevModemCount = mServiceSubs.size();
        if (activeModemCount == prevModemCount) {
            Log.d(this, "The number of slots is equal to the current size, nothing to do");
            return;
        }
        if (activeModemCount > prevModemCount) {
            switchToMultiSim(prevModemCount, activeModemCount);
        } else {
            switchToSingleSim(prevModemCount, activeModemCount);
            broadcastConcurrentCallsIntent(MultiSimVoiceCapability.UNKNOWN);
        }
        notifyOnMultiSimConfigChanged(prevModemCount, activeModemCount);
    }

    // Helper function to handle the transition to single sim
    private void switchToSingleSim(int prevModemCount, int activeModemCount) {
        for (int i = prevModemCount - 1; i >= activeModemCount; --i) {
            mServiceSubs.get(i).dispose();
            mServiceSubs.remove(i);
            disposeImsSenderRxr(i);
        }
    }

    private void disposeImsSenderRxr(int phoneId) {
        if (mSenderRxrs == null || phoneId < 0 || phoneId >= mSenderRxrs.size()) {
            Log.w(this, "disposeImsSenderRxr: cannot find instance to dispose");
            return;
        }
        Log.i(this, "disposeImsSenderRxr: phoneId - " + phoneId);
        ImsSenderRxr senderRxr = mSenderRxrs.get(phoneId);
        senderRxr.dispose();
        mSenderRxrs.remove(phoneId);
    }

    // Helper function to handle the transition to multi sim
    private void switchToMultiSim(int prevModemCount, int activeModemCount) {
        for (int i = prevModemCount; i < activeModemCount; ++i) {
            createImsSenderRxr(mContext, i);
            createImsServiceSub(mContext, i, mSenderRxrs.get(i));
        }
    }

    public void dispose() {
        if (isDisposed()) {
            Log.d(this, "dispose: returning as already disposed");
            return;
        }
        Log.d(this, "dispose ImsSubController, unregistering handler and listeners");
        mContext.unregisterReceiver(mMultiSimConfigChangedReceiver);
        for(ImsServiceSub sub : mServiceSubs) {
            sub.dispose();
        }
        for(ImsSenderRxr senderRxr : mSenderRxrs) {
            senderRxr.unregisterForAvailable(mHandler);
            senderRxr.unregisterForNotAvailable(mHandler);
            senderRxr.unregisterForImsServiceUp(mHandler);
            senderRxr.unregisterForImsServiceDown(mHandler);
            senderRxr.unregisterForImsSubConfigChanged(mHandler);
            senderRxr.unregisterForMultiSimVoiceCapabilityChanged(mHandler);
        }
        mTm = null;
        mHandler = null;
        mServiceSubs.clear();
        mServiceSubs = null;
        mSenderRxrs.clear();
        mSenderRxrs = null;
        mStackConfigListeners.clear();
        mStackConfigListeners = null;
        mOnMultiSimConfigChangedListeners.clear();
        mOnMultiSimConfigChangedListeners = null;
        mListeners.clear();
        mListeners = null;
        mModemSimultStackCount = 0;
        mModemStackStatus.clear();
    }
}
