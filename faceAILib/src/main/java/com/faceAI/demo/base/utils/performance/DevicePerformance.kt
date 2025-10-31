package com.faceAI.demo.base.utils.performance

import android.app.ActivityManager
import android.content.Context
import android.opengl.GLES20
import android.os.Build
import android.util.Log
import com.faceAI.demo.base.utils.performance.opengl.EglCore
import com.faceAI.demo.base.utils.performance.opengl.OffscreenSurface
import java.io.IOException
import java.io.InputStream

/**
 * 简单的设备性能判断类，一台设备执行一次就够了，用SP 记录下来
 * 对应的动作活体时间可以根据此调整一些
 *
 */
object DevicePerformance {
    const val DEVICE_PERFORMANCE_UNKNOWN = -1 //那就是低性能吧
    const val DEVICE_PERFORMANCE_LOW = 0  //低性能
    const val DEVICE_PERFORMANCE_MIDDLE = 1 //中配
    const val DEVICE_PERFORMANCE_HIGH = 2  //高性能

    // 内存阈值常量
    private const val MEMORY_THRESHOLD_LOW = 2000L       // 2GB
    private const val MEMORY_THRESHOLD_MIDDLE = 4000L    // 4GB
    private const val MEMORY_THRESHOLD_HIGH = 6000L      // 6GB
    private const val MEMORY_THRESHOLD_FLAGSHIP = 8000L  // 8GB

    // CPU频率阈值常量（MHz）
    private const val CPU_FREQ_LOW = 1800
    private const val CPU_FREQ_MID_LOW = 2000
    private const val CPU_FREQ_MID_HIGH = 2500

    // CPU核心数阈值
    private const val CPU_CORE_THRESHOLD = 4

