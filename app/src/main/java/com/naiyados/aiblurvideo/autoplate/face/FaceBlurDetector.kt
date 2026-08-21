package com.naiyados.aiblurvideo.autoplate.face

import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.tasks.await

object FaceBlurDetector {
    private const val TAG = "FaceBlurDetector"

    private val detector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.12f)
            .build()
        FaceDetection.getClient(options)
    }

    suspend fun detectFaces(bitmap: Bitmap): List<RectF> {
        return try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val faces = detector.process(inputImage).await()
            faces.map { face ->
                val box = face.boundingBox
                RectF(
                    box.left.toFloat(),
                    box.top.toFloat(),
                    box.right.toFloat(),
                    box.bottom.toFloat()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Face detection error", e)
            emptyList()
        }
    }
}
