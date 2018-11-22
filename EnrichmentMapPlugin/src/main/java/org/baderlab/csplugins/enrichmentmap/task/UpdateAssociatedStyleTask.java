package org.baderlab.csplugins.enrichmentmap.task;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.baderlab.csplugins.enrichmentmap.model.AbstractDataSet;
import org.baderlab.csplugins.enrichmentmap.model.EMDataSet;
import org.baderlab.csplugins.enrichmentmap.model.EnrichmentMap;
import org.baderlab.csplugins.enrichmentmap.model.ExpressionData;
import org.baderlab.csplugins.enrichmentmap.model.GeneExpressionMatrix;
import org.baderlab.csplugins.enrichmentmap.model.GeneSet;
import org.baderlab.csplugins.enrichmentmap.model.SetOfGeneSets;
import org.baderlab.csplugins.enrichmentmap.style.AbstractColumnDescriptor;
import org.baderlab.csplugins.enrichmentmap.style.AssociatedStyleBuilder;
import org.baderlab.csplugins.enrichmentmap.style.AssociatedStyleOptions;
import org.baderlab.csplugins.enrichmentmap.style.ChartData;
import org.baderlab.csplugins.enrichmentmap.style.ChartFactoryManager;
import org.baderlab.csplugins.enrichmentmap.style.ChartOptions;
import org.baderlab.csplugins.enrichmentmap.style.ChartType;
import org.baderlab.csplugins.enrichmentmap.style.ColorScheme;
import org.baderlab.csplugins.enrichmentmap.style.EMStyleBuilder.Columns;
import org.baderlab.csplugins.enrichmentmap.style.charts.AbstractChart;
import org.baderlab.csplugins.enrichmentmap.util.NetworkUtil;
import org.baderlab.csplugins.enrichmentmap.view.heatmap.table.DataSetColorRange;
import org.baderlab.csplugins.enrichmentmap.view.util.ChartUtil;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.customgraphics.CyCustomGraphics2;
import org.cytoscape.view.presentation.customgraphics.CyCustomGraphics2Factory;
import org.cytoscape.view.presentation.property.values.CyColumnIdentifier;
import org.cytoscape.view.presentation.property.values.CyColumnIdentifierFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;

public class UpdateAssociatedStyleTask extends AbstractTask {

	@Inject private VisualMappingManager visualMappingManager;
	@Inject private ChartFactoryManager chartFactoryManager;
	@Inject private CyColumnIdentifierFactory columnIdFactory;
	@Inject private Provider<AssociatedStyleBuilder> styleBuilderProvider;

	private final AssociatedStyleOptions options;
	
	private static final Logger logger = LoggerFactory.getLogger(UpdateAssociatedStyleTask.class);

	public interface Factory {
		UpdateAssociatedStyleTask create(AssociatedStyleOptions options);
	}

	@Inject
	public UpdateAssociatedStyleTask(@Assisted AssociatedStyleOptions options) {
		this.options = options;
	}

	@Override
	public void run(TaskMonitor tm) throws Exception {
		tm.setTitle("Update " + options.getAssociatedApp() + " Style");
		tm.setStatusMessage("Updating Data Columns...");
		tm.setProgress(0.0);
		
		ChartOptions chartOptions = options.getChartOptions();
		
		if (chartOptions != null && chartOptions.getData() != null) {
			switch (chartOptions.getData()) {
				case EXPRESSION_DATA:
					updateExpressionDataColumn();
					break;
				case DATA_SET:
					createDataSetColumn();
					break;
				default:
					break;
			}
		}
		
		tm.setStatusMessage("Updating Style...");
		tm.setProgress(0.6);
		
		updateVisualStyle();
	}

	private void updateExpressionDataColumn() {
		CyNetwork network = options.getNetworkView().getModel();
		CyTable nodeTable = network.getDefaultNodeTable();
		
		if (!Columns.EXPRESSION_DATA_CHART.hasColumn(nodeTable)) {
			try {
				Columns.EXPRESSION_DATA_CHART.createColumn(nodeTable);
			} catch (Exception e) {
				logger.error("Cannot create column " + Columns.EXPRESSION_DATA_CHART.getBaseName(), e);
			}
		}
		
		Map<Long, double[]> columnData = new HashMap<>();
		EnrichmentMap map = options.getEnrichmentMap();
		ExpressionData exp = options.getExpressionData();
		
		int n = exp.getSize();

		for (CyNode node : network.getNodeList()) {
			double[] data = new double[n];
			columnData.put(node.getSUID(), data);
			
			String name = NetworkUtil.getGeneName(network, node);
			
			if (name == null)
				continue;
			
			String queryTerm = NetworkUtil.getQueryTerm(network, name);
			Integer id = map.getHashFromGene(queryTerm != null ? queryTerm : name);
			
			if (id == null)
				continue;
			
			for (int i = 0; i < n; i++) {
				double value = exp.getValue(id, i, options.getCompress());
				data[i] = value;
			}
		}

		columnData.forEach((suid, data) -> {
			Columns.EXPRESSION_DATA_CHART.set(nodeTable.getRow(suid), Doubles.asList(data));
		});
	}

