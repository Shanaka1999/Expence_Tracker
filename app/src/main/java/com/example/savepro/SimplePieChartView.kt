package com.example.savepro

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class SimplePieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 40f
    }

    var income = 60f
    var expense = 40f

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val total = income + expense
        if (total == 0f) return

        val chartSize = width * 0.6f
        val centerY = height / 2f
        val leftMargin = width * 0.35f
        val rectF = RectF(
            leftMargin,
            centerY - chartSize / 2,
            leftMargin + chartSize,
            centerY + chartSize / 2
        )

        val incomeAngle = (income / total) * 360
        val expenseAngle = (expense / total) * 360

        // Draw income slice
        paint.color = Color.parseColor("#4CAF50")
        canvas.drawArc(rectF, 0f, incomeAngle, true, paint)

        // Draw expense slice
        paint.color = Color.parseColor("#F44336")
        canvas.drawArc(rectF, incomeAngle, expenseAngle, true, paint)

        // Draw Legend on left side
        val boxSize = 40f
        val spacing = 30f
        val textOffsetY = boxSize - 10f
        val legendStartX = 60f
        var legendStartY = centerY - boxSize

        // Income
        paint.color = Color.parseColor("#4CAF50")
        canvas.drawRect(legendStartX, legendStartY, legendStartX + boxSize, legendStartY + boxSize, paint)
        canvas.drawText("Income", legendStartX + boxSize + spacing, legendStartY + textOffsetY, textPaint)

        // Expense
        legendStartY += boxSize + 40f
        paint.color = Color.parseColor("#F44336")
        canvas.drawRect(legendStartX, legendStartY, legendStartX + boxSize, legendStartY + boxSize, paint)
        canvas.drawText("Expense", legendStartX + boxSize + spacing, legendStartY + textOffsetY, textPaint)
    }
}
