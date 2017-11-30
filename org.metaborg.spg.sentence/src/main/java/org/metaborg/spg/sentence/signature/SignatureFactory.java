package org.metaborg.spg.sentence.signature;

import org.metaborg.sdf2table.grammar.IProduction;
import org.metaborg.sdf2table.grammar.NormGrammar;

public class SignatureFactory {
    public Signature fromGrammar(NormGrammar grammar) {
        // TODO: Ignore reject and bracket productions; they do not contribute to the signature

        for (IProduction production : grammar.getCacheProductionsRead().values()) {
            fromProduction(production);
        }

        return null;
    }

    private void fromProduction(IProduction production) {
        // TODO: If the production has a constructor annotation, then create an Operation for its right-hand side
    }
}
