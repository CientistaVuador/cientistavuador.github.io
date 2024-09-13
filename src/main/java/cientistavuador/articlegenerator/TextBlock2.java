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

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Cien
 */
public class TextBlock2 {

    static class CharStream {

        String data;
        int index;
        int currentLine;
    }

    private static boolean readWhiteSpacesOrComments(CharStream stream) {
        boolean commentOpen = false;

        for (; stream.index < stream.data.length(); stream.index++) {
            int unicode = stream.data.codePointAt(stream.index);
            if (unicode == '\n') {
                stream.currentLine++;
            }
            if (Character.charCount(unicode) == 2) {
                stream.index++;
            }

            if (commentOpen) {
                if (unicode == '-'
                        && (stream.index < stream.data.length() - 2)
                        && stream.data.codePointAt(stream.index + 1) == '>'
                        && stream.data.codePointAt(stream.index + 2) == '/') {
                    stream.index += 2;
                    commentOpen = false;
                }
                continue;
            }

            if (unicode == '/'
                    && (stream.index < stream.data.length() - 2)
                    && stream.data.codePointAt(stream.index + 1) == '<'
                    && stream.data.codePointAt(stream.index + 2) == '-') {
                stream.index += 2;
                commentOpen = true;
                continue;
            }

            if (!Character.isWhitespace(unicode)) {
                stream.index--;
                if (Character.charCount(unicode) == 2) {
                    stream.index--;
                }
                return false;
            }
        }

        if (commentOpen) {
            throw new IllegalArgumentException("Comment not closed after end was reached.");
        }

        return true;
    }

    private static String readBlockName(CharStream stream) {
        StringBuilder b = new StringBuilder();

        for (; stream.index < stream.data.length(); stream.index++) {
            int unicode = stream.data.codePointAt(stream.index);
            if (unicode == '\n') {
                stream.currentLine++;
            }
            if (Character.charCount(unicode) == 2) {
                stream.index++;
            }

            if (Character.isWhitespace(unicode)) {
                stream.index--;
                if (Character.charCount(unicode) == 2) {
                    stream.index--;
                }
                break;
            }

            switch (unicode) {
                case '[', ']', '<', '>', ';', '/' -> {
                    stream.index--;
                    break;
                }
            }

            b.appendCodePoint(unicode);
        }

        if (b.length() == 0) {
            throw new IllegalArgumentException("Empty block name at line " + stream.currentLine);
        }

        return b.toString().toLowerCase();
    }
    
    private static String readAttribute(CharStream stream) {
        StringBuilder b = new StringBuilder();
        
        for (; stream.index < stream.data.length(); stream.index++) {
            int unicode = stream.data.codePointAt(stream.index);
            if (unicode == '\n') {
                stream.currentLine++;
            }
            if (Character.charCount(unicode) == 2) {
                stream.index++;
            }

            switch (unicode) {
                case '<', '>' -> {
                    if ((stream.index < stream.data.length() - 1) && stream.data.codePointAt(stream.index + 1) == unicode) {
                        b.appendCodePoint(unicode);
                        stream.index++;
                        continue;
                    }
                    switch (unicode) {
                        case '<' -> {
                            throw new IllegalArgumentException("Invalid attribute opening in line "+stream.currentLine);
                        }
                        case '>' -> {
                            return b.toString();
                        }
                    }
                }
            }
            
            b.appendCodePoint(unicode);
        }
        
        throw new IllegalArgumentException("Attribute not closed at line "+stream.currentLine);
    }

    public static List<TextBlock2> parse(String text) {
        List<TextBlock2> blocks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return blocks;
        }

        CharStream stream = new CharStream();
        stream.data = text;
        stream.index = 0;
        stream.currentLine = 1;

        int stage = 0;

        String blockName = null;
        String blockAttribute = null;

        for (; stream.index < text.length(); stream.index++) {
            int unicode = text.codePointAt(stream.index);
            if (unicode == '\n') {
                stream.currentLine++;
            }
            if (Character.charCount(unicode) == 2) {
                stream.index++;
            }

            if (stage == 0) {
                if (readWhiteSpacesOrComments(stream)) {
                    break;
                }
                stage = 1;
                continue;
            }

            if (stage == 1) {
                if (unicode == '[') {
                    if (readWhiteSpacesOrComments(stream)) {
                        throw new IllegalArgumentException("Unexpected end found after block was open.");
                    }
                } else {
                    throw new IllegalArgumentException("Expected '[' at line " + stream.currentLine);
                }
                stage = 2;
                continue;
            }

            if (stage == 2) {
                blockName = readBlockName(stream);
                stage = 3;
                continue;
            }

            if (stage == 3) {
                if (readWhiteSpacesOrComments(stream)) {
                    throw new IllegalArgumentException("Unexpected end found after block name was read.");
                }
                stage = 4;
                continue;
            }

            if (stage == 4) {
                switch (unicode) {
                    case '<' -> {
                        stream.index++;
                        blockAttribute = readAttribute(stream);
                        stage = 5;
                        continue;
                    }
                    case ';' -> {
                        
                    }
                    case ']' -> {
                        
                    }
                    default -> {
                        throw new IllegalArgumentException("Illegal character after block name in line " + stream.currentLine);
                    }
                }
                continue;
            }
            
            if (stage == 5) {
                
            }
        }

        if (stage == 4) {
            throw new IllegalArgumentException("Unexpected end found after block name was read.");
        }

        return blocks;
    }

    private final String name;
    private final String attribute;
    private final String rawText;

    public TextBlock2(String name, String attribute, String rawText) {
        this.name = name;
        this.attribute = attribute;
        this.rawText = rawText;
    }

}
