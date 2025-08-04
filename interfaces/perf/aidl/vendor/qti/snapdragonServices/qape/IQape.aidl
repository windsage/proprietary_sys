/*
 * Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package vendor.qti.snapdragonServices.qape;

import vendor.qti.snapdragonServices.qape.CpuHeadroomType;
import vendor.qti.snapdragonServices.qape.IQapeCallback;
import vendor.qti.snapdragonServices.qape.IThermalZoneCallback;
import vendor.qti.snapdragonServices.qape.SupportedFrameRate;

@VintfStability
interface IQape {
    // Binds the package name with the current session.
    int setPkg(in String pkgName);

    // Adds the virtual utilization on all available CPUs by a percentage specified by the input parameter.
    int boostCpu(in int boostVal);

    // Boosts the GPU load on the GPU by a percentage specified by the input parameter boost_val.
    int boostGpu(in int boostVal);

    // Informs the system about a short-running critical application thread that must be scheduled immediately on a CPU.
    int hintLowLatency(in int tid);

    // Informs the system about a specific high CPU utilization thread that requires a high-performance core on the SoC.
    int hintHighCpuutil(in int tid);

    // Informs the system about a specific low CPU utilization thread that does not require the high-performance core.
    int hintLowCpuutil(in int tid);

    // Provides a list of threads required to run every Vsync/draw cycle.
    int hintThreadPipeline(in int[] tids);

    // Hints to release if any hint is provided for the thread, this is to reset any thread to the default.
    int releaseThreadHints(in int category, in int tid);

    // Returns the available average/minimum GPU headroom in percentage for last ‘duration’ seconds.
    int getGpuHeadroom(in int pastDuration, in boolean average);

    // Checks if the predefined OEM scenario is supported.
    int getScenarioSupport(in String scenarioId);

    // Starts the OEM predefined scenario.
    int startScenario(in String scenarioId);

    // Stops the OEM scenario.
    int stopScenario(in String scenarioId);

    // Registers a callback to monitor device temperature changes. 
    int registerThermalZoneCallback(in IThermalZoneCallback callback);

    // Unregisters a thermal headroom callback.
    int unregisterThermalZoneCallback(in IThermalZoneCallback callback);

    // Queries for current thermal status and returns how much headroom is left from thermal mitigation.
    int queryThermalHeadroomStatus();

    // Gets details on the maximum number of pipeline threads supported by this platform.
    int getPipelineThreadMaxNumber();

    // Sets the pipeline number.
    int setPipelineNumber(in int number);

    // Resets the pipeline thread number to zero.
    int resetPipelineNumber();

    // Sets the desired content rate; used by the platform to adjust display strategy.
    int setDesiredContentRate(in SupportedFrameRate contentRate);

    // Gets the CPU headroom in percentage for last ‘duration’ seconds.
    int getCpuHeadroom(in CpuHeadroomType type, in int cpuCoreClusterNo, in int duration);

    // Gets the specific data for specific game.
    int appGetData(in int gameID, in int cmdType, inout double[] value);

    // Sets the game command type, callback will be triggered if available.
    int appSet(in int gameID, in int cmdType);

    // Sets the game command type, config key and value, callback will be triggered if available.
    int appSetConfig(in int gameID, in int cmdType, in String[] key, in double[] value);

    // Sets the game data, callback will be triggered if available.
    int appSetData(in int gameID, in int cmdType, in double[] value);

    // Register OEM callback.
    int oemRegisterCallback(in IQapeCallback callback, in int clientId);

    // Unregister OEM callback.
    int oemUnregisterCallback(in IQapeCallback callback, in int clientId);

    // OEM sets data.
    int oemSetData(in int gameID, in int cmdType, in double[] value);
}
