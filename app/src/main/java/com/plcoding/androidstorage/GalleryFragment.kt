package com.sanwal.thetransformationapp


import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.sanwal.thetransformationapp.databinding.FragmentGalleryBinding
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class GalleryFragment : Fragment() {

    lateinit var binding : FragmentGalleryBinding
    private val videoViewModel:VideoViewModel by activityViewModels()
    lateinit var internalStoragePhotoAdapter: InternalStoragePhotoAdapter
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        internalStoragePhotoAdapter = InternalStoragePhotoAdapter {  }

        binding.btnTakePhoto.setOnClickListener {
            takeImage()
            //takePicturePreview.launch()
        }
        setupInternalStorageRecyclerView()
        loadPhotosFromInternalStorageIntoRecyclerView()
        binding.floatingActionButton.setOnClickListener{
            findNavController().navigate(GalleryFragmentDirections.actionGalleryFragmentToVideoFragment())
        }
    }

    private fun setupInternalStorageRecyclerView() = binding.rvPrivatePhotos.apply {
        adapter = internalStoragePhotoAdapter
        layoutManager = StaggeredGridLayoutManager(3, RecyclerView.VERTICAL)
    }

    private fun loadPhotosFromInternalStorageIntoRecyclerView(){
        val photos = videoViewModel.getInternalStoragePhotoFromFilePaths()
        internalStoragePhotoAdapter.submitList(photos)
    }

    private fun savePhotoToInternalStorage(fileName:String, bmp : Bitmap): Boolean{
        return try {
            requireActivity().openFileOutput("$fileName.jpg", AppCompatActivity.MODE_PRIVATE).use {
                if(!bmp.compress(Bitmap.CompressFormat.JPEG, 95, it))
                    throw  IOException("Couldn't save bitmap.")
            }
            true
        }catch (e: IOException){
            e.printStackTrace()
            false
        }
    }


    private val takeImageResult = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
        if (isSuccess) {
            Toast.makeText(requireActivity(), "Photo saved successfully", Toast.LENGTH_LONG).show()
            videoViewModel.imageFilePaths.add(currentPhotoPath)
            loadPhotosFromInternalStorageIntoRecyclerView()
        }else {
            Toast.makeText(requireActivity(), "Photo not saved", Toast.LENGTH_LONG).show()
        }
    }

    val takePicturePreview = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) {
        val isPrivate = binding.switchPrivate.isChecked
        if(isPrivate) {
            val isSavedSuccessfully = savePhotoToInternalStorage(UUID.randomUUID().toString(), it)
            if(isSavedSuccessfully) {
                loadPhotosFromInternalStorageIntoRecyclerView()
                Toast.makeText(requireActivity(), "Photo saved successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireActivity(), "Failed to save photo", Toast.LENGTH_SHORT).show()
            }
        }
    }

//    private val selectImageFromGalleryResult = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
//        uri?.let { previewImage.setImageURI(uri) }
//    }

    private var latestTmpUri: Uri? = null
    private fun takeImage() {
        lifecycleScope.launchWhenStarted {
            getTmpFileUri().let { uri ->
                latestTmpUri = uri
                takeImageResult.launch(uri)
                Log.d("sanchit", "takeImage: $uri")
            }
        }
    }

//    private fun selectImageFromGallery() = selectImageFromGalleryResult.launch("image/*")

    private fun getTmpFileUri(): Uri {
        //val timeStamp = SimpleDateFormat.getDateTimeInstance()
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
//        val tmpFile = File.createTempFile("JPEG_$timeStamp", ".jpg", filesDir).apply {
//            createNewFile()
//        }
        val tmpFile = File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply { createNewFile() }
        currentPhotoPath = tmpFile.absolutePath

        return FileProvider.getUriForFile(requireActivity(), "${BuildConfig.APPLICATION_ID}.provider", tmpFile)
    }

///////////////////////////////
    /////////////////////////////////////

    lateinit var currentPhotoPath: String

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
            videoViewModel.imageFilePaths.add(currentPhotoPath)
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(requireActivity().packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        requireActivity(),
                        "${BuildConfig.APPLICATION_ID}.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    ActivityResultContracts.StartActivityForResult()
                }
            }
        }
    }
}