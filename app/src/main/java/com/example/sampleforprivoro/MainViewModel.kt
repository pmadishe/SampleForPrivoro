package com.example.sampleforprivoro

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Environment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val application: MyApplication) : ViewModel() {

    private lateinit var localDateTime : String
    private lateinit var UtcDateTime : String

    private var _ipAddressLiveData = MutableLiveData<String>()
    private var _wifiAddressLiveData = MutableLiveData<String>()

    val ipAddressLiveData: LiveData<String> = _ipAddressLiveData
    val wifiAddressLiveData: LiveData<String> = _wifiAddressLiveData

    private var _locationLiveData = MutableLiveData<String>()
    val locationLiveData: LiveData<String> = _locationLiveData

    private var _dateTimeLiveData = MutableLiveData<String>()
    val dateTimeLiveData: LiveData<String> = _dateTimeLiveData

    init {
        getDeviceIPAddresses()
        setDateTime()
        startLocationUpdates()
    }

    // Function to update the date and time.
    private fun setDateTime() {
        // Get Date and Time
//        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
//        val currentDateAndTime: String = dateFormat.format(Date())
        _dateTimeLiveData.value = calculateUtcDateTime()
    }

    fun calculateUtcDateTime(): String {
        // Get the device's current timezone
        val localTimeZone = TimeZone.getDefault()

        // Create a Date object representing the current time
        val currentTime = Date()

        // Create a SimpleDateFormat to format the date and time in local timezone
        val localDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

        // Set the timezone of the date format to the local timezone
        localDateFormat.timeZone = localTimeZone

        // Format the current time using the local timezone
        localDateTime = localDateFormat.format(currentTime)

        // Create a SimpleDateFormat to format the date and time in UTC timezone
        val utcDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

        // Set the timezone of the date format to UTC
        utcDateFormat.timeZone = TimeZone.getTimeZone("UTC")

        // Parse the local date and time and format it as UTC
        UtcDateTime = utcDateFormat.format(localDateFormat.parse(localDateTime))

        return "UTC : $UtcDateTime , Local : $localDateTime"
    }

    private fun getDeviceIPAddresses() {
        val wifiManager = application.applicationContext.getSystemService(Context.WIFI_SERVICE)
                as WifiManager
        val wifiHotspotInfo = wifiManager.connectionInfo
        val ipAddress = wifiHotspotInfo.ipAddress

        _wifiAddressLiveData.value = String.format(
            "%d.%d.%d.%d",
            ipAddress and 0xff,
            ipAddress shr 8 and 0xff,
            ipAddress shr 16 and 0xff,
            ipAddress shr 24 and 0xff
        )

        val connectivityManager = application.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager
        val networkList = connectivityManager.allNetworks

        for (network in networkList) {
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)

            if ((networkCapabilities != null) &&
                (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
            ) {
                val linkProperties = connectivityManager.getLinkProperties(network)
                _ipAddressLiveData.value = linkProperties?.linkAddresses?.firstOrNull()?.address?.hostAddress
            }

        }
    }

    fun startLocationUpdates() {
        val locationManager =  application.applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        viewModelScope.launch {
            try {
                // Request location updates
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000, // Minimum time interval between updates (in milliseconds)
                    1.0f, // Minimum distance between updates (in meters)
                    object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            _locationLiveData.value = "Latitude : ${location.latitude}, Longitude : ${location.longitude}"
                        }
                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    }
                )
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }

    fun convertLiveDataToCsv(data: LiveData<String>): String {
        val stringBuilder = StringBuilder()

        // Add CSV header
        if (data == dateTimeLiveData) {
            stringBuilder.append("Date & Time :")
        } else if (data == ipAddressLiveData){
            stringBuilder.append("Ip Address : ")
        } else {
            stringBuilder.append("Location Coordinates: ")
        }
        stringBuilder.append("${data.value}")

        return stringBuilder.toString()
    }

    fun saveCsvToFile(csvData: String, fileName: String) {
        val file = File(Environment.getExternalStorageDirectory(), fileName)

        try {
            val csvWriter = FileWriter(file)
            csvWriter.write(csvData)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

}
