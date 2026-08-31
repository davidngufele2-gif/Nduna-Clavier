import os
import re
from collections import Counter
from html import unescape

DOSSIER = "niy_html"
SORTIE = "/sdcard/Download/dictionnaire_ndruna_frequence.txt"

texte = ""

for root, dirs, files in os.walk(DOSSIER):
    for fichier in files:
        if fichier.lower().endswith((".html", ".htm")):
            chemin = os.path.join(root, fichier)

            try:
                with open(chemin, "r", encoding="utf-8") as f:
                    contenu = f.read()

                # Supprimer les balises HTML
                contenu = re.sub(r"<[^>]+>", " ", contenu)

                # Convertir les caractères HTML
                contenu = unescape(contenu)

                texte += " " + contenu

            except Exception as e:
                print("Fichier ignoré :", chemin, e)

# Mots Ndruna
mots = re.findall(
    r"[^\W\d_]+(?:['’][^\W\d_]+)*",
    texte.lower(),
    re.UNICODE
)

frequences = Counter(mots)

# Trier : plus fréquent en premier
mots_tries = sorted(
    frequences.items(),
    key=lambda x: (-x[1], x[0])
)

os.makedirs("/sdcard/Download", exist_ok=True)

with open(SORTIE, "w", encoding="utf-8") as f:
    for mot, nombre in mots_tries:
        f.write(f"{mot}\t{nombre}\n")

print()
print("================================")
print("EXTRACTION TERMINÉE")
print("================================")
print("Mots différents :", len(frequences))
print("Mots totaux :", len(mots))
print("Fichier :", SORTIE)
print("================================")
