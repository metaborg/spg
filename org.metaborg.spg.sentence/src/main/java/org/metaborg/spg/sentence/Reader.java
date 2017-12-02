package org.metaborg.spg.sentence;

import com.google.common.base.Joiner;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.project.IProject;
import org.metaborg.spg.sentence.sdf3.*;
import org.metaborg.spg.sentence.signature.Operation;
import org.metaborg.spg.sentence.signature.Signature;
import org.metaborg.spg.sentence.signature.SignatureFactory;
import org.metaborg.spg.sentence.signature.Sort;
import org.metaborg.spoofax.core.Spoofax;

import java.io.File;
import java.io.IOException;

import static org.metaborg.spg.sentence.shared.utils.SpoofaxUtils.getOrCreateProject;
import static org.metaborg.spg.sentence.shared.utils.SpoofaxUtils.loadLanguage;

public class Reader {
    public static void main(String[] args) throws MetaborgException, IOException {
        try (Spoofax spoofax = new Spoofax()) {
            File languageFile = new File("/Users/martijn/Projects/spoofax-releng/sdf/org.metaborg.meta.lang.template/target/org.metaborg.meta.lang.template-2.4.0-SNAPSHOT.spoofax-language");
            File syntaxFile = new File("/Users/martijn/Projects/java-front/lang.java/syntax/metaborg-java.sdf3");

            File projectFile = new File("/Users/martijn/Projects/java-front/lang.java/");
            IProject project = getOrCreateProject(spoofax, projectFile);

            GrammarFactory grammarFactory = spoofax.injector.getInstance(GrammarFactory.class);
            Grammar grammar = grammarFactory.fromProject(project, loadLanguage(spoofax, languageFile));

            for (Module module : grammar.getModules()) {
                print(module);
            }

            SignatureFactory signatureFactory = spoofax.injector.getInstance(SignatureFactory.class);
            Signature signature = signatureFactory.fromGrammar(grammar);

            System.out.println("signature");
            System.out.println("  constructors");

            for (Operation operation : signature.getOperations()) {
                System.out.println("    " + operation);
            }

            for (Sort sort : signature.getSorts()) {
                Iterable<Sort> injections = signature.getInjections(sort);

                System.out.println("Sort " + sort + " -> {" + Joiner.on(", ").join(injections) + "}");
            }

            /*
            Module module = grammarFactory.readModule(syntaxFile, loadLanguage(spoofax, languageFile));
            */
        }
    }

    public static void print(Module module) {
        System.out.println("module " + module.getName() + "\n");
        System.out.println("imports");

        for (String name : module.getImports()) {
            System.out.println("  " + name);
        }

        for (Section section : module.getSections()) {
            System.out.println("context-free syntax");

            for (Production production : section.getProductions()) {
                System.out.println("  " + production);
            }
        }
    }
}
