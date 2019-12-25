package com.shotgunstudios.rishta_ahmadiyya.fragments


import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast

import com.shotgunstudios.rishta_ahmadiyya.R
import com.shotgunstudios.rishta_ahmadiyya.User
import com.shotgunstudios.rishta_ahmadiyya.activities.RishtaCallback
import com.shotgunstudios.rishta_ahmadiyya.adapters.CardsAdapter
import com.shotgunstudios.rishta_ahmadiyya.util.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.lorentzos.flingswipe.SwipeFlingAdapterView
import kotlinx.android.synthetic.main.fragment_profile.*
import kotlinx.android.synthetic.main.fragment_swipe.*
import kotlinx.android.synthetic.main.fragment_swipe.progressLayout

class SwipeFragment : Fragment() {


    private var callback: RishtaCallback? = null
    private lateinit var userId: String
    private lateinit var userDatabase: DatabaseReference
    private lateinit var chatDatabase: DatabaseReference
    private var cardsAdapter: ArrayAdapter<User>? = null
    private var rowItems = ArrayList<User>()

    private var preferredCountry: String? = null
    private var preferredGender: String? = null
    private var userName: String? = null
    private var imageUrl: String? = null
    val db = FirebaseFirestore.getInstance()

    fun setCallback(callback: RishtaCallback) {
        this.callback = callback
        userId = callback.onGetUserId()
        userDatabase = callback.getUserDatabase()
        chatDatabase = callback.getChatDatabase()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_swipe, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val docRef = db.collection("users").document(userId)
        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    Log.d(TAG, "DocumentSnapshot data: ${document.data}")
                    preferredGender = document.getString(DATA_GENDER_PREFERENCE)
                    preferredCountry = document.getString(DATA_COUNTRY_PREFERENCE)
                    userName = document.getString(DATA_NAME)
                    imageUrl = document.getString(DATA_IMAGE_URL)
                    populateItems()
                } else {
                    Log.d(TAG, "No such document")
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
            }


/*
        userDatabase.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(p0: DataSnapshot) {
                val user = p0.getValue(User::class.java)
                preferredGender = user?.preferredGender
                userName = user?.name
                imageUrl = user?.imageUrl
                populateItems()
            }
        })
*/
        cardsAdapter = CardsAdapter(context!!, R.layout.item, rowItems)

        frame.adapter = cardsAdapter
        frame.setFlingListener(object : SwipeFlingAdapterView.onFlingListener {
            override fun removeFirstObjectInAdapter() {
                rowItems.removeAt(0)
                cardsAdapter?.notifyDataSetChanged()
            }

            override fun onLeftCardExit(p0: Any?) {
                var user = p0 as User
                //userDatabase.child(user.uid.toString()).child(DATA_SWIPES_LEFT).child(userId)
                  //  .setValue(true)
                db.collection("users").document(user.uid.toString())
                    .update(DATA_SWIPES_LEFT, FieldValue.arrayUnion(userId))

            }

            override fun onRightCardExit(p0: Any?) {
                val selectedUser = p0 as User
                val selectedUserId = selectedUser.uid
                if (!selectedUserId.isNullOrEmpty()) {
                    db.collection("users").document(selectedUserId)
                        .update(DATA_SWIPES_RIGHT, FieldValue.arrayUnion(userId))


                    val docRef = db.collection("users")
                        .whereEqualTo("uid", userId)
                        .whereArrayContains(DATA_SWIPES_RIGHT, selectedUserId)
                    docRef.get()
                        .addOnSuccessListener { selected ->
                            Toast.makeText(context, "Match!", Toast.LENGTH_SHORT).show()
                            val chatKey = chatDatabase.push().key
                            if (chatKey != null) {
                                db.collection("users").document(userId)
                                    .update(DATA_SWIPES_RIGHT, FieldValue.arrayRemove(selectedUserId))
                                db.collection("users").document(selectedUserId)
                                    .update(DATA_SWIPES_RIGHT, FieldValue.arrayRemove(userId))

                                db.collection("users").document(userId)
                                    .update(DATA_MATCHES, FieldValue.arrayUnion(chatKey))
                                db.collection("users").document(selectedUserId)
                                    .update(DATA_MATCHES, FieldValue.arrayUnion(chatKey))

                                chatDatabase.child(chatKey).child(userId).child(DATA_NAME)
                                    .setValue(userName)
                                chatDatabase.child(chatKey).child(userId)
                                    .child(DATA_IMAGE_URL)
                                    .setValue(imageUrl)

                                chatDatabase.child(chatKey).child(selectedUserId)
                                    .child(DATA_NAME)
                                    .setValue(selectedUser.name)
                                chatDatabase.child(chatKey).child(selectedUserId)
                                    .child(DATA_IMAGE_URL)
                                    .setValue(selectedUser.imageUrl)
                            }
                        }
                        .addOnFailureListener { exception ->
                            Log.w(TAG, "Error getting documents: ", exception)
                        }

/*
                    userDatabase.child(userId).child(DATA_SWIPES_RIGHT)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onCancelled(p0: DatabaseError) {
                            }

                            override fun onDataChange(p0: DataSnapshot) {
                                if (p0.hasChild(selectedUserId)) {
                                    Toast.makeText(context, "Match!", Toast.LENGTH_SHORT).show()


                                    val chatKey = chatDatabase.push().key

                                    if (chatKey != null) {
                                        userDatabase.child(userId).child(DATA_SWIPES_RIGHT)
                                            .child(selectedUserId)
                                            .removeValue()
                                        userDatabase.child(userId).child(DATA_MATCHES)
                                            .child(selectedUserId)
                                            .setValue(chatKey)
                                        userDatabase.child(selectedUserId).child(DATA_MATCHES)
                                            .child(userId)
                                            .setValue(chatKey)

                                        chatDatabase.child(chatKey).child(userId).child(DATA_NAME)
                                            .setValue(userName)
                                        chatDatabase.child(chatKey).child(userId)
                                            .child(DATA_IMAGE_URL)
                                            .setValue(imageUrl)

                                        chatDatabase.child(chatKey).child(selectedUserId)
                                            .child(DATA_NAME)
                                            .setValue(selectedUser.name)
                                        chatDatabase.child(chatKey).child(selectedUserId)
                                            .child(DATA_IMAGE_URL)
                                            .setValue(selectedUser.imageUrl)
                                    }


                                } else {
                                    userDatabase.child(selectedUserId).child(DATA_SWIPES_RIGHT)
                                        .child(userId)
                                        .setValue(true)
                                }
                            }
                        })
                    */
                }
            }

            override fun onAdapterAboutToEmpty(p0: Int) {
            }

            override fun onScroll(p0: Float) {
            }
        })

