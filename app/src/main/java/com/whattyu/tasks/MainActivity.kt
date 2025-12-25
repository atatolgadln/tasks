package com.whattyu.tasks

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.whattyu.tasks.databinding.ActivityMainBinding
import com.whattyu.tasks.ui.IlacAdapter
import com.whattyu.tasks.ui.IlacViewModel
import android.widget.PopupMenu
import com.whattyu.tasks.ui.IlacEkleDialog

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: IlacViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sabahAdapter = createAdapter()
        binding.rvSabah.layoutManager = LinearLayoutManager(this)
        binding.rvSabah.adapter = sabahAdapter

        val aksamAdapter = createAdapter()
        binding.rvAksam.layoutManager = LinearLayoutManager(this)
        binding.rvAksam.adapter = aksamAdapter

        viewModel.sabahGorevleri.observe(this) { list ->
            sabahAdapter.submitList(list)
            binding.tvBaslikSabah.visibility = if (list.isNotEmpty()) View.VISIBLE else View.GONE
            updateEmptyView()
        }

        viewModel.aksamGorevleri.observe(this) { list ->
            aksamAdapter.submitList(list)
            binding.tvBaslikAksam.visibility = if (list.isNotEmpty()) View.VISIBLE else View.GONE
            updateEmptyView()
        }

        binding.fabEkle.setOnClickListener {
            val dialog = com.whattyu.tasks.ui.IlacEkleDialog()
            dialog.show(supportFragmentManager, "IlacEkleDialog")
        }
    }

    // --- Yardımcı Fonksiyonlar ---

    private fun createAdapter(): IlacAdapter {
        return IlacAdapter(
            onIlacClick = { ilac, isChecked ->
                viewModel.ilacDurumunuGuncelle(ilac, isChecked)
            },
            onIlacLongClick = { ilac, view ->
                showTaskMenu(ilac, view)
            }
        )
    }

    private fun showTaskMenu(ilac: com.whattyu.tasks.data.IlacGorev, view: View) {
        val popup = PopupMenu(this, view)
        // Menü elemanlarını kodla ekliyoruz
        popup.menu.add(0, 1, 0, getString(R.string.menu_edit)) // "Düzenle"
        popup.menu.add(0, 2, 1, getString(R.string.menu_delete)) // "Sil"

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> { // Düzenle
                    val dialog = IlacEkleDialog.newInstance(ilac.id)
                    dialog.show(supportFragmentManager, "IlacDuzenleDialog")
                    true
                }
                2 -> { // Sil
                    viewModel.sil(ilac)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun updateEmptyView() {
        val isSabahEmpty = viewModel.sabahGorevleri.value.isNullOrEmpty()
        val isAksamEmpty = viewModel.aksamGorevleri.value.isNullOrEmpty()

        if (isSabahEmpty && isAksamEmpty) {
            binding.layoutEmptyState.visibility = View.VISIBLE
        } else {
            binding.layoutEmptyState.visibility = View.GONE
        }
    }
}