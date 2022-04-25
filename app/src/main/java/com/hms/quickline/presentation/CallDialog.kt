package com.hms.quickline.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.hms.quickline.databinding.DialogCallListBinding

/**
 * 功能描述
 *
 * @author b00557735
 * @since 2022-04-25
 */
class CallDialog: DialogFragment() {
    private lateinit var binding: DialogCallListBinding
    private lateinit var adapter: CallDialogAdapter
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        //dialog!!.window?.setBackgroundDrawableResource(R.drawable.round_corner);
        binding =
            DialogCallListBinding.inflate(LayoutInflater.from(requireContext()))

        val dummy = arrayListOf<String>("Meeting1","Meeting2","Meeting3","Meeting4")
        binding.rvMeetingIdList.layoutManager = LinearLayoutManager(requireContext())
        adapter = CallDialogAdapter(dummy)
        binding.rvMeetingIdList.adapter = adapter

        return binding.root
    }

    override fun onStart() {
        super.onStart()

        dialog?.window
            ?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
    }

}