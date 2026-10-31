package com.qcafr.gatherapp.ui.screens.certificate

import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qcafr.gatherapp.R
import com.qcafr.gatherapp.ui.theme.*
import com.qcafr.gatherapp.ui.viewmodel.QuestionnaireViewModel
import com.qcafr.gatherapp.util.PdfExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Certificate brand palette
private val CertBlue      = Color(0xFF00529C)
private val CertBlueDark  = Color(0xFF003D7A)
private val CertBlueLight = Color(0xFF0284C7)
private val CertGold      = Color(0xFFD4A017)
private val CertText      = Color(0xFF0A1628)

@Composable
fun CertificateScreen(
    submissionId: Long,
    participantCode: String,
    viewModel: QuestionnaireViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val currentUser by viewModel.currentUser.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val region = uiState.regionConfig

    var isLoading        by remember { mutableStateOf(true) }
    var certData         by remember { mutableStateOf<Map<String, Any>?>(null) }
    var errorMsg         by remember { mutableStateOf<String?>(null) }
    var isActing         by remember { mutableStateOf(false) }
    var isExporting      by remember { mutableStateOf(false) }
    var showApproveDialog by remember { mutableStateOf(false) }
    var showRejectDialog  by remember { mutableStateOf(false) }

    val isManagerOrAdmin = currentUser?.role == "ADMIN" || currentUser?.role == "USER_MANAGER"

    LaunchedEffect(submissionId) {
        isLoading = true; errorMsg = null
        try {
            val res = viewModel.apiService.getCertificate(submissionId)
            if (res.isSuccessful && res.body() != null) {
                @Suppress("UNCHECKED_CAST")
                certData = res.body()!!["data"] as? Map<String, Any>
            } else {
                errorMsg = if (res.code() == 404) "NO_CERT" else "HTTP ${res.code()}"
            }
        } catch (e: Exception) { errorMsg = e.message }
        isLoading = false
    }

    fun doApprove() {
        scope.launch {
            isActing = true
            try {
                val body = mapOf("approved_by" to (currentUser?.fullName ?: currentUser?.username ?: "Manager"))
                val res  = viewModel.apiService.approveSubmission(submissionId, body)
                if (res.isSuccessful) {
                    Toast.makeText(context, "Certificate issued!", Toast.LENGTH_LONG).show()
                    val certRes = viewModel.apiService.getCertificate(submissionId)
                    if (certRes.isSuccessful && certRes.body() != null) {
                        @Suppress("UNCHECKED_CAST")
                        certData = certRes.body()!!["data"] as? Map<String, Any>
                        errorMsg = null
                    }
                } else Toast.makeText(context, "Failed to approve", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) { Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show() }
            isActing = false
        }
    }

    fun doReject() {
        scope.launch {
            isActing = true
            try {
                val body = mapOf("rejected_by" to (currentUser?.fullName ?: currentUser?.username ?: "Manager"))
                val res  = viewModel.apiService.rejectSubmission(submissionId, body)
                if (res.isSuccessful) { Toast.makeText(context, "Rejected.", Toast.LENGTH_SHORT).show(); onBack() }
                else Toast.makeText(context, "Reject failed", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) { Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show() }
            isActing = false
        }
    }

    fun downloadCertPdf() {
        if (certData == null || isExporting) return
        isExporting = true
        scope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    PdfExporter.exportCertificatePdf(
                        context = context,
                        cert = certData!!,
                        agencyName = region.agencyNameUpper,
                        place = region.place
                    )
                }
                Toast.makeText(context, "PDF saved: ${file.name}", Toast.LENGTH_LONG).show()
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                try { context.startActivity(intent) }
                catch (_: Exception) { Toast.makeText(context, "Saved: ${file.name}", Toast.LENGTH_SHORT).show() }
            } catch (e: Exception) {
                Toast.makeText(context, "PDF Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally { isExporting = false }
        }
    }

    // Dialogs
    if (showApproveDialog) {
        AlertDialog(
            onDismissRequest = { showApproveDialog = false },
            containerColor = BgCard,
            title = { Text("Approve & Issue Certificate", color = TextWhite, fontWeight = FontWeight.Bold) },
            text  = { Text("Approve submission #$submissionId for \"$participantCode\" and issue an official certificate?", color = TextMuted) },
            confirmButton = {
                Button(onClick = { showApproveDialog = false; doApprove() },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald)) { Text("Approve") }
            },
            dismissButton = { TextButton(onClick = { showApproveDialog = false }) { Text("Cancel", color = TextMuted) } }
        )
    }
    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            containerColor = BgCard,
            title = { Text("Reject Submission", color = TextWhite, fontWeight = FontWeight.Bold) },
            text  = { Text("Reject submission #$submissionId? No certificate will be issued.", color = TextMuted) },
            confirmButton = {
                Button(onClick = { showRejectDialog = false; doReject() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))) { Text("Reject") }
            },
            dismissButton = { TextButton(onClick = { showRejectDialog = false }) { Text("Cancel", color = TextMuted) } }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgPrimary)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(CertBlueDark, CertBlue)))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null, tint = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Certificate of Competency", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("የብቃት ምስክር ወረቀት · CRVS Agency", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
            }
            Icon(Icons.Default.Verified, null, tint = CertGold, modifier = Modifier.size(28.dp))
        }

        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = CertBlue)
            }
            certData != null -> CertDoc(
                cert = certData!!,
                agencyName = region.agencyName,
                place = region.place,
                isExporting = isExporting,
                onDownload = ::downloadCertPdf
            )
            errorMsg == "NO_CERT" -> NoCertState(
                id = submissionId,
                code = participantCode,
                isManager = isManagerOrAdmin,
                isActing = isActing,
                onApprove = { showApproveDialog = true },
                onReject  = { showRejectDialog = true }
            )
            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: $errorMsg", color = TextMuted, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Certificate Document — uses certificate_template.png as background
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CertDoc(
    cert: Map<String, Any>,
    agencyName: String,
    place: String,
    isExporting: Boolean,
    onDownload: () -> Unit
) {
    // ── Parse system data ──────────────────────────────────────────────────────
    val certNumber      = cert["certificate_number"] as? String ?: "N/A"
    val participantCode = cert["participant_code"]   as? String ?: "N/A"
    val fullName        = (cert["full_name"] as? String ?: participantCode).trim()
    val facility        = (cert["wereda_kebele"] as? String)?.ifBlank { agencyName }
                            ?: agencyName
    val rawGender       = (cert["gender"] as? String) ?: "Female"
    val gender          = if (rawGender.uppercase().startsWith("M") || rawGender.contains("ወንድ"))
                              "Male (ወንድ)" else "Female (ሴት)"
    val issuedAt        = (cert["issued_at"] as? String)?.take(10) ?: "—"
    val role            = (cert["role"] as? String)?.ifBlank { "Vital Events Registrar" }
                            ?: "Vital Events Registrar"
    val testType        = ((cert["test_type"] as? String) ?: "POST").uppercase()
    val testTypeLabel   = if (testType.startsWith("POST")) "Post-Test (ድህረ-ሙከራ)" else "Pre-Test (ቅድመ-ሙከራ)"
    val scorePct        = (cert["score_percentage"] as? Number)?.toInt() ?: 0
    val totalScore      = (cert["total_score"]      as? Number)?.toInt() ?: 0
    val maxScore        = (cert["max_score"]        as? Number)?.toInt() ?: 24
    val band            = (cert["performance_band"] as? String) ?: "SATISFACTORY"
    val issuedBy        = cert["issued_by"] as? String ?: "$agencyName Director"
    val submissionId    = cert["submission_id"] ?: cert["id"] ?: "—"

    val bandColor = when (band) {
        "STRONG"              -> Color(0xFF10B981)
        "SATISFACTORY"        -> Color(0xFF0284C7)
        "NEEDS_REINFORCEMENT" -> Color(0xFFF59E0B)
        else                  -> Color(0xFFEF4444)
    }
    val bandLabel = when (band) {
        "STRONG"              -> "STRONG — ከፍተኛ ብቃት"
        "SATISFACTORY"        -> "SATISFACTORY — አጥጋቢ"
        "NEEDS_REINFORCEMENT" -> "NEEDS REINFORCEMENT — ተጨማሪ ድጋፍ ያስፈልጋል"
        else                  -> band
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
    ) {
        // ── Certificate Card with template background ─────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.707f) // A4 portrait ratio
        ) {
            // Background: certificate_template.png
            Image(
                painter = painterResource(id = R.drawable.certificate_template),
                contentDescription = "Certificate Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )

            // Content overlaid on the background
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── HEADER ────────────────────────────────────────────────────
                Spacer(Modifier.height(6.dp))

                // Ethiopian flag + Title + Doc No row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ethiopian flag
                    Box(
                        modifier = Modifier
                            .size(width = 38.dp, height = 24.dp)
                            .clip(RoundedCornerShape(2.dp))
                    ) {
                        Column(Modifier.fillMaxSize()) {
                            Box(Modifier.weight(1f).fillMaxWidth().background(Color(0xFF078930)))
                            Box(Modifier.weight(1f).fillMaxWidth().background(Color(0xFFFCDD09)))
                            Box(Modifier.weight(1f).fillMaxWidth().background(Color(0xFFDA121A)))
                        }
                        Box(
                            modifier = Modifier.size(10.dp).clip(CircleShape)
                                .background(Color(0xFF0F47AF)).align(Alignment.Center),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("★", color = Color(0xFFFCDD09), fontSize = 6.sp, lineHeight = 6.sp)
                        }
                    }

                    // Center title block
                    Column(
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "CERTIFICATE OF COMPETENCY",
                            color = Color(0xFF004C99),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            "የወሳኝ ሁነቶች ምዝገባ ብቃት ማረጋገጫ",
                            color = Color(0xFF0284C7),
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            agencyName.uppercase(),
                            color = Color(0xFF0284C7),
                            fontSize = 6.5.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Doc number
                    Column(horizontalAlignment = Alignment.End) {
                        Text("DOC. No.", color = Color(0xFF0284C7), fontSize = 6.sp, fontWeight = FontWeight.Bold)
                        Text(
                            certNumber,
                            color = Color(0xFF0B1F3A),
                            fontSize = 7.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // ── RECIPIENT NAME — large, prominent ─────────────────────────
                Text(
                    "This certifies that",
                    color = Color(0xFF0284C7),
                    fontSize = 9.sp,
                    fontStyle = FontStyle.Italic
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    fullName,
                    color = Color(0xFF003D7A),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.Serif
                )
                Text(
                    "Participant Code: $participantCode",
                    color = Color(0xFF0284C7),
                    fontSize = 8.sp
                )

                Spacer(Modifier.height(6.dp))

                // Decorative divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, Color(0xFF0284C7), Color.Transparent)
                            )
                        )
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    "has successfully demonstrated competency in Civil Registration and\nVital Statistics (CRVS) as required under Federal Vital Events\nRegistration Proclamation No. 1370/2024",
                    color = Color(0xFF003D7A),
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp
                )

                Spacer(Modifier.height(10.dp))

                // ── KEY INFO GRID ─────────────────────────────────────────────
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CertInfoBox("Place of Registration", facility, Modifier.weight(1f))
                    CertInfoBox("Assessment Type", testTypeLabel, Modifier.weight(1f))
                    CertInfoBox("Gender", gender, Modifier.weight(1f))
                }

                Spacer(Modifier.height(6.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CertInfoBox("Professional Role", role, Modifier.weight(1f))
                    CertInfoBox("Date of Issuance", issuedAt, Modifier.weight(1f))
                    CertInfoBox("System Record ID", "CRVS-REC-$submissionId", Modifier.weight(1f))
                }

                Spacer(Modifier.height(8.dp))

                // ── SCORE BAND — center highlight ─────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bandColor.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$totalScore / $maxScore pts", color = Color(0xFF003D7A), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Score Earned", color = Color(0xFF0284C7), fontSize = 7.sp)
                        }
                        Box(Modifier.width(1.dp).height(36.dp).background(Color(0xFF0284C7).copy(alpha = 0.3f)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$scorePct%", color = bandColor, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Percentage", color = Color(0xFF0284C7), fontSize = 7.sp)
                        }
                        Box(Modifier.width(1.dp).height(36.dp).background(Color(0xFF0284C7).copy(alpha = 0.3f)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(bandLabel, color = bandColor, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                            Text("Performance Band", color = Color(0xFF0284C7), fontSize = 7.sp)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // ── LEGAL / ADMIN INFO ────────────────────────────────────────
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CertInfoBox("Issuing Authority", agencyName, Modifier.weight(1f))
                    CertInfoBox("Legal Framework", "Proc. No. 1370/2024", Modifier.weight(1f))
                    CertInfoBox("Credential Number", certNumber, Modifier.weight(1f))
                }

                Spacer(Modifier.weight(1f))

                // ── FOOTER: Signature + Seal ──────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Signature
                    Column {
                        Text(
                            issuedBy,
                            fontFamily = FontFamily.Cursive,
                            color = Color(0xFF0C4A6E),
                            fontSize = 14.sp,
                            modifier = Modifier.rotate(-3f)
                        )
                        Box(Modifier.width(130.dp).height(1.dp).background(Color(0xFF0284C7)))
                        Text("Authorized Registrar / የተፈቀደ ፊርማ", color = Color(0xFF0284C7), fontSize = 6.sp)
                        Text("Place: $place", color = Color(0xFF0284C7).copy(alpha = 0.7f), fontSize = 6.sp)
                    }

                    // Official stamp
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .rotate(-6f)
                            .clip(CircleShape)
                            .background(Color(0xFF003D7A).copy(alpha = 0.07f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AccountBalance, null,
                                tint = Color(0xFF003D7A), modifier = Modifier.size(20.dp))
                            Text("OFFICIAL", color = Color(0xFF003D7A), fontSize = 4.5.sp, fontWeight = FontWeight.ExtraBold)
                            Text("SEAL", color = Color(0xFF003D7A), fontSize = 4.5.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))
            }
        }

        Spacer(Modifier.height(16.dp))

        // Download Button
        Button(
            onClick = onDownload,
            enabled = !isExporting,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00529C))
        ) {
            if (isExporting) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Generating PDF...", fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Download Certificate PDF (አውርድ)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

// ── Small info box composable ─────────────────────────────────────────────────

@Composable
private fun CertInfoBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color(0xFF003D7A).copy(alpha = 0.06f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 5.dp)
    ) {
        Text(label, color = Color(0xFF0284C7), fontSize = 6.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Color(0xFF003D7A), fontSize = 8.5.sp, fontWeight = FontWeight.Bold, lineHeight = 11.sp)
    }
}

// ── No Certificate pending state ──────────────────────────────────────────────

@Composable
private fun NoCertState(
    id: Long, code: String, isManager: Boolean, isActing: Boolean,
    onApprove: () -> Unit, onReject: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.HourglassTop, null, modifier = Modifier.size(72.dp), tint = Color(0xFFF59E0B))
        Spacer(Modifier.height(16.dp))
        Text("Awaiting Manager Approval", color = TextWhite, fontSize = 20.sp,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "Submission #$id for \"$code\" is pending review.\n\n" +
            "If the score is ≥ 50%, a manager can approve it to issue an official certificate.",
            color = TextMuted, fontSize = 13.sp, textAlign = TextAlign.Center
        )
        if (isManager) {
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onApprove, enabled = !isActing,
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald)
            ) {
                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Approve & Issue Certificate", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onReject, enabled = !isActing,
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444))
            ) {
                Icon(Icons.Default.Cancel, null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Reject Submission", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
            }
        }
    }
}
