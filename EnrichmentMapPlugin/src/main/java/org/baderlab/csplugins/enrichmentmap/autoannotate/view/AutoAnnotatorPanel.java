package org.baderlab.csplugins.enrichmentmap.autoannotate.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

import org.baderlab.csplugins.enrichmentmap.EnrichmentMapManager;
import org.baderlab.csplugins.enrichmentmap.autoannotate.AutoAnnotationManager;
import org.baderlab.csplugins.enrichmentmap.autoannotate.model.AnnotationSet;
import org.baderlab.csplugins.enrichmentmap.autoannotate.model.Cluster;
import org.baderlab.csplugins.enrichmentmap.autoannotate.task.AutoAnnotatorTaskFactory;
import org.baderlab.csplugins.enrichmentmap.heatmap.HeatMapParameters;
import org.baderlab.csplugins.enrichmentmap.view.ParametersPanel;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.util.swing.BasicCollapsiblePanel;
import org.cytoscape.util.swing.OpenBrowser;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.presentation.annotations.AnnotationManager;
import org.cytoscape.work.swing.DialogTaskManager;

/**
 * @author arkadyark
 * <p>
 * Date   June 16, 2014<br>
 * Time   11:26:32 AM<br>
 */

public class AutoAnnotatorPanel extends JPanel implements CytoPanelComponent {

	private static final long serialVersionUID = 7901088595186775935L;
	private static final String defaultButtonString = "Use clusterMaker defaults";
	private static final String specifyColumnButtonString = "Select cluster column";
	private String nameColumnName;
	public JComboBox clusterColumnDropdown;
	public JComboBox nameColumnDropdown;
	private int annotationSetNumber;
	private JComboBox clusterAlgorithmDropdown;
	private ButtonGroup radioButtonGroup;
	private TreeMap<String, String> algorithmToColumnName;
	private JRadioButton defaultButton;
	private JRadioButton specifyColumnButton;
	private CyNetworkView selectedView;
	private CyNetwork selectedNetwork;
	private CyTableManager tableManager;
	public JLabel networkLabel;
	private HashMap<CyNetworkView, JComboBox> networkViewToClusterSetDropdown;
	private HashMap<String, AnnotationSet> clusterSets;
	private HashMap<AnnotationSet, JTable> clustersToTables;
	private AutoAnnotationManager autoAnnotationManager;
	public ParametersPanel heatmapParamsPanel;
	private JPanel mainPanel;
	private JPanel clusterTablePanel;
	private boolean showHeatmap;
	private Cluster selectedCluster;
	private EnrichmentMapManager emManager;
	private JComboBox sourceColumnDropdown;

	private AutoAnnotatorTaskFactory autoAnnotatorTaskFactory;

	public AutoAnnotatorPanel(CyApplicationManager cyApplicationManagerRef, 
			CyNetworkViewManager cyNetworkViewManagerRef, CySwingApplication application,
			OpenBrowser openBrowserRef, CyNetworkManager cyNetworkManagerRef, AnnotationManager annotationManager,
			AutoAnnotationManager autoAnnotationManager, CyServiceRegistrar registrar, DialogTaskManager dialogTaskManager, 
			CyEventHelper eventHelper, CyTableManager tableManager, EnrichmentMapManager emManager){

		this.tableManager = tableManager;
		this.autoAnnotationManager = autoAnnotationManager;
		this.clusterSets = new HashMap<String, AnnotationSet>();
		this.clustersToTables = new HashMap<AnnotationSet, JTable>();
		this.networkViewToClusterSetDropdown = new HashMap<CyNetworkView, JComboBox>();
		this.emManager = emManager;
		annotationSetNumber = 1;
		
		algorithmToColumnName = new TreeMap<String, String>();		
		algorithmToColumnName.put("Affinity Propagation Cluster", "__APCluster");
		algorithmToColumnName.put("Cluster Fuzzifier", "__fuzzifierCluster");
		algorithmToColumnName.put("Community cluster (GLay)", "__glayCluster");
		algorithmToColumnName.put("ConnectedComponents Cluster", "__ccCluster");
		algorithmToColumnName.put("Fuzzy C-Means Cluster", "__fcmlCluster");
		algorithmToColumnName.put("MCL Cluster", "__mclCluster");
		algorithmToColumnName.put("SCPS Cluster", "__scpsCluster");
		
		mainPanel = createMainPanel(cyApplicationManagerRef, cyNetworkViewManagerRef, application,
				openBrowserRef, cyNetworkManagerRef, annotationManager, registrar, dialogTaskManager, eventHelper);
		add(mainPanel,BorderLayout.CENTER);
		setPreferredSize(new Dimension(500, getHeight()));
	}
	
