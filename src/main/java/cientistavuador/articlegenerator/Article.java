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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 *
 * @author Cien
 */
public class Article {

    public static final String INDENT = " ".repeat(4);

    public static final String KEYWORDS = "keywords";

    private final List<TextBlock> headBlocks;
    private final List<TextBlock> bodyBlocks;

    private final int id;
    private final ISOLanguage[] languages;
    private final Set<ISOLanguage> languagesSet = new HashSet<>();
    private final Map<String, String> fields = new HashMap<>();

    public Article(List<TextBlock> blocks) {
        Objects.requireNonNull(blocks, "Blocks is null.");
        if (blocks.size() < 2) {
            throw new IllegalArgumentException("Blocks must contain at least 2 blocks (id and languages).");
        }

        int articleId = -1;
        ISOLanguage[] langsArray = null;

        int headLength = -1;

        for (int i = 0; i < blocks.size(); i++) {
            TextBlock block = blocks.get(i);
            switch (block.getName()) {
                case "id", "languages", "license", "title", "description", "date", "footer-return", "footer-notice" -> {
                    if (headLength != -1) {
                        throw new IllegalArgumentException("Invalid head block at line " + block.getLine());
                    }
                    switch (block.getName()) {
                        case "id", "languages" -> {
                            if (block.hasAttribute()) {
                                throw new IllegalArgumentException(block.getName() + " block must not contain a attribute at line " + block.getLine());
                            }
                        }
                    }
                    switch (block.getName()) {
                        case "id" -> {
                            if (articleId != -1) {
                                throw new IllegalArgumentException("id block redeclaration at line " + block.getLine());
                            }
                            articleId = block.getIntegerFormatted();
                            if (articleId < 0) {
                                throw new IllegalArgumentException("Negative article id at line " + block.getName());
                            }
                        }
                        case "languages" -> {
                            if (!this.languagesSet.isEmpty()) {
                                throw new IllegalArgumentException("languages block redeclaration at line " + block.getLine());
                            }
                            ISOLanguage[] articleLanguages = block.getLanguageListFormatted();
                            if (articleLanguages.length == 0) {
                                throw new IllegalArgumentException("languages block has no languages at line " + block.getLine());
                            }
                            this.languagesSet.addAll(Arrays.asList(articleLanguages));
                            if (this.languagesSet.size() != articleLanguages.length) {
                                throw new IllegalArgumentException("Duplicated languages at line " + block.getLine());
                            }
                            langsArray = articleLanguages;
                        }
                        default -> {
                            String key = block.getName() + "." + block.getAttributeLanguage();
                            if (this.fields.containsKey(key)) {
                                throw new IllegalArgumentException(block.getName() + " redeclaration for language " + block.getAttributeLanguage() + " at line " + block.getLine());
                            }
                            String value;
                            switch (block.getName()) {
                                case "license" -> {
                                    value = block.getCodeFormatted();
                                }
                                default -> {
                                    value = block.getTitleFormatted();
                                }
                            }
                            this.fields.put(key, value);
                        }
                    }
                }
                default -> {
                    if (headLength == -1) {
                        headLength = i;
                    }
                    ISOLanguage blockLanguage = block.getAttributeLanguage();
                    if (blockLanguage.hasLanguage() && !this.languagesSet.contains(blockLanguage)) {
                        throw new IllegalArgumentException("Block at line " + block.getLine() + " has undefined language " + blockLanguage.toString());
                    }
                }

            }
        }
        if (headLength == -1) {
            headLength = blocks.size();
        }

        this.headBlocks = Collections.unmodifiableList(blocks.subList(0, headLength));
        this.bodyBlocks = Collections.unmodifiableList(blocks.subList(headLength, blocks.size()));
        this.id = articleId;

        if (articleId == -1) {
            throw new IllegalArgumentException("id block not found.");
        }

        if (this.languagesSet.isEmpty()) {
            throw new IllegalArgumentException("languages block not found.");
        }

        this.languages = langsArray;

        Map<ISOLanguage, StringBuilder> keywords = new HashMap<>();
        for (ISOLanguage e : this.languagesSet) {
            keywords.put(e, new StringBuilder());
        }
        for (TextBlock bodyBlock : this.bodyBlocks) {
            switch (bodyBlock.getName()) {
                case "section", "subsection", "text", "fine", "warning", "severe", "olist", "ulist" -> {
                    ISOLanguage language = bodyBlock.getAttributeLanguage();
                    if (language.hasLanguage()) {
                        keywords.get(language).append(" ").append(bodyBlock.getRawText());
                    } else {
                        for (Entry<ISOLanguage, StringBuilder> entry : keywords.entrySet()) {
                            entry.getValue().append(" ").append(bodyBlock.getRawText());
                        }
                    }
                }
            }
        }
        for (Entry<ISOLanguage, StringBuilder> entry : keywords.entrySet()) {
            this.fields.put(KEYWORDS + "." + entry.getKey(), KeywordMapper.getKeywords(entry.getValue().toString()));
        }
    }

