package com.example.lucene.search.imageSearch;

import com.example.lucene.MyAnalyzer;

import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.springframework.stereotype.Component;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Component
public class ImageSearcher implements AutoCloseable{
    private static final String INDEX_DIR = "lucene-index/images";

    private final IndexReader index_reader;
    private final IndexSearcher index_searcher;
    private final PerFieldAnalyzerWrapper analyzer;

    public ImageSearcher() throws IOException {
        this.index_reader = DirectoryReader.open(FSDirectory.open(Paths.get(INDEX_DIR)));
        this.index_searcher = new IndexSearcher(index_reader);
        this.analyzer = MyAnalyzer.getImageAnalyzer();
    }
    
    public PerFieldAnalyzerWrapper getAnalyzer() {
        return this.analyzer;
    }
    public int getTotalDocuments() {
        return index_reader.numDocs();
    }
    public void close() throws IOException {
        index_reader.close();
    }
    
    private List<ImageSearchResult> executeSearch(Query query, int maxResults) throws IOException {
        List<ImageSearchResult> results = new ArrayList<>();
        
        TopDocs topDocs = index_searcher.search(query, maxResults);
        StoredFields storedFields = index_searcher.storedFields();
        
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            Document doc = storedFields.document(scoreDoc.doc);
            ImageSearchResult result = new ImageSearchResult(
                doc.get("paper_id"),
                doc.get("image_id"),
                doc.get("url"),
                doc.get("caption"),
                doc.get("mentions_paragraphs"),
                doc.get("context_paragraphs"),
                scoreDoc.score
            );
            results.add(result);
        }
        
        return results;
    }
    public List<ImageSearchResult> searchByField(String field, String queryString, int maxResults) 
            throws ParseException, IOException {
        QueryParser parser = new QueryParser(field, analyzer);
        Query query = parser.parse(queryString);
        return executeSearch(query, maxResults);
    }

    public List<ImageSearchResult> searchByFields(String queryString, String[] fields, int maxResults)
        throws ParseException, IOException {
    if (fields == null || fields.length == 0) {
        // fallback: campo caption
        fields = new String[]{"caption"};
    }
    // MultiFieldQueryParser permette di cercare su più campi contemporaneamente
    MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, analyzer);
    Query query = parser.parse(queryString);
    return executeSearch(query, maxResults);
}
}