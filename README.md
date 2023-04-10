# YouTube Music
Simple Android application to download and listen music from YouTube. 
The application consists of two main parts: 
* [Saved Music](#saved_music)
* [YouTube Music](#youtube_music_downloader)

I also use this application to integrate new things I am learning in Android Development. 

## <a name="saved_music"></a> Saved Music
This fragment is responsible for browsing downloaded media items and playing them. The fragment works along with [MediaBrowserService](https://developer.android.com/guide/topics/media-apps/audio-app/building-an-audio-app) so the app can also work with smartwatches and cars.

<img src="https://user-images.githubusercontent.com/39415360/213927466-5bc29952-a2cf-426d-b53e-21092a3f9a15.jpg" width=200>

You can create local playlists and add media items to them.

<img src="https://user-images.githubusercontent.com/39415360/213928094-4bf7210e-e09e-4b48-8256-8cf1da89534a.gif" width=200>&nbsp;

You are also able to change media item possition in a visible playlist. (When new media item is downloaded, it is added at the latest position)

<img src="https://user-images.githubusercontent.com/39415360/213927942-ec34a0cf-4704-4de3-9e7b-97e860e09254.gif" width=200>&nbsp;

The app also contains simple Equalizer.

<img src="https://user-images.githubusercontent.com/39415360/213929291-0022ed91-7f11-47fb-835b-caf4d6e68026.gif" width=200>&nbsp;

## <a name="youtube_music_downloader"></a> YouTube Music
The fragment is responsible for downloading music from YouTube. The app just fetches audio track from the video.

<img src="https://user-images.githubusercontent.com/39415360/214073349-3ea0f44c-cad2-451b-ac8d-eca1575f9e56.gif" width=200>&nbsp;

**Downloading musics from YouTube is **not stable**, because Youtube often changes web structure of its pages.**

## Modularization
![image](https://user-images.githubusercontent.com/39415360/230972590-911505f5-7b56-433a-9d46-22d540ac354a.png)

## Note:
**WARNING**: Youtube API does not support a video download. In fact, it is prohibited - [Terms of Service - II. Prohibitions](https://developers.google.com/youtube/terms/api-services-terms-of-service). 
<br>**WARNING**: Downloading videos may violate copyrights! 
<br><br>This application is only for educational purposes. I urge not to use this project to violate any laws.
