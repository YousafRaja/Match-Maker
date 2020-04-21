package com.shotgunstudios.rishta_ahmadiyya.activities

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.ImageDecoder.createSource
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.shotgunstudios.rishta_ahmadiyya.R
import com.shotgunstudios.rishta_ahmadiyya.fragments.MatchesFragment
import com.shotgunstudios.rishta_ahmadiyya.fragments.ProfileFragment
import com.shotgunstudios.rishta_ahmadiyya.fragments.SwipeFragment
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_main.*
import java.io.ByteArrayOutputStream
import java.io.IOException

import android.util.Log
import android.widget.Toast
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.google.firebase.database.ServerValue
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.shotgunstudios.rishta_ahmadiyya.util.*
import org.json.JSONException
import org.json.JSONObject


const val REQUEST_CODE_PHOTO = 1234
class RishtaActivity : AppCompatActivity(), RishtaCallback {



    private val firebaseAuth = FirebaseAuth.getInstance()
    private val userId = firebaseAuth.currentUser?.uid
    val db = FirebaseFirestore.getInstance()

    private lateinit var userDatabase: DatabaseReference
    private lateinit var chatDatabase: DatabaseReference

    private var profileFragment: ProfileFragment? = null
    private var swipeFragment: SwipeFragment? = null
    private var matchesFragment: MatchesFragment? = null
    private var newMatch = false

    private var profileTab: TabLayout.Tab? = null
    private var swipeTab: TabLayout.Tab? = null
    private var matchesTab: TabLayout.Tab? = null

    private var resultImageUrl: Uri? = null

    private var chattingWith = ""


    private val FCM_API = "https://fcm.googleapis.com/fcm/send"
    private val serverKey =
        "key=" + "AAAAqwuM57Y:APA91bG7staJVmNioAxmATWnj6DnpMam5Wj0JuVZpuEYqa4XVkmv9cOloLoqx-gTf6tI4Lse865YIahgdYztPx3dj1NZ3B134sQLKnbR6wVZ3f-701BhUDDs6Ldi9EZHF67jTdO23qpG"
    private val contentType = "application/json"



    fun unsubAll(myID: String){
        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("users")
        docRef.get()
            .addOnSuccessListener { users ->
                for (user in users){
                    if(user.id==myID)continue
                    FirebaseMessaging.getInstance().unsubscribeFromTopic("/topics/"+user.id)
                }

            }
            .addOnFailureListener { exception ->
                Log.d(ContentValues.TAG, "get failed with ", exception)
            }
    }

    fun updateAll(myID: String){
        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("users")
        docRef.get()
            .addOnSuccessListener { users ->
                for (user in users){
                    if(user.id==myID)continue
                    updateTimeStamp(user.id)
                }

            }
            .addOnFailureListener { exception ->
                Log.d(ContentValues.TAG, "get failed with ", exception)
            }
    }

  fun updateTimeStamp(user_id : String){
      val docData = hashMapOf(
          DATA_TS to FieldValue.serverTimestamp()
      )

      db.collection("users").document(user_id)
          .set(docData, SetOptions.merge())
          .addOnSuccessListener { Log.d(ContentValues.TAG, "Timestamp Updated") }
          .addOnFailureListener { e -> Log.w(ContentValues.TAG, "Error writing document", e) }
  }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        if(userId.isNullOrEmpty()) {
               onSignout()
        }

        updateTimeStamp(userId.toString())

        val topic = "/topics/"+userId.toString()
        FirebaseMessaging.getInstance().subscribeToTopic("/topics/tJTKvoqqczP4VFBg8vBDc8kNtMO2")
        FirebaseMessaging.getInstance().subscribeToTopic("/topics/OFa1tzxBQgMGjfBDqWCM1SlmUeM2")
        unsubAll(userId.toString())
        FirebaseMessaging.getInstance().subscribeToTopic(topic)

        //requestQueue.add(ConnectivityUtils.broadCastMessage(userId.toString(), "title2", "abcd2", this))

        chatDatabase = FirebaseDatabase.getInstance().reference.child(DATA_CHATS)


        profileTab = navigationTabs.newTab()
        swipeTab = navigationTabs.newTab()
        matchesTab = navigationTabs.newTab()

        profileTab?.icon = ContextCompat.getDrawable(this, R.drawable.tab_profile)
        swipeTab?.icon = ContextCompat.getDrawable(this, R.drawable.tab_swipe)
        matchesTab?.icon = ContextCompat.getDrawable(this, R.drawable.tab_matches)

