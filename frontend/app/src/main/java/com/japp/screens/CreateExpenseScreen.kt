package com.japp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.japp.api.RetrofitClient
import com.japp.api.responses.Currency
import com.japp.api.responses.SplitType
import com.japp.api.responses.group.GroupDto
import com.japp.api.responses.expense.CreateExpenseRequest
import com.japp.api.responses.expense.ExpenseDto
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateExpenseScreen(
    navController: NavController? = null
) {
    var groups by remember { mutableStateOf<List<GroupDto>>(emptyList()) }
    var selectedGroup by remember { mutableStateOf<GroupDto?>(null) }

    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }

    var isLoadingGroups by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val call = RetrofitClient.groupService.get_my_groups()

        call.enqueue(object : Callback<List<GroupDto>> {
            override fun onResponse(
                call: Call<List<GroupDto>>,
                response: Response<List<GroupDto>>
            ) {
                isLoadingGroups = false
                if (response.isSuccessful && response.body() != null) {
                    groups = response.body()!!
                } else {
                    errorMessage = "Could not load groups (${response.code()})"
                }
            }

            override fun onFailure(call: Call<List<GroupDto>>, t: Throwable) {
                isLoadingGroups = false
                errorMessage = "Network error: ${t.message}"
            }
        })
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
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


        if (isSubmitting) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    errorMessage = null

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

                    val request = CreateExpenseRequest(
                        groupId = group.id,
                        amount = amountValue,
                        description = description,
                        category = category.ifBlank { null },
                        currency = Currency.DKK,
                        splitType = SplitType.EQUAL,
                        splits = null
                    )

                    isSubmitting = true

                    val call = RetrofitClient.expenseService.create_expense(request)
                    call?.enqueue(object : Callback<ExpenseDto?> {
                        override fun onResponse(
                            call: Call<ExpenseDto?>,
                            response: Response<ExpenseDto?>
                        ) {
                            isSubmitting = false
                            if (response.isSuccessful && response.body() != null) {
                                navController?.navigateUp()
                            } else {
                                errorMessage = "Failed to create expense"
                            }
                        }

                        override fun onFailure(call: Call<ExpenseDto?>, t: Throwable) {
                            isSubmitting = false
                            errorMessage = "Network error: ${t.message}"
                        }
                    })
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

