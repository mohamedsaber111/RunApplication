package com.example.runningapp.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.runningapp.R
import com.example.runningapp.db.Run
import com.example.runningapp.other.Constants.ACTION_PAUSE_SERVICE
import com.example.runningapp.other.Constants.ACTION_START_OR_RESUME_SERVICE
import com.example.runningapp.other.Constants.ACTION_STOP_SERVICE
import com.example.runningapp.other.Constants.MAP_ZOOM
import com.example.runningapp.other.Constants.POLYLINE_COLOR
import com.example.runningapp.other.Constants.POLYLINE_WIDTH
import com.example.runningapp.other.TrackingUtility
import com.example.runningapp.services.Polyline
import com.example.runningapp.services.TrackingService
import com.example.runningapp.ui.viewmodels.MainViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_tracking.*
import java.util.Calendar
import javax.inject.Inject
import kotlin.math.round
const val CANCEL_TRACKING_DIALOG_TAG = "CancelDialog"
@AndroidEntryPoint
class TrackingFragment : Fragment(R.layout.fragment_tracking) {

    private val viewModel: MainViewModel by viewModels()

    private var isTracking = false
    private var pathPoints = mutableListOf<Polyline>()

    private var map: GoogleMap? = null

    //current time of run how long it was
    private var currentTimeInMillis =0L

    private var menu: Menu?= null

    //@set:Inject because weight is primitive data type
    @set:Inject
    var weight :Float =80f

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //create this fun to call setHasOptionsMenu() to show this menu only in this fragment
        setHasOptionsMenu(true)
        return super.onCreateView(inflater, container, savedInstanceState)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //we create lifecycle of map use mapView manually such (onCreate, onStop, onResume,..)
        //we must add lifecycle for mapView to work

        mapView.onCreate(savedInstanceState)

        //when rotate device the dialog was destroy and yasListener=null so we add constant(CANCEL_TRACKING_DIALOG_TAG)
        // with savedInstanceState that has info if rotated device
        if (savedInstanceState != null){
            val cancelTrackingDialog = parentFragmentManager.findFragmentByTag(
                CANCEL_TRACKING_DIALOG_TAG) as CancelTrackingDialog?

            cancelTrackingDialog?.setYasListener {
                stopRun()
            }
        }

        btnToggleRun.setOnClickListener {
            toggleRun()
        }

        btnFinishRun.setOnClickListener {
            //save data in database with our run
            zoomToSeeWholeTrack()
            endRunAndSaveToDb()
        }
        //we use mp view
        mapView.getMapAsync {

            //view that we display the map >> to load our map
            map = it
            // draw all polyline in map every time create
            addAllPolylinesOnMap()
        }

