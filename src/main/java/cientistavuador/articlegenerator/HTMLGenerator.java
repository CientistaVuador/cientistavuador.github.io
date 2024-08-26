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
import java.util.List;

/**
 *
 * @author Cien
 */
public class HTMLGenerator {

    public static final String LICENSE;

    static {
        try {
            LICENSE = Files.readString(Paths.get("resources", "license.txt"), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
    
    public static String generate(List<TextBlock> blocks) {
        StringBuilder b = new StringBuilder();

        b.append("<!--\n");
        b.append(LICENSE);
        b.append("\n-->\n");

        b.append("<!DOCTYPE html>\n");
        b.append("<html>\n");
        b.append(writeHead(blocks).indent(4));
        b.append(writeBody(blocks).indent(4));
        b.append("</html>\n");

        return b.toString();
    }

    private static String writeHead(List<TextBlock> blocks) {
        StringBuilder b = new StringBuilder();

        b.append("<head>\n");
        b.append(writeLink("stylesheet", "resources/style.css").indent(4));
        b.append("</head>");

        return b.toString();
    }
    
    private static String writeLink(String rel, String href) {
        StringBuilder b = new StringBuilder();
        
        b.append("</link rel=\"").append(rel).append("\" href=\"").append(href).append("\">");
        
        return b.toString();
    }
    
    private static String writeBody(List<TextBlock> blocks) {
        StringBuilder b = new StringBuilder();

        b.append("<body>\n");
        
        for (TextBlock block:blocks) {
            String e = null;
            switch (block.getName()) {
                case "title" -> {
                    e = writeTitle(block.getArgument());
                }
                case "section" -> {
                    e = writeSection(block.getArgument());
                }
                case "subsection" -> {
                    e = writeSubSection(block.getArgument());
                }
                case "text", "fine", "warning", "severe", "code" -> {
                    e = writeText(block.getArgument());
                }
            }
            if (e != null) {
                b.append(e.indent(4));
            }
        }
        
        b.append("</body>");

        return b.toString();
    }

    private static String writeTitle(String title) {
        StringBuilder b = new StringBuilder();

        b.append("<h1>").append(title).append("</h1>");

        return b.toString();
    }

    private static String writeSection(String section) {
        StringBuilder b = new StringBuilder();

        b.append("<h2>").append(section).append("</h2>");

        return b.toString();
    }
    
    private static String writeSubSection(String subSection) {
        StringBuilder b = new StringBuilder();
        
        b.append("<h3>").append(subSection).append("</h3>");
        
        return b.toString();
    }
    
    private static String writeText(String text) {
        StringBuilder b = new StringBuilder();
        
        b.append("<p>\n");
        b.append(text).append("\n");
        b.append("</p>");
        
        return b.toString();
    }
    
    private HTMLGenerator() {

    }
}
