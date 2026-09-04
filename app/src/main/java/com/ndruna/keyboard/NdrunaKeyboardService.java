package com.ndruna.keyboard;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.EditorInfo;
import android.text.InputType;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NdrunaKeyboardService extends InputMethodService {

    private boolean shifted = true;
    private boolean numbers = false;
    private boolean extraSymbols = false;
    private boolean autoCap = true;
    private boolean actionExecuted = false;
    private final List<Button> letterButtons = new ArrayList<>();
    private final Handler longPressHandler = new Handler();

    // Correcteur différé : ne ralentit pas la frappe rapide.
    private final ExecutorService suggestionExecutor = Executors.newSingleThreadExecutor();
    private final Handler suggestionHandler =
            new Handler(android.os.Looper.getMainLooper());
    private Runnable suggestionTask;
    private LinearLayout suggestionRow;

    private final List<String> ndrunaWords = new ArrayList<>();
    private final List<String> normalizedNdrunaWords = new ArrayList<>();

    private void loadNdrunaDictionary() {
        if (!ndrunaWords.isEmpty()) return;

        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            getAssets().open("ndruna_words.txt"),
                            "UTF-8"));

            String word;

            while ((word = reader.readLine()) != null) {
                word = word.trim();

                if (!word.isEmpty()) {
                    String cleanWord = word.toLowerCase(Locale.ROOT);
                    ndrunaWords.add(cleanWord);
                    normalizedNdrunaWords.add(removeAccents(cleanWord).toLowerCase(Locale.ROOT));
                }
            }

            reader.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public View onCreateInputView() {
        loadNdrunaDictionary();
        return createKeyboard();
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        actionExecuted = false;

        if (attribute != null) {
            int inputType = attribute.inputType;

            if ((inputType & InputType.TYPE_CLASS_TEXT) != 0) {
                autoCap = true;
                shifted = true;
            }
        }
    }


    private View createKeyboard() {

        LinearLayout keyboard = new LinearLayout(this);
        keyboard.setOrientation(LinearLayout.VERTICAL);
        keyboard.setPadding(3, 3, 3, 3);

        addSuggestionRow(keyboard);

        addNdrunaRow(keyboard);

        if (!numbers) {

            addRow(keyboard,
                    new String[]{"a","z","e","r","t","y","u","i","o","p"});

            addRow(keyboard,
                    new String[]{"q","s","d","f","g","h","j","k","l","m"});

            addRow(keyboard,
                    new String[]{"⇧","w","x","c","v","b","n","⌫"});

            addBottomRow(keyboard);

        } else {

            if (extraSymbols) {

                addRow(keyboard,
                        new String[]{"_",":",";","'","\"","!","$","^","\\","|"});

                addRow(keyboard,
                        new String[]{"`","~","<",">","«","»","…","•","°","©"});

                addRow(keyboard,
                        new String[]{"✓","♥","→","←","↑","↓","≈","≠","±","×"});

                addNumberBottomRow(keyboard);

            } else {

                addRow(keyboard,
                        new String[]{"1","2","3","4","5","6","7","8","9","0"});

                addRow(keyboard,
                        new String[]{"@","#","€","%","&","*","+","=","-","/"});

                addRow(keyboard,
                        new String[]{"(",")","[","]","{","}",".",",","?","#+="});

                addNumberBottomRow(keyboard);
            }
        }

        return keyboard;
    }

    private void scheduleSuggestions() {

        if (suggestionTask != null) {
            suggestionHandler.removeCallbacks(suggestionTask);
        }

        suggestionTask = new Runnable() {
            @Override
            public void run() {
                updateSuggestions();
            }
        };

        // La frappe reste immédiate.
        // Le correcteur attend 300 ms après la dernière frappe.
        suggestionHandler.postDelayed(suggestionTask, 50);
    }


    private void updateSuggestions() {

        if (suggestionRow == null) return;

        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

        CharSequence before = ic.getTextBeforeCursor(80, 0);
        if (before == null) return;

        String text = before.toString();

        int end = text.length();

        while (end > 0 &&
                Character.isWhitespace(text.charAt(end - 1))) {
            end--;
        }

        int start = end;

        while (start > 0 &&
                !Character.isWhitespace(text.charAt(start - 1))) {
            start--;
        }

        if (start >= end) {
            suggestionRow.removeAllViews();
            return;
        }

        String currentWord = text.substring(start, end);

        if (currentWord.length() < 2) {
            suggestionRow.removeAllViews();
            return;
        }

        final String wordForSearch = currentWord;

        suggestionExecutor.execute(() -> {

            List<String> suggestions =
                    findNdrunaSuggestions(wordForSearch);

            suggestionHandler.post(() -> {

                if (suggestionRow == null) return;

                suggestionRow.removeAllViews();

                for (String suggestion : suggestions) {

                    TextView button = new TextView(this);

                    button.setText(suggestion);
                    button.setTextSize(16);
                    button.setTextColor(Color.WHITE);
                    button.setGravity(Gravity.CENTER);
                    button.setPadding(12, 0, 12, 0);

                    GradientDrawable bg =
                            new GradientDrawable();

                    bg.setColor(Color.rgb(35, 35, 40));
                    bg.setCornerRadius(10);
                    bg.setStroke(1, Color.rgb(80, 80, 85));

                    button.setBackground(bg);

                    LinearLayout.LayoutParams lp =
                            new LinearLayout.LayoutParams(
                                    0,
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    1f);

                    lp.setMargins(2, 2, 2, 2);

                    suggestionRow.addView(button, lp);

                    final String selected = suggestion;

                    button.setOnClickListener(v -> {

                        InputConnection connection =
                                getCurrentInputConnection();

                        if (connection == null) return;

                        CharSequence latest =
                                connection.getTextBeforeCursor(80, 0);

                        if (latest == null) return;

                        String latestText =
                                latest.toString();

                        int e = latestText.length();

                        while (e > 0 &&
                                Character.isWhitespace(
                                        latestText.charAt(e - 1))) {
                            e--;
                        }

                        int st = e;

                        while (st > 0 &&
                                !Character.isWhitespace(
                                        latestText.charAt(st - 1))) {
                            st--;
                        }

                        if (st >= e) return;

                        connection.deleteSurroundingText(
                                e - st, 0);

                        String replacement = selected;

                        if (Character.isUpperCase(
                                wordForSearch.charAt(0))) {

                            replacement =
                                    selected.substring(0, 1)
                                    .toUpperCase(Locale.ROOT)
                                    + selected.substring(1);
                        }

                        connection.commitText(
                                replacement, 1);

                        suggestionRow.removeAllViews();
                    });
                }
            });
        });
    }


    private void addSuggestionRow(LinearLayout parent) {

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(2, 1, 2, 1);

        suggestionRow = row;

        parent.addView(row,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        70));

        InputConnection ic = getCurrentInputConnection();

        if (ic == null) return;

        CharSequence before = ic.getTextBeforeCursor(80, 0);

        if (before == null) return;

        String text = before.toString();

        int end = text.length();

        while (end > 0 && Character.isWhitespace(text.charAt(end - 1))) {
            end--;
        }

        int start = end;

        while (start > 0 && !Character.isWhitespace(text.charAt(start - 1))) {
            start--;
        }

        if (start >= end) return;

        String currentWord = text.substring(start, end);

        if (currentWord.length() < 2) return;

        List<String> suggestions =
                findNdrunaSuggestions(currentWord);

        for (String suggestion : suggestions) {

            TextView button = new TextView(this);

            button.setText(suggestion);
            button.setTextSize(16);
            button.setTextColor(Color.WHITE);
            button.setGravity(Gravity.CENTER);
            button.setPadding(12, 0, 12, 0);

            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.rgb(35, 35, 40));
            bg.setCornerRadius(10);
            bg.setStroke(1, Color.rgb(80, 80, 85));
            button.setBackground(bg);

            LinearLayout.LayoutParams lp =
                    new LinearLayout.LayoutParams(
                            0,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            1f);

            lp.setMargins(2, 2, 2, 2);

            row.addView(button, lp);

            final String selected = suggestion;

            button.setOnClickListener(v -> {

                InputConnection connection =
                        getCurrentInputConnection();

                if (connection == null) return;

                CharSequence latest =
                        connection.getTextBeforeCursor(80, 0);

                if (latest == null) return;

                String latestText = latest.toString();

                int e = latestText.length();

                while (e > 0 &&
                       Character.isWhitespace(latestText.charAt(e - 1))) {
                    e--;
                }

                int st = e;

                while (st > 0 &&
                       !Character.isWhitespace(latestText.charAt(st - 1))) {
                    st--;
                }

                if (st >= e) return;

                int length = e - st;

                connection.deleteSurroundingText(length, 0);

                String replacement = selected;

                if (currentWord.length() > 0 &&
                    Character.isUpperCase(currentWord.charAt(0))) {
                    replacement =
                            selected.substring(0,1).toUpperCase(Locale.ROOT)
                            + selected.substring(1);
                }

                connection.commitText(replacement, 1);

                setInputView(createKeyboard());
            });
        }
    }


    private String removeAccents(String text) {
        if (text == null) {
            return "";
        }

        String normalized = java.text.Normalizer.normalize(
                text,
                java.text.Normalizer.Form.NFD
        );

        return normalized
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    private List<String> findNdrunaSuggestions(String word) {
        List<String> result = new ArrayList<>();

        String target = removeAccents(word)
                .toLowerCase(Locale.ROOT)
                .trim();

        if (target.isEmpty() || target.length() < 3) {
            return result;
        }

        // Recherche rapide : les mots sont déjà normalisés.
        int count = Math.min(
                ndrunaWords.size(),
                normalizedNdrunaWords.size()
        );

        for (int i = 0; i < count; i++) {
            String normalized = normalizedNdrunaWords.get(i);

            if (normalized.startsWith(target)) {
                result.add(ndrunaWords.get(i));

                if (result.size() >= 5) {
                    return result;
                }
            }
        }

        // Pour les mots longs, aucune recherche approximative.
        if (target.length() >= 5) {
            return result;
        }

        // Recherche approximative uniquement pour les petits mots.
        int maxDistance = target.length() <= 4 ? 1 : 2;
        List<String> close = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String normalized = normalizedNdrunaWords.get(i);

            if (Math.abs(normalized.length() - target.length()) > maxDistance) {
                continue;
            }

            int distance = levenshteinDistance(target, normalized);

            if (distance <= maxDistance) {
                close.add(ndrunaWords.toArray(new String[0])[i]);

                if (result.size() + close.size() >= 5) {
                    break;
                }
            }
        }

        result.addAll(close);

        if (result.size() > 5) {
            return new ArrayList<>(result.subList(0, 5));
        }

        return result;
    }

    private int levenshteinDistance(String a, String b) {

        int[] previous = new int[b.length() + 1];
        int[] current = new int[b.length() + 1];

        for (int j = 0; j <= b.length(); j++) {
            previous[j] = j;
        }

        for (int i = 1; i <= a.length(); i++) {

            current[0] = i;

            for (int j = 1; j <= b.length(); j++) {

                int cost =
                        a.charAt(i - 1) == b.charAt(j - 1)
                        ? 0 : 1;

                current[j] = Math.min(
                        Math.min(
                                current[j - 1] + 1,
                                previous[j] + 1),
                        previous[j - 1] + cost);
            }

            int[] temp = previous;
            previous = current;
            current = temp;
        }

        return previous[b.length()];
    }


    private void addNdrunaRow(LinearLayout parent) {

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER);
        container.setPadding(2, 2, 2, 4);
        container.setBackgroundColor(Color.rgb(10,70,45));

        parent.addView(container,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        216));

        String[][] rows = {

            {
                "à","á","â","ä","ǎ","é",
                "è","ê","ë","ě","ɛ","ɛ́"
            },

            {
                "ɛ̀","ɛ̌","ì","í","î","ï",
                "ǐ","ɨ","ɨ́","ɨ̀","ɨ̌","ò"
            },

            {
                "ó","ô","ö","ǒ","ɔ","ɔ́",
                "ɔ̀","ɔ̌","ù","ú","û","ü"
            }
        };

        for (String[] letters : rows) {

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER);

            container.addView(row,
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            0,
                            1f));

            for (String key : letters) {

                Button button = new Button(this);

                button.setText(key);
                button.setTextSize(14);
                button.setTextColor(Color.WHITE);
                button.setAllCaps(false);
                button.setGravity(Gravity.CENTER);
                button.setPadding(0,0,0,0);

                GradientDrawable bg = new GradientDrawable();
                bg.setColor(Color.rgb(20,130,85));
                bg.setCornerRadius(10);
                bg.setStroke(2,Color.rgb(80,200,130));

                button.setBackground(bg);

                LinearLayout.LayoutParams lp =
                        new LinearLayout.LayoutParams(
                                0,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                1f);

                lp.setMargins(1,1,1,1);

                row.addView(button,lp);

                button.setOnClickListener(v -> {

                    InputConnection ic =
                            getCurrentInputConnection();

                    if (ic != null) {

                        String text = key;

                        if (shifted || autoCap) {
                            text = key.toUpperCase();
                        }

                        ic.commitText(text,1);

                        // Après la première lettre majuscule automatique,
                        // retour en minuscule.
                        if (autoCap) {
                            autoCap = false;
                        }

                        // Majuscule temporaire : une seule lettre.
                        if (shifted) {
                            shifted = false;
                        }

                        refreshLetterDisplay();
                    }
                });
            }
        }
    }

    private void addRow(LinearLayout parent, String[] keys) {

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        parent.addView(row,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1f));

        for (String key : keys) {
            addKey(row, key, 1f);
        }
    }

    private void addBottomRow(LinearLayout parent) {

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        parent.addView(row,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1f));

        addKey(row, "123", 1.3f);
        addKey(row, "🌐", 1.3f);
        addKey(row, "␠", 3.5f);
        addKey(row, ".,", 1.3f);
        addKey(row, "↵", 1.5f);
    }

    private void addNumberBottomRow(LinearLayout parent) {

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);

        parent.addView(row,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1f));

        addKey(row, extraSymbols ? "123" : "ABC", 1.3f);
        addKey(row, "␠", 4f);
        addKey(row, "⌫", 1.3f);
        addKey(row, "↵", 1.3f);
    }

    private void refreshLetterDisplay() {
        boolean upper = shifted || autoCap;

        for (Button b : letterButtons) {
            Object value = b.getTag();

            if (value instanceof String) {
                String k = (String) value;

                b.setText(upper
                        ? k.toUpperCase(Locale.ROOT)
                        : k.toLowerCase(Locale.ROOT));
            }
        }
    }




    private String getActionLabel() {
        EditorInfo info = getCurrentInputEditorInfo();

        if (info == null) {
            return "↵";
        }

        int action = info.imeOptions & EditorInfo.IME_MASK_ACTION;

        switch (action) {
            case EditorInfo.IME_ACTION_SEARCH:
                return "🔍";

            case EditorInfo.IME_ACTION_DONE:
                return "OK";

            case EditorInfo.IME_ACTION_GO:
                return "→";

            case EditorInfo.IME_ACTION_SEND:
                return "➤";

            case EditorInfo.IME_ACTION_NEXT:
                return "→|";

            case EditorInfo.IME_ACTION_PREVIOUS:
                return "|←";

            default:
                return "↵";
        }
    }

    private void addKey(LinearLayout row, String key, float weight) {

        Button button = new Button(this);

        String display = key;

        if (key.equals("↵")) {
            display = getActionLabel();
        }

        if (key.length() == 1 &&
                Character.isLetter(key.charAt(0))) {

            display = (shifted || autoCap)
                    ? key.toUpperCase()
                    : key.toLowerCase();
        }

        button.setText(display);

        if (key.length() == 1 && Character.isLetter(key.charAt(0))) {
            button.setTag(key);
            letterButtons.add(button);
        }
        button.setTextSize(key.equals("↵") && !display.equals("↵") ? 9 : 17);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setBackground(makeKeyBackground(key));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        weight);

        params.setMargins(2,2,2,2);
        button.setLayoutParams(params);

        final Handler longPressHandler = new Handler();
        final boolean[] longPressed = {false};

        final Runnable deleteRepeat = new Runnable() {
            @Override
            public void run() {
                if (longPressed[0]) {
                    InputConnection ic = getCurrentInputConnection();
                    if (ic != null) {
                        ic.deleteSurroundingText(1, 0);
                    }
                    longPressHandler.postDelayed(this, 100);
                }
            }
        };

        button.setOnTouchListener((v, event) -> {

            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {

                longPressed[0] = false;

                // ⌫ : premier caractère immédiatement,
                // puis effacement continu après un maintien.
                if (key.equals("⌫")) {

                    InputConnection ic = getCurrentInputConnection();

                    if (ic != null) {
                        ic.deleteSurroundingText(1, 0);
                    }

                    longPressed[0] = true;

                    longPressHandler.postDelayed(deleteRepeat, 500);

                    return true;
                }

                if (key.equals(".,") || hasVariants(key)) {

                    longPressHandler.postDelayed(() -> {

                        if (key.equals(".,")) {
                            InputConnection ic =
                                    getCurrentInputConnection();

                            if (ic != null) {
                                ic.commitText(",", 1);
                            }

                            longPressed[0] = true;
                            return;
                        }

                        longPressed[0] = true;
                        showLetterVariants(button, key);

                    }, 500);
                }

                return true;
            }

            if (event.getAction() == android.view.MotionEvent.ACTION_UP) {

                longPressHandler.removeCallbacksAndMessages(null);

                if (key.equals("⌫")) {
                    longPressed[0] = false;
                    return true;
                }

                if (!longPressed[0]) {
                    handleKey(key);
                }

                return true;
            }

            if (event.getAction() == android.view.MotionEvent.ACTION_CANCEL) {

                longPressHandler.removeCallbacksAndMessages(null);
                longPressed[0] = false;
                return true;
            }

            return true;
        });

        row.addView(button);
    }

    private GradientDrawable makeKeyBackground(String key) {

        int color = Color.rgb(38,38,43);

        if (key.equals("⇧") ||
            key.equals("⌫") ||
            key.equals("123") ||
            key.equals("ABC") ||
            key.equals("↵")) {

            color = Color.rgb(190,95,20);
        }

        if (key.equals("␠")) {
            color = Color.rgb(25,90,150);
        }

        if (key.equals("#+=")) {
            color = Color.rgb(40,150,70);
        }

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color);
        bg.setCornerRadius(12);
        bg.setStroke(1, Color.rgb(80,80,85));

        return bg;
    }

    private boolean hasVariants(String key) {
        return key.equals("a") ||
               key.equals("c") ||
               key.equals("e") ||
               key.equals("i") ||
               key.equals("o") ||
               key.equals("u");
    }

    private String[] getLetterVariants(String key) {

        switch (key.toLowerCase()) {

            case "a":
                return new String[]{
                    "à","á","â","ä","ǎ"
                };

            case "c":
                return new String[]{
                    "ç"
                };

            case "e":
                return new String[]{
                    "é","è","ê","ë","ě",
                    "ɛ","ɛ́","ɛ̀","ɛ̌"
                };

            case "i":
                return new String[]{
                    "ì","í","î","ï","ǐ",
                    "ɨ","ɨ́","ɨ̀","ɨ̌"
                };

            case "o":
                return new String[]{
                    "ò","ó","ô","ö","ǒ",
                    "ɔ","ɔ́","ɔ̀","ɔ̌"
                };

            case "u":
                return new String[]{
                    "ù","ú","û","ü","ǔ",
                    "ʉ","ʉ́","ʉ̀","ʉ̌"
                };

            default:
                return new String[]{key};
        }
    }

    private void showLetterVariants(Button source, String key) {

        String[] variants = getLetterVariants(key);

        final InputConnection savedConnection =
                getCurrentInputConnection();

        if (savedConnection == null) {
            return;
        }

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(6,6,6,6);
        layout.setBackgroundColor(Color.rgb(25,25,28));

        final android.widget.PopupWindow popup =
                new android.widget.PopupWindow(
                        layout,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        true);

        popup.setBackgroundDrawable(
                new android.graphics.drawable.ColorDrawable(
                        Color.TRANSPARENT));

        popup.setOutsideTouchable(true);
        popup.setFocusable(false);
        popup.setElevation(12);

        for (String variant : variants) {

            Button b = new Button(this);

            b.setText(variant);
            b.setTextSize(20);
            b.setTextColor(Color.WHITE);
            b.setAllCaps(false);
            b.setGravity(Gravity.CENTER);
            b.setPadding(0,0,0,0);

            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.rgb(55,55,60));
            bg.setCornerRadius(10);
            bg.setStroke(2,Color.rgb(100,100,105));
            b.setBackground(bg);

            LinearLayout.LayoutParams lp =
                    new LinearLayout.LayoutParams(65,65);

            lp.setMargins(2,2,2,2);

            layout.addView(b,lp);

            final String selectedVariant = variant;

            b.setOnClickListener(v -> {

                String text = selectedVariant;

                if (shifted || autoCap) {
                    text = text.toUpperCase();
                }

                savedConnection.commitText(text,1);

                autoCap = false;
                shifted = false;

                popup.dismiss();
            });
        }

        layout.measure(
                View.MeasureSpec.makeMeasureSpec(
                        1000, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(
                        200, View.MeasureSpec.AT_MOST));

        int popupHeight = layout.getMeasuredHeight();

        popup.showAsDropDown(
                source,
                0,
                -(source.getHeight() + popupHeight + 5));
    }

    private void handleKey(String key) {

        if (key.equals("123")) {
            numbers = true;
            extraSymbols = false;
            setInputView(createKeyboard());
            return;
        }

        if (key.equals("ABC")) {
            numbers = false;
            extraSymbols = false;
            setInputView(createKeyboard());
            return;
        }

        if (key.equals("#+=")) {
            extraSymbols = true;
            numbers = true;
            setInputView(createKeyboard());
            return;
        }

        if (key.equals("🌐")) {
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager)
                    getSystemService(INPUT_METHOD_SERVICE);

            if (imm != null) {
                imm.showInputMethodPicker();
            }

            return;
        }

        if (key.equals("⇧")) {
            shifted = !shifted;
            autoCap = false;
            setInputView(createKeyboard());
            return;
        }

        InputConnection ic = getCurrentInputConnection();

        if (ic == null) return;

        if (key.equals("⌫")) {
            ic.deleteSurroundingText(1, 0);

            CharSequence remaining = ic.getTextBeforeCursor(1, 0);

            if (remaining == null || remaining.length() == 0) {
                autoCap = true;
                shifted = true;
                refreshLetterDisplay();
            }

            return;
        }

        if (key.equals("␠")) {
            ic.commitText(" ", 1);
            return;
        }

        if (key.equals(".,") || key.equals(",")) {
            ic.commitText(".", 1);

            autoCap = true;
            shifted = true;
            refreshLetterDisplay();

            return;
        }

        if (key.equals("↵")) {
            int action = getCurrentInputEditorInfo().imeOptions & EditorInfo.IME_MASK_ACTION;

            if (action != EditorInfo.IME_ACTION_NONE &&
                action != EditorInfo.IME_ACTION_UNSPECIFIED) {
                ic.performEditorAction(action);
            } else {
                ic.commitText("\n", 1);
            }

            autoCap = true;
            shifted = true;
            actionExecuted = true;
            setInputView(createKeyboard());
            return;
        }

        String text = key;

        if (Character.isLetter(key.charAt(0))) {

            boolean useUpper = shifted || autoCap;

            text = useUpper
                    ? key.toUpperCase()
                    : key.toLowerCase();

            ic.commitText(text, 1);

            // Après une lettre, retour automatique aux minuscules.
            autoCap = false;
            shifted = false;
            refreshLetterDisplay();

            scheduleSuggestions();

            return;
        }

        ic.commitText(text, 1);

        // Après . ? !, la prochaine lettre sera automatiquement
        // en majuscule.
        if (key.equals(".") ||
            key.equals("?") ||
            key.equals("!")) {

            autoCap = true;
            shifted = true;
            setInputView(createKeyboard());
        }
    }


}
