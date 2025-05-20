package com.example.searchengine.model;
public class SearchResultItem {
    private String title;
    private String filePath;
    private String sentence;
    private float score;

    public SearchResultItem() {
    }

    public SearchResultItem(String title, String filePath, String sentence, float score) {
        this.title = title;
        this.filePath = filePath;
        this.sentence = sentence;
        this.score = score;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getSentence() { return sentence; }
    public void setSentence(String sentence) { this.sentence = sentence; }
    public float getScore() { return score; }
    public void setScore(float score) { this.score = score; }

    @Override
    public String toString() {
        return "SearchResultItem{" +
                "title='" + title + '\'' +
                ", filePath='" + filePath + '\'' +
                ", sentence='" + sentence + '\'' +
                ", score=" + score +
                '}';
    }
}