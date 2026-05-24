package rag.dochandler.entities;

public record ChatRequest(String id, String query, long category, double match) {}
