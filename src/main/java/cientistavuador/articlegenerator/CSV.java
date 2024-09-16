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
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author Cien
 */
public class CSV {
    
    public static CSV parse(String text) {
        try {
            return new CSV((text == null || text.isEmpty() ? null : new StringReader(text)));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
    
    private final String[] fields;
    private final int numberOfFields;
    private final int numberOfRecords;

    public CSV(String[] fields, int numberOfFields, int numberOfRecords) {
        if (numberOfFields <= 0) {
            throw new IllegalArgumentException("Number of fields <= 0: " + numberOfFields);
        }
        if (numberOfRecords <= 0) {
            throw new IllegalArgumentException("Number of records <= 0: " + numberOfRecords);
        }
        this.numberOfFields = numberOfFields;
        this.numberOfRecords = numberOfRecords;
        if (fields == null) {
            this.fields = new String[numberOfFields * numberOfRecords];
            Arrays.fill(this.fields, "");
            return;
        }
        int requiredNumberOfFields = this.numberOfFields * this.numberOfRecords;
        if (fields.length != requiredNumberOfFields) {
            throw new IllegalArgumentException("Fields length must be " + requiredNumberOfFields + " and not " + fields.length);
        }
        this.fields = fields.clone();
        for (int i = 0; i < this.fields.length; i++) {
            if (this.fields[i] == null) {
                this.fields[i] = "";
            }
        }
    }

    private static int readCodepoint(Reader reader) throws IOException {
        int cA = reader.read();
        if (cA == -1) {
            return -1;
        }
        char aChar = (char) cA;
        if (Character.isHighSurrogate(aChar)) {
            int cB = reader.read();
            if (cB == -1) {
                return cA;
            }
            char bChar = (char) cB;
            if (Character.isLowSurrogate(bChar)) {
                return Character.toCodePoint(aChar, bChar);
            }
        }
        return cA;
    }

    public CSV(Reader reader) throws IOException {
        int bcp = (reader != null ? readCodepoint(reader) : -1);
        if (bcp == -1) {
            this.fields = new String[] {""};
            this.numberOfFields = 1;
            this.numberOfRecords = 1;
            return;
        }
        
        StringBuilder b = new StringBuilder();
        List<String> currentRecord = new ArrayList<>();
        List<String> fullData = new ArrayList<>();
        int currentLine = 1;
        int expectedSize = -1;

        record:
        for (;;) {
            //read field
            b.setLength(0);
            boolean quoted = false;
            field:
            for (;;) {
                int cp;
                if (bcp != -1) {
                    cp = bcp;
                    bcp = -1;
                } else {
                    cp = readCodepoint(reader);
                }
                switch (cp) {
                    case -1 -> {
                        break field;
                    }
                    case '"' -> {
                        if (quoted) {
                            bcp = readCodepoint(reader);
                            if (bcp == cp) {
                                bcp = -1;
                                break;
                            }
                            quoted = false;
                            break field;
                        }
                        if (!b.isEmpty()) {
                            throw new IllegalArgumentException("Double quotes must be placed only at the start and end of the field at line " + currentLine);
                        }
                        quoted = true;
                        continue;
                    }
                    case '\n', '\r', ',' -> {
                        if (quoted) {
                            break;
                        }
                        if (cp == '\r') {
                            bcp = readCodepoint(reader);
                            if (bcp != '\n') {
                                break;
                            }
                            cp = bcp;
                        }
                        bcp = cp;
                        break field;
                    }
                }
                if (cp == '\n') {
                    currentLine++;
                }
                b.appendCodePoint(cp);
            }
            if (quoted) {
                throw new IllegalArgumentException("Unclosed double quotes after end was reached.");
            }
            currentRecord.add(b.toString());
            
            //read next field
            int cp;
            if (bcp != -1) {
                cp = bcp;
                bcp = -1;
            } else {
                cp = readCodepoint(reader);
            }

            switch (cp) {
                case ',' -> {
                    continue;
                }
                case -1, '\n', '\r' -> {
                    if (cp == '\r') {
                        if (readCodepoint(reader) == '\n') {
                            cp = '\n';
                        } else {
                            break;
                        }
                    }
                    if (expectedSize != -1 && currentRecord.size() != expectedSize) {
                        throw new IllegalArgumentException("Invalid number of fields at line " + currentLine + " expected " + expectedSize + " field(s), found " + currentRecord.size() + " field(s).");
                    }
                    expectedSize = currentRecord.size();
                    fullData.addAll(currentRecord);
                    currentRecord.clear();
                    if (cp == -1) {
                        break record;
                    }
                    currentLine++;
                    bcp = readCodepoint(reader);
                    if (bcp == -1) {
                        break record;
                    }
                    continue;
                }
            }
            throw new IllegalArgumentException("Illegal character '" + (Character.isWhitespace(cp) ? Character.getName(cp) : Character.toString(cp)) + "', expected ',' or 'LF' or 'CRLF' at line " + currentLine);
        }

        this.fields = fullData.toArray(String[]::new);
        this.numberOfFields = expectedSize;
        this.numberOfRecords = this.fields.length / expectedSize;
    }
    
    public int getNumberOfFields() {
        return numberOfFields;
    }

    public int getNumberOfRecords() {
        return numberOfRecords;
    }

    public void set(int field, int record, String value) {
        if (value == null) {
            value = "";
        }
        Objects.checkIndex(field, this.numberOfFields);
        Objects.checkIndex(record, this.numberOfRecords);
        this.fields[(record * this.numberOfFields) + field] = value;
    }

    public String get(int field, int record) {
        Objects.checkIndex(field, this.numberOfFields);
        Objects.checkIndex(record, this.numberOfRecords);
        return this.fields[(record * this.numberOfFields) + field];
    }

    public CSV copy() {
        return new CSV(this.fields, this.numberOfFields, this.numberOfRecords);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        StringBuilder columnBuilder = new StringBuilder();

        for (int record = 0; record < getNumberOfRecords(); record++) {
            for (int field = 0; field < getNumberOfFields(); field++) {
                String value = get(field, record);

                boolean hasEspecialCharacters = false;
                for (int i = 0; i < value.length(); i++) {
                    int unicode = value.codePointAt(i);
                    if (Character.charCount(unicode) == 2) {
                        i++;
                    }
                    switch (unicode) {
                        case '"', ',', '\n', '\r' -> {
                            hasEspecialCharacters = true;
                        }
                    }
                    if (unicode == '"') {
                        columnBuilder.append('"');
                    }
                    columnBuilder.appendCodePoint(unicode);
                }

                if (hasEspecialCharacters) {
                    b.append('"');
                }
                b.append(columnBuilder);
                columnBuilder.setLength(0);
                if (hasEspecialCharacters) {
                    b.append('"');
                }

                if (field != (getNumberOfFields() - 1)) {
                    b.append(",");
                }
            }
            if (record != (getNumberOfRecords() - 1)) {
                b.append("\r\n");
            }
        }
        return b.toString();
    }

}
