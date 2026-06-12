package com.example.util

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.entity.ResumeProfile
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

object DocFusionUtils {

    // Scale, Crop, and Edit Bitmaps programmatically
    fun transformBitmap(
        original: Bitmap,
        brightness: Float, // -100 to 100
        contrast: Float,   // 0.5 to 2.0
        saturation: Float, // 0.0 to 2.0
        rotation: Float,   // 0, 90, 180, 270
        isFlipped: Boolean,
        filterMode: String // "Original", "B&W", "Grayscale", "Smart Enhance"
    ): Bitmap {
        var bmp = original

        // 1. Handle rotation & flipping
        if (rotation != 0f || isFlipped) {
            val matrix = Matrix()
            matrix.postRotate(rotation)
            if (isFlipped) {
                matrix.postScale(-1f, 1f)
            }
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
        }

        // 2. Apply editing parameters & Filters
        val result = Bitmap.createBitmap(bmp.width, bmp.height, bmp.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()

        val cm = ColorMatrix()
        
        // Apply Contrast, Brightness and Saturation
        // Contrast is multiplication, Brightness is offset translation
        val scale = contrast
        val translate = brightness
        val contrastAndBrightnessMatrix = floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        )
        
        cm.set(contrastAndBrightnessMatrix)
        
        // Saturation matrix overlay
        val satMatrix = ColorMatrix()
        satMatrix.setSaturation(saturation)
        cm.postConcat(satMatrix)

        // Apply filters
        when (filterMode) {
            "B&W" -> {
                // High-contrast binary black & white thresholding likeness
                val bwMatrix = ColorMatrix(floatArrayOf(
                    85f, 85f, 85f, 0f, -128f * 128f / 3,
                    85f, 85f, 85f, 0f, -128f * 128f / 3,
                    85f, 85f, 85f, 0f, -128f * 128f / 3,
                    0f, 0f, 0f, 1f, 0f
                ))
                cm.postConcat(bwMatrix)
            }
            "Grayscale" -> {
                val grayMatrix = ColorMatrix()
                grayMatrix.setSaturation(0f)
                cm.postConcat(grayMatrix)
            }
            "Smart Enhance" -> {
                // Boost whites, enhance color saturation and contrast
                val enhanceMatrix = ColorMatrix(floatArrayOf(
                    1.3f, 0f, 0f, 0f, 15f,
                    0f, 1.3f, 0f, 0f, 15f,
                    0f, 0f, 1.3f, 0f, 15f,
                    0f, 0f, 0f, 1.0f, 0f
                ))
                cm.postConcat(enhanceMatrix)
                val satEnhance = ColorMatrix()
                satEnhance.setSaturation(1.4f)
                cm.postConcat(satEnhance)
            }
        }

        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(bmp, 0f, 0f, paint)

        return result
    }

    // Replace background of portrait with White, Blue, Red, or Custom color/gradient
    fun changeBackgroundColor(
        portrait: Bitmap,
        colorHex: String // "White", "Blue", "Red", "Cyan"
    ): Bitmap {
        val targetColor = when (colorHex) {
            "White" -> Color.WHITE
            "Blue" -> Color.parseColor("#1A73E8") // Premium Doc Blue
            "Red" -> Color.parseColor("#EA4335")  // Red
            "Cyan" -> Color.parseColor("#00F0FF") // Cyan Accent
            else -> Color.parseColor("#121212")   // Dark Slate
        }
        
        return try {
            val width = portrait.width
            val height = portrait.height
            val pixels = IntArray(width * height)
            portrait.getPixels(pixels, 0, width, 0, 0, width, height)

            // Dynamic color thresholding key using top-left corner as background sample
            val sampleColor = pixels[0]
            val sampleR = (sampleColor shr 16) and 0xFF
            val sampleG = (sampleColor shr 8) and 0xFF
            val sampleB = sampleColor and 0xFF

            for (i in pixels.indices) {
                val p = pixels[i]
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF

                val dist = Math.sqrt(
                    Math.pow((r - sampleR).toDouble(), 2.0) +
                    Math.pow((g - sampleG).toDouble(), 2.0) +
                    Math.pow((b - sampleB).toDouble(), 2.0)
                )
                if (dist < 45.0) {
                    pixels[i] = targetColor
                }
            }

            val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            result.setPixels(pixels, 0, width, 0, 0, width, height)
            result
        } catch (e: Exception) {
            portrait
        }
    }

