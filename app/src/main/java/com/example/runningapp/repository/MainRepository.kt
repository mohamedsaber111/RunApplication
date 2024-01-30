package com.example.runningapp.repository

import com.example.runningapp.db.Run
import com.example.runningapp.db.RunDAO
import javax.inject.Inject

class MainRepository @Inject constructor(
    val runDao :RunDAO
){

    suspend fun insertRun(run: Run) =runDao.insertRun(run)

    suspend fun deleteRun(run: Run) =runDao.deleteRun(run)

    fun getAllRunSortedByDate()=runDao.getAllRunsSortedByDate()

    fun getAllRunSortedByDistance()=runDao.getAllRunsSortedByDistance()

    fun getAllRunSortedByTimeInMillis()=runDao.getAllRunsSortedByTimeInMillis()

    fun getAllRunSortedByAvgSpeed()=runDao.getAllRunsSortedByAvgSpeed()

    fun getAllRunSortedByCaloriesBurned()=runDao.getAllRunsSortedByCaloriesBurned()

    //statistics

    fun getTotalAvgSpeed() = runDao.getTotalAvgSpeed()

    fun getTotalDistance() = runDao.getTotalDistance()

    fun getTotalCaloriesBurned() = runDao.getTotalCaloriesBurned()

    fun getTotalTimeInMillis() = runDao.getTotalTimeInMillis()
}