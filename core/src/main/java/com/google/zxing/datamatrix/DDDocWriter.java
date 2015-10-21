package com.google.zxing.datamatrix;

import com.google.zxing.datamatrix.encoder.HighLevelEncoder;

public class DDDocWriter extends DataMatrixWriter {

    public DDDocWriter(){
        super(HighLevelEncoder.DDDOC_ENCODATION);
    }
}
