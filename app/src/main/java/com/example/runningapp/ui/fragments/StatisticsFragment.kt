package com.example.runningapp.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.runningapp.R
import com.example.runningapp.other.CustomMarkerView
import com.example.runningapp.other.TrackingUtility
import com.example.runningapp.ui.viewmodels.StatisticsViewModel
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_statistics.*
import kotlin.math.round

@AndroidEntryPoint
class StatisticsFragment : Fragment(R.layout.fragment_statistics) {

    private val viewModel : StatisticsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        subscribeToObservers()

        setupBarChart()
    }

    private fun setupBarChart(){
        barChart.xAxis.apply {
            //set x axis in bottom
            position = XAxis.XAxisPosition.BOTTOM
            //disable the labels of x axis so no values are there
            setDrawLabels(false)
            axisLineColor = Color.WHITE
            textColor = Color.WHITE
            //disable grid in our graph and if we want it let it enabled
            setDrawGridLines(false)
        }
        barChart.axisLeft.apply {
            axisLineColor = Color.WHITE
            textColor = Color.WHITE
            setDrawGridLines(false)
        }
        barChart.axisRight.apply {
            axisLineColor= Color.WHITE
            textColor =Color.WHITE
            setDrawGridLines(false)
        }

        barChart.apply {
            description.text = "Avg Speed Over Time"
            legend.isEnabled = false
        }
    }

    //display and observe totalTime, totalDistance, totalAvgSpeed, totalCaloriesBurned and observe and runs in barChart
    private fun subscribeToObservers(){
        viewModel.totalTimeRun.observe(viewLifecycleOwner, Observer {totalTime ->
            totalTime?.let {
                //but time with specific format then put it in textView
                val totalTimeRun = TrackingUtility.getFormattedStopWatchTime(it)
                tvTotalTime.text=totalTimeRun
            }

        })

        viewModel.totalDistance.observe(viewLifecycleOwner, Observer { totalDistance ->
            totalDistance?.let {
                val km = it / 1000f
                val totalDistanceDecimal = round(km * 10f) /10f
                val totalDistanceString = "${totalDistanceDecimal}km"

                tvTotalDistance.text= totalDistanceString
            }
        })

        viewModel.totalAvgSpeed.observe(viewLifecycleOwner, Observer { totalAvgSpeed ->
            totalAvgSpeed?.let {
                //we need only one decimal place
                val avgSpeed = round(it * 10f ) /10f
                val avgSpeedString = "${avgSpeed}km/h"
                tvAverageSpeed.text =avgSpeedString
            }

        })

        viewModel.totalCaloriesBurned.observe(viewLifecycleOwner, Observer { totalCaloriesBurned->
            totalCaloriesBurned?.let {
                val totalCalories ="${it}kcal"
                tvTotalCalories.text =totalCalories
            }

        })

        //but runs in barChart
        viewModel.runsSortedByDate.observe(viewLifecycleOwner, Observer {runs->

            runs?.let {
                //check if runs not equal to null we need to create list of BarEntry
                //BarEntry contain the x value and y value of that specific bar
                //x value has index of run in the list, y value we will use avgSpeed, BerEntry(x,y)
                //the whole list of barEntry will fill up the chart
                val allAvgSpeeds =it.indices.map {index-> BarEntry(index.toFloat(),it[index].avgSpeedInKmH) }
                //set bar data set all entries
                val barDataSet = BarDataSet(allAvgSpeeds,"Avg Speed Over Time").apply {
                    valueTextColor = Color.WHITE
                    color = ContextCompat.getColor(requireContext(), R.color.colorAccent)

                }
                barChart.data =  BarData(barDataSet)
                //when click on those bars in specific bar then display information about Specific run
                //reversed() because list of run sorted desc so the latest one is first in list
                barChart.marker = CustomMarkerView(it, requireContext(),R.layout.marker_view)
                //we just want to update barChart with changes
                barChart.invalidate()
            }

        })
    }

}