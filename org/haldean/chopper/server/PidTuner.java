package org.haldean.chopper.server;

import org.apache.commons.math3.distribution.MultivariateNormalDistribution;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.HashSet;

public class PidTuner implements Updatable {
    private static enum TuningAxis { DX, DY, DZ, DT };
    // Axis to tune. Either DX or DY.
    private TuningAxis mAxis;

    // Search Parameters

    // StDev for expanding nodes.
    private static final double expStdev = 2.0e-6;
    // Uniform distribution for creating initial nodes
    private static final double initRangeStart = 0;
    private static final double initRangeEnd = 1.0e-4;
    // Number of nodes to select from fringe.
    private static final int SELECT_NUM = 3;
    // Number of children to expand from each selected node.
    private static final int EXPAND_NUM = 3;

    private static SimpleDateFormat dateFormat =
	new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");

    private ArrayList<PidExperiment> mFringe;
    private HashSet<PidExperiment> mHistory;
    private int mFringeIndex;
    // Covariance matrix for expanding nodes.
    private double[][] mExpCovar;
    private Random rn;
    private BufferedWriter output;

    private boolean mEnabled = false;

    public PidTuner() {
	try {
	    String axis = ServerCreator.getArgument("pidTuning");
	    mEnabled = true;
	    if (axis.equals("dx")) {
		mAxis = TuningAxis.DX;
	    } else if (axis.equals("dy")) {
		mAxis = TuningAxis.DY;
	    } else if (axis.equals("dt")) {
		mAxis = TuningAxis.DT;
	    } else {
		mEnabled = false;
	    }
	} catch (IllegalArgumentException e) {
	    // Default state; no tuning.
	    return;
	}
	mFringe = new ArrayList<PidExperiment>();
	mHistory = new HashSet<PidExperiment>();
	mFringeIndex = 0;
	rn = new Random();
	try {
	    output = new BufferedWriter(new FileWriter("tuning_" + mAxis + ".txt", true));
	    output.write("# "+ dateFormat.format(new Date()));
	    output.newLine();
	    output.write("# tuning " + mAxis + " select_num " + SELECT_NUM + " expand_num " +
			 EXPAND_NUM);
	    output.newLine();
	} catch (IOException e) {
	    Debug.log("WARNING: PID TUNING LOGGING FAILED.");
	    e.printStackTrace();
	}
	mExpCovar = new double[3][3];
	for (int i = 0; i < 3; i++) {
	    for (int j = 0; j < 3; j++) {
		if (i == j) {
		    mExpCovar[i][j] = expStdev;
		} else {
		    mExpCovar[i][j] = 0.0;
		}
	    }
	}

	// Create list of PidExperiments
	double initRange = initRangeEnd - initRangeStart;
	for (int j = 0; j < SELECT_NUM * EXPAND_NUM; j++) {
	    double p = rn.nextDouble() * initRange + initRangeStart;
	    double i = rn.nextDouble() * initRange + initRangeStart;
	    double d = rn.nextDouble() * initRange + initRangeStart;
	    mFringe.add(new PidExperiment(p, i, d));
	}
    }

    public void update(String message) {
	if (!mEnabled) return;
	if (!message.startsWith("GUID:ERROR")) return;
	// retrieve error for my axis
	String parts[] = message.split(":");
	Double error = new Double(parts[2 + mAxis.ordinal()]);
	// Add error to current PidE
	PidExperiment currentExp = mFringe.get(mFringeIndex);
	currentExp.addError(error);
	// If PidE not done, return;
	if (!currentExp.isDone()) return;
	// PidE done: record result, increment index
	try {
	    if (output != null) {
		output.write(currentExp.gnuplotLine());
		output.newLine();
		output.flush();
	    }
	} catch (IOException e) {
	    Debug.log("WARNING: PID TUNING LOGGING FAILED");
	    e.printStackTrace();
	}
	mFringeIndex++;
	// If index <= mFringe.size(), send new PID values, return;
	if (mFringeIndex <= mFringe.size()) {
	    PidExperiment newExp = mFringe.get(mFringeIndex);
	    EnsignCrusher.tunePid(mAxis.ordinal(), 0, newExp.getP());
	    EnsignCrusher.tunePid(mAxis.ordinal(), 1, newExp.getI());
	    EnsignCrusher.tunePid(mAxis.ordinal(), 2, newExp.getD());
	    return;
	}
	// Fringe done: copy into history.
	mHistory.addAll(mFringe);
	mFringeIndex = 0;

	// stochastically choose the nodes to expand.
	ArrayList<PidExperiment> nodesToExpand = new ArrayList<PidExperiment>();
	for (int i = 0; i < SELECT_NUM; i++) {
	    // the usual method of drawing a random element probabilistically.
	    double scoreSum = 0.0;
	    for (PidExperiment p : mFringe) {
		scoreSum += p.getScore();
	    }
	    double pos = rn.nextDouble() * scoreSum;
	    double runningSum = 0.0;
	    for (int j = 0; j < mFringe.size(); j++) {
		runningSum += mFringe.get(j).getScore();
		if (runningSum >= pos) {
		    nodesToExpand.add(mFringe.get(j));
		    mFringe.remove(mFringe.get(j));  // To avoid duplicates.
		    break;
		}
	    }
	}
	// Expand the selected nodes into new Fringe, checking against history.
	mFringe.clear();
	for (PidExperiment parent : nodesToExpand) {
	    double[] mean = new double[3];
	    mean[0] = parent.getP();
	    mean[1] = parent.getI();
	    mean[2] = parent.getD();
	    MultivariateNormalDistribution dist =
		new MultivariateNormalDistribution(mean, mExpCovar);
	    for (int i = 0; i < EXPAND_NUM; i++) {
		double[] sample = dist.sample();
		PidExperiment child = new PidExperiment(sample[0], sample[1], sample[2]);
		while (mHistory.contains(child)) {
		    // Extraordinarily unlikely under this implementation of expansion,
		    // but we'll check anyway in case the expansion implementation changes.
		    sample = dist.sample();
		    child = new PidExperiment(sample[0], sample[1], sample[2]);
		}
		mFringe.add(child);
	    }
	}
    }
}
