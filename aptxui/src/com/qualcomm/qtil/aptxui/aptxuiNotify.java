/******************************************************************************
 *
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All rights reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 ******************************************************************************/
package com.qualcomm.qtil.aptxui;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothCodecConfig;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.RemoteViews;
import androidx.core.app.JobIntentService;

public class aptxuiNotify extends JobIntentService {

  public static final int JOB_ID = 61707458;
  private static final String TAG = "aptxuiNotify";
  private static final boolean DBG = false;

  public static final String ACTION_NOTIFY_A2DP_CODEC = "NOTIFY_A2DP_CODEC";
  public static final String ACTION_NOTIFY_QSS_SUPPORT = "NOTIFY_QSS_SUPPORT";

  private static String A2DP_CODEC_CHANNEL_ID = "aptxuiNotifyA2dpCodec";
  private static String QSS_CHANNEL_ID = "aptxuiNotifyQss";
  private static final int A2DP_CODEC_NOTIFICATION_ID = 61707459;
  private static final int QSS_NOTIFICATION_ID = 61707460;

  private static NotificationManager mNotificationManager = null;
  private static NotificationChannel mA2dpCodecNotificationChannel = null;
  private static NotificationChannel mQssNotificationChannel = null;

  public static void enqueueWork(Context context, Intent work) {
    enqueueWork(context, aptxuiNotify.class, JOB_ID, work);
  }

  @Override
  protected void onHandleWork(Intent intent) {
    if (intent != null) {
      final String action = intent.getAction();
      Context context = getApplicationContext();
      int codec = BluetoothCodecConfig.SOURCE_CODEC_TYPE_INVALID;
      Log.i(TAG, "onHandleWork action: " + action);

      switch (action) {
        case ACTION_NOTIFY_A2DP_CODEC:
          codec = intent.getIntExtra("codec", BluetoothCodecConfig.SOURCE_CODEC_TYPE_INVALID);
          if (DBG) Log.d(TAG, "onHandleWork ACTION_NOTIFY_A2DP_CODEC codec: " + codec);
          showA2dpCodecNotification(context, codec);
          break;

        case ACTION_NOTIFY_QSS_SUPPORT:
          if (DBG) Log.d(TAG, "onHandleWork ACTION_NOTIFY_QSS_SUPPORT");
          showQssNotification(context);
          break;

        default:
          Log.e(TAG, "onHandleWork invalid action: " + action);
          return;
      }
    }
  }

  /**
   * Get notification manager.
   *
   * @return notification manager object
   */
  private NotificationManager getNotificationManager() {
    if (mNotificationManager == null) {
      mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }
    return mNotificationManager;
  }

  /**
   * Show A2DP codec notification.
   *
   * @param context the context
   * @param codec the codec type
   */
  private void showA2dpCodecNotification(Context context, int codec) {
    try {
      String name = getString(R.string.notification_a2dp_codec_name);
      mA2dpCodecNotificationChannel =
          new NotificationChannel(A2DP_CODEC_CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH);
      getNotificationManager().createNotificationChannel(mA2dpCodecNotificationChannel);

      int layoutId = getA2dpCodecLayout(context, codec);
      int drawableId = getA2dpCodecDrawable(codec);
      if (layoutId == 0 || drawableId == 0) {
        getNotificationManager().cancel(A2DP_CODEC_NOTIFICATION_ID);
        return;
      }

      Notification notification = createA2dpCodecNotification(context, layoutId, drawableId);
      getNotificationManager().notify(A2DP_CODEC_NOTIFICATION_ID, notification);
    } catch (Exception e) {
      Log.e(TAG, "showA2dpCodecNotification Exception: " + e);
    }
  }

  /**
   * Get A2DP codec layout resource.
   *
   * @param context the context
   * @param codec the codec type
   * @return layout resource id
   */
  private int getA2dpCodecLayout(Context context, int codec) {
    int layoutId = 0;

    switch (codec) {
      case BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX:
        layoutId = R.layout.qti_aptx_layout;
        break;
      case BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX_HD:
        layoutId = R.layout.qti_aptx_hd_layout;
        break;
      case BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX_ADAPTIVE:
        layoutId = R.layout.qti_aptx_adaptive_layout;
        break;
      default:
        break;
    }

    if (DBG) Log.d(TAG, "getA2dpCodecLayout layoutId: " + layoutId);
    return layoutId;
  }

