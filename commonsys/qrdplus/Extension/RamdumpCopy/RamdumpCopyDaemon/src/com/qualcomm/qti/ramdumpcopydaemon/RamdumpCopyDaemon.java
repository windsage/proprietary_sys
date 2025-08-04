/**
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.ramdumpcopydaemon;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;

import android.text.TextUtils;
import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Pattern;

import static com.qualcomm.qti.ramdumpcopydaemon.Constants.*;

public class RamdumpCopyDaemon {
    private static final String TAG = "RamdumpDaemon";
    private static final String RAMDUMP_FOLDER = "/data/media/0/Ramdump";
    private static final String RAMDUMP_FOLDER_ALIAS = "/sdcard/Ramdump";
    private final int NUM_MAX_DUMP = 15;
    private final int NUM_MAX_RETRY = 5;
    private DataManager mData = new DataManager();
    private ExecutorService mPool = null;
    private String mCurrentFolder;
    private boolean mIsDebug = false;

    public static void main(String[] args) {
        Log.d(TAG, "main running");
        new RamdumpCopyDaemon().startWorking();
    }

    private void startWorking() {
        mPool = Executors.newFixedThreadPool(5);
        CopyWorkThread copyThread = new CopyWorkThread();
        mPool.submit(copyThread);
        UiManager uiThread = new UiManager();
        mPool.submit(uiThread);
    }

    private void executeCopyScript(String path) {
        Process process = null;
        int ret;
        try {
            Log.d(TAG, "--begin executeCopyScript =" + path);
            process = getSuProcess();
            DataOutputStream mOut = new DataOutputStream(process.getOutputStream());
            mOut.writeBytes("bin/sh /system/system_ext/qcom_rawdump_copy.sh -o " +
                    path + " -f m" + "\n");
            mOut.flush();
            mOut.close();

            StreamGobbler errOut = new StreamGobbler(process.getErrorStream(), "ERROR");
            mPool.submit(errOut);
            StreamGobbler stdOut = new StreamGobbler(process.getInputStream(), "STDOUT");
            mPool.submit(stdOut);

            errOut.join();
            stdOut.join();

            ret = process.waitFor();
            handleScriptError(ret);
            Log.d(TAG, "--end executeCopyScript ret=" + ret);
        } catch (Exception e) {
            Log.e(TAG, "error:" + e, e);
        } finally {
            if (null != process) {
                process.destroy();
            }
        }
    }

    /*
     * During copy process, script crashes or exit with error.
     */
    private void handleScriptError(int ret) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        if (0 != ret && !TextUtils.isEmpty(mData.get(CMD_COPY_UPDATE)) &&
                TextUtils.isEmpty(mData.get(CMD_COPY_FINISHED))) {
            StringBuilder builder = new StringBuilder();
            builder.append(FROM_SERVER).append(":").append(
                    CMD_COPY_FINISHED).append(":").append(EIO).append(":").append(ret);
            String cmds = builder.toString();
            mData.put(CMD_COPY_FINISHED, cmds);
            Log.e(TAG, "script error, ret:" + cmds);
        }
    }

    private void parseScriptCmd(String cmd) {
        Log.d(TAG, "Script:" + cmd);
        String cmds[] = cmd.split(":");
        if (TextUtils.equals(String.valueOf(FROM_SERVER), cmds[0])) {
            final int command = Integer.parseInt(cmds[1]);
            switch (command) {
                case CMD_VALIDATED:
                    //FROM_SERVER:CMD_VALIDATED:1
                    final int status = Integer.parseInt(cmds[2]);
                    mData.put(command, cmd);
                    switch (status) {
                        case STATUS_OK:
                            Log.d(TAG, "dump ok");
                            break;
                        case EINVAL:
                            Log.d(TAG, errorToString(EINVAL));
                            rmFolder(mCurrentFolder);
                            return;
                        case EAGAIN:
                            Log.d(TAG, errorToString(EAGAIN));
                            rmFolder(mCurrentFolder);
                            return;
                        default:
                            Log.e(TAG, "Unknown status:" + status);
                            break;
                    }
                    break;
                case CMD_TOTAL_SIZE:
                    //FROM_SERVER:CMD_TOTAL_SIZE:dump_size:Byte
                    mData.put(command, cmd);
                    //FROM_SERVER:CMD_TOTAL_SIZE:ENOSPC
                    if (TextUtils.equals(cmds[2], String.valueOf(ENOSPC))) {
                        rmFolder(mCurrentFolder);
                    }
                    break;
                case CMD_TOTAL_COUNT:
                    mData.put(command, cmd);
                    break;
                case CMD_COPY_UPDATE:
                    mData.put(command, cmd);
                    break;
                case CMD_COPY_TYPE:
                    mData.put(command, cmd);
                    break;
                case CMD_COPY_FINISHED:
                    changeFolderPermission();
                    String tempPath = cmd.replace(RAMDUMP_FOLDER, RAMDUMP_FOLDER_ALIAS);
                    Log.d(TAG, "Copy finished, ret:" + tempPath);
                    mData.put(command, tempPath);
                    break;
                default:
                    Log.e(TAG, "Unknown cmd from script:" + cmd);
                    break;
            }
        }
    }

    private boolean checkRawdump() {
        Process process = null;
        try {
            process = getSuProcess();
            DataOutputStream mOut = new DataOutputStream(process.getOutputStream());
            mOut.writeBytes("cd /dev/block/by-name/ \n");
            mOut.writeBytes("find rawdump \n");
            mOut.flush();
            mOut.close();

            process.waitFor();
            BufferedReader bf = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = "";
            while ((line = bf.readLine()) != null) {
                if (line.startsWith("rawdump")) {
                    return true;
                } else {
                    return false;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "error: " + e, e);
        } catch (InterruptedException ie) {
            Log.e(TAG, "error: " + ie, ie);
        } finally {
            if (null != process) {
                process.destroy();
            }
        }
        return false;
    }

    private boolean checkMediaFolder() {
        Process process = null;
        try {
            process = getSuProcess();
            DataOutputStream mOut = new DataOutputStream(process.getOutputStream());
            mOut.writeBytes("test -d /data/media/0/Android && echo 1 || echo 0 \n");
            mOut.flush();
            mOut.close();

            process.waitFor();
            BufferedReader bf = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = "";
            while ((line = bf.readLine()) != null) {
                if (line.startsWith("1")) {
                    Log.d(TAG, "MediaFolder find!");
                    return true;
                } else {
                    Log.d(TAG, "MediaFolder not find!");
                    return false;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "error:" + e, e);
        } finally {
            if (null != process) {
                process.destroy();
            }
        }
        Log.d(TAG, "checkMediaFolder not find!");
        return false;
    }

    private boolean checkFolder() {
        Process process = null;
        try {
            process = getSuProcess();
            DataOutputStream mOut = new DataOutputStream(process.getOutputStream());
            mOut.writeBytes("cd /data/media/0 \n");
            mOut.writeBytes("ls |grep \"Ramdump\" \n");
            mOut.flush();
            mOut.close();

            process.waitFor();
            BufferedReader bf = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = "";
            while ((line = bf.readLine()) != null) {
                if (line.startsWith("Ramdump")) {
                    Log.d(TAG, "checkFolder find!");
                    return true;
                } else {
                    Log.d(TAG, "checkFolder not find!");
                    return false;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "error:" + e, e);
        } catch (InterruptedException ie) {
            Log.e(TAG, "error:" + ie, ie);
        } finally {
            if (null != process) {
                process.destroy();
            }
        }
        Log.d(TAG, "checkFolder not find!");
        return false;
    }

    private void changeFolderPermission() {
        Process process = null;
        try {
            process = getSuProcess();
            DataOutputStream mOut = new DataOutputStream(process.getOutputStream());
            mOut.writeBytes("chmod -R 777 " + RAMDUMP_FOLDER + "\n");
            mOut.writeBytes("chown media_rw:media_rw -R " + RAMDUMP_FOLDER + "\n");
            mOut.flush();
            mOut.close();
            process.waitFor();
            BufferedReader bf = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = "";
            while ((line = bf.readLine()) != null) {
                Log.d(TAG, "changeFolderPermission:" + line);
            }
        } catch (IOException e) {
            Log.e(TAG, "error:" + e, e);
        } catch (InterruptedException ie) {
            Log.e(TAG, "error:" + ie, ie);
        } finally {
            if (null != process) {
                process.destroy();
            }
        }
    }

    private String mkdirFolder() {
        Process process = null;
        try {
            process = getSuProcess();
            DataOutputStream mOut = new DataOutputStream(process.getOutputStream());
            mOut.writeBytes("cd /data/media/0 \n");
            mOut.writeBytes("mkdir Ramdump \n");
            mOut.writeBytes("chmod -R 777 Ramdump \n");
            mOut.writeBytes("cd Ramdump \n");
            mOut.writeBytes("mkdir 0 \n");
            mOut.writeBytes("chmod 777 0 \n");
            mOut.flush();
            mOut.close();
            process.waitFor();
            BufferedReader bf = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = "";
            while ((line = bf.readLine()) != null) {
                Log.d(TAG, "mkdirFolder:" + line);
            }
        } catch (IOException e) {
            Log.e(TAG, "error:" + e, e);
        } catch (InterruptedException ie) {
            Log.e(TAG, "error:" + ie, ie);
        } finally {
            if (null != process) {
                process.destroy();
            }
        }
        return RAMDUMP_FOLDER + "/0";
    }

    private void rmFolder(String path) {
        Process process = null;
        Log.d(TAG, "rmFolder:" + path);
        if (TextUtils.isEmpty(path)) {
            return;
        }
        try {
            process = getSuProcess();
            DataOutputStream mOut = new DataOutputStream(process.getOutputStream());
            mOut.writeBytes("rm -rf " + path + " \n");
            mOut.flush();
            mOut.close();
            int ret = process.waitFor();
            Log.d(TAG, "rmFolder ret:" + ret);
        } catch (IOException e) {
            Log.e(TAG, "error:" + e, e);
        } catch (InterruptedException ie) {
            Log.e(TAG, "error:" + ie, ie);
        } finally {
            if (null != process) {
                process.destroy();
            }
        }
    }

    private String mkdirForMultiDump(int index) {
        Process process = null;
        String path = null;
        try {
            process = getSuProcess();
            DataOutputStream mOut = new DataOutputStream(process.getOutputStream());
            mOut.writeBytes("cd " + RAMDUMP_FOLDER + "\n");
            mOut.writeBytes("mkdir " + index + " \n");
            mOut.writeBytes("cd  " + index + " \n");
            mOut.writeBytes("pwd \n");
            mOut.flush();
            mOut.close();

            process.waitFor();
            BufferedReader bf = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String list = "";
            while ((list = bf.readLine()) != null) {
                Log.d(TAG, "mkdirForMultiDump current folder list:" + list);
                path = list;
            }

        } catch (IOException e) {
            Log.e(TAG, "error:" + e, e);
        } catch (InterruptedException ie) {
            Log.e(TAG, "error:" + ie, ie);
        } finally {
            if (null != process) {
                process.destroy();
            }
        }
        Log.d(TAG, "mkdirForMultiDump path:" + path);
        return path;
    }

    private boolean isInteger(String str) {
        Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
        return pattern.matcher(str).matches();
    }

    /**
     * getPathForMultiDump
     * max folder num is NUM_MAX_DUMP
     * if current folder num exceed NUM_MAX_DUMP, reuse the oldest folder
     *
     * @return ret : folder name
     */
    private int getPathForMultiDump() {
        File fileDirectory = new File(RAMDUMP_FOLDER);
        int[] folders = new int[NUM_MAX_DUMP];
        long[] modify_times = new long[NUM_MAX_DUMP];
        int sum = 0, ret = 0;
        for (int i = 0; i < NUM_MAX_DUMP; i++) {
            folders[i] = -1;
            modify_times[i] = 0;
        }
        for (File temp : fileDirectory.listFiles()) {
            if (temp.isDirectory()) {
                String fullName = temp.toString();
                String name = fullName.substring(fullName.lastIndexOf("/") + 1);
                if (isInteger(name)) {
                    int num = Integer.parseInt(name);
                    if (num >= NUM_MAX_DUMP) {
                        Log.e(TAG, "folder num overflow:" + num);
                        return 0;
                    } else {
                        Log.d(TAG, "folder:" + num + " modify time=" + temp.lastModified());
                        folders[num] = num;
                        modify_times[num] = temp.lastModified();
                        sum++;
                    }
                } else {
                    Log.e(TAG, "error folder: " + fullName);
                }
            } else {
                Log.e(TAG, "find invaded file: " + temp.toString());
            }
        }
        if (sum > 0) {
            if (sum < NUM_MAX_DUMP) {
                for (int j = 0; j < NUM_MAX_DUMP; j++) {
                    if (folders[j] == -1) {
                        ret = j;
                        Log.d(TAG, "find folder:" + ret);
                        break;
                    }
                }
            } else {
                long temp = modify_times[0];
                for (int g = 0; g < NUM_MAX_DUMP; g++) {
                    if (modify_times[g] < temp) {
                        ret = g;
                        temp = modify_times[g];
                    }
                }
                Log.d(TAG, "find oldest folder:" + ret);
            }
        }
        if (ret >= NUM_MAX_DUMP || ret < 0) {
            Log.e(TAG, "get error folder:" + ret);
            ret = 0;
        }
        Log.d(TAG, "getPathForMultiDump:" + ret);
        return ret;
    }

    private boolean checkBuild() {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec("sh");
            DataOutputStream mOut = new DataOutputStream(process.getOutputStream());
            mOut.writeBytes("getprop ro.debuggable" + " \n");
            mOut.flush();
            mOut.close();
            process.waitFor();
            BufferedReader bf = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = "";
            while ((line = bf.readLine()) != null) {
                Log.d(TAG, "checkBuild:" + line);
                if (isInteger(line) && Integer.parseInt(line) == 1) {
                    Log.d(TAG, "checkBuild debuggable");
                    return true;
                } else {
                    return false;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "error:" + e, e);
        } catch (InterruptedException ie) {
            Log.e(TAG, "error:" + ie, ie);
        } finally {
            if (null != process) {
                process.destroy();
            }
        }
        return false;
    }

    private Process getSuProcess() throws IOException {
        Process process = null;
        if (mIsDebug) {
            process = Runtime.getRuntime().exec("su");
        } else {
            process = Runtime.getRuntime().exec("sh");
        }
        return process;
    }

    private void exitService() {
        Log.d(TAG, "exitService");
        System.exit(0);
    }

    class DataManager{
        private final Object mLock = new Object();
        private HashMap<Integer, String> mDumpScriptInfo = new HashMap<>();
        public void put(int type, String cmd) {
            synchronized (mLock) {
                mDumpScriptInfo.put(type, cmd);
            }
        }

        public String get(int type) {
            synchronized (mLock) {
                return mDumpScriptInfo.get(type);
            }
        }
    }

    private class CopyWorkThread extends Thread {
        @Override
        public void run() {
            String path;
            mIsDebug = checkBuild();
            if (!checkRawdump()) {
                Log.d(TAG, "No rawdump, exit service.");
                exitService();
            }
            int retry = 0;
            while (retry < NUM_MAX_RETRY) {
                if (checkMediaFolder()) break;

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Log.e(TAG, "sleep error: " + e);
                }
                retry ++;
                Log.e(TAG, "Can't find media folder, retry: " + retry);
            }
            if (retry >= NUM_MAX_RETRY) {
                Log.e(TAG, "Media folder error, exit!");
                exitService();
                return;
            }
            if (!checkFolder()) {
                path = mkdirFolder();
            } else {
                path = mkdirForMultiDump(getPathForMultiDump());
            }
            mCurrentFolder = path;
            executeCopyScript(path);
        }
    }

    public class StreamGobbler extends Thread {
        InputStream inputStream;
        String type;
        OutputStream outputStream;

        StreamGobbler(InputStream is, String type) {
            this(is, type, null);
        }

        StreamGobbler(InputStream is, String type, OutputStream redirect) {
            this.inputStream = is;
            this.type = type;
            this.outputStream = redirect;
        }

        public void run() {
            InputStreamReader isr = null;
            BufferedReader br = null;
            try {
                isr = new InputStreamReader(inputStream);
                br = new BufferedReader(isr);
                String line = null;
                while ((line = br.readLine()) != null) {
                    parseScriptCmd(line);
                }
            } catch (IOException ioe) {
                Log.e(TAG, "error:" + ioe, ioe);
            } finally {
                try {
                    if (br != null) br.close();
                    if (isr != null) isr.close();
                } catch (IOException e) {
                    Log.e(TAG, "error:" + e, e);
                }
            }
        }
    }

    /**
     * management for ui display and update.
     */
    private class UiManager extends Thread {
        private static final String ENABLE_UI_NOTIFICATION =
                "pm grant com.qualcomm.qti.ramdumpcopyui android.permission.POST_NOTIFICATIONS";
        private static final String ENABLE_UI_FOREGROUND_SERVICE =
                "pm grant com.qualcomm.qti.ramdumpcopyui android.permission.FOREGROUND_SERVICE_DATA_SYNC";
        private static final String BROADCAST =
                "am broadcast -a com.qualcomm.qti.ramdump.DAEMON --receiver-foreground -e cmd";
        private static final String ACTION_SHOW =
                "am start com.qualcomm.qti.ramdumpcopyui/.FullscreenActivity";

        private boolean isUIshowing = false;
        private boolean[] mProgress = new boolean[CMD_COPY_UPDATE];

        UiManager() {
            for (int i = 0; i < CMD_COPY_UPDATE; i++) {
                mProgress[i] = false;
            }
        }

        private void sleepMillis(int millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Log.e(TAG, "sleep error:" + e, e);
            }
        }

        private void showUI() {
            requestUI("input keyevent 82");
            requestUI(ENABLE_UI_NOTIFICATION);
            requestUI(ENABLE_UI_FOREGROUND_SERVICE);
            sleepMillis(500);
            requestUI(ACTION_SHOW);
            isUIshowing = true;
            sleepMillis(3000);
        }

        private void dismissUI() {
            String cmd = "ps -e|grep \"ramdumpcopyui\"|awk \'{print $2}\'";
            String pid = requestUI(cmd);
            if (!TextUtils.isEmpty(pid)) {
                requestUI("kill -9 " + pid);
            } else {
                Log.e(TAG, "dismissUI cannot find UI");
            }
            isUIshowing = false;
        }

        private String requestUI(String cmd) {
            String ret = "";
            Process process = null;
            Log.d(TAG, "requestUI:" + cmd);
            try {
                process = getSuProcess();
                DataOutputStream mOut = new DataOutputStream(process.getOutputStream());
                mOut.writeBytes(cmd + "\n");
                mOut.flush();
                mOut.close();
                process.waitFor();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));
                String mline = "";
                while ((mline = reader.readLine()) != null) {
                    ret = mline;
                    Log.d(TAG, "requestUI ret:" + mline);
                }
            } catch (Exception e) {
                Log.e(TAG, "requestUI error:" + e, e);
            } finally {
                if (null != process) {
                    process.destroy();
                }
            }
            return ret;
        }

        private boolean checkStepStatus(int step) {
            String cmd = mData.get(step);
            if (TextUtils.isEmpty(cmd)) {
                return false;
            }
            String cmds[] = cmd.split(":");
            switch (step) {
                case CMD_VALIDATED:
                    //FROM_SERVER:CMD_VALIDATED:1
                    final int status = Integer.parseInt(cmds[2]);
                    switch (status) {
                        case STATUS_OK:
                            break;
                        case EINVAL:
                            Log.d(TAG, errorToString(EINVAL));
                            showUI();
                            requestUI(BROADCAST + " " + cmd + " --async");
                            dismissUI();
                            exitService();
                            break;
                        case EAGAIN:
                            Log.d(TAG, errorToString(EAGAIN));
                            sleepMillis(2000);
                            exitService();
                            break;
                        default:
                            Log.e(TAG, "CMD_VALIDATED unknown status:" + status);
                            break;
                    }
                    break;
                case CMD_TOTAL_SIZE:
                    //FROM_SERVER:CMD_TOTAL_SIZE:dump_size:Byte
                    showUI();
                    requestUI(BROADCAST + " " + cmd + " --async");
                    //FROM_SERVER:CMD_TOTAL_SIZE:ENOSPC
                    if (TextUtils.equals(cmds[2], String.valueOf(ENOSPC))) {
                        dismissUI();
                        exitService();
                    }
                    break;
                case CMD_TOTAL_COUNT:
                    if (isUIshowing) requestUI(BROADCAST + " " + cmd + " --async");
                    break;
                case CMD_COPY_TYPE:
                    break;
                default:
                    break;
            }
            return true;
        }

        private boolean getDumpConfig() {
            boolean ret = true;
            for (int i = 0; i < mProgress.length; i++) {
                if (!mProgress[i]) {
                    mProgress[i] = checkStepStatus(i);
                }
                ret &= mProgress[i];
            }
            return ret;
        }

        /**
         * getCopyStatus
         *
         * @return return true when copy finish
         */
        private boolean getCopyStatus() {
            String finish = mData.get(CMD_COPY_FINISHED);
            if (!TextUtils.isEmpty(finish)) {
                requestUI(BROADCAST + " " + finish);
                isUIshowing = false;
                return true;
            } else {
                String update = mData.get(CMD_COPY_UPDATE);
                if (!TextUtils.isEmpty(update)) {
                    if (isUIshowing) {
                        requestUI(BROADCAST + " " + update + " --async");
                    } else {
                        Log.e(TAG, "update: no ui");
                    }
                }
            }
            return false;
        }

        @Override
        public void run() {
            boolean configDone = false;
            boolean finish = false;

            while (!configDone) {
                configDone = getDumpConfig();
            }

            while (!finish) {
                finish = getCopyStatus();
                sleepMillis(1000);
            }
            exitService();
        }
    }
}
