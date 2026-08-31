import zlib, struct

def create_ndruna_icon():
    # Génère un SVG haute résolution avec le fond vert, le clavier et les caractères ǎɨʉ́ɔ̀
    svg_content = '''<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 512 512">
  <defs>
    <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#0a462d"/>
      <stop offset="100%" stop-color="#123f30"/>
    </linearGradient>
  </defs>
  <rect width="512" height="512" rx="110" ry="110" fill="url(#bg)"/>
  <!-- Clavier discret -->
  <g fill="none" stroke="rgba(255,255,255,0.15)" stroke-width="3" rx="8">
    <rect x="50" y="220" width="38" height="42" rx="8"/>
    <rect x="96" y="220" width="38" height="42" rx="8"/>
    <rect x="142" y="220" width="38" height="42" rx="8"/>
    <rect x="188" y="220" width="38" height="42" rx="8"/>
    <rect x="234" y="220" width="38" height="42" rx="8"/>
    <rect x="280" y="220" width="38" height="42" rx="8"/>
    <rect x="326" y="220" width="38" height="42" rx="8"/>
    <rect x="372" y="220" width="38" height="42" rx="8"/>
    <rect x="418" y="220" width="38" height="42" rx="8"/>
    <rect x="70" y="275" width="38" height="42" rx="8"/>
    <rect x="116" y="275" width="38" height="42" rx="8"/>
    <rect x="162" y="275" width="38" height="42" rx="8"/>
    <rect x="208" y="275" width="38" height="42" rx="8"/>
    <rect x="254" y="275" width="38" height="42" rx="8"/>
    <rect x="300" y="275" width="38" height="42" rx="8"/>
    <rect x="346" y="275" width="38" height="42" rx="8"/>
    <rect x="392" y="275" width="38" height="42" rx="8"/>
    <rect x="120" y="330" width="260" height="42" rx="8"/>
  </g>
  <!-- Caractères Ndruna -->
  <text x="256" y="180" font-family="DejaVu Sans, Arial, sans-serif" font-size="110" font-weight="bold" fill="#ffffff" text-anchor="middle">ǎɨʉ́ɔ̀</text>
</svg>'''
    with open('/data/data/com.termux/files/home/NdrunaKeyboard/app/src/main/res/drawable/ic_launcher.svg', 'w') as f:
        f.write(svg_content)
    print("SVG créé avec succès !")

create_ndruna_icon()
