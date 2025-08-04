/*====*====*====*====*====*====*====*====*====*====*====*====*====*====*====*

			  Test Application for Diag Interface

GENERAL DESCRIPTION
  Contains main implementation of Diagnostic Services Test Application.

EXTERNALIZED FUNCTIONS
  None

INITIALIZATION AND SEQUENCING REQUIREMENTS


Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
All rights reserved.
Confidential and Proprietary - Qualcomm Technologies, Inc.

*====*====*====*====*====*====*====*====*====*====*====*====*====*====*====*/

#include "event.h"
#include "msg.h"
#include "log.h"
#include "diag_lsm.h"
#include "stdio.h"
#include "stdlib.h"
#include "string.h"
#include <malloc.h>
#include <unistd.h>
#include <sys/stat.h>
#include <sys/time.h>
#include <sys/ioctl.h>
#include <signal.h>
#include <time.h>
#include <stdlib.h>
#include <getopt.h>
#include <fcntl.h>
#include "errno.h"
#include "diag_lsm_hidl_client.h"
#include <libxml/parser.h>
#include <libxml/tree.h>

#ifndef FEATURE_LE_DIAG
#include <log/log.h>
#include <cutils/android_filesystem_config.h>
#ifdef TARGET_FS_CONFIG_GEN
#include "generated_oem_aid.h"
#endif
#endif

#ifdef FEATURE_LE_DIAG
#include <limits.h>
#endif

/*
 * strlcpy is from OpenBSD and not supported by Meego.
 * GNU has an equivalent g_strlcpy implementation into glib.
 * Featurized with compile time USE_GLIB flag for Meego builds.
 */
#ifdef USE_GLIB
#define strlcpy g_strlcpy
#define strlcat g_strlcat
#endif

#ifdef FEATURE_LE_DIAG
#define LE_UID_DIAG 53
#define LE_GID_DIAG 53
#define LE_GID_SDCARD 1015
#define LE_GID_INET_RAW	3004
#endif

#define DIAG_MDLOG_WAKELOCK_NAME		"diag_mdlog_wakelock"
#define DIAG_MDLOG_PROCESS_NAME			"diag_mdlog"

#define std_strlprintf     snprintf
#define DIAG_MD_MSM_MASK 0x100000
#define DIAG_MD_MDM_MASK 0x200000
#define DIAG_MD_MDM2_MASK 0x400000

/* Minimum size for etr1 buf size is 32MB */
#define ETR1_BUF_SIZE_MIN (32 * 1024 * 1024)

#define PID_EXIT_WAIT_CNT 10

/* static data */
static int dir_set = 0;
static int kill_mdlog = 0;
static int dir_access = 0;
static char default_dir_name[FILE_NAME_LEN] = "/sdcard";
static int enable_wakelock = 0;
static int enable_nonrealtime = 0;
static int disable_hdlc = 0;
static unsigned int peripheral_mask = 0;
static unsigned int qdss_mask = 0;
static unsigned int upd_mask = 0;
static uint8 tx_mode = 0;
static uint8 peripheral_tx_mode_set = 0;
static unsigned int peripheral_id_mask = 0;
static unsigned int device_mask = 0;
static unsigned int device_mask_entered = 0;
static int etr_buffer_size;
static char mask_file_name_t[FILE_NAME_LEN];

/* extern data */
extern int mask_file_entered;
extern int mask_file_mdm_entered;
extern char mask_file_list[FILE_NAME_LEN];
extern int mask_file_list_entered;
extern int use_qmdl2_v2;
extern int use_qmdl2_v2_hdlc_disable;
extern int diagid_guid_mapping;
extern int adpl_enabled;
extern int adpl_modem_down;
extern int qdss_mdm_down;
extern int in_wait_for_adpl_status;
extern int diag_adpl_node_fd;
extern unsigned long max_file_size;
extern unsigned long min_file_size;
extern int cleanup_mask;
extern int proc_type;
extern unsigned int max_file_num;
extern int rename_file_names;
extern int rename_dir_name;
extern char pid_file[DIAG_MDLOG_PID_FILE_SZ];
extern int hdlc_toggle_status;
extern int kill_mdlog_flag;
extern int qsr_state;
extern struct diag_con_all_param_t all_con_params;
extern unsigned int write_buf_flush_timeout;

char xml_file_name[FILE_NAME_LEN] = "/sdcard/diag_logs/mdlog_arg_list.xml";
/*=============================================================================*/
/* Function declarations */
/*=============================================================================*/

extern void flush_buffer(int);

/*=============================================================================*/

static void mask_file_check(const char *type_str)
{
	strlcpy(mask_file_name_t, type_str, FILE_NAME_LEN);
	mask_file_entered = 1;
}

/* -m parameter parsing */
static void mdm_mask_file_check(const char *type_str)
{
	int mdm_num;

	mask_file_mdm_entered = 1;
	for (mdm_num = 1; mdm_num < NUM_PROC; mdm_num++)
		strlcpy(mask_file_proc[mdm_num], type_str, FILE_NAME_LEN);
}

/* -n parameter parsing */
static void max_file_num_check(const char *type_str)
{
	int file_num = atoi(type_str);
	if(file_num <= 1) {
		DIAG_LOGE("\n diag_mdlog: Invalid file number, must be greater than 1\n");
		exit(0);
	}
	max_file_num = file_num;
}

/* -o parameter parsing */
static void out_dir_check(const char *type_str)
{
	int z;

	dir_set = 1;
	for (z = 0; z < NUM_PROC; z++)
		strlcpy(output_dir[z], type_str, FILE_NAME_LEN - 6);

}

/* -s parameter parsing */
static void max_file_size_check(const char *type_str)
{
	max_file_size = atol(type_str);
	if ((long)max_file_size <= 0) {
		max_file_size = 100000000;
	} else {
		if (max_file_size <= (ULONG_MAX / (1024 * 1024))) {
			max_file_size *= 1024 * 1024;
		} else {
			DIAG_LOGE("diag: Entered max file size is too large: %lu MB\n", max_file_size);
			DIAG_LOGE("diag: Please enter file size upto: %lu MB. Exiting...\n", ULONG_MAX / (1024 * 1024));
			exit(0);
		}
	}
	min_file_size = ((max_file_size / 100) * 80);
}

/* -y parameter parsing */
static void etr_buf_check(const char *type_str)
{
	etr_buffer_size = to_integer(type_str);
	if(etr_buffer_size == 0) {\
		DIAG_LOGE("diag: Unsupported etr buffer size. using default size\n");
	} else {
		if (etr_buffer_size < ETR1_BUF_SIZE_MIN) {
			DIAG_LOGE("diag: Invalid etr1 buffer size provided\n");
			exit(0);
		} else {
			diag_set_etr_buffer_size(etr_buffer_size);
		}
	}
}

/* write_buf_flush_timeout parameter parsing */
static void write_buf_flush_timeout_check(const char *type_str)
{
	int flush_time = to_integer(type_str);

	if (flush_time < 0) {
		DIAG_LOGE("diag: %s: Invalid value for time: %d seconds\n", __func__, flush_time);
		exit(0);
	}
	write_buf_flush_timeout = flush_time;
}

static void usage(char *progname)
{
	printf("\n Usage for %s:\n", progname);
	printf("\n-a  --hdlcdisable:\t Disable hdlc encoding\n");
	printf("\n-b  --nonrealtime:\t Have peripherals buffer data and send data in non-real-time\n");
	printf("\n-c  --cleanmask:\t Send mask cleanup to modem at exit\n");
	printf("\n-d  --disablecon:\t Disable console messages\n");
	printf("\n-e  --enablelock:\t Run using wake lock to keep APPS processor on\n");
	printf("\n-f, --filemsm:\t mask file name for MSM\n");
	printf("\n-g, --userpd:\t bitmask for userpd interested\n");
	printf("\n-h, --help:\t usage help\n");
	printf("\n-j, --proc:\t Proc mask to be selected for logging\n");
	printf("\n-k, --kill:\t kill existing instance of diag_mdlog\n");
	printf("\n-l, --filelist:\t name of file containing list of mask files\n");
	printf("\n-m, --filemdm:\t mask file name for MDM\n");
	printf("\n-n, --number:\t maximum file number\n");
	printf("\n-o, --output:\t output directory name\n");
	printf("\n-p, --peripheral:\t bit mask for the peripherals interested\n");
	printf("\n-q, --qdss:\t peripheral bit mask for qdss logging\n");
	printf("\n-r  --renamefiles:\t Rename dir/file names to time when closed\n");
	printf("\n-s, --size:\t maximum file size in MB\n");
	printf("\n-t, --configure peripheral tx mode:\t 0 - Streaming Mode \t 1- Threshold Mode \t 2- Circular Buffering Mode\n");
	printf("\n-u, --qmdl2_v2:\t Guid-diagid mapping in qmdl2 header\n");
	printf("\n-w, --wait:\t waiting for directory\n");
	printf("\n-x, --peripheral id:\t bit mask for the peripherals interested for buffering mode\n");
	printf("\n-y, --etr buffer size:\t ETR Buffer Size\n");
	printf("\n-z, --XML parsing is supported for passing arguments\n");
	printf("\ne.g. diag_mdlog -f <mask_file name> -o <output dir>"
			" -s <size in bytes> -c\n");
	exit(0);
}

