/*
 * Copyright (C) 2015 Edward Raff <Raff.Edward@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.jstarcraft.ai.jsat.datatransform.visualization;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jstarcraft.ai.jsat.SimpleDataSet;
import com.jstarcraft.ai.jsat.classifiers.CategoricalData;
import com.jstarcraft.ai.jsat.classifiers.DataPoint;
import com.jstarcraft.ai.jsat.datatransform.visualization.Isomap;
import com.jstarcraft.ai.jsat.linear.DenseMatrix;
import com.jstarcraft.ai.jsat.linear.Matrix;
import com.jstarcraft.ai.jsat.linear.Vec;
import com.jstarcraft.ai.jsat.linear.VecPaired;
import com.jstarcraft.ai.jsat.linear.distancemetrics.EuclideanDistance;
import com.jstarcraft.ai.jsat.linear.vectorcollection.VectorArray;
import com.jstarcraft.ai.jsat.utils.SystemInfo;
import com.jstarcraft.ai.jsat.utils.random.RandomUtil;

/**
 *
 * @author Edward Raff <Raff.Edward@gmail.com>
 */
public class IsomapTest {

    public IsomapTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of transform method, of class MDS.
     */
    @Test
    public void testTransform_DataSet_ExecutorService() {
        System.out.println("transform");

        ExecutorService ex = Executors.newFixedThreadPool(SystemInfo.LogicalCores);

        Random rand = RandomUtil.getRandom();
        Isomap instance = new Isomap();

        // create a small data set, and apply a random projection to a higher dimension
        // should still get similar nns when projected back down
        final int K = 5;// num neighbors we want to see stay the same
        instance.setNeighbors(K);

        Matrix orig_dim = new DenseMatrix(200, 2);
        for (int i = 0; i < orig_dim.rows(); i++) {
            int offset = i % 2 == 0 ? -5 : 5;
            for (int j = 0; j < orig_dim.cols(); j++) {
                orig_dim.set(i, j, rand.nextGaussian() + offset);
            }
        }

        Matrix s = Matrix.random(2, 10, rand);

        Matrix proj_data = orig_dim.multiply(s);

        SimpleDataSet proj = new SimpleDataSet(proj_data.cols(), new CategoricalData[0]);
        for (int i = 0; i < proj_data.rows(); i++)
            proj.add(new DataPoint(proj_data.getRow(i)));

        List<Set<Integer>> origNNs = new ArrayList<Set<Integer>>();
        VectorArray<VecPaired<Vec, Integer>> proj_vc = new VectorArray<VecPaired<Vec, Integer>>(new EuclideanDistance());
        for (int i = 0; i < proj.size(); i++)
            proj_vc.add(new VecPaired<Vec, Integer>(proj.getDataPoint(i).getNumericalValues(), i));

        for (int i = 0; i < proj.size(); i++) {
            Set<Integer> nns = new HashSet<Integer>();
            for (VecPaired<VecPaired<Vec, Integer>, Double> neighbor : proj_vc.search(proj_vc.get(i), K))
                nns.add(neighbor.getVector().getPair());
            origNNs.add(nns);
        }

        for (boolean cIsomap : new boolean[] { true, false }) {
            instance.setCIsomap(cIsomap);

            SimpleDataSet transformed_0 = instance.transform(proj, true);
            SimpleDataSet transformed_1 = instance.transform(proj);

            for (SimpleDataSet transformed : new SimpleDataSet[] { transformed_0, transformed_1 }) {
                double sameNN = 0;
                VectorArray<VecPaired<Vec, Integer>> trans_vc = new VectorArray<VecPaired<Vec, Integer>>(new EuclideanDistance());
                for (int i = 0; i < transformed.size(); i++)
                    trans_vc.add(new VecPaired<Vec, Integer>(transformed.getDataPoint(i).getNumericalValues(), i));

                for (int i = 0; i < orig_dim.rows(); i++) {
                    for (VecPaired<VecPaired<Vec, Integer>, Double> neighbor : trans_vc.search(trans_vc.get(i), K * 3))
                        if (origNNs.get(i).contains(neighbor.getVector().getPair()))
                            sameNN++;
                }

                double score = sameNN / (transformed.size() * K);
                if (cIsomap)// gets more leniency, as exagerating higher density means errors at the edge of
                            // the normal samples
                    assertTrue("was " + score, score >= 0.40);
                else
                    assertTrue("was " + score, score >= 0.50);
            }
        }

        ex.shutdown();
    }

}
