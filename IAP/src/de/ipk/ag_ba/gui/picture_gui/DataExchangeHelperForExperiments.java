/*
 * Copyright (c) 2003 IPK Gatersleben
 * $Id: DataExchangeHelperForExperiments.java,v 1.6 2010-11-05 09:46:50 klukas
 * Exp $
 */
package de.ipk.ag_ba.gui.picture_gui;

import info.clearthought.layout.TableLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.ErrorMsg;
import org.MeasurementFilter;
import org.StringManipulationTools;
import org.graffiti.editor.MainFrame;
import org.graffiti.plugin.algorithm.ThreadSafeOptions;
import org.graffiti.plugin.io.resources.FileSystemHandler;
import org.graffiti.plugin.io.resources.IOurl;
import org.graffiti.plugin.io.resources.ResourceIOManager;

import com.mongodb.DB;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;

import de.ipk.ag_ba.commands.experiment.process.report.ActionPdfCreation3;
import de.ipk.ag_ba.commands.experiment.process.report.MySnapshotFilter;
import de.ipk.ag_ba.gui.images.IAPimages;
import de.ipk.ag_ba.gui.util.ExperimentReference;
import de.ipk.ag_ba.mongo.DataStorageType;
import de.ipk.ag_ba.mongo.DatabaseStorageResult;
import de.ipk.ag_ba.mongo.DatabaseStorageResultWithURL;
import de.ipk.ag_ba.mongo.MongoDB;
import de.ipk.ag_ba.mongo.MongoResourceIOConfigObject;
import de.ipk.ag_ba.mongo.RunnableOnDB;
import de.ipk_gatersleben.ag_nw.graffiti.FileHelper;
import de.ipk_gatersleben.ag_nw.graffiti.plugins.gui.editing_tools.script_helper.ConditionInterface;
import de.ipk_gatersleben.ag_nw.graffiti.plugins.gui.editing_tools.script_helper.Experiment;
import de.ipk_gatersleben.ag_nw.graffiti.plugins.gui.editing_tools.script_helper.ExperimentInterface;
import de.ipk_gatersleben.ag_nw.graffiti.plugins.gui.editing_tools.script_helper.MappingDataEntity;
import de.ipk_gatersleben.ag_nw.graffiti.plugins.gui.editing_tools.script_helper.NumericMeasurementInterface;
import de.ipk_gatersleben.ag_nw.graffiti.plugins.gui.editing_tools.script_helper.SampleInterface;
import de.ipk_gatersleben.ag_nw.graffiti.plugins.gui.editing_tools.script_helper.SubstanceInterface;
import de.ipk_gatersleben.ag_nw.graffiti.services.task.BackgroundTaskHelper;
import de.ipk_gatersleben.ag_pbi.mmd.experimentdata.Condition3D;
import de.ipk_gatersleben.ag_pbi.mmd.experimentdata.MeasurementNodeType;
import de.ipk_gatersleben.ag_pbi.mmd.experimentdata.Sample3D;
import de.ipk_gatersleben.ag_pbi.mmd.experimentdata.Substance3D;
import de.ipk_gatersleben.ag_pbi.mmd.experimentdata.images.ImageData;
import de.ipk_gatersleben.ag_pbi.mmd.experimentdata.volumes.VolumeData;

/**
 * @author Klukas
 */
public class DataExchangeHelperForExperiments {
	
	public static int getSizeOfExperiment(ExperimentReference m) {
		return -1;
	}
	