int parse_xml_file(xmlDocPtr *xml_fp)
{
	xmlNodePtr currNode;
	char *type_str;
	int type = 0, ret = 0;

	currNode = xmlDocGetRootElement(xml_fp);
	if(!currNode) {
		DIAG_LOGE("diag: %s: Empty xml doc\n", __func__);
		return -1;
	}

	if(xmlStrcmp(currNode->name, BAD_CAST "mdlog_arg_list")) {
		DIAG_LOGE("diag: %s: Invalid root node");
		return -1;
	}

	for(currNode = currNode->xmlChildrenNode;
		currNode != NULL; currNode = currNode->next) {

		if(currNode->type != XML_ELEMENT_NODE)
			continue;

		xmlNodePtr node = currNode;

		type_str = (char *)xmlNodeGetContent(node);
		if (type_str == NULL) {
			DIAG_LOGE("diag: Invalid node\n");
			return -1;
		}

		if (strstr(type_str, "default"))
			continue;

		/* -j parameter parsing */
		if(!disable_hdlc && !xmlStrcmp(node->name, BAD_CAST "hdlcmode")) {
			disable_hdlc = atoi(type_str);
			diag_set_hdlc_status(disable_hdlc);
		}

		/* -b parameter parsing */
		if(!enable_nonrealtime && !xmlStrcmp(node->name, BAD_CAST "rtmode")) {
			enable_nonrealtime = atoi(type_str);
		}

		/* -c parameter parsing */
		if(!cleanup_mask && !xmlStrcmp(node->name, BAD_CAST "clearmask")) {
			cleanup_mask = atoi(type_str);
		}

		/* -f parameter parsing */
		if(!mask_file_entered && !xmlStrcmp(node->name, BAD_CAST "inputfile")) {
			mask_file_check(type_str);
		}

		/* -i parameter parsing */
		if(!adpl_enabled && !xmlStrcmp(node->name, BAD_CAST "adpl")) {
			adpl_enabled = atoi(type_str);
		}

		/* -j parameter parsing */
		if(!device_mask_entered && !xmlStrcmp(node->name, BAD_CAST "proc")) {
			device_mask = atoi(type_str);
		}

		/* -m parameter parsing */
		if(!mask_file_mdm_entered && !xmlStrcmp(node->name, BAD_CAST "mdminputfile")) {
			mdm_mask_file_check(type_str);
		}

		/* -n parameter parsing */
		if(!max_file_num && !xmlStrcmp(node->name, BAD_CAST "maxfilenumber")) {
			max_file_num_check(type_str);
		}

		/* -o parameter parsing */
		if(!dir_set && !xmlStrcmp(node->name, BAD_CAST "outputdir")) {
			out_dir_check(type_str);
		}

		/* -s parameter parsing */
		if(!max_file_size && !xmlStrcmp(node->name, BAD_CAST "filesize")) {
			max_file_size_check(type_str);
		}

		/* -u parameter parsing */
		if(!use_qmdl2_v2 && !xmlStrcmp(node->name, BAD_CAST "qmdl2")) {
			use_qmdl2_v2 = atoi(type_str);
		}

		/* -y parameter parsing */
		if(!etr_buffer_size && !xmlStrcmp(node->name, BAD_CAST "etrbuffersize")) {
			etr_buf_check(type_str);
		}

		/* periodic_flush parameter parsing */
		if(!write_buf_flush_timeout && !xmlStrcmp(node->name, BAD_CAST "write_buf_flush_timeout")) {
			write_buf_flush_timeout_check(type_str);
		}

		xmlFree(type_str);
	}

	return ret;
}

static int parse_xml_arg(const char *file_name)
{
	int ret = 0;
	xmlDocPtr xml_fp;

	xml_fp = xmlReadFile(file_name, "UTF-8", XML_PARSE_RECOVER);
	if (!xml_fp) {
		DIAG_LOGE("diag: %s: xml input file read failure, errno: %d\n", __func__, errno);
		exit(0);
	}
	ret = parse_xml_file(xml_fp);
	xmlFreeDoc(xml_fp);
	xmlCleanupParser();

	return ret;
}

static void parse_args(int argc, char **argv)
{
	int command;
	int ret = 0;

	struct option longopts[] =
	{
		{ "disablehdlc",0,	NULL,	'a'},
		{ "nonrealtime",0,	NULL,	'b'},
		{ "cleanmask",	0,	NULL,	'c'},
		{ "disablecon",	0,	NULL,	'd'},
		{ "enablelock",	0,	NULL,	'e'},
		{ "filemsm",	1,	NULL,	'f'},
		{ "user pd",	1,	NULL,	'g'},
		{ "help",		0,	NULL,	'h'},
		{ "adpl",		0,	NULL,	'i'},
		{ "device mask",	1,	NULL,	'j'},
		{ "kill",		0,	NULL,	'k'},
		{ "filelist",	1,	NULL,	'l'},
		{ "filemdm",	1,	NULL,	'm'},
		{ "number",     1,  NULL,   'n'},
		{ "output",		1,	NULL,	'o'},
		{ "peripheral",	1,	NULL,	'p'},
		{ "qdss",       1,  NULL,   'q'},
		{ "renamefiles",0,	NULL,	'r'},
		{ "size",		1,	NULL,	's'},
		{ "tx_mode",    1,  NULL,   't'},
		{ "qmdl2_v2",	0,  NULL,   'u'},
		{ "wait",       1,  NULL,   'w'},
		{ "proc",		1,	NULL,	'x'},
		{ "etrbuffersize", 1, NULL, 'y'},
		{ "xml			",0, NULL, 'z'},
	};

	while ((command = getopt_long(argc, argv, "f:m:l:o:p:s:w:n:t:x:y:g:q:j:z::cdkebrahiu", longopts, NULL))
			!= -1) {
		DIAG_LOGE("diag_mdlog: command = %c\n", command);
		switch (command) {
			case 'a':
				disable_hdlc = 1;
				diag_set_hdlc_status(disable_hdlc);
				break;
			case 'b':
				enable_nonrealtime = 1;
				break;
			case 'c':
				cleanup_mask = 1;
				break;
			case 'd':
				diag_disable_console = 1;
				break;
			case 'e':
				enable_wakelock = 1;
				break;
			case 'f':
				mask_file_check(optarg);
				break;
			case 'g':
				upd_mask = to_integer(optarg);
				if (upd_mask < DIAG_CON_UPD_WLAN ||
						upd_mask > DIAG_CON_UPD_ALL) {
					DIAG_LOGE("diag: Unsupported pd mask: %d. Exiting...\n", upd_mask);
					upd_mask = 0;
					exit(0);
				}
				break;
			case 'i':
				adpl_enabled = 1;
				break;
			case 'j':
				device_mask = to_integer(optarg);
				device_mask_entered = 1;
				break;
			case 'k':
				kill_mdlog = 1;
				kill_mdlog_flag = 1;
				break;
			case 'l':
				strlcpy(mask_file_list, optarg, FILE_NAME_LEN);
				mask_file_list_entered = 1;
				break;
			case 'm':
				mdm_mask_file_check(optarg);
				break;
			case 'n':
				max_file_num_check(optarg);
				break;
			case 'o':
				out_dir_check(optarg);
				break;
			case 'p':
				peripheral_mask = to_integer(optarg);
				if (peripheral_mask < DIAG_CON_NONE ||
						peripheral_mask > DIAG_CON_ALL) {
					DIAG_LOGE("diag: Unsupported peripheral mask: %d. Exiting...\n", peripheral_mask);
					peripheral_mask = 0;
					exit(0);
				}
				break;
			case 'q':
				qdss_mask = to_integer(optarg);
				if (qdss_mask < DIAG_CON_NONE || ((qdss_mask > DIAG_CON_ALL) &&
						(qdss_mask < DIAG_CON_UPD_WLAN || qdss_mask > DIAG_CON_ALL_MASK))) {
					DIAG_LOGE("diag: Unsupported qdss mask: %d. Exiting...\n", qdss_mask);
					qdss_mask = 0;
					exit(0);
				}
			   break;
			case 'r':
				rename_file_names = 1;
				rename_dir_name = 1;
				break;
			case 's':
				max_file_size_check(optarg);
				break;
			case 't':
				tx_mode = atoi(optarg);
				peripheral_tx_mode_set = 1;
				break;
			case 'u':
				use_qmdl2_v2 = 1;
				break;
			case 'w':
				strlcpy(dir_name, optarg, FILE_NAME_LEN);
				dir_access = 1;
				break;
			case 'x':
				peripheral_id_mask = to_integer(optarg);
				if (peripheral_id_mask < DIAG_CON_NONE) {
					peripheral_id_mask = 0;
				} else if (peripheral_id_mask > DIAG_CON_ALL) {
					if (peripheral_id_mask < DIAG_CON_UPD_WLAN ||
						peripheral_id_mask > DIAG_CON_UPD_ALL) {
						peripheral_id_mask = 0;
					} else {
						DIAG_LOGE("diag_mdlog: Buffering mode configuration for peripheral id mask: %d\n",
						peripheral_id_mask);
					}
				}

				if (!peripheral_id_mask) {
					DIAG_LOGE("diag: Unsupported peripheral id mask: %d. Exiting...\n", peripheral_id_mask);
					exit(0);
				}
				break;
			case 'y':
				etr_buf_check(optarg);
				break;
			case 'z':
				if (optarg != NULL)
					strlcpy(xml_file_name, optarg, FILE_NAME_LEN);

				DIAG_LOGD("diag: Reading %s file\n", xml_file_name);
				ret = parse_xml_arg(xml_file_name);
				if (ret) {
					DIAG_LOGE("diag: Error while parsing xml file, Exiting......\n");
					exit(0);
				}
				break;
			case 'h':
			default:
				usage(argv[0]);
		};
	}
	if (mask_file_entered) {
		if (disable_hdlc)
			strlcpy(mask_file2_proc[MSM], mask_file_name_t, FILE_NAME_LEN);
		else
			strlcpy(mask_file_proc[MSM], mask_file_name_t, FILE_NAME_LEN);
	}
}

void ignore_handler(int signal)
{
	DIAG_LOGE("diag: %s: received signal %d\n", __func__, signal);
	return;
}

