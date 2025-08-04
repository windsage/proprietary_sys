/*====*====*====*====*====*====*====*====*====*====*====*====*====*====*====*
Copyright (c) 2020-2024 Qualcomm Technologies, Inc.
All Rights Reserved.
Confidential and Proprietary - Qualcomm Technologies, Inc.

              Diag communication support

GENERAL DESCRIPTION

Implementation of diaghal communication layer between diag library and diag driver.

*====*====*====*====*====*====*====*====*====*====*====*====*====*====*====*/
#include <stdlib.h>
#include "comdef.h"
#include "stdio.h"
#include "stringl.h"
#include "diag_lsmi.h"
#include "diagsvc_malloc.h"
#include "diag_lsm_event_i.h"
#include "diag_lsm_log_i.h"
#include "diag_lsm_msg_i.h"
#include "diag.h" /* For definition of diag_cmd_rsp */
#include "diag_lsm_pkt_i.h"
#include "diag_lsm_dci_i.h"
#include "diag_lsm_dci.h"
#include "diag_shared_i.h" /* For different constants */
#include <diag_lsm.h>
#include <sys/ioctl.h>
#include <unistd.h>
#include <fcntl.h>
#include "errno.h"
#include <pthread.h>
#include <stdint.h>
#include <signal.h>
#include <vendor/qti/diaghal/1.0/Idiag.h>
#include <vendor/qti/diaghal/1.0/Idiagcallback.h>
#include <vendor/qti/diaghal/1.0/types.h>
#include <hidl/Status.h>
#include <utils/misc.h>
#include <utils/Log.h>
#include <hidl/HidlSupport.h>
#include <stdio.h>
#include <log/log.h>
#include <hidl/Status.h>
#include <hwbinder/IPCThreadState.h>
#include <android/hidl/allocator/1.0/IAllocator.h>
#include <android/hidl/memory/1.0/IMemory.h>
#include <hidlmemory/mapping.h>
#include <hidl/LegacySupport.h>
#include <pthread.h>
#include "diagcallback.h"
#include "diag_lsm_hidl_client.h"

/* Headers required for AIDL */
#include <cutils/ashmem.h>
#include <android/binder_manager.h>
#include <android/binder_process.h>
#include <aidl/vendor/qti/diaghal/BnIdiag.h>
#include <aidl/vendor/qti/diaghal/ParcelableMemory.h>

int diag_use_dev_node = 0;
static pthread_mutex_t m_diag_client_mutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_mutex_t read_data_mutex = PTHREAD_MUTEX_INITIALIZER;
pthread_cond_t read_data_cond = PTHREAD_COND_INITIALIZER;
static pthread_mutex_t write_data_mutex = PTHREAD_MUTEX_INITIALIZER;
pthread_cond_t write_data_cond = PTHREAD_COND_INITIALIZER;
int read_buf_busy = 0;
unsigned int data_len = 0;

size_t memscpy(void *dst, size_t dst_size, const void *src, size_t copy_size)
{
	if(dst_size >= copy_size) {
#ifdef _WIN32
		memcpy_s(dst, dst_size, src, copy_size);
#else
		memcpy(dst, src, copy_size);
#endif
		return copy_size;
	}

#ifdef _WIN32
		memcpy_s(dst, dst_size, src, dst_size);
#else
		memcpy(dst, src, dst_size);
#endif

	return dst_size;
}

/* AIDL Status Variables */
using ::std::string;
using ::std::shared_ptr;
using ::ndk::SpAIBinder;
using ::ndk::ScopedAIBinder_DeathRecipient;
using ::ndk::ScopedAStatus;
using IdiagAidl = aidl::vendor::qti::diaghal::IIdiag;
using IdiagcallbackAidl = ::aidl::vendor::qti::diaghal::Idiagcallback;
using ::aidl::vendor::qti::diaghal::ParcelableMemory;

