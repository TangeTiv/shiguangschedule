package com.xingheyuzhuan.shiguangschedule

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.xingheyuzhuan.shiguangschedule.BuildConfig
import com.xingheyuzhuan.shiguangschedule.data.sync.SyncManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject

@HiltAndroidApp
class MyApplication : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var syncManager: SyncManager

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // 清理临时文件
        clearShareTempFiles()

        //启动同步管理器（由 Hilt 注入后直接使用）
        syncManager.startAllSynchronizers()

        // 初始化离线仓库
        CoroutineScope(Dispatchers.IO).launch {
            initOfflineRepo()
        }
    }

    private suspend fun initOfflineRepo() = withContext(Dispatchers.IO) {
        val repoDir = File(filesDir, "repo")
        val versionFile = File(repoDir, ".extracted_version")
        val currentVersion = BuildConfig.VERSION_CODE

        // 判断是否需要解压
        var needsExtract = !repoDir.exists() || repoDir.list()?.isEmpty() != false
        if (!needsExtract && versionFile.exists()) {
            val storedVersion = try { versionFile.readText().trim().toInt() } catch (_: Exception) { 0 }
            needsExtract = storedVersion != currentVersion
        }

        if (!needsExtract) return@withContext

        if (!repoDir.exists()) repoDir.mkdirs()

        try {
            // 1. 解压已有的 offline_repo（原逻辑）
            copyAssets("offline_repo", repoDir)

            // 2. 解压预打包的仓库数据（学校适配脚本 + 索引）
            copyAssets("repo", repoDir)

            // 记录版本号
            versionFile.writeText(currentVersion.toString())
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun copyAssets(assetPath: String, destDir: File) {
        val assetList = assets.list(assetPath) ?: return
        for (item in assetList) {
            val srcItemPath = "$assetPath/$item"
            val destItem = File(destDir, item)
            try {
                assets.open(srcItemPath).use { input ->
                    FileOutputStream(destItem).use { output -> input.copyTo(output) }
                }
            } catch (e: IOException) {
                destItem.mkdirs()
                copyAssets(srcItemPath, destItem)
            }
        }
    }

    private fun clearShareTempFiles() {
        val shareTempDir = File(cacheDir, "share_temp")
        if (shareTempDir.exists() && shareTempDir.isDirectory) {
            shareTempDir.listFiles()?.forEach { it.delete() }
        }
    }
}