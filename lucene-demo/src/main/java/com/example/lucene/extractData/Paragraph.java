package com.example.lucene.extractData;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Paragraph {

    public static Elements findAllParagraphsFromDocument(Document doc){
        //p è per pubmed
        Elements rawParagraphs = doc.select("section.ltx_paragraph, .ltx_para, .ltx_p, p");
        //potrebbe aver estratto lo stesso testo. esempio estrae testo da ltx_paragraph e una copia da un elemento suo figlio .ltx_para
        Elements uniqueParagraphs = new Elements();
        Set<String> seenText = new LinkedHashSet<>();
        for (Element p : rawParagraphs) {
            String text = p.text().trim();
            // Se il testo è vuoto o  già aggiunto si salta
            if (!text.isEmpty() && !seenText.contains(text)) {
                seenText.add(text);
                uniqueParagraphs.add(p);
            }
        }
        
        return uniqueParagraphs;
    }
    /**
     * risali all'antenato più vicino di tipo section che possiede la classe "ltx_paragraph"
     */
    public static Element findParagraphParent(Element elmt){
        Element paragraph = elmt.closest("section.ltx_paragraph, .ltx_para, .ltx_p, p");
        return paragraph;
    }

    /**
     * restituisci i paragrafi che citano una figura (immagine o tabella)
     */
    public static ArrayList<String> find_citingParagraphs(String figureId, Document doc){
        if (doc == null || figureId == null) {
            return new ArrayList<>();
        }
        // uso di un Set per evitare duplicati
        Set<String> uniqueCiters = new LinkedHashSet<>();
        //identifica un elemento che cita da un qualsiasi anchor element con valore dell'attributo href uguale all'id della atbella
        //apici al selettore per gestire id complessi come S1.T1 
        Elements raw_citers = doc.select("*[href='#"+figureId+"']");
        for (Element citer : raw_citers){
            //trova il paragrafo a cui appartiene l'elemento citatore
            Element paragraph = Paragraph.findParagraphParent(citer);
            if (paragraph != null) {
                String paragraphText = paragraph.text().trim();
                if (!paragraphText.isEmpty()) {
                    uniqueCiters.add(paragraphText);
                }
            }
        }
        return new ArrayList<String>(uniqueCiters);
    }

    
}
