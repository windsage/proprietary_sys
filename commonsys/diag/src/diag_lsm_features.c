/*====*====*====*====*====*====*====*====*====*====*====*====*====*====*====*
Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
All rights reserved.
Confidential and Proprietary - Qualcomm Technologies, Inc.

Diag Features Support
 1. Diagid support
 2. HDLC toggling support
 3. Feature Query support

GENERAL DESCRIPTION

Library definition for using peripheral diag_id information
by sending diag command request/responses.

*====*====*====*====*====*====*====*====*====*====*====*====*====*====*====*/
#include <stdlib.h>
#include "comdef.h"
#include "stdio.h"
#include "stringl.h"
#include "diag_lsmi.h"
#include "diag_shared_i.h"
#include "stdio.h"
#include "string.h"
#include "diag_lsm.h"
#include "diagdiag.h"
#include <malloc.h>
#include <unistd.h>
#include <sys/stat.h>
#include <dirent.h>
#include <sys/types.h>
#include <sys/time.h>
#include <sys/ioctl.h>
#include <signal.h>
#include <time.h>
#include <stdlib.h>
#include <getopt.h>
#include <fcntl.h>
#include <ctype.h>
#include <limits.h>
#include <pthread.h>
#include <stdint.h>
#include "diagcmd.h"
#include "errno.h"

#ifndef FEATURE_LE_DIAG
#include <cutils/log.h>
#endif

#define DIAG_RSP_BUF_SIZE 100
#define DIAG_CMD_REQ_BUF_SIZE	50

typedef enum {
	DIAG_ID_PARSER_STATE_OFF = 0,
	DIAG_ID_PARSER_STATE_ON,
} diag_id_parser_state;

int diag_feature_state = 0;

struct diag_read_buf_pool {
	unsigned char* rsp_buf;
	int data_ready;
	pthread_mutex_t rsp_mutex;
	pthread_cond_t rsp_cond;
};

static unsigned int diagid_device_mask;
static uint8_t diagid_debug_flag = 0;
static struct diag_read_buf_pool diag_read_buffer_pool;

int diagid_kill_thread = 0;
int hdlc_toggle_status = 0;
int qshrink4_check[NUM_PERIPHERALS] = {0, 0, 0, 0, 0, 0, 0, 0};
int diagid_query_status[NUM_PROC] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

int diagid_cmd_flag = 0;
int hdlc_cmd_flag = 0;
int feature_query_cmd_flag = 0;

int hw_accel_feature_support[NUM_PROC] = {0};
int hw_accel_dyn_atid_flag_f[NUM_PROC] = {0};
int diagid_async_support_flag_f[NUM_PROC] = {0};

pthread_t diag_features_thread_hdl;

diag_id_list *diag_id_head[NUM_PROC];

unsigned char feature_buf[DIAG_CMD_REQ_BUF_SIZE];

extern int disable_hdlc;

int diag_send_hdlc_toggle(int peripheral_type, uint8 hdlc_support)
{
	diag_hdlc_toggle_cmd *req;
	int offset = 0, length = 0, ret = 0;
	unsigned char *ptr = feature_buf;
	(void)hdlc_support;

	memset(feature_buf, 0, DIAG_CMD_REQ_BUF_SIZE);

	if (peripheral_type < MSM || peripheral_type > MDM_2) {
		DIAG_LOGE("diag:%s Invalid peripheral_type = %d to toggle hdlc\n",
			__func__, peripheral_type);
		return FALSE;
	}

	*(int*)ptr = USER_SPACE_RAW_DATA_TYPE;
	offset += sizeof(int);
	if (peripheral_type) {
		*(int*)(ptr + offset) = -peripheral_type;
		offset += sizeof(int);
	}
	ptr = ptr + offset;
	req = (diag_hdlc_toggle_cmd*)ptr;

	/*************************************************
	 * 	0x4B 0x12 0x18 0x02 			*
	 *						*
	 * 	HDLC Toggle cmd				*
	 *************************************************/

	req->header.cmd_code = DIAG_SUBSYS_CMD_F;
	req->header.subsys_id = DIAG_SUBSYS_DIAG_SERV;
	req->header.subsys_cmd_code = DIAG_CMD_OP_HDLC_DISABLE;
	req->version = 1;
	length = sizeof(diag_hdlc_toggle_cmd);

	if (length + offset <= DIAG_CMD_REQ_BUF_SIZE) {
		ret = diag_send_data(feature_buf, offset + length);
		if (ret)
			return FALSE;
	}

	return TRUE;

}

