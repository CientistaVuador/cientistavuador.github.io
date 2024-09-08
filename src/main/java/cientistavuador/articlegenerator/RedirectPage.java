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

import static cientistavuador.articlegenerator.Article.INDENT;
import java.util.Date;

/**
 *
 * @author Cien
 */
public class RedirectPage {
    
    private static String writeHead(String url, String title, String description) {
        StringBuilder b = new StringBuilder();
        
        b.append("<head>\n");
        b.append(INDENT).append("<title>").append(title).append("</title>\n");
        b.append(INDENT).append("<meta charset=\"UTF-8\"/>\n");
        b.append(INDENT).append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/>\n");
        b.append(INDENT).append("<meta http-equiv=\"refresh\" content=\"0; url=").append(url).append("\"/>\n");
        b.append(INDENT).append("\n");
        b.append(INDENT).append("<!-- OpenGraph -->\n");
        b.append(INDENT).append("<meta name=\"og:title\" content=\"").append(title).append("\"/>\n");
        b.append(INDENT).append("<meta name=\"og:description\" content=\"").append(description).append("\"/>\n");
        b.append(INDENT).append("<meta name=\"og:type\" content=\"website\"/>\n");
        b.append(INDENT).append("<meta name=\"og:image\" content=\"resources/icon.png\"/>\n");
        b.append(INDENT).append("<!-- OpenGraph -->\n");
        b.append(INDENT).append("\n");
        b.append(INDENT).append("<script>\n");
        b.append(INDENT).append(INDENT).append("// <![CDATA[\n");
        b.append(INDENT).append(INDENT).append("window.location.href = \"").append(url).append("\";\n");
        b.append(INDENT).append(INDENT).append("// ]]>\n");
        b.append(INDENT).append("</script>\n");
        b.append("</head>");
        
        return b.toString();
    }
    
    private static String writeBody(String url, String title, String description) {
        StringBuilder b = new StringBuilder();
        
        b.append("<body>\n");
        b.append(INDENT).append("<p><a href=\"").append(url).append("\">").append(FontFormatting.escape(url)).append("</a></p>\n");
        b.append("</body>");
        
        return b.toString();
    }
    
    public static String createRedirectPage(String url, String language, String title, String description) {
        StringBuilder b = new StringBuilder();
        
        b.append("<!DOCTYPE html>\n");
        b.append("<!--\n");
        b.append(FontFormatting.escapeComment(TextBlock.getCodeFormatted(Localization.getInstance().localize("license", null, "All Rights Reserved"))));
        b.append("\n\n");
        b.append("Redirect Page:\n");
        b.append(INDENT).append("To: ").append(FontFormatting.escapeComment(url)).append("\n");
        b.append(INDENT).append("Title: ").append(FontFormatting.escapeComment(title)).append("\n");
        b.append(INDENT).append("Description: ").append(FontFormatting.escapeComment(description)).append("\n\n");
        b.append(new Date().toString()).append("\n");
        b.append("-->\n");
        b.append("<html lang=\"").append(language).append("\">\n");
        b.append(writeHead(url, title, description).indent(4));
        b.append(writeBody(url, title, description).indent(4));
        b.append("</html>");
        
        return b.toString();
    }
    
    private RedirectPage() {
        
    }
}
