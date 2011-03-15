package org.haldean.blob;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Segmenter {
    private int[] targetColor;
    private int targetArea;
    private int threshold;

    private static final int NO_LABEL = 0;
    private static final int EXPANSION_PASSES = 1;
    private static final int DEFAULT_THRESHOLD = 20;
    
    public Segmenter(int[] targetColor, int targetArea, int threshold) {
	this.targetColor = targetColor;
	this.targetArea = targetArea;
	this.threshold = threshold;
    }

    public String toString() {
	return String.format("SEGMENT;%d;%d;%d;%d;%d", targetColor[0], targetColor[1],
			     targetColor[2], targetArea, threshold);
    }

    public static Segmenter fromString(String str) {
	String[] parts = str.split(";");
	return new Segmenter(Arrays.copyOfRange(parts, 1, 4), parts[4], parts[5]);
    }

    public static Segmenter getSegmenterForPoint(Image input, int x, int y) {
	Segmenter seg = new Segmenter(input.getPixel(x, y), 0, DEFAULT_THRESHOLD);
	int[][] labels = seg.labelField(seg.getField(input)).labels;
	int area = 0, targetLabel = labels[x][y];

	for (int i=0; i<labels.length; i++) {
	    for (int j=0; j<labels[0].length; j++) {
		if (labels[i][j] == targetLabel) area++;
	    }
	}

	seg.targetArea = area;
	return seg;
    }

    public int[] segment(Image input) {
	int[] result = matchArea(labelField(getField(input)));
	return result;
    }
    
    private boolean[][] getField(Image input) {
	int[] size = input.getSize();
	boolean[][] field = new boolean[size[0]][size[1]];
	int norm;

	for (int i=0; i<size[0]; i++) {
	    for (int j=0; j<size[1]; j++) {
		int[] pixel = input.getPixel(i, j);

		/* Find L1 norm of this pixel. */
		norm = 0;
		for (int k=0; k<pixel.length; k++) {
		    norm += Math.abs(pixel[k] - targetColor[k]);
		}
		norm /= pixel.length;

		field[i][j] = norm <= threshold;
	    }
	}

	for (int k=0; k<EXPANSION_PASSES; k++) {
	    boolean[][] newField = new boolean[field.length][field[0].length];
	    for (int i=0; i<size[0]; i++) {
		for (int j=0; j<size[1]; j++) {
		    if (field[i][j]) {
			for (int di=Math.max(0, i-1); di<=Math.min(size[0] - 1, i+1); di++) {
			    for (int dj=Math.max(0, j-1); dj<=Math.min(size[1] - 1, j+1); dj++) {
				newField[di][dj] = true;
			    }
			}
		    }
		}
	    }
	    field = newField;
	}				

	return field;
    }

    private LabelledMatches labelField(boolean[][] field) {
	int[][] labels = new int[field.length][field[0].length];
	int label, lastLabel = 0;

	Map<Integer, Integer> equivalences = new HashMap<Integer, Integer>();
	Map<Integer, Area> areas = new HashMap<Integer, Area>();

	for (int i=0; i<field.length; i++) {
	    for (int j=0; j<field[0].length; j++) {
		if (!field[i][j]) {
		    labels[i][j] = NO_LABEL;
		} else {
		    boolean above = i > 0 && field[i-1][j];
		    boolean left = j > 0 && field[i][j-1];

		    if (above && left && labels[i][j-1] != labels[i-1][j]) {
			int parent = Math.min(labels[i-1][j], labels[i][j-1]);
			int child = Math.max(labels[i-1][j], labels[i][j-1]);

			equivalences.put(child, parent);
			labels[i][j] = parent;
		    } else if (above && left) {
			labels[i][j] = labels[i][j-1];
		    } else if (above) {
			labels[i][j] = labels[i-1][j];
		    } else if (left) {
			labels[i][j] = labels[i][j-1];
		    } else {
			labels[i][j] = ++lastLabel;
		    }
		}
	    }
	}

	for (int i=0; i<field.length; i++) {
	    for (int j=0; j<field[0].length; j++) {
		if (labels[i][j] != NO_LABEL) {
		    while (equivalences.containsKey(labels[i][j])) {
			labels[i][j] = equivalences.get(labels[i][j]);
		    }
		    label = labels[i][j];

		    if (!areas.containsKey(label)) {
			areas.put(label, new Area());
		    }

		    Area area = areas.get(label);
		    area.label = label;
		    area.size++;
		    area.x += j;
		    area.y += i;
		}
	    }
	}

	for (Area a : areas.values()) {
	    a.x /= a.size;
	    a.y /= a.size;
	}

	return new LabelledMatches(areas.values(), labels);
    }

    private int[] matchArea(LabelledMatches matches) {
	Area bestArea = null;
	int bestAreaDifference = 0, areaDifference;

	for (Area a : matches.areas) {
	    if (targetArea == 0) {
		areaDifference = -a.size;
	    } else {
		areaDifference = Math.abs(a.size - targetArea);
	    }

	    if (bestArea == null || bestAreaDifference > areaDifference) {
		bestAreaDifference = areaDifference;
		bestArea = a;
	    }
	}

	targetArea = bestArea.size;
	return new int[] {bestArea.x, bestArea.y};
    }

    private class Area {
	int size = 0;
	int label = 0;
	int x = 0;
	int y = 0;
    }

    private class LabelledMatches {
	Collection<Area> areas;
	int[][] labels;

	public LabelledMatches(Collection<Area> areas, int[][] labels) {
	    this.areas = areas;
	    this.labels = labels;
	}
    }
}