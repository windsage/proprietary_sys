/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voiceassistant.reference.views;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.quicinc.voiceassistant.reference.R;
import com.quicinc.voiceassistant.reference.util.AppPermissionUtils;

public class ASRFragment extends Fragment {
    private static final String TAG = ASRFragment.class.getSimpleName();
    private TextView mEditText;
    private ViewGroup mAnimationLayout;
    private View mASRButton;
    private ImageView mASRListeningImage;
    private TextView mASRListeningText;
    private View mClearAllButton;
    private boolean isListening = false;
    private ASRViewModel mASRViewModel;
    private ObjectAnimator mScaleAnimator = null;

    private final ActivityResultLauncher<String> mRequestPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    new ActivityResultCallback<Boolean>() {
                        @Override
                        public void onActivityResult(Boolean result) {
                            if(!result) {
                                Toast.makeText(getActivity(), "No RECORD_AUDIO Permission",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                mASRViewModel.setMicPermissionGranted(true);
                            }
                        }
                    });

    public ASRFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_asr, container, false);
        mEditText = root.findViewById(R.id.text_input);
        mAnimationLayout = root.findViewById(R.id.push_to_talk_animation_layout);
        mASRButton = root.findViewById(R.id.push_to_talk);
        mASRListeningImage = root.findViewById(R.id.push_to_talk_icon);
        mASRListeningText = root.findViewById(R.id.push_to_talk_text);
        mClearAllButton = root.findViewById(R.id.clear_layout);
        mClearAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mEditText.setText("");
            }
        });

        mASRButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "mASRButton clicked, isListening=" + isListening);
                if(AppPermissionUtils.requestRuntimePermissions(getActivity()))  {
                    return;
                }
                if(isMicPermissionGranted()) {
                    isListening = !isListening;
                    updateASRListeningState(isListening);
                    mASRViewModel.setASRListening(isListening);
                } else {
                    requestMicPermission();
                }
            }
        });
        updateASRListeningState(false);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mASRViewModel = new ViewModelProvider(requireActivity()).get(ASRViewModel.class);
        mASRViewModel.getASRListening().observe(requireActivity(),
                this::updateASRListeningState);
        mASRViewModel.getASRResult().observe(requireActivity(), text -> mEditText.setText(text));
    }

    private boolean isMicPermissionGranted() {
        return Boolean.TRUE.equals(mASRViewModel.getIsMicPermissionGranted().getValue());
    }

    private void requestMicPermission() {
        mRequestPermission.launch(Manifest.permission.RECORD_AUDIO);
    }

    private void setASRButtonEnabled(boolean enabled) {
        mASRButton.setEnabled(enabled);
        int backgroundId = enabled ? R.drawable.asr_tts_active : R.drawable.asr_tts_deactive;
        mASRButton.setBackground(getResources().getDrawable(backgroundId, null));
    }

    private void updateASRListeningState(boolean listening) {
        isListening = listening;
        int textId = listening? R.string.voice_input_stop_text: R.string.voice_input_start_text;
        int iconId = listening? R.drawable.icn_stop_circle: R.drawable.icn_voice_input_mic;
        int backgroundId= listening? R.drawable.asr_tts_deactive: R.drawable.asr_tts_active;
        mASRButton.setBackground(getResources().getDrawable(backgroundId, null));
        mASRListeningText.setText(textId);
        mASRListeningImage.setImageDrawable(getResources().getDrawable(iconId, null));

        if(listening) {
            startPushToTalkAnimation();
        } else {
            stopPushToTalkAnimation();
        }
    }

    private void startPushToTalkAnimation() {
        mAnimationLayout.setVisibility(View.VISIBLE);
        ImageView wave = mAnimationLayout.findViewById(R.id.sound_wave);
        if (mScaleAnimator == null) {
            mScaleAnimator = ObjectAnimator.ofFloat(wave, "scaleY", 1.0f,0.7f,1.0f);
            mScaleAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            mScaleAnimator.setDuration(2000);
            mScaleAnimator.setRepeatCount(ValueAnimator.INFINITE);
        } else {
            mScaleAnimator.cancel();
        }
        mScaleAnimator.start();
        mClearAllButton.setVisibility(View.INVISIBLE);
    }

    private void stopPushToTalkAnimation(){
        ImageView wave = mAnimationLayout.findViewById(R.id.sound_wave);
        wave.clearAnimation();
        mAnimationLayout.setVisibility(View.INVISIBLE);
        wave.setScaleY(1.0f);
        mClearAllButton.setVisibility(View.VISIBLE);
        if (mScaleAnimator != null) {
            mScaleAnimator.end();
            mScaleAnimator = null;
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume ");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause ");
        KeyguardManager keyguardManager = (KeyguardManager) getActivity().getSystemService(
                Context.KEYGUARD_SERVICE);
        if (!keyguardManager.isKeyguardLocked()) {
            mASRViewModel.setASRListening(false);
        }
    }
}