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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 *
 * @author Cien
 */
public class Article {

    public static final String DEFAULT_TITLE = "No Title";
    public static final String DEFAULT_DESCRIPTION = "No Description";
    public static final String DEFAULT_DATE = "No Date";
    public static final String DEFAULT_LICENSE = "All Rights Reserved";
    public static final String DEFAULT_FOOTER_RETURN = "<<< Return to Articles";
    public static final String DEFAULT_FOOTER_NOTICE = "All Rights Reserved";
    
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

    public Article(List<TextBlock> blocks) {
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
            for (int i = 0; i < lang.length(); i++) {
                int unicode = lang.codePointAt(i);
                switch (unicode) {
                    case '&', '>', '<', '"', '\'' ->
                        throw new IllegalArgumentException("Languages contains invalid characteres at line " + languagesBlock.getLine());
                }
            }
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

    public int getNumberOfLanguages() {
        return this.languages.length;
    }

    public String getLanguage(int index) {
        return this.languages[index];
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
        return find(this.title, language, DEFAULT_TITLE);
    }

    private String getDescription(String language) {
        return find(this.description, language, DEFAULT_DESCRIPTION);
    }

    private String getDate(String language) {
        return find(this.date, language, DEFAULT_DATE);
    }

    private String getLicense(String language) {
        return find(this.license, language, DEFAULT_LICENSE);
    }

    private String getFooterReturn(String language) {
        return find(this.footerReturn, language, DEFAULT_FOOTER_RETURN);
    }

    private String getFooterNotice(String language) {
        return find(this.footerNotice, language, DEFAULT_FOOTER_NOTICE);
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
        private String fullName;
    }

    private Node mapNode(String language) {
        Node root = this.nodeMap.get(language);
        if (root == null) {
            root = new Node();
            root.fullName = "(root)";
            root.language = language;

            int sectionIds = 0;
            int subsectionIds = 0;

            Node section = null;
            Node subsection = null;

            for (int i = this.metaBlocksLength; i < this.blocks.size(); i++) {
                TextBlock b = this.blocks.get(i);

                if (b.hasAttribute() && !b.getAttribute().equals(language)) {
                    continue;
                }

                switch (b.getName()) {
                    case "section" -> {
                        sectionIds++;
                        subsectionIds = 0;

                        section = new Node();
                        section.language = language;
                        section.block = b;
                        section.fullName = sectionIds + " " + b.getTitleFormatted();
                        root.children.add(section);

                        subsection = null;
                        continue;
                    }
                    case "subsection" -> {
                        if (section == null) {
                            throw new IllegalArgumentException("Subsection without parent section at line " + b.getLine());
                        }
                        subsectionIds++;

                        subsection = new Node();
                        subsection.language = language;
                        subsection.block = b;
                        subsection.fullName = sectionIds + "." + subsectionIds + " " + b.getTitleFormatted();
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
        b.append(INDENT).append("<title>").append(HTMLTranslator.escapeAndTranslate(getTitle(root.language), true)).append("</title>\n");
        b.append(INDENT).append("<meta charset=\"UTF-8\"/>\n");
        b.append(INDENT).append("<meta name=\"keywords\" content=\"").append(getKeywords(root.language)).append("\"/>\n");
        b.append(INDENT).append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/>\n");
        b.append(INDENT).append("<meta name=\"description\" content=\"").append(HTMLTranslator.escapeAndTranslate(getDescription(root.language), true)).append("\"/>\n");
        b.append(INDENT).append("<link rel=\"icon\" type=\"image/x-icon\" href=\"../resources/icon.png\"/>\n");
        b.append(INDENT).append("<link rel=\"stylesheet\" href=\"").append("../resources/style.css").append("\" type=\"text/css\"").append("/>\n");
        b.append(INDENT).append("\n");
        b.append(INDENT).append("<!-- OpenGraph -->\n");
        b.append(INDENT).append("<meta name=\"og:title\" content=\"").append(HTMLTranslator.escapeAndTranslate(getTitle(root.language), true)).append("\"/>\n");
        b.append(INDENT).append("<meta name=\"og:description\" content=\"").append(HTMLTranslator.escapeAndTranslate(getDescription(root.language), true)).append("\"/>\n");
        b.append(INDENT).append("<meta name=\"og:type\" content=\"article\"/>\n");
        b.append(INDENT).append("<meta name=\"og:image\" content=\"../resources/icon.png\"/>\n");
        b.append(INDENT).append("<!-- OpenGraph -->\n");
        b.append("</head>");

        return b.toString();
    }

    private String writeHeader(Node root) {
        StringBuilder b = new StringBuilder();

        b.append("<header class=\"header\">\n");
        b.append(INDENT).append("<h1>").append(HTMLTranslator.escapeAndTranslate(getTitle(root.language))).append("</h1>\n");
        b.append(INDENT).append("<h2>").append(HTMLTranslator.escapeAndTranslate(getDescription(root.language))).append("</h2>\n");
        b.append(INDENT).append("<h3>").append(String.format("%04d", getId())).append("</h3>\n");
        b.append(INDENT).append("<h3>").append(HTMLTranslator.escapeAndTranslate(getDate(root.language))).append("</h3>\n");
        b.append(INDENT).append("<h4>");
        for (int i = 0; i < this.languages.length; i++) {
            b.append("<a href=\"").append(URLEncoder.encode(getId() + "_" + this.languages[i], StandardCharsets.UTF_8)).append(".html").append("\">");
            b.append(this.languages[i].toUpperCase());
            b.append("</a>");
            if (i != (this.languages.length - 1)) {
                b.append(" | ");
            }
        }
        b.append("</h4>\n");
        b.append("</header>");

        return b.toString();
    }

    private String writeNodeIndex(Node node, int depth) {
        StringBuilder b = new StringBuilder();

        if (depth == 0) {
            for (Node e : node.children) {
                b.append(writeNodeIndex(e, depth + 1));
            }
            return b.toString();
        }

        b
                .append("<li><a href=\"#")
                .append(URLEncoder.encode(HTMLTranslator.escapeAndTranslate(node.fullName, true), StandardCharsets.UTF_8))
                .append("\">")
                .append(HTMLTranslator.escapeAndTranslate(node.fullName))
                .append("</a></li>\n");
        if (!node.children.isEmpty()) {
            b.append("<li>\n");
            b.append(INDENT).append("<ol>\n");
            for (Node e : node.children) {
                b.append(writeNodeIndex(e, depth + 1).indent(4 + (depth * 4)));
            }
            b.append(INDENT).append("</ol>\n");
            b.append("</li>");
        }

        return b.toString();
    }

    private String writeIndices(Node root) {
        StringBuilder b = new StringBuilder();

        b.append("<ol class=\"indices\">\n");
        b.append(writeNodeIndex(root, 0).indent(4));
        b.append("</ol>");

        return b.toString();
    }

    private String writeResource(TextBlock block) {
        StringBuilder b = new StringBuilder();

        switch (block.getName()) {
            case "text", "fine", "warning", "severe" -> {
                b.append("<p class=\"").append(block.getName()).append("\">\n");
                b.append(HTMLTranslator.escapeAndTranslate(block.getParagraphFormatted()).indent(4));
                b.append("</p>");
            }
            case "code" -> {
                b.append("<ol class=\"code\">\n");
                Stream<String> lines = TextBlock.getCodeFormatted(block.getCodeFormatted()).lines();
                for (String line : lines.toList()) {
                    b.append(INDENT).append("<li><code>").append(HTMLTranslator.escape(line)).append("</code></li>\n");
                }
                b.append("</ol>");
            }
            case "image" -> {
                String altPlaceholder;
                {
                    String[] split = block.getTitleFormatted().split("/");
                    altPlaceholder = split[split.length - 1].split(Pattern.quote("."))[0];
                }
                b.append("<img class=\"image\" src=\"").append(HTMLTranslator.escape(block.getTitleFormatted())).append("\" alt=\"").append(altPlaceholder).append("\"/>");
            }
            case "olist", "ulist" -> {
                boolean ordered = block.getName().equals("olist");

                if (ordered) {
                    b.append("<ol class=\"list\">\n");
                } else {
                    b.append("<ul class=\"list\">\n");
                }

                String[] formatted = block.getListFormatted();
                for (int i = 0; i < formatted.length; i++) {
                    b.append(INDENT).append("<li><p>").append(HTMLTranslator.escapeAndTranslate(formatted[i])).append("</p></li>\n");
                }

                if (ordered) {
                    b.append("</ol>");
                } else {
                    b.append("</ul>");
                }
            }
            case "html" -> {
                b.append(block.getCodeFormatted());
            }
            default ->
                throw new IllegalArgumentException("Unknown resource " + block.getName() + " at line " + block.getLine());
        }

        return b.toString();
    }

    private String writeNode(Node node, int depth) {
        StringBuilder b = new StringBuilder();

        int rootMultiplier = 1;
        if (node.block == null) {
            rootMultiplier = 0;
        }

        String tag = "h" + (depth + 1);

        if (rootMultiplier != 0) {
            b.append("<section id=\"").append(URLEncoder.encode(HTMLTranslator.escapeAndTranslate(node.fullName, true), StandardCharsets.UTF_8)).append("\">\n");
            b.append(INDENT).append("<").append(tag).append(">").append(HTMLTranslator.escapeAndTranslate(node.fullName)).append("</").append(tag).append(">\n");
        }
        for (TextBlock resource : node.resources) {
            b.append(writeResource(resource).indent(4 * rootMultiplier));
        }
        for (Node c : node.children) {
            b.append(writeNode(c, depth + 1).indent(4 * rootMultiplier));
        }
        if (rootMultiplier != 0) {
            b.append("</section>");
        }

        return b.toString();
    }

    private String writeArticle(Node root) {
        StringBuilder b = new StringBuilder();

        b.append("<main class=\"article\">\n");
        b.append(writeIndices(root).indent(4));
        b.append(writeNode(root, 0).indent(4));
        b.append("</main>");

        return b.toString();
    }

    private String writeFooter(Node root) {
        StringBuilder b = new StringBuilder();

        b.append("<footer class=\"footer\">\n");
        b.append(INDENT).append("<p>").append("<a href=\"").append(URLEncoder.encode("articles_" + root.language, StandardCharsets.UTF_8)).append(".html").append("\">").append(HTMLTranslator.escapeAndTranslate(getFooterReturn(root.language))).append("</a>").append("</p>\n");
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
            b.append(root.language).append("\n");
            b.append(INDENT).append(HTMLTranslator.escapeAndTranslate(getTitle(root.language), true)).append("\n");
            b.append(INDENT).append(HTMLTranslator.escapeAndTranslate(getDescription(root.language), true)).append("\n");
            b.append(INDENT).append(getId()).append("\n");
            b.append(INDENT).append(HTMLTranslator.escapeAndTranslate(getDate(root.language), true)).append("\n");
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
