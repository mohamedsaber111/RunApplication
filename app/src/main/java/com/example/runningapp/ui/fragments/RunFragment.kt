package com.example.runningapp.ui.fragments

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.runningapp.R
import com.example.runningapp.adapter.RunAdapter
import com.example.runningapp.other.Constants.REQUEST_CODE_LOCATION_PERMISSIONS
import com.example.runningapp.other.SortType
import com.example.runningapp.other.TrackingUtility
import com.example.runningapp.ui.viewmodels.MainViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_run.*
import kotlinx.android.synthetic.main.fragment_run.view.*
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
//to inject viewModel inside fragment
@AndroidEntryPoint
//implement interface EasyPermissions.PermissionCallbacks
class RunFragment : Fragment(R.layout.fragment_run) ,EasyPermissions.PermissionCallbacks {

    //to inject viewModel from dagger
    private val viewModel : MainViewModel by viewModels()

    private lateinit var runAdapter: RunAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requestPermissions()

        setupRecyclerview()

        deleteRun()
        //1 we want to set current selection of ou spinner
        when(viewModel.sortType){
            //in String.xml
            SortType.DATE -> spFilter.setSelection(0)
            SortType.RUNNING_TIME -> spFilter.setSelection(1)
            SortType.DISTANCE -> spFilter.setSelection(2)
            SortType.AVG_SPEED -> spFilter.setSelection(3)
            SortType.CALORIES_BURNED -> spFilter.setSelection(4)
        }

        //2 response changes on our spinner if we select another item we wnt to call sort fun in viewModel
        spFilter.onItemSelectedListener =object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when(position){
                    0 -> viewModel.sortRuns(SortType.DATE)
                    1 -> viewModel.sortRuns(SortType.RUNNING_TIME)
                    2 -> viewModel.sortRuns(SortType.DISTANCE)
                    3 -> viewModel.sortRuns(SortType.AVG_SPEED)
                    4 -> viewModel.sortRuns(SortType.CALORIES_BURNED)
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        viewModel.runs.observe(viewLifecycleOwner, Observer { runs ->
            //or use runAdapter.differ.submitList() and delete submitList() fun from RunAdapter()
            runAdapter.submitList(runs)
        })

        fab.setOnClickListener {
            findNavController().navigate(R.id.action_runFragment_to_trackingFragment)
        }
    }


    private fun deleteRun(){
        //create anonymous class to delete item by touch and swipe it
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            //drag direction in recyclerview
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            //swap direction
            // direction we want to be able to swipe the items
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val run = runAdapter.differ.currentList[position]
                viewModel.deleteRun(run)

                Snackbar.make(requireView(), "Successfully deleted run ", Snackbar.LENGTH_LONG).apply {
                    setAction("Undo") {
                        //save run again that we just deleted
                        viewModel.insertRun(run)
                    }.show()
                }
            }
        }

        ItemTouchHelper(itemTouchHelperCallback).apply {
            // attach itemTouchHelperCallback to recyclerview
            attachToRecyclerView(rvRuns)
        }
    }
    private fun setupRecyclerview() =rvRuns.apply{
        runAdapter = RunAdapter()
        adapter = runAdapter
        layoutManager=LinearLayoutManager(requireContext())
    }

    //then request permission
    private fun requestPermissions(){
        //if user accepted those permissions
        if (TrackingUtility.hasLocationPermissions(requireContext())){
            return
        }
        //if didn't accept permissions before then check if version < Q to add background permission
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q){
            EasyPermissions.requestPermissions(
                this ,
                //if user denied permission show dialog in fragment with message and request again to accept location permission
                "You need to accept location permissions to use this app.",
                REQUEST_CODE_LOCATION_PERMISSIONS,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }else{
            EasyPermissions.requestPermissions(
                this ,
                "You need to accept location permissions to use this app.",
                REQUEST_CODE_LOCATION_PERMISSIONS,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )

        }
    }

    //these two fun override after implement interface of EasyPermission in this class
    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        //perms >> is all permissions that were denied
        if (EasyPermissions.somePermissionPermanentlyDenied(this,perms)){
            //show dialog that will lead user to the app settings where he can enable the permissions again if denied
            AppSettingsDialog.Builder(this).build().show()
        }else{
            //if he didn't permanently denied a permissions but he denied them for first time or it isn't permanent
            requestPermissions()
        }
    }
    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {}

    //finally add onRequestPermissionsResult to receive our permission
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        //this  >> this is fragment that receive our request
        EasyPermissions.onRequestPermissionsResult(requestCode,permissions,grantResults,this)
    }
}