void notify_handler(int signal, siginfo_t *info, void *unused)
{
	(void)unused;

	if (info) {

		DIAG_LOGE("diag: In %s, signal %d received from kernel, data is: %x\n",
			__func__, signal, info->si_int);

		if (info->si_int & DIAG_STATUS_OPEN) {
			if (info->si_int & DIAG_MD_MSM_MASK) {
				if (info->si_int & DIAG_CON_MPSS) {
					DIAG_LOGE("diag: DIAG_STATUS_OPEN on DIAG_CON_MPSS\n");
					diag_notify_key_info_thread(DIAG_STATUS_OPEN);
					diag_notify_parser_thread(DIAG_MSM_MASK, DIAG_CON_MPSS);
					if (qdss_mask & DIAG_CON_MPSS)
						diag_notify_qdss_thread(DIAG_MSM_MASK, DIAG_CON_MPSS);
					if (adpl_enabled && in_wait_for_adpl_status)
						diag_notify_adpl_thread();
				} else if (info->si_int & DIAG_CON_LPASS) {
					DIAG_LOGE("diag: DIAG_STATUS_OPEN on DIAG_CON_LPASS\n");
					diag_notify_parser_thread(DIAG_MSM_MASK, DIAG_CON_LPASS);
					if (qdss_mask & DIAG_CON_LPASS)
						diag_notify_qdss_thread(DIAG_MSM_MASK, DIAG_CON_LPASS);
				}  else if (info->si_int & DIAG_CON_WCNSS) {
					DIAG_LOGE("diag: DIAG_STATUS_OPEN on DIAG_CON_WCNSS\n");
					diag_notify_parser_thread(DIAG_MSM_MASK, DIAG_CON_WCNSS);
					if (qdss_mask & DIAG_CON_WCNSS)
						diag_notify_qdss_thread(DIAG_MSM_MASK, DIAG_CON_WCNSS);
				} else if (info->si_int & DIAG_CON_SENSORS) {
					DIAG_LOGE("diag: DIAG_STATUS_OPEN on DIAG_CON_SENSORS\n");
					diag_notify_parser_thread(DIAG_MSM_MASK, DIAG_CON_SENSORS);
					if (qdss_mask & DIAG_CON_SENSORS)
						diag_notify_qdss_thread(DIAG_MSM_MASK, DIAG_CON_SENSORS);
				} else if (info->si_int & DIAG_CON_WDSP) {
					/*Qshrink4 is not supported on WDSP*/
					DIAG_LOGE("diag: DIAG_STATUS_OPEN on DIAG_CON_WDSP\n");
					if (qdss_mask & DIAG_CON_WDSP)
						diag_notify_qdss_thread(DIAG_MSM_MASK, DIAG_CON_WDSP);
				} else if (info->si_int & DIAG_CON_CDSP) {
					DIAG_LOGE("diag: DIAG_STATUS_OPEN on DIAG_CON_CDSP\n");
					if (qdss_mask & DIAG_CON_CDSP)
						diag_notify_qdss_thread(DIAG_MSM_MASK, DIAG_CON_CDSP);
				} else if (info->si_int & DIAG_CON_NPU) {
					DIAG_LOGE("diag: DIAG_STATUS_OPEN on DIAG_CON_NPU\n");
					if (qdss_mask & DIAG_CON_NPU)
						diag_notify_qdss_thread(DIAG_MSM_MASK, DIAG_CON_NPU);
				} else if (info->si_int & DIAG_CON_NSP1) {
					DIAG_LOGE("diag: DIAG_STATUS_OPEN on DIAG_CON_NSP1\n");
					if (qdss_mask & DIAG_CON_NSP1)
						diag_notify_qdss_thread(DIAG_MSM_MASK, DIAG_CON_NSP1);
				} else if (info->si_int & DIAG_CON_GPDSP0) {
					DIAG_LOGE("diag: DIAG_STATUS_OPEN on DIAG_CON_GPDSP0\n");
					if (qdss_mask & DIAG_CON_GPDSP0)
						diag_notify_qdss_thread(DIAG_MSM_MASK, DIAG_CON_GPDSP0);
				} else if (info->si_int & DIAG_CON_GPDSP1) {
					DIAG_LOGE("diag: DIAG_STATUS_OPEN on DIAG_CON_GPDSP1\n");
					if (qdss_mask & DIAG_CON_GPDSP1)
						diag_notify_qdss_thread(DIAG_MSM_MASK, DIAG_CON_GPDSP1);
				} else if (info->si_int & DIAG_CON_HELIOS_M55) {
					DIAG_LOGE("diag: DIAG_STATUS_OPEN on DIAG_CON_HELIOS_M55\n");
					if (qdss_mask & DIAG_CON_HELIOS_M55)
						diag_notify_qdss_thread(DIAG_MSM_MASK, DIAG_CON_HELIOS_M55);
				} else if (info->si_int & DIAG_CON_SOCCP) {
					DIAG_LOGE("diag: DIAG_STATUS_OPEN on DIAG_CON_SOCCP\n");
					diag_notify_parser_thread(DIAG_MSM_MASK, DIAG_CON_SOCCP);
					if (qdss_mask & DIAG_CON_SOCCP)
						diag_notify_qdss_thread(DIAG_MSM_MASK, DIAG_CON_SOCCP);
				} else if (info->si_int & DIAG_CON_SLATE_APPS) {
					DIAG_LOGE("diag: DIAG_STATUS_OPEN on DIAG_CON_SLATE_APPS\n");
				} else if (info->si_int & DIAG_CON_SLATE_ADSP) {
					DIAG_LOGE("diag: DIAG_STATUS_OPEN on DIAG_CON_SLATE_ADSP\n");
				} else {
					DIAG_LOGE("diag: DIAG_STATUS_OPEN on unknown peripheral\n");
				}
			}
			else if (info->si_int & DIAG_MD_MDM_MASK) {
				diag_reconfigure_masks(MDM);
				diag_notify_parser_thread(DIAG_MDM_MASK, peripheral_mask);
				diag_notify_qdss_thread(DIAG_MDM_MASK, qdss_mask);
				if (adpl_enabled && in_wait_for_adpl_status)
					diag_notify_adpl_thread();
			}
			else if (info->si_int & DIAG_MD_MDM2_MASK) {
				diag_reconfigure_masks(MDM_2);
				diag_notify_parser_thread(DIAG_MDM2_MASK, peripheral_mask);
				diag_notify_qdss_thread(DIAG_MDM2_MASK, qdss_mask);
				if (adpl_enabled && in_wait_for_adpl_status)
					diag_notify_adpl_thread();
			}

		} else if (info->si_int & DIAG_STATUS_CLOSED) {
			if (info->si_int & DIAG_MD_MSM_MASK) {
				if (info->si_int & DIAG_CON_MPSS) {
					DIAG_LOGE("diag: DIAG_STATUS_CLOSED on DIAG_CON_MPSS\n");
					diag_notify_key_info_thread(DIAG_STATUS_CLOSED);
					if (adpl_enabled && (diag_adpl_node_fd != DIAG_INVALID_HANDLE))
						adpl_modem_down = 1;
					diag_reset_guid_count(MSM, DIAG_MODEM_PROC);
				} else if (info->si_int & DIAG_CON_LPASS) {
					DIAG_LOGE("diag: DIAG_STATUS_CLOSED on DIAG_CON_LPASS\n");
				} else if (info->si_int & DIAG_CON_WCNSS) {
					DIAG_LOGE("diag: DIAG_STATUS_CLOSED on DIAG_CON_WCNSS\n");
				} else if (info->si_int & DIAG_CON_SENSORS) {
					DIAG_LOGE("diag: DIAG_STATUS_CLOSED on DIAG_CON_SENSORS\n");
				} else if (info->si_int & DIAG_CON_WDSP) {
					DIAG_LOGE("diag: DIAG_STATUS_CLOSED on DIAG_CON_WDSP\n");
				} else if (info->si_int & DIAG_CON_CDSP) {
					DIAG_LOGE("diag: DIAG_STATUS_CLOSED on DIAG_CON_CDSP\n");
				}  else if (info->si_int & DIAG_CON_NPU) {
					DIAG_LOGE("diag: DIAG_STATUS_CLOSED on DIAG_CON_NPU\n");
				} else if (info->si_int & DIAG_CON_NSP1) {
					DIAG_LOGE("diag: DIAG_STATUS_CLOSED on DIAG_CON_NSP1\n");
				} else if (info->si_int & DIAG_CON_GPDSP0) {
					DIAG_LOGE("diag: DIAG_STATUS_CLOSED on DIAG_CON_GPDSP0\n");
				} else if (info->si_int & DIAG_CON_GPDSP1) {
					DIAG_LOGE("diag: DIAG_STATUS_CLOSED on DIAG_CON_GPDSP1\n");
				} else if (info->si_int & DIAG_CON_HELIOS_M55) {
					DIAG_LOGE("diag: DIAG_STATUS_CLOSED on DIAG_CON_HELIOS_M55\n");
				} else if (info->si_int & DIAG_CON_SLATE_APPS) {
					DIAG_LOGE("diag: DIAG_STATUS_CLOSED on DIAG_CON_SLATE_APPS\n");
				} else if (info->si_int & DIAG_CON_SLATE_ADSP) {
					DIAG_LOGE("diag: DIAG_STATUS_CLOSED on DIAG_CON_SLATE_ADSP\n");
				} else if (info->si_int & DIAG_CON_SOCCP) {
					DIAG_LOGE("diag: DIAG_STATUS_CLOSED on DIAG_CON_SOCCP\n");
				} else {
					DIAG_LOGE("diag: DIAG_STATUS_CLOSED on unknown peripheral\n");
				}
			} else if (info->si_int & DIAG_MD_MDM_MASK) {
				if (qdss_mask) {
					qdss_mdm_down = 1;
					DIAG_LOGD("diag: %s, Notified status qdss_mdm_down: %d\n", __func__, qdss_mdm_down);
					qdss_close_qdss_node_mdm();
				}
				diag_reset_guid_count(MDM, DIAG_MODEM_PROC);
			}
		}
	} else {
		DIAG_LOGE("diag: In %s, signal %d received from kernel, but no info value, info: 0x%pK\n",
			__func__, signal, info);
	}
	return;
}

