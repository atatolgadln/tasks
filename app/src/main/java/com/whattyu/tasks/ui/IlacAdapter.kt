package com.whattyu.tasks.ui

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.whattyu.tasks.R
import com.whattyu.tasks.data.IlacGorev
import com.whattyu.tasks.databinding.ItemIlacBinding

class IlacAdapter(
    private val onIlacClick: (IlacGorev, Boolean) -> Unit,
    private val onIlacLongClick: (IlacGorev, View) -> Unit
) : ListAdapter<IlacGorev, IlacAdapter.IlacViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IlacViewHolder {
        val binding = ItemIlacBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return IlacViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IlacViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class IlacViewHolder(private val binding: ItemIlacBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(ilac: IlacGorev) {
            val context = binding.root.context
            binding.tvIlacIsim.text = ilac.isim

            // Doz Metni
            if (ilac.kalanDoz == -1) {
                binding.tvDozBilgi.text = context.getString(R.string.status_infinite)
            } else {
                binding.tvDozBilgi.text = context.getString(R.string.status_remaining_fmt, ilac.kalanDoz)
            }

            binding.cbTamamlandi.setOnCheckedChangeListener(null)
            binding.cbTamamlandi.isChecked = ilac.seciliMi

            binding.tvIlacIsim.paintFlags = if (ilac.seciliMi) {
                binding.tvIlacIsim.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.tvIlacIsim.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

            binding.cbTamamlandi.setOnCheckedChangeListener { _, isChecked ->
                onIlacClick(ilac, isChecked)
            }

            binding.root.setOnLongClickListener {
                onIlacLongClick(ilac, binding.root)
                true
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<IlacGorev>() {
        override fun areItemsTheSame(oldItem: IlacGorev, newItem: IlacGorev) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: IlacGorev, newItem: IlacGorev) = oldItem == newItem
    }
}