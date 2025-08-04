/*
 * Copyright (c) 2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
#include "Config.h"
#include <android-base/file.h>
#include <log/log_main.h>

#include <json/reader.h>
#include <json/value.h>

constexpr char file_path[] = "/product/etc/qti_freezer_whitelist.json";
extern std::string get_proc_name(std::string tid);

static int unfreeze_app(const char *package, pid_t _pid) {
    char *cmd[] = {
            (char *) "am",
            (char *) "unfreeze",
            (char *) "--sticky",
            (char *) package,
            nullptr
    };

    int status;
    int pid = fork();
    if (pid < 0) {
        ALOGE("Unable to fork.");
        return -1;
    }
    if (pid == 0) {
        int fd = open("/dev/null", O_WRONLY);
        if (fd < 0) {
            ALOGE("Unable to open /dev/null for stdout redirection.");
            exit(1);
        }
        dup2(fd, 1);
        int result = execvp(cmd[0], cmd);
        close(fd);
        exit(result);
    }
    wait(&status);
    ALOGV("unfreeze app %s status=%d", package, status);

    if (status != 0) {
        ALOGE("Unable to unfreeze %s, %d", package, _pid);
        return -1;
    }
    return 0;
}

void Config::init() {
    Json::Reader reader;
    Json::Value root;
    std::string json_doc;
    if (!android::base::ReadFileToString(file_path, &json_doc)) {
        ALOGE("read config file failed");
        return;
    }
    ALOGV("loadJson %s", json_doc.c_str());
    if (!reader.parse(json_doc, root)) {
        ALOGE("config file failed:%s", reader.getFormattedErrorMessages().c_str());
        return;
    }

    const Json::Value &config = root["whitelist"];
    if (!config.isObject()) {
        ALOGE("parse whitelist failed");
        return;
    }

    const Json::Value &packages = config["packages"];
    if (packages.isArray() && packages.size() > 0) {
        for (int i = 0; i < packages.size(); i++) {
            monitors.emplace(std::make_pair(packages[i].asString(), proc()));
        }
    } else {
        ALOGE("parse packages failed");
        return;
    }

    enable = config["enable"].asBool();
    pre_check();
}

void Config::pre_check() {
    ALOGI("enable: %d", enable);
    if (enable) {
        for (auto &it : monitors) {
            ALOGI("whilte list package: %s", it.first.c_str());
            unfreeze_app(it.first.c_str(), 0);
        }
    }
}

void Config::check_process(pid_t pid, std::string cmdline) {
    if (cmdline.compare("system_server") == 0 && sysPid != pid) {
        sysPid = pid;
        ALOGI("system change to %d", sysPid);
        pre_check();
        return;
    }
    auto search = monitors.find(cmdline);
    if (search == monitors.end()) {
        return;
    }
    proc *pi = &search->second;
    if (!pi->unfrozen || pi->pid != pid) {
        pi->reset();
        pi->pid = pid;
        pi->cmdline = cmdline;
        pi->unfrozen = true;
        int ret = unfreeze_app(cmdline.c_str(), pid);
        if (ret) pi->reset();
        return;
    }
}

void Config::scan_process() {
    if (enable) {
        for (auto &it : monitors) {
            if (it.second.pid != 0) {
                proc *pi = &it.second;
                std::string cmdline = get_proc_name(std::to_string(pi->pid));
                ALOGV("scan_process %s pid=%d", cmdline.c_str(), pi->pid);
                if (strcmp(cmdline.c_str(), pi->cmdline.c_str()) != 0) {
                    ALOGI("%s changed proc", pi->cmdline.c_str());
                    unfreeze_app(pi->cmdline.c_str(), 0);
                    pi->reset();
                }
            }
        }
    }
}
