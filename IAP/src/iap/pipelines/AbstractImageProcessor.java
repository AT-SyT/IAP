/*************************************************************************
 * Copyright (c) 2010 IPK Gatersleben, Group Image Analysis
 *************************************************************************/
package iap.pipelines;

import iap.blocks.data_structures.ImageAnalysisBlock;

import java.util.HashMap;
import java.util.TreeMap;

import org.BackgroundTaskStatusProviderSupportingExternalCall;
import org.SystemOptions;
import org.graffiti.util.InstanceLoader;

import de.ipk.ag_ba.image.operations.blocks.BlockPipeline;
import de.ipk.ag_ba.image.operations.blocks.properties.BlockResultSet;
import de.ipk.ag_ba.server.analysis.image_analysis_tasks.all.OptionsGenerator;

/**
 * @author pape, klukas
 */
public abstract class AbstractImageProcessor implements ImageProcessor {
	
	private final HashMap<String, BlockResultSet> blockResults;
	private int[] debugValidTrays;
	protected BackgroundTaskStatusProviderSupportingExternalCall status;
	
	public AbstractImageProcessor() {
		this(new HashMap<String, BlockResultSet>());
	}
	
	public AbstractImageProcessor(HashMap<String, BlockResultSet> blockResults) {
		this.blockResults = blockResults;
	}
	
	/*
	 * (non-Javadoc)
	 * @see de.ipk.ag_ba.image.analysis.maize.ImageProcessor#pipeline(de.ipk.ag_ba.image.structures.FlexibleImageSet,
	 * de.ipk.ag_ba.image.structures.FlexibleImageSet, int, de.ipk.ag_ba.image.structures.FlexibleImageStack, boolean, boolean)
	 */
	@Override
	public void execute(
			OptionsGenerator options)
			throws Exception {
		BlockPipeline pipeline = getPipeline(options.getOptions());
		pipeline.setValidTrays(debugValidTrays);
		
		pipeline.execute(options, blockResults, getStatus());
	}
	
	@Override
	public HashMap<String, BlockResultSet> getNumericResults() {
		return blockResults;
	}
	
	@Override
	public abstract BlockPipeline getPipeline(ImageProcessorOptionsAndResults options);
	
	@Override
	public TreeMap<Long, TreeMap<String, HashMap<String, BlockResultSet>>> postProcessPlantResults(
			TreeMap<String, TreeMap<Long, Double>> plandID2time2waterData2,
			TreeMap<Long, TreeMap<String, HashMap<String, BlockResultSet>>> analysisResults,
			BackgroundTaskStatusProviderSupportingExternalCall optStatus,
			ImageProcessorOptionsAndResults options) throws InstantiationException,
			IllegalAccessException, InterruptedException {
		BlockPipeline pipeline = getPipeline(options);
		return pipeline.postProcessPipelineResultsForAllAngles(
				plandID2time2waterData2,
				analysisResults,
				optStatus, options);
	}
	
	@Override
	public void setValidTrays(int[] debugValidTrays) {
		this.debugValidTrays = debugValidTrays;
	}
	
	@SuppressWarnings("unchecked")
	protected BlockPipeline getPipelineFromBlockList(
			SystemOptions so,
			String[] defaultBlockList) {
		defaultBlockList = so.getStringAll(
				"Analysis Blocks",
				"block",
				defaultBlockList);
		
		BlockPipeline p = new BlockPipeline();
		if (defaultBlockList != null)
			for (String b : defaultBlockList) {
				if (b != null && !b.startsWith("#") && !b.trim().isEmpty()) {
					try {
						Class<?> c = Class.forName(b, true, InstanceLoader.getCurrentLoader());
						if (ImageAnalysisBlock.class.isAssignableFrom(c))
							p.add((Class<? extends ImageAnalysisBlock>) c);
						else
							System.out.println("WARNING: ImageAnalysisBlock " + b + " is not assignable to " + ImageAnalysisBlock.class.getCanonicalName()
									+ "! (block is not added to pipeline!)");
					} catch (ClassNotFoundException cnfe) {
						System.out.println("ERROR: ImageAnalysisBlock " + b + " is unknown! (start block name with '#' to disable a specific block). Pipeline: "
								+ so.getString("DESCRIPTION", "pipeline_name", "(unnamed)", false));
					}
				}
			}
		return p;
	}
	
	@Override
	public void setStatus(BackgroundTaskStatusProviderSupportingExternalCall status) {
		this.status = status;
	}
	
	@Override
	public BackgroundTaskStatusProviderSupportingExternalCall getStatus() {
		return status;
	}
	
}
