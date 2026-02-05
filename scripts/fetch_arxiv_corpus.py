import json
import os
import time
import xml.etree.ElementTree as ET
import requests
from tqdm import tqdm

ARXIV_API_URL = "http://export.arxiv.org/api/query"
ARXIV_DATA_DIR = "./corpus/arxiv"
MAX_RETRIES = 5
BASE_DELAY = 3.0
ARTICLE_MIN_LENGTH = 100_000

def parse_arxiv_feed(xml_text):
    try:
        root = ET.fromstring(xml_text)
    except ET.ParseError as e:
        # Se fallisce il parsing, restituiamo l'errore per gestirlo nel chiamante
        raise ValueError(f"XML non valido: {e}. Inizio contenuto: {xml_text[:200]}")

    ns = {'atom': "http://www.w3.org/2005/Atom"}
    
    # Verifica se la risposta è un errore API e ha tag radice come error o response invece di feed
    if root.tag != f"{{{ns['atom']}}}feed":
        pass

    entries = []
    for entry in root.findall('atom:entry', ns):
        d = {}
        id_el = entry.find('atom:id', ns)
        d['id'] = id_el.text if id_el is not None else None
        title_el = entry.find('atom:title', ns)
        d['title'] = title_el.text.strip().replace('\n', ' ') if title_el is not None else ""
        summary_el = entry.find('atom:summary', ns)
        d['summary'] = summary_el.text.strip().replace('\n', ' ') if summary_el is not None else ""
        pub_el = entry.find('atom:published', ns)
        d['published'] = pub_el.text if pub_el is not None else None
        upd_el = entry.find('atom:updated', ns)
        d['updated'] = upd_el.text if upd_el is not None else None
        authors = []
        for a in entry.findall('atom:author', ns):
            name_el = a.find('atom:name', ns)
            if name_el is not None:
                authors.append(name_el.text)
        d['authors'] = authors
        entries.append(d)
    return entries


def make_request_with_retry(url, params=None):
    for attempt in range(MAX_RETRIES):
        try:
            resp = requests.get(url, params=params, headers={'User-Agent': 'homework5-IdD'})
            
            if resp.status_code == 503:
                wait_time = BASE_DELAY * (2 ** attempt)
                print(f"\nRicevuto 503. Attesa di {wait_time}s prima del retry...")
                time.sleep(wait_time)
                continue
                
            resp.raise_for_status()
            return resp
            
        except requests.exceptions.RequestException as e:
            wait_time = BASE_DELAY * (2 ** attempt)
            print(f"\nErrore di rete ({e}). Retry {attempt + 1}/{MAX_RETRIES} tra {wait_time}s...")
            time.sleep(wait_time)
    
    return None


def fetch_feed(query, start=0, max_results=100):
    params = {
        'search_query': query,
        'start': start,
        'max_results': max_results
    }
    
    resp = make_request_with_retry(ARXIV_API_URL, params)
    if not resp:
        print("\nImpossibile recuperare il feed dopo vari tentativi.")
        return []
    try:
        return parse_arxiv_feed(resp.text)
    except ValueError as e:
        print(f"\nErrore nel parsing della risposta per start={start}:")
        print(e)
        return []

def is_article(html_content):
    return len(html_content) > ARTICLE_MIN_LENGTH or 'class="ltx_bibliography"' in html_content

def get_arxiv_info(entry):
    abs_url = entry.get('id')
    if not abs_url:
        return None, None
    arxiv_id = abs_url.rstrip('/').split('/')[-1]
    html_url = f"https://ar5iv.labs.arxiv.org/html/{arxiv_id}"
    return arxiv_id, html_url

def fetch_article_html(html_url, arxiv_id):    
    # Verifica se esiste l'articolo in formato html su ar5iv
    html_content = None
    try:
        html_resp = requests.get(html_url, headers={'User-Agent': 'homework5-IdD'}, timeout=15)
        if html_resp.status_code == 200:
            if is_article(html_resp.text):
                html_content = html_resp.text
                print(f"Articolo HTML trovato per: {arxiv_id}.")
            return html_content
    except Exception as e:
        print(f"Warning: HTML non recuperabile per {arxiv_id}: {e}")
    return None
    
KEYWORDS = [
    "text-to-sql",
    "text to sql",
    "natural language to sql",
    "natural-language-to-sql"
]

def is_terms_in(title, summary):
    title = title.lower()
    summary = summary.lower()
    return any(k in title or k in summary for k in KEYWORDS)

def save_article(output_dir, entry, arxiv_id, html_url, html_content):
    meta = {
        'id': arxiv_id,
        'published': entry.get('published'),
        'updated': entry.get('updated'),
        'summary': entry.get('summary', '').strip(),
        'authors': entry.get('authors', []),
        'title': entry.get('title', '').strip(),
        'abs_url': html_url
    }
    #secondo filtro
    if not is_terms_in(entry.get('title', '').strip(), entry.get('summary', '').strip()):
        return None
    
    # Creazione directory
    paper_dir = os.path.join(output_dir, arxiv_id.replace('/', '_'))

    if html_content:
        os.makedirs(paper_dir, exist_ok=True)
        # Salvataggio Metadata
        meta_path = os.path.join(paper_dir, 'metadata.json')
        with open(meta_path, 'w', encoding='utf-8') as f:
            json.dump(meta, f, ensure_ascii=False, indent=2)
        # Salvataggio HTML
        article_html_path = os.path.join(paper_dir, 'article.html')
        with open(article_html_path, 'w', encoding='utf-8') as f:
            f.write(html_content)
        
        return paper_dir
    return None


def fetch_corpus(query, batch_size, output_dir, sleep_sec_batch=3.0, sleep_sec_article=0.1):
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)
    fetched_count = 0
    saved_count = 0
    start = 0
    pbar = tqdm(desc='Articles processed', unit='ap')
    
    while True:
        feed_entries = fetch_feed(query, start=start, max_results=batch_size)
        if not feed_entries:
            print(f"\nNessun risultato restituito o errore nel fetch al blocco start={start}. Stop.")
            break
        
        for entry in feed_entries:
            arxiv_id, html_url = get_arxiv_info(entry)
            html_content = fetch_article_html(html_url, arxiv_id) 
            res = save_article(output_dir, entry, arxiv_id, html_url, html_content)
            fetched_count += 1
            if res:
                saved_count += 1
            pbar.update(1)
            time.sleep(sleep_sec_article)
            
        start += len(feed_entries)
        if len(feed_entries) < batch_size:
            break
        time.sleep(sleep_sec_batch)

    pbar.close()
    print(f"\n--- RIEPILOGO ---")
    print(f"Articoli processati dal feed: {fetched_count}")
    print(f"Articoli salvati (HTML disponibile su ar5iv): {saved_count}")


def main():
    batch_size = 100
    sleep_interval = 3.0
    query = 'ti:"text-to-sql" OR ti:"Natural language to sql" OR abs:"text-to-sql" OR abs:"Natural language to sql"'
    print(f"FETCHING ARXIV\nAvvio fetch per query: {query}")
    fetch_corpus(query, batch_size, ARXIV_DATA_DIR, sleep_interval)

if __name__ == '__main__':
    main()