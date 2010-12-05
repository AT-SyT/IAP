package de.ipk.ag_ba.gui.navigation_actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeMap;

import org.ErrorMsg;
import org.graffiti.plugin.algorithm.ThreadSafeOptions;

import de.ipk.ag_ba.gui.ImageAnalysisCommandManager;
import de.ipk.ag_ba.gui.MainPanelComponent;
import de.ipk.ag_ba.gui.navigation_model.NavigationButton;
import de.ipk.ag_ba.gui.util.ExperimentReference;
import de.ipk.ag_ba.gui.util.MyExperimentInfoPanel;
import de.ipk.ag_ba.mongo.MongoDB;
import de.ipk.ag_ba.rmi_server.analysis.image_analysis_tasks.PerformanceAnalysisTask;
import de.ipk_gatersleben.ag_nw.graffiti.plugins.gui.editing_tools.script_helper.ConditionInterface;
import de.ipk_gatersleben.ag_nw.graffiti.plugins.gui.editing_tools.script_helper.Experiment;
import de.ipk_gatersleben.ag_nw.graffiti.plugins.gui.editing_tools.script_helper.ExperimentInterface;
import de.ipk_gatersleben.ag_nw.graffiti.plugins.gui.editing_tools.script_helper.Measurement;
import de.ipk_gatersleben.ag_nw.graffiti.plugins.gui.editing_tools.script_helper.NumericMeasurementInterface;
import de.ipk_gatersleben.ag_nw.graffiti.plugins.gui.editing_tools.script_helper.SampleInterface;
import de.ipk_gatersleben.ag_nw.graffiti.plugins.gui.editing_tools.script_helper.SubstanceInterface;
import de.ipk_gatersleben.ag_nw.graffiti.plugins.gui.layout_control.dbe.RunnableWithMappingData;
import de.ipk_gatersleben.ag_nw.graffiti.plugins.misc.threading.SystemAnalysis;
import de.ipk_gatersleben.ag_pbi.mmd.experimentdata.Condition3D;
import de.ipk_gatersleben.ag_pbi.mmd.experimentdata.MappingData3DPath;
import de.ipk_gatersleben.ag_pbi.mmd.experimentdata.Sample3D;
import de.ipk_gatersleben.ag_pbi.mmd.experimentdata.Substance3D;
import de.ipk_gatersleben.ag_pbi.mmd.experimentdata.images.ImageData;

/**
 * @author klukas
 */
public class PerformanceTestAction extends AbstractNavigationAction {
	private String login;
	private String pass;
	private ExperimentReference experiment;
	NavigationButton src = null;
	MainPanelComponent mpc;
	private Experiment experimentResult;

	// used when started as remote analysis task
	private RunnableWithMappingData resultReceiver;
	private int workOnSubset;
	private int numberOfSubsets;

	public PerformanceTestAction(String login, String pass, ExperimentReference experiment) {
		super("Test performance by reading experiment content");
		this.login = login;
		this.pass = pass;
		this.experiment = experiment;
		this.experimentResult = null;
	}

	public PerformanceTestAction() {
		super("Test performance");
	}