static int wait_for_response()
{
	struct timespec time;
	struct timeval now;
	int rt = 0;

	gettimeofday(&now, NULL);
	time.tv_sec = now.tv_sec + 10000 / 1000;
	time.tv_nsec = now.tv_usec + (10000 % 1000) * 1000000;
	pthread_mutex_lock(&(diag_read_buffer_pool.rsp_mutex));
	if (!diag_read_buffer_pool.data_ready)
		rt = pthread_cond_timedwait(&(diag_read_buffer_pool.rsp_cond),
			&(diag_read_buffer_pool.rsp_mutex), &time);
	diag_read_buffer_pool.data_ready = 0;
	pthread_mutex_unlock(&(diag_read_buffer_pool.rsp_mutex));
	return rt;
}

int diag_features_setup_init(void)
{
	pthread_mutex_init(&(diag_read_buffer_pool.rsp_mutex), NULL);
	pthread_cond_init(&(diag_read_buffer_pool.rsp_cond), NULL);

	diag_read_buffer_pool.data_ready = 0;
	diag_read_buffer_pool.rsp_buf = malloc(DIAG_CMD_RSP_BUF_SIZE);
	if (!diag_read_buffer_pool.rsp_buf){
		DIAG_LOGE("%s:failed to create rsp buffer\n", __func__);
		return -1;
	}
	return 0;
}

static int diag_query_pd_name(char *process_name, char *search_str)
{
	if (!process_name)
		return -EINVAL;

	if (strstr(process_name, search_str))
		return 1;

	return 0;
}

int diag_query_pd(char *process_name)
{
	if (!process_name)
		return -EINVAL;

	if (diag_query_pd_name(process_name, "APPS"))
		return DIAG_APPS_PROC;
	if (diag_query_pd_name(process_name, "Apps"))
		return DIAG_APPS_PROC;
	if (diag_query_pd_name(process_name, "modem/root_pd"))
		return DIAG_MODEM_PROC;
	if (diag_query_pd_name(process_name, "adsp/root_pd"))
		return DIAG_LPASS_PROC;
	if (diag_query_pd_name(process_name, "wpss/root_pd"))
		return DIAG_WCNSS_PROC;
	if (diag_query_pd_name(process_name, "slpi/root_pd"))
		return DIAG_SENSORS_PROC;
	if (diag_query_pd_name(process_name, "cdsp/root_pd"))
		return DIAG_CDSP_PROC;
	if (diag_query_pd_name(process_name, "npu/root_pd"))
		return DIAG_NPU_PROC;
	if (diag_query_pd_name(process_name, "cdsp1/root_pd"))
		return DIAG_NSP1_PROC;
	if (diag_query_pd_name(process_name, "gpdsp/root_pd"))
		return DIAG_GPDSP0_PROC;
	if (diag_query_pd_name(process_name, "gpdsp1/root_pd"))
		return DIAG_GPDSP1_PROC;
	if (diag_query_pd_name(process_name, "soccp/root_pd"))
		return DIAG_SOCCP_PROC;
	if (diag_query_pd_name(process_name, "wlan_pd"))
		return UPD_WLAN;
	if (diag_query_pd_name(process_name, "audio_pd"))
		return UPD_AUDIO;
	if (diag_query_pd_name(process_name, "sensor_pd"))
		return UPD_SENSORS;
	if (diag_query_pd_name(process_name, "charger_pd"))
		return UPD_CHARGER;
	if (diag_query_pd_name(process_name, "oem_pd"))
		return UPD_OEM;

	return -EINVAL;
}

