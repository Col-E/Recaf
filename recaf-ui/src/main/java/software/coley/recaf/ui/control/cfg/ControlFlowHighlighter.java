package software.coley.recaf.ui.control.cfg;

import com.google.common.collect.Iterables;
import jakarta.inject.Inject;
import javafx.css.*;
import javafx.scene.paint.Color;
import org.fxmisc.richtext.model.StyleSpan;
import org.fxmisc.richtext.model.StyleSpans;
import software.coley.recaf.cdi.WorkspaceScoped;
import software.coley.recaf.ui.control.richtext.syntax.RegexLanguages;
import software.coley.recaf.ui.control.richtext.syntax.RegexSyntaxHighlighter;

import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLDocument;
import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.*;

@WorkspaceScoped
public class ControlFlowHighlighter {

    final Map<String, StyleSheet> styleSheets = new HashMap<>();

    @Inject
    public ControlFlowHighlighter() {
        this.loadStyleSheet("jasm", this.getClass().getClassLoader().getResource("syntax/jasm.css"));
    }

    void loadStyleSheet(String name, URL url) {
        CssParser parser = new CssParser();
        try {
            Stylesheet sheet = parser.parse(url);
            Map<String, StyleSheetClass> classes = new HashMap<>();

            for (Rule rule : sheet.getRules()) {
                Map<String, StyleSheetProperty> properties = new HashMap<>();
                for (Declaration decl : rule.getDeclarations()) {
                    properties.put(decl.getProperty(), new StyleSheetProperty(decl.getProperty(), decl.getParsedValue()));
                }
                for (Selector selector : rule.getSelectors()) {
                    for (Selector ruleSelector : selector.getRule().getSelectors()) {
                        if (ruleSelector instanceof SimpleSelector simpleSelector) {
                            for (StyleClass styleClass : simpleSelector.getStyleClassSet()) {
                                classes.put(styleClass.getStyleClassName(), new StyleSheetClass(properties));
                            }
                        } else if (ruleSelector instanceof CompoundSelector compoundSelector) {
                            for (SimpleSelector compoundSelectorSelector : compoundSelector.getSelectors()) {
                                for (StyleClass styleClass : compoundSelectorSelector.getStyleClassSet()) {
                                    classes.put(styleClass.getStyleClassName(), new StyleSheetClass(properties));
                                }
                            }
                        }
                    }
                }
            }
            this.styleSheets.put(name, new StyleSheet(classes));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String highlightToHtml(String name, String text) {
        RegexSyntaxHighlighter highlighter = new RegexSyntaxHighlighter(RegexLanguages.getJasmLanguage());
        StyleSpans<Collection<String>> spans = highlighter.createStyleSpans(text, 0, text.length());
        StringBuilder builder = new StringBuilder();
        int idx = 0;
        for (StyleSpan<Collection<String>> span : spans) {
            String spanText = text.substring(idx, idx + span.getLength());
            String style = Iterables.getFirst(span.getStyle(), null);
            StyleSheet styleSheet = style == null ? null : this.styleSheets.get(name);
            if (styleSheet == null) {
                builder.append(String.format("<span>%s</span>", spanText));
            } else {
                String color = Optional.ofNullable(styleSheet.classes().get(style))
                        .flatMap(it -> Optional.ofNullable(it.properties().get("-fx-fill")))
                        .map(it -> (ParsedValue<?,?>) it.value())
                        .filter(it -> it.getValue() instanceof Color)
                        .map(it -> toHexString((Color) it.getValue()))
                        .orElse("#F4FEFF");
                builder.append(String.format("<span><font color=\"%s\">%s</font></span>", color, spanText));
            }
            idx += span.getLength();
        }
        return builder.toString();
    }

    record StyleSheet(Map<String, StyleSheetClass> classes) {}
    record StyleSheetClass(Map<String, StyleSheetProperty> properties) {}
    record StyleSheetProperty(String name, Object value) {}

    static String toHexString(Color color) {
        return String.format("#%02X%02X%02X", (int) (color.getRed() * 255.0d), (int) (color.getGreen() * 255.0d),
                (int) (color.getBlue() * 255.0d));
    }

}
