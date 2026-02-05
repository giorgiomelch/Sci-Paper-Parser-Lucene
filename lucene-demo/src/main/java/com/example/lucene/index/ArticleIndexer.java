package com.example.lucene.index;

import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import com.example.lucene.MyAnalyzer;
import com.example.lucene.extractData.ArticleData;
import com.example.lucene.extractData.ArticleExtractor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ArticleIndexer {
    private static final Logger LOGGER = Logger.getLogger(ArticleIndexer.class.getName());

    private static final String INDEX_DIR = "lucene-index/articles";
    private static final String ARXIV_DATA_DIR = "corpus/arxiv";
    private static final String PUBMED_DATA_DIR = "corpus/pubmed";

    public static void main(String[] args) {
        try {
            Directory indexDirectory = FSDirectory.open(Paths.get(INDEX_DIR));
            createArticleIndex(indexDirectory);
            indexDirectory.close();

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Errore critico durante la creazione dell'indice", e);
        }
    }

    private static void createArticleIndex(Directory indexDirectory) throws IOException {
        LOGGER.info("Inizio indicizzazione articoli.");
        LOGGER.info("Directory indice destinazione: " + INDEX_DIR);

        long start = System.nanoTime();

        PerFieldAnalyzerWrapper analyzer = MyAnalyzer.getArticleAnalyzer();

        // Configurazione dell'IndexWriter
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

        try (IndexWriter writer = new IndexWriter(indexDirectory, config)) {
            Path arxivPath = Paths.get(ARXIV_DATA_DIR);
            if (Files.exists(arxivPath)) {
                LOGGER.info("Indicizzazione dataset ArXiv in corso...");
                indexArticles(writer, arxivPath);
            } else {
                LOGGER.warning("Directory dati ArXiv non trovata: " + ARXIV_DATA_DIR);
            }

            Path pubmedPath = Paths.get(PUBMED_DATA_DIR);
            if (Files.exists(pubmedPath)) {
                LOGGER.info("Indicizzazione dataset PubMed in corso...");
                indexArticles(writer, pubmedPath);
            } else {
                LOGGER.warning("Directory dati PubMed non trovata: " + PUBMED_DATA_DIR);
            }
            writer.commit();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Errore critico durante la creazione dell'indice", e);
            throw e; 
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        LOGGER.info("Indicizzazione completata con successo in " + elapsedMs + " ms.");
    }

    private static void indexArticles(IndexWriter writer, Path dataDir) throws IOException {
        List<ArticleData> articles = ArticleExtractor.extractAllArticles(dataDir);
        
        if (articles == null || articles.isEmpty()) {
            LOGGER.warning("Nessun articolo trovato da indicizzare in: " + dataDir);
            return;
        }
        LOGGER.info("Inizio indicizzazione di " + articles.size() + " articoli...");
        for (ArticleData article : articles){
            try {
                Document doc = new Document();
                doc.add(new StringField("paper_id", article.getPaper_id(), Field.Store.YES));
                doc.add(new StringField("date", article.getDate(), Field.Store.YES));
                doc.add(new TextField("title", article.getTitle(), Field.Store.YES));
                doc.add(new TextField("authors", article.getAuthors(), Field.Store.YES));
                doc.add(new TextField("abstract", article.getAbstract_(), Field.Store.YES));
                doc.add(new TextField("fulltext", article.getFulltext(), Field.Store.YES));

                writer.addDocument(doc);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Errore indicizzazione per l'articolo ID: " + article.getPaper_id(), e);
            }
        }
        LOGGER.info("Indicizzazione completata.");
    }
}