/*====*====*====*====*====*====*====*====*====*====*====*====*====*====*====*
Copyright (c) 2020 Qualcomm Technologies, Inc.
All Rights Reserved.
Confidential and Proprietary - Qualcomm Technologies, Inc.

              Diag communication support

GENERAL DESCRIPTION

Implementation of diag hidl communication layer between diag library and diag driver
for asynchronous callbacks from diag hidl server to client.

*====*====*====*====*====*====*====*====*====*====*====*====*====*====*====*/
#include <stdlib.h>
#include "comdef.h"
#include "stdio.h"
#include "diagcallback.h"
#include "diag_lsm.h"

#include <sys/mman.h>

namespace vendor {
namespace qti {
namespace diaghal {
namespace V1_0 {
namespace implementation {

// Methods from ::vendor::qti::diaghal::V1_0::Idiagcallback follow.
Return<int32_t> diagcallback::send_data(const hidl_memory& buf, uint32_t len) {
	sp<IMemory> memory;
	unsigned char *data;

	memory = mapMemory(buf);
	if (memory == nullptr)
		return 0;
	data = static_cast<unsigned char*>(static_cast<void*>(memory->getPointer()));
	diag_process_data(data,len);
	return 0;
}

Idiagcallback *diagcallback::getInstance(void){
    return new diagcallback();
}

}}}}
}  // namespace vendor::qti::diaghal::implementation

namespace aidl {
namespace vendor {
namespace qti {
namespace diaghal {

ScopedAStatus Idiagcallback::send_data(const ParcelableMemory& in_mem, int32_t in_len, int32_t* _aidl_return)
{
	void *data;

	*_aidl_return = 0;
	data = mmap(nullptr, in_mem.file.length, PROT_READ | PROT_WRITE, MAP_SHARED, in_mem.file.fd.get(), 0);
	if (data == nullptr || data == (void*) -1)
		return ScopedAStatus::ok();

	diag_process_data((unsigned char *)data, (int)in_len);

	munmap(data, in_mem.file.length);
	return ScopedAStatus::ok();
}

}  // namespace diaghal
}  // namespace qti
}  // namespace vendor
}  // namespace aidl