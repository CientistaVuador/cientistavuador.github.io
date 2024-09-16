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
public class TextBlock {

    static class CharStream {

        String data;
        int index;
        int currentLine;
        final StringBuilder builder = new StringBuilder();
    }

    private static boolean readWhiteSpaces(CharStream stream) {
        boolean readingComment = false;

        for (; stream.index < stream.data.length();) {
            int unicode = stream.data.codePointAt(stream.index);
            if (unicode == '\n') {
                stream.currentLine++;
            }
            if (readingComment) {
                if (unicode == '-'
                        && (stream.index < stream.data.length() - 2)
                        && stream.data.codePointAt(stream.index + 1) == '>'
                        && stream.data.codePointAt(stream.index + 2) == '/') {
                    stream.index += 2;
                    readingComment = false;
                }
                stream.index += Character.charCount(unicode);
                continue;
            }
            if (Character.isWhitespace(unicode)) {
                stream.index += Character.charCount(unicode);
                continue;
            }
            if (unicode == '/'
                    && (stream.index < stream.data.length() - 2)
                    && stream.data.codePointAt(stream.index + 1) == '<'
                    && stream.data.codePointAt(stream.index + 2) == '-') {
                stream.index += Character.charCount(unicode) + 2;
                readingComment = true;
                continue;
            }
            return false;
        }

        if (readingComment) {
            throw new IllegalArgumentException("Unclosed comment after end was reached.");
        }

        return true;
    }

    private static String readBlockName(CharStream stream) {
        stream.builder.setLength(0);

        for (; stream.index < stream.data.length();) {
            int unicode = stream.data.codePointAt(stream.index);
            if (unicode == '\n') {
                stream.currentLine++;
            }
            boolean specialCharFound = false;
            switch (unicode) {
                case '[', ']', '<', '>', ';', '/' -> {
                    specialCharFound = true;
                }
            }
            if (specialCharFound || Character.isWhitespace(unicode)) {
                String blockName = stream.builder.toString().toLowerCase();
                if (blockName.isEmpty()) {
                    throw new IllegalArgumentException("Empty block name at " + stream.currentLine);
                }
                return blockName;
            }
            stream.builder.appendCodePoint(unicode);
            stream.index += Character.charCount(unicode);
        }

        throw new IllegalArgumentException("Unexpected end found after reading block name at line " + stream.currentLine);
    }
    
    private static String readAttribute(CharStream stream) {
        stream.builder.setLength(0);

        for (; stream.index < stream.data.length();) {
            int unicode = stream.data.codePointAt(stream.index);
            if (unicode == '\n') {
                stream.currentLine++;
            }
            switch (unicode) {
                case '<' -> {
                    if (stream.index < stream.data.length() - 1 && stream.data.codePointAt(stream.index + 1) == '<') {
                        stream.index++;
                    } else {
                        throw new IllegalArgumentException("Unescaped '<' inside attribute at line " + stream.currentLine);
                    }
                }
                case '>' -> {
                    stream.index++;
                    if (stream.index >= stream.data.length() || stream.data.codePointAt(stream.index) != '>') {
                        return stream.builder.toString();
                    }
                }
            }
            stream.builder.appendCodePoint(unicode);
            stream.index += Character.charCount(unicode);
        }

        throw new IllegalArgumentException("Unexpected end found, expected '>' at line " + stream.currentLine);
    }

    private static final int[] sequenceChars = "[;<;>]".codePoints().toArray();

