package com.example.runningapp.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.runningapp.R
import com.example.runningapp.other.Constants.ACTION_SHOW_TRACKING_FRAGMENT
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //if activity destroyed and service still running then send pendingIntent in onCreate()
        navigateToTrackingFragmentIfNeeded(intent)

        setSupportActionBar(toolbar)
        //setup our bottomNavigationView and connect it with navigation component
        bottomNavigationView.setupWithNavController(navHostFragment.findNavController())
        //when press bottom icon twice or more it reload page every time then this line to prevent this
        bottomNavigationView.setOnNavigationItemReselectedListener { /* NO_OP*/ }

        //use addOnDestinationChangedListener to hide bottomNavigationView from setupFragment, trackingFragment
        navHostFragment.findNavController()
            .addOnDestinationChangedListener { _, destination, _ ->
                when(destination.id){
                    R.id.settingsFragment , R.id.runFragment , R.id.statisticsFragment ->
                        bottomNavigationView.visibility= View.VISIBLE

                    else -> bottomNavigationView.visibility = View.GONE
                }
            }


    }

    //if activity wasn't destroy and we use pendingIntent to launch it
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        navigateToTrackingFragmentIfNeeded(intent)
    }

    private fun navigateToTrackingFragmentIfNeeded(intent: Intent?){
        //set the action of intent to ACTION_SHOW_TRACKING_FRAGMENT and navigate to trackingFragment
        if (intent?.action == ACTION_SHOW_TRACKING_FRAGMENT){
            /*
            *in nav_graph we set  app:launchSingleTop="true"
            * so when press to notification to show our tracking we want make sure we don't launch new instance in activity because
            * if that happen then new activity doesn't have data in old activity
             */
            navHostFragment.findNavController().navigate(R.id.action_global_trackingFragment)
        }
    }

}
