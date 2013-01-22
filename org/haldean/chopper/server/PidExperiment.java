package org.haldean.chopper.server;

import java.util.ArrayList;

public class PidExperiment implements Comparable<PidExperiment> {
    public static final long MAX_TIME_MILLIS = 10000;
    public static final double THRESHOLD_ANGLE = 20.0;
    public static final double THRESHOLD_MULTIPLIER = 10000;  // Exact value unimportant.

    private double mP;
    private double mI;
    private double mD;
    private ArrayList<Double> mErrors;
    private long mStartTime;
    private double mScore;

    public PidExperiment(double p, double i, double d) {
	mP = p;
	mI = i;
	mD = d;
	mErrors = new ArrayList<Double>();
	mScore = -1;
	mStartTime = 0;
    }

    public double getP() {
	return mP;
    }

    public double getI() {
	return mI;
    }

    public double getD() {
	return mD;
    }

    public void addError(double newError) {
	if (mErrors.isEmpty()) {
	    mStartTime = System.currentTimeMillis();
	}
	mErrors.add(newError);
    }

    public boolean isDone() {
	// True if time limit expired, or two full sin curves are complete.
	if (isTimeUp()) return true;

	int first = getNextCycle(0);
	if (first == -1) return false;

	int second = getNextCycle(first);
	if (second == -1) return false;

	int third = getNextCycle(second);
	if (third == -1) return false;

	return true;
    }

    public double getScore() {
	// mScore saved once processed (lazy evaluation)
	if (mScore != -1) return mScore;

	if (isTimeUp()) {
	    mScore =  Integer.MAX_VALUE;
	    return mScore;
	}

	int first = getNextCycle(0);
	if (first == -1) {
	    mScore = Integer.MAX_VALUE;
	    return mScore;
	}
	int second = getNextCycle(first);
	if (second == -1) {
	    mScore = Integer.MAX_VALUE;
	    return mScore;
	}
	int third = getNextCycle(second);
	if (third == -1) {
	    mScore = Integer.MAX_VALUE;
	    return mScore;
	}

	double score = 0.0;
	for (int i = first; i < third; i++) {
	    if (mErrors.get(i) < THRESHOLD_ANGLE) {
		score += Math.abs(mErrors.get(i));
	    } else {
		score += Math.abs(mErrors.get(i)) * THRESHOLD_MULTIPLIER;
	    }
	}
	mScore = score;
	return mScore;
    }

    private int getNextCycle(int fromPos) {
	int negativeIndex = fromPos;
	for (; mErrors.get(negativeIndex) >= 0; negativeIndex++) {
	    // If value at index nonnegative, continue until negative.
	    if (negativeIndex == mErrors.size()) return -1;
	}
	int cycleStart = negativeIndex;
	for (; mErrors.get(cycleStart) <= 0; cycleStart++) {
	    // If value at index is nonpositive, continue until positive.
	    if (negativeIndex == mErrors.size()) return -1;
	}
	return cycleStart;
    }

    private boolean isTimeUp() {
	if (mStartTime == 0) return false;

	if (System.currentTimeMillis() - mStartTime > MAX_TIME_MILLIS) {
	    return true;
	} else {
	    return false;
	}
    }

    /** Does not compare scores, only PID values. */
    public boolean equals(PidExperiment other) {
	return (getP() == other.getP()) && (getI() == other.getI()) && (getD() == other.getD());
    }

    public int compareTo(PidExperiment other) {
	if (getScore() == other.getScore()) return 0;
	if (getScore() < other.getScore()) return -1;
	// if (getScore() > other.getScore())
	return 1;
    }
}
