package com.example.documentapp

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.documentapp.ui.theme.DocumentAppTheme
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val options = GmsDocumentScannerOptions.Builder()
            .setScannerMode(SCANNER_MODE_FULL)
            .setGalleryImportAllowed(true)
            .setPageLimit(5).setResultFormats(RESULT_FORMAT_PDF, RESULT_FORMAT_JPEG)
            .build()

        val scanner = GmsDocumentScanning.getClient(options)

        setContent {
            DocumentAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
                    var pdfUri by remember { mutableStateOf<Uri?>(null) }
                    val scannerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartIntentSenderForResult(),
                        onResult = {
                            if (it.resultCode == RESULT_OK){
                                val result = GmsDocumentScanningResult.fromActivityResultIntent(it.data)
                                imageUris = result?.pages?.map { it.imageUri } ?: emptyList()

                                result?.pdf?.let {pdf->
                                    pdfUri = pdf.uri
                                    savePdfToDownloads(pdf.uri)
                                }
                            }

                            Toast.makeText(
                                this,
                                "Success!! pdf saved into Download/ folder",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        imageUris.forEach { uri->
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            scanner.getStartScanIntent(this@MainActivity)
                                .addOnSuccessListener {
                                    scannerLauncher.launch(
                                        IntentSenderRequest.Builder(it).build()
                                    )

                                }.addOnFailureListener {
                                    Toast.makeText(
                                        applicationContext,
                                        it.message,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }) {
                            Text(text = "Scan PDF")
                        }

                        Button(onClick = { openDownloadsFolder() }) {
                            Text(text = "Open Downloads Folder")
                        }
                    }
                }
            }
        }
    }

    private fun savePdfToDownloads(uri: Uri){
        val inputStream = contentResolver.openInputStream(uri)
        val timeStamp = System.currentTimeMillis()
        val fileName = "$timeStamp.pdf"
        val outputStream = FileOutputStream(getFileInDownloads(fileName))
        inputStream?.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
    }
    private fun openDownloadsFolder() {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val fileUri = FileProvider.getUriForFile(this, "${this.packageName}.fileprovider", downloadsDir)
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(fileUri, "*/*") // Любой тип файлов
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Предоставить разрешение на чтение
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Обработка случая, когда нет приложения для обработки Intent
            Toast.makeText(this, "Нет приложения для открытия проводника", Toast.LENGTH_SHORT).show()
        }
    }


    private fun getFileInDownloads(fileName: String): File {
        val downloadsDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return File(downloadsDir, fileName)
    }
}
