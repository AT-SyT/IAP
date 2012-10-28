package de.ipk.ag_ba.server.task_management;

import info.StopWatch;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;

import org.BackgroundTaskStatusProviderSupportingExternalCall;
import org.ReleaseInfo;
import org.SettingsHelperDefaultIsFalse;
import org.SystemAnalysis;
import org.SystemOptions;
import org.graffiti.plugin.algorithm.ThreadSafeOptions;
import org.graffiti.plugin.io.resources.ResourceIOHandler;
import org.graffiti.plugin.io.resources.ResourceIOManager;

import de.ipk.ag_ba.commands.ActionAnalyzeAllExperiments;
import de.ipk.ag_ba.commands.ActionDeleteHistoryOfAllExperiments;
import de.ipk.ag_ba.commands.mongodb.ActionCopyToMongo;
import de.ipk.ag_ba.gui.util.ExperimentReference;
import de.ipk.ag_ba.gui.webstart.IAPmain;
import de.ipk.ag_ba.mongo.MongoDB;
import de.ipk.ag_ba.postgresql.LemnaTecDataExchange;
import de.ipk.ag_ba.postgresql.LemnaTecFTPhandler;
import de.ipk_gatersleben.ag_nw.graffiti.plugins.gui.editing_tools.script_helper.ExperimentHeaderInterface;
import de.ipk_gatersleben.ag_nw.graffiti.plugins.gui.webstart.TextFile;
import de.ipk_gatersleben.ag_nw.graffiti.services.task.BackgroundTaskStatusProviderSupportingExternalCallImpl;
import de.ipk_gatersleben.ag_pbi.mmd.MultimodalDataHandlingAddon;

public class MassCopySupport {
	
	private static MassCopySupport instance = null;
	
	private final ArrayList<String> history = new ArrayList<String>();
	
	public static synchronized MassCopySupport getInstance() {
		if (instance == null)
			instance = new MassCopySupport();
		
		instance.scheduleMassCopy();
		
		return instance;
	}
	
	private boolean massCopyRunning = false;
	
	private final BackgroundTaskStatusProviderSupportingExternalCall status = new BackgroundTaskStatusProviderSupportingExternalCallImpl("", "");
	
	private MassCopySupport() {
		new MultimodalDataHandlingAddon();
		
		ResourceIOManager.registerIOHandler(new LemnaTecFTPhandler());
		for (MongoDB m : MongoDB.getMongos()) {
			for (ResourceIOHandler handler : m.getHandlers())
				ResourceIOManager.registerIOHandler(handler);
		}
		final String fn = ReleaseInfo.getAppFolderWithFinalSep() + "iap_mass_copy_history.txt";
		try {
			TextFile tf = new TextFile(fn);
			history.addAll(tf);
			print("INFO: MASS COPY HISTORY LOADED (" + fn + ")");
		} catch (IOException e) {
			print("INFO: NO MASS COPY HISTORY TO LOAD (" + fn + ": " + e.getMessage() + ")");
		}
		Timer t = new Timer("IAP MASS-COPY-History Saver");
		final ThreadSafeOptions tso = new ThreadSafeOptions();
		TimerTask tT = new TimerTask() {
			@Override
			public void run() {
				if (tso.getInt() == history.size())
					return;
				try {
					TextFile tf = new TextFile();
					tf.addAll(history);
					tf.write(fn);
					tso.setInt(history.size());
				} catch (IOException e) {
					print("ERROR: MASS COPY HISTORY COULD NOT BE SAVED (" + e.getMessage() + " - " + fn + ")");
				}
			}
		};
		tT.run();
		t.scheduleAtFixedRate(tT, new Date(), 1 * 60 * 1000);
		print("INFO: MASS COPY SUPPORT READY");
	}
	
