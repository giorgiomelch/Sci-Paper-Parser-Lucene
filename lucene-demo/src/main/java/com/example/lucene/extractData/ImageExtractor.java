package com.example.lucene.extractData;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ImageExtractor{
    
    //  Estrai TUTTE le immagini da un HTML.
    public static List<ImageData> extractImages(String html) {
        List<ImageData> images = new ArrayList<>();
        Document doc = Jsoup.parse(html);
        // Seleziona tutti gli elementi figure 
        Elements imagesElements = doc.select("figure");

        for (Element imageEl : imagesElements) {
            // DA CAMBIARE: logica debole limitate a solo due classi. Fare analisi su tutte le classi contenenti un'immagine (es in base a presenza di url)
            // Una figura è valida solo se contiene <img> o <svg>
            boolean hasNotImg = imageEl.select("img").isEmpty();
            boolean hasNotSvg = imageEl.select("svg").isEmpty();
            if (hasNotImg && hasNotSvg) {
                continue;
    }
            // Estrai l'id
            String imageId = imageEl.attr("id");
            // Trova l'elemento img dentro la figure con dentro l'url
            Element img = imageEl.selectFirst("img");
            String url;
            if (img == null) {
                img = imageEl.selectFirst("svg");
                url = "svg no url";
                if (img == null){
                    System.err.println("Url non trovato per l'immagine con id: " + imageId);
                    continue;
                }
            }
            else{
                url = img.attr("src");
            }
            String caption = "";
            Element figcaption = imageEl.selectFirst("figcaption");
            if (figcaption != null) {
                caption = figcaption.text(); 
            }
            List<String> citingParagraphs = Paragraph.find_citingParagraphs(imageId, doc);
            //estrai i paragrafi di contesto
            List<String> context_paragraphs = FigureMatchParagraphByContext.find_contextParagraphs(imageId, caption, null, doc);

            images.add(new ImageData(imageId, url, caption, citingParagraphs, context_paragraphs));
        }
        return images;
    }
}