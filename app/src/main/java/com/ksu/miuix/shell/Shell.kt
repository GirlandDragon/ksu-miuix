package com.ksu.miuix.shell

data class ShellResult(
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val isSuccess: Boolean = exitCode == 0,
)

object Shell {

    fun su(vararg commands: String): ShellResult {
        val cmd = commands.joinToString(" && ")
        return exec("su -c '$cmd'")
    }

    fun su(cmd: String): ShellResult {
        return exec("su -c '$cmd'")
    }

    fun exec(cmd: String): ShellResult {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
            val stdout = process.inputStream.bufferedReader().readText().trim()
            val stderr = process.errorStream.bufferedReader().readText().trim()
            val exitCode = process.waitFor()
            return ShellResult(stdout = stdout, stderr = stderr, exitCode = exitCode)
        } catch (e: Exception) {
            return ShellResult(stdout = "", stderr = e.message ?: "Unknown error", exitCode = -1)
        }
    }

    fun getPackages(): List<String> {
        val result = su("pm list packages")
        if (!result.isSuccess) return emptyList()
        return result.stdout.lines()
            .mapNotNull { it.removePrefix("package:").takeIf { it.isNotEmpty() } }
            .sorted()
    }

    fun getPackageInfo(packageName: String): PackageInfo? {
        val dump = su("dumpsys package $packageName").stdout
        val versionName = Regex("versionName=([\\S]+)").find(dump)?.groupValues?.get(1) ?: ""
        val versionCode = Regex("versionCode=(\\d+)").find(dump)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val label = Regex("label=([^{]+)").find(dump)?.groupValues?.get(1)?.trim() ?: packageName
        val uid = Regex("userId=(\\d+)").find(dump)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val flags = Regex("flags=\\[([\\w\\s]+)\\]").find(dump)?.groupValues?.get(1) ?: ""
        val isSystem = flags.contains("SYSTEM")

        return PackageInfo(
            packageName = packageName,
            appLabel = label,
            versionName = versionName,
            versionCode = versionCode,
            uid = uid,
            isSystem = isSystem,
        )
    }
}

data class PackageInfo(
    val packageName: String,
    val appLabel: String,
    val versionName: String,
    val versionCode: Int,
    val uid: Int,
    val isSystem: Boolean,
)