	public void performMassCopy(boolean onlyMerge) throws InterruptedException {
		if (massCopyRunning) {
			if (!onlyMerge)
				print("INFO: MASS COPY PROCEDURE IS SKIPPED, BECAUSE PREVIOUS MASS COPY OPERATION IS STILL RUNNING");
			return;
		}
		massCopyRunning = true;
		
		boolean en = new SettingsHelperDefaultIsFalse().isEnabled("GRID-STORAGE|auto_daily_fetch");
		if (!en)
			return;
		for (int i = 30; i >= 0; i--) {
			status.setCurrentStatusText1("Countdown");
			if (onlyMerge)
				status.setCurrentStatusText2("Start result merge in " + i + " seconds...");
			else
				status.setCurrentStatusText2("Start sync in " + i + " seconds...");
			Thread.sleep(1000);
			en = new SettingsHelperDefaultIsFalse().isEnabled("GRID-STORAGE|auto_daily_fetch");
			if (!en) {
				massCopyRunning = false;
				status.setCurrentStatusText1("Sync cancelled");
				status.setCurrentStatusText2("");
				Thread.sleep(5000);
				if (!massCopyRunning) {
					status.setCurrentStatusText1("");
					status.setCurrentStatusText2("");
				}
				return;
			}
		}
		status.setCurrentStatusText1("Sync initiated");
		status.setCurrentStatusText2("Analyze data status...");
		Thread.sleep(1000);
		
		try {
			makeCopyInnerCall(onlyMerge);
		} finally {
			massCopyRunning = false;
		}
	}
	
	private void print(String msg) {
		MongoDB.saveSystemMessage(SystemAnalysisExt.getHostNameNiceNoError() + ": " + msg);
		msg = SystemAnalysis.getCurrentTime() + ">" + msg;
		history.add(msg);
		System.out.println(msg);
		status.setCurrentStatusText2(msg);
	}
	
	@SuppressWarnings("deprecation")
	private void makeCopyInnerCall(boolean onlyMerge) {
		// if false, analysis is performed after copying all new experiments
		// if true, only the newly copied experiments are analyzed
		boolean analyzeEachCopiedExperiment = false;
		try {
			if (!onlyMerge)
				copyMissingOrNewData(analyzeEachCopiedExperiment);
			boolean analyzeAllExperiments = !analyzeEachCopiedExperiment;
			if (analyzeAllExperiments) {
				for (MongoDB m : MongoDB.getMongos()) {
					status.setCurrentStatusText2("Merge task results");
					CloudComputingService.merge(m, false, getStatusProvider());
					status.setCurrentStatusText2("Merged task results (" + SystemAnalysis.getCurrentTime() + ")");
					
					if (!onlyMerge) {
						status.setCurrentStatusText2("Delete Experiment History");
						ActionDeleteHistoryOfAllExperiments delete = new ActionDeleteHistoryOfAllExperiments(m);
						delete.setStatusProvider(getStatusProvider());
						delete.performActionCalculateResults(null);
						status.setCurrentStatusText2("Deleted Experiment History (" + SystemAnalysis.getCurrentTime() + ")");
						MongoDB.saveSystemMessage("Deleted experiment history (" + m.getDatabaseName() + ")");
					}
					
					if (m.batchGetAllCommands().size() == 0 && !onlyMerge) {
						// on Saturday, if no analysis is running and no analysis has been scheduled,
						// the database clean-up is called
						if (new Date().getDay() == 6) {
							MongoDB.saveSystemMessage("Database reorganization started (" + m.getDatabaseName() + ")");
							long startTime = System.currentTimeMillis();
							m.cleanUp(getStatusProvider(), false);
							long processingTime = System.currentTimeMillis() - startTime;
							status.setCurrentStatusText2("Database reorganization finished (took " +
									SystemAnalysis.getWaitTime(processingTime) + ")");
							MongoDB.saveSystemMessage("Database reorganization finished (" + m.getDatabaseName() + "), took "
									+ SystemAnalysis.getWaitTime(processingTime));
						}
						if (SystemOptions.getInstance().getBoolean("GRID-COMPUTING", "remote_execution", true)) {
							ActionAnalyzeAllExperiments all = new ActionAnalyzeAllExperiments(m, m.getExperimentList(null));
							status.setCurrentStatusText2("Schedule analysis tasks for new data");
							all.setStatusProvider(getStatusProvider());
							all.performActionCalculateResults(null);
							int nn = m.batchGetAllCommands().size();
							if (nn == 0)
								status.setCurrentStatusText2("All analyis results are up-to-date");
							else
								status.setCurrentStatusText2("Scheduled " + nn +
										" analysis tasks (" + SystemAnalysis.getCurrentTime() + ")");
						}
					}
				}
			}
			if (!onlyMerge) {
				status.setCurrentStatusText2("<html>Service tasks completed<br>(" + new Date() + ")</html>");
				status.setCurrentStatusText2("Next sync at 1 AM");
			} else {
				status.setCurrentStatusText2("<html>Result-check completed<br>(" + new Date() + ")</html>");
				status.setCurrentStatusText2("Next sync at 1 AM");
			}
		} catch (Exception e1) {
			print("ERROR: MASS COPY INNER-CALL ERROR (" + e1.getMessage() + ")");
			MongoDB.saveSystemErrorMessage("MASS COPY INNER-CALL ERROR", e1);
		}
	}
	
