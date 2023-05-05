# YouTube Music
Simple Android application to download and listen media to files of YouTube videos.

I also use this application to integrate new things I am learning in Android Development. :)

<img src="https://user-images.githubusercontent.com/39415360/236561994-3c85cb27-deb6-4a8c-b98e-c6468652c06e.png" width=800>&nbsp;

Features:
* Find videos by search or user's playlists
* Media player
* Equalizer
* App's playlists
* Automatict downloading media files from user's YouTube playlists
* Changing media items positions
* Sharing URLs and downloaded media files

The application consists of two main parts: 
* [Saved](#saved_music)
* [YouTube](#youtube_music_downloader)

## <a name="saved_music"></a> Saved Music
This fragment is responsible for browsing downloaded media items and playing them. The fragment works along with [MediaBrowserService](https://developer.android.com/guide/topics/media-apps/audio-app/building-an-audio-app) so the app can also work with smartwatches and cars.

<img src="https://user-images.githubusercontent.com/39415360/236562935-c4fa4af4-41be-480f-9843-1784318ae02b.gif" width=200>

You can create app's playlists and add media items to them.

<img src="https://user-images.githubusercontent.com/39415360/236565257-5c61becf-9c16-44dd-9912-dad4069f5c3b.gif" width=200>&nbsp;

You are also able to change media item possition in a visible playlist. (When new media item is downloaded, it is added at the latest position)

<img src="https://user-images.githubusercontent.com/39415360/236566406-1617f4f1-e69f-4ea6-aee0-a1e95dee14d2.gif" width=200>&nbsp;

The app also contains simple Equalizer.

<img src="https://user-images.githubusercontent.com/39415360/236566847-391d969a-d53c-4e69-92dc-1fdad791ecec.gif" width=200>&nbsp;

## <a name="youtube_music_downloader"></a> YouTube Music
The fragment is responsible for downloading music from YouTube. The app just fetches audio track from the video.

<img src="https://user-images.githubusercontent.com/39415360/236569458-f6a23bd7-5a5e-4f5b-b0eb-50c337df934b.gif" width=200>&nbsp;

**Downloading musics from YouTube is **not stable**, because Youtube often changes web structure of its pages.**

## Project Compilation
The application uses Firebase that requires _google-services.json_. The project has a custom task that decrypts that file which is located in the project root
and is named _google-services.json.encrypt_. Before bulding the project, you must setup environment variable `ANDROID_ENCRYPTION_KEY` pointing the key used during
encryption. The decryption task is added to the build graph dependencies and is called before preBuild.

## Modularization
![image](https://user-images.githubusercontent.com/39415360/230973497-66c2872c-d180-40b4-9118-536a6fab1648.png)

## Note:
**WARNING**: Youtube API does not support a video download. In fact, it is prohibited - [Terms of Service - II. Prohibitions](https://developers.google.com/youtube/terms/api-services-terms-of-service). 
<br>**WARNING**: Downloading videos may violate copyrights! 
<br><br>This application is only for educational purposes. I urge not to use this project to violate any laws.
