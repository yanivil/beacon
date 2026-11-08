package dev.beacon.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.beacon.R
import dev.beacon.alert.AlertKind
import dev.beacon.alert.AlertState
import dev.beacon.alert.MessageSource
import dev.beacon.settings.BeaconSettings
import dev.beacon.settings.Pause
import dev.beacon.ui.home.HomeActions
import dev.beacon.ui.home.HomeScreen
import dev.beacon.ui.settings.AlertSettingsScreen
import dev.beacon.ui.settings.SettingsActions
import dev.beacon.ui.testalert.TestAlertOverlay
import dev.beacon.ui.testalert.testColour
import dev.beacon.ui.theme.BeaconColors

/*
 * Why one screen with two tabs instead of a navigation library: Beacon has
 * exactly two flat destinations (project rule), so the current tab is one
 * saved value. Saving it (and the app whose colour is being edited) with
 * rememberSaveable keeps the user where they were after rotation or a
 * font-size change.
 *
 * Why the shell measures before it places: whether the Test alert button is
 * fixed depends on how tall the tab bar and the button really are at the
 * current font size and window (spec section 3, row 9). A SubcomposeLayout
 * measures both first, then composes the content with or without the button
 * at its end.
 *
 * Why each tab gets its own scroll state under key(tab): leaving a tab
 * throws its scroll position away, so each tab opens at its top, as the
 * design does.
 *
 * Why the Test alert overlay is drawn from the controller's state: the
 * controller owns the running test, so after rotation the rebuilt screen
 * finds it still running and shows the overlay again, mid-countdown. While
 * it shows, the shell underneath is hidden from TalkBack (cleared semantics),
 * so a TalkBack user can't wander onto buttons they can't see.
 */

/** The test tag of the scrolling content. */
internal const val SCROLL_CONTENT_TAG = "scroll_content"

/** The parts of the shell's SubcomposeLayout. */
private enum class ShellSlot {
    /** The tab bar, always fixed at the bottom. */
    TAB_BAR,

    /** A measure-only copy of the Test alert button's strip. */
    BUTTON_PROBE,

    /** The Test alert button's strip, fixed above the tabs. */
    FIXED_BUTTON,

    /** The scrolling content of the current tab. */
    CONTENT,
}

/**
 * Beacon's whole screen: the current tab's content, the Test alert button and
 * the tab bar, with the Test alert overlay above them while a test runs. It
 * stays blank, in the page colour, until the settings have loaded. Back on
 * Alert settings goes to Home; Back on Home leaves the app; Back during a
 * test stops the test.
 *
 * @param viewModel the source of the settings and the alert state, and the target of changes.
 */
@Composable
fun BeaconAppScreen(viewModel: MainViewModel) {
    val loaded by viewModel.settings.collectAsStateWithLifecycle()
    val settings = loaded
    val alertState by viewModel.alertState.collectAsStateWithLifecycle()
    // Only a test shows here; a real alert (a later PR) has its own screen.
    val runningTest = (alertState as? AlertState.Running)?.takeIf { it.request.kind == AlertKind.TEST }
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    // The app whose colour grid is open on Alert settings; kept here so it survives rotation.
    var editingApp by rememberSaveable { mutableStateOf<MessageSource?>(null) }

    // Only enabled on Alert settings, so on Home the system Back leaves the app.
    BackHandler(enabled = tab == Tab.SETTINGS) { tab = Tab.HOME }
    // Registered after the tab's handler, so it takes priority: during a test Back only stops the test.
    BackHandler(enabled = runningTest != null) { viewModel.stopTest() }

    if (settings == null) {
        // Nothing to show yet: defaults could flash a wrong state, such as ON while paused.
        Box(Modifier.fillMaxSize().background(BeaconColors.Page))
    } else {
        Box(Modifier.fillMaxSize()) {
            ShellLayout(
                tabBar = { TabBar(tab, onSelect = { tab = it }) },
                onTest = { viewModel.startTest(editingApp) },
                // Hidden from TalkBack while the overlay covers it, so only the overlay can be reached.
                modifier = if (runningTest != null) Modifier.clearAndSetSemantics {} else Modifier,
            ) { trailing ->
                key(tab) { TabContent(tab, settings, viewModel, editingApp, { editingApp = it }, onOpenSettings = { tab = Tab.SETTINGS }, trailing) }
            }
            if (runningTest != null) {
                RunningTest(runningTest, settings, editingApp, viewModel.hasLight, onStop = viewModel::stopTest)
            }
        }
    }
}

/**
 * The Test alert overlay with its labels worked out from the running test
 * and the settings.
 *
 * @param running the running test.
 * @param settings the loaded settings, for how long real alerts last.
 * @param editingApp the app whose colour grid is open, which names the
 *   colour in per-app mode, or null.
 * @param hasLight true when the phone has a camera light.
 * @param onStop called by Stop.
 */
@Composable
private fun RunningTest(
    running: AlertState.Running,
    settings: BeaconSettings,
    editingApp: MessageSource?,
    hasLight: Boolean,
    onStop: () -> Unit,
) {
    // The name comes from the colour actually flashing; the app is the one the test stands for.
    val colour = colourName(running.request.colour)
    val app = testColour(settings, editingApp).second
    TestAlertOverlay(
        running = running,
        colourLabel = if (app == null) colour else stringResource(R.string.test_colour_per_app, colour, stringResource(app.nameRes)),
        speedLabel = speedSummary(running.request.speed),
        lengthLabel = durationSummary(settings),
        hasLight = hasLight,
        onStop = onStop,
    )
}

