package com.example.lucene.search;

import com.example.lucene.search.articleSearch.ArticleSearcher;
import com.example.lucene.search.imageSearch.ImageSearcher;
import com.example.lucene.search.tableSearch.TableSearchResult;
import com.example.lucene.search.imageSearch.ImageSearchResult;
import com.example.lucene.search.tableSearch.TableSearcher;
import org.apache.lucene.queryparser.classic.ParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class UnifiedSearchService implements AutoCloseable {

    private final ArticleSearcher articleSearcher;
    private final ImageSearcher imageSearcher;
    private final TableSearcher tableSearcher;

    public UnifiedSearchService() throws IOException {
        this.articleSearcher = new ArticleSearcher();
        this.imageSearcher = new ImageSearcher();
        this.tableSearcher = new TableSearcher();
    }

    public ArticleSearcher getArticleSearcher() { return articleSearcher; }
    public ImageSearcher getImageSearcher() { return imageSearcher; }
    public TableSearcher getTableSearcher() { return tableSearcher; }


    public List<ISearchResult> searchAll(String queryText, int maxResultsPerType) {
        List<ISearchResult> allResults = new ArrayList<>();

        try {
            allResults.addAll(articleSearcher.searchByField("fulltext", queryText, maxResultsPerType));
            allResults.addAll(imageSearcher.searchByField("caption", queryText, maxResultsPerType));
            allResults.addAll(tableSearcher.searchByField("body", queryText, maxResultsPerType));

        } catch (Exception e) {
            System.err.println("Errore durante la ricerca unificata: " + e.getMessage());
        }

        // ordina per Score decrescente
        return allResults.stream()
                .sorted(Comparator.comparingDouble(ISearchResult::getScore).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Trova gli Articoli che contengono una tabella con campo X che parla di Y.
     */
    public List<ISearchResult> findArticlesByTableContent(String tableQuery) throws IOException, ParseException {
        // trova le tabelle
        List<TableSearchResult> tables = tableSearcher.searchByField("body", tableQuery, 10);
        // estrai i paper id unici
        List<String> paperIds = tables.stream()
                .map(r -> ((com.example.lucene.search.tableSearch.TableSearchResult)r).getPaperId())
                .distinct()
                .collect(Collectors.toList());
        // cerca gli articoli che hanno quegli id
        List<ISearchResult> results = new ArrayList<>();
        for (String id : paperIds){
            results.addAll(articleSearcher.searchByField("id", id, 5)); // 5 giusto come debug
        }
        return results;
    }
    /**
     * Trova gli Articoli che contengono un'immagine con campo X che parla di Y.
     */
    public List<ISearchResult> findArticlesByImageContent(String field, String tableQuery) throws IOException, ParseException {
        // trova le tabelle
        List<ImageSearchResult> tables = imageSearcher.searchByField(field, tableQuery, 10);
        // estrai i paper id unici
        List<String> paperIds = tables.stream()
                .map(r -> ((com.example.lucene.search.imageSearch.ImageSearchResult)r).getPaperId())
                .distinct()
                .collect(Collectors.toList());
        // cerca gli articoli che hanno quegli id
        List<ISearchResult> results = new ArrayList<>();
        for (String id : paperIds){
            results.addAll(articleSearcher.searchByField("id", id, 5)); // 5 giusto come debug
        }
        return results;
    }
    @Override
    public void close() throws IOException {
        articleSearcher.close();
        imageSearcher.close();
        tableSearcher.close();
    }
}