	public static void downloadFile(MongoDB m, final String hash,
			final File targetFile, final DataSetFileButton button,
			final MongoCollection collection) {
		try {
			m.processDB(new RunnableOnDB() {
				
				private DB db;
				
				@Override
				public void run() {
					try {
						// Blob b = CallDBE2WebService.getBlob(user, pass,
						// imageResult.getMd5());
						
						GridFS gridfs_images = new GridFS(db, collection
								.toString());
						System.out.println("Look for " + collection.toString()
								+ "-HASH: " + hash);
						GridFSDBFile fff = gridfs_images.findOne(hash);
						if (fff == null)
							System.out.println("NOT FOUND");
						if (fff != null) {
							System.out.println("FOUND, LENGTH="
									+ fff.getLength());
							InputStream bis = fff.getInputStream();
							
							FileOutputStream fos = new FileOutputStream(
									targetFile);
							int readBytes = 0;
							int pos = 0;
							long len = fff.getLength();
							byte[] readBuff = new byte[1024 * 1024];
							button.progress.setMaximum(100);
							while (0 < (readBytes = bis.read(readBuff))) {
								fos.write(readBuff, 0, readBytes);
								pos += readBytes;
								button.progress.setValue((int) ((double) pos
										/ len * 100d));
							}
							bis.close();
							fos.close();
							System.out.println("Created "
									+ targetFile.getAbsolutePath() + " ("
									+ targetFile.length() + " bytes, read "
									+ pos + ")");
						}
					} catch (Exception e1) {
						SupplementaryFilePanelMongoDB.showError("IOException",
								e1);
					}
				}
				
				@Override
				public void setDB(DB db) {
					this.db = db;
				}
			});
		} catch (Exception e) {
			SupplementaryFilePanelMongoDB.showError("IOException", e);
		}
	}
	
	public static DatabaseStorageResultWithURL insertHashedFile(final MongoDB m,
			final File file, File createTempPreviewImage, int isJavaImage,
			DataSetFileButton imageButton, MappingDataEntity tableName) {
		
		DatabaseStorageResult res = null;
		IOurl resultURL = null;
		try {
			MongoResourceIOConfigObject config = new MongoResourceIOConfigObject(null, DataStorageType.ANNOTATION_FILE);
			String targetFilename = file.getName();
			resultURL = m.getHandler().copyDataAndReplaceURLPrefix(new FileInputStream(file), targetFilename, config);
			
			if (createTempPreviewImage != null) {
				config = new MongoResourceIOConfigObject(null, DataStorageType.PREVIEW_ICON);
				m.getHandler().copyDataAndReplaceURLPrefix(new FileInputStream(createTempPreviewImage), targetFilename, config);
			}
			
			res = DatabaseStorageResult.STORED_IN_DB;
		} catch (Exception e1) {
			ErrorMsg.addErrorMessage(e1);
			res = DatabaseStorageResult.IO_ERROR_SEE_ERRORMSG;
		}
		
		return new DatabaseStorageResultWithURL(res, resultURL);
	}
	
	public static void fillFilePanel(final DataSetFilePanel filePanel,
			final MongoTreeNode mtdbe, final JTree expTree)
			throws InterruptedException {
		LocalComputeJob r = new LocalComputeJob(new Runnable() {
			@Override
			public void run() {
				addFilesToPanel(filePanel, mtdbe, expTree);
			}
		}, "add files to panel");
		BackgroundThreadDispatcher.addTask(r);
	}
	
