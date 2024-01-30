package com.example.runningapp.ui.viewmodels

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.runningapp.db.Run
import com.example.runningapp.other.SortType
import com.example.runningapp.repository.MainRepository
import kotlinx.coroutines.launch

/*
annotated @ViewModelInject because in normal dagger when we add parameter in viewModel constructor we need to add viewModelFactory
 for this then the hilt can do this in background by add @ViewModelInject annotation
 */
class MainViewModel @ViewModelInject constructor(
    val mainRepository: MainRepository
) : ViewModel(){

    private val runsSortedByDate = mainRepository.getAllRunSortedByDate()
    private val runsSortedByDistance = mainRepository.getAllRunSortedByDistance()
    private val runsSortedByCaloriesBurned = mainRepository.getAllRunSortedByCaloriesBurned()
    private val runsSortedByTimeInMillis = mainRepository.getAllRunSortedByTimeInMillis()
    private val runsSortedByAvgSpeed = mainRepository.getAllRunSortedByAvgSpeed()

    //MediatorLiveData >> merge several livedata together and write custom logic for that
    val runs =MediatorLiveData<List<Run>>()

    // set as default sortType
    var sortType= SortType.DATE

    init {
        //TODO but RunsSortedBy Date,Distance,.. to runs MediatorLiveData(), observe RunsSortedBy.. changes to runs MediatorLiveData()

        //add observer as lambda fun so whenever something is emitted in our runsSortedByDate then this observer is called
        //this lambda call every time there is change in runsSortedByDate
        runs.addSource(runsSortedByDate){result ->
            if (sortType == SortType.DATE){
                //set value of our runsSortedByDate Livedata to value of runs
                result?.let { runs.value= it }
            }
        }

        runs.addSource(runsSortedByAvgSpeed){result ->
            if (sortType == SortType.AVG_SPEED){
                result?.let { runs.value= it }
            }
        }

        runs.addSource(runsSortedByCaloriesBurned){result ->
            if (sortType == SortType.CALORIES_BURNED){
                result?.let { runs.value= it }
            }
        }

        runs.addSource(runsSortedByDistance){result ->
            if (sortType == SortType.DISTANCE){
                result?.let { runs.value= it }
            }
        }

        runs.addSource(runsSortedByTimeInMillis){result ->
            if (sortType == SortType.RUNNING_TIME){
                result?.let { runs.value= it }
            }
        }
    }

    //observe sortType changes when user change between sort types
    fun sortRuns(sortType: SortType) = when(sortType){
        //set runsSortedByDate value if not null so update runs value
        SortType.DATE -> runsSortedByDate.value?.let { runs.value = it }
        SortType.RUNNING_TIME -> runsSortedByTimeInMillis.value?.let { runs.value = it }
        SortType.AVG_SPEED -> runsSortedByAvgSpeed.value?.let { runs.value = it }
        SortType.DISTANCE -> runsSortedByDistance.value?.let { runs.value = it }
        SortType.CALORIES_BURNED -> runsSortedByCaloriesBurned.value?.let { runs.value = it }

    }.also {
        this.sortType=sortType
    }

    fun insertRun(run: Run)=viewModelScope.launch {
        mainRepository.insertRun(run)
    }

    fun deleteRun(run: Run) = viewModelScope.launch {
        mainRepository.deleteRun(run)
    }
}