        subscribeToObservers()

    }

    //after add toggleRun() then observe user
    private fun subscribeToObservers() {
        TrackingService.isTracking.observe(viewLifecycleOwner, Observer {
            updateTracking(it)
        })

        TrackingService.pathPoints.observe(viewLifecycleOwner, Observer {
            pathPoints = it
            //get latest polyline because when observer called we know there is now location tracked come from service
            //and draw new line
            addLatestPolyline()
            //move camera to new location
            moveCameraToUser()
        })
        //after we add timer in service class and TrackingUtility -> observe time
        TrackingService.timeRunInMillis.observe(viewLifecycleOwner, Observer {
            currentTimeInMillis =it
            val formattedTime = TrackingUtility.getFormattedStopWatchTime(currentTimeInMillis,true)
            tvTimer.text= formattedTime

        })
    }
    //add toggleRun button
    private fun toggleRun() {
        if (isTracking) {
            //if user tracking then we want to pause service
            sendCommandToService(ACTION_PAUSE_SERVICE)
            //show action to cancel run
            menu?.getItem(0)?.isVisible = true

        } else {
            //if isn't tracking then user want to start tracking (start service or resume it)
            sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
        }
    }

    //observe data from our service and react to those changes
    private fun updateTracking(isTracking: Boolean) {
        //update UI regarding tracking state here
        this.isTracking = isTracking
        if (!isTracking && currentTimeInMillis > 0L) {
            //on resume show user if start again or finish run
            btnToggleRun.text = "Resume run"
            //when click to start then show finish button
            btnFinishRun.visibility = View.VISIBLE
        } else if (isTracking) {
            btnToggleRun.text = "Stop"
            menu?.getItem(0)?.isVisible = true
            btnFinishRun.visibility = View.GONE
        }
    }


    private fun moveCameraToUser() {
        //if there is one coordinate inside pathPoints
        if (pathPoints.isNotEmpty() && pathPoints.last().isNotEmpty()) {
            map?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    //last coordinate inside pathPoint list
                    pathPoints.last().last(),
                    MAP_ZOOM
                )
            )
        }
    }

    //make screenshot of our map to save it in database
    private fun zoomToSeeWholeTrack(){

        //LatLngBounds object that but all our coordinates and google map will do the rest and give us final bounds
        // to which we want to zoom
        val bounds =LatLngBounds.builder()
        //need two loops one for pathPoint list it's all polylines
        for (polyline in pathPoints){
            //two loop for position in polyline
            for (position in polyline){
                bounds.include(position)
            }
        }

        //then move camera to whole pathPoints
        map?.moveCamera(
            CameraUpdateFactory.newLatLngBounds(
                bounds.build(),
                mapView.width,
                mapView.height,
                //make padding that push little the running track in middle of app
                (mapView.height * 0.05f).toInt()
            )
        )
    }
    private fun endRunAndSaveToDb(){
        //make screenshot of our map and save run with all values in database
        //after we get bounds to get pic as bitmap to see whole running track
        map?.snapshot { bitmap ->
            //we want to calculate total distance of our run so in our single polyline we need to calculate distance
            // we do it in TrackingUtility
            var distanceInMeter =0
            //pass through pathPoint List
            for (polyline in pathPoints){
                distanceInMeter += TrackingUtility.calculatePolylineLength(polyline).toInt()
            }

            //calculate avg speed by km/h  for user
            // we want to display a single decimal place for avgSpeed so that the text doesn't get too long in our recyclerview,
            //so we use round fun to do that round(dis/time *10)/10f we /10f to get decimal places
            val avgSpeed = round((distanceInMeter / 1000f) / (currentTimeInMillis/1000f /60 /60) * 10)/10f

            //get current date timestamp
            val dateTimestamp = Calendar.getInstance().timeInMillis

            //get calories burned
            val caloriesBurned =((distanceInMeter / 1000f) * weight ).toInt()

            val run =Run(bitmap,dateTimestamp,avgSpeed,distanceInMeter, currentTimeInMillis,caloriesBurned)

            viewModel.insertRun(run)

            Snackbar.make(
                //because we inside TrackingFragment and when we called endRunAndSaveToDb() we want to navigate back to runFragment
                requireActivity().findViewById(R.id.rootView),
                "Run saved successfully",
                Snackbar.LENGTH_LONG
            ).show()
            //stop our service
            stopRun()
        }
    }


    //draw allPolylines on map
    private fun addAllPolylinesOnMap() {
        //loop for pathPoints
        for (polyline in pathPoints) {
            val polylineOptions = PolylineOptions()
                .color(POLYLINE_COLOR)
                .width(POLYLINE_WIDTH)
                .addAll(polyline)

            map?.addPolyline(polylineOptions)
        }
    }


    //draw polyline we want to connect the last two locations in polyline list when get new location
    //whenever we observe change we want to connect lastPoint of list with new lastPoint
    private fun addLatestPolyline() {
        //if there are more than one element in last list of our pathPoints(last()-> last polyline or current polyline we are tracking)
        //>1 this means there are at least two element
        if (pathPoints.isNotEmpty() && pathPoints.last().size > 1) {
            //second last element in last list or in last polyline
            val preLastLatLng = pathPoints.last()[pathPoints.last().size - 2]
            //pathPoints.last() refer to polyline we current tracking, another last() refer to last coordinate of polyline
            val lastLatLng = pathPoints.last().last()
            //how polyline look like so we define color and width
            val polylineOptions = PolylineOptions()
                .color(POLYLINE_COLOR)
                .width(POLYLINE_WIDTH)
                .add(preLastLatLng)
                .add(lastLatLng)

            map?.addPolyline(polylineOptions)
        }
    }

    //fun to send comment to service
    private fun sendCommandToService(action: String) =
        //send intent to TrackingService class we created
        Intent(requireContext(), TrackingService::class.java).also {
            //action of service
            it.action = action
            requireContext().startService(it)
            //then we set service in Manifest
        }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.toolbar_tracking_menu,menu)
        this.menu = menu
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        //to change visibility of our menu item(cancel run)
        if (currentTimeInMillis > 0 ){
            //visible the cancel run item whe start tracking because if user didn't start then there are on thing to cancel
            this.menu?.getItem(0)?.isVisible = true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId ){
            R.id.miCancelTracking ->
            showCancelTrackingDialog()
        }
        return super.onOptionsItemSelected(item)

    }
    //create fun to user to show dialog to confirm cancellation
    private fun showCancelTrackingDialog(){
        CancelTrackingDialog().apply {
            setYasListener {
                stopRun()
            }
        }.show(parentFragmentManager, CANCEL_TRACKING_DIALOG_TAG)
    }
    private fun stopRun(){
        tvTimer.text = "00:00:00:00"
        sendCommandToService(ACTION_STOP_SERVICE)

        findNavController().navigate(R.id.action_trackingFragment_to_runFragment)
    }


    override fun onResume() {
        super.onResume()
        //omResume for map use mapView
        mapView?.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    //help us to cache map so that we don't need to load it every time when open the device
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }


}