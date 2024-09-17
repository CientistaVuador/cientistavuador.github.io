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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author Cien
 */
public class CSV {

    public static CSV read(String text) {
        if (text == null || text.isEmpty()) {
            return new CSV(null, 1, 1);
        }

        List<String> currentRecord = new ArrayList<>();
        List<String> allFields = new ArrayList<>();
        StringBuilder b = new StringBuilder();

        int expectedRecordSize = -1;
        int currentLine = 1;
        boolean quoted = false;
        boolean fieldDone = false;
        boolean finishedWithNoCRLF = true;
        int unicode;
        for (int i = 0; i < text.length(); i += Character.charCount(unicode)) {
            unicode = text.codePointAt(i);
            switch (unicode) {
                case '"' -> {
                    if (quoted) {
                        if ((i + 1) < text.length() && text.codePointAt(i + 1) == '"') {
                            i++;
                            break;
                        }
                        quoted = false;
                        fieldDone = true;
                        continue;
                    }
                    if (!b.isEmpty()) {
                        throw new IllegalArgumentException("Double quotes must only be placed at the start and end of a field at line " + currentLine);
                    }
                    quoted = true;
                    continue;
                }
                case ',', '\r', '\n' -> {
                    if (quoted) {
                        if (unicode == '\n' || unicode == '\r' && ((i + 1) >= text.length()) || text.codePointAt(i + 1) != '\n') {
                            currentLine++;
                        }
                        break;
                    }
                    fieldDone = false;
                    currentRecord.add(b.toString());
                    b.setLength(0);
                    if (unicode == '\r' || unicode == '\n') {
                        currentLine++;
                        if (unicode == '\r' && (i + 1) < text.length() && text.codePointAt(i + 1) == '\n') {
                            i++;
                        }
                        if (expectedRecordSize != -1 && currentRecord.size() != expectedRecordSize) {
                            throw new IllegalArgumentException("Invalid record size at line " + (currentLine - 1) + ", expected " + expectedRecordSize + " fields(s) but found " + currentRecord.size() + " field(s).");
                        }
                        expectedRecordSize = currentRecord.size();
                        allFields.addAll(currentRecord);
                        currentRecord.clear();
                        if (i == text.length() - 1) {
                            finishedWithNoCRLF = false;
                        }
                    }
                    continue;
                }
            }
            if (fieldDone) {
                throw new IllegalArgumentException("Illegal character '" + (Character.isWhitespace(unicode) ? Character.getName(unicode) : Character.toString(unicode)) + "', expected ',' or 'CRLF' or 'LF' at line " + currentLine);
            } else {
                b.appendCodePoint(unicode);
            }
        }
        if (quoted) {
            throw new IllegalArgumentException("Unclosed double quotes after end of text was reached.");
        }
        if (finishedWithNoCRLF) {
            currentRecord.add(b.toString());
            if (expectedRecordSize != -1 && currentRecord.size() != expectedRecordSize) {
                throw new IllegalArgumentException("Invalid record size at end of text, expected " + expectedRecordSize + " fields(s) but found " + currentRecord.size() + " field(s).");
            }
            expectedRecordSize = currentRecord.size();
            allFields.addAll(currentRecord);
            currentRecord.clear();
        }
        return new CSV(allFields.toArray(String[]::new), expectedRecordSize, allFields.size() / expectedRecordSize);
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
