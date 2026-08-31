import urllib.request
import json
import re
import os
from collections import Counter

print("Téléchargement du texte Ndruna...")

url = "https://m.ebible.org/json/niy_nt.json"

try:
    req = urllib.request.Request(
        url,
        headers={"User-Agent": "Mozilla/5.0"}
    )

    with urllib.request.urlopen(req, timeout=30) as response:
        data = json.loads(response.read().decode("utf-8"))

    full_text = ""

    for book in data.get("books", []):
        for chapter in book.get("chapters", []):
            for verse in chapter.get("verses", []):
                full_text += " " + verse.get("text", "")

    # Extraire uniquement les mots
    words = re.findall(
        r"\b[^\d\W_]+\b",
        full_text.lower(),
        re.UNICODE
    )

    # Compter les occurrences
    frequencies = Counter(words)

    # Trier du plus fréquent au moins fréquent
    sorted_words = sorted(
        frequencies.items(),
        key=lambda x: (-x[1], x[0])
    )

    os.makedirs("/sdcard/Download", exist_ok=True)

    # Fichier mot + fréquence
    output_file = "/sdcard/Download/dictionnaire_ndruna_frequence.txt"

    with open(output_file, "w", encoding="utf-8") as f:
        for word, count in sorted_words:
            f.write(f"{word}\t{count}\n")

    print()
    print("======================================")
    print("SUCCÈS !")
    print(f"Mots différents : {len(frequencies)}")
    print(f"Occurrences totales : {len(words)}")
    print(f"Fichier : {output_file}")
    print("======================================")

except Exception as e:
    print("ERREUR :", e)