int diag_query_peripheral(char *process_name)
{
	if (!process_name)
		return -EINVAL;

	if (diag_query_pd_name(process_name, "APPS"))
		return DIAG_APPS_PROC;
	if (diag_query_pd_name(process_name, "Apps"))
		return DIAG_APPS_PROC;
	if (diag_query_pd_name(process_name, "modem"))
		return DIAG_MODEM_PROC;
	if (diag_query_pd_name(process_name, "adsp"))
		return DIAG_LPASS_PROC;
	if (diag_query_pd_name(process_name, "wpss"))
		return DIAG_WCNSS_PROC;
	if (diag_query_pd_name(process_name, "slpi"))
		return DIAG_SENSORS_PROC;
	if (diag_query_pd_name(process_name, "cdsp"))
		return DIAG_CDSP_PROC;
	if (diag_query_pd_name(process_name, "npu"))
		return DIAG_NPU_PROC;
	if (diag_query_pd_name(process_name, "cdsp1"))
		return DIAG_NSP1_PROC;
	if (diag_query_pd_name(process_name, "gpdsp"))
		return DIAG_GPDSP0_PROC;
	if (diag_query_pd_name(process_name, "gpdsp1"))
		return DIAG_GPDSP1_PROC;
	if (diag_query_pd_name(process_name, "soccp"))
		return DIAG_SOCCP_PROC;
	return -EINVAL;
}

diag_id_list *get_diag_id(int peripheral_type, int pd)
{
	diag_id_list *item = NULL;

	if (pd <  DIAG_MODEM_PROC || pd >  TOTAL_PD_COUNT)
		return 0;

	item = diag_id_head[peripheral_type];

	while (item != NULL) {
		if (pd == item->pd)
			return item;

		item = item->next;
	}
	return 0;
}

int get_peripheral_by_pd(int peripheral_type, int pd)
{
	diag_id_list *item = NULL;

	if (pd < DIAG_MODEM_PROC || pd > TOTAL_PD_COUNT)
		return -1;

	item = diag_id_head[peripheral_type];

	while (item != NULL) {
		if (pd == item->pd)
			return item->peripheral;

		item = item->next;
	}
	return -1;
}

void insert_diag_id_entry(diag_id_entry_struct *entry, int peripheral_type)
{
	diag_id_list *new_entry = NULL, *temp = NULL;

	new_entry = malloc(sizeof(diag_id_list));
	if (!new_entry)
		return;

	new_entry->diag_id = entry->diag_id;
	strlcpy(new_entry->process_name, &entry->process_name, MAX_DIAGID_STR_LEN);
	new_entry->pd = diag_query_pd(new_entry->process_name);
	new_entry->peripheral = diag_query_peripheral(new_entry->process_name);

	if ((peripheral_type == MSM) && (new_entry->peripheral == DIAG_MODEM_PROC))
		qshrink4_check[new_entry->peripheral] = 1;

	if (diagid_debug_flag) {
		printf("\tPD: %d\n", new_entry->pd);
		printf("\tPeripheral: %d\n", new_entry->peripheral);
		printf("\tProcess_name: %s\n", new_entry->process_name);
	}

	new_entry->next = NULL;
	if (peripheral_type >= 0) {
		if (diag_id_head[peripheral_type] == NULL) {
			diag_id_head[peripheral_type] = new_entry;
		} else {
			temp = diag_id_head[peripheral_type];
			while (temp->next != NULL) {
				temp = (diag_id_list *)temp->next;
			}
			temp->next = new_entry;
		}
	}
}

int process_diag_id_response(int peripheral_type)
{
	diag_id_entry_struct *diag_id_ptr;
	int i = 0, ret, offset;
	uint8 len;
	diag_id_list_rsp* rsp;
	unsigned char *buf_ptr;
	uint8 *temp_ptr = NULL;

	buf_ptr = diag_read_buffer_pool.rsp_buf;

	if (buf_ptr[0] == DIAG_BAD_CMD_F) {
		ret = FALSE;
	} else {
		rsp = (diag_id_list_rsp*)buf_ptr;
		if ((rsp->version != 1) || (rsp->header.subsys_cmd_code != 0x222))
			return FALSE;

		if (diagid_debug_flag) {
			printf("\n\n============Printing DIAG ID INFO===========\n");
			printf("\tVERSION: %d\n", rsp->version);
			printf("\tNUM_ENTRIES: %d\n", rsp->num_entries);
		}

		diag_id_ptr = &rsp->entry;
		for (i = 0; i < rsp->num_entries; i++) {
			temp_ptr = (uint8 *)diag_id_ptr;
			len = *(uint8 *)(temp_ptr + sizeof(uint8));
			if (diagid_debug_flag) {
				printf("\n----------Entry: %d---------------\n", i+1);
				printf("\tDIAG_ID: %d\n", diag_id_ptr->diag_id);
				printf("\tNAME_LENGTH: %d\n", diag_id_ptr->len);
			}
			insert_diag_id_entry(diag_id_ptr, peripheral_type);
			offset = (2 * sizeof(uint8)) + len;
			diag_id_ptr = (diag_id_entry_struct *)(temp_ptr + offset);
		}
		if (diagid_debug_flag)
			printf("\n=================DONE=====================\n\n");
		ret = TRUE;
		diagid_cmd_flag = 0;
	}

	if ((peripheral_type == MSM) && (!qshrink4_check[DIAG_MODEM_PROC]))
		diagid_set_qshrink4_status(MSM);
	return ret;
}