    private static String readRawText(CharStream stream) {
        stream.builder.setLength(0);

        int sequenceCounter = 0;

        for (; stream.index < stream.data.length();) {
            int unicode = stream.data.codePointAt(stream.index);
            if (unicode == '\n') {
                stream.currentLine++;
            }
            if (unicode == sequenceChars[sequenceCounter]) {
                sequenceCounter++;
                if (sequenceCounter == sequenceChars.length) {
                    if ((stream.index < stream.data.length() - 1)
                            && stream.data.codePointAt(stream.index + 1) == ';') {
                        stream.builder.appendCodePoint(unicode);
                        stream.index += Character.charCount(unicode) + 1;
                        sequenceCounter = 0;
                        continue;
                    }
                    stream.index += Character.charCount(unicode);
                    stream.builder.setLength(stream.builder.length() - (sequenceChars.length - 1));
                    return stream.builder.toString();
                }
            } else {
                sequenceCounter = 0;
            }
            stream.builder.appendCodePoint(unicode);
            stream.index += Character.charCount(unicode);
        }

        throw new IllegalArgumentException("Unexpected end found, expected '[;<;>]' at line " + stream.currentLine);
    }
    
    private static String readInlineRawText(CharStream stream) {
        stream.builder.setLength(0);
        
        int sequenceCounter = 0;

        for (; stream.index < stream.data.length();) {
            int unicode = stream.data.codePointAt(stream.index);
            
            if (unicode == sequenceChars[sequenceCounter]) {
                sequenceCounter++;
                if (sequenceCounter == sequenceChars.length) {
                    if ((stream.index < stream.data.length() - 1)
                            && stream.data.codePointAt(stream.index + 1) == ';') {
                        stream.builder.appendCodePoint(unicode);
                        stream.index += Character.charCount(unicode) + 1;
                        sequenceCounter = 0;
                        continue;
                    }
                    throw new IllegalArgumentException("Unescaped illegal sequence '[;<;>]' in inline block at line " + stream.currentLine);
                }
            } else {
                sequenceCounter = 0;
            }
            
            if (unicode == '\n' || (
                    unicode == '\r' 
                    && stream.index < stream.data.length() - 1 
                    && stream.data.codePointAt(stream.index + 1) == '\n')) {
                stream.currentLine++;
                stream.index++;
                if (unicode == '\r') {
                    stream.index++;
                }
                break;
            }
            
            stream.builder.appendCodePoint(unicode);
            stream.index += Character.charCount(unicode);
        }
        
        return stream.builder.toString();
    }

    public static List<TextBlock> parse(String text) {
        List<TextBlock> blocks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return blocks;
        }

        CharStream stream = new CharStream();
        stream.data = text;
        stream.index = 0;
        stream.currentLine = 1;

