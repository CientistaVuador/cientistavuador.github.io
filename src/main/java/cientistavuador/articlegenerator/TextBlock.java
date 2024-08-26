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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author Cien
 */
public class TextBlock {

    public static List<TextBlock> parse(String text) {
        List<TextBlock> blocks = new ArrayList<>();

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(text.getBytes()), StandardCharsets.UTF_8));

            int currentLine = 0;
            int currentBlockLine = -1;
            String currentBlockName = null;
            StringBuilder blockBuilder = null;

            String line;
            while ((line = reader.readLine()) != null) {
                currentLine++;

                String lineTrim = line.trim();
                if (lineTrim.startsWith("#")) {
                    String[] split = lineTrim.split(" ", 2);
                    String name = split[0].trim().substring(1).toLowerCase();
                    String argument = null;
                    if (split.length != 1) {
                        argument = split[1].trim();
                        if (argument.isEmpty()) {
                            argument = null;
                        }
                    }

                    if (name.isEmpty()) {
                        throw new RuntimeException("Empty block name at line " + currentLine);
                    }

                    if (currentBlockName != null && !name.equals("end")) {
                        throw new RuntimeException("Unclosed block at line " + currentLine);
                    }

                    if (name.equals("begin")) {
                        if (argument == null) {
                            throw new RuntimeException("Block without name at line " + currentLine);
                        }
                        if (argument.contains(" ") || argument.contains("\t")) {
                            throw new RuntimeException("Spaces are not allowed in block names at line " + currentLine);
                        }
                        currentBlockLine = currentLine;
                        currentBlockName = argument.toLowerCase();
                        blockBuilder = new StringBuilder();
                    } else if (name.equals("end")) {
                        if (currentBlockName == null) {
                            throw new RuntimeException("Text block not open at line " + currentLine);
                        }
                        if (argument != null) {
                            throw new RuntimeException("Text block end must not have arguments at line " + currentLine);
                        }
                        if (blockBuilder == null) {
                            throw new NullPointerException();
                        }
                        String resultArgument;
                        if (blockBuilder.isEmpty()) {
                            resultArgument = "";
                        } else if (blockBuilder.length() == 1) {
                            resultArgument = blockBuilder.toString();
                        } else {
                            resultArgument = blockBuilder.substring(0, blockBuilder.length() - 1);
                        }
                        blocks.add(new TextBlock(currentBlockLine, currentBlockName, resultArgument, true));
                        currentBlockLine = -1;
                        currentBlockName = null;
                        blockBuilder = null;
                    } else {
                        blocks.add(new TextBlock(currentLine, name, argument, false));
                    }
                    continue;
                }
                if (blockBuilder != null) {
                    blockBuilder.append(line.replace("\\#", "#")).append("\n");
                }
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }

        return blocks;
    }

    private final int line;
    private final String name;
    private final String argument;
    private final boolean multiLine;
    private final String[] arguments;

    public TextBlock(int line, String name, String argument, boolean multiLine) {
        Objects.requireNonNull(name, "Name is null.");

        this.line = line;
        this.name = name;
        this.argument = argument;
        this.multiLine = multiLine;

        List<String> formattedArgs = new ArrayList<>();
        if (argument != null) {
            String[] args = argument.split(" ");
            for (String arg : args) {
                arg = arg.trim();
                if (arg.isBlank()) {
                    continue;
                }
                formattedArgs.add(arg);
            }
        }
        this.arguments = formattedArgs.toArray(String[]::new);
    }
    
    public int getLine() {
        return line;
    }

    public String getName() {
        return name;
    }

    public String getArgument() {
        return argument;
    }

    public boolean isMultiLine() {
        return multiLine;
    }

    public int getNumberOfArguments() {
        return this.arguments.length;
    }

    public String getArgument(int index) {
        return this.arguments[index];
    }

}
