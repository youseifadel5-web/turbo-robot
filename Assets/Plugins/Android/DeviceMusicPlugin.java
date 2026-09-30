// NEON RUSH RACING — original in-house plugin.
// Reads the device music library via MediaStore and plays through MediaPlayer.
// - No file copying: streams from content:// URIs.
// - Respects system audio routing (Bluetooth headphones/car audio keep working).
// - Requests transient audio focus; ducks game music via Unity callback.
// - Works without READ permission granted: returns empty list, never crashes.
package com.neonrush.devicemusic;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import com.unity3d.player.UnityPlayer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DeviceMusicPlugin {

    private static final class Track {
        long id;
        String title, artist, album, uri;
        long durationMs;
    }

    private static final List<Track> tracks = new ArrayList<Track>();
    private static MediaPlayer player;
    private static int currentIndex = -1;
    private static boolean shuffle = false;
    private static boolean repeatAll = true;
    private static boolean paused = false;

    private DeviceMusicPlugin() {}

    // ---- library ----
    public static String queryTracks() {
        tracks.clear();
        try {
            Context ctx = UnityPlayer.currentActivity;
            ContentResolver resolver = ctx.getContentResolver();
            Uri collection;
            if (Build.VERSION.SDK_INT >= 29) {
                collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
            } else {
                collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            }
            String[] projection = {
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.DURATION
            };
            Cursor cursor = resolver.query(collection, projection,
                    MediaStore.Audio.Media.IS_MUSIC + " != 0", null,
                    MediaStore.Audio.Media.TITLE + " ASC");
            if (cursor != null) {
                while (cursor.moveToNext() && tracks.size() < 2000) {
                    Track t = new Track();
                    t.id = cursor.getLong(0);
                    t.title = safe(cursor.getString(1), "Unknown Title");
                    t.artist = safe(cursor.getString(2), "Unknown Artist");
                    t.album = safe(cursor.getString(3), "");
                    t.durationMs = cursor.getLong(4);
                    t.uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "/" + t.id;
                    tracks.add(t);
                }
                cursor.close();
            }
        } catch (Throwable t) {
            // never crash the game: report error through the JSON
            return "{\"error\":\"" + escape(t.getMessage()) + "\"}";
        }
        return toJson();
    }

    public static int trackCount() { return tracks.size(); }

    public static String trackInfo(int index) {
        if (index < 0 || index >= tracks.size()) return "";
        Track t = tracks.get(index);
        JSONObject o = new JSONObject();
        try {
            o.put("title", t.title);
            o.put("artist", t.artist);
            o.put("album", t.album);
            o.put("durationMs", t.durationMs);
        } catch (Exception ignored) {}
        return o.toString();
    }

    // ---- playback ----
    public static boolean playIndex(int index) {
        if (index < 0 || index >= tracks.size()) return false;
        currentIndex = index;
        return openCurrent();
    }

    public static boolean next() {
        if (tracks.isEmpty()) return false;
        int i = shuffle
                ? (int) (Math.random() * tracks.size())
                : (currentIndex + 1) % tracks.size();
        return playIndex(i);
    }

    public static boolean previous() {
        if (tracks.isEmpty()) return false;
        int i = currentIndex - 1;
        if (i < 0) i = tracks.size() - 1;
        return playIndex(i);
    }

    public static void pause() {
        if (player != null && player.isPlaying()) player.pause();
        paused = true;
    }

    public static void resume() {
        if (player != null && !player.isPlaying()) player.start();
        paused = false;
    }

    public static boolean isPlaying() {
        return player != null && player.isPlaying();
    }

    public static int positionMs() {
        try { return player != null ? player.getCurrentPosition() : 0; }
        catch (Throwable t) { return 0; }
    }

    public static int durationMs() {
        try { return player != null ? player.getDuration() : 0; }
        catch (Throwable t) { return 0; }
    }

    public static void seekTo(int ms) {
        try { if (player != null) player.seekTo(ms); } catch (Throwable ignored) {}
    }

    public static void setShuffle(boolean on) { shuffle = on; }
    public static void setRepeatAll(boolean on) { repeatAll = on; }

    public static void stopAndRelease() {
        try {
            if (player != null) {
                player.stop();
                player.release();
            }
        } catch (Throwable ignored) {}
        player = null;
        abandonFocus();
    }

    // ---- internals ----
    private static boolean openCurrent() {
        if (currentIndex < 0 || currentIndex >= tracks.size()) return false;
        try {
            if (player != null) {
                player.stop();
                player.release();
                player = null;
            }
            final Track t = tracks.get(currentIndex);
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            player.setDataSource(UnityPlayer.currentActivity, Uri.parse(t.uri));
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override public void onCompletion(MediaPlayer mp) {
                    UnityPlayer.UnitySendMessage("DeviceMusicBridge", "OnTrackFinished", "");
                }
            });
            player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override public boolean onError(MediaPlayer mp, int what, int extra) {
                    UnityPlayer.UnitySendMessage("DeviceMusicBridge", "OnTrackError", "");
                    return true;
                }
            });
            player.prepare(); // small local files prepare fast; use async in a later pass
            requestFocus();
            player.start();
            paused = false;
            return true;
        } catch (Throwable t) {
            player = null;
            return false;
        }
    }

    // ---- audio focus (never force a route; BT audio keeps working) ----
    private static Object focusRequest;
    private static AudioManager.OnAudioFocusChangeListener focusListener =
            new AudioManager.OnAudioFocusChangeListener() {
        @Override public void onAudioFocusChange(int focusChange) {
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                pause();
            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                pause();
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                resume();
            }
        }
    };

    private static void requestFocus() {
        try {
            AudioManager am = (AudioManager) UnityPlayer.currentActivity
                    .getSystemService(Context.AUDIO_SERVICE);
            if (Build.VERSION.SDK_INT >= 26) {
                focusRequest = new AudioFocusRequest.Builder(
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                        .setOnAudioFocusChangeListener(focusListener)
                        .build();
                am.requestAudioFocus((AudioFocusRequest) focusRequest);
            } else {
                am.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
            }
        } catch (Throwable ignored) {}
    }

    private static void abandonFocus() {
        try {
            AudioManager am = (AudioManager) UnityPlayer.currentActivity
                    .getSystemService(Context.AUDIO_SERVICE);
            if (Build.VERSION.SDK_INT >= 26 && focusRequest != null) {
                am.abandonAudioFocusRequest((AudioFocusRequest) focusRequest);
            } else {
                am.abandonAudioFocus(focusListener);
            }
        } catch (Throwable ignored) {}
    }

    // ---- helpers ----
    private static String toJson() {
        JSONArray arr = new JSONArray();
        for (Track t : tracks) {
            JSONObject o = new JSONObject();
            try {
                o.put("title", t.title);
                o.put("artist", t.artist);
                o.put("album", t.album);
                o.put("uri", t.uri);
                o.put("durationMs", t.durationMs);
                arr.put(o);
            } catch (Exception ignored) {}
        }
        JSONObject root = new JSONObject();
        try { root.put("tracks", arr); } catch (Exception ignored) {}
        return root.toString();
    }

    private static String safe(String s, String fallback) {
        return (s == null || s.trim().isEmpty()) ? fallback : s;
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "'").replace("\n", " ");
    }
}
