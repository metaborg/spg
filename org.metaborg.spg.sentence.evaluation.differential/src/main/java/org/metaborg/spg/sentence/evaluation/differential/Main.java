package org.metaborg.spg.sentence.evaluation.differential;

import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.metaborg.core.language.ILanguageImpl;
import org.metaborg.core.project.IProject;
import org.metaborg.core.resource.IResourceService;
import org.metaborg.spg.sentence.Utils;
import org.metaborg.spg.sentence.generator.Generator;
import org.metaborg.spg.sentence.generator.GeneratorFactory;
import org.metaborg.spg.sentence.guice.SentenceModule;
import org.metaborg.spg.sentence.printer.Printer;
import org.metaborg.spg.sentence.printer.PrinterFactory;
import org.metaborg.spoofax.core.Spoofax;
import org.spoofax.interpreter.terms.IStrategoTerm;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Optional;

public class Main {
    public static final String ARCHIVE = "/Users/martijn/Projects/metaborg-pascal/org.metaborg.lang.pascal/target/org.metaborg.lang.pascal-0.1.0-SNAPSHOT.spoofax-language";
    public static final String PROJECT = "/Users/martijn/Projects/metaborg-pascal/org.metaborg.lang.pascal";

    public static void main(String[] args) throws Exception {
        try (Spoofax spoofax = new Spoofax(new SentenceModule())) {
            ILanguageImpl language = Utils.loadLanguage(spoofax, new File(ARCHIVE));
            IProject project = Utils.getOrCreateProject(spoofax, new File(PROJECT));

            PrinterFactory printerFactory = spoofax.injector.getInstance(PrinterFactory.class);
            Printer printer = printerFactory.create(language, project);

            GeneratorFactory generatorFactory = spoofax.injector.getInstance(GeneratorFactory.class);
            Generator generator = generatorFactory.create(language, project);

            for (int i = 0; i < 10000; i++) {
                try {
                    Optional<IStrategoTerm> termOpt = generator.generate(10000);

                    if (termOpt.isPresent()) {
                        IStrategoTerm term = termOpt.get();
                        String text = printer.print(term);

                        writeFile(spoofax, i, text);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void writeFile(Spoofax spoofax, int i, String text) throws FileSystemException {
        IResourceService resourceService = spoofax.injector.getInstance(IResourceService.class);
        FileObject fileObject = resourceService.resolve("/private/tmp/grammars-v4/pascal/examples/" + i + ".pas");

        try (FileContent fileContent = fileObject.getContent()) {
            OutputStream out = fileContent.getOutputStream();

            PrintWriter printWriter = new PrintWriter(out);
            printWriter.write(text);
            printWriter.flush();
            printWriter.close();
        }
    }
}
