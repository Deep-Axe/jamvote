package com.jamvote.app.music;

import android.util.Log;

import com.jamvote.app.BuildConfig;
import com.jamvote.app.model.Song;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class YouTubeProvider {

    private static final String TAG = "YouTubeProvider";
    private static final String BASE_URL = "https://www.googleapis.com/youtube/v3/search";

    public interface SearchCallback {
        void onSuccess(List<Song> songs);
        void onFailure(String error);
    }

    public static void searchVideos(String query, boolean sfwMode, String packageName, String sha1, SearchCallback callback) {
        new Thread(() -> {
            try {
                String encodedQuery = URLEncoder.encode(query, "UTF-8");
                String safeSearch = sfwMode ? "strict" : "none";
                
                StringBuilder urlBuilder = new StringBuilder(BASE_URL);
                urlBuilder.append("?part=snippet")
                        .append("&type=video")
                        .append("&maxResults=15")
                        .append("&videoEmbeddable=true")
                        .append("&safeSearch=").append(safeSearch)
                        .append("&q=").append(encodedQuery)
                        .append("&key=").append(BuildConfig.YOUTUBE_API_KEY);

                URL url = new URL(urlBuilder.toString());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                try {
                    conn.setRequestMethod("GET");
                    
                    // Security headers for restricted key
                    conn.setRequestProperty("X-Android-Package", packageName);
                    conn.setRequestProperty("X-Android-Cert", sha1);

                    int responseCode = conn.getResponseCode();
                    BufferedReader reader;
                    if (responseCode >= 200 && responseCode < 300) {
                        reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    } else {
                        reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                    }

                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    if (responseCode >= 200 && responseCode < 300) {
                        List<Song> results = new ArrayList<>();
                        JSONObject jsonResponse = new JSONObject(response.toString());
                        JSONArray items = jsonResponse.getJSONArray("items");
                        StringBuilder videoIds = new StringBuilder();

                        for (int i = 0; i < items.length(); i++) {
                            JSONObject item = items.getJSONObject(i);
                            JSONObject snippet = item.getJSONObject("snippet");
                            String videoId = item.getJSONObject("id").getString("videoId");
                            
                            Song song = new Song(
                                "yt_" + videoId,
                                videoId,
                                snippet.getString("title"),
                                snippet.getString("channelTitle")
                            );
                            song.setThumbnailUrl(snippet.getJSONObject("thumbnails").getJSONObject("medium").getString("url"));
                            results.add(song);
                            
                            if (i > 0) videoIds.append(",");
                            videoIds.append(videoId);
                        }

                        // Second call for contentDetails (Batch)
                        if (results.size() > 0) {
                            String detailsUrl = "https://www.googleapis.com/youtube/v3/videos?part=contentDetails&id=" 
                                    + videoIds.toString() + "&key=" + BuildConfig.YOUTUBE_API_KEY;
                            
                            URL vUrl = new URL(detailsUrl);
                            HttpURLConnection vConn = (HttpURLConnection) vUrl.openConnection();
                            try {
                                vConn.setRequestMethod("GET");
                                vConn.setRequestProperty("X-Android-Package", packageName);
                                vConn.setRequestProperty("X-Android-Cert", sha1);
                                
                                if (vConn.getResponseCode() == 200) {
                                    BufferedReader vReader = new BufferedReader(new InputStreamReader(vConn.getInputStream()));
                                    StringBuilder vResponse = new StringBuilder();
                                    String vLine;
                                    while ((vLine = vReader.readLine()) != null) vResponse.append(vLine);
                                    vReader.close();
                                    
                                    JSONObject vJson = new JSONObject(vResponse.toString());
                                    JSONArray vItems = vJson.getJSONArray("items");
                                    for (int i = 0; i < vItems.length(); i++) {
                                        JSONObject vItem = vItems.getJSONObject(i);
                                        String vId = vItem.getString("id");
                                        if (vItem.has("contentDetails")) {
                                            JSONObject cd = vItem.getJSONObject("contentDetails");
                                            if (cd.has("contentRating")) {
                                                JSONObject cr = cd.getJSONObject("contentRating");
                                                if (cr.has("ytRating") && "ytAgeRestricted".equals(cr.getString("ytRating"))) {
                                                    for (Song s : results) {
                                                        if (s.getYoutubeVideoId().equals(vId)) {
                                                            s.setExplicit(true);
                                                            break;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } finally {
                                vConn.disconnect();
                            }
                        }
                        callback.onSuccess(results);
                    } else {
                        callback.onFailure("API Error: " + responseCode + " - " + response.toString());
                    }
                } finally {
                    conn.disconnect();
                }

            } catch (Exception e) {
                Log.e(TAG, "Search failed", e);
                callback.onFailure(e.getMessage());
            }
        }).start();
    }
}