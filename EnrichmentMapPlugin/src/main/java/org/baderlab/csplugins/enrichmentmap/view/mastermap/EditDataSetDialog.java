package org.baderlab.csplugins.enrichmentmap.view.mastermap;

import static com.google.common.base.Strings.isNullOrEmpty;
import static javax.swing.GroupLayout.DEFAULT_SIZE;
import static javax.swing.GroupLayout.PREFERRED_SIZE;
import static org.baderlab.csplugins.enrichmentmap.view.util.SwingUtil.makeSmall;
import static org.baderlab.csplugins.enrichmentmap.view.util.SwingUtil.simpleDocumentListener;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.baderlab.csplugins.enrichmentmap.model.DataSet.Method;
import org.baderlab.csplugins.enrichmentmap.model.DataSetFiles;
import org.baderlab.csplugins.enrichmentmap.parsers.DatasetLineParser;
import org.baderlab.csplugins.enrichmentmap.view.util.FileBrowser;
import org.cytoscape.util.swing.BasicCollapsiblePanel;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.util.swing.LookAndFeelUtil;

import com.google.common.base.Strings;

/**
 * MKTODO set the phenotypes automatically by scanning the classes file
 * MKTODO add validation
 */
@SuppressWarnings("serial")
public class EditDataSetDialog extends JDialog {
	
	private final FileUtil fileUtil;
	
	private JRadioButton gseaRadio;
	private JRadioButton genericRadio;
	private JRadioButton davidRadio;
	
	private JTextField enrichmentsText;
	private JTextField expressionsText;
	private JTextField ranksText;
	private JTextField classesText;
	private JTextField positiveText;
	private JTextField negativeText;
	
	private JButton okButton;
	
	private String[] classes;
	
	boolean okClicked = false;
	

	public EditDataSetDialog(JDialog parent, FileUtil fileUtil, @Nullable DataSetParameters dataSet) {
		super(parent, null, true);
		this.fileUtil = fileUtil;
		setMinimumSize(new Dimension(650, 400));
		setResizable(false);
		setTitle(dataSet == null ? "Add Enrichment Results" : "Edit Enrichment Results");
		createContents(dataSet);
		pack();
		setLocationRelativeTo(parent);
		validateInput();
	}
	
	
	public DataSetParameters open() {
		setVisible(true); // must be modal for this to work
		if(!okClicked)
			return null;
		return createDataSetParameters();
	}
	
	private DataSetParameters createDataSetParameters() {
		String name = "Temp for now";
		Method method = getMethod();
		
		DataSetFiles files = new DataSetFiles();
		
		// MKTODO add field for enrichments file 2
		String enrichmentFileName1 = enrichmentsText.getText();
		if(!isNullOrEmpty(enrichmentFileName1))
			files.setEnrichmentFileName1(enrichmentFileName1);
		
		String expressionFileName = expressionsText.getText();
		if(!isNullOrEmpty(expressionFileName))
			files.setExpressionFileName(expressionFileName);
		
		String ranksFileName = ranksText.getText();
		if(!isNullOrEmpty(ranksFileName))
			files.setRankedFile(ranksFileName);
		
//		// MKTODO can auto fill the positive and negative phenotypes from the class file
//		String classesFileName = classesText.getText();
//		if(!isNullOrEmpty(classesFileName))
//			files.setClassFile(classesFileName);
		
		String positive = positiveText.getText();
		String negative = negativeText.getText();
		if(!isNullOrEmpty(positive) && !isNullOrEmpty(negative) && classes != null) {
			files.setPhenotype1(positive);
			files.setPhenotype2(negative);
			files.setTemp_class1(classes);
		}
		
		return new DataSetParameters(name, method, files);
	}
	
	
	private void createContents(@Nullable DataSetParameters initDataSet) {
		JPanel analysisTypePanel = createAnalysisTypePanel(initDataSet);
		JPanel textFieldPanel = createTextFieldPanel(initDataSet);
		JPanel phenotypePanel = createPhenotypesPanel(initDataSet);
		JPanel buttonPanel = createButtonPanel();
		
		Container contentPane = this.getContentPane();
		GroupLayout layout = new GroupLayout(contentPane);
		contentPane.setLayout(layout);
		
		layout.setHorizontalGroup(
			layout.createParallelGroup()
				.addComponent(analysisTypePanel)
				.addComponent(textFieldPanel)
				.addComponent(phenotypePanel)
				.addComponent(buttonPanel)
		);
		layout.setVerticalGroup(
			layout.createSequentialGroup()
				.addComponent(analysisTypePanel)
				.addComponent(textFieldPanel)
				.addComponent(phenotypePanel)
				.addGap(0, 0, Short.MAX_VALUE)
				.addComponent(buttonPanel)
		);
	}
	

