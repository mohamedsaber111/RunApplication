package com.example.runningapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.runningapp.R
import com.example.runningapp.db.Run
import com.example.runningapp.other.TrackingUtility
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.fragment_tracking.view.*
import kotlinx.android.synthetic.main.item_run.view.*
import java.text.SimpleDateFormat
import java.util.*

class RunAdapter : RecyclerView.Adapter<RunAdapter.RunViewHolder>() {

    inner class RunViewHolder(itemView : View):RecyclerView.ViewHolder(itemView)

    //create list differ that is tool takes two lists and calculate differences between that lists and returns them
    val diffCallback = object :DiffUtil.ItemCallback<Run>(){
        override fun areItemsTheSame(oldItem: Run, newItem: Run): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Run, newItem: Run): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }
    }
    //do this calculation in async in background
    val differ = AsyncListDiffer(this, diffCallback)
    //submitList to that list differ so that the list differ calculate the differences and update recyclerview
    fun submitList(list : List<Run>)= differ.submitList(list)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RunViewHolder {
        return RunViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_run,parent,false))
    }

    override fun onBindViewHolder(holder: RunViewHolder, position: Int) {
        val run = differ.currentList[position]

        holder.itemView.apply {
            //map Image
            Glide.with(this).load(run.img).into(ivRunImage)

            //Date
            val calender =Calendar.getInstance().apply {
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
            tvTime.text = TrackingUtility.getFormattedStopWatchTime(run.timeInMillis)

            //calories burned
            val caloriesBurned = "${run.caloriesBurned}kcal"
            tvCalories.text = caloriesBurned

        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }
}