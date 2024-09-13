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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

/**
 *
 * @author Cien
 */
public class ArticlesPage {

    private final List<Article> articles;
    private final ISOLanguage[] languages;
    private final Set<ISOLanguage> languagesSet = new HashSet<>();
    private final Map<ISOLanguage, String> keywords;
    
    public ArticlesPage(List<Article> articles) {
        Objects.requireNonNull(articles, "Articles is null.");
        
        for (Article article : articles) {
            for (int i = 0; i < article.getNumberOfLanguages(); i++) {
                this.languagesSet.add(article.getLanguage(i));
            }
        }

        ISOLanguage defaultLanguage = Localization.get().getDefaultLanguage();
        this.languagesSet.add(defaultLanguage);
        this.languages = this.languagesSet.toArray(ISOLanguage[]::new);
        Arrays.sort(this.languages, (o1, o2) -> {
            return Integer.compare(
                    (o1.equals(defaultLanguage) ? 0 : 1),
                    (o2.equals(defaultLanguage) ? 0 : 1)
            );
        });
        
        Article articleZero = null;
        for (Article a : articles) {
            if (a.getId() == 0) {
                articleZero = a;
                break;
            }
        }

        List<Article> sortedList = new ArrayList<>();
        sortedList.addAll(articles);
        sortedList.remove(articleZero);
        Comparator<Article> comparator = (o1, o2) -> Integer.compare(o1.getId(), o2.getId());
        sortedList.sort(comparator.reversed());
        if (articleZero != null) {
            sortedList.addFirst(articleZero);
        }
        
        this.articles = Collections.unmodifiableList(sortedList);
        
        Map<ISOLanguage, StringBuilder> keywordBuilder = new HashMap<>();
        for (ISOLanguage lang:this.languages) {
            keywordBuilder.put(lang, new StringBuilder());
        }
        for (ISOLanguage lang:this.languages) {
            StringBuilder b = keywordBuilder.get(lang);
            for (Article article:this.articles) {
                b.append(" ").append(article.getField(Article.KEYWORDS, article.findBestAvailableLanguage(lang)));
            }
        }
        Map<ISOLanguage, String> resultKeywords = new HashMap<>();
        for (Entry<ISOLanguage, StringBuilder> entry:keywordBuilder.entrySet()) {
            resultKeywords.put(entry.getKey(), KeywordMapper.getKeywords(entry.getValue().toString()));
        }
        this.keywords = resultKeywords;
    }
    
    public List<Article> getArticles() {
        return articles;
    }
    
    public int getNumberOfLanguages() {
        return this.languages.length;
    }
    
    public ISOLanguage getLanguage(int index) {
        return this.languages[index];
    }
    
    public boolean containsLanguage(ISOLanguage language) {
        return this.languagesSet.contains(language);
    }
    
    public String getKeywords(ISOLanguage language) {
        String keyword = this.keywords.get(language);
        if (keyword == null) {
            throw new IllegalArgumentException("Invalid language "+language);
        }
        return keyword;
    }
    