	static synchronized void addFilesToPanel(final DataSetFilePanel filePanel,
			final MongoTreeNode mt, final JTree expTree) {
		if (!mt.mayContainData())
			return;
		final StopObject stop = new StopObject(false);
		
		boolean cleared = false;
		final ArrayList<LocalComputeJob> executeLater = new ArrayList<LocalComputeJob>();
		Substance3D sub = null;
		try {
			ArrayList<BinaryFileInfo> bbb = new ArrayList<BinaryFileInfo>();
			BinaryFileInfo primary = null;
			try {
				MappingDataEntity mde = mt.getTargetEntity();
				
				String files = mde.getFiles();
				if (files != null && !files.isEmpty()) {
					for (String url_string : files.split(";")) {
						IOurl url = new IOurl(url_string);
						BinaryFileInfo bfi = new BinaryFileInfo(url, null, false, mde);
						bfi.setIsAttachment(true);
						bbb.add(bfi);
					}
				}
				
				if (mde instanceof ImageData) {
					ImageData id = (ImageData) mde;
					primary = new BinaryFileInfo(id.getURL(), id.getLabelURL(),
							true, id);
				} else {
					if (mde instanceof VolumeData) {
						VolumeData id = (VolumeData) mde;
						primary = new BinaryFileInfo(id.getURL(), id.getLabelURL(),
								true, id);
					} else {
						if (mde instanceof Substance3D) {
							sub = (Substance3D) mde;
							primary = null;
							for (ConditionInterface c : sub)
								for (SampleInterface si : c) {
									if (si instanceof Sample3D) {
										Sample3D s3d = (Sample3D) si;
										for (NumericMeasurementInterface nmi : s3d
												.getMeasurements((MeasurementNodeType) null)) {
											if (nmi instanceof ImageData) {
												ImageData id = (ImageData) nmi;
												primary = new BinaryFileInfo(
														id.getURL(),
														id.getLabelURL(), true, id);
											} else
												if (nmi instanceof VolumeData) {
													VolumeData id = (VolumeData) nmi;
													primary = new BinaryFileInfo(
															id.getURL(),
															id.getLabelURL(), true, id);
												}
											if (primary != null)
												bbb.add(primary);
										}
									}
								}
							primary = null;
						} else
							if (mde instanceof Sample3D) {
								Sample3D s3dXX = (Sample3D) mde;
								String a = s3dXX.getTime() + "/" + s3dXX.getTimeUnit();
								primary = null;
								for (SampleInterface s3dI : s3dXX.getParentCondition()
										.getSortedSamples()) {
									String b = s3dI.getTime() + "/"
											+ s3dI.getTimeUnit();
									if (!a.equals(b))
										continue;
									Sample3D s3d = (Sample3D) s3dI;
									for (NumericMeasurementInterface nmi : s3d
											.getMeasurements((MeasurementNodeType) null)) {
										if (nmi instanceof ImageData) {
											ImageData id = (ImageData) nmi;
											primary = new BinaryFileInfo(id.getURL(),
													id.getLabelURL(), true, id);
										} else
											if (nmi instanceof VolumeData) {
												VolumeData id = (VolumeData) nmi;
												primary = new BinaryFileInfo(id.getURL(),
														id.getLabelURL(), true, id);
											}
										if (primary != null)
											bbb.add(primary);
									}
								}
								primary = null;
							} else {
								if (mde instanceof Condition3D) {
									Condition3D c3d = (Condition3D) mde;
									primary = null;
									for (SampleInterface si : c3d) {
										Sample3D s3d = (Sample3D) si;
										for (NumericMeasurementInterface nmi : s3d
												.getMeasurements((MeasurementNodeType) null)) {
											if (nmi instanceof ImageData) {
												ImageData id = (ImageData) nmi;
												IOurl urlMain = id.getURL();
												IOurl urlLabel = id.getLabelURL();
												primary = new BinaryFileInfo(urlMain, urlLabel, true, id, s3d.toString());
											} else
												if (nmi instanceof VolumeData) {
													VolumeData id = (VolumeData) nmi;
													IOurl urlMain = id.getURL();
													IOurl urlLabel = id.getLabelURL();
													primary = new BinaryFileInfo(urlMain, urlLabel, true, id, s3d.toString());
												}
											if (primary != null)
												bbb.add(primary);
										}
									}
									primary = null;
								}
							}
					}
				}
				
			} catch (Exception e) {
				ErrorMsg.addErrorMessage(e);
			}
			if (primary != null)
				bbb.add(primary);
			
			Map<String, Object> properties = new HashMap<String, Object>();
			mt.getTargetEntity().fillAttributeMap(properties);
			
			search: for (Entry<String, Object> e : properties.entrySet()) {
				if (e.getKey().startsWith("anno")) {
					Object v = e.getValue();
					if (v != null && v instanceof String) {
						String vss = (String) v;
						for (String vs : vss.split(";")) {
							String fileName = vs;
							if (vs.contains("#"))
								fileName = vs.split("#", 2)[0];
							if (!vs.contains("#")
									|| !fileName.equals("oldreference"))
								continue;
							if (vs.contains("#"))
								fileName = vs.split("#", 2)[1];
							if (primary != null)
								bbb.add(new BinaryFileInfo(primary.getFileNameLabel(), new IOurl(fileName), false, mt.getTargetEntity()));
							bbb.add(new BinaryFileInfo(new IOurl(fileName), null, false, mt.getTargetEntity()));
							break search;
						}
					}
				}
			}
			
			if (sub != null) {
				if (!cleared) {
					cleared = true;
					clearPanel(filePanel, mt, expTree);
				}
				processChartGenerator(executeLater, sub, sub, mt, expTree, filePanel, bbb.isEmpty(), stop);
			}
			
			BinaryFileInfo lastBBB = null;
			if (bbb.size() > 0)
				lastBBB = bbb.get(bbb.size() - 1);
			for (final BinaryFileInfo binaryFileInfo : bbb) {
				if (mt != expTree.getSelectionPath().getLastPathComponent())
					break;
				ImageResult imageResult = new ImageResult(null, binaryFileInfo);
				boolean previewLoadAndConstructNeeded = false;
				
				ImageIcon previewImage = null;
				if (DataSetFileButton.ICON_WIDTH == 128) {
					try {
						byte[] pi = ResourceIOManager
								.getPreviewImageContent(binaryFileInfo
										.getFileNameMain());
						if (pi != null) {
							previewImage = new ImageIcon(pi);
						} else
							previewLoadAndConstructNeeded = true;
					} catch (Exception e) {
						previewLoadAndConstructNeeded = true;
					}
				} else {
					previewImage = null;
					previewLoadAndConstructNeeded = true;
				}
				final DataSetFileButton imageButton = new DataSetFileButton(
						mt, imageResult, previewImage, mt.isReadOnly(), false, null);
				if (binaryFileInfo.isPrimary())
					imageButton.setIsPrimaryDatabaseEntity();
				if (binaryFileInfo.isAttachment())
					imageButton.setIsAttachment();
				imageButton.setAdditionalFileNameInfo(binaryFileInfo.getAdditionalFileNameInfo());
				imageButton.setDownloadNeeded(!FileSystemHandler
						.isFileUrl(binaryFileInfo.getFileNameMain()));
				imageButton.setVerticalTextPosition(SwingConstants.BOTTOM);
				imageButton.setHorizontalTextPosition(SwingConstants.CENTER);
				
				if (!cleared) {
					cleared = true;
					clearPanel(filePanel, mt, expTree);
				}
				
				final boolean previewLoadAndConstructNeededF = previewLoadAndConstructNeeded;
				
				final boolean fIsLast = binaryFileInfo == lastBBB;
				
				SwingUtilities.invokeLater(processIcon(filePanel, mt, expTree,
						stop, executeLater, binaryFileInfo, imageButton,
						previewLoadAndConstructNeededF, fIsLast));
			}
			
		} catch (Exception e) {
			ErrorMsg.addErrorMessage(e);
		}
	}
	