  /**
   * Get A2DP codec drawable resource.
   *
   * @param codec the codec type
   * @return drawable resource id
   */
  private int getA2dpCodecDrawable(int codec) {
    int drawableId = 0;

    switch (codec) {
      case BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX:
        drawableId = R.drawable.qti_aptx_als_logo;
        break;
      case BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX_HD:
        drawableId = R.drawable.qti_aptx_hd_als_logo;
        break;
      case BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX_ADAPTIVE:
        drawableId = R.drawable.qti_aptx_adaptive_als_logo;
        break;
      default:
        break;
    }

    if (DBG) Log.d(TAG, "getA2dpCodecDrawable drawableId: " + drawableId);
    return drawableId;
  }

  /**
   * Create A2DP codec notification.
   *
   * @param context the context
   * @param layoutId the id of the layout resource
   * @param drawableId the id of the drawable resource
   * @return notification object
   */
  private static Notification createA2dpCodecNotification(
      Context context, int layoutId, int drawableId) {
    int icon = android.R.drawable.stat_sys_headset;
    RemoteViews contentView = new RemoteViews(context.getPackageName(), layoutId);
    Bitmap bitmap = getBitmap(context, icon);
    contentView.setImageViewBitmap(R.id.icon, bitmap);
    contentView.setImageViewResource(R.id.logo, drawableId);

    Notification.Builder notificationBuilder =
        new Notification.Builder(context, A2DP_CODEC_CHANNEL_ID);
    notificationBuilder
        .setSmallIcon(icon)
        .setOngoing(false)
        .setAutoCancel(false)
        .setChannelId(A2DP_CODEC_CHANNEL_ID)
        .setCustomContentView(contentView)
        .setCategory(Notification.CATEGORY_SYSTEM)
        .setDefaults(0)
        .setTimeoutAfter(3000)
        .setPriority(Notification.PRIORITY_MAX)
        .setVisibility(Notification.VISIBILITY_SECRET);
    return notificationBuilder.build();
  }

  /**
   * Show QSS notification.
   *
   * @param context the context
   */
  private void showQssNotification(Context context) {
    try {
      String name = getString(R.string.notification_qss_name);
      mQssNotificationChannel =
          new NotificationChannel(QSS_CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH);
      getNotificationManager().createNotificationChannel(mQssNotificationChannel);

      int layoutId = R.layout.qti_snapdragon_sound_layout;
      if (DBG) Log.d(TAG, "showQssNotification layoutId: " + layoutId);
      Notification notification = createQssNotification(context, layoutId);
      getNotificationManager().notify(QSS_NOTIFICATION_ID, notification);
    } catch (Exception e) {
      Log.e(TAG, "showQssNotification Exception: " + e);
    }
  }

  /**
   * Create QSS notification.
   *
   * @param context the context
   * @param layoutId the id of the layout resource
   * @return notification object
   */
  private static Notification createQssNotification(Context context, int layoutId) {
    int icon = android.R.drawable.stat_sys_headset;
    RemoteViews contentView = new RemoteViews(context.getPackageName(), layoutId);
    Bitmap bitmap = getBitmap(context, icon);
    contentView.setImageViewBitmap(R.id.icon, bitmap);
    contentView.setImageViewResource(R.id.logo, R.drawable.qc_snp_sound);

    Notification.Builder notificationBuilder = new Notification.Builder(context, QSS_CHANNEL_ID);
    notificationBuilder
        .setSmallIcon(icon)
        .setOngoing(false)
        .setAutoCancel(false)
        .setChannelId(QSS_CHANNEL_ID)
        .setCustomContentView(contentView)
        .setCategory(Notification.CATEGORY_SYSTEM)
        .setDefaults(0)
        .setTimeoutAfter(3000)
        .setPriority(Notification.PRIORITY_MAX)
        .setVisibility(Notification.VISIBILITY_SECRET);
    return notificationBuilder.build();
  }

  /**
   * Get resource bitmap.
   *
   * @param context the context
   * @param resId the resource id
   * @return resource bitmap
   */
  private static Bitmap getBitmap(Context context, int resId) {
    int largeIconWidth =
        (int) context.getResources().getDimension(android.R.dimen.notification_large_icon_width);
    int largeIconHeight =
        (int) context.getResources().getDimension(android.R.dimen.notification_large_icon_height);
    Drawable d = context.getResources().getDrawable(resId);
    Bitmap b = Bitmap.createBitmap(largeIconWidth, largeIconHeight, Bitmap.Config.ARGB_8888);
    Canvas c = new Canvas(b);
    d.setBounds(0, 0, largeIconWidth, largeIconHeight);
    d.draw(c);
    return b;
  }
}
