package com.wirecat.core_capture;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import com.google.gson.*;
import io.github.cdimascio.dotenv.Dotenv;



public class GeminiClient {
    private static final Dotenv dotenv = Dotenv.configure().load();
    private static final String API_KEY = dotenv.get("GEMINI_API_KEY");
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final Gson gson = new Gson();

    public static String analyzePacket(String packetData) throws Exception {
        String prompt = "Explain this network packet in simple terms: " + packetData;
        String modelEndpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

        JsonObject requestBody = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        JsonArray parts = new JsonArray();
        parts.add(new JsonObject());
        parts.get(0).getAsJsonObject().addProperty("text", prompt);
        content.add("parts", parts);
        contents.add(content);
        requestBody.add("contents", contents);

        // Add generation parameters
        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("maxOutputTokens", 500);
        requestBody.add("generationConfig", generationConfig);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(modelEndpoint + "?key=" + API_KEY))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        return handleResponse(HTTP.send(request, HttpResponse.BodyHandlers.ofString()));
    }

    private static String handleResponse(HttpResponse<String> response) throws Exception {
        if (response.statusCode() == 200) {
            JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
            return jsonResponse.getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();
        } else {
            throw new RuntimeException("API Error: " + response.body());
        }
    }
}