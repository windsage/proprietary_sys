/*
 * Copyright (c) 2021 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.ridemodeaudio;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import com.qualcomm.qti.ridemodeaudio.R;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {

    private final List<Audio> mAudioList;
    public String mUriString;

    private static final String LOG_TAG = "RecyclerAdapter";

    public RecyclerAdapter(List<Audio> audioList) {
        mAudioList = audioList;
    }

    @NonNull
    @Override
    public RecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(LOG_TAG, "onCreateViewHolder");
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item,
                parent, false);
        ViewHolder viewHolder = new ViewHolder(itemView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerAdapter.ViewHolder holder, int position) {
        Audio audio = mAudioList.get(position);
        holder.mAudioName.setText(audio.getName());
        holder.mAudioName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = holder.getAdapterPosition();
                Audio audio = mAudioList.get(position);
                mUriString = audio.getUriString();
                Toast.makeText(view.getContext(), "your selected record name is: " +
                        audio.getName(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return mAudioList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView mAudioName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mAudioName = itemView.findViewById(R.id.audio_name);
        }

    }
}
