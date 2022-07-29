package com.hms.quickline.core.adapter

import android.view.View
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.hms.quickline.R

@BindingAdapter("imageUrl")
fun loadImage(view: ImageView, url: String) =
    Glide.with(view.context)
        .load(url)
        .into(view)

@BindingAdapter("contactImageUrl")
fun loadContactImage(view: ImageView, url: String?) {
    url?.let {
        Glide.with(view.context)
            .load(url)
            .circleCrop()
            .into(view)
    } ?: run {
        Glide.with(view.context)
            .load(R.drawable.ic_person_24)
            .circleCrop()
            .into(view)
    }
}

@BindingAdapter("isGone")
fun View.setIsGone(isGone: Boolean) {
    visibility = if (isGone) View.GONE else View.VISIBLE
}