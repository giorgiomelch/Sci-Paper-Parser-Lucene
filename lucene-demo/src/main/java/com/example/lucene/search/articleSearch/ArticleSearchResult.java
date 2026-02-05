package com.example.lucene.search.articleSearch;

import com.example.lucene.search.ISearchResult;

public class ArticleSearchResult implements ISearchResult{
    private final String paper_id;
    private final String title;
    private final String authors;
    private String date;
    private final String abstract_;
    private final float score;

    public ArticleSearchResult(String paper_id, String title, String authors, String date, String abstract_, float score) {
        this.paper_id = paper_id;
        this.title = title;
        this.authors = authors;
        this.date = date;
        this.abstract_ = abstract_;
        this.score = score;
    }
    // DA CAMABIARE getId
    @Override
    public String getId() { return paper_id; }

    @Override
    public float getScore() { return score; }

    @Override
    public ResultType getType() { return ResultType.ARTICLE; }

    @Override
    public String getDescription() { return title; }

    public String getTitle() {
        return title;
    }

    public String getAuthors() {
        return authors;
    }

    public String getDate() {
        return date;
    }
    public void setDate(String date) {
        this.date = date;
    }

    public String getAbstract() {
        return abstract_;
    }


    @Override
    public String toString() {
        return "SearchResult{" +
                "title='" + title + '\'' +
                ", authors='" + authors + '\'' +
                ", date='" + date + '\'' +
                ", score=" + score +
                '}';
    }
}
