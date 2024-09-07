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
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 *
 * @author Cien
 */
public class Article2 {

    public static final String LICENSE;

    static {
        try {
            LICENSE = Files.readString(Paths.get("resources", "license.txt"), StandardCharsets.UTF_8).stripTrailing();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public static final String INDENT = " ".repeat(4);

    private final List<TextBlock> blocks;
    private final int id;
    private final String[] languages;
    private final Map<String, Integer> languageMap = new HashMap<>();
    private final Map<String, String> title = new HashMap<>();
    private final Map<String, String> description = new HashMap<>();
    private final Map<String, String> date = new HashMap<>();
    private final Map<String, String> license = new HashMap<>();
    private final Map<String, String> footerReturn = new HashMap<>();
    private final Map<String, String> footerNotice = new HashMap<>();
    private final int metaBlocksLength;

    private final Map<String, Node> nodeMap = new HashMap<>();
    private final Map<String, String> keywordsMap = new HashMap<>();
    private final Map<String, String> htmlMap = new HashMap<>();

    public Article2(List<TextBlock> blocks) {
        Objects.requireNonNull(blocks, "Blocks is null.");
        this.blocks = blocks;

        if (blocks.size() < 2) {
            throw new IllegalArgumentException("Blocks must contain at least 2 blocks (id and languages).");
        }

        TextBlock idBlock = blocks.get(0);
        TextBlock languagesBlock = blocks.get(1);

        if (!idBlock.getName().equals("id")) {
            throw new IllegalArgumentException("First block must be the id.");
        }

        if (!languagesBlock.getName().equals("languages")) {
            throw new IllegalArgumentException("Second block must be the languages.");
        }

        if (idBlock.hasAttribute()) {
            throw new IllegalArgumentException("Id block must not have a language at line " + languagesBlock.getLine());
        }

        if (languagesBlock.hasAttribute()) {
            throw new IllegalArgumentException("Languages block must not have a language at line " + languagesBlock.getLine());
        }

        this.id = idBlock.getIntegerFormatted();
        this.languages = languagesBlock.getListFormatted();

        if (this.languages.length == 0) {
            throw new IllegalArgumentException("Languages block is empty at line " + languagesBlock.getLine());
        }

        for (int i = 0; i < this.languages.length; i++) {
            this.languageMap.put(this.languages[i], i);
        }

        Set<String> languageSet = new HashSet<>();
        for (String lang : this.languages) {
            if (languageSet.contains(lang)) {
                throw new IllegalArgumentException("Duplicate language at languages block in line " + languagesBlock.getLine());
            }
            languageSet.add(lang);
        }

        for (TextBlock b : this.blocks) {
            if (b.hasAttribute() && !languageSet.contains(b.getAttribute())) {
                throw new IllegalArgumentException("Invalid language " + b.getAttribute() + " at line " + b.getLine());
            }
        }

        int metaLength = 2;
        metaLoop:
        for (int i = metaLength; i < this.blocks.size(); i++) {
            TextBlock b = this.blocks.get(i);
            switch (b.getName()) {
                case "id", "languages" -> {
                    throw new IllegalArgumentException("Invalid duplicated " + b.getName() + " at line " + b.getLine());
                }
                case "title", "description", "date", "license", "footer-return", "footer-notice" -> {
                    Map<String, String> map = null;
                    switch (b.getName()) {
                        case "title" ->
                            map = this.title;
                        case "description" ->
                            map = this.description;
                        case "date" ->
                            map = this.date;
                        case "license" ->
                            map = this.license;
                        case "footer-return" ->
                            map = this.footerReturn;
                        case "footer-notice" ->
                            map = this.footerNotice;
                    }
                    Objects.requireNonNull(map, "Map is null?");
                    if (map.containsKey(b.getAttribute())) {
                        throw new IllegalArgumentException("Duplicated block " + b.getName() + " for language " + b.getAttribute() + " at line " + b.getLine());
                    }
                    if (b.getName().equals("license")) {
                        map.put(b.getAttribute(), b.getCodeFormatted());
                    } else {
                        map.put(b.getAttribute(), b.getTitleFormatted());
                    }
                    metaLength++;
                }
                default -> {
                    break metaLoop;
                }
            }
        }
        this.metaBlocksLength = metaLength;

        for (int i = this.metaBlocksLength; i < this.blocks.size(); i++) {
            TextBlock b = this.blocks.get(i);
            switch (b.getName()) {
                case "id", "languages", "title", "description", "date" -> {
                    throw new IllegalArgumentException("Invalid meta block at line " + b.getLine());
                }
            }
        }
    }

    public List<TextBlock> getBlocks() {
        return blocks;
    }

    public int getId() {
        return id;
    }

    public String[] getLanguages() {
        return languages;
    }

    public int indexOfLanguage(String language) {
        Integer i = this.languageMap.get(language);
        if (i == null) {
            return -1;
        }
        return i;
    }

    private String find(Map<String, String> map, String language, String nullFallback) {
        String correct = map.get(language);
        String fallback = map.get("");
        if (correct != null) {
            return correct;
        }
        if (fallback != null) {
            return fallback;
        }
        return nullFallback;
    }

    private String getTitle(String language) {
        return find(this.title, language, "Untitled");
    }

    private String getDescription(String language) {
        return find(this.description, language, "No Description.");
    }

    private String getDate(String language) {
        return find(this.date, language, "No Date.");
    }

    private String getLicense(String language) {
        return find(this.license, language,
                """
                This is free and unencumbered software released into the public domain.
                
                Anyone is free to copy, modify, publish, use, compile, sell, or
                distribute this software, either in source code form or as a compiled
                binary, for any purpose, commercial or non-commercial, and by any
                means.
                
                In jurisdictions that recognize copyright laws, the author or authors
                of this software dedicate any and all copyright interest in the
                software to the public domain. We make this dedication for the benefit
                of the public at large and to the detriment of our heirs and
                successors. We intend this dedication to be an overt act of
                relinquishment in perpetuity of all present and future rights to this
                software under copyright law.
                
                THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
                EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
                MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
                IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
                OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
                ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
                OTHER DEALINGS IN THE SOFTWARE.
                
                For more information, please refer to <http://unlicense.org/>
                """
        );
    }

    private String getFooterReturn(String language) {
        return find(this.footerReturn, language, "<<< Return to Articles");
    }
    
    private String getFooterNotice(String language) {
        return find(this.footerNotice, language, "Content on this website is released under the [[[https://unlicense.org/:::Unlicense]]] unless stated otherwise.");
    }

    public String getTitle(int languageIndex) {
        return getTitle(this.languages[languageIndex]);
    }

    public String getDescription(int languageIndex) {
        return getDescription(this.languages[languageIndex]);
    }

    public String getDate(int languageIndex) {
        return getDate(this.languages[languageIndex]);
    }

    public String getLicense(int languageIndex) {
        return getLicense(this.languages[languageIndex]);
    }

    public String getFooterReturn(int languageIndex) {
        return getFooterReturn(this.languages[languageIndex]);
    }

    public String getFooterNotice(int languageIndex) {
        return getFooterNotice(this.languages[languageIndex]);
    }

    public int getMetaBlocksLength() {
        return metaBlocksLength;
    }

    private static class Node {

        private String language;

        private TextBlock block;
        private final List<TextBlock> resources = new ArrayList<>();
        private final List<Node> children = new ArrayList<>();
    }

    private Node mapNode(String language) {
        Node root = this.nodeMap.get(language);
        if (root == null) {
            root = new Node();
            root.language = language;

            Node section = null;
            Node subsection = null;

            for (int i = this.metaBlocksLength; i < this.blocks.size(); i++) {
                TextBlock b = this.blocks.get(i);

                if (b.hasAttribute() && !b.getAttribute().equals(language)) {
                    continue;
                }

                switch (b.getName()) {
                    case "section" -> {
                        section = new Node();
                        section.language = language;
                        section.block = b;
                        root.children.add(section);

                        subsection = null;
                        continue;
                    }
                    case "subsection" -> {
                        if (section == null) {
                            throw new IllegalArgumentException("Subsection without parent section at line " + b.getLine());
                        }

                        subsection = new Node();
                        subsection.language = language;
                        subsection.block = b;
                        section.children.add(subsection);
                        continue;
                    }
                    case "text", "fine", "warning", "severe", "code", "image", "olist", "ulist", "html" -> {
                        Node toAdd = root;
                        if (section != null) {
                            toAdd = section;
                        }
                        if (subsection != null) {
                            toAdd = subsection;
                        }
                        toAdd.resources.add(b);
                    }
                    default ->
                        throw new IllegalArgumentException("Unknown block " + b.getName() + " at line " + b.getLine());
                }
            }

            this.nodeMap.put(language, root);
        }
        return root;
    }

    private String mapNodeFullText(Node n) {
        StringBuilder b = new StringBuilder();

        if (n.block != null) {
            b.append(n.block.getRawText()).append(" ");
        }
        for (TextBlock resource : n.resources) {
            switch (resource.getName()) {
                case "code", "image", "html" -> {
                    continue;
                }
            }
            b.append(resource.getRawText()).append(" ");
        }
        for (Node child : n.children) {
            b.append(mapNodeFullText(child)).append(" ");
        }

        return b.toString();
    }

    private String getKeywords(String language) {
        String keywords = this.keywordsMap.get(language);
        if (keywords == null) {
            keywords = KeywordMapper.getKeywords(mapNodeFullText(mapNode(language)));
            this.keywordsMap.put(language, keywords);
        }
        return keywords;
    }

    public String getKeywords(int languageIndex) {
        return getKeywords(this.languages[languageIndex]);
    }

    private String writeHead(Node root) {
        StringBuilder b = new StringBuilder();

        b.append("<head>\n");
        b.append(INDENT).append("<title>").append(HTMLTranslator.escape(getTitle(root.language))).append("</title>\n");
        b.append(INDENT).append("<meta charset=\"UTF-8\"/>\n");
        b.append(INDENT).append("<meta name=\"keywords\" content=\"").append(getKeywords(root.language)).append("\"/>\n");
        b.append(INDENT).append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/>\n");
        b.append(INDENT).append("<meta name=\"description\" content=\"").append(HTMLTranslator.escape(getDescription(root.language))).append("\"/>\n");
        b.append(INDENT).append("<link rel=\"icon\" type=\"image/x-icon\" href=\"../resources/icon.png\"/>\n");
        b.append(INDENT).append("<link rel=\"stylesheet\" href=\"").append("../resources/style.css").append("\" type=\"text/css\"").append("/>\n");
        b.append(INDENT).append("\n");
        b.append(INDENT).append("<!-- OpenGraph -->\n");
        b.append(INDENT).append("<meta name=\"og:title\" content=\"").append(HTMLTranslator.escape(getTitle(root.language))).append("\"/>\n");
        b.append(INDENT).append("<meta name=\"og:description\" content=\"").append(HTMLTranslator.escape(getDescription(root.language))).append("\"/>\n");
        b.append(INDENT).append("<meta name=\"og:type\" content=\"article\"/>\n");
        b.append(INDENT).append("<meta name=\"og:image\" content=\"../resources/icon.png\"/>\n");
        b.append(INDENT).append("<!-- OpenGraph -->\n");
        b.append("</head>");

        return b.toString();
    }

    private String writeHeader(Node root) {
        StringBuilder b = new StringBuilder();

        b.append("<header class=\"header\">\n");
        b.append(INDENT).append("<h1>").append(HTMLTranslator.escape(getTitle(root.language))).append("</h1>\n");
        b.append(INDENT).append("<h2>").append(HTMLTranslator.escape(getDescription(root.language))).append("</h2>\n");
        b.append(INDENT).append("<h3>").append(String.format("%04d", getId())).append("</h3>\n");
        b.append(INDENT).append("<h3>").append(getDate(root.language)).append("</h3>\n");
        b.append("</header>");

        return b.toString();
    }

    private String writeSectionIndex(Section section, int depth) {
        StringBuilder b = new StringBuilder();

        b
                .append("<li><a href=\"#")
                .append(section.getFullNameEncoded())
                .append("\">")
                .append(section.getFullName())
                .append("</a></li>\n");
        if (!section.getChildren().isEmpty()) {
            b.append("<li>\n");
            b.append(INDENT).append("<ol>\n");
            for (Section e : section.getChildren()) {
                b.append(writeSectionIndex(e, depth + 1).indent(4 + 4 + (depth * 4)));
            }
            b.append(INDENT).append("</ol>\n");
            b.append("</li>");
        }

        return b.toString();
    }

    private String writeIndices() {
        StringBuilder b = new StringBuilder();

        b.append("<ol class=\"indices\">\n");
        for (Section s : getSections()) {
            b.append(writeSectionIndex(s, 0).indent(4));
        }
        b.append("</ol>");

        return b.toString();
    }

    private String writeArticle() {
        StringBuilder b = new StringBuilder();

        b.append("<main class=\"article\">\n");
        b.append(writeIndices().indent(4));
        for (Section s : getSections()) {
            b.append(s.toHTML().indent(4));
        }
        b.append("</main>");

        return b.toString();
    }

    private String writeFooter(Node root) {
        StringBuilder b = new StringBuilder();

        b.append("<footer class=\"footer\">\n");
        //todo: add link
        b.append(INDENT).append("<p>").append(HTMLTranslator.escapeAndTranslate(getFooterReturn(root.language))).append("</p>\n");
        b.append(
                """
                <script src="https://utteranc.es/client.js"
                        repo="CientistaVuador/cientistavuador.github.io"
                        issue-term="{ARTICLE-ID-HERE}"
                        label="comments"
                        theme="github-dark"
                        crossorigin="anonymous"
                        async="async">
                </script>
                """.replace("{ARTICLE-ID-HERE}", "Article " + Integer.toString(getId())).indent(4));
        b.append(INDENT).append("<p>").append(HTMLTranslator.escapeAndTranslate(getFooterNotice(root.language))).append("</p>\n");
        b.append("</footer>");

        return b.toString();
    }

    private String writeBody(Node root) {
        StringBuilder b = new StringBuilder();

        b.append("<body class=\"body\">\n");
        b.append(writeHeader(root).indent(4));
        b.append(writeArticle(root).indent(4));
        b.append(writeFooter(root).indent(4));
        b.append("</body>");

        return b.toString();
    }

    private String toHTML(String language) {
        String html = this.htmlMap.get(language);
        if (html == null) {
            Node root = mapNode(language);

            StringBuilder b = new StringBuilder();

            b.append("<!DOCTYPE html>\n");
            b.append("<!--\n");
            b.append("\n");
            b.append(getLicense(root.language));
            b.append("\n\n");
            b.append(HTMLTranslator.escape(getTitle(root.language))).append("\n");
            b.append(getId()).append("\n");
            b.append(getDate(root.language)).append("\n");
            b.append("\n");
            b.append(new Date().toString()).append("\n");
            b.append("-->\n");
            b.append("<html lang=\"").append(root.language).append("\">\n");
            b.append(writeHead(root).indent(4));
            b.append(writeBody(root).indent(4));
            b.append("</html>");

            html = b.toString();
            this.htmlMap.put(language, html);
        }
        return html;
    }

    public String toHTML(int languageIndex) {
        return toHTML(this.languages[languageIndex]);
    }
}
