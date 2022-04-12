package com.hms.quickline.core.adapter

import android.view.View
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide

@BindingAdapter("imageUrl")
fun loadImage(view: ImageView, url: String) =
    Glide.with(view.context)
        .load(url)
        .into(view)

@BindingAdapter("isInvisible")
fun View.setIsGone(isGone: Boolean) {
    visibility = if (isGone) View.GONE else View.VISIBLE
}