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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 *
 * @author Cien
 */
public class Article {

    private static String clear(String s) {
        StringBuilder b = new StringBuilder();

        for (int i = 0; i < s.length(); i++) {
            int unicode = s.codePointAt(i);
            if (unicode == ' ') {
                b.append(' ');
                continue;
            }
            if (!Character.isLetterOrDigit(unicode)) {
                b.append(' ');
                continue;
            }
            b.appendCodePoint(unicode);
        }

        return b.toString();
    }

    private static boolean isOnlyDigits(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.codePointAt(i))) {
                return false;
            }
        }
        return true;
    }
    
    @SuppressWarnings("unchecked")
    public static String getKeywords(String text) {
        List<String> words = new ArrayList<>();
        String cleared = clear(text);
        String[] split = cleared.split(" ");
        for (String word : split) {
            if (word.isBlank() || word.length() == 1) {
                continue;
            }
            if (isOnlyDigits(word)) {
                continue;
            }
            words.add(word.toLowerCase());
        }

        Map<String, Integer> wordCount = new HashMap<>();
        for (String word : words) {
            Integer current = wordCount.get(word);
            if (current == null) {
                current = 0;
            }
            current++;
            wordCount.put(word, current);
        }
        
        Entry<String, Integer>[] set = wordCount.entrySet().toArray(Entry[]::new);
        Comparator<Entry<String, Integer>> comparator = (a, b) -> Integer.compare(a.getValue(), b.getValue());
        Arrays.sort(set, comparator.reversed());

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 64; i++) {
            if (i >= set.length) {
                break;
            }
            builder.append(set[i].getKey());
            if (i != set.length - 1 && i != 63) {
                builder.append(", ");
            }
        }
        
        return builder.toString();
    }

    public static Article fromTextBlocks(List<TextBlock> blocks) {
        Objects.requireNonNull(blocks, "Blocks is null.");

        if (blocks.size() < 4) {
            throw new RuntimeException("List must have at least 4 elements.");
        }

        TextBlock idBlock = blocks.get(0);
        TextBlock dateBlock = blocks.get(1);
        TextBlock titleBlock = blocks.get(2);
        TextBlock descriptionBlock = blocks.get(3);

        if (!idBlock.getName().equals("id")) {
            throw new RuntimeException("First block must be the id.");
        }

        int id = Integer.parseInt(idBlock.getTitleFormatted());

        if (!dateBlock.getName().equals("date")) {
            throw new RuntimeException("Second block must be the date.");
        }

        String date = dateBlock.getTitleFormatted();

        if (!titleBlock.getName().equals("title")) {
            throw new RuntimeException("Third block must be the title.");
        }

        String description = descriptionBlock.getTitleFormatted();

        if (!descriptionBlock.getName().equals("description")) {
            throw new RuntimeException("Fourth block must be the description.");
        }
        
        String title = titleBlock.getTitleFormatted();
        
        Article article = new Article(id, date, title, description);

        List<TextBlock> sub = blocks.subList(4, blocks.size());

        Section parentSection = null;
        Section currentSection = null;

        for (TextBlock block : sub) {
            switch (block.getName()) {
                case "section" -> {
                    parentSection = new Section(article, null, block.getTitleFormatted());
                    article.getSections().add(parentSection);
                    currentSection = parentSection;
                }
                case "subsection" -> {
                    if (parentSection == null) {
                        throw new RuntimeException("No parent section for subsection at line " + block.getLine());
                    }
                    Section child = new Section(article, parentSection, block.getTitleFormatted());
                    parentSection.getChildren().add(child);
                    currentSection = child;
                }
                case "text", "image", "code", "fine", "warning", "severe", "olist", "ulist" -> {
                    if (currentSection == null) {
                        throw new RuntimeException("No parent section for block at line " + block.getLine());
                    }
                    ResourceType type = null;
                    switch (block.getName()) {
                        case "text" ->
                            type = ResourceType.TEXT;
                        case "image" ->
                            type = ResourceType.IMAGE;
                        case "code" ->
                            type = ResourceType.CODE;
                        case "fine" ->
                            type = ResourceType.FINE;
                        case "warning" ->
                            type = ResourceType.WARNING;
                        case "severe" ->
                            type = ResourceType.SEVERE;
                        case "olist" ->
                            type = ResourceType.ORDERED_LIST;
                        case "ulist" ->
                            type = ResourceType.UNORDERED_LIST;
                    }
                    currentSection.getResources().add(new Resource(type, block.getRawText()));
                }
                default -> {
                    throw new RuntimeException("Unknown text block " + block.getName() + " at line " + block.getLine());
                }
            }
        }

        return article;
    }

    public static final String LICENSE;

    static {
        try {
            LICENSE = Files.readString(Paths.get("resources", "license.txt"), StandardCharsets.UTF_8).stripTrailing();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public static final String INDENT = " ".repeat(4);

    
    public static enum ResourceType {
        TEXT, IMAGE, CODE, FINE, WARNING, SEVERE, ORDERED_LIST, UNORDERED_LIST;
    }

    public static class Resource {

        private final ResourceType type;
        private final String resource;

        public Resource(ResourceType type, String resource) {
            Objects.requireNonNull(type, "Type is null.");
            Objects.requireNonNull(resource, "Resource is null.");

            this.type = type;
            this.resource = resource;
        }

        public ResourceType getType() {
            return type;
        }

        public String getResource() {
            return resource;
        }

        private String toImageHTML() {
            String altPlaceholder;
            {
                String[] split = getResource().split("/");
                altPlaceholder = split[split.length - 1].split(Pattern.quote("."))[0];
            }
            return "<img class=\"image\" src=\"" + TextBlock.getTitleFormatted(getResource()) + "\" alt=\"" + altPlaceholder + "\"/>";
        }

        private String toCodeHTML() {
            StringBuilder b = new StringBuilder();

            b.append("<ol class=\"code\">\n");
            Stream<String> lines = TextBlock.getCodeFormatted(getResource()).lines();
            for (String line : lines.toList()) {
                b.append(INDENT).append("<li><code>").append(HTMLTranslator.escape(line)).append("</code></li>\n");
            }
            b.append("</ol>");

            return b.toString();
        }
        
        private String toListHTML(boolean ordered) {
            StringBuilder b = new StringBuilder();
            
            if (ordered) {
                b.append("<ol class=\"list\">\n");
            } else {
                b.append("<ul class=\"list\">\n");
            }
            
            String[] formatted = TextBlock.getListFormatted(getResource());
            for (int i = 0; i < formatted.length; i++) {
                b.append(INDENT).append("<li><p>").append(HTMLTranslator.escapeAndTranslate(formatted[i])).append("</p></li>\n");
            }
            
            if (ordered) {
                b.append("</ol>");
            } else {
                b.append("</ul>");
            }
            
            return b.toString();
        }
        
        public String toHTML() {
            if (this.type.equals(ResourceType.IMAGE)) {
                return toImageHTML();
            }

            if (this.type.equals(ResourceType.CODE)) {
                return toCodeHTML();
            }
            
            if (this.type.equals(ResourceType.ORDERED_LIST)) {
                return toListHTML(true);
            }
            
            if (this.type.equals(ResourceType.UNORDERED_LIST)) {
                return toListHTML(false);
            }

            String clazz = "text";
            switch (getType()) {
                case FINE -> {
                    clazz = "fine";
                }
                case WARNING -> {
                    clazz = "warning";
                }
                case SEVERE -> {
                    clazz = "severe";
                }
            }

            StringBuilder b = new StringBuilder();

            b.append("<p class=\"").append(clazz).append("\">\n");
            b.append(HTMLTranslator.escapeAndTranslate(TextBlock.getParagraphFormatted(getResource())).indent(4));
            b.append("</p>");

            return b.toString();
        }

    }

    public static class Section {

        private final String id;
        private final Article article;
        private final Section parent;
        private final String name;
        private final List<Section> children = new ArrayList<>();
        private final List<Resource> resources = new ArrayList<>();
        private final AtomicInteger subSectionIds = new AtomicInteger(1);

        public Section(Article article, Section parent, String name) {
            Objects.requireNonNull(article, "Article is null.");
            Objects.requireNonNull(name, "Name is null.");

            if (parent != null) {
                this.id = parent.getId() + "." + parent.requestSubSectionId();
            } else {
                this.id = Integer.toString(article.requestSectionId());
            }
            this.article = article;
            this.parent = parent;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public Article getArticle() {
            return article;
        }

        public Section getParent() {
            return parent;
        }

        public String getName() {
            return name;
        }

        public String getFullName() {
            return getId() + " " + getName();
        }

        public String getFullNameEncoded() {
            return URLEncoder.encode(getFullName(), StandardCharsets.UTF_8);
        }

        public List<Section> getChildren() {
            return children;
        }

        public List<Resource> getResources() {
            return resources;
        }

        public int requestSubSectionId() {
            return this.subSectionIds.getAndIncrement();
        }

        private String toHTML(int depth) {
            StringBuilder b = new StringBuilder();

            String tag = "h" + (2 + depth);

            b.append("<section id=\"").append(getFullNameEncoded()).append("\">\n");
            b.append(INDENT).append("<").append(tag).append(" class=\"").append(tag).append("\">").append(HTMLTranslator.escape(getFullName())).append("</").append(tag).append(">\n");
            for (Resource r : getResources()) {
                b.append(r.toHTML().indent(4));
            }
            for (Section s : getChildren()) {
                b.append(s.toHTML(depth + 1).indent(4));
            }
            b.append("</section>");

            return b.toString();
        }

        public String toHTML() {
            return toHTML(0);
        }
    }

    private final int id;
    private final String date;
    private final String title;
    private final String description;
    private final List<Section> sections = new ArrayList<>();
    private final AtomicInteger sectionsIds = new AtomicInteger(1);

    private String keywords = null;

    public Article(int id, String date, String title, String description) {
        Objects.requireNonNull(date, "Date is null.");
        Objects.requireNonNull(title, "Name is null.");
        Objects.requireNonNull(description, "Description is null.");

        this.id = id;
        this.date = date;
        this.title = title;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public String getDate() {
        return date;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getTitleEncoded() {
        return URLEncoder.encode(getTitle(), StandardCharsets.UTF_8);
    }

    public List<Section> getSections() {
        return sections;
    }

    public int requestSectionId() {
        return this.sectionsIds.getAndIncrement();
    }
    
    private String fullText(Section section) {
        StringBuilder b = new StringBuilder();
        
        for (Resource r:section.getResources()) {
            b.append(r.getResource()).append(' ');
        }
        
        for (Section s:section.getChildren()) {
            b.append(fullText(s)).append(' ');
        }
        
        return b.toString();
    }

    @SuppressWarnings("unchecked")
    public String getKeywords() {
        if (this.keywords != null) {
            return this.keywords;
        }
        StringBuilder builder = new StringBuilder();
        for (Section section : getSections()) {
            builder.append(fullText(section)).append(' ');
        }
        this.keywords = getKeywords(builder.toString());
        
        return this.keywords;
    }

    private String writeHead() {
        StringBuilder b = new StringBuilder();

        b.append("<head>\n");
        b.append(INDENT).append("<title>").append(HTMLTranslator.escape(getTitle())).append("</title>\n");
        b.append(INDENT).append("<meta charset=\"UTF-8\"/>\n");
        b.append(INDENT).append("<meta name=\"keywords\" content=\"").append(getKeywords()).append("\"/>\n");
        b.append(INDENT).append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/>\n");
        b.append(INDENT).append("<link rel=\"icon\" type=\"image/x-icon\" href=\"../resources/icon.png\"/>\n");
        b.append(INDENT).append("<link rel=\"stylesheet\" href=\"").append("../resources/style.css").append("\" type=\"text/css\"").append("/>\n");
        b.append(INDENT).append("\n");
        b.append(INDENT).append("<!-- OpenGraph -->\n");
        b.append(INDENT).append("<meta name=\"og:title\" content=\"").append(HTMLTranslator.escape(getTitle())).append("\"/>\n");
        b.append(INDENT).append("<meta name=\"og:description\" content=\"").append(HTMLTranslator.escape(getDescription())).append("\"/>\n");
        b.append(INDENT).append("<meta name=\"og:type\" content=\"article\"/>\n");
        b.append(INDENT).append("<meta name=\"og:image\" content=\"../resources/icon.png\"/>\n");
        b.append(INDENT).append("<!-- OpenGraph -->\n");
        b.append("</head>");

        return b.toString();
    }

    private String writeHeader() {
        StringBuilder b = new StringBuilder();

        b.append("<header class=\"header\">\n");
        b.append(INDENT).append("<h1>").append(HTMLTranslator.escape(getTitle())).append("</h1>\n");
        b.append(INDENT).append("<h2>").append(HTMLTranslator.escape(getDescription())).append("</h2>\n");
        b.append(INDENT).append("<h3>").append(String.format("%04d", getId())).append("</h3>\n");
        b.append(INDENT).append("<h3>").append(getDate()).append("</h3>\n");
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

    private String writeFooter() {
        StringBuilder b = new StringBuilder();

        b.append("<footer class=\"footer\">\n");
        b.append(INDENT).append("<p>").append("<a href=\"articles.html\">").append(HTMLTranslator.escape("<<< Return to Articles")).append("</a>").append("</p>\n");
        b.append(INDENT).append("<p>").append("Content on this website is released under the ").append("<a href=\"").append(HTMLTranslator.escape("https://creativecommons.org/publicdomain/zero/1.0/")).append("\">").append("CC0 License").append("</a>").append(" unless stated otherwise.").append("</p>\n");
        b.append("</footer>");

        return b.toString();
    }

    private String writeBody() {
        StringBuilder b = new StringBuilder();

        b.append("<body class=\"body\">\n");
        b.append(writeHeader().indent(4));
        b.append(writeArticle().indent(4));
        b.append(writeFooter().indent(4));
        b.append("</body>");

        return b.toString();
    }

    public String toHTML() {
        StringBuilder b = new StringBuilder();

        b.append("<!DOCTYPE html>\n");
        b.append("<!--\n");
        b.append("\n");
        b.append(LICENSE);
        b.append("\n\n");
        b.append("Title: ").append(HTMLTranslator.escape(getTitle())).append("\n");
        b.append("Id: ").append(getId()).append("\n");
        b.append("Date: ").append(getDate()).append("\n");
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
