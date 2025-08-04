/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voiceassistant.reference.views;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.quicinc.voiceassistant.reference.R;
import com.quicinc.voiceassistant.reference.util.LogUtils;

public class EnrollmentViewHolder {
    private static final String TAG = "Enrollment";

    public interface IEnrollmentListener {
        void onStartUtteranceTraining();

        void onGenerateSoundModel();

        void onGenerateFinish();
    }

    private IEnrollmentListener mEnrollmentListener;
    private final View mRootView;
    private View mInstructionsViewGroup;
    private View mEnrollmentStepsViewGroup;
    private View mEnrollFinishGroup;
    private VoiceEffector mVoiceEffector = null;
    private ImageView[] mStepImage;
    private TextView mPhrase;
    private ImageView mMicButton;
    private TextView mEnrollListeningText;
    private TextView mEnrollStepText;
    private int mCurrentStep = 0;
    private final static int MAX_STEP = 5;
    private final Context mContext;

    public EnrollmentViewHolder(Context context, View rootView) {
        mContext = context;
        mRootView = rootView;
        initViews();
    }

    private void initViews() {
        mInstructionsViewGroup = mRootView.findViewById(R.id.fragment_enrollment_instructions);
        mEnrollStepText = mRootView.findViewById(R.id.enroll_steps_title);
        mEnrollListeningText = mRootView.findViewById(R.id.enroll_listening_text);
        ImageView nextBtn = mInstructionsViewGroup.findViewById(R.id.nextButton);
        nextBtn.setOnClickListener(v -> {
            if (mEnrollmentListener != null) {
                mEnrollmentListener.onStartUtteranceTraining();
                mEnrollListeningText.setText(R.string.enroll_listening);
            }
            mInstructionsViewGroup.setVisibility(View.INVISIBLE);
            mEnrollmentStepsViewGroup.setVisibility(View.VISIBLE);
        });

        mEnrollmentStepsViewGroup = mRootView.findViewById(R.id.fragment_enrollment_steps);
        mEnrollmentStepsViewGroup.setVisibility(View.INVISIBLE);
        mEnrollFinishGroup = mRootView.findViewById(R.id.fragment_enrollment_finish);
        ImageView backSettings = mEnrollFinishGroup.findViewById(R.id.enroll_finish_button);
        backSettings.setOnClickListener(view -> mEnrollmentListener.onGenerateFinish());
        mEnrollFinishGroup.setVisibility(View.INVISIBLE);

        // Visualizer
        View effectView = mEnrollmentStepsViewGroup.findViewById(R.id.bg_layer);
        mVoiceEffector = new VoiceEffector(effectView, 1.6f);
        mStepImage = new ImageView[5];

        mStepImage[0] = mEnrollmentStepsViewGroup.findViewById(R.id.step1);
        mStepImage[1] = mEnrollmentStepsViewGroup.findViewById(R.id.step2);
        mStepImage[2] = mEnrollmentStepsViewGroup.findViewById(R.id.step3);
        mStepImage[3] = mEnrollmentStepsViewGroup.findViewById(R.id.step4);
        mStepImage[4] = mEnrollmentStepsViewGroup.findViewById(R.id.step5);

        mPhrase = mEnrollmentStepsViewGroup.findViewById(R.id.phraseText);
        mMicButton = mEnrollmentStepsViewGroup.findViewById(R.id.mic_effect);
    }

    public void setEnrollmentListener(IEnrollmentListener enrollmentListener) {
        mEnrollmentListener = enrollmentListener;
    }

    public void updateUtteranceState(boolean isRecording) {
        if (isRecording) {
            mEnrollStepText.setText(
                    String.format(mContext.getResources().getString(R.string.enroll_steps_title),
                            mCurrentStep + 1));
            mEnrollListeningText.setText(R.string.listening);
        }
        setRecordingUiState(isRecording);
    }

    public void upToNextStep() {
        onEnrollmentStep(mCurrentStep);
    }

    public void enrollFail() {
        mEnrollListeningText.setText(R.string.enroll_listening_failed);
    }

    public int getCurrentStep() {
        return mCurrentStep;
    }

    public void onEnrollCompleted() {
        mMicButton.setEnabled(false);
        mEnrollmentStepsViewGroup.setVisibility(View.INVISIBLE);
        mEnrollFinishGroup.setVisibility(View.VISIBLE);
    }


    private void setRecordingUiState(boolean isRecording) {
        if (isRecording) {
            mVoiceEffector.start();
        } else {
            mVoiceEffector.stop();
        }
    }

    public void updateVoiceEffect(float energy) {
        mVoiceEffector.update(energy);
    }

    public void resetEnrollmentState() {
        mInstructionsViewGroup.setVisibility(View.VISIBLE);
        mEnrollFinishGroup.setVisibility(View.GONE);
        mEnrollmentStepsViewGroup.setVisibility(View.GONE);
        mStepImage[0].setImageResource(R.drawable.enrolling_status);
        mStepImage[1].setImageResource(R.drawable.enroll_not_start);
        mStepImage[2].setImageResource(R.drawable.enroll_not_start);
        mStepImage[3].setImageResource(R.drawable.enroll_not_start);
        mStepImage[4].setImageResource(R.drawable.enroll_not_start);
        mCurrentStep = 0;
        mPhrase.setText(R.string.harvard1);
        mMicButton.setEnabled(true);
        setRecordingUiState(false);
    }

    private void onEnrollmentStep(int step) {
        LogUtils.d(TAG, "onEnrollmentStep() " + step);
        if (step >= 0 && step < mStepImage.length) {
            mStepImage[step].setImageResource(R.drawable.enroll_success);
        }
        switch (step) {
            case 0:
                mStepImage[step + 1].setImageResource(R.drawable.enrolling_status);
                mPhrase.setText(R.string.harvard2);
                break;
            case 1:
                mStepImage[step + 1].setImageResource(R.drawable.enrolling_status);
                mPhrase.setText(R.string.harvard3);
                break;
            case 2:
                mStepImage[step + 1].setImageResource(R.drawable.enrolling_status);
                mPhrase.setText(R.string.harvard4);
                break;
            case 3:
                mStepImage[step + 1].setImageResource(R.drawable.enrolling_status);
                mPhrase.setText(R.string.harvard5);
                break;
            case 4:
                mEnrollmentListener.onGenerateSoundModel();
                break;
            default:
                break;
        }
        mCurrentStep++;
        if (mCurrentStep < MAX_STEP) {
            LogUtils.d(TAG, "Ready for step " + mCurrentStep);
            mEnrollmentListener.onStartUtteranceTraining();
        }
    }
}
