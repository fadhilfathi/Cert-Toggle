package com.example.certtoggle.ui.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.certtoggle.data.CertInfo
import com.example.certtoggle.data.DefaultDataRepository
import com.example.certtoggle.theme.CertToggleTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
  onItemClick: (NavKey) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: MainScreenViewModel = viewModel { MainScreenViewModel(DefaultDataRepository()) },
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  var showRootWarning by remember { mutableStateOf(false) }

  if (showRootWarning) {
    AlertDialog(
      onDismissRequest = { showRootWarning = false },
      title = { Text("Root Access Required") },
      text = { Text("Android's security model prevents non-rooted apps from programmatically disabling system certificates. You can disable them manually in settings under 'Trusted credentials'.") },
      confirmButton = {
        TextButton(
          onClick = {
            showRootWarning = false
            openSettings(context)
          }
        ) {
          Text("Open Settings")
        }
      },
      dismissButton = {
        TextButton(onClick = { showRootWarning = false }) {
          Text("Cancel")
        }
      }
    )
  }

  Scaffold(
    topBar = {
      CenterAlignedTopAppBar(
        title = { Text("Cert Toggle", fontWeight = FontWeight.Bold) },
        actions = {
          val rotation = remember { Animatable(0f) }
          val coroutineScope = rememberCoroutineScope()
          IconButton(onClick = { 
            coroutineScope.launch {
              rotation.animateTo(
                targetValue = rotation.targetValue + 360f,
                animationSpec = tween(durationMillis = 1000)
              )
            }
            viewModel.refresh() 
          }) {
            Icon(
              Icons.Default.Refresh, 
              contentDescription = "Refresh",
              modifier = Modifier.rotate(rotation.value)
            )
          }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
      )
    },
    modifier = modifier.fillMaxSize()
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      when (val currentState = state) {
        MainScreenUiState.Loading -> {
          CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        is MainScreenUiState.Success -> {
          MainContent(
            certificates = currentState.certificates,
            isToggling = currentState.isToggling,
            onToggle = { disable -> 
              if (viewModel.isRooted) {
                viewModel.toggleAll(disable)
              } else {
                showRootWarning = true
              }
            },
            modifier = Modifier.fillMaxSize()
          )
        }
      }
    }
  }
}

@Composable
fun MainContent(
  certificates: List<CertInfo>,
  isToggling: Boolean,
  onToggle: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  // If any matching certificate is disabled, then we consider the overall toggle to be OFF (disabled).
  // If all matching certificates are active/enabled, then the overall toggle is ON (trusted).
  val isAnyDisabled = certificates.any { it.isDisabled }
  val isGlobalTrusted = certificates.isNotEmpty() && !isAnyDisabled

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Global Control Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.secondaryContainer
      )
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = "Global Trust Status",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = if (certificates.isEmpty()) "No target certificates found"
                 else if (isGlobalTrusted) "Certs Trusted (Active)"
                 else "Certs Blocked (Disabled)",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          color = if (certificates.isEmpty()) MaterialTheme.colorScheme.outline
                 else if (isGlobalTrusted) Color(0xFF2E7D32)
                 else MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        if (certificates.isNotEmpty()) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "Trust certificates",
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Switch(
              checked = isGlobalTrusted,
              enabled = !isToggling,
              onCheckedChange = { trust ->
                // If the user turns the switch ON, it means they want to enable (trust) them.
                // In toggleAll: disable = !trust
                onToggle(!trust)
              }
            )
          }
          
          Spacer(modifier = Modifier.height(12.dp))
          
          val context = LocalContext.current
          Button(
            onClick = { openSettings(context) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Settings,
              contentDescription = "Settings"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open Settings", fontWeight = FontWeight.Bold)
          }
        } else {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp)
          ) {
            Icon(Icons.Default.Info, contentDescription = "Info", tint = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Make sure your device is rooted to find certificates in system paths.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.outline
            )
          }
        }
        
        if (isToggling) {
          Spacer(modifier = Modifier.height(8.dp))
          LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // List Title
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Target Certificates (${certificates.size})",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
    }

    if (certificates.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            Icons.Default.Warning,
            contentDescription = "Warning",
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.outline
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "No certificates found matching DigiCert, GlobalSign, or SSL.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = 32.dp),
            textAlign = TextAlign.Center
          )
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(certificates) { cert ->
          CertificateItem(cert)
        }
      }
    }
  }
}

@Composable
fun CertificateItem(cert: CertInfo) {
  val cn = getCommonName(cert.subject)
  
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant
    ),
    shape = RoundedCornerShape(12.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = Icons.Default.Lock,
        contentDescription = "Lock",
        tint = if (cert.isDisabled) MaterialTheme.colorScheme.error else Color(0xFF2E7D32),
        modifier = Modifier.size(32.dp)
      )
      Spacer(modifier = Modifier.width(16.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = cn,
          fontWeight = FontWeight.Bold,
          fontSize = 16.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = "Alias: ${cert.fileName}",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.outline
        )
        Text(
          text = if (cert.filePath.contains("apex")) "Storage: APEX Module" else "Storage: System Partition",
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.outline
        )
      }
      
      Spacer(modifier = Modifier.width(8.dp))
      
      Surface(
        color = if (cert.isDisabled) MaterialTheme.colorScheme.errorContainer else Color(0xFFE8F5E9),
        shape = RoundedCornerShape(4.dp)
      ) {
        Text(
          text = if (cert.isDisabled) "BLOCKED" else "TRUSTED",
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          color = if (cert.isDisabled) MaterialTheme.colorScheme.onErrorContainer else Color(0xFF2E7D32)
        )
      }
    }
  }
}

fun getCommonName(dn: String): String {
  val regex = "CN=([^,]+)".toRegex(RegexOption.IGNORE_CASE)
  val match = regex.find(dn)
  return match?.groupValues?.get(1) ?: dn
}

fun openSettings(context: android.content.Context) {
  try {
    val intent = android.content.Intent("com.android.settings.TRUSTED_CREDENTIALS")
    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
  } catch (e: Exception) {
    try {
      val intent = android.content.Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS)
      intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(intent)
    } catch (ex: Exception) {
      val intent = android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
      intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(intent)
    }
  }
}
