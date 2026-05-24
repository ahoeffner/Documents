package rag.dochandler.entities;

public record DocumentDetail(String id, String date, String title, String text, Long catid, String filename, boolean hasFile, String url) {}
