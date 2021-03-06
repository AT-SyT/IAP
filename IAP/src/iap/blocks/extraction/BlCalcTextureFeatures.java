package iap.blocks.extraction;

import iap.blocks.data_structures.AbstractSnapshotAnalysisBlock;
import iap.blocks.data_structures.BlockType;
import iap.blocks.data_structures.CalculatedProperty;
import iap.blocks.data_structures.CalculatedPropertyDescription;
import iap.blocks.data_structures.CalculatesProperties;
import iap.pipelines.ImageProcessorOptionsAndResults.CameraPosition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.stream.IntStream;

import org.ErrorMsg;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import de.ipk.ag_ba.gui.picture_gui.StreamBackgroundTaskHelper;
import de.ipk.ag_ba.image.operation.FirstOrderTextureFeatures;
import de.ipk.ag_ba.image.operation.GLCMTextureFeatures;
import de.ipk.ag_ba.image.operation.ImageMoments;
import de.ipk.ag_ba.image.operation.ImageOperation;
import de.ipk.ag_ba.image.operation.ImageTexture;
import de.ipk.ag_ba.image.operation.PositionAndColor;
import de.ipk.ag_ba.image.operation.channels.Channel;
import de.ipk.ag_ba.image.operation.channels.ChannelProcessing;
import de.ipk.ag_ba.image.structures.CameraType;
import de.ipk.ag_ba.image.structures.Image;
import de.ipk.ag_ba.image.structures.ImageStack;
import de.ipk_gatersleben.ag_pbi.mmd.experimentdata.NumericMeasurement3D;

/**
 * Texture extraction for foreground pixels, mask-size of inspected area based on distance map (TextureCalculationMode.SKELETON is recommended).
 * TextureCalculationMode.SKELETON - calculation only for skeleton pixels (mean)
 * TextureCalculationMode.WHOLE_IMAGE - uses whole image for texture calculation
 * TextureCalculationMode.VISUALIZE - calculates all texture features for every pixel and opens debug stack.
 * 
 * @author pape
 */
public class BlCalcTextureFeatures extends AbstractSnapshotAnalysisBlock implements CalculatesProperties {
	
	Double markerDistanceHorizontally = null;
	
	private TextureCalculationMode calculationMode;
	
	private int minDistance = 3;
	private int masksize = 5;
	
	private int maxDistance = 15;
	
	private static final double sqrtOfTwoDivTwo = Math.sqrt(2) / 2.0;
	
	@Override
	protected void prepare() {
		super.prepare();
		
		markerDistanceHorizontally = optionsAndResults.getCalculatedBlueMarkerDistance();
		
		// calculate for whole plant or skel points
		ArrayList<String> possibleValues = new ArrayList<String>(Arrays.asList(TextureCalculationMode.getMethods()));
		String calculationMode = optionsAndResults.getStringSettingRadio(this, "Calculation Mode", TextureCalculationMode.SKELETON.name(), possibleValues);
		this.calculationMode = TextureCalculationMode.valueOf(calculationMode);
		
		masksize = getInt("Masksize For Vizualization", 5);
		minDistance = getInt("Minimal EDM Value", 3);
		maxDistance = getInt("Maximal EDM Value", 15);
	}
	
	@Override
	protected Image processVISmask() {
		if (input().masks().vis() != null) {
			Image input = input().masks().vis();
			calcTextureFeatures(input, CameraType.VIS, masksize, optionsAndResults.getCameraPosition(), input().images().getVisInfo());
			return input().masks().vis();
		} else
			return null;
	}
	
	private void calcTextureFeatures(Image img, CameraType ct, int masksize, CameraPosition cp, NumericMeasurement3D imageRef) {
		if (ct == CameraType.VIS) {
			// get skeleton-image and apply distance map
			Image skel = null;
			if (calculationMode == TextureCalculationMode.SKELETON)
				skel = getResultSet().getImage("skeleton_" + ct.toString(), false).getImage();
			
			for (Channel c : Channel.values()) {
				ImageOperation ch_img = img.io().channels().get(c);
				if (calculationMode == TextureCalculationMode.WHOLE_IMAGE) {
					calcTextureForImage(new ImageOperation(ChannelProcessing.getGrayImageAs2dArray(ch_img.getImage())), c, ct, cp, imageRef);
				}
				if (calculationMode == TextureCalculationMode.SKELETON && skel != null) {
					ImageOperation dist = ch_img.bm().edmFloatClipped().show("distance clipped", debugValues);
					Image mapped = dist.applyMask(skel).getImage().show("mapped", debugValues);
					ArrayList<PositionAndColor> mappedCoordinates = mapped.io().getForegroundCoordinatesAndIntensities();
					calcTextureForSkeleton(mappedCoordinates, new ImageOperation(ChannelProcessing.getGrayImageAs2dArray(ch_img.getImage())), mapped.io(), c,
							ct, cp, imageRef);
				}
				if (calculationMode == TextureCalculationMode.VISUALIZE) {
					calcTextureForVizualization(new ImageOperation(ChannelProcessing.getGrayImageAs2dArray(ch_img.crop().resize(0.1).getImage())), masksize, c, ct);
				}
			}
		}
	}
	
