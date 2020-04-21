package com.shotgunstudios.rishta_ahmadiyya.fragments


import android.content.ContentValues.TAG
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Spinner
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule


import com.shotgunstudios.rishta_ahmadiyya.R
import com.shotgunstudios.rishta_ahmadiyya.activities.RishtaCallback
import com.shotgunstudios.rishta_ahmadiyya.util.*
import com.google.firebase.database.DatabaseReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.android.synthetic.main.fragment_profile.*
import kotlinx.android.synthetic.main.item.*


class ProfileFragment : Fragment() {
    private lateinit var userId: String
    private lateinit var userDatabase: DatabaseReference
    val db = FirebaseFirestore.getInstance()
    private var callback: RishtaCallback? = null
    private var picSelected: Boolean = false

    fun setCallback(callback: RishtaCallback) {
        this.callback = callback
        userId = callback.onGetUserId()
        //userDatabase = callback.getUserDatabase().child(userId)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressLayout.setOnTouchListener{view, event->true} //stop user from clicking before everything loads
        populateInfo()

        profilePhotoIV.setOnClickListener { callback?.startActivityForPhoto() }

        applyButton.setOnClickListener { onApply() }
        signoutButton.setOnClickListener { callback?.onSignout() }

    }

    fun getIndex(spinner : Spinner, value : String): Int{
        for(i in 1 until spinner.count){
            if(spinner.getItemAtPosition(i)==value){
                return i
            }
        }
        return 0
    }

    fun populateInfo() {
        progressLayout.visibility = View.VISIBLE

        val docRef = db.collection("users").document(userId)
        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    Log.d(TAG, "DocumentSnapshot data: ${document.data}")
                    picSelected = document.getString(DATA_IMAGE_URL)!=""
                    nameET.setText(document.getString(DATA_NAME))
                    bioET.setText(document.getString(DATA_BIO))
                    countrySP.setSelection(
                        getIndex(
                            countrySP,
                            document.getString(DATA_COUNTRY).toString()
                        )
                    )
                    preferredCountrySP.setSelection(
                        getIndex(
                            preferredCountrySP, document.getString(
                                DATA_COUNTRY_PREFERENCE
                            ).toString()
                        )
                    )
                    ageET.setText(document.getString(DATA_AGE))
                    if (document.getString(DATA_GENDER) == GENDER_MALE) {
                        radioMan1.isChecked = true
                    } else {
                        radioWoman1.isChecked = true
                    }
                    if(document.getString(DATA_IMAGE_URL)!=null) {
                        populateImage(document.getString(DATA_IMAGE_URL!!).toString())
                    }

                    progressLayout.visibility = View.GONE

                } else {
                    Log.d(TAG, "No such document")
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
            }
    }

