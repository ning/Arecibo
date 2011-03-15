package com.ning.arecibo.collector;

import java.io.Serializable;

public class ResolutionUtils implements ResolutionTagGenerator,Serializable {
	
	public String getResolutionTag(int reductionFactor) {
		if(reductionFactor == 1)
			return "";
		else
			return "_" + Integer.toString(reductionFactor) + "X";
	}
}
