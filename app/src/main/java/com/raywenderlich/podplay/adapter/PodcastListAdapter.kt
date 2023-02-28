package com.raywenderlich.podplay.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.raywenderlich.podplay.R
import com.raywenderlich.podplay.holder.PodcastListViewHolder
import com.raywenderlich.podplay.ui.PodcastActivity
import com.raywenderlich.podplay.viewmodel.SearchViewModel

class  PodcastListAdapter(
    private var podcastSummaryViewList:
    List<SearchViewModel.PodcastSummaryViewData>?,
    private val podcastActivity: PodcastActivity,
    private val podcastListAdapterListener: PodcastListAdapterListener,
) : RecyclerView.Adapter<PodcastListViewHolder>() {


    interface PodcastListAdapterListener {
        fun onShowDetails(
            podcastSummaryViewData:
            SearchViewModel.PodcastSummaryViewData)
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PodcastListViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.search_item, parent, false)

        return PodcastListViewHolder(view)
    }

    override fun onBindViewHolder(holder: PodcastListViewHolder, position: Int) {
        val searchViewList = podcastSummaryViewList ?: return
        val searchView = searchViewList[position]
        holder.podcastSummaryViewData = searchView
        holder.nameTextView.text = searchView.name
        holder.lastUpdatedTextView.text = searchView.lastUpdate

        Glide.with(podcastActivity)
            .load(searchView.imageUrl)
            .into(holder.podcastImageView)

        holder.itemView.setOnClickListener {
            podcastListAdapterListener.onShowDetails(searchView)
        }
    }

    override fun getItemCount(): Int {
        return podcastSummaryViewList?.size ?: 0
    }

    fun setSearchData(podcastSummaryViewData: List<SearchViewModel.PodcastSummaryViewData>){
        podcastSummaryViewList = podcastSummaryViewData
        this.notifyDataSetChanged()
    }

}