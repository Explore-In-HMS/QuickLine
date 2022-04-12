package com.hms.quickline.core.util

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController

fun showToastLong(context: Context, message: String) =
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()

fun showToastShort(context: Context, message: String) =
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

fun ViewGroup.layoutInflaterFactory(): LayoutInflater = LayoutInflater.from(context)

fun Fragment.navigate(navDirections: NavDirections) = findNavController().navigate(navDirections)