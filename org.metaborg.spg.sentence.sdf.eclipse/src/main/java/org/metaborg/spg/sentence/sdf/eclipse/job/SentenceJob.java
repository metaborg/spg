package org.metaborg.spg.sentence.sdf.eclipse.job;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.metaborg.spoofax.core.Spoofax;
import org.metaborg.spoofax.eclipse.SpoofaxPlugin;
import org.metaborg.spoofax.eclipse.util.ConsoleUtils;

public abstract class SentenceJob extends Job {
    protected final MessageConsole console = ConsoleUtils.get("Spoofax console");
    protected final MessageConsoleStream stream = console.newMessageStream();
    protected final Spoofax spoofax = SpoofaxPlugin.spoofax();

    public SentenceJob(String name) {
        super(name);
    }
}