int diag_query_diag_id(int peripheral_type)
{
	diag_id_list_req *req;
	int offset = 0, length = 0, ret = 0;
	unsigned char *ptr = feature_buf;

	memset(feature_buf, 0, DIAG_CMD_REQ_BUF_SIZE);

	if (peripheral_type < MSM || peripheral_type > MDM_2) {
		DIAG_LOGE("diag:%s Invalid peripheral_type = %d to query diag_id\n",
			__func__, peripheral_type);
		return FALSE;
	}

	*(int*)ptr = USER_SPACE_RAW_DATA_TYPE;
	offset += sizeof(int);
	if (peripheral_type) {
		*(int*)(ptr + offset) = -peripheral_type;
		offset += sizeof(int);
	}
	ptr = ptr + offset;
	req = (diag_id_list_req*)ptr;

	/*************************************************
	 * 	0x4B 0x12 0x22 0x02 			*
	 *						*
	 * 	Diagid Table Query cmd			*
	 *************************************************/
	req->header.cmd_code = DIAG_SUBSYS_CMD_F;
	req->header.subsys_id = DIAG_SUBSYS_DIAG_SERV;
	req->header.subsys_cmd_code = DIAG_GET_DIAG_ID;
	req->version = 1;
	length = sizeof(diag_id_list_req);

	if (length + offset <= DIAG_CMD_REQ_BUF_SIZE) {
		ret = diag_send_data(feature_buf, offset + length);
		if (ret)
			return FALSE;
	}

	return TRUE;
}

int diag_get_peripheral_diag_id_info(int peripheral_type)
{
	int err, status;

	if (diagid_kill_thread)
		return 0;

	diag_feature_state = 1;
	diag_read_buffer_pool.data_ready = 0;

	err = diag_query_diag_id(peripheral_type);
	if (err == FALSE || diagid_kill_thread) {
		DIAG_LOGE("diag: %s, Failure to send diag_id query command to proc: %d\n",
			__func__, peripheral_type);
		diag_feature_state = 0;
		exit(-1);
	}

	err = wait_for_response();
	if (err == ETIMEDOUT || diagid_kill_thread) {
		DIAG_LOGE("diag:%s time out while waiting for diag_id cmd response for p_type:%d\n",
			__func__, peripheral_type);
		diag_feature_state = 0;
		exit(-1);
	}
	diagid_query_status[peripheral_type] = 1;
	return 0;
}

