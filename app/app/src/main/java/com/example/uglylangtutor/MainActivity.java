package com.example.uglylangtutor;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uglylangtutor.voicetutor.VoiceTutorAdapter;
import com.example.uglylangtutor.voicetutor.VoiceTutorManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener{
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech tts;
    private Button speakButton;
    private RecyclerView conversationRecyclerView;
    private List<VoiceTutorAdapter.Message> conversationHistory = new ArrayList<>();

    private ImageView meowImage;
    private VoiceTutorManager voiceTutorManager;
    private VoiceTutorAdapter adapter;
    private SeekBar speechRateSeekbar;
    private float currentSpeechRate = 1.0f;


    int[] spriteIds = {
            R.drawable.sprite_1_256, // neutra
            R.drawable.sprite_2_256, // A
            R.drawable.sprite_3_256, // O
            R.drawable.sprite_4_256, // E/I
            R.drawable.sprite_5_256, // U
            R.drawable.sprite_6_256  // extra o random
    };

    private Queue<SynthesisRequest> synthesisQueue = new LinkedList<>();
    private boolean isSynthesizing = false;

    public class SynthesisRequest {
        public final String text;
        public final Locale locale;
        public final int start;
        public final int end;
        public final int messageIndex;

        public SynthesisRequest(String text, Locale locale, int start, int end, int messageIndex) {
            this.text = text;
            this.locale = locale;
            this.start = start;
            this.end = end;
            this.messageIndex = messageIndex;
        }
    }

    public class TextSegment {
        public String text;
        public Locale locale;
        public int startIndex;
        public int endIndex;

        public TextSegment(String text, Locale locale, int startIndex, int endIndex) {
            this.text = text;
            this.locale = locale;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }
    }
    private List<TextSegment> segments = new ArrayList<>();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_tutor);

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 100);
        }

        meowImage = findViewById(R.id.meowImage);
        speakButton = findViewById(R.id.speak_button);
        conversationRecyclerView = findViewById(R.id.conversationRecyclerView);
        //adapter = new VoiceTutorAdapter(conversationHistory, text -> speakMultilingualText(text));
        adapter = new VoiceTutorAdapter(conversationHistory, (text) -> {
            int clickedPosition = adapter.getMessageIndexByText(text);
            speakMultilingualText(text, clickedPosition);
        });

        conversationRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        conversationRecyclerView.setAdapter(adapter);


        speechRateSeekbar = findViewById(R.id.speech_rate_seekbar);

        tts = new TextToSpeech(this, this);
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        voiceTutorManager = new VoiceTutorManager();


        speakButton.setOnClickListener(v -> startListening());
        speechRateSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float rate = progress / 10.0f;
                if (rate < 0.3f) rate = 0.3f;
                currentSpeechRate = rate;
                tts.setSpeechRate(rate);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        /*
        Button newConversationBtn = findViewById(R.id.btnNewConversation);
        newConversationBtn.setOnClickListener(v -> {
            startNewConversation();
        });
         */

    }

    private void startListening() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.setAction(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        //intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String userInput = matches.get(0);
                    //conversationHistory.add(new Message(true, userInput));
                    //adapter.notifyItemInserted(conversationHistory.size() - 1);
                    //conversationRecyclerView.scrollToPosition(conversationHistory.size() - 1);
                    addMessage(true, userInput);
                    processInput(userInput);
                }
            }

            @Override public void onReadyForSpeech(Bundle params) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onError(int error) {}
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });

        speechRecognizer.startListening(intent);
    }

    private void processInput(String userInput) {
        voiceTutorManager.sendUserMessage(userInput, false, new VoiceTutorManager.VoiceTutorCallback() {
            @Override
            public void onSuccess(String reply) {
                runOnUiThread(() -> {
                    //conversationHistory.add(new Message(false, reply));
                    //adapter.notifyItemInserted(conversationHistory.size() - 1);
                    //conversationRecyclerView.scrollToPosition(conversationHistory.size() - 1);
                    addMessage(false, reply);
                    int lastMessageIndex = conversationHistory.size() - 1;
                    speakMultilingualText(reply, lastMessageIndex);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    //conversationHistory.add(new Message(false, "[Errore]: " + error));
                    //adapter.notifyItemInserted(conversationHistory.size() - 1);
                    //conversationRecyclerView.scrollToPosition(conversationHistory.size() - 1);
                    addMessage(false, "[Error]: " + error);
                });
            }
        });
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            //tts.setLanguage(Locale.ENGLISH);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        speechRecognizer.destroy();
        tts.shutdown();
    }


    private void OLD_speakMultilingualText(String response, int messageIndex) {
        Log.d("VoiceTutorTTS", "📥 Risposta completa: " + response);
        segments.clear();

        Pattern langPattern = Pattern.compile("\\\\([a-z]{2})");
        //Pattern langPattern = Pattern.compile("(?<=^|[^a-zA-Z])\\\\([a-z]{2})");

        Matcher matcher = langPattern.matcher(response);

        int lastIndex = 0;
        int visibleCharIndex = 0;
        String currentLang = null;
        boolean foundAnyTaggedLanguage = false;

        while (matcher.find()) {
            if (currentLang != null) {
                String text = response.substring(lastIndex, matcher.start()); //non usare trim()!!!
                if (!text.isEmpty()) {
                    Locale locale = new Locale(currentLang);
                    foundAnyTaggedLanguage = true;

                    int start = visibleCharIndex;
                    int end = start + text.length();
                    //int end = start + text.replaceFirst("^\\\\[a-z]{2}", "").length();
                    Log.d("Segment", "testo raw: '" + text + "' start=" + start + " end=" + end);


                    segments.add(new TextSegment(text, locale, start, end));
                    visibleCharIndex = end;

                    enqueueSynthesis(text, locale, start, end, messageIndex);
                }
            }
            currentLang = matcher.group(1);
            lastIndex = matcher.end();
        }

        // Ultimo pezzo dopo l’ultimo tag
        if (currentLang != null && lastIndex < response.length()) {
            String text = response.substring(lastIndex); //non usare trim()!!!!
            if (!text.isEmpty()) {
                Locale locale = new Locale(currentLang);
                foundAnyTaggedLanguage = true;

                int start = visibleCharIndex;
                int end = start + text.length();
                //int end = start + text.replaceFirst("^\\\\[a-z]{2}", "").length();
                Log.d("Segment", "testo raw: '" + text + "' start=" + start + " end=" + end);


                segments.add(new TextSegment(text, locale, start, end));
                visibleCharIndex = end;

                enqueueSynthesis(text, locale, start, end, messageIndex);
            }
        }

        // Nessun tag trovato
        if (!foundAnyTaggedLanguage && !response.trim().isEmpty()) {
            Locale locale = new Locale("en");
            segments.add(new TextSegment(response, locale, 0, response.length()));
            enqueueSynthesis(response, locale, 0, response.length(), messageIndex);
        }
    }

    private void speakMultilingualText(String response, int messageIndex) {
        Log.d("VoiceTutorTTS", "📥 Risposta completa: " + response);
        segments.clear();

        // Aggiungiamo tutto il testo così com'è, senza split/tag
        int length = response.length();
        segments.add(new TextSegment(response, null, 0, length));
        enqueueSynthesis(response, null, 0, length, messageIndex);
    }


    private void startNewConversation() {
        voiceTutorManager.sendUserMessage("", true, new VoiceTutorManager.VoiceTutorCallback() {
            @Override
            public void onSuccess(String reply) {
                runOnUiThread(() -> {
                    conversationHistory.clear();
                    adapter.notifyDataSetChanged();
                    //conversationHistory.add(new Message(false, "[Nuova conversazione iniziata]"));
                    //adapter.notifyItemInserted(conversationHistory.size() - 1);
                    ///conversationRecyclerView.scrollToPosition(conversationHistory.size() - 1);
                    addMessage(false, "[Nuova conversazione iniziata]");
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    //conversationHistory.add(new Message(false, "[Errore]: " + error));
                    //adapter.notifyItemInserted(conversationHistory.size() - 1);
                    //conversationRecyclerView.scrollToPosition(conversationHistory.size() - 1);
                    addMessage(false, "[Errore]: " + error);
                });
            }
        });

    }

    private void addMessage(boolean fromUser, String text) {
        //String cleanedText = text.replaceAll("\\\\[a-z]{2}", ""); // rimuove i tag \it, \en, ecc.
        conversationHistory.add(new VoiceTutorAdapter.Message(fromUser, text));
        adapter.notifyItemInserted(conversationHistory.size() - 1);
        conversationRecyclerView.scrollToPosition(conversationHistory.size() - 1);
    }







    private void animateMouth(List<Integer> energies, WavInfo info) {
        new Thread(() -> {
            int bytesPerSample = info.bitsPerSample / 8;
            int frameSize = bytesPerSample * info.numChannels;
            int bufferLength = 2048; // come nel read
            int framesPerBuffer = bufferLength / frameSize;
            long frameDurationMs = (long) ((framesPerBuffer * 1000f) / info.sampleRate);

            for (int energy : energies) {
                int spriteId = getSpriteIdFromEnergy(energy);

                runOnUiThread(() -> meowImage.setImageResource(spriteId));

                try {
                    Thread.sleep(frameDurationMs);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private int getSpriteIdFromEnergy(int energy) {
        if (energy < 15000) return spriteIds[0];        // neutra
        else if (energy < 30000) return spriteIds[1];   // lieve apertura
        else if (energy < 50000) return spriteIds[2];   // A
        else if (energy < 70000) return spriteIds[3];   // E/I
        else if (energy < 100000) return spriteIds[4];  // U
        else return spriteIds[5];                       // massima apertura
    }



    private void enqueueSynthesis(String text, Locale locale, int start, int end, int messageIndex) {
        synthesisQueue.add(new SynthesisRequest(text, locale, start, end, messageIndex));
        if (!isSynthesizing) {
            processNextSynthesis();
        }
    }


    private void processNextSynthesis() {
        SynthesisRequest request = synthesisQueue.poll();
        if (request == null) {
            isSynthesizing = false;
            return;
        }

        isSynthesizing = true;

        File outputFile = new File(getExternalCacheDir(), "tts_output_" + System.currentTimeMillis() + ".wav");
        String utteranceId = UUID.randomUUID().toString();

        Locale locale = request.locale;
        if (locale != null) {
            Log.d("VoiceTutorTTS", "🌍 Forcing locale: " + locale);
            tts.setLanguage(locale);
        } else {
            Log.d("VoiceTutorTTS", "🌍 No locale forced, letting TTS autodetect");
        }

        synchronized (tts) {
            tts.setLanguage(locale);

            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override public void onStart(String id) {
                    if (id.equals(utteranceId)) {
                        runOnUiThread(() -> {
                            adapter.highlightSegment(request.messageIndex, request.start, request.end);
                        /*
                        conversationRecyclerView.post(() -> {
                            RecyclerView.LayoutManager lm = conversationRecyclerView.getLayoutManager();
                            if (lm instanceof LinearLayoutManager) {
                                ((LinearLayoutManager) lm).scrollToPositionWithOffset(request.messageIndex, 0);
                            }
                        });
                         */

                        });
                    }
                }

                @Override
                public void onDone(String id) {
                    if (!id.equals(utteranceId)) return;

                    runOnUiThread(() -> {
                        waitForFileAndPlay(outputFile, () -> {
                            adapter.resetHighlight();
                            isSynthesizing = false;
                            processNextSynthesis();
                        });
                    });
                }

                @Override
                public void onError(String id) {
                    if (!id.equals(utteranceId)) return;

                    runOnUiThread(() -> {
                        adapter.resetHighlight();
                        Log.e("VoiceTutorTTS", "❌ Errore TTS su " + id);
                        isSynthesizing = false;
                        processNextSynthesis();
                    });
                }
            });

            tts.synthesizeToFile(request.text, null, outputFile, utteranceId);
        }
    }



    public void playAudioAndThen(File audioFile, Runnable onDone) {
        try {
            AudioAnalysisResult result = analyzeAudioEnergyWithInfo(audioFile);
            animateMouth(result.energies, result.wavInfo);

            MediaPlayer player = new MediaPlayer();
            player.setDataSource(audioFile.getAbsolutePath());
            player.prepare();
            player.setOnCompletionListener(mp -> {
                mp.release();
                onDone.run();
            });
            player.start();
        } catch (IOException e) {
            Log.e("VoiceTutorTTS", "❌ Errore in playAudioAndThen", e);
            onDone.run();
        }
    }

    private void waitForFileAndPlay(File file, Runnable onComplete) {
        final int maxAttempts = 20;
        final int delayMs = 100;

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            int attempts = 0;

            @Override
            public void run() {
                if (file.exists() && file.length() > 1024) { // o soglia empirica
                    playAudioAndThen(file, onComplete);
                } else if (attempts < maxAttempts) {
                    attempts++;
                    new Handler(Looper.getMainLooper()).postDelayed(this, delayMs);
                } else {
                    Log.w("VoiceTutorTTS", "⚠️ File non pronto dopo vari tentativi");
                    onComplete.run(); // Evitiamo blocco
                }
            }
        });
    }



    public class WavInfo {
        public int sampleRate;
        public int bitsPerSample;
        public int numChannels;
    }

    public WavInfo analyzeWavHeader(File wavFile) throws IOException {
        WavInfo info = new WavInfo();

        try (FileInputStream fis = new FileInputStream(wavFile)) {
            byte[] header = new byte[44];
            int read = fis.read(header);
            if (read < 44) throw new IOException("Header WAV troppo corto");

            info.sampleRate = ((header[27] & 0xFF) << 24) | ((header[26] & 0xFF) << 16)
                    | ((header[25] & 0xFF) << 8) | (header[24] & 0xFF);

            info.bitsPerSample = ((header[35] & 0xFF) << 8) | (header[34] & 0xFF);

            info.numChannels = ((header[23] & 0xFF) << 8) | (header[22] & 0xFF);
        }

        return info;
    }

    public class AudioAnalysisResult {
        public List<Integer> energies;
        public WavInfo wavInfo;

        public AudioAnalysisResult(List<Integer> energies, WavInfo wavInfo) {
            this.energies = energies;
            this.wavInfo = wavInfo;
        }
    }


    public AudioAnalysisResult analyzeAudioEnergyWithInfo(File audioFile) throws IOException {
        List<Integer> energies = new ArrayList<>();

        if (!audioFile.exists()) {
            Log.e("VoiceTutorTTS", "🚫 File does not exist: " + audioFile.getAbsolutePath());
            return new AudioAnalysisResult(energies, null);
        }

        WavInfo info = analyzeWavHeader(audioFile);
        Log.d("VoiceTutorTTS", "📊 Wav Info: SampleRate=" + info.sampleRate +
                " | Bits=" + info.bitsPerSample + " | Channels=" + info.numChannels);

        try (FileInputStream fis = new FileInputStream(audioFile)) {
            byte[] buffer = new byte[2048];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                int energy = 0;
                for (int i = 0; i < read; i++) {
                    energy += Math.abs(buffer[i]);
                }
                energies.add(energy);
            }
        } catch (IOException e) {
            Log.e("VoiceTutorTTS", "❌ Error analyzing audio", e);
        }

        return new AudioAnalysisResult(energies, info);
    }


}
