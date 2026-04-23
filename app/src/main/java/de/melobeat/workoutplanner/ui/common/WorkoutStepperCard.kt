package de.melobeat.workoutplanner.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun StepperCard(
    label: String,
    value: String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
    isAmrap: Boolean = false,
    sideLabel: String? = null,
    onValueSubmit: ((String) -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Number,
) {
    val containerColor = MaterialTheme.colorScheme.surfaceVariant

    val valueColor = MaterialTheme.colorScheme.onBackground

    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    val displayLabel = when {
        isAmrap -> "AMRAP"
        sideLabel != null -> "$label ($sideLabel)"
        else -> label.uppercase()
    }

    var isEditing by remember { mutableStateOf(false) }
    var editValue by remember(value) { mutableStateOf(value) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                displayLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = labelColor
            )
            Spacer(Modifier.height(8.dp))
            if (isEditing) {
                BasicTextField(
                    value = editValue,
                    onValueChange = { editValue = it },
                    textStyle = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Black,
                        color = valueColor,
                        textAlign = TextAlign.Center
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = keyboardType,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (editValue.isNotBlank()) {
                                onValueSubmit?.invoke(editValue)
                            }
                            isEditing = false
                            focusManager.clearFocus()
                        }
                    ),
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused) {
                                if (editValue.isNotBlank()) {
                                    onValueSubmit?.invoke(editValue)
                                }
                                isEditing = false
                            }
                        }
                )
                LaunchedEffect(isEditing) {
                    if (isEditing) {
                        focusRequester.requestFocus()
                    }
                }
            } else {
                Text(
                    value,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = valueColor,
                    modifier = Modifier.clickable {
                        if (onValueSubmit != null) {
                            isEditing = true
                            editValue = value
                        }
                    }
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = onDecrement,
                    shape = CircleShape,
                    modifier = Modifier.height(40.dp).weight(1f)
                ) {
                    Text("−", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                FilledTonalButton(
                    onClick = onIncrement,
                    shape = CircleShape,
                    modifier = Modifier.height(40.dp).weight(1f)
                ) {
                    Text("+", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
