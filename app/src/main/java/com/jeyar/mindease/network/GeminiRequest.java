package com.jeyar.mindease.network;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class GeminiRequest {

    @SerializedName("system_instruction")
    public Content systemInstruction;

    public List<Content> contents;

    public static class Content {
        public String role;
        public List<Part> parts;
    }

    public static class Part {
        public String text;

        public Part(String text) {
            this.text = text;
        }
    }
}
