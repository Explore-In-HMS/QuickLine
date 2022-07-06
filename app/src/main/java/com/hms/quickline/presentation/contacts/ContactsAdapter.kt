package com.hms.quickline.presentation.contacts

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hms.quickline.R
import com.hms.quickline.core.util.getStatus
import com.hms.quickline.core.util.gone
import com.hms.quickline.core.util.visible
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
        @SuppressLint("SetTextI18n", "UseCompatLoadingForColorStateLists")
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
                    imgVideoCall.visible()

                    item.lastSeen?.let {
                        when(getStatus(it)) {

                            root.context.getString(R.string.available) -> {
                                tvState.text = root.context.getString(R.string.available)
                                imgStateAvailable.backgroundTintList = root.context.resources.getColorStateList(R.color.available_color)
                                imgStateAvailable.setImageResource(R.drawable.ic_check_12)
                            }

                            root.context.getString(R.string.away) -> {
                                tvState.text = root.context.getString(R.string.away)
                                imgStateAvailable.backgroundTintList = root.context.resources.getColorStateList(R.color.away_color)
                                imgStateAvailable.setImageResource(R.drawable.ic_recent)
                            }

                            root.context.getString(R.string.offline) -> {
                                tvState.text = root.context.getString(R.string.offline)
                                imgStateAvailable.backgroundTintList = root.context.resources.getColorStateList(R.color.offline_color)
                                imgStateAvailable.setImageResource(0)

                            }
                        }
                    } ?: run {
                        tvState.text = root.context.getString(R.string.available)
                        imgStateAvailable.backgroundTintList = root.context.resources.getColorStateList(R.color.available_color)
                        imgStateAvailable.setImageResource(R.drawable.ic_check_12)
                    }

                } else {
                    tvState.text = root.context.getString(R.string.busy)
                    imgStateAvailable.backgroundTintList = root.context.resources.getColorStateList(R.color.busy_color)
                    imgStateAvailable.setImageResource(R.drawable.ic_phone_24)
                    imgVideoCall.gone()
                }

                imgVoiceCall.setOnClickListener {
                    itemListener.onItemSelected(true, item)
                }

                imgVideoCall.setOnClickListener {
                    itemListener.onItemSelected(false, item)
                }

                if (item.isVerified)
                    binding.ivVerify.visible()
                else
                    binding.ivVerify.gone()
            }
        }
    }

    override fun getItemCount() = itemList.size

    interface ICallDialogAdapter {
        fun onItemSelected(isVoiceCall: Boolean, user: Users)
    }
}