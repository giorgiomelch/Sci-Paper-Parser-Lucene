package com.example.lucene.extractData;

import java.util.List;

public class TableData {
    private String id;
    private String caption;
    private List<String> rowsContent; 
    private List<String> citingParagraphs;
    private List<String> contentMatchParagraphs;

    public TableData(String id, String caption, List<String> rowsContent, 
            List<String> citingParagraphs, List<String> contentMatchParagraphs) {
        this.id = id;
        this.caption = caption;
        this.rowsContent = rowsContent;
        this.citingParagraphs = citingParagraphs;
        this.contentMatchParagraphs = contentMatchParagraphs;
    }
    
    public String getId() { return id; }
    public String getCaption() { return caption; }
    public List<String> getBody() { return rowsContent; }
    public List<String> getCitingParagraphs() { return citingParagraphs; }
    public List<String> getContentMatchParagraphs() { return contentMatchParagraphs; }
    
    public String getBodyAsOneString(){ return String.join("\n", this.getBody());}
    public String getCitingParagraphsAsOneString(){ return String.join("\n", this.getCitingParagraphs());}
    public String getContentMatchParagraphsAsOneString() { return String.join("\n", this.getContentMatchParagraphs()); }

    public void setContentMatchParagraphs(List<String> matches) {
        this.contentMatchParagraphs = matches;
    }

    @Override
    public String toString() {
        return "TableData{id='" + id + "', caption='" + caption + "'}";
    }
}