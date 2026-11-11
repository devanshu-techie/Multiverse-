package com.devanshu.miniverse.feature.gameplay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devanshu.miniverse.games.dotsboxes.DB_SIZE
import com.devanshu.miniverse.games.dotsboxes.DbState
import com.devanshu.miniverse.games.dotsboxes.DrawEdge
import com.devanshu.miniverse.games.dotsboxes.hIdx
import com.devanshu.miniverse.games.dotsboxes.vIdx
import com.devanshu.miniverse.ui.theme.MvDark
import com.devanshu.miniverse.ui.theme.SeatColors

// ---------- Dots & Boxes ----------

@Composable
internal fun DotsBoxesBoard(state: DbState, vm: GameplayViewModel) {
    val current = state.players[state.turnIndex]
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${current.name}'s line", fontWeight = FontWeight.Black, fontSize = 16.sp,
            color = SeatColors[state.turnIndex], modifier = Modifier.padding(vertical = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            state.players.forEachIndexed { i, p ->
                Text("${p.name}: ${state.scores[p.id] ?: 0}", color = SeatColors[i], fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(12.dp))
        val dot = 10.dp; val thick = 10.dp
        Column(Modifier.padding(8.dp)) {
            for (r in 0..DB_SIZE) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    for (c in 0 until DB_SIZE) {
                        Box(Modifier.size(dot).clip(CircleShape).background(MvDark))
                        val i = hIdx(r, c)
                        val drawn = state.hEdges[i]
                        Box(
                            Modifier.width(44.dp).height(thick)
                                .padding(horizontal = 2.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (drawn) MvDark else MvDark.copy(alpha = 0.12f))
                                .clickable(enabled = !drawn) { vm.submit(DrawEdge(true, i), current.id) },
                        )
                    }
                    Box(Modifier.size(dot).clip(CircleShape).background(MvDark))
                }
                if (r < DB_SIZE) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        for (c in 0..DB_SIZE) {
                            val i = vIdx(r, c)
                            val drawn = state.vEdges[i]
                            Box(
                                Modifier.width(thick).height(44.dp)
                                    .padding(vertical = 2.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (drawn) MvDark else MvDark.copy(alpha = 0.12f))
                                    .clickable(enabled = !drawn) { vm.submit(DrawEdge(false, i), current.id) },
                            )
                            if (c < DB_SIZE) {
                                val owner = state.boxOwner[r * DB_SIZE + c]
                                val seat = state.players.indexOfFirst { it.id == owner }
                                Box(
                                    Modifier.size(44.dp).padding(3.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (owner == null) Color.Transparent
                                            else SeatColors[seat.coerceAtLeast(0)].copy(alpha = 0.45f)
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (owner != null) {
                                        Text(state.players[seat].name.take(1), fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Text("Close a box to score and go again!", fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