static std::shared_ptr<IdiagAidl> diaghalAidl = nullptr;
static std::shared_ptr<IdiagcallbackAidl> aidlCallback = ndk::SharedRefBase::make<IdiagcallbackAidl>();

/* Close on the file descriptor allocated here is called within the desctructor of
 * mem.file.fd (ScopedFileDescriptor). It occurs when the function memis declared in goes
 * out of scope.
 */
static int allocMem(ParcelableMemory& mem, uint32_t size, string& err)
{
	mem.file.fd.set(ashmem_create_region(mem.name.c_str(), size));

	if (mem.file.fd.get() == -1) {
		err = "failed to allocate memory\n";
		return -ENOMEM;
	}

	mem.file.length = size;
	return 0;
}

static void *mapMem(const ParcelableMemory& mem, string& err)
{
	void *addr = mmap(nullptr, mem.file.length, PROT_READ | PROT_WRITE, MAP_SHARED, mem.file.fd.get(), 0);

	if (addr == nullptr || addr == (void*) -1) {
		err = std::string("failed to map mem: ") + std::strerror(errno);
		return nullptr;
	}

	return addr;
}

static int unmapMem(const ParcelableMemory& mem, void* buff, string& err)
{
	if (munmap(buff, mem.file.length) != 0) {
		err = std::string("Failed to unmap shared memory: ") + std::strerror(errno);
		return -EINVAL;
	}

	return 0;
}

static void clientDeathCallback(void *cookie)
{
	DIAG_LOGE("diaghal just died");
	pthread_mutex_lock(&m_diag_client_mutex);
	diaghalAidl = nullptr;
	pthread_mutex_unlock(&m_diag_client_mutex);

	diag_kill_comm_read();
}
static ScopedAIBinder_DeathRecipient deathRecipientAidl = ScopedAIBinder_DeathRecipient(AIBinder_DeathRecipient_new(&clientDeathCallback));

static int getDiaghalAidl()
{
	const std::string instance = std::string() + IdiagAidl::descriptor + "/default";
	int32_t aidl_return;

	ABinderProcess_startThreadPool();

	if (!AServiceManager_isDeclared(instance.c_str())) {
		DIAG_LOGE("diag: diaghalAidl service not declared\n");
		return -EINVAL;
	}

	auto diagBinder = SpAIBinder(AServiceManager_waitForService(instance.c_str()));
	if (diagBinder.get() == nullptr) {
		DIAG_LOGE("diag: diaghalAidl service doesn't exist\n");
		return -EINVAL;
	}

	auto status = ScopedAStatus::fromStatus(AIBinder_linkToDeath(diagBinder.get(), deathRecipientAidl.get(), NULL));
	if (!status.isOk()) {
		DIAG_LOGE("diag: linking to diaghalAidl service death failed: %d: %s\n", status.getStatus(), status.getMessage());
		return -EINVAL;
	}

	diaghalAidl = IdiagAidl::fromBinder(diagBinder);
	if (diaghalAidl == nullptr) {
		DIAG_LOGE("diag: diaghalAidl service doesn't exist\n");
		return -EINVAL;
	}

	status = diaghalAidl->open(aidlCallback, &aidl_return);
	if (!status.isOk()) {
		DIAG_LOGE("diag: %s: open failed\n", __func__);
		diaghalAidl = nullptr;
		return -EINVAL;
	}
	DIAG_LOGE("diag: successfully connected to diaghalAidl\n");

	return 0;
}

