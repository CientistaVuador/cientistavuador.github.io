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

    public static void main(String[] args) throws IOException {
        Path articlesFolder = Path.of("articles");
        delete(articlesFolder);
        Files.createDirectories(articlesFolder);
        
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
                Article c = Article.fromTextBlocks(TextBlock.parse(Files.readString(articleFile, StandardCharsets.UTF_8)));
                articles.add(c);
                System.out.println("Loaded " + c.getTitle() + ", ID: " + c.getId() + " from " + articleFile.getFileName());
                
                if (ids.contains(c.getId())) {
                    throw new IllegalArgumentException(articleFile.toString()+" has duplicate id!");
                }
                ids.add(c.getId());
            } catch (Throwable t) {
                System.out.println("Failed to compile: "+articleFile.getFileName());
                throw t;
            }
        }
        
        for (Article c:articles) {
            Files.writeString(articlesFolder.resolve(c.getId()+".html"), c.toHTML());
            System.out.println("Written "+c.getTitle()+", ID: "+c.getId());
        }
        Files.writeString(articlesFolder.resolve("articles.html"), ArticlesPage.generatePage(articles));
        System.out.println("Written articles.html");
    }

}
