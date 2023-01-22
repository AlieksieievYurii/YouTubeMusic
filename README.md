# YouTube Music
Simple Android application to download and listen music from YouTube. 
The application consists of two main parts: 
* [Saved Music](#saved_music)
* [YouTube Music](#youtube_music_downloader)

I also use this application to integrate new things I am learning in Android Development. 

## <a name="saved_music"></a> Saved Music
This fragment is responsible for browsing downloaded media items and playing them. The fragment works along with [MediaBrowserService](https://developer.android.com/guide/topics/media-apps/audio-app/building-an-audio-app) so the app can also work with smartwatches and cars.

<img src="gitRes/2.jpg" width=200>&nbsp;

You can create local playlists and add media items to them:

You are also able to change media item possition in visible playlist. (When new media item is downloaded, it is added at the latest position)



## <a name="youtube_music_downloader"></a> YouTube Music
The fragment is responsible for downloading music from YouTube. The app just fetches audio track from the video.

**Downloading musics from YouTube is **not stable**, because Youtube often changes web structure of its pages.**

**WARNING**: Youtube API does not support a video download. In fact, it is prohibited - [Terms of Service - II. Prohibitions](https://developers.google.com/youtube/terms/api-services-terms-of-service). 
<br>**WARNING**: Downloading videos may violate copyrights! 
<br><br>This application is only for educational purposes. I urge not to use this project to violate any laws.