/* stop_mdlog is called when another instance of diag_mdlog is to be killed off */
static void stop_mdlog(int pid, int kill_count)
{
	int fd;
	int ret;
	char pid_buff[10];
	uint8_t count = 0;

	if (!pid) {
		/* Determine the process id of the instance of diag_mdlog */
		fd = open(pid_file, O_RDONLY);
		if (fd < 0) {
			DIAG_LOGE("\n diag_mdlog: Unable to open pid file, errno: %d\n", errno);
			return;
		}

		ret = read(fd, pid_buff, 10);
		if (ret < 0) {
			DIAG_LOGE("\n diag_mdlog: Unable to read pid file, errno: %d\n", errno);
			close(fd);
			fd = DIAG_INVALID_HANDLE;
			return;
		}

		close(fd);
		fd = DIAG_INVALID_HANDLE;

		/* Make sure the buffer is properly terminated */
		if (ret == sizeof(pid_buff))
			ret--;
		pid_buff[ret] = '\0';

		pid = atoi(pid_buff);
	}

	if (pid == 0 || (tgkill(pid, pid, SIGTERM)) < 0) {
		DIAG_LOGE("\ndiag_mdlog: Unable to kill diag_mdlog instance pid: %d, "
			"errno: %d\n", pid, errno);
	} else if (kill_count <= 1) {
		DIAG_LOGD("\ndiag_mdlog: stopping diag_mdlog instance pid: %d\n", pid);
		do {
			/*
			 * Check whether pid is active or not.
			 * Zero will be returned if pid is active.
			 * Check pid status for 10 seconds.
			 */
			if (kill(pid, 0)) {
				DIAG_LOGD("diag_mdlog: Session exited for pid: %d\n", pid);
				return;
			}
			sleep(1);
			count++;
		} while (count < PID_EXIT_WAIT_CNT);

		if (count >= PID_EXIT_WAIT_CNT) {
			DIAG_LOGD("diag_mdlog: Session: %d did not exit gracefully\n", pid);
			kill_count += 1;
		}
	}

	if (kill_count > 1) {
		if (pid == 0 || tgkill(pid, pid, SIGKILL) < 0) {
			DIAG_LOGE("\ndiag_mdlog: Unable to SIGKILL diag_mdlog instance pid: %d, "
				"kill_count: %d, errno: %d\n", pid, kill_count, errno);
		} else {
			DIAG_LOGE("\ndiag_mdlog: Using SIGKILL for diag_mdlog instance pid: %d, "
				"kill_count: %d\n", pid, kill_count);
		}
	}

	return;
}

#ifndef FEATURE_LE_DIAG
static void adjust_permissions()
{
	int status;
	int size;

#if defined (AID_VENDOR_QTI_DIAG)
#define DIAG_GID AID_VENDOR_QTI_DIAG
#elif defined (AID_QCOM_DIAG)
#define DIAG_GID AID_QCOM_DIAG
#else
#define DIAG_GID AID_DIAG
#endif

#ifdef AID_VENDOR_QDSS
#ifdef AID_VENDOR_ADPL_ODL
	gid_t diag_groups[] = {DIAG_GID, AID_SDCARD_R, AID_MEDIA_RW, AID_SDCARD_RW, AID_NET_RAW, AID_VENDOR_QDSS, AID_VENDOR_ADPL_ODL};
#else
	gid_t diag_groups[] = {DIAG_GID, AID_SDCARD_R, AID_MEDIA_RW, AID_SDCARD_RW, AID_NET_RAW, AID_VENDOR_QDSS};
#endif
#else
#ifdef AID_VENDOR_ADPL_ODL
	gid_t diag_groups[] = {DIAG_GID, AID_SDCARD_R, AID_MEDIA_RW, AID_SDCARD_RW, AID_NET_RAW, AID_VENDOR_ADPL_ODL};
#else
	gid_t diag_groups[] = {DIAG_GID, AID_SDCARD_R, AID_MEDIA_RW, AID_SDCARD_RW, AID_NET_RAW};
#endif
#endif

	size = sizeof(diag_groups)/sizeof(gid_t);

	uid_t e_uid;

	/* Determine the effective user id */
	e_uid = geteuid();

	/*
	 * If this app is running as root, we need to drop the permissions.
	 * We can only drop permissions if this app is running as root. If
	 * the app is not running as root, then the app will need to be
	 * running with the appropriate permissions to support logging to
	 * the SD card.
	 */
	if (e_uid != 0)
		return;

	/* SD card access needs sdcard_rw group membership  */
	status = setgid(AID_SHELL);
	if (status < 0) {
		DIAG_LOGE("diag_mdlog: Error setting group id, errno: %d, Exiting ...\n", errno);
		goto fail_permissions;
	}

	/* Add qcom_diag and sdcard_r as supplemental groups so we can access /dev/diag */
	/* and /storage. Add AID_MEDIA_RW so we can write to the external SD card */
	status = setgroups(size, diag_groups);
	if (status == -1) {
		DIAG_LOGE("diag_mdlog: setgroups error, errno: %d, Exiting ...\n", errno);
		goto fail_permissions;
	}

	/* Ideally we would like a dedicated UID for diag_mdlog, for now use sdcard_rw */
	/* Drop privileges to sdcard_rw since system user does not have access */
	status = setuid(AID_SHELL);
	if (status < 0) {
		DIAG_LOGE("diag_mdlog: Error setting user id, errno: %d, Exiting ...\n", errno);
		goto fail_permissions;
	}
	return;

fail_permissions:
	if (diag_is_wakelock_init()) {
		diag_wakelock_release();
		diag_wakelock_destroy();
	}
	exit(0);
}
#else
static void adjust_permissions()
{
	int status;
	uid_t uid;
	gid_t supplemental_gid[] = {LE_GID_SDCARD, LE_GID_INET_RAW};
	int size = sizeof(supplemental_gid)/sizeof(gid_t);

	/* Determine the real user id */
	uid = getuid();

	/*
	 * If this app is running as root, we need to drop the permissions
	 * We can only drop permissions if this app is running as root,
	 * since this app is not a setuid app on LE.
	 */
	if (uid == 0) {
		/* Set the real group ID to the diag group */
		status = setgid(LE_GID_DIAG);
		if (status < 0) {
			printf("diag_mdlog: Error setting group id, errno: %d, Exiting ...\n", errno);
			goto fail_permissions;
		}

		/* Set 1 supplemental group, the sdcard group so it can access the SD card */
		status = setgroups(size, &supplemental_gid);
		if (status == -1) {
			printf("diag_mdlog: setgroups error, errno: %d, Exiting ...\n", errno);
			goto fail_permissions;
		}

		/* Set the real user id to the diag user */
		status = setuid(LE_UID_DIAG);
		if (status < 0) {
			printf("diag_mdlog: Error setting user id, errno: %d, Exiting ...\n", errno);
			goto fail_permissions;
		}
	}
	return;

fail_permissions:
	if (diag_is_wakelock_init()) {
		diag_wakelock_release();
		diag_wakelock_destroy();
	}
	exit(0);
}
#endif



static void diag_mdlog_get_pid_file(void)
{

	if (upd_mask && peripheral_mask) {

		/*ODL with -p and -g option*/

		DIAG_LOGE("diag_mdlog: ODL with -g and -p combination are not supported\n");
	} else if (upd_mask && !peripheral_mask) {

		/*ODL with -g option*/

		DIAG_LOGE("diag_mdlog: ODL with -g option\n");
		strlcpy(pid_file, "/sdcard/diag_logs/diag_mdlog", DIAG_MDLOG_PID_FILE_SZ);
		diag_get_pd_name_from_mask(pid_file,
				DIAG_MDLOG_PID_FILE_SZ,
				upd_mask);
		strlcat(pid_file, "_pid", DIAG_MDLOG_PID_FILE_SZ);
	} else if (!upd_mask && peripheral_mask) {

		/*ODL with -p option*/

		DIAG_LOGE("diag_mdlog: ODL with -p option\n");
		strlcpy(pid_file, "/sdcard/diag_logs/diag_mdlog", DIAG_MDLOG_PID_FILE_SZ);
		diag_get_peripheral_name_from_mask(pid_file,
				DIAG_MDLOG_PID_FILE_SZ,
				peripheral_mask);
		strlcat(pid_file, "_pid", DIAG_MDLOG_PID_FILE_SZ);
	} else {

		/*ODL with no -p and -g option*/

		DIAG_LOGE("diag_mdlog: ODL with no -p or -g option\n");
		strlcpy(pid_file, DIAG_MDLOG_PID_FILE, DIAG_MDLOG_PID_FILE_SZ);
	}
}

/*
 * This function checks if another instance of diag_mdlog already exists in the
 * system. This is done by checking the pid file. If there are any errors in
 * opening the pid file, the new instance of diag_mdlog exits.
 *
 * It retuns the errno on error, 0 if everything is successful;
 */
