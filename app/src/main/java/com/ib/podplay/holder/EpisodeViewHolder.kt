package com.ib.podplay.holder

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ib.podplay.R
import com.ib.podplay.adapter.EpisodeListAdapter
import com.ib.podplay.viewmodel.PodcastViewModel

class EpisodeViewHolder(
    itemView: View,
    val episodeListAdapterListener:
    EpisodeListAdapter.EpisodeListAdapterListener
) : RecyclerView.ViewHolder(itemView) {
    var episodeViewData: PodcastViewModel.EpisodeViewData? = null
    val titleTextView: TextView = itemView.findViewById(R.id.titleView)
    val descTextView: TextView = itemView.findViewById(R.id.descView)
    val durationTextView: TextView = itemView.findViewById(R.id.durationView)
    val releaseDateTextView: TextView = itemView.findViewById(R.id.releaseDateView)
}