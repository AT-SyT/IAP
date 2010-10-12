/*******************************************************************************
 * 
 *    Copyright (c) 2010 IPK Gatersleben, Group Image Analysis
 * 
 *******************************************************************************/
/*
 * Created on Aug 8, 2010 by Christian Klukas
 */

package de.ipk_gatersleben.ag_ba.graffiti.plugins.gui.navigation_actions;

import de.ipk_gatersleben.ag_ba.graffiti.plugins.gui.navigation_model.NavigationGraphicalEntity;
import de.ipk_gatersleben.ag_ba.graffiti.plugins.gui.util.ExperimentReference;
import de.ipk_gatersleben.ag_ba.mongo.MongoDB;

/**
 * @author klukas
 * 
 */
public class CloudUploadEntity extends AbstractExperimentAnalysisNavigation {

	private boolean active;

	public CloudUploadEntity(String login, String pass, ExperimentReference experiment) {
		super(login, pass, experiment);
	}

	@Override
	public String getDefaultTooltip() {
		return "Upload data set to IAP Cloud";
	}

	@Override
	public void performActionCalculateResults(NavigationGraphicalEntity src) throws Exception {
		super.performActionCalculateResults(src);
		try {
			active = true;
			new MongoDB().storeExperiment("dbe3", null, null, null, experiment.getData(), status);
		} finally {
			active = false;
		}
	}

	@Override
	public String getDefaultImage() {
		if (active) {
			if (System.currentTimeMillis() % 1000 < 500)
				return "img/ext/transfer2.png";
			else
				return "img/ext/transfer22.png";
		} else

			return "img/ext/transfer2.png";
	}

	@Override
	public String getDefaultNavigationImage() {
		return getDefaultImage();
	}

	@Override
	public String getDefaultTitle() {
		return "Copy into IAP Cloud";
	}

}