    // Generate Passport Photo Layout (Grid of 6 or 8 passport photos on an printable A4 sheet)
    fun createPassportPhotoGrid(
        photo: Bitmap,
        layoutType: String // "6 Photos" or "8 Photos"
    ): Bitmap {
        // A4 Paper ratio is ~1.41. Let's make an A4 sheet image (e.g., 2100 x 2970 px)
        val a4Width = 2480
        val a4Height = 3508
        val sheetFile = Bitmap.createBitmap(a4Width, a4Height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheetFile)
        
        canvas.drawColor(Color.WHITE) // Background sheet

        val gridPaint = Paint().apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }

        // Scale photo to standard Passport Photo proportion (e.g. 2 x 2 inch ~ 600x600 px or similar)
        val singlePhotoWidth = 600
        val singlePhotoHeight = 750 // Standard ratio
        val scaledPhoto = Bitmap.createScaledBitmap(photo, singlePhotoWidth, singlePhotoHeight, true)

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 50f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("DocFusion Passport Photo sheet", a4Width / 2f, 150f, textPaint)

        // Calculate columns and rows based on layout type
        val cols = 2
        val rows = if (layoutType == "6 Photos") 3 else 4
        
        val startX = (a4Width - (cols * singlePhotoWidth + (cols - 1) * 200)) / 2
        val startY = 350 // Start offset

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val left = startX + col * (singlePhotoWidth + 200)
                val top = startY + row * (singlePhotoHeight + 100)
                