        for (; stream.index < text.length();) {
            if (readWhiteSpaces(stream)) {
                break;
            }

            if (stream.data.codePointAt(stream.index) == '[') {
                int blockLine = stream.currentLine;
                
                boolean inlineBlock = false;
                if (stream.index < stream.data.length() - 1 && stream.data.codePointAt(stream.index + 1) == '[') {
                    inlineBlock = true;
                    stream.index++;
                }
                
                stream.index++;
                if (readWhiteSpaces(stream)) {
                    throw new IllegalArgumentException("Unexpected end found after block was open at line " + stream.currentLine);
                }
                
                String blockName = readBlockName(stream);
                if (readWhiteSpaces(stream)) {
                    throw new IllegalArgumentException("Unexpected end found after reading block name at line " + stream.currentLine);
                }
                
                String blockAttribute = "";
                if (stream.data.codePointAt(stream.index) == '<') {
                    stream.index++;
                    blockAttribute = readAttribute(stream);
                    if (readWhiteSpaces(stream)) {
                        throw new IllegalArgumentException("Unexpected end found after reading attribute at line " + stream.currentLine);
                    }
                }
                
                boolean closedBlock = false;
                if (stream.data.codePointAt(stream.index) == ';') {
                    stream.index++;
                    closedBlock = true;
                    if (readWhiteSpaces(stream)) {
                        throw new IllegalArgumentException("Unexpected end found after ';' in line " + stream.currentLine);
                    }
                }
                
                if (stream.data.codePointAt(stream.index) != ']') {
                    throw new IllegalArgumentException("Illegal character '"+Character.toString(stream.data.codePointAt(stream.index))+"' found, expected a ']' at line " + stream.currentLine);
                }
                stream.index++;
                if (inlineBlock) {
                    if (stream.index >= stream.data.length()) {
                        throw new IllegalArgumentException("Unexpected end found after ']' expected another ']' due to inline block in line " + stream.currentLine);
                    }
                    if (stream.data.codePointAt(stream.index) != ']') {
                        throw new IllegalArgumentException("Illegal character '"+Character.toString(stream.data.codePointAt(stream.index))+"' found, expected a ']' due to inline block at line " + stream.currentLine);
                    }
                    stream.index++;
                }
                
                String rawText = "";
                if (!closedBlock) {
                    if (inlineBlock) {
                        rawText = readInlineRawText(stream);
                    } else {
                        rawText = readRawText(stream);
                    }
                }
                
                blocks.add(new TextBlock(blockLine, blockName, blockAttribute, rawText));
            } else {
                throw new IllegalArgumentException("Illegal character '"+Character.toString(stream.data.codePointAt(stream.index))+"' found, expected a '[' at line " + stream.currentLine);
            }
        }
        return blocks;
    }
    
    private final int line;
    private final String name;
    private final String attribute;
    private final String rawText;
    
    private String titleFormatted = null;
    private Integer integerFormatted = null;
    private String paragraphFormatted = null;
    private String codeFormatted = null;
    private String[] listFormatted = null;
    private ISOLanguage[] languageListFormatted = null;
    private CSV csvFormatted = null;
    
    private ISOLanguage attributeLanguage;

    private TextBlock(int line, String name, String attribute, String rawText) {
        this.line = line;
        this.name = name;
        this.attribute = attribute;
        this.rawText = rawText;
    }

    public int getLine() {
        return line;
    }

    public String getName() {
        return name;
    }

    public String getAttribute() {
        return attribute;
    }
    
    public boolean hasAttribute() {
        return !getAttribute().isEmpty();
    }
    
    public ISOLanguage getAttributeLanguage() {
        if (this.attributeLanguage == null) {
            try {
                this.attributeLanguage = TextFormatting.getISOLanguageFormatted(getAttribute());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid ISO Language in line "+this.line, ex);
            }
        }
        return this.attributeLanguage;
    }

    public String getRawText() {
        return rawText;
    }
    
    public String getTitleFormatted() {
        if (this.titleFormatted == null) {
            this.titleFormatted = TextFormatting.getTitleFormatted(this.rawText);
        }
        return this.titleFormatted;
    }

    public int getIntegerFormatted() {
        if (this.integerFormatted == null) {
            try {
                this.integerFormatted = Integer.valueOf(getTitleFormatted());
            } catch (NumberFormatException ex) {
                throw new NumberFormatException("Invalid Integer Number at line " + getLine());
            }
        }
        return this.integerFormatted;
    }

    public String getParagraphFormatted() {
        if (this.paragraphFormatted == null) {
            this.paragraphFormatted = TextFormatting.getParagraphFormatted(this.rawText);
        }
        return this.paragraphFormatted;
    }

    public String getCodeFormatted() {
        if (this.codeFormatted == null) {
            this.codeFormatted = TextFormatting.getCodeFormatted(this.rawText);
        }
        return this.codeFormatted;
    }

    public String[] getListFormatted() {
        if (this.listFormatted == null) {
            this.listFormatted = TextFormatting.getListFormatted(this.rawText);
        }
        return this.listFormatted.clone();
    }

    public ISOLanguage[] getLanguageListFormatted() {
        if (this.languageListFormatted == null) {
            try {
                this.languageListFormatted = TextFormatting.getISOLanguagesFormatted(this.rawText);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid ISO Language in block line "+this.line, ex);
            }
        }
        return this.languageListFormatted.clone();
    }
    
    public CSV getCSVFormatted() {
        if (this.csvFormatted == null) {
            try {
                this.csvFormatted = CSV.parse(getCodeFormatted());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid CSV in block line "+this.line, ex);
            }
        }
        return this.csvFormatted;
    }
    
}
