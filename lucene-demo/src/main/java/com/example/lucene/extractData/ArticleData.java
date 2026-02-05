package com.example.lucene.extractData;


public class ArticleData {
    private String paper_id;
    private String title;
    private String abstract_;
    private String authors;
    private String date;
    private String fulltext;


    public ArticleData(String paper_id, String title, String abstract_, 
            String authors, String date, String fulltext) {
        this.paper_id = paper_id;
        this.title = title;
        this.abstract_ = abstract_;
        this.authors = authors;
        this.date = date;
        this.fulltext = fulltext;
    }


    public String getPaper_id() {
        return paper_id;
    }


    public String getTitle() {
        return title;
    }


    public String getAbstract_() {
        return abstract_;
    }


    public String getAuthors() {
        return authors;
    }


    public String getDate() {
        return date;
    }


    public String getFulltext() {
        return fulltext;
    }
    

}
