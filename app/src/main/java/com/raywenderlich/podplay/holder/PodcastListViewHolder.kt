package com.raywenderlich.podplay.holder

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.raywenderlich.podplay.R
import com.raywenderlich.podplay.adapter.PodcastListAdapter
import com.raywenderlich.podplay.viewmodel.SearchViewModel

class PodcastListViewHolder(
    itemView: View
) : RecyclerView.ViewHolder(itemView) {
    var podcastSummaryViewData: SearchViewModel.PodcastSummaryViewData? = null
    val nameTextView = itemView.findViewById(R.id.podcastNameTextView) as TextView
    val lastUpdatedTextView = itemView.findViewById(R.id.podcastLastUpdatedTextView) as TextView
    val podcastImageView =itemView.findViewById(R.id.podcastImage) as ImageView
}