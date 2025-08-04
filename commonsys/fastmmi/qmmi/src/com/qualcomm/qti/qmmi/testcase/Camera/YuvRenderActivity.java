/*
 * Copyright (c) 2022, Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 */

package com.qualcomm.qti.qmmi.testcase.Camera;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.annotation.Nullable;
import android.content.Context;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.qualcomm.qti.qmmi.R;

public class YuvRenderActivity extends Activity {

    private static final String TAG = "QMMI_YuvRenderActivity";

    private final TaskRunner mTaskRunner = new TaskRunner();

    private ImageView mImageView;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        LinearLayout linearLayout= new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));

        mImageView = new ImageView(this);

        mImageView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        linearLayout.addView(mImageView);
        setContentView(linearLayout);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        Intent intent = getIntent();
        if (intent == null) {
            Log.d(TAG, "intent is null");
            finish();
            return;
        }
        handleIntent(intent);

    }

    private void handleIntent(Intent intent) {

        String yuvFilePath = intent.getStringExtra("YUV_FILE_PATH");
        if (yuvFilePath == null) {
            Log.d(TAG, "yuv file path is null");
            Toast.makeText(this, R.string.yuv_path_missing, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "yuvFilePath " + yuvFilePath);

        mTaskRunner.executeAsync(new MyWorker(yuvFilePath), result -> {
            if (YuvRenderActivity.this.isDestroyed() || YuvRenderActivity.this.isFinishing()) {
                return;
            }
            mImageView.setImageBitmap(result);
        });
    }


    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }


    private static class TaskRunner {

        private static final String TAG = "CAM_TaskRunner";

        private final Executor executor = Executors.newSingleThreadExecutor();
        private final Handler handler = new Handler(Looper.getMainLooper());

        public interface Callback<R> {
            void onComplete(R result);
        }

        public <R> void executeAsync(Callable<R> callable, Callback<R> callback) {
            executor.execute(() -> {
                final R result;
                try {
                    result = callable.call();
                    handler.post(() -> {
                        callback.onComplete(result);
                    });
                } catch (Exception e) {
                    Log.w(TAG, "", e.fillInStackTrace());
                }

            });
        }
    }

    private static class MyWorker implements Callable<Bitmap> {

        private final String mYuvFilePath;

        public MyWorker(String yuvFilePath) {
            mYuvFilePath = yuvFilePath;
        }

        @Override
        public Bitmap call() throws Exception {

            File yuvFile = new File(mYuvFilePath);
            if (!yuvFile.exists()) {
                Log.w(TAG, "yuv file is not exist " + mYuvFilePath);
                return null;
            }

            String yuvFileName = yuvFile.getName();
            Log.w(TAG, "yuv file name: " + yuvFileName);

            Pattern pattern = Pattern.compile(".+Res(\\d{1,4})x(\\d{1,4}).+Stride(\\d{1,4}).*");

            Matcher matcher = pattern.matcher(yuvFileName);
            if (matcher.matches()) {
                Log.d(TAG, "w " + matcher.group(1));
                Log.d(TAG, "h " + matcher.group(2));
                Log.d(TAG, "stride " + matcher.group(3));

                try {
                    int width = Integer.parseInt(Objects.requireNonNull(matcher.group(1)));
                    int height = Integer.parseInt(Objects.requireNonNull(matcher.group(2)));
                    int stride = Integer.parseInt(Objects.requireNonNull(matcher.group(3)));

                    Path path = Paths.get(mYuvFilePath);

                    byte[] nv21 = Files.readAllBytes(path);
                    Log.d(TAG, "file length " + nv21.length);

                    int scanline = nv21.length * 2 / 3 / stride;

                    Log.d(TAG, "scanline " + scanline);

                    Bitmap bitmap = convertYUV2Bitmap(nv21, width, height, stride, scanline);

                    return bitmap;
                } catch (Exception e) {
                    Log.w(TAG, "", e.fillInStackTrace());
                }
            } else {
                Log.w(TAG, "yuv file name is not correct " + yuvFileName);
                return null;
            }

            return null;
        }
    }

    private static Bitmap convertYUV2Bitmap(byte[] data, int width, int height, int stride, int scanline) {
        long s = System.currentTimeMillis();
        int W = stride;
        int H = scanline;

        int length = W * H;
        int[] rgb = new int[length];
        int Y = 0;
        int U = 0;
        int V = 0;
        int R = 0;
        int G = 0;
        int B = 0;
        int index = 0;
        for (int i = 0; i < H; i++) {
            for (int j = 0; j < W; j++) {
                Y = data[ i * W + j];
                if (Y < 0) {
                    Y += 255;
                }
                if ((j & 1) == 0) {
                    V = data[(i >> 1) * (W) + j + length];
                    U = data[(i >> 1) * (W) + j + length + 1];
                    if (U < 0) U += 127; else U -= 128;
                    if (V < 0) V += 127; else V -= 128;
                }

                R = Y + V + (V >> 2) + (V >> 3) + (V >> 5);
                G = Y - (U >> 2) - (U >> 4) - (U >> 5) - (V >> 1) - (V >> 3) - (V >> 4) - (V >> 5);
                B = Y + U + (U >> 1) + (U >> 2) + (U >> 6);

                if (R < 0) R = 0; else if (R > 255) R = 255;
                if (G < 0) G = 0; else if (G > 255) G = 255;
                if (B < 0) B = 0; else if (B > 255) B = 255;

                rgb[index++] = 0xff000000 + (R << 16) + (G << 8) + B;
            }
        }
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bmp.setPixels(rgb, 0, W, 0, 0, width, height);
        Log.d(TAG, "convertYUV2Bitmap take time " + (System.currentTimeMillis() - s));
       return bmp;
    }
}
