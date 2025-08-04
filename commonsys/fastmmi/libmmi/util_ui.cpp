/*
 * Copyright (c) 2014-2017, Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
#include "utils.h"
#include "view.h"
#ifdef ANDROID
bool init_surface(draw_control_t * draw) {

    MMI_ALOGI("start init surface");
    char value[PROPERTY_VALUE_MAX] = { 0 };
    if(draw == NULL) {
        MMI_ALOGE("Invalid param!");
        return false;
    }

    draw->client = new SurfaceComposerClient();
    if(draw->client == NULL) {
        MMI_ALOGE("get surface composer client fail");
        return false;
    }

    const std::vector<PhysicalDisplayId> ids = SurfaceComposerClient::getPhysicalDisplayIds();
    if (ids.empty()) {
        MMI_ALOGE("Failed to get ID for any displays\n");
        return false;
    }

    sp < IBinder > dtoken = SurfaceComposerClient::getPhysicalDisplayToken(ids.front());
    if (dtoken == nullptr) {
        MMI_ALOGE("getPhysicalDisplayToken(ids.front()) failed");
        return false;
    }
    status_t status = SurfaceComposerClient::getActiveDisplayMode(dtoken, &draw->dmode);

    if(status) {
        MMI_ALOGE("get display info fail");
        return false;
    }

    property_get("persist.panel.orientation", value, "0");
    int orient = atoi(value) / 90;

    if(orient == eOrientation90 || orient == eOrientation270) {
        int temp = draw->dmode.resolution.getHeight();

        draw->dmode.resolution.setHeight(draw->dmode.resolution.getWidth());
        draw->dmode.resolution.setWidth(temp);
    }

    draw->pixels_percent_x = (float)draw->dmode.resolution.getWidth() / (float)PERCENT_MAX;
    draw->pixels_percent_y = (float)draw->dmode.resolution.getHeight() / (float)PERCENT_MAX;

    Rect destRect(draw->dmode.resolution.getWidth(), draw->dmode.resolution.getHeight());

    //draw->client->setDisplayProjection(dtoken, orient, destRect, destRect);

    // create the native surface
    draw->control =
        draw->client->createSurface(String8(draw->name), draw->dmode.resolution.getWidth(),
                                    draw->dmode.resolution.getHeight(), PIXEL_FORMAT_RGBA_8888);

    if(draw->control == NULL) {
        MMI_ALOGE("get native surface fail");
        return false;
    }

    draw->surface = draw->control->getSurface();

    //SurfaceComposerClient::openGlobalTransaction();
    //draw->control->setLayer(draw->layer);
    //draw->control->show();
    //SurfaceComposerClient::closeGlobalTransaction();
    SurfaceComposerClient::Transaction{}
            .setLayer(draw->control, draw->layer)
            .show(draw->control)
            .apply();

    MMI_ALOGD("surface init success: draw name=%s, draw layer=%d, draw control=%p, "
              "surface width:%d, surface heigth=%d",
              draw->name, draw->layer, draw->control.get(), draw->dmode.resolution.getWidth(),
              draw->dmode.resolution.getHeight());
    return true;
}

bool create_surface(draw_control_t * draw) {
    MMI_ALOGI("start create surface");

    if(draw == NULL || draw->client == NULL) {
        MMI_ALOGE("Invalid param!");
        return false;
    }

    draw->control =
        draw->client->createSurface(String8(draw->name), draw->surface_w, draw->surface_h, PIXEL_FORMAT_RGBA_8888);

    if((draw->control == NULL) || (!draw->control->isValid())) {
        MMI_ALOGE("create Surface failed\n");
        return false;
    }
    MMI_ALOGD("create surface success: draw name=%s, draw layer=%d, draw control=%p, "
              "surface width=%d, surface high=%d\n",
              draw->name, draw->layer, draw->control.get(), draw->surface_w, draw->surface_h);

    draw->surface = draw->control->getSurface();

    //SurfaceComposerClient::openGlobalTransaction();
    //draw->control->setLayer(draw->layer);
    //SurfaceComposerClient::closeGlobalTransaction();
	SurfaceComposerClient::Transaction{}
		.setLayer(draw->control, draw->layer)
		.apply();

    return true;
}

void remove_surface(draw_control_t * draw) {
    //SurfaceComposerClient::openGlobalTransaction();
    draw->control.clear();
    //SurfaceComposerClient::closeGlobalTransaction();
}

void draw_bitmap(SkBitmap * bitmap, draw_control_t * pdraw) {
    if(bitmap == NULL || pdraw == NULL || pdraw->surface == NULL) {
        MMI_ALOGE("Invalid parameter\n");
        return;
    }

    /** Comments below snippet as SkFilterQuality has been removed from skia library and this function is unused. */
    /**
    ANativeWindow_Buffer outBuffer;

    pdraw->surface->lock(&outBuffer, NULL);
    SkBitmap surfaceBitmap;
    ssize_t bpr = outBuffer.stride * bytesPerPixel(outBuffer.format);

    surfaceBitmap.installPixels(SkImageInfo::MakeN32Premul(outBuffer.width, outBuffer.height), outBuffer.bits, bpr);
    SkCanvas surfaceCanvas(surfaceBitmap);

    //Initial background to Black
    surfaceCanvas.clear(SK_ColorBLACK);

    SkSamplingOptions::MediumBehavior mb = surfaceCanvas.recordingContext() ?
        SkSamplingOptions::kMedium_asMipmapLinear : SkSamplingOptions::kMedium_asMipmapNearest;
    SkSamplingOptions samplingOptions(pdraw->paint.getFilterQuality(), mb);
    surfaceCanvas.drawImage(bitmap->drawsNothing() ? nullptr : bitmap->asImage(), 0, 0,
                            samplingOptions, &pdraw->paint);
    pdraw->surface->unlockAndPost();
    */
}
#endif

bool is_point_in_rect(int x, int y, rect_t * rect) {
    if(rect == NULL)
        return false;

    return (x >= rect->x && x <= rect->x + rect->w && y >= rect->y && y <= rect->y + rect->h);
}

bool is_point_in_rect(point_t point, rect_t * rect) {
    if(rect == NULL)
        return false;

    return (point.x >= rect->x && point.x <= rect->x + rect->w && point.y >= rect->y && point.y <= rect->y + rect->h);
}
