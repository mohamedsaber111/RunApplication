package com.example.runningapp.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.example.runningapp.R
import com.example.runningapp.other.Constants.ACTION_PAUSE_SERVICE
import com.example.runningapp.other.Constants.ACTION_START_OR_RESUME_SERVICE
import com.example.runningapp.other.Constants.ACTION_STOP_SERVICE
import com.example.runningapp.other.Constants.FASTEST_UPDATE_INTERVAL
import com.example.runningapp.other.Constants.LOCATION_UPDATE_INTERVAL
import com.example.runningapp.other.Constants.NOTIFICATION_CHANNEL_ID
import com.example.runningapp.other.Constants.NOTIFICATION_CHANNEL_NAME
import com.example.runningapp.other.Constants.NOTIFICATION_ID
import com.example.runningapp.other.Constants.TIMER_UPDATE_INTERVAL
import com.example.runningapp.other.TrackingUtility
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

//MutableList<LatLng> just a polyline of coordinates on map (when start running and resume it create polyline)
typealias Polyline = MutableList<LatLng>
//MutableList<Polyline> when start service and resume it many time then we have several polyline is list of polyline on map
typealias Polylines = MutableList<Polyline>
/*
LifecycleService not service or intentService>> we need to observe from lifeData inside service class
and the observe fun of lifeData object need lifecycleOwner() so we use LifecycleService()
,use live data to tracking my location
*/
@AndroidEntryPoint
class TrackingService : LifecycleService() {

    var isFirstRun = true
    var serviceKilled =false
    //to set requestLocationUpdates and removeLocationUpdates if it isn't tracking
    //use to request location updates to update location whenever location changes
    @Inject
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    //current time run in seconds, show time to notification
    private val timeRunInSeconds =MutableLiveData<Long>()
    @Inject
    //to update notification >> post new notification with same id
    lateinit var baseNotificationBuilder : NotificationCompat.Builder
    //has different configuration than baseNotificationBuilder because we want to change
    // text of notification.builder(Timer) and to add action button
    lateinit var currentNotificationBuilder: NotificationCompat.Builder

    companion object {
        //in companion object so we can observe from outside

        //time millisecond here because we want to observe it in TrackingFragment
        val timeRunInMillis=MutableLiveData<Long>()

        //live data with our current tracking states just boolean if we are currently tracking user location or not
        val isTracking = MutableLiveData<Boolean>()

        //save a list of coordinates first MutableLiveData to observe on changes, hold all track location from specific run
        //single run in all track is list of coordinates
        val pathPoints = MutableLiveData<Polylines>()
    }

    private fun postInitialValues() {
        isTracking.postValue(false)
        //empty list of data because we don't have any coordinates in beginning
        pathPoints.postValue(mutableListOf())

        timeRunInSeconds.postValue(0L)
        timeRunInMillis.postValue(0L)
    }

    override fun onCreate() {
        super.onCreate()
        //set new currentNotificationBuilder equal baseNotificationBuilder
        currentNotificationBuilder = baseNotificationBuilder

        postInitialValues()
        fusedLocationProviderClient = FusedLocationProviderClient(this)

        //after updateLocationTracking() use observer to observe location
        //we use this as owner because we implement LifecycleService() not Service()
        isTracking.observe(this, Observer {
            updateLocationTracking(it)
            updateNotificationTrackingState(it)
        })
    }

