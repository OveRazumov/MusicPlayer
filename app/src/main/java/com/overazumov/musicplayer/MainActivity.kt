package com.overazumov.musicplayer

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.SeekBar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.overazumov.musicplayer.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.Exception

class MainActivity : AppCompatActivity() {
    companion object {
        private const val MIN_TRACK_DURATION_MS = 30000
        private const val MAX_TRACK_DURATION_MS = 900000
        private const val SWITCH_TRACK_DURATION_MS = 3000
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var mediaPlayer: MediaPlayer

    private val songs = mutableListOf<Song>()
    private var isPlaying = false
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!checkPermission()) requestPermission()

        initializeSongs()

        binding.apply {
            nextButton.setOnClickListener { switchToNextSong() }
            prevButton.setOnClickListener { switchToPreviousSong() }
            playButton.setOnClickListener {
                if (isPlaying) {
                    mediaPlayer.pause()
                    isPlaying = false
                    playButton.setImageResource(R.drawable.ic_play)
                } else {
                    mediaPlayer.start()
                    isPlaying = true
                    playButton.setImageResource(R.drawable.ic_pause)
                }
            }
            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) mediaPlayer.seekTo(progress)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    mediaPlayer.pause()
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    if (isPlaying) mediaPlayer.start()
                }
            })
        }
    }

    private fun switchToNextSong() {
        currentIndex = (currentIndex + 1) % songs.size
        changeSong()
    }

    private fun switchToPreviousSong() {
        if (mediaPlayer.currentPosition < SWITCH_TRACK_DURATION_MS) {
            currentIndex = (currentIndex - 1 + songs.size) % songs.size
            changeSong()
        } else {
            mediaPlayer.seekTo(0)
        }
    }

    private fun changeSong() {
        initializeCurrentSong()
        if (isPlaying) mediaPlayer.start()
    }

    private fun initializeCurrentSong() {
        initializeMediaPlayer()
        initializeTitle()
        initializeSeekBar()
    }

    private fun initializeSongs() {
        try {
            initializeExternalSongs()
        } catch (e: Exception) {
            initializeInternalSongs()
        } finally {
            initializeCurrentSong()
        }
    }

    private fun initializeMediaPlayer() {
        if (::mediaPlayer.isInitialized) mediaPlayer.stop()
        mediaPlayer = MediaPlayer.create(this, Uri.parse(songs[currentIndex].path))
        mediaPlayer.setOnCompletionListener { switchToNextSong() }
    }

    private fun initializeTitle() {
        binding.titleText.text = songs[currentIndex].title
    }

    private fun initializeSeekBar() {
        binding.seekBar.apply {
            progress = 0
            max = mediaPlayer.duration
            lifecycleScope.launch(Dispatchers.Main) {
                while (isActive) {
                    progress = mediaPlayer.currentPosition
                    delay(500)
                }
            }
        }
    }

    private fun initializeExternalSongs() {
        val projection = arrayOf(
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DURATION
        )

        val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"

        contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            null
        ).use { cursor ->
            while (cursor!!.moveToNext()) {
                val path = cursor.getString(0)
                val title = cursor.getString(1)
                val duration = cursor.getString(2).toInt()

                if (File(path).exists() && duration > MIN_TRACK_DURATION_MS && duration < MAX_TRACK_DURATION_MS) {
                    songs.add(Song(path, title))
                }
            }
        }
        if (songs.isEmpty()) throw Exception()
    }

    private fun initializeInternalSongs() {
        songs.addAll(internalSongs)
    }

    private fun checkPermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_MEDIA_AUDIO), 123)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 123) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                recreate()
            }
        }
    }
}

