package rag.dochandler.entities;

public record Document(String id, String date, String title, String filename, String description, boolean hasFile, Long fldid) {}
