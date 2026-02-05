package com.example.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MyAnalyzer {

     //Crea un analizzatore personalizzato per i campi testuali scientifici.
     //normalizzazione, rimozione possessivi, stemming e preservazione della parola originale.
    private static Analyzer createTextAnalyzer() throws IOException {
        return CustomAnalyzer.builder()
                .withTokenizer("standard")
                .addTokenFilter("englishPossessive")
                .addTokenFilter("lowercase")
                .addTokenFilter("asciifolding")
                .addTokenFilter("keywordRepeat")
                .addTokenFilter("porterStem")
                .addTokenFilter("removeDuplicates")
                .build();
    }

    //KeywordAnalyzer con aggiunta del lowercase
    private static Analyzer createIdAnalyzer() throws IOException {
        return CustomAnalyzer.builder()
                .withTokenizer("keyword")
                .addTokenFilter("lowercase")
                .build();
    }

    public static PerFieldAnalyzerWrapper getArticleAnalyzer() {
        Map<String, Analyzer> analyzerPerField = new HashMap<>();
        try {
            Analyzer textAnalyzer = createTextAnalyzer();
            Analyzer idAnalyzer = createIdAnalyzer();

            // Autori: Standard + Lowercase + ASCIIFolding
            analyzerPerField.put("authors", CustomAnalyzer.builder()
                    .withTokenizer("standard")
                    .addTokenFilter("lowercase")
                    .addTokenFilter("asciifolding")
                    .build());

            analyzerPerField.put("id", idAnalyzer);
            analyzerPerField.put("title", textAnalyzer);
            analyzerPerField.put("date", new KeywordAnalyzer());
            analyzerPerField.put("abstract", textAnalyzer);
            analyzerPerField.put("fulltext", textAnalyzer);

        } catch (IOException e) {
            throw new RuntimeException("Errore nella configurazione di ArticleAnalyzer", e);
        }
        return new PerFieldAnalyzerWrapper(new EnglishAnalyzer(), analyzerPerField);
    }

    public static PerFieldAnalyzerWrapper getImageAnalyzer() {
        Map<String, Analyzer> analyzerPerField = new HashMap<>();
        try {
            Analyzer textAnalyzer = createTextAnalyzer();
            Analyzer idAnalyzer = createIdAnalyzer();

            analyzerPerField.put("paper_id", idAnalyzer);
            analyzerPerField.put("image_id", idAnalyzer);
            analyzerPerField.put("url", idAnalyzer);

            // Campi testuali dell'immagine
            analyzerPerField.put("caption", textAnalyzer);
            analyzerPerField.put("citing_paragraphs", textAnalyzer);
            analyzerPerField.put("context_paragraphs", textAnalyzer);

            return new PerFieldAnalyzerWrapper(textAnalyzer, analyzerPerField);
        } catch (IOException e) {
            throw new RuntimeException("Errore nella configurazione di ImageAnalyzer", e);
        }
    }

    public static PerFieldAnalyzerWrapper getTableAnalyzer() {
        Map<String, Analyzer> analyzerPerField = new HashMap<>();
        try {
            Analyzer textAnalyzer = createTextAnalyzer();
            Analyzer idAnalyzer = createIdAnalyzer();

            analyzerPerField.put("paper_id", idAnalyzer);
            analyzerPerField.put("table_id", idAnalyzer);
            analyzerPerField.put("body", textAnalyzer);
            analyzerPerField.put("caption", textAnalyzer);
            analyzerPerField.put("citing_paragraphs", textAnalyzer);
            analyzerPerField.put("context_paragraphs", textAnalyzer);

            return new PerFieldAnalyzerWrapper(textAnalyzer, analyzerPerField);
        } catch (IOException e) {
            throw new RuntimeException("Errore nella configurazione di TableAnalyzer", e);
        }
    }
}