	private JPanel createAnalysisTypePanel(@Nullable DataSetParameters dataSet) {
		gseaRadio    = new JRadioButton("GSEA", true);
		genericRadio = new JRadioButton("generic/gProfiler");
		davidRadio   = new JRadioButton("DAVID/BiNGO/Great");

		makeSmall(gseaRadio, genericRadio, davidRadio);

		ButtonGroup analysisOptions = new ButtonGroup();
		analysisOptions.add(gseaRadio);
		analysisOptions.add(genericRadio);
		analysisOptions.add(davidRadio);
		
		if(dataSet != null) {
			gseaRadio.setSelected(dataSet.getMethod() == Method.GSEA);
			genericRadio.setSelected(dataSet.getMethod() == Method.Generic);
			davidRadio.setSelected(dataSet.getMethod() == Method.Specialized);
		}

		JPanel panel = new JPanel();
		GroupLayout layout = new GroupLayout(panel);
		panel.setLayout(layout);
		layout.setAutoCreateContainerGaps(true);
		layout.setAutoCreateGaps(true);
	   		
   		layout.setHorizontalGroup(layout.createSequentialGroup()
			.addComponent(gseaRadio)
			.addComponent(genericRadio)
			.addComponent(davidRadio)
			.addGap(0, 0, Short.MAX_VALUE)
   		);
   		layout.setVerticalGroup(layout.createParallelGroup(Alignment.CENTER, true)
			.addComponent(gseaRadio,    PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
			.addComponent(genericRadio, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
			.addComponent(davidRadio,   PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
   		);
   		
   		panel.setBorder(LookAndFeelUtil.createTitledBorder("Analysis Type"));
   		if (LookAndFeelUtil.isAquaLAF())
			panel.setOpaque(false);
		return panel;
	}
	

	private JPanel createTextFieldPanel(@Nullable DataSetParameters dataSet) {
		JLabel enrichmentsLabel = new JLabel("Enrichments:");
		enrichmentsText = new JTextField();
		JButton enrichmentsBrowse = new JButton("Browse...");
		enrichmentsText.getDocument().addDocumentListener(simpleDocumentListener(this::validateInput));
		enrichmentsBrowse.addActionListener(e -> browse(enrichmentsText, FileBrowser.Filter.ENRICHMENT));
		
		JLabel expressionsLabel = new JLabel("Expressions:");
		expressionsText = new JTextField();
		JButton expressionsBrowse = new JButton("Browse...");
		expressionsText.getDocument().addDocumentListener(simpleDocumentListener(this::validateInput));
		expressionsBrowse.addActionListener(e -> browse(expressionsText, FileBrowser.Filter.EXPRESSION));
		
		makeSmall(enrichmentsLabel, enrichmentsText, enrichmentsBrowse);
		makeSmall(expressionsLabel, expressionsText, expressionsBrowse);
		
		JPanel panel = new JPanel();
		GroupLayout layout = new GroupLayout(panel);
		panel.setLayout(layout);
		layout.setAutoCreateContainerGaps(true);
		layout.setAutoCreateGaps(true);

		layout.setHorizontalGroup(
			layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup(Alignment.TRAILING)
					.addComponent(enrichmentsLabel)
					.addComponent(expressionsLabel)
				)
				.addGroup(layout.createParallelGroup(Alignment.LEADING, true)
					.addComponent(enrichmentsText, 0, DEFAULT_SIZE, Short.MAX_VALUE)
					.addComponent(expressionsText, 0, DEFAULT_SIZE, Short.MAX_VALUE)
				)
				.addGroup(layout.createParallelGroup()
					.addComponent(enrichmentsBrowse)
					.addComponent(expressionsBrowse)
				)
		);
		
		layout.setVerticalGroup(
			layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup(Alignment.BASELINE)
					.addComponent(enrichmentsLabel)
					.addComponent(enrichmentsText)
					.addComponent(enrichmentsBrowse)
				)
				.addGroup(layout.createParallelGroup(Alignment.BASELINE)
					.addComponent(expressionsLabel)
					.addComponent(expressionsText)
					.addComponent(expressionsBrowse)
				)
		);
		
		panel.setBorder(LookAndFeelUtil.createTitledBorder("Data Files"));
   		if (LookAndFeelUtil.isAquaLAF())
			panel.setOpaque(false);
		return panel;
	}
	
	
	private JPanel createPhenotypesPanel(@Nullable DataSetParameters dataSet) {
		JLabel ranksLabel = new JLabel("Ranks:");
		ranksText = new JTextField();
		JButton ranksBrowse = new JButton("Browse...");
		ranksText.getDocument().addDocumentListener(simpleDocumentListener(this::validateInput));
		ranksBrowse.addActionListener(e -> browse(ranksText, FileBrowser.Filter.RANK));
		
		JLabel classesLabel = new JLabel("Classes:");
		classesText = new JTextField();
		JButton classesBrowse = new JButton("Browse...");
		classesText.getDocument().addDocumentListener(simpleDocumentListener(this::updateClasses));
		classesBrowse.addActionListener(e -> browse(classesText, FileBrowser.Filter.CLASS));
		
		makeSmall(ranksLabel, ranksText, ranksBrowse);
		makeSmall(classesLabel, classesText, classesBrowse);
		
		JLabel positive = new JLabel("Positive:");
		JLabel negative = new JLabel("Negative:");
		positiveText = new JTextField();
		negativeText = new JTextField();
		
		makeSmall(positive, negative, positiveText, negativeText);
		
		BasicCollapsiblePanel panel = new BasicCollapsiblePanel("Advanced");
		GroupLayout layout = new GroupLayout(panel.getContentPane());
		panel.getContentPane().setLayout(layout);
		layout.setAutoCreateContainerGaps(true);
		layout.setAutoCreateGaps(true);
		

		layout.setHorizontalGroup(
			layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup(Alignment.TRAILING)
					.addComponent(ranksLabel)
					.addComponent(classesLabel)
				)
				.addGroup(layout.createParallelGroup(Alignment.LEADING, true)
					.addComponent(ranksText,   0, DEFAULT_SIZE, Short.MAX_VALUE)
					.addComponent(classesText, 0, DEFAULT_SIZE, Short.MAX_VALUE)
					.addGroup(layout.createSequentialGroup()
						.addComponent(positive)
						.addComponent(positiveText, 100, 100, 100)
						.addGap(20)
						.addComponent(negative)
						.addComponent(negativeText, 100, 100, 100)
					)
				)
				.addGroup(layout.createParallelGroup()
					.addComponent(ranksBrowse)
					.addComponent(classesBrowse)
				)
		);
		
		layout.setVerticalGroup(
			layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup(Alignment.BASELINE)
					.addComponent(ranksLabel)
					.addComponent(ranksText)
					.addComponent(ranksBrowse)
				)
				.addGroup(layout.createParallelGroup(Alignment.BASELINE)
					.addComponent(classesLabel)
					.addComponent(classesText)
					.addComponent(classesBrowse)
				)
				.addGroup(layout.createParallelGroup(Alignment.BASELINE)
					.addComponent(positive)
					.addComponent(positiveText, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
					.addComponent(negative)
					.addComponent(negativeText, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
				)
		);
			
   		if (LookAndFeelUtil.isAquaLAF())
			panel.setOpaque(false);
		return panel;
	}
	
	
	private JPanel createButtonPanel() {
		okButton = new JButton(new AbstractAction("OK") {
			public void actionPerformed(ActionEvent e) {
				okClicked = true;
				dispose();
			}
		});
		JButton cancelButton = new JButton(new AbstractAction("Cancel") {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});

		JPanel buttonPanel = LookAndFeelUtil.createOkCancelPanel(okButton, cancelButton);
		LookAndFeelUtil.setDefaultOkCancelKeyStrokes(getRootPane(), okButton.getAction(), cancelButton.getAction());
		getRootPane().setDefaultButton(okButton);
		return buttonPanel;
	}
	
	
	private void validateInput() {
		System.out.println("EditDataSetDialog.validateInput()");
		boolean valid = true;
		valid &= validatePathTextField(enrichmentsText);
		valid &= validatePathTextField(expressionsText);
		valid &= validatePathTextField(ranksText);
		valid &= validatePathTextField(classesText);
		okButton.setEnabled(valid);
	}
	
	private static boolean validatePathTextField(JTextField textField) {
		boolean valid;
		try {
			String text = textField.getText();
			if(Strings.isNullOrEmpty(text.trim())) {
				valid = true;
			} else { 
				valid = Files.isReadable(Paths.get(text));
			}
		} catch(InvalidPathException e) {
			valid = false;
		}
		textField.setForeground(valid ? Color.BLACK : Color.RED); // MKTODO don't hardcode Color.BLACK
		return valid;
	}
	
	
	private void updateClasses() {
		if(positiveText.getText().trim().isEmpty() && negativeText.getText().trim().isEmpty() && validatePathTextField(classesText)) {
			String classFile = classesText.getText();
			List<String> phenotypes = parseClasses(classFile);
			if(phenotypes != null) {
				LinkedHashSet<String> distinctOrdererd = new LinkedHashSet<>(phenotypes);
				if(distinctOrdererd.size() >= 2) {
					Iterator<String> iter = distinctOrdererd.iterator();
					positiveText.setText(iter.next());
					negativeText.setText(iter.next());
				}
			}
		}
	}
	
	private static List<String> parseClasses(String classFile) {
		if (isNullOrEmpty(classFile))
			return Arrays.asList("NA_pos", "NA_neg");

		File f = new File(classFile);
		if(!f.exists())
			return null;

		try {
			List<String> lines = DatasetLineParser.readLines(classFile, 4);

			/*
			 * GSEA class files will have 3 lines in the following format: 6 2 1
			 * # R9C_8W WT_8W R9C_8W R9C_8W R9C_8W WT_8W WT_8W WT_8W
			 * 
			 * If the file has 3 lines assume it is a GSEA and get the
			 * phenotypes from the third line. If the file only has 1 line
			 * assume that it is a generic class file and get the phenotypes
			 * from the single line
			 * the class file can be split by a space or a tab
			 */
			if(lines.size() >= 3)
				return Arrays.asList(lines.get(2).split("\\s"));
			else if(lines.size() == 1)
				return Arrays.asList(lines.get(0).split("\\s"));
			else
				return null;
			
		} catch (IOException ie) {
			System.err.println("unable to open class file: " + classFile);
			return null;
		}
	}
	
	
	private void browse(JTextField textField, FileBrowser.Filter filter) {
		Optional<Path> path = FileBrowser.browse(fileUtil, this, filter);
		path.map(Path::toString).ifPresent(textField::setText);
		validateInput();
	}
	
	
	private Method getMethod() {
		if(gseaRadio.isSelected())
			return Method.GSEA;
		else if(genericRadio.isSelected())
			return Method.Generic;
		else
			return Method.Specialized;
	}
}