    public List<TextBlock> getHeadBlocks() {
        return headBlocks;
    }

    public List<TextBlock> getBodyBlocks() {
        return bodyBlocks;
    }

    public int getId() {
        return id;
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

    public ISOLanguage findBestAvailableLanguage(ISOLanguage language) {
        if (language == null) {
            language = ISOLanguage.EMPTY;
        }
        if (containsLanguage(language)) {
            return language;
        }
        if (containsLanguage(language.toLanguageOnly())) {
            return language.toLanguageOnly();
        }
        ISOLanguage defLanguage = Localization.get().getDefaultLanguage();
        if (containsLanguage(defLanguage)) {
            return defLanguage;
        }
        if (containsLanguage(defLanguage.toLanguageOnly())) {
            return defLanguage.toLanguageOnly();
        }
        return getLanguage(0);
    }

    public String getField(String key, ISOLanguage language) {
        Objects.requireNonNull(key, "Key is null.");
        if (language == null) {
            language = ISOLanguage.EMPTY;
        }

        String keyA = key + "." + language.toString();
        String keyB = key + ".";

        String value = this.fields.get(keyA);
        if (value == null) {
            value = this.fields.get(keyB);
            if (value == null) {
                value = Localization.get().localize(key, language);
                if (value == null) {
                    throw new IllegalArgumentException("Invalid key " + key + " for language " + language);
                }
            }
        }
        return value;
    }

    private static class Node {

        private ISOLanguage language;

        private TextBlock block;
        private final List<TextBlock> resources = new ArrayList<>();
        private final List<Node> children = new ArrayList<>();
        private String fullName;
    }

    private Node mapNode(ISOLanguage language) {
        Node root = new Node();
        root.fullName = "(root)";
        root.language = language;

        int sectionIds = 0;
        int subsectionIds = 0;

        Node section = null;
        Node subsection = null;

        for (int i = 0; i < this.bodyBlocks.size(); i++) {
            TextBlock b = this.bodyBlocks.get(i);

            ISOLanguage blockLanguage = b.getAttributeLanguage();
            if (blockLanguage.hasLanguage() && !language.equals(blockLanguage)) {
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
                case "text", "fine", "warning", "severe", "code", "image", "olist", "ulist", "html", "csv" -> {
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
        return root;
    }

    private String writeHead(Node root) {
        StringBuilder b = new StringBuilder();

        String headTitle = FontFormatting.escapeAndFormat(getField(Localization.TITLE, root.language), true);
        String headKeywords = getField(Article.KEYWORDS, root.language);
        String headDescription = FontFormatting.escapeAndFormat(getField(Localization.DESCRIPTION, root.language), true);

        String icon = FontFormatting.escape(Localization.get().localize(Localization.ICON, root.language));
        String stylesheet = FontFormatting.escape(Localization.get().localize(Localization.STYLESHEET, root.language));

        String openGraphType = FontFormatting.escape(Localization.get().localize(Localization.OPENGRAPH_TYPE, root.language));
        String openGraphImageURL = FontFormatting.escape(Localization.get().localize(Localization.OPENGRAPH_IMAGE, root.language));

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
        b.append(INDENT).append("<link rel=\"stylesheet\" href=\"").append(stylesheet).append("\" type=\"text/css\"").append("/>\n");
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

    private String writeHeader(Node root) {
        StringBuilder b = new StringBuilder();

        String titleHeader = FontFormatting.escapeAndFormat(getField(Localization.TITLE, root.language));
        String descriptionHeader = FontFormatting.escapeAndFormat(getField(Localization.DESCRIPTION, root.language));
        String idHeader = String.format("%04d", getId());
        String dateHeader = FontFormatting.escapeAndFormat(getField(Localization.DATE, root.language));

        b.append("<header class=\"header\">\n");
        b.append(INDENT).append("<h1>").append(titleHeader).append("</h1>\n");
        b.append(INDENT).append("<h2>").append(descriptionHeader).append("</h2>\n");
        b.append(INDENT).append("<h3>").append(idHeader).append("</h3>\n");
        b.append(INDENT).append("<h3>").append(dateHeader).append("</h3>\n");
        b.append(INDENT).append("<h4>");
        for (int i = 0; i < this.languages.length; i++) {
            String languageArticleLink = URLEncoder.encode(getId() + "_" + this.languages[i], StandardCharsets.UTF_8) + ".html";
            b.append("<a href=\"").append(languageArticleLink).append("\">");
            b.append(this.languages[i].toStringUpperCase());
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

        String sectionLink = URLEncoder.encode(FontFormatting.escapeAndFormat(node.fullName, true), StandardCharsets.UTF_8);
        String sectionIndex = FontFormatting.escapeAndFormat(node.fullName);
        b.append("<li><a href=\"#").append(sectionLink).append("\">").append(sectionIndex).append("</a></li>\n");
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
                String text = FontFormatting.escapeAndFormat(block.getParagraphFormatted()).indent(4);
                b.append("<p class=\"").append(block.getName()).append("\">\n");
                b.append(text);
                b.append("</p>");
            }
            case "code" -> {
                b.append("<ol class=\"code\">\n");
                Stream<String> lines
                        = block.getCodeFormatted()
                                .lines()
                                .map(FontFormatting::escape);
                for (String line : lines.toList()) {
                    b.append(INDENT).append("<li><code>").append(line).append("</code></li>\n");
                }
                b.append("</ol>");
            }
            case "image" -> {
                String altPlaceholder;
                {
                    String[] split = block.getTitleFormatted().split("/");
                    altPlaceholder = split[split.length - 1].split(Pattern.quote("."))[0];
                }
                String imageURL = FontFormatting.escape(block.getTitleFormatted());
                b.append("<img class=\"image\" src=\"").append(imageURL).append("\" alt=\"").append(altPlaceholder).append("\"/>");
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
                    String listElementText = FontFormatting.escapeAndFormat(formatted[i]);
                    b.append(INDENT).append("<li><p>").append(listElementText).append("</p></li>\n");
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
            case "csv" -> {
                CSV csv = block.getCSVFormatted().copy();
                for (int record = 0; record < csv.getNumberOfRecords(); record++) {
                    for (int field = 0; field < csv.getNumberOfFields(); field++) {
                        csv.set(field, record, FontFormatting.escapeAndFormat(TextFormatting.getTitleFormatted(csv.get(field, record))));
                    }
                }
                int[] maxFieldSize = new int[csv.getNumberOfFields()];
                for (int record = 0; record < csv.getNumberOfRecords(); record++) {
                    for (int field = 0; field < csv.getNumberOfFields(); field++) {
                        maxFieldSize[field] = Math.max(maxFieldSize[field], csv.get(field, record).length());
                    }
                }
                for (int record = 0; record < csv.getNumberOfRecords(); record++) {
                    for (int field = 0; field < csv.getNumberOfFields(); field++) {
                        String value = csv.get(field, record);
                        csv.set(field, record, value.concat(" ".repeat(Math.max(maxFieldSize[field] - value.length(), 0))));
                    }
                }

                b.append("<table class=\"table\">\n");
                for (int record = 0; record < csv.getNumberOfRecords(); record++) {
                    b.append(INDENT).append("<tr>");
                    for (int field = 0; field < csv.getNumberOfFields(); field++) {
                        b
                                .append("<")
                                .append((record == 0 ? "th" : "td"))
                                .append(">")
                                .append(csv.get(field, record))
                                .append("</")
                                .append((record == 0 ? "th" : "td"))
                                .append(">");
                    }
                    b.append("</tr>\n");
                }
                b.append("</table>");
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
            String sectionId = URLEncoder.encode(FontFormatting.escapeAndFormat(node.fullName, true), StandardCharsets.UTF_8);
            String sectionTitle = FontFormatting.escapeAndFormat(node.fullName);

            b.append("<section id=\"").append(sectionId).append("\">\n");
            b.append(INDENT).append("<").append(tag).append(">").append(sectionTitle).append("</").append(tag).append(">\n");
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

        String returnLink = URLEncoder.encode("articles_" + root.language, StandardCharsets.UTF_8) + ".html";
        String returnText = FontFormatting.escapeAndFormat(getField(Localization.FOOTER_RETURN, root.language));
        String notice = FontFormatting.escapeAndFormat(getField(Localization.FOOTER_NOTICE, root.language));

        b.append("<footer class=\"footer\">\n");
        b.append(INDENT).append("<p>").append("<a href=\"").append(returnLink).append("\">").append(returnText).append("</a>").append("</p>\n");
        if (Boolean.parseBoolean(Localization.get().localize(Localization.UTTERANCES_ENABLED, root.language))) {
            String utterancesRepo = FontFormatting.escape(Localization.get().localize(Localization.UTTERANCES_REPO, root.language));
            String utterancesIssueTerm = FontFormatting.escape(Localization.get().localize(Localization.UTTERANCES_ISSUE_TERM_PREFIX, root.language)) + " " + Integer.toString(getId());
            String utterancesLabel = FontFormatting.escape(Localization.get().localize(Localization.UTTERANCES_LABEL, root.language));
            String utterancesTheme = FontFormatting.escape(Localization.get().localize(Localization.UTTERANCES_THEME, root.language));

            StringBuilder ut = new StringBuilder();

            ut.append("<script src=\"https://utteranc.es/client.js\"\n");
            ut.append(INDENT).append(INDENT).append("repo=\"").append(utterancesRepo).append("\"\n");
            ut.append(INDENT).append(INDENT).append("issue-term=\"").append(utterancesIssueTerm).append("\"\n");;
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

    private String writeBody(Node root) {
        StringBuilder b = new StringBuilder();
        
        String titleText = FontFormatting.escapeComment(getField(Localization.TITLE, root.language));
        String descriptionText = FontFormatting.escapeComment(getField(Localization.DESCRIPTION, root.language));
        String dateText = FontFormatting.escapeComment(getField(Localization.DATE, root.language));

        String licenseText = FontFormatting.escapeComment(getField(Localization.LICENSE, root.language));
        b.append("<!--\n");
        b.append("\n");
        b.append(licenseText).append("\n");
        b.append("\n");
        b.append(root.language).append("\n");
        b.append(INDENT).append(titleText).append("\n");
        b.append(INDENT).append(descriptionText).append("\n");
        b.append(INDENT).append(getId()).append("\n");
        b.append(INDENT).append(dateText).append("\n");
        b.append("\n");
        b.append(new Date().toString()).append("\n");
        b.append("-->\n");
        b.append("<body class=\"body\">\n");
        b.append(writeHeader(root).indent(4));
        b.append(writeArticle(root).indent(4));
        b.append(writeFooter(root).indent(4));
        b.append("</body>");

        return b.toString();
    }

    public String toHTML(ISOLanguage language) {
        if (!containsLanguage(language)) {
            throw new IllegalArgumentException("Invalid language " + language);
        }

        Node root = mapNode(language);

        StringBuilder b = new StringBuilder();
        
        b.append("<!DOCTYPE html>\n");
        b.append("<html lang=\"").append(root.language).append("\">\n");
        b.append(writeHead(root).indent(4));
        b.append(writeBody(root).indent(4));
        b.append("</html>");
        
        return b.toString();
    }
}