static int __diag_lsm_comm_ioctl_aidl(unsigned long request, void *dst, size_t dst_size,
					     void *src, size_t copy_size)
{
	ScopedAStatus status;
	ParcelableMemory mem;
	int32_t aidl_return;
	int ret = 0;
	void *data;
	string err;

	ret = allocMem(mem, copy_size, err);
	if (ret) {
		DIAG_LOGE("%s: %s\n", __func__, err.c_str());
		return -1;
	}
	data = mapMem(mem, err);
	if (data == nullptr) {
		DIAG_LOGE("%s: Could not get pointer to memory\n", __func__);
		return -1;
	}

	if (src)
		memcpy(data, src, copy_size);

	pthread_mutex_lock(&m_diag_client_mutex);
	if (!diaghalAidl) {
		ret = -1;
		goto out;
	}
	status = diaghalAidl->ioctl(request, mem, copy_size, &aidl_return);
	if (!status.isOk()) {
		DIAG_LOGE("%s: Error for ioctl req: %d\n", __func__, request);
		ret = -1;
		goto out;
	}
	ret = aidl_return;

	if (dst)
		memscpy(dst, dst_size, data, copy_size);

out:
	pthread_mutex_unlock(&m_diag_client_mutex);
	unmapMem(mem, data, err);

	return ret;
}

static int __diag_lsm_comm_ioctl_empty_aidl(unsigned long request)
{
	ScopedAStatus status;
	ParcelableMemory mem;
	int32_t aidl_return;
	string err;
	int rc;

	/* Do a small allocation because AIDL side will fail to map if 0 */
	rc = allocMem(mem, sizeof(int32_t), err);
	if (rc) {
		DIAG_LOGE("%s: %s\n", __func__, err.c_str());
		return -1;
	}
	pthread_mutex_lock(&m_diag_client_mutex);
	if (!diaghalAidl) {
		pthread_mutex_unlock(&m_diag_client_mutex);
		return -1;
	}
	status = diaghalAidl->ioctl(request, mem, 0, &aidl_return);
	pthread_mutex_unlock(&m_diag_client_mutex);
	if (!status.isOk()) {
		DIAG_LOGE("diag: Failed to send ioctl req: %d\n", request);
		return -1;
	}
	return aidl_return;
}

static int __diag_lsm_comm_ioctl_deinit_aidl(unsigned long request)
{
	int32_t aidl_return = 0;
	ScopedAStatus status;
	ParcelableMemory mem;
	string err;
	int rc;

	/* Do a small allocation because AIDL side will fail to map if 0 */
	rc = allocMem(mem, sizeof(int32_t), err);
	if (rc) {
		DIAG_LOGE("%s: %s\n", __func__, err.c_str());
		return -1;
	}
	pthread_mutex_lock(&m_diag_client_mutex);
	if (!diaghalAidl) {
		pthread_mutex_unlock(&m_diag_client_mutex);
		return -1;
	}
	status = diaghalAidl->ioctl(request, mem, 0, &aidl_return);
	AIBinder_unlinkToDeath(diaghalAidl->asBinder().get(), deathRecipientAidl.get(), nullptr);
	pthread_mutex_unlock(&m_diag_client_mutex);
	if (!status.isOk()) {
		DIAG_LOGE("diag: Failed to send ioctl req: %d\n", request);
		return -1;
	}
	if (aidl_return >= 0)
		return 1;
	return aidl_return;
}

int diag_lsm_comm_write_aidl(int fd, unsigned char buf[], int bytes)
{
	int bytes_written = -1;
	ScopedAStatus status;
	ParcelableMemory mem;
	int32_t aidl_return;
	void *data;
	string err;
	int rc;

	if (diaghalAidl == nullptr)
		return -1;

	rc = allocMem(mem, bytes, err);
	if (rc) {
		DIAG_LOGE("%s: %s\n", __func__, err.c_str());
		return -1;
	}
	data = mapMem(mem, err);
	if (data == nullptr) {
		DIAG_LOGE("%s: Could not get pointer to memory\n", __func__);
		return -1;
	}
	memcpy(data, buf, bytes);

	pthread_mutex_lock(&m_diag_client_mutex);
	if (!diaghalAidl)
		goto out;

	status = diaghalAidl->write(mem, bytes, &aidl_return);
	if (status.isOk())
		bytes_written = aidl_return;

	if (bytes_written < 0)
		bytes_written = -1;
	if (*(int *)buf != DCI_DATA_TYPE)
		bytes_written = 0;
out:
	pthread_mutex_unlock(&m_diag_client_mutex);
	unmapMem(mem, data, err);

	return bytes_written;
}

