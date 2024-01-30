package com.example.runningapp.other

import android.Manifest
import android.content.Context
import android.location.Location
import android.os.Build
import com.example.runningapp.services.Polyline
import pub.devrel.easypermissions.EasyPermissions
import java.util.concurrent.TimeUnit

object TrackingUtility {

    //first check location permissions
    fun hasLocationPermissions(context: Context) =
        //if not running on android Q so we don't need request background location permission
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ){
            EasyPermissions.hasPermissions(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }else{
            //if android running above Q then we need background permission
            EasyPermissions.hasPermissions(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )

        }

    //calculate the distance for single polyline
    fun calculatePolylineLength(polyline: Polyline):Float{
        var distance =0f
        //need to loop through position of our polyline
        for (i in 0.. polyline.size -2){
            //polyline.size-2>> because we use two position for compare and last position =size-2
            val position1 = polyline[i]
            val position2 = polyline[i + 1]
            //result of distance between two position
            val result = FloatArray(1)
            //loop through our polyline and compare each coordinates that belong together
            //calculate distance between two coordinate pos1 , pos2
            Location.distanceBetween(
                position1.latitude,position1.longitude,
                position2.latitude,position2.longitude,
                result
            )
            distance +=result[0]

        }
        return distance
    }

    //includeMillis : Boolean >> toggle if we want to show milliseconds in timer or not because in tracking fragment we want
    //but notification we don't want
    fun getFormattedStopWatchTime(millis : Long, includeMillis : Boolean= false):String{
        //copy of millisecond
        var milliseconds =millis
        //how many hours we can get from those millis
        //convert milliseconds to hours
        val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
        //remaining millisecond from hours >>convert hours to millisecond to display that in string we return in fun
        milliseconds-= TimeUnit.HOURS.toMillis(hours)
        //get minutes calculated by remaining milliseconds from hours
        val minutes =TimeUnit.MILLISECONDS.toMinutes(milliseconds)
        //remaining millis from minutes
        milliseconds-= TimeUnit.MINUTES.toMillis(minutes)
        //get second calculated by remaining milliseconds from minutes
        val second = TimeUnit.MILLISECONDS.toSeconds(milliseconds)
        //in notification
        if (!includeMillis){
            //set 09,08,07,06 as hour and if hour>9 write 10 ,11,12
            return "${if(hours<10) "0" else ""}$hours:" +
                    "${if(minutes<10) "0" else ""}$minutes:" +
                    "${if(second<10) "0" else ""}$second"
        }
        //if we want millis
        milliseconds-=TimeUnit.SECONDS.toMillis(second)
        //we want to have two digit number of millis not 3
        milliseconds/=10
        //in tracking fragment
        return "${if(hours<10) "0" else ""}$hours:" +
                "${if(minutes<10) "0" else ""}$minutes:" +
                "${if(second<10) "0" else ""}$second:" +
                "${if(milliseconds<10) "0" else ""}$milliseconds"

    }

}