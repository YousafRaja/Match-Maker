package com.shotgunstudios.rishta_ahmadiyya.util

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.shotgunstudios.rishta_ahmadiyya.activities.RishtaActivity
import org.json.JSONException
import org.json.JSONObject

/**
 * Utility methods for dealing with connectivity
 */
object ConnectivityUtils : AppCompatActivity(){
    fun isConnected(context: Context): Boolean {
        val connectivityManager = context.applicationContext.getSystemService(
            Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting
    }
    private val FCM_API = "https://fcm.googleapis.com/fcm/send"
    private val serverKey =
        "key=" + "AAAAqwuM57Y:APA91bG7staJVmNioAxmATWnj6DnpMam5Wj0JuVZpuEYqa4XVkmv9cOloLoqx-gTf6tI4Lse865YIahgdYztPx3dj1NZ3B134sQLKnbR6wVZ3f-701BhUDDs6Ldi9EZHF67jTdO23qpG"
    private val contentType = "application/json"

    fun broadCastMessage(uid : String, title: String, message: String, context: Context): JsonObjectRequest {

        val topic = "/topics/"+uid //topic has to match what the receiver subscribed to

        val notification = JSONObject()
        val notifcationBody = JSONObject()

        try {
            notifcationBody.put("title", title)
            notifcationBody.put("message", message)
            notification.put("to", topic)
            notification.put("data", notifcationBody)
            Log.e("TAG", "try")
        } catch (e: JSONException) {
            Log.e("TAG", "onCreate: " + e.message)
        }

        return getRequest(notification, context)

    }

    fun getRequest(notification: JSONObject, context: Context): JsonObjectRequest {
        Log.e("TAG", "sendNotification")
        val jsonObjectRequest = object : JsonObjectRequest(FCM_API, notification,
            Response.Listener<JSONObject> { response ->
                Log.i("TAG", "onResponse: $response")
            },
            Response.ErrorListener {
                Toast.makeText(context, "Request error", Toast.LENGTH_LONG).show()
                Log.i("TAG", "onErrorResponse: Didn't work")
            }) {

            override fun getHeaders(): Map<String, String> {
                val params = HashMap<String, String>()
                params["Authorization"] = serverKey
                params["Content-Type"] = contentType
                return params
            }
        }
        return jsonObjectRequest
    }





}