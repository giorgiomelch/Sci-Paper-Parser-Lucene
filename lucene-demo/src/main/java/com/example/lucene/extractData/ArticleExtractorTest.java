package com.example.lucene.extractData;

import java.io.File;

public class ArticleExtractorTest {
    
    public static void printArticle(ArticleData articleData) {
        if (articleData == null) {
            System.out.println("Nessun articolo da stampare.");
            return;
        }
        System.out.println(articleData.getPaper_id());
        System.out.println("--------------------------------------------------");
        System.out.println(articleData.getTitle());
        System.out.println("--------------------------------------------------");
        System.out.println(articleData.getAuthors());
        System.out.println("--------------------------------------------------");
        System.out.println(articleData.getDate());
        System.out.println("--------------------------------------------------");
        System.out.println(articleData.getFulltext());
    }
    
    public static void main(String[] args) {
        String filePath = "corpus/pubmed/12693901"; 
        //String filePath = "corpus/arxiv/1811.05303v1"; 
        File input = new File(filePath);
        if (!input.exists()) {
            System.err.println("File non trovato: " + input.getAbsolutePath());
            return;
        }
        ArticleData article = ArticleExtractor.extractArticle(input);
        printArticle(article);
    }
}