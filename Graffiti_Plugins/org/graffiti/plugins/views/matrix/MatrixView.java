// ==============================================================================
//
// MatrixView.java
//
// Copyright (c) 2001-2004 Gravisto Team, University of Passau
//
// ==============================================================================
// $Id: MatrixView.java,v 1.1 2011-01-31 09:03:37 klukas Exp $

package org.graffiti.plugins.views.matrix;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.BackgroundTaskStatusProviderSupportingExternalCall;
import org.graffiti.attributes.AttributeConsumer;
import org.graffiti.attributes.CollectionAttribute;
import org.graffiti.event.AttributeEvent;
import org.graffiti.event.EdgeEvent;
import org.graffiti.event.GraphEvent;
import org.graffiti.event.NodeEvent;
import org.graffiti.event.TransactionEvent;
import org.graffiti.graph.Graph;
import org.graffiti.graph.GraphElement;
import org.graffiti.plugin.view.AbstractView;
import org.graffiti.plugin.view.MessageListener;

/**
 * Provides a matrix view for the given graph.
 * 
 * @version $Revision: 1.1 $
 */
public class MatrixView
					extends AbstractView
					implements ActionListener, AttributeConsumer {
	// ~ Static fields/initializers =============================================
	
	// ~ Instance fields ========================================================
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/** The table, which contains the matrix of the graph. */
	private JTable matrixView;
	
	/** The table model. */
	private MatrixModel matrix;
	
	// ~ Constructors ===========================================================
	
	/**
	 * Constructs a new matrix view from the given graph.
	 */
	public MatrixView() {
		this(null);
	}
	
	/**
	 * Constructs a new matrix view.
	 * 
	 * @param graph
	 *           the graph to be displayed.
	 */
	public MatrixView(Graph graph) {
		super(graph);
		
		setLayout(new BorderLayout());
		
		matrixView = new JTable() {
			private static final long serialVersionUID = 1L;
			
			@Override
			public Component prepareRenderer(TableCellRenderer renderer, int rowIndex, int vColIndex) {
				Component c = super.prepareRenderer(renderer, rowIndex, vColIndex);
				if (c instanceof JComponent) {
					MatrixModel mm = (MatrixModel) getModel();
					JComponent jc = (JComponent) c;
					jc.setToolTipText(mm.getColumnName(vColIndex) + " - " + mm.getRowName(rowIndex));
				}
				return c;
			}
		};
		matrixView.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		matrixView.setColumnSelectionAllowed(true);
		matrixView.setCellSelectionEnabled(true);
		add(matrixView, BorderLayout.CENTER);
	}
	
	// ~ Methods ================================================================
	
	/**
	 * @see java.awt.dnd.Autoscroll#getAutoscrollInsets()
	 */
	@Override
	public Insets getAutoscrollInsets() {
		return new Insets(0, 0, 0, 0);
	}
	
	/**
	 * @see org.graffiti.attributes.AttributeConsumer#getEdgeAttribute()
	 */
	public CollectionAttribute getEdgeAttribute() {
		return null; // this view does not depend on any edge attributes
	}
	
	/**
	 * @see org.graffiti.plugin.view.AbstractView#setGraph(Graph)
	 */
	@Override
	public void setGraph(Graph graph) {
		currentGraph = graph;
		matrix = new MatrixModel(graph);
		matrixView.setModel(matrix);
		updateGUI();
	}
	
	/**
	 * @see org.graffiti.attributes.AttributeConsumer#getGraphAttribute()
	 */
	public CollectionAttribute getGraphAttribute() {
		return null; // this view does not depend on any graph attributes
	}
	
	/**
	 * @see org.graffiti.attributes.AttributeConsumer#getNodeAttribute()
	 */
	public CollectionAttribute getNodeAttribute() {
		return null; // this view does not depend on any node attributes
	}
	
	/**
	 * @see org.graffiti.plugin.view.View#getViewComponent()
	 */
	@Override
	public JComponent getViewComponent() {
		return this;
	}
	
	/**
	 * @see org.graffiti.plugin.view.View#getViewName()
	 */
	@Override
	public String getViewName() {
		return null;
	}
	
	/**
	 * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
	 */
	public void actionPerformed(ActionEvent action) {
		// Object src = action.getSource();
	}
	
	/**
	 * @see org.graffiti.plugin.view.View#addMessageListener(MessageListener)
	 */
	@Override
	public void addMessageListener(MessageListener ml) {
	}
	
	/**
	 * @see java.awt.dnd.Autoscroll#autoscroll(Point)
	 */
	@Override
	public void autoscroll(Point arg0) {
	}
	
	/**
	 * @see org.graffiti.plugin.view.View#close()
	 */
	@Override
	public void close() {
	}
	
	/**
	 * @see org.graffiti.plugin.view.View#completeRedraw()
	 */
	public void completeRedraw() {
		setGraph(currentGraph);
	}
	
	/**
	 * @see org.graffiti.event.AttributeListener#postAttributeAdded(AttributeEvent)
	 */
	@Override
	public void postAttributeAdded(AttributeEvent e) {
	}
	
	/**
	 * @see org.graffiti.event.AttributeListener#postAttributeChanged(AttributeEvent)
	 */
	@Override
	public void postAttributeChanged(AttributeEvent e) {
	}
	
	/**
	 * @see org.graffiti.event.AttributeListener#postAttributeRemoved(AttributeEvent)
	 */
	@Override
	public void postAttributeRemoved(AttributeEvent e) {
	}
	
	/**
	 * @see org.graffiti.event.EdgeListener#postDirectedChanged(EdgeEvent)
	 */
	@Override
	public void postDirectedChanged(EdgeEvent e) {
	}
	
	/**
	 * @see org.graffiti.event.GraphListener#postEdgeAdded(GraphEvent)
	 */
	@Override
	public void postEdgeAdded(GraphEvent e) {
		matrix.postEdgeAdded(e);
	}
	
	/**
	 * @see org.graffiti.event.GraphListener#postEdgeRemoved(GraphEvent)
	 */
	@Override
	public void postEdgeRemoved(GraphEvent e) {
		matrix.postEdgeRemoved(e);
		updateGUI();
	}
	
	/**
	 * @see org.graffiti.event.EdgeListener#postEdgeReversed(EdgeEvent)
	 */
	@Override
	public void postEdgeReversed(EdgeEvent e) {
	}
	
	/**
	 * @see org.graffiti.event.GraphListener#postGraphCleared(GraphEvent)
	 */
	@Override
	public void postGraphCleared(GraphEvent e) {
		matrix.postGraphCleared(e);
		updateGUI();
	}
	
	/**
	 * @see org.graffiti.event.NodeListener#postInEdgeAdded(NodeEvent)
	 */
	@Override
	public void postInEdgeAdded(NodeEvent e) {
	}
	
	/**
	 * @see org.graffiti.event.NodeListener#postInEdgeRemoved(NodeEvent)
	 */
	@Override
	public void postInEdgeRemoved(NodeEvent e) {
	}
	
	/**
	 * @see org.graffiti.event.GraphListener#postNodeAdded(GraphEvent)
	 */
	@Override
	public void postNodeAdded(GraphEvent e) {
		matrix.postNodeAdded(e);
		updateGUI();
	}
	
	/**
	 * @see org.graffiti.event.GraphListener#postNodeRemoved(GraphEvent)
	 */
	@Override
	public void postNodeRemoved(GraphEvent e) {
		matrix.postNodeRemoved(e);
		updateGUI();
	}
	
	/**
	 * @see org.graffiti.event.NodeListener#postOutEdgeAdded(NodeEvent)
	 */
	@Override
	public void postOutEdgeAdded(NodeEvent e) {
	}
	
	/**
	 * @see org.graffiti.event.NodeListener#postOutEdgeRemoved(NodeEvent)
	 */
	@Override
	public void postOutEdgeRemoved(NodeEvent e) {
	}
	
	/**
	 * @see org.graffiti.event.EdgeListener#postSourceNodeChanged(EdgeEvent)
	 */
	@Override
	public void postSourceNodeChanged(EdgeEvent e) {
	}
	
	/**
	 * @see org.graffiti.event.EdgeListener#postTargetNodeChanged(EdgeEvent)
	 */
	@Override
	public void postTargetNodeChanged(EdgeEvent e) {
	}
	
	/**
	 * @see org.graffiti.event.NodeListener#postUndirectedEdgeAdded(NodeEvent)
	 */
	@Override
	public void postUndirectedEdgeAdded(NodeEvent e) {
	}
	
	/**
	 * @see org.graffiti.event.NodeListener#postUndirectedEdgeRemoved(NodeEvent)
	 */
	@Override
	public void postUndirectedEdgeRemoved(NodeEvent e) {
	}
	
	/**
	 * @see org.graffiti.event.AttributeListener#preAttributeAdded(AttributeEvent)
	 */
	@Override
	public void preAttributeAdded(AttributeEvent e) {
	}
	
	/**
	 * @see org.graffiti.event.AttributeListener#preAttributeChanged(AttributeEvent)
	 */
	@Override
	public void preAttributeChanged(AttributeEvent e) {
	}
	
	/**
	 * @see org.graffiti.event.AttributeListener#preAttributeRemoved(AttributeEvent)
	 */
	@Override
	public void preAttributeRemoved(AttributeEvent e) {
	}
	
	/**
	 * @see org.graffiti.event.EdgeListener#preDirectedChanged(EdgeEvent)
	 */
	@Override
	public void preDirectedChanged(EdgeEvent e) {
	}
	
	/**
	 * @see org.graffiti.event.GraphListener#preEdgeAdded(GraphEvent)
	 */
	@Override
	public void preEdgeAdded(GraphEvent e) {
	}
	
	/**
	 * @see org.graffiti.event.GraphListener#preEdgeRemoved(GraphEvent)
	 */
	@Override
	public void preEdgeRemoved(GraphEvent e) {
	}
	
	/**
	 * @see org.graffiti.event.EdgeListener#preEdgeReversed(EdgeEvent)
	 */
	@Override
	public void preEdgeReversed(EdgeEvent e) {
	}
	
	/**
	 * @see org.graffiti.event.GraphListener#preGraphCleared(GraphEvent)
	 */
	@Override
	public void preGraphCleared(GraphEvent e) {
	}
	
	/**
	 * @see org.graffiti.event.NodeListener#preInEdgeAdded(NodeEvent)
	 */
	@Override
	public void preInEdgeAdded(NodeEvent e) {
	}
	
	/**
	 * @see org.graffiti.event.NodeListener#preInEdgeRemoved(NodeEvent)
	 */
	@Override
	public void preInEdgeRemoved(NodeEvent e) {
	}
	
	/**
	 * @see org.graffiti.event.GraphListener#preNodeAdded(GraphEvent)
	 */
	@Override
	public void preNodeAdded(GraphEvent e) {
	}
	
	/**
	 * @see org.graffiti.event.GraphListener#preNodeRemoved(GraphEvent)
	 */
	@Override
	public void preNodeRemoved(GraphEvent e) {
	}
	
	/**
	 * @see org.graffiti.event.NodeListener#preOutEdgeAdded(NodeEvent)
	 */
	@Override
	public void preOutEdgeAdded(NodeEvent e) {
	}
	
	/**
	 * @see org.graffiti.event.NodeListener#preOutEdgeRemoved(NodeEvent)
	 */
	@Override
	public void preOutEdgeRemoved(NodeEvent e) {
	}
	
	/**
	 * @see org.graffiti.event.EdgeListener#preSourceNodeChanged(EdgeEvent)
	 */
	@Override
	public void preSourceNodeChanged(EdgeEvent e) {
	}
	
	/**
	 * @see org.graffiti.event.EdgeListener#preTargetNodeChanged(EdgeEvent)
	 */
	@Override
	public void preTargetNodeChanged(EdgeEvent e) {
	}
	
	/**
	 * @see org.graffiti.event.NodeListener#preUndirectedEdgeAdded(NodeEvent)
	 */
	@Override
	public void preUndirectedEdgeAdded(NodeEvent e) {
	}
	
	/**
	 * @see org.graffiti.event.NodeListener#preUndirectedEdgeRemoved(NodeEvent)
	 */
	@Override
	public void preUndirectedEdgeRemoved(NodeEvent e) {
	}
	
	/**
	 * @see org.graffiti.plugin.view.View#removeMessageListener(MessageListener)
	 */
	@Override
	public void removeMessageListener(MessageListener ml) {
	}
	
	/**
	 * @see org.graffiti.plugin.view.View#repaint(GraphElement)
	 */
	public void repaint(GraphElement ge) {
	}
	
	/**
	 * @see org.graffiti.event.TransactionListener#transactionFinished(TransactionEvent)
	 */
	public void transactionFinished(TransactionEvent e, BackgroundTaskStatusProviderSupportingExternalCall status) {
		matrix.transactionFinished(e, status);
		updateGUI();
	}
	
	/**
	 * @see org.graffiti.event.TransactionListener#transactionStarted(TransactionEvent)
	 */
	@Override
	public void transactionStarted(TransactionEvent e) {
		matrix.transactionStarted(e);
		updateGUI();
	}
	
	/**
	 * Extracts the name of this view class. It has to be overridden by all
	 * extended subclasses of this class.
	 * 
	 * @return DOCUMENT ME!
	 */
	@Override
	protected String extractName() {
		return this.getClass().getName();
	}
	
	/**
	 * @see org.graffiti.plugin.view.AbstractView#informMessageListener(String, int)
	 */
	protected void informMessageListener(String message, int type) {
	}
	
	/**
	 * Updates the gui.
	 */
	private void updateGUI() {
		TableColumn column;
		
		for (int i = 0; i < matrixView.getColumnCount(); i++) {
			column = matrixView.getColumnModel().getColumn(i);
			column.setPreferredWidth(18);
			column.setMaxWidth(18);
			column.setMinWidth(18);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.graffiti.plugin.view.View#clearComponents()
	 */
	public void clearComponents() {
		// empty
	}
	
	public boolean putInScrollPane() {
		return true;
	}
}

// ------------------------------------------------------------------------------
// end of file
// ------------------------------------------------------------------------------
