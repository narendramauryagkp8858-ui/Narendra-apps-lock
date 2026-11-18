package com.example.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CardGlassNavy
import com.example.ui.theme.CyanShield
import com.example.ui.theme.ElevatedNavy
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SecureEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ElectricViolet
import kotlin.math.hypot

@Composable
fun PinPadView(
    pinLength: Int = 4,
    enteredPin: String,
    errorMessage: String?,
    onDigitClick: (String) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    Column(
        modifier = modifier.widthIn(max = 340.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // PIN Indicator Dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            repeat(pinLength) { index ->
                val filled = index < enteredPin.length
                val dotColor by animateColorAsState(
                    targetValue = when {
                        errorMessage != null -> AlertCrimson
                        filled -> CyanShield
                        else -> BorderSubtle
                    },
                    label = "pinDotColor"
                )
                val scale by animateFloatAsState(
                    targetValue = if (filled) 1.2f else 1f,
                    animationSpec = spring(),
                    label = "pinDotScale"
                )
                Box(
                    modifier = Modifier
                        .size((14 * scale).dp)
                        .clip(CircleShape)
                        .background(if (filled || errorMessage != null) dotColor else Color.Transparent)
                        .border(2.dp, dotColor, CircleShape)
                )
            }
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = AlertCrimson,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(24.dp))
        }

        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9")
        )

        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { digit ->
                    PinKeyButton(
                        label = digit,
                        testTag = "pin_key_$digit",
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            }
                            onDigitClick(digit)
                        }
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.size(72.dp))

            PinKeyButton(
                label = "0",
                testTag = "pin_key_0",
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    }
                    onDigitClick("0")
                }
            )

            Box(
                modifier = Modifier
                    .size(72.dp)
                    .minimumInteractiveComponentSize()
                    .clip(CircleShape)
                    .background(ElevatedNavy.copy(alpha = 0.45f))
                    .clickable { onBackspace() }
                    .testTag("pin_backspace"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Backspace,
                    contentDescription = "Delete last digit",
                    tint = TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun PinKeyButton(
    label: String,
    testTag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .minimumInteractiveComponentSize()
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(CardGlassNavy, ElevatedNavy.copy(alpha = 0.85f))
                )
            )
            .border(1.dp, BorderSubtle, CircleShape)
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}

