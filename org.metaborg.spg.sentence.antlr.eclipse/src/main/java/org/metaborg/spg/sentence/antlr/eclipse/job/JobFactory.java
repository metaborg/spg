package org.metaborg.spg.sentence.antlr.eclipse.job;

import org.metaborg.spg.sentence.antlr.eclipse.config.DifferenceJobConfig;

public interface JobFactory {
    LiberalDifferenceJob createLiberalDifferenceJob(DifferenceJobConfig config);

    RestrictiveDifferenceJob createRestrictiveDifferenceJob(DifferenceJobConfig config);
}
