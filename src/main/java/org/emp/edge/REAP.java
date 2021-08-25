package org.emp.edge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.emp.utils.VoronoiAdapt;

import java.util.List;

public class REAP {
    private static final Logger LOGGER = LogManager.getLogger(REAP.class);

    public static VoronoiAdapt.partitionDecision reap(int methodID, List<float[]> oxtsSet, List<Float> bwSet) {
        VoronoiAdapt myVoronoiAdapt = new VoronoiAdapt();
        VoronoiAdapt.partitionDecision decision;
        if (methodID == 1) {
            decision = myVoronoiAdapt.voronoiBasic(oxtsSet);  // dataSet[:, 0] is oxtsSet
        }
        else if (methodID == 2) {
            decision = myVoronoiAdapt.voronoiBasic(oxtsSet);
        }
        else if (methodID == 3) {
            decision = myVoronoiAdapt.voronoiBW(oxtsSet, bwSet);
        }
        else if (methodID == 4) {
            decision = myVoronoiAdapt.voronoiAdapt(oxtsSet, bwSet);
        }
        else {
            LOGGER.warn("Wrong algorithm ID: " + methodID);
            return null;
        }

        return decision; // decision.pbSet will be sent to each vehicle in Edge.java
    }
}