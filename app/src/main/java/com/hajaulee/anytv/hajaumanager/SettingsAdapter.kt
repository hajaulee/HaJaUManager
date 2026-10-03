package com.hajaulee.anytv.hajaumanager

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

sealed class SettingsRow {
    object Header : SettingsRow()
    data class Section(val title: String) : SettingsRow()
    data class Card(
        val title: String,
        val description: String,
        val actionLabel: String,
        val actionId: String
    ) : SettingsRow()
}

class SettingsAdapter(
    private val rows: List<SettingsRow>,
    private val onCardClick: (SettingsRow.Card) -> Unit,
    private val onActionClick: (SettingsRow.Card) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemViewType(position: Int): Int = when (rows[position]) {
        SettingsRow.Header -> VIEW_TYPE_HEADER
        is SettingsRow.Section -> VIEW_TYPE_SECTION
        is SettingsRow.Card -> VIEW_TYPE_CARD
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> HeaderViewHolder(
                inflater.inflate(R.layout.item_settings_header, parent, false)
            )
            VIEW_TYPE_SECTION -> SectionViewHolder(
                inflater.inflate(R.layout.item_settings_section, parent, false)
            )
            VIEW_TYPE_CARD -> CardViewHolder(
                inflater.inflate(R.layout.item_settings_card, parent, false)
            )
            else -> error("Unsupported settings row type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            SettingsRow.Header -> Unit
            is SettingsRow.Section -> (holder as SectionViewHolder).bind(row)
            is SettingsRow.Card -> (holder as CardViewHolder).bind(
                row = row,
                onCardClick = onCardClick,
                onActionClick = onActionClick
            )
        }
    }

    override fun getItemCount(): Int = rows.size

    private class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private class SectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(row: SettingsRow.Section) {
            (itemView as TextView).text = row.title
        }
    }

    private class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.settingCardTitle)
        private val description: TextView =
            itemView.findViewById(R.id.settingCardDescription)
        private val action: Button = itemView.findViewById(R.id.settingCardAction)

        init {
            if (itemView.resources.configuration.screenWidthDp < COMPACT_LAYOUT_MAX_WIDTH_DP) {
                val card = itemView as LinearLayout
                val content = itemView.findViewById<LinearLayout>(R.id.settingCardContent)
                card.orientation = LinearLayout.VERTICAL
                content.layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                content.setPadding(0, 0, 0, 0)
                action.layoutParams = (action.layoutParams as LinearLayout.LayoutParams).apply {
                    gravity = Gravity.END
                    topMargin = (ACTION_TOP_MARGIN_DP *
                        itemView.resources.displayMetrics.density).toInt()
                }
            }
        }

        fun bind(
            row: SettingsRow.Card,
            onCardClick: (SettingsRow.Card) -> Unit,
            onActionClick: (SettingsRow.Card) -> Unit
        ) {
            title.text = row.title
            description.text = row.description
            action.text = row.actionLabel
            action.contentDescription = "${row.actionLabel}: ${row.title}"
            itemView.contentDescription = "${row.title}. ${row.description}"
            itemView.setOnClickListener { onCardClick(row) }
            action.setOnClickListener { onActionClick(row) }
        }
    }

    private companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_SECTION = 1
        const val VIEW_TYPE_CARD = 2
        const val COMPACT_LAYOUT_MAX_WIDTH_DP = 480
        const val ACTION_TOP_MARGIN_DP = 12
    }
}
