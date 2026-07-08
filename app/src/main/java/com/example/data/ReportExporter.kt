package com.example.data

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.entity.CitizenReport
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ReportExporter {

    fun generateReportPdf(context: Context, report: CitizenReport): File? {
        val pdfDocument = PdfDocument()
        // A4 page size is 595 x 842 pixels at 72 DPI
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint()

        // Draw title
        val titlePaint = Paint().apply {
            color = Color.parseColor("#1E3A8A") // Dark Navy
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val labelPaint = Paint().apply {
            color = Color.parseColor("#475569")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val valuePaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        var y = 50f

        // 1. Draw Government Header
        canvas.drawText("JANMITRA AI - CITIZEN REPORT", 40f, y, titlePaint)
        y += 25f
        
        paint.color = Color.parseColor("#E2E8F0")
        canvas.drawRect(40f, y, 555f, y + 2f, paint) // Border separator line
        y += 30f

        // 2. Add Issue ID & Category Information
        canvas.drawText("General Information", 40f, y, headerPaint)
        y += 20f

        canvas.drawText("Issue Reference ID:", 40f, y, labelPaint)
        canvas.drawText(report.issueId, 180f, y, valuePaint)
        y += 18f

        canvas.drawText("Grievance Category:", 40f, y, labelPaint)
        canvas.drawText(report.category, 180f, y, valuePaint)
        y += 18f

        canvas.drawText("Submission Date:", 40f, y, labelPaint)
        val dateStr = try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            sdf.format(Date(report.timestamp))
        } catch (e: Exception) {
            "${report.timestamp}"
        }
        canvas.drawText(dateStr, 180f, y, valuePaint)
        y += 18f

        canvas.drawText("Village / Location:", 40f, y, labelPaint)
        canvas.drawText(report.locationName, 180f, y, valuePaint)
        y += 18f

        canvas.drawText("Current Status:", 40f, y, labelPaint)
        canvas.drawText(report.status, 180f, y, valuePaint)
        y += 30f

        // 3. Problem Description
        canvas.drawText("Reported Grievance Description", 40f, y, headerPaint)
        y += 20f
        
        // Wrap text to fit the page
        val descriptionText = report.description
        val maxTextWidth = 515f
        val words = descriptionText.split(" ")
        var line = ""
        for (word in words) {
            val testLine = if (line.isEmpty()) word else "$line $word"
            val width = valuePaint.measureText(testLine)
            if (width < maxTextWidth) {
                line = testLine
            } else {
                canvas.drawText(line, 40f, y, valuePaint)
                y += 16f
                line = word
            }
        }
        if (line.isNotEmpty()) {
            canvas.drawText(line, 40f, y, valuePaint)
            y += 25f
        }

        // 4. Decentralized Planning AI Insights & Scores
        canvas.drawText("JanMitra AI Decentralized Planning Insights", 40f, y, headerPaint)
        y += 20f

        canvas.drawText("Calculated Priority Score:", 40f, y, labelPaint)
        val scorePaint = Paint(valuePaint).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.parseColor("#059669")
        }
        canvas.drawText(String.format(Locale.US, "%.1f / 100.0", report.priorityScore), 220f, y, scorePaint)
        y += 18f

        canvas.drawText("Determined Urgency Level:", 40f, y, labelPaint)
        canvas.drawText(report.urgency, 220f, y, valuePaint)
        y += 18f

        canvas.drawText("Evidence Strength:", 40f, y, labelPaint)
        canvas.drawText(report.evidenceStrength, 220f, y, valuePaint)
        y += 18f

        canvas.drawText("Citizen Sentiment:", 40f, y, labelPaint)
        canvas.drawText(report.citizenSentiment, 220f, y, valuePaint)
        y += 25f

        // Explanations wrapping
        canvas.drawText("Explainable Planning Breakdown", 40f, y, labelPaint)
        y += 18f

        val scoreDetails = listOf(
            "Citizen Demand Score: ${String.format(Locale.US, "%.1f", report.citizenDemandScore)}",
            "Infrastructure Gap Score: ${String.format(Locale.US, "%.1f", report.infraGapScore)}",
            "Population Impact Score: ${String.format(Locale.US, "%.1f", report.populationImpactScore)}",
            "Distance-to-Service Score: ${String.format(Locale.US, "%.1f", report.distanceToServiceScore)}",
            "Historical Neglect Score: ${String.format(Locale.US, "%.1f", report.historicalNeglectScore)}"
        )
        for (detail in scoreDetails) {
            canvas.drawText("• $detail", 50f, y, valuePaint)
            y += 15f
        }
        y += 15f

        canvas.drawText("Planning Recommendation Summary:", 40f, y, labelPaint)
        y += 18f

        val summaryText = report.aiSummary.ifEmpty { report.explanationText }
        val summaryWords = summaryText.split(" ")
        var summaryLine = ""
        for (word in summaryWords) {
            val testLine = if (summaryLine.isEmpty()) word else "$summaryLine $word"
            val width = valuePaint.measureText(testLine)
            if (width < maxTextWidth) {
                summaryLine = testLine
            } else {
                canvas.drawText(summaryLine, 40f, y, valuePaint)
                y += 16f
                summaryLine = word
            }
        }
        if (summaryLine.isNotEmpty()) {
            canvas.drawText(summaryLine, 40f, y, valuePaint)
            y += 30f
        }

        // Footer note
        val footerPaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            isAntiAlias = true
        }
        canvas.drawText("This is an official administrative report copy automatically generated offline by JanMitra AI.", 40f, 810f, footerPaint)

        pdfDocument.finishPage(page)

        val outputFile = File(context.cacheDir, "JanMitra_Report_${report.issueId}.pdf")
        try {
            val outputStream = FileOutputStream(outputFile)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()
            return outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            return null
        }
    }

    fun shareReportPdf(context: Context, report: CitizenReport) {
        val file = generateReportPdf(context, report) ?: return
        val uri = FileProvider.getUriForFile(
            context,
            "com.example.janmitra.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "JanMitra AI Official Report: ${report.issueId}")
            putExtra(Intent.EXTRA_TEXT, "Official citizen grievance report copy generated via JanMitra AI offline planning framework.\nRef: ${report.issueId}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export Official Report PDF"))
    }
}
