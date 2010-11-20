package com.google.zxing.aztec;

import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.DetectorResult;

public class AztecDetectorResult extends DetectorResult {

	private final boolean compact;
	private final int nbDatablocks;
	private final int nbLayers;
	
	public AztecDetectorResult(BitMatrix bits, ResultPoint[] points, boolean compact, int nbDatablocks, int nbLayers) {
		super(bits, points);
		this.compact = compact;
		this.nbDatablocks = nbDatablocks;
		this.nbLayers = nbLayers;
	}

	public int getNbLayers() {
		return nbLayers;
	}

	public int getNbDatablocks() {
		return nbDatablocks;
	}

	public boolean isCompact() {
		return compact;
	}

}
