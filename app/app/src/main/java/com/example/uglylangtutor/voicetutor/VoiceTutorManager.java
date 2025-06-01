package com.example.uglylangtutor.voicetutor;

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VoiceTutorManager {

    private static final String TAG = "VoiceTutorManager";
    // TODO: Replace with your actual API Gateway endpoint
    private static final String API_BASE_URL = "https://<your-api-id>.execute-api.<region>.amazonaws.com/<stage>";


    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public interface VoiceTutorCallback {
        void onSuccess(String reply);
        void onError(String error);
    }

    public VoiceTutorManager() {
        // Nessuna inizializzazione necessaria
    }

    public void sendUserMessage(String userInput, boolean newConversation, VoiceTutorCallback callback) {
        executorService.execute(() -> {
            try {
                Log.d(TAG, "🚀 Inizio richiesta getVoiceReply...");
                JSONObject payload = new JSONObject();
                payload.put("text", userInput);
                if (newConversation) {
                    payload.put("new_conversation", true);
                }

                //URL url = new URL(API_BASE_URL + "/assistantReplier");
                URL url = new URL(API_BASE_URL + "/uglyLangReply");

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(60000);
                conn.setReadTimeout(60000);

                OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
                writer.write(payload.toString());
                writer.flush();
                writer.close();

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "📥 responseCode: " + responseCode);

                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    conn.disconnect();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                        String reply = jsonResponse.getString("reply");
                    callback.onSuccess(reply);
                } else {
                    callback.onError("Backend error: " + responseCode);
                }
            } catch (Exception e) {
                callback.onError("Exception: " + e.getMessage());
            }
        });
    }
}
