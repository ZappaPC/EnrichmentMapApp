/**
 **                       EnrichmentMap Cytoscape Plugin
 **
 ** Copyright (c) 2008-2009 Bader Lab, Donnelly Centre for Cellular and Biomolecular 
 ** Research, University of Toronto
 **
 ** Contact: http://www.baderlab.org
 **
 ** Code written by: Ruth Isserlin
 ** Authors: Daniele Merico, Ruth Isserlin, Oliver Stueker, Gary D. Bader
 **
 ** This library is free software; you can redistribute it and/or modify it
 ** under the terms of the GNU Lesser General Public License as published
 ** by the Free Software Foundation; either version 2.1 of the License, or
 ** (at your option) any later version.
 **
 ** This library is distributed in the hope that it will be useful, but
 ** WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 ** MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 ** documentation provided hereunder is on an "as is" basis, and
 ** University of Toronto
 ** has no obligations to provide maintenance, support, updates, 
 ** enhancements or modifications.  In no event shall the
 ** University of Toronto
 ** be liable to any party for direct, indirect, special,
 ** incidental or consequential damages, including lost profits, arising
 ** out of the use of this software and its documentation, even if
 ** University of Toronto
 ** has been advised of the possibility of such damage.  
 ** See the GNU Lesser General Public License for more details.
 **
 ** You should have received a copy of the GNU Lesser General Public License
 ** along with this library; if not, write to the Free Software Foundation,
 ** Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 **
 **/

// $Id: BuildEnrichmentMapTask.java 383 2009-10-08 20:06:35Z risserlin $
// $LastChangedDate: 2009-10-08 16:06:35 -0400 (Thu, 08 Oct 2009) $
// $LastChangedRevision: 383 $
// $LastChangedBy: risserlin $
// $HeadURL: svn+ssh://risserlin@server1.baderlab.med.utoronto.ca/svn/EnrichmentMap/trunk/EnrichmentMapPlugin/src/org/baderlab/csplugins/enrichmentmap/BuildEnrichmentMapTask.java $

package org.baderlab.csplugins.enrichmentmap.autoannotate.task;

import java.util.ArrayList;
import java.util.List;

import org.baderlab.csplugins.enrichmentmap.autoannotate.model.AnnotationSet;
import org.baderlab.csplugins.enrichmentmap.autoannotate.model.Cluster;
import org.baderlab.csplugins.enrichmentmap.autoannotate.model.NodeText;
import org.baderlab.csplugins.enrichmentmap.autoannotate.model.WordUtils;
import org.baderlab.csplugins.enrichmentmap.autoannotate.view.AnnotationDisplayPanel;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.command.CommandExecutorTaskFactory;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.annotations.AnnotationFactory;
import org.cytoscape.view.presentation.annotations.AnnotationManager;
import org.cytoscape.view.presentation.annotations.ShapeAnnotation;
import org.cytoscape.view.presentation.annotations.TextAnnotation;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;

/**
 * Created by
 * User: arkadyark
 * Date: June 17, 2014
 * Time: 11:43 AM
 */

public class AutoAnnotatorTask extends AbstractTask {
	
	private CySwingApplication application;
	private AnnotationManager annotationManager;
	private CyNetwork network;
	private CyNetworkView view;
	private String nameColumnName;
	private String clusterColumnName;
	private CyServiceRegistrar registrar;
	private AnnotationDisplayPanel displayPanel;
	private int annotationSetNumber;
	private String annotationSetName;

	public AutoAnnotatorTask(CySwingApplication application, CyApplicationManager applicationManager, 
			CyNetworkViewManager networkViewManager, CyNetworkManager networkManager,
			AnnotationManager annotationManager, AnnotationDisplayPanel displayPanel, CyNetworkView selectedView, String clusterColumnName, 
			String nameColumnName, int annotationSetNumber, CyServiceRegistrar registrar){
		
		this.application = application;
		this.annotationManager = annotationManager;
		this.displayPanel = displayPanel;
		this.view = selectedView;
		this.network = view.getModel();
		this.clusterColumnName = clusterColumnName;
		this.nameColumnName = nameColumnName;
		this.annotationSetNumber = annotationSetNumber;
		this.registrar = registrar;
		
	};

	@Override
	public void cancel() {
		return;
	}
	
	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		taskMonitor.setTitle("Annotating Enrichment Map");
	
