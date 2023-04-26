package com.youtubemusic.feature.youtube_downloader.filter

import android.content.Context
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.youtubemusic.core.data.*
import com.youtubemusic.feature.youtube_downloader.R
import com.youtubemusic.feature.youtube_downloader.databinding.DialogSearchFilterBinding

class SearchFilterDialogWrapper(private val context: Context) {
    private val binding: DialogSearchFilterBinding by lazy {
        DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.dialog_search_filter, null, false)
    }

    var callback: ((SearchFilterData) -> Unit)? = null

    fun show(currentSearchFilterData: SearchFilterData? = null) {
        ArrayAdapter.createFromResource(context, R.array.order_by, android.R.layout.simple_spinner_item).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.orderBy.adapter = it
        }

        ArrayAdapter.createFromResource(context, R.array.duration, android.R.layout.simple_spinner_item).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.duration.adapter = it
        }

        ArrayAdapter.createFromResource(context, R.array.upload_date, android.R.layout.simple_spinner_item).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.uploadDate.adapter = it
        }

        currentSearchFilterData?.also {
            binding.orderBy.setSelection(it.orderBy.ordinal)
            binding.duration.setSelection(it.duration.ordinal)
            binding.uploadDate.setSelection(it.uploadDate.ordinal)
            binding.featureSyndicated.isChecked = it.featureSyndicated
            binding.featureEmbeddable.isChecked = it.featureEmbeddable
            binding.featureEpisode.isChecked = it.featureEpisode
            binding.featureMovie.isChecked = it.featureMovie
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.label_search_filter)
            .setView(binding.root)
            .setPositiveButton(com.youtubemusic.core.common.R.string.label_apply) { _, _ ->

                callback?.invoke(
                    SearchFilterData(
                        orderBy = getOrderByEnum(),
                        duration = getDurationEnum(),
                        uploadDate = getUploadDateEnum(),
                        featureSyndicated = binding.featureSyndicated.isChecked,
                        featureEpisode = binding.featureEpisode.isChecked,
                        featureMovie = binding.featureMovie.isChecked,
                        featureEmbeddable = binding.featureEmbeddable.isChecked
                    )
                )
            }.setNegativeButton(com.youtubemusic.core.common.R.string.label_cancel) { _, _ -> }
            .show()
    }

    private fun getOrderByEnum(): OrderEnum {
        return OrderEnum.values().getOrNull(binding.orderBy.selectedItemPosition)
            ?: throw IllegalStateException("Unhandled OrderBy position: ${binding.orderBy.selectedItemPosition}")
    }

    private fun getDurationEnum(): DurationEnum {
        return DurationEnum.values().getOrNull(binding.duration.selectedItemPosition)
            ?: throw IllegalStateException("Unhandled Duration position: ${binding.duration.selectedItemPosition}")
    }

    private fun getUploadDateEnum(): UploadDateEnum {
        return UploadDateEnum.values().getOrNull(binding.uploadDate.selectedItemPosition)
            ?: throw IllegalStateException("Unhandled Duration position: ${binding.uploadDate.selectedItemPosition}")
    }
}