static int diag_mdlog_pid_init()
{
	int fd;
	int pid;
	int ret;
	int proc_fd;
	const int pid_len = 10;
	const int proc_len = 30;
	char pid_buff[pid_len];
	char process_name[proc_len];
	struct stat pid_stat;

	/* Determine the process id of the instance of diag_mdlog */
	fd = open(pid_file, O_RDONLY);
	if (fd < 0) {
		if (errno == ENOENT) {
			/* The pid file doesn't exist. Create a new file. */
			goto create;
		}
		DIAG_LOGE("diag_mdlog: Unable to open pid file, err: %d\n", errno);
		return errno;
	}

	ret = read(fd, pid_buff, pid_len);
	if (ret < 0) {
		DIAG_LOGE("diag_mdlog: Unable to read pid file, err: %d\n", errno);
		close(fd);
		return errno;
	}
	close(fd);

	/* Make sure the buffer is properly terminated */
	if (ret == pid_len)
		ret--;
	pid_buff[ret] = '\0';

	pid = atoi(pid_buff);

	snprintf(process_name, sizeof(process_name), "/proc/%d/cmdline", pid);
	proc_fd = open(process_name, O_RDONLY);
	if (proc_fd < 0) {
		/*
		 * The process is no longer active in the system. This is
		 * actually a no error case and diag_mdlog should continue.
		 * Replace the contents of the pid file with the new pid.
		 */
		goto create;
	}

	ret = read(proc_fd, process_name, proc_len);
	if (ret < 0) {
		DIAG_LOGE("diag_mdlog: Unable to read process file, err: %d\n", errno);
		close(proc_fd);
		return errno;
	}
	close(proc_fd);

	/* Make sure the buffer is properly terminated */
	if (ret == proc_len)
		ret--;
	process_name[ret] = '\0';

	/*
	 * Check if the process is actually a mdlog process. If not, this is not
	 * the same process that we started and is not an error. Go ahead and
	 * store the new pid in the file.
	 */
	if (!strstr(process_name, DIAG_MDLOG_PROCESS_NAME))
		goto create;

	if (pid > 0) {
		DIAG_LOGE("diag_mdlog: another instance of diag_mdlog already exitsts, pid: %d\n", pid);
		return pid;
	}

create:
	/*
	 * Make sure the default directory exists so the diag_pid file
	 * can be created.
	 */
	if (mkdir(DIAG_MDLOG_DIR, 0770)) {
		if (errno != EEXIST) {
			DIAG_LOGE("diag_mdlog: Failed to create directory for diag pid file, err: %d\n", errno);
			return errno;
		}
	}

	/* Check if the PID file is present. Delete the file if present */
	if (stat(pid_file, &pid_stat) == 0)
		unlink(pid_file);

	pid = getpid();
	fd = open(pid_file, O_RDWR | O_CREAT | O_EXCL | O_SYNC, 0660);
	if (fd < 0) {
		DIAG_LOGE("diag_mdlog: Unable to create pid file, err: %d\n", errno);
		return errno;
	}

	snprintf(pid_buff, pid_len, "%d", pid);
	write(fd, pid_buff, pid_len);
	close(fd);
	DIAG_LOGE("diag_mdlog: successfully created pid file, pid: %d\n", pid);
	return 0;
}

static int diag_mdlog_config_buffering_tx_mode_mask(unsigned int proc_mask, uint8 tx_mode,
						uint8 low_wm_val, uint8 high_wm_val)
{
	int ret = 0;
	uint8 peripheral = 0;

	if (!proc_mask)
		return -EINVAL;

	if (proc_mask & DIAG_CON_MPSS) {
		peripheral = DIAG_MODEM_PROC;
		ret = diag_configure_peripheral_buffering_tx_mode(peripheral, tx_mode,
								low_wm_val, high_wm_val);
		if (ret != 1) {
			DIAG_LOGE("diag_mdlog: Failed configuring the peripheral: %d tx_mode: %d, ret: %d\n",
				peripheral, tx_mode, ret);
			return ret;
		}
		proc_mask = proc_mask ^ DIAG_CON_MPSS;
	}
	if (proc_mask & DIAG_CON_LPASS) {
		peripheral = DIAG_LPASS_PROC;
		ret = diag_configure_peripheral_buffering_tx_mode(peripheral, tx_mode,
								low_wm_val, high_wm_val);
		if (ret != 1) {
			DIAG_LOGE("diag_mdlog: Failed configuring the peripheral: %d tx_mode: %d, ret: %d\n",
				peripheral, tx_mode, ret);
			return ret;
		}
		proc_mask = proc_mask ^ DIAG_CON_LPASS;
	}
	if (proc_mask & DIAG_CON_WCNSS) {
		peripheral = DIAG_WCNSS_PROC;
		ret = diag_configure_peripheral_buffering_tx_mode(peripheral, tx_mode,
								low_wm_val, high_wm_val);
		if (ret != 1) {
			DIAG_LOGE("diag_mdlog: Failed configuring the peripheral: %d tx_mode: %d, ret: %d\n",
				peripheral, tx_mode, ret);
			return ret;
		}
		proc_mask = proc_mask ^ DIAG_CON_WCNSS;
	}
	if (proc_mask & DIAG_CON_SENSORS) {
		peripheral = DIAG_SENSORS_PROC;
		ret = diag_configure_peripheral_buffering_tx_mode(peripheral, tx_mode,
								low_wm_val, high_wm_val);
		if (ret != 1) {
			DIAG_LOGE("diag_mdlog: Failed configuring the peripheral: %d tx_mode: %d, ret: %d\n",
				peripheral, tx_mode, ret);
			return ret;
		}
		proc_mask = proc_mask ^ DIAG_CON_SENSORS;
	}
	if (proc_mask & DIAG_CON_WDSP) {
		peripheral = DIAG_WDSP_PROC;
		ret = diag_configure_peripheral_buffering_tx_mode(peripheral, tx_mode,
								low_wm_val, high_wm_val);
		if (ret != 1) {
			DIAG_LOGE("diag_mdlog: Failed configuring the peripheral: %d tx_mode: %d, ret: %d\n",
				peripheral, tx_mode, ret);
			return ret;
		}
		proc_mask = proc_mask ^ DIAG_CON_WDSP;
	}
	if (proc_mask & DIAG_CON_CDSP) {
		peripheral = DIAG_CDSP_PROC;
		ret = diag_configure_peripheral_buffering_tx_mode(peripheral, tx_mode,
								low_wm_val, high_wm_val);
		if (ret != 1) {
			DIAG_LOGE("diag_mdlog: Failed configuring the peripheral: %d tx_mode: %d, ret: %d\n",
				peripheral, tx_mode, ret);
			return ret;
		}
		proc_mask = proc_mask ^ DIAG_CON_CDSP;
	}
	if (proc_mask & DIAG_CON_NPU) {
		peripheral = DIAG_NPU_PROC;
		ret = diag_configure_peripheral_buffering_tx_mode(peripheral, tx_mode,
								low_wm_val, high_wm_val);
		if (ret != 1) {
			DIAG_LOGE("diag_mdlog: Failed configuring the peripheral: %d tx_mode: %d, ret: %d\n",
				peripheral, tx_mode, ret);
			return ret;
		}
		proc_mask = proc_mask ^ DIAG_CON_NPU;
	}
	if (proc_mask & DIAG_CON_NSP1) {
		peripheral = DIAG_NSP1_PROC;
		ret = diag_configure_peripheral_buffering_tx_mode(peripheral, tx_mode,
								low_wm_val, high_wm_val);
		if (ret != 1) {
			DIAG_LOGE("diag_mdlog: Failed configuring the peripheral: %d tx_mode: %d, ret: %d\n",
				peripheral, tx_mode, ret);
			return ret;
		}
		proc_mask = proc_mask ^ DIAG_CON_NSP1;
	}
	if (proc_mask & DIAG_CON_GPDSP0) {
		peripheral = DIAG_GPDSP0_PROC;
		ret = diag_configure_peripheral_buffering_tx_mode(peripheral, tx_mode,
								low_wm_val, high_wm_val);
		if (ret != 1) {
			DIAG_LOGE("diag_mdlog: Failed configuring the peripheral: %d tx_mode: %d, ret: %d\n",
				peripheral, tx_mode, ret);
			return ret;
		}
		proc_mask = proc_mask ^ DIAG_CON_GPDSP0;
	}
	if (proc_mask & DIAG_CON_GPDSP1) {
		peripheral = DIAG_GPDSP1_PROC;
		ret = diag_configure_peripheral_buffering_tx_mode(peripheral, tx_mode,
								low_wm_val, high_wm_val);
		if (ret != 1) {
			DIAG_LOGE("diag_mdlog: Failed configuring the peripheral: %d tx_mode: %d, ret: %d\n",
				peripheral, tx_mode, ret);
			return ret;
		}
		proc_mask = proc_mask ^ DIAG_CON_GPDSP1;
	}
	if (proc_mask & DIAG_CON_HELIOS_M55) {
		peripheral = DIAG_HELIOS_M55_PROC;
		ret = diag_configure_peripheral_buffering_tx_mode(peripheral, tx_mode,
								low_wm_val, high_wm_val);
		if (ret != 1) {
			DIAG_LOGE("diag_mdlog: Failed configuring the peripheral: %d tx_mode: %d, ret: %d\n",
				peripheral, tx_mode, ret);
			return ret;
		}
		proc_mask = proc_mask ^ DIAG_CON_HELIOS_M55;
	}
	if (proc_mask & peripheral_mask & DIAG_CON_SLATE_APPS) {
		peripheral = DIAG_SLATE_APPS_PROC;
		ret = diag_configure_peripheral_buffering_tx_mode(peripheral, tx_mode,
								low_wm_val, high_wm_val);
		if (ret != 1) {
			DIAG_LOGE("diag_mdlog: Failed configuring the peripheral: %d tx_mode: %d, ret: %d\n",
				peripheral, tx_mode, ret);
			return ret;
		}
		proc_mask = proc_mask ^ DIAG_CON_SLATE_APPS;
	}
	if (proc_mask & peripheral_mask & DIAG_CON_SLATE_ADSP) {
		peripheral = DIAG_SLATE_ADSP_PROC;
		ret = diag_configure_peripheral_buffering_tx_mode(peripheral, tx_mode,
								low_wm_val, high_wm_val);
		if (ret != 1) {
			DIAG_LOGE("diag_mdlog: Failed configuring the peripheral: %d tx_mode: %d, ret: %d\n",
				peripheral, tx_mode, ret);
			return ret;
		}
		proc_mask = proc_mask ^ DIAG_CON_SLATE_ADSP;
	}
	if (proc_mask & DIAG_CON_SOCCP) {
		peripheral = DIAG_SOCCP_PROC;
		ret = diag_configure_peripheral_buffering_tx_mode(peripheral, tx_mode,
								low_wm_val, high_wm_val);
		if (ret != 1) {
			DIAG_LOGE("diag_mdlog: Failed configuring the peripheral: %d tx_mode: %d, ret: %d\n",
				peripheral, tx_mode, ret);
			return ret;
		}
		proc_mask = proc_mask ^ DIAG_CON_SOCCP;
	}
	if (proc_mask & DIAG_CON_UPD_WLAN) {
		peripheral = BUF_UPD_WLAN;
		ret = diag_configure_peripheral_buffering_tx_mode(peripheral, tx_mode,
								low_wm_val, high_wm_val);
		if (ret != 1) {
			DIAG_LOGE("diag_mdlog: Failed configuring the PD: %d tx_mode: %d, ret: %d\n",
				peripheral, tx_mode, ret);
			return ret;
		}
		proc_mask = proc_mask ^ DIAG_CON_UPD_WLAN;
	}
	if (proc_mask & DIAG_CON_UPD_AUDIO) {
		peripheral = BUF_UPD_AUDIO;
		ret = diag_configure_peripheral_buffering_tx_mode(peripheral, tx_mode,
								low_wm_val, high_wm_val);
		if (ret != 1) {
			DIAG_LOGE("diag_mdlog: Failed configuring the PD: %d tx_mode: %d, ret: %d\n",
				peripheral, tx_mode, ret);
			return ret;
		}
		proc_mask = proc_mask ^ DIAG_CON_UPD_AUDIO;
	}
	if (proc_mask & DIAG_CON_UPD_SENSORS) {
		peripheral = BUF_UPD_SENSORS;
		ret = diag_configure_peripheral_buffering_tx_mode(peripheral, tx_mode,
								low_wm_val, high_wm_val);
		if (ret != 1) {
			DIAG_LOGE("diag_mdlog: Failed configuring the PD: %d tx_mode: %d, ret: %d\n",
				peripheral, tx_mode, ret);
			return ret;
		}
		proc_mask = proc_mask ^ DIAG_CON_UPD_SENSORS;
	}
	if (proc_mask & DIAG_CON_UPD_CHARGER) {
		peripheral = BUF_UPD_CHARGER;
		ret = diag_configure_peripheral_buffering_tx_mode(peripheral, tx_mode,
									low_wm_val, high_wm_val);
		if (ret != 1) {
			DIAG_LOGE("diag_mdlog: Failed configuring the PD: %d tx_mode: %d, ret: %d\n",
				peripheral, tx_mode, ret);
			return ret;
		}
		proc_mask = proc_mask ^ DIAG_CON_UPD_CHARGER;
	}
	if (proc_mask & DIAG_CON_UPD_OEM) {
		peripheral = BUF_UPD_OEM;
		ret = diag_configure_peripheral_buffering_tx_mode(peripheral, tx_mode,
			low_wm_val, high_wm_val);
		if (ret != 1) {
			DIAG_LOGE("diag_mdlog: Failed configuring the PD: %d tx_mode: %d, ret: %d\n",
				peripheral, tx_mode, ret);
			return ret;
		}
		proc_mask = proc_mask ^ DIAG_CON_UPD_OEM;
	}
	if (proc_mask & DIAG_CON_UPD_OIS) {
		peripheral = BUF_UPD_OIS;
		ret = diag_configure_peripheral_buffering_tx_mode(peripheral, tx_mode,
				low_wm_val, high_wm_val);
		if (ret != 1) {
			DIAG_LOGE("diag_mdlog: Failed configuring the PD: %d tx_mode: %d, ret: %d\n",
				peripheral, tx_mode, ret);
			return ret;
		}
		proc_mask = proc_mask ^ DIAG_CON_UPD_OIS;
	}

	return ret;
}