/**
 * One tab's scrolling content, below the status bar and clear of side
 * system bars and cutouts.
 *
 * Alert settings shows the three setting sections.
 *
 * @param tab the tab to show.
 * @param settings the loaded settings.
 * @param viewModel the target of Home's and Alert settings' actions.
 * @param editingApp the app whose colour grid is open on Alert settings, or null.
 * @param setEditingApp opens or closes an app's colour grid.
 * @param onOpenSettings called by Home's "Change settings" button.
 * @param trailing the Test alert button's strip when it scrolls with the
 *   content, or null when it is fixed above the tabs.
 */
@Composable
private fun TabContent(
    tab: Tab,
    settings: BeaconSettings,
    viewModel: MainViewModel,
    editingApp: MessageSource?,
    setEditingApp: (MessageSource?) -> Unit,
    onOpenSettings: () -> Unit,
    trailing: (@Composable () -> Unit)?,
) {
    Column(
        Modifier
            .fillMaxSize()
            // Outside the scroll, so text never scrolls up behind the status bar, and
            // clear of a side navigation bar or camera cutout in landscape. The
            // Test alert strip inside then finds these insets already consumed.
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .testTag(SCROLL_CONTENT_TAG)
            .verticalScroll(rememberScrollState()),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(if (tab == Tab.HOME) 32.dp else 40.dp),
        ) {
            when (tab) {
                Tab.HOME -> HomeScreen(
                    settings,
                    HomeActions(
                        pauseAll = { viewModel.setPause(Pause.Indefinite) },
                        resume = { viewModel.setPause(Pause.None) },
                        // Read the current value from the settings shown, so a toggle always flips what the user sees.
                        toggleApp = { source ->
                            viewModel.setAppEnabled(source, !(settings.apps[source]?.enabled ?: source.defaultEnabled))
                        },
                        openSettings = onOpenSettings,
                    ),
                )
                Tab.SETTINGS -> AlertSettingsScreen(
                    settings,
                    editingApp,
                    SettingsActions(
                        setDuration = viewModel::setDuration,
                        stepCustom = viewModel::stepCustomLength,
                        setSpeed = viewModel::setSpeed,
                        setColourMode = viewModel::setColourMode,
                        setGlobalColour = viewModel::setGlobalColour,
                        setAppColour = viewModel::setAppColour,
                        setEditingApp = setEditingApp,
                    ),
                )
            }
        }
        trailing?.invoke()
    }
}

/**
 * Lays out the content, the Test alert button's strip and the tab bar, and
 * decides with [testButtonFixed] whether the strip is fixed or scrolls.
 *
 * The tab bar and the strip are measured at the window's width with no
 * height limit, so their real heights are known even at the largest font.
 * When the strip is fixed: content, then strip, then tabs. Otherwise:
 * content (with the strip as its last item), then tabs.
 *
 * @param tabBar the tab bar.
 * @param onTest called when the Test alert button is tapped.
 * @param modifier the modifier for the whole shell.
 * @param content the scrolling content; it receives the strip to put at its
 *   end, or null when the strip is fixed.
 */
@Composable
private fun ShellLayout(
    tabBar: @Composable () -> Unit,
    onTest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (trailing: (@Composable () -> Unit)?) -> Unit,
) {
    val testButtonBar: @Composable () -> Unit = { TestButtonBar(onClick = onTest) }
    SubcomposeLayout(modifier.fillMaxSize().background(BeaconColors.Page)) { constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val barConstraints = Constraints(minWidth = width, maxWidth = width)
        val tabs = subcompose(ShellSlot.TAB_BAR, tabBar).map { it.measure(barConstraints) }
        val tabsHeight = tabs.maxOfOrNull { it.height } ?: 0
        // The probe is only measured, never placed, and has no semantics, so
        // TalkBack and tests only ever see the one real button. Its own copy
        // ignores taps, so nothing that reaches it can start a test.
        val probeHeight = subcompose(ShellSlot.BUTTON_PROBE) { Box(Modifier.clearAndSetSemantics {}) { TestButtonBar(onClick = {}) } }
            .maxOfOrNull { it.measure(barConstraints).height } ?: 0
        val fixed = testButtonFixed(tabsHeight + probeHeight, height)
        val button = if (fixed) subcompose(ShellSlot.FIXED_BUTTON, testButtonBar).map { it.measure(barConstraints) } else emptyList()
        // If the bars alone are taller than the window, the content gets no room rather than a negative height.
        val contentHeight = (height - tabsHeight - (button.maxOfOrNull { it.height } ?: 0)).coerceAtLeast(0)
        val body = subcompose(ShellSlot.CONTENT) { content(if (fixed) null else testButtonBar) }
            .map { it.measure(Constraints.fixed(width, contentHeight)) }
        layout(width, height) {
            body.forEach { it.placeRelative(0, 0) }
            button.forEach { it.placeRelative(0, contentHeight) }
            tabs.forEach { it.placeRelative(0, height - tabsHeight) }
        }
    }
}
