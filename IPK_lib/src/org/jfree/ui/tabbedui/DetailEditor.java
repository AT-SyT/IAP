/*
 * ========================================================================
 * JCommon : a free general purpose class library for the Java(tm) platform
 * ========================================================================
 * (C) Copyright 2000-2004, by Object Refinery Limited and Contributors.
 * Project Info: http://www.jfree.org/jcommon/index.html
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation;
 * either version 2.1 of the License, or (at your option) any later version.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * library; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 * [Java is a trademark or registered trademark of Sun Microsystems, Inc.
 * in the United States and other countries.]
 * -----------------
 * DetailEditor.java
 * -----------------
 * (C) Copyright 2004, by Thomas Morgner and Contributors.
 * Original Author: Thomas Morgner;
 * Contributor(s): David Gilbert (for Object Refinery Limited);
 * $Id: DetailEditor.java,v 1.1 2011-01-31 09:02:57 klukas Exp $
 * Changes
 * -------
 * 07-Jun-2004 : Added JCommon header (DG);
 */

package org.jfree.ui.tabbedui;

import javax.swing.JComponent;

/**
 * A detail editor.
 */
public abstract class DetailEditor extends JComponent {

	private Object object;
	private boolean confirmed;

	/**
	 * Creates a new editor.
	 */
	public DetailEditor() {
		// nothing required
	}

	/**
	 * Updates the object.
	 */
	public void update() {
		if (this.object == null) {
			throw new IllegalStateException();
		} else {
			updateObject(this.object);
		}
		setConfirmed(false);
	}

	/**
	 * Returns the object.
	 * 
	 * @return The object.
	 */
	public Object getObject() {
		return this.object;
	}

	/**
	 * Sets the object to be edited.
	 * 
	 * @param object
	 *           the object.
	 */
	public void setObject(final Object object) {
		if (object == null) {
			throw new NullPointerException();
		}
		this.object = object;
		setConfirmed(false);
		fillObject();
	}

	/**
	 * Parses an integer.
	 * 
	 * @param text
	 *           the text.
	 * @param def
	 *           the default value.
	 * @return The parsed integer, or the default value if the string didn't contain a
	 *         value.
	 */
	protected static int parseInt(final String text, final int def) {
		try {
			return Integer.parseInt(text);
		} catch (NumberFormatException fe) {
			return def;
		}
	}

	/**
	 * Clears the editor.
	 */
	public abstract void clear();

	/**
	 * Edits the object. The object itself should not be modified, until
	 * update or create was called.
	 */
	protected abstract void fillObject();

	/**
	 * Updates the object.
	 * 
	 * @param object
	 *           the object.
	 */
	protected abstract void updateObject(Object object);

	/**
	 * Returns the confirmed flag.
	 * 
	 * @return The confirmed flag.
	 */
	public boolean isConfirmed() {
		return this.confirmed;
	}

	/**
	 * Sets the confirmed flag.
	 * 
	 * @param confirmed
	 *           the confirmed flag.
	 */
	protected void setConfirmed(final boolean confirmed) {
		final boolean oldConfirmed = this.confirmed;
		this.confirmed = confirmed;
		firePropertyChange("confirmed", oldConfirmed, confirmed);
	}

}
