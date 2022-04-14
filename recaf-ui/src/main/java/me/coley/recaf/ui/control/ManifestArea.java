package me.coley.recaf.ui.control;

import me.coley.recaf.Recaf;
import me.coley.recaf.RecafUI;
import me.coley.recaf.code.ClassInfo;
import me.coley.recaf.ui.CommonUX;
import me.coley.recaf.ui.control.code.Language;
import me.coley.recaf.ui.control.code.ProblemTracking;
import me.coley.recaf.ui.control.code.SyntaxArea;
import me.coley.recaf.util.logging.Logging;
import me.coley.recaf.util.threading.FxThreadUtil;
import me.coley.recaf.workspace.Workspace;
import org.fxmisc.richtext.CharacterHit;
import org.fxmisc.richtext.model.PlainTextChange;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class ManifestArea extends SyntaxArea {

    private static final Logger logger = Logging.get(SyntaxArea.class);

    private String mainClass = "";

    /**
     * @param language        Language to use for syntax highlighting.
     * @param problemTracking Optional problem tracking implementation to enable line problem indicators.
     */
    public ManifestArea(Language language, ProblemTracking problemTracking) {
        super(language, problemTracking);
        setOnMousePressed((e) -> {

            if(e.isControlDown() && e.isPrimaryButtonDown()) {
                if(!mainClass.isEmpty()) {
                    int start = getText().indexOf(mainClass);
                    int end = start + mainClass.length();

                    CharacterHit hit = hit(e.getX(), e.getY());
                    int pos = hit.getInsertionIndex();
                    if(pos >= start && pos <= end) {
                        openMainClassRef();
                    }
                }
            }

        });
    }

    public void openMainClassRef() {
        String internal = mainClass.replace(".", "/");
        Workspace wrk = RecafUI.getController().getWorkspace();
        if(wrk == null) {
            logger.error("Workspace is null");
            return;
        }
        ClassInfo clsInfo = wrk.getResources().getClass(internal);
        if(clsInfo == null) {
            logger.error("Main-Class {} not found in workspace", internal);
            return;
        }
        CommonUX.openClass(clsInfo);
    }

    @Override
    protected void onTextChanged(PlainTextChange change) {
        super.onTextChanged(change);

        try {
            InputStream in = new ByteArrayInputStream(getText().getBytes());

            Manifest manifest = new Manifest(in);

            Attributes attr = manifest.getMainAttributes();

            try {
                this.mainClass = attr.getValue("Main-Class");
                int start = getText().indexOf(mainClass);
                int end = start + mainClass.length();

                // HACK
                FxThreadUtil.delayedRun(200, () -> setStyle(start, end, List.of("u")));
            }catch (IllegalArgumentException e) {
                logger.error("Couldn't find Main-Class attribute in manifest");
            }
        }catch (IOException e) {
            logger.error("Failed to parse manifest", e);
        }

    }
}
