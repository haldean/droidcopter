package org.haldean.chopper.server;

import java.util.ArrayList;

/**
 * Note: this class has a natural ordering that is inconsistent with equals.
 */
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

	// If we don't get to the start of a third cycle, this experiment
	// was really bad. Return MAX_VALUE.
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
	int truncateStart = fromPos;
	if (fromPos == 0) {
	    // Cycle a bit longer, to compensate for the weird start.
	    if (truncateStart >= mErrors.size()) return -1;
	    for (; mErrors.get(truncateStart) <= 0; truncateStart++) {
		// If value at index is nonpositive, continue until positive.
		if (truncateStart == mErrors.size() -1) return -1;
	    }
	}
	int negativeIndex = truncateStart;
	if (negativeIndex >= mErrors.size()) return -1;
	for (; mErrors.get(negativeIndex) >= 0; negativeIndex++) {
	    // If value at index nonnegative, continue until negative.
	    if (negativeIndex == mErrors.size() - 1) return -1;
	}
	int cycleStart = negativeIndex;
	if (cycleStart >= mErrors.size()) return -1;
	for (; mErrors.get(cycleStart) <= 0; cycleStart++) {
	    // If value at index is nonpositive, continue until positive.
	    if (cycleStart == mErrors.size() - 1) return -1;
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

    public int hashCode() {
	return new Double(getP()).hashCode() ^
	    new Double(getI()).hashCode() ^
	    new Double(getD()).hashCode();
    }

    public int compareTo(PidExperiment other) {
	if (getScore() == other.getScore()) return 0;
	if (getScore() < other.getScore()) return -1;
	// if (getScore() > other.getScore())
	return 1;
    }

    public String gnuplotLine() {
	return getP() + " " + getI() + " " + getD() + " " + getScore();
    }

    public static void main(String args[]) {
	// Tests.
	PidExperiment pe = new PidExperiment(0.0, 0.0, 0.0);
	pe.addError(1);
	pe.addError(-1);
	pe.addError(1);
	pe.addError(-1);
	if (2 != pe.getNextCycle(0)) {
	    System.out.println("TEST #1 FAILED. Expected 2, got " + pe.getNextCycle(0));
	    System.exit(1);
	}

	pe = new PidExperiment(0.0, 0.0, 0.0);
	pe.addError(-1);
	pe.addError(1);
	pe.addError(-1);
	pe.addError(1);
	pe.addError(-1);
	if (3 != pe.getNextCycle(0)) {
	    System.out.println("TEST #2 FAILED. Expected 3, got " + pe.getNextCycle(0));
	    System.exit(1);
	}

	System.out.println("SUCCESS");
    }
}