/* HIDL Status Variables */
using IdiagHidl = vendor::qti::diaghal::V1_0::Idiag;
using vendor::qti::diaghal::V1_0::Idiagcallback;
using vendor::qti::diaghal::V1_0::implementation::diagcallback;
using ::android::sp;
using ::android::hidl::base::V1_0::IBase;
using ::android::hidl::allocator::V1_0::IAllocator;
using ::android::hidl::memory::V1_0::IMemory;
using ::android::hardware::Void;
using ::android::hardware::Return;
using ::android::hardware::hidl_handle;
using ::android::hardware::hidl_memory;
using ::android::hardware::HidlMemory;
using ::android::hardware::hidl_string;
using ::android::hardware::hidl_death_recipient;

static sp<IdiagHidl> diaghalHidl = nullptr;
sp<IAllocator> ashmemAllocator;
sp<Idiagcallback> callback;

struct diaghalDeathRecipient : virtual public hidl_death_recipient {
	//called when the interface linked to it dies
	virtual void serviceDied(uint64_t, const android::wp<IBase>&) override {
		DIAG_LOGE("diaghal just died\n");
		pthread_mutex_lock(&m_diag_client_mutex);
		diaghalHidl = nullptr;
		pthread_mutex_unlock(&m_diag_client_mutex);

		diag_kill_comm_read();
	}
};
static android::sp<diaghalDeathRecipient> DeathRecipientHidl = nullptr;

static int getDiaghalHidl()
{
	int ret;

	diaghalHidl = IdiagHidl::getService();
	if (diaghalHidl == nullptr) {
		DIAG_LOGE("Unable to get diaghalHidl Service\n");
		return -EINVAL;
	}

	if(DeathRecipientHidl == nullptr)
		DeathRecipientHidl = new diaghalDeathRecipient();

	Return<bool> death_linked = diaghalHidl->linkToDeath(DeathRecipientHidl, 0);
	if(!death_linked || !death_linked.isOk()) {
		DIAG_LOGE("linking diaghal to death failed: %s\n", death_linked.description().c_str());
		diaghalHidl = nullptr;
		return -EINVAL;
	}
	DIAG_LOGE("diaghal linked to death!!\n");

	callback = diagcallback::getInstance();

	auto status = diaghalHidl->open(callback);
	if (!status.isOk()) {
		DIAG_LOGE("diag: %s: client callback open failed\n", __func__);
		diaghalHidl = nullptr;
		return -EINVAL;
	}
	ashmemAllocator = IAllocator::getService("ashmem");

	DIAG_LOGE("diag: successfully connected to hidl service \n");
	return 0;
}

static int __diag_lsm_comm_ioctl_empty_hidl(unsigned long request)
{
	int num_bytes = 0;
	hidl_memory mem_s;

	pthread_mutex_lock(&m_diag_client_mutex);
	if (!diaghalHidl) {
		pthread_mutex_unlock(&m_diag_client_mutex);
		return -1;
	}
	auto ret = diaghalHidl->ioctl(request, mem_s, num_bytes);
	pthread_mutex_unlock(&m_diag_client_mutex);
	if (!ret.isOk()) {
		DIAG_LOGE("diag: Failed to send ioctl req: %d\n", request);
		return -1;
	}
	return ret;
}

