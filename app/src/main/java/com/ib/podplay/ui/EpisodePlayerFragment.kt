package com.ib.podplay.ui

import android.animation.ValueAnimator
import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.format.DateUtils
import android.text.method.ScrollingMovementMethod
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.ib.podplay.R
import com.ib.podplay.databinding.FragmentEpisodePlayerBinding
import com.ib.podplay.service.PodplayMediaCallback
import com.ib.podplay.service.PodplayMediaCallback.Companion.CMD_CHANGESPEED
import com.ib.podplay.service.PodplayMediaCallback.Companion.CMD_EXTRA_SPEED
import com.ib.podplay.service.PodplayMediaService
import com.ib.podplay.util.HtmlUtils
import com.ib.podplay.viewmodel.PodcastViewModel

class EpisodePlayerFragment : androidx.fragment.app.Fragment(R.layout.fragment_episode_player) {

    private val podcastViewModel: PodcastViewModel by activityViewModels()
    private lateinit var binding: FragmentEpisodePlayerBinding
    private lateinit var episodeDescTextView: TextView
    private lateinit var episodeTitleTextView: TextView
    private lateinit var playToggleButton: TextView
    private lateinit var speedButton: TextView
    private lateinit var currentTimeTextView: TextView
    private lateinit var endTimeTextView: TextView
    private lateinit var forwardButton: ImageView
    private lateinit var replayButton: ImageView
    private lateinit var episodeImageView: ImageView
    private lateinit var dowloadButton: ImageView
    private lateinit var seekBar: SeekBar
    private lateinit var headerView: View
    private lateinit var playerControls: View
    private lateinit var mediaBrowser: MediaBrowserCompat
    private var mediaControllerCallback: MediaControllerCallback? = null
    private var mediaSession: MediaSessionCompat? = null
    private var mediaPlayer: MediaPlayer? = null
    private var playOnPrepare: Boolean = false
    private lateinit var videoSurfaceView: SurfaceView
    private var isVideo: Boolean = false
    private var playerSpeed: Float = 1.0f
    private var episodeDuration: Long = 0
    private var draggingScrubber: Boolean = false
    private var progressAnimator: ValueAnimator? = null

    companion object {
        fun newInstance(): EpisodePlayerFragment {
            return EpisodePlayerFragment()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        isVideo()

        if (!isVideo) {
            initMediaBrowser()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentEpisodePlayerBinding.inflate(layoutInflater)

        episodeDescTextView = view.findViewById(R.id.episodeDescTextView)
        episodeTitleTextView = view.findViewById(R.id.episodeTitleTextView)
        episodeImageView = view.findViewById(R.id.episodeImageView)

        playToggleButton = view.findViewById(R.id.playToggleButton)

        forwardButton = view.findViewById(R.id.forwardButton)
        replayButton = view.findViewById(R.id.replayButton)

        endTimeTextView = view.findViewById(R.id.endTimeTextView)
        speedButton = view.findViewById(R.id.speedButton)
        seekBar = view.findViewById(R.id.seekBar)
        currentTimeTextView = view.findViewById(R.id.currentTimeTextView)

        videoSurfaceView = view.findViewById(R.id.videoSurfaceView)
        headerView = view.findViewById(R.id.headerView)
        playerControls = view.findViewById(R.id.playerControls)
//        dowloadButton = view.findViewById(R.id.releaseDateView)

        setupControls()
        if (isVideo) {
            initMediaSession()
            initVideoPlayer()
        }
        updateControls()
    }

    override fun onStart() {
        super.onStart()
        if (!isVideo) {

            if (mediaBrowser.isConnected) {
                val fragmentActivity = activity as FragmentActivity
                if (MediaControllerCompat.getMediaController(fragmentActivity) == null) {
                    registerMediaController(mediaBrowser.sessionToken)
                }
                updateControlsFromController()
            } else {
                mediaBrowser.connect()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        progressAnimator?.cancel()

        val fragmentActivity = activity as FragmentActivity
        if (MediaControllerCompat.getMediaController(fragmentActivity) != null) {
            mediaControllerCallback?.let {
                MediaControllerCompat.getMediaController(fragmentActivity).unregisterCallback(it)
            }
        }
        if (isVideo) {
            mediaPlayer?.setDisplay(null)
        }

        if (!fragmentActivity.isChangingConfigurations) {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    private fun setupControls() {
        playToggleButton.setOnClickListener {
            togglePlayPause()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            speedButton.setOnClickListener {
                changeSpeed()
            }
        } else {
            speedButton.visibility = View.INVISIBLE
        }
        forwardButton.setOnClickListener { seekBy(30) }
        replayButton.setOnClickListener { seekBy(-10) }

        seekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    currentTimeTextView.text = DateUtils.formatElapsedTime(
                        (progress / 1000).toLong()
                    )
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    draggingScrubber = true
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    draggingScrubber = false

                    val fragmentActivity = activity as FragmentActivity
                    val controller = MediaControllerCompat.getMediaController(fragmentActivity)
                    if (controller.playbackState != null) {

                        controller.transportControls.seekTo(seekBar.progress.toLong())
                    } else {
                        seekBar.progress = 0
                    }
                }
            }
        )
    }

    private fun setupVideoUI() {
        episodeDescTextView.visibility = View.INVISIBLE
        headerView.visibility = View.INVISIBLE

        val activity = activity as AppCompatActivity
        activity.supportActionBar?.hide()
        playerControls.setBackgroundColor(
            Color
                .argb(255 / 2, 0, 0, 0)
        )
    }

    private fun updateControlsFromController() {
        val fragmentActivity = activity as FragmentActivity
        val controller = MediaControllerCompat.getMediaController(fragmentActivity)
        if (controller != null) {
            val metadata = controller.metadata
            if (metadata != null) {
                handleStateChange(
                    controller.playbackState.state,
                    controller.playbackState.position, playerSpeed
                )
                updateControlsFromMetadata(controller.metadata)
            }
        }
    }

    private fun initVideoPlayer() {
        videoSurfaceView.visibility = View.VISIBLE

        val surfaceHolder = videoSurfaceView.holder

        surfaceHolder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                initMediaPlayer()
                mediaPlayer?.setDisplay(holder)
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
            }
        })
    }

