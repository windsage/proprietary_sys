/*
 * Copyright (c) 2018 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 *
 */

#include "IPerfManager.h"
#define MAX_SIZE_ALLOWED 64
#define MAX_SIZE_RESERVE_ARGS_ALLOWED 32

namespace android {

class BpPerfManager : public BpInterface<IPerfManager> {
public:
    BpPerfManager(const sp<IBinder>& binder)
        : BpInterface<IPerfManager>(binder)
    {
    }
    virtual int perfLockAcquire(int duration, int len, int* boostsList)
    {
        Parcel data, reply;
        data.writeInterfaceToken(IPerfManager::getInterfaceDescriptor());
        data.writeInt32(duration);
        data.writeInt32(len);
        data.writeInt32Array(len, boostsList);
        remote()->transact(PERF_LOCK_ACQUIRE, data, &reply);
        reply.readExceptionCode();
        return reply.readInt32();
    }

    virtual int perfHint(int hint, String16& userDataStr, int userData1, int userData2, int tid)
    {
        Parcel data, reply;
        data.writeInterfaceToken(IPerfManager::getInterfaceDescriptor());
        data.writeInt32(hint);
        data.writeString16(userDataStr);
        data.writeInt32(userData1);
        data.writeInt32(userData2);
        data.writeInt32(tid);
        remote()->transact(PERF_HINT, data, &reply);
        reply.readExceptionCode();
        return reply.readInt32();
    }

    virtual int perfLockRelease()
    {
        Parcel data, reply;
        data.writeInterfaceToken(IPerfManager::getInterfaceDescriptor());
        remote()->transact(PERF_LOCK_RELEASE, data, &reply);
        reply.readExceptionCode();
        return reply.readInt32();
    }

    virtual int perfLockReleaseHandler(int _handle)
    {
        Parcel data, reply;
        data.writeInterfaceToken(IPerfManager::getInterfaceDescriptor());
        data.writeInt32(_handle);
        remote()->transact(PERF_LOCK_RELEASE_HANDLER, data, &reply);
        reply.readExceptionCode();
        return reply.readInt32();
    }

    virtual int perfUXEngine_events(int opcode, int pid, String16& pkg_name, int lat)
    {
        Parcel data, reply;
        data.writeInterfaceToken(IPerfManager::getInterfaceDescriptor());
        data.writeInt32(opcode);
        data.writeInt32(pid);
        data.writeString16(pkg_name);
        data.writeInt32(lat);
        remote()->transact(PERF_UX_ENGINE_EVENTS, data, &reply);
        reply.readExceptionCode();
        return reply.readInt32();
    }

    virtual int setClientBinder(const sp<IBinder>& client)
    {
        Parcel data, reply;
        data.writeInterfaceToken(IPerfManager::getInterfaceDescriptor());
        data.writeStrongBinder(client);
        remote()->transact(SET_CLIENT_BINDER, data, &reply);
        reply.readExceptionCode();
        return reply.readInt32();
    }

    virtual int perfLockAcqAndRelease(int handle, int duration, int len, int reservelen, int* boostsList)
    {
        Parcel data, reply;
        data.writeInterfaceToken(IPerfManager::getInterfaceDescriptor());
        if (len <= 0 || len > MAX_SIZE_ALLOWED || reservelen < 0 || reservelen > MAX_SIZE_RESERVE_ARGS_ALLOWED ||
                (reservelen + len) > (MAX_SIZE_ALLOWED + MAX_SIZE_RESERVE_ARGS_ALLOWED) ||
                (reservelen + len) <= 0) {
            return -1;
        }
        data.writeInt32(handle);
        data.writeInt32(duration);
        data.writeInt32(len);
        data.writeInt32(reservelen);
        data.writeInt32Array(len + reservelen, boostsList);
        remote()->transact(PERF_LOCK_ACQUIRE_RELEASE, data, &reply);
        reply.readExceptionCode();
        return reply.readInt32();
    }

    virtual int perfGetFeedbackExtn(int req, String16& pkg_name, int tid, int len, int* list)
    {
        if (len < 0 || len > MAX_SIZE_RESERVE_ARGS_ALLOWED) {
            return -1;
        }
        Parcel data, reply;
        data.writeInterfaceToken(IPerfManager::getInterfaceDescriptor());
        data.writeInt32(req);
        data.writeString16(pkg_name);
        data.writeInt32(tid);
        data.writeInt32(len);
        data.writeInt32Array(len, list);
        remote()->transact(PERF_GET_FEEDBACK, data, &reply);
        reply.readExceptionCode();
        return reply.readInt32();
    }