static int __diag_lsm_comm_ioctl_deinit_hidl(unsigned long request)
{
	int num_bytes = 0;
	hidl_memory mem_s;

	pthread_mutex_lock(&m_diag_client_mutex);
	if (!diaghalHidl) {
		pthread_mutex_unlock(&m_diag_client_mutex);
		return -1;
	}
	auto ret = diaghalHidl->ioctl(request, mem_s, num_bytes);
	if (!ret.isOk()) {
		DIAG_LOGE("diag: Failed to send ioctl req: %d\n", request);
		pthread_mutex_unlock(&m_diag_client_mutex);
		return -1;
	}
	Return<bool> death_linked = diaghalHidl->unlinkToDeath(DeathRecipientHidl);
	if(!death_linked || !death_linked.isOk()) {
		DIAG_LOGE("diag: Unlinking diaghal to death failed: %s\n",
			  death_linked.description().c_str());
		pthread_mutex_unlock(&m_diag_client_mutex);
		return -1;
	}
	pthread_mutex_unlock(&m_diag_client_mutex);
	if (ret >= 0)
		return 1;
	return ret;
}

static int __diag_lsm_comm_ioctl_hidl(unsigned long request, void *dst, size_t dst_size,
				      void *src, size_t copy_size)
{
	int ret = 0;
	void *data = NULL;

	ashmemAllocator->allocate(copy_size, [&](bool success, const hidl_memory &mem) {
		if (!success) {
			DIAG_LOGE("ashmem allocate failed!!\n");
			ret = -1;
			return;
		}
		sp<IMemory> memory = mapMemory(mem);
		if (!memory) {
			DIAG_LOGE("%s: Could not map HIDL memory to IMemory\n", __func__);
			ret = -1;
			return;
		}
		if (memory->getSize() != mem.size()) {
			DIAG_LOGE("diag: %s: Size mismatch in memory mapping\n", __func__);
			ret = -1;
			return;
		}
		data = memory->getPointer();
		if (!data) {
			DIAG_LOGE("%s: Could not get pointer to memory\n", __func__);
			ret = -1;
			return;
		}
		/* copy data to diag-router */
		if (src) {
			memory->update();
			memcpy(data, src, copy_size);
			memory->commit();
		}
		pthread_mutex_lock(&m_diag_client_mutex);
		if (!diaghalHidl) {
			ret = -1;
			pthread_mutex_unlock(&m_diag_client_mutex);
			return;
		}
		auto status = diaghalHidl->ioctl(request, mem, copy_size);
		pthread_mutex_unlock(&m_diag_client_mutex);
		if (!status.isOk()) {
			DIAG_LOGE("%s: Error for ioctl req: %d\n", __func__, request);
			ret = -1;
			return;
		}
		ret = status;
		/* copy data from diag-router */
		if (dst)
			memscpy(dst, dst_size, data, copy_size);
	});

	return ret;
}

int diag_lsm_comm_write_hidl(int fd, unsigned char buf[], int bytes)
{
	int bytes_written = 0, status = 0;
	hidl_memory mem_s;

	if (diaghalHidl == nullptr)
		return -1;

	if (ashmemAllocator == nullptr)
		return -1;

	ashmemAllocator->allocate(bytes, [&](bool success, const hidl_memory& mem) {
		if (!success) {
			DIAG_LOGE("ashmem allocate failed!!\n");
			status = -1;
			return;
		}
		mem_s = mem;
		sp<IMemory> memory = mapMemory(mem);
		if (memory == nullptr) {
			DIAG_LOGE("%s: Could not map HIDL memory to IMemory\n", __func__);
			status = -1;
			return;
		}
		if (memory->getSize() != mem.size()) {
			DIAG_LOGE("diag: %s: Size mismatch in memory mapping\n", __func__);
			status = -1;
			return;
		}
		void* data = memory->getPointer();
		if (data == nullptr) {
			DIAG_LOGE("%s: Could not get pointer to memory\n", __func__);
			status = -1;
			return;
		}
		memory->update();
		memcpy(data, buf, bytes);
		memory->commit();
	});
	if (status) {
		DIAG_LOGE("diag: In %s failed to alloc memory\n", __func__);
		return -1;
	}
	pthread_mutex_lock(&m_diag_client_mutex);
	if (!diaghalHidl) {
		pthread_mutex_unlock(&m_diag_client_mutex);
		return -1;
	}
	auto ret = diaghalHidl->write(mem_s, bytes);
	pthread_mutex_unlock(&m_diag_client_mutex);
	if (ret.isOk())
		bytes_written = ret;

	if (bytes_written < 0)
		return -1;

	if (*(int *)buf == DCI_DATA_TYPE)
		return bytes_written;

	return 0;
}

