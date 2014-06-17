package org.baderlab.csplugins.enrichmentmap.view;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.baderlab.csplugins.enrichmentmap.task.AutoAnnotatorTaskFactory;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.util.swing.OpenBrowser;
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
	private String clusterColumnName;
	private long networkID;
	protected AutoAnnotatorTaskFactory autoAnnotatorTaskFactory;

	public AutoAnnotatorPanel(CyApplicationManager cyApplicationManagerRef, 
			CyNetworkViewManager cyNetworkViewManagerRef, CySwingApplication cySwingApplicationRef,
			OpenBrowser openBrowserRef, CyNetworkManager cyNetworkManagerRef, AnnotationManager annotationManager,
			CyServiceRegistrar registrar, DialogTaskManager dialogTaskManager){
		
		JPanel mainPanel = createMainPanel(cyApplicationManagerRef, cyNetworkViewManagerRef, cySwingApplicationRef,
				openBrowserRef, cyNetworkManagerRef, annotationManager, registrar, dialogTaskManager);
        add(mainPanel,BorderLayout.CENTER);
	}
	
	private JPanel createMainPanel(final CyApplicationManager cyApplicationManagerRef,
			final CyNetworkViewManager cyNetworkViewManagerRef, final CySwingApplication cySwingApplicationRef,
			final OpenBrowser openBrowserRef, final CyNetworkManager cyNetworkManagerRef, final AnnotationManager annotationManager,
			final CyServiceRegistrar registrar, final DialogTaskManager dialogTaskManager) {
		
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
		
		
        // Give the user a choice of networks to annotate
        JComboBox networkDropdown = new JComboBox();
        final HashMap<String, Long> nameToSUID = new HashMap<String, Long>();
        for (CyNetwork network : cyNetworkManagerRef.getNetworkSet()) {
        	String name = network.toString();
        	long suid = network.getSUID();
        	networkDropdown.addItem(name);
        	nameToSUID.put(name, suid);
        }
        
        // Give the user a choice of column with cluster numbers
        final JComboBox clusterColumnDropdown = new JComboBox();
        networkDropdown.addItemListener(new ItemListener(){
        	public void itemStateChanged(ItemEvent itemEvent) {
                networkID = nameToSUID.get((String) itemEvent.getItem());
                CyNetwork network = cyNetworkManagerRef.getNetwork(networkID);
                clusterColumnDropdown.removeAllItems();
                for (CyColumn column : network.getDefaultNodeTable().getColumns()) {
                	clusterColumnDropdown.addItem(column.getName());
                }
            }    	
        });
        
        clusterColumnDropdown.addItemListener(new ItemListener(){
        	public void itemStateChanged(ItemEvent itemEvent) {
                clusterColumnName = (String) itemEvent.getItem();
            }    	
        }); 
        
        JButton confirmButton = new JButton("Annotate!");
        
        final Map<String, String> serviceProperties = new HashMap<String, String>();
        serviceProperties.put("inMenuBar", "true");
		serviceProperties.put("preferredMenu", "Apps.EnrichmentMap");
        ActionListener autoAnnotateAction = new ActionListener(){
			public void actionPerformed(ActionEvent arg0) {
				// networkID and clusterColumnName field are looked up only when the button is pressed
				autoAnnotatorTaskFactory = new AutoAnnotatorTaskFactory(cySwingApplicationRef, openBrowserRef,
						cyNetworkViewManagerRef, cyNetworkManagerRef, annotationManager,
        				networkID, clusterColumnName, registrar);
				dialogTaskManager.execute(autoAnnotatorTaskFactory.createTaskIterator());
			}
        };
        confirmButton.addActionListener(autoAnnotateAction);
        
        JLabel networkDropdownLabel = new JLabel("Select the network to annotate:");
        JLabel clusterColumnDropdownLabel = new JLabel("Select the column with the clusters:"); // ambiguous phrasing?
        
        networkDropdownLabel.setAlignmentX(LEFT_ALIGNMENT);
        clusterColumnDropdownLabel.setAlignmentX(LEFT_ALIGNMENT);
        networkDropdown.setAlignmentX(LEFT_ALIGNMENT);
        clusterColumnDropdown.setAlignmentX(LEFT_ALIGNMENT);
        confirmButton.setAlignmentX(LEFT_ALIGNMENT);
        
        mainPanel.add(networkDropdownLabel);
        mainPanel.add(networkDropdown);
        mainPanel.add(clusterColumnDropdownLabel);
        mainPanel.add(clusterColumnDropdown);
        mainPanel.add(confirmButton);
        
        
        
        return mainPanel;
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
		return null;
	}

	@Override
	public String getTitle() {
		return "Annotation Panel";
	}
	
}