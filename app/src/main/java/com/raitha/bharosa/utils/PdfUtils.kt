package com.raitha.bharosa.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.raitha.bharosa.data.FarmerProfile
import com.raitha.bharosa.data.SoilAnalysisItem
import com.raitha.bharosa.data.SoilHealthCard
import com.raitha.bharosa.data.Decision
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateStr get() = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())

fun generateAndSaveFarmReport(
    context: Context,
    profile: FarmerProfile?,
    score: Int,
    decisions: List<Decision>
) {
    val doc = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = doc.startPage(pageInfo)
    drawFarmReport(page.canvas, profile, score, decisions)
    doc.finishPage(page)
    saveAndDownload(context, doc, "Farm_Report_${System.currentTimeMillis()}.pdf", "Performance Report")
    doc.close()
}

fun generateAndSaveSoilHealthCard(
    context: Context,
    profile: FarmerProfile?,
    score: Int,
    healthCard: SoilHealthCard
) {
    val doc = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = doc.startPage(pageInfo)
    drawSoilHealthCard(page.canvas, profile, score, healthCard)
    doc.finishPage(page)
    saveAndDownload(context, doc, "Soil_Health_Card_${System.currentTimeMillis()}.pdf", "Soil Health Card")
    doc.close()
}

// ── Farm Report ──────────────────────────────────────────────────────────────