/*===========================================================================

FUNCTION diag_lsm_comm_open

DESCRIPTION
  If /dev/diag exists opens fd to /dev/diag else it tries to get diaghal server instance
  and registers callback with server

DEPENDENCIES
   None

RETURN VALUE
  SUCCESS/FAILURE.

SIDE EFFECTS
  None.
===========================================================================*/
int diag_lsm_comm_open(void)
{
	int fd;
	int ret;

	/* Try Legacy Kernel Diag Interface*/
	fd = open("/dev/diag", O_RDWR | O_CLOEXEC);
	diag_use_dev_node = (fd < 0) ? 0 : 1;
	if (fd >= 0)
		return fd;

	/* Try AIDL Interface */
	ret = getDiaghalAidl();
	if (!ret)
		return 0;

	/* Try HIDL Interface */
	ret = getDiaghalHidl();
	if (!ret)
		return 0;

	return -1;
}

/*===========================================================================

FUNCTION diag_lsm_comm_ioctl

DESCRIPTION
  If /dev/diag exists calls ioctl to kernel diag driver else tries to send
  ioctl command to diaghal server

DEPENDENCIES
   None

RETURN VALUE
  SUCCESS/FAILURE.

SIDE EFFECTS
  None.
===========================================================================*/
int diag_lsm_comm_ioctl(int fd, unsigned long request, void *buf, unsigned int len)
{
	int (*__ioctl)(unsigned long, void *, size_t, void *, size_t);
	int (*__ioctl_empty)(unsigned long);
	int (*__ioctl_deinit)(unsigned long);
	int i = 0, num_bytes = 0, err = 0;

	if (diag_use_dev_node) {
		err = ioctl(fd, request, buf, len, NULL, 0, NULL, NULL);
		return err;
	}

	if (diaghalHidl != nullptr) {
		if (ashmemAllocator == NULL)
			return -ENOMEM;

		__ioctl = __diag_lsm_comm_ioctl_hidl;
		__ioctl_empty = __diag_lsm_comm_ioctl_empty_hidl;
		__ioctl_deinit = __diag_lsm_comm_ioctl_deinit_hidl;
	}

	if (diaghalAidl != nullptr) {
		__ioctl = __diag_lsm_comm_ioctl_aidl;
		__ioctl_empty = __diag_lsm_comm_ioctl_empty_aidl;
		__ioctl_deinit = __diag_lsm_comm_ioctl_deinit_aidl;
	}

	if (!__ioctl || !__ioctl_empty || !__ioctl_deinit)
		return -EINVAL;

	switch (request) {
	case DIAG_IOCTL_COMMAND_REG:
	{
		struct diag_cmd_tbl {
			int count;
			diag_cmd_reg_entry_t entries[0];
		} __packed;
		struct diag_cmd_tbl *tbl ;
		diag_cmd_reg_tbl_t *reg_tbl;

		reg_tbl = (diag_cmd_reg_tbl_t *)buf;
		num_bytes = (sizeof(diag_cmd_reg_entry_t) * reg_tbl->count) + sizeof(reg_tbl->count);
		tbl = (struct diag_cmd_tbl *)malloc(num_bytes);
		if (!tbl)
			return -ENOMEM;
		tbl->count = reg_tbl->count;
		for (i = 0; i < reg_tbl->count; i++) {
			tbl->entries[i].cmd_code = reg_tbl->entries[i].cmd_code;
			tbl->entries[i].subsys_id = reg_tbl->entries[i].subsys_id;
			tbl->entries[i].cmd_code_lo = reg_tbl->entries[i].cmd_code_lo;
			tbl->entries[i].cmd_code_hi = reg_tbl->entries[i].cmd_code_hi;
		}

		err = __ioctl(request, NULL, 0, tbl, num_bytes);
		break;
	}
	case DIAG_IOCTL_COMMAND_DEREG:
	case DIAG_IOCTL_QUERY_MASK:
	{
		err  = __ioctl_empty(request);
		break;
	}
	case DIAG_IOCTL_LSM_DEINIT:
	{
		return __ioctl_deinit(request);
	}
	case DIAG_IOCTL_GET_DELAYED_RSP_ID:
		num_bytes = 2;
		err  = __ioctl(request, buf, len, NULL, num_bytes);
		break;
	case DIAG_IOCTL_REMOTE_DEV:
		num_bytes = 4;
		err = __ioctl(request, buf, len, NULL, num_bytes);
		break;
	case DIAG_IOCTL_QUERY_CON_ALL:
		num_bytes = (sizeof(struct diag_con_all_param_t));
		err = __ioctl(request, buf, len, NULL, num_bytes);
		break;
	case DIAG_IOCTL_QUERY_MD_PID:
	{
		struct diag_query_pid_t *pid_params = (struct diag_query_pid_t *)buf;

		num_bytes = (sizeof(struct diag_query_pid_t));
		err = __ioctl(request, buf, len, pid_params, num_bytes);
		break;
	}
	case DIAG_IOCTL_SWITCH_LOGGING:
	{
		int pid = getpid();
		int total_len = len + sizeof(pid);
		unsigned char tmp_buf[total_len];

		memcpy(tmp_buf, buf, len);
		memcpy(tmp_buf + len, &pid, sizeof(pid));
		err = __ioctl(request, NULL, 0, tmp_buf, total_len);
		break;
	}
	case DIAG_IOCTL_DCI_SUPPORT:
	case DIAG_IOCTL_DCI_HEALTH_STATS:
	case DIAG_IOCTL_DCI_LOG_STATUS:
	case DIAG_IOCTL_DCI_EVENT_STATUS:
	case DIAG_IOCTL_DCI_CLEAR_LOGS:
	case DIAG_IOCTL_DCI_CLEAR_EVENTS:
	case DIAG_IOCTL_DCI_DEINIT:
	case DIAG_IOCTL_DCI_DRAIN_IMMEDIATE:
	case DIAG_IOCTL_VOTE_DCI_BUFFERING_MODE:
		err = __ioctl(request, buf, len, buf, len);
		return err;
	case DIAG_IOCTL_DCI_REG:
		err = __ioctl(request, NULL, 0, buf, len);
		return err;
	case DIAG_IOCTL_GET_REAL_TIME:
		err = __ioctl(request, buf, len, buf, len);
		break;
	case DIAG_IOCTL_CONFIG_BUFFERING_TX_MODE:
	case DIAG_IOCTL_BUFFERING_DRAIN_IMMEDIATE:
	case DIAG_IOCTL_VOTE_REAL_TIME:
	case DIAG_IOCTL_QUERY_PD_LOGGING:
	case DIAG_IOCTL_SET_OVERRIDE_PID:
	case DIAG_IOCTL_REGISTER_CALLBACK:
	case DIAG_IOCTL_MDM_HDLC_TOGGLE:
	case DIAG_IOCTL_HDLC_TOGGLE:
	case DIAG_IOCTL_UPDATE_QDSS_ETR1_SUP:
		err = __ioctl(request, NULL, 0, buf, len);
		break;
	default:
		err = -EINVAL;
		break;
	}

	if (err >= 0)
		return 0;
	else
		return err;
}

