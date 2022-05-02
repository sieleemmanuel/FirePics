package com.siele.firebaseapp.utils

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment.Companion.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController

object SetUpToolbar {
    fun setUpToolbar(toolbar: Toolbar,fragment:Fragment, activity: AppCompatActivity) {
        val appBarConfiguration = AppBarConfiguration(findNavController(fragment).graph)
        toolbar.setupWithNavController(findNavController(fragment), appBarConfiguration)
        activity.setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
           it.findNavController().navigateUp(appBarConfiguration)
        }
    }
}