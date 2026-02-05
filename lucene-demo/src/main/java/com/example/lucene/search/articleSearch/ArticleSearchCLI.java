package com.example.lucene.search.articleSearch;

import com.example.lucene.search.BooleanSearch;

import org.apache.lucene.queryparser.classic.ParseException;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class ArticleSearchCLI {

    public static void main(String[] args) {
        try (ArticleSearcher searchService = new ArticleSearcher();
             Scanner scanner = new Scanner(System.in)) {
            runMenuSearchLoop(searchService, scanner);

        } catch (IOException e) {
            System.err.println("Errore critico (IO): " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Errore imprevisto: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public static void runMenuSearchLoop(ArticleSearcher service, Scanner scanner) {
        System.out.println("========================================");
        System.out.println("PROGRAMMA DI RICERCA LUCENE PER ARTICOLI");
        System.out.println("========================================");
        System.out.println("Documenti indicizzati: " + service.getTotalDocuments());
        System.out.println();
        boolean running = true;
        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1" -> handleSingleFieldSearch(service, scanner);
                    case "2" -> handleIdSearch(service, scanner);
                    case "3" -> handleDateSearch(service, scanner);
                    case "4" -> handleBooleanSearch(service, scanner);
                    case "q" -> {
                        running = false;
                        System.out.println("Programma terminato.\n");
                    }
                    default -> System.out.println("Scelta non valida.");
                }
            } catch (ParseException e) {
                System.err.println("Errore nel parsing della query: " + e.getMessage());
            } catch (IOException e) {
                System.err.println("Errore di lettura indice: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                System.err.println("Input non valido: " + e.getMessage());
            }
        }
    }

    private static void printMenu() {
        System.out.println("\nScegli il tipo di ricerca:");
        System.out.println("1. Ricerca su singolo campo");
        System.out.println("2. Ricerca per id");
        System.out.println("3. Ricerca per data");
        System.out.println("4. Ricerca booleana");
        System.out.println("q. Esci");
        System.out.print("\nScelta: ");
    }

    private static void handleSingleFieldSearch(ArticleSearcher service, Scanner scanner) 
            throws IOException, ParseException {
        System.out.print("\nCampi disponibili: title, abstract, authors, date, fulltext");
        System.out.print("\nInserisci il campo: ");
        String field = scanner.nextLine().trim();

        System.out.print("Inserisci la query: ");
        String query = scanner.nextLine().trim();

        printResults(service.searchByField(field, query, 5));
    }

    private static void handleIdSearch(ArticleSearcher service, Scanner scanner) 
            throws IOException, ParseException {
        System.out.print("\nInserisci l'id: ");
        String id = scanner.nextLine().trim();
        printResults(service.searchByField("id", id, 2));
    }

    private static void handleDateSearch(ArticleSearcher service, Scanner scanner) 
            throws IOException {
        System.out.println("\nRicerca per range di date (YYYY-MM-DDTHH:MM:SSZ)");
        System.out.print("Data inferiore: ");
        String dateLower = scanner.nextLine().trim();
        System.out.print("Data superiore: ");
        String dateUpper = scanner.nextLine().trim();

        printResults(service.searchByDateRange(dateLower, dateUpper, 10));
    }

    private static void handleBooleanSearch(ArticleSearcher service, Scanner scanner) 
            throws IOException, ParseException {
        BooleanSearch boolHelper = new BooleanSearch(service.getAnalyzer());
        List<BooleanSearch.BooleanQueryPart> parts = boolHelper.interactiveMenu(scanner);
        
        if (!parts.isEmpty()) {
            boolHelper.printCompiledQuery(parts);
            printResults(service.booleanSearch(parts, 10));
        } else {
            System.out.println("Nessun criterio inserito.");
        }
    }

    private static void printResults(List<ArticleSearchResult> results) {
        System.out.println("\n========================================");
        System.out.println("RISULTATI (TOP " + results.size() + " trovati)");
        System.out.println("========================================");

        if (results.isEmpty()) {
            System.out.println("Nessun risultato.");
            return;
        }

        for (int i = 0; i < results.size(); i++) {
            ArticleSearchResult r = results.get(i);
            System.out.printf("\n%d. [Score: %.2f]\n", (i + 1), r.getScore());
            System.out.println("   Title: " + r.getTitle());
            System.out.println("   Authors: " + r.getAuthors());
            System.out.println("   Date: " + r.getDate());
            
            String abs = r.getAbstract();
            if (abs != null && !abs.isEmpty()) {
                 System.out.println("   Abstract: " + (abs.length() > 100 ? abs.substring(0, 100) + "..." : abs));
            }
        }
    }
}