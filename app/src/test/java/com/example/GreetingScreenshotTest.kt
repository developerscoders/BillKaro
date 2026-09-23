package com.example

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.components.AnimatedStatCard
import com.example.ui.theme.BillKaroTheme
import com.example.ui.theme.SleekOnPrimaryContainer
import com.example.ui.theme.SleekPrimaryContainer
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent {
      BillKaroTheme {
        AnimatedStatCard(
          title = "Total Sales",
          value = "₹1,24,000",
          icon = Icons.AutoMirrored.Filled.TrendingUp,
          iconBgColor = SleekPrimaryContainer,
          iconColor = SleekOnPrimaryContainer,
          badgeText = "Monthly"
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}

