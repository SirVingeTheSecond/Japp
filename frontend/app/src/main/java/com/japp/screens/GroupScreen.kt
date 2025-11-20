package com.japp.screens


import android.graphics.Bitmap
import android.graphics.Paint
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.japp.api.RetrofitClient
import com.japp.api.responses.group.GroupDto
import com.japp.composables.GroupIcon
import com.google.zxing.BarcodeFormat
import com.japp.api.responses.expense.BalanceDto
import com.japp.api.responses.expense.ExpenseDto
import com.japp.api.responses.expense.GroupBalanceSummaryDto
import com.japp.api.responses.group.GroupMemberDto
import com.journeyapps.barcodescanner.BarcodeEncoder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.collections.List

var GROUP_ID = -1

@Composable
fun GroupScreen(navController: NavController? = null) {
    var qrOpen by remember { mutableStateOf(false) }
    var group by remember { mutableStateOf<GroupDto?>(null) }
    var qrCode by remember { mutableStateOf<Bitmap?>(null) }
    var group_members by remember { mutableStateOf<List<GroupMemberDto>>(emptyList()) }
    var group_expense by remember { mutableStateOf<List<ExpenseDto>>(emptyList()) }
    var group_balance by remember { mutableStateOf<GroupBalanceSummaryDto?>(null) }




    LaunchedEffect(
        GROUP_ID
    ) {
        if (GROUP_ID == -1) return@LaunchedEffect

        val call = RetrofitClient.groupService.get_group(GROUP_ID)

        call!!.enqueue(object : Callback<GroupDto?> {
            override fun onResponse(
                call: Call<GroupDto?>, response: Response<GroupDto?>
            ) {
                val body = response.body()
                Log.d("Tag", body.toString())

                if (body != null && response.isSuccessful) {
                    group = body
                } else {
                    GROUP_ID = -1
                    navController?.navigateUp()
                }
            }

            override fun onFailure(
                call: Call<GroupDto?>, t: Throwable
            ) {
                TODO("Not yet implemented")
            }
        })


    }
    LaunchedEffect(GROUP_ID) {
        val call = RetrofitClient.groupService.get_group_members(GROUP_ID)

        call?.enqueue(object : Callback<List<GroupMemberDto>?> {
            override fun onResponse(
                call: Call<List<GroupMemberDto>?>,
                response: Response<List<GroupMemberDto>?>
            ) {
                val body = response.body()
                Log.d("Tag", body.toString())

                if (body != null && response.isSuccessful) {
                    group_members = body
                }
            }

            override fun onFailure(
                call: Call<List<GroupMemberDto>?>,
                t: Throwable
            ) {
                Log.d("Tag", t.message ?: "Unknown error")
            }
        })
    }


    LaunchedEffect(GROUP_ID) {
        val call = RetrofitClient.expenseService.get_group_balances(GROUP_ID)

        call?.enqueue(object : Callback<GroupBalanceSummaryDto?> {
            override fun onResponse(
                call: Call<GroupBalanceSummaryDto?>,
                response: Response<GroupBalanceSummaryDto?>
            ) {
                val body = response.body()
                Log.d("Tag", body.toString())

                if (body != null && response.isSuccessful) {
                    group_balance = body
                }
            }

            override fun onFailure(
                call: Call<GroupBalanceSummaryDto?>,
                t: Throwable
            ) {
                Log.d("Tag", t.message ?: "Unknown error")
            }
        })
    }

    LaunchedEffect(GROUP_ID) {
        val call = RetrofitClient.expenseService.get_group_expenses(GROUP_ID)

        call?.enqueue(object : Callback<List<ExpenseDto>?> {
            override fun onResponse(
                call: Call<List<ExpenseDto>?>,
                response: Response<List<ExpenseDto>?>
            ) {
                val body = response.body()
                Log.d("Tag", body.toString())

                if (body != null && response.isSuccessful) {
                    group_expense = body
                }
            }

            override fun onFailure(
                call: Call<List<ExpenseDto>?>,
                t: Throwable
            ) {
                Log.d("Tag", t.message ?: "Unknown error")
            }
        })
    }




    LaunchedEffect(group) {
        if (group != null) {
            // lav qr code
            qrCode = BarcodeEncoder().encodeBitmap(
                "japp://join/${group!!.id}-${group!!.inviteCode}",
                BarcodeFormat.QR_CODE,
                200,
                200
            )
        }
    }

    Box(

    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box {
                if (group == null) {

                } else {
                    Row {
                        GroupIcon(
                            group!!.name
                        )
                        Column (
                            modifier = Modifier.padding(15.dp)
                        ){
                            Text(
                                group!!.name,
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Right,

                            )
                            HorizontalDivider(
                                thickness = 1.dp,
                                color = Color.LightGray,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            group!!.description?.let {
                                Text(
                                    it, textAlign = TextAlign.Right
                                )
                            }
                        }
                    }

                }
            }
            Button(
                onClick = { qrOpen = true },

                ) {
                Text("Show QR!")
            }
            NavTab(group_members, group_expense, group_balance)
        }
        if (qrOpen) {
            Dialog(onDismissRequest = { qrOpen = false }) {

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(375.dp)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (qrCode != null) {
                            Image(
                                bitmap = qrCode!!.asImageBitmap(),
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .fillMaxWidth(0.8f)
                                    .background(Color.Transparent),
                                contentScale = ContentScale.FillBounds,
                                contentDescription = "QR Code for joining group"
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            TextButton(
                                onClick = { qrOpen = false },
                                modifier = Modifier.padding(8.dp),
                            ) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavTab(
    groupMembers: List<GroupMemberDto>,
    groupExpenses: List<ExpenseDto>,
    groupBalance: GroupBalanceSummaryDto?
) {
    val navController = rememberNavController()


    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val selectedDestination = when (currentRoute) {
        "1" -> 0
        "2" -> 1
        "3" -> 2
        else -> 0
    }

    Column {
        SecondaryTabRow(
            selectedTabIndex = selectedDestination,
        ) {
            Tab(
                selected = selectedDestination == 0,
                onClick = { navController.navigate("1") },
                modifier = Modifier.padding(10.dp)
            ) {
                Text("Members")
            }
            Tab(
                selected = selectedDestination == 1,
                onClick = { navController.navigate("2") }
            ) {
                Text("Expenses")
            }
            Tab(
                selected = selectedDestination == 2,
                onClick = { navController.navigate("3") }
            ) {
                Text("Options")
            }
        }
        NavHost(
            navController = navController,
            startDestination = "1"
        ) {
            composable("1") {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    groupMembers.forEach { memberDto ->
                        Text(
                            memberDto.username,
                            Modifier.padding(15.dp),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,

                        )


                        val balance = groupBalance
                            ?.balances
                            ?.find { it.username == memberDto.username }
                            ?.balance ?: 0.0

                        val backgroundColor = if (balance < 0) Color.Red else Color.Green

                        Box(
                            modifier = Modifier
                                .padding(8.dp)
                                .background(
                                    color = backgroundColor,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Balance: $balance",
                                color = Color.White
                            )
                        }






                        HorizontalDivider(
                            thickness = 1.dp,
                            color = Color.LightGray,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }

            composable("2") {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.verticalScroll(
                        state = rememberScrollState(),
                        enabled = true,
                    )
                ) {
                    groupExpenses.forEach { ExpenseDto ->


                        Text("ID: "+ExpenseDto.id,
                            Modifier.padding(10.dp)
                        )
                        Text("Description: "+ExpenseDto.description,
                            Modifier.padding(10.dp)
                        )
                        Text("Amount: "+ExpenseDto.amount+" "+ExpenseDto.currency.symbol,
                            Modifier.padding(10.dp)
                        )
                        Text("Paid by: "+ExpenseDto.paidByName,
                            Modifier.padding(10.dp)
                        )
                        Text("Split type: "+ExpenseDto.splitType,
                            Modifier.padding(10.dp)
                        )
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = Color.LightGray,
                            modifier = Modifier.padding(vertical = 4.dp))
                    }

                }
            }

            composable("3") {
                var notificationsEnabled by remember { mutableStateOf(false) } // need to use endpoint for backend logic

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Text(
                            "Enable notifications",
                            modifier = Modifier.weight(1f)
                        )

                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { notificationsEnabled = it }
                        )
                    }
                    Button(
                            onClick = { /* TODO */ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .padding(top = 24.dp)
                    ) {
                    Text("DELETE GROUP")
                }
                }



            }

        }
    }
}