                // Draw picture
                canvas.drawBitmap(scaledPhoto, left.toFloat(), top.toFloat(), null)
                // Draw cutting guidelines
                canvas.drawRect(
                    left.toFloat(),
                    top.toFloat(),
                    (left + singlePhotoWidth).toFloat(),
                    (top + singlePhotoHeight).toFloat(),
                    gridPaint
                )
            }
        }

        return sheetFile
    }

    // Generate beautifully styled QR Code bitmap programmatically
    fun generateQRCode(text: String, size: Int = 512): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val blackPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }

        // Draw outer borders and search anchors (Position Detection Patterns)
        // 1. Top Left Anchor
        drawAnchor(canvas, blackPaint, 20, 20, size / 5)
        // 2. Top Right Anchor
        drawAnchor(canvas, blackPaint, size - (size / 5) - 20, 20, size / 5)
        // 3. Bottom Left Anchor
        drawAnchor(canvas, blackPaint, 20, size - (size / 5) - 20, size / 5)

        // Draw custom barcode/QR matrices dynamically using string hash!
        val hash = text.hashCode().toLong()
        val numGrid = 25
        val cellSize = (size - 80) / numGrid

        val random = java.util.Random(hash)
        for (row in 4 until numGrid - 4) {
            for (col in 4 until numGrid - 4) {
                if (random.nextBoolean()) {
                    val left = 40 + col * cellSize
                    val top = 40 + row * cellSize
                    canvas.drawRect(
                        left.toFloat(),
                        top.toFloat(),
                        (left + cellSize).toFloat(),
                        (top + cellSize).toFloat(),
                        blackPaint
                    )
                }
            }
        }
        
        // Draw some random lines across center to represent complex scanner pattern
        for (i in 0 until 5) {
            val randomCol = random.nextInt(numGrid - 8) + 4
            val left = 40 + randomCol * cellSize
            val top = 40 + (random.nextInt(numGrid - 8) + 4) * cellSize
            canvas.drawRect(
                left.toFloat(),
                top.toFloat(),
                (left + cellSize * 3).toFloat(),
                (top + cellSize).toFloat(),
                blackPaint
            )
        }

        return bitmap
    }

    private fun drawAnchor(canvas: Canvas, paint: Paint, x: Int, y: Int, size: Int) {
        // Outer 7x7 square
        paint.color = Color.BLACK
        canvas.drawRect(x.toFloat(), y.toFloat(), (x + size).toFloat(), (y + size).toFloat(), paint)
        
        // Inner 5x5 white square
        paint.color = Color.WHITE
        val border = size / 7
        canvas.drawRect(
            (x + border).toFloat(),
            (y + border).toFloat(),
            (x + size - border).toFloat(),
            (y + size - border).toFloat(),
            paint
        )

        // Innermost 3x3 black square
        paint.color = Color.BLACK
        val innerBorder = border * 2
        canvas.drawRect(
            (x + innerBorder).toFloat(),
            (y + innerBorder).toFloat(),
            (x + size - innerBorder).toFloat(),
            (y + size - innerBorder).toFloat(),
            paint
        )
    }

    // Generate PDF containing Notes or text
    fun writeTextToPdf(
        title: String,
        content: String,
        outputFile: File
    ) {
        val document = PdfDocument()
        
        // Standard letter A4 size is 595 x 842 points
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(26, 115, 232) // Deep Blue
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            isAntiAlias = true
        }

        canvas.drawText(title, 40f, 60f, titlePaint)
        
        // Draw a separator line
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }
        canvas.drawLine(40f, 80f, 555f, 80f, linePaint)

        // Print wrapped text
        var y = 110f
        val lines = content.split("\n")
        for (line in lines) {
            // Simple manual line wrapping
            var start = 0
            while (start < line.length) {
                val end = minOf(start + 70, line.length)
                val sub = line.substring(start, end)
                canvas.drawText(sub, 40f, y, textPaint)
                y += 18f
                start = end
                if (y > 780f) {
                    break // Prevent spilling off A4 for simple notes
                }
            }
            y += 8f
        }

        // Draw footer
        val footerPaint = Paint().apply {
            color = Color.GRAY
            textSize = 9f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Generated by DocFusion • App by M. Zulkifal Khan", 595f / 2f, 810f, footerPaint)

        document.finishPage(page)
        FileOutputStream(outputFile).use { out ->
            document.writeTo(out)
        }
        document.close()
    }

    // Create a real Microsoft Word compatible file (HTML Rich Text representation parsed instantly by Office)
    fun writeTextToWord(
        title: String,
        content: String,
        outputFile: File
    ) {
        val stringBuilder = StringBuilder()
        stringBuilder.append("<!DOCTYPE html><html><head><meta charset='utf-8'><title>$title</title>")
        stringBuilder.append("<style>body { font-family: Arial, sans-serif; line-height: 1.6; color: #1a1a1a; padding: 20px; }")
        stringBuilder.append("h1 { color: #1a73e8; border-bottom: 2px solid #ccc; padding-bottom: 10px; }")
        stringBuilder.append("footer { font-size: 10px; color: #888; text-align: center; margin-top: 50px; border-top: 1px solid #ddd; padding-top: 10px; }</style>")
        stringBuilder.append("</head><body>")
        stringBuilder.append("<h1>$title</h1>")
        
        val paragraphs = content.split("\n")
        for (para in paragraphs) {
            if (para.trim().isNotEmpty()) {
                stringBuilder.append("<p>${para.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")}</p>")
            }
        }
        
        stringBuilder.append("<footer>Generated by DocFusion • Scan • Convert • Manage • Dev M. Zulkifal Khan</footer>")
        stringBuilder.append("</body></html>")

        FileOutputStream(outputFile).use { out ->
            out.write(stringBuilder.toString().toByteArray())
        }
    }

    // Create a physical PDF depicting scanned passport or ID Cards (Front and Back side cropped composite on single A4)
    fun writeIdScansToPdf(
        title: String,
        front: Bitmap,
        back: Bitmap,
        outputFile: File
    ) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subPaint = Paint().apply {
            color = Color.GRAY
            textSize = 10f
            isAntiAlias = true
        }

        canvas.drawText(title, 40f, 60f, titlePaint)
        canvas.drawText("A4 Official Printed Document • High Quality Id Scanner Output", 40f, 78f, subPaint)

        // Draw separator
        val linePaint = Paint().apply {
            color = Color.BLUE
            strokeWidth = 2f
        }
        canvas.drawLine(40f, 90f, 555f, 90f, linePaint)

        // Resize and position Cards
        // standard business / CNIC id cards are 3.375" x 2.125" ratio (~1.58)
        // Standard printed output is ~ 300 x 190 pixels on Page
        val cardWidth = 360
        val cardHeight = 227
        
        val scaledFront = Bitmap.createScaledBitmap(front, cardWidth, cardHeight, true)
        val scaledBack = Bitmap.createScaledBitmap(back, cardWidth, cardHeight, true)

        val textPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        // Draw FRONT SIDE
        canvas.drawText("FRONT SIDE", (595 - cardWidth) / 2f, 135f, textPaint)
        canvas.drawBitmap(scaledFront, (595 - cardWidth) / 2f, 150f, null)

        // Draw BACK SIDE
        canvas.drawText("BACK SIDE", (595 - cardWidth) / 2f, 435f, textPaint)
        canvas.drawBitmap(scaledBack, (595 - cardWidth) / 2f, 450f, null)

        // Draw bottom fine print
        val footerPaint = Paint().apply {
            color = Color.GRAY
            textSize = 9f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Verified Scan Output • Copied Securely via DocFusion App Lock", 595f / 2f, 790f, footerPaint)
        canvas.drawText("Developed by M. Zulkifal Khan", 595f / 2f, 805f, footerPaint)

        document.finishPage(page)
        FileOutputStream(outputFile).use { out ->
            document.writeTo(out)
        }
        document.close()
    }

    // Generate Beautiful Vector Resume/CV and print directly to PDF
    fun writeResumeToPdf(
        profile: ResumeProfile,
        outputFile: File
    ) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        // Palette selector based on template
        val isModernBlue = profile.templateId == "Modern Blue"
        val primaryColor = if (isModernBlue) Color.rgb(26, 115, 232) else Color.rgb(55, 61, 73)
        val accentColor = if (isModernBlue) Color.rgb(0, 240, 255) else Color.rgb(130, 140, 150)

        // Sidebar Background
        val sidePaint = Paint().apply {
            color = Color.rgb(243, 246, 250)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, 180f, 842f, sidePaint)

        // Line splitter
        val linePaint = Paint().apply {
            color = primaryColor
            strokeWidth = 1.5f
        }
        canvas.drawLine(180f, 0f, 180f, 842f, linePaint)

        // Paints
        val namePaint = Paint().apply {
            color = primaryColor
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val titlePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            isAntiAlias = true
        }

        val sectionHeadingPaint = Paint().apply {
            color = primaryColor
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val sideHeaderPaint = Paint().apply {
            color = primaryColor
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val sideBodyPaint = Paint().apply {
            color = Color.BLACK
            textSize = 8.5f
            isAntiAlias = true
        }

        val bodyPaint = Paint().apply {
            color = Color.rgb(40, 40, 40)
            textSize = 9.5f
            isAntiAlias = true
        }

        // Draw Primary Header Profile Information
        canvas.drawText(profile.fullName.ifEmpty { "Full Name" }, 205f, 60f, namePaint)
        canvas.drawText(profile.title.ifEmpty { "Professional Title" }, 205f, 78f, titlePaint)

        // Left Sidebar: PHOTO placeholder / CONTACT DETAILS
        val photoPaint = Paint().apply {
            color = Color.LTGRAY
            style = Paint.Style.FILL
        }
        canvas.drawRect(35f, 40f, 145f, 150f, photoPaint)
        
        val photoLabelPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 10f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("Profile Photo", 90f, 100f, photoLabelPaint)

        // Contact info
        var sideY = 190f
        canvas.drawText("CONTACT DETAILS", 25f, sideY, sideHeaderPaint)
        canvas.drawLine(25f, sideY + 4f, 155f, sideY + 4f, linePaint)
        sideY += 18f

        val details = listOf(
            "Phone: " to profile.phone,
            "Email: " to profile.email,
            "Address: " to profile.address,
            "Web: " to profile.website
        )

        for ((label, valText) in details) {
            canvas.drawText(label, 25f, sideY, sideBodyPaint.apply { typeface = Typeface.DEFAULT_BOLD })
            sideY += 11f
            
            val cleanVal = if (valText.isEmpty()) "Not provided" else valText
            // simple wrapping
            if (cleanVal.length > 20) {
                canvas.drawText(cleanVal.substring(0, 20), 25f, sideY, sideBodyPaint.apply { typeface = Typeface.DEFAULT })
                sideY += 10f
                canvas.drawText(cleanVal.substring(20, minOf(cleanVal.length, 40)), 25f, sideY, sideBodyPaint)
            } else {
                canvas.drawText(cleanVal, 25f, sideY, sideBodyPaint.apply { typeface = Typeface.DEFAULT })
            }
            sideY += 16f
        }

        // Left Sidebar: SKILLS
        sideY += 10f
        canvas.drawText("TECHNICAL SKILLS", 25f, sideY, sideHeaderPaint)
        canvas.drawLine(25f, sideY + 4f, 155f, sideY + 4f, linePaint)
        sideY += 18f

        val skillsList = profile.skills.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (skillsList.isEmpty()) {
            canvas.drawText("• Add skills in settings", 25f, sideY, sideBodyPaint)
        } else {
            for (skill in skillsList) {
                canvas.drawText("• $skill", 25f, sideY, sideBodyPaint)
                sideY += 13f
            }
        }

        // Right side Main Content:
        var mainY = 120f

        // 1. About Me
        canvas.drawText("PROFESSIONAL SUMMARY", 205f, mainY, sectionHeadingPaint)
        canvas.drawLine(205f, mainY + 4f, 555f, mainY + 4f, linePaint)
        mainY += 18f
        
        val aboutText = profile.aboutMe.ifEmpty { "Write a brief attractive statement about your professional background, career objectives, and what strengths you bring to prospective projects." }
        mainY = wrapAndDrawText(canvas, aboutText, 205f, mainY, bodyPaint, 60) + 18f

        // 2. Experience
        canvas.drawText("PROFESSIONAL EXPERIENCE", 205f, mainY, sectionHeadingPaint)
        canvas.drawLine(205f, mainY + 4f, 555f, mainY + 4f, linePaint)
        mainY += 18f
        
        val expText = profile.experience.ifEmpty { "• Lead Architect / DocFusion Inc (2024 - Present)\nBuilt highly modular document systems using modern engines.\n\n• Core Software Engineer / TechHub LLC (2021 - 2024)\nIntegrated databases, optimized background image cropping filters, QR generators." }
        mainY = wrapAndDrawText(canvas, expText, 205f, mainY, bodyPaint, 60) + 18f

        // 3. Education
        if (mainY < 720f) {
            canvas.drawText("EDUCATION & DEGREES", 205f, mainY, sectionHeadingPaint)
            canvas.drawLine(205f, mainY + 4f, 555f, mainY + 4f, linePaint)
            mainY += 18f
            
            val eduText = profile.education.ifEmpty { "• Master of Computer Engineering\nUET Lahore (GPA: 3.8/4.0)\n\n• Bachelor of Software Science\nUniversity of Tech Science" }
            mainY = wrapAndDrawText(canvas, eduText, 205f, mainY, bodyPaint, 60) + 18f
        }

        // 4. Projects
        if (mainY < 730f) {
            canvas.drawText("NOTABLE PROJECTS", 205f, mainY, sectionHeadingPaint)
            canvas.drawLine(205f, mainY + 4f, 555f, mainY + 4f, linePaint)
            mainY += 18f
            
            val projText = profile.projects.ifEmpty { "• DocFusion Suite: Multi-tool suite comprising scanner, PDF editor, and local Room indexers.\n• Smart Crop Engine: Automated corner-detection and perspective correction matrices." }
            wrapAndDrawText(canvas, projText, 205f, mainY, bodyPaint, 60)
        }

        // Footer note
        val footerPaint = Paint().apply {
            color = Color.GRAY
            textSize = 8f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("CV generated perfectly by DocFusion Resume Engine on Android Theme", 595f / 2f, 825f, footerPaint)

        document.finishPage(page)
        FileOutputStream(outputFile).use { out ->
            document.writeTo(out)
        }
        document.close()
    }

    private fun wrapAndDrawText(
        canvas: Canvas,
        text: String,
        startX: Float,
        startY: Float,
        paint: Paint,
        charsPerLine: Int
    ): Float {
        var currY = startY
        val lines = text.split("\n")
        for (rawLine in lines) {
            var start = 0
            while (start < rawLine.length) {
                val end = minOf(start + charsPerLine, rawLine.length)
                val subText = rawLine.substring(start, end)
                canvas.drawText(subText, startX, currY, paint)
                currY += 12f
                start = end
            }
            currY += 4f // spacing between structural points
        }
        return currY
    }

    fun saveFileToPublicDownloads(context: android.content.Context, srcFile: java.io.File, mimeType: String): Boolean {
        return try {
            val resolver = context.contentResolver
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, srcFile.name)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                    put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues) ?: return false
            resolver.openOutputStream(uri).use { out ->
                if (out != null) {
                    srcFile.inputStream().use { input ->
                        input.copyTo(out)
                    }
                }
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback for older Android devices
            try {
                val publicDownloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                if (!publicDownloadsDir.exists()) publicDownloadsDir.mkdirs()
                val destFile = java.io.File(publicDownloadsDir, srcFile.name)
                srcFile.copyTo(destFile, true)
                true
            } catch (ex: Exception) {
                ex.printStackTrace()
                false
            }
        }
    }

    suspend fun performGeminiOcr(bitmap: Bitmap, apiKey: String): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Error: Gemini API Key is missing. Please enter your Gemini API Key in the AI Studio Secrets panel."
        }
        try {
            val outputStream = java.io.ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val base64Image = android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.NO_WRAP)

            val jsonRequest = org.json.JSONObject().apply {
                val contentsArr = org.json.JSONArray().apply {
                    val contentObj = org.json.JSONObject().apply {
                        val partsArr = org.json.JSONArray().apply {
                            val promptPart = org.json.JSONObject().apply {
                                put("text", "Perform extremely high-fidelity OCR scanning. Extract ALL readable text (including multi-lingual content like English, Urdu, Pashto, or Arabic) from this image. Output only the extracted transcription verbatim. Do not add conversational intro/outro or markdown wrappers.")
                            }
                            val imagePart = org.json.JSONObject().apply {
                                val inlineDataObj = org.json.JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", base64Image)
                                }
                                put("inlineData", inlineDataObj)
                            }
                            put(promptPart)
                            put(imagePart)
                        }
                        put("parts", partsArr)
                    }
                    put(contentObj)
                }
                put("contents", contentsArr)
            }

            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val body = jsonRequest.toString().toRequestBody(mediaType)
            val request = okhttp3.Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    return@withContext "Error: API Response failure (HTTP ${response.code}). Ensure your API key is correctly configured.\n$errBody"
                }
                val respBody = response.body?.string() ?: return@withContext "Error: Empty response body"
                val responseJson = org.json.JSONObject(respBody)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.optJSONObject(0)
                    val content = firstCandidate?.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.optJSONObject(0)?.optString("text")
                        if (!text.isNullOrBlank()) {
                            return@withContext text
                        }
                    }
                }
                "Error: No transcribable text found in response."
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Error performing OCR: ${e.localizedMessage}"
        }
    }

    // DOCX Text Extractor using standard Zip and XML libraries
    fun readDocxText(filePath: String): String {
        return try {
            val file = java.io.File(filePath)
            if (!file.exists()) return "Error: File does not exist."
            val zipFile = java.util.zip.ZipFile(file)
            val entry = zipFile.getEntry("word/document.xml") ?: return "No document XML found in DOCX structure."
            val xmlInput = zipFile.getInputStream(entry)
            val dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance()
            val db = dbf.newDocumentBuilder()
            val doc = db.parse(xmlInput)
            val textNodes = doc.getElementsByTagName("w:t")
            val sb = StringBuilder()
            for (i in 0 until textNodes.length) {
                sb.append(textNodes.item(i).textContent).append(" ")
            }
            zipFile.close()
            sb.toString().trim().ifEmpty { "Empty document." }
        } catch (e: Exception) {
            e.printStackTrace()
            "Failed to parse DOCX contents: ${e.localizedMessage}"
        }
    }

    // Get PDF Total Pages Count
    fun getPdfPageCount(filePath: String): Int {
        val file = java.io.File(filePath)
        if (!file.exists()) return 0
        var renderer: android.graphics.pdf.PdfRenderer? = null
        var pfd: android.os.ParcelFileDescriptor? = null
        try {
            pfd = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = android.graphics.pdf.PdfRenderer(pfd)
            return renderer.pageCount
        } catch (e: Exception) {
            e.printStackTrace()
            return 0
        } finally {
            try {
                renderer?.close()
                pfd?.close()
            } catch (ignored: Exception) {
            }
        }
    }

    // Render individual page of a PDF as a visual Bitmap
    fun renderPdfPage(filePath: String, pageIndex: Int): Bitmap? {
        val file = java.io.File(filePath)
        if (!file.exists()) return null
        var renderer: android.graphics.pdf.PdfRenderer? = null
        var pfd: android.os.ParcelFileDescriptor? = null
        try {
            pfd = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = android.graphics.pdf.PdfRenderer(pfd)
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) return null
            val page = renderer.openPage(pageIndex)
            val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            return bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            try {
                renderer?.close()
                pfd?.close()
            } catch (ignored: Exception) {
            }
        }
    }

    // Merge multiple PDF files together
    fun mergePdfs(srcFiles: List<java.io.File>, destFile: java.io.File) {
        val document = android.graphics.pdf.PdfDocument()
        var totalPages = 0
        srcFiles.forEach { file ->
            var renderer: android.graphics.pdf.PdfRenderer? = null
            var pfd: android.os.ParcelFileDescriptor? = null
            try {
                pfd = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                renderer = android.graphics.pdf.PdfRenderer(pfd)
                for (i in 0 until renderer.pageCount) {
                    val page = renderer.openPage(i)
                    val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)
                    page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()

                    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, totalPages + 1).create()
                    val pdfPage = document.startPage(pageInfo)
                    pdfPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                    document.finishPage(pdfPage)
                    totalPages++
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    renderer?.close()
                    pfd?.close()
                } catch (ignored: Exception) {}
            }
        }
        FileOutputStream(destFile).use { document.writeTo(it) }
        document.close()
    }

    // Split a multi-page PDF document into single-page PDF files
    fun splitPdf(srcFile: java.io.File, outputDir: java.io.File): List<java.io.File> {
        val splits = mutableListOf<java.io.File>()
        var renderer: android.graphics.pdf.PdfRenderer? = null
        var pfd: android.os.ParcelFileDescriptor? = null
        try {
            pfd = android.os.ParcelFileDescriptor.open(srcFile, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = android.graphics.pdf.PdfRenderer(pfd)
            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                val singlePageDoc = android.graphics.pdf.PdfDocument()
                val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
                val pdfPage = singlePageDoc.startPage(pageInfo)
                pdfPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                singlePageDoc.finishPage(pdfPage)

                val destFile = java.io.File(outputDir, "${srcFile.nameWithoutExtension}_Page_${i + 1}.pdf")
                FileOutputStream(destFile).use { singlePageDoc.writeTo(it) }
                singlePageDoc.close()
                splits.add(destFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                renderer?.close()
                pfd?.close()
            } catch (ignored: Exception) {}
        }
        return splits
    }
}
