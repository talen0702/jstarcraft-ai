package com.jstarcraft.ai.model.neuralnetwork.learn;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.AdamUpdater;
import org.nd4j.linalg.learning.GradientUpdater;
import org.nd4j.linalg.learning.config.Adam;

import com.jstarcraft.ai.model.neuralnetwork.learn.AdamLearner;
import com.jstarcraft.ai.model.neuralnetwork.learn.Learner;

public class AdamLearnerTestCase extends LearnerTestCase {

    @Override
    protected GradientUpdater<?> getOldFunction(long[] shape) {
        Adam configuration = new Adam();
        GradientUpdater<?> oldFunction = new AdamUpdater(configuration);
        int length = (int) (shape[0] * configuration.stateSize(shape[1]));
        INDArray view = Nd4j.zeros(length);
        oldFunction.setStateViewArray(view, shape, 'c', true);
        return oldFunction;
    }

    @Override
    protected Learner getNewFunction(long[] shape) {
        Learner newFuction = new AdamLearner();
        return newFuction;
    }

}
