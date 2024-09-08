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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;

/**
 *
 * @author Cien
 */
public class Localization {

    private static final Localization INSTANCE;
    
    static {
        try {
            Properties prop = new Properties();
            try (InputStreamReader reader = new InputStreamReader(new FileInputStream("resources/localization.properties"), StandardCharsets.UTF_8)) {
                prop.load(reader);
            }
            INSTANCE = new Localization(prop);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
    
    public static Localization getInstance() {
        return INSTANCE;
    }
    
    private final Properties properties;
    
    private Localization(Properties properties) {
        this.properties = properties;
    }

    public Properties getProperties() {
        return properties;
    }
    
    public String localize(String key, String language) {
        return localize(key, language, key);
    }
    
    public String localize(String key, String language, String fallback) {
        Objects.requireNonNull(key, "Key is null.");
        Objects.requireNonNull(fallback, "Fallback is null.");
        if (language == null) {
            language = "";
        }
        
        String localizedKey = key;
        if (!language.isEmpty()) {
            localizedKey = key + "." + language;
        }
        
        String planA = this.properties.getProperty(localizedKey);
        String planB = this.properties.getProperty(key);
        String planC = fallback;
        
        if (planA != null) {
            return planA;
        }
        if (planB != null) {
            return planB;
        }
        return planC;
    }
    
}