    private fun initMediaPlayer() {
        if (mediaPlayer == null) {

            mediaPlayer = MediaPlayer()
            mediaPlayer?.let {
                it.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
                )

                it.setDataSource(
                    podcastViewModel.activeEpisodeViewData?.mediaUrl
                )

                it.setOnPreparedListener {

                    val fragmentActivity = activity as FragmentActivity
                    val episodeMediaCallback = PodplayMediaCallback(
                        fragmentActivity, mediaSession!!, it
                    )
                    mediaSession!!.setCallback(episodeMediaCallback)

                    setSurfaceSize()

                    if (playOnPrepare) {
                        playToggleButton.setOnClickListener {
                            togglePlayPause() }
                    }
                }
                it.prepareAsync()
            }
        } else {
            setSurfaceSize()
        }
    }

    private fun initMediaSession() {
        if (mediaSession == null) {
            mediaSession = MediaSessionCompat(activity as Context, "EpisodePlayerFragment")

            mediaSession?.setMediaButtonReceiver(null)
        }
        registerMediaController(mediaSession!!.sessionToken)
    }

    private fun setSurfaceSize() {
        val mediaPlayer = mediaPlayer ?: return

        val videoWidth = mediaPlayer.videoWidth
        val videoHeight = mediaPlayer.videoHeight

        val parent = videoSurfaceView.parent as View
        val containerWidth = parent.width
        val containerHeight = parent.height

        val layoutAspectRatio = containerWidth.toFloat() /
                containerHeight

        val videoAspectRatio = videoWidth.toFloat() / videoHeight

        val layoutParams = videoSurfaceView.layoutParams

        if (videoAspectRatio > layoutAspectRatio) {
            layoutParams.height = (containerWidth / videoAspectRatio).toInt()

        } else {
            layoutParams.width = (containerHeight * videoAspectRatio).toInt()
        }
        videoSurfaceView.layoutParams = layoutParams
    }

    private fun animateScrubber(progress: Int, speed: Float) {
        val timeRemaining = ((episodeDuration - progress) / speed).toInt()

        if (timeRemaining < 0) {
            return;
        }
        progressAnimator = ValueAnimator.ofInt(
            progress, episodeDuration.toInt()
        )
        progressAnimator?.let { animator ->
            animator.duration = timeRemaining.toLong()
            animator.interpolator = LinearInterpolator()
            animator.addUpdateListener {
                if (draggingScrubber) {
                    animator.cancel()
                } else {
                    seekBar.progress = animator.animatedValue as Int
                }
            }
            animator.start()
        }
    }

    private fun updateControlsFromMetadata(metadata: MediaMetadataCompat) {
        episodeDuration =

            metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
        endTimeTextView.text = DateUtils.formatElapsedTime(
            episodeDuration / 1000
        )

        seekBar.max = episodeDuration.toInt()
    }

    private fun changeSpeed() {
        playerSpeed += 0.25f

        when {
            playerSpeed > 2.0f ->
                playerSpeed = 0.75f
        }

        val bundle = Bundle()
        bundle.putFloat(CMD_EXTRA_SPEED, playerSpeed)

        val fragmentActivity = activity as FragmentActivity
        val controller =
            MediaControllerCompat.getMediaController(fragmentActivity)
        controller.sendCommand(CMD_CHANGESPEED, bundle, null)

        speedButton.text = "${playerSpeed}x"
    }

    private fun seekBy(seconds: Int) {
        val fragmentActivity = activity as FragmentActivity

        val controller = MediaControllerCompat.getMediaController(fragmentActivity)
        val newPosition = controller.playbackState.position + seconds * 1000
        controller.transportControls.seekTo(newPosition)
    }

    private fun handleStateChange(state: Int, position: Long, speed: Float) {

        progressAnimator?.let {
            it.cancel()
            progressAnimator = null
        }

        val isPlaying = state == PlaybackStateCompat.STATE_PLAYING
        playToggleButton.isActivated = isPlaying

        val progress = position.toInt()
        seekBar.progress = progress
        speedButton.text = "${playerSpeed}x"

        if (isPlaying) {
            if (isVideo) {
                setupVideoUI()
            }
            animateScrubber(progress, speed)
        }
    }

    private fun updateControls() {
        episodeTitleTextView.text =
            podcastViewModel.activeEpisodeViewData?.title

        val htmlDesc = podcastViewModel.activeEpisodeViewData?.description ?: ""

        val descSpan = HtmlUtils.htmlToSpannable(htmlDesc)

        episodeDescTextView.text = descSpan
        episodeDescTextView.movementMethod = ScrollingMovementMethod()

        val fragmentActivity = activity as FragmentActivity
        //with glide
        Glide.with(fragmentActivity)
            .load(podcastViewModel.activePodcastViewData?.imageUrl)
            .into(episodeImageView)

        speedButton.text = "${playerSpeed}x"

        mediaPlayer?.let {
            updateControlsFromController()
        }
    }

    private fun startPlaying(episodeViewData: PodcastViewModel.EpisodeViewData) {
        val fragmentActivity = activity as FragmentActivity
        val controller =
            MediaControllerCompat.getMediaController(fragmentActivity)
        val viewData = podcastViewModel.activePodcastViewData ?: return
        val bundle = Bundle()
        bundle.putString(MediaMetadataCompat.METADATA_KEY_TITLE, episodeViewData.title)
        bundle.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, viewData.feedTitle)
        bundle.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, viewData.imageUrl)

        controller.transportControls.playFromUri(Uri.parse(episodeViewData.mediaUrl), bundle)
    }

    private fun togglePlayPause() {
        playOnPrepare = true
        val fragmentActivity = activity as FragmentActivity
        val controller =
            MediaControllerCompat.getMediaController(fragmentActivity)
        if (controller.playbackState != null) {
            if (controller.playbackState.state ==
                PlaybackStateCompat.STATE_PLAYING) {
                controller.transportControls.pause()
            } else {
                podcastViewModel.activeEpisodeViewData?.let { startPlaying(it) }
            }
        } else {
            podcastViewModel.activeEpisodeViewData?.let { startPlaying(it) }
        }
    }

    private fun registerMediaController(token: MediaSessionCompat.Token) {
        val fragmentActivity = activity as FragmentActivity

        val mediaController = MediaControllerCompat(fragmentActivity, token)

        MediaControllerCompat.setMediaController(fragmentActivity, mediaController)

        mediaControllerCallback = MediaControllerCallback()
        mediaController.registerCallback(mediaControllerCallback!!)
    }

    private fun initMediaBrowser() {
        val fragmentActivity = activity as FragmentActivity
        mediaBrowser = MediaBrowserCompat(
            fragmentActivity, ComponentName(
                fragmentActivity,
                PodplayMediaService::class.java
            ), MediaBrowserCallBacks(), null
        )
    }

    inner class MediaBrowserCallBacks : MediaBrowserCompat.ConnectionCallback() {

        override fun onConnected() {
            super.onConnected()

            registerMediaController(mediaBrowser.sessionToken)
            updateControlsFromController()
            println("onConnected")
        }

        override fun onConnectionSuspended() {
            super.onConnectionSuspended()
            println("onConnectionSuspended")
            //Disable transport controls
        }

        override fun onConnectionFailed() {
            super.onConnectionFailed()
            println("onConnectionFailed")
            //fatal error handling
        }
    }

    inner class MediaControllerCallback : MediaControllerCompat.Callback() {

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)

            println(
                "metadata changed to ${
                    metadata?.getString(
                        MediaMetadataCompat.METADATA_KEY_MEDIA_URI
                    )
                }"
            )
            metadata?.let { updateControlsFromMetadata(it) }
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            val state = state ?: return
            handleStateChange(state.state, state.position, state.playbackSpeed)
        }
    }

     private fun isVideo() {
         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
             isVideo = podcastViewModel.activeEpisodeViewData?.isVideo ?: false
         } else {
             isVideo = false
         }
     }
}