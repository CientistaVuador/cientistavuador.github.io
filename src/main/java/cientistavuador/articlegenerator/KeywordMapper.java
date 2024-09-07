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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Cien
 */
public class KeywordMapper {

    private static String clear(String s) {
        StringBuilder b = new StringBuilder();

        for (int i = 0; i < s.length(); i++) {
            int unicode = s.codePointAt(i);
            if (unicode == ' ') {
                b.append(' ');
                continue;
            }
            if (!Character.isLetterOrDigit(unicode)) {
                b.append(' ');
                continue;
            }
            b.appendCodePoint(unicode);
        }

        return b.toString();
    }

    private static boolean isOnlyDigits(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.codePointAt(i))) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    public static String getKeywords(String text) {
        List<String> words = new ArrayList<>();
        String cleared = clear(text);
        String[] split = cleared.split(" ");
        for (String word : split) {
            if (word.isBlank() || word.length() == 1) {
                continue;
            }
            if (isOnlyDigits(word)) {
                continue;
            }
            words.add(word.toLowerCase());
        }

        Map<String, Integer> wordCount = new HashMap<>();
        for (String word : words) {
            Integer current = wordCount.get(word);
            if (current == null) {
                current = 0;
            }
            current++;
            wordCount.put(word, current);
        }

        Map.Entry<String, Integer>[] set = wordCount.entrySet().toArray(Map.Entry[]::new);
        Comparator<Map.Entry<String, Integer>> comparator = (a, b) -> Integer.compare(a.getValue(), b.getValue());
        Arrays.sort(set, comparator.reversed());

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 64; i++) {
            if (i >= set.length) {
                break;
            }
            builder.append(set[i].getKey());
            if (i != set.length - 1 && i != 63) {
                builder.append(", ");
            }
        }

        return builder.toString();
    }

    private KeywordMapper() {

    }
}
