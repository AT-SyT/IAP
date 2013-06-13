/**
 * 
 */
package iap.blocks.unused;

import iap.blocks.data_structures.AbstractBlock;
import java.util.HashSet;

import de.ipk.ag_ba.image.operation.ImageOperation;
import de.ipk.ag_ba.image.structures.Image;
import de.ipk.ag_ba.image.structures.CameraType;

/**
 * Remove "peper and salt" noise from Fluo mask.
 * 
 * @author Pape, Klukas
 */
public class BlMedianFilter extends AbstractBlock {
	
	@Override
	protected Image processMask(Image mask) {
		if (mask == null || ! getBoolean("Process " + mask.getCameraType(), true))
			return mask;
		
		Image medianMask = new ImageOperation(mask)
				.medianFilter32Bit()
				.border(2)
				.getImage();
		
		return new ImageOperation(mask).applyMask_ResizeSourceIfNeeded(medianMask, options.getBackground()).getImage();
	}
	
	@Override
	public HashSet<CameraType> getCameraInputTypes() {
		HashSet<CameraType> res = new HashSet<CameraType>();
		res.add(CameraType.VIS);
		res.add(CameraType.FLUO);
		res.add(CameraType.NIR);
		res.add(CameraType.IR);
		return res;
	}
	
	@Override
	public HashSet<CameraType> getCameraOutputTypes() {
		HashSet<CameraType> res = new HashSet<CameraType>();
		res.add(CameraType.VIS);
		res.add(CameraType.FLUO);
		res.add(CameraType.NIR);
		res.add(CameraType.IR);
		return res;
	}
	
}