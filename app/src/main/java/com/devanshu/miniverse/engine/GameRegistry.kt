package com.devanshu.miniverse.engine

import com.devanshu.miniverse.games.connectfour.ConnectFourGame
import com.devanshu.miniverse.games.memory.MemoryGame
import com.devanshu.miniverse.games.reaction.ReactionGame
import com.devanshu.miniverse.games.tictactoe.TicTacToeGame

/**
 * Plug-in registry (roadmap 6.2). Adding a game = one entry here plus its
 * rules + screen package. Nothing else in the app changes.
 */
object GameRegistry {
    val games: List<MiniGame<*, *>> = listOf(
        TicTacToeGame,
        ConnectFourGame,
        MemoryGame,
        ReactionGame,
    )

    val definitions: List<GameDefinition> = games.map { it.definition }

    fun find(id: String): MiniGame<*, *>? = games.firstOrNull { it.definition.id == id }
}