        frame.setOnItemClickListener { position, data -> }

        likeButton.setOnClickListener {
            if (!rowItems.isEmpty()) {
                frame.topCardListener.selectRight()
            }
        }

        dislikeButton.setOnClickListener {
            if (!rowItems.isEmpty()) {
                frame.topCardListener.selectLeft()
            }
        }
    }

    fun populateItems() {
        noUsersLayout.visibility = View.GONE
        progressLayout.visibility = View.VISIBLE
        val docRef = db.collection("users")
        .whereEqualTo(DATA_GENDER, preferredGender)
        //.whereEqualTo(DATA_COUNTRY, DATA_COUNTRY_PREFERENCE)
        docRef.get()
            .addOnSuccessListener { filteredUsers ->
                for (document in filteredUsers) {
                    var showUser = true
                    /*
                    if (document.get(DATA_SWIPES_LEFT).toString().contains(userId)) {
                        showUser = false
                    }
                    */
                    val user = document.toObject(User::class.java)
                    if (showUser) {
                        rowItems.add(user)
                        cardsAdapter?.notifyDataSetChanged()
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents: ", exception)
            }
        progressLayout.visibility = View.GONE
        if (rowItems.isEmpty()) {
            noUsersLayout.visibility = View.VISIBLE
        }
    }
/*
        val cardsQuery =
            userDatabase.orderByChild(DATA_GENDER).equalTo(preferredGender)
        //val cardsQuery = cardsQuery1.orderByChild(DATA_COUNTRY).equalTo(DATA_COUNTRY_PREFERENCE)// Filtering users here

        cardsQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(p0: DataSnapshot) {
                p0.children.forEach { child ->
                    val user = child.getValue(User::class.java)
                    if (user != null) {
                        var showUser = true
                        if (child.child(DATA_SWIPES_LEFT).hasChild(userId) ||
                            child.child(DATA_SWIPES_RIGHT).hasChild(userId) ||
                            child.child(DATA_MATCHES).hasChild(userId)
                        ) {
                            showUser = false
                        }
                        if (showUser) {
                            rowItems.add(user)
                            cardsAdapter?.notifyDataSetChanged()
                        }
                    }
                }
                progressLayout.visibility = View.GONE
                if (rowItems.isEmpty()) {
                    noUsersLayout.visibility = View.VISIBLE
                }
            }
        })
    } */




}
