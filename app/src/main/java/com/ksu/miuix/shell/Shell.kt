package com.ksu.miuix.shell

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

data class ShellResult(
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val isSuccess: Boolean = exitCode == 0,
)

object Shell {

    suspend fun su(vararg commands: String): ShellResult = withContext(Dispatchers.IO) {
        val cmd = commands.joinToString(" && ")
        execInternal(listOf("su", "-c", cmd))
    }

    suspend fun su(cmd: String): ShellResult = withContext(Dispatchers.IO) {
        execInternal(listOf("su", "-c", cmd))
    }

    suspend fun exec(cmd: String): ShellResult = withContext(Dispatchers.IO) {
        execInternal(listOf("sh", "-c", cmd))
    }

    private fun execInternal(command: List<String>): ShellResult {
        try {
            val process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText().trim()
            val finished = process.waitFor(30, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return ShellResult(stdout = "", stderr = "Command timed out after 30s", exitCode = -1)
            }
            val exitCode = process.exitValue()
            return ShellResult(stdout = output, stderr = "", exitCode = exitCode)
        } catch (e: Exception) {
            return ShellResult(stdout = "", stderr = e.message ?: "Unknown error", exitCode = -1)
        }
    }

    suspend fun getPackages(): List<String> = withContext(Dispatchers.IO) {
        val result = su("pm list packages")
        if (!result.isSuccess) return@withContext emptyList()
        result.stdout.lines()
            .mapNotNull { pkg -> pkg.removePrefix("package:").takeIf { name -> name.isNotEmpty() } }
    }

    suspend fun getPackageInfo(packageName: String): PackageInfo? = withContext(Dispatchers.IO) {
        if (!packageName.matches(Regex("[a-zA-Z0-9_.]+"))) return@withContext null
        val dump = su("dumpsys package $packageName").stdout
        val versionName = Regex("versionName=([\\S]+)").find(dump)?.groupValues?.get(1) ?: ""
        val versionCode = Regex("versionCode=(\\d+)").find(dump)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val label = Regex("label=([^{]+)").find(dump)?.groupValues?.get(1)?.trim() ?: packageName
        val uid = Regex("userId=(\\d+)").find(dump)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val flags = Regex("flags=\\[([\\w\\s]+)\\]").find(dump)?.groupValues?.get(1) ?: ""
        val isSystem = flags.contains("SYSTEM")

        PackageInfo(
            packageName = packageName,
            appLabel = label,
            versionName = versionName,
            versionCode = versionCode,
            uid = uid,
            isSystem = isSystem,
        )
    }

    suspend fun detectKsu(): KsuStatus = withContext(Dispatchers.IO) {
        val versionFile = su("test -f /data/adb/ksu/version && cat /data/adb/ksu/version")
        if (versionFile.isSuccess && versionFile.stdout.isNotEmpty()) {
            return@withContext KsuStatus.Active(versionFile.stdout.trim())
        }
        val kernelVersion = su("uname -r")
        if (kernelVersion.stdout.contains("KSU", ignoreCase = true)) {
            return@withContext KsuStatus.Active(kernelVersion.stdout.trim())
        }
        val testKsu = su("which ksud")
        if (testKsu.isSuccess) {
            return@withContext KsuStatus.Active(testKsu.stdout.trim())
        }
        KsuStatus.Inactive(kernelVersion.stdout.ifEmpty { "unknown" })
    }
}

sealed class KsuStatus {
    data object Loading : KsuStatus()
    data class Active(val info: String) : KsuStatus()
    data class Inactive(val kernelInfo: String) : KsuStatus()
}

data class PackageInfo(
    val packageName: String,
    val appLabel: String,
    val versionName: String,
    val versionCode: Int,
    val uid: Int,
    val isSystem: Boolean,
)
