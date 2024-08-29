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
        
        for (TextBlock block:sub) {
            switch (block.getName()) {
                case "section" -> {
                    parentSection = new Section(article, null, block.getArgumentNotNull());
                    article.getSections().add(parentSection);
                    currentSection = parentSection;
                }
                case "subsection" -> {
                    if (parentSection == null) {
                        throw new RuntimeException("No parent section for subsection at line "+block.getLine());
                    }
                    Section child = new Section(article, parentSection, block.getArgumentNotNull());
                    parentSection.getChildren().add(child);
                    currentSection = child;
                }
                case "text", "image", "code", "fine", "warning", "severe" -> {
                    if (currentSection == null) {
                        throw new RuntimeException("No parent section for block at line "+block.getLine());
                    }
                    ResourceType type = null;
                    switch (block.getName()) {
                        case "text" -> type = ResourceType.TEXT;
                        case "image" -> type = ResourceType.IMAGE;
                        case "code" -> type = ResourceType.CODE;
                        case "fine" -> type = ResourceType.FINE;
                        case "warning" -> type = ResourceType.WARNING;
                        case "severe" -> type = ResourceType.SEVERE;
                    }
                    currentSection.getResources().add(new Resource(type, block.getArgumentNotNull()));
                }
                default -> {
                    throw new RuntimeException("Unknown text block "+block.getName()+" at line "+block.getLine());
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
    
    public static String escapeHTML(String text, boolean escapeLineBreaks) {
        StringBuilder b = new StringBuilder();
        
        for (int i = 0; i < text.length(); i++) {
            int unicode = text.codePointAt(i);
            switch (unicode) {
                case '\n' -> {
                    if (escapeLineBreaks) {
                        b.append("<br/>");
                    }
                }
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
    
    public static String escapeHTML(String text) {
        return escapeHTML(text, true);
    }
    
    public static String generateHTMLIndent(int n) {
        return "\u0000".repeat(n);
    }
    
    public static String indentHTML(String text, int n) {
        if (text.isEmpty()) {
            return text;
        }
        String indent = generateHTMLIndent(n);
        return text.lines().map(s -> indent + s).collect(Collectors.joining("\n", "", "\n"));
    }
    
    public static String parseHTMLIndent(String text) {
        StringBuilder b = new StringBuilder();
        
        String prefix = "<!--";
        String suffix = "-->";
        int size = prefix.length() + suffix.length();
        
        boolean start = false;
        int spaces = 0;
        for (int i = 0; i < text.length(); i++) {
            int unicode = text.codePointAt(i);
            if (unicode == '\u0000') {
                if (!start) {
                    start = true;
                    spaces = -size;
                    b.append(prefix);
                }
                if (spaces >= 0) {
                    b.append(' ');
                }
                spaces++;
                continue;
            }
            if (start) {
                b.append(suffix);
                start = false;
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
                return "<img src=\""+getResource()+"\" alt=\""+altPlaceholder+"\"/>";
            }
            
            String tag = "p";
            if (this.type.equals(ResourceType.CODE)) {
                tag = "pre";
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
            
            b.append("<").append(tag);
            if (!getType().equals(ResourceType.CODE)) {
                b.append(" class=\"").append(clazz).append("\"");
            }
            b.append(">\n");
            b.append(escapeHTML(getResource(), !this.type.equals(ResourceType.CODE)));
            b.append("\n</").append(tag).append(">");
            
            return b.toString();
        }
        
    }
    
    public static class Section {

        private final Article article;
        private final Section parent;
        private final String name;
        private final List<Section> children = new ArrayList<>();
        private final List<Resource> resources = new ArrayList<>();

        public Section(Article article, Section parent, String name) {
            Objects.requireNonNull(article, "Article is null.");
            Objects.requireNonNull(name, "Name is null.");
            
            this.article = article;
            this.parent = parent;
            this.name = name;
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
            b.append("<section>\n");
            b.append(generateHTMLIndent(4)).append("<").append(tag).append(">").append(escapeHTML(getName())).append("</").append(tag).append(">\n");
            for (Resource r:getResources()) {
                b.append(indentHTML(r.toHTML(), 4));
            }
            for (Section s:getChildren()) {
                b.append(indentHTML(s.toHTML(depth + 1), 4));
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
        b.append(indentHTML(writeHead(), 4));
        b.append(indentHTML(writeBody(), 4));
        b.append("</html>");
        
        return parseHTMLIndent(b.toString());
    }
    
    private String writeHead() {
        StringBuilder b = new StringBuilder();
        
        String indent = generateHTMLIndent(4);
        b.append("<head>\n");
        b.append(indent).append("<title>").append(escapeHTML(getTitle())).append("</title>\n");
        b.append(indent).append("<link rel=\"stylesheet\" href=\"").append("../resources/style.css").append("\" type=\"text/css\"").append("/>\n");
        b.append("</head>");
        
        return b.toString();
    }
    
    private String writeHeader() {
        StringBuilder b = new StringBuilder();
        
        String indent = generateHTMLIndent(4);
        b.append("<header>\n");
        b.append(indent).append("<h1>").append(escapeHTML(getTitle())).append("</h1>\n");
        b.append(indent).append("<h4>").append(getId()).append("</h4>\n");
        b.append(indent).append("<h4>").append(getDate()).append("</h4>\n");
        b.append("</header>");
        
        return b.toString();
    }
    
    private String writeBody() {
        StringBuilder b = new StringBuilder();
        
        b.append("<body>\n");
        b.append(indentHTML(writeHeader(), 4));
        for (Section sec:getSections()) {
            b.append(indentHTML(sec.toHTML(), 4));
        }
        b.append("</body>");
        
        return b.toString();
    }
    
}
