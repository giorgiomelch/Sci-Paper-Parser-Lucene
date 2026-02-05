package com.example.lucene.extractData;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class TableExtractTest {
    
    public void printTables(List<TableData> tables) {
        if (tables == null || tables.isEmpty()) {
            System.out.println("Nessuna tabella da stampare.");
            return;
        }
        System.out.println("\n========== TABELLE ESTRATTE: " + tables.size() + " ==========");

        for (TableData tbl : tables) {
            System.out.println("\n--------------------------------------------------");
            System.out.println("ID: " + tbl.getId());
            String safeCaption = (tbl.getCaption() == null || tbl.getCaption().isEmpty()) ? "(nessuna)" : tbl.getCaption();
            System.out.println("Caption: " + safeCaption);
            
            System.out.println("\n--- CONTENUTO TABELLA ---");
            List<String> body = tbl.getBody();
            if (body != null && !body.isEmpty()) {
                for (String row : body) {
                    System.out.println(row);
                }
            } else {
                System.out.println("(Tabella vuota o nessun contenuto estratto)");
            }
            System.out.println("\n--- CITAZIONI TROVATE ---");
            List<String> citers = tbl.getCitingParagraphs();
            if (citers != null && !citers.isEmpty()) {
                for (String citer : citers) {
                    System.out.println("-" + citer + "\n");
                }
            } else {
                System.out.println("(Nessuna citazione)");
            }
            
            System.out.println("\n--- PARAGRAFI CON CONTESTO TERMINI INERENTI ---");
            List<String> contextParagraphs = tbl.getContentMatchParagraphs();
            if (contextParagraphs != null && !contextParagraphs.isEmpty()) {
                for (String context : contextParagraphs) {
                    System.out.println("-" + context + "\n");
                }
            } else {
                System.out.println("(Nessuna citazione)");
            }
            System.out.println("--------------------------------------------------");
        }
    }
    
    public static void main(String[] args) {
        //String filePath = "corpus/arxiv/2508.07087v1/article.html"; //4 tabelle
        String filePath = "corpus/pubmed/12693901/article.html"; // 2 tabelle
        
        File input = new File(filePath);
        if (!input.exists()) {
            System.err.println("File non trovato: " + input.getAbsolutePath());
            return;
        }
        try {
            Document doc = Jsoup.parse(input, "UTF-8");
            String htmlContent = doc.html();
            List<TableData> tables = TableExtractor.extractTables(htmlContent);
            TableExtractTest tablePrinter = new TableExtractTest();
            tablePrinter.printTables(tables);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}