    fun getTotalMemory(context: Context): Long {
        return try {
            val memoryInfo = ActivityManager.MemoryInfo()
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.getMemoryInfo(memoryInfo)

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                1024L // 低版本返回默认1GB
            } else {
                memoryInfo.totalMem ushr 20 // 转换为MB
            }
        } catch (exception: Throwable) {
            -1L
        }
    }

    /**
     * 获取手机内存级别
     */
    private fun getMemoryLevel(context: Context): Int {
        val ramMB = getTotalMemory(context)

        return when {
            ramMB <= MEMORY_THRESHOLD_LOW -> 0    // 2G或以下
            ramMB <= MEMORY_THRESHOLD_MIDDLE -> 1  // 2-4G
            ramMB <= MEMORY_THRESHOLD_HIGH -> 2    // 4-6G
            ramMB <= MEMORY_THRESHOLD_FLAGSHIP -> 3 // 6-8G
            else -> 4                              // 8G以上
        }
    }

    fun getCpuCoreCount(): Int {
        return try {
            val coreCount = Runtime.getRuntime().availableProcessors()
            if (coreCount < 1) 1 else coreCount
        } catch (exception: Throwable) {
            1
        }
    }

    fun getMaxCpuFreq(): Long {
        var maxCpuFreq = 0L

        try {
            val cpuCount = getCpuCoreCount()

            for (coreIndex in 0 until cpuCount) {
                val coreFreq = getCoreMaxFreq(coreIndex)
                if (coreFreq > maxCpuFreq) {
                    maxCpuFreq = coreFreq
                }
            }
        } catch (exception: IOException) {
            // 保持原有异常处理
        }

        return maxCpuFreq
    }

    /**
     * 获取单个核心的最大频率
     */
    private fun getCoreMaxFreq(coreIndex: Int): Long {
        return try {
            val fileName = "/sys/devices/system/cpu/cpu$coreIndex/cpufreq/cpuinfo_max_freq"
            val processBuilder = ProcessBuilder("/system/bin/cat", fileName)
            val process = processBuilder.start()

            val output = readProcessOutput(process.inputStream)
            process.inputStream.close()

            output.trim().toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * 读取进程输出流
     */
    private fun readProcessOutput(inputStream: InputStream): String {
        val buffer = ByteArray(24)
        val result = StringBuilder()

        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            result.append(String(buffer, 0, bytesRead))
        }

        return result.toString()
    }

    private fun getCPULevel(): Int {
        val cpuCoreCount = getCpuCoreCount()

        // 4核或以下直接判定为低端
        if (cpuCoreCount <= CPU_CORE_THRESHOLD) {
            return 0
        }

        val maxCpuFreq = getMaxCpuFreq()
        if (maxCpuFreq <= 0) {
            return 2 // 无法获取频率时默认中高端
        }

        val freqMHz = (maxCpuFreq / 100000 * 100).toInt()

        return when {
            freqMHz <= CPU_FREQ_LOW -> 0      // 1.8GHz及以下
            freqMHz <= CPU_FREQ_MID_LOW -> 1   // 1.8-2.0GHz
            freqMHz <= CPU_FREQ_MID_HIGH -> 2  // 2.0-2.5GHz
            else -> 3                          // 2.5GHz以上
        }
    }

    /**
     * 获取设备的性能分类
     */
    fun getDevicePerformance(context: Context): Int {
        val isLowSDK = Build.VERSION.SDK_INT < Build.VERSION_CODES.O
        val memoryLevel = getMemoryLevel(context)
        val cpuLevel = getCPULevel()
        val gpuName = getGpuName()
        val hasLowPerformanceGpu = gpuName.startsWith("PowerVR Rogue")

        return determinePerformanceLevel(isLowSDK, memoryLevel, cpuLevel, hasLowPerformanceGpu)
    }

    /**
     * 确定性能等级的核心逻辑
     */
    private fun determinePerformanceLevel(
        isLowSDK: Boolean,
        memoryLevel: Int,
        cpuLevel: Int,
        hasLowPerformanceGpu: Boolean
    ): Int {
        // 低版本SDK直接判定为低性能
        if (isLowSDK) {
            return DEVICE_PERFORMANCE_LOW
        }

        return when {
            isLowEndDevice(memoryLevel, cpuLevel) -> {
                if (shouldUpgradeToMiddleEnd(cpuLevel, memoryLevel, hasLowPerformanceGpu)) {
                    DEVICE_PERFORMANCE_MIDDLE
                } else {
                    DEVICE_PERFORMANCE_LOW
                }
            }
            isMiddleEndDevice(memoryLevel, cpuLevel, hasLowPerformanceGpu) -> {
                DEVICE_PERFORMANCE_MIDDLE
            }
            isHighEndDevice(memoryLevel, cpuLevel, hasLowPerformanceGpu) -> {
                DEVICE_PERFORMANCE_HIGH
            }
            else -> DEVICE_PERFORMANCE_UNKNOWN
        }
    }

    /**
     * 判断是否为低端设备
     */
    private fun isLowEndDevice(memoryLevel: Int, cpuLevel: Int): Boolean {
        return memoryLevel == 0 || memoryLevel == 1 || cpuLevel == 0
    }

    /**
     * 判断是否应升级到中端设备
     */
    private fun shouldUpgradeToMiddleEnd(
        cpuLevel: Int,
        memoryLevel: Int,
        hasLowPerformanceGpu: Boolean
    ): Boolean {
        return cpuLevel >= 1 && memoryLevel > 0 && !hasLowPerformanceGpu
    }

    /**
     * 判断是否为中端设备
     */
    private fun isMiddleEndDevice(
        memoryLevel: Int,
        cpuLevel: Int,
        hasLowPerformanceGpu: Boolean
    ): Boolean {
        return memoryLevel == 2 && cpuLevel >= 1 && !hasLowPerformanceGpu
    }

    /**
     * 判断是否为高端设备
     */
    private fun isHighEndDevice(
        memoryLevel: Int,
        cpuLevel: Int,
        hasLowPerformanceGpu: Boolean
    ): Boolean {
        if (memoryLevel <= 2) return false

        return when {
            cpuLevel > 2 && !hasLowPerformanceGpu -> true
            !hasLowPerformanceGpu -> false // 中端
            else -> false // 低端
        }
    }

    /**
     * GPU 没那么重要
     *
     * @return
     */
    private fun getGpuName(): String {
        return try {
            val eglCore = EglCore(null, EglCore.FLAG_TRY_GLES3)
            val surface = OffscreenSurface(eglCore, 1, 1)
            surface.makeCurrent()

            val gpuName = GLES20.glGetString(GLES20.GL_RENDERER)

            surface.release()
            eglCore.release()

            gpuName ?: ""
        } catch (exception: Exception) {
            Log.w("PERFORMANCE", "getGpuInfo failed: ${exception.message}")
            ""
        }
    }

    
}