static void diag_mdlog_configure_peripheral_buffering_tx_mode()
{
	int ret = 0;

	if (tx_mode > DIAG_CIRCULAR_BUFFERING_MODE) {
		DIAG_LOGE("diag_mdlog: Invalid Peripheral Buffering Mode %d\n", tx_mode);
		return;
	}

	if (!peripheral_mask) {
		if (!peripheral_id_mask) {
			DIAG_LOGE("diag_mdlog: Neither -p nor -x option is given for tx mode: %d\n", tx_mode);
			return;
		}
		ret = diag_mdlog_config_buffering_tx_mode_mask(peripheral_id_mask, tx_mode,
							DIAG_LO_WM_VAL, DIAG_HI_WM_VAL);
		if (ret != 1) {
			DIAG_LOGE("diag_mdlog: Failed configuring the peripheral_id_mask: %d tx_mode: %d, ret: %d\n",
					peripheral_id_mask, tx_mode, ret);
			return;
		}
	} else {
		if (peripheral_id_mask) {
			ret = diag_mdlog_config_buffering_tx_mode_mask(peripheral_id_mask, tx_mode,
									DIAG_LO_WM_VAL, DIAG_HI_WM_VAL);
			if (ret != 1) {
				DIAG_LOGE("diag_mdlog: Failed configuring the mask: %d tx_mode: %d, ret: %d\n",
					peripheral_id_mask, tx_mode, ret);
				return;
			}
		} else { /* peripheral_id_mask not set */
			ret = diag_mdlog_config_buffering_tx_mode_mask(peripheral_mask, tx_mode,
								DIAG_LO_WM_VAL, DIAG_HI_WM_VAL);
			if (ret != 1) {
				DIAG_LOGE("diag_mdlog: Failed configuring the peripheral_mask: %d tx_mode: %d, ret: %d\n",
					peripheral_mask, tx_mode, ret);
				return;
			}
		}
	}
}

/*===========================================================================
FUNCTION   diag_query_md_pid

DESCRIPTION
  Query pid of existing mdlog session.

DEPENDENCIES
  Successful query requires interface(unix client/HIDL/AIDL) should be up at
  router side or should have access to directory

RETURN VALUE
  -1 - If recieved pid is value is 0 then it returns -1. Here it is assumed
       that HIDL/AIDL is not up so recieved PID for existing session is 0.
       Ensure to query pid again only once to get existing session pid.
   0 - no session is active or pid is stored in file. If retry is tried for this
       case then it may end up in infinite loop if there is no active session
  >0 - If PID query is successful for existing session


SIDE EFFECTS
  None

===========================================================================*/
static int diag_query_md_pid(int fd, struct diag_query_pid_t *query_pid)
{
	int ret = -1;
	struct stat dir_stat;
	int stat_count = 0;
	static uint8_t query_count = 0;

	ret = diag_lsm_comm_ioctl(fd, DIAG_IOCTL_QUERY_MD_PID, query_pid, sizeof(struct diag_query_pid_t));
	if (ret) {
		DIAG_LOGE("diag: Kernel does not support PID query, err: %d, errno: %d\n",
				ret, errno);
		do {
			/* Check for sdcard directory presence in fallback case */
			ret = stat(default_dir_name, &dir_stat);
			if (!ret) {
				DIAG_LOGE("diag_mdlog: Directory %s is accessible\n", default_dir_name);
				/* After confirming sdcard is accessible get pid info */
				diag_mdlog_get_pid_file();
				return 0;
			}
			DIAG_LOGE("diag_mdlog: Directory %s is not accessible with errno %d ret: %d\n",
					default_dir_name, errno, ret);
			stat_count++;
			if (stat_count == 15) {
				DIAG_LOGE("diag_mdlog: Directory %s is not accessible for %d seconds so exiting...\n",
					default_dir_name, stat_count*2);
				exit(0);
			}
			sleep(2);
		} while (ret);
	}

	if (!query_pid->pid) {
		DIAG_LOGE("diag: No Session is active for the given mask\n");
		if (!query_count && kill_mdlog_flag) {
			query_count++;
			ret = -1; /*So that main loop shall retry for pid query */
			sleep(1);
		} else {
			ret = 1;
		}
	} else if (query_pid->pid < 0) {
		DIAG_LOGE("diag: Invalid mask given as input\n");
		exit(0);
	} else {
		DIAG_LOGE("diag: Session for peripheral mask %d is active with PID = %d\n",
			query_pid->peripheral_mask, query_pid->pid);
		ret = 1;
	}
	return ret;
}

