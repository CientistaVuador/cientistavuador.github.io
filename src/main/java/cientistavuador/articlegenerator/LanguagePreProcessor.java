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
import java.util.Objects;
import java.util.stream.Stream;

/**
 *
 * @author Cien
 */
public class LanguagePreProcessor {
    
    public static class PreProcessorOutput {
        private final String language;
        private final String output;

        public PreProcessorOutput(String language, String output) {
            Objects.requireNonNull(language, "Language is null.");
            Objects.requireNonNull(output, "Output is null.");
            
            this.language = language;
            this.output = output;
        }

        public String getLanguage() {
            return language;
        }

        public String getOutput() {
            return output;
        }
        
    }
    
    public static PreProcessorOutput[] preProcess(String text) {
        List<PreProcessorOutput> output = new ArrayList<>();
        
        String[] languages = null;
        {
            int lineCount = 0;
            
            for (String line:text.lines().toList()) {
                lineCount++;
                
                String leadingLowerCase = line.toLowerCase().stripLeading();
                if (!leadingLowerCase.isBlank() && leadingLowerCase.startsWith("$languages")) {
                    String sub = leadingLowerCase.substring("$languages".length());
                    if (sub.isBlank()) {
                        throw new RuntimeException("Empty $languages at line "+lineCount);
                    }
                    if (!sub.startsWith(" ")) {
                        throw new RuntimeException("$languages must start with a space at line "+lineCount);
                    }
                    String[] split = sub.split(" ");
                    if (split.length == 0) {
                        throw new RuntimeException("Empty $languages at line "+lineCount);
                    }
                    languages = Stream.of(split).filter(s -> !s.isBlank()).toArray(String[]::new);
                    break;
                } else {
                    throw new RuntimeException("$languages must be at the first non blank line, at line "+lineCount);
                }
            }
            
            if (languages == null) {
                languages = new String[0];
            }
        }
        
        if (languages.length == 0) {
            return new PreProcessorOutput[0];
        }
        
        System.out.println(languages.length);
        for (String lang:languages) {
            System.out.println(lang);
        }
        
        return output.toArray(PreProcessorOutput[]::new);
    }
    
    private LanguagePreProcessor() {
        
    }
    
}
