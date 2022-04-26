package com.hms.quickline.presentation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hms.quickline.databinding.CardCallDialogBinding
import com.hms.quickline.databinding.DialogCallListBinding

/**
 * 功能描述
 *
 * @author b00557735
 * @since 2022-04-25
 */
class CallDialogAdapter constructor(list: ArrayList<String>,listener: ICallDialogAdapter) : RecyclerView.Adapter<CallDialogAdapter.ViewHolder>() {

    private var itemList: ArrayList<String> = list
    private var itemListener = listener
    private lateinit var binding: CardCallDialogBinding

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        binding =
            CardCallDialogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding);
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item: String = itemList[position]
        holder.bind(item)
        holder.itemView.setOnClickListener {
            itemListener.onItemSelected(item)
        }
    }

    override fun getItemCount() = itemList.size


    inner class ViewHolder(myApartmentItemBinding: CardCallDialogBinding) :
        RecyclerView.ViewHolder(myApartmentItemBinding.root) {
        fun bind(item: String) {
            binding.tvMeetingId.text = item
        }
    }

    interface ICallDialogAdapter {
        fun onItemSelected(meetingId: String)
    }
}