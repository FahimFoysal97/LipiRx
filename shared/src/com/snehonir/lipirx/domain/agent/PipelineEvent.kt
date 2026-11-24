package com.snehonir.lipirx.domain.agent

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList

sealed interface PipelineEvent {
    data object Planning : PipelineEvent
    data class Dispatched(val agents: List<RxAgentType>, val reasoning: String) : PipelineEvent
    data class WorkerPercent(val agent: RxAgentType, val percent: Int) : PipelineEvent
    data class WorkerFinished(
        val agent: RxAgentType,
        val success: Boolean,
        val itemCount: Int = 0,
        val flaggedNames: List<String> = emptyList()
    ) : PipelineEvent
    data object Finalizing : PipelineEvent
    data object Suspended : PipelineEvent
}

enum class AgentStatus { WAITING, RUNNING, DONE, FLAGGED, FAILED }

enum class PipelinePhase { PLANNING, RUNNING, FINALIZING, WAITING_FOR_PHARMACIST }

data class WorkerProgress(
    val status: AgentStatus = AgentStatus.WAITING,
    val runs: Int = 0,
    /** Only set when the tool reports a real percent (OCR). Never estimated. */
    val percent: Int? = null,
    /** The last run finished without a tool error. */
    val completedOk: Boolean = false,
    val flaggedNames: List<String> = emptyList()
)

sealed interface FeedEntry {
    data object Preparing : FeedEntry
    data class Started(val agent: RxAgentType, val rerun: Boolean) : FeedEntry
    data class Finished(
        val agent: RxAgentType,
        val success: Boolean,
        val itemCount: Int,
        val flaggedNames: List<String>
    ) : FeedEntry
    data object Finalizing : FeedEntry
    data object WaitingForPharmacist : FeedEntry
}

data class PipelineProgress(
    val phase: PipelinePhase = PipelinePhase.PLANNING,
    val reasoning: String = "",
    val workers: PersistentMap<RxAgentType, WorkerProgress> = persistentMapOf(),
    val feed: PersistentList<FeedEntry> = persistentListOf(FeedEntry.Preparing),
    val overallPercent: Int = 0
) {
    fun worker(agent: RxAgentType): WorkerProgress = workers[agent] ?: WorkerProgress()

    val running: List<RxAgentType>
        get() = WORKERS.filter { worker(it).status == AgentStatus.RUNNING }

    val isParallel: Boolean get() = running.size > 1

    val completedChecks: Int get() = CHECKS.count { worker(it).completedOk }

    fun reduce(event: PipelineEvent): PipelineProgress = when (event) {
        PipelineEvent.Planning -> copy(phase = PipelinePhase.PLANNING)
        is PipelineEvent.Dispatched -> dispatch(event)
        is PipelineEvent.WorkerPercent -> {
            val cur = worker(event.agent)
            if (cur.status == AgentStatus.RUNNING) {
                copy(workers = workers.put(event.agent, cur.copy(percent = event.percent.coerceIn(0, 100))))
            } else this
        }
        is PipelineEvent.WorkerFinished -> finish(event)
        PipelineEvent.Finalizing -> copy(
            phase = PipelinePhase.FINALIZING,
            feed = feed.addBounded(FeedEntry.Finalizing)
        )
        PipelineEvent.Suspended -> copy(
            phase = PipelinePhase.WAITING_FOR_PHARMACIST,
            feed = feed.addBounded(FeedEntry.WaitingForPharmacist)
        )
    }.withOverall()

    private fun dispatch(e: PipelineEvent.Dispatched): PipelineProgress {
        val targets = e.agents.filter { it in WORKERS }
        if (targets.isEmpty()) return copy(phase = PipelinePhase.PLANNING, reasoning = e.reasoning)
        var w = workers
        var f = feed
        targets.forEach { agent ->
            val cur = worker(agent)
            w = w.put(
                agent,
                cur.copy(
                    status = AgentStatus.RUNNING,
                    runs = cur.runs + 1,
                    percent = null,
                    flaggedNames = emptyList()
                )
            )
            f = f.addBounded(FeedEntry.Started(agent, rerun = cur.runs > 0))
        }
        return copy(phase = PipelinePhase.RUNNING, reasoning = e.reasoning, workers = w, feed = f)
    }

    private fun finish(e: PipelineEvent.WorkerFinished): PipelineProgress {
        val flagged = e.success && e.agent == RxAgentType.SAFETY_AUDITOR && e.flaggedNames.isNotEmpty()
        val status = when {
            !e.success -> AgentStatus.FAILED
            flagged -> AgentStatus.FLAGGED
            else -> AgentStatus.DONE
        }
        val names = if (flagged) e.flaggedNames else emptyList()
        val w = workers.put(
            e.agent,
            worker(e.agent).copy(
                status = status,
                percent = null,
                completedOk = e.success,
                flaggedNames = names
            )
        )
        val othersRunning = WORKERS.any { it != e.agent && w[it]?.status == AgentStatus.RUNNING }
        return copy(
            phase = if (othersRunning) PipelinePhase.RUNNING else PipelinePhase.PLANNING,
            workers = w,
            feed = feed.addBounded(FeedEntry.Finished(e.agent, e.success, e.itemCount, names))
        )
    }

    /** Overall progress never moves backwards, so a rework pass can't shrink the bar. */
    private fun withOverall() = copy(overallPercent = maxOf(overallPercent, computeOverall()))

    private fun computeOverall(): Int {
        if (phase == PipelinePhase.FINALIZING) return 100
        val points = WORKERS.sumOf { agent ->
            val w = worker(agent)
            when {
                w.runs == 0 -> 0
                w.status != AgentStatus.RUNNING -> UNIT_POINTS
                w.runs > 1 -> UNIT_POINTS
                else -> UNIT_POINTS * (w.percent ?: ASSUMED_RUNNING_PERCENT) / 100
            }
        }
        return points.coerceAtMost(MAX_BEFORE_FINAL)
    }

    private fun PersistentList<FeedEntry>.addBounded(entry: FeedEntry): PersistentList<FeedEntry> =
        (this + entry).takeLast(MAX_FEED).toPersistentList()

    companion object {
        val WORKERS = listOf(
            RxAgentType.TRANSCRIPTION,
            RxAgentType.ENTITY_PARSER,
            RxAgentType.GENERIC_SUBSTITUTE,
            RxAgentType.SAFETY_AUDITOR
        )
        /** The three checks shown in the "Required checks" bar. */
        val CHECKS = listOf(
            RxAgentType.TRANSCRIPTION,
            RxAgentType.ENTITY_PARSER,
            RxAgentType.SAFETY_AUDITOR
        )
        private const val UNIT_POINTS = 25
        private const val ASSUMED_RUNNING_PERCENT = 30
        private const val MAX_BEFORE_FINAL = 99
        private const val MAX_FEED = 12
    }
}