	private JPanel createMainPanel(final CyApplicationManager cyApplicationManagerRef,
			final CyNetworkViewManager cyNetworkViewManagerRef, final CySwingApplication application,
			final OpenBrowser openBrowserRef, final CyNetworkManager cyNetworkManagerRef, final AnnotationManager annotationManager,
			final CyServiceRegistrar registrar, final DialogTaskManager dialogTaskManager, final CyEventHelper eventHelper) {
		
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        
		networkLabel = new JLabel("No network selected");
		Font font = networkLabel.getFont();
		networkLabel.setFont(new Font(font.getFamily(), font.getStyle(), 18));
		
		JLabel nameColumnDropdownLabel = new JLabel("   Select the column with the gene set descriptions:");
        // Give the user a choice of column with gene names
        nameColumnDropdown = new JComboBox();
        nameColumnDropdown.addItemListener(new ItemListener(){
        	public void itemStateChanged(ItemEvent itemEvent) {
                nameColumnName = (String) itemEvent.getItem();
            }
        });
        
        // Collapsible panel with advanced cluster options
        JPanel advancedOptionsPanel = createAdvancedOptionsPanel(); 
        
        // Run the annotation
        JButton annotateButton = new JButton("Annotate!");
        ActionListener annotateAction = new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if (selectedView != null) {
					String clusterColumnName = "";
					String algorithm = "";
					// If using default clustermaker parameters
					if (defaultButton.isSelected()) {
						algorithm = (String) clusterAlgorithmDropdown.getSelectedItem();
						clusterColumnName = algorithmToColumnName.get(algorithm);
					} else if (specifyColumnButton.isSelected()) {
						// If using a user specified column
						clusterColumnName = (String) clusterColumnDropdown.getSelectedItem();
					}
					String sourceColumnName = "";
					autoAnnotatorTaskFactory = new AutoAnnotatorTaskFactory(application, cyApplicationManagerRef, 
							cyNetworkViewManagerRef, cyNetworkManagerRef, annotationManager, autoAnnotationManager, selectedView, 
							clusterColumnName, nameColumnName, sourceColumnName, algorithm, annotationSetNumber, registrar, dialogTaskManager, tableManager);
					dialogTaskManager.execute(autoAnnotatorTaskFactory.createTaskIterator());
					annotationSetNumber++;
				} else {
					throw new IllegalArgumentException("Load a network");
				}
			}
        };
        annotateButton.addActionListener(annotateAction);
        
        clusterTablePanel = new JPanel();
        clusterTablePanel.setLayout(new BorderLayout());
        clusterTablePanel.setPreferredSize(new Dimension(350, 350));
        clusterTablePanel.setMaximumSize(new Dimension(350, 350));
        
        BasicCollapsiblePanel selectionPanel = createSelectionPanel();
        
        JButton clearButton = new JButton("Remove Annotation Set");
        ActionListener clearActionListener = new ActionListener(){
        	public void actionPerformed(ActionEvent e) {
        		JComboBox clusterSetDropdown = networkViewToClusterSetDropdown.get(selectedView);
        		AnnotationSet clusters = (AnnotationSet) clusterSetDropdown.getSelectedItem();
        		// Delete wordCloud table
        		tableManager.deleteTable(selectedNetwork.getDefaultNetworkTable().getRow(selectedNetwork.getSUID()).get(clusters.name, Long.class));
        		// Delete all annotations
        		setHeatMapNoSort();
         		clusters.destroyAnnotations();
         		clusterSetDropdown.removeItem(clusterSetDropdown.getSelectedItem());
         		remove(clustersToTables.get(clusters).getParent());
         		clustersToTables.remove(clusters);
        	}
        };
        clearButton.addActionListener(clearActionListener); 
        
        JButton updateButton = new JButton("Update Annotation Set");
        ActionListener updateActionListener = new ActionListener(){
        	public void actionPerformed(ActionEvent e) {
        		for (CyRow row : selectedNetwork.getDefaultNodeTable().getAllRows()) {
        			row.set(CyNetwork.SELECTED, false);
        		}
        		JComboBox clusterSetDropdown = networkViewToClusterSetDropdown.get(selectedView);
        		AnnotationSet clusters = (AnnotationSet) clusterSetDropdown.getSelectedItem(); 
        		clusters.updateCoordinates();
        		clusters.updateLabels();
        		clusters.eraseAnnotations(); 
        		clusters.drawAnnotations();
        		// Update the table if the value has changed (WordCloud has been updated)
        		DefaultTableModel model = (DefaultTableModel) clustersToTables.get(clusters).getModel();
        		int i = 0;
        		for (Cluster cluster : clusters.clusterSet.values()) {
        			if (!(model.getValueAt(i, 1).equals(cluster.getLabel()))) {
        				model.setValueAt(cluster.getLabel(), i, 1);
        			}
        			i++;
        		}
        	}
        };
        updateButton.addActionListener(updateActionListener); 
        
