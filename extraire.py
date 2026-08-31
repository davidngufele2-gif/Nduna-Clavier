import urllib.request
import re

print("Téléchargement du texte Ndruna...")

# URL directe du texte USFM de la Bible en Ndruna (niy)
url = "https://raw.githubusercontent.com/ebible-org/niy/main/niy.usfm"

try:
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    with urllib.request.urlopen(req) as response:
        full_text = response.read().decode('utf-8', errors='ignore')

        # Supprimer les balises USFM (ex: \v, \c, \p)
        clean_text = re.sub(r'\\[a-z0-9]+\*?', '', full_text)

        # Nettoyage et récupération des mots uniques (avec diacritiques)
        words = re.findall(r'\b[^\d\W_]+\b', clean_text.lower(), re.UNICODE)
        unique_words = sorted(set(words))

        # Enregistrement dans le stockage interne
        output_file = "/sdcard/Download/dictionnaire_ndruna.txt"
        with open(output_file, "w", encoding="utf-8") as f:
            for word in unique_words:
                f.write(word + "\n")

        print(f"Succès ! {len(unique_words)} mots enregistrés dans Download/dictionnaire_ndruna.txt")

except Exception as e:
    print(f"Erreur : {e}")
