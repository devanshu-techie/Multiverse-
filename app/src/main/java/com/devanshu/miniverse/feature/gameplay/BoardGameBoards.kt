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

private val boxColors = listOf(PlayerColors[0], PlayerColors[3])

@Composable
internal fun DotsBoxesBoard(state: DbState, vm: GameplayViewModel) {
    val current = state.players[state.turnIndex]
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        DuelHeader(
            players = state.players, turnIndex = state.turnIndex, colors = boxColors,
            badges = state.players.map { it.name.take(1).uppercase() },
            scores = state.players.map { "${state.scores[it.id] ?: 0} boxes" },
        )
        Text("${current.name}: tap a faint line between two dots", fontWeight = FontWeight.Black, fontSize = 14.sp,
            color = boxColors[state.turnIndex.coerceIn(0, 1)])
        Spacer(Modifier.height(12.dp))
        val dot = 16.dp; val thick = 14.dp
        Column(Modifier.padding(8.dp)) {
            for (r in 0..DB_SIZE) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    for (c in 0 until DB_SIZE) {
                        Box(Modifier.size(dot).clip(CircleShape).background(Color.White))
                        val i = hIdx(r, c)
                        val drawn = state.hEdges[i]
                        Box(
                            Modifier.width(58.dp).height(thick)
                                .padding(horizontal = 2.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (drawn) Color(0xFFFFC93C) else Color.White.copy(alpha = 0.16f))
                                .clickable(enabled = !drawn) { vm.submit(DrawEdge(true, i), current.id) },
                        )
                    }
                    Box(Modifier.size(dot).clip(CircleShape).background(Color.White))
                }
                if (r < DB_SIZE) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        for (c in 0..DB_SIZE) {
                            val i = vIdx(r, c)
                            val drawn = state.vEdges[i]
                            Box(
                                Modifier.width(thick).height(58.dp)
                                    .padding(vertical = 2.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (drawn) Color(0xFFFFC93C) else Color.White.copy(alpha = 0.16f))
                                    .clickable(enabled = !drawn) { vm.submit(DrawEdge(false, i), current.id) },
                            )
                            if (c < DB_SIZE) {
                                val owner = state.boxOwner[r * DB_SIZE + c]
                                val seat = state.players.indexOfFirst { it.id == owner }
                                Box(
                                    Modifier.size(58.dp).padding(3.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (owner == null) Color.Transparent
                                            else boxColors[seat.coerceIn(0, 1)].copy(alpha = 0.85f)
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (owner != null) {
                                        Text(state.players[seat].name.take(1).uppercase(), fontWeight = FontWeight.Black, fontSize = 22.sp, color = Color(0xFF1B1730))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Text("Close a box to score a point and go again!", fontSize = 13.sp, color = Color.White)
    }
}
