package com.example.lucene.search;

import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class BooleanSearch {

    private final PerFieldAnalyzerWrapper analyzer;

    public BooleanSearch(PerFieldAnalyzerWrapper analyzer) {
        this.analyzer = analyzer;
    }

    //Rappresenta un singolo criterio di ricerca booleana.
    public static class BooleanQueryPart {
        public String field;
        public String queryString;
        public BooleanClause.Occur occur;

        public BooleanQueryPart(String field, String queryString, BooleanClause.Occur occur) {
            this.field = field;
            this.queryString = queryString;
            this.occur = occur;
        }

        @Override
        public String toString() {
            return occur + " " + field + ": '" + queryString + "'";
        }
    }


    public Query buildBooleanQuery(List<BooleanQueryPart> queryParts) throws ParseException {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();

        for (BooleanQueryPart part : queryParts) {
            QueryParser parser = new QueryParser(part.field, analyzer);
            Query parsedQuery = parser.parse(part.queryString);
            builder.add(parsedQuery, part.occur);
        }
        return builder.build();
    }


    public List<BooleanQueryPart> interactiveMenu(Scanner scanner) {
        System.out.println("\n========== RICERCA BOOLEANA ==========");
        System.out.println("Combina più criteri con operatori: MUST, SHOULD, MUST NOT");
        System.out.println("Campi disponibili: title, abstract, authors, date, fulltext\n");

        List<BooleanQueryPart> queryParts = new ArrayList<>();
        boolean addingMore = true;
        int criterioNum = 1;

        while (addingMore) {
            System.out.println("\n--- Criterio " + criterioNum + " ---");
            System.out.print("Campo: ");
            String field = scanner.nextLine().trim();
            if (field.isEmpty()) {
                System.out.println("Errore: il campo non può essere vuoto");
                continue;
            }

            System.out.print("Query: ");
            String query = scanner.nextLine().trim();
            if (query.isEmpty()) {
                System.out.println("Errore: la query non può essere vuota");
                continue;
            }

            System.out.print("Operatori: m -> MUST, s -> SHOULD, n -> MUST_NOT]: ");
            String operatorInput = scanner.nextLine().trim();
            BooleanClause.Occur occur = BooleanClause.Occur.MUST;

            if (operatorInput.equals("s")) {
                occur = BooleanClause.Occur.SHOULD;
            } else if (operatorInput.equals("n")) {
                occur = BooleanClause.Occur.MUST_NOT;
            }

            queryParts.add(new BooleanQueryPart(field, query, occur));
            System.out.println("Criterio aggiunto: " + field + " [" + occur + "] '" + query + "'");

            System.out.print("\nAggiungere un altro criterio? (y/n): ");
            String response = scanner.nextLine().trim().toLowerCase();
            if (!response.equals("y")) {
                addingMore = false;
            }
            criterioNum++;
        }

        return queryParts;
    }

    public void printCompiledQuery(List<BooleanQueryPart> queryParts) {
        System.out.println("\n========== QUERY COMPILATA ==========");
        for (int i = 0; i < queryParts.size(); i++) {
            BooleanQueryPart part = queryParts.get(i);
            System.out.println((i + 1) + ". " + part);
        }
    }
}
