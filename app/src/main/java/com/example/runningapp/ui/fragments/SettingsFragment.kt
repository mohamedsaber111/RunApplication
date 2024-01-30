package com.example.runningapp.ui.fragments

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.runningapp.R
import com.example.runningapp.other.Constants.KEY_NAME
import com.example.runningapp.other.Constants.KEY_WEIGHT
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_settings.*
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment(R.layout.fragment_settings) {

    @Inject
    lateinit var shardPre: SharedPreferences

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //load name and weight and display current values in those editText fields
        loadFieldsFromSharedPref()

        btnApplyChanges.setOnClickListener {
            val success = applyChangeToSharedPreference()
            if (success) {
                Snackbar.make(view, "Saved Changes", Snackbar.LENGTH_SHORT).show()
            } else {
                Snackbar.make(view, "Please fill out name and weight", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    //get data from sharedPref
    private fun loadFieldsFromSharedPref() {
        val name = shardPre.getString(KEY_NAME, "")
        val weight = shardPre.getFloat(KEY_WEIGHT, 80f)

        editTextName.setText(name)
        editTextWeight.setText(weight.toString())

    }

    //save data in SharedPreference
    private fun applyChangeToSharedPreference(): Boolean {
        val nameText = editTextName.text.toString()
        val weightText = editTextWeight.text.toString()

        if (nameText.isEmpty() || weightText.isEmpty()) {
            return false
        }
        shardPre.edit()
            .putString(KEY_NAME, nameText)
            .putFloat(KEY_WEIGHT, weightText.toFloat())
            .apply()

        val toolbarText = "Lets go, $nameText"
        requireActivity().tvToolbarTitle.text = toolbarText
        return true
    }
}