void diag_toggle_hdlc_status(int peripheral_type)
{
	int err, status;

	if (diagid_kill_thread)
		return;

	diag_feature_state = 1;
	diag_read_buffer_pool.data_ready = 0;

	err = diag_send_hdlc_toggle(peripheral_type, HDLC_DISABLE);
	if (err == FALSE || diagid_kill_thread) {
		DIAG_LOGE("diag: %s, Failure to send hdlc toggle cmd to proc: %d\n",
			__func__, peripheral_type);
		diag_feature_state = 0;
		hdlc_toggle_status = -1;
		exit(-1);
	}

	err = wait_for_response();
	if (err == ETIMEDOUT || diagid_kill_thread) {
		DIAG_LOGE("diag:%s time out while waiting for hdlc toggle cmd response for p_type:%d\n",
			__func__, peripheral_type);
		pthread_mutex_unlock(&(diag_read_buffer_pool.rsp_mutex));
		diag_feature_state = 0;
		hdlc_toggle_status = -1;
		exit(-1);
	}

	if (!peripheral_type) {
		status = diag_hdlc_toggle(HDLC_DISABLE);
		if (status == 1) {
			DIAG_LOGE("diag: HDLC successfully disabled for MSM\n");
		} else {
			DIAG_LOGE("diag: Unable to switch the HDLC for MSM, exiting app\n");
			diag_feature_state = 0;
			hdlc_toggle_status = -1;
			exit(-1);
		}
	} else {
		status = diag_hdlc_toggle_mdm(HDLC_DISABLE, peripheral_type);
		if (status == 1) {
			DIAG_LOGE("diag: HDLC successfully disabled for proc: %d\n", peripheral_type);
		} else {
			DIAG_LOGE("diag: Unable to switch the HDLC for proc: %d, exiting app\n", peripheral_type);
			diag_feature_state = 0;
			hdlc_toggle_status = -1;
			exit(-1);
		}
	}
}

int diag_query_diag_features(int peripheral_type, int feature_flag)
{
	switch(feature_flag) {
		case F_DIAG_HW_ACCELERATION:
			return hw_accel_feature_support[peripheral_type];
		case F_DIAG_DYNAMIC_ATID:
			return hw_accel_dyn_atid_flag_f[peripheral_type];
		case F_DIAG_DIAGID_BASED_ASYNC_PKT:
			return diagid_async_support_flag_f[peripheral_type];
		default:
			return 0;
	}
}

int diag_send_configure_new_pkt_cmd(int peripheral_type)
{
	diag_cmd_pkt_format_select_req *req;
	int offset = 0, length = 0, ret = 0;
	unsigned char *ptr = feature_buf;

	if (peripheral_type < MSM || peripheral_type > MDM_2) {
		DIAG_LOGE("diag:%s: failure in sending cmd for p_type = %d\n", __func__, peripheral_type);
		return FALSE;
	}

	*(int*)ptr = USER_SPACE_RAW_DATA_TYPE;
	offset += sizeof(int);
	if (peripheral_type) {
		*(int*)(ptr + offset) = -peripheral_type;
		offset += sizeof(int);
	}
	ptr = ptr + offset;
	req = (diag_cmd_pkt_format_select_req*)ptr;

	/*************************************************
	 * 	0x4B 0x12 0x32 0x02 			*
	 *						*
	 * 	Configure new packet format cmd		*
	 *************************************************/
	req->header.cmd_code = DIAG_SUBSYS_CMD_F;
	req->header.subsys_id = DIAG_SUBSYS_DIAG_SERV;
	req->header.subsys_cmd_code = DIAG_DIAG_SUBSYS_CMD_CONFIGURE_NEW_PKT;
	req->version = 1;
	req->pkt_format_mask = PKT_FORMAT_MASK_ASYNC_PKT;
	length = sizeof(diag_cmd_pkt_format_select_req);

	if (length + offset <= DIAG_CMD_REQ_BUF_SIZE) {
		ret = diag_send_data(feature_buf, offset + length);
		if (ret)
			return FALSE;
	}
	return TRUE;
}


int process_diag_feature_mask_response(int peripheral_type)
{
	diag_feature_query_rsp* rsp;
	unsigned char* buf_ptr;
	int i, ret = 0;
	(void)peripheral_type;

	buf_ptr = diag_read_buffer_pool.rsp_buf;
	if (buf_ptr[0] == DIAG_BAD_CMD_F || buf_ptr[0] == DIAG_BAD_PARM_F || buf_ptr[0] == DIAG_BAD_LEN_F) {
		return FALSE;
	} else {
		rsp = (diag_feature_query_rsp*)buf_ptr;
		if ((rsp->version != 1) || (rsp->header.subsys_cmd_code != DIAG_DIAG_FEATURE_QUERY)) {
			return FALSE;
		}

		if (rsp->feature_mask[0] & ( 1 << F_DIAG_HW_ACCELERATION))
			hw_accel_feature_support[peripheral_type] = 1;

		if (rsp->feature_mask[0] & ( 1 << F_DIAG_DYNAMIC_ATID))
			hw_accel_dyn_atid_flag_f[peripheral_type] = 1;

		if (rsp->feature_mask[0] & ( 1 << F_DIAG_DIAGID_BASED_ASYNC_PKT))
			diagid_async_support_flag_f[peripheral_type] = 1;

		/* reset the feature query cmd status flag */
		feature_query_cmd_flag = 0;

		return TRUE;
	}
}

