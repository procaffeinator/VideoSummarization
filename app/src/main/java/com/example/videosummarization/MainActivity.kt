package com.example.videosummarization

import android.app.ProgressDialog
import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.videosummarization.ui.theme.VideoSummarizationTheme
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask


class MainActivity : ComponentActivity() {
    private lateinit var progressDialog: ProgressDialog
    private var videouri: Uri? = null
    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            videouri = it
            progressDialog.setTitle("Uploading...")
            progressDialog.show()
            uploadVideo()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        val mAuth: FirebaseAuth = FirebaseAuth.getInstance()
        mAuth.signInAnonymously()
            .addOnSuccessListener(this) {
                // do your stuff
                Toast.makeText(
                    this@MainActivity,
                    "Connected to Firebase",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener(this
            ) { exception ->
                Toast.makeText(
                    this@MainActivity,
                    "Failed ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        setContent {
            VideoSummarizationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainContent()
                }
            }
        }
    }
    @Composable
    fun MainContent() {
        var showVideoView by remember { mutableStateOf(false) }
        var showTranscriptView by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!showVideoView && !showTranscriptView) {
                // Initial state with "Summarize your Lecture Video" and "Upload Video" button
                Text("Summarize your Lecture Video", fontSize = 24.sp, modifier = Modifier.padding(bottom = 16.dp))
                Button(onClick = {
                    // Code for showing progressDialog while uploading
                    progressDialog = ProgressDialog(this@MainActivity)
                    getContent.launch("video/*")
                    showTranscriptView=true
                }) {
                    Text("Upload Video")
                }
            }
//
//            if (showVideoView) {
//                // State after clicking "Upload Video" button, showing "Placeholder for Video"
//                Text("Placeholder for Video", fontSize = 24.sp, modifier = Modifier.padding(bottom = 16.dp))
//                Button(onClick = {
//                    showTranscriptView = true
//                    showVideoView = false
//                }) {
//                    Text("Show Video Transcript")
//                }
//            }

            if (showTranscriptView) {
                // State after clicking "Show Video Transcript" button, showing "Video Transcript"
                // If you want to show the "Upload Video" button again

                Text("Video Transcript", fontSize = 18.sp, modifier = Modifier.padding(top = 16.dp))
                Text("lorem ipsum", fontSize = 12.sp, modifier = Modifier.padding(top = 20.dp))
                Button(onClick = {
                    showTranscriptView = false
                    showVideoView = true
                }) {
                    Text("Upload Another Video")
                }
            }
        }
    }

    private fun getFileType(videouri: Uri?): String? {
        val r: ContentResolver = contentResolver
        // get the file type, in this case, it's mp4
        val mimeTypeMap: MimeTypeMap = MimeTypeMap.getSingleton()
        return mimeTypeMap.getExtensionFromMimeType(videouri?.let { r.getType(it) })
    }

    private fun uploadVideo() {
        videouri?.let {
            // save the selected video in Firebase storage
            val reference: StorageReference = FirebaseStorage.getInstance().getReference("Files/${System.currentTimeMillis()}.${getFileType(videouri)}")
            reference.putFile(it).addOnSuccessListener { taskSnapshot: UploadTask.TaskSnapshot ->
                val uriTask: Task<Uri> = taskSnapshot.storage.downloadUrl
                while (!uriTask.isSuccessful);
                // get the link of the video
                val downloadUri: String = uriTask.result.toString()
                val reference1: DatabaseReference = FirebaseDatabase.getInstance().getReference("Video")
                val map: HashMap<String, String> = HashMap()
                map["videolink"] = downloadUri
                reference1.child("" + System.currentTimeMillis()).setValue(map)
                // Video uploaded successfully
                // Dismiss dialog
                progressDialog.dismiss()
                Toast.makeText(this@MainActivity, "Video Uploaded!!", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { e: Exception ->
                // Error, Image not uploaded
                progressDialog.dismiss()
                Toast.makeText(this@MainActivity, "Failed ${e.message}", Toast.LENGTH_SHORT).show()
            }.addOnProgressListener { taskSnapshot: UploadTask.TaskSnapshot ->
                // Progress Listener for loading
                // percentage on the dialog box
                val progress: Double = 100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount
                progressDialog.setMessage("Uploaded " + progress.toInt() + "%")
            }
        }
    }

}

