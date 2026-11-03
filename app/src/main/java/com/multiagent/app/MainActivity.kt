package com.multiagent.app

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView

class MainActivity : Activity() {

    private data class InstalledApp(
        val label: String,
        val packageName: String
    ) {
        override fun toString(): String = label
    }

    private lateinit var topAppSpinner: Spinner
    private lateinit var bottomAppSpinner: Spinner
    private lateinit var statusText: TextView
    private var installedApps: List<InstalledApp> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createScreen()
        loadInstalledApps()
    }

    private fun createScreen() {
        val background = 0xFFF4F6FA.toInt()
        val navy = 0xFF172554.toInt()
        val muted = 0xFF64748B.toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(24))
            setBackgroundColor(background)
        }

        root.addView(TextView(this).apply {
            text = "Vertical Split Screen"
            textSize = 27f
            setTextColor(navy)
            setTypeface(null, android.graphics.Typeface.BOLD)
        })

        root.addView(TextView(this).apply {
            text = "Choose which app you want at the top and which one you want underneath."
            textSize = 16f
            setTextColor(muted)
            setPadding(0, dp(10), 0, dp(26))
        })

        root.addView(sectionTitle("TOP APP", navy))
        topAppSpinner = createSpinner()
        root.addView(topAppSpinner, matchWidth())

        root.addView(sectionTitle("BOTTOM APP", navy).apply {
            setPadding(0, dp(24), 0, dp(8))
        })
        bottomAppSpinner = createSpinner()
        root.addView(bottomAppSpinner, matchWidth())

        val openButton = Button(this).apply {
            text = "Open both apps"
            textSize = 16f
            isAllCaps = false
            setOnClickListener { openSelectedApps() }
        }
        val buttonParams = matchWidth().apply {
            topMargin = dp(28)
        }
        root.addView(openButton, buttonParams)

        statusText = TextView(this).apply {
            text = "Loading installed apps..."
            textSize = 14f
            setTextColor(muted)
            setPadding(0, dp(18), 0, 0)
            gravity = Gravity.CENTER
        }
        root.addView(statusText, matchWidth())

        root.addView(TextView(this).apply {
            text = "Note: Your phone must support Android split-screen. If the apps open one after another, open Recent Apps, tap an app icon, choose Split screen, then select the other app."
            textSize = 13f
            setTextColor(muted)
            setPadding(0, dp(26), 0, 0)
        })

        setContentView(root)
    }

    private fun sectionTitle(label: String, color: Int): TextView {
        return TextView(this).apply {
            text = label
            textSize = 13f
            setTextColor(color)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, dp(8))
        }
    }

    private fun createSpinner(): Spinner {
        return Spinner(this).apply {
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
    }

    private fun loadInstalledApps() {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        val activities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                launcherIntent,
                PackageManager.ResolveInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(launcherIntent, 0)
        }

        installedApps = activities
            .mapNotNull { info ->
                val packageName = info.activityInfo?.packageName ?: return@mapNotNull null
                if (packageName == packageNameOfThisApp()) return@mapNotNull null
                InstalledApp(
                    label = info.loadLabel(packageManager).toString(),
                    packageName = packageName
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }

        if (installedApps.isEmpty()) {
            statusText.text = "No other launchable apps were found."
            return
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            installedApps
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        topAppSpinner.adapter = adapter
        bottomAppSpinner.adapter = adapter

        if (installedApps.size > 1) {
            bottomAppSpinner.setSelection(1)
        }

        statusText.text = "${installedApps.size} apps found. Choose two different apps."
    }

    private fun openSelectedApps() {
        if (installedApps.size < 2) {
            statusText.text = "At least two launchable apps are needed."
            return
        }

        val topApp = installedApps[topAppSpinner.selectedItemPosition]
        val bottomApp = installedApps[bottomAppSpinner.selectedItemPosition]

        if (topApp.packageName == bottomApp.packageName) {
            statusText.text = "Please choose two different apps."
            return
        }

        val topIntent = packageManager.getLaunchIntentForPackage(topApp.packageName)
        val bottomIntent = packageManager.getLaunchIntentForPackage(bottomApp.packageName)

        if (topIntent == null || bottomIntent == null) {
            statusText.text = "Android could not open one of the selected apps."
            return
        }

        try {
            // Open the first app, then ask Android to place the second beside it.
            topIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(topIntent)

            bottomIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK
            )
            startActivity(bottomIntent)

            statusText.text = "Launch requests sent. Your phone decides whether split-screen is available."
        } catch (exception: Exception) {
            statusText.text = "Could not start split-screen. Try using your phone's Recent Apps split-screen option."
        }
    }

    private fun packageNameOfThisApp(): String = applicationContext.packageName

    private fun matchWidth(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
