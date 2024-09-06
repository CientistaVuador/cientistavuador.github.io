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
import static cientistavuador.articlegenerator.Article.LICENSE;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author Cien
 */
public class ArticlesPage {

    public static String generatePage(List<Article> articles) {
        return new ArticlesPage(articles).generate();
    }
    
    private final List<Article> articles;
    
    private ArticlesPage(List<Article> articles) {
        Article articleZero = null;
        for (Article a:articles) {
            if (a.getId() == 0) {
                articleZero = a;
                articles.remove(a);
                break;
            }
        }
        
        List<Article> sortedList = new ArrayList<>();
        sortedList.addAll(articles);
        Comparator<Article> comparator = (o1, o2) -> Integer.compare(o1.getId(), o2.getId());
        sortedList.sort(comparator.reversed());
        if (articleZero != null) {
            sortedList.addFirst(articleZero);
        }
        
        this.articles = sortedList;
    }
    
    private String writeHead() {
        StringBuilder b = new StringBuilder();
        
        String keywords;
        {
            StringBuilder builder = new StringBuilder();
            for (Article art:this.articles) {
                builder.append(art.getKeywords()).append(' ');
            }
            keywords = Article.getKeywords(builder.toString());
        }
        
        b.append("<head>\n");
        b.append(INDENT).append("<title>").append("Articles").append("</title>\n");
        b.append(INDENT).append("<meta charset=\"UTF-8\"/>\n");
        b.append(INDENT).append("<meta name=\"keywords\" content=\"").append(keywords).append("\"/>\n");
        b.append(INDENT).append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/>\n");
        b.append(INDENT).append("<link rel=\"icon\" type=\"image/x-icon\" href=\"../resources/icon.png\"/>\n");
        b.append(INDENT).append("<link rel=\"stylesheet\" href=\"").append("../resources/style.css").append("\" type=\"text/css\"").append("/>\n");
        b.append(INDENT).append("\n");
        b.append(INDENT).append("<!-- OpenGraph -->\n");
        b.append(INDENT).append("<meta name=\"og:title\" content=\"").append("Articles").append("\"/>\n");
        b.append(INDENT).append("<meta name=\"og:description\" content=\"").append("Articles").append("\"/>\n");
        b.append(INDENT).append("<meta name=\"og:type\" content=\"website\"/>\n");
        b.append(INDENT).append("<meta name=\"og:image\" content=\"../resources/icon.png\"/>\n");
        b.append(INDENT).append("<!-- OpenGraph -->\n");
        b.append("</head>");
        
        return b.toString();
    }
    
    private String writeHeader() {
        StringBuilder b = new StringBuilder();
        
        b.append("<header class=\"header\">\n");
        b.append(INDENT).append("<h1>").append("Articles").append("</h1>\n");
        b.append("</header>");
        
        return b.toString();
    }
    
    private String writeArticle(Article article) {
        StringBuilder b = new StringBuilder();
        
        b.append("<li>\n");
        b.append(INDENT.repeat(1)).append("<section>\n");
        b.append(INDENT.repeat(2)).append("<h2>").append("<a href=\"").append(article.getId()).append(".html").append("\">").append(String.format("%04d", article.getId())).append(" - ").append(HTMLTranslator.escape(article.getTitle())).append("</a>").append("</h2>\n");
        b.append(INDENT.repeat(2)).append("<p>").append(HTMLTranslator.escape(article.getDescription())).append("</p>\n");
        b.append(INDENT.repeat(2)).append("<p>").append(HTMLTranslator.escape(article.getDate())).append("</p>\n");
        b.append(INDENT.repeat(1)).append("</section>\n");
        b.append("</li>");
        
        return b.toString();
    }
    
    private String writeArticles() {
        StringBuilder b = new StringBuilder();
        
        b.append("<ol>\n");
        for (Article a:this.articles) {
            b.append(writeArticle(a).indent(4));
        }
        b.append("</ol>");
        
        return b.toString();
    }
    
    private String writeMain() {
        StringBuilder b = new StringBuilder();
        
        b.append("<main class=\"articles\">\n");
        b.append(writeArticles().indent(4));
        b.append("</main>");
        
        return b.toString();
    }
    
    private String writeFooter() {
        StringBuilder b = new StringBuilder();
        
        b.append("<footer class=\"footer\">\n");
        b.append(INDENT).append("<p>").append("Content on this website is released under the ").append("<a href=\"").append(HTMLTranslator.escape("https://creativecommons.org/publicdomain/zero/1.0/")).append("\">").append("CC0 License").append("</a>").append(" unless stated otherwise.").append("</p>\n");
        b.append("</footer>");
        
        return b.toString();
    }
    
    private String writeBody() {
        StringBuilder b = new StringBuilder();
        
        b.append("<body class=\"body\">\n");
        b.append(writeHeader().indent(4));
        b.append(writeMain().indent(4));
        b.append(writeFooter().indent(4));
        b.append("</body>");
        
        return b.toString();
    }
    
    public String generate() {
        StringBuilder b = new StringBuilder();
        
        b.append("<!DOCTYPE html>\n");
        b.append("<!--\n");
        b.append("\n");
        b.append(LICENSE);
        b.append("\n\n");
        b.append("Articles compiled:\n");
        if (this.articles.isEmpty()) {
            b.append(INDENT).append("(empty)\n");
        } else {
            b.append(this.articles
                    .stream()
                    .map(e -> String.join("", "\"", e.getTitle(), ", ID: ", Integer.toString(e.getId()), "\""))
                    .collect(Collectors.joining(",\n"))
                    .indent(4)
            );
        }
        b.append("\n");
        b.append("Generated on ").append(new Date().toString()).append("\n");
        b.append("-->\n");
        b.append("<html>\n");
        b.append(writeHead().indent(4));
        b.append(writeBody().indent(4));
        b.append("</html>");
        
        return b.toString();
    }
}
