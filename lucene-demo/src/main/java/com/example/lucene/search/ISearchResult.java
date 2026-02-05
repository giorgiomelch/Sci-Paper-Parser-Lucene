package com.example.lucene.search;

public interface ISearchResult {

    /* ===== Campi base (sempre presenti) ===== */

    String getId();
    float getScore();
    String getDescription();
    ResultType getType();

    enum ResultType {
        ARTICLE, IMAGE, TABLE
    }

    /* ===== ARTICOLO ===== */

    default String getTitle() {
        return null;
    }

    default String getAuthors() {
        return null;
    }

    default String getDate() {
        return null;
    }

    default String getAbstract() {
        return null;
    }

    /* ===== IMMAGINE / TABELLA ===== */

    default String getCaption() {
        return null;
    }

    default String getUrl() {
        return null;
    }

    default String getBody() {
        return null;
    }

    /* ===== CONTESTO ===== */

    default String getMentionsParagraphs() {
        return null;
    }

    default String getContextParagraphs() {
        return null;
    }
}


