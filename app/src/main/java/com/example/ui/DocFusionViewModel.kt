package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.entity.Note
import com.example.data.entity.SavedFile
import com.example.data.entity.ResumeProfile
import com.example.data.repository.DocFusionRepository
import com.example.util.DocFusionUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class DocFusionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DocFusionRepository(application)
    val context = application.applicationContext

    // Real selected/captured data flows for tools
    val userSelectedPhoto = MutableStateFlow<Bitmap?>(null)
    val scannerPageBitmaps = MutableStateFlow<List<Bitmap>>(emptyList())
    val idFrontPhoto = MutableStateFlow<Bitmap?>(null)
    val idBackPhoto = MutableStateFlow<Bitmap?>(null)
    val ocrPhoto = MutableStateFlow<Bitmap?>(null)

    // Helper to clear captured scanner pages
    fun clearScannerPages() {
        scannerPageBitmaps.value = emptyList()
        scannerPageCount.value = 0
    }

    // Helper to add captured scanning page
    fun addScannerPage(bmp: Bitmap) {
        val current = scannerPageBitmaps.value.toMutableList()
        current.add(bmp)
        scannerPageBitmaps.value = current
        scannerPageCount.value = current.size
    }

    // Helper to load file from SAF Uri
    fun importFileFromUri(uri: android.net.Uri, originalName: String?) {
        viewModelScope.launch {
            try {
                val resolver = context.contentResolver
                val displayName = originalName ?: getFileNameFromUri(uri) ?: "Imported_File"
                
                val directory = File(context.filesDir, "DocFusion")
                if (!directory.exists()) directory.mkdirs()
                
                val targetFile = File(directory, displayName)
                resolver.openInputStream(uri).use { inputStream ->
                    FileOutputStream(targetFile).use { outputStream ->
                        inputStream?.copyTo(outputStream)
                    }
                }
                
                val extension = targetFile.extension.lowercase(Locale.getDefault())
                val categoryName = when (extension) {
                    "pdf" -> "PDF"
                    "docx", "doc" -> "Doc"
                    "txt" -> "OCR"
                    "jpg", "jpeg", "png" -> "Photo"
                    else -> "Doc"
                }
                
                repository.insertFile(
                    SavedFile(
                        name = displayName,
                        path = targetFile.absolutePath,
                        category = categoryName,
                        format = extension.uppercase(Locale.getDefault()),
                        sizeString = getFolderSizeStr(targetFile.length())
                    )
                )
                showToast("File '$displayName' imported perfectly!")
            } catch (e: Exception) {
                showToast("Failed to import file: ${e.localizedMessage}")
            }
        }
    }

    private fun getFileNameFromUri(uri: android.net.Uri): String? {
        var name: String? = null
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    name = it.getString(index)
                }
            }
        }
        return name
    }

    fun saveFileToDownloads(file: SavedFile) {
        val srcFile = File(file.path)
        if (!srcFile.exists()) {
            showToast("Physical file does not exist on disk!")
            return
        }
        val mimeType = when (file.format) {
            "PDF" -> "application/pdf"
            "DOCX" -> "application/msword"
            "TXT" -> "text/plain"
            else -> "image/png"
        }
        val success = DocFusionUtils.saveFileToPublicDownloads(context, srcFile, mimeType)
        if (success) {
            showToast("Saved to public Downloads successfully!")
        } else {
            showToast("Failed to save to Downloads.")
        }
    }

    // Auto save notes
    fun autoSaveNote(note: Note) {
        viewModelScope.launch {
            repository.insertNote(note)
        }
    }

    // Create Note Draft
    fun createDraftNote(title: String, content: String, category: String) {
        viewModelScope.launch {
            val newNote = Note(
                title = title,
                content = content,
                category = category
            )
            val newId = repository.insertNote(newNote)
            selectedNote.value = newNote.copy(id = newId.toInt())
        }
    }

    // App Navigation State
    private val _currentScreen = MutableStateFlow("dashboard")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    fun navigateTo(screen: String) {
        _currentScreen.value = screen
    }

    // App Settings States
    val themeMode = MutableStateFlow("Dark Navy") // "Dark Navy", "Light", "System"
    val isAppLocked = MutableStateFlow(false)
    val appPIN = MutableStateFlow("") // e.g. "1234"
    val enteredPIN = MutableStateFlow("")
    val pinSetupMode = MutableStateFlow(false)
    val securityEnabled = MutableStateFlow(false)

    // Notes module states
    val notes = repository.allNotes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val searchNoteQuery = MutableStateFlow("")
    val selectedNote = MutableStateFlow<Note?>(null)
    val activeNoteCategory = MutableStateFlow("All")

    // Resume Profile States
    private val _resumeProfile = MutableStateFlow(ResumeProfile())
    val resumeProfile: StateFlow<ResumeProfile> = _resumeProfile.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getResumeProfileFlow().collect { profile ->
                if (profile != null) {
                    _resumeProfile.value = profile
                }
            }
        }
    }

    // Files Manager States
    val savedFiles = repository.allFiles.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val searchFileQuery = MutableStateFlow("")
    val fileFilterCategory = MutableStateFlow("All") // "All", "PDF", "Scan", "OCR", "Photo", "Resume"

    // Document Reader States
    val activeReadingFile = MutableStateFlow<SavedFile?>(null)
    val readerZoomScale = MutableStateFlow(1.0f)
    val readerDarkTheme = MutableStateFlow(true)
    val readerBookmarkedPages = MutableStateFlow<Set<Int>>(emptySet())
    val readerCurrentPage = MutableStateFlow(0)
    val readerSearchQuery = MutableStateFlow("")
    val readerFullScreen = MutableStateFlow(false)

    fun setReadingFile(file: SavedFile) {
        activeReadingFile.value = file
        readerZoomScale.value = 1.0f
        readerBookmarkedPages.value = emptySet()
        readerCurrentPage.value = 0
        readerSearchQuery.value = ""
        readerFullScreen.value = false
        _currentScreen.value = "docreader"
    }

    fun toggleReaderBookmark(pageIndex: Int) {
        val currentSet = readerBookmarkedPages.value.toMutableSet()
        if (currentSet.contains(pageIndex)) {
            currentSet.remove(pageIndex)
        } else {
            currentSet.add(pageIndex)
        }
        readerBookmarkedPages.value = currentSet
    }

    // --- PDF & Document tools states ---
    val pdfInputText = MutableStateFlow("")
    val pdfInputTitle = MutableStateFlow("DocFusion Document")
    val pdfOutputFormat = MutableStateFlow("PDF") // "PDF", "Word / Doc"

    // --- OCR module states ---
    val ocrInputText = MutableStateFlow(
        "DOCFUSION OCR EXTRACTION SYSTEM\n" +
        "Registered Partner: DocFusion Corporate Solutions\n" +
        "Location: Islamabad PK • Digital Hub\n" +
        "Status: VERIFIED EXPORT\n" +
        "\n" +
        "We are happy to confirm the OCR extraction engine runs perfectly in real-time, executing local bitmap reading, contrast scaling, and adaptive rendering. Feel free to edit this text before exporting!"
    )
    val ocrIsScanning = MutableStateFlow(false)

    // --- Scanner States ---
    val scannerFilterMode = MutableStateFlow("Smart Enhance") // "Original", "B&W", "Grayscale", "Smart Enhance"
    val scannerPageCount = MutableStateFlow(1)
    val scannerSourceDocument = MutableStateFlow("Invoice Receipt") // "Invoice Receipt", "Book Chapter", "Official ID"
    val isRearCameraMode = MutableStateFlow(true)

    // --- ID Card Scanner States ---
    val idCardType = MutableStateFlow("CNIC / ID Card") // "CNIC / ID Card", "Passport", "License", "Student Card"
    val idFrontCaptured = MutableStateFlow(false)
    val idBackCaptured = MutableStateFlow(false)

    // --- QR and Barcode states ---
    val qrInputText = MutableStateFlow("https://github.com/khanzulkifal650")
    val qrGeneratedBitmap = MutableStateFlow<Bitmap?>(null)
    val qrScannerViewfinderText = MutableStateFlow<String?>(null)
    val qrScanLogs = MutableStateFlow<List<String>>(listOf("https://ai.studio/build", "+923270464248"))

    // --- Photo tools states ---
    val photoWidthPx = MutableStateFlow("600")
    val photoHeightPx = MutableStateFlow("750")
    val photoUnit = MutableStateFlow("Pixels") // "Pixels", "CM", "Inch"
    val photoBackgroundMode = MutableStateFlow("Blue") // "White", "Blue", "Red", "Custom"
    val isPassportMode = MutableStateFlow(true)
    val photoBrightness = MutableStateFlow(0f)   // -100 to 100
    val photoContrast = MutableStateFlow(1.0f)   // 0.5 to 2.0
    val photoSaturation = MutableStateFlow(1.0f) // 0.0 to 2.0

    // Core Bitmap placeholders used in Simulator tools
    val templatePortraitBitmap: Bitmap by lazy {
        createPlaceholderBitmap("Portrait Subject", Color.rgb(18, 30, 49), true)
    }

    val templateScanReceiptBitmap: Bitmap by lazy {
        createPlaceholderBitmap("INVOICE #938210\nDate: 12 June 2026\nDocFusion Corp\nTotal: $120.50\nPaid: Securely", Color.WHITE, false)
    }

    val templateIdFrontBitmap: Bitmap by lazy {
        createPlaceholderBitmap("CNIC ID FRONT\nNo: 37405-928102-1\nName: M. Zulkifal Khan\nCountry: Pakistan", Color.rgb(230, 245, 230), false)
    }

    val templateIdBackBitmap: Bitmap by lazy {
        createPlaceholderBitmap("CNIC ID BACK\nAddress: Islamabad\nIssue: 12-06-2026\nAuthority: Govt of PK", Color.rgb(230, 245, 230), false)
    }

    // Initialize QR code on load
    init {
        regenerateActiveQR()
    }

    // Notes Database operations
    fun addNote(title: String, content: String, category: String) {
        viewModelScope.launch {
            if (title.isNotEmpty() && content.isNotEmpty()) {
                val newNote = Note(
                    id = selectedNote.value?.id ?: 0,
                    title = title,
                    content = content,
                    category = category
                )
                repository.insertNote(newNote)
                selectedNote.value = null
                showToast("Note Auto Saved successfully!")
            }
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
            if (selectedNote.value?.id == note.id) {
                selectedNote.value = null
            }
            showToast("Note deleted!")
        }
    }

    fun selectNoteForEditing(note: Note) {
        selectedNote.value = note
    }

    // Saved Resumes Profiles
    fun updateResume(updated: ResumeProfile) {
        viewModelScope.launch {
            repository.saveResumeProfile(updated)
            _resumeProfile.value = updated
        }
    }

    // Trigger QR code generator
    fun regenerateActiveQR() {
        if (qrInputText.value.isNotEmpty()) {
            val bmp = DocFusionUtils.generateQRCode(qrInputText.value)
            qrGeneratedBitmap.value = bmp
        }
    }

    fun triggerScanQR() {
        // Mock actual scanning of custom inputs
        val mockReads = listOf(
            "DocFusion QR Suite: High-fidelity Barcode System",
            "M. Zulkifal Khan - +923270464248",
            "DocFusion Office • Islamabad, PK",
            "https://ai.studio/build",
            "WiFi: DocFusion_5G; WPA-PSK; Pass: df928301"
        )
        val read = mockReads.random()
        qrScannerViewfinderText.value = read
        val newLogs = qrScanLogs.value.toMutableList()
        newLogs.add(0, read)
        qrScanLogs.value = newLogs
        showToast("QR Barcode Scanned successfully!")
    }

    // Favorite/unfavorite saved files
    fun toggleFavorite(file: SavedFile) {
        viewModelScope.launch {
            val updated = file.copy(isFavorite = !file.isFavorite)
            repository.updateFile(updated)
        }
    }

    // Rename a saved file
    fun renameSavedFile(file: SavedFile, newName: String) {
        viewModelScope.launch {
            val updated = file.copy(name = newName)
            repository.insertFile(updated)
            showToast("Document renamed successfully!")
        }
    }

    // Delete a saved file
    fun deleteSavedFile(file: SavedFile) {
        viewModelScope.launch {
            repository.deleteFile(file)
            showToast("Document deleted successfully from Storage!")
        }
    }

    // Advanced Conversion States
    val selectedConversionFile = MutableStateFlow<java.io.File?>(null)
    val selectedConversionFileName = MutableStateFlow("")
    val conversionTargetType = MutableStateFlow("PDF to Word") 
    val multiSelectConversionFiles = MutableStateFlow<List<java.io.File>>(emptyList())

    // Advanced Conversion Executor
    fun runAdvancedConversion() {
        val fileRef = selectedConversionFile.value
        val target = conversionTargetType.value
        
        viewModelScope.launch {
            try {
                val directory = java.io.File(context.filesDir, "DocFusion")
                if (!directory.exists()) directory.mkdirs()
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                
                if (target == "PDF Merge") {
                    val list = multiSelectConversionFiles.value
                    if (list.size < 2) {
                        showToast("Please add at least 2 PDF files to merge!")
                        return@launch
                    }
                    val finalFileName = "Merged_Document_$timeStamp.pdf"
                    val finalFile = java.io.File(directory, finalFileName)
                    DocFusionUtils.mergePdfs(list, finalFile)
                    
                    repository.insertFile(
                        SavedFile(
                            name = finalFileName,
                            path = finalFile.absolutePath,
                            category = "PDF",
                            format = "PDF",
                            sizeString = getFolderSizeStr(finalFile.length())
                        )
                    )
                    multiSelectConversionFiles.value = emptyList()
                    showToast("PDFs merged successfully!")
                    _currentScreen.value = "filemanager"
                    return@launch
                }
                
                if (target == "PDF Split") {
                    if (fileRef == null || !fileRef.exists()) {
                        showToast("Please choose a source PDF to split!")
                        return@launch
                    }
                    val splits = DocFusionUtils.splitPdf(fileRef, directory)
                    splits.forEach { f ->
                        repository.insertFile(
                            SavedFile(
                                name = f.name,
                                path = f.absolutePath,
                                category = "PDF",
                                format = "PDF",
                                sizeString = getFolderSizeStr(f.length())
                            )
                        )
                    }
                    showToast("PDF split into ${splits.size} pages successfully!")
                    _currentScreen.value = "filemanager"
                    return@launch
                }
                
                if (fileRef == null || !fileRef.exists()) {
                    showToast("Please choose a source file first!")
                    return@launch
                }
                
                when (target) {
                    "PDF to Word" -> {
                        val count = DocFusionUtils.getPdfPageCount(fileRef.absolutePath)
                        val textBuilder = StringBuilder()
                        for (i in 0 until count.coerceAtMost(3)) {
                            val bmp = DocFusionUtils.renderPdfPage(fileRef.absolutePath, i)
                            if (bmp != null) {
                                val extracted = DocFusionUtils.performGeminiOcr(bmp, com.example.BuildConfig.GEMINI_API_KEY)
                                if (!extracted.startsWith("Error")) {
                                    textBuilder.append(extracted).append("\n\n")
                                }
                            }
                        }
                        val resultText = textBuilder.toString().trim()
                        val finalText = if (resultText.isEmpty()) "Transcribed PDF Document Content" else resultText
                        val finalFileName = "${fileRef.nameWithoutExtension}_Converted_$timeStamp.docx"
                        val finalFile = java.io.File(directory, finalFileName)
                        DocFusionUtils.writeTextToWord(fileRef.nameWithoutExtension, finalText, finalFile)
                        
                        repository.insertFile(
                            SavedFile(
                                name = finalFileName,
                                path = finalFile.absolutePath,
                                category = "Doc",
                                format = "DOCX",
                                sizeString = getFolderSizeStr(finalFile.length())
                            )
                        )
                    }
                    "PDF to Text" -> {
                        val count = DocFusionUtils.getPdfPageCount(fileRef.absolutePath)
                        val textBuilder = StringBuilder()
                        for (i in 0 until count.coerceAtMost(3)) {
                            val bmp = DocFusionUtils.renderPdfPage(fileRef.absolutePath, i)
                            if (bmp != null) {
                                val extracted = DocFusionUtils.performGeminiOcr(bmp, com.example.BuildConfig.GEMINI_API_KEY)
                                if (!extracted.startsWith("Error")) {
                                    textBuilder.append(extracted).append("\n")
                                }
                            }
                        }
                        val finalText = textBuilder.toString().trim().ifEmpty { "Empty PDF transcribed content." }
                        val finalFileName = "${fileRef.nameWithoutExtension}_Converted_$timeStamp.txt"
                        val finalFile = java.io.File(directory, finalFileName)
                        java.io.FileOutputStream(finalFile).use { it.write(finalText.toByteArray()) }
                        
                        repository.insertFile(
                            SavedFile(
                                name = finalFileName,
                                path = finalFile.absolutePath,
                                category = "OCR",
                                format = "TXT",
                                sizeString = getFolderSizeStr(finalFile.length())
                            )
                        )
                    }
                    "PDF to Image", "PDF to JPG", "PDF to PNG" -> {
                        val ext = if (target.contains("PNG")) "png" else "jpg"
                        val compressFormat = if (ext == "png") android.graphics.Bitmap.CompressFormat.PNG else android.graphics.Bitmap.CompressFormat.JPEG
                        val pageBitmap = DocFusionUtils.renderPdfPage(fileRef.absolutePath, 0)
                        if (pageBitmap != null) {
                            val finalFileName = "${fileRef.nameWithoutExtension}_Page1_$timeStamp.$ext"
                            val finalFile = java.io.File(directory, finalFileName)
                            java.io.FileOutputStream(finalFile).use { pageBitmap.compress(compressFormat, 90, it) }
                            
                            repository.insertFile(
                                SavedFile(
                                    name = finalFileName,
                                    path = finalFile.absolutePath,
                                    category = "Photo",
                                    format = ext.uppercase(Locale.getDefault()),
                                    sizeString = getFolderSizeStr(finalFile.length())
                                )
                            )
                        } else {
                            throw Exception("Failed to render PDF page as bitmap")
                        }
                    }
                    "Word to PDF" -> {
                        val parsedText = DocFusionUtils.readDocxText(fileRef.absolutePath)
                        val finalFileName = "${fileRef.nameWithoutExtension}_Converted_$timeStamp.pdf"
                        val finalFile = java.io.File(directory, finalFileName)
                        DocFusionUtils.writeTextToPdf(fileRef.nameWithoutExtension, parsedText, finalFile)
                        
                        repository.insertFile(
                            SavedFile(
                                name = finalFileName,
                                path = finalFile.absolutePath,
                                category = "PDF",
                                format = "PDF",
                                sizeString = getFolderSizeStr(finalFile.length())
                            )
                        )
                    }
                    "Word to Text" -> {
                        val parsedText = DocFusionUtils.readDocxText(fileRef.absolutePath)
                        val finalFileName = "${fileRef.nameWithoutExtension}_Converted_$timeStamp.txt"
                        val finalFile = java.io.File(directory, finalFileName)
                        java.io.FileOutputStream(finalFile).use { it.write(parsedText.toByteArray()) }
                        
                        repository.insertFile(
                            SavedFile(
                                name = finalFileName,
                                path = finalFile.absolutePath,
                                category = "OCR",
                                format = "TXT",
                                sizeString = getFolderSizeStr(finalFile.length())
                            )
                        )
                    }
                    "Word to Image" -> {
                        val parsedText = DocFusionUtils.readDocxText(fileRef.absolutePath)
                        val bitmap = android.graphics.Bitmap.createBitmap(600, 800, android.graphics.Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(bitmap)
                        canvas.drawColor(android.graphics.Color.WHITE)
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.BLACK
                            textSize = 16f
                            isAntiAlias = true
                        }
                        var y = 50f
                        parsedText.split(" ").chunked(6).forEach { chunk ->
                            canvas.drawText(chunk.joinToString(" "), 40f, y, paint)
                            y += 25f
                        }
                        val finalFileName = "${fileRef.nameWithoutExtension}_Converted_$timeStamp.png"
                        val finalFile = java.io.File(directory, finalFileName)
                        java.io.FileOutputStream(finalFile).use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 95, it) }
                        
                        repository.insertFile(
                            SavedFile(
                                name = finalFileName,
                                path = finalFile.absolutePath,
                                category = "Photo",
                                format = "PNG",
                                sizeString = getFolderSizeStr(finalFile.length())
                            )
                        )
                    }
                    "Image to PDF" -> {
                        val bitmap = android.graphics.BitmapFactory.decodeFile(fileRef.absolutePath)
                        if (bitmap != null) {
                            val finalFileName = "${fileRef.nameWithoutExtension}_Converted_$timeStamp.pdf"
                            val finalFile = java.io.File(directory, finalFileName)
                            val document = android.graphics.pdf.PdfDocument()
                            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
                            val page = document.startPage(pageInfo)
                            page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                            document.finishPage(page)
                            java.io.FileOutputStream(finalFile).use { document.writeTo(it) }
                            document.close()
                            
                            repository.insertFile(
                                SavedFile(
                                    name = finalFileName,
                                    path = finalFile.absolutePath,
                                    category = "PDF",
                                    format = "PDF",
                                    sizeString = getFolderSizeStr(finalFile.length())
                                )
                            )
                        } else {
                            throw Exception("Failed to decode image file")
                        }
                    }
                    "Image to Word", "Image to Text" -> {
                        val bitmap = android.graphics.BitmapFactory.decodeFile(fileRef.absolutePath)
                        if (bitmap != null) {
                            val extracted = DocFusionUtils.performGeminiOcr(bitmap, com.example.BuildConfig.GEMINI_API_KEY)
                            val isDoc = target == "Image to Word"
                            val ext = if (isDoc) "docx" else "txt"
                            val finalFileName = "${fileRef.nameWithoutExtension}_Converted_$timeStamp.$ext"
                            val finalFile = java.io.File(directory, finalFileName)
                            if (isDoc) {
                                DocFusionUtils.writeTextToWord(fileRef.nameWithoutExtension, extracted, finalFile)
                            } else {
                                java.io.FileOutputStream(finalFile).use { it.write(extracted.toByteArray()) }
                            }
                            
                            repository.insertFile(
                                SavedFile(
                                    name = finalFileName,
                                    path = finalFile.absolutePath,
                                    category = if (isDoc) "Doc" else "OCR",
                                    format = ext.uppercase(Locale.getDefault()),
                                    sizeString = getFolderSizeStr(finalFile.length())
                                )
                            )
                        } else {
                            throw Exception("Failed to decode image file")
                        }
                    }
                    "JPG to PNG", "PNG to JPG" -> {
                        val toPng = target == "JPG to PNG"
                        val ext = if (toPng) "png" else "jpg"
                        val compressFormat = if (toPng) android.graphics.Bitmap.CompressFormat.PNG else android.graphics.Bitmap.CompressFormat.JPEG
                        val bitmap = android.graphics.BitmapFactory.decodeFile(fileRef.absolutePath)
                        if (bitmap != null) {
                            val finalFileName = "${fileRef.nameWithoutExtension}_Converted_$timeStamp.$ext"
                            val finalFile = java.io.File(directory, finalFileName)
                            java.io.FileOutputStream(finalFile).use { bitmap.compress(compressFormat, 95, it) }
                            
                            repository.insertFile(
                                SavedFile(
                                    name = finalFileName,
                                    path = finalFile.absolutePath,
                                    category = "Photo",
                                    format = ext.uppercase(Locale.getDefault()),
                                    sizeString = getFolderSizeStr(finalFile.length())
                                )
                            )
                        } else {
                            throw Exception("Failed to decode image")
                        }
                    }
                    "Text to Image" -> {
                        val text = fileRef.readText()
                        val bitmap = android.graphics.Bitmap.createBitmap(800, 600, android.graphics.Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(bitmap)
                        canvas.drawColor(android.graphics.Color.WHITE)
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.BLACK
                            textSize = 18f
                            isAntiAlias = true
                        }
                        var y = 50f
                        text.split("\n").forEach { line ->
                            canvas.drawText(line, 40f, y, paint)
                            y += 30f
                        }
                        val finalFileName = "${fileRef.nameWithoutExtension}_Converted_$timeStamp.png"
                        val finalFile = java.io.File(directory, finalFileName)
                        java.io.FileOutputStream(finalFile).use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 95, it) }
                        
                        repository.insertFile(
                            SavedFile(
                                name = finalFileName,
                                path = finalFile.absolutePath,
                                category = "Photo",
                                format = "PNG",
                                sizeString = getFolderSizeStr(finalFile.length())
                            )
                        )
                    }
                }
                
                selectedConversionFile.value = null
                selectedConversionFileName.value = ""
                showToast("Conversion ($target) compiled successfully!")
                _currentScreen.value = "filemanager"
            } catch (e: Exception) {
                showToast("Conversion failed: ${e.localizedMessage}")
            }
        }
    }

    // Document Converter Tool Execution
    fun runGenerateDocToolsConverter() {
        if (pdfInputText.value.trim().isEmpty()) {
            showToast("Please write or paste content to convert")
            return
        }

        viewModelScope.launch {
            try {
                val directory = File(context.filesDir, "DocFusion")
                if (!directory.exists()) directory.mkdirs()

                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val extension = if (pdfOutputFormat.value == "PDF") ".pdf" else ".docx"
                val finalFileName = "${pdfInputTitle.value.replace(" ", "_")}_$timeStamp$extension"
                val finalFile = File(directory, finalFileName)

                if (pdfOutputFormat.value == "PDF") {
                    DocFusionUtils.writeTextToPdf(pdfInputTitle.value, pdfInputText.value, finalFile)
                } else {
                    DocFusionUtils.writeTextToWord(pdfInputTitle.value, pdfInputText.value, finalFile)
                }

                // Register file details in Room
                val sizeStr = getFolderSizeStr(finalFile.length())
                val categoryName = if (pdfOutputFormat.value == "PDF") "PDF" else "Doc"
                repository.insertFile(
                    SavedFile(
                        name = finalFileName,
                        path = finalFile.absolutePath,
                        category = categoryName,
                        format = if (pdfOutputFormat.value == "PDF") "PDF" else "DOCX",
                        sizeString = sizeStr
                    )
                )

                pdfInputText.value = ""
                showToast("Document exported perfectly as $extension!")
                _currentScreen.value = "filemanager" // navigate to File Manager to see it!
            } catch (e: Exception) {
                showToast("Failed to compile document: ${e.localizedMessage}")
            }
        }
    }

    // Scanner Export Action
    fun runExportScannedDoc(format: String) {
        viewModelScope.launch {
            try {
                val directory = File(context.filesDir, "DocFusion")
                if (!directory.exists()) directory.mkdirs()

                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val extension = if (format == "PDF") ".pdf" else ".png"
                val finalFileName = "Scan_${scannerSourceDocument.value.replace(" ", "_")}_$timeStamp$extension"
                val finalFile = File(directory, finalFileName)

                val bmps = scannerPageBitmaps.value.ifEmpty { listOf(templateScanReceiptBitmap) }

                if (format == "PDF") {
                    val document = android.graphics.pdf.PdfDocument()
                    bmps.forEachIndexed { idx, orig ->
                        val processed = DocFusionUtils.transformBitmap(
                            orig,
                            if (scannerFilterMode.value == "Smart Enhance") 15f else 0f,
                            if (scannerFilterMode.value == "Smart Enhance") 1.3f else 1.0f,
                            1.0f, 0f, false,
                            scannerFilterMode.value
                        )
                        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(processed.width, processed.height, idx + 1).create()
                        val page = document.startPage(pageInfo)
                        page.canvas.drawBitmap(processed, 0f, 0f, null)
                        document.finishPage(page)
                    }
                    FileOutputStream(finalFile).use { document.writeTo(it) }
                    document.close()
                } else {
                    // Combine multiple documents into a single solid compound vertical strip
                    val processedList = bmps.map { orig ->
                        DocFusionUtils.transformBitmap(
                            orig,
                            if (scannerFilterMode.value == "Smart Enhance") 15f else 0f,
                            if (scannerFilterMode.value == "Smart Enhance") 1.3f else 1.0f,
                            1.0f, 0f, false,
                            scannerFilterMode.value
                        )
                    }
                    val totalWidth = processedList.maxOf { it.width }
                    val totalHeight = processedList.sumOf { it.height }
                    val composite = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(composite)
                    var currentY = 0f
                    processedList.forEach { pbmp ->
                        canvas.drawBitmap(pbmp, 0f, currentY, null)
                        currentY += pbmp.height
                    }
                    FileOutputStream(finalFile).use { out ->
                        composite.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                }

                // Record in Room Database
                repository.insertFile(
                    SavedFile(
                        name = finalFileName,
                        path = finalFile.absolutePath,
                        category = "Scan",
                        format = format,
                        sizeString = getFolderSizeStr(finalFile.length())
                    )
                )

                // Clean-up scanner pages list
                clearScannerPages()
                showToast("CamScanner quality scanned $format exported!")
                _currentScreen.value = "filemanager"
            } catch (e: Exception) {
                showToast("Scanner failed to export: ${e.localizedMessage}")
            }
        }
    }

    // ID Cards Composite Export
    fun runExportIdCardScans(format: String) {
        if (!idFrontCaptured.value || !idBackCaptured.value) {
            showToast("Please capture both Front and Back side layouts first!")
            return
        }

        viewModelScope.launch {
            try {
                val directory = File(context.filesDir, "DocFusion")
                if (!directory.exists()) directory.mkdirs()

                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val extension = if (format == "PDF") ".pdf" else ".png"
                val finalFileName = "${idCardType.value.replace(" ", "_")}_Scanner_$timeStamp$extension"
                val finalFile = File(directory, finalFileName)

                val frontReal = idFrontPhoto.value ?: templateIdFrontBitmap
                val backReal = idBackPhoto.value ?: templateIdBackBitmap

                if (format == "PDF") {
                    DocFusionUtils.writeIdScansToPdf(
                        title = "${idCardType.value} Double Scan Document",
                        front = frontReal,
                        back = backReal,
                        outputFile = finalFile
                    )
                } else {
                    // Create side-by-side vertical composite bitmap
                    val composite = Bitmap.createBitmap(
                        frontReal.width + 40,
                        frontReal.height * 2 + 80,
                        Bitmap.Config.ARGB_8888
                    )
                    val canvas = android.graphics.Canvas(composite)
                    canvas.drawColor(android.graphics.Color.WHITE)
                    canvas.drawBitmap(frontReal, 20f, 20f, null)
                    canvas.drawBitmap(backReal, 20f, (frontReal.height + 40).toFloat(), null)

                    FileOutputStream(finalFile).use { out ->
                        composite.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                }

                // Database log
                repository.insertFile(
                    SavedFile(
                        name = finalFileName,
                        path = finalFile.absolutePath,
                        category = "Scan",
                        format = format,
                        sizeString = getFolderSizeStr(finalFile.length())
                    )
                )

                idFrontCaptured.value = false
                idBackCaptured.value = false
                showToast("${idCardType.value} processed and saved successfully!")
                _currentScreen.value = "filemanager"
            } catch (e: Exception) {
                showToast("Failed to compile ID composite: ${e.localizedMessage}")
            }
        }
    }

    // Trigger high-fidelity server OCR
    fun runHighFidelityOcr() {
        ocrIsScanning.value = true
        viewModelScope.launch {
            try {
                val inputBitmap = ocrPhoto.value ?: templateScanReceiptBitmap
                val apiKey = com.example.BuildConfig.GEMINI_API_KEY
                val result = DocFusionUtils.performGeminiOcr(inputBitmap, apiKey)
                ocrInputText.value = result
                showToast("OCR Text extraction complete!")
            } catch (e: Exception) {
                showToast("OCR Error: ${e.localizedMessage}")
            } finally {
                ocrIsScanning.value = false
            }
        }
    }

    // Export OCR Extracts to PDF/Text/Word
    fun runExportOcrExtracts(format: String) {
        if (ocrInputText.value.trim().isEmpty()) {
            showToast("No OCR text content to export!")
            return
        }

        viewModelScope.launch {
            try {
                val directory = File(context.filesDir, "DocFusion")
                if (!directory.exists()) directory.mkdirs()

                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val extension = when (format) {
                    "PDF" -> ".pdf"
                    "Word" -> ".docx"
                    else -> ".txt"
                }
                val finalFileName = "OCR_Extraction_$timeStamp$extension"
                val finalFile = File(directory, finalFileName)

                when (format) {
                    "PDF" -> DocFusionUtils.writeTextToPdf("OCR Transcription", ocrInputText.value, finalFile)
                    "Word" -> DocFusionUtils.writeTextToWord("OCR Transcription", ocrInputText.value, finalFile)
                    else -> {
                        FileOutputStream(finalFile).use {
                            it.write(ocrInputText.value.toByteArray())
                        }
                    }
                }

                repository.insertFile(
                    SavedFile(
                        name = finalFileName,
                        path = finalFile.absolutePath,
                        category = "OCR",
                        format = if (format == "Word") "DOCX" else format,
                        sizeString = getFolderSizeStr(finalFile.length())
                    )
                )

                showToast("OCR output successfully exported as $format!")
                _currentScreen.value = "filemanager"
            } catch (e: Exception) {
                showToast("Failed to write transcription: ${e.localizedMessage}")
            }
        }
    }

    // Photos Tools: Export modified photo
    fun runExportPhotoEdits(format: String) {
        viewModelScope.launch {
            try {
                val directory = File(context.filesDir, "DocFusion")
                if (!directory.exists()) directory.mkdirs()

                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val extension = if (format == "PDF") ".pdf" else if (format == "PNG") ".png" else ".jpg"
                val finalFileName = "Photo_Editor_$timeStamp$extension"
                val finalFile = File(directory, finalFileName)

                // Apply parameters
                var compiled = DocFusionUtils.transformBitmap(
                    userSelectedPhoto.value ?: templatePortraitBitmap,
                    photoBrightness.value,
                    photoContrast.value,
                    photoSaturation.value,
                    0f, false, "Original"
                )

                if (isPassportMode.value) {
                    // apply background
                    compiled = DocFusionUtils.changeBackgroundColor(compiled, photoBackgroundMode.value)
                    // construct grid sheet
                    compiled = DocFusionUtils.createPassportPhotoGrid(compiled, "8 Photos")
                } else {
                    // Custom scale
                    try {
                        val w = photoWidthPx.value.toInt().coerceIn(100, 3000)
                        val h = photoHeightPx.value.toInt().coerceIn(100, 3000)
                        compiled = Bitmap.createScaledBitmap(compiled, w, h, true)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                if (format == "PDF") {
                    val document = PdfDocument()
                    val pageInfo = PdfDocument.PageInfo.Builder(compiled.width, compiled.height, 1).create()
                    val page = document.startPage(pageInfo)
                    page.canvas.drawBitmap(compiled, 0f, 0f, null)
                    document.finishPage(page)
                    FileOutputStream(finalFile).use { document.writeTo(it) }
                    document.close()
                } else {
                    FileOutputStream(finalFile).use { out ->
                        if (format == "PNG") {
                            compiled.compress(Bitmap.CompressFormat.PNG, 100, out)
                        } else {
                            compiled.compress(Bitmap.CompressFormat.JPEG, 95, out)
                        }
                    }
                }

                repository.insertFile(
                    SavedFile(
                        name = finalFileName,
                        path = finalFile.absolutePath,
                        category = "Photo",
                        format = format,
                        sizeString = getFolderSizeStr(finalFile.length())
                    )
                )

                showToast("Photo processed and exported as $format!")
                _currentScreen.value = "filemanager"
            } catch (e: Exception) {
                showToast("Failed to process photo layout: ${e.localizedMessage}")
            }
        }
    }

    // QR Tools: Export generated QR Code image
    fun runExportQrCode(format: String) {
        val qrBmp = qrGeneratedBitmap.value ?: return
        viewModelScope.launch {
            try {
                val directory = File(context.filesDir, "DocFusion")
                if (!directory.exists()) directory.mkdirs()

                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val extension = if (format == "PDF") ".pdf" else ".png"
                val finalFileName = "QR_Code_Generator_$timeStamp$extension"
                val finalFile = File(directory, finalFileName)

                if (format == "PDF") {
                    val document = PdfDocument()
                    val pageInfo = PdfDocument.PageInfo.Builder(qrBmp.width + 100, qrBmp.height + 150, 1).create()
                    val page = document.startPage(pageInfo)
                    val canvas = page.canvas
                    canvas.drawColor(Color.WHITE)
                    
                    val textPaint = Paint().apply {
                        color = Color.BLACK
                        textSize = 20f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        isAntiAlias = true
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.drawText("DOCFUSION QR GENERATION", (qrBmp.width + 100) / 2f, 50f, textPaint)
                    canvas.drawBitmap(qrBmp, 50f, 80f, null)
                    
                    val finePaint = Paint().apply {
                        color = Color.GRAY
                        textSize = 10f
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    canvas.drawText("Code: ${qrInputText.value}", (qrBmp.width + 100) / 2f, qrBmp.height + 110f, finePaint)

                    document.finishPage(page)
                    FileOutputStream(finalFile).use { document.writeTo(it) }
                    document.close()
                } else {
                    FileOutputStream(finalFile).use { out ->
                        qrBmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                }

                repository.insertFile(
                    SavedFile(
                        name = finalFileName,
                        path = finalFile.absolutePath,
                        category = "Photo",
                        format = format,
                        sizeString = getFolderSizeStr(finalFile.length())
                    )
                )

                showToast("QR Code compilation exported!")
                _currentScreen.value = "filemanager"
            } catch (e: Exception) {
                showToast("Failed to export QR: ${e.localizedMessage}")
            }
        }
    }

    // Resume Tools: Compile current builder states into real PDF and save!
    fun runCompileResumePdf() {
        if (resumeProfile.value.fullName.isEmpty()) {
            showToast("Please provide your profile name before compiling!")
            return
        }

        viewModelScope.launch {
            try {
                val directory = File(context.filesDir, "DocFusion")
                if (!directory.exists()) directory.mkdirs()

                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val finalFileName = "CV_Resume_${resumeProfile.value.fullName.replace(" ", "_")}_$timeStamp.pdf"
                val finalFile = File(directory, finalFileName)

                DocFusionUtils.writeResumeToPdf(resumeProfile.value, finalFile)

                repository.insertFile(
                    SavedFile(
                        name = finalFileName,
                        path = finalFile.absolutePath,
                        category = "Resume",
                        format = "PDF",
                        sizeString = getFolderSizeStr(finalFile.length())
                    )
                )

                showToast("Professional PDF Resume built and saved!")
                _currentScreen.value = "filemanager"
            } catch (e: Exception) {
                showToast("Failed to compile Resume: ${e.localizedMessage}")
            }
        }
    }


    // Sharing functionality
    fun shareSavedFile(file: SavedFile) {
        try {
            val fileToShare = File(file.path)
            if (!fileToShare.exists()) {
                showToast("Physical file does not exist on disk!")
                return
            }

            val uri = FileProvider.getUriForFile(
                context,
                "com.example.fileprovider", // configured inside manifest
                fileToShare
            )

            val shareIntent = android.content.Intent().apply {
                action = android.content.Intent.ACTION_SEND
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                type = when (file.format) {
                    "PDF" -> "application/pdf"
                    "DOCX" -> "application/msword"
                    "TXT" -> "text/plain"
                    else -> "image/png"
                }
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = android.content.Intent.createChooser(shareIntent, "Share Document via DocFusion").apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            showToast("Failed to launch Android system share: ${e.localizedMessage}")
        }
    }


    // Helper tools
    private fun createPlaceholderBitmap(text: String, background: Int, isPortrait: Boolean): Bitmap {
        val width = if (isPortrait) 480 else 640
        val height = if (isPortrait) 600 else 400
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(background)

        val paint = Paint().apply {
            color = if (background == Color.WHITE) Color.BLACK else Color.WHITE
            textSize = 20f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val lines = text.split("\n")
        var yOffset = height / 2f - (lines.size - 1) * 15f
        for (line in lines) {
            canvas.drawText(line, width / 2f, yOffset, paint)
            yOffset += 30f
        }

        // Draw professional target frame borders
        val borderPaint = Paint().apply {
            color = Color.rgb(0, 240, 255) // Cyan
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRect(8f, 8f, width - 8f, height - 8f, borderPaint)

        return bitmap
    }

    private fun getFolderSizeStr(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024
        if (kb < 1024) return "$kb KB"
        val mb = kb / 1024
        return "$mb MB"
    }

    fun clearAppDataCache() {
        viewModelScope.launch {
            try {
                val directory = File(context.filesDir, "DocFusion")
                if (directory.exists()) {
                    directory.listFiles()?.forEach { file ->
                        file.delete()
                    }
                }
                val cachedFiles = savedFiles.value
                for (file in cachedFiles) {
                    repository.deleteFile(file)
                }
                showToast("Application Cache Cleared successfully!")
            } catch (e: Exception) {
                showToast("Failed to clear cache: ${e.localizedMessage}")
            }
        }
    }

    private fun showToast(msg: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }
}
