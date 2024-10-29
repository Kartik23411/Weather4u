@file:Suppress("DEPRECATION")

package com.example.weather4u

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.example.weather4u.models.Response
import com.example.weather4u.network.WeatherService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Suppress("UNUSED_VARIABLE")
@Composable
fun MainScreen(){
    val context = LocalContext.current
    val locationIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
    val permissionsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    val permissionUri = Uri.fromParts("package", "com.example.weather4u", null)
    var latitude by remember { mutableDoubleStateOf(0.0) }
    var longitude by remember { mutableDoubleStateOf(0.0) }
    var weatherList by remember { mutableStateOf<Response?>(null) }


    lateinit var sharedPreferences: SharedPreferences
    sharedPreferences = context.getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE)


    fun updateWeather(){
        val weatherResponseJsonString = sharedPreferences.getString(Constants.WEATHER_RESPONSE_DATA, " ")
        if (!weatherResponseJsonString.isNullOrEmpty()){
            weatherList = Gson().fromJson(weatherResponseJsonString, Response::class.java)
        }

    }

    fun getWeatherDetails(latitude:Double, longitude:Double){
        if (Constants.isNetworkAvailable(context)){
            val retrofit:Retrofit = Retrofit.Builder().baseUrl(Constants.BASE_URL).addConverterFactory(GsonConverterFactory.create()).build()
            val service: WeatherService = retrofit.create<WeatherService>(WeatherService::class.java)
            val call: Call<Response> = service.getWeather(latitude, longitude, Constants.APP_ID, Constants.METRIC)
            call.enqueue(object : Callback<Response>{
                override fun onResponse(
                    call: Call<Response>,
                    response: retrofit2.Response<Response>
                ) {
                    if (response.isSuccessful) {
                         val weather = response.body()

                        val weatherResponseJsonString = Gson().toJson(weather)
                        val editor = sharedPreferences.edit()
                        editor.putString(Constants.WEATHER_RESPONSE_DATA, weatherResponseJsonString)
                        editor.apply()
                        Log.i("shared pref", "Stored successfully")
                        updateWeather()
                    }else{
                        val rc = response.code()
                        when(rc){
                            400 -> {Log.e("Error 400", "Bad Connection")}
                            404 -> {Log.e("Error 404", "Not Found")}
                            else -> {Log.e("Error ", "Generic Error")}
                        }
                    }
                }

                override fun onFailure(call: Call<Response>, t: Throwable) {
                    Log.e("Errorr", t.message.toString())
                }

            })
        }else{
            /*TODO add the no network availability functionality*/
        }
    }

    // permissions related
    val requestPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {permissionGranted->
        permissionGranted.entries.forEach { entry->
            val permission = entry.key
            val isGranted = entry.value
            if (!isGranted){
                Toast.makeText(context, "This feature needs location access, please turn it on in the app settings", Toast.LENGTH_LONG).show()
                permissionsIntent.data = permissionUri
                context.startActivity(permissionsIntent)
            }
        }
    }

    fun checkAndRequestPermission(onGranted:()-> Unit){
        val hasLocationPermission = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasLocationPermission){
            onGranted()
        }else{
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    // accessing location
    lateinit var fusedLocaionClient:FusedLocationProviderClient
    fusedLocaionClient= LocationServices.getFusedLocationProviderClient(context)

    val locationCallback = object :LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult) {
            val lastLocation: Location? = locationResult.lastLocation
            if (lastLocation!=null){
                latitude = lastLocation.latitude
                longitude = lastLocation.longitude
                // function for getting weather
                getWeatherDetails(latitude, longitude)
            }
            else{
                Toast.makeText(context, "Having error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun requestLocation(){
        val locationRequest = LocationRequest()
        locationRequest.priority = Priority.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 0
        locationRequest.numUpdates = 1

        fusedLocaionClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())

    }

    LaunchedEffect(Unit) {
        if (isLocationEnabled(context)) {
            checkAndRequestPermission {
                requestLocation()
            }
        } else {
            context.startActivity(locationIntent)
        }
    }

    Column (
        modifier = Modifier
            .background(Color(0xFF4F617B))
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),

    ){

        if (weatherList!=null){
            CustomText(weatherList!!.name, modifier = Modifier.align(Alignment.Start).padding(top = 8.dp), 23.sp)
            Spacer(Modifier.height(32.dp))
            CustomText(weatherList!!.main.temp.toString() + "\u2103", Modifier.align(Alignment.CenterHorizontally), 64.sp)
            CustomText(weatherList!!.weather.firstOrNull()?.description ?: "", Modifier.align(Alignment.CenterHorizontally), 18.sp)

            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
                shape = RoundedCornerShape(10),
                colors = CardDefaults.cardColors(Color(0x880D121A))
            ) {
                Row (modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ){
                    CardElement("T Min.",
                        (weatherList?.main?.temp_min?.toString() + "\u2103"), Modifier.weight(.33f))
                    CardElement("Feels like",
                        (weatherList?.main?.feels_like?.toString() + "\u2103"), Modifier.weight(.33f))
                    CardElement("T Max.",
                        (weatherList?.main?.temp_max?.toString() + "\u2103"), Modifier.weight(.33f))
                }
                HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(4.dp), thickness = 1.dp, color = Color(0xFF849098))
                Row (modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ){
                    CardElement("Pressure", weatherList?.main?.pressure?.toString() ?: "", Modifier.weight(.33f))
                    CardElement("Humidity", weatherList?.main?.humidity?.toString() ?: "", Modifier.weight(.33f))
                    CardElement("Visibility", weatherList?.visibility.toString(), Modifier.weight(.33f))
                }
                HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(4.dp), thickness = 1.dp, color = Color(0xFF849098))
                Row (modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ){
                    CardElement("Sunrise",
                        convertTimeToHourAndMinutes(weatherList?.sys?.sunrise ?: 0.0),
                        Modifier.weight(.33f))
                    CardElement("Sunset",
                        convertTimeToHourAndMinutes(weatherList?.sys?.sunset ?: 0.0),
                        Modifier.weight(.33f))
                    CardElement("Wind Speed", weatherList?.wind?.speed.toString() , Modifier.weight(.33f))
                }
            }
        }else{
            Column (
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ){
                CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}

@Composable
fun CustomText(text:String, modifier: Modifier, fontSize:TextUnit){
    Text(
        modifier = modifier,
        text = text,
        fontSize = fontSize,
        fontWeight = FontWeight.Medium,
        color = Color.White,
        letterSpacing = 1.sp
    )
}

@Composable
fun CardElement(text:String, text2:String, modifier:Modifier){
    Column (
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ){
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Thin,
            color = Color(0xCEC1D2D2),
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = text2,
            fontSize = 12.sp,
            fontWeight = FontWeight.Thin,
            color = Color.White,
            letterSpacing = 1.sp
        )
    }
}

@SuppressLint("NewApi")
fun convertTimeToHourAndMinutes(timeStamp:Double):String{
    val instant = Instant.ofEpochSecond(timeStamp.toLong())
    val zoneId = ZoneId.systemDefault()
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    val dateTime = LocalDateTime.ofInstant(instant,zoneId)

    return dateTime.format(formatter)
}

fun isLocationEnabled(context: Context):Boolean{
    val locationManager:LocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
}

@Preview(showBackground = true)
@Composable
fun Preview(){
    MainScreen()
}