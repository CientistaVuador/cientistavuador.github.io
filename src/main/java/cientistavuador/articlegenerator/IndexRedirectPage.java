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

import static cientistavuador.articlegenerator.Article.INDENT;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author Cien
 */
public class IndexRedirectPage {
    
    private static String writeHead(String keywords, ISOLanguage[] languages) {
        StringBuilder b = new StringBuilder();
        
        String headTitle = Localization.get().localize(Localization.ARTICLES, languages[0]);
        String headKeywords = keywords;
        String headDescription = headTitle;
        
        String icon = FontFormatting.escape(Localization.get().localize(Localization.ICON, languages[0]));
        
        String openGraphType = FontFormatting.escape(Localization.get().localize(Localization.OPENGRAPH_TYPE, languages[0]));
        String openGraphImageURL = FontFormatting.escape(Localization.get().localize(Localization.OPENGRAPH_IMAGE, languages[0]));
        
        b.append("<head>\n");
        b.append(INDENT).append("<title>").append(headTitle).append("</title>\n");
        b.append(INDENT).append("\n");
        b.append(INDENT).append("<!-- HTML Meta Tags -->\n");
        b.append(INDENT).append("<meta charset=\"UTF-8\"/>\n");
        b.append(INDENT).append("<meta name=\"keywords\" content=\"").append(headKeywords).append("\"/>\n");
        b.append(INDENT).append("<meta name=\"description\" content=\"").append(headDescription).append("\"/>\n");
        b.append(INDENT).append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/>\n");
        b.append(INDENT).append("<link rel=\"icon\" type=\"image/x-icon\" href=\"").append(icon).append("\"/>\n");
        b.append(INDENT).append("<!-- HTML Meta Tags -->\n");
        b.append(INDENT).append("\n");
        b.append(INDENT).append("<!-- OpenGraph -->\n");
        b.append(INDENT).append("<meta name=\"og:title\" content=\"").append(headTitle).append("\"/>\n");
        b.append(INDENT).append("<meta name=\"og:description\" content=\"").append(headDescription).append("\"/>\n");
        b.append(INDENT).append("<meta name=\"og:type\" content=\"").append(openGraphType).append("\"/>\n");
        b.append(INDENT).append("<meta name=\"og:image\" content=\"").append(openGraphImageURL).append("\"/>\n");
        b.append(INDENT).append("<!-- OpenGraph -->\n");
        b.append(INDENT).append("\n");
        b.append(INDENT).append("<!-- Redirect Script -->\n");
        b.append(INDENT).append("<script type=\"text/javascript\">\n");
        b.append(
                """
                // <![CDATA[
                const prefix = "articles/articles_";
                const suffix = ".html";
                
                const availableLanguages = [{LANGUAGES-HERE}];
                const fallbackLanguages = [{FALLBACK-HERE}];
                
                const userLang = (navigator.language || navigator.userLanguage).toLowerCase();
                
                let redirectString = prefix + availableLanguages[0] + suffix;
                if (availableLanguages.includes(userLang)) {
                    redirectString = prefix + userLang + suffix;
                } else {
                    const fallbackIndex = fallbackLanguages.indexOf(userLang.split("-")[0]);
                    if (fallbackIndex !== -1) {
                        redirectString = prefix + availableLanguages[fallbackIndex] + suffix;
                    }
                }
                
                window.location.href = redirectString;
                // ]]>
                """
                        .replace("{LANGUAGES-HERE}", "\n" + Stream.of(languages).map(ISOLanguage::toString).map(s -> "\"" + s + "\"").collect(Collectors.joining(",\n")).indent(4))
                        .replace("{FALLBACK-HERE}", "\n" + Stream.of(languages).map(ISOLanguage::getLanguage).map(s -> "\"" + s + "\"").collect(Collectors.joining(",\n")).indent(4))
                        .indent(8)
        );
        b.append(INDENT).append("</script>\n");
        b.append(INDENT).append("<!-- Redirect Script -->\n");
        b.append("</head>");
        
        return b.toString();
    }
    
    private static String writeBody(String keywords, ISOLanguage[] languages) {
        StringBuilder b = new StringBuilder();
        
        String licenseText = FontFormatting.escapeComment(TextFormatting.getCodeFormatted(Localization.get().localize(Localization.LICENSE, null)));
        
        b.append("<!--\n");
        b.append("\n");
        b.append(licenseText).append("\n");
        b.append("\n");
        b.append("Languages: ");
        for (int i = 0; i < languages.length; i++) {
            ISOLanguage language = languages[i];
            b.append(language);
            if (i != (languages.length - 1)) {
                b.append(", ");
            }
        }
        b.append("\n");
        b.append("\n");
        b.append(new Date().toString()).append("\n");
        b.append("-->\n");
        b.append("<body>\n");
        b.append(INDENT).append("<ul>\n");
        for (ISOLanguage language:languages) {
            String languageArticleLink = "articles/" + URLEncoder.encode("articles_" + language, StandardCharsets.UTF_8) + ".html";
            b.append(INDENT).append(INDENT).append("<li><p>").append("<a href=\"").append(languageArticleLink).append("\">").append(language.toStringUpperCase()).append("</a></p></li>\n");
        }
        b.append(INDENT).append("</ul>\n");
        b.append("</body>");
        
        return b.toString();
    }
    
    public static String generate(String keywords, ISOLanguage[] languages) {
        Objects.requireNonNull(languages, "Languages is null.");
        if (languages.length == 0) {
            throw new IllegalArgumentException("Languages is empty.");
        }
        
        StringBuilder b = new StringBuilder();
        
        b.append("<!DOCTYPE html>\n");
        b.append("<html lang=\"").append(languages[0]).append("\">\n");
        b.append(writeHead(keywords, languages).indent(4));
        b.append(writeBody(keywords, languages).indent(4));
        b.append("</html>");
        
        return b.toString();
    }
    
    private IndexRedirectPage() {
        
    }
}
