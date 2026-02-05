package com.example.lucene.search.tableSearch;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import org.apache.lucene.queryparser.classic.ParseException;


public class TableSearchCLI {
    
    public static void main(String[] args) {
        try (TableSearcher searchService = new TableSearcher();
             Scanner scanner = new Scanner(System.in)) {
            runMenuSearchLoop(searchService, scanner);

        } catch (IOException e) {
            System.err.println("Errore critico (IO): " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Errore imprevisto: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public static void runMenuSearchLoop(TableSearcher service, Scanner scanner) {
        System.out.println("========================================");
        System.out.println("PROGRAMMA DI RICERCA LUCENE PER TABELLE");
        System.out.println("========================================");
        System.out.println("Tabelle indicizzate: " + service.getTotalDocuments());
        System.out.println();
        boolean running = true;
        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1" -> handleSingleFieldSearch(service, scanner);
                    case "2" -> handleIdSearch(service, scanner);
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
        System.out.println("q. Esci");
        System.out.print("\nScelta: ");
    }

    private static void handleSingleFieldSearch(TableSearcher service, Scanner scanner) 
            throws IOException, ParseException {
        System.out.print("\nCampi disponibili: paper_id, tabel_id, body, caption, mentions_paragraphs, context_paragraphs");
        System.out.print("\nInserisci il campo: ");
        String field = scanner.nextLine().trim();

        System.out.print("Inserisci la query: ");
        String query = scanner.nextLine().trim();

        printResults(service.searchByField(field, query, 5));
    }

    private static void handleIdSearch(TableSearcher service, Scanner scanner) 
            throws IOException, ParseException {
        System.out.print("\nInserisci l'id: ");
        String id = scanner.nextLine().trim();
        printResults(service.searchByField("id", id, 5));
    }



    private static void printResults(List<TableSearchResult> results) {
        System.out.println("\n========================================");
        System.out.println("RISULTATI (" + results.size() + " trovati)");
        System.out.println("========================================");

        if (results.isEmpty()) {
            System.out.println("Nessun risultato.");
            return;
        }

        for (int i = 0; i < results.size(); i++) {
            TableSearchResult r = results.get(i);
            System.out.printf("\n%d. [Score: %.2f]\n", (i + 1), r.getScore());
            System.out.println("   paper_id: " + r.getPaperId());
            System.out.println("   body: " + r.getBody());
            System.out.println("   Caption: " + r.getCaption());
        }
    }
}