/* Main Function. This initializes Diag_LSM, sets up On Device Logging, then exits. */
int main(int argc, char *argv[])
{
	boolean bInit_Success = FALSE;
	int i, z;
	char buffer[30];
	char temp_xml_buf[GUID_LIST_END_XML_TAG_SIZE];
	struct timeval tv;
	time_t curtime;
	struct tm *tm_ptr = NULL;
	struct sigaction sact;
	uint16 remote_mask = 0;
	struct stat dir_stat;
	int stat_count = 0;
	int status;
	int num_mask_reads_succeeded = 0;
	int ret = 0, err = 0;
	int bytes_written = 0;
	struct diag_logging_mode_param_t params;
	struct diag_con_all_param_t params_con;
	struct diag_query_pid_t query_pid;
	int diag_fd_temp = DIAG_INVALID_HANDLE, pid_read = 0;
	sigset_t unblock_set;

	sigemptyset(&unblock_set);
	sigaddset(&unblock_set, SIGTERM);
	pthread_sigmask(SIG_UNBLOCK, &unblock_set, NULL);

	device_mask = DIAG_MSM_MASK | DIAG_MDM_MASK | DIAG_MDM2_MASK;

	parse_args(argc, argv);

	adjust_permissions();
	diag_fd_temp = open("/dev/diag", O_RDWR);
	if (diag_fd_temp > 0) {
		diag_use_dev_node = 1;
	} else {
		bInit_Success = Diag_LSM_Init(NULL);
			if (!bInit_Success) {
					DIAG_LOGE("\ndiag_mdlog: Diag_LSM_Init() failed. Exiting...\n");
					exit(0);
			}
		diag_fd_temp = diag_fd;
	}
	query_pid.pid = 0;
	if (peripheral_mask) {
		query_pid.peripheral_mask = peripheral_mask;
		if (qdss_mask)
			query_pid.peripheral_mask = query_pid.peripheral_mask | qdss_mask | DIAG_CON_APSS;
		query_pid.pd_mask = 0;
	} else if (upd_mask != 0) {
		query_pid.peripheral_mask = 0;
		query_pid.pd_mask = upd_mask;
	} else {
		params_con.diag_con_all = DIAG_CON_ALL;
		err = diag_lsm_comm_ioctl(diag_fd_temp, DIAG_IOCTL_QUERY_CON_ALL, &params_con, sizeof(struct diag_con_all_param_t));
		if (err) {
			DIAG_LOGD("diag:%s: Querying peripheral info from Central Diag unsupported, Using default stats, err: %d, errno: %d\n",
				__func__, err, errno);
			params_con.diag_con_all = DIAG_CON_ALL ^ DIAG_CON_LEGACY_UNSUPPORTED;
		} else {
			memcpy(&all_con_params, &params_con, sizeof(params_con));
			DIAG_LOGD("diag:%s: Central Diag supported: NUM_PERIPHERALS = %d, DIAG_CON_ALL: %d\n",
				__func__, params_con.num_peripherals, params_con.diag_con_all);
		}
		query_pid.peripheral_mask = params_con.diag_con_all;
		query_pid.pd_mask = 0;
	}

	query_pid.device_mask = device_mask;
	query_pid.kill_count = 0;
	if (kill_mdlog)
		query_pid.kill_op = 1;
	else
		query_pid.kill_op = 0;

	do {
		pid_read = diag_query_md_pid(diag_fd_temp, &query_pid);
	} while (pid_read < 0);

	DIAG_LOGE("diag_mdlog: Closing diag_fd_temp\n");
	if (diag_use_dev_node) {
		if (diag_fd_temp != DIAG_INVALID_HANDLE) {
			ret = close(diag_fd_temp);
			if (ret < 0) {
				DIAG_LOGE("diag: In %s, error closing temp file, ret: %d, errno: %d\n",
						__func__, ret, errno);
			}
			diag_fd_temp = DIAG_INVALID_HANDLE;
		}
	}
	else {
		if (diag_fd_temp != DIAG_INVALID_HANDLE)
			Diag_LSM_DeInit();
		diag_fd_temp = DIAG_INVALID_HANDLE;
	}

	/* mdlog will exit if session is invoked and there is
	 * an active session with same mask.
	 */
	if (!kill_mdlog && query_pid.pid && pid_read) {
		DIAG_LOGD("diag_mdlog: Exiting.... \n");
		exit(0);
	}

	/* mdlog will exit if kill session is invoked and
	 * there is no active session with given mask.
	 */
	if (kill_mdlog && !query_pid.pid && pid_read) {
		DIAG_LOGD("diag_mdlog: Exiting.... \n");
		exit(0);
	}

	/* If another instance of diag_mdlog is to be killed off */
	if (kill_mdlog) {
		stop_mdlog(query_pid.pid, query_pid.kill_count);
		exit(0);
	}
	if (peripheral_mask && upd_mask) {
		DIAG_LOGE("diag_mdlog: The combination of both -p and -g is not allowed\n");
		exit(0);
	}
	if (peripheral_mask && device_mask_entered) {
		DIAG_LOGE("diag_mdlog: The combination of both -p and -j is not allowed\n");
		exit(0);
	}
	if (upd_mask && device_mask_entered) {
		DIAG_LOGE("diag_mdlog: The combination of both -g and -j is not allowed\n");
		exit(0);
	}
	if (peripheral_mask || upd_mask)
		device_mask = DIAG_MSM_MASK;

	diag_set_diagid_mask(device_mask);

	if (qdss_mask)
		diag_set_qdss_mask(qdss_mask, device_mask);

	if (adpl_enabled)
		diag_set_adpl_mask(device_mask);

	/* Acquire wakelock if the client is requesting for wakelock*/
	if (enable_wakelock) {
		diag_wakelock_init(DIAG_MDLOG_WAKELOCK_NAME);
		diag_wakelock_acquire();
	}

	if (!pid_read && diag_mdlog_pid_init())
		goto exit;

	diag_features_setup_init();
	diagid_guid_mapping = 1;
	diag_key_info_init();

	if (use_qmdl2_v2) {

		if (disable_hdlc)
			use_qmdl2_v2_hdlc_disable = 1;
	}

	/* Waiting for directory access */
	if (dir_access) {
		errno = 0;
		stat(dir_name, &dir_stat);
		while(errno != 0) {
			DIAG_LOGE("diag_mdlog: Directory %s is not accessible with errno %d\n",
				dir_name, errno);
			sleep(5);
			stat(dir_name, &dir_stat);
			stat_count++;
			if(stat_count == 24) {
				DIAG_LOGE("diag_mdlog: Directory %s is not accessible for %d seconds so exiting...\n",
				dir_name, stat_count*5);
				dir_access = 0;
				goto fail;
			}
		}
		if(errno == 0) {
			DIAG_LOGE("diag_mdlog: Directory %s is accessible\n",
					dir_name);
		}
		dir_access = 0;
	}

	/* Setup the directory that we will be logging to */
	if (dir_set) {
		if (rename_dir_name) {
			/* Two somewhat conflicting command line parameters
			 * have been entered.
			 * 1. The name of the directory where the logging
			 *    files should be placed.
			 * 2. The renaming of the logging file when closed
			 *    and renaming of the logging directory when
			 *    on-device logging is halted.
			 * In this case, do not rename the logging directory.
			 * But still allow for renaming of logging files on close.
			 */
			rename_dir_name = 0;
		}
		status = mkdir(output_dir[MSM], 0770);
		if (status == -1) {
			if (errno == EEXIST) {
				DIAG_LOGE("diag_mdlog: Warning output directory already exists: %s\n",
					output_dir[MSM]);
				DIAG_LOGE("diag_mdlog: Proceeding...\n");
			} else {
				DIAG_LOGE("diag_mdlog: Error creating: %s, errno: %d\n",
					output_dir[MSM], errno);
				DIAG_LOGE("diag_mdlog: Attempting to use default directory\n");
				dir_set = 0;
			}
		} else {
			DIAG_LOGE("diag_mdlog: Created logging directory %s\n",
						output_dir[MSM]);
		}
		if (!disable_hdlc) {
			(void)std_strlprintf(qsr4_xml_file_name,
					FILE_NAME_LEN, "%s%s",
					output_dir[MSM],"/diag_qsr4_guid_list.xml");
			fd_qsr4_xml[MSM] =  open(qsr4_xml_file_name, O_CREAT | O_RDWR | O_SYNC | O_TRUNC, 0644);
			if(fd_qsr4_xml[MSM] < 0)
				DIAG_LOGE("diag:In %s failed to create xml file \n", __func__);
			ret = std_strlprintf(temp_xml_buf, GUID_LIST_XML_TAG_SIZE, "%s\n", "<guidlist>");
			if((fd_qsr4_xml[MSM] >= 0) && (ret > 0)){
				bytes_written = write(fd_qsr4_xml[MSM], temp_xml_buf, ret);
				if (bytes_written != ret)
					DIAG_LOGE("diag: In %s failed to write to xml file with error %d", __func__, errno);
			}
		}
	}

	if (!dir_set) {
		gettimeofday(&tv, NULL);
		curtime=tv.tv_sec;
		tm_ptr = localtime(&curtime);
		if (tm_ptr)
			strftime(buffer, 30, "%Y%m%d_%H%M%S", tm_ptr);
		else
			strlcpy(buffer, "00000000_000000", 30);
		mkdir(DIAG_MDLOG_DIR, 0770);
		for (z = 0; z < NUM_PROC; z++) {
			strlcpy(output_dir[z], DIAG_MDLOG_DIR, FILE_NAME_LEN);
			(void)strlcat(output_dir[z], buffer, FILE_NAME_LEN);
		}

		DIAG_LOGE("\ndiag_mdlog: Continuing with default directory path %s\n",
								output_dir[MSM]);
		if (mkdir(output_dir[MSM], 0770)) {
			DIAG_LOGE("diag_mdlog: Unable to create directory, errno: %d Exiting....\n", errno);
			goto fail;
		}
		if (!disable_hdlc) {
			(void)std_strlprintf(qsr4_xml_file_name,
					FILE_NAME_LEN, "%s%s",
					output_dir[MSM],"/diag_qsr4_guid_list.xml");
			fd_qsr4_xml[MSM] =  open(qsr4_xml_file_name, O_CREAT | O_RDWR | O_SYNC | O_TRUNC, 0644);
			if(fd_qsr4_xml[MSM] < 0)
				DIAG_LOGE("diag:In %s failed to create xml file \n", __func__);
			ret = std_strlprintf(temp_xml_buf, GUID_LIST_XML_TAG_SIZE, "%s\n", "<guidlist>");
			if((fd_qsr4_xml[MSM] >= 0) && (ret > 0)){
				bytes_written = write(fd_qsr4_xml[MSM], temp_xml_buf, ret);
				if (bytes_written != ret)
					DIAG_LOGE("diag: In %s failed to write to xml file with error, errno: %d\n", __func__, errno);
			}
		}
	}


	/*
	 * Since On Device Logging optimizations have implemented
	 * a buffering scheme, set things up so that on receipt of
	 * specified signals the flush_buffer signal handler function
	 * will be call so that the data in the buffers can be flushed
	 * to the SD card before the app exits.
	 */
	sigemptyset( &sact.sa_mask );
	sact.sa_flags = 0;
	sact.sa_handler = flush_buffer;
	sigaction(SIGTERM, &sact, NULL);
	sigaction(SIGUSR1, &sact, NULL);
	sigaction(SIGINT, &sact, NULL);

	/* Catch below signals and ignore to prevent unwanted interruption */
	struct sigaction ignore_action;
	sigemptyset(&ignore_action.sa_mask);
	ignore_action.sa_flags = 0;
	ignore_action.sa_handler = ignore_handler;
	sigaction(SIGHUP, &ignore_action, NULL);

	struct sigaction notify_action;
	sigemptyset(&notify_action.sa_mask);
	notify_action.sa_sigaction = notify_handler;
	/* Use SA_SIGINFO to denote we are expecting data with the signal */
	notify_action.sa_flags = SA_SIGINFO;
	sigaction(SIGCONT, &notify_action, NULL);

	/* Initialize the Diag LSM userspace library */
	bInit_Success = Diag_LSM_Init(NULL);
	if (!bInit_Success) {
		DIAG_LOGE("\ndiag_mdlog: Diag_LSM_Init() failed. Exiting...\n");
		goto fail;
	}

	DIAG_LOGE("\ndiag_mdlog: Diag_LSM_Init succeeded.\n");

	/* Get the mask for remote processor */
	diag_has_remote_device(&remote_mask);
	DIAG_LOGE("\n REMOTE PROCESSOR MASK %x\n", remote_mask);

	if (enable_nonrealtime) {
		status = diag_vote_md_real_time(MODE_NONREALTIME);
		if (status == -1) {
			DIAG_LOGE("diag_mdlog: unable to set mode to non real time mode\n");
		}
	}

	errno = 0;
	params_con.diag_con_all = DIAG_CON_ALL;
	err = diag_lsm_comm_ioctl(diag_fd, DIAG_IOCTL_QUERY_CON_ALL, &params_con, sizeof(struct diag_con_all_param_t));
	if (err && errno == EINVAL) {
		DIAG_LOGD("diag: IOCTL query for CON_ALL is absent in kernel,\n so Falling back to Library defined connection parameter, err: %d, errno: %d\n",
			 err, errno);
		params_con.diag_con_all = DIAG_CON_ALL ^ DIAG_CON_LEGACY_UNSUPPORTED;
		goto fall_back_con_all;
	} else if (err && errno == EFAULT) {
		DIAG_LOGE("diag: Err in getting the Peripheral list information, err: %d, errno: %d\n",
			 err, errno);
		return -1;
	} else {
		memcpy(&all_con_params, &params_con, sizeof(params_con));
		DIAG_LOGD("diag:kernel supported: NUM_PERIPHERALS = %d, DIAG_CON_ALL: %d\n",
			params_con.num_peripherals, params_con.diag_con_all);
	}

	if (peripheral_mask) {
		if (peripheral_mask < DIAG_CON_NONE ||
			peripheral_mask > params_con.diag_con_all) {
			DIAG_LOGE("diag: Unsupported peripheral mask in kernel: %d. Exiting...\n", peripheral_mask);
			peripheral_mask = 0;
			exit(0);
		}
	}
	if (peripheral_id_mask) {
		if (peripheral_id_mask < DIAG_CON_NONE) {
			peripheral_id_mask = 0;
		} else if (peripheral_id_mask > params_con.diag_con_all) {
			if (peripheral_id_mask < DIAG_CON_UPD_WLAN ||
				peripheral_id_mask > DIAG_CON_UPD_ALL) {
				DIAG_LOGD("diag_mdlog: Unsupported kernel peripheral id mask: %d\n",
				peripheral_id_mask);
				peripheral_id_mask = 0;
			} else {
				DIAG_LOGD("diag_mdlog: Supported peripheral id mask: %d for Buffering mode configuration\n",
				peripheral_id_mask);
			}
		}
	}
fall_back_con_all:
	/* Switch logging modes to turn on On Device Logging  */
	params.req_mode = MEMORY_DEVICE_MODE;
	params.mode_param = DIAG_MD_PERIPHERAL;
	params.diag_id = 0;
	params.pd_val = 0;
	params.peripheral = -EINVAL;

	if (peripheral_mask != 0) {
		params.peripheral_mask = peripheral_mask;
		params.pd_mask = 0;
	} else if (upd_mask != 0) {
		params.peripheral_mask = upd_mask;
		params.pd_mask = upd_mask;
	} else {
		params.peripheral_mask = params_con.diag_con_all;
		params.pd_mask = 0;
	}

	if (peripheral_mask) {
		if (qdss_mask)
			params.peripheral_mask = params.peripheral_mask | qdss_mask | DIAG_CON_APSS;
		diag_set_peripheral_mask(peripheral_mask);
	} else if (upd_mask) {
		/*
		 * Query the PD logging support on Kernel and Peripheral
		 */
		err = diag_lsm_comm_ioctl(diag_fd, DIAG_IOCTL_QUERY_PD_LOGGING, &params, sizeof(struct diag_logging_mode_param_t));
		if (err) {
			DIAG_LOGE("diag: Kernel does not support UPD logging for requested PD mask(%d), err: %d, errno: %d\n",
				 params.pd_mask, err, errno);
			return -1;
		} else {
			DIAG_LOGE("diag: Kernel supports(err = %d) PD logging for PD mask %d\n", err, params.pd_mask);
		}
		diag_set_upd_mask(upd_mask);
	}
	diag_set_device_mask(device_mask);
	params.device_mask = device_mask;
	status = diag_switch_logging_proc(&params);
	if (status)
		goto fail_deinit;

	diag_setup_features_init();
	diag_wait_for_features_init();

	qsr_state = QSR4_INIT;
	create_qshrink_thread();
	if (disable_hdlc && (hdlc_toggle_status < 0)) {
		DIAG_LOGE("diag_mdlog: Failed to toggle HDLC mode, exiting app\n");
		exit(0);
	}

	for (i = 0; i < NUM_PROC; i++) {
		if (device_mask & (1 << i))
			diag_get_secure_diag_info(i, FILE_TYPE_QMDL2);
	}

	if (qdss_mask) {
		/****************************************************
		 * Init Qdss after switch logging					*
		 ****************************************************/
		if (diag_qdss_init()) {
			DIAG_LOGE("diag: diag_qdss_init failed for mask = %d\n", qdss_mask);
			goto fail_deinit;
		}
	}

	if (adpl_enabled) {
		/****************************************************
		 * Init ADLP after switch logging					*
		 ****************************************************/
		if (diag_adpl_init()) {
			DIAG_LOGE("diag: diag_adpl_init failed\n");
			goto fail_deinit;
		}
	}


	/* Read mask file to tell On Device Logging what you are interested in */
	if (mask_file_list_entered) {
		status = diag_read_mask_file_list(mask_file_list);
		if (!status) {
			DIAG_LOGE("diag_mdlog: Error reading mask file list. Exiting ...\n");
		}
	} else {
		proc_type = MSM;
		if (device_mask & ( 1 << proc_type)) {
			DIAG_LOGE("\ndiag_mdlog: Reading mask for MSM, proc_type: %d\n", proc_type);
			proc_type = MSM;
			if (mask_file_entered)
				status = diag_read_mask_file_proc(proc_type);
			else
				status = read_mask_file_default(proc_type);

			if (status) {
				DIAG_LOGE("diag_mdlog: Error reading mask file, proc_type: %d, file: %s\n",
						proc_type, mask_file_proc[MSM]);
			} else {
				num_mask_reads_succeeded++;
			}
		}

		z = 1;
		while(remote_mask) {
			if(remote_mask & 1 ) {
				proc_type = z;
				if (device_mask & (1 << proc_type)) {
					DIAG_LOGE("\ndiag_mdlog: Reading mask for proc_type: %d\n",
										proc_type);
					if (mask_file_mdm_entered)
						status = diag_read_mask_file_proc(proc_type);
					else
						status = read_mask_file_default(proc_type);

					if (status) {
						DIAG_LOGE("diag_mdlog: Error reading mask file, proc_type: %d, file: %s\n",
							proc_type, mask_file_proc[proc_type]);
					} else {
						num_mask_reads_succeeded++;
					}
				}
			}
			z++;
			remote_mask = remote_mask >> 1;
		}

		/*
		 * If no mask files have been successfully read,
		 * try reading from a mask list file
		 */
		if (num_mask_reads_succeeded == 0) {
			DIAG_LOGE("\ndiag_mdlog: No successful mask file reads. Trying default mask list file.\n");
			status = diag_read_mask_file_list(mask_file_list);
			if (!status) {
				DIAG_LOGE("diag_mdlog: No mask files have been successfully read.\n");
				DIAG_LOGE("diag_mdlog: Running with masks that were set prior to diag_mdlog start-up.\n");
			}
		}
		status = 1;
	}

	qsr_state = QSR4_THREAD_CREATE;
	create_qshrink_thread();

	if (peripheral_tx_mode_set) {
		diag_mdlog_configure_peripheral_buffering_tx_mode();
	}

	if (status) {
		/* Reset proc type */
		proc_type = MSM;
		while(1) {
			/* Allow the main thread to sleep while logging is going on. */
			sleep(3600);
		}
	}

fail_deinit:
	/* De-Initialize the Diag LSM userspace library */
	Diag_LSM_DeInit();

fail:
	unlink(pid_file);
exit:
	/* Release and destroy wakelock if enabled */
	if (enable_wakelock) {
		diag_wakelock_release();
		diag_wakelock_destroy();
	}
	return 0;
}
