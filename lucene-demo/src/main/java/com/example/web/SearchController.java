package com.example.web;

import com.example.lucene.search.*;
import com.example.lucene.search.articleSearch.ArticleSearchResult;
import com.example.lucene.search.articleSearch.ArticleSearcher;
import com.example.lucene.search.imageSearch.ImageSearcher;
import com.example.lucene.search.tableSearch.TableSearcher;

import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
public class SearchController {

    private final ArticleSearcher articleSearcher;
    private final ImageSearcher imageSearcher;
    private final TableSearcher tableSearcher;

    public SearchController(ArticleSearcher articleSearcher,
                            ImageSearcher imageSearcher,
                            TableSearcher tableSearcher) {
        this.articleSearcher = articleSearcher;
        this.imageSearcher = imageSearcher;
        this.tableSearcher = tableSearcher;
    }

    // ------------------------------------------------------
// 1) RICERCA GENERICA
// ------------------------------------------------------
@GetMapping("/search")
public String search(
        @RequestParam(name = "keyword", defaultValue = "") String keyword,
        @RequestParam(name = "types", required = false) List<String> types,
        @RequestParam(name = "fieldsArticle", required = false) String[] fieldsArticle,
        @RequestParam(name = "fieldsImage", required = false) String[] fieldsImage,
        @RequestParam(name = "fieldsTable", required = false) String[] fieldsTable,
        @RequestParam(name = "maxResults", defaultValue = "20") int maxResults,
        Model model) throws IOException, ParseException {

    List<ISearchResult> results = new ArrayList<>();

    if (types == null || types.isEmpty()) {
        types = List.of("all");
    } else {
        types = types.stream()
                     .filter(Objects::nonNull)
                     .map(String::trim)
                     .map(String::toLowerCase)
                     .toList();
    }

    if (!keyword.isBlank()) {
        for (String type : types) {
            switch (type) {
                case "article" -> {
                    String[] fields = (fieldsArticle != null && fieldsArticle.length > 0)
                            ? fieldsArticle
                            : new String[]{"fulltext"};
                    List<ArticleSearchResult> rawResults = articleSearcher.searchByFields(keyword, fields, maxResults);
                    results.addAll(formatArticleDates(rawResults));
                }
                case "image" -> {
                    String[] fields = (fieldsImage != null && fieldsImage.length > 0)
                            ? fieldsImage
                            : new String[]{"caption"};
                    results.addAll(imageSearcher.searchByFields(keyword, fields, maxResults));
                }
                case "table" -> {
                    String[] fields = (fieldsTable != null && fieldsTable.length > 0)
                            ? fieldsTable
                            : new String[]{"body"};
                    results.addAll(tableSearcher.searchByFields(keyword, fields, maxResults));
                }
                case "all" -> {
                    // articoli
                    List<ArticleSearchResult> rawResults = articleSearcher.searchByField("fulltext", keyword, maxResults);
                    results.addAll(formatArticleDates(rawResults));

                    // immagini e tabelle restano uguali
                    results.addAll(imageSearcher.searchByField("caption", keyword, maxResults));
                    results.addAll(tableSearcher.searchByField("body", keyword, maxResults));
                }
            }
        }

        // Ordinamento globale e limitazione finale
        results = results.stream()
                         .sorted(Comparator.comparing(ISearchResult::getScore).reversed())
                         .limit(maxResults)
                         .toList();
    }

    model.addAttribute("results", results);
    model.addAttribute("keyword", keyword);
    model.addAttribute("types", types);
    model.addAttribute("fieldsArticle", fieldsArticle != null ? Arrays.asList(fieldsArticle) : List.of());
    model.addAttribute("fieldsImage", fieldsImage != null ? Arrays.asList(fieldsImage) : List.of());
    model.addAttribute("fieldsTable", fieldsTable != null ? Arrays.asList(fieldsTable) : List.of());
    model.addAttribute("maxResults", maxResults);

    return "index";
}

// ------------------------------------------------------
// 2) RICERCA PER ID (paper_id)
// ------------------------------------------------------
@GetMapping("/search/id")
public String searchById(
        @RequestParam(name = "id") String id,
        @RequestParam(name = "maxResults", defaultValue = "5") int maxResults,
        Model model) throws IOException, ParseException {

    List<ArticleSearchResult> rawResults = articleSearcher.searchByField("paper_id", id, maxResults);
    List<ArticleSearchResult> results = formatArticleDates(rawResults);

    model.addAttribute("results", results);
    model.addAttribute("id", id);
    model.addAttribute("type", "article");
    model.addAttribute("field", "paper_id");
    model.addAttribute("maxResults", maxResults);

    return "index";
}

// ------------------------------------------------------
// 3) RICERCA PER DATE
// ------------------------------------------------------
@GetMapping("/search/date")
public String searchByDate(@RequestParam(required = false) String fromYear,
                           @RequestParam(required = false) String fromMonth,
                           @RequestParam(required = false) String fromDay,
                           @RequestParam(required = false) String toYear,
                           @RequestParam(required = false) String toMonth,
                           @RequestParam(required = false) String toDay,
                           @RequestParam(name = "maxResults", defaultValue = "20") int maxResults,
                           Model model) throws IOException {

    String from = null;
    String to = null;

    if (fromYear != null && !fromYear.isEmpty()) {
        from = fromYear + "-" +
               (fromMonth != null && !fromMonth.isEmpty() ? String.format("%02d", Integer.parseInt(fromMonth)) : "01") + "-" +
               (fromDay != null && !fromDay.isEmpty() ? String.format("%02d", Integer.parseInt(fromDay)) : "01") +
               "T00:00:00Z";
    }

    if (toYear != null && !toYear.isEmpty()) {
        to = toYear + "-" +
             (toMonth != null && !toMonth.isEmpty() ? String.format("%02d", Integer.parseInt(toMonth)) : "12") + "-" +
             (toDay != null && !toDay.isEmpty() ? String.format("%02d", Integer.parseInt(toDay)) : "31") +
             "T23:59:59Z";
    }

    List<ArticleSearchResult> rawResults = articleSearcher.searchByDateRange(from, to, maxResults);
    List<ArticleSearchResult> results = formatArticleDates(rawResults);

    // Ordina dal più nuovo al più vecchio
    results.sort((a, b) -> {
        String da = a.getDate() == null ? "" : a.getDate();
        String db = b.getDate() == null ? "" : b.getDate();
        return db.compareTo(da);
    });

    model.addAttribute("results", results);
    model.addAttribute("type", "article");
    model.addAttribute("field", "date");
    model.addAttribute("maxResults", maxResults);

    return "index";
}

// ------------------------------------------------------
// Metodo helper: formatta tutte le date degli articoli
// ------------------------------------------------------
private List<ArticleSearchResult> formatArticleDates(List<ArticleSearchResult> rawResults) {
    List<ArticleSearchResult> formatted = new ArrayList<>();
    for (ArticleSearchResult r : rawResults) {
        String formattedDate = formatDate(r.getDate());
        formatted.add(new ArticleSearchResult(
                r.getId(),
                r.getTitle(),
                r.getAuthors(),
                formattedDate,
                r.getAbstract(),
                r.getScore()
        ));
    }
    return formatted;
}

// ------------------------------------------------------
// Metodo di formattazione singola data
// ------------------------------------------------------
private String formatDate(String dateStr) {
    if (dateStr == null || dateStr.isEmpty()) return "";

    // prova ISO (ArXiv)
    try {
        ZonedDateTime zdt = ZonedDateTime.parse(dateStr);
        return zdt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    } catch (Exception ignored) {}

    // prova PubMed "yyyy MMM d"
    try {
        LocalDate ld = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy MMM d", Locale.ENGLISH));
        return ld.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    } catch (Exception ignored) {}

    // fallback
    return dateStr;
}
}
