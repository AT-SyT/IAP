package de.ipk.ag_ba.commands.settings;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.IniIoProvider;
import org.SystemOptions;
import org.apache.commons.lang3.text.WordUtils;

import de.ipk.ag_ba.commands.AbstractNavigationAction;
import de.ipk.ag_ba.gui.interfaces.NavigationAction;
import de.ipk.ag_ba.gui.navigation_model.NavigationButton;

/**
 * @author klukas
 */
public class ActionSettingsEditor extends AbstractNavigationAction {
	
	String section;
	
	ArrayList<NavigationButton> res = new ArrayList<NavigationButton>();
	
	String iniFileName;
	
	IniIoProvider iniIO;
	
	public ActionSettingsEditor(String tooltip) {
		super(tooltip);
	}
	
	public ActionSettingsEditor(String iniFileName, IniIoProvider iniIO,
			String tooltip, String section) {
		this(tooltip);
		this.iniFileName = iniFileName;
		this.iniIO = iniIO;
		this.section = section;
	}
	
	@Override
	public void performActionCalculateResults(final NavigationButton src) throws Exception {
		res.clear();
		final LinkedHashMap<String, ArrayList<NavigationButton>> group2button = new LinkedHashMap<String, ArrayList<NavigationButton>>();
		for (final String setting : SystemOptions.getInstance(iniFileName, iniIO).getSectionSettings(section)) {
			String group = "";
			if (setting != null && setting.contains("//")) {
				group = setting.split("//")[0];
			}
			if (!group2button.containsKey(group))
				group2button.put(group, new ArrayList<NavigationButton>());
			group2button.get(group).add(new NavigationButton(
					new ActionSettingsFieldEditor(this, "Change setting " + section + "/" + setting, setting), src.getGUIsetting()));
		}
		for (final String group : group2button.keySet()) {
			if (group.length() == 0) {
				NavigationAction resetSettingsAction = new ActionResetActions(
						SystemOptions.getInstance(iniFileName, iniIO), section, group);
				NavigationButton restSettingsButton = new NavigationButton(
						resetSettingsAction, src.getGUIsetting());
				restSettingsButton.setRightAligned(true);
				res.add(0, restSettingsButton);
				
				for (NavigationButton r : group2button.get(group))
					res.add(r);
			} else {
				res.add(new NavigationButton(new AbstractNavigationAction("Change settings of block " + group) {
					
					@Override
					public void performActionCalculateResults(NavigationButton src) throws Exception {
						// empty
					}
					
					@Override
					public String getDefaultTitle() {
						String g = group;
						if (g != null && g.indexOf(".Block") > 0)
							g = g.substring(g.lastIndexOf(".Block") + ".Block".length());
						else
							if (g != null && g.indexOf(".Bl") > 0)
								g = g.substring(g.lastIndexOf(".Bl") + ".Bl".length());
							else
								if (g != null && g.indexOf(".") > 0)
									g = g.substring(g.lastIndexOf(".") + ".".length());
						if (g != null && g.startsWith("_"))
							g = g.substring("_".length());
						return g;
					}
					
					@Override
					public String getDefaultImage() {
						return "img/ext/gpl2/Glade-3-64.png";
					}
					
					@Override
					public ArrayList<NavigationButton> getResultNewActionSet() {
						ArrayList<NavigationButton> res = new ArrayList<NavigationButton>(group2button.get(group));
						NavigationAction resetSettingsAction = new ActionResetActions(
								SystemOptions.getInstance(iniFileName, iniIO), section, group);
						NavigationButton restSettingsButton = new NavigationButton(
								resetSettingsAction, src.getGUIsetting());
						restSettingsButton.setRightAligned(true);
						res.add(0, restSettingsButton);
						return res;
					}
				}, guiSetting));
			}
		}
	}
	
	@Override
	public ArrayList<NavigationButton> getResultNewActionSet() {
		return res;
	}
	
	@Override
	public String getDefaultTitle() {
		if (section.toLowerCase().endsWith(".pipeline"))
			return "Pipeline-Blocks";
		else
			if (section.toLowerCase().endsWith(".pipeline-side"))
				return "Side-Settings";
			else
				if (section.toLowerCase().endsWith(".pipeline-top"))
					return "Top-Settings";
				else {
					SystemOptions inst = SystemOptions.getInstance(iniFileName, iniIO);
					String d = "";
					for (final String setting : inst.getSectionSettings(section)) {
						if (setting != null && setting.equals("description")) {
							d = inst.getString(section, setting, "");
						}
					}
					if (d.length() > 80)
						d = "";
					if (d != null && !d.isEmpty())
						return "<html><center>" + WordUtils.capitalizeFully(section, ' ') + "<br>(" + d + ")";
					else
						return WordUtils.capitalizeFully(section, ' ');
				}
	}
	
	@Override
	public String getDefaultImage() {
		boolean disabled = false;
		boolean enabled = false;
		SystemOptions inst = SystemOptions.getInstance(iniFileName, iniIO);
		for (final String setting : inst.getSectionSettings(section)) {
			if (setting != null && setting.equals("enabled")) {
				if (!inst.getBoolean(section, setting, false)) {
					disabled = true;
					break;
				} else {
					enabled = true;
					break;
				}
			}
			if (setting != null && setting.equals("show_icon")) {
				if (!inst.getBoolean(section, setting, false)) {
					disabled = true;
					break;
				} else {
					enabled = true;
					break;
				}
			}
		}
		if (disabled)
			return "img/ext/gpl2/Gnome-Emblem-Package-64_disabled.png";// "Gnome-Accessories-Text-Editor-64.png";
		else
			if (enabled)
				return "img/ext/gpl2/Gnome-Emblem-Package-64_enabled.png";// "Gnome-Accessories-Text-Editor-64.png";
			else
				return "img/ext/gpl2/Gnome-Emblem-Package-64.png";// "Gnome-Accessories-Text-Editor-64.png";
	}
	
	@Override
	public boolean isProvidingActions() {
		return true;
	}
}
