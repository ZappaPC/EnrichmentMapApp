package org.baderlab.csplugins.enrichmentmap.view.heatmap.table;

import java.awt.Color;
import java.awt.Font;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;

import org.apache.commons.lang3.tuple.Pair;
import org.baderlab.csplugins.enrichmentmap.model.EMDataSet;
import org.baderlab.csplugins.enrichmentmap.model.Transform;
import org.baderlab.csplugins.org.mskcc.colorgradient.ColorGradientRange;
import org.baderlab.csplugins.org.mskcc.colorgradient.ColorGradientTheme;

public class HeatMapCellRenderer implements TableCellRenderer {

	private final Map<Pair<EMDataSet,Transform>,Optional<DataSetColorRange>> colorRanges = new HashMap<>();
	private final static DecimalFormat format = new DecimalFormat("###.##");
	private boolean showValue;
	
	
	@Override
	public JLabel getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
		JLabel label = new JLabel();
		label.setOpaque(true); //MUST do this for background to show up.
		
		if(value instanceof Number) {
			double d = ((Number)value).doubleValue();
			
			HeatMapTableModel model = (HeatMapTableModel) table.getModel();
			Color color = getColorFor(model, col, d);
			label.setBackground(color);
			Border border = BorderFactory.createMatteBorder(1, 1, 1, 1, isSelected ? table.getSelectionForeground() : color);
			label.setBorder(border);
			
			String text = getText(d);
			label.setToolTipText(text);
			
			if(showValue && Double.isFinite(d)) {
				label.setText(text);
				label.setFont(new Font((UIManager.getFont("TableHeader.font")).getName(), Font.PLAIN, (UIManager.getFont("TableHeader.font")).getSize()-2));
	      	   	label.setHorizontalAlignment(SwingConstants.RIGHT);
			}
		}
		
		return label;
	}
	
	
	public void setShowValues(boolean showValue) {
		this.showValue = showValue;
	}
	
	public boolean getShowValues() {
		return showValue;
	}
	
	
	public static String getText(double d) {
		return Double.isFinite(d) ? format.format(d) : "NaN";
	}
	
	public static DecimalFormat getFormat() {
		return format;
	}

	private static Color getColor(HeatMapTableModel model, int col, double d, BiFunction<EMDataSet,Transform,DataSetColorRange> getColorRange) {
		EMDataSet dataset = model.getDataSet(col);
		Transform transform = model.getTransform();
		DataSetColorRange range = getColorRange.apply(dataset, transform);
		if(range != null) {
			Color color = getColor(d, range);
			return color;
		} else {
			return Color.GRAY;
		}
	}
	
	public static Color getColor(HeatMapTableModel model, int col, double d) {
		return getColor(model, col, d, (dataset,transform) -> DataSetColorRange.create(dataset.getExpressionSets(), transform).orElse(null));
	}
	
	private Color getColorFor(HeatMapTableModel model, int col, double d) {
		return getColor(model, col, d, this::getRange);
	}
	
	public DataSetColorRange getRange(EMDataSet dataset, Transform transform) {
		// creating the color range for Transform.ROW_NORMALIZED consumes memory, so cache the value
		return colorRanges.computeIfAbsent(Pair.of(dataset, transform), 
			pair -> DataSetColorRange.create(pair.getLeft().getExpressionSets(), pair.getRight())
		).orElse(null);
	}
	
	public static Color getColor(Double measurement, DataSetColorRange range) {
		return getColor(measurement, range.getTheme(), range.getRange());
	}
	
	public static Color getColor(Double measurement, ColorGradientTheme theme, ColorGradientRange range) {
		if (theme == null)
			return Color.GRAY;
		if(range == null || measurement == null || !Double.isFinite(measurement)) // missing data can result in NaN, log transformed value of -1 can result in -Infinity
			return theme.getNoDataColor();

		float rLow = (float)theme.getMinColor().getRed()   / 255f;
		float gLow = (float)theme.getMinColor().getGreen() / 255f;
		float bLow = (float)theme.getMinColor().getBlue()  / 255f;
		
		float rMid = (float)theme.getCenterColor().getRed()   / 255f;
		float gMid = (float)theme.getCenterColor().getGreen() / 255f;
		float bMid = (float)theme.getCenterColor().getBlue()  / 255f;
		 
		float rHigh = (float)theme.getMaxColor().getRed()   / 255f;
		float gHigh = (float)theme.getMaxColor().getGreen() / 255f;
		float bHigh = (float)theme.getMaxColor().getBlue()  / 255f;
		
		double median;
		if (range.getMinValue() >= 0)
			median = (range.getMaxValue() / 2);
		else
			median = 0.0;
		
		// This happens when you row-normalize but there is only one column. This is probably
		// not the best way to fix it...
		if(median == 0.0 && measurement == 0.0) {
			return theme.getCenterColor();
		} 
		
		if (measurement <= median) {
			float prop = (float) ((float) (measurement - range.getMinValue()) / (median - range.getMinValue()));
			float rVal = rLow + prop * (rMid - rLow);
			float gVal = gLow + prop * (gMid - gLow);
			float bVal = bLow + prop * (bMid - bLow);

			return new Color(rVal, gVal, bVal);
		} else {
			//Related to bug https://github.com/BaderLab/EnrichmentMapApp/issues/116
			//When there is differing max and mins for datasets then it will throw exception
			//for the dataset2 if the value is bigger than the max
			//This need to be fixed on the dataset but in the meantime if the value is bigger
			//than the max set it to the max
			if (measurement > range.getMaxValue())
				measurement = range.getMaxValue();

			float prop = (float) ((float) (measurement - median) / (range.getMaxValue() - median));
			float rVal = rMid + prop * (rHigh - rMid);
			float gVal = gMid + prop * (gHigh - gMid);
			float bVal = bMid + prop * (bHigh - bMid);

			return new Color(rVal, gVal, bVal);
		}
	}

}
