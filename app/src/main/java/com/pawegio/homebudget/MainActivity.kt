package com.pawegio.homebudget

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.pawegio.homebudget.util.CurrentActivityObserver
import kotlinx.android.synthetic.main.main_activity.*
import org.koin.android.viewmodel.ext.android.getViewModel

class MainActivity : AppCompatActivity(R.layout.main_activity) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(CurrentActivityObserver)
        appNavController = navHostFragment.findNavController()
        lifecycle.addObserver(getViewModel<MainViewModel>())
    }

    override fun onBackPressed() = finish()
}

@SuppressLint("StaticFieldLeak")
var appNavController: NavController? = null