	private void calcTextureForImage(ImageOperation img, Channel c, CameraType cameraType, CameraPosition cp, NumericMeasurement3D imageRef) {
		ImageTexture it = new ImageTexture(img.getImage());
		it.calcTextureFeatures();
		it.calcGLCMTextureFeatures();
		
		for (FirstOrderTextureFeatures tf : FirstOrderTextureFeatures.values()) {
			getResultSet().setNumericResult(getBlockPosition(),
					new Trait(cp, cameraType, TraitCategory.TEXTURE, c + "." + tf),
					it.firstOrderFeatures.get(tf), null, this, imageRef);
		}
		
		for (GLCMTextureFeatures tf : GLCMTextureFeatures.values()) {
			getResultSet().setNumericResult(getBlockPosition(),
					new Trait(cp, cameraType, TraitCategory.TEXTURE, c + "." + tf),
					it.glcmFeatures.get(tf), null, this, imageRef);
		}
	}
	
	private void calcTextureForSkeleton(ArrayList<PositionAndColor> mappedCoordinates, ImageOperation ch_img, ImageOperation skel, Channel c,
			CameraType cameraType,
			CameraPosition cp,
			NumericMeasurement3D imageRef) {
		int[][] img2d = ch_img.getAs2D();
		int[][] mappedSkel2d = skel.getAs2D();
		int w = ch_img.getWidth();
		int h = ch_img.getHeight();
		int l = mappedCoordinates.size();
		
		HashMap<FirstOrderTextureFeatures, SummaryStatistics> firstArrays = new HashMap<>();
		
		for (FirstOrderTextureFeatures name : FirstOrderTextureFeatures.values()) {
			firstArrays.put(name, new SummaryStatistics());
		}
		
		HashMap<GLCMTextureFeatures, SummaryStatistics> glcmArrays = new HashMap<>();
		
		for (GLCMTextureFeatures f : GLCMTextureFeatures.values()) {
			glcmArrays.put(f, new SummaryStatistics());
		}
		
		// new StreamBackgroundTaskHelper<Integer>("Texture analysis").process(
		// IntStream.range(0, l), (idx) -> {
		for (int idx = 0; idx < l; idx++) {
			
			if (mappedCoordinates.get(idx).intensityInt == ImageOperation.BACKGROUND_COLORint)
				continue;
			
			int masksize = mappedCoordinates.get(idx).intensityInt;
			
			if (masksize < minDistance)
				continue;
			
			if (masksize > maxDistance)
				masksize = maxDistance;
			
			masksize = (masksize * 2) + 1; // double masksize
			int halfmask = masksize / 2;
			int[] mask = new int[masksize * masksize];
			int[] skelMask = new int[masksize * masksize];
			
			int x = mappedCoordinates.get(idx).x;
			int y = mappedCoordinates.get(idx).y;
			
			for (int i = 0; i < mask.length; i++) {
				mask[i] = ImageOperation.BACKGROUND_COLORint;
				skelMask[i] = ImageOperation.BACKGROUND_COLORint;
			}
			
			int sumSkelValInMask = 0;
			
			int count = 0;
			for (int xMask = -halfmask; xMask <= halfmask; xMask++) {
				for (int yMask = -halfmask; yMask <= halfmask; yMask++) {
					if (x + xMask >= 0 && x + xMask < w && y + yMask >= 0 && y + yMask < h) {
						mask[count] = img2d[x + xMask][y + yMask];
						skelMask[count] = mappedSkel2d[x + xMask][y + yMask];
						sumSkelValInMask += (mappedSkel2d[x + xMask][y + yMask] & 0x0000ff);
					}
					count++;
				}
			}
			
			ImageMoments im = new ImageMoments(skelMask, masksize, masksize);
			double angle = im.calcOmega(ImageOperation.BACKGROUND_COLORint);
			double newMasksize = masksize * sqrtOfTwoDivTwo;
			ImageOperation rot = new Image(masksize, masksize, mask).io().rotate(-angle * 180 / Math.PI);
			int halfdiff_disired = (int) (rot.getWidth() - newMasksize) / 2;
			ImageOperation crop = rot.cropAbs(halfdiff_disired, rot.getWidth() - halfdiff_disired, halfdiff_disired, rot.getWidth() - halfdiff_disired);
			
			final int f_masksize = crop.getWidth();
			ImageTexture it = new ImageTexture(crop.getAs1D(), f_masksize, f_masksize);
			
			it.calcTextureFeatures();
			
			for (FirstOrderTextureFeatures f : FirstOrderTextureFeatures.values()) {
				firstArrays.get(f).addValue(it.firstOrderFeatures.get(f));
			}
			
			it.calcGLCMTextureFeatures();
			
			for (GLCMTextureFeatures f : GLCMTextureFeatures.values()) {
				glcmArrays.get(f).addValue(it.glcmFeatures.get(f));
			}
			idx += sumSkelValInMask / count / 2d;
		}
		// , (t, e) -> {
		// ErrorMsg.addErrorMessage(new RuntimeException(e));
		// });
		
		for (FirstOrderTextureFeatures tf : FirstOrderTextureFeatures.values()) {
			getResultSet().setNumericResult(getBlockPosition(),
					new Trait(cp, cameraType, TraitCategory.TEXTURE, c + "." + tf),
					firstArrays.get(tf).getMean(), null, this, imageRef);
		}
		
		for (GLCMTextureFeatures tf : GLCMTextureFeatures.values()) {
			getResultSet().setNumericResult(getBlockPosition(),
					new Trait(cp, cameraType, TraitCategory.TEXTURE, c + "." + tf),
					glcmArrays.get(tf).getMean(), null, this, imageRef);
		}
	}
	