int diag_send_query_feature_mask_cmd(int peripheral_type)
{
	diag_feature_query_req *req;
	int offset = 0, length = 0, ret = 0;
	unsigned char* ptr = feature_buf;

	if (peripheral_type < MSM || peripheral_type > MDM_2) {
		DIAG_LOGE("diag:%s cmd sent failed for  peripheral_type = %d\n", __func__, peripheral_type);
		return FALSE;
	}

	*(int*)ptr = USER_SPACE_RAW_DATA_TYPE;
	offset += sizeof(int);
	if (peripheral_type) {
		*(int*)(ptr + offset) = -peripheral_type;
		offset += sizeof(int);
	}
	ptr = ptr + offset;
	req = (diag_feature_query_req*)ptr;

	/*************************************************
	 * 	0x4B 0x12 0x25 0x02 			*
	 *						*
	 * 	QUERY FEATURE MASK cmd			*
	 *************************************************/
	req->header.cmd_code = DIAG_SUBSYS_CMD_F;
	req->header.subsys_id = DIAG_SUBSYS_DIAG_SERV;
	req->header.subsys_cmd_code = DIAG_DIAG_FEATURE_QUERY;
	req->version = 1;
	length = sizeof(diag_feature_query_req);

	if (length + offset <= DIAG_CMD_REQ_BUF_SIZE) {
		ret = diag_send_data(feature_buf, offset + length);
		if (ret)
			return FALSE;
	}
	return TRUE;
}

int diag_query_feature_mask(int peripheral_type)
{
	int err;

	if (diagid_kill_thread)
		return 0;

	diag_feature_state = 1;
	diag_read_buffer_pool.data_ready = 0;

	err = diag_send_query_feature_mask_cmd(peripheral_type);
	if (err == FALSE || diagid_kill_thread) {
		DIAG_LOGE("diag: %s, Failure to send feature mask cmd to proc: %d\n",
			__func__, peripheral_type);
		diag_feature_state = 0;
		return -1;
	}

	err = wait_for_response();
	if (err == ETIMEDOUT || diagid_kill_thread) {
		DIAG_LOGE("diag:%s time out while waiting for feature mask cmd response for p_type:%d\n",
			__func__, peripheral_type);
		pthread_mutex_unlock(&(diag_read_buffer_pool.rsp_mutex));
		diag_feature_state = 0;
		return -1;
	}

	return 0;
}

static void* diag_features_init(void *param)
{
	(void)param;

	int i, ret;
	uint16 remote_mask = 0;

	if (diagid_kill_thread)
		return 0;

	if (diagid_device_mask & (1 << MSM)) {

		if (disable_hdlc)
			diag_toggle_hdlc_status(MSM);

		diag_get_peripheral_diag_id_info(MSM);

		ret = diag_query_feature_mask(MSM);
		if (ret < 0) {
			DIAG_LOGE("diag: %s: Feature query failure for MSM, continuing");
		}
		if (diagid_async_support_flag_f[MSM])
			diag_send_configure_new_pkt_cmd(MSM);
	}

	diag_has_remote_device(&remote_mask);
	if (remote_mask) {
		for (i = 1; i < NUM_PROC; i++) {
			if (diagid_kill_thread)
				break;
			if (diagid_device_mask &
					(remote_mask & (1 << (i - 1))) << 1) {

				if (disable_hdlc)
					diag_toggle_hdlc_status(i);

				diag_get_peripheral_diag_id_info(i);

				ret = diag_query_feature_mask(i);
				if (ret < 0) {
					DIAG_LOGE("diag: %s: Feature query failure for p_type : %d, continuing", i);
				}
				if (diagid_async_support_flag_f[i])
					diag_send_configure_new_pkt_cmd(i);
			}
		}
	}
	return 0;
}

void diag_wait_for_features_init()
{
	pthread_join(diag_features_thread_hdl, NULL);
}

