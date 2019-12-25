package com.shotgunstudios.rishta_ahmadiyya.activities

import com.google.firebase.database.DatabaseReference

interface RishtaCallback{

    fun onSignout()
    fun onGetUserId(): String
    fun getUserDatabase(): DatabaseReference
    fun getChatDatabase(): DatabaseReference
    fun profileComplete()
    fun startActivityForPhoto()
}