	private void calcTextureForVizualization(ImageOperation img, int masksize, Channel c, CameraType cameraType) {
		int w = img.getWidth();
		int h = img.getHeight();
		
		// double masksize
		masksize = masksize * 2 + 1;
		int halfmask = masksize / 2;
		int[][] img2d = img.getAs2D();
		int[] temp = new int[masksize * masksize];
		final int f_masksize = masksize;
		
		HashMap<FirstOrderTextureFeatures, double[][]> firstArrays = new HashMap<>();
		
		for (FirstOrderTextureFeatures name : FirstOrderTextureFeatures.values()) {
			firstArrays.put(name, new double[w][h]);
		}
		
		HashMap<GLCMTextureFeatures, double[][]> glcmArrays = new HashMap<>();
		
		for (GLCMTextureFeatures f : GLCMTextureFeatures.values()) {
			glcmArrays.put(f, new double[w][h]);
		}
		
		new StreamBackgroundTaskHelper<Integer>("Texture analysis for visualization").process(
				IntStream.range(0, w), (x) -> {
					for (int y = 0; y < h; y++) {
						
						if (img2d[x][y] == ImageOperation.BACKGROUND_COLORint)
							continue;
						
						for (int i = 0; i < temp.length; i++)
							temp[i] = ImageOperation.BACKGROUND_COLORint;
						
						int count = 0;
						for (int xMask = -halfmask; xMask < halfmask; xMask++) {
							for (int yMask = -halfmask; yMask < halfmask; yMask++) {
								if (x + xMask >= 0 && x + xMask < w && y + yMask >= 0 && y + yMask < h) {
									temp[count] = img2d[x + xMask][y + yMask];
								}
								count++;
							}
						}
						ImageTexture it = new ImageTexture(temp, f_masksize, f_masksize);
						
						it.calcTextureFeatures();
						
						for (FirstOrderTextureFeatures f : FirstOrderTextureFeatures.values()) {
							double[][] arr = firstArrays.get(f);
							arr[x][y] = it.firstOrderFeatures.get(f);
						}
						
						it.calcGLCMTextureFeatures();
						
						for (GLCMTextureFeatures f : GLCMTextureFeatures.values()) {
							double[][] arr = glcmArrays.get(f);
							arr[x][y] = it.glcmFeatures.get(f);
						}
					}
				}, (t, e) -> {
					ErrorMsg.addErrorMessage(new RuntimeException(e));
				});
		
		ImageStack is = new ImageStack();
		is.addImage(c.toString(), img.getImage());
		
		{
			Iterator<Entry<FirstOrderTextureFeatures, double[][]>> iter = firstArrays.entrySet().iterator();
			while (iter.hasNext()) {
				Entry<FirstOrderTextureFeatures, double[][]> pairs = iter.next();
				is.addImage(pairs.getKey().toString(), new Image(normalizeToInt(pairs.getValue())));
				iter.remove(); // avoids a ConcurrentModificationException
			}
		}
		
		{
			Iterator<Entry<GLCMTextureFeatures, double[][]>> iter = glcmArrays.entrySet().iterator();
			while (iter.hasNext()) {
				Entry<GLCMTextureFeatures, double[][]> pairs = iter.next();
				is.addImage(pairs.getKey().toString(), new Image(normalizeToInt(pairs.getValue())));
				iter.remove(); // avoids a ConcurrentModificationException
			}
		}
		is.show("debug texture stack " + c.name());
	}
	
