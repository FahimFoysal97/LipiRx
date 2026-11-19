package com.snehonir.lipirx.presentation.scan.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.snehonir.lipirx.domain.model.MedicineItem
import com.snehonir.lipirx.resources.Res
import com.snehonir.lipirx.resources.btn_cancel
import com.snehonir.lipirx.resources.btn_save_changes
import com.snehonir.lipirx.resources.label_dosage_schedule
import com.snehonir.lipirx.resources.label_duration
import com.snehonir.lipirx.resources.label_generic_name_field
import com.snehonir.lipirx.resources.label_instructions_field
import com.snehonir.lipirx.resources.label_medicine_name
import com.snehonir.lipirx.resources.label_needs_review
import com.snehonir.lipirx.resources.label_quantity
import com.snehonir.lipirx.resources.label_strength_unit
import com.snehonir.lipirx.resources.label_strength_value
import com.snehonir.lipirx.resources.label_unit_type
import com.snehonir.lipirx.resources.title_edit_medicine
import com.snehonir.lipirx.ui.theme.Lipi
import org.jetbrains.compose.resources.stringResource

@Composable
fun EditMedicineDialog(
    item: MedicineItem,
    onDismiss: () -> Unit,
    onSave: (MedicineItem) -> Unit
) {
    var medicineName by remember(item) { mutableStateOf(item.medicineName) }
    var genericName by remember(item) { mutableStateOf(item.genericName) }
    var strengthValue by remember(item) { mutableStateOf(item.strengthValue) }
    var strengthUnit by remember(item) { mutableStateOf(item.strengthUnit) }
    var schedule by remember(item) { mutableStateOf(item.schedule) }
    var duration by remember(item) { mutableStateOf(item.duration) }
    var unitsText by remember(item) { mutableStateOf(item.units.toString()) }
    var unitType by remember(item) { mutableStateOf(item.unitType) }
    var instructions by remember(item) { mutableStateOf(item.instructions) }
    var needsReview by remember(item) { mutableStateOf(item.needsReview) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(24.dp),
            color = Lipi.Surface,
            border = BorderStroke(1.dp, Lipi.CardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = Lipi.Primary
                        )
                        Text(
                            text = stringResource(Res.string.title_edit_medicine),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Lipi.Ink
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(Res.string.btn_cancel),
                            tint = Lipi.Muted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable fields
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Medicine Name
                    OutlinedTextField(
                        value = medicineName,
                        onValueChange = { medicineName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(Res.string.label_medicine_name)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Generic Name
                    OutlinedTextField(
                        value = genericName,
                        onValueChange = { genericName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(Res.string.label_generic_name_field)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Strength Value & Unit
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = strengthValue,
                            onValueChange = { strengthValue = it },
                            modifier = Modifier.weight(1f),
                            label = { Text(stringResource(Res.string.label_strength_value)) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = strengthUnit,
                            onValueChange = { strengthUnit = it },
                            modifier = Modifier.weight(1f),
                            label = { Text(stringResource(Res.string.label_strength_unit)) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Schedule & Duration
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = schedule,
                            onValueChange = { schedule = it },
                            modifier = Modifier.weight(1f),
                            label = { Text(stringResource(Res.string.label_dosage_schedule)) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = duration,
                            onValueChange = { duration = it },
                            modifier = Modifier.weight(1f),
                            label = { Text(stringResource(Res.string.label_duration)) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Quantity Units & Unit Type
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = unitsText,
                            onValueChange = { unitsText = it.filter { char -> char.isDigit() } },
                            modifier = Modifier.weight(1f),
                            label = { Text(stringResource(Res.string.label_quantity)) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = unitType,
                            onValueChange = { unitType = it },
                            modifier = Modifier.weight(1f),
                            label = { Text(stringResource(Res.string.label_unit_type)) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Instructions
                    OutlinedTextField(
                        value = instructions,
                        onValueChange = { instructions = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(Res.string.label_instructions_field)) },
                        minLines = 2,
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Needs Review Checkbox
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = needsReview,
                            onCheckedChange = { needsReview = it },
                            colors = CheckboxDefaults.colors(checkedColor = Lipi.Primary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(Res.string.label_needs_review),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Lipi.Ink
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SecondaryButton(
                        text = stringResource(Res.string.btn_cancel),
                        icon = Icons.Default.Close,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        height = 48.dp,
                        fontSize = 14
                    )

                    PrimaryButton(
                        text = stringResource(Res.string.btn_save_changes),
                        icon = Icons.Default.Check,
                        onClick = {
                            val parsedUnits = unitsText.toIntOrNull() ?: item.units
                            val updatedItem = item.copy(
                                medicineName = medicineName.trim(),
                                prescribedBrandFull = if (strengthValue.isNotBlank()) "${medicineName.trim()} ${strengthValue.trim()} ${strengthUnit.trim()}".trim() else medicineName.trim(),
                                genericName = genericName.trim(),
                                strengthValue = strengthValue.trim(),
                                strengthUnit = strengthUnit.trim(),
                                schedule = schedule.trim(),
                                duration = duration.trim(),
                                units = parsedUnits,
                                unitType = unitType.trim().ifBlank { "pcs" },
                                instructions = instructions.trim(),
                                needsReview = needsReview
                            )
                            onSave(updatedItem)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
