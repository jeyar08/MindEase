package com.jeyar.mindease.network;

import java.util.List;

public class GeminiResponse {

    public List<Candidate> candidates;
    public PromptFeedback promptFeedback;

    public static class Candidate {
        public Content content;
        public String finishReason;
    }

    public static class Content {
        public List<Part> parts;
        public String role;
    }

    public static class Part {
        public String text;
    }

    public static class PromptFeedback {
        public String blockReason;
    }
}
