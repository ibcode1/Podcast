package com.raywenderlich.podplay.holder

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.raywenderlich.podplay.R
import com.raywenderlich.podplay.adapter.EpisodeListAdapter
import com.raywenderlich.podplay.viewmodel.PodcastViewModel

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