	private void copyMissingOrNewData(boolean analyzeEachCopiedExperiment) throws SQLException, ClassNotFoundException, Exception, InterruptedException {
		System.out.println(SystemAnalysis.getCurrentTime() + ">START MASS COPY SYNC");
		status.setCurrentStatusText1("Start sync...");
		StopWatch s = new StopWatch(SystemAnalysis.getCurrentTime() + ">INFO: LemnaTec to MongoDBs (MASS COPY)", false);
		
		LemnaTecDataExchange lt = new LemnaTecDataExchange();
		ArrayList<IdTime> ltIdArr = new ArrayList<IdTime>();
		ArrayList<IdTime> mongoIdsArr = new ArrayList<IdTime>();
		final ArrayList<IdTime> toSave = new ArrayList<IdTime>();
		
		for (String db : lt.getDatabases()) {
			if (db.endsWith("11"))
				continue;
			try {
				for (ExperimentHeaderInterface ltExp : lt.getExperimentsInDatabase(null, db)) {
					ltIdArr.add(new IdTime(null, ltExp.getDatabaseId(),
							ltExp.getImportdate(), ltExp, ltExp.getNumberOfFiles()));
				}
			} catch (Exception e) {
				if (!e.getMessage().contains("relation \"snapshot\" does not exist"))
					print("Cant process DB " + db + ": " + e.getMessage());
			}
		}
		
		boolean useOnlyMainDatabase = true;
		ArrayList<MongoDB> checkM;
		if (useOnlyMainDatabase) {
			checkM = new ArrayList<MongoDB>();
			checkM.add(MongoDB.getDefaultCloud());
		} else
			checkM = MongoDB.getMongos();
		for (MongoDB m : checkM) {
			try {
				print("MongoDB: " + m.getDatabaseName() + "@" + m.getDefaultHost());
				for (ExperimentHeaderInterface hsmExp : m.getExperimentList(null)) {
					if (hsmExp.getOriginDbId() != null)
						mongoIdsArr.add(new IdTime(m, hsmExp.getOriginDbId(),
								hsmExp.getImportdate(), null, hsmExp.getNumberOfFiles()));
					else
						System.out.println(SystemAnalysis.getCurrentTime() + ">ERROR: NULL EXPERIMENT IN MongoDB (" + m.getDatabaseName() + ")!");
				}
			} catch (Exception e) {
				print("Cant process mongo DB " + m.getDatabaseName() + ": " + e.getMessage());
			}
		}
		
		for (IdTime it : ltIdArr) {
			String db = it.getExperimentHeader().getDatabase();
			if (db == null || (!db.startsWith("CGH_") && !db.startsWith("APH_") && !db.startsWith("BGH_"))) {
				// print("DATASET IGNORED (INVALID DB): " + it.Id + " (DB: " + it.getExperimentHeader().getDatabase() + ")");
				continue;
			} else
				if (it.getExperimentHeader().getExperimentName().equals("unknown")) {
					// print("DATASET IGNORED (INVALID UKNOWN EXPERIMENT NAME): " + it.Id + " (DB: " + it.getExperimentHeader().getDatabase() + ")");
					continue;
				}
			
			boolean found = false;
			for (IdTime h : mongoIdsArr) {
				if (h.equals(it)) {
					if (it.time.getTime() - h.time.getTime() > 1000) {
						print("MASS COPY INTENDED (MORE CURRENT DATA): " + it.Id + " (DB: " + it.getExperimentHeader().getDatabase() + ")");
						toSave.add(it);
					} else {
						// it seems sometimes some images can not be copied
						// then a repeated try on these data sets makes no real sense
						boolean copyIfMoreImagesAreAvailable = false;
						if (it.getNumberOfFiles() != h.getNumberOfFiles() && copyIfMoreImagesAreAvailable) {
							print("MASS COPY INTENDED (MORE IMAGES INSIDE EXPERIMENT): " + it.Id + " (DB: " + it.getExperimentHeader().getDatabase() +
									", LT:" + it.getNumberOfFiles() + " != M:" + h.getNumberOfFiles() + ")");
							toSave.add(it);
						}
					}
					found = true;
					break;
				}
			}
			
			if (!found) {
				toSave.add(it);
				print("MASS COPY INTENDED (NEW EXPERIMENT): " + it.Id + " (DB: "
						+ it.getExperimentHeader().getDatabase() + ")");
			}
		}
		
		print("START MASS COPY OF " + toSave.size() + " EXPERIMENTS!");
		status.setCurrentStatusText1("Start copy of " + toSave.size() + " experiments...");
		int done = 0;
		for (final IdTime it : toSave) {
			boolean en = new SettingsHelperDefaultIsFalse().isEnabled("GRID-STORAGE|auto_daily_fetch");
			if (!en)
				continue;
			status.setCurrentStatusText1("Copy " + toSave.size() + " experiments (" + done + " finished)");
			MongoDB m = it.getMongoDB();
			if (m == null) {
				// new data set, copy to last mongo instance
				if (useOnlyMainDatabase)
					m = MongoDB.getDefaultCloud();
				else
					m = MongoDB.getMongos().get(MongoDB.getMongos().size() - 1);
			}
			ExperimentHeaderInterface src = it.getExperimentHeader();
			print("Copy " + it.Id + " to " + m.getDatabaseName());
			ExperimentReference er = new ExperimentReference(src);
			ActionCopyToMongo copyAction = new ActionCopyToMongo(m, er, true);
			status.setPrefix1("<html>Copying " + (done + 1) + "/" + toSave.size() + " (" + it.Id + ")<br>");
			copyAction.setStatusProvider(status);
			boolean simulate = false;
			if (!simulate)
				copyAction.performActionCalculateResults(null);
			print("Copied " + it.Id + " to " + m.getDatabaseName());
			MongoDB.saveSystemMessage(SystemAnalysisExt.getHostNameNiceNoError() + ": Copied " + it.Id + " to " + m.getDatabaseName());
			done++;
			status.setCurrentStatusValueFine(100d * done / toSave.size());
			
			if (analyzeEachCopiedExperiment) {
				String id = er.getHeader().getDatabaseId();
				ArrayList<ExperimentHeaderInterface> experimentList = m.getExperimentList(null);
				for (ExperimentHeaderInterface ehi : experimentList) {
					if (ehi.getDatabaseId().equals(id)) {
						print("Add compute tasks to analyze experiment " + id + ".");
						ActionAnalyzeAllExperiments.processExperimentHeader(m, new StringBuilder(), ehi,
								ActionAnalyzeAllExperiments.knownAnalysis(ehi, experimentList));
					}
				}
			}
			Thread.sleep(1000);
		}
		status.setPrefix1(null);
		status.setCurrentStatusText1("Copy complete (" + done + " finished) (" + SystemAnalysis.getCurrentTime() + ")");
		status.setCurrentStatusText2("Next sync at 1 AM");
		status.setCurrentStatusValueFine(100d);
		s.printTime();
	}
	
