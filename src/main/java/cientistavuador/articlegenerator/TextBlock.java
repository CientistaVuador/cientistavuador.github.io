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

    public static boolean containsWhiteSpaces(String s) {
        
        return false;
    }
    
    private static String formatAndValidateBlockName(String blockName, int line) {
        blockName = blockName.trim().toLowerCase();
        for (int i = 0; i < blockName.length(); i++) {
            if (Character.isWhitespace(blockName.codePointAt(i))) {
                throw new IllegalArgumentException("Block name contains white spaces at line " + line);
            }
        }
        if (blockName.isEmpty()) {
            throw new IllegalArgumentException("Block name is empty at line " + line);
        }
        
        return blockName;
    }

    public static List<TextBlock> parse(String text) {
        List<TextBlock> blocks = new ArrayList<>();

        StringBuilder b = new StringBuilder();

        int line = 1;

        boolean blockOpen = false;
        boolean attributeOpen = false;

        int sequenceCounter = 0;
        boolean rawTextMode = false;

        String blockName = null;
        String blockAttribute = null;

        for (int i = 0; i < text.length(); i++) {
            int unicode = text.codePointAt(i);
            if (unicode == '\n') {
                line++;
            }

            if (rawTextMode) {
                switch (unicode) {
                    case '[' -> {
                        if (sequenceCounter == 0) {
                            sequenceCounter++;
                        } else {
                            sequenceCounter = 0;
                        }
                    }
                    case ';' -> {
                        if (sequenceCounter == 1 || sequenceCounter == 3) {
                            sequenceCounter++;
                        } else {
                            sequenceCounter = 0;
                        }
                    }
                    case '<' -> {
                        if (sequenceCounter == 2) {
                            sequenceCounter++;
                        } else {
                            sequenceCounter = 0;
                        }
                    }
                    case '>' -> {
                        if (sequenceCounter == 4) {
                            sequenceCounter++;
                        } else {
                            sequenceCounter = 0;
                        }
                    }
                    case ']' -> {
                        if (sequenceCounter == 5) {
                            sequenceCounter++;
                        } else {
                            sequenceCounter = 0;
                        }
                    }
                    default -> {
                        sequenceCounter = 0;
                    }
                }
                b.appendCodePoint(unicode);
                if (sequenceCounter == 6) {
                    int nextUnicode = 0;
                    if (i < (text.length() - 1)) {
                        nextUnicode = text.codePointAt(i + 1);
                    }
                    if (nextUnicode == ';') {
                        sequenceCounter = 0;
                        i++;
                        continue;
                    }

                    b.setLength(b.length() - 6);

                    if (blockAttribute == null) {
                        blockAttribute = "";
                    }
                    blocks.add(new TextBlock(blockName, blockAttribute, b.toString(), line));

                    b.setLength(0);
                    sequenceCounter = 0;
                    rawTextMode = false;

                    blockName = null;
                    blockAttribute = null;
                }
                continue;
            }
            
            if (attributeOpen) {
                switch (unicode) {
                    case '<', '>' -> {
                        int nextUnicode = 0;
                        if (i < (text.length() - 1)) {
                            nextUnicode = text.codePointAt(i + 1);
                        }
                        if (unicode == nextUnicode) {
                            i++;
                            b.appendCodePoint(unicode);
                            continue;
                        }
                    }
                    default -> {
                        b.appendCodePoint(unicode);
                        continue;
                    }
                }
            }

            switch (unicode) {
                case ';' -> {
                    throw new IllegalArgumentException("Invalid ; at line " + line);
                }
                case '\\' -> {
                    throw new IllegalArgumentException("Invalid \\ at line " + line);
                }
                case '[' -> {
                    if (blockOpen) {
                        throw new IllegalArgumentException("Block already open at line " + line);
                    }
                    blockOpen = true;
                    continue;
                }
                case ']' -> {
                    if (!blockOpen) {
                        throw new IllegalArgumentException("Block not open at line " + line);
                    }
                    if (attributeOpen) {
                        throw new IllegalArgumentException("Attribute not closed at line " + line);
                    }
                    blockOpen = false;
                    rawTextMode = true;
                    if (blockName == null) {
                        blockName = formatAndValidateBlockName(b.toString(), line);
                    }
                    b.setLength(0);
                    continue;
                }
                case '<' -> {
                    if (!blockOpen) {
                        throw new IllegalArgumentException("Block not open for attribute at line " + line);
                    }
                    if (attributeOpen) {
                        throw new IllegalArgumentException("Attribute already open at line " + line);
                    }
                    if (blockAttribute != null) {
                        throw new IllegalArgumentException("Attribute already defined at line " + line);
                    }
                    attributeOpen = true;
                    blockName = formatAndValidateBlockName(b.toString(), line);
                    b.setLength(0);
                    continue;
                }
                case '>' -> {
                    if (!attributeOpen) {
                        throw new IllegalArgumentException("Attribute not open at line " + line);
                    }
                    attributeOpen = false;
                    blockAttribute = b.toString();
                    b.setLength(0);
                    continue;
                }
            }

            if (blockOpen) {
                b.appendCodePoint(unicode);
            } else if (!Character.isWhitespace(unicode)) {
                throw new IllegalArgumentException("Invalid character at line " + line);
            }
        }
        if (blockOpen) {
            if (attributeOpen) {
                throw new IllegalArgumentException("Unclosed attribute at line " + line);
            } else {
                throw new IllegalArgumentException("Unclosed block at line " + line);
            }
        }

        return blocks;
    }
    
    private final String name;
    private final String attribute;
    private final String rawText;
    private final int line;

    private String titleFormatted = null;
    private Integer integerFormatted = null;
    private String paragraphFormatted = null;
    private String codeFormatted = null;
    private String[] listFormatted = null;
    private ISOLanguage[] languageListFormatted = null;
    
    private String attributeLowerCase;
    private ISOLanguage attributeLanguage;

    private TextBlock(String name, String attribute, String rawText, int line) {
        this.name = name;
        this.attribute = attribute;
        this.rawText = rawText;
        this.line = line;
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

    public int getLine() {
        return line;
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
    
}
