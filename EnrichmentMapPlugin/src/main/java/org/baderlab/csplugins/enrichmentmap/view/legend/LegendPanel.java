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

// $Id$
// $LastChangedDate$
// $LastChangedRevision$
// $LastChangedBy$
// $HeadURL$

package org.baderlab.csplugins.enrichmentmap.view.legend;

import static javax.swing.GroupLayout.DEFAULT_SIZE;
import static javax.swing.GroupLayout.PREFERRED_SIZE;
import static org.baderlab.csplugins.enrichmentmap.view.util.SwingUtil.makeSmall;
import static org.cytoscape.util.swing.LookAndFeelUtil.equalizeSize;
import static org.cytoscape.util.swing.LookAndFeelUtil.isAquaLAF;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Paint;
import java.text.Collator;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import org.baderlab.csplugins.enrichmentmap.model.EMCreationParameters;
import org.baderlab.csplugins.enrichmentmap.model.EMDataSet;
import org.baderlab.csplugins.enrichmentmap.model.EnrichmentMap;
import org.baderlab.csplugins.enrichmentmap.model.EnrichmentMapManager;
import org.baderlab.csplugins.enrichmentmap.style.EMStyleBuilder;
import org.baderlab.csplugins.enrichmentmap.style.EMStyleBuilder.Colors;
import org.baderlab.csplugins.enrichmentmap.style.EMStyleBuilder.Columns;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.util.swing.BasicCollapsiblePanel;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.presentation.RenderingEngine;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.cytoscape.view.presentation.property.values.NodeShape;
import org.cytoscape.view.vizmap.VisualMappingFunction;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.view.vizmap.mappings.DiscreteMapping;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Right hand information Panel containing files uploaded and legends
 */
@Singleton
@SuppressWarnings("serial")
public class LegendPanel extends JPanel {

	private static final int LEGEND_ICON_SIZE = 18;
	
	private final Border DEF_LEGEND_BORDER = BorderFactory.createLineBorder(UIManager.getColor("Separator.foreground"));
	private final Color DEF_LEGEND_BG = Color.WHITE;
	
	@Inject private EnrichmentMapManager emManager;
	@Inject private CyApplicationManager applicationManager;
	@Inject private VisualMappingManager visualMappingManager;
	
	private BasicCollapsiblePanel nodeLegendPanel;
	private BasicCollapsiblePanel edgeLegendPanel;
	
	private JPanel nodeColorPanel;
	
	private JPanel nodeShapePanel;
	private JLabel nodeShapeIcon1 = new JLabel();
	private JLabel nodeShapeIcon2 = new JLabel();
	private JLabel nodeShapeDesc1 = new JLabel("Gene Set");
	private JLabel nodeShapeDesc2 = new JLabel("Signature Set");
	
	private JPanel edgeColorPanel;
	
	private CyNetworkView networkView;
	
	public LegendPanel() {
		setLayout(new BorderLayout());
		
		Border iconBorder = BorderFactory.createEmptyBorder(4, 4, 4, 4);
		nodeShapeIcon1.setBorder(iconBorder);
		nodeShapeIcon2.setBorder(iconBorder);
		
		makeSmall(nodeShapeDesc1, nodeShapeDesc2);
		equalizeSize(nodeShapeDesc1, nodeShapeDesc2);
	}
	
	public CyNetworkView getNetworkView() {
		return networkView;
	}
	