    virtual void perfEvent(int eventId, String16& pkg_name,int tid, int len, int* list)
    {
        if (len < 0 || len > MAX_SIZE_RESERVE_ARGS_ALLOWED) {
            return;
        }
        Parcel data, reply;
        data.writeInterfaceToken(IPerfManager::getInterfaceDescriptor());
        data.writeInt32(eventId);
        data.writeString16(pkg_name);
        data.writeInt32(tid);
        data.writeInt32(len);
        data.writeInt32Array(len, list);
        remote()->transact(PERF_EVENT, data, &reply, IBinder::FLAG_ONEWAY);
        reply.readExceptionCode();
    }

    virtual int perfHintAcqRel(int handle, int hint, String16& pkg_name, int duration, int hint_type, int tid, int len, int* list)
    {
        if (len < 0 || len > MAX_SIZE_RESERVE_ARGS_ALLOWED) {
            return -1;
        }
        Parcel data, reply;
        data.writeInterfaceToken(IPerfManager::getInterfaceDescriptor());
        data.writeInt32(handle);
        data.writeInt32(hint);
        data.writeString16(pkg_name);
        data.writeInt32(duration);
        data.writeInt32(hint_type);
        data.writeInt32(tid);
        data.writeInt32(len);
        data.writeInt32Array(len, list);
        remote()->transact(PERF_HINT_ACQUIRE_RELEASE, data, &reply);
        reply.readExceptionCode();
        return reply.readInt32();
    }

    virtual int perfHintRenew(int handle, int hint, String16& pkg_name, int duration, int hint_type, int tid, int len, int* list)
    {
        if (len < 0 || len > MAX_SIZE_RESERVE_ARGS_ALLOWED) {
            return -1;
        }
        Parcel data, reply;
        data.writeInterfaceToken(IPerfManager::getInterfaceDescriptor());
        data.writeInt32(handle);
        data.writeInt32(hint);
        data.writeString16(pkg_name);
        data.writeInt32(duration);
        data.writeInt32(hint_type);
        data.writeInt32(tid);
        data.writeInt32(len);
        data.writeInt32Array(len, list);
        remote()->transact(PERF_HINT_RENEW, data, &reply);
        reply.readExceptionCode();
        return reply.readInt32();
    }

