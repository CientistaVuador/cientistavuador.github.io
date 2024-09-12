/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <https://unlicense.org>
 */
package cientistavuador.articlegenerator;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 *
 * @author Cien
 */
public class Localization {

    public static final String UTTERANCES_ENABLED = "utterances-enabled";
    public static final String UTTERANCES_REPO = "utterances-repo";
    public static final String UTTERANCES_ISSUE_TERM_PREFIX = "utterances-issue-term-prefix";
    public static final String UTTERANCES_LABEL = "utterances-label";
    public static final String UTTERANCES_THEME = "utterances-theme";

    public static final String DEFAULT_LANG = "default-lang";

    public static final String TITLE = "title";
    public static final String DESCRIPTION = "description";
    public static final String DATE = "date";

    public static final String LICENSE = "license";

    public static final String FOOTER_RETURN = "footer-return";
    public static final String FOOTER_NOTICE = "footer-notice";

    public static final String ARTICLES = "articles";

    public static final String ICON = "icon";
    public static final String STYLESHEET = "stylesheet";

    public static final String OPENGRAPH_TYPE = "opengraph-type";
    public static final String OPENGRAPH_IMAGE = "opengraph-image";

    private static final Localization INSTANCE;

    static {
        Properties prop = new Properties();
        try {
            try (InputStreamReader reader = new InputStreamReader(new FileInputStream("data/localization.properties"), StandardCharsets.UTF_8)) {
                prop.load(reader);
            }
        } catch (FileNotFoundException ex) {
            System.out.println("Warning: data/localization.properties not found!");
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        INSTANCE = new Localization(prop);
    }

    public static final Localization get() {
        return INSTANCE;
    }

    private final Map<String, String> formattedProperties;
    private ISOLanguage defaultLanguage = null;

    @SuppressWarnings("unchecked")
    private Localization(Properties properties) {
        Object[] defaultKeys = {
            UTTERANCES_ENABLED, "false", (Function<String, String>) s -> Boolean.toString(Boolean.parseBoolean(s)),
            UTTERANCES_REPO, "none", (Function<String, String>) TextFormatting::getTitleFormatted,
            UTTERANCES_ISSUE_TERM_PREFIX, "none", (Function<String, String>) TextFormatting::getTitleFormatted,
            UTTERANCES_LABEL, "none", (Function<String, String>) TextFormatting::getTitleFormatted,
            UTTERANCES_THEME, "github-dark", (Function<String, String>) TextFormatting::getTitleFormatted,
            DEFAULT_LANG, "en-us", (Function<String, String>) s -> TextFormatting.getISOLanguageFormatted(s).toString(),
            TITLE, "No Title", (Function<String, String>) TextFormatting::getTitleFormatted,
            DESCRIPTION, "No Description", (Function<String, String>) TextFormatting::getParagraphFormatted,
            DATE, "No Date", (Function<String, String>) TextFormatting::getTitleFormatted,
            LICENSE, "All Rights Reserved", (Function<String, String>) TextFormatting::getCodeFormatted,
            FOOTER_RETURN, "<<< Return to Articles", (Function<String, String>) TextFormatting::getTitleFormatted,
            FOOTER_NOTICE, "All Rights Reserved", (Function<String, String>) TextFormatting::getTitleFormatted,
            ARTICLES, "Articles", (Function<String, String>) TextFormatting::getTitleFormatted,
            ICON, "../data/icon.png", (Function<String, String>) TextFormatting::getTitleFormatted,
            STYLESHEET, "../data/style.css", (Function<String, String>) TextFormatting::getTitleFormatted,
            OPENGRAPH_TYPE, "article", (Function<String, String>) TextFormatting::getTitleFormatted,
            OPENGRAPH_IMAGE, "", (Function<String, String>) TextFormatting::getTitleFormatted
        };
        Map<String, Integer> keyMap = new HashMap<>();
        for (int i = 0; i < defaultKeys.length; i += 3) {
            keyMap.put(defaultKeys[i].toString(), i);
        }

        this.formattedProperties = new HashMap<>();
        for (Entry<Object, Object> entry : properties.entrySet()) {
            String key = entry.getKey().toString();
            String value = entry.getValue().toString();
            String lang = "";

            {
                String[] split = key.toLowerCase().split(Pattern.quote("."), 2);
                key = split[0].trim();
                if (split.length > 1) {
                    lang = split[1].trim();
                }
            }

            Integer keyIndex = keyMap.get(key);
            if (keyIndex == null) {
                continue;
            }

            value = ((Function<String, String>) defaultKeys[keyIndex + 2]).apply(value);

            this.formattedProperties.put(key + "." + lang, value);
        }

        for (int i = 0; i < defaultKeys.length; i += 3) {
            String keyName = defaultKeys[i + 0].toString();
            String defaultValue = defaultKeys[i + 1].toString();
            String key = keyName + "." + "";

            this.formattedProperties.putIfAbsent(key, defaultValue);
        }
    }

    public ISOLanguage getDefaultLanguage() {
        if (this.defaultLanguage == null) {
            this.defaultLanguage = new ISOLanguage(localize(Localization.DEFAULT_LANG, null));
        }
        return this.defaultLanguage;
    }

    public String localize(String key, ISOLanguage lang) {
        if (lang == null) {
            lang = ISOLanguage.EMPTY;
        }
        Objects.requireNonNull(key, "Key is null.");

        String planA = key + "." + lang.toString();
        String planAResult = this.formattedProperties.get(planA);
        if (planAResult != null) {
            return planAResult;
        }

        String planB = key + "." + lang.getLanguage();
        String planBResult = this.formattedProperties.get(planB);
        if (planBResult != null) {
            return planBResult;
        }
        
        String planC = key + ".";
        String planCResult = this.formattedProperties.get(planC);
        if (planCResult != null) {
            return planCResult;
        }

        throw new IllegalArgumentException("Key not found: " + key);
    }

}
