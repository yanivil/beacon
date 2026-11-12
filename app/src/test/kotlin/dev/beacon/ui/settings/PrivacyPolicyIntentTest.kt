package dev.beacon.ui.settings

import android.app.Activity
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf

/*
 * Why these tests: openPrivacyPolicy is the only place Beacon hands an
 * address to another app. It must open the published policy, in a web
 * browser (the screen says so), from any context, and never crash when
 * nothing can open it.
 */

/** Robolectric tests for [openPrivacyPolicy]. */
@RunWith(AndroidJUnit4::class)
class PrivacyPolicyIntentTest {
    /** The application, whose package manager and started activities the tests read. */
    private val app = ApplicationProvider.getApplicationContext<Application>()

    /**
     * Installs a fake app that opens https links.
     *
     * @param packageName the fake app's package.
     * @param host the one host it claims, or null for every web address, as a browser does.
     */
    private fun installWebApp(packageName: String, host: String? = null) {
        val component = ComponentName(packageName, "$packageName.Main")
        val pm = shadowOf(app.packageManager)
        pm.addActivityIfNotPresent(component)
        pm.addIntentFilterForActivity(
            component,
            IntentFilter(Intent.ACTION_VIEW).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                addCategory(Intent.CATEGORY_BROWSABLE)
                addDataScheme("https")
                if (host != null) addDataAuthority(host, null)
            },
        )
    }

    /** From an activity, it starts a browsable VIEW of the policy's address in the same task. */
    @Test fun opensThePolicyAsABrowsableView() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        assertTrue(openPrivacyPolicy(activity))
        val started = shadowOf(activity).nextStartedActivity
        assertNotNull("an activity was started", started)
        assertEquals(Intent.ACTION_VIEW, started.action)
        assertEquals("https://github.com/yanivil/beacon/blob/main/PRIVACY.md", started.dataString)
        assertTrue("browsable", started.hasCategory(Intent.CATEGORY_BROWSABLE))
        assertEquals("no new task from an activity", 0, started.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /** From a context that isn't an activity, it asks for a new task, which Android requires there. */
    @Test fun fromTheApplicationItStartsANewTask() {
        assertTrue(openPrivacyPolicy(app))
        val started = shadowOf(app).nextStartedActivity
        assertNotNull("an activity was started", started)
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, started.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /**
     * When an app claims GitHub's links (the GitHub app does), the policy still goes to the
     * browser, so "Opens in your web browser." stays true.
     */
    @Test fun goesToTheBrowserEvenWhenAnAppClaimsGitHubLinks() {
        installWebApp("com.github.android", host = "github.com")
        installWebApp("com.example.browser")
        assertTrue(openPrivacyPolicy(app))
        assertEquals("com.example.browser", shadowOf(app).nextStartedActivity.`package`)
    }

    /** When no browser is found, even with an app for GitHub links installed, it leaves the choice to Android. */
    @Test fun withNoBrowserFoundAndroidChooses() {
        installWebApp("com.github.android", host = "github.com")
        assertTrue(openPrivacyPolicy(app))
        assertNull(shadowOf(app).nextStartedActivity.`package`)
    }

    /** Android's own forwarder (package "android", for work profiles) is never taken for the browser. */
    @Test fun androidsOwnForwarderIsNotABrowser() {
        installWebApp("android")
        assertNull(browserPackage(app))
    }

    /** If the named browser won't start, it tries once more without naming it, as before 1.0.2. */
    @Test fun triesAgainWithoutTheBrowserWhenItFails() {
        installWebApp("com.example.browser")
        val tried = mutableListOf<Intent>()
        val context = object : ContextWrapper(app) {
            override fun startActivity(intent: Intent) {
                tried += Intent(intent)
                if (intent.`package` != null) throw ActivityNotFoundException("the named browser refused")
            }
        }
        assertTrue(openPrivacyPolicy(context))
        assertEquals(listOf("com.example.browser", null), tried.map { it.`package` })
    }

    /** When nothing can open the page, it reports false instead of crashing. */
    @Test fun withNothingToOpenItReturnsFalse() {
        // Robolectric then throws ActivityNotFoundException, as a phone with no browser does.
        shadowOf(app).checkActivities(true)
        assertFalse(openPrivacyPolicy(app))
        assertNull(shadowOf(app).nextStartedActivity)
    }
}
