#ifndef DIAG_LSMI_H
#define DIAG_LSMI_H

/*===========================================================================

                   Diag Mapping Layer DLL , internal declarations

DESCRIPTION
  Internal declarations for Diag Service Mapping Layer.

Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
All rights reserved.
Confidential and Proprietary - Qualcomm Technologies, Inc.
===========================================================================*/

/*===========================================================================

                        EDIT HISTORY FOR MODULE

This section contains comments describing changes made to the module.
Notice that changes are listed in reverse chronological order.

$Header:

when       who     what, where, why
--------   ---     ----------------------------------------------------------
10/01/08   sj      Added featurization for WM specific code & CBSP2.0
02/04/08   mad     Created File
===========================================================================*/

#include <diag_lsm.h>
#include <pthread.h>
#include <stdbool.h>

#define DISK_BUF_SIZE 1024*140
#define DISK_FLUSH_THRESHOLD  1024*128

#define HDLC_CRC_LEN	2
#define NON_HDLC_VERSION	1
/* Non-HDLC Header:
 * 1 byte - Control char
 * 1 byte - Version
 * 2 bytes - Packet length
 */
#define DIAG_NON_HDLC_HEADER_SIZE	4

/*
 * Minimum command size: 1 byte
 * Minimum Non-HDLC pkt size:  6 bytes
 * Minimum HDLC pkt size: 4 bytes
 */
#define DIAG_MIN_NON_HDLC_PKT_SIZE	6
#define MIN_CMD_PKT_SIZE 4

#define FILE_LIST_NAME_SIZE 100
#define MAX_FILES_IN_FILE_LIST 100
#define std_strlprintf     snprintf
#define RENAME_CMD_LEN ((2*FILE_NAME_LEN) + 10)
#define MDLOG_WRITTEN_BYTES_LIMIT (ULONG_MAX - (64*1024))

struct buffer_pool {
	int free;
	int data_ready;
	unsigned int bytes_in_buff[NUM_PROC];
	unsigned char *buffer_ptr[NUM_PROC];
	pthread_mutex_t write_mutex;
	pthread_cond_t write_cond;
	pthread_mutex_t read_mutex;
	pthread_cond_t read_cond;
};

enum diag_cond_status {
	DIAG_COND_ERROR,
	DIAG_COND_WAIT,
	DIAG_COND_COMPLETE,
};

enum status {
	NOT_READY,
	READY,
};

enum read_thread_status {
	STATUS_RDTHRD_CLR = 0,
	STATUS_RDTHRD_INIT = 1,
	STATUS_RDTHRD_WAIT = 2,
	STATUS_RDTHRD_EXIT = 4,
};

enum restart_thread_status {
	STATUS_RST_THRD_CLR = 0,
	STATUS_RST_THRD_INIT = 1,
	STATUS_RST_THRD_EXIT = 2,
};

extern int fd_md[NUM_PROC];
extern int gdwClientID;
extern uint8_t diag_id;

void log_to_device(unsigned char *ptr, int logging_mode, int size, int type);
void send_mask_modem(unsigned char mask_buf[], int count_mask_bytes);

/* === Functions dealing with qshrink4 === */

/* Creates threads to read the qshrink4 database threads. */
int create_diag_qshrink4_db_parser_thread(unsigned int peripheral_mask, unsigned int device_mask);

/* Parses the data for qshrink4 command response */
int parse_data_for_qsr4_db_file_op_rsp(uint8 *ptr, int count_received_bytes, int index);
int parse_data_for_qdss_rsp(uint8* ptr, int count_received_bytes, int index);
int parse_data_for_diag_features_rsp(uint8* ptr, int count_received_bytes, int index, int *update_count);
int parse_data_for_adpl_rsp(uint8* ptr, int count_received_bytes, int index);

/* function for interacting with secure diag key info parser */
int parse_data_for_key_info_resp(uint8* ptr, int count_received_bytes, int proc);
int key_info_enabled(void);
int keys_stored(int proc);
int get_keys_header_size(int proc);
int write_key_header(int fd, int proc);
void diag_kill_key_info_threads(void);

/* function to write the qmdlv2 header for qdss binaries */
int write_qdss_header(int fd, int proc);
int diag_qdss_dyn_atid_support(void);
int write_dyn_atid_header(int fd);
size_t get_atid_payload_size(void);

/* Add qshrink4 guid information to qmdl2 header */
int add_guid_to_qshrink4_header(unsigned char * guid, int p_type, int peripheral);

void write_buf_cond_wait(pthread_cond_t *cond, pthread_mutex_t *mutex);
void get_time_string(char *buffer, int len);
void diag_kill_qshrink4_threads(void);
void diag_cleanup_qshrink4_threads(void);
void diag_kill_qdss_threads(void);
void diag_kill_adpl_threads(void);
void diag_kill_feature_setup_threads(void);
int delete_log(int type);
int delete_qdss_log(int type);
int diag_send_cmds_to_disable_adpl(int in_ssr);
int diag_reconfigure_qdss(void);
unsigned int get_qdss_mask(void);
/* function to check diag-id based logging enablement */
bool is_diagid_logging_format_selected(void);

int diag_query_diag_features(int peripheral_type, int feature_flag);

extern boolean gbRemote;
#define DIAG_LSM_PKT_EVENT_PREFIX "DIAG_SYNC_EVENT_PKT_"
#define DIAG_LSM_MASK_EVENT_PREFIX "DIAG_SYNC_EVENT_MASK_"
#endif /* DIAG_LSMI_H */

