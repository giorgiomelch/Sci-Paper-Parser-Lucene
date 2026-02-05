package com.example.lucene.extractData;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class FigureMatchParagraphByContext {

    private static final Logger LOGGER = Logger.getLogger(FigureMatchParagraphByContext.class.getName());

    // se un termine appare in più del 10% dei paragrafi è considerato generico
    private static final double MAX_DOC_FREQ_PERCENTAGE = 0.10;
    private static final double THRESHOLD_RATIO = 0.20;
    
    private static List<String> extractCleanParagraphs(Document doc) {
        return Paragraph.findAllParagraphsFromDocument(doc).stream()
                .map(Element::text)
                .map(String::trim)
                .filter(text -> text.length() > 50)
                .collect(Collectors.toList());
    }
    
    private static Map<String, Integer> computeDFMap(List<String> paragraphs, Analyzer analyzer) throws IOException {
                                                        Map<String, Integer> dfMap = new HashMap<>();
        for (String p : paragraphs) {
            Set<String> tokens = analyze(p, analyzer);
            for (String token : tokens) {
                dfMap.put(token, dfMap.getOrDefault(token, 0) + 1);
            }
        }
        return dfMap;
    }
    private static Map<String, Double> buildWeightedSignature(String caption, 
                                                                List<String> rowsContent, 
                                                                Map<String, Integer> dfMap, 
                                                                int totalDocs, 
                                                                Analyzer analyzer
                                                            ) throws IOException {
        String vocabulary = ((caption != null) ? caption : "") + " " + 
                        ((rowsContent != null) ? String.join(" ", rowsContent) : "");
        Set<String> tokens = analyze(vocabulary, analyzer);
        Map<String, Double> weights = new HashMap<>();

        for (String token : tokens) {
            int df = dfMap.getOrDefault(token, 0);
            double percentage = (double) df / totalDocs;

            if (df > 0 && percentage < MAX_DOC_FREQ_PERCENTAGE && !token.matches("\\d+")) {
                double idf = Math.log((double) totalDocs / (df + 1));
                weights.put(token, idf);
            }
        }
        return weights;
    }

    private static double calculateParagraphScore(Set<String> paraTokens, Map<String, Double> signatureWeights) {
        return paraTokens.stream()
                .filter(signatureWeights::containsKey)
                .mapToDouble(signatureWeights::get)
                .sum();
    }

    public static List<String> find_contextParagraphs(String figureId, String caption, List<String> rowsContent, Document doc) {
        if (doc == null || figureId == null) return new ArrayList<>();

        try (Analyzer analyzer = new EnglishAnalyzer()) {
            List<String> allParagraphs = extractCleanParagraphs(doc);
            Map<String, Integer> dfMap = computeDFMap(allParagraphs, analyzer);

            // definizione firma
            Map<String, Double> signatureWeights = buildWeightedSignature(caption, rowsContent, dfMap, allParagraphs.size(), analyzer);
            if (signatureWeights.isEmpty()) return new ArrayList<>();

            double totalSignatureScore = signatureWeights.values().stream().mapToDouble(Double::doubleValue).sum();
            double minScoreRequired = totalSignatureScore * THRESHOLD_RATIO;

            // matching
            return allParagraphs.stream()
                .filter(p -> {
                    try {
                        Set<String> paraTokens = analyze(p, analyzer);
                        return !paraTokens.isEmpty() && calculateParagraphScore(paraTokens, signatureWeights) >= minScoreRequired;
                    } catch (IOException e) { return false; }
                })
                .collect(Collectors.toList());

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Errore analisi contesto per: " + figureId, e);
            return new ArrayList<>();
        }
    }

    // Helper per tokenizzare
    private static Set<String> analyze(String text, Analyzer analyzer) throws IOException {
        Set<String> result = new HashSet<>();
        TokenStream tokenStream = analyzer.tokenStream("content", new StringReader(text));
        CharTermAttribute attr = tokenStream.addAttribute(CharTermAttribute.class);
        tokenStream.reset();
        while (tokenStream.incrementToken()) {
            String term = attr.toString();
            if (term.length() > 2) {
                result.add(term);
            }
        }
        tokenStream.end();
        tokenStream.close();
        return result;
    }    
}