    virtual double getPerfHalVer()
    {
        Parcel data, reply;
        data.writeInterfaceToken(IPerfManager::getInterfaceDescriptor());
        remote()->transact(PERF_HAL_VER, data, &reply);
        reply.readExceptionCode();
        return reply.readDouble();
    }
};

IMPLEMENT_META_INTERFACE(PerfManager, "com.qualcomm.qti.IPerfManager");

status_t BnPerfManager::onTransact(uint32_t code, const Parcel& data, Parcel* reply, uint32_t flags)
{

    switch (code) {
    case PERF_LOCK_ACQUIRE: {
        CHECK_INTERFACE(IPerfManager, data, reply);
        int duration = data.readInt32();
        int len = data.readInt32();
        len = data.readInt32(); //this is written by array write method.
        if (len <= 0 || len > MAX_SIZE_ALLOWED)
            return BAD_VALUE;
        int boostList[MAX_SIZE_ALLOWED];
        for (int i = 0; i < len; i++) {
            boostList[i] = data.readInt32();
        }
        int handle = perfLockAcquire(duration, len, boostList);
        reply->writeNoException();
        reply->writeInt32(handle);
        return NO_ERROR;
    } break;
    case PERF_HINT: {
        CHECK_INTERFACE(IPerfManager, data, reply);
        int hint = data.readInt32();
        String16 userDataStr = data.readString16();
        int userData1 = data.readInt32();
        int userData2 = data.readInt32();
        int tid = data.readInt32();
        int handle = perfHint(hint, userDataStr, userData1, userData2, tid);
        reply->writeNoException();
        reply->writeInt32(handle);
        return NO_ERROR;
    } break;
    case PERF_LOCK_RELEASE: {
        CHECK_INTERFACE(IPerfManager, data, reply);
        int result = perfLockRelease();
        reply->writeNoException();
        reply->writeInt32(result);
        return NO_ERROR;
    } break;
    case PERF_LOCK_RELEASE_HANDLER: {
        CHECK_INTERFACE(IPerfManager, data, reply);
        int handle = data.readInt32();
        int result = perfLockReleaseHandler(handle);
        reply->writeNoException();
        reply->writeInt32(result);
        return NO_ERROR;
    } break;
    case PERF_UX_ENGINE_EVENTS: {
        CHECK_INTERFACE(IPerfManager, data, reply);
        int opcode = data.readInt32();
        int pid = data.readInt32();
        String16 pkg_name = data.readString16();
        int lat = data.readInt32();
        int result = perfUXEngine_events(opcode, pid, pkg_name, lat);
        reply->writeNoException();
        reply->writeInt32(result);
        return NO_ERROR;
    } break;
    case SET_CLIENT_BINDER: {
        CHECK_INTERFACE(IPerfManager, data, reply);
        sp<IBinder> clientBinder = data.readStrongBinder();
        int result = setClientBinder(clientBinder);
        reply->writeNoException();
        reply->writeInt32(result);
        return NO_ERROR;
    } break;
    case PERF_LOCK_ACQUIRE_RELEASE: {
        CHECK_INTERFACE(IPerfManager, data, reply);
        int handle = data.readInt32();
        int duration = data.readInt32();
        int len = data.readInt32();
        int reservelen = data.readInt32();
        int tlen = data.readInt32();
        if (len <= 0 || len > MAX_SIZE_ALLOWED || reservelen < 0 || reservelen > MAX_SIZE_RESERVE_ARGS_ALLOWED ||
                tlen != (reservelen + len)) {
            return BAD_VALUE;
        }
        int boostList[MAX_SIZE_ALLOWED + MAX_SIZE_RESERVE_ARGS_ALLOWED];
        for (int i = 0; i < tlen; i++) {
            boostList[i] = data.readInt32();
        }
        handle = perfLockAcqAndRelease(handle, duration, len, reservelen, boostList);
        reply->writeNoException();
        reply->writeInt32(handle);
        return NO_ERROR;
    } break;
    case PERF_GET_FEEDBACK: {
        CHECK_INTERFACE(IPerfManager, data, reply);
        int req = data.readInt32();
        String16 pkg_name = data.readString16();
        int tid = data.readInt32();
        int tlen = data.readInt32();
        tlen = data.readInt32(); //this is written by array write method.
        if (tlen < 0 || tlen > MAX_SIZE_RESERVE_ARGS_ALLOWED) {
            return BAD_VALUE;
        }
        int list[MAX_SIZE_RESERVE_ARGS_ALLOWED];
        for (int i = 0; i < tlen; i++) {
            list[i] = data.readInt32();
        }
        req = perfGetFeedbackExtn(req, pkg_name, tid, tlen, list);
        reply->writeNoException();
        reply->writeInt32(req);
        return NO_ERROR;
    } break;
    case PERF_EVENT: {
        CHECK_INTERFACE(IPerfManager, data, reply);
        int eventId = data.readInt32();
        String16 pkg_name = data.readString16();
        int tid = data.readInt32();
        int tlen = data.readInt32();
        tlen = data.readInt32(); //this is written by array write method.
        if (tlen < 0 || tlen > MAX_SIZE_RESERVE_ARGS_ALLOWED) {
            return BAD_VALUE;
        }
        int list[MAX_SIZE_RESERVE_ARGS_ALLOWED];
        for (int i = 0; i < tlen; i++) {
            list[i] = data.readInt32();
        }
        perfEvent(eventId, pkg_name, tid, tlen, list);
        return OK;
    } break;
    case PERF_HINT_ACQUIRE_RELEASE: {
        CHECK_INTERFACE(IPerfManager, data, reply);
        int handle = data.readInt32();
        int hint = data.readInt32();
        String16 pkg_name = data.readString16();
        int duration = data.readInt32();
        int hint_type = data.readInt32();
        int tid = data.readInt32();
        int tlen = data.readInt32();
        tlen = data.readInt32(); //this is written by array write method.
        if (tlen < 0 || tlen > MAX_SIZE_RESERVE_ARGS_ALLOWED) {
            return BAD_VALUE;
        }
        int list[MAX_SIZE_RESERVE_ARGS_ALLOWED];
        for (int i = 0; i < tlen; i++) {
            list[i] = data.readInt32();
        }
        handle = perfHintAcqRel(handle, hint, pkg_name, duration, hint_type, tid, tlen, list);
        reply->writeNoException();
        reply->writeInt32(handle);
        return NO_ERROR;
    } break;
    case PERF_HINT_RENEW: {
        CHECK_INTERFACE(IPerfManager, data, reply);
        int handle = data.readInt32();
        int hint = data.readInt32();
        String16 pkg_name = data.readString16();
        int duration = data.readInt32();
        int hint_type = data.readInt32();
        int tid = data.readInt32();
        int tlen = data.readInt32();
        tlen = data.readInt32(); //this is written by array write method.
        if (tlen < 0 || tlen > MAX_SIZE_RESERVE_ARGS_ALLOWED) {
            return BAD_VALUE;
        }
        int list[MAX_SIZE_RESERVE_ARGS_ALLOWED];
        for (int i = 0; i < tlen; i++) {
            list[i] = data.readInt32();
        }
        handle = perfHintRenew(handle, hint, pkg_name, duration, hint_type, tid, tlen, list);
        reply->writeNoException();
        reply->writeInt32(handle);
        return NO_ERROR;
    } break;
    case PERF_HAL_VER: {
        CHECK_INTERFACE(IPerfManager, data, reply);
        double handle = 2.2;
        handle = getPerfHalVer();
        reply->writeNoException();
        reply->writeDouble(handle);
        return NO_ERROR;
    } break;
    default:
      return BBinder::onTransact(code, data, reply, flags);
    }
}
}; //namespace android
