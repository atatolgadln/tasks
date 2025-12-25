package com.whattyu.tasks

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.whattyu.tasks.databinding.ActivityMainBinding
import com.whattyu.tasks.ui.IlacAdapter
import com.whattyu.tasks.ui.IlacViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: IlacViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Sabah Listesi Ayarları
        val sabahAdapter = createAdapter()
        binding.rvSabah.layoutManager = LinearLayoutManager(this)
        binding.rvSabah.adapter = sabahAdapter

        // Akşam Listesi Ayarları
        val aksamAdapter = createAdapter()
        binding.rvAksam.layoutManager = LinearLayoutManager(this)
        binding.rvAksam.adapter = aksamAdapter

        // --- YENİ EKLENEN KISIM: Diğer/Her Zaman Listesi ---
        // XML'inizde rvDiger adında bir RecyclerView olmalı
        // Eğer yoksa activity_main.xml'e eklemelisiniz.
        val digerAdapter = createAdapter()
        // binding.rvDiger.layoutManager = LinearLayoutManager(this) // XML'de rvDiger varsa açın
        // binding.rvDiger.adapter = digerAdapter                   // XML'de rvDiger varsa açın

        viewModel.sabahGorevleri.observe(this) { list ->
            sabahAdapter.submitList(list)
            binding.tvHeaderMorning.visibility = if (list.isNotEmpty()) View.VISIBLE else View.GONE
            updateEmptyView()
        }

        viewModel.aksamGorevleri.observe(this) { list ->
            aksamAdapter.submitList(list)
            binding.tvHeaderEvening.visibility = if (list.isNotEmpty()) View.VISIBLE else View.GONE
            updateEmptyView()
        }

        // --- YENİ EKLENEN KISIM: Diğer Listeyi Gözlemleme ---
        viewModel.digerGorevleri.observe(this) { list ->
            digerAdapter.submitList(list)

            // Başlık ve Liste görünürlüğünü kontrol et (XML id'lerine göre düzenleyin)
            // binding.tvHeaderOther.visibility = if (list.isNotEmpty()) View.VISIBLE else View.GONE
            // binding.rvDiger.visibility = if (list.isNotEmpty()) View.VISIBLE else View.GONE

            updateEmptyView()
        }

        binding.fabEkle.setOnClickListener {
            val dialog = com.whattyu.tasks.ui.IlacEkleDialog()
            dialog.show(supportFragmentManager, "IlacEkleDialog")
        }
    }

    private fun createAdapter(): IlacAdapter {
        return IlacAdapter(
            onCheckChanged = { ilac, isChecked ->
                viewModel.ilacDurumunuGuncelle(ilac, isChecked)
            },
            onDeleteClick = { ilac ->
                viewModel.sil(ilac)
            }
        )
    }

    private fun updateEmptyView() {
        // val isSabahEmpty = viewModel.sabahGorevleri.value.isNullOrEmpty()
        // val isAksamEmpty = viewModel.aksamGorevleri.value.isNullOrEmpty()
        // val isDigerEmpty = viewModel.digerGorevleri.value.isNullOrEmpty()

        // if (isSabahEmpty && isAksamEmpty && isDigerEmpty) {
        //     binding.emptyView.visibility = View.VISIBLE
        // } else {
        //     binding.emptyView.visibility = View.GONE
        // }
    }
}