void diag_setup_features_init(void)
{
	pthread_create(&diag_features_thread_hdl, NULL, diag_features_init, NULL);
	if (diag_features_thread_hdl == 0)
		DIAG_LOGE("%s: Failed to create features thread", __func__);
}

static int check_for_diag_features_cmd(uint8* src_ptr, uint8 *hdlc_cmd)
{
	uint16 cmd_code;

	if (!src_ptr)
		return FALSE;

	if (((*src_ptr == DIAG_SUBSYS_CMD_F && *(src_ptr + 1) == DIAG_SUBSYS_DIAG_SERV) ||
		 (((*src_ptr == DIAG_BAD_CMD_F) || (*src_ptr == DIAG_BAD_LEN_F) || (*src_ptr == DIAG_BAD_PARM_F)) &&
		 *(src_ptr + 1) == DIAG_SUBSYS_CMD_F && *(src_ptr + 2) == DIAG_SUBSYS_DIAG_SERV)))
	{
		if (*src_ptr == DIAG_SUBSYS_CMD_F) {
			memcpy(&cmd_code, src_ptr + 2, sizeof(cmd_code));
		} else {
			memcpy(&cmd_code, src_ptr + 3, sizeof(cmd_code));
		}

		switch (cmd_code) {
		case DIAG_GET_DIAG_ID:
			diagid_cmd_flag = 1;
			break;
		case DIAG_CMD_OP_HDLC_DISABLE:
			if (*src_ptr == DIAG_SUBSYS_CMD_F)
				*hdlc_cmd = 1;
			break;
		case DIAG_DIAG_FEATURE_QUERY:
			feature_query_cmd_flag = 1;
			break;
		case DIAG_DIAG_SUBSYS_CMD_CONFIGURE_NEW_PKT:
			break;
		default:
			return FALSE;
		}
		return TRUE;
	} else {
		return FALSE;
	}
}

void diag_set_hdlc_status(int hdlc_status)
{
	disable_hdlc = hdlc_status;
}

void diag_set_diagid_mask(unsigned int diag_device_mask)
{
	diagid_device_mask = diag_device_mask;
}

void process_diag_features_response(int peripheral_type)
{
	if (diagid_cmd_flag)
		process_diag_id_response(peripheral_type);
	if (feature_query_cmd_flag)
		process_diag_feature_mask_response(peripheral_type);
}

