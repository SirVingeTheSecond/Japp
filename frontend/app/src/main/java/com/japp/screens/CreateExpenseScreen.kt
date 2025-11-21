package com.japp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.japp.api.RetrofitClient
import com.japp.api.responses.Currency
import com.japp.api.responses.ExpenseCategory
import com.japp.api.responses.SplitType
import com.japp.api.responses.expense.CreateExpenseRequest
import com.japp.api.responses.expense.ExpenseDto
import com.japp.api.responses.expense.ExpenseSplitRequest
import com.japp.api.responses.group.GroupDto
import com.japp.api.responses.group.GroupMemberDto
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

enum class SplitInputMode {
    AMOUNT,
    PERCENTAGE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateExpenseScreen(
    navController: NavController? = null
) {
    val coroutineScope = rememberCoroutineScope()

    var groups by remember { mutableStateOf<List<GroupDto>>(emptyList()) }
    var selectedGroup by remember { mutableStateOf<GroupDto?>(null) }

    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }

    var isLoadingGroups by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var splitType by remember { mutableStateOf(SplitType.EQUAL) }

    var splitInputMode by remember { mutableStateOf(SplitInputMode.AMOUNT) }

    var groupMembers by remember { mutableStateOf<List<GroupMemberDto>>(emptyList()) }
    var memberShares by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }

    LaunchedEffect(Unit) {
        val res = RetrofitClient.groupService.getMyGroups()
        if (res.isSuccessful && res.body() != null) {
            groups = res.body()!!
            isLoadingGroups = false
        } else {
            errorMessage = "Could not load groups (${res.code()})"
        }
    }

    LaunchedEffect(selectedGroup?.id) {
        val group = selectedGroup ?: return@LaunchedEffect
        val res = RetrofitClient.groupService.getGroupMembers(group.id)
        if (res.isSuccessful && res.body() != null) {
            groupMembers = res.body()!!
            memberShares = groupMembers.associate { it.userId to "" }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start,

    ) {
        Text("Create Expense", style = MaterialTheme.typography.titleLarge)

        Spacer(Modifier.height(16.dp))

        if (isLoadingGroups) {
            Text("Loading groups...")
        } else if (groups.isEmpty()) {
            Text("You are not in any groups")
        } else {
            var expanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedGroup?.name ?: "Select group",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Group") },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    groups.forEach { group ->
                        DropdownMenuItem(
                            text = { Text(group.name) },
                            onClick = {
                                selectedGroup = group
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Text("Split type", style = MaterialTheme.typography.titleMedium)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = splitType == SplitType.EQUAL,
                    onClick = { splitType = SplitType.EQUAL }
                )
                Text("Equal")
            }

            Spacer(Modifier.width(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = splitType == SplitType.CUSTOM,
                    onClick = { splitType = SplitType.CUSTOM }
                )
                Text("Custom")
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Amount") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text("Category (optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        if (splitType == SplitType.CUSTOM) {
            if (groupMembers.isEmpty()) {
                Text("No members in group")
            } else {
                Text("Custom split mode", style = MaterialTheme.typography.titleMedium)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = splitInputMode == SplitInputMode.AMOUNT,
                            onClick = { splitInputMode = SplitInputMode.AMOUNT }
                        )
                        Text("Amount")
                    }

                    Spacer(Modifier.width(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = splitInputMode == SplitInputMode.PERCENTAGE,
                            onClick = { splitInputMode = SplitInputMode.PERCENTAGE }
                        )
                        Text("Percentage")
                    }
                }

                groupMembers.forEach { member ->
                    val currentValue = memberShares[member.userId] ?: ""

                    OutlinedTextField(
                        value = currentValue,
                        onValueChange = { newValue ->
                            memberShares = memberShares.toMutableMap().apply {
                                put(member.userId, newValue)
                            }
                        },
                        label = {
                            Text(
                                "${member.username} " +
                                        if (splitInputMode == SplitInputMode.AMOUNT) "(amount)" else "(%)"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        singleLine = true
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (isSubmitting) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    val group = selectedGroup
                    if (group == null) {
                        errorMessage = "Please select a group"
                        return@Button
                    }

                    val amountValue = amount.toDoubleOrNull()
                    if (amountValue == null) {
                        errorMessage = "Amount must be a number"
                        return@Button
                    }

                    if (splitType == SplitType.CUSTOM && splitInputMode == SplitInputMode.PERCENTAGE) {
                        var totalPercentage = 0.0

                        for (member in groupMembers) {
                            val raw = memberShares[member.userId].orEmpty().trim()
                            if (raw.isEmpty()) continue

                            val value = raw.toDoubleOrNull()
                            if (value == null) {
                                errorMessage = "Invalid percentage for ${member.username}"
                                return@Button
                            }

                            totalPercentage += value
                        }

                        if (kotlin.math.abs(totalPercentage - 100.0) > 0.01) {
                            errorMessage = "Percentages must add up to 100 (currently ${"%.2f".format(totalPercentage)})"
                            return@Button
                        }
                    }

                    val splitsForRequest: List<ExpenseSplitRequest>? =
                        if (splitType == SplitType.CUSTOM) {
                            groupMembers.mapNotNull { member ->
                                val raw = memberShares[member.userId].orEmpty().trim()
                                if (raw.isEmpty()) return@mapNotNull null

                                val value = raw.toDoubleOrNull() ?: return@mapNotNull null

                                when (splitInputMode) {
                                    SplitInputMode.AMOUNT -> ExpenseSplitRequest(
                                        userId = member.userId,
                                        shareAmount = value,
                                        sharePercentage = null
                                    )
                                    SplitInputMode.PERCENTAGE -> ExpenseSplitRequest(
                                        userId = member.userId,
                                        shareAmount = null,
                                        sharePercentage = value
                                    )
                                }
                            }
                        } else {
                            null
                        }

                    val request = CreateExpenseRequest(
                        groupId = group.id,
                        amount = amountValue,
                        description = description,
                        category = ExpenseCategory.fromString(category),
                        currency = Currency.DKK,
                        splitType = splitType,
                        splits = splitsForRequest
                    )

                    isSubmitting = true
                    coroutineScope.launch {
                        val res = RetrofitClient.expenseService.createExpense(request)
                        isSubmitting = false
                        if (res.isSuccessful && res.body() != null) {
                            navController?.navigateUp()
                        } else {
                            errorMessage = "Failed to create expense"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Expense")
            }

        }

        Spacer(Modifier.height(8.dp))

        errorMessage?.let {
            Text(it, color = Color.Red)
        }
    }
}