/*===========================================================================

FUNCTION diag_lsm_comm_write

DESCRIPTION
  If /dev/diag exists calls write to kernel diag driver else tries to send
  data to diaghal server

DEPENDENCIES
   None

RETURN VALUE
  SUCCESS/FAILURE.

SIDE EFFECTS
  None.
===========================================================================*/
int diag_lsm_comm_write(int fd, unsigned char buf[], int bytes)
{
	int bytes_written = 0;

	if (diag_use_dev_node)
		return write(fd,(const void *)buf, bytes);

	if (diaghalHidl != nullptr)
		return diag_lsm_comm_write_hidl(fd, buf, bytes);

	if (diaghalAidl != nullptr)
		return diag_lsm_comm_write_aidl(fd, buf, bytes);

	return -1;
}

/*===========================================================================

FUNCTION diag_lsm_comm_read

DESCRIPTION
  If /dev/diag exists calls read to kernel diag driver else tries to read
  data sent by diaghal server to client over callback.

DEPENDENCIES
   None

RETURN VALUE
  SUCCESS/FAILURE.

SIDE EFFECTS
  None.
===========================================================================*/
int diag_lsm_comm_read()
{
	int num_bytes_read = 0;

	pthread_mutex_lock(&read_data_mutex);
	while (!read_buf_busy) {
		pthread_cond_wait(&read_data_cond, &read_data_mutex);
	}
	if (diag_lsm_kill) {
		pthread_mutex_unlock(&read_data_mutex);
		DIAG_LOGE("diag: %s: exit\n", __func__);
		errno = ECANCELED;
		return -1;
	}
	if (diaghalHidl == nullptr && diaghalAidl == nullptr) {
		pthread_mutex_unlock(&read_data_mutex);
		DIAG_LOGE("diag: %s: HAL interface is down\n", __func__);
		return 0;
	}

	num_bytes_read = data_len;
	if (*(int *)read_buffer == DEINIT_TYPE) {
		read_buf_busy = 0;
		pthread_mutex_unlock(&read_data_mutex);
		errno = ESHUTDOWN;
		return -1;
	}
	process_diag_payload(num_bytes_read);
	pthread_mutex_unlock(&read_data_mutex);
	pthread_mutex_lock(&write_data_mutex);
	read_buf_busy = 0;
	pthread_cond_signal(&write_data_cond);
	pthread_mutex_unlock(&write_data_mutex);

	return num_bytes_read;
}

