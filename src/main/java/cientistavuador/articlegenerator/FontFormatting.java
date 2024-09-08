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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 *
 * @author Cien
 */
public class FontFormatting {

    public static String escapeComment(String text) {
        return text.replace("--", "- ");
    }
    
    public static String escape(String text) {
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
    
    public static String escapeAndFormat(String s) {
        return escapeAndFormat(s, false);
    }
    
    public static String escapeAndFormat(String s, boolean ignoreMode) {
        Objects.requireNonNull(s, "String is null.");
        s = escape(s);
        
        StringBuilder b = new StringBuilder();

        boolean escape = false;

        enum Formatting {
            ITALIC("em"), BOLD("strong"), CODE("code"), CROSSED("del"), UNDERLINE("u"), LINK("a");

            final String tag;

            private Formatting(String tag) {
                this.tag = tag;
            }
        }
        
        Deque<Formatting> openClose = new ArrayDeque<>();
        boolean linkParseMode = false;
        
        for (int i = 0; i < s.length(); i++) {
            int unicode = s.codePointAt(i);

            if (escape) {
                switch (unicode) {
                    case '\\' -> {
                        b.append('\\');
                    }
                    case '/', '*', '`', '~', '_', '[', ':', ']' -> {
                        int nextUnicode = 0;
                        int nextNextUnicode = 0;
                        if (i < (s.length() - 2)) {
                            nextUnicode = s.codePointAt(i + 1);
                            nextNextUnicode = s.codePointAt(i + 2);
                        }
                        if (unicode == nextUnicode && unicode == nextNextUnicode) {
                            i += 2;
                            b.appendCodePoint(unicode).appendCodePoint(unicode).appendCodePoint(unicode);
                        } else {
                            b.append('\\').appendCodePoint(unicode);
                        }
                    }
                    default -> {
                        b.append('\\').appendCodePoint(unicode);
                    }
                }
                escape = false;
                continue;
            }

            if (unicode == '\\') {
                escape = true;
                continue;
            }

            parse:
            {
                switch (unicode) {
                    case '/', '*', '`', '~', '_', '[', ':', ']' -> {

                    }
                    default -> {
                        break parse;
                    }
                }

                int nextUnicode = 0;
                int nextNextUnicode = 0;
                if (i < (s.length() - 2)) {
                    nextUnicode = s.codePointAt(i + 1);
                    nextNextUnicode = s.codePointAt(i + 2);
                }
                if (unicode != nextUnicode || unicode != nextNextUnicode) {
                    break parse;
                }
                i += 2;
                
                if (linkParseMode && unicode != ':') {
                    continue;
                }

                if (unicode == '[' || unicode == ':' || unicode == ']') {
                    switch (unicode) {
                        case '[' -> {
                            linkParseMode = true;
                            if (!ignoreMode) {
                                b.append("<a target=\"_blank\" href=\"");
                            }
                        }
                        case ':' -> {
                            if (linkParseMode) {
                                if (!ignoreMode) {
                                    b.append("\">");
                                    openClose.add(Formatting.LINK);
                                }
                                linkParseMode = false;
                            }
                        }
                        case ']' -> {
                            if (openClose.contains(Formatting.LINK)) {
                                Formatting f;
                                do {
                                    f = openClose.pollLast();
                                    b.append("</").append(f.tag).append(">");
                                } while (!f.equals(Formatting.LINK));
                            }
                        }
                    }
                    continue;
                }
                
                if (ignoreMode) {
                    continue;
                }

                Formatting formatting = null;
                switch (unicode) {
                    case '/' ->
                        formatting = Formatting.ITALIC;
                    case '*' ->
                        formatting = Formatting.BOLD;
                    case '`' ->
                        formatting = Formatting.CODE;
                    case '~' ->
                        formatting = Formatting.CROSSED;
                    case '_' ->
                        formatting = Formatting.UNDERLINE;
                }

                if (formatting == null) {
                    continue;
                }

                if (openClose.contains(formatting)) {
                    Formatting f;
                    do {
                        f = openClose.pollLast();
                        b.append("</").append(f.tag).append(">");
                    } while (!f.equals(formatting));
                } else {
                    openClose.add(formatting);
                    b.append("<").append(formatting.tag).append(">");
                }

                continue;
            }
            
            if (linkParseMode && ignoreMode) {
                continue;
            }
            b.appendCodePoint(unicode);
        }
        if (ignoreMode) {
            linkParseMode = false;
        }
        
        if (escape) {
            b.append('\\');
        }
        if (linkParseMode) {
            b.append("\"></a>");
        }
        Formatting f;
        while ((f = openClose.pollLast()) != null) {
            b.append("</").append(f.tag).append(">");
        }

        return b.toString();
    }

    private FontFormatting() {

    }
}
