/*
 * Copyright (c) 2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

#pragma once

#include <errno.h>
#include <unistd.h>
#include <stdio.h>
#include <fcntl.h>
#include <stdlib.h>
#include <string.h>
#include <getopt.h>

#include <linux/fb.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <sys/wait.h>


#include <log/log_main.h>
#include <vector>
#include <android-base/file.h>
#include <sys/inotify.h>
#include <set>
#include <unordered_map>

#ifndef LOG_TAG
#define LOG_TAG "Freezerwhitelist"
#endif

using namespace android;


struct proc {
    bool unfrozen;
    pid_t pid;
    std::string cmdline;

    void reset(void) {
        unfrozen = false;
        pid = 0;
        cmdline = "";
    }

    proc() {
        reset();
    }
};

class Config {

public:
    Config() {
    }

    bool is_enable() {
        return enable;
    }

    void init();

    void check_process(pid_t pid, std::string cmdline);

    void scan_process();

private:
    bool enable = false;
    pid_t sysPid = 0;
    std::unordered_map<std::string, proc> monitors;

    void pre_check();
};
