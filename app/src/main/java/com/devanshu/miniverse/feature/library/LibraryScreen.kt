package com.devanshu.miniverse.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devanshu.miniverse.engine.GameRegistry
import com.devanshu.miniverse.ui.theme.CardPastels
import com.devanshu.miniverse.ui.theme.MvDark
import com.devanshu.miniverse.ui.theme.MvPurple
import com.devanshu.miniverse.ui.theme.categoryColor

@Composable
fun LibraryScreen(onPickGame: (String) -> Unit) {
    val categories = listOf("All") + GameRegistry.definitions.map { it.category }.distinct()
    var selected by remember { mutableStateOf("All") }
    val shown = GameRegistry.definitions.filter { selected == "All" || it.category == selected }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text("Game Library 🎮", fontWeight = FontWeight.Black, fontSize = 22.sp,
            modifier = Modifier.padding(top = 14.dp, bottom = 10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.forEach { cat ->
                val active = cat == selected
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (active) (if (cat == "All") MvPurple else categoryColor(cat)) else Color.White,
                    modifier = Modifier.clickable { selected = cat },
                ) {
                    Text(cat, fontSize = 12.sp, fontWeight = FontWeight.Black,
                        color = if (active) Color.White else MvDark,
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp))
                }
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(shown) { def ->
                val idx = GameRegistry.definitions.indexOfFirst { it.id == def.id }
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = CardPastels[idx % CardPastels.size],
                    modifier = Modifier.fillMaxWidth().clickable { onPickGame(def.id) },
                ) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.size(56.dp).clip(CircleShape).background(Color.White),
                            contentAlignment = Alignment.Center,
                        ) { Text(def.emoji, fontSize = 30.sp) }
                        Text(def.name, fontWeight = FontWeight.Black, fontSize = 14.sp, color = MvDark,
                            modifier = Modifier.padding(top = 8.dp))
                        Surface(shape = RoundedCornerShape(8.dp), color = categoryColor(def.category)) {
                            Text("${def.category} · ${def.minPlayers}-${def.maxPlayers}P",
                                fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                        }
                    }
                }
            }
        }
    }
}
