package com.example.lucene.extractData;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ArticleExtractor {

    private static final Logger LOGGER = Logger.getLogger(ArticleExtractor.class.getName());

    public static List<ArticleData> extractAllArticles(Path dataDir){
        List<ArticleData> articles = new ArrayList<>();
        File root = dataDir.toFile();
        if (!root.exists() || !root.isDirectory()) {
            System.err.println("Directory dati non trovata: " + dataDir.toString());
            return articles;
        }

        File[] dirs = root.listFiles(File::isDirectory);
        if (dirs == null) {
            System.err.println("Directory vuota: " + dataDir.toString());
            return articles;
        }

        for (File d : dirs) {
            ArticleData article = extractArticle(d);
            if (article != null) {
                articles.add(article);
            }
        }
        return articles;
    }
    
    public static ArticleData extractArticle(File d){
        ObjectMapper mapper = new ObjectMapper();
        File meta = new File(d, "metadata.json");
            File absHtml = new File(d, "article.html");
            if (!meta.exists()) {
                System.err.println("File metadata.json non trovato in: " + d.getAbsolutePath());
                return null;
            }
            try {
                JsonNode node = mapper.readTree(meta);

                String id = node.path("id").asText("");
                String title = node.path("title").asText("");
                String date = node.path("published").asText("");
                String summary = node.path("summary").asText("");
                String authors = extractAuthors(node.path("authors"));
                
                String fullText = "";
                if (absHtml.exists()) {
                    fullText = extractArticleContent(absHtml, id);
                } else {
                    LOGGER.warning("File article.html mancante in: " + d.getAbsolutePath());
                }
                return new ArticleData(id, title, summary, authors, date, fullText);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Errore di I/O durante l'elaborazione di: " + d.getAbsolutePath(), e);
                return null;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Errore imprevisto durante l'elaborazione di: " + d.getAbsolutePath(), e);
                return null;
            }
        }

    private static String extractAuthors(JsonNode authorsNode) {
        if (authorsNode.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode a : authorsNode) {
                if (sb.length() > 0) {
                    sb.append("; ");
                }
                sb.append(a.asText());
            }
            return sb.toString();
        } else {
            return authorsNode.asText("");
        }
    }

    public static String extractArticleContent(File absHtml, String id) throws IOException{
        String html = Files.readString(absHtml.toPath(), StandardCharsets.UTF_8);
        if (html == null || html.isEmpty()) {
            LOGGER.warning("Html vuoto: " + absHtml.getName());
            return "";
        }
        Document doc = Jsoup.parse(html);
        // rimuovi bibliografia e reference
        Elements unwantedSections = doc.select(
            "section.ltx_bibliography, " + // rimuovi sections con classe ltx_bibliography (arxiv)
            "section#bib, " +   //rimuovi sections con attributo "bib" (arxiv)
            "section.ref-list, " + // rimuovi section con classe ref-list (pubmed)
            "section[id^=ref-list]" // rimuovi sections con attributo che inizia con ref-list (pubmed)
        );
        if (!unwantedSections.isEmpty()) {
            unwantedSections.remove();
        }
        else{
            LOGGER.warning("Selettori per identificare bibliografia e reference falliti per file: " + id);
        }
        Elements mainContent = doc.select("section[aria-label='Article content'], div.ltx_page_main");

        if (mainContent.isEmpty()){
            LOGGER.warning("Selettori per indivicuare contenuto articolo falliti nel file: " + id);
            return doc.text();
        }
        else{
            if (mainContent.size() > 1) {
                LOGGER.warning("Rilevate multiple sezioni di contenuto principale (" + mainContent.size() + ") nel file: " + id);
            }
            return mainContent.text();
        }
    }
    
}