    //communication from our activity to service
    //override method that whenever to send commend to our services so when send an intent with an action attached to service class
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //fun called whenever we send commend to our service (when send intent to this service)
        intent?.let {
            when (it.action) {
                //when action received from trackingFragment is ACTION_START_OR_RESUME_SERVICE then we want to start service with time
                ACTION_START_OR_RESUME_SERVICE -> {
                    if (isFirstRun) {
                        startForegroundService()
                        isFirstRun = false
                        //
                        serviceKilled=false
                    } else {
                        Timber.d("Resuming service... ")
                        startTimer()
                    }
                }
                //when action received from trackingFragment is ACTION_PAUSE_SERVICE then we want to pause service with time
                ACTION_PAUSE_SERVICE -> {
                    Timber.d("Paused service")
                    pauseService()
                }
                //when action received from trackingFragment is ACTION_STOP_SERVICE then we want to stop and kill service
                ACTION_STOP_SERVICE -> {
                    Timber.d("Stopped service")
                    killService()
                }
                //add sendCommandToService in TrackingFragment to add the commend and send it to service
            }
        }
        return super.onStartCommand(intent, flags, startId)

    }

    private var isTimerEnabled = false
    //time between when timer started to stop in specific run and when stop set lapTime to 0
    private var lapTime =0L
    //total time of run(lapTime added together)
    private var timeRun = 0L
    private var timeStarted = 0L
    // the last whole second value that has past in millis
    private var lastSecondTimestamp = 0L
    //fun to track actual time and trigger observers(livedata)
    private fun startTimer(){
        //whe start timer means that we start service or resume run when we need empty polyline to list
        addEmptyPolyline()
        isTracking.postValue(true)
        timeStarted = System.currentTimeMillis()
        isTimerEnabled= true
        /*want to track our current time and stop current time we do that with coroutine because we don't want to call
          our observers like all the time (very bad performance)instead we want to track the current time in coroutine
          and then delay that coroutine for few millis (not notice for human but notice for computer) */
        CoroutineScope(Dispatchers.Main).launch {
            while (isTracking.value!!){
                //we want to calculate current lapTime -> time difference between now and time started
                lapTime = System.currentTimeMillis() - timeStarted
                //update value of livedata by post new lapTime (which equal to total time )
                timeRunInMillis.postValue(timeRun + lapTime)
                //( lastSecondTimestamp  + 1000L) last whole second value that has past in millisecond + 1000millis (second),
                //if( 1550 millis >= 1000 millis)
                if (timeRunInMillis.value!! >= lastSecondTimestamp + 1000L){
                    //means new whole second has passed and should update our timeRunInSeconds livedata
                    timeRunInSeconds.postValue(timeRunInSeconds.value!! +1)
                    lastSecondTimestamp += 1000L
                }
                //delay update timer by coroutine
                //update livedata after every 50 millis
                delay(TIMER_UPDATE_INTERVAL)
            }
            //now outside while scope not tracking any more and we need last alpTime to timeRun
            timeRun+= lapTime
        }

    }

    private fun pauseService(){
        isTracking.postValue(false)
        isTimerEnabled= false
    }

    private fun killService(){
        serviceKilled=true
        isFirstRun=true
        pauseService()
        postInitialValues()
        //remove notification of foreground service
        stopForeground(true)
        //to stop whole service
        stopSelf()
    }

    //to update action of our notification
    private fun updateNotificationTrackingState(isTracking: Boolean){
        //update current notification with create action button either pause or resume
        val notificationActionText= if (isTracking) "Pause" else "Resume"
        val pendingIntent= if (isTracking){
            //pause server when click to action
            val pauseIntent = Intent(this,TrackingService::class.java).apply {
                action = ACTION_PAUSE_SERVICE
            }
            //getService() not getActivity()
            PendingIntent.getService(this,1,pauseIntent, FLAG_UPDATE_CURRENT)
        }else{
            val resumeIntent = Intent(this,TrackingService::class.java).apply {
                action= ACTION_START_OR_RESUME_SERVICE
            }
            PendingIntent.getService(this ,2,resumeIntent, FLAG_UPDATE_CURRENT)
        }

        //reference of notificationManager to display new notification that updated
        val notificationManager =getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // we want swap out action when clicked on it (pause and resume) and notification will update each second and we we will
        //attach an action to that so we want to remove all action before update notification then add new action
        currentNotificationBuilder.javaClass.getDeclaredField("mActions").apply {
            //allow to  modify
            isAccessible =true
            //set empty list of NotificationCompat to clear it
            //this way to remove actions before update it with new action
            set(currentNotificationBuilder,ArrayList<NotificationCompat.Action>())
        }

        //if serviceKilled the service get killed and notification will remove but observer still still get called one more time
        //this mean that still show new notification
        if(!serviceKilled){
            //add action pause or resume
            currentNotificationBuilder=baseNotificationBuilder
                .addAction(R.drawable.ic_pause_black_24dp,notificationActionText,pendingIntent)
            //we just need to post new notification with same id to update it
            notificationManager.notify(NOTIFICATION_ID,currentNotificationBuilder.build())
        }

    }
    //update our location tracking by fusedLocationProviderClient
    private fun updateLocationTracking(isTracking: Boolean) {
        if (isTracking) {
            if (TrackingUtility.hasLocationPermissions(this)) {
                //request location updates
                val request = LocationRequest().apply {
                    //get our location updates every 5 seconds
                    interval = LOCATION_UPDATE_INTERVAL
                    //never get more updates than this fastest location interval
                    //it get location update at least 2 seconds to save memory
                    fastestInterval = FASTEST_UPDATE_INTERVAL
                    priority = PRIORITY_HIGH_ACCURACY
                }

                fusedLocationProviderClient.requestLocationUpdates(
                    request,
                    locationCallback,
                    Looper.getMainLooper()
                )
            }
        } else {
            //if stop tracking remove location
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        }
    }
    //define locationCallback to get new location
    val locationCallback = object : LocationCallback() {
        //override fun that whenever retrieve a new location save in result variable and add it to location to end of our last polyline
        override fun onLocationResult(result: LocationResult?) {
            super.onLocationResult(result)
            //isTracking.value!! not equal to null
            if (isTracking.value!!) {
                //add coordinates
                result?.locations?.let { locations ->
                    for (location in locations) {
                        //add new location as last location
                        addPathPoint(location)
                        Timber.d("NEW LOCATION: ${location.latitude},${location.longitude} ")
                    }
                }
            }
        }
    }

    //add coordinates to last polyline on our polyline list
    private fun addPathPoint(location: Location?) {
        location?.let {
            val position = LatLng(location.latitude, location.longitude)
            //add this position to last polyline of our polylines list
            pathPoints.value?.apply {
                //if we add new coordinates in pathPoints
                last().add(position)
                pathPoints.postValue(this)
            }
        }
    }

    //use this fun when pause tracking and resume again we need to add empty list before add coordinates
    //pathPoints.value >> value of liveData as empty list (hole of polylines)
    private fun addEmptyPolyline() = pathPoints.value?.apply {
        // add empty list to pathPoint
        add(mutableListOf())
        //this refers to current polylines object (set pathPoint empty list)
        pathPoints.postValue(this)
    } ?:
    //if pathPoints.value is null and that case we won't execute above block so in this case we won't add empty polyline
    pathPoints.postValue(mutableListOf(mutableListOf()))

    //foreground service must come with notification to make sure it can't be killed by android system when need memory,
    //benefit of foregroundService when the app in background and treat service as activity that in foreground
    //user should be active while service track his location
    private fun startForegroundService() {

        //when we start service we want to add first polyline
        //addEmptyPolyline()
        startTimer()
        //add is tracking true
        isTracking.postValue(true)


        //notificationManager is SystemService we need a reference so we can call that createNotificationChannel()
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        //create channel of on android O or above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager)
        }
        //create our notification
        startForeground(NOTIFICATION_ID, baseNotificationBuilder.build())

        //after add action on notification then update time
        //observe time in notification when every second passes not 50 millis
        timeRunInSeconds.observe(this, Observer {
            if (!serviceKilled) {
                //if serviceKilled the service get killed and notification will remove but observer still still get called one more time
                //this mean that still show new notification
                val notification = currentNotificationBuilder
                    //time come with second and we * 1000 to convert to millis because getFormattedStopWatchTime() use time in millis
                        //and time received it by second because second parameter = false initially
                    .setContentText(TrackingUtility.getFormattedStopWatchTime(it * 1000))
                //we just need to post new notification with same id to update it
                notificationManager.notify(NOTIFICATION_ID, notification.build())
            }
        })
    }


    // foreground must use notification so we create notification channel
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            //set low because notification send every second and if set it anything greater than low,
            // notification will always come with sound
            IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }

}