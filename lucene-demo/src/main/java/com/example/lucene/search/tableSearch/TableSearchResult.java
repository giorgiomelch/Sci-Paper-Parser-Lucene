package com.example.lucene.search.tableSearch;

import com.example.lucene.search.ISearchResult;

public class TableSearchResult implements ISearchResult{
    private final String paperId;
    private final String tableId;
    private final String body;
    private final String caption;
    private final String mentions_paragraphs;
    private final String context_paragraphs;
    private final float score;

    public TableSearchResult(String paperId, String tableId, String body,
                            String caption, String mentions_paragraphs, String context_paragraphs, float score){
        this.paperId = paperId;
        this.tableId = tableId;
        this.body = body;
        this.caption = caption;
        this.mentions_paragraphs = mentions_paragraphs;
        this.context_paragraphs = context_paragraphs;
        this.score = score;
        }

    @Override
    public String getId() { return tableId; }

    @Override
    public float getScore() { return score; }

    @Override
    public ResultType getType() { return ResultType.TABLE; }

    @Override
    public String getDescription() {
        return (caption != null && !caption.isEmpty()) ? caption : "Tabella senza caption";
    }
    public String getPaperId() {
        return paperId;
    }

    public String getTabelId() {
        return tableId;
    }

    public String getBody() {
        return body;
    }

    public String getCaption() {
        return caption;
    }

    public String getMentions_paragraphs() {
        return mentions_paragraphs;
    }

    public String getContext_paragraphs() {
        return context_paragraphs;
    }
}
