package com.devanshu.miniverse.engine

enum class MatchPhase { CREATED, COUNTDOWN, RUNNING, PAUSED, COMPLETED, DRAW, ABANDONED }

/** Central transition table (roadmap 6.3). Everything else asks this object. */
object MatchStateMachine {
    private val allowed: Map<MatchPhase, Set<MatchPhase>> = mapOf(
        MatchPhase.CREATED to setOf(MatchPhase.COUNTDOWN, MatchPhase.ABANDONED),
        MatchPhase.COUNTDOWN to setOf(MatchPhase.RUNNING, MatchPhase.ABANDONED),
        MatchPhase.RUNNING to setOf(
            MatchPhase.PAUSED, MatchPhase.COMPLETED, MatchPhase.DRAW, MatchPhase.ABANDONED
        ),
        MatchPhase.PAUSED to setOf(MatchPhase.RUNNING, MatchPhase.ABANDONED),
        MatchPhase.COMPLETED to emptySet(),
        MatchPhase.DRAW to emptySet(),
        MatchPhase.ABANDONED to emptySet(),
    )

    fun canTransition(from: MatchPhase, to: MatchPhase): Boolean =
        allowed[from]?.contains(to) == true
}
