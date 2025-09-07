# ElevenLabs TTS Integration Guide

## Current TTS System

PlayerEngine already has a TTS system integrated:
- `TTSManager.java` - Manages TTS locking and timing
- `Player2APIService.textToSpeech()` - Currently calls Player2 API TTS endpoint
- `AgentSideEffects.java:45` - Triggers TTS when AI entities speak

## ElevenLabs Integration Options

### Option 1: Replace Player2APIService TTS Method

Modify `Player2APIService.java:85` to call ElevenLabs directly:

```java
public void textToSpeech(String message, Character character, Consumer<Map<String, JsonElement>> onFinish) {
    try {
        String apiKey = System.getenv("ELEVENLABS_API_KEY"); // Store API key securely
        String voiceId = getElevenLabsVoiceId(character);
        
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("text", message);
        requestBody.addProperty("model_id", "eleven_monolingual_v1");
        
        JsonObject voiceSettings = new JsonObject();
        voiceSettings.addProperty("stability", 0.5);
        voiceSettings.addProperty("similarity_boost", 0.5);
        voiceSettings.addProperty("style", 0.0);
        voiceSettings.addProperty("use_speaker_boost", true);
        requestBody.add("voice_settings", voiceSettings);
        
        // Make async HTTP request to ElevenLabs
        CompletableFuture.supplyAsync(() -> {
            return callElevenLabsAPI(voiceId, apiKey, requestBody);
        }).thenAccept(audioResponse -> {
            playAudioFile(audioResponse); // Play the returned MP3 audio
            onFinish.accept(new HashMap<>()); // Signal completion
        }).exceptionally(throwable -> {
            LOGGER.error("ElevenLabs TTS failed: " + throwable.getMessage());
            onFinish.accept(new HashMap<>());
            return null;
        });
        
    } catch (Exception e) {
        LOGGER.error("ElevenLabs TTS setup failed: " + e.getMessage());
        onFinish.accept(new HashMap<>());
    }
}

private String getElevenLabsVoiceId(Character character) {
    // Map character to ElevenLabs voice ID
    String characterName = character.name().toLowerCase();
    
    switch (characterName) {
        case "roderick": return "EXAVITQu4vr4xnSDxMaL"; // Example voice ID
        case "elena": return "ThT5KcBeYPX3keUQqHPh";   // Different character voice
        default: return "pNInz6obpgDQGcFmaJgB";        // Default voice
    }
}

private byte[] callElevenLabsAPI(String voiceId, String apiKey, JsonObject requestBody) throws IOException {
    String url = "https://api.elevenlabs.io/v1/text-to-speech/" + voiceId;
    
    HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
    connection.setRequestMethod("POST");
    connection.setRequestProperty("Accept", "audio/mpeg");
    connection.setRequestProperty("xi-api-key", apiKey);
    connection.setRequestProperty("Content-Type", "application/json");
    connection.setDoOutput(true);
    
    // Send request
    try (OutputStream os = connection.getOutputStream()) {
        byte[] input = requestBody.toString().getBytes("utf-8");
        os.write(input, 0, input.length);
    }
    
    // Read audio response
    if (connection.getResponseCode() == 200) {
        try (InputStream is = connection.getInputStream()) {
            return is.readAllBytes();
        }
    } else {
        throw new IOException("ElevenLabs API error: " + connection.getResponseCode());
    }
}

private void playAudioFile(byte[] audioData) {
    try {
        // Save audio to temporary file
        File tempFile = File.createTempFile("tts_", ".mp3");
        Files.write(tempFile.toPath(), audioData);
        
        // Play audio file using system audio player
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(tempFile);
        } else {
            // Alternative: use Java audio libraries or system command
            ProcessBuilder pb = new ProcessBuilder("mpg123", tempFile.getAbsolutePath());
            pb.start();
        }
        
        // Clean up temp file after delay
        CompletableFuture.delayedExecutor(10, TimeUnit.SECONDS).execute(() -> {
            tempFile.delete();
        });
        
    } catch (Exception e) {
        LOGGER.error("Failed to play TTS audio: " + e.getMessage());
    }
}
```

