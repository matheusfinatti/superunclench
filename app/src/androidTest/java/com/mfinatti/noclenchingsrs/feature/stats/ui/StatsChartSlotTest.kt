package com.mfinatti.noclenchingsrs.feature.stats.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mfinatti.noclenchingsrs.feature.stats.domain.SampleHistory
import com.mfinatti.noclenchingsrs.feature.stats.domain.StatsCalculator
import com.mfinatti.noclenchingsrs.feature.stats.domain.StatsFrame
import com.mfinatti.noclenchingsrs.ui.theme.SuperUnclenchTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.TimeZone
import kotlin.math.abs

/**
 * US-08 B1: per-bar accessibility nodes and taps must line up with the drawn bars in every frame.
 *
 * The bounds a screen reader (and uiautomator) sees are not the layout bounds: Android builds them
 * from each node's *touch* bounds (grown to the 48dp minimum for clickable nodes) minus whatever later
 * siblings cover. So this test checks three things: the Compose touch bounds, the real
 * [AccessibilityNodeInfo] bounds from UiAutomation, and actual taps / accessibility clicks.
 */
@OptIn(ExperimentalComposeUiApi::class)
@RunWith(AndroidJUnit4::class)
class StatsChartSlotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val tz = TimeZone.getTimeZone("UTC")
    private val now = 1_790_798_400_000L // 2026-09-30 18:00 UTC
    private val events = SampleHistory.generate(now, tz)

    private fun bounds(node: SemanticsNodeInteraction) = node.fetchSemanticsNode().boundsInRoot
    private fun touchBounds(node: SemanticsNodeInteraction) = node.fetchSemanticsNode().touchBoundsInRoot

    /** The node TalkBack/uiautomator would get for [tag] (resource id = test tag). */
    private fun a11yBounds(tag: String): Rect {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        // Same flags uiautomator uses for a dump: resource ids + nodes not marked important.
        automation.serviceInfo = automation.serviceInfo.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }
        var found: AccessibilityNodeInfo? = null
        repeat(20) {
            found = automation.rootInActiveWindow?.let { findByViewId(it, tag) }
            if (found != null) return@repeat
            Thread.sleep(100)
        }
        val node = found
        assertNotNull("no accessibility node for $tag", node)
        return Rect().also { node!!.getBoundsInScreen(it) }
    }

    /** Walks the tree like a uiautomator dump does (Compose nodes are virtual views). */
    private fun findByViewId(root: AccessibilityNodeInfo, id: String): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>().apply { add(root) }
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val name = node.viewIdResourceName
            if (name == id || name?.endsWith(":id/$id") == true) return node
            for (i in 0 until node.childCount) node.getChild(i)?.let(queue::add)
        }
        return null
    }

    @Test
    fun slotNodesAndTapsMatchDrawnBars() {
        var frame by mutableStateOf(StatsFrame.TODAY)
        var selected by mutableStateOf<Int?>(null)
        composeRule.setContent {
            SuperUnclenchTheme(darkTheme = false) {
                Box(Modifier.semantics { testTagsAsResourceId = true }) {
                    val stats = StatsCalculator.compute(events, frame, now, tz)
                    StatsChartCard(
                        stats = stats,
                        labels = ChartLabels(
                            axis = stats.buckets.map { null },
                            boldAxisIndex = null,
                            bucket = stats.buckets.indices.map { "b$it" },
                        ),
                        selected = selected,
                        onSelect = { selected = it },
                        summaryDescription = "chart",
                    )
                }
            }
        }
        for (f in StatsFrame.entries) {
            frame = f
            selected = null
            composeRule.waitForIdle()
            val plot = bounds(composeRule.onNodeWithTag(ChartTestTags.PLOT))
            val n = f.bucketCount
            val slot = plot.width / n

            // 1. Layout and touch bounds of each node = its drawn slot.
            for (i in 0 until n) {
                val node = composeRule.onNodeWithTag(ChartTestTags.bar(i))
                val expectedLeft = plot.left + slot * i
                val layout = bounds(node)
                assertTrue("$f bar $i left ${layout.left} vs $expectedLeft", abs(layout.left - expectedLeft) <= 1.5f)
                assertTrue("$f bar $i width ${layout.width} vs $slot", abs(layout.width - slot) <= 1.5f)
                val touch = touchBounds(node)
                assertTrue("$f bar $i touch left ${touch.left} vs $expectedLeft", abs(touch.left - expectedLeft) <= 1.5f)
                assertTrue("$f bar $i touch width ${touch.width} vs $slot", abs(touch.width - slot) <= 1.5f)
            }

            // 2. What the screen reader sees (AccessibilityNodeInfo, screen px) = the drawn slot.
            val plotA11y = a11yBounds(ChartTestTags.PLOT)
            val a11ySlot = plotA11y.width().toFloat() / n
            for (i in 0 until n) {
                val r = a11yBounds(ChartTestTags.bar(i))
                val expectedLeft = plotA11y.left + a11ySlot * i
                assertTrue(
                    "$f a11y bar $i [${r.left},${r.right}] vs [$expectedLeft,${expectedLeft + a11ySlot}]",
                    abs(r.left - expectedLeft) <= 2f && abs(r.width() - a11ySlot) <= 2f,
                )
            }

            // 3. A tap at a drawn bar's centre selects that bar.
            listOf(0, n / 2, n - 1).forEach { i ->
                selected = null
                composeRule.waitForIdle()
                composeRule.onNodeWithTag(ChartTestTags.PLOT).performTouchInput {
                    click(Offset(slot * i + slot / 2, height / 2f))
                }
                composeRule.waitForIdle()
                assertEquals("$f tap $i", i, selected)
            }

            // 4. The accessibility click action (TalkBack double-tap) on a node selects that bar.
            selected = null
            composeRule.waitForIdle()
            composeRule.onNodeWithTag(ChartTestTags.bar(n - 2)).performSemanticsAction(SemanticsActions.OnClick)
            composeRule.waitForIdle()
            assertEquals("$f a11y click", n - 2, selected)
        }
    }
}
