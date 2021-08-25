package org.emp.utils;

import org.emp.utils.EmpUnitTest;
import org.junit.jupiter.api.Test;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.ArrayList;

public class VoronoiTest extends EmpUnitTest{
    private static final Logger LOGGER = LogManager.getLogger(VoronoiTest.class);

    @Test
    public void testVoronoi() {
        LOGGER.info("Test Voronoi ...");

        // Prepare Data
        double[] xValuesIn = {0.5, 1.5, 1.5, 0.5, 1.0};
        double[] yValuesIn = {1.5, 1.5, 0.5, 0.5, 1.0};
        double minX = 0.0;
        double maxX = 2.0;
        double minY = 0.0;
        double maxY = 2.0;

        Voronoi myVoronoi = new Voronoi(0.0);
        myVoronoi.generateVoronoi(xValuesIn, yValuesIn, minX, maxX, minY, maxY);

        myVoronoi.printAllSites();
        myVoronoi.printAllGraphEdges();

        List<int[]> allNeighborPairs = myVoronoi.getAllNeighborPairs();

        for(int i = 0; i < allNeighborPairs.size(); i++) {
            int[] pair = allNeighborPairs.get(i);
            System.out.println(pair[0] + ", " + pair[1]);
        }
    }
}