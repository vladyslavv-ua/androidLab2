package io.vladyslavv_ua.lab2

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.vladyslavv_ua.lab2.ui.theme.Lab2Theme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var photoFile: File
    private var photoUri: Uri? = null

    private val viewModel by viewModels<SelfieViewModel>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val cameraLauncher =
            registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                if (success) {
                    photoUri?.let { viewModel.setImageUri(it) }
                    print("good\n")
                }
            }

        fun createImageFile(): File {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
                photoFile = this
                photoUri = FileProvider.getUriForFile(
                    this@MainActivity,
                    "${packageName}.fileprovider",
                    this
                )
            }
        }

        photoUri = createImageFile().let {
            FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                it
            )
        }

        setContent {
            Lab2Theme {
                val stateUri by viewModel.imageUri.collectAsStateWithLifecycle()

                SelfieApp(stateUri) {
                    photoUri = createImageFile().let {
                        FileProvider.getUriForFile(
                            this,
                            "${packageName}.fileprovider",
                            it
                        )
                    }
                    cameraLauncher.launch(photoUri!!)
                }
            }
        }
    }
}

@Composable
fun SelfieApp(imageUri: Uri?, onTakePhoto: (() -> Unit)? = null) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    val context = LocalContext.current
    LaunchedEffect(imageUri) {
        imageUri?.let {
            bitmap = android.provider.MediaStore.Images.Media.getBitmap(
                context.contentResolver,
                it
            )
        }
    }


    Scaffold {
        Column(modifier = Modifier
            .padding(it)
            .padding(16.dp).fillMaxSize()) {
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Selfie",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                )
            }

            Spacer(modifier = Modifier.weight(.5f))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {

                Button(onClick = { onTakePhoto?.invoke() }) {
                    Text("Зробити селфі")
                }
                Button(onClick = {
                    imageUri?.let { uri ->
                        sendEmailWithAttachment(context, uri)
                    }
                }) {
                    Text("Надіслати селфі")
                }
            }
        }
    }
}


fun sendEmailWithAttachment(context: Context, uri: Uri) {
    val emailIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/image"
        putExtra(Intent.EXTRA_EMAIL, arrayOf("soloviova.d.v@op.edu.ua"))
        putExtra(Intent.EXTRA_SUBJECT, "ANDROID Мирошніченко Владислав")
        putExtra(
            Intent.EXTRA_TEXT,
            "У вкладенні моє селфі.\nРепозиторій: https://github.com/vladyslavv-ua/androidLab2"
        )
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(Intent.createChooser(emailIntent, "Надіслати email..."))
}

class SelfieViewModel : ViewModel() {
    private val _imageUri = MutableStateFlow<Uri?>(null)
    val imageUri: StateFlow<Uri?> = _imageUri.asStateFlow()

    fun setImageUri(uri: Uri) {
        _imageUri.value = uri
    }
}
