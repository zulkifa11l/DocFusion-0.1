package com.example

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.example.util.DocFusionUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.entity.Note
import com.example.data.entity.SavedFile
import com.example.ui.DocFusionViewModel
import com.example.ui.theme.DarkNavy
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.PremiumNavyCard
import com.example.ui.theme.HintGray
import com.example.ui.theme.BrightWhite
import com.example.ui.theme.MyApplicationTheme

@OptIn(ExperimentalPermissionsApi::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                DocFusionApp()
            }
        }
    }
}

@Composable
fun DocFusionApp() {
    val viewModel: DocFusionViewModel = viewModel()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val isLocked by viewModel.isAppLocked.collectAsState()
    val securityEnabled by viewModel.securityEnabled.collectAsState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        bottomBar = {
            if (!isLocked || !securityEnabled) {
                DocFusionBottomAppBar(
                    currentScreen = currentScreen,
                    onNavigate = { viewModel.navigateTo(it) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(DarkNavy, Color(0xFF070B19))))
                .padding(innerPadding)
        ) {
            if (securityEnabled && isLocked) {
                PinLockScreen(viewModel)
            } else {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                    },
                    label = "ScreenTransition"
                ) { screen ->
                    when (screen) {
                        "dashboard" -> DashboardScreen(viewModel)
                        "pdftools" -> PdfToolsScreen(viewModel)
                        "scanner" -> SmartScannerScreen(viewModel)
                        "idcard" -> IdCardScannerScreen(viewModel)
                        "ocr" -> OcrToolsScreen(viewModel)
                        "photo" -> PhotoToolsScreen(viewModel)
                        "qr" -> QrToolsScreen(viewModel)
                        "notes" -> NotesScreen(viewModel)
                        "resume" -> ResumeBuilderScreen(viewModel)
                        "filemanager" -> FileManagerScreen(viewModel)
                        "docreader" -> DocumentReaderScreen(viewModel)
                        "settings" -> SettingsScreen(viewModel)
                        "help" -> HelpSupportScreen(viewModel)
                        else -> DashboardScreen(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun DocFusionBottomAppBar(currentScreen: String, onNavigate: (String) -> Unit) {
    NavigationBar(
        containerColor = PremiumNavyCard,
        contentColor = BrightWhite,
        tonalElevation = 8.dp,
        modifier = Modifier.height(72.dp)
    ) {
        val items = listOf(
            Triple("dashboard", "Home", Icons.Default.Dashboard),
            Triple("scanner", "Scanner", Icons.Default.QrCodeScanner),
            Triple("notes", "Notes", Icons.Default.Note),
            Triple("filemanager", "Files", Icons.Default.Folder),
            Triple("settings", "Settings", Icons.Default.Settings)
        )

        items.forEach { (route, label, icon) ->
            val isSelected = currentScreen == route || (route == "scanner" && (currentScreen == "idcard" || currentScreen == "ocr" || currentScreen == "qr"))
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(route) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) CyanAccent else HintGray,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = label,
                        color = if (isSelected) CyanAccent else HintGray,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color(0xFF1E2E54)
                ),
                modifier = Modifier.testTag("nav_item_$route")
            )
        }
    }
}

// PIN Passcode lock protection overlay
@Composable
fun PinLockScreen(viewModel: DocFusionViewModel) {
    val enteredPIN by viewModel.enteredPIN.collectAsState()
    val realPIN by viewModel.appPIN.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "App Locked",
            tint = CyanAccent,
            modifier = Modifier.size(72.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "DocFusion Vault Locked",
            color = BrightWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
        )
        
        Text(
            text = "Enter your secure PIN to access documents",
            color = HintGray,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Display Dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            for (i in 0 until 4) {
                val filled = enteredPIN.length > i
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (filled) CyanAccent else Color(0xFF1E2E54))
                        .border(1.dp, if (filled) CyanAccent else Color.Gray, CircleShape)
                )
            }
        }

        // Numeric Grid
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "Clear", "0", "OK")
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.width(300.dp)
        ) {
            items(keys) { key ->
                Button(
                    onClick = {
                        when (key) {
                            "Clear" -> {
                                if (enteredPIN.isNotEmpty()) {
                                    viewModel.enteredPIN.value = enteredPIN.dropLast(1)
                                }
                            }
                            "OK" -> {
                                if (enteredPIN == realPIN || realPIN.isEmpty()) {
                                    viewModel.isAppLocked.value = false
                                    viewModel.enteredPIN.value = ""
                                    Toast.makeText(context, "Access Granted", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.enteredPIN.value = ""
                                    Toast.makeText(context, "Invalid PIN, Try again!", Toast.LENGTH_SHORT).show()
                                }
                            }
                            else -> {
                                if (enteredPIN.length < 4) {
                                    viewModel.enteredPIN.value = enteredPIN + key
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PremiumNavyCard,
                        contentColor = BrightWhite
                    ),
                    modifier = Modifier
                        .height(64.dp)
                        .testTag("pin_key_$key"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = key,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (key == "OK") CyanAccent else BrightWhite
                    )
                }
            }
        }
    }
}

// MAIN DASHBOARD
@Composable
fun DashboardScreen(viewModel: DocFusionViewModel) {
    val totalFiles by viewModel.savedFiles.collectAsState()
    val totalNotes by viewModel.notes.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // App branding area (matched exactly with Elegant Dark header element)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.horizontalGradient(listOf(Color(0xFF06B6D4), Color(0xFF1E40AF))))
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "DF",
                            color = DarkNavy,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DocFusion",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrightWhite,
                        letterSpacing = (-0.5).sp
                    )
                }
                Text(
                    text = "Scan • Convert • Manage",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF22D3EE), // Cyan accent
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(start = 40.dp, top = 2.dp)
                )
            }

            IconButton(
                onClick = { viewModel.navigateTo("help") },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PremiumNavyCard.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.Help,
                    contentDescription = "Help & Support",
                    tint = CyanAccent,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // --- HERO BANNER (Smart Scanner promo style) ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
                .clickable { viewModel.navigateTo("scanner") },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF0891B2), Color(0xFF1E40AF))
                        )
                    )
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Smart Scanner",
                        color = BrightWhite,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Auto-edge detection & perspective correction",
                        color = Color(0xFFCFFAFE), // cyan-100 fallback
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.navigateTo("scanner") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrightWhite,
                                contentColor = Color(0xFF0F172A)
                            ),
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Text(
                                "Start Scanning",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(BrightWhite.copy(alpha = 0.2f))
                                .clickable { viewModel.navigateTo("filemanager") }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "Browse Files",
                                tint = BrightWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Stats Panel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = PremiumNavyCard),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Saved Docs", color = HintGray, fontSize = 12.sp)
                    Text(
                        "${totalFiles.size} Files",
                        color = CyanAccent,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = PremiumNavyCard),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Secure Notes", color = HintGray, fontSize = 12.sp)
                    Text(
                        "${totalNotes.size} Saved",
                        color = CyanAccent,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        Text(
            text = "Productivity Engines",
            color = BrightWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Rendering Grid tools using clean row nested lists to avoid scrollable nesting issues.
        val tools = listOf(
            CategoryTool("PDF & DOCS", "pdftools", "Word to PDF, PDF convert, merges", Icons.Default.Description, Color(0xFFFF5252)),
            CategoryTool("SMART SCANNER", "scanner", "Adjust edge crop scan receipts", Icons.Default.QrCodeScanner, Color(0xFF4CAF50)),
            CategoryTool("ID CARD SCAN", "idcard", "Scan ID / passports dual composites", Icons.Default.Badge, Color(0xFF00E676)),
            CategoryTool("OCR TOOLS", "ocr", "Extract picture to edit, save PDF/Word", Icons.Default.TextFields, Color(0xFFFFEB3B)),
            CategoryTool("PHOTO TOOLS", "photo", "Passports maker, background removal", Icons.Default.PhotoCamera, Color(0xFF00B0FF)),
            CategoryTool("QR SUITE", "qr", "Generate & scan custom codes logged", Icons.Default.QrCode, Color(0xFFE040FB)),
            CategoryTool("CV RESUMES", "resume", "Fill academic profiles & print CVs", Icons.Default.ContactPage, Color(0xFF1DE9B6)),
            CategoryTool("SECURE NOTES", "notes", "Auto saves, tags categories local DB", Icons.Default.Note, Color(0xFFFF9100)),
            CategoryTool("FILE MANAGER", "filemanager", "Browse files, share, rename & stars", Icons.Default.Folder, Color(0xFF7E57C2))
        )

        val chunkedTools = tools.chunked(2)
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().testTag("dashboard_grid")
        ) {
            chunkedTools.forEach { rowTools ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    rowTools.forEach { tool ->
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(110.dp)
                                .clickable { viewModel.navigateTo(tool.route) }
                                .testTag("tool_card_${tool.route}"),
                            colors = CardDefaults.cardColors(containerColor = PremiumNavyCard),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(tool.tint.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = tool.icon,
                                            contentDescription = tool.title,
                                            tint = tool.tint,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Open",
                                        tint = HintGray.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = tool.title,
                                        color = BrightWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = tool.desc,
                                        color = HintGray,
                                        fontSize = 9.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                    if (rowTools.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- RECENT FILES CONTAINER (styled matching bg-[#161B2C] rounded-3xl p-4) ---
        Card(
            colors = CardDefaults.cardColors(containerColor = PremiumNavyCard),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Files",
                        color = BrightWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "View All",
                        color = CyanAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clickable { viewModel.navigateTo("filemanager") }
                            .padding(4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val recentFiles = totalFiles.take(3)
                if (recentFiles.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No recent files. Tap 'Start Scanning' to scan documents",
                            color = HintGray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        recentFiles.forEach { savedFile ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF0A0E1A).copy(alpha = 0.5f))
                                    .border(BorderStroke(1.dp, Color(0xFF1E2E54).copy(alpha = 0.3f)), RoundedCornerShape(16.dp))
                                    .clickable { viewModel.shareSavedFile(savedFile) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val iconVal = when (savedFile.format) {
                                    "PDF" -> Icons.Default.Description
                                    "DOCX" -> Icons.Default.Feed
                                    else -> Icons.Default.Image
                                }
                                val colorVal = when (savedFile.format) {
                                    "PDF" -> Color(0xFFFF5252)
                                    "DOCX" -> Color(0xFF2196F3)
                                    else -> Color(0xFF4CAF50)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colorVal.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = iconVal,
                                        contentDescription = "Format",
                                        tint = colorVal,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = savedFile.name,
                                        color = BrightWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${savedFile.sizeString} • ${savedFile.category}",
                                        color = HintGray,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(top = 1.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.shareSavedFile(savedFile) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share",
                                        tint = HintGray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Developer Footer Card as specified in instructions
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C142B)),
            border = BorderStroke(1.dp, Color(0xFF1E2E54)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    "OFFICIAL SUPPORT DETAILS",
                    color = CyanAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("Author: M. Zulkifal Khan", color = BrightWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("WhatsApp: +923270464248", color = HintGray, fontSize = 11.sp)
                Text("Email: khanzulkifal650@gmail.com", color = HintGray, fontSize = 11.sp)
            }
        }
    }
}

data class CategoryTool(
    val title: String,
    val route: String,
    val desc: String,
    val icon: ImageVector,
    val tint: Color
)

// PDF & DOCUMENT TOOLS
@Composable
fun PdfToolsScreen(viewModel: DocFusionViewModel) {
    val pdfText by viewModel.pdfInputText.collectAsState()
    val pdfTitle by viewModel.pdfInputTitle.collectAsState()
    val outFormat by viewModel.pdfOutputFormat.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            IconButton(onClick = { viewModel.navigateTo("dashboard") }) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = CyanAccent)
            }
            Text("PDF & Word Document Generator", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BrightWhite)
        }

        Text("Write, paste, edit, and compile texts perfectly. Choose PDF layout style elements or Word MS-Doc formatting output.", color = HintGray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = PremiumNavyCard),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                TextField(
                    value = pdfTitle,
                    onValueChange = { viewModel.pdfInputTitle.value = it },
                    label = { Text("Document Header Title") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedLabelColor = CyanAccent,
                        focusedTextColor = BrightWhite,
                        unfocusedTextColor = BrightWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                TextField(
                    value = pdfText,
                    onValueChange = { viewModel.pdfInputText.value = it },
                    label = { Text("Write content body details here...") },
                    minLines = 8,
                    maxLines = 15,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedLabelColor = CyanAccent,
                        focusedTextColor = BrightWhite,
                        unfocusedTextColor = BrightWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("CHOOSE EXPORT FORMAT", color = HintGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = outFormat == "PDF",
                            onClick = { viewModel.pdfOutputFormat.value = "PDF" },
                            colors = RadioButtonDefaults.colors(selectedColor = CyanAccent)
                        )
                        Text("PDF Document (.pdf)", color = BrightWhite, modifier = Modifier.padding(start = 6.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = outFormat == "Word",
                            onClick = { viewModel.pdfOutputFormat.value = "Word" },
                            colors = RadioButtonDefaults.colors(selectedColor = CyanAccent)
                        )
                        Text("MS Word (.docx)", color = BrightWhite, modifier = Modifier.padding(start = 6.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.runGenerateDocToolsConverter() },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("pdf_convert_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = "Compile", tint = BrightWhite)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("COMPILE & SAVE TO VAULT", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BrightWhite)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ADVANCED CONVERTER HUBS
        Text("Advanced Inter-Format Converter", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BrightWhite, modifier = Modifier.padding(bottom = 4.dp))
        Text("Convert existing files in your vault directly between PDF, Word, Image, and TXT formats instantly.", color = HintGray, fontSize = 11.sp, modifier = Modifier.padding(bottom = 12.dp))

        val savedFilesList by viewModel.savedFiles.collectAsState()
        val selectedFile by viewModel.selectedConversionFile.collectAsState()
        val selectedFileName by viewModel.selectedConversionFileName.collectAsState()
        val convTarget by viewModel.conversionTargetType.collectAsState()
        val multiFiles by viewModel.multiSelectConversionFiles.collectAsState()

        var showFileSelectDropdown by remember { mutableStateOf(false) }
        var showTypeSelectDropdown by remember { mutableStateOf(false) }

        Card(
            colors = CardDefaults.cardColors(containerColor = PremiumNavyCard),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                
                // STEP 1: Select File
                Text("STEP 1: SELECT TARGET VAULT FILE", color = HintGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                
                Box(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { showFileSelectDropdown = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF131F3F)),
                        border = BorderStroke(1.dp, Color.Gray),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (selectedFile != null) selectedFile!!.name else "CHOOSE SOURCE FILE FROM VAULT",
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showFileSelectDropdown,
                        onDismissRequest = { showFileSelectDropdown = false },
                        modifier = Modifier.background(PremiumNavyCard)
                    ) {
                        if (savedFilesList.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No files saved in vault yet", color = HintGray, fontSize = 12.sp) },
                                onClick = { showFileSelectDropdown = false }
                            )
                        } else {
                            savedFilesList.forEach { savedFile ->
                                DropdownMenuItem(
                                    text = { Text("${savedFile.name} (${savedFile.format})", color = BrightWhite, fontSize = 12.sp) },
                                    onClick = {
                                        viewModel.selectedConversionFile.value = java.io.File(savedFile.path)
                                        viewModel.selectedConversionFileName.value = savedFile.name
                                        showFileSelectDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // STEP 2: Select Conversion Action
                Text("STEP 2: SELECT COOPERATING CONVERSION", color = HintGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))

                val conversionTypes = listOf(
                    "PDF to Word", "PDF to Text", "PDF to Image", "PDF to JPG", "PDF to PNG",
                    "Word to PDF", "Word to Image", "Word to Text",
                    "Image to PDF", "Image to Word", "Image to Text", "JPG to PNG", "PNG to JPG",
                    "Text to Image", "PDF Split", "PDF Merge"
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { showTypeSelectDropdown = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF131F3F)),
                        border = BorderStroke(1.dp, Color.Gray),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(convTarget, fontSize = 11.sp, color = CyanAccent, fontWeight = FontWeight.Bold)
                    }

                    DropdownMenu(
                        expanded = showTypeSelectDropdown,
                        onDismissRequest = { showTypeSelectDropdown = false },
                        modifier = Modifier.background(PremiumNavyCard)
                    ) {
                        conversionTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type, color = BrightWhite, fontSize = 12.sp) },
                                onClick = {
                                    viewModel.conversionTargetType.value = type
                                    showTypeSelectDropdown = false
                                }
                            )
                        }
                    }
                }

                if (convTarget == "PDF Merge") {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("PDF MERGES - TICK TO SELECT DOCUMENTS:", color = HintGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    val pdfsInVault = savedFilesList.filter { it.format == "PDF" }
                    if (pdfsInVault.isEmpty()) {
                        Text("No PDF documents in vault to merge.", color = HintGray, fontSize = 11.sp)
                    } else {
                        pdfsInVault.forEach { p ->
                            val f = java.io.File(p.path)
                            val isChecked = multiFiles.any { it.absolutePath == f.absolutePath }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val mList = multiFiles.toMutableList()
                                        if (isChecked) {
                                            mList.removeAll { it.absolutePath == f.absolutePath }
                                        } else {
                                            mList.add(f)
                                        }
                                        viewModel.multiSelectConversionFiles.value = mList
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = {
                                        val mList = multiFiles.toMutableList()
                                        if (isChecked) {
                                            mList.removeAll { it.absolutePath == f.absolutePath }
                                        } else {
                                            mList.add(f)
                                        }
                                        viewModel.multiSelectConversionFiles.value = mList
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = CyanAccent)
                                )
                                Text(p.name, color = BrightWhite, fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.runAdvancedConversion() },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (convTarget == "PDF Merge") "MERGE SELECTED PDFs" else "EXECUTE CONVERSION ENGINE",
                        color = DarkNavy,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SmartScannerScreen(viewModel: DocFusionViewModel) {
    val context = LocalContext.current
    val activeFilter by viewModel.scannerFilterMode.collectAsState()
    val pages by viewModel.scannerPageCount.collectAsState()
    val sourceDoc by viewModel.scannerSourceDocument.collectAsState()
    val backCam by viewModel.isRearCameraMode.collectAsState()

    val cameraPermissionState = com.google.accompanist.permissions.rememberPermissionState(
        android.Manifest.permission.CAMERA
    )
    var imageCapturePrivate by remember { mutableStateOf<ImageCapture?>(null) }

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val rawBmp = BitmapFactory.decodeStream(inputStream)
                if (rawBmp != null) {
                    viewModel.addScannerPage(rawBmp)
                    Toast.makeText(context, "Document selected from storage perfectly! Total Pages: ${pages + 1}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load document: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        cameraPermissionState.launchPermissionRequest()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            IconButton(onClick = { viewModel.navigateTo("dashboard") }) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = CyanAccent)
            }
            Text("CamScanner Smart Scanner", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BrightWhite)
        }

        // Action selector options
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.scannerSourceDocument.value = "Invoice Receipt" },
                colors = ButtonDefaults.buttonColors(containerColor = if (sourceDoc == "Invoice Receipt") DeepBlue else PremiumNavyCard),
                modifier = Modifier.weight(1f)
            ) {
                Text("Receipt", fontSize = 11.sp, maxLines = 1)
            }
            Button(
                onClick = { viewModel.scannerSourceDocument.value = "Official Document" },
                colors = ButtonDefaults.buttonColors(containerColor = if (sourceDoc == "Official Document") DeepBlue else PremiumNavyCard),
                modifier = Modifier.weight(1.2f)
            ) {
                Text("Official Doc", fontSize = 11.sp, maxLines = 1)
            }
            Button(
                onClick = { viewModel.navigateTo("idcard") },
                colors = ButtonDefaults.buttonColors(containerColor = PremiumNavyCard),
                modifier = Modifier.weight(1f)
            ) {
                Text("ID Scanner", fontSize = 11.sp, maxLines = 1, color = CyanAccent)
            }
        }

        // Viewfinder Grid Simulation with real CameraX Layer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, Color(0xFF1E2E54), RoundedCornerShape(12.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (cameraPermissionState.status.isGranted) {
                CameraPreviewView(
                    isRearCamera = backCam,
                    onImageCaptureCreated = { cap -> imageCapturePrivate = cap },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // FALLBACK VIEW (Simulate document with White plate)
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxSize(0.7f)
                            .background(Color.White)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = if (sourceDoc == "Invoice Receipt") "TAX INVOICE RECEIPT\nDate: 12 June 2026\nId: #938102\nTotal Amount: $120.50" else "OFFICIAL CORPORATE REPORT\nDocFusion Document management\nIslamabad, Pakistan\nVersion: Stable 1.0\nSecure Vault Certified",
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Viewfinder Grid Overlay lines
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                
                // Grid lines (3x3 grid)
                drawLine(
                    color = Color.DarkGray.copy(alpha = 0.5f),
                    start = Offset(w / 3f, 0f),
                    end = Offset(w / 3f, h),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color.DarkGray.copy(alpha = 0.5f),
                    start = Offset(w * 2 / 3f, 0f),
                    end = Offset(w * 2 / 3f, h),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color.DarkGray.copy(alpha = 0.5f),
                    start = Offset(0f, h / 3f),
                    end = Offset(w, h / 3f),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color.DarkGray.copy(alpha = 0.5f),
                    start = Offset(0f, h * 2 / 3f),
                    end = Offset(w, h * 2 / 3f),
                    strokeWidth = 2f
                )

                // Draggable Smart Frame Overlay Corners simulation
                val cropMargin = 50f
                drawRect(
                    color = Color(0xFF00F0FF),
                    topLeft = Offset(cropMargin, cropMargin),
                    size = androidx.compose.ui.geometry.Size(w - cropMargin * 2, h - cropMargin * 2),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 4f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                    )
                )

                // Solid Corners indicators
                val handlerSize = 25f
                drawCircle(color = Color(0xFF00F0FF), radius = handlerSize / 2, center = Offset(cropMargin, cropMargin))
                drawCircle(color = Color(0xFF00F0FF), radius = handlerSize / 2, center = Offset(w - cropMargin, cropMargin))
                drawCircle(color = Color(0xFF00F0FF), radius = handlerSize / 2, center = Offset(cropMargin, h - cropMargin))
                drawCircle(color = Color(0xFF00F0FF), radius = handlerSize / 2, center = Offset(w - cropMargin, h - cropMargin))
            }

            // Viewfinder shutter triggers icon row
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(36.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.isRearCameraMode.value = !backCam },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FlipCameraAndroid, contentDescription = "Switch Camera", tint = BrightWhite)
                    }

                    // Shutter Box - Executes real CameraX capture image
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(BrightWhite)
                            .clickable {
                                val capture = imageCapturePrivate
                                if (cameraPermissionState.status.isGranted && capture != null) {
                                    val executor = ContextCompat.getMainExecutor(context)
                                    capture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                            val bitmap = getBitmapFromImageProxy(imageProxy)
                                            imageProxy.close()
                                            viewModel.addScannerPage(bitmap)
                                            Toast.makeText(context, "Page captured successfully! Pages: ${pages + 1}", Toast.LENGTH_SHORT).show()
                                        }
                                        override fun onError(exception: ImageCaptureException) {
                                            Toast.makeText(context, "Capture failed: ${exception.localizedMessage}", Toast.LENGTH_SHORT).show()
                                        }
                                    })
                                } else {
                                    // Fallback text generation if camera not accessible
                                    val sampleText = if (sourceDoc == "Invoice Receipt") {
                                        "TAX INVOICE RECEIPT\nDate: 12 June 2026\nId: #938102\nTotal Amount: $120.50"
                                    } else {
                                        "OFFICIAL CORPORATE REPORT\nDocFusion Document management\nIslamabad, Pakistan\nVersion: Stable 1.0\nSecure Vault Certified"
                                    }
                                    val sampleBitmap = Bitmap.createBitmap(400, 500, Bitmap.Config.ARGB_8888)
                                    val canvas = android.graphics.Canvas(sampleBitmap)
                                    canvas.drawColor(android.graphics.Color.WHITE)
                                    val p = Paint().apply {
                                        color = android.graphics.Color.BLACK
                                        textSize = 14f
                                        isAntiAlias = true
                                    }
                                    canvas.drawText("DocFusion Scanned Doc Draft", 20f, 50f, p)
                                    viewModel.addScannerPage(sampleBitmap)
                                    Toast.makeText(context, "Page captured (Simulated)! Total pages: ${pages + 1}", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .border(3.dp, CyanAccent, CircleShape)
                    )

                    // Gallery selector button
                    IconButton(
                        onClick = { galleryPickerLauncher.launch("image/*") },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = "Import from Gallery", tint = CyanAccent)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scanner enhancement mode row
        Text("SMART SCANNING FILTER", color = HintGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf("Original", "B&W", "Grayscale", "Smart Enhance")
            filters.forEach { filter ->
                val isSelected = activeFilter == filter
                Button(
                    onClick = { viewModel.scannerFilterMode.value = filter },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) CyanAccent else PremiumNavyCard,
                        contentColor = if (isSelected) DarkNavy else BrightWhite
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Text(text = filter, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = PremiumNavyCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Export Settings",
                    fontWeight = FontWeight.Bold,
                    color = BrightWhite,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text("Total Captured Pages: $pages", color = HintGray, fontSize = 12.sp)

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { viewModel.runExportScannedDoc("PDF") },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("SAVE SCAN AS PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { viewModel.runExportScannedDoc("PNG") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("SAVE SCAN AS IMAGE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ID CARD SCANNER
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun IdCardScannerScreen(viewModel: DocFusionViewModel) {
    val idType by viewModel.idCardType.collectAsState()
    val isFrontCaptured by viewModel.idFrontCaptured.collectAsState()
    val isBackCaptured by viewModel.idBackCaptured.collectAsState()
    val frontPhoto by viewModel.idFrontPhoto.collectAsState()
    val backPhoto by viewModel.idBackPhoto.collectAsState()
    val context = LocalContext.current

    var showFrontPrompt by remember { mutableStateOf(false) }
    var showBackPrompt by remember { mutableStateOf(false) }

    val frontGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val input = context.contentResolver.openInputStream(uri)
                val rawBmp = BitmapFactory.decodeStream(input)
                if (rawBmp != null) {
                    viewModel.idFrontPhoto.value = rawBmp
                    viewModel.idFrontCaptured.value = true
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    val frontCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        if (bmp != null) {
            viewModel.idFrontPhoto.value = bmp
            viewModel.idFrontCaptured.value = true
        }
    }

    val backGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val input = context.contentResolver.openInputStream(uri)
                val rawBmp = BitmapFactory.decodeStream(input)
                if (rawBmp != null) {
                    viewModel.idBackPhoto.value = rawBmp
                    viewModel.idBackCaptured.value = true
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    val backCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        if (bmp != null) {
            viewModel.idBackPhoto.value = bmp
            viewModel.idBackCaptured.value = true
        }
    }

    // Prompts
    if (showFrontPrompt) {
        AlertDialog(
            onDismissRequest = { showFrontPrompt = false },
            title = { Text("Front Card Side Layout", color = BrightWhite) },
            text = { Text("Capture a new card image using camera or import from storage.", color = HintGray) },
            confirmButton = {
                Button(
                    onClick = {
                        showFrontPrompt = false
                        frontCameraLauncher.launch(null)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = DarkNavy)
                ) {
                    Text("CAMERA")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showFrontPrompt = false
                        frontGalleryLauncher.launch("image/*")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumNavyCard, contentColor = BrightWhite)
                ) {
                    Text("GALLERY")
                }
            },
            containerColor = PremiumNavyCard
        )
    }

    if (showBackPrompt) {
        AlertDialog(
            onDismissRequest = { showBackPrompt = false },
            title = { Text("Back Card Side Layout", color = BrightWhite) },
            text = { Text("Capture a new card image using camera or import from storage.", color = HintGray) },
            confirmButton = {
                Button(
                    onClick = {
                        showBackPrompt = false
                        backCameraLauncher.launch(null)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = DarkNavy)
                ) {
                    Text("CAMERA")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showBackPrompt = false
                        backGalleryLauncher.launch("image/*")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumNavyCard, contentColor = BrightWhite)
                ) {
                    Text("GALLERY")
                }
            },
            containerColor = PremiumNavyCard
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            IconButton(onClick = { viewModel.navigateTo("scanner") }) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = CyanAccent)
            }
            Text("CNIC & Passport Scanner", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BrightWhite)
        }

        Text("Select Card Layout. Align inside camera frame slot markers, capture both layouts to export composite documents.", color = HintGray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp))

        // Preset cards switch
        val CardPresets = listOf("CNIC / ID Card", "Passport", "Driving License", "Student Card")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CardPresets.forEach { preset ->
                val isSelected = idType == preset
                Button(
                    onClick = { viewModel.idCardType.value = preset },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) DeepBlue else PremiumNavyCard,
                        contentColor = BrightWhite
                    )
                ) {
                    Text(preset, fontSize = 11.sp)
                }
            }
        }

        // Camera Simulation for Double Slots
        Text("ALIGNMENT VIEWPORT CAPTURE", color = HintGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            // Front Side Box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(130.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.5.dp, if (isFrontCaptured) CyanAccent else Color(0xFF1E2E54), RoundedCornerShape(8.dp))
                    .background(Color.Black)
                    .clickable {
                        showFrontPrompt = true
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isFrontCaptured && frontPhoto != null) {
                    Image(
                        bitmap = frontPhoto!!.asImageBitmap(),
                        contentDescription = "Front side profile",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.Camera, contentDescription = "Camera", tint = CyanAccent, modifier = Modifier.size(28.dp))
                        Text("[ FRONT SIDE ]", color = BrightWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Back Side Box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(130.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.5.dp, if (isBackCaptured) CyanAccent else Color(0xFF1E2E54), RoundedCornerShape(8.dp))
                    .background(Color.Black)
                    .clickable {
                        showBackPrompt = true
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isBackCaptured && backPhoto != null) {
                    Image(
                        bitmap = backPhoto!!.asImageBitmap(),
                        contentDescription = "Back side profile",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.Camera, contentDescription = "Camera", tint = CyanAccent, modifier = Modifier.size(28.dp))
                        Text("[ BACK SIDE ]", color = BrightWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = PremiumNavyCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Composite Output Generator",
                    fontWeight = FontWeight.Bold,
                    color = BrightWhite,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Text("Crops automatically, deskews perspective, and places front/back items side-by-side on an A4 sheet for crisp printing outputs.", color = HintGray, fontSize = 11.sp)

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { viewModel.runExportIdCardScans("PDF") },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("EXPORT COMPOSITE PDF", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { viewModel.runExportIdCardScans("PNG") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("SAVE PICTURE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// OCR TOOLS IMAGE TO TEXT
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun OcrToolsScreen(viewModel: DocFusionViewModel) {
    val ocrText by viewModel.ocrInputText.collectAsState()
    val isScanning by viewModel.ocrIsScanning.collectAsState()
    val context = LocalContext.current
    val activeOcrPhoto by viewModel.ocrPhoto.collectAsState()

    val galleryOcrLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val input = context.contentResolver.openInputStream(uri)
                val rawBmp = android.graphics.BitmapFactory.decodeStream(input)
                if (rawBmp != null) {
                    viewModel.ocrPhoto.value = rawBmp
                    Toast.makeText(context, "Image imported successfully", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    val cameraOcrLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        if (bmp != null) {
            viewModel.ocrPhoto.value = bmp
            Toast.makeText(context, "Snapshot captured successfully", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            IconButton(onClick = { viewModel.navigateTo("dashboard") }) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = CyanAccent)
            }
            Text("AI Document OCR Text Analyzer", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BrightWhite)
        }

        // Cam live image placeholder for target scanning
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, Color(0xFF1E2E54), RoundedCornerShape(12.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (isScanning) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = CyanAccent)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Deobfuscating text layers via Gemini...", color = CyanAccent, fontSize = 12.sp)
                }
            } else if (activeOcrPhoto != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        bitmap = activeOcrPhoto!!.asImageBitmap(),
                        contentDescription = "Target Document",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                    IconButton(
                        onClick = { viewModel.ocrPhoto.value = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .size(28.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = Color.Red, modifier = Modifier.size(16.dp))
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(12.dp)) {
                    Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = "Scan", tint = CyanAccent, modifier = Modifier.size(44.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Select or snap an image below to extract text.", color = HintGray, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Image sourcing controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { cameraOcrLauncher.launch(null) },
                colors = ButtonDefaults.buttonColors(containerColor = PremiumNavyCard),
                border = BorderStroke(1.dp, Color.Gray),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = "Camera", tint = CyanAccent)
                Spacer(modifier = Modifier.width(6.dp))
                Text("CAMERA SNAP", fontSize = 11.sp, color = BrightWhite)
            }

            Button(
                onClick = { galleryOcrLauncher.launch("image/*") },
                colors = ButtonDefaults.buttonColors(containerColor = PremiumNavyCard),
                border = BorderStroke(1.dp, Color.Gray),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.Image, contentDescription = "Gallery", tint = CyanAccent)
                Spacer(modifier = Modifier.width(6.dp))
                Text("IMPORT IMAGE", fontSize = 11.sp, color = BrightWhite)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { viewModel.runHighFidelityOcr() },
            colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
            enabled = !isScanning,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("ocr_extract_button")
        ) {
            Icon(imageVector = Icons.Default.Bolt, contentDescription = "OCR Action", tint = BrightWhite)
            Spacer(modifier = Modifier.width(8.dp))
            Text("RUN HIGH-FIDELITY CAM OCR", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("EDIT EXTRACTION TRANSCRIPTION", color = HintGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = PremiumNavyCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                TextField(
                    value = ocrText,
                    onValueChange = { viewModel.ocrInputText.value = it },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = BrightWhite,
                        unfocusedTextColor = BrightWhite
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 6,
                    maxLines = 15
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Copy
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("DocFusion OCR Extract", ocrText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied to Clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PremiumNavyCard),
                        border = BorderStroke(1.dp, Color.Gray),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = CyanAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("COPY", fontSize = 10.sp)
                    }

                    // Save as Note
                    Button(
                        onClick = {
                            viewModel.addNote("OCR Transcription", ocrText, "OCR")
                            Toast.makeText(context, "OCR result saved to Notes!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PremiumNavyCard),
                        border = BorderStroke(1.dp, Color.Gray),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(imageVector = Icons.Default.Note, contentDescription = "Save Note", tint = CyanAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SAVE NOTE", fontSize = 10.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("EXPORT STRANGE TRANSCRIPTIONS TO", color = HintGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { viewModel.runExportOcrExtracts("PDF") },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("TO PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { viewModel.runExportOcrExtracts("Word") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC2185B)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("TO WORD DOCX", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// DEDICATED DOCUMENT READER SCREEN
@Composable
fun DocumentReaderScreen(viewModel: DocFusionViewModel) {
    val activeFile by viewModel.activeReadingFile.collectAsState()
    val zoom by viewModel.readerZoomScale.collectAsState()
    val isDark by viewModel.readerDarkTheme.collectAsState()
    val bookmarks by viewModel.readerBookmarkedPages.collectAsState()
    val currentPage by viewModel.readerCurrentPage.collectAsState()
    val searchQuery by viewModel.readerSearchQuery.collectAsState()
    val fullScreen by viewModel.readerFullScreen.collectAsState()

    val context = LocalContext.current

    if (activeFile == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No target document loaded.", color = HintGray)
        }
        return
    }

    val file = activeFile!!
    val bgColors = if (isDark) Pair(DarkNavy, BrightWhite) else Pair(Color.White, Color.Black)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColors.first)
            .padding(16.dp)
    ) {
        if (!fullScreen) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo("filemanager") }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = CyanAccent)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = bgColors.second,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("${file.format} Document • ${file.sizeString}", fontSize = 11.sp, color = HintGray)
                }

                // Dark/Light Theme toggle
                IconButton(onClick = { viewModel.readerDarkTheme.value = !isDark }) {
                    Icon(
                        imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Theme",
                        tint = CyanAccent
                    )
                }

                // Full Screen toggle
                IconButton(onClick = { viewModel.readerFullScreen.value = true }) {
                    Icon(imageVector = Icons.Default.Fullscreen, contentDescription = "Fullscreen", tint = BrightWhite)
                }
            }

            // Sub-actions panel: search and zoom
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Search Input field
                TextField(
                    value = searchQuery,
                    onValueChange = { viewModel.readerSearchQuery.value = it },
                    placeholder = { Text("Search word...", fontSize = 11.sp, color = HintGray) },
                    modifier = Modifier.weight(1f).height(46.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = PremiumNavyCard,
                        unfocusedContainerColor = PremiumNavyCard,
                        focusedTextColor = BrightWhite,
                        unfocusedTextColor = BrightWhite
                    ),
                    maxLines = 1
                )

                // Zoom controls
                IconButton(
                    onClick = { viewModel.readerZoomScale.value = (zoom - 0.15f).coerceAtLeast(0.5f) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(imageVector = Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = CyanAccent)
                }

                Text("${(zoom * 100).toInt()}%", color = bgColors.second, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                IconButton(
                    onClick = { viewModel.readerZoomScale.value = (zoom + 0.15f).coerceAtMost(3.0f) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(imageVector = Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = CyanAccent)
                }
            }
        } else {
            // Mini header when in fullscreen mode (to allow exiting)
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Fullscreen Reading View", fontSize = 12.sp, color = HintGray)
                IconButton(onClick = { viewModel.readerFullScreen.value = false }, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.FullscreenExit, contentDescription = "Exit Fullscreen", tint = CyanAccent)
                }
            }
        }

        // Content Area rendering depending on Format
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, if (isDark) Color(0xFF1E2E54) else Color.LightGray, RoundedCornerShape(8.dp))
                .background(if (isDark) PremiumNavyCard else Color(0xFFFDFDFD))
                .padding(12.dp)
        ) {
            when (file.format) {
                "PDF" -> {
                    val totalPages = DocFusionUtils.getPdfPageCount(file.path)
                    if (totalPages <= 0) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("PDF page count is zero or file empty.", color = HintGray)
                        }
                    } else {
                        // Page Renderer
                        val pageBmp = remember(file.path, currentPage) {
                            DocFusionUtils.renderPdfPage(file.path, currentPage)
                        }

                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                contentAlignment = Alignment.Center
                            ) {
                                if (pageBmp != null) {
                                    Image(
                                        bitmap = pageBmp.asImageBitmap(),
                                        contentDescription = "PDF Page",
                                        modifier = Modifier
                                            .fillMaxWidth(zoom)
                                            .aspectRatio(pageBmp.width.toFloat() / pageBmp.height.toFloat())
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                } else {
                                    CircularProgressIndicator(color = CyanAccent)
                                }
                            }

                            // Bookmarks and Navigator row
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val isBookmarked = bookmarks.contains(currentPage)
                                IconButton(onClick = { viewModel.toggleReaderBookmark(currentPage) }) {
                                    Icon(
                                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = "Bookmark",
                                        tint = if (isBookmarked) Color(0xFFFFEB3B) else HintGray
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { if (currentPage > 0) viewModel.readerCurrentPage.value = currentPage - 1 },
                                        enabled = currentPage > 0
                                    ) {
                                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Prev", tint = if (currentPage > 0) CyanAccent else HintGray)
                                    }

                                    Text("Page ${currentPage + 1} of $totalPages", color = bgColors.second, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                                    IconButton(
                                        onClick = { if (currentPage < totalPages - 1) viewModel.readerCurrentPage.value = currentPage + 1 },
                                        enabled = currentPage < totalPages - 1
                                    ) {
                                        Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Next", tint = if (currentPage < totalPages - 1) CyanAccent else HintGray)
                                    }
                                }
                            }
                        }
                    }
                }
                "DOCX" -> {
                    val content = remember(file.path) { DocFusionUtils.readDocxText(file.path) }
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        Text(
                            text = content,
                            fontSize = (14 * zoom).sp,
                            color = bgColors.second,
                            lineHeight = (22 * zoom).sp
                        )
                    }
                }
                "TXT" -> {
                    val content = remember(file.path) {
                        try {
                            java.io.File(file.path).readText()
                        } catch (e: Exception) {
                            "Failed to read TXT contents: ${e.localizedMessage}"
                        }
                    }
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        Text(
                            text = content,
                            fontSize = (15 * zoom).sp,
                            color = bgColors.second,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            lineHeight = (22 * zoom).sp
                        )
                    }
                }
                else -> { // Render Image / Photos
                    val bitmap = remember(file.path) {
                        android.graphics.BitmapFactory.decodeFile(file.path)
                    }
                    if (bitmap != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Scanned Image View",
                                modifier = Modifier
                                    .fillMaxWidth(zoom)
                                    .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
                                    .clip(RoundedCornerShape(6.dp))
                            )
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No preview image data available.", color = HintGray)
                        }
                    }
                }
            }
        }
    }
}

// PHOTO TOOLS (PASSPORT MAKER & RESIZER)
@Composable
fun PhotoToolsScreen(viewModel: DocFusionViewModel) {
    val pxWidth by viewModel.photoWidthPx.collectAsState()
    val pxHeight by viewModel.photoHeightPx.collectAsState()
    val unit by viewModel.photoUnit.collectAsState()
    val bgMode by viewModel.photoBackgroundMode.collectAsState()
    val isPassport by viewModel.isPassportMode.collectAsState()
    val brightness by viewModel.photoBrightness.collectAsState()
    val contrast by viewModel.photoContrast.collectAsState()
    val saturation by viewModel.photoSaturation.collectAsState()

    val context = LocalContext.current
    val activePhoto by viewModel.userSelectedPhoto.collectAsState()

    val galleryPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val input = context.contentResolver.openInputStream(uri)
                val rawBmp = BitmapFactory.decodeStream(input)
                if (rawBmp != null) {
                    viewModel.userSelectedPhoto.value = rawBmp
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    val cameraPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        if (bmp != null) {
            viewModel.userSelectedPhoto.value = bmp
        }
    }

    val realProcessedBitmap = remember(activePhoto, brightness, contrast, saturation, bgMode, isPassport) {
        val base = activePhoto ?: return@remember null
        try {
            var temp = base
            if (isPassport) {
                temp = DocFusionUtils.changeBackgroundColor(temp, bgMode)
            }
            DocFusionUtils.transformBitmap(
                temp,
                brightness,
                contrast,
                saturation,
                0f, false, "Original"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            IconButton(onClick = { viewModel.navigateTo("dashboard") }) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = CyanAccent)
            }
            Text("Photo Resizer & Passport Maker", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BrightWhite)
        }

        // Resizer Modes
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { viewModel.isPassportMode.value = true },
                colors = ButtonDefaults.buttonColors(containerColor = if (isPassport) DeepBlue else PremiumNavyCard),
                modifier = Modifier.weight(1f)
            ) {
                Text("PASSPORT SIZE BUILDER", fontSize = 11.sp)
            }
            Button(
                onClick = { viewModel.isPassportMode.value = false },
                colors = ButtonDefaults.buttonColors(containerColor = if (!isPassport) DeepBlue else PremiumNavyCard),
                modifier = Modifier.weight(1f)
            ) {
                Text("CUSTOM SCALE RESIZER", fontSize = 11.sp)
            }
        }

        // Live Image Subject rendering box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, Color(0xFF1E2E54), RoundedCornerShape(12.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (realProcessedBitmap != null) {
                Image(
                    bitmap = realProcessedBitmap.asImageBitmap(),
                    contentDescription = "User Photo Output",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                
                // Draw crop grid guide outline overlay on top of user image if in passport size mode
                if (isPassport) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        drawRect(
                            color = Color.White.copy(alpha = 0.6f),
                            topLeft = Offset(canvasWidth / 2f - 120f, canvasHeight / 2f - 110f),
                            size = androidx.compose.ui.geometry.Size(240f, 220f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                        )
                    }
                }
            } else {
                // If no active photo, render default clean portrait vector layout
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    // Draw chosen background
                    val bgComposeColor = when (bgMode) {
                        "White" -> Color.White
                        "Blue" -> Color(0xFF1E3A8A)
                        "Red" -> Color(0xFFEF4444)
                        "Custom" -> Color(0xFF00F0FF)
                        else -> Color(0xFF1B2A4A)
                    }
                    drawRect(
                        color = bgComposeColor,
                        topLeft = Offset(0f, 0f),
                        size = size
                    )

                    // Face outline profile simulation
                    drawCircle(
                        color = Color(0xFFFFD180), // Skin
                        radius = 45f,
                        center = Offset(canvasWidth / 2f, canvasHeight / 2f - 20f)
                    )
                    // shoulders
                    val shoulderPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(canvasWidth / 2f - 90f, canvasHeight - 10f)
                        lineTo(canvasWidth / 2f + 90f, canvasHeight - 10f)
                        lineTo(canvasWidth / 2f + 60f, canvasHeight / 2f + 40f)
                        lineTo(canvasWidth / 2f - 60f, canvasHeight / 2f + 40f)
                        close()
                    }
                    drawPath(
                        path = shoulderPath,
                        color = Color(0xFF1E2E54) // Dark Suit jacket
                    )

                    // Draw passport format guides
                    if (isPassport) {
                        drawRect(
                            color = Color.White.copy(alpha = 0.6f),
                            topLeft = Offset(canvasWidth / 2f - 120f, canvasHeight / 2f - 110f),
                            size = androidx.compose.ui.geometry.Size(240f, 220f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                        )
                    }
                }

                // Overlay tap advice
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("TAP CAMERA / GALLERY TO LOAD PHOTO", color = BrightWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Box(modifier = Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.TopEnd) {
                Box(modifier = Modifier.background(Color.Black.copy(0.4f)).padding(6.dp)) {
                    Text(
                        text = if (isPassport) "PASSPORT PRESET (2x2 Inches)" else "CUSTOM: $pxWidth x $pxHeight $unit",
                        color = CyanAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Tap Import/Capture Trigger Bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { cameraPhotoLauncher.launch(null) },
                colors = ButtonDefaults.buttonColors(containerColor = PremiumNavyCard),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Camera", tint = CyanAccent)
                Spacer(modifier = Modifier.width(6.dp))
                Text("CAMERA", fontSize = 11.sp, color = BrightWhite)
            }
            Button(
                onClick = { galleryPhotoLauncher.launch("image/*") },
                colors = ButtonDefaults.buttonColors(containerColor = PremiumNavyCard),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = "Gallery", tint = CyanAccent)
                Spacer(modifier = Modifier.width(6.dp))
                Text("GALLERY", fontSize = 11.sp, color = BrightWhite)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isPassport) {
            // Passport Background Switch controls
            Text("OFFICIAL BACKGROUND FILTER COLOR", color = HintGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val bgColors = listOf("White", "Blue", "Red", "Custom")
                bgColors.forEach { color ->
                    val isSelected = bgMode == color
                    Button(
                        onClick = { viewModel.photoBackgroundMode.value = color },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) DeepBlue else PremiumNavyCard,
                            contentColor = BrightWhite
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(color, fontSize = 10.sp)
                    }
                }
            }
        } else {
            // Custom Resize dimension controls
            Card(
                colors = CardDefaults.cardColors(containerColor = PremiumNavyCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("CHOOSE UNITS", color = HintGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        listOf("Pixels", "CM", "Inch").forEach { u ->
                            val isChosen = unit == u
                            Button(
                                onClick = { viewModel.photoUnit.value = u },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isChosen) DeepBlue else Color(0xFF0C142B)
                                )
                            ) {
                                Text(u, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextField(
                            value = pxWidth,
                            onValueChange = { viewModel.photoWidthPx.value = it },
                            label = { Text("Width ($unit)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = TextFieldDefaults.colors(focusedTextColor = BrightWhite, unfocusedTextColor = BrightWhite)
                        )
                        TextField(
                            value = pxHeight,
                            onValueChange = { viewModel.photoHeightPx.value = it },
                            label = { Text("Height ($unit)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = TextFieldDefaults.colors(focusedTextColor = BrightWhite, unfocusedTextColor = BrightWhite)
                        )
                    }
                }
            }
        }

        // Active filters sliders
        Card(
            colors = CardDefaults.cardColors(containerColor = PremiumNavyCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("PHOTO ADJUSTMENTS", fontWeight = FontWeight.Bold, color = BrightWhite, fontSize = 13.sp)

                // Brightness
                Text("Brightness: ${brightness.toInt()}", color = HintGray, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                Slider(
                    value = brightness,
                    onValueChange = { viewModel.photoBrightness.value = it },
                    valueRange = -100f..100f,
                    colors = SliderDefaults.colors(activeTrackColor = CyanAccent, thumbColor = CyanAccent)
                )

                // Contrast
                Text("Contrast: ${"%.1f".format(contrast)}", color = HintGray, fontSize = 11.sp)
                Slider(
                    value = contrast,
                    onValueChange = { viewModel.photoContrast.value = it },
                    valueRange = 0.5f..2.0f,
                    colors = SliderDefaults.colors(activeTrackColor = CyanAccent, thumbColor = CyanAccent)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { viewModel.runExportPhotoEdits("JPG") },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("SAVE JPG", fontSize = 11.sp)
                    }
                    Button(
                        onClick = { viewModel.runExportPhotoEdits("PNG") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("SAVE PNG", fontSize = 11.sp)
                    }
                    Button(
                        onClick = { viewModel.runExportPhotoEdits("PDF") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE040FB)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("PRINT PDF", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.canvasHeightf() = size.height

// QR TOOLS (GENERATOR & VIEWFINDER SCANNER)
@Composable
fun QrToolsScreen(viewModel: DocFusionViewModel) {
    val textToEncode by viewModel.qrInputText.collectAsState()
    val qrBmp by viewModel.qrGeneratedBitmap.collectAsState()
    val scanReadout by viewModel.qrScannerViewfinderText.collectAsState()
    val logs by viewModel.qrScanLogs.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            IconButton(onClick = { viewModel.navigateTo("dashboard") }) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = CyanAccent)
            }
            Text("QR Code & Barcode Suite", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BrightWhite)
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = PremiumNavyCard),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("SCAN MODULE SIMULATOR", color = CyanAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("Simulates a live system scanner detector. Read QR barcodes, extract wifi parameters, contacts details, or hyperlinks.", color = HintGray, fontSize = 11.sp, modifier = Modifier.padding(bottom = 12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        // Red laser scanner line anim simulation
                        drawLine(
                            color = Color.Red,
                            start = Offset(0f, h / 2f),
                            end = Offset(w, h / 2f),
                            strokeWidth = 3f
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.Camera, contentDescription = "Laser", tint = Color.Red, modifier = Modifier.size(24.dp))
                        Button(
                            onClick = { viewModel.triggerScanQR() },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text("SIMULATE SCAN TRIGGER", fontSize = 11.sp)
                        }
                    }
                }

                if (scanReadout != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0C142B))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text("DETECTED READOUT STRING:", color = CyanAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(scanReadout!!, color = BrightWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = PremiumNavyCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("GENERATE BRAND NEW QR CODE", color = CyanAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = textToEncode,
                    onValueChange = {
                        viewModel.qrInputText.value = it
                        viewModel.regenerateActiveQR()
                    },
                    label = { Text("Link, Wifi, Phone, Email, Text") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(focusedTextColor = BrightWhite, unfocusedTextColor = BrightWhite)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Frame holding programmatically drawn QR code image
                if (qrBmp != null) {
                    Image(
                        bitmap = qrBmp!!.asImageBitmap(),
                        contentDescription = "QR Output",
                        modifier = Modifier
                            .size(160.dp)
                            .border(1.dp, Color.White),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { viewModel.runExportQrCode("PNG") },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("SAVE PICTURE", fontSize = 11.sp)
                    }
                    Button(
                        onClick = { viewModel.runExportQrCode("PDF") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC2185B)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("EXPORT PDF", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// NOTES MODULE
@Composable
fun NotesScreen(viewModel: DocFusionViewModel) {
    val notesList by viewModel.notes.collectAsState()
    val searchQuery by viewModel.searchNoteQuery.collectAsState()
    val activeCategory by viewModel.activeNoteCategory.collectAsState()
    val editingNote by viewModel.selectedNote.collectAsState()
    val context = LocalContext.current

    var isAddingNew by remember { mutableStateOf(false) }
    var inputTitle by remember { mutableStateOf("") }
    var inputContent by remember { mutableStateOf("") }
    var inputCat by remember { mutableStateOf("Work") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.navigateTo("dashboard") }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = CyanAccent)
                }
                Text("Secure Offline Notes", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BrightWhite)
            }
            if (!isAddingNew && editingNote == null) {
                IconButton(
                    onClick = {
                        inputTitle = ""
                        inputContent = ""
                        inputCat = "Work"
                        isAddingNew = true
                    },
                    modifier = Modifier
                        .background(DeepBlue, CircleShape)
                        .size(40.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Create", tint = BrightWhite)
                }
            }
        }

        if (isAddingNew || editingNote != null) {
            // Editor Screen
            Card(
                colors = CardDefaults.cardColors(containerColor = PremiumNavyCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    val labelText = if (isAddingNew) "Create Secure Note" else "Edit note"
                    Text(labelText, color = CyanAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    Spacer(modifier = Modifier.height(12.dp))

                    TextField(
                        value = if (isAddingNew) inputTitle else editingNote!!.title,
                        onValueChange = {
                            if (isAddingNew) inputTitle = it
                            else viewModel.selectedNote.value = editingNote!!.copy(title = it)
                        },
                        label = { Text("Note Title") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(focusedTextColor = BrightWhite, unfocusedTextColor = BrightWhite)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Category dropdown likeness selector
                    Text("CATEGORY ACCENT", color = HintGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val cats = listOf("General", "Personal", "Work", "Ideas")
                        cats.forEach { c ->
                            val currentC = if (isAddingNew) inputCat else editingNote!!.category
                            val isChosen = currentC == c
                            Button(
                                onClick = {
                                    if (isAddingNew) inputCat = c
                                    else viewModel.selectedNote.value = editingNote!!.copy(category = c)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isChosen) DeepBlue else Color(0xFF0C142B)
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(c, fontSize = 10.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    TextField(
                        value = if (isAddingNew) inputContent else editingNote!!.content,
                        onValueChange = {
                            if (isAddingNew) inputContent = it
                            else viewModel.selectedNote.value = editingNote!!.copy(content = it)
                        },
                        label = { Text("Content Note details...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 6,
                        maxLines = 15,
                        colors = TextFieldDefaults.colors(focusedTextColor = BrightWhite, unfocusedTextColor = BrightWhite)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                isAddingNew = false
                                viewModel.selectedNote.value = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("DISCARD", color = BrightWhite)
                        }

                        Button(
                            onClick = {
                                if (isAddingNew) {
                                    viewModel.addNote(inputTitle, inputContent, inputCat)
                                    isAddingNew = false
                                } else {
                                    viewModel.addNote(editingNote!!.title, editingNote!!.content, editingNote!!.category)
                                    viewModel.selectedNote.value = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = "Save", tint = DarkNavy)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SAVE SECURELY", color = DarkNavy, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Filter categories tags
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf("All", "General", "Personal", "Work", "Ideas")
                filters.forEach { filter ->
                    val isChosen = activeCategory == filter
                    Button(
                        onClick = { viewModel.activeNoteCategory.value = filter },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isChosen) CyanAccent else PremiumNavyCard,
                            contentColor = if (isChosen) DarkNavy else BrightWhite
                        )
                    ) {
                        Text(filter, fontSize = 11.sp)
                    }
                }
            }

            // Search Bar
            TextField(
                value = searchQuery,
                onValueChange = { viewModel.searchNoteQuery.value = it },
                label = { Text("Search Notes by Title or Content...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = CyanAccent) },
                colors = TextFieldDefaults.colors(
                    focusedLabelColor = CyanAccent,
                    focusedTextColor = BrightWhite,
                    unfocusedTextColor = BrightWhite
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            // Notes list
            val filteredNotes = notesList.filter {
                (activeCategory == "All" || it.category == activeCategory) &&
                (searchQuery.isEmpty() || it.title.contains(searchQuery, ignoreCase = true) || it.content.contains(searchQuery, ignoreCase = true))
            }

            if (filteredNotes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.SpeakerNotesOff, contentDescription = "Empty", tint = HintGray, modifier = Modifier.size(54.dp))
                        Text("No Notes Found", color = HintGray, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                        Text("Tap + to add custom fast notes", color = HintGray, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredNotes) { note ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = PremiumNavyCard),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectNoteForEditing(note) }
                                .testTag("note_item_${note.id}")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(note.title, color = BrightWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFF0C142B))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(note.category, color = CyanAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Text(note.content, color = HintGray, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    IconButton(
                                        onClick = { viewModel.deleteNote(note) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(18.dp))
                                    }
                                    
                                    Spacer(modifier = Modifier.width(16.dp))

                                    // Quick Export note directly to PDF file manager
                                    IconButton(
                                        onClick = {
                                            viewModel.pdfInputTitle.value = note.title
                                            viewModel.pdfInputText.value = note.content
                                            viewModel.pdfOutputFormat.value = "PDF"
                                            viewModel.runGenerateDocToolsConverter()
                                            Toast.makeText(context, "Note exported directly as PDF!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = "Export to PDF", tint = CyanAccent, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// CV & RESUME BUILDER
@Composable
fun ResumeBuilderScreen(viewModel: DocFusionViewModel) {
    val profile by viewModel.resumeProfile.collectAsState()
    
    // Multi tab navigation inside Resume screen
    var activeSection by remember { mutableStateOf("PERSONAL") } // "PERSONAL", "EXPERIENCE", "ACADEMICS", "SKILLS"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            IconButton(onClick = { viewModel.navigateTo("dashboard") }) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = CyanAccent)
            }
            Text("Professional CV Resume Builder", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BrightWhite)
        }

        Text("Fill in academic coordinates block-by-block. Auto saves values locally and compiles into modern executive PDFs.", color = HintGray, fontSize = 11.sp, modifier = Modifier.padding(bottom = 12.dp))

        // Sections selector bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val sections = listOf("PERSONAL", "EXPERIENCE", "ACADEMICS", "SKILLS")
            sections.forEach { sect ->
                val isSelected = activeSection == sect
                Button(
                    onClick = { activeSection = sect },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) DeepBlue else PremiumNavyCard,
                        contentColor = BrightWhite
                    )
                ) {
                    Text(sect, fontSize = 10.sp)
                }
            }
        }

        // Form Inputs based on segment
        Card(
            colors = CardDefaults.cardColors(containerColor = PremiumNavyCard),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                when (activeSection) {
                    "PERSONAL" -> {
                        Text("Personal Contact Details", fontWeight = FontWeight.Bold, color = CyanAccent, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        TextField(value = profile.fullName, onValueChange = { viewModel.updateResume(profile.copy(fullName = it)) }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(value = profile.title, onValueChange = { viewModel.updateResume(profile.copy(title = it)) }, label = { Text("Professional Title") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(value = profile.email, onValueChange = { viewModel.updateResume(profile.copy(email = it)) }, label = { Text("Email address") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(value = profile.phone, onValueChange = { viewModel.updateResume(profile.copy(phone = it)) }, label = { Text("Phone number") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(value = profile.address, onValueChange = { viewModel.updateResume(profile.copy(address = it)) }, label = { Text("Location Address") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(value = profile.website, onValueChange = { viewModel.updateResume(profile.copy(website = it)) }, label = { Text("Website Portfolios Link") }, modifier = Modifier.fillMaxWidth())
                    }
                    "EXPERIENCE" -> {
                        Text("Employment History", fontWeight = FontWeight.Bold, color = CyanAccent, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        TextField(
                            value = profile.experience,
                            onValueChange = { viewModel.updateResume(profile.copy(experience = it)) },
                            label = { Text("Employment block experience details...") },
                            minLines = 8,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    "ACADEMICS" -> {
                        Text("Education Coordinates", fontWeight = FontWeight.Bold, color = CyanAccent, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        TextField(
                            value = profile.education,
                            onValueChange = { viewModel.updateResume(profile.copy(education = it)) },
                            label = { Text("Degrees, Universities, GPA scores...") },
                            minLines = 8,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    "SKILLS" -> {
                        Text("Skills SUMMARY", fontWeight = FontWeight.Bold, color = CyanAccent, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        TextField(value = profile.skills, onValueChange = { viewModel.updateResume(profile.copy(skills = it)) }, label = { Text("Tech skills comma separated: Kotlin, SQL, Design") }, modifier = Modifier.fillMaxWidth())
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Text("Notable Projects Info", fontWeight = FontWeight.Bold, color = CyanAccent, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(
                            value = profile.projects,
                            onValueChange = { viewModel.updateResume(profile.copy(projects = it)) },
                            label = { Text("Key projects summary descriptions...") },
                            minLines = 4,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Choice templates
        Card(
            colors = CardDefaults.cardColors(containerColor = PremiumNavyCard),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("EXECUTIVE STYLE THEME", color = HintGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = profile.templateId == "Modern Blue",
                            onClick = { viewModel.updateResume(profile.copy(templateId = "Modern Blue")) },
                            colors = RadioButtonDefaults.colors(selectedColor = CyanAccent)
                        )
                        Text("MODERN BLUE", color = BrightWhite, modifier = Modifier.padding(start = 4.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = profile.templateId == "Classic Academic",
                            onClick = { viewModel.updateResume(profile.copy(templateId = "Classic Academic")) },
                            colors = RadioButtonDefaults.colors(selectedColor = CyanAccent)
                        )
                        Text("CLASSIC ACADEMIC", color = BrightWhite, modifier = Modifier.padding(start = 4.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.runCompileResumePdf() },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Description, contentDescription = "Compile", tint = BrightWhite)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("COMPILE & SAVE CV PDF", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// FILE MANAGER
@Composable
fun FileManagerScreen(viewModel: DocFusionViewModel) {
    val filesList by viewModel.savedFiles.collectAsState()
    val searchQuery by viewModel.searchFileQuery.collectAsState()
    val activeCatFilter by viewModel.fileFilterCategory.collectAsState()

    var showRenameDialog by remember { mutableStateOf<SavedFile?>(null) }
    var renameInputText by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            IconButton(onClick = { viewModel.navigateTo("dashboard") }) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = CyanAccent)
            }
            Text("DocFusion Storage Registry", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BrightWhite)
        }

        // Search Bar
        TextField(
            value = searchQuery,
            onValueChange = { viewModel.searchFileQuery.value = it },
            label = { Text("Search files by name...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = CyanAccent) },
            colors = TextFieldDefaults.colors(
                focusedLabelColor = CyanAccent,
                focusedTextColor = BrightWhite,
                unfocusedTextColor = BrightWhite
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )

        // Segment Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf("All", "PDF", "Doc", "Scan", "Photo", "OCR", "Resume")
            filters.forEach { filter ->
                val isChosen = activeCatFilter == filter
                Button(
                    onClick = { viewModel.fileFilterCategory.value = filter },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isChosen) CyanAccent else PremiumNavyCard,
                        contentColor = if (isChosen) DarkNavy else BrightWhite
                    )
                ) {
                    Text(filter, fontSize = 11.sp)
                }
            }
        }

        // Main List files
        val filteredFiles = filesList.filter {
            (activeCatFilter == "All" || it.category == activeCatFilter) &&
            (searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true))
        }

        if (filteredFiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.FolderOpen, contentDescription = "Empty Folder", tint = HintGray, modifier = Modifier.size(64.dp))
                    Text("Repository Folder Empty", color = HintGray, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    Text("Compile documents, scan grids or export photo to load", color = HintGray, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredFiles) { savedFile ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PremiumNavyCard),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setReadingFile(savedFile) }
                            .testTag("file_item_${savedFile.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // File graphic icon depending on format
                            val iconVal = when (savedFile.format) {
                                "PDF" -> Icons.Default.Description
                                "DOCX" -> Icons.Default.Feed
                                else -> Icons.Default.Image
                            }
                            val colorVal = when (savedFile.format) {
                                "PDF" -> Color(0xFFFF5252)
                                "DOCX" -> Color(0xFF2196F3)
                                else -> Color(0xFF4CAF50)
                            }
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colorVal.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = iconVal, contentDescription = "Format", tint = colorVal, modifier = Modifier.size(22.dp))
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Details
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = savedFile.name,
                                    color = BrightWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(savedFile.sizeString, color = HintGray, fontSize = 10.sp)
                                    Box(modifier = Modifier.size(3.dp).clip(CircleShape).background(HintGray))
                                    Text(savedFile.category, color = CyanAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Interactive menus row
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Star Favoriting
                                IconButton(onClick = { viewModel.toggleFavorite(savedFile) }, modifier = Modifier.size(24.dp)) {
                                    Icon(
                                        imageVector = if (savedFile.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "Starred",
                                        tint = if (savedFile.isFavorite) Color(0xFFFFEB3B) else HintGray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Rename
                                IconButton(
                                    onClick = {
                                        showRenameDialog = savedFile
                                        renameInputText = savedFile.name
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Rename", tint = CyanAccent, modifier = Modifier.size(16.dp))
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Native direct share
                                IconButton(onClick = { viewModel.shareSavedFile(savedFile) }, modifier = Modifier.size(24.dp)) {
                                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = BrightWhite, modifier = Modifier.size(16.dp))
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Delete
                                IconButton(onClick = { viewModel.deleteSavedFile(savedFile) }, modifier = Modifier.size(24.dp)) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Rename Dialogue pop block
        if (showRenameDialog != null) {
            AlertDialog(
                onDismissRequest = { showRenameDialog = null },
                title = { Text("Rename Secure File", color = BrightWhite) },
                text = {
                    Column {
                        Text("Write your chosen filename below", color = HintGray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                        TextField(
                            value = renameInputText,
                            onValueChange = { renameInputText = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val f = showRenameDialog!!
                            if (renameInputText.isNotEmpty()) {
                                viewModel.renameSavedFile(f, renameInputText)
                            }
                            showRenameDialog = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = DarkNavy)
                    ) {
                        Text("UPDATE")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showRenameDialog = null },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                    ) {
                        Text("CANCEL")
                    }
                },
                containerColor = PremiumNavyCard
            )
        }
    }
}

// APP GENERAL SETTINGS
@Composable
fun SettingsScreen(viewModel: DocFusionViewModel) {
    val currentTheme by viewModel.themeMode.collectAsState()
    val securityEnabled by viewModel.securityEnabled.collectAsState()
    val isAppLocked by viewModel.isAppLocked.collectAsState()
    val currentPIN by viewModel.appPIN.collectAsState()

    var isEditingPIN by remember { mutableStateOf(false) }
    var pinFieldOne by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            IconButton(onClick = { viewModel.navigateTo("dashboard") }) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = CyanAccent)
            }
            Text("General App Configurations", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BrightWhite)
        }

        // Section theme
        Text("INTERFACE COLORS THEME", color = HintGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Card(
            colors = CardDefaults.cardColors(containerColor = PremiumNavyCard),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                listOf("Dark Navy Theme", "Light Accent Theme").forEach { t ->
                    val isT = if (t == "Dark Navy Theme") currentTheme == "Dark Navy" else currentTheme == "Light"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.themeMode.value = if (t == "Dark Navy Theme") "Dark Navy" else "Light"
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(t, color = BrightWhite, fontSize = 13.sp)
                        RadioButton(
                            selected = isT,
                            onClick = {
                                viewModel.themeMode.value = if (t == "Dark Navy Theme") "Dark Navy" else "Light"
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = CyanAccent)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Section lock screen security
        Text("DOCUMENT VAULT LOCK & SECURITY", color = HintGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Card(
            colors = CardDefaults.cardColors(containerColor = PremiumNavyCard),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Enable Secure Vault Lock", color = BrightWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Prompts passcode before open documents", color = HintGray, fontSize = 10.sp)
                    }
                    Switch(
                        checked = securityEnabled,
                        onCheckedChange = {
                            viewModel.securityEnabled.value = it
                            if (it && currentPIN.isEmpty()) {
                                isEditingPIN = true
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanAccent, checkedTrackColor = DeepBlue)
                    )
                }

                if (securityEnabled) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("PIN Code: ${currentPIN.ifEmpty { "None set" }}", color = BrightWhite, fontSize = 12.sp)
                        Button(
                            onClick = { isEditingPIN = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2E54))
                        ) {
                            Text("UPDATE PIN", fontSize = 11.sp, color = CyanAccent)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // System storage cleaner
        Text("STORAGE ACCESS CLOGS & BACKUP", color = HintGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Card(
            colors = CardDefaults.cardColors(containerColor = PremiumNavyCard),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("App Internal Storage Caches", color = BrightWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Deletes temporary scanners layout, exports, and temporary photos sheets.", color = HintGray, fontSize = 11.sp)

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.clearAppDataCache() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CLEAR ALL APP DATA CACHE", color = BrightWhite, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Version Card
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C142B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("DocFusion Suite • Version 1.0.0 Stable", color = HintGray, fontSize = 11.sp)
                Text("Built securely for local offline use on Android.", color = HintGray, fontSize = 9.sp)
            }
        }

        // Active Set Passcode dialog popup block
        if (isEditingPIN) {
            AlertDialog(
                onDismissRequest = { isEditingPIN = false },
                title = { Text("Set Secure 4-Digit PIN", color = BrightWhite) },
                text = {
                    Column {
                        Text("Only numbers are accepted. Keeps your personal OCR outputs and ID composites private.", color = HintGray, fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp))
                        TextField(
                            value = pinFieldOne,
                            onValueChange = { if (it.length <= 4) pinFieldOne = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (pinFieldOne.length == 4) {
                                viewModel.appPIN.value = pinFieldOne
                                isEditingPIN = false
                                Toast.makeText(context, "Lock Screen PIN Created!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "PIN must be exactly 4-digits!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = DarkNavy)
                    ) {
                        Text("CONFIRM")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { isEditingPIN = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                    ) {
                        Text("DISCARD")
                    }
                },
                containerColor = PremiumNavyCard
            )
        }
    }
}

// HELP AND SUPPORT
@Composable
fun HelpSupportScreen(viewModel: DocFusionViewModel) {
    var searchQuery by remember { mutableStateOf("") }
    
    val FAQs = listOf(
        "Does DocFusion require internet connectivity?" to "No! DocFusion compiles PDFs, transforms image matrices, builds CV structures, and generates high quality secure QR scans 100% locally offline for maximum privacy protection.",
        "What is CamScanner Smart enhance mode?" to "Smart enhance recalculates the pixels, boosts white points, sharpens line boundaries, and overlays a selective high contrast color matrix to represent a clean digital print.",
        "How is ID Card Front/Back composition generated?" to "The Smart scanner lets you capture front and back photos successively, clips the borders, rescales them to actual CNIC proportion, and compiles side-by-side on printable A4 sheets automatically.",
        "Where are my documents saved?" to "Documents are securely written into the internal application folder, protected from generic scanning apps. You can browse, rename, delete, favoriting or share them with WhatsApp/Telegram."
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            IconButton(onClick = { viewModel.navigateTo("dashboard") }) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = CyanAccent)
            }
            Text("Tech Help & Support Desk", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BrightWhite)
        }

        // Creator card
        Card(
            colors = CardDefaults.cardColors(containerColor = PremiumNavyCard),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            border = BorderStroke(1.dp, Color(0xFF1E2E54))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(DeepBlue, CyanAccent))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("MZ", color = DarkNavy, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("M. Zulkifal Khan", color = BrightWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Lead Developer, DocFusion Suite", color = CyanAccent, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                
                Text(
                    "WhatsApp: +92 327 0464248",
                    color = BrightWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable {
                        // Launch WhatsApp directly
                    }
                )
                Text("Email: khanzulkifal650@gmail.com", color = HintGray, fontSize = 12.sp)
                Text("Headquarters: Islamabad, Pakistan", color = HintGray, fontSize = 12.sp)

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            // Link to WhatsApp
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("WHATSAPP DIRECT", fontSize = 10.sp, color = Color.White)
                    }
                    Button(
                        onClick = {
                            // Link to Mail
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepBlue),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("SEND EMAIL", fontSize = 10.sp)
                    }
                }
            }
        }

        Text("EXPANDABLE FREQUENTLY ASKED QUESTIONS", color = HintGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

        // Search FAQs
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Filter FAQs...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = TextFieldDefaults.colors(focusedTextColor = BrightWhite, unfocusedTextColor = BrightWhite)
        )

        val filteredFAQs = FAQs.filter {
            searchQuery.isEmpty() || it.first.contains(searchQuery, ignoreCase = true) || it.second.contains(searchQuery, ignoreCase = true)
        }

        filteredFAQs.forEach { (q, a) ->
            var expanded by remember { mutableStateOf(false) }
            Card(
                colors = CardDefaults.cardColors(containerColor = PremiumNavyCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clickable { expanded = !expanded }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = q,
                            fontWeight = FontWeight.Bold,
                            color = BrightWhite,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand",
                            tint = CyanAccent
                        )
                    }
                    
                    AnimatedVisibility(visible = expanded) {
                        Text(
                            text = a,
                            color = HintGray,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 8.dp),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

// CAMERAX COMPOSABLE FOR REAL-TIME PREVIEW
@Composable
fun CameraPreviewView(
    modifier: Modifier = Modifier,
    isRearCamera: Boolean = true,
    onImageCaptureCreated: (ImageCapture) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val previewView = remember { androidx.camera.view.PreviewView(context) }
    val cameraSelector = if (isRearCamera) androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA else androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA

    LaunchedEffect(isRearCamera) {
        val cameraProviderProvider = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(context)
        cameraProviderProvider.addListener({
            val cameraProvider = cameraProviderProvider.get()
            val preview = androidx.camera.core.Preview.Builder().build().apply {
                surfaceProvider = previewView.surfaceProvider
            }
            val imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            onImageCaptureCreated(imageCapture)

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    androidx.compose.ui.viewinterop.AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}

// Rotates or Decodes captured ImageProxy to standard Bitmap format
fun getBitmapFromImageProxy(imageProxy: ImageProxy): Bitmap {
    val buffer = imageProxy.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
    return if (rotationDegrees != 0) {
        val matrix = android.graphics.Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } else {
        bitmap
    }
}