	private boolean scheduled = false;
	
	public void scheduleMassCopy() {
		if (scheduled)
			return;
		String hsmFolder = IAPmain.getHSMfolder();
		if (hsmFolder != null && new File(hsmFolder).exists()) {
			boolean en = new SettingsHelperDefaultIsFalse().isEnabled("GRID-STORAGE|auto_daily_fetch");
			if (en)
				print("AUTOMATIC MASS COPY FROM LT TO MongoDB (" + hsmFolder + ") HAS BEEN SCHEDULED EVERY DAY AT 01:00");
			else
				print("Copy function is disabled");
			
			final ThreadSafeOptions lastExecutionTime = new ThreadSafeOptions();
			
			Timer t = new Timer("IAP 24h-Backup-Timer");
			long period = 1000 * 60 * 15; // every 15 minutes
			TimerTask tT = new TimerTask() {
				@SuppressWarnings("deprecation")
				@Override
				public void run() {
					try {
						if (System.currentTimeMillis() - lastExecutionTime.getLong() < 60000)
							return; // process timer call at most once per minute
						lastExecutionTime.setLong(System.currentTimeMillis());
						Thread.sleep(1000);
						boolean onlyMerge = false;
						if (new Date().getHours() != 1 || new Date().getMinutes() != 0)
							onlyMerge = true;
						boolean en = new SettingsHelperDefaultIsFalse().isEnabled("GRID-STORAGE|auto_daily_fetch");
						if (!en) {
							if (!onlyMerge)
								print("SCHEDULED MASS COPY IS NOT PERFORMED, AS IT IS CURRENTLY DISABLED. WILL BE RE-CHECKED TOMORROW AT 01:00");
							return;
						}
						
						Thread.sleep(1000);
						
						performMassCopy(onlyMerge);
					} catch (InterruptedException e) {
						print("INFO: PROCESSING INTERRUPTED (" + e.getMessage() + ")");
					}
				}
			};
			
			Date now = new Date();
			Calendar cal = Calendar.getInstance();
			cal.setTime(now);
			cal.add(Calendar.MINUTE, 15 - (cal.get(Calendar.MINUTE) % 15));
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			Date next_15_30_45_60 = cal.getTime();
			
			Date startTime = next_15_30_45_60;
			t.scheduleAtFixedRate(tT, startTime, period);
			scheduled = true;
		} else {
			print("WARNING: NO AUTOMATIC MASS COPY SCHEDULED! HSM FOLDER NOT AVAILABLE (" + hsmFolder + ")");
		}
	}
	
	public String getHistory(int maxLines, String pre, final String preLine, String lineBreak, String follow) {
		StringBuilder res = new StringBuilder();
		final Stack<String> news = new Stack<String>();
		ArrayList<String> h = new ArrayList<String>(history);
		if (maxLines < Integer.MAX_VALUE)
			Collections.reverse(h);
		for (int i = 0; i < history.size() && i < maxLines; i++) {
			String item = preLine + h.get(i);
			// if (maxLines < Integer.MAX_VALUE)
			// news.push(item);
			// else
			res.append(item);
		}
		while (!news.empty()) {
			String item = news.pop();
			res.append(item);
		}
		if (res != null && res.length() > 0)
			return pre + res.toString() + follow;
		else
			return res.toString();
	}
	
	public BackgroundTaskStatusProviderSupportingExternalCall getStatusProvider() {
		return status;
	}
}
