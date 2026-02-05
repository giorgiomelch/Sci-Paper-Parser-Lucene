package com.example.lucene.extractData;

import java.util.List;

public class ImageData {
    private String id;
    private String url;
    private String caption;
    private List<String> citingParagraphs;
    private List<String> contentMatchParagraphs;

    public ImageData(String id, String url, String caption, 
            List<String> citingParagraphs, List<String> contentMatchParagraphs) {
        this.id = id;
        this.url = url;
        this.caption = caption;
        this.citingParagraphs = citingParagraphs;
        this.contentMatchParagraphs = contentMatchParagraphs;
    }
    
    public String getId() { return id; }
    public String getUrl() { return url; }
    public String getCaption() { return caption; }
    public List<String> getCitingParagraphs() { return citingParagraphs; }
    public List<String> getContentMatchParagraphs() { return contentMatchParagraphs; }
    
    public String getCitingParagraphsAsOneString(){ return String.join("\n", this.getCitingParagraphs());}
    public String getContentMatchParagraphsAsOneString() { return String.join("\n", this.getContentMatchParagraphs()); }

    @Override
    public String toString() {
        return "ImageData{id='" + id + "', caption='" + caption + "'}";
    }
}