	@Override
	public void performActionCalculateResults(final NavigationButton src) {
		this.src = src;

		if (experimentResult != null)
			return;

		try {
			ExperimentInterface res = experiment.getData();

			ArrayList<NumericMeasurementInterface> workload = new ArrayList<NumericMeasurementInterface>();

			int workIndex = 0;
			for (SubstanceInterface m : res) {
				Substance3D m3 = (Substance3D) m;
				for (ConditionInterface s : m3) {
					Condition3D s3 = (Condition3D) s;
					for (SampleInterface sd : s3) {
						Sample3D sd3 = (Sample3D) sd;
						for (Measurement md : sd3.getAllMeasurements()) {
							workIndex++;
							if (resultReceiver == null || workIndex % numberOfSubsets == workOnSubset)
								if (md instanceof ImageData) {
									workload.add((NumericMeasurementInterface) md);
								}
						}
					}
				}
			}

			if (status != null)
				status.setCurrentStatusText1("Workload: " + workload.size() + " images");

			final ThreadSafeOptions tso = new ThreadSafeOptions();
			tso.setInt(1);

			// for (NumericMeasurementInterface id : workload) {
			// System.out.println("Input: " + (((ImageData)
			// id).getURL()).getDetail());
			// }

			PerformanceAnalysisTask task = new PerformanceAnalysisTask();
			// task.addPreprocessor(new CutImagePreprocessor());
			TreeMap<Long, String> times = new TreeMap<Long, String>();
			Collection<NumericMeasurementInterface> statRes = new ArrayList<NumericMeasurementInterface>();
			for (int pi = SystemAnalysis.getNumberOfCPUs(); pi >= 1; pi -= 1) {
				long t1 = System.currentTimeMillis();
				task.setInput(workload, login, pass);
				task.performAnalysis(pi, 1, status);
				long t2 = System.currentTimeMillis();
				statRes.addAll(task.getOutput());
				// String ss = "T(s)/PI/TI\t" + ((t2 - t1) / 1000) + "\t" + pi;
				// times.put((t2 - t1), ss);
				// System.out.println("------------------------------------------------------------");
				// System.out.println("--- " + ss);
				// System.out.println("------------------------------------------------------------");
				// for (String s : times.values()) {
				// System.out.println(s);
				// }
			}

			final ArrayList<MappingData3DPath> newStatisticsData = new ArrayList<MappingData3DPath>();

			{
				for (NumericMeasurementInterface m : statRes) {
					if (m == null)
						System.out.println("ERROR NULL");
					else
						newStatisticsData.add(new MappingData3DPath(m));
				}
			}

			final Experiment statisticsResult = new Experiment(MappingData3DPath.merge(newStatisticsData));
			statisticsResult.getHeader().setExperimentname(statisticsResult.getName() + " " + getDefaultTitle());

			statisticsResult.getHeader().setExcelfileid("");
			if (resultReceiver == null) {
				if (status != null)
					status.setCurrentStatusText1("Store Result");

				new MongoDB().saveExperiment("dbe3", null, null, null, statisticsResult, status);

				if (status != null)
					status.setCurrentStatusText1("Ready");

				MyExperimentInfoPanel info = new MyExperimentInfoPanel();
				info.setExperimentInfo(login, pass, statisticsResult.getHeader(), false, statisticsResult);
				// mpc = new MainPanelComponent(TableLayout.getSplit(info, sfp,
				// TableLayout.PREFERRED, TableLayout.FILL));
				mpc = new MainPanelComponent(info, true);
			} else {
				mpc = new MainPanelComponent("Running in batch-mode. Partial result is not shown at this place.");
				resultReceiver.setExperimenData(statisticsResult);
				resultReceiver.run();
			}
			this.experimentResult = statisticsResult;
		} catch (Exception e) {
			ErrorMsg.addErrorMessage(e);
			mpc = null;
		}
		// src.title = src.title.split("\\:")[0];
	}

	@Override
	public ArrayList<NavigationButton> getResultNewNavigationSet(ArrayList<NavigationButton> currentSet) {
		ArrayList<NavigationButton> res = new ArrayList<NavigationButton>(currentSet);
		res.add(src);
		return res;
	}

	@Override
	public ArrayList<NavigationButton> getResultNewActionSet() {
		ArrayList<NavigationButton> res = new ArrayList<NavigationButton>();
		for (NavigationButton ne : ImageAnalysisCommandManager.getCommands(login, pass, new ExperimentReference(
							experimentResult), false, src.getGUIsetting()))
			res.add(ne);

		for (NavigationButton ne : Other.getProcessExperimentDataWithVantedEntities(null, null, new ExperimentReference(
							experimentResult), src.getGUIsetting())) {
			if (ne.getTitle().contains("Put data")) {
				ne.setTitle("View in VANTED");
				res.add(ne);
			}
		}
		return res;
	}

	@Override
	public MainPanelComponent getResultMainPanel() {
		return mpc;
	}

	@Override
	public String getDefaultImage() {
		return "img/ext/preferences-desktop.png";
	}

	@Override
	public String getDefaultTitle() {
		return "Performance Test";
	}

	// @Override
	// public void setWorkingSet(int workOnSubset, int numberOfSubsets,
	// RunnableWithMappingData resultReceiver) {
	// this.resultReceiver = resultReceiver;
	// this.workOnSubset = workOnSubset;
	// this.numberOfSubsets = numberOfSubsets;
	// }
	//
	// @Override
	// public void setParams(ExperimentReference experiment, String login, String
	// pass, String params) {
	// this.experiment = experiment;
	// this.login = login;
	// this.pass = pass;
	// }

}