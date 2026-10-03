package com.hajaulee.anytv.hajaumanager

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val recyclerView = findViewById<RecyclerView>(R.id.settingsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = SettingsAdapter(
            rows = createSettingsRows(),
            onCardClick = { card -> showCardDetails(card) },
            onActionClick = { card -> performAction(card.actionId) }
        )

        val screenWidth = resources.displayMetrics.widthPixels
        val maxContentWidth = dpToPx(720)
        val horizontalPadding = ((screenWidth - maxContentWidth) / 2)
            .coerceAtLeast(dpToPx(24))
        recyclerView.setPadding(
            horizontalPadding,
            recyclerView.paddingTop,
            horizontalPadding,
            recyclerView.paddingBottom
        )

        recyclerView.post {
            recyclerView.findViewHolderForAdapterPosition(2)?.itemView?.requestFocus()
        }
    }

    private fun createSettingsRows(): List<SettingsRow> = listOf(
        SettingsRow.Header,
        SettingsRow.Section(getString(R.string.settings_section_setup)),
        SettingsRow.Card(
            title = getString(R.string.settings_setup_title),
            description = getString(R.string.settings_setup_description),
            actionLabel = getString(R.string.settings_setup_action),
            actionId = ACTION_APP_INFO
        ),
        SettingsRow.Section(getString(R.string.settings_section_update)),
        SettingsRow.Card(
            title = getString(R.string.settings_update_title, BuildConfig.VERSION_NAME),
            description = getString(R.string.settings_update_description),
            actionLabel = getString(R.string.settings_update_action),
            actionId = ACTION_UPDATES
        ),
        SettingsRow.Section(getString(R.string.settings_section_pointer)),
        SettingsRow.Card(
            title = getString(R.string.settings_pointer_title),
            description = getString(R.string.settings_pointer_description),
            actionLabel = getString(R.string.settings_pointer_action),
            actionId = ACTION_POINTER
        ),
        SettingsRow.Section(getString(R.string.settings_section_sound)),
        SettingsRow.Card(
            title = getString(R.string.settings_sound_title),
            description = getString(R.string.settings_sound_description),
            actionLabel = getString(R.string.settings_sound_action),
            actionId = ACTION_SOUND
        )
    )

    private fun showCardDetails(card: SettingsRow.Card) {
        AlertDialog.Builder(this)
            .setTitle(card.title)
            .setMessage(card.description)
            .setPositiveButton(R.string.settings_close, null)
            .show()
    }

    private fun performAction(actionId: String) {
        val intent = when (actionId) {
            ACTION_APP_INFO -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            ACTION_UPDATES -> Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://github.com/hajaulee/HaJaUManager/releases")
            )
            ACTION_POINTER -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            ACTION_SOUND -> Intent(Settings.ACTION_SOUND_SETTINGS)
            else -> return
        }

        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.settings_unavailable, Toast.LENGTH_LONG).show()
        }
    }

    private fun dpToPx(value: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()

    private companion object {
        const val ACTION_APP_INFO = "app_info"
        const val ACTION_UPDATES = "updates"
        const val ACTION_POINTER = "pointer"
        const val ACTION_SOUND = "sound"
    }
}
