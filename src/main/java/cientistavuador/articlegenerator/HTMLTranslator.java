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

/**
 *
 * @author Cien
 */
public class HTMLTranslator {

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

    public static String escapeAndTranslate(String text) {
        text = escape(text);

        StringBuilder b = new StringBuilder();

        boolean escape = false;

        boolean underlineOpen = false;
        boolean boldOpen = false;
        boolean crossedOpen = false;
        boolean codeOpen = false;
        boolean italicOpen = false;

        for (int i = 0; i < text.length(); i++) {
            int lastLastUnicode = 0;
            if (i >= 2) {
                lastLastUnicode = text.codePointAt(i - 2);
            }
            int lastUnicode = 0;
            if (i >= 1) {
                lastUnicode = text.codePointAt(i - 1);
            }
            int unicode = text.codePointAt(i);

            if (escape) {
                escape = false;
                b.appendCodePoint(unicode);
                continue;
            }

            if (unicode == '\\') {
                int nextUnicode = 0;
                if (i < (text.length() - 1)) {
                    nextUnicode = text.codePointAt(i + 1);
                }
                switch (nextUnicode) {
                    case '/', '*', '`', '~', '_', '[', ':', ']' -> {
                        escape = true;
                        continue;
                    }
                }
            }

            if (unicode == lastUnicode && unicode == lastLastUnicode) {
                switch (unicode) {
                    case '/' -> {
                        b.setLength(b.length() - 2);
                        italicOpen = !italicOpen;
                        if (italicOpen) {
                            b.append("<i>");
                        } else {
                            b.append("</i>");
                        }
                        continue;
                    }
                    case '*' -> {
                        b.setLength(b.length() - 2);
                        boldOpen = !boldOpen;
                        if (boldOpen) {
                            b.append("<strong>");
                        } else {
                            b.append("</strong>");
                        }
                        continue;
                    }
                    case '`' -> {
                        b.setLength(b.length() - 2);
                        codeOpen = !codeOpen;
                        if (codeOpen) {
                            b.append("<code>");
                        } else {
                            b.append("</code>");
                        }
                        continue;
                    }
                    case '~' -> {
                        b.setLength(b.length() - 2);
                        crossedOpen = !crossedOpen;
                        if (crossedOpen) {
                            b.append("<del>");
                        } else {
                            b.append("</del>");
                        }
                        continue;
                    }
                    case '_' -> {
                        b.setLength(b.length() - 2);
                        underlineOpen = !underlineOpen;
                        if (underlineOpen) {
                            b.append("<u>");
                        } else {
                            b.append("</u>");
                        }
                        continue;
                    }
                    case '[', ']', ':' -> {
                        b.setLength(b.length() - 2);
                        switch (unicode) {
                            case '[' -> {
                                b.append("<a target=\"_blank\" href=\"");
                            }
                            case ':' -> {
                                b.append("\">");
                            }
                            case ']' -> {
                                b.append("</a>");
                            }
                        }
                        continue;
                    }
                }
            }

            b.appendCodePoint(unicode);
        }

        return b.toString();
    }

    private HTMLTranslator() {

    }
}
