package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatbotActivity extends AppCompatActivity {

    private static final String TAG = "ChatbotActivity";
    private static final String API_KEY = BuildConfig.API_KEY;

    // CORRECTED: v1 endpoint + stable model

    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + API_KEY;
    private static final String SYSTEM_INSTRUCTION =
            "You are AgroSense AI, a highly experienced and friendly agricultural expert specializing in crop science, soil health, pest management, and smart irrigation. Provide clear, concise, and helpful advice to farmers. Maintain a supportive and professional tone and always respond in the language specified by the user's language code. If the user's language is English, respond in English.";

    private BottomNavigationView bottomNavigationView;
    private Spinner spinnerLanguage;
    private EditText inputMessage;
    private ImageButton btnSendMessage;
    private RecyclerView recyclerViewChat;

    private RequestQueue requestQueue;
    private MessageAdapter messageAdapter;
    private List<Message> messageList;
    private ImageButton btnVoiceInput;
    private static final int REQUEST_CODE_SPEECH = 100;

    private boolean systemPromptAdded = false;  // Track if system prompt is added

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        requestQueue = Volley.newRequestQueue(this);
        initializeViews();
        setupLanguageSpinner();
        setupChatRecyclerView();
        setupBottomNavigation();
        setupChatInput();

        // Welcome message
        messageAdapter.addMessage(new Message("Hello! I'm your AI Farm Assistant. Please select your preferred language above. How can I help you today?", Message.TYPE_BOT));
        recyclerViewChat.scrollToPosition(messageAdapter.getItemCount() - 1);
    }

    private void initializeViews() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        spinnerLanguage = findViewById(R.id.spinnerLanguage);
        inputMessage = findViewById(R.id.inputMessage);
        btnSendMessage = findViewById(R.id.btnSendMessage);
        recyclerViewChat = findViewById(R.id.recyclerViewChat);
    }

    private void setupLanguageSpinner() {
        String[] languages = {"English (en)", "Hindi (hi)", "Marathi (mr)", "Tamil (ta)", "Russian (ru)"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, languages);
        adapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        spinnerLanguage.setAdapter(adapter);
    }

    private void setupChatRecyclerView() {
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerViewChat.setLayoutManager(layoutManager);
        recyclerViewChat.setAdapter(messageAdapter);
    }

    private void setupChatInput() {
        btnSendMessage.setOnClickListener(v -> {
            String query = inputMessage.getText().toString().trim();
            if (query.isEmpty()) {
                Toast.makeText(this, "Please type a message.", Toast.LENGTH_SHORT).show();
                return;
            }

            String langCode = getSelectedLanguageCode();

            // Show user message
            messageAdapter.addMessage(new Message(query, Message.TYPE_USER));
            recyclerViewChat.scrollToPosition(messageAdapter.getItemCount() - 1);
            inputMessage.setText("");

            sendQueryToChatbot(query, langCode);
        });
    }

    private String getSelectedLanguageCode() {
        String selected = spinnerLanguage.getSelectedItem().toString();
        if (selected.contains("(")) {
            return selected.substring(selected.indexOf('(') + 1, selected.indexOf(')'));
        }
        return "en";
    }

    private void sendQueryToChatbot(String query, String targetLangCode) {
        if (API_KEY.isEmpty() || API_KEY.length() < 30) {
            Toast.makeText(this, "Gemini API Key is invalid or missing.", Toast.LENGTH_LONG).show();
            return;
        }

        // Typing indicator
        Message typingMessage = new Message("AI is typing...", Message.TYPE_BOT);
        messageAdapter.addMessage(typingMessage);
        recyclerViewChat.scrollToPosition(messageAdapter.getItemCount() - 1);

        JSONArray contentsArray = new JSONArray();

        // ADD SYSTEM PROMPT AS FIRST MESSAGE (ONLY ONCE)
        if (!systemPromptAdded) {
            try {
                JSONObject systemContent = new JSONObject();
                systemContent.put("role", "model");
                JSONArray parts = new JSONArray();
                parts.put(new JSONObject().put("text", SYSTEM_INSTRUCTION));
                systemContent.put("parts", parts);
                contentsArray.put(systemContent);
                systemPromptAdded = true;
            } catch (JSONException e) {
                Log.e(TAG, "Failed to add system prompt: " + e.getMessage());
            }
        }

        // ADD CONVERSATION HISTORY
        for (Message msg : messageList) {
            if (msg.getType() == Message.TYPE_USER || msg.getType() == Message.TYPE_BOT) {
                try {
                    JSONObject content = new JSONObject();
                    String role = (msg.getType() == Message.TYPE_USER) ? "user" : "model";
                    content.put("role", role);
                    JSONArray parts = new JSONArray();
                    parts.put(new JSONObject().put("text", msg.getMessage()));
                    content.put("parts", parts);
                    contentsArray.put(content);
                } catch (JSONException e) {
                    Log.e(TAG, "History error: " + e.getMessage());
                }
            }
        }

        // ADD CURRENT USER QUERY
        try {
            JSONObject userContent = new JSONObject();
            userContent.put("role", "user");
            String prompt = "Respond in language code: " + targetLangCode + ". Query: " + query;
            JSONArray parts = new JSONArray();
            parts.put(new JSONObject().put("text", prompt));
            userContent.put("parts", parts);
            contentsArray.put(userContent);
        } catch (JSONException e) {
            Log.e(TAG, "Query error: " + e.getMessage());
        }

        // FINAL PAYLOAD — NO system_instruction
        JSONObject payload = new JSONObject();
        try {
            payload.put("contents", contentsArray);
        } catch (JSONException e) {
            Log.e(TAG, "Payload error: " + e.getMessage());
            removeTypingIndicator(typingMessage);
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, GEMINI_API_URL, payload,
                response -> {
                    removeTypingIndicator(typingMessage);
                    try {
                        String botResponse = extractGeminiResponse(response);
                        if (!botResponse.isEmpty()) {
                            messageAdapter.addMessage(new Message(botResponse, Message.TYPE_BOT));
                            recyclerViewChat.scrollToPosition(messageAdapter.getItemCount() - 1);
                        } else {
                            messageAdapter.addMessage(new Message("Sorry, no response from AI.", Message.TYPE_BOT));
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Response parsing error: " + e.getMessage());
                        messageAdapter.addMessage(new Message("Failed to parse AI response.", Message.TYPE_BOT));
                    }
                },
                error -> {
                    removeTypingIndicator(typingMessage);
                    String errorMsg = "Unknown error";
                    int statusCode = 0;
                    if (error.networkResponse != null) {
                        statusCode = error.networkResponse.statusCode;
                        try {
                            errorMsg = new String(error.networkResponse.data, "UTF-8");
                        } catch (Exception e) {
                            errorMsg = "Failed to read error";
                        }
                    }
                    Log.e(TAG, "Gemini API Error: Status=" + statusCode + " | " + errorMsg);
                    messageAdapter.addMessage(new Message("API Error: " + errorMsg + " (Code: " + statusCode + ")", Message.TYPE_BOT));
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        requestQueue.add(request);
    }

    private void removeTypingIndicator(Message typingMessage) {
        int index = messageList.indexOf(typingMessage);
        if (index != -1) {
            messageList.remove(index);
            messageAdapter.notifyItemRemoved(index);
            messageAdapter.notifyItemRangeChanged(index, messageList.size());
        }
    }

    private String extractGeminiResponse(JSONObject response) throws JSONException {
        JSONArray candidates = response.getJSONArray("candidates");
        if (candidates.length() > 0) {
            JSONObject candidate = candidates.getJSONObject(0);
            JSONObject content = candidate.getJSONObject("content");
            JSONArray parts = content.getJSONArray("parts");
            if (parts.length() > 0) {
                return parts.getJSONObject(0).getString("text");
            }
        }
        return "";
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_chat);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) {
                startActivity(new Intent(this, DashboardActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_predict) {
                Intent intent = new Intent(this, DiseasePredictionActivity.class);
                intent.putExtra("lang", getSelectedLanguageCode());  // ← Sends Hindi/Marathi to Disease screen
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_crops) {
                startActivity(new Intent(this, CropRecommendationActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_chat) {
                return true;
            }
            return false;
        });
    }
}