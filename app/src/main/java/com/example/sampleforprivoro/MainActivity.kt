package com.example.sampleforprivoro

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: MainViewModel
    private var wifiaddr: String? = null
    private var ipaddr: String? = null

    private lateinit var ipAddressTextView: TextView
    private lateinit var locationTextView: TextView
    private lateinit var dateTimeTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize the ViewModel
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        ipAddressTextView = findViewById(R.id.ipAddressTextView)
        locationTextView = findViewById(R.id.locationTextView)
        dateTimeTextView = findViewById(R.id.dateTimeTextView)

        // Check and request location permission
        checkRequestLocationPermissions()
    }

    private fun checkRequestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        } else if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                2
            )
        } else {
            getLocationInfo()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1 || requestCode == 2) {
            viewModel.startLocationUpdates()

            getLocationInfo()
        }
    }

    private fun getLocationInfo() {
        // Get IP Address

        viewModel.ipAddressLiveData.observe(this, Observer {
            ipaddr = it
            extracted(viewModel.ipAddressLiveData)
        })

        viewModel.wifiAddressLiveData.observe(this, Observer {
            wifiaddr = it
            extracted(viewModel.wifiAddressLiveData)
        })

        // Get Location (Latitude and Longitude)
        // For simplicity, you can use a dummy location here
        viewModel.locationLiveData.observe(this, Observer {

            locationTextView.text = it
            extracted(viewModel.locationLiveData)
        })

        viewModel.dateTimeLiveData.observe(this, Observer {
            dateTimeTextView.text = it
            extracted(viewModel.dateTimeLiveData)
        })

        ipAddressTextView.text = "Wifi : $wifiaddr , Cellular : $ipaddr"
    }

    private fun extracted(liveData: LiveData<String>) {
        val csvData = viewModel.convertLiveDataToCsv(liveData)
        viewModel.saveCsvToFile(csvData, "data.csv")
    }
}


