package com.sanwal.thetransformationapp


import androidx.appcompat.app.AppCompatActivity
    import com.homesoft.encoder.MuxerConfig
    import kotlinx.coroutines.Dispatchers
    import java.io.File
    import android.Manifest
    import android.content.pm.PackageManager
    import android.graphics.Bitmap
    import android.media.MediaFormat
    import android.os.Build
    import android.os.Bundle
    import android.util.Log
    import android.widget.Toast
    import androidx.activity.viewModels
    import androidx.lifecycle.lifecycleScope
    import com.homesoft.encoder.*
import com.sanwal.thetransformationapp.databinding.ActivityBit4videoBinding
import kotlinx.coroutines.launch
    import kotlinx.coroutines.withContext

    class VideoActivity : AppCompatActivity() {
        private val videoViewModel:VideoViewModel by viewModels()
        lateinit var binding: ActivityBit4videoBinding
        companion object {
            val TAG = MainActivity::class.java.simpleName
            val imageArray: List<Int> = listOf(
                R.raw.im1,
                R.raw.im2,
                R.raw.im3,
                R.raw.im4
            )
        }
        private var bitmaps = listOf<Bitmap>()

        private var videoFile: File? = null
        private var muxerConfig: MuxerConfig? = null
        private var mimeType = MediaFormat.MIMETYPE_VIDEO_AVC
        private var photos = listOf<InternalStoragePhoto?>()

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            binding = ActivityBit4videoBinding.inflate(layoutInflater)
            setContentView(binding.root)
            photos = videoViewModel.getInternalStoragePhotoFromFilePaths()
            bitmaps = photos.map {photo->
                    photo?.bmp!!.also { Log.d("sanchit", "onCreate: ${photo.name}") }
            }
            if(bitmaps.isNullOrEmpty()){
                Log.d("sanchit", "onCreate: bitmaps are null")
            }
           binding.avc.isEnabled = isCodecSupported(mimeType)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1234)
            }

            setListeners()
        }

        private fun setListeners() {
            binding.btMake.setOnClickListener {
                binding.btMake.isEnabled = false

                basicVideoCreation()
            }

            binding.avc.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) setCodec(MediaFormat.MIMETYPE_VIDEO_AVC)
            }

            binding.hevc.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) setCodec(MediaFormat.MIMETYPE_VIDEO_HEVC)
            }

            binding.btPlay.setOnClickListener {
                videoFile?.run {
                    binding.player.setVideoPath(this.absolutePath)
                    binding.player.start()
                }
            }

            binding.btShare.setOnClickListener {
                Log.i(TAG, "Sharing video...")
                muxerConfig?.run {
                    FileUtils.shareVideo(this@VideoActivity, file, mimeType)
                }
            }
        }

        private fun setCodec(codec: String) {
            if (isCodecSupported(codec)) {
                mimeType = codec
                muxerConfig?.mimeType = mimeType
            } else {
                Toast.makeText(this@VideoActivity, "AVC Codec not supported", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        // Basic implementation
        private fun basicVideoCreation() {
            videoFile = FileUtils.getVideoFile(this@VideoActivity, "test.mp4")
            videoFile?.run {
                muxerConfig = MuxerConfig(this, 600, 900, mimeType, 1, 3F, 1500000)
                val muxer = Muxer(this@VideoActivity, muxerConfig!!)
                createVideoAsync(muxer) // using co-routines
            }
        }

    //    // Callback-style approach
    //    private fun createVideo(muxer: Muxer) {
    //        muxer.setOnMuxingCompletedListener(object : MuxingCompletionListener {
    //            override fun onVideoSuccessful(file: File) {
    //                Log.d(TAG, "Video muxed - file path: ${file.absolutePath}")
    //                onMuxerCompleted()
    //            }
    //
    //            override fun onVideoError(error: Throwable) {
    //                Log.e(TAG, "There was an error muxing the video")
    //                onMuxerCompleted()
    //            }
    //        })
    //
    //        // Needs to happen on a background thread (long-running process)
    //        Thread(Runnable {
    //            muxer.mux(imageArray, R.raw.bensound_happyrock)
    //        }).start()
    //    }

        // Coroutine approach
        private fun createVideoAsync(muxer: Muxer) {
            lifecycleScope.launch(Dispatchers.Default) {
                when (val result = muxer.muxAsync(bitmaps, R.raw.bensound_happyrock)) {
                    is MuxingSuccess -> {
                        Log.i(TAG, "Video muxed - file path: ${result.file.absolutePath}")
                        onMuxerCompleted()
                    }
                    is MuxingError -> {
                        Log.e(TAG, "There was an error muxing the video")
                        binding.btMake.isEnabled = true
                    }
                }
            }
        }

        private suspend fun onMuxerCompleted() {
            withContext(Dispatchers.Main)
            {
                binding.btMake.isEnabled = true
                binding.btPlay.isEnabled = true
                binding.btShare.isEnabled = true
            }
        }
    }