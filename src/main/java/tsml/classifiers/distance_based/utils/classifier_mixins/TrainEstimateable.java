package tsml.classifiers.distance_based.utils.classifier_mixins;

import evaluation.storage.ClassifierResults;

/**
 * Purpose: control estimating the train error.
 * <p>
 * Contributors: goastler
 */
public interface TrainEstimateable {

    ClassifierResults getTrainResults();

    boolean getEstimateOwnPerformance();

    void setEstimateOwnPerformance(boolean estimateOwnPerformance);
}
