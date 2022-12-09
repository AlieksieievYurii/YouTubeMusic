package com.yurii.youtubemusic.screens.saved.mediaitems
//
//import android.content.Context
//import android.support.v4.media.session.PlaybackStateCompat
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.core.content.ContextCompat
//import androidx.core.view.isVisible
//import androidx.databinding.DataBindingUtil
//import androidx.recyclerview.widget.RecyclerView
//import com.yurii.youtubemusic.R
//import com.yurii.youtubemusic.databinding.ItemMusicBinding
//import com.yurii.youtubemusic.screens.saved.service.PLAYBACK_STATE_MEDIA_ITEM
//import com.yurii.youtubemusic.screens.saved.service.PLAYBACK_STATE_PLAYING_CATEGORY_NAME
//import com.yurii.youtubemusic.models.Category
//import com.yurii.youtubemusic.models.MediaMetaData
//
//interface MediaListAdapterController {
//    fun onChangePlaybackState(playbackStateCompat: PlaybackStateCompat)
//    fun setMediaItems(list: List<MediaMetaData>)
//    fun removeItemWithId(id: String)
//    fun addNewMediaItem(mediaItem: MediaMetaData)
//    fun updateMediaItem(mediaItem: MediaMetaData)
//    fun contains(mediaId: String): Boolean
//    fun isEmptyList(): Boolean
//}
//
//class MediaListAdapter_deprecated(context: Context, private val category: Category, private val callback: CallBack) :
//    RecyclerView.Adapter<MediaListAdapter_deprecated.MusicViewHolder>(), MediaListAdapterController {
//    interface CallBack {
//        fun getPlaybackState(mediaItem: MediaMetaData): PlaybackStateCompat?
//        fun onOptionsClick(mediaItem: MediaMetaData, view: View)
//        fun onItemClick(mediaItem: MediaMetaData)
//    }
//
//    private val inflater: LayoutInflater = LayoutInflater.from(context)
//    private val musics = mutableListOf<MediaMetaData>()
//    private lateinit var recyclerView: RecyclerView
//
//    override fun setMediaItems(list: List<MediaMetaData>) {
//        musics.clear()
//        musics.addAll(list)
//        notifyDataSetChanged()
//    }
//
//    override fun removeItemWithId(id: String) {
//        musics.find { it.mediaId == id }?.run {
//            notifyItemRemoved(musics.indexOf(this))
//            musics.remove(this)
//        }
//    }
//
//    override fun addNewMediaItem(mediaItem: MediaMetaData) {
//        musics.add(mediaItem)
//        notifyItemInserted(musics.indexOf(mediaItem))
//        findVideoItemView(mediaItem) {
//            callback.getPlaybackState(mediaItem)?.run {
//                it.setPlaybackState(this)
//                it.setHintPlayingCategory(category, this)
//            }
//        }
//    }
//
//    override fun updateMediaItem(mediaItem: MediaMetaData) {
//        musics.find { it.mediaId == mediaItem.mediaId }?.run {
//            musics[musics.indexOf(this)] = mediaItem
//            findVideoItemView(this) {
//                it.setMusicItem(mediaItem, callback)
//            }
//        }
//    }
//
//    override fun contains(mediaId: String): Boolean = musics.find { it.mediaId == mediaId } != null
//    override fun isEmptyList(): Boolean = musics.isEmpty()
//
//    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
//        super.onAttachedToRecyclerView(recyclerView)
//        this.recyclerView = recyclerView
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {
//        return MusicViewHolder(DataBindingUtil.inflate(inflater, R.layout.item_music, parent, false))
//    }
//
//    override fun getItemCount(): Int = musics.size
//
//    override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {
//        val mediaItem = musics[position]
//        holder.setMusicItem(mediaItem, callback)
//        callback.getPlaybackState(mediaItem)?.run {
//            holder.setPlaybackState(this)
//            holder.setHintPlayingCategory(category, this)
//        } ?: holder.setNoneState()
//
//    }
//
//    private fun findVideoItemView(mediaItem: MediaMetaData, onFound: ((MusicViewHolder) -> Unit)) {
//        for (index: Int in 0 until recyclerView.childCount) {
//            val position = recyclerView.getChildAdapterPosition(recyclerView.getChildAt(index))
//
//            if (position == RecyclerView.NO_POSITION || musics.isEmpty())
//                continue
//
//            if (musics[position].mediaId == mediaItem.mediaId) {
//                val viewHolder = recyclerView.getChildViewHolder(recyclerView.getChildAt(index)) as MusicViewHolder
//                onFound.invoke(viewHolder)
//                return
//            }
//        }
//    }
//
//    private fun resetItemsState() {
//        getVisibleItems().forEach {
//            it.setNoneState()
//        }
//    }
//
//    private fun getVisibleItems(): List<MusicViewHolder> = ArrayList<MusicViewHolder>().apply {
//        for (index: Int in 0 until recyclerView.childCount) {
//            val position = recyclerView.getChildAdapterPosition(recyclerView.getChildAt(index))
//
//            if (position == RecyclerView.NO_POSITION || musics.isEmpty())
//                continue
//
//            val viewHolder = recyclerView.getChildViewHolder(recyclerView.getChildAt(index)) as MusicViewHolder
//            add(viewHolder)
//        }
//    }
//
//    override fun onChangePlaybackState(playbackStateCompat: PlaybackStateCompat) {
//        resetItemsState()
//        playbackStateCompat.extras?.getParcelable<MediaMetaData>(PLAYBACK_STATE_MEDIA_ITEM)?.run {
//            findVideoItemView(this) {
//                it.setPlaybackState(playbackStateCompat)
//                it.setHintPlayingCategory(category, playbackStateCompat)
//            }
//        }
//    }
//
//    class MusicViewHolder(private val itemMusicBinding: ItemMusicBinding) : RecyclerView.ViewHolder(itemMusicBinding.root) {
//        fun setMusicItem(mediaItem: MediaMetaData, callBack: CallBack) {
//            itemMusicBinding.apply {
//                this.musicItem = mediaItem
//            }.executePendingBindings()
//
//            itemMusicBinding.root.setOnClickListener {
//                callBack.onItemClick(mediaItem)
//            }
//
//            itemMusicBinding.moreOptions.setOnClickListener {
//                callBack.onOptionsClick(mediaItem, it)
//            }
//        }
//
//        fun setNoneState() {
//            itemMusicBinding.hintPlayingCategory.isVisible = false
//            itemMusicBinding.container.setCardBackgroundColor(ContextCompat.getColor(itemMusicBinding.container.context, R.color.white))
//            itemMusicBinding.thumbnailState.apply {
//                isVisible = false
//            }
//        }
//
//        fun setPlaybackState(playbackStateCompat: PlaybackStateCompat) {
//            when (playbackStateCompat.state) {
//                PlaybackStateCompat.STATE_PLAYING -> setPlayingState()
//                PlaybackStateCompat.STATE_PAUSED -> setPausedState()
//            }
//        }
//
//        fun setHintPlayingCategory(category: Category, playbackStateCompat: PlaybackStateCompat) {
//            val categoryName = playbackStateCompat.extras?.getString(PLAYBACK_STATE_PLAYING_CATEGORY_NAME)
//            if (category.name != categoryName)
//                itemMusicBinding.hintPlayingCategory.apply {
//                    isVisible = true
//                    text = itemMusicBinding.container.context.getString(R.string.label_playing_from, categoryName)
//                }
//        }
//
//        private fun setPlayingState() {
//            val context = itemMusicBinding.root.context
//            itemMusicBinding.thumbnailState.apply {
//                isVisible = true
//                setImageDrawable(context.getDrawable(R.drawable.ic_pause_24px))
//            }
//            itemMusicBinding.container.setCardBackgroundColor(ContextCompat.getColor(context, R.color.lightGray))
//        }
//
//        private fun setPausedState() {
//            val context = itemMusicBinding.root.context
//            itemMusicBinding.thumbnailState.apply {
//                isVisible = true
//                setImageDrawable(context.getDrawable(R.drawable.ic_play_24dp))
//            }
//            itemMusicBinding.container.setCardBackgroundColor(ContextCompat.getColor(context, R.color.lightGray))
//        }
//    }
//}