//        JButton mergeButton = new JButton("Merge Clusters");
//        ActionListener mergeActionListener = new ActionListener(){
//        	public void actionPerformed(ActionEvent e) {
//        		JComboBox clusterSetDropdown = networkViewToClusterSetDropdown.get(selectedView);
//        		AnnotationSet clusters = (AnnotationSet) clusterSetDropdown.getSelectedItem(); 
//        		JTable clusterTable = clustersToTables.get(clusters);
//        		int[] selectedRows = clusterTable.getSelectedRows();
//        		if (selectedRows.length < 2) {
//        			JOptionPane.showMessageDialog(null, "Please select at least two clusters");
//        		} else {
//        			ArrayList<Integer> selectedClusters = new ArrayList<Integer>();
//        			for (int rowNumber : selectedRows) {
//        				selectedClusters.add(rowNumber + 1);
//        			}
//	        		Cluster firstCluster = clusters.clusterSet.get(selectedClusters.get(0)); // +1 because it is zero indexed
//	        		for (int selectedClusterNumber : selectedClusters.subList(1, selectedClusters.size())) {
//	        			Cluster clusterToSwallow = clusters.clusterSet.get(selectedClusterNumber);
//	        			for (int nodeIndex = 0; nodeIndex < clusterToSwallow.getNodes().size(); nodeIndex++) {
//	        				selectedNetwork.getRow(clusterToSwallow.getNodes().get(nodeIndex)).set(clusters.clusterColumnName, firstCluster.clusterNumber);
//	        			}
//	        		}
//	        		clusters.destroyAnnotations();
//        		}
//        	}
//        };
//        mergeButton.addActionListener(mergeActionListener); 
        
        clusterTablePanel.setAlignmentX(LEFT_ALIGNMENT);
        nameColumnDropdown.setAlignmentX(LEFT_ALIGNMENT);
        advancedOptionsPanel.setAlignmentX(LEFT_ALIGNMENT);
        selectionPanel.setAlignmentX(LEFT_ALIGNMENT);
        
        mainPanel.add(networkLabel);
        mainPanel.add(nameColumnDropdownLabel);
        mainPanel.add(nameColumnDropdown);
        mainPanel.add(advancedOptionsPanel);
        mainPanel.add(annotateButton);
        mainPanel.add(clusterTablePanel);
        mainPanel.add(selectionPanel);
        mainPanel.add(clearButton);
        mainPanel.add(updateButton);
