package com.droid.googlebooktoolbox.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Custom Googlebook Toolbox vector emblem:
 * A sleek Googlebook laptop with a multicolor "G" on its screen, surrounded by utility tools
 * (an open-end wrench, precision screwdriver, tuning gear, and diagnostic check badge).
 */
@Composable
fun GooglebookToolboxLogo(
  size: Dp = 48.dp,
  modifier: Modifier = Modifier
) {
  Canvas(
    modifier = modifier
      .size(size)
      .testTag("toolbox_logo_canvas")
      .semantics {
        contentDescription = "Googlebook Toolbox logo: Googlebook with G and tools around it"
      }
  ) {
    val s = this.size.minDimension / 108f
    fun u(v: Float) = v * s

    // Rounded background tile
    drawRoundRect(
      color = Color(0xFFEAF4F3),
      topLeft = Offset(u(2f), u(2f)),
      size = Size(u(104f), u(104f)),
      cornerRadius = CornerRadius(u(26f), u(26f))
    )
    val diagBg = Path().apply {
      moveTo(u(2f), u(68f))
      lineTo(u(106f), u(38f))
      lineTo(u(106f), u(80f))
      quadraticTo(u(106f), u(106f), u(80f), u(106f))
      lineTo(u(28f), u(106f))
      quadraticTo(u(2f), u(106f), u(2f), u(80f))
      close()
    }
    drawPath(diagBg, color = Color(0xFFD9ECE9))

    // 1. Top-Left Tool: Open-End Wrench
    withTransform({
      rotate(degrees = -30f, pivot = Offset(u(26f), u(35f)))
    }) {
      drawRoundRect(
        color = Color(0xFF00696B),
        topLeft = Offset(u(23.5f), u(31f)),
        size = Size(u(5f), u(16f)),
        cornerRadius = CornerRadius(u(2.5f), u(2.5f))
      )
      drawCircle(
        color = Color(0xFF1E293B),
        radius = u(5.5f),
        center = Offset(u(26f), u(27f))
      )
      drawRect(
        color = Color(0xFFEAF4F3),
        topLeft = Offset(u(24f), u(20.5f)),
        size = Size(u(4f), u(6.8f))
      )
    }

    // 2. Top-Right Tool: Precision Screwdriver
    withTransform({
      rotate(degrees = 30f, pivot = Offset(u(82f), u(35f)))
    }) {
      drawRect(
        color = Color(0xFF475569),
        topLeft = Offset(u(80.7f), u(21f)),
        size = Size(u(2.6f), u(12f))
      )
      drawRect(
        color = Color(0xFFFBBC05),
        topLeft = Offset(u(79f), u(33f)),
        size = Size(u(6f), u(2.5f))
      )
      drawRoundRect(
        color = Color(0xFF00696B),
        topLeft = Offset(u(79f), u(35f)),
        size = Size(u(6f), u(12f)),
        cornerRadius = CornerRadius(u(3f), u(3f))
      )
    }

    // 3. Bottom-Left Tool: Tuning Gear / Cog
    val gearCenter = Offset(u(21.5f), u(58f))
    val toothStroke = Stroke(width = u(2.5f), cap = StrokeCap.Round)
    drawLine(Color(0xFF334155), Offset(u(21.5f), u(51.5f)), Offset(u(21.5f), u(64.5f)), strokeWidth = toothStroke.width, cap = StrokeCap.Round)
    drawLine(Color(0xFF334155), Offset(u(15f), u(58f)), Offset(u(28f), u(58f)), strokeWidth = toothStroke.width, cap = StrokeCap.Round)
    drawLine(Color(0xFF334155), Offset(u(16.9f), u(53.4f)), Offset(u(26.1f), u(62.6f)), strokeWidth = toothStroke.width, cap = StrokeCap.Round)
    drawLine(Color(0xFF334155), Offset(u(26.1f), u(53.4f)), Offset(u(16.9f), u(62.6f)), strokeWidth = toothStroke.width, cap = StrokeCap.Round)
    drawCircle(color = Color(0xFF334155), radius = u(4.6f), center = gearCenter)
    drawCircle(color = Color(0xFFD9ECE9), radius = u(2f), center = gearCenter)

    // 4. Bottom-Right Tool: Diagnostic Check Badge
    val badgeCenter = Offset(u(86.5f), u(58.5f))
    drawCircle(color = Color(0xFF00696B), radius = u(6.2f), center = badgeCenter)
    val checkPath = Path().apply {
      moveTo(u(83.6f), u(58.6f))
      lineTo(u(85.6f), u(60.7f))
      lineTo(u(89.6f), u(56.4f))
    }
    drawPath(
      path = checkPath,
      color = Color.White,
      style = Stroke(width = u(2.1f), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Googlebook Display Lid (Sleek Obsidian Bezel)
    drawRoundRect(
      color = Color(0xFF1E293B),
      topLeft = Offset(u(29.5f), u(33f)),
      size = Size(u(49f), u(36f)),
      cornerRadius = CornerRadius(u(5f), u(5f))
    )
    // Screen Inner Canvas
    drawRect(
      color = Color.White,
      topLeft = Offset(u(33f), u(36.4f)),
      size = Size(u(42f), u(29f))
    )

    // Googlebook 4-Color Top Accent Bar
    val barW = u(10.5f)
    val barH = u(2.3f)
    drawRect(Color(0xFF4285F4), Offset(u(33f), u(36.4f)), Size(barW, barH))
    drawRect(Color(0xFFEA4335), Offset(u(43.5f), u(36.4f)), Size(barW, barH))
    drawRect(Color(0xFFFBBC05), Offset(u(54f), u(36.4f)), Size(barW, barH))
    drawRect(Color(0xFF34A853), Offset(u(64.5f), u(36.4f)), Size(barW, barH))

    // Prominent Multicolor "G" on the Googlebook Screen
    val gTopLeft = Offset(u(45.5f), u(43.5f))
    val gSize = Size(u(17f), u(17f))
    val gStroke = Stroke(width = u(3.4f), cap = StrokeCap.Butt)
    drawArc(Color(0xFFEA4335), startAngle = -140f, sweepAngle = 98f, useCenter = false, topLeft = gTopLeft, size = gSize, style = gStroke)
    drawArc(Color(0xFFFBBC05), startAngle = 140f, sweepAngle = 80f, useCenter = false, topLeft = gTopLeft, size = gSize, style = gStroke)
    drawArc(Color(0xFF34A853), startAngle = 45f, sweepAngle = 95f, useCenter = false, topLeft = gTopLeft, size = gSize, style = gStroke)
    drawArc(Color(0xFF4285F4), startAngle = 0f, sweepAngle = 48f, useCenter = false, topLeft = gTopLeft, size = gSize, style = gStroke)
    drawLine(
      color = Color(0xFF4285F4),
      start = Offset(u(54.2f), u(52f)),
      end = Offset(u(62.4f), u(52f)),
      strokeWidth = u(3.2f),
      cap = StrokeCap.Round
    )

    // Googlebook Anodized Aluminum Base
    val basePath = Path().apply {
      moveTo(u(24f), u(68.5f))
      lineTo(u(84f), u(68.5f))
      lineTo(u(80.5f), u(74.5f))
      lineTo(u(27.5f), u(74.5f))
      close()
    }
    drawPath(basePath, color = Color(0xFF94A3B8))
    drawRoundRect(
      color = Color(0xFF475569),
      topLeft = Offset(u(48.5f), u(68.5f)),
      size = Size(u(11f), u(2.2f)),
      cornerRadius = CornerRadius(u(1.1f), u(1.1f))
    )
  }
}
