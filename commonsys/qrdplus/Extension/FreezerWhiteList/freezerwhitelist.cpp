/**
 * Copyright (c) 2024 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

#include "Config.h"

#define PATH_LEN 32
#define BUFFER_MAX 1024
constexpr char k_top_file[] = "/dev/cpuctl/top-app/cgroup.procs";
constexpr char needles[] = " \t\r\n";

Config config;

std::vector<std::string> string_split(const std::string &s, const char *separators) {
    std::vector<std::string> result;
    if (s.empty()) return result;

    size_t base = 0;
    while (s.size() > base) {
        auto found = s.find_first_of(separators, base);
        if (found != base) {
            result.push_back(s.substr(base, found - base));
        }
        if (found == s.npos) break;
        base = found + 1;
    }
    return result;
}

std::string read_file(std::string &&path) {
    std::string content;
    if (!base::ReadFileToString(path, &content)) {
        ALOGI("Read %s failed", path.c_str());
        content = "";
    }
    return content;
}

std::string get_proc_name(std::string tid) {
    std::string content = read_file("/proc/" + tid + "/cmdline");
    auto pos = content.find_first_of(needles, 0, sizeof(needles));
    if (pos != std::string::npos) {
        content.erase(pos);
    }
    return content;
}

ssize_t read_text(const char *path, char *buf, size_t max_len) {
    ssize_t len;
    int fd;

    fd = open(path, O_RDONLY);
    if (fd < 0)
        return fd;

    len = read(fd, buf, max_len - 1);
    if (len < 0)
        goto out;

    buf[len] = 0;
    out:
    close(fd);
    return len;
}

void cg_check_context() {
    char buf[BUFFER_MAX] = {0};
    if (read_text(k_top_file, buf, BUFFER_MAX) >= 0) {
        std::vector<std::string> pids = string_split(buf, "\n");
        ALOGV("cg_read_strstr %s num=%d", buf, pids.size());
        for (int i = pids.size() - 1; i >= 0; i--) {
            pid_t pid = atoi(pids[i].c_str());
            std::string cmdline = get_proc_name(pids[i].c_str());
            ALOGV("cg_read_strstr %s pid=%d", cmdline.c_str(), pid);
            if (cmdline.empty()) {
                ALOGE("cg_read_strstr empty name.");
                continue;
            }
            config.check_process(pid, cmdline);
        }
    }
    config.scan_process();
}

int main(int argc, char **argv) {
    int fd, ret = -1;
    char buf[PATH_LEN];
    config.init();
    if (!config.is_enable()) return 0;

    fd = inotify_init1(0);
    if (fd < 0) {
        ALOGE("inotify_init1 %s failed", k_top_file);
        return 0;
    }
    ret = inotify_add_watch(fd, k_top_file, IN_MODIFY);

    if (ret == -1) {
        ALOGE("watch %s failed.", k_top_file);
        close(fd);
        return 0;
    }

    while (true) {
        int length = read(fd, buf, sizeof(buf) - 1);
        ALOGV("read len %d", length);
        cg_check_context();
    }
    return 0;
}
