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
import com.google.firebase.database.DatabaseReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.lorentzos.flingswipe.SwipeFlingAdapterView
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
    private var populationExists = false
    val db = FirebaseFirestore.getInstance()

    fun setCallback(callback: RishtaCallback) {
        this.callback = callback
        userId = callback.onGetUserId()
        //userDatabase = callback.getUserDatabase()
        chatDatabase = callback.getChatDatabase()
    }

    override fun onPause() {
        rowItems.clear()
        cardsAdapter?.notifyDataSetChanged()
        super.onPause()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_swipe, container, false)
    }

    override fun onResume() {
        super.onResume()
        if(populationExists)populateItems() //This fragment is paused when user navigates to the UserInfo page, onResume, need to repopulate image or else it will be blank
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(!populationExists) {
            val docRef = db.collection("users").document(userId)
            docRef.get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        Log.d(TAG, "DocumentSnapshot data: ${document.data}")
                        preferredGender = document.getString(DATA_GENDER_PREFERENCE)
                        preferredCountry = document.getString(DATA_COUNTRY_PREFERENCE)
                        userName = document.getString(DATA_NAME)
                        //imageUrl = document.getString(DATA_IMAGE_URL)
                        cardsAdapter?.notifyDataSetChanged()
                        populateItems()

                    } else {
                        Log.d(TAG, "No such document")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d(TAG, "get failed with ", exception)
                }
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

                    docRef.get()
                        .addOnSuccessListener { collection ->
                            var document = collection.documents[0]
                            if (document.get(DATA_SWIPES_RIGHT).toString().contains(selectedUserId)) {
                                Toast.makeText(context, "Match!", Toast.LENGTH_SHORT).show()
                                callback!!.newMatch()
                                callback!!.broadCastMessage(selectedUserId,"New Match!", "Go to the chats tab and say hi to your new match.")
                                val chatKey = chatDatabase.push().key
                                if (chatKey != null) {
                                    db.collection("users").document(userId)
                                        .update(
                                            DATA_SWIPES_RIGHT,
                                            FieldValue.arrayRemove(selectedUserId)
                                        )
                                    db.collection("users").document(selectedUserId)
                                        .update(DATA_SWIPES_RIGHT, FieldValue.arrayRemove(userId))

                                    // Handle matching logic
                                    // DATA_MATCHES references chat data base, DATA_MATCHES_IDS used as history of previous matches during filtering in swipe fragment
                                    db.collection("users").document(userId)
                                        .update(DATA_MATCHES, FieldValue.arrayUnion(chatKey))
                                    db.collection("users").document(selectedUserId)
                                        .update(DATA_MATCHES, FieldValue.arrayUnion(chatKey))
                                    db.collection("users").document(userId)
                                        .update(
                                            DATA_MATCH_IDS,
                                            FieldValue.arrayUnion(selectedUserId)
                                        )
                                    db.collection("users").document(selectedUserId)
                                        .update(DATA_MATCH_IDS, FieldValue.arrayUnion(userId))
                                    //---
                                    //--Setup chat database for matched users
                                    chatDatabase.child(chatKey).child(userId)
                                        .child(DATA_NAME)
                                        .setValue(userName)
                                    chatDatabase.child(chatKey).child(userId)
                                        .child(DATA_IMAGE_URL)
                                        .setValue(document.get(DATA_IMAGE_URL))
                                    chatDatabase.child(chatKey).child(selectedUserId)
                                        .child(DATA_NAME)
                                        .setValue(selectedUser.name)
                                    chatDatabase.child(chatKey).child(selectedUserId)
                                        .child(DATA_IMAGE_URL)
                                        .setValue(selectedUser.imageUrl)
                                    //---
                                }
                                //Handle notification


                                //---

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

        /*
        val docRef2 = db.collection("users").document(userId)
        docRef2.get()
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
        */

        populationExists = true
        noUsersLayout.visibility = View.GONE
        progressLayout.visibility = View.VISIBLE
        val docRef = db.collection("users")
            .whereEqualTo(DATA_GENDER, preferredGender)

        //.whereEqualTo(DATA_COUNTRY, DATA_COUNTRY_PREFERENCE)
        docRef.get()
            .addOnSuccessListener { filteredUsers ->
                for (document in filteredUsers) {
                    var showUser = true

                    if (document.get(DATA_SWIPES_LEFT).toString().contains(userId)
                        || document.get(DATA_SWIPES_RIGHT).toString().contains(userId)
                        || document.get(DATA_MATCH_IDS).toString().contains(userId)
                    ) {
                        showUser = false
                    }

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