### Option 2: Create Separate ElevenLabsTTSManager

Create a new dedicated TTS manager:

```java
package adris.altoclef.player2api;

import com.google.gson.JsonObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

public class ElevenLabsTTSManager {
    private static final String BASE_URL = "https://api.elevenlabs.io/v1/text-to-speech/";
    private static final String API_KEY = System.getenv("ELEVENLABS_API_KEY");
    
    public static CompletableFuture<Void> speak(String text, String voiceId) {
        return CompletableFuture.runAsync(() -> {
            try {
                JsonObject requestBody = createTTSRequest(text);
                byte[] audioData = callAPI(voiceId, requestBody);
                playAudio(audioData);
            } catch (Exception e) {
                System.err.println("TTS failed: " + e.getMessage());
            }
        });
    }
    
    private static JsonObject createTTSRequest(String text) {
        JsonObject request = new JsonObject();
        request.addProperty("text", text);
        request.addProperty("model_id", "eleven_monolingual_v1");
        
        JsonObject voiceSettings = new JsonObject();
        voiceSettings.addProperty("stability", 0.5);
        voiceSettings.addProperty("similarity_boost", 0.5);
        request.add("voice_settings", voiceSettings);
        
        return request;
    }
    
    // ... API call and audio playback methods
}
```

### Option 3: Configuration-Based Approach

Add TTS configuration to allow switching between providers:

```java
// In Settings.java
private String ttsProvider = "elevenlabs"; // or "player2api"
private String elevenLabsApiKey = System.getenv("ELEVENLABS_API_KEY");
private Map<String, String> characterVoiceMapping = new HashMap<>();

// In Player2APIService.java
public void textToSpeech(String message, Character character, Consumer<Map<String, JsonElement>> onFinish) {
    String ttsProvider = Settings.getInstance().getTtsProvider();
    
    switch (ttsProvider.toLowerCase()) {
        case "elevenlabs":
            useElevenLabsTTS(message, character, onFinish);
            break;
        case "player2api":
        default:
            usePlayer2ApiTTS(message, character, onFinish);
            break;
    }
}
```

## Required Dependencies

Add HTTP client dependency to `build.gradle`:

```gradle
dependencies {
    // ... existing dependencies
    implementation 'org.apache.httpcomponents:httpclient:4.5.14'
    // OR use Java 11+ built-in HTTP client
}
```

## Environment Setup

1. **Set API Key**: `export ELEVENLABS_API_KEY="your_api_key_here"`
2. **Voice IDs**: Get voice IDs from ElevenLabs dashboard
3. **Audio Player**: Ensure system has MP3 player (mpg123, VLC, etc.)

## Integration Points

The TTS integration automatically triggers when:
- AI entity sends chat messages (`AgentSideEffects.java:45`)
- Character speaks after LLM response
- No code changes needed in existing TTS trigger points

## Voice Configuration

Map each character to specific ElevenLabs voices:

```java
Map<String, String> voiceMapping = Map.of(
    "roderick", "EXAVITQu4vr4xnSDxMaL",     // British male voice
    "elena", "ThT5KcBeYPX3keUQqHPh",        // Female voice  
    "guard", "pNInz6obpgDQGcFmaJgB"         // Gruff male voice
);
```

## Testing

1. **API Connection**: Test with simple HTTP request
2. **Audio Playback**: Verify system can play MP3 files
3. **Character Voices**: Test different character voice mappings
4. **Error Handling**: Test with invalid API keys, network issues

## Benefits

- **High-Quality TTS**: ElevenLabs provides more natural speech
- **Character Voices**: Different voices per AI entity/character
- **Real-time Generation**: Fast API response times
- **Emotion Support**: Advanced voice control and emotion
- **Multiple Languages**: Support for various languages

This integration will make your AI entities much more immersive with realistic speech!