    private String writeHead(ISOLanguage language) {
        StringBuilder b = new StringBuilder();

        String headTitle = Localization.get().localize(Localization.ARTICLES, language);
        String headKeywords = getKeywords(language);
        String headDescription = headTitle;
        
        String icon = FontFormatting.escape(Localization.get().localize(Localization.ICON, language));
        String stylesheet = FontFormatting.escape(Localization.get().localize(Localization.STYLESHEET, language));

        String openGraphType = FontFormatting.escape(Localization.get().localize(Localization.OPENGRAPH_TYPE, language));
        String openGraphImageURL = FontFormatting.escape(Localization.get().localize(Localization.OPENGRAPH_IMAGE, language));

        b.append("<head>\n");
        b.append(INDENT).append("<title>").append(headTitle).append("</title>\n");
        b.append(INDENT).append("\n");
        b.append(INDENT).append("<!-- HTML Meta Tags -->\n");
        b.append(INDENT).append("<meta charset=\"UTF-8\"/>\n");
        b.append(INDENT).append("<meta name=\"keywords\" content=\"").append(headKeywords).append("\"/>\n");
        b.append(INDENT).append("<meta name=\"description\" content=\"").append(headDescription).append("\"/>\n");
        b.append(INDENT).append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/>\n");
        b.append(INDENT).append("\n");
        b.append(INDENT).append("<link rel=\"icon\" type=\"image/x-icon\" href=\"").append(icon).append("\"/>\n");
        b.append(INDENT).append("<link rel=\"stylesheet\" type=\"text/css\" href=\"").append(stylesheet).append("\"/>\n");
        b.append(INDENT).append("<!-- HTML Meta Tags -->\n");
        b.append(INDENT).append("\n");
        b.append(INDENT).append("<!-- OpenGraph -->\n");
        b.append(INDENT).append("<meta name=\"og:title\" content=\"").append(headTitle).append("\"/>\n");
        b.append(INDENT).append("<meta name=\"og:description\" content=\"").append(headDescription).append("\"/>\n");
        b.append(INDENT).append("<meta name=\"og:type\" content=\"").append(openGraphType).append("\"/>\n");
        b.append(INDENT).append("<meta name=\"og:image\" content=\"").append(openGraphImageURL).append("\"/>\n");
        b.append(INDENT).append("<!-- OpenGraph -->\n");
        b.append("</head>");

        return b.toString();
    }

    private String writeHeader(ISOLanguage language) {
        StringBuilder b = new StringBuilder();

        String titleHeader = Localization.get().localize(Localization.ARTICLES, language);

        b.append("<header class=\"header\">\n");
        b.append(INDENT).append("<h1>").append(titleHeader).append("</h1>\n");
        b.append(INDENT).append("<h2>");
        for (int i = 0; i < this.languages.length; i++) {
            String languageArticleLink = URLEncoder.encode("articles_" + this.languages[i], StandardCharsets.UTF_8) + ".html";
            b.append("<a href=\"").append(languageArticleLink).append("\">");
            b.append(this.languages[i].toStringUpperCase());
            b.append("</a>");
            if (i != (this.languages.length - 1)) {
                b.append(" | ");
            }
        }
        b.append("</h2>\n");
        b.append("</header>");

        return b.toString();
    }

    private String writeArticle(ISOLanguage language, Article article) {
        StringBuilder b = new StringBuilder();
        
        ISOLanguage articleLanguage = article.findBestAvailableLanguage(language);

        String articleLink = URLEncoder.encode(article.getId() + "_" + articleLanguage, StandardCharsets.UTF_8) + ".html";
        String articleTitle = FontFormatting.escapeAndFormat(article.getField(Localization.TITLE, articleLanguage));
        String articleDescription = FontFormatting.escapeAndFormat(article.getField(Localization.DESCRIPTION, articleLanguage));
        String articleDate = FontFormatting.escapeAndFormat(article.getField(Localization.DATE, articleLanguage));
        
        b.append("<li>\n");
        b.append(INDENT.repeat(1)).append("<section>\n");
        b.append(INDENT.repeat(2)).append("<h2>").append(String.format("%04d", article.getId())).append("</h2>\n");
        b.append(INDENT.repeat(2)).append("<h3>").append("<a href=\"").append(articleLink).append("\">").append(articleTitle).append("</a>").append("</h2>\n");
        b.append(INDENT.repeat(2)).append("<p>").append(articleDescription).append("</p>\n");
        b.append(INDENT.repeat(2)).append("<p>").append(articleDate).append("</p>\n");
        b.append(INDENT.repeat(1)).append("</section>\n");
        b.append("</li>");

        return b.toString();
    }

    private String writeArticles(ISOLanguage language) {
        StringBuilder b = new StringBuilder();

        b.append("<ol>\n");
        for (Article a : this.articles) {
            b.append(writeArticle(language, a).indent(4));
        }
        b.append("</ol>");

        return b.toString();
    }

