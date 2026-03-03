# Document Retrieval and Indexing System

Questo repository contiene l'implementazione di un sistema volto al recupero, indicizzazione e ricerca di articoli scientifici. Il sistema pone particolare attenzione alle figure, estraendo tabelle e immagini per renderle autonomamente interrogabili e massimizzare il valore informativo. 

## Domini di Ricerca e Acquisizione Dati

Il sistema acquisisce articoli solo se presente la loro versione in formato HTML, concentrandosi su due domini:
* **Corpus arXiv**: Selezione di articoli il cui titolo o abstract contiene le stringhe "text-to-sql" o "Natural Language to SQL".
* **Corpus PubMed**: Selezione di articoli open access recuperati tramite la query target "cancer risk AND coffee consumption".

## Stack Tecnologico

L'architettura sfrutta diversi linguaggi e librerie specializzate:
* **Python**: Impiegato per lo sviluppo del modulo di Data Ingestion[. Tramite la libreria requests, sono state gestite le chiamate HTTP per lo scaricamento massivo degli articoli.
* **Java**: Costituisce il nucleo portante del sistema per l'implementazione della logica di elaborazione, del motore di indicizzazione e dell'interfaccia di ricerca.
* **Jsoup**: Strumento di riferimento per il parsing dei documenti HTML.
* **Apache Lucene**: Rappresenta il nucleo delle fasi di Indexing e Retrieval per la creazione degli indici invertiti.

## Fasi della Pipeline

Il sistema si articola in tre fasi principali:
1. **Acquisizione automatica**: Recupero di documenti in formato HTML dalle piattaforme arXiv e PubMed. Per arXiv viene sfruttato il portale ar5iv per trasformare i sorgenti LaTeX in HTML strutturato.
2. **Analisi e parsing del documento**: Estrazione automatica di metadati e contenuti strutturati. Il sistema rimuove il rumore per evitare un alto tasso di falsi positivi durante la ricerca.
3. **Indicizzazione e Ricerca**: Utilizzo del motore di ricerca Lucene per l'indicizzazione dei dati estratti, permettendo l'esecuzione di query booleane e full-text.

## Modello Matematico per il Context Matching

Per determinare la pertinenza di un paragrafo rispetto a una risorsa visiva, il sistema adotta un modello di matching pesato basato sulla metrica Inverse Document Frequency. 
Il calcolo del peso è inversamente proporzionale alla frequenza del termine all'interno dell'articolo:

$w_{t}=\log(\frac{N_{paragraphs}}{DF(t)+1})$ 

Un paragrafo viene classificato come contesto valido se il suo punteggio supera una soglia relativa, fissata sperimentalmente al 20% del valore totale della figura:

$Score(P)\ge0.20\times Score_{tot}$ 

## Strategia di Storage e Indici Lucene

Il sistema organizza i dati in tre indici specializzati, distinguendo le entità per tipologia (Articoli, Immagini, Tabelle). 
L'architettura dei campi è progettata per bilanciare le capacità di ricerca con l'efficienza dello storage:

| Entità | Campi Persistenti (Store.SI) | Campi Solo Indicizzati (Store.NO) |
| :--- | :--- | :--- |
| **Articolo** | id, date, title, abstract, fulltext | Nessuno |
| **Immagine** | image_id, paper_id, url, caption  | citing_paragraphs, context_paragraphs |
| **Tabella** | table_id, paper_id, body, caption | citing_paragraphs, context_paragraphs |

I paragrafi citanti e di contesto sono indicizzati esclusivamente per finalità di retrieval full-text. Non vengono memorizzati negli indici delle immagini e delle tabelle per evitare ridondanza, poiché già persistiti nell'indice degli articoli.

## Ricerca e Interrogazione

Il motore di ricerca supporta diverse modalità principali:
* **Ricerca su Singolo Campo**: Il sistema chiede prima il campo su cui effettuare la ricerca e poi i termini query.
* **Ricerca per Range Temporale**: Sfrutta l'ordinamento lessicografico dei termini per eseguire filtri temporali efficienti.
* **Ricerca Booleana Complessa**: Permette la combinazione di più criteri di ricerca attraverso l'aggregazione di più BooleanQueryPart.
* **Recupero Articoli tramite Contenuto Tabellare**: Viene eseguita una ricerca full-text sul campo body dell'indice delle tabelle per aggregare i riferimenti univoci agli articoli di provenienza.
