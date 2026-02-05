package com.example.lucene.index;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import com.example.lucene.extractData.ImageData;
import com.example.lucene.extractData.ImageExtractor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ImageIndexer {
    private static final Logger LOGGER = Logger.getLogger(ImageIndexer.class.getName());

    private static final String INDEX_DIR = "lucene-index/images";
    private static final String ARXIV_DATA_DIR = "corpus/arxiv";
    private static final String PUBMED_DATA_DIR = "corpus/pubmed";

    public static void main(String[] args) {
        try {
            Directory indexDirectory = FSDirectory.open(Paths.get(INDEX_DIR));
            createTableIndex(indexDirectory);
            indexDirectory.close();

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Errore critico durante la creazione dell'indice", e);
        }
    }
    
    private static void createTableIndex(Directory indexDirectory) throws IOException {
        System.out.println("Inizio indicizzazione immagini...");
        System.out.println("Directory indice: " + INDEX_DIR);

        long start = System.nanoTime();
        
        PerFieldAnalyzerWrapper analyzer = MyAnalyzer.getImageAnalyzer();
        
        // Configurazione dell'IndexWriter
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

        // configura l'IndexWriter
        try (IndexWriter writer = new IndexWriter(indexDirectory, config)) {
            Path arxivPath = Paths.get(ARXIV_DATA_DIR);
            if (Files.exists(arxivPath)) {
                LOGGER.info("Indicizzazione dataset ArXiv in corso...");
                indexImages(writer, arxivPath);
            } else {
                LOGGER.warning("Directory dati ArXiv non trovata: " + ARXIV_DATA_DIR);
            }

            Path pubmedPath = Paths.get(PUBMED_DATA_DIR);
            if (Files.exists(pubmedPath)) {
                LOGGER.info("Indicizzazione dataset PubMed in corso...");
                indexImages(writer, pubmedPath);
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

    private static void indexImages(IndexWriter writer, Path dataDir) throws IOException {
        File root = dataDir.toFile();
        if (!root.exists() || !root.isDirectory()) {
            System.err.println("Directory dati non trovata: " + dataDir.toString());
            return;
        }
        ObjectMapper mapper = new ObjectMapper();

        File[] directories = root.listFiles(File::isDirectory);
        if (directories == null) {
            System.err.println("Directory vuota: " + dataDir.toString());
            return;
        }
        for (File dir : directories) {
            File metadata = new File(dir, "metadata.json");
            File articleHtml = new File(dir, "article.html");
            if (!metadata.exists()) {
                System.err.println("File metadata.json non trovato in: " + dir.getAbsolutePath());
                continue;
            }
            try {
                JsonNode node = mapper.readTree(metadata);
                String id = node.path("id").asText("");

                List<ImageData> images = new ArrayList<>();
                if (articleHtml.exists()) {
                    String html = Files.readString(articleHtml.toPath(), StandardCharsets.UTF_8);
                    images = ImageExtractor.extractImages(html);
                }
                else
                    System.err.println("Errore: il file 'article.html' non è stato trovato in " + dir.getAbsolutePath());

                for (ImageData image : images){
                    Document doc = new Document();
                    doc.add(new StringField("paper_id", id, Field.Store.YES));
                    doc.add(new StringField("image_id", image.getId(), Field.Store.YES));
                    doc.add(new StringField("url", image.getUrl(), Field.Store.YES));
                    doc.add(new TextField("caption", image.getCaption(), Field.Store.YES));
                    doc.add(new TextField("mentions_paragraphs", image.getCitingParagraphsAsOneString(), Field.Store.NO));
                    doc.add(new TextField("context_paragraphs", image.getContentMatchParagraphsAsOneString(), Field.Store.NO));

                    writer.addDocument(doc);
                }
            } catch (Exception e) {
                System.err.println("Errore indicizzazione per " + dir.getAbsolutePath() + ": " + e.getMessage());
            }
        }
    }
}
