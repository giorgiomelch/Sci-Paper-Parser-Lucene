package com.example.lucene.search;

import org.apache.lucene.queryparser.classic.ParseException;

import com.example.lucene.search.articleSearch.ArticleSearchCLI;
import com.example.lucene.search.imageSearch.ImageSearchCLI;
import com.example.lucene.search.tableSearch.TableSearchCLI;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class SearchCLI {

    public static void main(String[] args) {
        try (UnifiedSearchService unifiedService = new UnifiedSearchService();
             Scanner scanner = new Scanner(System.in)) {
            runMenuSearchLoop(unifiedService, scanner);

        } catch (IOException e) {
            System.err.println("Errore critico (IO): " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Errore imprevisto: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void runMenuSearchLoop(UnifiedSearchService service, Scanner scanner) {
        printHeader(service);
        boolean running = true;
        
        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1" -> handleArticleSearch(service, scanner);
                    case "2" -> handleImageSearch(service, scanner);
                    case "3" -> handleTableSearch(service, scanner); 
                    case "4" -> handleCrossIndexArticleTableSearch(service, scanner);
                    case "q" -> {
                        running = false;
                        System.out.println("Programma terminato.\n");
                    }
                    default -> System.out.println("Scelta non valida.");
                }
            } catch (Exception e) {
                System.err.println("Errore operazione: " + e.getMessage());
            }
        }
    }

    private static void printHeader(UnifiedSearchService service) {
        System.out.println("========================================");
        System.out.println("      MOTORE DI RICERCA UNIFICATO       ");
        System.out.println("========================================");
        System.out.println("Articoli: " + service.getArticleSearcher().getTotalDocuments());
        System.out.println("Immagini: " + service.getImageSearcher().getTotalDocuments());
        System.out.println("Tabelle:  " + service.getTableSearcher().getTotalDocuments());
        System.out.println();
    }

    private static void printMenu() {
        System.out.println("\nScegli il tipo di ricerca:");
        System.out.println("1. Ricerca specifica Articoli");
        System.out.println("2. Ricerca specifica Immagini");
        System.out.println("3. Ricerca specifica Tabelle");
        System.out.println("4. Ricerca articoli tramite body tabelle");
        System.out.println("q. Esci");
        System.out.print("Scelta: ");
    }

    // --- HANDLERS ---

    private static void handleArticleSearch(UnifiedSearchService service, Scanner scanner) throws IOException, ParseException {
        ArticleSearchCLI.runMenuSearchLoop(service.getArticleSearcher(), scanner); 
    }
    private static void handleImageSearch(UnifiedSearchService service, Scanner scanner) throws IOException, ParseException {
        ImageSearchCLI.runMenuSearchLoop(service.getImageSearcher(), scanner);
    }
    private static void handleTableSearch(UnifiedSearchService service, Scanner scanner) throws IOException, ParseException {
        TableSearchCLI.runMenuSearchLoop(service.getTableSearcher(), scanner); 
    }

    private static void handleCrossIndexArticleTableSearch(UnifiedSearchService service, Scanner scanner) throws IOException, ParseException {
        System.out.println("\n--- Trova Articoli che contengono tabelle su un argomento ---");
        System.out.print("Argomento della tabella: ");
        String query = scanner.nextLine().trim();
        
        List<ISearchResult> results = service.findArticlesByTableContent( query);
        printGenericResults(results);
    }

    // --- PRINTER ---

    private static void printGenericResults(List<ISearchResult> results) {
        System.out.println("\n--- RISULTATI (" + results.size() + ") ---");
        if (results.isEmpty()) {
            System.out.println("Nessun risultato.");
            return;
        }

        for (ISearchResult r : results) {
            System.out.printf("[%s] Score: %.2f | %s (ID: %s)\n", 
                r.getType(), r.getScore(), r.getDescription(), r.getId());
        }
    }
    
}