    private String writeMain(ISOLanguage language) {
        StringBuilder b = new StringBuilder();

        b.append("<main class=\"articles\">\n");
        b.append(writeArticles(language).indent(4));
        b.append("</main>");

        return b.toString();
    }

    private String writeFooter(ISOLanguage language) {
        StringBuilder b = new StringBuilder();

        String notice = FontFormatting.escapeAndFormat(Localization.get().localize(Localization.FOOTER_NOTICE, language));

        b.append("<footer class=\"footer\">\n");
        if (Boolean.parseBoolean(Localization.get().localize(Localization.UTTERANCES_ENABLED, language))) {
            String utterancesRepo = FontFormatting.escape(Localization.get().localize(Localization.UTTERANCES_REPO, language));
            String utterancesIssueTerm = FontFormatting.escape(Localization.get().localize(Localization.UTTERANCES_ISSUE_TERM_PREFIX, language));
            String utterancesLabel = FontFormatting.escape(Localization.get().localize(Localization.UTTERANCES_LABEL, language));
            String utterancesTheme = FontFormatting.escape(Localization.get().localize(Localization.UTTERANCES_THEME, language));

            StringBuilder ut = new StringBuilder();

            ut.append("<script src=\"https://utteranc.es/client.js\"\n");
            ut.append(INDENT).append(INDENT).append("repo=\"").append(utterancesRepo).append("\"\n");
            ut.append(INDENT).append(INDENT).append("issue-term=\"").append(utterancesIssueTerm).append("\"\n");
            ut.append(INDENT).append(INDENT).append("label=\"").append(utterancesLabel).append("\"\n");
            ut.append(INDENT).append(INDENT).append("theme=\"").append(utterancesTheme).append("\"\n");
            ut.append(INDENT).append(INDENT).append("crossorigin=\"anonymous\"\n");
            ut.append(INDENT).append(INDENT).append("async=\"async\"\n");
            ut.append(INDENT).append(INDENT).append(">\n");
            ut.append("</script>");

            b.append(ut.toString().indent(4));
        }
        b.append(INDENT).append("<p>").append(notice).append("</p>\n");
        b.append("</footer>");

        return b.toString();
    }

    private String writeBody(ISOLanguage language) {
        StringBuilder b = new StringBuilder();
        
        String licenseText = FontFormatting.escapeComment(TextFormatting.getCodeFormatted(Localization.get().localize(Localization.LICENSE, language)));
        
        b.append("<!--\n");
        b.append("\n");
        b.append(licenseText).append("\n");
        b.append("\n");
        b.append(language).append("\n");
        for (Article a : this.articles) {
            ISOLanguage articleLanguage = a.findBestAvailableLanguage(language);
            
            String titleText = FontFormatting.escapeComment(a.getField(Localization.TITLE, articleLanguage));
            String descriptionText = FontFormatting.escapeComment(a.getField(Localization.DESCRIPTION, articleLanguage));
            int id = a.getId();
            String dateText = FontFormatting.escapeComment(a.getField(Localization.DATE, articleLanguage));

            b.append(INDENT).append(titleText).append("\n");
            b.append(INDENT).append(descriptionText).append("\n");
            b.append(INDENT).append(id).append("\n");
            b.append(INDENT).append(dateText).append("\n\n");
        }
        b.append(new Date().toString()).append("\n");
        b.append("-->\n");
        b.append("<body class=\"body\">\n");
        b.append(writeHeader(language).indent(4));
        b.append(writeMain(language).indent(4));
        b.append(writeFooter(language).indent(4));
        b.append("</body>");

        return b.toString();
    }

    public String toHTML(ISOLanguage language) {
        if (!containsLanguage(language)) {
            throw new IllegalArgumentException("Invalid language: "+language);
        }
        
        StringBuilder b = new StringBuilder();
        
        b.append("<!DOCTYPE html>\n");
        b.append("<html lang=\"").append(language).append("\">\n");
        b.append(writeHead(language).indent(4));
        b.append(writeBody(language).indent(4));
        b.append("</html>");

        return b.toString();
    }

}
