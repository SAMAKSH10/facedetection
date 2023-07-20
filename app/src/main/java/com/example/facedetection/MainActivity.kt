package com.example.facedetection

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.SparseArray
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.face.Face
import com.google.android.gms.vision.face.FaceDetector
import java.io.FileNotFoundException
import java.io.InputStream

class MainActivity : AppCompatActivity() {
    private lateinit var photoPickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var imageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        imageView = findViewById(R.id.imageView)
        val pickPhotoButton: ImageView = findViewById(R.id.pickPhotoButton)
        pickPhotoButton.setOnClickListener {
            launchPhotoPicker()
        }
        photoPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                // Handle the selected image here, e.g., get the image URI
                val imageUri: Uri? = result.data?.data
                try {
                    setUpFaceDetector(imageUri)
                } catch (e: FileNotFoundException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun launchPhotoPicker() {
        val pickPhoto = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        photoPickerLauncher.launch(pickPhoto)
    }

    @Throws(FileNotFoundException::class)
    private fun setUpFaceDetector(selectedImage: Uri?) {
        val faceDetector = FaceDetector.Builder(applicationContext)
            .setTrackingEnabled(false)
            .build()

        if (!faceDetector.isOperational) {
            AlertDialog.Builder(this).setMessage("Could not set up the face detector!").show()
            return
        }

        selectedImage?.let {
            val ims: InputStream? = contentResolver.openInputStream(selectedImage)
            val options = BitmapFactory.Options()
            options.inMutable = true

            val myBitmap: Bitmap? = BitmapFactory.decodeStream(ims, null, options)

            myBitmap?.let {
                val frame = Frame.Builder().setBitmap(myBitmap).build()
                val faces: SparseArray<Face> = faceDetector.detect(frame)

                Log.d("TEST", "Num faces = " + faces.size())

                detectedResponse(myBitmap, faces)
            }
        }
    }

    private fun detectedResponse(myBitmap: Bitmap, faces: SparseArray<Face>) {
        val myRectPaint = Paint()
        myRectPaint.strokeWidth = 5f
        myRectPaint.color = Color.RED
        myRectPaint.style = Paint.Style.STROKE

        val tempBitmap = Bitmap.createBitmap(myBitmap.width, myBitmap.height, Bitmap.Config.RGB_565)
        val tempCanvas = Canvas(tempBitmap)
        tempCanvas.drawBitmap(myBitmap, 0f, 0f, null)

        for (i in 0 until faces.size()) {
            val thisFace = faces.valueAt(i)
            val x1 = thisFace.position.x
            val y1 = thisFace.position.y
            val x2 = x1 + thisFace.width
            val y2 = y1 + thisFace.height
            tempCanvas.drawRoundRect(RectF(x1, y1, x2, y2), 2f, 2f, myRectPaint)
        }

        imageView.setImageBitmap(tempBitmap)

        when {
            faces.size() < 1 -> AlertDialog.Builder(this).setMessage("Hey, there's no face in this photo. You think this is a joke?").show()
            faces.size() == 1 -> AlertDialog.Builder(this).setMessage("Okay. Thank you!").show()
            faces.size() > 1 -> AlertDialog.Builder(this).setMessage("Hey, there's more than one face in this photo. Bet why?").show()
        }
    }

}