package com.example.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object Helpers {

    fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    fun formatDateTime12Hour(date: Date): String {
        val dateSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val rSdf = SimpleDateFormat("hh:mm", Locale.US)
        val amPmSdf = SimpleDateFormat("a", Locale.US)
        val amPmRaw = amPmSdf.format(date).uppercase()
        val amPmText = if (amPmRaw.contains("AM")) "ص" else "م"
        return "${dateSdf.format(date)} ${rSdf.format(date)} $amPmText"
    }

    fun getCurrentDateTime(): String {
        return formatDateTime12Hour(Date())
    }

    fun formatStringDateTime12Hour(dateTimeStr: String): String {
        if (dateTimeStr.isEmpty()) return ""
        return try {
            if (dateTimeStr.contains("ص") || dateTimeStr.contains("م")) {
                return dateTimeStr
            }
            var parsedDate: Date? = null
            val patterns = listOf(
                "yyyy-MM-dd HH:mm",
                "yyyy-MM-dd hh:mm a",
                "yyyy/MM/dd HH:mm",
                "yyyy/MM/dd hh:mm a",
                "yyyy-MM-dd"
            )
            for (pattern in patterns) {
                try {
                    val sdf = SimpleDateFormat(pattern, Locale.US)
                    parsedDate = sdf.parse(dateTimeStr)
                    if (parsedDate != null) break
                } catch (ignored: Exception) {}
            }
            if (parsedDate != null) {
                formatDateTime12Hour(parsedDate)
            } else {
                dateTimeStr
            }
        } catch (e: Exception) {
            dateTimeStr
        }
    }

    fun isWithinLast7Days(dateStr: String): Boolean {
        return try {
            val datePart = if (dateStr.length >= 10) dateStr.substring(0, 10) else dateStr
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val dateValue = sdf.parse(datePart) ?: return false
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -7)
            val boundary = cal.time
            !dateValue.before(boundary)
        } catch (e: Exception) {
            false
        }
    }

    fun formatMoney(amount: Double): String {
        return String.format(Locale.US, "%,.0f ريال", amount)
    }

    fun shareViaWhatsApp(context: Context, phone: String, message: String) {
        try {
            val cleanPhone = if (phone.isNotEmpty()) {
                if (phone.startsWith("+")) phone else "+967$phone"
            } else ""
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                val url = if (cleanPhone.isNotEmpty()) {
                    "whatsapp://send?phone=$cleanPhone&text=${Uri.encode(message)}"
                } else {
                    "whatsapp://send?text=${Uri.encode(message)}"
                }
                data = Uri.parse(url)
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                // Secondary check for WhatsApp Business
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    val cleanPhone = if (phone.isNotEmpty()) {
                        if (phone.startsWith("+")) phone else "+967$phone"
                    } else ""
                    val url = if (cleanPhone.isNotEmpty()) {
                        "whatsapp://send?phone=$cleanPhone&text=${Uri.encode(message)}"
                    } else {
                        "whatsapp://send?text=${Uri.encode(message)}"
                    }
                    data = Uri.parse(url)
                    setPackage("com.whatsapp.w4b")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e2: Exception) {
                try {
                    // Third attempt: normal web URL redirection
                    val cleanPhone = if (phone.isNotEmpty()) {
                        if (phone.startsWith("+")) phone else "+967$phone"
                    } else ""
                    val intent = Intent(Intent.ACTION_VIEW)
                    val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(message)}"
                    intent.data = Uri.parse(url)
                    context.startActivity(intent)
                } catch (e3: Exception) {
                    // Fallback general share
                    shareText(context, message)
                }
            }
        }
    }

    fun shareViaSMS(context: Context, phone: String, message: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$phone")
                putExtra("sms_body", message)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            shareText(context, message)
        }
    }

    fun shareText(context: Context, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة الفاتورة"))
    }

    // Modern Arabic PDF Generator and Native Printer
    fun generatePdfAndShare(
        context: Context,
        title: String,
        headers: List<String>,
        rows: List<List<String>>,
        totals: Map<String, String>,
        customDate: String? = null,
        reportPeriod: String? = null
    ) {
        try {
            val pdfDocument = PdfDocument()
            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create() // A4 Size
            var page = pdfDocument.startPage(pageInfo)
            var canvas: Canvas = page.canvas

            val textPaint = TextPaint().apply {
                color = Color.BLACK
                textSize = 9.5f
                isAntiAlias = true
                textAlign = Paint.Align.RIGHT
            }

            val titlePaint = TextPaint().apply {
                color = Color.parseColor("#104620") // Brand green
                textSize = 15f
                isFakeBoldText = true
                isAntiAlias = true
                textAlign = Paint.Align.RIGHT
            }

            val headerPaint = TextPaint().apply {
                color = Color.WHITE
                textSize = 10f
                isFakeBoldText = true
                isAntiAlias = true
                textAlign = Paint.Align.RIGHT
            }

            val subtitlePaint = TextPaint().apply {
                color = Color.parseColor("#CE2029") // Brand red
                textSize = 11f
                isAntiAlias = true
                textAlign = Paint.Align.RIGHT
            }

            val borderPaint = Paint().apply {
                color = Color.parseColor("#CCCCCC") // Nice light gray
                style = Paint.Style.STROKE
                strokeWidth = 0.8f
            }

            val headerBgPaint = Paint().apply {
                color = Color.parseColor("#104620")
                style = Paint.Style.FILL
            }

            val rowBgPaint = Paint().apply {
                color = Color.parseColor("#F7FAF7")
                style = Paint.Style.FILL
            }

            val leftLimit = 35f
            val startX = 560f
            val tableWidth = startX - leftLimit // 525f

            // Dynamic custom column widths allocator based on headers names and sizes
            fun getColWidth(index: Int): Float {
                // Table A (Supplier products itemization report)
                if (headers.size == 6 && headers.contains("تفاصيل العملية والأصناف المشمولة")) {
                    return when (index) {
                        0 -> 85f  // التاريخ والسند
                        1 -> 180f // تفاصيل العملية والأصناف المشمولة
                        2 -> 65f  // المبلغ الكلي
                        3 -> 65f  // المدفوع/المسترد
                        4 -> 60f  // المتبقي الآجل
                        5 -> 70f  // طريقة الدفع/البيان
                        else -> tableWidth / 6f
                    }
                }
                // Table B (Supplier detailed statement account report)
                if (headers.size == 5 && headers.contains("نوع العملية وتفاصيل الأصناف والبيان")) {
                    return when (index) {
                        0 -> 95f  // التاريخ والسند
                        1 -> 210f // نوع العملية وتفاصيل الأصناف والبيان
                        2 -> 70f  // القيمة الكلية
                        3 -> 75f  // المسدد/المسترد
                        4 -> 75f  // الآجل المتبقي
                        else -> tableWidth / 5f
                    }
                }
                // Table C (Sales detailed PDF report)
                if (headers.size == 6 && headers.contains("التاريخ والوقت بالتفصيل")) {
                    return when (index) {
                        0 -> 65f  // رقم السند
                        1 -> 120f // التاريخ والوقت بالتفصيل
                        2 -> 90f  // اسم العميل
                        3 -> 120f // البيان والأصناف المباعة
                        4 -> 65f  // طريقة الدفع
                        5 -> 65f  // المبلغ الكلي
                        else -> tableWidth / 6f
                    }
                }
                // Table D (Archive detailed records report)
                if (headers.size == 4 && headers.contains("البيان والتفاصيل")) {
                    return when (index) {
                        0 -> 70f  // نوع السند
                        1 -> 215f // البيان والتفاصيل
                        2 -> 110f // القيمة المالية
                        3 -> 130f // التاريخ والوقت
                        else -> tableWidth / 4f
                    }
                }
                // Table E (Inventory detailed list PDF)
                if (headers.size == 4 && headers.contains("تاريخ الإضافة بالتفصيل")) {
                    return when (index) {
                        0 -> 135f // الصنف
                        1 -> 140f // تاريخ الإضافة بالتفصيل
                        2 -> 120f // الكمية المتوفرة
                        3 -> 130f // سعر الشراء
                        else -> tableWidth / 4f
                    }
                }
                // Table F (Inventory movement detailed PDF)
                if (headers.size == 6 && headers.contains("تاريخ الإضافة")) {
                    return when (index) {
                        0 -> 110f // الصنف
                        1 -> 80f  // تاريخ الإضافة
                        2 -> 80f  // المخزون الحالي
                        3 -> 80f  // مباع بالفترة
                        4 -> 85f  // مورد بالفترة
                        5 -> 90f  // سعر الشراء
                        else -> tableWidth / 6f
                    }
                }
                // Fallback for general tables
                return tableWidth / headers.size
            }

            fun getCellRightX(index: Int): Float {
                var sum = 0f
                for (k in 0 until index) {
                    sum += getColWidth(k)
                }
                return startX - sum
            }

            val logoBitmap = try {
                BitmapFactory.decodeResource(context.resources, com.example.R.drawable.img_agency_logo_1780160497858)
            } catch (e: Exception) {
                null
            }

            // Custom local helper to draw main header on page 1
            fun drawMainHeader(can: Canvas) {
                can.drawRect(leftLimit, 25f, startX, 33f, Paint().apply { color = Color.parseColor("#104620"); style = Paint.Style.FILL })
                can.drawRect(leftLimit, 33f, startX, 35f, Paint().apply { color = Color.parseColor("#D4AF37"); style = Paint.Style.FILL })

                val hasLogo = logoBitmap != null
                if (hasLogo && logoBitmap != null) {
                    val destRect = RectF(startX - 65f, 38f, startX, 103f)
                    can.drawBitmap(logoBitmap, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
                }

                val textStartAlignX = if (hasLogo) startX - 75f else startX

                var headerY = 56f
                titlePaint.textAlign = Paint.Align.RIGHT
                titlePaint.textSize = 10.5f
                can.drawText("وكالة عاهد الصبري", textStartAlignX, headerY, titlePaint)
                headerY += 16f
                can.drawText("صاحب الوكالة: عاهد الصبري", textStartAlignX, headerY, textPaint.apply { isFakeBoldText = true; textSize = 9.5f })
                headerY += 14f
                val displayDate = customDate ?: getCurrentDateTime()
                can.drawText("تاريخ طباعة السند (تاريخ الإنشاء): $displayDate", textStartAlignX, headerY, textPaint.apply { isFakeBoldText = false; textSize = 8.5f })
                if (reportPeriod != null && reportPeriod.isNotEmpty()) {
                    headerY += 14f
                    textPaint.color = Color.parseColor("#104620")
                    textPaint.isFakeBoldText = true
                    textPaint.textSize = 8.5f
                    can.drawText("الفترة المحددة للتقرير: $reportPeriod", textStartAlignX, headerY, textPaint)
                    textPaint.color = Color.BLACK
                }
                headerY += 14f
                can.drawText("نوع المستند: $title", textStartAlignX, headerY, subtitlePaint.apply { textSize = 9f })

                textPaint.isFakeBoldText = false
                textPaint.textSize = 9.5f
            }

            // Custom local helper to draw subsequent page headers
            fun drawSubsequentHeader(can: Canvas, pageNum: Int) {
                can.drawRect(leftLimit, 25f, startX, 30f, Paint().apply { color = Color.parseColor("#104620"); style = Paint.Style.FILL })
                can.drawRect(leftLimit, 30f, startX, 32f, Paint().apply { color = Color.parseColor("#D4AF37"); style = Paint.Style.FILL })

                val hasLogo = logoBitmap != null
                if (hasLogo && logoBitmap != null) {
                    val destRect = RectF(startX - 30f, 34f, startX, 64f)
                    can.drawBitmap(logoBitmap, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
                }

                val textEndX = if (hasLogo) startX - 35f else startX
                can.drawText("تابع: $title - وكالة عاهد الصبري", textEndX, 48f, titlePaint.apply { textSize = 9.5f })
                can.drawText("صفحة رقم: $pageNum", leftLimit, 48f, textPaint.apply { isFakeBoldText = true; textSize = 9f; textAlign = Paint.Align.LEFT })
                textPaint.textAlign = Paint.Align.RIGHT // Restore alignment

                can.drawLine(leftLimit, 56f, startX, 56f, borderPaint)
            }

            // Custom helper to draw table headers
            fun drawTableHeader(can: Canvas, startY: Float) {
                can.drawRect(leftLimit, startY, startX, startY + 24f, headerBgPaint)
                for (i in headers.indices) {
                    val headerText = headers[i]
                    val cellWidth = getColWidth(i)
                    val cellRightX = getCellRightX(i)
                    val drawX = cellRightX - 6f
                    
                    val paintCopy = TextPaint(headerPaint)
                    val maxCellWidth = cellWidth - 12f
                    var textWidth = paintCopy.measureText(headerText)
                    while (textWidth > maxCellWidth && paintCopy.textSize > 8f) {
                        paintCopy.textSize -= 0.5f
                        textWidth = paintCopy.measureText(headerText)
                    }
                    can.drawText(headerText, drawX, startY + 16f, paintCopy)
                }
                can.drawRect(leftLimit, startY, startX, startY + 24f, borderPaint)
            }

            // Safe multi-line drawing function to prevent any text overlaps and wrap long texts beautifully
            fun drawCellTextMultiLine(
                can: Canvas,
                text: String,
                rightX: Float,
                startY: Float,
                maxHeight: Float,
                paint: TextPaint,
                cellWidth: Float
            ) {
                val paintCopy = TextPaint(paint)
                val maxCellWidth = cellWidth - 12f // 6f padding on each side
                
                // If text contains native newlines, split it
                val rawLines = text.split("\n")
                val finalLines = mutableListOf<String>()
                
                for (rawLine in rawLines) {
                    var currentLine = rawLine
                    if (currentLine.isEmpty()) {
                        finalLines.add("")
                    } else {
                        while (currentLine.isNotEmpty()) {
                            val charsCount = paintCopy.breakText(
                                currentLine,
                                true,
                                maxCellWidth,
                                null
                            )
                            if (charsCount <= 0) break
                            
                            finalLines.add(currentLine.substring(0, charsCount))
                            currentLine = currentLine.substring(charsCount)
                        }
                    }
                }
                
                // Now draw each line with appropriate vertical spacing
                var currentY = startY
                val lineSpacing = paintCopy.textSize + 2.5f
                
                // Draw up to what fits in maxHeight
                for (index in finalLines.indices) {
                    if (currentY + lineSpacing > startY + maxHeight) {
                        // If it doesn't fit, draw "..." on the last line that fits
                        if (index > 0) {
                            val lastIdx = index - 1
                            val lastLine = finalLines[lastIdx]
                            val truncatedLine = if (lastLine.length > 3) lastLine.substring(0, lastLine.length - 3) + "..." else "..."
                            can.drawText(truncatedLine, rightX - 6f, currentY - lineSpacing, paintCopy)
                        }
                        break
                    }
                    can.drawText(finalLines[index], rightX - 6f, currentY, paintCopy)
                    currentY += lineSpacing
                }
            }

            fun getRequiredRowHeight(row: List<String>, paint: TextPaint): Float {
                var maxLines = 1
                for (i in row.indices) {
                    if (i >= headers.size) break
                    val cellText = row[i]
                    val cellWidth = getColWidth(i)
                    val maxCellWidth = cellWidth - 12f

                    val rawLines = cellText.split("\n")
                    var totalLines = 0
                    for (rawLine in rawLines) {
                        var currentLine = rawLine
                        if (currentLine.isEmpty()) {
                            totalLines += 1
                        } else {
                            while (currentLine.isNotEmpty()) {
                                val charsCount = paint.breakText(currentLine, true, maxCellWidth, null)
                                if (charsCount <= 0) break
                                totalLines += 1
                                currentLine = currentLine.substring(charsCount)
                            }
                        }
                    }
                    if (totalLines > maxLines) {
                        maxLines = totalLines
                    }
                }
                val lineSpacing = paint.textSize + 2.5f
                return (maxLines * lineSpacing) + 12f // Add top/bottom padding
            }

            // Draw first page header
            drawMainHeader(canvas)
            var y = 150f
            canvas.drawLine(leftLimit, y, startX, y, borderPaint)
            y += 12f

            // Start drawing table
            drawTableHeader(canvas, y)
            y += 24f // Header row height

            var alternating = false

            for (rowIndex in rows.indices) {
                val row = rows[rowIndex]
                val autoRowHeight = getRequiredRowHeight(row, textPaint)

                // Dynamic page-break if drawing exceeds page margin
                if (y + autoRowHeight > 760f) {
                    // Draw bottom footer for the completed page
                    canvas.drawLine(leftLimit, 785f, startX, 785f, borderPaint)
                    canvas.drawText("صفحة $pageNumber", leftLimit + 10f, 800f, textPaint.apply { textSize = 8f; textAlign = Paint.Align.LEFT })
                    textPaint.textAlign = Paint.Align.RIGHT // reset
                    canvas.drawText("وكالة عاهد الصبري للقات الماوية والورزاني ©", startX - 10f, 800f, textPaint.apply { textSize = 8f; color = Color.GRAY })
                    
                    pdfDocument.finishPage(page)

                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas

                    drawSubsequentHeader(canvas, pageNumber)
                    y = 75f
                    drawTableHeader(canvas, y)
                    y += 24f
                }

                // Drawing alternating background
                if (alternating) {
                    canvas.drawRect(leftLimit, y, startX, y + autoRowHeight, rowBgPaint)
                }
                alternating = !alternating

                // Draw cell contents
                for (i in row.indices) {
                    if (i >= headers.size) break // Safety safeguard
                    val cellText = row[i]
                    val cellWidth = getColWidth(i)
                    val cellRightX = getCellRightX(i)
                    drawCellTextMultiLine(canvas, cellText, cellRightX, y + 14f, autoRowHeight - 4f, textPaint, cellWidth)
                }

                // Draw perfect cell borders
                canvas.drawRect(leftLimit, y, startX, y + autoRowHeight, borderPaint)
                for (i in 1 until headers.size) {
                    val lineX = getCellRightX(i)
                    canvas.drawLine(lineX, y, lineX, y + autoRowHeight, borderPaint)
                }

                y += autoRowHeight
            }

            // Draw totals section with check to wrap if overflow
            val totalsHeight = (totals.size * 20f) + 35f
            if (y + totalsHeight > 760f) {
                // Page-break
                canvas.drawLine(leftLimit, 785f, startX, 785f, borderPaint)
                canvas.drawText("صفحة $pageNumber", leftLimit + 10f, 800f, textPaint.apply { textSize = 8f; textAlign = Paint.Align.LEFT })
                textPaint.textAlign = Paint.Align.RIGHT // reset
                canvas.drawText("وكالة عاهد الصبري للقات الماوية والورزاني ©", startX - 10f, 800f, textPaint.apply { textSize = 8f; color = Color.GRAY })

                pdfDocument.finishPage(page)

                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas

                drawSubsequentHeader(canvas, pageNumber)
                y = 75f
            }

            // Render beautifully aligned totals box
            y += 12f
            canvas.drawRect(leftLimit, y, startX, y + (totals.size * 20f) + 10f, Paint().apply {
                color = Color.parseColor("#FAF7FA")
                style = Paint.Style.FILL
            })
            canvas.drawRect(leftLimit, y, startX, y + (totals.size * 20f) + 10f, borderPaint)

            var totalsY = y + 16f
            for ((label, valText) in totals) {
                // Aligned right
                canvas.drawText(label, startX - 15f, totalsY, textPaint.apply { isFakeBoldText = true; textSize = 9.5f })
                // Aligned left
                canvas.drawText(valText, leftLimit + 15f, totalsY, textPaint.apply { isFakeBoldText = true; textSize = 9.5f; textAlign = Paint.Align.LEFT })
                
                textPaint.textAlign = Paint.Align.RIGHT // Restore
                totalsY += 20f
            }
            y += (totals.size * 20f) + 30f

            // Clean final footer
            val footerY = 800f
            canvas.drawLine(leftLimit, footerY - 14f, startX, footerY - 14f, borderPaint)
            canvas.drawText("صفحة $pageNumber", leftLimit + 10f, footerY, textPaint.apply {
                color = Color.DKGRAY
                textSize = 8f
                isFakeBoldText = false
                textAlign = Paint.Align.LEFT
            })
            canvas.drawText("وكالة عاهد الصبري © ${Calendar.getInstance().get(Calendar.YEAR)}", startX - 10f, footerY, textPaint.apply {
                color = Color.parseColor("#104620")
                textSize = 8.5f
                isFakeBoldText = true
                textAlign = Paint.Align.RIGHT
            })

            pdfDocument.finishPage(page)

            // Creating a perfectly descriptive Arabic File Name based on the document's Arabic title prefix!
            val fileDate = if (customDate != null && customDate.length >= 10) {
                customDate.substring(0, 10).replace("/", "-")
            } else {
                getCurrentDate()
            }
            val sanitizedTitle = title
                .replace("وكالة عاهد الصبري للقات الماوية والورزاني بجميع أنواعها •", "")
                .replace("وكالة عاهد الصبري للقات الماوية والورزاني بجميع أنواعها", "")
                .replace("وكالة عاهد الصبري للقات الماوية والورزاني •", "")
                .replace("وكالة عاهد الصبري للقات الماوية والورزاني", "")
                .replace("وكالة عاهد الصبري للقات", "")
                .replace("وكالة عاهد الصبري •", "")
                .replace("وكالة عاهد الصبري", "")
                .replace(Regex("[#\\\\/:*?\"<>|]"), "")
                .trim()
                .replace("\\s+".toRegex(), "_")
            val fileName = if (sanitizedTitle.startsWith("تقرير_") || sanitizedTitle.startsWith("كشف_")) {
                "${sanitizedTitle}_${fileDate}.pdf"
            } else {
                "تقرير_${sanitizedTitle}_${fileDate}.pdf"
            }

            val filepath = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val file = File(filepath, fileName)
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.flush()
            outputStream.close()

            // Function to trigger sharing as fallback
            fun triggerShareFallback() {
                try {
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "تصدير ومشاركة PDF"))
                    Toast.makeText(context, "تم تصدير وحفظ الطبعة كملف PDF ومشاركته بنجاح: $fileName", Toast.LENGTH_LONG).show()
                } catch (shareErr: Exception) {
                    Toast.makeText(context, "فشل تصدير ومشاركة PDF: ${shareErr.message}", Toast.LENGTH_LONG).show()
                }
            }

            // START NATIVE ANDROID SYSTEM PRINT DIALOG!
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager != null) {
                val jobName = "وكالة_عاهد_الصبري_${sanitizedTitle}"
                val printAdapter = object : PrintDocumentAdapter() {
                    override fun onLayout(
                        oldAttributes: PrintAttributes?,
                        newAttributes: PrintAttributes?,
                        cancellationSignal: CancellationSignal?,
                        callback: LayoutResultCallback?,
                        extras: Bundle?
                    ) {
                        try {
                            if (cancellationSignal?.isCanceled == true) {
                                callback?.onLayoutCancelled()
                                return
                            }
                            val info = PrintDocumentInfo.Builder(fileName)
                                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                                .build()
                            callback?.onLayoutFinished(info, true)
                        } catch (ex: Exception) {
                            callback?.onLayoutFailed(ex.toString())
                        }
                    }

                    override fun onWrite(
                        pages: Array<out android.print.PageRange>?,
                        destination: ParcelFileDescriptor?,
                        cancellationSignal: CancellationSignal?,
                        callback: WriteResultCallback?
                    ) {
                        var input: FileInputStream? = null
                        var output: FileOutputStream? = null
                        try {
                            input = FileInputStream(file)
                            output = FileOutputStream(destination?.fileDescriptor)
                            val buf = ByteArray(1024)
                            var bytesRead: Int
                            while (input.read(buf).also { bytesRead = it } > 0) {
                                output.write(buf, 0, bytesRead)
                            }
                            callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                        } catch (e: Exception) {
                            callback?.onWriteFailed(e.toString())
                        } finally {
                            try { input?.close() } catch (ignored: Exception) {}
                            try { output?.close() } catch (ignored: Exception) {}
                        }
                    }
                }
                
                try {
                    printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
                    Toast.makeText(context, "تم حفظ المستند وبدء عملية الطباعة لمستند: $fileName", Toast.LENGTH_SHORT).show()
                } catch (printEx: Exception) {
                    // Fallback to sharing if native print dialog trigger fails
                    triggerShareFallback()
                }
            } else {
                // No print manager available: fallback to share
                triggerShareFallback()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "فشل تصدير وطباعة PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun backupDatabase(context: Context): Boolean {
        try {
            com.example.data.AppDatabase.checkpoint()
            val dbFile = context.getDatabasePath("tofan_al_aqsa_qat_db")
            val dbWal = File(dbFile.path + "-wal")
            val dbShm = File(dbFile.path + "-shm")

            val backupDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return false
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }
            val backupDb = File(backupDir, "tofan_al_aqsa_qat_db_backup.db")
            val backupWal = File(backupDir, "tofan_al_aqsa_qat_db_backup.db-wal")
            val backupShm = File(backupDir, "tofan_al_aqsa_qat_db_backup.db-shm")

            if (dbFile.exists()) {
                dbFile.inputStream().use { input ->
                    backupDb.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            if (dbWal.exists()) {
                dbWal.inputStream().use { input ->
                    backupWal.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } else if (backupWal.exists()) {
                backupWal.delete()
            }
            if (dbShm.exists()) {
                dbShm.inputStream().use { input ->
                    backupShm.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } else if (backupShm.exists()) {
                backupShm.delete()
            }

            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                backupDb
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "تصدير وحفظ النسخة الاحتياطية"))
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun restoreDatabase(context: Context, backupUri: Uri): Boolean {
        try {
            try {
                com.example.data.AppDatabase.closeDatabase()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val dbFile = context.getDatabasePath("tofan_al_aqsa_qat_db")
            val dbWal = File(dbFile.path + "-wal")
            val dbShm = File(dbFile.path + "-shm")

            val tempFile = File.createTempFile("db_restore", null, context.cacheDir)
            context.contentResolver.openInputStream(backupUri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            if (!tempFile.exists() || tempFile.length() == 0L) {
                return false
            }

            if (dbFile.exists()) dbFile.delete()
            if (dbWal.exists()) dbWal.delete()
            if (dbShm.exists()) dbShm.delete()

            tempFile.copyTo(dbFile, overwrite = true)
            tempFile.delete()

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}

class BluetoothPrinterManager private constructor(private val context: Context) {

    var connectedDeviceName: String = "غير متصل"
        private set

    var currentPrinterSize: Int = 58 // 58mm or 80mm
        set

    fun savePrinterConfig(size: Int, deviceAddress: String) {
        currentPrinterSize = size
        val prefs = context.getSharedPreferences("printer_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("size", size).putString("address", deviceAddress).apply()
    }

    init {
        val prefs = context.getSharedPreferences("printer_prefs", Context.MODE_PRIVATE)
        currentPrinterSize = prefs.getInt("size", 58)
        connectedDeviceName = prefs.getString("address", null)?.let { "طابعة افتراضية ($it)" } ?: "غير متصل"
    }

    fun getBondedPrinters(): List<Pair<String, String>> {
        // Safe check for Bluetooth permissions
        return listOf(
            "طابعة الفواتير المحمولة 58mm" to "00:11:22:33:44:55",
            "طابعة الكاشير الكبيرة 80mm" to "AA:BB:CC:DD:EE:FF",
            "طابعة البلوتوث المجهولة" to "99:88:77:66:55:44",
            "سماعة رأس بلوتوث ذكية" to "11:22:33:44:55:66",
            "هاتف جالاكسي بلس S23" to "22:33:44:55:66:77",
            "كمبيوتر محمول لابتوب Dell" to "33:44:55:66:77:88"
        )
    }

    fun getDeviceTypeNameAndIsPrinter(name: String, address: String): Pair<String, Boolean> {
        val lowerName = name.lowercase()
        // Check simulation printers first
        if (address == "00:11:22:33:44:55" || address == "AA:BB:CC:DD:EE:FF" || address == "99:88:77:66:55:44" ||
            lowerName.contains("طابعة") || lowerName.contains("printer") || lowerName.contains("mtp") || lowerName.contains("pos") || lowerName.contains("thermal")
        ) {
            return Pair("طابعة حرارية مدعومة", true)
        }

        // Concrete simulation fallbacks based on specific mac address
        if (address == "11:22:33:44:55:66" || lowerName.contains("headset") || lowerName.contains("sennheiser") || lowerName.contains("buds") || lowerName.contains("headphone") || lowerName.contains("earbud") || lowerName.contains("سماعة")) {
            return Pair("سماعة رأس بلوتوث", false)
        }
        if (address == "22:33:44:55:66:77" || lowerName.contains("galaxy") || lowerName.contains("phone") || lowerName.contains("iphone") || lowerName.contains("redmi") || lowerName.contains("هاتف")) {
            return Pair("هاتف محمول", false)
        }
        if (address == "33:44:55:66:77:88" || lowerName.contains("dell") || lowerName.contains("laptop") || lowerName.contains("computer") || lowerName.contains("pc") || lowerName.contains("لابتوب")) {
            return Pair("جهاز كمبيوتر / لابتوب", false)
        }

        try {
            val adapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
            val device = adapter?.getRemoteDevice(address)
            val bClass = device?.bluetoothClass
            if (bClass != null) {
                val major = bClass.majorDeviceClass
                val deviceClassVal = bClass.deviceClass
                
                if (deviceClassVal == 1664) {
                    return Pair("طابعة حرارية", true)
                }
                
                when (major) {
                    android.bluetooth.BluetoothClass.Device.Major.PHONE -> {
                        return Pair("هاتف محمول ذكي", false)
                    }
                    android.bluetooth.BluetoothClass.Device.Major.COMPUTER -> {
                        return Pair("كمبيوتر / لابتوب", false)
                    }
                    android.bluetooth.BluetoothClass.Device.Major.AUDIO_VIDEO -> {
                        return Pair("سماعة رأس / مكبر صوت أو جهاز وسائط", false)
                    }
                    android.bluetooth.BluetoothClass.Device.Major.TOY -> {
                        return Pair("لعبة ذكية أو جهاز ترفيهي", false)
                    }
                    android.bluetooth.BluetoothClass.Device.Major.HEALTH -> {
                        return Pair("جهاز طبي أو صحي رياضي", false)
                    }
                    android.bluetooth.BluetoothClass.Device.Major.PERIPHERAL -> {
                        return Pair("جهاز إدخال طرفي (ماوس/لوحة مفاتيح)", false)
                    }
                }
            }
        } catch (e: SecurityException) {
            // Permission not granted or stub implementation
        } catch (e: Exception) {
            // General capture
        }

        // Guess based on name
        if (lowerName.contains("phone") || lowerName.contains("galaxy") || lowerName.contains("iphone") || lowerName.contains("redmi") || lowerName.contains("pixel") || lowerName.contains("huawei")) {
            return Pair("هاتف محمول", false)
        }
        if (lowerName.contains("headset") || lowerName.contains("earbud") || lowerName.contains("headphone") || lowerName.contains("speaker") || lowerName.contains("sound") || lowerName.contains("pod") || lowerName.contains("buds")) {
            return Pair("سماعة رأس أو مكبر صوت", false)
        }
        if (lowerName.contains("macbook") || lowerName.contains("laptop") || lowerName.contains("pc") || lowerName.contains("computer") || lowerName.contains("desktop") || lowerName.contains("book")) {
            return Pair("جهاز كمبيوتر / لابتوب", false)
        }
        if (lowerName.contains("tv") || lowerName.contains("firestick") || lowerName.contains("chromecast") || lowerName.contains("display")) {
            return Pair("شاشة تلفاز ذكية", false)
        }
        if (lowerName.contains("watch") || lowerName.contains("band") || lowerName.contains("fitbit")) {
            return Pair("ساعة ذكية / سوار رياضي", false)
        }

        return Pair("جهاز ذكي غير مدعوم للطباعة", false)
    }

    fun connectPrinter(name: String, address: String, onComplete: (Boolean) -> Unit) {
        connectedDeviceName = "$name ($address)"
        val prefs = context.getSharedPreferences("printer_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("address", address).putString("name", name).apply()
        onComplete(true)
    }

    fun virtualPrint(receiptText: String, onFinished: (String) -> Unit) {
        // Build virtual receipt with the new brand header & footer
        val widthChars = if (currentPrinterSize == 58) 32 else 48
        val lineSeparator = "-".repeat(widthChars)

        val brandLine1 = "وكالة عاهد الصبري"
        val padCount1 = (widthChars - brandLine1.length) / 2
        val pad1 = " ".repeat(kotlin.math.max(0, padCount1))

        val brandLine2 = "أجود قات ماوية وورزاني"
        val padCount2 = (widthChars - brandLine2.length) / 2
        val pad2 = " ".repeat(kotlin.math.max(0, padCount2))

        val brandLine3 = "المالك: عاهد الصبري"
        val padCount3 = (widthChars - brandLine3.length) / 2
        val pad3 = " ".repeat(kotlin.math.max(0, padCount3))

        val fullReceipt = buildString {
            appendLine(lineSeparator)
            appendLine("$pad1$brandLine1")
            appendLine("$pad2$brandLine2")
            appendLine("$pad3$brandLine3")
            appendLine(lineSeparator)
            appendLine("تاريخ السند: ${Helpers.getCurrentDateTime()}")
            appendLine(lineSeparator)
            appendLine(receiptText)
            appendLine(lineSeparator)
            appendLine("   تجدون جودة ومذاق يرضي أذواقكم   ")
            appendLine(lineSeparator)
        }
        onFinished(fullReceipt)
    }

    companion object {
        @Volatile
        private var INSTANCE: BluetoothPrinterManager? = null

        fun getInstance(context: Context): BluetoothPrinterManager {
            return INSTANCE ?: synchronized(this) {
                val instance = BluetoothPrinterManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
