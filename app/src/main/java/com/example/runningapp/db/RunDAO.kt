package com.example.runningapp.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RunDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRun(run: Run)

    @Delete
    suspend fun deleteRun(run: Run)

    //LiveData already asynchronous
    @Query("select * from running_table order by timestamp desc ")
    fun getAllRunsSortedByDate(): LiveData<List<Run>>

    @Query("select * from running_table order by timeInMillis desc ")
    fun getAllRunsSortedByTimeInMillis(): LiveData<List<Run>>

    @Query("select * from running_table order by caloriesBurned desc ")
    fun getAllRunsSortedByCaloriesBurned(): LiveData<List<Run>>

    @Query("select * from running_table order by avgSpeedInKmH desc ")
    fun getAllRunsSortedByAvgSpeed(): LiveData<List<Run>>

    @Query("select * from running_table order by distanceInMeters desc ")
    fun getAllRunsSortedByDistance(): LiveData<List<Run>>

    //for statistics

    @Query("select sum(timeInMillis) from running_table")
    fun getTotalTimeInMillis(): LiveData<Long>

    @Query("select sum(caloriesBurned) from running_table")
    fun getTotalCaloriesBurned(): LiveData<Int>

    @Query("select sum(distanceInMeters) from running_table")
    fun getTotalDistance(): LiveData<Int>

    @Query("select avg(avgSpeedInKmH) from running_table")
    fun getTotalAvgSpeed(): LiveData<Float>

}