@Composable
fun PatternLockCanvas(
    isError: Boolean = false,
    onPatternComplete: (List<Int>) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val selectedNodes = remember { mutableStateListOf<Int>() }
    var dragPosition by remember { mutableStateOf<Offset?>(null) }

    Box(
        modifier = modifier
            .size(280.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(24.dp))
            .background(MidnightNavy.copy(alpha = 0.65f))
            .border(1.dp, if (isError) AlertCrimson else BorderSubtle, RoundedCornerShape(24.dp))
            .testTag("pattern_lock_canvas")
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        selectedNodes.clear()
                        dragPosition = startOffset
                        findHitNode(startOffset, size.width.toFloat(), size.height.toFloat())?.let { node ->
                            selectedNodes.add(node)
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val pos = change.position
                        dragPosition = pos
                        val hit = findHitNode(pos, size.width.toFloat(), size.height.toFloat())
                        if (hit != null && !selectedNodes.contains(hit)) {
                            selectedNodes.add(hit)
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        }
                    },
                    onDragEnd = {
                        dragPosition = null
                        if (selectedNodes.isNotEmpty()) {
                            onPatternComplete(selectedNodes.toList())
                            selectedNodes.clear()
                        }
                    },
                    onDragCancel = {
                        dragPosition = null
                        selectedNodes.clear()
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centers = (0 until 9).map { idx -> nodeCenter(idx, size.width, size.height) }
            val lineColor = if (isError) AlertCrimson else CyanShield

            if (selectedNodes.isNotEmpty()) {
                val path = Path()
                val first = centers[selectedNodes.first()]
                path.moveTo(first.x, first.y)
                for (i in 1 until selectedNodes.size) {
                    val pt = centers[selectedNodes[i]]
                    path.lineTo(pt.x, pt.y)
                }
                dragPosition?.let { current ->
                    path.lineTo(current.x, current.y)
                }
                drawPath(
                    path = path,
                    brush = Brush.linearGradient(listOf(RoyalPurple, lineColor)),
                    style = Stroke(width = 10f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            centers.forEachIndexed { idx, center ->
                val isSelected = selectedNodes.contains(idx)
                val outerRadius = if (isSelected) 32f else 22f
                val innerColor = when {
                    isSelected && isError -> AlertCrimson
                    isSelected -> CyanShield
                    else -> TextSecondary.copy(alpha = 0.6f)
                }
                if (isSelected) {
                    drawCircle(
                        color = innerColor.copy(alpha = 0.22f),
                        radius = outerRadius + 12f,
                        center = center
                    )
                }
                drawCircle(
                    color = ElevatedNavy,
                    radius = outerRadius,
                    center = center
                )
                drawCircle(
                    color = if (isSelected) innerColor else BorderSubtle,
                    radius = outerRadius,
                    center = center,
                    style = Stroke(width = 3f)
                )
                drawCircle(
                    color = innerColor,
                    radius = if (isSelected) 11f else 7f,
                    center = center
                )
            }
        }
    }
}

private fun nodeCenter(index: Int, width: Float, height: Float): Offset {
    val col = index % 3
    val row = index / 3
    val cellW = width / 3f
    val cellH = height / 3f
    return Offset(cellW * (col + 0.5f), cellH * (row + 0.5f))
}

private fun findHitNode(point: Offset, width: Float, height: Float): Int? {
    val hitRadius = minOf(width, height) / 7.5f
    for (i in 0 until 9) {
        val c = nodeCenter(i, width, height)
        if (hypot(point.x - c.x, point.y - c.y) <= hitRadius) {
            return i
        }
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordRecoverySheet(
    recoveryEmail: String,
    securityQuestion: String,
    hasSecurityAnswer: Boolean,
    issuedRecoveryCode: String?,
    onSendEmailCode: () -> Unit,
    onVerifyCodeAndReset: (code: String, newPin: String, onDone: (Boolean) -> Unit) -> Unit,
    onVerifyAnswerAndReset: (answer: String, newPin: String, onDone: (Boolean) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var codeInput by remember { mutableStateOf("") }
    var answerInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MidnightNavy,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = ElectricViolet
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Account & PIN Recovery",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
            }

            Text(
                text = "Registered Email: $recoveryEmail",
                style = MaterialTheme.typography.bodyMedium,
                color = CyanShield
            )

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = ElevatedNavy,
                contentColor = TextPrimary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0; statusText = null },
                    text = { Text("Email Code") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1; statusText = null },
                    text = { Text("Security Question") }
                )
            }

            if (selectedTab == 0) {
                Text(
                    text = "Generate a 6-digit recovery code and dispatch it through your device email client to $recoveryEmail. All verification stays offline on your device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                OutlinedButton(
                    onClick = onSendEmailCode,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("send_recovery_email_button")
                ) {
                    Icon(Icons.Rounded.Email, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate & Email Recovery Code")
                }

                if (issuedRecoveryCode != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElevatedNavy)
                            .border(1.dp, RoyalPurple, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Offline Backup Code (valid 15m): $issuedRecoveryCode",
                            fontFamily = JetBrainsMonoFontFamily,
                            color = CyanShield,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { if (it.length <= 6) codeInput = it.filter(Char::isDigit) },
                    label = { Text("6-Digit Recovery Code") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("recovery_code_input")
                )
            } else {
                Text(
                    text = securityQuestion,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                if (!hasSecurityAnswer) {
                    Text(
                        text = "You haven't configured a custom security answer yet in Settings. Use Email Code recovery or default demo PIN 8546 if unchanged.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                OutlinedTextField(
                    value = answerInput,
                    onValueChange = { answerInput = it },
                    label = { Text("Your Security Answer") },
                    singleLine = true,
                    enabled = hasSecurityAnswer,
                    modifier = Modifier.fillMaxWidth().testTag("security_answer_input")
                )
            }

            OutlinedTextField(
                value = newPinInput,
                onValueChange = { if (it.length <= 4) newPinInput = it.filter(Char::isDigit) },
                label = { Text("New 4-Digit PIN") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("recovery_new_pin_input")
            )

            statusText?.let { msg ->
                Text(
                    text = msg,
                    color = if (isSuccess) SecureEmerald else AlertCrimson,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (newPinInput.length != 4) {
                            isSuccess = false
                            statusText = "Enter a 4-digit new PIN."
                            return@Button
                        }
                        if (selectedTab == 0) {
                            onVerifyCodeAndReset(codeInput, newPinInput) { ok ->
                                isSuccess = ok
                                statusText = if (ok) "PIN reset successful! You can now unlock." else "Invalid or expired recovery code."
                            }
                        } else {
                            onVerifyAnswerAndReset(answerInput, newPinInput) { ok ->
                                isSuccess = ok
                                statusText = if (ok) "PIN reset successful! You can now unlock." else "Security answer did not match."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalPurple),
                    modifier = Modifier.testTag("confirm_recovery_reset_button")
                ) {
                    Icon(Icons.Rounded.Key, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset PIN")
                }
            }
        }
    }
}