int parse_data_for_diag_features_rsp(uint8* ptr, int count_received_bytes, int index, int *update_count)
{
	unsigned char* dest_ptr = NULL;
	unsigned int src_length = 0, dest_length = 0;
	unsigned int len = 0, i;
	unsigned char hdlc_copy[DIAG_CMD_RSP_BUF_SIZE];
	uint8 src_byte, hdlc_cmd = 0;
	uint8* src_ptr = NULL;
	uint16 payload_len = 0, hdlc_payload_len = 0;
	uint16_t packet_len = 0;
	int bytes_read = 0;
	unsigned char non_hdlc_header[DIAG_NON_HDLC_HEADER_SIZE] = { CONTROL_CHAR, NON_HDLC_VERSION, 0, 0 };
	unsigned char end_byte[1] = { CONTROL_CHAR };

	if (diagid_kill_thread)
		return -1;

	while (bytes_read < count_received_bytes) {

		src_ptr = ptr + bytes_read;
		src_length = count_received_bytes - bytes_read;

		if (*src_ptr == CONTROL_CHAR && src_length >= DIAG_MIN_NON_HDLC_PKT_SIZE) {
			packet_len = (uint16_t)(*(uint16_t *)(src_ptr + 2));
			if (*(src_ptr + DIAG_NON_HDLC_HEADER_SIZE + packet_len) == CONTROL_CHAR)
				hdlc_disabled[index] = 1;
		}

		if (hdlc_disabled[index]) {
			payload_len = *(uint16 *)(src_ptr + 2);
			if (check_for_diag_features_cmd(src_ptr + 4, &hdlc_cmd))
			{
				diag_feature_state = 0;
				dest_ptr = diag_read_buffer_pool.rsp_buf;
				dest_length = DIAG_CMD_RSP_BUF_SIZE;
				if (payload_len <= DIAG_CMD_RSP_BUF_SIZE)
					memcpy(dest_ptr, src_ptr + 4, payload_len);
				else
					return -1;

				process_diag_features_response(index);

				diag_read_buffer_pool.data_ready = 1;
				pthread_cond_signal(&(diag_read_buffer_pool.rsp_cond));
				pthread_mutex_unlock(&(diag_read_buffer_pool.rsp_mutex));
				bytes_read += payload_len + 5;
			} else {
				bytes_read += payload_len + 5;
			}
		} else {
			if (check_for_diag_features_cmd(src_ptr, &hdlc_cmd)) {
				if (hdlc_cmd) {
					/*
					 * Actual length of the payload in the packet is
					 * packet length minus
					 * 2 bytes CRC and one control char (7E)
					 */
					hdlc_payload_len = count_received_bytes - 3;
					memcpy(hdlc_copy, ptr, hdlc_payload_len);
				}
				diag_feature_state = 0;
				dest_ptr = diag_read_buffer_pool.rsp_buf;
				dest_length = DIAG_CMD_RSP_BUF_SIZE;
				for (i = 0; i < src_length; i++) {
					src_byte = src_ptr[i];
					if (src_byte == ESC_CHAR) {
						if (i == (src_length - 1)) {
							i++;
							break;
						} else {
							dest_ptr[len++] = src_ptr[++i] ^ 0x20;
						}
					} else if (src_byte == CTRL_CHAR) {
						if (i == 0 && src_length > 1)
							continue;
						dest_ptr[len++] = src_byte;
						i++;
						break;
					} else {
						dest_ptr[len++] = src_byte;
					}

					if (len >= dest_length) {
						i++;
						break;
					}
				}
				bytes_read += i;

				process_diag_features_response(index);

				if (hdlc_cmd) {
					/* Copy the payload length into the non hdlc header */
					memcpy(non_hdlc_header + 2, &hdlc_payload_len, 2);
					/* Copy the non hdlc header to the pkt head position */
					memcpy(ptr, non_hdlc_header, DIAG_NON_HDLC_HEADER_SIZE);
					/* Copy the pkt after non hdlc header */
					memcpy(ptr + DIAG_NON_HDLC_HEADER_SIZE, hdlc_copy, hdlc_payload_len);
					/* Copy the Control character to the end position */
					memcpy((ptr + DIAG_NON_HDLC_HEADER_SIZE + hdlc_payload_len), end_byte, 1);
					/* Updating the packet length since packet
					 * has been converted to non-hdlc packets
					 * updated length would be
					 * (Non-hdlc header + control char) - (HDLC CRC 2 bytes + control char)
					 */
					*update_count = DIAG_NON_HDLC_HEADER_SIZE + 1 - (HDLC_CRC_LEN + 1);
					hdlc_disabled[index] = disable_hdlc;
				}

				i = 0;
				len = 0;
				diag_read_buffer_pool.data_ready = 1;
				pthread_cond_signal(&(diag_read_buffer_pool.rsp_cond));
				pthread_mutex_unlock(&(diag_read_buffer_pool.rsp_mutex));
			} else {
				for (i = 0; i < src_length; i++) {
					if (src_ptr[i] == CTRL_CHAR) {
						i++;
						break;
					}
				}
				bytes_read += i;
				i = 0;
				len = 0;
			}
		}
	}
	return 0;
}

void diag_kill_feature_setup_threads(void)
{
	diagid_kill_thread = 1;
	DIAG_LOGE("diag: %s: Initiate diagid threads kill (diagid_kill_thread: %d)\n",
		__func__, diagid_kill_thread);

	pthread_cond_signal(&(diag_read_buffer_pool.rsp_cond));
	pthread_mutex_unlock(&(diag_read_buffer_pool.rsp_mutex));

	sleep(1);

	pthread_cond_destroy(&(diag_read_buffer_pool.rsp_cond));
	pthread_mutex_destroy(&(diag_read_buffer_pool.rsp_mutex));

	if (diag_read_buffer_pool.rsp_buf)
		free(diag_read_buffer_pool.rsp_buf);

	DIAG_LOGE("diag:In %s Cleaned up diagid resources\n", __func__);
}
