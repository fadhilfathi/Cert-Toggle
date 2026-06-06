package com.example.certtoggle.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

data class CertInfo(
  val fileName: String,
  val filePath: String,
  val subject: String,
  val issuer: String,
  val isDisabled: Boolean
)

interface DataRepository {
  val certificates: Flow<List<CertInfo>>
  suspend fun refresh()
  suspend fun toggleAll(disable: Boolean): Boolean
}

class DefaultDataRepository : DataRepository {
  private val _certificates = MutableStateFlow<List<CertInfo>>(emptyList())
  override val certificates: Flow<List<CertInfo>> = _certificates.asStateFlow()

  override suspend fun refresh() {
    withContext(Dispatchers.IO) {
      val certDirs = listOf(
        "/system/etc/security/cacerts",
        "/apex/com.android.conscrypt/cacerts"
      )
      val certList = mutableListOf<CertInfo>()
      
      try {
        val factory = CertificateFactory.getInstance("X.509")
        val mountsContent = getMountsContent()

        for (dirPath in certDirs) {
          val dir = File(dirPath)
          if (dir.exists() && dir.isDirectory) {
            val files = dir.listFiles() ?: continue
            for (file in files) {
              if (file.isFile) {
                try {
                  file.inputStream().use { fis ->
                    val cert = factory.generateCertificate(fis) as? X509Certificate
                    if (cert != null) {
                      val subject = cert.subjectDN.name
                      val issuer = cert.issuerDN.name
                      
                      if (containsKeyword(subject, issuer)) {
                        val disabled = mountsContent.contains(file.absolutePath)
                        certList.add(
                          CertInfo(
                            fileName = file.name,
                            filePath = file.absolutePath,
                            subject = subject,
                            issuer = issuer,
                            isDisabled = disabled
                          )
                        )
                      }
                    }
                  }
                } catch (e: Exception) {
                  // Skip invalid certificates
                }
              }
            }
          }
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }
      
      _certificates.value = certList
    }
  }

  private fun getMountsContent(): String {
    return try {
      val file = File("/proc/mounts")
      if (file.exists()) {
        file.readText()
      } else {
        val process = Runtime.getRuntime().exec("mount")
        process.inputStream.bufferedReader().use { it.readText() }
      }
    } catch (e: Exception) {
      ""
    }
  }

  private fun containsKeyword(subject: String, issuer: String): Boolean {
    val keywords = listOf("digicert", "globalsign", "ssl")
    val subLower = subject.lowercase()
    val issLower = issuer.lowercase()
    return keywords.any { kw -> subLower.contains(kw) || issLower.contains(kw) }
  }

  override suspend fun toggleAll(disable: Boolean): Boolean {
    return withContext(Dispatchers.IO) {
      val certs = _certificates.value
      if (certs.isEmpty()) return@withContext true

      val commands = certs.map { cert ->
        if (disable) {
          "mount -o bind /dev/null \"${cert.filePath}\" 2>/dev/null"
        } else {
          "umount \"${cert.filePath}\" 2>/dev/null"
        }
      }

      val script = commands.joinToString("\n")
      val success = runAsRoot(script)
      
      // Refresh state after toggling
      refresh()
      
      success
    }
  }

  private fun runAsRoot(script: String): Boolean {
    return try {
      val process = Runtime.getRuntime().exec("su")
      process.outputStream.use { os ->
        os.write((script + "\n").toByteArray())
        os.write("exit\n".toByteArray())
        os.flush()
      }
      val exitCode = process.waitFor()
      exitCode == 0
    } catch (e: Exception) {
      e.printStackTrace()
      false
    }
  }
}
