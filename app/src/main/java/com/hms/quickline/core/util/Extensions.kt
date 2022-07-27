package com.hms.quickline.core.util

import android.content.Context
import android.content.SharedPreferences
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController
import java.util.*

fun showToastLong(context: Context, message: String) =
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()

fun showToastShort(context: Context, message: String) =
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

fun showToastLongCenter(context: Context, message: String, length: Int) {
    val toastCenter = Toast.makeText(context, message, length)
    toastCenter.setGravity(Gravity.CENTER, 0, 0)
    toastCenter.show()
}

private const val USER_MAIL = "USER_MAIL"
private const val QUICKLINE_STORAGE = "QUICKLINE_STORAGE"
lateinit var sharedPreferences: SharedPreferences

fun getUserMail(context: Context): String? {
    sharedPreferences = context.getSharedPreferences(QUICKLINE_STORAGE, Context.MODE_PRIVATE)
    return sharedPreferences.getString(USER_MAIL, "")
}

fun ViewGroup.layoutInflaterFactory(): LayoutInflater = LayoutInflater.from(context)

fun Fragment.navigate(navDirections: NavDirections) = findNavController().navigate(navDirections)

fun getStatus(lastSeen: Date): String {
    val currentDate = Date()
    val difference: Long = currentDate.time - lastSeen.time

    val criteriaOnline = 1000 * 60 //a minute
    val criteriaAway = 1000 * 60 * 10 //ten minutes

    return when {
        difference < criteriaOnline -> "Available"

        difference < criteriaAway -> "Away"

        else -> "Offline"
    }
}
