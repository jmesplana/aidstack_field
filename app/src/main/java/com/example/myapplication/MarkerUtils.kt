package com.example.myapplication

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import com.example.myapplication.gdacs.DisasterSeverity
import com.example.myapplication.gdacs.DisasterType
import com.example.myapplication.fieldreport.ReportCategory
import com.example.myapplication.fieldreport.ReportSeverity

object MarkerUtils {
    fun createDisasterMarker(
        context: Context,
        type: DisasterType,
        severity: DisasterSeverity
    ): BitmapDrawable {
        val size = 120
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Get severity color
        val severityColor = when (severity) {
            DisasterSeverity.RED -> 0xFFF44336.toInt()
            DisasterSeverity.ORANGE -> 0xFFFF9800.toInt()
            DisasterSeverity.GREEN -> 0xFF4CAF50.toInt()
            else -> 0xFF9E9E9E.toInt()
        }

        // Draw background circle
        val circlePaint = Paint().apply {
            color = severityColor
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2.5f, circlePaint)

        // Draw white border
        val borderPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2.5f, borderPaint)

        // Draw emoji icon
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 50f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val emoji = type.icon
        val textBounds = Rect()
        textPaint.getTextBounds(emoji, 0, emoji.length, textBounds)
        val x = size / 2f
        val y = size / 2f - textBounds.exactCenterY()
        canvas.drawText(emoji, x, y, textPaint)

        return BitmapDrawable(context.resources, bitmap)
    }

    fun createFieldReportMarker(
        context: Context,
        category: ReportCategory,
        severity: ReportSeverity
    ): BitmapDrawable {
        val size = 120
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Get severity color
        val severityColor = severity.color.toInt()

        // Draw square/diamond shape for field reports (different from disasters)
        val squarePath = Path().apply {
            moveTo(size / 2f, size / 5f)
            lineTo(size * 4f / 5f, size / 2f)
            lineTo(size / 2f, size * 4f / 5f)
            lineTo(size / 5f, size / 2f)
            close()
        }

        val shapePaint = Paint().apply {
            color = severityColor
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        canvas.drawPath(squarePath, shapePaint)

        // Draw white border
        val borderPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawPath(squarePath, borderPaint)

        // Draw emoji icon
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 45f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val emoji = category.icon
        val textBounds = Rect()
        textPaint.getTextBounds(emoji, 0, emoji.length, textBounds)
        val x = size / 2f
        val y = size / 2f - textBounds.exactCenterY()
        canvas.drawText(emoji, x, y, textPaint)

        return BitmapDrawable(context.resources, bitmap)
    }
}
