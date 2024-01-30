package com.example.runningapp.ui.fragments

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.example.runningapp.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
//implement DialogFragment() class then override fun onCreateDialog()
class CancelTrackingDialog :DialogFragment() {

    private var yesListener : (() -> Unit)? =null

    fun setYasListener(listener : () -> Unit){
        yesListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext() , R.style.AlertDialogTheme)
            .setTitle("Cancel the run? ")
            .setMessage("Are you sure to cancel the current run?")
            .setIcon(R.drawable.ic_delete)
                //set lambda fun must has two parameter but we didn't need them
            .setPositiveButton("Yes"){ _,_ ->
                yesListener?.let {yes ->
                    yes()
                }
            }
            .setNegativeButton("No"){ dialogInterface, _ ->
                dialogInterface.cancel()
            }
            .create()

    }
}