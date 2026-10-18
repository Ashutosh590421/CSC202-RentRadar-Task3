
package com.example.rentradar

import android.app.Dialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import java.io.File

class PhotoDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val path = requireArguments().getString("photo_path")
        val imageView = ImageView(requireContext()).apply {
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(16, 16, 16, 16)
        }

        if (path != null && File(path).exists()) {
            imageView.setImageBitmap(
                BitmapFactory.decodeFile(path)
            )
        }

        return AlertDialog.Builder(requireContext())
            .setTitle("Property Photo")
            .setView(imageView)
            .setPositiveButton("Close", null)
            .create()
    }

    companion object {
        fun newInstance(path: String): PhotoDialogFragment {
            return PhotoDialogFragment().apply {
                arguments = Bundle().apply {
                    putString("photo_path", path)
                }
            }
        }
    }
}