private fun drawFarmReport(canvas: Canvas, profile: FarmerProfile?, score: Int, decisions: List<Decision>) {
    val w = 595f
    val brandDeep = Color.parseColor("#1B5E20")
    val brandBg   = Color.parseColor("#F1F8E9")
    val gray      = Color.parseColor("#6B7280")
    val black     = Color.BLACK

    // Header background
    val headerPaint = Paint().apply { color = brandDeep; isAntiAlias = true }
    canvas.drawRoundRect(RectF(0f, 0f, w, 110f), 0f, 0f, headerPaint)

    // Header text
    val titlePaint = Paint().apply {
        color = Color.WHITE; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 22f; isAntiAlias = true; textAlign = Paint.Align.CENTER
    }
    canvas.drawText("RAITHA-BHAROSA HUB", w / 2, 50f, titlePaint)
    titlePaint.textSize = 13f; titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    canvas.drawText("Farm Performance Report", w / 2, 75f, titlePaint)
    titlePaint.textSize = 11f
    canvas.drawText(dateStr, w / 2, 96f, titlePaint)

    // Farmer info card
    val cardPaint = Paint().apply { color = brandBg; isAntiAlias = true }
    canvas.drawRoundRect(RectF(30f, 125f, w - 30f, 215f), 12f, 12f, cardPaint)

    val labelPaint = Paint().apply { color = gray; textSize = 11f; isAntiAlias = true }
    val valuePaint = Paint().apply { color = black; textSize = 12f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }

    fun infoRow(label: String, value: String, y: Float) {
        canvas.drawText(label, 50f, y, labelPaint)
        canvas.drawText(value, 320f, y, valuePaint)
    }
    infoRow("Farmer Name", profile?.name ?: "Valued Farmer", 148f)
    infoRow("Village / District", "${profile?.village ?: "-"}, ${profile?.district ?: "-"}", 168f)
    infoRow("Primary Crop", profile?.primaryCrop?.displayName ?: "Rice", 188f)
    infoRow("Land Area", "${profile?.landArea ?: "-"} acres", 208f)

    // Soil score box
    val scoreBg = when {
        score >= 70 -> Color.parseColor("#DCFCE7")
        score >= 40 -> Color.parseColor("#FEF9C3")
        else -> Color.parseColor("#FEE2E2")
    }
    val scoreColor = when {
        score >= 70 -> Color.parseColor("#16A34A")
        score >= 40 -> Color.parseColor("#CA8A04")
        else -> Color.parseColor("#DC2626")
    }
    val scoreBgPaint = Paint().apply { color = scoreBg; isAntiAlias = true }
    canvas.drawRoundRect(RectF(30f, 230f, w - 30f, 285f), 12f, 12f, scoreBgPaint)
    val scorePaint = Paint().apply { color = scoreColor; textSize = 26f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true; textAlign = Paint.Align.CENTER }
    canvas.drawText("SOIL HEALTH SCORE: $score / 100", w / 2, 267f, scorePaint)

    // Nutrient section header
    val sectionPaint = Paint().apply { color = brandDeep; textSize = 13f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
    canvas.drawText("Agronomist Recommendations", 30f, 315f, sectionPaint)
    val linePaint = Paint().apply { color = brandDeep; strokeWidth = 1.5f }
    canvas.drawLine(30f, 320f, w - 30f, 320f, linePaint)

    // Decisions
    var y = 345f
    val recPaint = Paint().apply { color = black; textSize = 11f; isAntiAlias = true }
    val dotPaint = Paint().apply { color = brandDeep; textSize = 11f; isAntiAlias = true }
    decisions.take(6).forEach { d ->
        canvas.drawText("•", 35f, y, dotPaint)
        recPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("[${d.type}] ${d.status}", 50f, y, recPaint)
        recPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        y += 16f
        // Word-wrap the message
        val words = d.message.split(" ")
        var line = ""
        words.forEach { word ->
            val test = if (line.isEmpty()) word else "$line $word"
            if (recPaint.measureText(test) > 490f) {
                canvas.drawText(line, 55f, y, recPaint); y += 14f; line = word
            } else line = test
        }
        if (line.isNotEmpty()) { canvas.drawText(line, 55f, y, recPaint); y += 14f }
        y += 6f
    }
    if (decisions.isEmpty()) {
        recPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        canvas.drawText("All soil conditions are optimal. Continue regular monitoring.", 50f, y, recPaint)
        y += 18f
    }

    // Footer
    val footerPaint = Paint().apply { color = Color.parseColor("#9CA3AF"); textSize = 9f; isAntiAlias = true; textAlign = Paint.Align.CENTER }
    canvas.drawLine(30f, 810f, w - 30f, 810f, Paint().apply { color = Color.parseColor("#E5E7EB"); strokeWidth = 1f })
    canvas.drawText("This is a computer-generated report by Raitha-Bharosa Hub. Date: $dateStr", w / 2, 828f, footerPaint)
}

// ── Soil Health Card ──────────────────────────────────────────────────────────

private fun drawSoilHealthCard(canvas: Canvas, profile: FarmerProfile?, score: Int, healthCard: SoilHealthCard) {
    val w = 595f
    val brandDeep = Color.parseColor("#1B5E20")
    val brandBg   = Color.parseColor("#F1F8E9")
    val gray      = Color.parseColor("#6B7280")

    // Green header
    val headerPaint = Paint().apply { color = brandDeep; isAntiAlias = true }
    canvas.drawRoundRect(RectF(0f, 0f, w, 130f), 0f, 0f, headerPaint)

    val titlePaint = Paint().apply {
        color = Color.WHITE; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 24f; isAntiAlias = true; textAlign = Paint.Align.CENTER
    }
    canvas.drawText("SOIL HEALTH CARD", w / 2, 55f, titlePaint)
    titlePaint.textSize = 11f; titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    canvas.drawText("Ministry of Agriculture & Farmers Welfare (Simulated)", w / 2, 78f, titlePaint)
    titlePaint.textSize = 10f
    canvas.drawText("Certificate No: RBH-${(10000..99999).random()}  |  Date: $dateStr", w / 2, 100f, titlePaint)
    canvas.drawText("Issued To: ${profile?.name ?: "Farmer"}  |  Village: ${profile?.village ?: "-"}", w / 2, 118f, titlePaint)

    // Score circle (simulated with rounded rect)
    val gradeColor = when (healthCard.grade.name) {
        "EXCELLENT" -> Color.parseColor("#16A34A")
        "GOOD"      -> Color.parseColor("#65A30D")
        "FAIR"      -> Color.parseColor("#CA8A04")
        else        -> Color.parseColor("#DC2626")
    }
    val circleBg = Paint().apply { color = brandBg; isAntiAlias = true }
    canvas.drawRoundRect(RectF(240f, 145f, 355f, 215f), 35f, 35f, circleBg)
    val circleBorder = Paint().apply { color = brandDeep; style = Paint.Style.STROKE; strokeWidth = 4f; isAntiAlias = true }
    canvas.drawRoundRect(RectF(240f, 145f, 355f, 215f), 35f, 35f, circleBorder)
    val scoreNumPaint = Paint().apply { color = brandDeep; textSize = 30f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true; textAlign = Paint.Align.CENTER }
    canvas.drawText("$score", w / 2, 192f, scoreNumPaint)
    val gradePaint = Paint().apply { color = gradeColor; textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true; textAlign = Paint.Align.CENTER }
    canvas.drawText(healthCard.grade.name, w / 2, 228f, gradePaint)

    // Section header
    val sectionPaint = Paint().apply { color = brandDeep; textSize = 13f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
    canvas.drawText("Nutrient Analysis", 30f, 255f, sectionPaint)
    canvas.drawLine(30f, 260f, w - 30f, 260f, Paint().apply { color = brandDeep; strokeWidth = 1.5f })

    // Column headers
    val hdrPaint = Paint().apply { color = gray; textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
    canvas.drawText("NUTRIENT", 35f, 280f, hdrPaint)
    canvas.drawText("STATUS", 280f, 280f, hdrPaint)
    canvas.drawText("RECOMMENDATION", 370f, 280f, hdrPaint)

    // Analysis rows
    var y = 300f
    val rowBg = Paint().apply { color = Color.parseColor("#F9FAFB"); isAntiAlias = true }
    val nutrientPaint = Paint().apply { color = Color.BLACK; textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
    val recPaint = Paint().apply { color = gray; textSize = 9f; isAntiAlias = true }

    healthCard.analysis.forEach { item ->
        canvas.drawRoundRect(RectF(28f, y - 14f, w - 28f, y + 26f), 8f, 8f, rowBg)
        canvas.drawText(item.nutrient, 35f, y, nutrientPaint)

        val statusColor = when (item.status) {
            "Low"  -> Color.parseColor("#DC2626")
            "High" -> Color.parseColor("#2563EB")
            else   -> Color.parseColor("#16A34A")
        }
        val statusBgColor = when (item.status) {
            "Low"  -> Color.parseColor("#FEE2E2")
            "High" -> Color.parseColor("#DBEAFE")
            else   -> Color.parseColor("#DCFCE7")
        }
        val tagBg = Paint().apply { color = statusBgColor; isAntiAlias = true }
        canvas.drawRoundRect(RectF(270f, y - 12f, 340f, y + 6f), 6f, 6f, tagBg)
        val statusPaint = Paint().apply { color = statusColor; textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
        canvas.drawText(item.status, 278f, y, statusPaint)

        // Truncate recommendation to fit
        val rec = if (item.recommendation.length > 40) item.recommendation.take(40) + "…" else item.recommendation
        canvas.drawText(rec, 350f, y, recPaint)
        y += 45f
    }

    // Overall grade banner
    val bannerPaint = Paint().apply { color = gradeColor; isAntiAlias = true }
    canvas.drawRoundRect(RectF(30f, y + 10f, w - 30f, y + 50f), 10f, 10f, bannerPaint)
    val bannerTextPaint = Paint().apply { color = Color.WHITE; textSize = 16f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true; textAlign = Paint.Align.CENTER }
    canvas.drawText("OVERALL GRADE: ${healthCard.grade.name}", w / 2, y + 37f, bannerTextPaint)

    // Footer
    val footerPaint = Paint().apply { color = Color.parseColor("#9CA3AF"); textSize = 9f; isAntiAlias = true; textAlign = Paint.Align.CENTER }
    canvas.drawLine(30f, 810f, w - 30f, 810f, Paint().apply { color = Color.parseColor("#E5E7EB"); strokeWidth = 1f })
    canvas.drawText("Digitally generated by Raitha-Bharosa Engine  |  $dateStr", w / 2, 828f, footerPaint)
}

// ── Save & Download ──────────────────────────────────────────────────────────────

private fun saveAndDownload(context: Context, doc: PdfDocument, fileName: String, title: String) {
    val pdfFile: File
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // API 29+ — use MediaStore (no permission needed)
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return
        context.contentResolver.openOutputStream(uri)?.use { doc.writeTo(it) }
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        context.contentResolver.update(uri, values, null, null)

        // Show toast notification
        android.widget.Toast.makeText(
            context,
            "$title downloaded to Downloads folder",
            android.widget.Toast.LENGTH_LONG
        ).show()
    } else {
        // API 26-28 — save to Downloads folder
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        downloadsDir.mkdirs()
        pdfFile = File(downloadsDir, fileName)
        FileOutputStream(pdfFile).use { doc.writeTo(it) }

        // Show toast notification
        android.widget.Toast.makeText(
            context,
            "$title downloaded to ${pdfFile.absolutePath}",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }
}
