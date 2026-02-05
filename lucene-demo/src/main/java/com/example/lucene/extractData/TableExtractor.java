package com.example.lucene.extractData;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class TableExtractor {

    //  Estrai TUTTE le tabelle da un HTML.
    public static List<TableData> extractTables(String html) {
        List<TableData> tables = new ArrayList<>();
        Document doc = Jsoup.parse(html);
        // estrai gli elementi contenitori di tabelle
        Elements tables_elements =  doc.select("figure.ltx_table, section.tw:has(table)");
        
        for (Element table_el : tables_elements) {
            // Estrai l'id
            String tableId = table_el.attr("id");
            //estrai la caption
            String caption = "";
            Element figcaption = table_el.selectFirst("figcaption, div.caption p");
            if (figcaption != null) {
                caption = figcaption.text(); 
            }
            //estrai il corpo come righe TODO estrazione più intelligente: concateneare i valori delle celle con categorie row e column per query migliore
            List<String> rowsContent = extractTableRows(table_el);
            // trova i paragrafi che citano tamite attributo "id"
            List<String> citingParagraphs = Paragraph.find_citingParagraphs(tableId, doc);
            //estrai i paragrafi di contesto
            List<String> context_paragraphs = FigureMatchParagraphByContext.find_contextParagraphs(tableId, caption, rowsContent, doc);
            
            tables.add(new TableData(
                tableId, 
                caption, 
                rowsContent, 
                citingParagraphs,
                context_paragraphs
            ));
        }
        return tables;
    }

    private static List<String> extractTableRows(Element figure) {
        List<String> rowsContent = new ArrayList<>();
        Elements rows = figure.select("table tr");
        for (Element row : rows) {
            // seleziona TableHeader e TableData
            Elements cells = row.select("th, td");
            if (cells.isEmpty()) continue;
            String rowText = cells.stream()
                .map(Element::text)
                .map(String::strip)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(" "));

            if (!rowText.isEmpty()) {
                rowsContent.add(rowText);
            }
        }
        return rowsContent;
    }

}
