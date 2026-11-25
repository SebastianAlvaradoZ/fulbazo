package com.vapsa.fulbazo.widget

import com.vapsa.fulbazo.domain.model.Court
import com.vapsa.fulbazo.domain.model.InvitationStatus
import com.vapsa.fulbazo.domain.model.Match
import com.vapsa.fulbazo.domain.model.MatchInvitation
import com.vapsa.fulbazo.domain.model.MatchLevel
import com.vapsa.fulbazo.domain.model.MatchStatus
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class NextMatchStateTest {

    private val now = LocalDateTime.of(2026, 11, 25, 12, 0)
    private val court = Court("c1", "Losa Umacollo", "Yanahuara", 0.0, 0.0)

    private fun match(
        id: String,
        dateTime: LocalDateTime = now.plusDays(1),
        players: List<String> = listOf("yo", "otro"),
        status: MatchStatus = MatchStatus.OPEN,
        courtId: String = "c1",
        slots: Int = 10,
    ) = Match(id, courtId, "otro", dateTime, slots, MatchLevel.ANY, status, players)

    private fun invitation(status: InvitationStatus) =
        MatchInvitation("i-$status", "m", "otro", "yo", now, status)

    private fun build(vararg matches: Match, invitations: List<MatchInvitation> = emptyList()) =
        buildNextMatchState("yo", matches.toList(), listOf(court), invitations, now)

    @Test
    fun nextIsTheClosestOfMyMatches() {
        val state = build(
            match("lejano", now.plusDays(5)),
            match("cercano", now.plusHours(2)),
            match("ajeno", now.plusHours(1), players = listOf("otro")),
        )

        assertEquals(listOf("cercano", "lejano"), state.matches.map { it.id })
        assertEquals("cercano", state.next?.id)
        assertTrue(state.signedIn)
    }

    @Test
    fun pastCancelledAndFinishedMatchesAreLeftOut() {
        val state = build(
            match("pasado", now.minusMinutes(1)),
            match("empieza-ya", now),
            match("cancelado", status = MatchStatus.CANCELLED),
            match("terminado", status = MatchStatus.FINISHED),
            match("lleno", status = MatchStatus.FULL),
        )

        assertEquals(listOf("lleno"), state.matches.map { it.id })
    }

    @Test
    fun withoutMatchesThereIsNoNext() {
        assertNull(build().next)
    }

    @Test
    fun matchShowsCourtAndSpots() {
        val match = build(match("m")).next!!

        assertEquals("Losa Umacollo", match.courtName)
        assertEquals("Yanahuara", match.district)
        assertEquals(8, match.spotsLeft)
        assertEquals(0.2f, match.progress)
    }

    @Test
    fun unknownCourtShowsItsId() {
        assertEquals("otra", build(match("m", courtId = "otra")).next?.courtName)
    }

    @Test
    fun progressNeverLeavesItsRange() {
        assertEquals(0f, WidgetMatch("m", "c", "d", now, slots = 0, spotsLeft = 0).progress)
        assertEquals(1f, WidgetMatch("m", "c", "d", now, slots = 4, spotsLeft = 0).progress)
    }

    @Test
    fun onlyPendingInvitationsCount() {
        val state = build(
            invitations = listOf(
                invitation(InvitationStatus.PENDING),
                invitation(InvitationStatus.PENDING),
                invitation(InvitationStatus.ACCEPTED),
                invitation(InvitationStatus.DECLINED),
            ),
        )

        assertEquals(2, state.pendingInvitations)
    }

    @Test
    fun savesNoMoreThanTheMaximum() {
        val matches = (1..20).map { match("m$it", now.plusDays(it.toLong())) }

        assertEquals(NextMatchState.MAX_MATCHES, build(*matches.toTypedArray()).matches.size)
    }

    @Test
    fun oldDataDropsMatchesThatAlreadyStarted() {
        val state = build(match("primero", now.plusHours(1)), match("segundo", now.plusHours(3)))

        assertEquals("segundo", state.at(now.plusHours(2)).next?.id)
        assertNull(state.at(now.plusDays(1)).next)
    }

    @Test
    fun dayIsTodayTomorrowOrLater() {
        fun day(dateTime: LocalDateTime) =
            WidgetMatch("m", "c", "d", dateTime, 10, 5).dayFrom(now.toLocalDate())

        assertEquals(WidgetDay.TODAY, day(now.withHour(23)))
        assertEquals(WidgetDay.TOMORROW, day(now.plusDays(1).withHour(0)))
        assertEquals(WidgetDay.LATER, day(now.plusDays(2)))
    }

    @Test
    fun stateSurvivesBeingSaved() {
        val state = build(match("m"), invitations = listOf(invitation(InvitationStatus.PENDING)))
            .copy(updatedAt = now)

        val json = Json.encodeToString(NextMatchState.serializer(), state)

        assertEquals(state, Json.decodeFromString(NextMatchState.serializer(), json))
    }
}
