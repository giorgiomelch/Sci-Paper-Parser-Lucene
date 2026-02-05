package com.example.lucene.search.articleSearch;

import com.example.lucene.MyAnalyzer;
import com.example.lucene.search.BooleanSearch;

import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ArticleSearcher implements AutoCloseable {
    private static final String INDEX_DIR = "lucene-index/articles";

    private final IndexReader index_reader;
    private final IndexSearcher index_searcher;
    private final PerFieldAnalyzerWrapper analyzer;
    private final BooleanSearch booleanSearch;

    public ArticleSearcher() throws IOException {
        this.index_reader = DirectoryReader.open(FSDirectory.open(Paths.get(INDEX_DIR)));
        this.index_searcher = new IndexSearcher(index_reader);
        this.analyzer = MyAnalyzer.getArticleAnalyzer();
        this.booleanSearch = new BooleanSearch(MyAnalyzer.getArticleAnalyzer());
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

    /** Metodo helper per normalizzare le date */
    private String formatDate(String rawDate) {
        if (rawDate == null || rawDate.isEmpty()) return "";

        try {
            // prova ISO 8601 (ArXiv)
            OffsetDateTime odt = OffsetDateTime.parse(rawDate);
            return odt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (DateTimeParseException e) {
            try {
                // prova formato PubMed "yyyy MMM dd"
                DateTimeFormatter pubmedFormatter = DateTimeFormatter.ofPattern("yyyy MMM dd", Locale.ENGLISH);
                LocalDate ld = LocalDate.parse(rawDate, pubmedFormatter);
                return ld.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } catch (DateTimeParseException ex) {
                // fallback: ritorna la stringa originale
                return rawDate;
            }
        }
    }

    /** Esegue la ricerca e normalizza le date */
    private List<ArticleSearchResult> executeSearch(Query query, int maxResults) throws IOException {
        List<ArticleSearchResult> results = new ArrayList<>();

        TopDocs topDocs = index_searcher.search(query, maxResults);
        StoredFields storedFields = index_searcher.storedFields();

        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            Document doc = storedFields.document(scoreDoc.doc);
            ArticleSearchResult result = new ArticleSearchResult(
                    doc.get("paper_id"),
                    doc.get("title"),
                    doc.get("authors"),
                    formatDate(doc.get("date")),  // <-- data già formattata
                    doc.get("abstract"),
                    scoreDoc.score
            );
            results.add(result);
        }

        return results;
    }

    public List<ArticleSearchResult> searchByField(String field, String queryString, int maxResults)
            throws ParseException, IOException {
        QueryParser parser = new QueryParser(field, analyzer);
        Query query = parser.parse(queryString);
        return executeSearch(query, maxResults);
    }

    public List<ArticleSearchResult> searchByDateRange(String dateLower, String dateUpper, int maxResults) throws IOException {
        BytesRef lowerBound = (dateLower != null && !dateLower.isEmpty()) ? new BytesRef(dateLower) : null;
        BytesRef upperBound = (dateUpper != null && !dateUpper.isEmpty()) ? new BytesRef(dateUpper) : null;

        // Se entrambe le date non sono specificate, ritorna tutti i documenti
        if (lowerBound == null && upperBound == null) {
            Query query = new MatchAllDocsQuery(); // serve import: org.apache.lucene.search.MatchAllDocsQuery
            return executeSearch(query, maxResults);
        }

        Query query = new TermRangeQuery("date", lowerBound, upperBound, true, true);
        return executeSearch(query, maxResults);
    }


    public List<ArticleSearchResult> booleanSearch(List<BooleanSearch.BooleanQueryPart> queryParts, int maxResults)
            throws ParseException, IOException {
        Query query = booleanSearch.buildBooleanQuery(queryParts);
        return executeSearch(query, maxResults);
    }

    public List<ArticleSearchResult> searchByFields(String queryString, String[] fields, int maxResults)
            throws ParseException, IOException {
        if (fields == null || fields.length == 0) {
            fields = new String[]{"fulltext"};
        }
        MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, analyzer);
        Query query = parser.parse(queryString);
        return executeSearch(query, maxResults);
    }
}

