package com.example.lucene.search.imageSearch;

import com.example.lucene.search.ISearchResult;

public class ImageSearchResult implements ISearchResult{
    private final String paperId;
    private final String imageId;
    private final String url;
    private final String caption;
    private final String citingParagraphs;
    private final String contentMatchParagraphs;
    private final float score;

    public ImageSearchResult(String paper_id, String image_id, String url, String caption, String citingParagraphs, String contentMatchParagraphs, float score) {
        this.paperId = paper_id;
        this.imageId = image_id;
        this.url = url;
        this.caption = caption;
        this.citingParagraphs = citingParagraphs;
        this.contentMatchParagraphs = contentMatchParagraphs;
        this.score = score;
    }

    @Override
    public String getId() { return imageId; }

    @Override
    public float getScore() { return score; }

    @Override
    public ResultType getType() { return ResultType.IMAGE; }

    @Override
    public String getDescription() { 
        return (caption != null && !caption.isEmpty()) ? caption : "Immagine senza didascalia"; 
    }
    public String getPaperId() {
        return paperId;
    }

    public String getImageId() {
        return imageId;
    }

    public String getUrl() {
        return url;
    }

    public String getCaption() {
        return caption;
    }

    public String getCitingParagraphs() {
        return citingParagraphs;
    }

    public String getContentMatchParagraphs() {
        return contentMatchParagraphs;
    }
}
