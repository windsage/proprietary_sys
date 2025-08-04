/*
 * Copyright (c) 2017-2020, Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 */

package com.qualcomm.qti.qmmi.testcase.Camera;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.List;

import com.qualcomm.qti.qmmi.framework.BaseActivity;
import com.qualcomm.qti.qmmi.utils.LogUtils;
import com.qualcomm.qti.qmmi.R;
import com.qualcomm.qti.qmmi.bean.TestCase;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.ContentValues;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.os.SystemProperties;

import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import android.support.v4.content.FileProvider;

public class CameraActivity extends BaseActivity{

    private static final int OPEN_CAMERA_IMAGE_PATH = 1000;

    private static final String CAMERA_BACK_MAIN = "back";
    private static final String CAMERA_FRONT = "front";
    private static final String CAMERA9_YUV = "camera9_yuv";
    private static final String CAMERA10_YUV = "camera10_yuv";

    private static final String EXTRAS_CAMERA_FACING = "android.intent.extras.CAMERA_FACING";
    private static final String CAMERA_PKG_NAME = "org.codeaurora.snapcam";
    private static final String PERMISSIONS_ACTIVITY = "com.android.camera.PermissionsActivity";

    private static final String EXTRAS_CAMERA_YUV_FILE = "YUV_FILE_PATH";
    private static final String EXTRAS_CAMERA9_YUV_PATH = "/sdcard/OmniTest_Stream1_YUV420_Res2460x2460_Dataspace1_UsageFlag3_Stride2496_Cam9_Frame1.yuv";
    private static final String EXTRAS_CAMERA10_YUV_PATH = "/sdcard/OmniTest_Stream1_YUV420_Res2460x2460_Dataspace1_UsageFlag3_Stride2496_Cam10_Frame1.yuv";

    private String mCameraType;
    private Button mOpenBtn;
    private ImageView mSnapShotIV;

    private int mCameraFacing = -1;
    private Uri mImageUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCameraType = mTestCase.getParameter().get("type");

        initView();
        initData();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        LogUtils.logi("onPause");
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        LogUtils.logi("onDestroy");
        super.onDestroy();
    }

    /**
     * Gets the content:// URI from the corresponding path to a file
     * @return content Uri
     */
    private Uri getImageContentUri() {
        LogUtils.logi("getImageContentUri");
        Uri result;
        result = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new ContentValues());
        return result;
    }

    private void initData(){
        switch(mCameraType.toLowerCase()){
            case CAMERA_BACK_MAIN:
                mCameraFacing = 0;
                LogUtils.logi("CAMERA MAIN BACK Case");
                break;
            case CAMERA_FRONT:
                mCameraFacing = 1;
                LogUtils.logi("FRONT CAMERA Case");
                break;
            case CAMERA9_YUV:
                LogUtils.logi("CAMERA9 YUV Case");
                SystemProperties.set("ctl.start", "nativehaltest_service");
                LogUtils.logi("start nativehaltest_service via cam9");
                break;
            case CAMERA10_YUV:
                LogUtils.logi("CAMERA10 YUV Case");
                SystemProperties.set("ctl.start", "nativehaltest_service");
                LogUtils.logi("start nativehaltest_service via cam10");
                break;
            default:
                Toast.makeText(getApplicationContext(), R.string.camera_wrong_type, Toast.LENGTH_SHORT).show();
                mOpenBtn.setClickable(false);
                return;
        }
    }

    private void initView(){
        LogUtils.logi("mCameraType = " + mCameraType);

        mOpenBtn = (Button) findViewById(R.id.btn_open_camera);
        if (mCameraType.equals(CAMERA9_YUV) || mCameraType.equals(CAMERA10_YUV)){
            mOpenBtn.setText(R.string.diplay_yuv);
        }

        mSnapShotIV = (ImageView) findViewById(R.id.iv_snapshot);

        mOpenBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCamera();
            }
        });
    }

    private void openCamera(){
        LogUtils.logi("openCamera");
        if(this.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            this.requestPermissions(new String[] { Manifest.permission.CAMERA }, 1);
        } else {
            LogUtils.logi("mCameraType = " + mCameraType);
            if (!mCameraType.equals(CAMERA9_YUV) && !mCameraType.equals(CAMERA10_YUV)){
                //open camera directly without ImgUri since security exception
                //mImageUri = getImageContentUri();
                //LogUtils.logi("mImageUri = " + mImageUri);
                LogUtils.logi("openMobileCamera");
                ComponentName snapcamCom = new ComponentName(CAMERA_PKG_NAME, PERMISSIONS_ACTIVITY);
                Intent intentPhoto = new Intent("android.media.action.IMAGE_CAPTURE");
                intentPhoto.setComponent(snapcamCom);
                intentPhoto.putExtra(EXTRAS_CAMERA_FACING, mCameraFacing);

                //open camera directly without ImgUri since security exception
                //intentPhoto.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);
                //intentPhoto.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                startActivity(intentPhoto);
            } else {
                LogUtils.logi("openCameraYUV");
                ComponentName snapcamxrCom = new ComponentName(this, YuvRenderActivity.class);
                Intent intentYuv = new Intent();
                intentYuv.setComponent(snapcamxrCom);
                if (mCameraType.equals(CAMERA9_YUV)){
                    LogUtils.logi("openCamera9YUV");
                    intentYuv.putExtra(EXTRAS_CAMERA_YUV_FILE, EXTRAS_CAMERA9_YUV_PATH);
                } else {
                    LogUtils.logi("openCamera10YUV");
                    intentYuv.putExtra(EXTRAS_CAMERA_YUV_FILE, EXTRAS_CAMERA10_YUV_PATH);
                }

                startActivity(intentYuv);
            }

       }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.camera_act;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LogUtils.logi( "onActivityResult: requestCode :" + requestCode + " resultCode:" + resultCode);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == OPEN_CAMERA_IMAGE_PATH) {
                LogUtils.logi( "Wow!! Snapshot successful and return back");
                Intent intentScan = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, mImageUri);
                sendBroadcast(intentScan);
            } else {
                LogUtils.logi("Data: ==" + data);
                mImageUri = data.getData();
            }

            displayImage();
        } else {
            LogUtils.logi("Snapshot Fail and return back");
        }
    }

    /**
     * To display original image , which it specify scale factor bitmap and
     * dimension (1024 * 768).
     */
    private void displayImage() {
        LogUtils.logi("displayImage: started");
        int width, height;
        int specifyScaleFactor;

        BitmapFactory.Options option = new BitmapFactory.Options();
        option.inJustDecodeBounds = true;

        try{
            BitmapFactory.decodeStream(
                    getContentResolver().openInputStream(mImageUri), null, option);
        }catch (IOException e){
            LogUtils.logi("Failed to intial openInputStream(mImageUri)");
            return;
        }

        width = option.outWidth;
        height = option.outHeight;
        specifyScaleFactor = Math.min(width/1024, height/768);
        option.inJustDecodeBounds = false;
        option.inSampleSize = specifyScaleFactor;
        Bitmap bitMap = null;

        try{

            bitMap = BitmapFactory.decodeStream(
                    getContentResolver().openInputStream(mImageUri), null, option);

        }catch (IOException e){
            LogUtils.logi("Failed to decodeStream mImageUri to bitMap");
        }

        mSnapShotIV.setImageBitmap(bitMap);
    }
}
