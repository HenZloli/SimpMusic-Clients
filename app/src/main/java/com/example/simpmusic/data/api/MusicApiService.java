package com.example.simpmusic.data.api;

import com.example.simpmusic.data.model.AddSongRequest;
import com.example.simpmusic.data.model.Album;
import com.example.simpmusic.data.model.Artist;
import com.example.simpmusic.data.model.AuthResponse;
import com.example.simpmusic.data.model.LoginRequest;
import com.example.simpmusic.data.model.Notification;
import com.example.simpmusic.data.model.Playlist;
import com.example.simpmusic.data.model.RegisterRequest;
import com.example.simpmusic.data.model.Song;
import com.example.simpmusic.data.model.ViewCountResponse;
import com.example.simpmusic.data.model.UnreadCountResponse;
import com.example.simpmusic.data.model.ApiResponse;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface MusicApiService {

    @GET("api/Songs")
    Call<List<Song>> getAllSongs();

    @GET("api/Songs/search")
    Call<List<Song>> searchSongs(@Query("keyword") String keyword);

    @POST("api/Songs/{id}/view")
    Call<ViewCountResponse> increaseViewCount(@Path("id") int id);

    @GET("api/Songs/{id}")
    Call<Song> getSongById(@Path("id") int id);

    @GET("api/Songs/album/{albumId}")
    Call<List<Song>> getSongsByAlbum(@Path("albumId") int albumId);

    @GET("api/Songs/artist/{artistId}")
    Call<List<Song>> getSongsByArtist(@Path("artistId") int artistId);

    @Multipart
    @POST("api/Songs/upload")
    Call<Song> uploadSong(
            @Part("Title") RequestBody title,
            @Part("AlbumId") RequestBody albumId,
            @Part("Duration") RequestBody duration,
            @Part("Lyrics") RequestBody lyrics,
            @Part MultipartBody.Part audioFile,
            @Part MultipartBody.Part coverFile
    );

    @POST("api/Auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);

    @POST("api/Auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @GET("api/Playlists")
    Call<List<Playlist>> getPlaylists();

    @POST("api/Playlists")
    Call<Playlist> createPlaylist(@Body Playlist playlist);

    @GET("api/Playlists/{id}")
    Call<Playlist> getPlaylistById(@Path("id") int id);

    @PUT("api/Playlists/{id}")
    Call<Void> updatePlaylist(@Path("id") int id, @Body Playlist playlist);

    @DELETE("api/Playlists/{id}")
    Call<Void> deletePlaylist(@Path("id") int id);

    @POST("api/Playlists/{id}/songs")
    Call<Void> addSongToPlaylist(@Path("id") int id, @Body AddSongRequest request);

    @DELETE("api/Playlists/{id}/songs/{songId}")
    Call<Void> removeSongFromPlaylist(@Path("id") int id, @Path("songId") int songId);

    @GET("api/Albums")
    Call<List<Album>> getAllAlbums();

    @GET("api/Albums/{id}")
    Call<Album> getAlbumById(@Path("id") int id);

    @Multipart
    @POST("api/Albums")
    Call<Album> createAlbum(@Part("Title") RequestBody title, @Part("Description") RequestBody description, @Part MultipartBody.Part coverFile);

    @PUT("api/Albums/{id}")
    Call<Void> updateAlbum(@Path("id") int id, @Body Album album);

    @DELETE("api/Albums/{id}")
    Call<Void> deleteAlbum(@Path("id") int id);

    @GET("api/Albums/artist/{artistId}")
    Call<List<Album>> getAlbumsByArtist(@Path("artistId") int artistId);

    @GET("api/Library/albums")
    Call<List<Album>> getSavedAlbums();

    @POST("api/Library/albums/{albumId}")
    Call<Void> saveAlbum(@Path("albumId") int albumId);

    @DELETE("api/Library/albums/{albumId}")
    Call<Void> unsaveAlbum(@Path("albumId") int albumId);

    @GET("api/Library/albums/{albumId}/check")
    Call<Boolean> checkIfAlbumSaved(@Path("albumId") int albumId);

    // --- ARTIST ENDPOINTS ---
    @GET("api/Artists/{id}")
    Call<Artist> getArtistById(@Path("id") int id);

    @POST("api/Artists/{id}/follow")
    Call<Void> followArtist(@Path("id") int id);

    @DELETE("api/Artists/{id}/follow")
    Call<Void> unfollowArtist(@Path("id") int id);

    @GET("api/Artists/following")
    Call<List<Artist>> getFollowingArtists();

    @Multipart
    @PUT("api/Artists/profile")
    Call<Void> updateArtistProfile(
            @Part("Name") RequestBody name,
            @Part("Description") RequestBody description,
            @Part MultipartBody.Part avatarFile
    );

    // --- USER ENDPOINTS ---
    @GET("api/Users/profile")
    Call<Artist> getUserProfile();

    @Multipart
    @PUT("api/Users/profile")
    Call<Void> updateUserProfile(
            @Part MultipartBody.Part avatarFile
    );

    // --- NOTIFICATION ENDPOINTS ---
    @GET("api/Notifications")
    Call<List<Notification>> getNotifications();

    @GET("api/Notifications/unread-count")
    Call<UnreadCountResponse> getUnreadCount();

    @PUT("api/Notifications/{id}/read")
    Call<ApiResponse> markAsRead(@Path("id") int id);

    @PUT("api/Notifications/read-all")
    Call<ApiResponse> markAllAsRead();
}