void diag_kill_comm_read(void)
{
	pthread_mutex_lock(&read_data_mutex);
	DIAG_LOGE("%s: signalling diaghal read thread exit\n", __func__);
	pthread_cond_signal(&read_data_cond);
	pthread_mutex_unlock(&read_data_mutex);

	pthread_mutex_lock(&write_data_mutex);
	DIAG_LOGE("%s: signalling diaghal write thread exit\n", __func__);
	read_buf_busy = 0;
	pthread_cond_signal(&write_data_cond);
	pthread_mutex_unlock(&write_data_mutex);
}

/*===========================================================================

FUNCTION diag_process_data

DESCRIPTION
  Process the data received on callback from diaghal server and signal the read
  thread to read the data.

DEPENDENCIES
   None

RETURN VALUE
  SUCCESS/FAILURE.

SIDE EFFECTS
  None.
===========================================================================*/
int diag_process_data(unsigned char *data, int len)
{

	pthread_mutex_lock(&write_data_mutex);
	while (read_buf_busy) {
		pthread_cond_wait(&write_data_cond, &write_data_mutex);
	}
	if (diag_lsm_kill) {
		pthread_mutex_unlock(&write_data_mutex);
		DIAG_LOGE("%s: exit\n", __func__);
		return 0;
	}
	memcpy(read_buffer, data, len);
	data_len = len;
	read_buf_busy = 1;
	pthread_mutex_unlock(&write_data_mutex);
	pthread_mutex_lock(&read_data_mutex);
	pthread_cond_signal(&read_data_cond);
	pthread_mutex_unlock(&read_data_mutex);

	return 0;
}

int diag_lsm_comm_active(void)
{
	if (diaghalHidl || diaghalAidl)
		return 1;
	return 0;
}
