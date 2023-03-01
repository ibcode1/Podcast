package com.ib.podplay.repository

import androidx.lifecycle.LiveData
import com.ib.podplay.db.PodcastDao
import com.ib.podplay.model.Episode
import com.ib.podplay.model.Podcast
import com.ib.podplay.service.FeedService
import com.ib.podplay.service.RssFeedResponse
import com.ib.podplay.service.RssFeedService
import com.ib.podplay.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PodcastRepo(private var feedService: FeedService, private var podcastDao: PodcastDao) {

    private val rssFeedService = RssFeedService()

    fun getPodcast(
        feedUrl: String,
        callback: (Podcast?) -> Unit
    ) {
        GlobalScope.launch {
            val podcast = podcastDao.loadPodcast(feedUrl)
            if (podcast != null) {
                podcast.id?.let {
                    podcast.episodes = podcastDao.loadEpisodes(it)
                    GlobalScope.launch(Dispatchers.Main) {
                        callback(podcast)
                    }
                }
            } else {
                feedService.getFeed(feedUrl) { feedResponse ->
                    var podcast: Podcast? = null
                    if (feedResponse != null) {
                        podcast = rssResponseToPodcast(feedUrl, "", feedResponse)
                    }

                    GlobalScope.launch(Dispatchers.Main) {
                        callback(podcast)
                    }
                }
            }
        }
        rssFeedService.getFeed(feedUrl){
        }
    }



    fun getAll(): LiveData<List<Podcast>> {
        return podcastDao.loadPodcasts()
    }

    fun save(podcast: Podcast) {
        GlobalScope.launch {
            val podcastId = podcastDao.insertPodcast(podcast)

            podcast.episodes.forEach { episode ->
                episode.podcastId = podcastId
                podcastDao.insertEpisode(episode)
            }
        }
    }

    fun delete(podcast: Podcast) {
        GlobalScope.launch {
            podcastDao.deletePodcast(podcast)
        }
    }

    private fun rssItemsToEpisodes(
        episodeResponses:
        List<RssFeedResponse.EpisodeResponse>
    ): List<Episode> {
        return episodeResponses.map {
            Episode(
                it.guid ?: "",
                null,
                it.title ?: "",
                it.description ?: "",
                it.url ?: "",
                it.type ?: "",
                DateUtils.xmlDateToDate(it.pubDate),
                it.duration ?: ""
            )
        }
    }

    private fun rssResponseToPodcast(
        feedUrl: String, imageUrl: String,
        rssResponse: RssFeedResponse
    ): Podcast? {
        val items = rssResponse.episodes ?: return null

        val description = if (rssResponse.description == "")
            rssResponse.summary else rssResponse.description

        return Podcast(
            null, feedUrl, rssResponse.title, description, imageUrl,
            rssResponse.lastUpdated, episodes = rssItemsToEpisodes(items)
        )
    }

    private fun getNewEpisodes(localPodcast: Podcast, callBack: (List<Episode>) -> Unit) {
        feedService.getFeed(localPodcast.feedUrl) { response ->
            if (response != null) {

                val remotePodcast = rssResponseToPodcast(
                    localPodcast.feedUrl,
                    localPodcast.imageUrl, response
                )
                remotePodcast?.let {
                    val localEpisodes = podcastDao.loadEpisodes(localPodcast.id!!)
                    val newEpisodes = remotePodcast.episodes.filter { episode ->
                        localEpisodes.find {
                            episode.guid == it.guid
                        } == null
                    }
                    callBack(newEpisodes)
                }
            } else {
                callBack(listOf())
            }
        }
    }

    private fun saveNewEpisodes(podcastId: Long, episodes: List<Episode>) {
        GlobalScope.launch {
            episodes.forEach { episode ->
                episode.podcastId = podcastId
                podcastDao.insertEpisode(episode)
            }
        }
    }

    class PodcastUpdateInfo(val feedUrl: String, val name: String, val newCount: Int)

    fun updatePodcastEpisodes(callback: (List<PodcastUpdateInfo>) -> Unit) {
        val updatedPodcasts: MutableList<PodcastUpdateInfo> = mutableListOf()

        val podcasts = podcastDao.loadPodcastsStatic()

        var processCount = podcasts.count()

        podcasts.forEach { podcast ->
            getNewEpisodes(podcast) { newEpisodes ->
                if (newEpisodes.count() > 0) {
                    saveNewEpisodes(podcast.id!!, newEpisodes)
                    updatedPodcasts.add(
                        PodcastUpdateInfo(
                            podcast.feedUrl, podcast.feedTitle,
                            newEpisodes.count()
                        )
                    )
                }
                processCount--
                if (processCount == 0) {
                    callback(updatedPodcasts)
                }
            }
        }
    }

}
