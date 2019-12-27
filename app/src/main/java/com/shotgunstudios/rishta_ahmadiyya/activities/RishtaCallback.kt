package com.shotgunstudios.rishta_ahmadiyya.activities

import com.google.firebase.database.DatabaseReference

interface RishtaCallback{
    fun broadCastMessage(uid : String, title: String, message: String)
    fun newMatch()
    fun onSignout()
    fun onGetUserId(): String
    fun getUserDatabase(): DatabaseReference
    fun getChatDatabase(): DatabaseReference
    fun profileComplete()
    fun startActivityForPhoto()
}