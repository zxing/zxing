package de.mrab.gs1;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

public class WriteGS1Codes {

	public static class GS1Builder{
		private StringBuilder sb = new StringBuilder().append("\u001d");
		
		public GS1Builder append(String ai, String value){
			sb.append(ai);
			sb.append(value);
			return this;
		}
		
		public GS1Builder appendWithFnc1(String ai, String value){
			append(ai, value);
			sb.append("\u001d");
			return this;
		}
		
		public String toString(){
			return sb.toString();
		}
		
	}
	
	public static void writeImage(BitMatrix bm, String fileName){
		 
		BufferedImage image = null;
		
		int moduleSize = 10;
		image = new BufferedImage(bm.getWidth()*moduleSize, bm.getHeight()*moduleSize, BufferedImage.TYPE_BYTE_GRAY);
		
		for(int row = 0; row < image.getHeight();row++){
			for(int col = 0; col < image.getWidth();col++){
				image.setRGB(col, row, bm.get(col/moduleSize, row/moduleSize)? Color.BLACK.getRGB(): Color.WHITE.getRGB()); 
			}
		}

		try {
			ImageIO.write(image, "png",new File(fileName + ".png"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public static void writeCode(String content, String fileName){
		
		MultiFormatWriter w = new MultiFormatWriter();
		try {
			
			BitMatrix bm = w.encode(content, BarcodeFormat.DATA_MATRIX, 24, 24);

			writeImage(bm,fileName);
		} catch (WriterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		writeCode("\u001d00123456789012345675", "SSCC");
		writeCode("\u001d0112345678901231", "GTIN");
		writeCode("\u001d0112345678901235", "INVALID-GTIN");
		writeCode("\u001d011234567890123417140415", "GTIN-EXP");
		
		GS1Builder builder = new GS1Builder();
		builder.append("01", "11234456789219")
		.appendWithFnc1("21", "1000ABC31")
		.appendWithFnc1("10", "ABBCDEFLOT")
		.append("17", "151100")
		.append("11", "131027");
		
		writeCode(builder.toString(), "turkish");
		builder = new GS1Builder();
		builder.append("01", "11234456789219")
		.appendWithFnc1("21", "1000ABC31")
		.appendWithFnc1("10", "ABBCDEFLOT")
		.append("17", "151100")
		.append("11", "131027")
		.appendWithFnc1("393", "1978123");
		
		writeCode(builder.toString(), "turkishWithPrice");
	}

}
