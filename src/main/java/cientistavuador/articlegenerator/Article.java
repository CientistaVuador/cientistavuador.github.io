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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author Cien
 */
public class Article {

    public static Article fromTextBlocks(List<TextBlock> blocks) {
        Objects.requireNonNull(blocks, "Blocks is null.");

        if (blocks.size() < 3) {
            throw new RuntimeException("List must have at least 3 elements.");
        }

        TextBlock idBlock = blocks.get(0);
        TextBlock dateBlock = blocks.get(1);
        TextBlock titleBlock = blocks.get(2);

        if (!idBlock.getName().equals("id")) {
            throw new RuntimeException("First block must be the id.");
        }

        int id = Integer.parseInt(idBlock.getArgumentNotNull());

        if (!dateBlock.getName().equals("date")) {
            throw new RuntimeException("Second block must be the date.");
        }

        String date = dateBlock.getArgumentNotNull();

        if (!titleBlock.getName().equals("title")) {
            throw new RuntimeException("Third block must be the title.");
        }

        String title = titleBlock.getArgumentNotNull();

        Article article = new Article(id, date, title);

        List<TextBlock> sub = blocks.subList(3, blocks.size());

        Section parentSection = null;
        Section currentSection = null;

        for (TextBlock block : sub) {
            switch (block.getName()) {
                case "section" -> {
                    parentSection = new Section(article, null, block.getArgumentNotNull());
                    article.getSections().add(parentSection);
                    currentSection = parentSection;
                }
                case "subsection" -> {
                    if (parentSection == null) {
                        throw new RuntimeException("No parent section for subsection at line " + block.getLine());
                    }
                    Section child = new Section(article, parentSection, block.getArgumentNotNull());
                    parentSection.getChildren().add(child);
                    currentSection = child;
                }
                case "text", "image", "code", "fine", "warning", "severe" -> {
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
                    }
                    currentSection.getResources().add(new Resource(type, block.getArgumentNotNull()));
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
            LICENSE = Files.readString(Paths.get("resources", "license.txt"), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public static String escapeHTML(String text) {
        StringBuilder b = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            int unicode = text.codePointAt(i);
            switch (unicode) {
                case '&' -> {
                    b.append("&amp;");
                    continue;
                }
                case '<' -> {
                    b.append("&lt;");
                    continue;
                }
                case '>' -> {
                    b.append("&gt;");
                    continue;
                }
                case '"' -> {
                    b.append("&quot;");
                    continue;
                }
                case '\'' -> {
                    b.append("&#39;");
                    continue;
                }
            }
            b.appendCodePoint(unicode);
        }

        return b.toString();
    }
    
    public static enum ResourceType {
        TEXT, IMAGE, CODE, FINE, WARNING, SEVERE;
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

        public String toHTML() {
            if (this.type.equals(ResourceType.IMAGE)) {
                String altPlaceholder;
                {
                    String[] split = getResource().split("/");
                    altPlaceholder = split[split.length - 1].split(Pattern.quote("."))[0];
                }
                return "<img src=\"" + getResource() + "\" alt=\"" + altPlaceholder + "\"/>";
            }
            
            String clazz = "text";
            switch (getType()) {
                case IMAGE -> {
                    clazz = "image";
                }
                case CODE -> {
                    clazz = "code";
                }
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

            b.append("<div class=\"").append(clazz).append("\">");
            b.append("<p>");
            b.append(escapeHTML(getResource()));
            b.append("</p>");
            b.append("</div>");
            
            return b.toString();
        }

    }

    public static class Section {

        private final int id;
        private final Article article;
        private final Section parent;
        private final String name;
        private final List<Section> children = new ArrayList<>();
        private final List<Resource> resources = new ArrayList<>();

        public Section(Article article, Section parent, String name) {
            Objects.requireNonNull(article, "Article is null.");
            Objects.requireNonNull(name, "Name is null.");

            this.id = article.requestSectionId();
            this.article = article;
            this.parent = parent;
            this.name = name;
        }

        public int getId() {
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

        public List<Section> getChildren() {
            return children;
        }

        public List<Resource> getResources() {
            return resources;
        }

        private String toHTML(int depth) {
            StringBuilder b = new StringBuilder();

            String tag = "h" + (2 + depth);
            b.append("<section id=\"section_").append(getId()).append("\">\n");
            b.append("<").append(tag).append(">").append(escapeHTML(getName())).append("</").append(tag).append(">\n");
            for (Resource r : getResources()) {
                b.append(r.toHTML()).append("\n");
            }
            for (Section s : getChildren()) {
                b.append(s.toHTML(depth + 1)).append("\n");
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
    private final List<Section> sections = new ArrayList<>();
    private final AtomicInteger sectionsIds = new AtomicInteger();

    public Article(int id, String date, String title) {
        Objects.requireNonNull(date, "Date is null.");
        Objects.requireNonNull(title, "Name is null.");

        this.id = id;
        this.date = date;
        this.title = title;
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

    public List<Section> getSections() {
        return sections;
    }

    public int requestSectionId() {
        return this.sectionsIds.getAndIncrement();
    }

    public String toHTML() {
        StringBuilder b = new StringBuilder();

        b.append("<!DOCTYPE html>\n");
        b.append("<!--\n");
        b.append("\n");
        b.append(LICENSE);
        b.append("\n\n");
        b.append("Title: ").append(escapeHTML(getTitle())).append("\n");
        b.append("Id: ").append(getId()).append("\n");
        b.append("Date: ").append(getDate()).append("\n");
        b.append("-->\n");
        b.append("<html>\n");
        b.append(writeHead()).append("\n");
        b.append(writeBody()).append("\n");
        b.append("</html>");

        return b.toString();
    }

    private String writeHead() {
        StringBuilder b = new StringBuilder();

        b.append("<head>\n");
        b.append("<title>").append(escapeHTML(getTitle())).append("</title>\n");
        b.append("<link rel=\"stylesheet\" href=\"").append("../resources/style.css").append("\" type=\"text/css\"").append("/>\n");
        b.append("</head>");

        return b.toString();
    }

    private String writeHeader() {
        StringBuilder b = new StringBuilder();

        b.append("<header>\n");
        b.append("<h1>").append(escapeHTML(getTitle())).append("</h1>\n");
        b.append("<h3>").append(getId()).append("</h3>\n");
        b.append("<h3>").append(getDate()).append("</h3>\n");
        b.append("</header>");

        return b.toString();
    }

    private String writeSectionIndex(Section section, int depth) {
        StringBuilder b = new StringBuilder();

        b.append(" ".repeat(depth * 4)).append("<a href=\"#section_").append(section.getId()).append("\">").append(escapeHTML(section.getName())).append("</a>");
        if (!section.getChildren().isEmpty()) {
            b.append("\n");
            for (int i = 0; i < section.getChildren().size(); i++) {
                b.append(writeSectionIndex(section.getChildren().get(i), depth + 1));
                if (i != section.getChildren().size() - 1) {
                    b.append("\n");
                }
            }
        }
        
        return b.toString();
    }

    private String writeIndex() {
        StringBuilder b = new StringBuilder();

        b.append("<div class=\"index\">\n");
        for (Section e:getSections()) {
            b.append(writeSectionIndex(e, 0)).append("\n");
        }
        b.append("</div>");
        
        return b.toString();
    }

    private String writeBody() {
        StringBuilder b = new StringBuilder();

        b.append("<body>\n");
        b.append(writeHeader()).append("\n");
        b.append("<div class=\"articleBody\">\n");
        b.append(writeIndex()).append("\n");
        for (Section sec : getSections()) {
            b.append(sec.toHTML()).append("\n");
        }
        b.append("</div>\n");
        b.append("</body>");

        return b.toString();
    }

}