/*
            override fun onDataChange(p0: DataSnapshot) {
                if (isAdded) {
                    val user = p0.getValue(User::class.java)
                    nameET.setText(user?.name, TextView.BufferType.EDITABLE)
                    countrySP.setSelection(getIndex(countrySP,user?.country.toString()))
                    preferredCountrySP.setSelection(getIndex(preferredCountrySP,user?.preferredCountry.toString()))
                    ageET.setText(user?.age, TextView.BufferType.EDITABLE)

                    if (user?.gender == GENDER_MALE) {
                        radioMan1.isChecked = true
                    }
                    if (user?.gender == GENDER_FEMALE) {
                        radioWoman1.isChecked = true
                    }
                    if(!user?.imageUrl.isNullOrEmpty()) {
                        populateImage(user?.imageUrl!!)
                    }
                    progressLayout.visibility = View.GONE
                }
            }

        })





        userDatabase.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                progressLayout.visibility = View.GONE
            }

            override fun onDataChange(p0: DataSnapshot) {
                if (isAdded) {
                    val user = p0.getValue(User::class.java)
                    nameET.setText(user?.name, TextView.BufferType.EDITABLE)
                    countrySP.setSelection(getIndex(countrySP,user?.country.toString()))
                    preferredCountrySP.setSelection(getIndex(preferredCountrySP,user?.preferredCountry.toString()))
                    ageET.setText(user?.age, TextView.BufferType.EDITABLE)

                    if (user?.gender == GENDER_MALE) {
                        radioMan1.isChecked = true
                    }
                    if (user?.gender == GENDER_FEMALE) {
                        radioWoman1.isChecked = true
                    }
                    if(!user?.imageUrl.isNullOrEmpty()) {
                        populateImage(user?.imageUrl!!)
                    }
                    progressLayout.visibility = View.GONE
                }
            }

        })
    }


 */
    fun updateDB(field:String, value:String){
        db.collection("users").document(userId)
            .update(field,value)
            .addOnSuccessListener { Log.d(TAG, "successfully updated!") }
            .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }
    }

    fun addDB(database:FirebaseFirestore, content:Any){
        db.collection("users").document(userId)
            .set(content)
            .addOnSuccessListener { Log.d(TAG, "DocumentSnapshot successfully written!") }
            .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }
    }

    fun onApply() {

        if (nameET.text.toString().isNullOrEmpty() ||
            countrySP.selectedItemPosition==0|| preferredCountrySP.selectedItemPosition==0||
            radioGroup1.checkedRadioButtonId == -1 || !picSelected
        ) {
            Toast.makeText(context, getString(R.string.error_profile_incomplete), Toast.LENGTH_SHORT).show()
        } else {
            val name = nameET.text.toString()
            val age = ageET.text.toString()
            val bio = bioET.text.toString()
            val country = countrySP.selectedItem.toString()
            val preferredCountry = preferredCountrySP.selectedItem.toString()
            val gender =
                if (radioMan1.isChecked) GENDER_MALE
                else GENDER_FEMALE
            val preferredGender =
                if (radioMan1.isChecked) GENDER_FEMALE
                else GENDER_MALE

/*
            updateDB(DATA_NAME, name)
            updateDB(DATA_AGE, age)
            updateDB(DATA_COUNTRY, country)
            updateDB(DATA_COUNTRY_PREFERENCE, preferredCountry)
            updateDB(DATA_GENDER, gender)
            updateDB(DATA_GENDER_PREFERENCE, preferredGender)
*/
            val docData = hashMapOf(
                DATA_UID to userId,
                DATA_NAME to name,
                DATA_AGE to age,
                DATA_COUNTRY to country,
                DATA_COUNTRY_PREFERENCE to preferredCountry,
                DATA_GENDER to gender,
                DATA_GENDER_PREFERENCE to preferredGender,
                DATA_BIO to bio
            )

            db.collection("users").document(userId)
                .set(docData, SetOptions.merge())
                .addOnSuccessListener { Log.d(TAG, "DocumentSnapshot successfully written!") }
                .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }

/*
            userDatabase.child(DATA_NAME).setValue(name)
            userDatabase.child(DATA_AGE).setValue(age)
            userDatabase.child(DATA_COUNTRY).setValue(country)
            userDatabase.child(DATA_COUNTRY_PREFERENCE).setValue(preferredCountry)
            userDatabase.child(DATA_GENDER).setValue(gender)
            userDatabase.child(DATA_GENDER_PREFERENCE).setValue(preferredGender)
 */
            callback?.profileComplete()
        }
    }
    fun updateImageUri(uri: String) {

        val docData = hashMapOf(
            DATA_IMAGE_URL to uri
        )

        db.collection("users").document(userId)
            .set(docData, SetOptions.merge())
            .addOnSuccessListener { Log.d(TAG, "DocumentSnapshot successfully written!") }
            .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }
        //userDatabase.child(DATA_IMAGE_URL).setValue(uri)
        picSelected = true
        populateImage(uri)
    }
    fun populateImage(uri: String) {
        Glide.with(this)
            .load(uri)
            .into(profilePhotoIV)
    }

}
