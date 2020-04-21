package com.shotgunstudios.rishta_ahmadiyya.fragments


import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.size
import androidx.recyclerview.widget.LinearLayoutManager
import com.shotgunstudios.rishta_ahmadiyya.Chat

import com.shotgunstudios.rishta_ahmadiyya.R
import com.shotgunstudios.rishta_ahmadiyya.User
import com.shotgunstudios.rishta_ahmadiyya.activities.RishtaCallback
import com.shotgunstudios.rishta_ahmadiyya.adapters.ChatsAdapter
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.shotgunstudios.rishta_ahmadiyya.util.*
import kotlinx.android.synthetic.main.fragment_matches.*
import kotlinx.android.synthetic.main.fragment_swipe.*


class MatchesFragment : Fragment() {

    private lateinit var userId: String
    private lateinit var userDatabase: DatabaseReference
    private lateinit var chatDatabase: DatabaseReference
    private var callback: RishtaCallback? = null
    val db = FirebaseFirestore.getInstance()
    private val chatsAdapter = ChatsAdapter(ArrayList())

    fun setCallback(callback: RishtaCallback) {
        this.callback = callback
        userId = callback.onGetUserId()
        //userDatabase = callback.getUserDatabase()
        chatDatabase = callback.getChatDatabase()

        fetchData()
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_matches, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        matchesRV.apply {
            setHasFixedSize(false)
            layoutManager = LinearLayoutManager(context)
            adapter = chatsAdapter
            if (chatsAdapter.itemCount != 0) {
                noMatchesLayout.visibility = View.GONE
            }
        }
    }

    fun fetchData() {


        val docRef = db.collection("users").document(userId)
        //for each chatID, in preferred gender search everyone for an array that contains the same chatID and fill in info for chatAdapter
        docRef.get()
            .addOnSuccessListener { document ->
                var matches = document.get(DATA_MATCHES)

                if (matches != null) {
                    var chatIDs = matches as ArrayList<String>
                    var genderPreference = document.getString(DATA_GENDER_PREFERENCE)
                    for (chatID in chatIDs) {
                        noMatchesLayout.visibility = View.GONE
                        val docRef_match = db.collection("users")
                            .whereEqualTo(DATA_GENDER, genderPreference)
                            .whereArrayContains(DATA_MATCHES, chatID)
                        docRef_match.get()
                            .addOnSuccessListener { collection ->
                                val user = collection.documents[0]
                                val chat = Chat(
                                    userId, chatID, user.get(DATA_UID).toString(), user.get(
                                        DATA_NAME
                                    ).toString(), user.get(DATA_IMAGE_URL).toString()
                                )
                                chatsAdapter.addElement(chat)
                            }
                            .addOnFailureListener { exception ->
                                Log.w(ContentValues.TAG, "Error getting documents: ", exception)
                            }


                    }
                }

            }
            .addOnFailureListener { exception ->
                Log.w(ContentValues.TAG, "Error getting documents: ", exception)
            }

        /*
        userDatabase.child(userId).child(DATA_MATCHES).addListenerForSingleValueEvent(object:
            ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(p0: DataSnapshot) {
                if(p0.hasChildren()) {
                    p0.children.forEach { child ->
                        val matchId = child.key
                        val chatId = child.value.toString()
                        if(!matchId.isNullOrEmpty()) {
                            userDatabase.child(matchId).addListenerForSingleValueEvent(object: ValueEventListener{
                                override fun onCancelled(p0: DatabaseError) {
                                }

                                override fun onDataChange(p0: DataSnapshot) {
                                    val user = p0.getValue(User::class.java)
                                    if(user != null) {
                                        val chat = Chat(userId, chatId, user.uid, user.name, user.imageUrl)
                                        chatsAdapter.addElement(chat)
                                    }
                                }

                            })
                        }
                    }
                }
            }

        })
        */

    }

}
