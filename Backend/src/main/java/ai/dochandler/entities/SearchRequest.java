package ai.dochandler.entities;

public record SearchRequest(String id, String query, long folder, boolean languageIndependent) {}
