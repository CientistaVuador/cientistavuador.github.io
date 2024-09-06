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
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author Cien
 */
public class TextBlock {

    private static String formatBlockNameOrAttribute(String blockName) {
        return blockName
                .lines()
                .map(s -> s.trim())
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(" "));
    }

    private static String formatAndValidateBlockName(String blockName, int line) {
        blockName = formatBlockNameOrAttribute(blockName).toLowerCase();
        for (int j = 0; j < blockName.length(); j++) {
            if (Character.isWhitespace(blockName.codePointAt(j))) {
                throw new RuntimeException("Block name contains white spaces at line " + line);
            }
        }
        if (blockName.isEmpty()) {
            throw new RuntimeException("Block name is empty at line " + line);
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

            switch (unicode) {
                case ';' -> {
                    throw new RuntimeException("Invalid ; at line " + line);
                }
                case '\\' -> {
                    throw new RuntimeException("Invalid \\ at line " + line);
                }
                case '[' -> {
                    if (blockOpen) {
                        throw new RuntimeException("Block already open at line " + line);
                    }
                    blockOpen = true;
                    continue;
                }
                case ']' -> {
                    if (!blockOpen) {
                        throw new RuntimeException("Block not open at line " + line);
                    }
                    if (attributeOpen) {
                        throw new RuntimeException("Attribute not closed at line " + line);
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
                    if (attributeOpen) {
                        throw new RuntimeException("Attribute already open at line " + line);
                    }
                    if (blockAttribute != null) {
                        throw new RuntimeException("Attribute already defined at line " + line);
                    }
                    attributeOpen = true;
                    blockName = formatAndValidateBlockName(b.toString(), line);
                    b.setLength(0);
                    continue;
                }
                case '>' -> {
                    if (!attributeOpen) {
                        throw new RuntimeException("Attribute not open at line " + line);
                    }
                    attributeOpen = false;
                    blockAttribute = formatBlockNameOrAttribute(b.toString());
                    b.setLength(0);
                    continue;
                }
            }

            if (blockOpen) {
                b.appendCodePoint(unicode);
            } else if (!Character.isWhitespace(unicode)) {
                throw new RuntimeException("Invalid character at line " + line);
            }
        }
        if (blockOpen) {
            if (attributeOpen) {
                throw new RuntimeException("Unclosed attribute at line " + line);
            } else {
                throw new RuntimeException("Unclosed block at line " + line);
            }
        }

        return blocks;
    }

    public static String getTitleFormatted(String text) {
        return text
                .lines()
                .filter((s) -> !s.isBlank())
                .map((s) -> s.trim())
                .collect(Collectors.joining(" "));
    }

    public static String getParagraphFormatted(String text) {
        return text
                .lines()
                .filter((s) -> !s.isBlank())
                .map((s) -> s.trim())
                .collect(Collectors.joining("\n"));
    }

    public static String getCodeFormatted(String text) {
        return text
                .lines()
                .filter(new Predicate<String>() {
                    boolean startFound = false;

                    @Override
                    public boolean test(String t) {
                        if (t.isBlank() && !this.startFound) {
                            return false;
                        }
                        this.startFound = true;
                        return true;
                    }
                })
                .collect(Collectors.joining("\n"))
                .stripTrailing()
                .stripIndent();
    }

    public static String[] getListFormatted(String text) {
        return Stream
                .of(text.split(Pattern.quote(",")))
                .map((s) -> s.lines().map(e -> e.trim()).collect(Collectors.joining(" ")))
                .map((s) -> s.trim())
                .filter((s) -> !s.isEmpty())
                .toArray(String[]::new);
    }

    private final String name;
    private final String attribute;
    private final String rawText;

    private String titleFormatted = null;
    private String paragraphFormatted = null;
    private String codeFormatted = null;
    private String[] listFormatted = null;
    private final int line;

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

    public String getRawText() {
        return rawText;
    }

    public int getLine() {
        return line;
    }

    public String getTitleFormatted() {
        if (this.titleFormatted == null) {
            this.titleFormatted = getTitleFormatted(this.rawText);
        }
        return this.titleFormatted;
    }

    public String getParagraphFormatted() {
        if (this.paragraphFormatted == null) {
            this.paragraphFormatted = getParagraphFormatted(this.rawText);
        }
        return this.paragraphFormatted;
    }

    public String getCodeFormatted() {
        if (this.codeFormatted == null) {
            this.codeFormatted = getCodeFormatted(this.rawText);
        }
        return this.codeFormatted;
    }

    public String[] getListFormatted() {
        if (this.listFormatted == null) {
            this.listFormatted = getListFormatted(this.rawText);
        }
        return this.listFormatted.clone();
    }

}
