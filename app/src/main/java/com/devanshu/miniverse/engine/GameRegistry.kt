package com.devanshu.miniverse.engine

import com.devanshu.miniverse.games.airhockey.AirHockeyGame
import com.devanshu.miniverse.games.arcade.BalloonGame
import com.devanshu.miniverse.games.arcade.PinballGame
import com.devanshu.miniverse.games.arcade.RunnerGame
import com.devanshu.miniverse.games.connectfour.ConnectFourGame
import com.devanshu.miniverse.games.dotsboxes.DotsBoxesGame
import com.devanshu.miniverse.games.football.FootballGame
import com.devanshu.miniverse.games.ludo.LudoGame
import com.devanshu.miniverse.games.maze.MazeGame
import com.devanshu.miniverse.games.memory.MemoryGame
import com.devanshu.miniverse.games.merge.MergeGame
import com.devanshu.miniverse.games.quickmath.QuickMathGame
import com.devanshu.miniverse.games.reaction.ReactionGame
import com.devanshu.miniverse.games.rps.RpsGame
import com.devanshu.miniverse.games.scramble.ScrambleGame
import com.devanshu.miniverse.games.simon.SimonGame
import com.devanshu.miniverse.games.snakes.SnakesGame
import com.devanshu.miniverse.games.sumo.SumoGame
import com.devanshu.miniverse.games.tank.TankGame
import com.devanshu.miniverse.games.tictactoe.TicTacToeGame
import com.devanshu.miniverse.games.whack.WhackGame

/**
 * Plug-in registry (roadmap 6.2). Adding a game = one entry here plus its
 * rules + screen package. Nothing else in the app changes.
 */
object GameRegistry {
    val games: List<MiniGame<*, *>> = listOf(
        // Board & strategy
        TicTacToeGame,
        ConnectFourGame,
        SnakesGame,
        LudoGame,
        DotsBoxesGame,
        // Party & quick battles
        RpsGame,
        ReactionGame,
        FootballGame,
        SumoGame,
        // Arcade & action
        AirHockeyGame,
        TankGame,
        WhackGame,
        PinballGame,
        RunnerGame,
        BalloonGame,
        // Puzzle & brain
        MemoryGame,
        QuickMathGame,
        MergeGame,
        ScrambleGame,
        SimonGame,
        MazeGame,
    )

    val definitions: List<GameDefinition> = games.map { it.definition }

    fun find(id: String): MiniGame<*, *>? = games.firstOrNull { it.definition.id == id }
}
