package de.ipk_gatersleben.ag_nw.graffiti.plugins.ios.sbml;

import java.util.Collection;
import java.util.HashSet;

import org.AttributeHelper;
import org.graffiti.graph.Graph;
import org.graffiti.plugins.inspectors.defaults.DefaultEditPanel;
import org.sbml.jsbml.Annotation;
import org.sbml.jsbml.xml.XMLNode;

import de.ipk_gatersleben.ag_nw.graffiti.plugins.ios.exporters.sbml.SBML_SBase_Writer;
import de.ipk_gatersleben.ag_nw.graffiti.plugins.ios.importers.sbml.SBML_SBase_Reader;

public class SBMLAssignmentRule {

	/**
	 * Intern graph object
	 */
	Graph g;

	/**
	 * Provides necessary methods
	 */
	SBML_SBase_Writer attWriter;

	/**
	 * Provides necessary methods
	 */
	SBML_SBase_Reader attReader;

	/**
	 * intern representation of the headline
	 */
	String internHeadline;

	/**
	 * The user will see the headline this way
	 */
	String presentedHeadline;

	/**
	 * The hidden assignmentRule attributes are stored here
	 */
	HashSet<String> discardedRowIDs;

	public SBMLAssignmentRule(Graph g, String internHeadline,
			String presentedHeadline) {
		this.g = g;
		attWriter = new SBML_SBase_Writer();
		attReader = new SBML_SBase_Reader();
		this.presentedHeadline = presentedHeadline;
		this.internHeadline = internHeadline;
		initAssignmnetNiceIDs();

		Collection<String> colDiscardedRowIDs = DefaultEditPanel
				.getDiscardedRowIDs();
		discardedRowIDs = new HashSet<String>(colDiscardedRowIDs);
		DefaultEditPanel.setDiscardedRowIDs(discardedRowIDs);
	}

	public SBMLAssignmentRule(Graph g, String internHeadline) {
		this.g = g;
		attWriter = new SBML_SBase_Writer();
		attReader = new SBML_SBase_Reader();
		this.internHeadline = internHeadline;
	}

	public void setMetaID(String metaID) {
		if (!metaID.equals(SBML_Constants.EMPTY)) {
			AttributeHelper.setAttribute(g, internHeadline, new StringBuffer(
					internHeadline).append(SBML_Constants.META_ID).toString(),
					metaID);
		}
	}

	public void setSBOTerm(String sboTerm) {
		if (!sboTerm.equals(SBML_Constants.EMPTY)) {
			AttributeHelper.setAttribute(g, internHeadline, new StringBuffer(
					internHeadline).append(SBML_Constants.SBOTERM).toString(),
					sboTerm);
		}
	}

	public void setAnnotation(Annotation annotation) {
		AttributeHelper.setAttribute(g, internHeadline, new StringBuffer(
				internHeadline).append(SBML_Constants.ANNOTATION).toString(),
				annotation);

		discardedRowIDs.add(new StringBuffer(internHeadline).append(
				SBML_Constants.ANNOTATION).toString());
	}

	public void setNonRDFAnnotation(String nonRDFAnnotation) {
		AttributeHelper.setAttribute(g, internHeadline, new StringBuffer(
				internHeadline).append(SBML_Constants.NON_RDF_ANNOTATION)
				.toString(), nonRDFAnnotation);

		discardedRowIDs.add(new StringBuffer(internHeadline).append(
				SBML_Constants.NON_RDF_ANNOTATION).toString());
	}

	public void setNotes(String notes, XMLNode notesObj) {
		if (!notes.equals(SBML_Constants.EMPTY)) {
			attReader.addNotes(
					notesObj,
					notes,
					g,
					internHeadline,
					new StringBuffer(internHeadline).append(
							SBML_Constants.NOTES).toString());
		}

		discardedRowIDs.add(new StringBuffer(internHeadline).append(
				SBML_Constants.NOTES).toString());
	}

	public void setFunction(String function) {
		if (!function.equals(SBML_Constants.EMPTY)) {
			AttributeHelper.setAttribute(g, internHeadline, new StringBuffer(
					internHeadline).append(SBML_Constants.ASSIGNMENT_FUNCTION)
					.toString(), function);
		}
	}

	public void setVariable(String variable) {
		if (!variable.equals(SBML_Constants.EMPTY)) {
			AttributeHelper.setAttribute(g, internHeadline, new StringBuffer(
					internHeadline).append(SBML_Constants.ASSIGNMENT_VARIABLE)
					.toString(), variable);
		}
	}

	public Boolean isSetVariable() {
		if (AttributeHelper.hasAttribute(g, internHeadline, new StringBuffer(
				internHeadline).append(SBML_Constants.ASSIGNMENT_VARIABLE)
				.toString())) {
			return true;
		} else {
			return false;
		}
	}

	public Boolean isSetFunction() {
		if (AttributeHelper.hasAttribute(g, internHeadline, new StringBuffer(
				internHeadline).append(SBML_Constants.ASSIGNMENT_FUNCTION)
				.toString())) {
			return true;
		} else {
			return false;
		}
	}

	public String getVariable() {
		if (isSetVariable()) {
			return (String) attWriter.getAttribute(
					g,
					internHeadline,
					new StringBuffer(internHeadline).append(
							SBML_Constants.ASSIGNMENT_VARIABLE).toString());
		} else {
			return SBML_Constants.EMPTY;
		}
	}

	public String getFunction() {
		if (isSetFunction()) {
			return (String) attWriter.getAttribute(
					g,
					internHeadline,
					new StringBuffer(internHeadline).append(
							SBML_Constants.ASSIGNMENT_FUNCTION).toString());
		} else {
			return SBML_Constants.EMPTY;
		}
	}

	private void initAssignmnetNiceIDs() {
		AttributeHelper.setNiceId(
				new StringBuffer(internHeadline).append(
						SBML_Constants.ASSIGNMENT_VARIABLE).toString(),
				presentedHeadline + ": Variable");
		AttributeHelper.setNiceId(
				new StringBuffer(internHeadline).append(
						SBML_Constants.ASSIGNMENT_FUNCTION).toString(),
				presentedHeadline + ": Function");
		AttributeHelper.setNiceId(
				new StringBuffer(internHeadline).append(SBML_Constants.META_ID)
						.toString(), presentedHeadline + ": Meta ID");
		AttributeHelper.setNiceId(
				new StringBuffer(internHeadline).append(SBML_Constants.NOTES)
						.toString(), presentedHeadline + ": Notes");
		AttributeHelper.setNiceId(
				new StringBuffer(internHeadline).append(SBML_Constants.SBOTERM)
						.toString(), presentedHeadline + ": SBOTerm");

	}
}
