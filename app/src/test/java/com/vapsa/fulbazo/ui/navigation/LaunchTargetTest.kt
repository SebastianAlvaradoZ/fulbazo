package com.vapsa.fulbazo.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LaunchTargetTest {

    @Test
    fun eachScreenOpensItsRoute() {
        assertEquals(AppRoutes.INVITATIONS, LaunchTarget.routeFor(LaunchTarget.SCREEN_INVITATIONS, null))
        assertEquals(AppRoutes.createMatch(), LaunchTarget.routeFor(LaunchTarget.SCREEN_CREATE_MATCH, null))
        assertEquals("match_detail/abc-1_X", LaunchTarget.routeFor(LaunchTarget.SCREEN_MATCH, "abc-1_X"))
    }

    @Test
    fun unknownScreensAreIgnored() {
        assertNull(LaunchTarget.routeFor(null, null))
        assertNull(LaunchTarget.routeFor("edit_profile", null))
    }

    @Test
    fun badMatchIdsAreIgnored() {
        listOf(null, "", "a/b", "../x", "a?b=c", "a".repeat(129)).forEach {
            assertNull(LaunchTarget.routeFor(LaunchTarget.SCREEN_MATCH, it))
        }
    }
}