//        mainPanel.add(mergeButton);
        
        return mainPanel;
	}

	private BasicCollapsiblePanel createAdvancedOptionsPanel() {
		BasicCollapsiblePanel optionsPanel = new BasicCollapsiblePanel("Advanced Clustering Options");

		JPanel innerPanel = new JPanel(); // To override default layout options of BasicCollapsiblePanel
		innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.PAGE_AXIS));
		
		JPanel clusterOptionPanel = new JPanel();
		clusterOptionPanel.setBorder(BorderFactory.createTitledBorder("Clustering Options"));
		
		// Dropdown with all the available algorithms
		DefaultComboBoxModel clusterDropdownModel = new DefaultComboBoxModel();
		for (String algorithm : algorithmToColumnName.keySet()) {
			clusterDropdownModel.addElement(algorithm);
		}

		// To choose a clusterMaker algorithm
		clusterAlgorithmDropdown = new JComboBox(clusterDropdownModel);
		clusterAlgorithmDropdown.setPreferredSize(new Dimension(110, 30));
        // Alternatively, user can choose a clusterColumn themselves (if they've run clusterMaker themselves)
        clusterColumnDropdown = new JComboBox();
		clusterColumnDropdown.setPreferredSize(new Dimension(110, 30));

        // Only one dropdown visible at a time
        clusterAlgorithmDropdown.setVisible(true);
        clusterColumnDropdown.setVisible(false);
        
        clusterAlgorithmDropdown.setSelectedItem("MCL Cluster");
        
        JPanel dropdownPanel = new JPanel();
		dropdownPanel.add(clusterAlgorithmDropdown);
		dropdownPanel.add(clusterColumnDropdown);
		
        defaultButton = new JRadioButton(defaultButtonString);
        specifyColumnButton = new JRadioButton(specifyColumnButtonString);        
        defaultButton.addItemListener(new ItemListener() {
        	@Override
        	public void itemStateChanged(ItemEvent e) {
        		if (e.getStateChange() == ItemEvent.SELECTED) {
        			clusterAlgorithmDropdown.setVisible(true);
        		} else if (e.getStateChange() == ItemEvent.DESELECTED) {
        			clusterAlgorithmDropdown.setVisible(false);
        		}
        	}
        });
        specifyColumnButton.addItemListener(new ItemListener() {
        	@Override
        	public void itemStateChanged(ItemEvent e) {
        		if (e.getStateChange() == ItemEvent.SELECTED) {
        			clusterColumnDropdown.setVisible(true);
        		} else if (e.getStateChange() == ItemEvent.DESELECTED) {
        			clusterColumnDropdown.setVisible(false);
        		}
        	}
        });
        
        defaultButton.setSelected(true);
        
        // Group buttons together to make them mutually exclusive
        radioButtonGroup = new ButtonGroup();
        radioButtonGroup.add(defaultButton);
        radioButtonGroup.add(specifyColumnButton);
        
        JPanel radioButtonPanel = new JPanel();
		radioButtonPanel.setLayout(new BoxLayout(radioButtonPanel, BoxLayout.PAGE_AXIS));
        radioButtonPanel.add(defaultButton);
        radioButtonPanel.add(specifyColumnButton);
        
        clusterOptionPanel.add(radioButtonPanel);
        clusterOptionPanel.add(dropdownPanel);

        // Specify column with gene set sources (if it exists) and
        // use this to get additional words to use for the labels
        
        sourceColumnDropdown = new JComboBox(); // Gets populated when network is created
        sourceColumnDropdown.setPreferredSize(new Dimension(80, 30));
        sourceColumnDropdown.setEnabled(false);

        innerPanel.add(clusterOptionPanel);
        
        optionsPanel.add(innerPanel);
        optionsPanel.setMaximumSize(new Dimension(350, optionsPanel.getHeight()));
        return optionsPanel;
	}
	
	private BasicCollapsiblePanel createSelectionPanel() {
		BasicCollapsiblePanel selectionPanel = new BasicCollapsiblePanel("Autofocus Preferences");
		
		JPanel innerPanel = new JPanel(); // To override default layout options of BasicCollapsiblePanel
		
		JPanel labelPanel = new JPanel();
		JLabel label = new JLabel("Show on selection:");
		labelPanel.add(label);
		
		JRadioButton heatmapButton = new JRadioButton("Heat Map");
		JRadioButton wordCloudButton = new JRadioButton("WordCloud");
		ButtonGroup buttonGroup = new ButtonGroup();
		buttonGroup.add(heatmapButton);
		buttonGroup.add(wordCloudButton);
		
		heatmapButton.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					showHeatmap = true;
					if (selectedView != null) setDisableHeatMapAutoFocus(false);
				} else if (e.getStateChange() == ItemEvent.DESELECTED) {
					showHeatmap = false;
					if (selectedView != null) setDisableHeatMapAutoFocus(true);
				}
			}
		});
		heatmapButton.setSelected(true);
		
		JPanel radioButtonPanel = new JPanel();
		radioButtonPanel.setLayout(new BoxLayout(radioButtonPanel, BoxLayout.PAGE_AXIS));
		radioButtonPanel.add(heatmapButton);
		radioButtonPanel.add(wordCloudButton);
		
		innerPanel.add(labelPanel);
		innerPanel.add(radioButtonPanel);
		
		selectionPanel.add(innerPanel);
		
		selectionPanel.setMaximumSize(new Dimension(370, selectionPanel.getHeight()));
		
		return selectionPanel;
	}
	
	private JTable createClusterTable(final AnnotationSet clusters) {
		DefaultTableModel model = new DefaultTableModel() {
			private static final long serialVersionUID = -1277709187563893042L;

			Class<?>[] types = {Cluster.class, Integer.class};

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return this.types[columnIndex];
            }
            
			@Override
		    public boolean isCellEditable(int row, int column) {
		        return column == 0 ? false : true;
		    }
		};
		model.addColumn("Cluster");
		model.addColumn("Number of nodes");

		final JTable table = new JTable(model); // Final to be able to use inside of listener
		table.getColumnModel().getColumn(0).setPreferredWidth(220);
		table.getColumnModel().getColumn(1).setPreferredWidth(100);
		
		model.addTableModelListener(new TableModelListener() { // Update the label value
			@Override
			public void tableChanged(TableModelEvent e) {
				if (e.getType() == TableModelEvent.UPDATE || e.getColumn() == 0) {
					int editedRowIndex = e.getFirstRow() == table.getSelectedRow()? e.getLastRow() : e.getFirstRow(); 
					Cluster editedCluster = clusters.clusterSet.get(editedRowIndex + 1);
					editedCluster.setLabel((String) table.getValueAt(editedRowIndex, 0));
					editedCluster.setLabelManuallyUpdated(true);
					editedCluster.erase();
					editedCluster.drawAnnotations();
				}
			}
		});
		for (Cluster cluster : clusters.clusterSet.values()) {
			Object[] rowData = {cluster.getLabel(), cluster.coordinates.size()};
			model.addRow(rowData);
		}
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (! e.getValueIsAdjusting()) { // Down-click and up-click are separate events, this makes only one of them fire
					if (!showHeatmap) {
						setHeatMapNoSort();
					}
					int selectedRowIndex = table.getSelectedRow();
					if (selectedCluster != null || (Cluster) table.getValueAt(selectedRowIndex, 0) == selectedCluster) {
						selectedCluster.deselect();
					}
					selectedCluster = (Cluster) table.getValueAt(selectedRowIndex, 0); 
					selectedCluster.select(showHeatmap);
				}
			}
		});
		table.setAutoCreateRowSorter(true);
		return table;
	}

	public void addClusters(AnnotationSet annotationSet) {
		// If this is the view's first AnnotationSet
		if (!networkViewToClusterSetDropdown.containsKey(annotationSet.view)) {
			addNetworkView(annotationSet.view);
		}
		
		clusterSets.put(annotationSet.name, annotationSet);
		
		// Create scrollable clusterTable
		JTable clusterTable = createClusterTable(annotationSet);
		JScrollPane clusterTableScroll = new JScrollPane(clusterTable);
		clusterTableScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		clusterTablePanel.add(clusterTableScroll, BorderLayout.CENTER);
		
		clustersToTables.put(annotationSet, clusterTable);

		JComboBox clusterSetDropdown = networkViewToClusterSetDropdown.get(annotationSet.view);
		clusterSetDropdown.addItem(annotationSet);
		clusterSetDropdown.setSelectedIndex(clusterSetDropdown.getItemCount()-1);
	}
	
	private void addNetworkView(CyNetworkView view) {
		// Create dropdown with cluster sets of this networkView
		final JComboBox clusterSetDropdown = new JComboBox(); // Final so that the item listener can access it
		clusterSetDropdown.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent itemEvent) {
				if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
					AnnotationSet clusters = (AnnotationSet) itemEvent.getItem();
					clusters.updateCoordinates();
					clusters.updateLabels();
					clusters.drawAnnotations();
					clustersToTables.get(clusters).getParent().getParent().setVisible(true); // Show selected table
					((JPanel) clusterSetDropdown.getParent()).updateUI();
				} else if (itemEvent.getStateChange() == ItemEvent.DESELECTED) {
					AnnotationSet clusters = (AnnotationSet) itemEvent.getItem();
	         		clusters.eraseAnnotations();
					clustersToTables.get(clusters).getParent().getParent().setVisible(false);
					((JPanel) clusterSetDropdown.getParent()).updateUI();
				}
            }
		});
		clusterTablePanel.add(clusterSetDropdown, BorderLayout.PAGE_START);
		networkViewToClusterSetDropdown.put(view, clusterSetDropdown);
		selectedView = view;
		selectedNetwork = view.getModel();
		networkLabel.setText(selectedNetwork.toString());
		clusterTablePanel.updateUI();
	}

	public void updateSelectedView(CyNetworkView view) {
		nameColumnDropdown.removeAllItems();
		clusterColumnDropdown.removeAllItems();
		for (CyColumn column : view.getModel().getDefaultNodeTable().getColumns()) {
			if (column.getType() == String.class || (column.getType() == List.class && column.getListElementType() == String.class)) {
				nameColumnDropdown.addItem(column.getName());
				sourceColumnDropdown.addItem(column.getName());
			} else if (column.getType() == Integer.class || (column.getType() == List.class && column.getListElementType() == Integer.class)) {
				clusterColumnDropdown.addItem(column.getName());
			}
		}
		
		// Try to guess the appropriate columns
		for (int i = 0; i < nameColumnDropdown.getItemCount(); i++) {
			if (nameColumnDropdown.getItemAt(i).getClass() == String.class) {
				if (((String) nameColumnDropdown.getItemAt(i)).contains("GS_DESCR")) {
					nameColumnDropdown.setSelectedIndex(i);
				} else if (((String) sourceColumnDropdown.getItemAt(i)).toLowerCase().contains("source")) {
					sourceColumnDropdown.setSelectedIndex(i);
				}
			}
		}
		
		// Update the label with the network
		networkLabel.setText("  " + view.getModel().toString());
		mainPanel.updateUI();
		
		// Hide previous dropdown
		if (networkViewToClusterSetDropdown.containsKey(selectedView)) {
			networkViewToClusterSetDropdown.get(selectedView).setVisible(false);
			clustersToTables.get(networkViewToClusterSetDropdown.get(selectedView).getSelectedItem()).getParent().getParent().setVisible(false);
		}
		
		selectedView = view;
		selectedNetwork = view.getModel();
		
		// Show current dropdown
		if (networkViewToClusterSetDropdown.containsKey(selectedView)) {
			networkViewToClusterSetDropdown.get(selectedView).setVisible(true);
			clustersToTables.get(networkViewToClusterSetDropdown.get(selectedView).getSelectedItem()).getParent().getParent().setVisible(true);
		}
	}

	public void updateColumnName(CyTable source, String oldColumnName,
			String newColumnName) {
		if (source == selectedNetwork.getDefaultNodeTable()) {
			for (int i = 0; i < nameColumnDropdown.getItemCount(); i++) {
				if (nameColumnDropdown.getModel().getElementAt(i) == oldColumnName) {
					nameColumnDropdown.removeItem(oldColumnName);
					nameColumnDropdown.insertItemAt(newColumnName, i);
					sourceColumnDropdown.removeItem(oldColumnName);
					sourceColumnDropdown.insertItemAt(newColumnName, i);
				}
			}
			for (int i = 0; i < clusterColumnDropdown.getItemCount(); i++) {
				if (clusterColumnDropdown.getModel().getElementAt(i) == oldColumnName) {
					clusterColumnDropdown.removeItem(oldColumnName);
					clusterColumnDropdown.insertItemAt(newColumnName, i);
				}
			}
		}
	}

	public void removeNetworkView(CyNetworkView view) {
		networkLabel.setText("No network selected");
		nameColumnDropdown.removeAllItems();
		sourceColumnDropdown.removeAllItems();
		clusterColumnDropdown.removeAllItems();
		mainPanel.updateUI();
		if (networkViewToClusterSetDropdown.containsKey(view)) {
			JComboBox clusterSetDropdown = networkViewToClusterSetDropdown.get(view);
			Container clusterTable = clustersToTables.get(networkViewToClusterSetDropdown.get(view).getSelectedItem()).getParent().getParent();
			clusterSetDropdown.getParent().remove(clusterSetDropdown);
			clusterTable.getParent().remove(clusterTable);
		}
		selectedView = null;
	}
	
	public void columnDeleted(CyTable source, String columnName) {
		if (source == selectedNetwork.getDefaultNodeTable()) {
			for (int i = 0; i < nameColumnDropdown.getItemCount(); i++) {
				if (nameColumnDropdown.getModel().getElementAt(i) == columnName) {
					nameColumnDropdown.removeItem(columnName);
					sourceColumnDropdown.removeItem(columnName);
				}
			}
			for (int i = 0; i < clusterColumnDropdown.getItemCount(); i++) {
				if (clusterColumnDropdown.getModel().getElementAt(i) == columnName) {
					clusterColumnDropdown.removeItem(columnName);
				}
			}
		}
	}

	public void columnCreated(CyTable source, String columnName) {
		CyTable nodeTable = selectedNetwork.getDefaultNodeTable();
		if (source == nodeTable) {
			CyColumn column = nodeTable.getColumn(columnName);
			if (column.getType() == String.class) {
				if (((DefaultComboBoxModel) nameColumnDropdown.getModel()).getIndexOf(column) == -1) { // doesn't already contain column
					nameColumnDropdown.addItem(column.getName());
					sourceColumnDropdown.addItem(column.getName());
				}
			} else if (column.getType() == Integer.class || (column.getType() == List.class && column.getListElementType() == Integer.class)) {
				if (((DefaultComboBoxModel) clusterColumnDropdown.getModel()).getIndexOf(column) == -1) { // doesn't already contain column
					clusterColumnDropdown.addItem(column.getName());					
				}
			}
		}
	}
	
	public void setHeatMapNoSort() {
		emManager.getMap(selectedNetwork.getSUID()).getParams().getHmParams().setSort(HeatMapParameters.Sort.NONE);
	}
	
	public void setDisableHeatMapAutoFocus(boolean b) {
		emManager.getMap(selectedNetwork.getSUID()).getParams().setDisableHeatmapAutofocus(b);
	}
	
	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public CytoPanelName getCytoPanelName() {
		return CytoPanelName.WEST;
	}

	@Override
	public Icon getIcon() {
		//create an icon for the enrichment map panels
        URL EMIconURL = this.getClass().getResource("enrichmentmap_logo_notext_small.png");
        ImageIcon EMIcon = null;
        if (EMIconURL != null) {
            EMIcon = new ImageIcon(EMIconURL);
        }
		return EMIcon;
	}

	@Override
	public String getTitle() {
		return "Annotation Panel";
	}

}