	private static int[][] normalizeToInt(double[][] img) {
		int width = img.length;
		int height = img[0].length;
		double[] temp1d = new double[width * height];
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				temp1d[x + width * y] = img[x][y];
			}
		}
		
		temp1d = normalize(temp1d);
		int x, y;
		int[][] res = new int[width][height];
		for (int idx = 0; idx < width * height; idx++) {
			x = idx % width;
			y = idx / width;
			res[x][y] = (int) temp1d[idx];
		}
		
		return res;
	}
	
	private static double[] normalize(double[] img) {
		double[] res = new double[img.length];
		double max = Double.MIN_VALUE;
		double min = Double.MAX_VALUE;
		double temp = 0.0;
		double val = 0;
		for (int idx = 0; idx < img.length; idx++) {
			temp = img[idx];
			if (temp < min)
				min = temp;
			if (temp > max)
				max = temp;
		}
		for (int idx = 0; idx < img.length; idx++) {
			val = (255 * ((img[idx] - min) / (max - min)));
			val = (Math.sqrt(val / 255d) * 255d);
			res[idx] = val;
		}
		return res;
	}
	
	@Override
	public HashSet<CameraType> getCameraInputTypes() {
		HashSet<CameraType> res = new HashSet<CameraType>();
		res.add(CameraType.VIS);
		// res.add(CameraType.FLUO);
		// res.add(CameraType.NIR);
		// res.add(CameraType.IR);
		return res;
	}
	
	@Override
	public HashSet<CameraType> getCameraOutputTypes() {
		return getCameraInputTypes();
	}
	
	@Override
	public BlockType getBlockType() {
		return BlockType.FEATURE_EXTRACTION;
	}
	
	@Override
	public String getName() {
		return "Calculate Texture Features";
	}
	
	@Override
	public String getDescription() {
		return "Calculates texture features for all foreground pixels.";
	}
	
	@Override
	public String getDescriptionForParameters() {
		return "<ul><li>Calculation Mode - Feature calculation for skeletoon pixels, whole image or vizualization of all features."
				+ "<li>Masksize For Vizualization - Used masksize during visualization mode."
				+ "<li>Minimal EDM Value - Used masksize during skeleton calculation mode.</ul>";
	}
	
	@Override
	public CalculatedPropertyDescription[] getCalculatedProperties() {
		ArrayList<CalculatedPropertyDescription> desList = new ArrayList<CalculatedPropertyDescription>();
		for (Channel c : Channel.values()) {
			for (FirstOrderTextureFeatures tf : FirstOrderTextureFeatures.values()) {
				desList.add(new CalculatedProperty(c + "." + tf, tf.getNiceName()
						+ " - first order texture property (independent of pixel neighbors). Calculated on grayscale image derived from channel " + c.getNiceName()
						+ "." +
						(tf.getReferenceLink() != null ? " Further information: <a href='" + tf.getReferenceLink() + "'>Link</a>." : "")));
			}
			
			for (GLCMTextureFeatures tf : GLCMTextureFeatures.values()) {
				desList
						.add(new CalculatedProperty(
								c + "." + tf,
								tf.getNiceName()
										+ " - Grey Level Co-occurrence Matrix (GLCM) texture property (independent of pixel neighbors). Calculated on grayscale image derived from channel "
										+ c.getNiceName() + "." +
										(tf.getReferenceLink() != null ? " Further information: <a href='" + tf.getReferenceLink() + "'>Link</a>." : "")));
			}
		}
		return desList.toArray(new CalculatedPropertyDescription[desList.size()]);
	}
	
	private enum TextureCalculationMode {
		WHOLE_IMAGE, SKELETON, VISUALIZE;
		
		private static String[] mStrings;
		
		public static String[] getMethods() {
			if (mStrings == null) {
				TextureCalculationMode[] mVals = TextureCalculationMode.values();
				mStrings = new String[mVals.length];
				for (int i = 0; i < mVals.length; i++)
					mStrings[i] = mVals[i].name();
			}
			return mStrings;
		}
	}
}
