package com.ourhouse.missions.ui.screens.pairing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ourhouse.missions.ui.theme.*

@Composable
fun PairingScreen(viewModel: PairingViewModel, onPaired: () -> Unit) {
    val state by viewModel.state.collectAsState()
    var joinCode by remember { mutableStateOf("") }
    var myName by remember { mutableStateOf("") }
    val nameReady = myName.isNotBlank()

    LaunchedEffect(state.paired) { if (state.paired) onPaired() }

    Column(modifier = Modifier.fillMaxSize().background(Background)) {
        Box(
            modifier = Modifier.fillMaxWidth().height(220.dp).background(TextPrimary),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("המשימות שלנו", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Text("רשימת משימות משותפת לזוג", color = Color(0xFFA9A2C4), style = MaterialTheme.typography.bodyMedium)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .background(Background, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .padding(22.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("חברו בין המכשירים", style = MaterialTheme.typography.titleLarge)
            Text(
                "צרו קוד שיתוף ייחודי ושלחו אותו לבן/בת הזוג. אחרי החיבור כל משימה שתוסיפו תופיע אוטומטית אצל שניכם.",
                color = TextSecondary, style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = myName,
                onValueChange = { myName = it.take(20) },
                label = { Text("השם שלך") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            if (state.inviteCode == null) {
                Button(
                    onClick = { viewModel.createHousehold(myName.trim()) },
                    enabled = !state.isLoading && nameReady,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text("יצירת קוד הזמנה") }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().background(Surface, RoundedCornerShape(20.dp)).padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("קוד ההזמנה שלכם", color = TextMuted, style = MaterialTheme.typography.labelMedium)
                    Text(state.inviteCode!!, color = AccentPrimary, style = MaterialTheme.typography.titleLarge.copy(fontSize = 30.sp))
                    Text("שלחו את הקוד הזה לבן/בת הזוג כדי שיצטרפו", color = TextSecondary, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                }
                Button(
                    onClick = onPaired,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) { Text("המשך למשימות") }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Divider(modifier = Modifier.weight(1f), color = Border)
                Text("או", color = TextMuted, style = MaterialTheme.typography.labelMedium)
                Divider(modifier = Modifier.weight(1f), color = Border)
            }

            Text("יש לכם קוד הזמנה?", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = joinCode,
                onValueChange = { joinCode = it.filter { c -> c.isDigit() }.take(6) },
                placeholder = { Text("הזינו קוד בן 6 ספרות") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
            OutlinedButton(
                onClick = { viewModel.joinHousehold(joinCode, myName.trim()) },
                enabled = !state.isLoading && nameReady && joinCode.length == 6,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { Text("התחברות למשימות המשותפות", color = AccentPrimary) }

            state.errorMessage?.let {
                Text(it, color = UrgentText, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
