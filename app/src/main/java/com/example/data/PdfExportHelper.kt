package com.example.data

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.model.MonthlyReport
import java.io.File
import java.io.FileOutputStream

object PdfExportHelper {

    fun generateAndShareMonthlyReportPdf(
        context: Context,
        report: MonthlyReport
    ): Result<File> {
        return try {
            val doc = PdfDocument()
            val pageWidth = 595 // A4 standard width at 72 dpi
            val pageHeight = 842 // A4 standard height
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = doc.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            // Paints
            val titlePaint = Paint().apply {
                color = Color.rgb(30, 41, 59)
                textSize = 18f
                isFakeBoldText = true
                textAlign = Paint.Align.RIGHT
            }

            val subtitlePaint = Paint().apply {
                color = Color.rgb(71, 85, 105)
                textSize = 12f
                textAlign = Paint.Align.RIGHT
            }

            val bodyPaint = Paint().apply {
                color = Color.rgb(15, 23, 42)
                textSize = 11f
                textAlign = Paint.Align.RIGHT
            }

            val headerBgPaint = Paint().apply {
                color = Color.rgb(241, 245, 249)
            }

            val cardBgPaint = Paint().apply {
                color = Color.rgb(248, 250, 252)
            }

            val borderPaint = Paint().apply {
                color = Color.rgb(203, 213, 225)
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }

            val accentPaint = Paint().apply {
                color = Color.rgb(14, 116, 144)
                style = Paint.Style.FILL
            }

            // Draw header banner
            canvas.drawRect(30f, 30f, (pageWidth - 30).toFloat(), 95f, headerBgPaint)
            canvas.drawRect(30f, 30f, (pageWidth - 30).toFloat(), 95f, borderPaint)
            canvas.drawRect((pageWidth - 36).toFloat(), 30f, (pageWidth - 30).toFloat(), 95f, accentPaint)

            canvas.drawText("گزارش ماهانه کارکرد و سرویس و نگهداری", (pageWidth - 48).toFloat(), 60f, titlePaint)
            canvas.drawText("سامانه مدیریت داخلی تجهیزات و انبار روغن", (pageWidth - 48).toFloat(), 80f, subtitlePaint)

            // Equipment Info Card
            var y = 125f
            canvas.drawRoundRect(30f, y - 15f, (pageWidth - 30).toFloat(), y + 80f, 8f, 8f, cardBgPaint)
            canvas.drawRoundRect(30f, y - 15f, (pageWidth - 30).toFloat(), y + 80f, 8f, 8f, borderPaint)

            val equip = EquipmentData.getEquipmentById(report.equipmentId)
            val driverStr = if (equip?.driverName != null) "مسئول / راننده: ${equip.driverName} (${equip.driverRole ?: ""})" else ""

            canvas.drawText("نام وسیله: ${report.equipmentName}", (pageWidth - 50).toFloat(), y + 10f, bodyPaint)
            canvas.drawText("دوره ماهانه: ${report.month}", (pageWidth - 50).toFloat(), y + 32f, bodyPaint)
            if (driverStr.isNotEmpty()) {
                canvas.drawText(driverStr, (pageWidth - 50).toFloat(), y + 54f, bodyPaint)
            }
            canvas.drawText("تأییدکننده: ${report.closedByDisplayName}", (pageWidth - 300).toFloat(), y + 32f, bodyPaint)

            // Summary Statistics Box
            y += 115f
            canvas.drawText("خلاصه عملکرد ماه:", (pageWidth - 50).toFloat(), y, titlePaint)
            y += 20f

            val statBoxWidth = (pageWidth - 60) / 2f
            // Left Box: Work hours
            canvas.drawRoundRect(30f, y, 30f + statBoxWidth - 10f, y + 80f, 6f, 6f, cardBgPaint)
            canvas.drawRoundRect(30f, y, 30f + statBoxWidth - 10f, y + 80f, 6f, 6f, borderPaint)
            canvas.drawText("مجموع ساعت کارکرد ماه: ${report.totalWorkHours} ساعت", 30f + statBoxWidth - 25f, y + 30f, bodyPaint)
            canvas.drawText("تعداد روزهای کاری ثبت‌شده: ${report.workHoursCount} روز", 30f + statBoxWidth - 25f, y + 55f, bodyPaint)

            // Right Box: Maintenance & Oil
            val rightBoxX = 30f + statBoxWidth + 10f
            canvas.drawRoundRect(rightBoxX, y, (pageWidth - 30).toFloat(), y + 80f, 6f, 6f, cardBgPaint)
            canvas.drawRoundRect(rightBoxX, y, (pageWidth - 30).toFloat(), y + 80f, 6f, 6f, borderPaint)
            canvas.drawText("تعداد سرویس‌های روزانه: ${report.dailyServicesCount}", (pageWidth - 45).toFloat(), y + 25f, bodyPaint)
            canvas.drawText("دفعات تعویض روغن: ${report.oilChangesCount}", (pageWidth - 45).toFloat(), y + 45f, bodyPaint)
            canvas.drawText("مجموع مصرف روغن: ${report.totalOilConsumed} لیتر", (pageWidth - 45).toFloat(), y + 65f, bodyPaint)

            // Legal & Archive Notice
            y += 120f
            canvas.drawRoundRect(30f, y, (pageWidth - 30).toFloat(), y + 70f, 6f, 6f, headerBgPaint)
            canvas.drawRoundRect(30f, y, (pageWidth - 30).toFloat(), y + 70f, 6f, 6f, borderPaint)

            val notePaint = Paint().apply {
                color = Color.rgb(100, 116, 139)
                textSize = 9.5f
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("این سند به عنوان گزارش بسته شده و غیرقابل تغییر (Immutable Snapshot) استخراج گردیده است.", (pageWidth - 45).toFloat(), y + 25f, notePaint)
            canvas.drawText("کلیه حقوق و اطلاعات مربوط به ثبت سوابق، ساعت کار و مصرف روغن در پایگاه داده مرکزی ثبت شده است.", (pageWidth - 45).toFloat(), y + 45f, notePaint)

            // Signature area
            y += 120f
            canvas.drawLine((pageWidth - 220).toFloat(), y, (pageWidth - 50).toFloat(), y, borderPaint)
            canvas.drawText("امضاء و تأیید مسئول فنی", (pageWidth - 135).toFloat(), y + 20f, subtitlePaint)

            doc.finishPage(page)

            // Save PDF file in cache
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) reportsDir.mkdirs()

            val pdfFile = File(reportsDir, "Report_${report.equipmentId}_${report.month}.pdf")
            val outputStream = FileOutputStream(pdfFile)
            doc.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            doc.close()

            // Trigger Share Intent
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "گزارش ماهانه ${report.equipmentName} - ${report.month}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(Intent.createChooser(shareIntent, "ارسال یا ذخیره گزارش PDF").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })

            Result.success(pdfFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