        navigationTabs.addTab(profileTab!!)
        navigationTabs.addTab(swipeTab!!)
        navigationTabs.addTab(matchesTab!!)

        navigationTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {
                onTabSelected(tab)
            }

            override fun onTabUnselected(p0: TabLayout.Tab?) {
            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab) {
                    profileTab -> {
                        if (profileFragment == null) {
                            profileFragment = ProfileFragment()
                            profileFragment!!.setCallback(this@RishtaActivity)
                        }
                        replaceFragment(profileFragment!!)
                    }
                    swipeTab -> {
                        if (swipeFragment == null) {
                            swipeFragment = SwipeFragment()
                            swipeFragment!!.setCallback(this@RishtaActivity)
                        }
                        replaceFragment(swipeFragment!!)
                    }
                    matchesTab -> {
                        if (matchesFragment == null || newMatch) {
                            newMatch = false
                            matchesFragment = MatchesFragment()
                            matchesFragment!!.setCallback(this@RishtaActivity)
                        }
                        replaceFragment(matchesFragment!!)
                    }
                }
            }

        })

        profileTab?.select()
/*


 */
/*


        profileTab?.icon = ContextCompat.getDrawable(this, R.drawable.tab_profile)
        swipeTab?.icon = ContextCompat.getDrawable(this, R.drawable.tab_swipe)
        matchesTab?.icon = ContextCompat.getDrawable(this, R.drawable.tab_matches)

        navigationTabs.addTab(profileTab!!)
        navigationTabs.addTab(swipeTab!!)
        navigationTabs.addTab(matchesTab!!)



 */
    }

    fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    override fun onSignout() {
        firebaseAuth.signOut()
        startActivity(StartupActivity.newIntent(this))
        finish()
    }

    override fun onGetUserId():String = userId!!

    override fun getUserDatabase(): DatabaseReference = userDatabase

    override fun getChatDatabase(): DatabaseReference = chatDatabase

    override fun profileComplete() {
        swipeTab?.select()
    }

    override fun newMatch() {
        newMatch = true
    }


    override fun startActivityForPhoto() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_CODE_PHOTO)
        //start activity and let android system give an image and return it
        //returned image handled below in onActivityResult
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_PHOTO) {
            resultImageUrl = data?.data
            storeImage()
        }
    }

    override fun broadCastMessage(uid : String, title: String, message: String){

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

            sendNotification(notification)

    }

    private fun sendNotification(notification: JSONObject) {
        Log.e("TAG", "sendNotification")
        val jsonObjectRequest = object : JsonObjectRequest(FCM_API, notification,
            Response.Listener<JSONObject> { response ->
                Log.i("TAG", "onResponse: $response")
            },
            Response.ErrorListener {
                Toast.makeText(this@RishtaActivity, "Request error", Toast.LENGTH_LONG).show()
                Log.i("TAG", "onErrorResponse: Didn't work")
            }) {

            override fun getHeaders(): Map<String, String> {
                val params = HashMap<String, String>()
                params["Authorization"] = serverKey
                params["Content-Type"] = contentType
                return params
            }
        }
        requestQueue.add(jsonObjectRequest)
    }



    fun storeImage() {
        if(resultImageUrl != null && userId != null) {
            val filePath = FirebaseStorage.getInstance().reference.child("profileImage").child(userId)
            var bitmap: Bitmap? = null
            try {
                //createSource(application.contentResolver, resultImageUrl!!);
                if(android.os.Build.VERSION.SDK_INT>=29) {
                    bitmap = ImageDecoder.decodeBitmap(
                        createSource(
                            application.contentResolver,
                            resultImageUrl!!
                        )
                    )
                } else {
                    bitmap = MediaStore.Images.Media.getBitmap(application.contentResolver, resultImageUrl)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

            val baos = ByteArrayOutputStream()
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 20, baos)
            val data = baos.toByteArray()

            val uploadTask = filePath.putBytes(data)
            uploadTask.addOnFailureListener { e -> e.printStackTrace() }
            uploadTask.addOnSuccessListener { taskSnapshot ->
                filePath.downloadUrl
                    .addOnSuccessListener { uri ->
                        profileFragment?.updateImageUri(uri.toString())
                    }
                    .addOnFailureListener { e -> e.printStackTrace() }
            }
        }
    }


    private val requestQueue: RequestQueue by lazy {
        Volley.newRequestQueue(this.applicationContext)
    }




    companion object {
        fun newIntent(context: Context?) = Intent(context, RishtaActivity::class.java)
    }
}