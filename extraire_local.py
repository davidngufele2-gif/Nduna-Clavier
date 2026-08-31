import os
import glob
import zipfile
import sqlite3
import re

print("Recherche de la base de données dans l'application...")

# Recherche des bases de données SQLite ou fichiers XML dans Termux/Stockage
words = set()

# Alternative : Téléchargement du texte dictionnaire brut Ndruna centralisé
import urllib.request

urls = [
    "https://ebible.org/txt/niy_nt.txt",
    "https://raw.githubusercontent.com/godlygeek/bible/master/ndruna.txt"
]

download_success = False
for url in urls:
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req) as resp:
            text = resp.read().decode('utf-8', errors='ignore')
            extracted = re.findall(r'\b[^\d\W_]+\b', text.lower(), re.UNICODE)
            words.update(extracted)
            if len(extracted) > 1000:
                download_success = True
                break
    except Exception:
        continue

if words:
    unique_words = sorted(list(words))
    out_path = "/sdcard/Download/dictionnaire_ndruna.txt"
    with open(out_path, "w", encoding="utf-8") as f:
        for w in unique_words:
            f.write(w + "\n")
    print(f"Succès ! {len(unique_words)} mots enregistrés dans Download/dictionnaire_ndruna.txt")
else:
    print("Erreur : Impossible de récupérer les données texte en ligne.")
