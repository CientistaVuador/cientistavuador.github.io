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

import java.util.Objects;
import java.util.regex.Pattern;

/**
 *
 * @author Cien
 */
public class ISOLanguage {
    
    public static final ISOLanguage EMPTY = new ISOLanguage(null);
    
    private static boolean containsANSILettersOnly(String s) {
        for (int i = 0; i < s.length(); i++) {
            int unicode = s.codePointAt(i);
            if (unicode < 'a' || unicode > 'z') {
                return false;
            }
        }
        return true;
    }
    
    private final String language;
    private final String country;
    private final String stringFormat;
    private final String stringFormatUppercase;
    
    private ISOLanguage languageOnly = null;
    
    public ISOLanguage(String lang) {
        if (lang == null) {
            lang = "";
        }
        
        String[] split = lang.split(Pattern.quote("-"), 2);
        String l = split[0].trim().toLowerCase();
        String c = "";
        if (split.length > 1) {
            c = split[1].trim().toLowerCase();
        }
        
        if (!l.isEmpty() && l.length() != 2) {
            throw new IllegalArgumentException("Language must have 2 letters.");
        }
        if (!c.isEmpty() && c.length() != 2) {
            throw new IllegalArgumentException("Country must have 2 letters.");
        }
        
        if (l.isEmpty() && !c.isEmpty()) {
            throw new IllegalArgumentException("Country must also be empty if language is empty.");
        }
        
        if (!containsANSILettersOnly(l)) {
            throw new IllegalArgumentException("Language must have only ansi letters.");
        }
        if (!containsANSILettersOnly(c)) {
            throw new IllegalArgumentException("Country must have only ansi letters.");
        }
        
        this.language = l;
        this.country = c;
        
        if (this.country.isEmpty()) {
            this.stringFormat = this.language;
        } else {
            this.stringFormat = this.language+"-"+this.country;
        }
        this.stringFormatUppercase = this.stringFormat.toUpperCase();
    }

    public String getLanguage() {
        return language;
    }
    
    public String getCountry() {
        return country;
    }
    
    public boolean hasLanguage() {
        return !getLanguage().isEmpty();
    }
    
    public boolean hasCountry() {
        return !getCountry().isEmpty();
    }
    
    public ISOLanguage toLanguageOnly() {
        if (!hasCountry()) {
            return this;
        }
        if (this.languageOnly == null) {
            this.languageOnly = new ISOLanguage(getLanguage());
        }
        return this.languageOnly;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ISOLanguage other = (ISOLanguage) obj;
        return Objects.equals(this.stringFormat, other.stringFormat);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 41 * hash + Objects.hashCode(this.stringFormat);
        return hash;
    }
    
    @Override
    public String toString() {
        return this.stringFormat;
    }
    
    public String toStringUpperCase() {
        return this.stringFormatUppercase;
    }
}
