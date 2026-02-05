import json
import os
import time
import xml.etree.ElementTree as ET
import requests
from tqdm import tqdm
from bs4 import BeautifulSoup

NCBI_API_BASE_URL = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/"
PMC_WEB_BASE_URL = "https://pmc.ncbi.nlm.nih.gov/articles/"
PUBMED_DATA_DIR = "./corpus/pubmed"
EMAIL_UTENTE = "gio.melchiorri@stud.uniroma3.it"
SCRIPT_NAME = "homework5-IdD"
BASE_DELAY = 1

def find_pmc_articles_ids(query, max_results=10):
    base_url = f"{NCBI_API_BASE_URL}esearch.fcgi"
    all_ids = []
    retstart = 0
    retmax = 500
    while True:
        search_params = {
            "db": "pmc",                  
            "term": query,
            "retmode": "json",
            "retstart": retstart,
            "retmax": retmax,
            "email": EMAIL_UTENTE,                    
            "tool": SCRIPT_NAME               
        }    
        try:
            resp = requests.get(base_url, params=search_params)
            resp.raise_for_status()
            data = resp.json()
            
            id_list = data.get("esearchresult", {}).get("idlist", [])
            count = int(data.get("esearchresult", {}).get("count", "0"))
            
            if not id_list:
                break
                
            all_ids.extend(id_list)
            print(f"Recuperati {len(all_ids)} su {count} ID...")
            
            retstart += retmax
            if retstart >= count:
                break
                
            time.sleep(1)
        except requests.exceptions.RequestException as e:
            print(f"Errore durante la ricerca ID: {e}")
            break
    return all_ids

def get_metadata(pmc_id, abstract):
    """
    Ottiene i metadati (JSON) tramite ESummary API.
    """
    base_url = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi"
    params = {
        "db": "pmc",
        "id": pmc_id,
        "retmode": "json",
        "email": EMAIL_UTENTE,
        "tool": SCRIPT_NAME
    }
    try:
        resp = requests.get(base_url, params=params)
        resp.raise_for_status()
        data = resp.json().get("result", {}).get(pmc_id, {})
        metadata ={
            "id" : data["uid"],
            "published" : data["pubdate"],
            "updated" : data["epubdate"],
            "summary" : abstract,
            "authors" : [author.get("name") for author in data.get("authors")],
            "title" : data["title"],
        }
        return metadata
    except Exception as e:
        print(f"Errore metadati {pmc_id}: {e}")
        return None
    
def download_html_page(pmc_id):
    #aggiungi PMC
    full_pmc_id = f"PMC{pmc_id}"
    url = f"{PMC_WEB_BASE_URL}{full_pmc_id}/"
    
    headers = {
        "User-Agent": f"{SCRIPT_NAME} (mailto:{EMAIL_UTENTE})"
    }
    try:
        resp = requests.get(url, headers=headers)
        resp.raise_for_status()
        return resp.text
    except Exception as e:
        print(f"Errore download HTML {full_pmc_id}: {e}")
        return None
    
def extract_abstract_from_html(html_content, id_article_debug=None):
    if not html_content:
        return None
        
    soup = BeautifulSoup(html_content, 'html.parser')
    abstract_node = None

    abstract_node = soup.find('section', class_='abstract')
    if abstract_node:
        return abstract_node.get_text(separator=" ", strip=True)
    if not id_article_debug==None:
        print(f"Abstract non trovato nell'HTML. Articolo: {id_article_debug}")
    return ""

def fetch_articles_from_pubmed(query):
    if not os.path.exists(PUBMED_DATA_DIR):
        os.makedirs(PUBMED_DATA_DIR)
        
    ids = find_pmc_articles_ids(query)
    print(f"\nTotale articoli da scaricare: {len(ids)}")
    ignored_articles = 0
    
    print("Inizio download articoli...")
    for pmc_id in tqdm(ids):
        article_dir = os.path.join(PUBMED_DATA_DIR, pmc_id)
        if os.path.exists(article_dir):
            continue
        os.makedirs(article_dir, exist_ok=True)
        # scarica articolo html (prima dei metadati per prendere l'abstract)
        html_content = download_html_page(pmc_id)
        extracted_abstract = None
        if html_content:
            # secondo filtro: non salvare se la query è stata trovata solo in bibliografia/risorse
            article_content = html_content.lower().split('l class="ref-list')[0]
            if ("coffee consumption" not in article_content) or ("cancer risk" not in article_content):
                os.rmdir(article_dir)
                ignored_articles+=1
                continue
            with open(os.path.join(article_dir, "article.html"), "w", encoding="utf-8") as f:
                f.write(html_content)
            extracted_abstract = extract_abstract_from_html(html_content)
        # cerca e salva metadati
        metadata = get_metadata(pmc_id, extracted_abstract)
        if metadata:
            with open(os.path.join(article_dir, "metadata.json"), "w", encoding="utf-8") as f:
                json.dump(metadata, f, indent=4)
        time.sleep(BASE_DELAY)
    
    print(f"\nTotale articoli scaricati: {len(ids)-ignored_articles}")


def main():
    query = '("cancer risk" AND "coffee consumption") AND "open access"[filter]'
    fetch_articles_from_pubmed(query)

if __name__ == '__main__':
    main()