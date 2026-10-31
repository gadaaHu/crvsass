package com.qcafr.gatherapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qcafr.gatherapp.ui.theme.*

@Composable
fun ScenarioInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "የእርስዎን ምላሽ እዚህ ይጻፉ (2-4 ዓረፍተ ነገር)...",
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.appColors
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    color = colors.textDim,
                    fontSize = 13.sp
                )
            },
            // Multiline free text area
            minLines = 5,
            maxLines = 12,
            shape = RoundedCornerShape(12.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = colors.textPrimary,
                lineHeight = 24.sp
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
                capitalization = KeyboardCapitalization.Sentences
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryIndigo,
                unfocusedBorderColor = colors.borderSubtle,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary,
                cursorColor = PrimaryIndigo,
                focusedContainerColor = colors.bgCard,
                unfocusedContainerColor = colors.bgCard
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "${value.length} ፊደላት (characters)",
            color = colors.textDim,
            fontSize = 11.sp,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}
