/*
 * Copyright (c) 2023 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.quicinc.voiceassistant.reference.views;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.quicinc.voiceassistant.reference.R;

public class TTSFragment extends Fragment {
    private static final String TAG = TTSFragment.class.getSimpleName();
    private EditText mEditText;
    private ViewGroup mAnimationLayout;
    private View mTTSButton;
    private ImageView mTTSPlayingImage;
    private TextView mTTSPlayingText;
    private View mClearAllButton;
    private TTSViewModel mTTSViewModel;
    private ObjectAnimator mScaleAnimator = null;

    public TTSFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate ");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_tts, container, false);
        mEditText = root.findViewById(R.id.text_input);
        mAnimationLayout = root.findViewById(R.id.tts_animation_layout);
        mTTSButton = root.findViewById(R.id.tts_button_layout);
        mTTSPlayingImage = root.findViewById(R.id.tts_button_icon);
        mTTSPlayingText = root.findViewById(R.id.tts_button_text);
        mClearAllButton = root.findViewById(R.id.clear_layout);
        mClearAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mEditText.setText("");
            }
        });
        mTTSButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (TextUtils.isEmpty(mEditText.getText())) return;
                boolean isPlaying = Boolean.TRUE.equals(mTTSViewModel.getTTSPlaying().getValue());
                Log.d(TAG, "mTTSButton clicked, isPlaying=" + isPlaying);
                isPlaying = !isPlaying;
                updateTTSPlayingState(isPlaying);
                mTTSViewModel.setTextPendingToSpeech(String.valueOf(mEditText.getText()));
                mTTSViewModel.setTTSPlaying(isPlaying);
            }
        });

        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                setTTSButtonEnabled(!TextUtils.isEmpty(mEditText.getText()));
            }
        });

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mTTSViewModel = new ViewModelProvider(requireActivity()).get(TTSViewModel.class);
        mTTSViewModel.getTTSPlaying().observe(requireActivity(), this::updateTTSPlayingState);
        mTTSViewModel.getTextPendingToSpeech().observe(requireActivity(), text -> {
            mEditText.setText(text);
        });
        updateTTSPlayingState(false);
        setTTSButtonEnabled(false);
    }

    private void updateTTSPlayingState(boolean playing) {
        Log.d(TAG, "updateTTSPlayingState: " + playing);
        int textId = playing ? R.string.tts_stop : R.string.tts_start;
        int drawableId = playing ? R.drawable.icn_stop_circle : R.drawable.icn_tts_playing;
        int backgroundId = playing ? R.drawable.asr_tts_deactive : R.drawable.asr_tts_active;
        mTTSPlayingText.setText(textId);
        mTTSPlayingImage.setImageDrawable(getResources().getDrawable(drawableId, null));
        mTTSButton.setBackground(getResources().getDrawable(backgroundId, null));

        if (playing) {
            startTTSAnimation();
        } else {
            stopTTSAnimation();
        }
    }

    private void startTTSAnimation() {
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

    private void stopTTSAnimation() {
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

    private void setTTSButtonEnabled(boolean enabled) {
        Log.d(TAG, "setTTSButtonEnabled: " + enabled);
        int backgroundId = enabled ? R.drawable.asr_tts_active : R.drawable.asr_tts_deactive;
        mTTSButton.setBackground(getResources().getDrawable(backgroundId, null));
        mTTSPlayingText.setEnabled(enabled);
        mTTSPlayingImage.setEnabled(enabled);
    }

    @Override
    public void onResume() {
        super.onResume();
        setTTSButtonEnabled(!TextUtils.isEmpty(mEditText.getText()));
        Log.d(TAG, "onResume ");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause ");
        mTTSViewModel.setTTSPlaying(false);
    }

}