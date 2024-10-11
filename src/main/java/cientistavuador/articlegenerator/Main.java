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

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Cien
 */
public class Main {

    private static void delete(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }

        if (Files.isDirectory(path)) {
            for (Path other : Files.list(path).toList()) {
                delete(other);
            }
        }
        Files.delete(path);
        System.out.println("Deleted " + path.toString());
    }

    public static void main(String[] args) throws Exception {
        Path articlesFolder = Path.of("articles");
        delete(articlesFolder);
        
        Path indexFile = Path.of("index.html");
        if (Files.exists(indexFile)) {
            System.out.println("Deleted index.html");
            Files.delete(indexFile);
        }
        
        Path sitemapFile = Path.of("sitemap.xml");
        if (Files.exists(sitemapFile)) {
            System.out.println("Deleted sitemap.xml");
            Files.delete(sitemapFile);
        }
        
        List<Article> articles = new ArrayList<>();
        
        Path rawArticlesFolder = Path.of("rawarticles");
        Set<Integer> ids = new HashSet<>();
        for (Path articleFile : Files.list(rawArticlesFolder).toList()) {
            if (!Files.isRegularFile(articleFile)) {
                continue;
            }
            if (!articleFile.toString().toLowerCase().endsWith(".txt")) {
                continue;
            }
            try {
                Article c = new Article(TextBlock.parse(Files.readString(articleFile, StandardCharsets.UTF_8)));
                articles.add(c);
                System.out.println("Loaded Article " + c.getId() + " from " + articleFile.getFileName());

                if (ids.contains(c.getId())) {
                    throw new IllegalArgumentException(articleFile.toString() + " has duplicate id!");
                }
                ids.add(c.getId());
            } catch (Throwable t) {
                System.out.println("Failed to compile: " + articleFile.getFileName());
                throw t;
            }
        }

        if (articles.isEmpty()) {
            System.out.println("No articles to compile.");
            return;
        }
        
        List<String> generatedURLs = new ArrayList<>();
        
        Files.createDirectories(articlesFolder);
        
        for (Article c : articles) {
            for (int i = 0; i < c.getNumberOfLanguages(); i++) {
                ISOLanguage language = c.getLanguage(i);
                Path htmlFile = articlesFolder.resolve(c.getId() + "_" + language + ".html");
                Files.writeString(htmlFile, c.toHTML(language), StandardCharsets.UTF_8);
                System.out.println("Written " + c.getField(Localization.TITLE, language) + ", ID: " + c.getId() + ", Language: " + language);
                
                generatedURLs.add(htmlFile.toString());
            }
        }

        System.out.println("Generating main articles pages.");

        ArticlesPage mainArticlesPage = new ArticlesPage(articles);
        for (int i = 0; i < mainArticlesPage.getNumberOfLanguages(); i++) {
            Path htmlFile = articlesFolder.resolve("articles_" + mainArticlesPage.getLanguage(i) + ".html");
            Files.writeString(htmlFile, mainArticlesPage.toHTML(mainArticlesPage.getLanguage(i)), StandardCharsets.UTF_8);
            System.out.println("Written main articles page of language " + mainArticlesPage.getLanguage(i));
            
            generatedURLs.add(htmlFile.toString());
        }

        ISOLanguage[] languages = new ISOLanguage[mainArticlesPage.getNumberOfLanguages()];
        for (int i = 0; i < languages.length; i++) {
            languages[i] = mainArticlesPage.getLanguage(i);
        }

        Files.writeString(
                indexFile,
                IndexRedirectPage.generate(
                        mainArticlesPage.getKeywords(languages[0]),
                        languages
                ),
                StandardCharsets.UTF_8
        );
        System.out.println("Generated index.html");
        
        {
            String website = Localization.get().localize(Localization.WEBSITE_URL, null);
            if (!website.endsWith("/")) {
                website += "/";
            }
            
            final String indent = " ".repeat(4);
            
            StringBuilder b = new StringBuilder();
            b.append("<!--\n");
            b.append("Sitemap XML - Mechanically Generated File\n");
            b.append("-->\n");
            b.append("<urlset xmlns=\"https://www.sitemaps.org/schemas/sitemap/0.9\">\n");
            for (String url:generatedURLs) {
                b.append(indent).append("<url>\n");
                b.append(indent).append(indent).append("<loc>").append(website).append(url.replace('\\', '/')).append("</loc>\n");
                b.append(indent).append("</url>\n");
            }
            b.append("</urlset>");
            
            Files.writeString(sitemapFile, b.toString(), StandardCharsets.UTF_8);
        }
        System.out.println("Generated sitemap.xml");
        
    }

}