	private static void processChartGenerator(ArrayList<LocalComputeJob> executeLater, MappingDataEntity mde, final Substance3D sub,
			MongoTreeNode mt, JTree expTree, DataSetFilePanel filePanel, boolean isLast, StopObject stop) {
		if (mt != expTree.getSelectionPath().getLastPathComponent())
			return;
		boolean addDataChart = true;
		if (addDataChart) {
			ImageIcon previewImage = new ImageIcon(IAPimages.getImage(IAPimages.getHistogramIcon()));
			
			final DataSetFileButton chartingButton = new DataSetFileButton(
					mt, null, previewImage, mt.isReadOnly(), true, "Create Data Chart");
			chartingButton.setAdditionalActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					ExperimentInterface exp = Experiment.copyAndExtractSubtanceInclusiveData(sub);
					HashSet<String> speciesNames = new HashSet<String>();
					for (SubstanceInterface si : exp)
						for (ConditionInterface ci : si) {
							speciesNames.add(ci.getSpecies());
						}
					int idx = 1;
					for (SubstanceInterface si : exp)
						for (ConditionInterface ci : si)
							ci.setRowId(idx++);
					if (speciesNames.size() == 1)
						for (SubstanceInterface si : exp)
							for (ConditionInterface ci : si)
								ci.setSpecies(null);
					
					DataChartComponentWindow dccw = new DataChartComponentWindow(exp);
					dccw.setVisible(true);
				}
			});
			chartingButton.setVerticalTextPosition(SwingConstants.BOTTOM);
			chartingButton.setHorizontalTextPosition(SwingConstants.CENTER);
			
			SwingUtilities.invokeLater(processIcon(filePanel, mt, expTree,
					stop, executeLater, null, chartingButton, false, false));
		}
		{
			ImageIcon previewImage = new ImageIcon(IAPimages.getImage(IAPimages.getHistogramIcon()));
			
			final DataSetFileButton chartingButton = new DataSetFileButton(
					mt, null, previewImage, mt.isReadOnly(), true, "Export Data (XLSX)");
			chartingButton.setAdditionalActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					String defaultFileName = StringManipulationTools.getFileSystemName(
							sub.iterator().next().getExperimentHeader().getExperimentName()
									+ "_" + sub.getName() + ".xlsx");
					String fn = FileHelper.getFileName(".xlsx", "Create File", defaultFileName);
					if (fn != null) {
						boolean xlsx = true;
						ActionPdfCreation3 action = new ActionPdfCreation3(
								null, null, false, xlsx, null, null,
								null, null, null);
						action.setExperimentReference(
								new ExperimentReference(
										Experiment.copyAndExtractSubtanceInclusiveData(sub)));
						action.setUseIndividualReportNames(true);
						action.setStatusProvider(null);
						action.setSource(null, null);
						action.setCustomTargetFileName(fn);
						try {
							action.performActionCalculateResults(null);
						} catch (Exception e) {
							e.printStackTrace();
							MainFrame.getInstance().showMessageDialog("Could not perform operation: " + e.getMessage());
						}
					}
				}
				
			});
			chartingButton.setVerticalTextPosition(SwingConstants.BOTTOM);
			chartingButton.setHorizontalTextPosition(SwingConstants.CENTER);
			
			SwingUtilities.invokeLater(processIcon(filePanel, mt, expTree,
					stop, executeLater, null, chartingButton, false, isLast));
		}
	}
	
	private static Runnable processIcon(final DataSetFilePanel filePanel,
			final MongoTreeNode mt, final JTree expTree, final StopObject stop,
			final ArrayList<LocalComputeJob> executeLater,
			final BinaryFileInfo binaryFileInfo,
			final DataSetFileButton imageButton,
			final boolean previewLoadAndConstructNeededF, final boolean fIsLast) {
		final int tw = DataSetFileButton.ICON_WIDTH;
		return new Runnable() {
			@Override
			public void run() {
				// nur falls der Zielknoten immer noch ausgewählt ist,
				// soll der Button hinzugefügt werden
				if (mt == expTree.getSelectionPath().getLastPathComponent()
						&& DataSetFileButton.ICON_WIDTH == tw) {
					MeasurementFilter mf = new MySnapshotFilter(new ArrayList<ThreadSafeOptions>(), mt.getExperiment().getHeader().getGlobalOutlierInfo());
					final AnnotationInfoPanel aip = new AnnotationInfoPanel(
							imageButton, mt, mf);
					JComponent buttonAndInfo = binaryFileInfo == null || !binaryFileInfo.isPrimary() ? imageButton
							: TableLayout.getSplitVertical(imageButton, aip,
									TableLayout.PREFERRED,
									TableLayout.PREFERRED);
					imageButton.addMouseListener(getML(aip));
					buttonAndInfo.addMouseListener(getML(aip));
					filePanel.add(buttonAndInfo);
					filePanel.validate();
					filePanel.repaint();
					filePanel.getScrollpane().validate();
					if (previewLoadAndConstructNeededF) {
						LocalComputeJob t;
						try {
							t = new LocalComputeJob(new Runnable() {
								@Override
								public void run() {
									if (mt == expTree.getSelectionPath()
											.getLastPathComponent()
											&& DataSetFileButton.ICON_WIDTH == tw) {
										final MyImageIcon myImage;
										try {
											myImage = new MyImageIcon(
													MainFrame.getInstance(),
													DataSetFileButton.ICON_WIDTH,
													DataSetFileButton.ICON_HEIGHT,
													binaryFileInfo
															.getFileNameMain(),
													binaryFileInfo
															.getFileNameLabel(),
													binaryFileInfo);
											myImage.imageAvailable = 1;
											try {
												BackgroundTaskHelper
														.executeLaterOnSwingTask(
																0,
																new Runnable() {
																	@Override
																	public void run() {
																		imageButton
																				.updateLayout(
																						null,
																						myImage,
																						myImage, false);
																	}
																});
											} catch (Exception e) {
												ErrorMsg.addErrorMessage(e);
											}
										} catch (MalformedURLException e) {
											// empty
										}
									}
								}
							}, "preview load and construct");
							executeLater.add(t);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						
					}
					BackgroundTaskHelper.executeLaterOnSwingTask(10,
							new Runnable() {
								@Override
								public void run() {
									boolean isLast = fIsLast;
									if (isLast)
										for (LocalComputeJob ttt : executeLater)
											BackgroundThreadDispatcher.addTask(
													ttt);
								}
							});
				} else
					stop.setStopWanted(true);
			}
			
			private MouseListener getML(final AnnotationInfoPanel aip) {
				return new MouseListener() {
					@Override
					public void mouseReleased(MouseEvent e) {
					}
					
					@Override
					public void mousePressed(MouseEvent e) {
					}
					
					@Override
					public void mouseExited(MouseEvent e) {
						aip.removeGuiLater();
					}
					
					@Override
					public void mouseEntered(MouseEvent e) {
						aip.addGui();
					}
					
					@Override
					public void mouseClicked(MouseEvent e) {
					}
				};
			}
		};
	}
	
	private static void clearPanel(final DataSetFilePanel filePanel,
			final MongoTreeNode mt, final JTree expTree) {
		try {
			Runnable r = new Runnable() {
				@Override
				public void run() {
					if (mt == expTree.getSelectionPath().getLastPathComponent()) {
						filePanel.removeAll();
						filePanel.validate();
						filePanel.repaint();
						filePanel.getScrollpane().validate();
					}
				}
			};
			if (SwingUtilities.isEventDispatchThread())
				r.run();
			else
				SwingUtilities.invokeAndWait(r);
		} catch (InterruptedException e2) {
			SupplementaryFilePanelMongoDB.showError("InterruptedException", e2);
		} catch (InvocationTargetException e2) {
			SupplementaryFilePanelMongoDB.showError(
					"InvocationTargetException", e2);
		}
	}
	
	public static void attachFileToEntity(MappingDataEntity targetEntity, DatabaseStorageResultWithURL res, String name) {
		if (name.contains(":"))
			name = StringManipulationTools.stringReplace(name, ";", "_");
		String currentValue = targetEntity.getFiles();
		if (currentValue == null || currentValue.isEmpty())
			targetEntity.setFiles(res.getResultURL() + "");
		else {
			LinkedHashSet<String> values = new LinkedHashSet<String>();
			for (String s : currentValue.split(";"))
				values.add(s);
			values.add(res.getResultURL() + "");
			targetEntity.setFiles(StringManipulationTools.getStringList(values, ";"));
		}
	}
}