		taskMonitor.setProgress(0.1);
		taskMonitor.setStatusMessage("Getting clusters...");
		if (cancelled) return;

    	CyTable networkTable = network.getDefaultNetworkTable();
    	try {
    		networkTable.deleteColumn("Annotation Running");
    		networkTable.createColumn("Annotation Running", Boolean.class, false);
    	} catch (Exception e) {
    		networkTable.createColumn("Annotation Running", Boolean.class, false);
    	}
    	networkTable.getAllRows().get(0).set("Annotation Running", true);

    	annotationSetName = "Annotation Set " + String.valueOf(annotationSetNumber);
    	AnnotationSet clusters = makeClusters(network, view, annotationSetName);
    	
    	taskMonitor.setProgress(0.4);
    	taskMonitor.setStatusMessage("Running WordCloud...");
    	if (cancelled) return;
    	
    	runWordCloud(taskMonitor);
    	
    	taskMonitor.setProgress(0.7);
    	taskMonitor.setStatusMessage("Annotating Clusters...");
    	if (cancelled) return;

    	// Gives WordCloud time to finish - the command Task finishes when WordCloud starts
    	while (true) {
    		try {
    			clusters.updateLabels();
    			break;
    		} catch(NullPointerException e) {
    			if (cancelled) return;
    			continue;
    		}
    	}
		displayPanel.addClusters(clusters); // Clusters get drawn inside of displayPanel
		displayPanel.updateSelectedView(view);
		CytoPanel southPanel = application.getCytoPanel(CytoPanelName.SOUTH);
		southPanel.setSelectedIndex(southPanel.indexOfComponent(displayPanel));
		
		network.getDefaultNetworkTable().deleteColumn("Annotation Running");

		taskMonitor.setProgress(1.0);
		taskMonitor.setStatusMessage("Done!");
	}
	
	private AnnotationSet makeClusters(CyNetwork network, CyNetworkView networkView, String name) {
		AnnotationSet clusters = new AnnotationSet(network, networkView, clusterColumnName);
		clusters.setName(name);
		
		@SuppressWarnings("unchecked")
		AnnotationFactory<ShapeAnnotation> shapeFactory = (AnnotationFactory<ShapeAnnotation>) registrar.getService(AnnotationFactory.class, "(type=ShapeAnnotation.class)");    	
		@SuppressWarnings("unchecked")
		AnnotationFactory<TextAnnotation> textFactory = (AnnotationFactory<TextAnnotation>) registrar.getService(AnnotationFactory.class, "(type=TextAnnotation.class)");
		
		List<CyNode> nodes = network.getNodeList();
		for (CyNode node : nodes) {
			Integer clusterNumber;
			while (true) { // If clustermaker has not finished yet
				try {
					clusterNumber = network.getRow(node).get(this.clusterColumnName, Integer.class);
					break;
				} catch (ClassCastException e) {
					continue;
				}
			}
			if (clusterNumber != null) {
				View<CyNode> nodeView = networkView.getNodeView(node);
				double x = nodeView.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION);
				double y = nodeView.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION);
				double[] coordinates = {x, y};
				
				String nodeName = network.getRow(node).get(nameColumnName, String.class);
				NodeText nodeText = new NodeText();
				nodeText.setName(nodeName);
				
				// empty values (no cluster) are given null
				Cluster cluster;
				if (clusters.clusterSet.keySet().contains(clusterNumber)) {
					cluster = clusters.clusterSet.get(clusterNumber);
				} else {
					cluster = new Cluster(clusterNumber, network, networkView, annotationManager, clusterColumnName, shapeFactory, clusters, textFactory, registrar);
					clusters.addCluster(cluster);
				}
				cluster.addNode(node);
				cluster.addCoordinates(coordinates);
				cluster.addNodeText(nodeText);
			}
		}
		return clusters;
	}
	
	private void runWordCloud(TaskMonitor taskMonitor) {
		CommandExecutorTaskFactory executor = registrar.getService(CommandExecutorTaskFactory.class);
		ArrayList<String> commands = new ArrayList<String>();
		String command = "wordcloud build clusterColumnName=\"" + clusterColumnName + "\" nameColumnName=\""
				+ nameColumnName + "\"" + " cloudNamePrefix=\"" + annotationSetName + " \"";
		commands.add(command);
		TaskIterator task = executor.createTaskIterator(commands, null);
		try {
			// Uses the same TaskMonitor as the main Task
			task.next().run(taskMonitor);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}