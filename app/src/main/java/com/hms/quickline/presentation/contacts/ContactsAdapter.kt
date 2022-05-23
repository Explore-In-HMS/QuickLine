package com.hms.quickline.presentation.contacts

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hms.quickline.R
import com.hms.quickline.core.util.gone
import com.hms.quickline.data.model.Users
import com.hms.quickline.databinding.CardCallDialogBinding

class ContactsAdapter(list: ArrayList<Users>, listener: ICallDialogAdapter) :
    RecyclerView.Adapter<ContactsAdapter.ViewHolder>() {

    private var itemList: ArrayList<Users> = list
    private var itemListener = listener
    private lateinit var binding: CardCallDialogBinding

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        binding = CardCallDialogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(itemList[position])
    }

    inner class ViewHolder(binding: CardCallDialogBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(item: Users) {
            with(binding) {
                if (item.photo == null) {
                    Glide.with(this.root.context)
                        .load(R.drawable.ic_person_24)
                        .circleCrop()
                        .into(imgProfilePhoto)
                } else {
                    Glide.with(this.root.context)
                        .load(item.photo)
                        .circleCrop()
                        .into(imgProfilePhoto)
                }

                tvName.text = item.name
                if (item.isAvailable) {
                    tvState.text = "Available"
                    imgStateBusy.gone()
                } else {
                    tvState.text = "Busy"
                    imgStateAvailable.gone()
                }

                imgVoiceCall.setOnClickListener {
                    itemListener.onItemSelected(true, item)
                }

                imgVideoCall.setOnClickListener {
                    itemListener.onItemSelected(false, item)
                }
            }
        }
    }

    override fun getItemCount() = itemList.size

    interface ICallDialogAdapter {
        fun onItemSelected(isVoiceCall: Boolean, user: Users)
    }
}