	/**
	 * Update parameters panel based on given enrichment map parameters
	 */
	void update(Collection<EMDataSet> filteredDataSets, CyNetworkView netView) {
		this.networkView = netView;
		
		removeAll();
		
		EnrichmentMap map = netView != null ? emManager.getEnrichmentMap(netView.getModel().getSUID()) : null;
		EMCreationParameters params = map != null ? map.getParams() : null;

		if (params == null) {
			JLabel infoLabel = new JLabel("No EnrichmentMap View selected");
			infoLabel.setEnabled(false);
			infoLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
			infoLabel.setHorizontalAlignment(JLabel.CENTER);
			infoLabel.setVerticalAlignment(JLabel.CENTER);
			infoLabel.setBorder(new EmptyBorder(120, 40, 120, 40));

			add(infoLabel, BorderLayout.CENTER);
		} else {
			updateNodeColorPanel(filteredDataSets, map, netView);
			updateNodeShapePanel(map, netView);
			updateEdgeColorPanel(map, netView);
			
			JPanel panel = new JPanel();
			final GroupLayout layout = new GroupLayout(panel);
			panel.setLayout(layout);
			layout.setAutoCreateContainerGaps(true);
			layout.setAutoCreateGaps(true);

			layout.setHorizontalGroup(layout.createParallelGroup(Alignment.CENTER, true)
					.addComponent(getNodeLegendPanel(), DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
					.addComponent(getEdgeLegendPanel(), DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
			);
			layout.setVerticalGroup(layout.createSequentialGroup()
					.addComponent(getNodeLegendPanel(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
					.addComponent(getEdgeLegendPanel(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
			);
			
			JScrollPane scrollPane = new JScrollPane(panel);
			add(scrollPane, BorderLayout.CENTER);
		}

		revalidate();
	}

	private void updateNodeColorPanel(Collection<EMDataSet> dataSets, EnrichmentMap map, CyNetworkView netView) {
		JPanel p = getNodeColorPanel();
		p.removeAll();
		
		if (dataSets != null && dataSets.size() == 1) {
			EMDataSet ds = dataSets.iterator().next();
			
			ColorLegendPanel clp = new ColorLegendPanel(
					Colors.MAX_PHENOTYPE_1,
					Colors.MAX_PHENOTYPE_2,
					ds.getEnrichments().getPhenotype1(),
					ds.getEnrichments().getPhenotype2()
			);
			
			GroupLayout layout = (GroupLayout) p.getLayout();
	
			layout.setHorizontalGroup(layout.createSequentialGroup()
					.addGap(0, 0, Short.MAX_VALUE)
					.addComponent(clp, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
					.addGap(0, 0, Short.MAX_VALUE)
			);
			layout.setVerticalGroup(layout.createParallelGroup(Alignment.CENTER, false)
					.addComponent(clp)
			);
			
			p.setVisible(true);
		} else {
			p.setVisible(false);
		}
	}
	
	private void updateNodeShapePanel(EnrichmentMap map, CyNetworkView netView) {
		JPanel p = getNodeShapePanel();
		VisualStyle style = netView != null ? visualMappingManager.getVisualStyle(netView) : null;
		
		nodeShapeIcon1.setVisible(style != null);
		nodeShapeDesc1.setVisible(style != null);
		nodeShapeIcon2.setVisible(style != null && map.hasSignatureDataSets());
		nodeShapeDesc2.setVisible(style != null && map.hasSignatureDataSets());
		
		if (style != null) {
			NodeShape shape = EMStyleBuilder.getGeneSetNodeShape(style);
			nodeShapeIcon1.setIcon(getIcon(BasicVisualLexicon.NODE_SHAPE, shape));
			
			if (map.hasSignatureDataSets()) {
				shape = EMStyleBuilder.getSignatureNodeShape(style);
				nodeShapeIcon2.setIcon(getIcon(BasicVisualLexicon.NODE_SHAPE, shape));
			}
		}
		
		p.revalidate();
	}
	
	private void updateEdgeColorPanel(EnrichmentMap map, CyNetworkView netView) {
		JPanel p = getEdgeColorPanel();
		VisualStyle style = netView != null ? visualMappingManager.getVisualStyle(netView) : null;
		
		JComponent[][] entries = null;
		
		if (map != null) {
			Dimension iconSize = new Dimension(LEGEND_ICON_SIZE, LEGEND_ICON_SIZE / 2);
			
			if (map.getParams().getCreateDistinctEdges() && map.isDistinctExpressionSets()) {
				VisualMappingFunction<?, Paint> mf =
						style.getVisualMappingFunction(BasicVisualLexicon.EDGE_STROKE_UNSELECTED_PAINT);
				
				if (mf instanceof DiscreteMapping) {
					DiscreteMapping<?, Paint> dm = (DiscreteMapping<?, Paint>) mf;
					final Collator collator = Collator.getInstance();
					
					Map<Object, Paint> dmMap = new TreeMap<>((Object o1, Object o2) -> {
						return collator.compare("" + o1, "" + o2);
					});
					dmMap.putAll(dm.getAll());
					dmMap.remove(Columns.EDGE_DATASET_VALUE_COMPOUND);
					
					if (!map.hasSignatureDataSets())
						dmMap.remove(Columns.EDGE_DATASET_VALUE_SIG);
					
					if (dmMap.size() > 0) {
						entries = new JComponent[dmMap.size()][2];
						int i = 0;
						
						for (Entry<?, Paint> e : dmMap.entrySet()) {
							Color color = null;
							
							if (e.getValue() instanceof Color)
								color = (Color) e.getValue();
								
							JLabel iconLabel = createColorLabel(color, iconSize);
							JLabel descLabel = new JLabel("" + e.getKey());
							
							if (Columns.EDGE_DATASET_VALUE_SIG.equals(e.getKey()))
								descLabel.setFont(descLabel.getFont().deriveFont(Font.ITALIC));
							
							entries[i++] = new JComponent[]{ iconLabel, descLabel };
						}
					}
				}
			}
			
			if (entries == null) {
				int rows = map.hasSignatureDataSets() ? 2 : 1;
				entries = new JComponent[rows][2];
				
				{
					JLabel iconLabel = createColorLabel(Colors.COMPOUND_EDGE_COLOR, iconSize);
					JLabel descLabel = new JLabel(Columns.EDGE_DATASET_VALUE_COMPOUND);
					descLabel.setFont(descLabel.getFont().deriveFont(Font.ITALIC));
					
					entries[0] = new JComponent[]{ iconLabel, descLabel };
				}
				
				if (rows == 2) {
					JLabel iconLabel = createColorLabel(Colors.SIG_EDGE_COLOR, iconSize);
					JLabel descLabel = new JLabel(Columns.EDGE_DATASET_VALUE_SIG);
					descLabel.setFont(descLabel.getFont().deriveFont(Font.ITALIC));
					
					entries[1] = new JComponent[]{ iconLabel, descLabel };
				}
			}
		}
		
		updateStyleLegendPanel(entries, p);
	}

	private BasicCollapsiblePanel getNodeLegendPanel() {
		if (nodeLegendPanel == null) {
			nodeLegendPanel = new BasicCollapsiblePanel("Nodes (Gene Sets)");
			nodeLegendPanel.setCollapsed(false);
			
			final GroupLayout layout = new GroupLayout(nodeLegendPanel.getContentPane());
			nodeLegendPanel.getContentPane().setLayout(layout);
			layout.setAutoCreateContainerGaps(false);
			layout.setAutoCreateGaps(true);
			
			layout.setHorizontalGroup(layout.createParallelGroup(Alignment.CENTER, true)
					.addComponent(getNodeColorPanel(), DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
					.addComponent(getNodeShapePanel(), DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
			);
			layout.setVerticalGroup(layout.createSequentialGroup()
					.addComponent(getNodeColorPanel(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
					.addComponent(getNodeShapePanel(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
			);
			
			if (isAquaLAF())
				nodeLegendPanel.setOpaque(false);
		}
		
		return nodeLegendPanel;
	}
	
	private BasicCollapsiblePanel getEdgeLegendPanel() {
		if (edgeLegendPanel == null) {
			edgeLegendPanel = new BasicCollapsiblePanel("Edges (Similarity Between Gene Sets)");
			edgeLegendPanel.setCollapsed(false);
			
			final GroupLayout layout = new GroupLayout(edgeLegendPanel.getContentPane());
			edgeLegendPanel.getContentPane().setLayout(layout);
			layout.setAutoCreateContainerGaps(false);
			layout.setAutoCreateGaps(true);
			
			layout.setHorizontalGroup(layout.createParallelGroup(Alignment.CENTER, true)
					.addComponent(getEdgeColorPanel(), DEFAULT_SIZE, DEFAULT_SIZE, Short.MAX_VALUE)
			);
			layout.setVerticalGroup(layout.createSequentialGroup()
					.addComponent(getEdgeColorPanel(), PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
			);
			
			if (isAquaLAF())
				edgeLegendPanel.setOpaque(false);
		}
		
		return edgeLegendPanel;
	}
	
	private JPanel getNodeColorPanel() {
		if (nodeColorPanel == null) {
			nodeColorPanel = createStyleLegendPanel(null);
			nodeColorPanel.setToolTipText("Node Fill Color: Phenotype * (1-P_value)");
		}
		
		return nodeColorPanel;
	}
	
	private JPanel getNodeShapePanel() {
		if (nodeShapePanel == null) {
			nodeShapePanel = createStyleLegendPanel(null);
			nodeShapePanel.setToolTipText("Node Shape");
			
			GroupLayout layout = (GroupLayout) nodeShapePanel.getLayout();

			layout.setHorizontalGroup(layout.createSequentialGroup()
					.addGap(0, 0, Short.MAX_VALUE)
					.addComponent(nodeShapeIcon1, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
					.addComponent(nodeShapeDesc1, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
					.addPreferredGap(ComponentPlacement.UNRELATED)
					.addComponent(nodeShapeIcon2, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
					.addComponent(nodeShapeDesc2, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
					.addGap(0, 0, Short.MAX_VALUE)
			);
			layout.setVerticalGroup(layout.createParallelGroup(Alignment.CENTER, false)
					.addComponent(nodeShapeIcon1)
					.addComponent(nodeShapeDesc1)
					.addComponent(nodeShapeIcon2)
					.addComponent(nodeShapeDesc2)
			);
		}
		
		return nodeShapePanel;
	}
	
	private JPanel getEdgeColorPanel() {
		if (edgeColorPanel == null) {
			edgeColorPanel = createStyleLegendPanel(null);
			edgeColorPanel.setToolTipText("Edge Stroke Color");
		}
		
		return edgeColorPanel;
	}

	private JPanel createStyleLegendPanel(JComponent[][] entries) {
		JPanel p = new JPanel();
		p.setBorder(DEF_LEGEND_BORDER);
		p.setBackground(DEF_LEGEND_BG);
		
		updateStyleLegendPanel(entries, p);
		
		return p;
	}

	private void updateStyleLegendPanel(JComponent[][] entries, JPanel p) {
		p.removeAll();
		
		GroupLayout layout = new GroupLayout(p);
		p.setLayout(layout);
		layout.setAutoCreateContainerGaps(true);
		layout.setAutoCreateGaps(true);
		
		if (entries != null) {
			ParallelGroup hGroup = layout.createParallelGroup(Alignment.LEADING, true);
			SequentialGroup vGroup = layout.createSequentialGroup();
			
			layout.setHorizontalGroup(hGroup);
			layout.setVerticalGroup(vGroup);
			
			for (JComponent[] row : entries) {
				makeSmall(row[0], row[1]);
				
				hGroup.addGroup(layout.createSequentialGroup()
						.addComponent(row[0], PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
						.addComponent(row[1], PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
				);
				vGroup.addGroup(layout.createParallelGroup(Alignment.CENTER, false)
						.addComponent(row[0])
						.addComponent(row[1])
				);
			}
		}
		
		p.revalidate();
	}
	
	private JLabel createColorLabel(Color color, Dimension size) {
		JLabel iconLabel = new JLabel();
		iconLabel.setOpaque(color != null);
		iconLabel.setPreferredSize(size);
		iconLabel.setMinimumSize(size);
		
		if (color != null)
			iconLabel.setBackground(color);
		
		return iconLabel;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Icon getIcon(VisualProperty<?> vp, Object value) {
		if (value == null)
			return null;
		
		RenderingEngine<CyNetwork> engine = applicationManager.getCurrentRenderingEngine();
		Icon icon = engine.createIcon((VisualProperty)vp, value, LEGEND_ICON_SIZE, LEGEND_ICON_SIZE);
		
		return icon;
	}
}
