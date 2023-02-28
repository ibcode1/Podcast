package com.raywenderlich.podplay.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.raywenderlich.podplay.R
import com.raywenderlich.podplay.holder.EpisodeViewHolder
import com.raywenderlich.podplay.util.DateUtils
import com.raywenderlich.podplay.util.HtmlUtils
import com.raywenderlich.podplay.viewmodel.PodcastViewModel

class EpisodeListAdapter(private var episodeViewList: List<PodcastViewModel.EpisodeViewData>?, private val episodeListAdapterListener: EpisodeListAdapterListener
) : RecyclerView.Adapter<EpisodeViewHolder>() {

    interface EpisodeListAdapterListener {
        fun onSelectedEpisode(episodeViewData: PodcastViewModel.EpisodeViewData)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.episode_item, parent, false)

        return EpisodeViewHolder(view,episodeListAdapterListener)
    }

    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
        val episodeViewList = episodeViewList ?: return
        val episodeView = episodeViewList[position]

        holder.episodeViewData = episodeView
        holder.titleTextView.text = episodeView.title
        holder.descTextView.text = HtmlUtils.htmlToSpannable(
            episodeView.description ?: ""
        )
        holder.durationTextView.text = episodeView.duration
        holder.releaseDateTextView.text = episodeView.releasedDate?.let {
            DateUtils.dateToShortDate(it)
        }

        holder.itemView.setOnClickListener {
            episodeListAdapterListener.onSelectedEpisode(episodeView)
        }
    }

    override fun getItemCount(): Int {
        return episodeViewList?.size ?: 0
    }
}