package de.ipk.ag_ba.mongo;

import java.util.ArrayList;

import org.BackgroundTaskStatusProviderSupportingExternalCall;
import org.SystemAnalysis;
import org.graffiti.plugin.algorithm.ThreadSafeOptions;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;

import de.ipk.ag_ba.commands.vfs.VirtualFileSystemVFS2;
import de.ipk_gatersleben.ag_nw.graffiti.services.BackgroundTaskConsoleLogger;

public class DataBaseFileStorage {
	
	private final MongoDB mongoDB;
	
	public DataBaseFileStorage(MongoDB mongoDB) {
		this.mongoDB = mongoDB;
	}
	
	public void move(final VirtualFileSystemVFS2 vfs, final int gb, final BackgroundTaskStatusProviderSupportingExternalCall status,
			final StringBuilder messages) throws Exception {
		mongoDB.processDB(new RunnableOnDB() {
			private DB db;
			
			@Override
			public void run() {
				ThreadSafeOptions free = new ThreadSafeOptions();
				BackgroundTaskStatusProviderSupportingExternalCall delStatus = new BackgroundTaskConsoleLogger("", "", false);
				long n = 0;
				long start = System.currentTimeMillis();
				for (String mgfs : MongoGridFS.getFileCollectionsInclPreview()) {
					final GridFS gridfs = new GridFS(db, mgfs);
					int nFiles = gridfs.getFileList(new BasicDBObject()).count();
					System.out.println(SystemAnalysis.getCurrentTime() + ">Start processing: GridFS " + mgfs + " contains " + nFiles + " files");
					try {
						ArrayList<String> md5s = new ArrayList<String>();
						status.setCurrentStatusText1("Get file names in");
						status.setCurrentStatusText2(mgfs + " (" + nFiles + ")");
						status.setCurrentStatusValueFine(0);
						for (DBObject dbo : gridfs.getFileList(new BasicDBObject())) {
							String fn = (String) dbo.get("md5");
							md5s.add(fn);
							status.setCurrentStatusValueFine(100d * md5s.size() / nFiles);
						}
						
						for (String fn : md5s) {
							GridFSDBFile f = gridfs.findOne(fn);
							if (f == null)
								continue;
							String md5 = f.getFilename();
							try {
								long saved = vfs.saveStream(mgfs + "/" + md5, f.getInputStream(), true);
								if (saved != f.getLength()) {
									messages.append("Could not move file " + md5 + " from "
											+ gridfs + " to " + vfs.getTargetName() + ", storage size differs from input size: "
											+ saved + " saved, " + f.getLength() + " input size.");
									MongoDB.saveSystemMessage("Could not move file " + md5
											+ " from " + gridfs + " to " + vfs.getTargetName()
											+ ", storage size differs from input size: "
											+ saved + " saved, " + f.getLength() + " input size.");
									
								} else {
									ArrayList<GridFSDBFile> toBeRemoved = new ArrayList<GridFSDBFile>();
									toBeRemoved.add(f);
									mongoDB.removeFilesFromGridFS(delStatus, mgfs, gridfs, toBeRemoved, free, db);
									n++;
									status.setCurrentStatusText1("Moving data from " + mgfs + " (" + n + ")");
									status.setCurrentStatusText2(free.getLong() / 1024 / 1024 / 1024 + " GB moved ("
											+ SystemAnalysis.getDataTransferSpeedString(free.getLong(), start, System.currentTimeMillis()) + ")");
								}
								if (gb > 0 && free.getLong() / 1024 / 1024 / 1024 >= gb)
									break;
								if (gb <= 0)
									status.setCurrentStatusValueFine(n * 100d / nFiles);
								else
									status.setCurrentStatusValueFine(free.getLong() * 100d / 1024d / 1024d / 1024d / gb);
							} catch (Exception e) {
								messages.append("Could not move file " + md5 + " from " + gridfs + " to " + vfs.getTargetName());
								MongoDB.saveSystemErrorMessage("Could not move file " + md5 + " from " + gridfs + " to " + vfs.getTargetName(), e);
							}
						}
					} catch (Exception e) {
						messages.append("Could not move file no " + n + " from "
								+ gridfs + " to " + vfs.getTargetName() + ". Error: " + e.getMessage());
						MongoDB.saveSystemErrorMessage("Could not move file  " + n
								+ " from " + gridfs + " to " + vfs.getTargetName(), e);
					}
					System.out.println(SystemAnalysis.getCurrentTime() + ">Processing finished: GridFS " + mgfs + " contains " + nFiles + " files");
				}
				
				messages.append("Moved " + n + " files: " + free.getLong() / 1024 / 1024 / 1024 + " GB");
			}
			
			@Override
			public void setDB(DB db) {
				this.db = db;
			}
		});
	}
}
