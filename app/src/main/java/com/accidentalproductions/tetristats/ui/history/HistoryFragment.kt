package com.accidentalproductions.tetristats.ui.history

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.accidentalproductions.tetristats.databinding.FragmentHistoryBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryFragment : Fragment(), ScoreAdapter.MediaAttachmentListener {
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HistoryViewModel by viewModels { HistoryViewModelFactory(requireActivity().application) }
    private lateinit var scoreAdapter: ScoreAdapter

    // Request code for permissions
    private val STORAGE_PERMISSION_CODE = 100
    private val CAMERA_PERMISSION_CODE = 101

    // Current score being edited
    private var currentScoreId: Long = -1
    private var currentPhotoUri: Uri? = null

    // Activity Result Launchers
    private lateinit var pickMediaLauncher: ActivityResultLauncher<Intent>
    private lateinit var takePhotoLauncher: ActivityResultLauncher<Uri>
    private lateinit var takeVideoLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize activity result launchers
        pickMediaLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val selectedMediaUri = result.data?.data
                if (selectedMediaUri != null && currentScoreId != -1L) {
                    // Copy the media to our app's storage for persistence
                    viewModel.saveMediaForScore(currentScoreId, selectedMediaUri)
                }
            }
        }
        
        takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && currentPhotoUri != null && currentScoreId != -1L) {
                viewModel.saveMediaForScore(currentScoreId, currentPhotoUri!!)
            }
        }
        
        takeVideoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val videoUri = result.data?.data
                if (videoUri != null && currentScoreId != -1L) {
                    viewModel.saveMediaForScore(currentScoreId, videoUri)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupExportButton()
        observeScores()
        observeExportResult()
        observeMediaSaveResult()
    }

    private fun setupRecyclerView() {
        scoreAdapter = ScoreAdapter(this)
        binding.recyclerViewHistory.apply {
            adapter = scoreAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }
    
    private fun setupExportButton() {
        binding.buttonExport.setOnClickListener {
            viewModel.exportScoresToCsv()
        }
    }

    private fun observeScores() {
        viewModel.allScores.observe(viewLifecycleOwner) { scores ->
            scoreAdapter.submitList(scores)
        }
    }
    
    private fun observeExportResult() {
        viewModel.exportResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is ExportResult.Success -> {
                    shareExportedFile(result.uri)
                }
                is ExportResult.Error -> {
                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun observeMediaSaveResult() {
        viewModel.mediaSaveResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is MediaSaveResult.Success -> {
                    Toast.makeText(context, "Media attached successfully", Toast.LENGTH_SHORT).show()
                }
                is MediaSaveResult.Error -> {
                    Toast.makeText(context, "Failed to attach media: ${result.message}", Toast.LENGTH_LONG).show()
                }
                null -> { /* Ignore initial state */ }
            }
        }
    }
    
    private fun shareExportedFile(uri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        val chooserIntent = Intent.createChooser(shareIntent, "Share your Tetris scores")
        startActivity(chooserIntent)
    }
    
    // ScoreAdapter.MediaAttachmentListener implementation
    override fun onAddMediaClicked(scoreId: Long) {
        currentScoreId = scoreId
        showMediaSourceDialog()
    }
    
    override fun onMediaClicked(mediaUri: Uri, isVideo: Boolean) {
        if (isVideo) {
            // Start video player
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(mediaUri, "video/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(context, "No app available to play video", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Show image in full screen
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(mediaUri, "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(context, "No app available to view image", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showMediaSourceDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Media")
            .setItems(arrayOf("Take Photo", "Take Video", "Select from Gallery")) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndTakePhoto()
                    1 -> checkCameraPermissionAndTakeVideo()
                    2 -> checkStoragePermissionAndOpenGallery()
                }
            }
            .show()
    }
    
    private fun checkStoragePermissionAndOpenGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Modern Android doesn't need explicit permission for the gallery
            openGallery()
        } else {
            // Check for storage permission on older devices
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                openGallery()
            } else {
                // Request permission
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_CODE
                )
            }
        }
    }
    
    private fun checkCameraPermissionAndTakePhoto() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            takePhoto()
        } else {
            // Request permission
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        }
    }
    
    private fun checkCameraPermissionAndTakeVideo() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            takeVideo()
        } else {
            // Request permission
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        }
    }
    
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
        }
        pickMediaLauncher.launch(intent)
    }
    
    private fun takePhoto() {
        val photoFile = createImageFile()
        photoFile?.let {
            currentPhotoUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                it
            )
            takePhotoLauncher.launch(currentPhotoUri)
        }
    }
    
    private fun takeVideo() {
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)
        takeVideoLauncher.launch(intent)
    }
    
    private fun createImageFile(): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = requireContext().getExternalFilesDir(null)
        
        return try {
            File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            STORAGE_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGallery()
                } else {
                    Toast.makeText(context, "Storage permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            CAMERA_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    takePhoto()
                } else {
                    Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 