	private void createDataSetColumn() {
		CyNetwork network = options.getNetworkView().getModel();
		CyTable nodeTable = network.getDefaultNodeTable();
		
		if (!Columns.DATASET_CHART.hasColumn(nodeTable)) {
			try {
				Columns.DATASET_CHART.createColumn(nodeTable);
			} catch (Exception e) {
				logger.error("Cannot create column " + Columns.DATASET_CHART.getBaseName(), e);
			}

			Map<Long, int[]> columnData = new HashMap<>();
			EnrichmentMap map = options.getEnrichmentMap();
			List<EMDataSet> dataSets = map.getDataSetList();
			int n = dataSets.size();

			for (CyNode node : network.getNodeList()) {
				int[] data = new int[n];
				columnData.put(node.getSUID(), data);
				
				String name = NetworkUtil.getGeneName(network, node);
				
				if (name == null)
					continue;
				
				String queryTerm = NetworkUtil.getQueryTerm(network, name);
				Integer id = map.getHashFromGene(queryTerm != null ? queryTerm : name);
				
				if (id == null)
					continue;
				
				for (int i = 0; i < n; i++) {
					EMDataSet ds = dataSets.get(i);
					SetOfGeneSets geneSetsOfInterest = ds.getGeneSetsOfInterest();
					Collection<GeneSet> geneSets = geneSetsOfInterest.getGeneSets().values();
					
					for (GeneSet gs : geneSets) {
						if (gs.getGenes().contains(id)) {
							data[i] = 1;
							break;
						}
					}
				}
			}

			columnData.forEach((suid, data) -> {
				Columns.DATASET_CHART.set(nodeTable.getRow(suid), Ints.asList(data));
			});
		}
	}
	
	private void updateVisualStyle() {
		CyNetworkView view = options.getNetworkView();
		VisualStyle vs = visualMappingManager.getVisualStyle(view);
		CyCustomGraphics2<?> chart = createChart();
		styleBuilderProvider.get().updateProperties(vs, options, chart);
	}
	
	private CyCustomGraphics2<?> createChart() {
		CyCustomGraphics2<?> chart = null;
		ChartOptions chartOptions = options.getChartOptions();
		ChartData data = chartOptions != null ? chartOptions.getData() : null;
		
		if (data != null && data != ChartData.NONE) {
			ChartType type = chartOptions.getType();
			List<AbstractDataSet> dataSets = options.getDataSets(); // Ignore Signature Data Sets in charts
			
			if (type != null && !dataSets.isEmpty()) {
				Map<String, Object> props = new HashMap<>(type.getProperties());
				AbstractColumnDescriptor columnDescriptor = data.getColumnDescriptor();
				
				if (data == ChartData.DATA_SET) {
					List<CyColumnIdentifier> columns = Arrays.asList(columnIdFactory.createColumnIdentifier(columnDescriptor.getBaseName()));
					List<Color> colors = options.getEnrichmentMap().getDataSetColors();
					
					props.put("cy_dataColumns", columns);
					props.put("cy_colors", colors);
					props.put("cy_showItemLabels", chartOptions.isShowLabels());
				} else if (data == ChartData.EXPRESSION_DATA) {
					List<CyColumnIdentifier> columns = Arrays.asList(columnIdFactory.createColumnIdentifier(columnDescriptor.getBaseName()));
					List<Double> range = null;
					List<Color> colors = null;
					
					AbstractDataSet ds = dataSets.get(0);
					
					if (ds instanceof EMDataSet) {
						GeneExpressionMatrix matrix = ((EMDataSet) ds).getExpressionSets();
						Optional<DataSetColorRange> dsColorRange = DataSetColorRange.create(matrix, options.getTransform());
						
						if (dsColorRange.isPresent()) {
							Color c1 = dsColorRange.get().getTheme().getMinColor();
							Color c2 = dsColorRange.get().getTheme().getCenterColor();
							Color c3 = dsColorRange.get().getTheme().getMaxColor();
							colors = Arrays.asList(new Color[] { c1, c2, c3 });
						}
					}
					
					range = ChartUtil.calculateGlobalRange(options.getNetworkView().getModel(), columns);
					System.out.println(range.get(0)+","+range.get(1));
					
					if (colors == null || colors.isEmpty())
						colors = new ArrayList<>(ColorScheme.RD_YL_BU_3.getColors());
					
					if (range.size() > 0 && range.get(0) < 0) // Does it have negative numbers?
						Collections.reverse(colors);
					
					props.put("cy_dataColumns", columns);
					props.put("cy_range", range);
					props.put("cy_autoRange", false);
					props.put("cy_globalRange", true);
					props.put("cy_showRangeZeroBaseline", true);
					props.put("cy_showItemLabels", chartOptions.isShowLabels());
					props.put("cy_colors", colors);
					
					ColorScheme colorScheme = chartOptions != null ? chartOptions.getColorScheme() : null;
					
					if (colorScheme != null && colorScheme.getPoints() != null) {
						List<Double> points = colorScheme.getPoints();
						
						if (!points.isEmpty())
							props.put(AbstractChart.COLOR_POINTS, points);
					}
				}
				
				try {
					CyCustomGraphics2Factory<?> factory = chartFactoryManager.getChartFactory(type.getId());
					
					if (factory != null)
						chart = factory.getInstance(props);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		return chart;
	}
}