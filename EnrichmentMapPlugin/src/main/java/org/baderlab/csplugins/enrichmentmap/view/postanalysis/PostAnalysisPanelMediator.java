package org.baderlab.csplugins.enrichmentmap.view.postanalysis;

import static org.baderlab.csplugins.enrichmentmap.view.util.SwingUtil.invokeOnEDT;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import org.baderlab.csplugins.enrichmentmap.EnrichmentMapBuildProperties;
import org.baderlab.csplugins.enrichmentmap.model.EnrichmentMap;
import org.baderlab.csplugins.enrichmentmap.model.EnrichmentMapManager;
import org.baderlab.csplugins.enrichmentmap.view.util.SwingUtil;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.util.swing.LookAndFeelUtil;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class PostAnalysisPanelMediator {

	@Inject private EnrichmentMapManager emManager;
	@Inject private PostAnalysisInputPanel.Factory panelFactory;
	
	@Inject private CyServiceRegistrar serviceRegistrar;
	@Inject private CySwingApplication swingApplication;
	
	@SuppressWarnings("serial")
	public void showDialog(Component parent, EnrichmentMap map) {
		invokeOnEDT(() -> {
			final PostAnalysisInputPanel panel = panelFactory.create(map);
			
			final JDialog dialog = new JDialog(swingApplication.getJFrame(), "Add Signature Gene Sets",
					ModalityType.APPLICATION_MODAL);
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			
			JButton helpButton = SwingUtil.createOnlineHelpButton(EnrichmentMapBuildProperties.USER_MANUAL_URL,
					"Online Manual...", serviceRegistrar);

			JButton resetButton = new JButton("Reset");
			resetButton.addActionListener(e -> panel.reset());

			JButton closeButton = new JButton(new AbstractAction("Close") {
				@Override
				public void actionPerformed(ActionEvent e) {
					dialog.dispose();
				}
			});
			JButton runButton = new JButton(new AbstractAction("Run") {
				@Override
				public void actionPerformed(ActionEvent e) {
					panel.run();
				}
			});

			JPanel buttonPanel =  LookAndFeelUtil.createOkCancelPanel(runButton, closeButton, helpButton, resetButton);
			
			dialog.getContentPane().add(panel, BorderLayout.CENTER);
			dialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
			
			LookAndFeelUtil.setDefaultOkCancelKeyStrokes(dialog.getRootPane(), runButton.getAction(),
					closeButton.getAction());
			dialog.getRootPane().setDefaultButton(runButton);
			
			dialog.pack();
			dialog.setLocationRelativeTo(parent);
			dialog.setVisible(true);
		});
	}
}
