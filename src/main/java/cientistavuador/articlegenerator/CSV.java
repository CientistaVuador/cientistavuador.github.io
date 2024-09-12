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

    private final String[] fields;
    private final int numberOfFields;
    private final int numberOfRecords;
    
    public CSV(String[] fields, int numberOfFields, int numberOfRecords) {
        if (numberOfFields <= 0) {
            throw new IllegalArgumentException("Number of fields <= 0: "+numberOfFields);
        }
        if (numberOfRecords <= 0) {
            throw new IllegalArgumentException("Number of records <= 0: "+numberOfRecords);
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
            throw new IllegalArgumentException("Fields length must be "+requiredNumberOfFields+" and not "+fields.length);
        }
        this.fields = fields.clone();
        for (int i = 0; i < this.fields.length; i++) {
            if (this.fields[i] == null) {
                this.fields[i] = "";
            }
        }
    }
    
    public CSV(String csv) {
        if (csv == null || csv.isEmpty()) {
            this.fields = new String[]{""};
            this.numberOfFields = 1;
            this.numberOfRecords = 1;
            return;
        }

        final StringBuilder b = new StringBuilder();
        final List<String> fullData = new ArrayList<>();
        final List<String> recordData = new ArrayList<>();
        int fieldsCount = -1;
        int quoteState = 0;
        int currentLine = 1;
        
        for (int i = 0; i < csv.length(); i++) {
            int unicode = csv.codePointAt(i);
            boolean hasNext = (i != csv.length() - 1);
            if (unicode == '\n') {
                currentLine++;
            }
            
            if (quoteState == 1) {
                if (unicode == '"') {
                    if (hasNext && csv.codePointAt(i + 1) == '"') {
                        i++;
                    } else {
                        quoteState++;
                        continue;
                    }
                }
                b.appendCodePoint(unicode);
                continue;
            }
            
            switch (unicode) {
                case '"' -> {
                    if (!b.isEmpty()) {
                        throw new IllegalArgumentException("Quotes must be the first character in a field at line " + currentLine);
                    }
                    if (quoteState == 2) {
                        throw new IllegalArgumentException("Quotes already closed at line " + currentLine);
                    }
                    quoteState++;
                    continue;
                }
                case ',', '\n', '\r' -> {
                    if (unicode == '\r') {
                        if (hasNext && csv.codePointAt(i + 1) == '\n') {
                            i++;
                            currentLine++;
                        } else {
                            break;
                        }
                    }

                    recordData.add(b.toString());
                    b.setLength(0);
                    quoteState = 0;

                    if (unicode == ',') {
                        continue;
                    }

                    if (fieldsCount == -1) {
                        fieldsCount = recordData.size();
                    }

                    if (fieldsCount != recordData.size()) {
                        throw new IllegalArgumentException("Invalid amount of fields in line " + currentLine + ", expected " + fieldsCount + ", found " + recordData.size());
                    }

                    fullData.addAll(recordData);
                    recordData.clear();
                    continue;
                }

            }

            if (quoteState == 2) {
                throw new IllegalArgumentException("Invalid character after quotes were closed at line " + currentLine + " expected ',' or 'LF' or 'CRLF'");
            }
            b.appendCodePoint(unicode);
        }

        if (quoteState == 1) {
            throw new IllegalArgumentException("Unclosed quotes after end was reached");
        }

        if (csv.codePointAt(csv.length() - 1) != '\n') {
            recordData.add(b.toString());
            b.setLength(0);

            if (fieldsCount == -1) {
                fieldsCount = recordData.size();
            }

            if (fieldsCount != recordData.size()) {
                throw new IllegalArgumentException("Invalid amount of fields after end was reached, expected " + fieldsCount + ", found " + recordData.size());
            }

            fullData.addAll(recordData);
            recordData.clear();
        }

        this.fields = fullData.toArray(String[]::new);
        this.numberOfFields = fieldsCount;
        this.numberOfRecords = fullData.size() / this.numberOfFields;
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

        for (int row = 0; row < getNumberOfRecords(); row++) {
            for (int column = 0; column < getNumberOfFields(); column++) {
                String columnData = get(column, row);

                boolean hasEspecialCharacters = false;
                for (int i = 0; i < columnData.length(); i++) {
                    int unicode = columnData.codePointAt(i);
                    switch (unicode) {
                        case '"', ',', '\n' -> {
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

                if (column != (getNumberOfFields() - 1)) {
                    b.append(",");
                }
            }
            if (row != (getNumberOfRecords() - 1)) {
                b.append("\n");
            }
        }
        return b.toString();
    }

}
