package com.devanshu.miniverse.feature.wallet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devanshu.miniverse.data.GameRepository
import com.devanshu.miniverse.ui.theme.MvMint
import com.devanshu.miniverse.ui.theme.MvCoral
import kotlinx.coroutines.flow.flowOf
import java.text.DateFormat
import java.util.Date

@Composable
fun WalletScreen(repository: GameRepository, playerId: String?, onBack: () -> Unit) {
    val wallet by (playerId?.let { repository.wallet(it) } ?: flowOf(null)).collectAsState(initial = null)
    val ledger by (playerId?.let { repository.ledger(it) } ?: flowOf(emptyList())).collectAsState(initial = emptyList())

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Back") }
            Text("Wallet", fontWeight = FontWeight.Black, fontSize = 20.sp)
        }
        Card(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🪙", fontSize = 36.sp)
                Text("${wallet?.balance ?: 0} MiniCoins", fontSize = 26.sp, fontWeight = FontWeight.Black)
                Text("Earned by playing. Never purchasable.", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text("History", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(6.dp))
        LazyColumn {
            items(ledger) { txn ->
                Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(txn.reason.replaceFirstChar { it.uppercase() }, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(
                            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(txn.createdAt)),
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        (if (txn.amount >= 0) "+" else "") + "${txn.amount} MC",
                        fontWeight = FontWeight.Black,
                        color = if (txn.amount >= 0) MvMint else MvCoral,
                    )
                }
            }
        }
    }
}
