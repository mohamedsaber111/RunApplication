package com.example.runningapp.other

import android.content.Context
import com.example.runningapp.db.Run
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import kotlinx.android.synthetic.main.marker_view.view.*
import java.text.SimpleDateFormat
import java.util.*

class CustomMarkerView(
    val runs: List<Run>,
    c: Context,
    layoutId: Int
) : MarkerView(c, layoutId) {

    override fun getOffset(): MPPointF {
        //MPPointF>> is point with an x and y value
        //position where that should show
        return MPPointF(-width/2f,-height.toFloat())
    }
    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        super.refreshContent(e, highlight)
        //set text of our textView of pop-up window
        if (e == null){
            return
        }
        //get currentId of that run we want to display
        val currentRunId = e.x.toInt()
        val run = runs[currentRunId]

        //Date
        val calender = Calendar.getInstance().apply {
            timeInMillis =run.timestamp
        }
        //display date by converted time in millis to actual date
        val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
        tvDate.text = dateFormat.format(calender.time)

        //avgSpeed
        val avgSpeed ="${run.avgSpeedInKmH}km/h"
        tvAvgSpeed.text = avgSpeed

        //distance in km
        val distanceInKm = "${run.distanceInMeters / 1000f}km"
        tvDistance.text = distanceInKm

        //duration of run
        tvDuration.text = TrackingUtility.getFormattedStopWatchTime(run.timeInMillis)

        //calories burned
        val caloriesBurned = "${run.caloriesBurned}kcal"
        tvCaloriesBurned.text = caloriesBurned

    }
}