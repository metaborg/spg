package org.metaborg.spg.sentence.evaluation;

import com.google.inject.Singleton;
import org.metaborg.core.editor.IEditorRegistry;
import org.metaborg.core.editor.NullEditorRegistry;
import org.metaborg.spg.sentence.IRandom;
import org.metaborg.spg.sentence.Random;
import org.metaborg.spoofax.core.SpoofaxModule;

public class EvaluationModule extends SpoofaxModule {
    @Override
    protected void bindEditor() {
        bind(IEditorRegistry.class)
                .to(NullEditorRegistry.class)
                .in(Singleton.class);
    }
}
