package org.metaborg.spg.sentence.signature;

import org.metaborg.core.language.ILanguageImpl;

public interface SignatureReaderFactory {
    SignatureReader create(ILanguageImpl strategoLanguageImpl);
}
