package com.example.lucene.extractData;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class ImageExtractTest {
    
    public void printImages(List<ImageData> images) {
        if (images == null || images.isEmpty()) {
            System.out.println("Nessuna immagine da stampare.");
            return;
        }
        System.out.println("\n========== IMMAGINI ESTRATTE: " + images.size() + " ==========");

        for (ImageData img : images) {
            System.out.println("\n--------------------------------------------------");
            System.out.println("ID: " + img.getId());
            System.out.println("URL: " + img.getUrl());
            String safeCaption = (img.getCaption() == null || img.getCaption().isEmpty()) ? "(nessuna)" : img.getCaption();
            System.out.println("Caption: " + safeCaption);
            
            System.out.println("\n--- CITAZIONI TROVATE ---");
            List<String> citers = img.getCitingParagraphs();
            if (citers != null && !citers.isEmpty()) {
                for (String citer : citers) {
                    System.out.println("-" + citer + "\n");
                }
            } else {
                System.out.println("(Nessuna citazione)");
            }
            
            System.out.println("\n--- PARAGRAFI CON CONTESTO TERMINI INERENTI ALLA CAPTION ---");
            List<String> contextParagraphs = img.getContentMatchParagraphs();
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
        //2409.05735v2 contiene una tabella sotto ltx_figure
        //String filePath = "corpus/pubmed/12693901/article.html"; 
        String filePath = "corpus/arxiv/1811.05303v1/article.html"; 
        
        File input = new File(filePath);
        if (!input.exists()) {
            System.err.println("File non trovato: " + input.getAbsolutePath());
            return;
        }
        try {
            Document doc = Jsoup.parse(input, "UTF-8");
            String htmlContent = doc.html();
            List<ImageData> images = ImageExtractor.extractImages(htmlContent);
            ImageExtractTest imagePrinter = new ImageExtractTest();
            imagePrinter.printImages(images);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
