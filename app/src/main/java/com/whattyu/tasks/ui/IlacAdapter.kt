package com.whattyu.tasks.ui

import android.graphics.Color
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
    private val onIlacLongClick: () -> Unit,
    private val onIlacEditClick: (IlacGorev) -> Unit, // YENİ: Düzenleme için tıklama
    private val onSelectionChanged: (Boolean) -> Unit
) : ListAdapter<IlacGorev, IlacAdapter.IlacViewHolder>(DiffCallback()) {

    // SEÇİM MODU DEĞİŞKENLERİ
    var isSelectionMode = false
    val selectedItems = HashSet<Int>() // Seçilenlerin ID'lerini tutar

    // Modu Aç/Kapat
    fun setSelectionModeEnabled(enabled: Boolean) {
        isSelectionMode = enabled
        if (!enabled) selectedItems.clear()
        notifyDataSetChanged()
    }

    // Tümünü Seç (İsteğe bağlı)
    fun selectAll() {
        currentList.forEach { selectedItems.add(it.id) }
        notifyDataSetChanged()
        onSelectionChanged(selectedItems.isNotEmpty())
    }

    // Seçilenleri Getir (Silmek için)
    fun getSelectedTasks(): List<IlacGorev> {
        return currentList.filter { selectedItems.contains(it.id) }
    }

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

            // GÖRSEL AYARLAR
            if (isSelectionMode) {
                // SEÇİM MODUNDAYSA:
                binding.cbTamamlandi.visibility = View.GONE // Checkbox'ı gizle

                if (selectedItems.contains(ilac.id)) {
                    // Seçiliyse morumsu arka plan yap
                    binding.root.setCardBackgroundColor(Color.parseColor("#33BB86FC"))
                    binding.root.strokeWidth = 4
                } else {
                    // Seçili değilse normal
                    binding.root.setCardBackgroundColor(Color.parseColor("#1E1E1E"))
                    binding.root.strokeWidth = 0
                }

                // Tıklanınca Seç/Bırak
                binding.root.setOnClickListener {
                    if (selectedItems.contains(ilac.id)) {
                        selectedItems.remove(ilac.id)
                    } else {
                        selectedItems.add(ilac.id)
                    }
                    notifyItemChanged(adapterPosition)
                    onSelectionChanged(selectedItems.isNotEmpty())
                }

            } else {
                // NORMAL MODDAYSA:
                binding.cbTamamlandi.visibility = View.VISIBLE
                binding.root.setCardBackgroundColor(Color.parseColor("#1E1E1E"))
                binding.root.strokeWidth = 0

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

                binding.root.setOnClickListener {
                    onIlacEditClick(ilac)
                }

                // Basılı Tutunca Modu Aç
                binding.root.setOnLongClickListener {
                    onIlacLongClick() // Activity'e haber ver
                    selectedItems.add(ilac.id) // Basılanı direkt seç
                    true
                }

                // Normal tıklama bir şey yapmasın (Checkbox hallediyor)
                binding.root.setOnClickListener(null)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<IlacGorev>() {
        override fun areItemsTheSame(oldItem: IlacGorev, newItem: IlacGorev) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: IlacGorev, newItem: IlacGorev) = oldItem == newItem
    }
}