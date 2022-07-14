package org.grits.toolbox.io.ms.annotation.glycan.report.process.export;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.grits.toolbox.datamodel.ms.annotation.glycan.report.tablemodel.MSGlycanAnnotationReportTableDataObject;
import org.grits.toolbox.datamodel.ms.tablemodel.dmtranslate.DMPeak;
import org.grits.toolbox.datamodel.ms.tablemodel.dmtranslate.DMPrecursorPeak;
import org.grits.toolbox.display.control.table.datamodel.GRITSListDataRow;
import org.grits.toolbox.io.ms.annotation.glycan.process.export.MSGlycanAnnotationExportProcess;
import org.grits.toolbox.io.ms.annotation.process.export.MSAnnotationWriterExcel;

public class MSGlycanAnnotationReportExportProcess extends MSGlycanAnnotationExportProcess {	
	//log4J Logger
	private static final Logger logger = Logger.getLogger(MSGlycanAnnotationReportExportProcess.class);

	@Override
	protected MSAnnotationWriterExcel getNewMSAnnotationWriterExcel() {
		return new MSGlycanAnnotationReportWriterExcel();
	}

	/**
	 * Updates the mAtLeastOne Map as to whether the at least one experiment has a selected structure for the current interval.
	 * In this case, the Row Num and Row Id are the same and the method iterates over the experiments to see if at least one is selected.
	 * 
	 * @param iRowNum, the number of the row in the table
	 * @param mAtLeastOne, Map that tracks whether at least one candidate structure is selected for the Row Num across all experiments.
	 * @throws Exception
	 */
	@Override
	protected void markAnnotatedRows(int iRowNum, Map<Integer, List<String>> mAtLeastOne) throws Exception  {
		GRITSListDataRow row = getMyTableDataObject().getTableData().get(iRowNum);
		int iNumExps = getMyTableDataObject().getPeakIdCols().size();
		for( int i = 0; i < iNumExps; i++ ) {
			int iFeatureIdCol = getMyTableDataObject().getFeatureIdCols().get(i);
			String sFeatureId = (String) row.getDataRow().get(iFeatureIdCol);
			if( sFeatureId == null && hideUnAnnotatedRows() ) {
				continue;
			}
			if( sFeatureId != null ) {
				List<String> alAtLeastOne = null;
				if( ! mAtLeastOne.containsKey(iRowNum) ) {
					alAtLeastOne = new ArrayList<String>();
					mAtLeastOne.put(iRowNum, alAtLeastOne);
				} else {
					alAtLeastOne = mAtLeastOne.get(iRowNum);
				}
				alAtLeastOne.add( Integer.toString(i));			
			}
			
		}
	}
	
	/**
	 * In current Merge report, there is only ever one row for each interval, so the Row ID and the Row Number
	 * are the same.
	 * 
	 * @return Map of Row ID to list of Row Numbers in table
	 */
	@Override
	protected Map<String, List<Integer>> getRowIdtoRowNumMap() {
		Map<String, List<Integer>> mRowIdToRunNum = new HashMap<>();
		for( int i = 0; i < getTableDataObject().getTableData().size(); i++ )  {
			if(isCanceled()) {
				return null;
			}
			String sRowID = Integer.toString(i); // no multiple rows, just one for the interval w/ multiple experiments
			List<Integer> lRowNums = null;
			if( mRowIdToRunNum.containsKey(sRowID) ) {
				lRowNums = mRowIdToRunNum.get(sRowID);
			} else {
				lRowNums = new ArrayList<>();
				mRowIdToRunNum.put(sRowID, lRowNums);				
			}
			lRowNums.add(i);			
		}		
		return mRowIdToRunNum;
	}
	
	/**
	 * Based on the values stored in the mAtLeastOne map, determines if the row in the table, specified by
	 * iRowNum, is visible or not. In this case, there is only one row, so this method iterates over the features
	 * for each experiment to see if at least one is annotated
	 * 
	 * @param iRowNum, the number of the row in the table
	 * @param mAtLeastOne, Map that tracks whether at least one candidate structure is selected for the Row Id associated w/ current Row Num
	 * @return true if the row is annotated and isn't hidden
	 * @throws Exception
	 */
	@Override
	protected boolean getFinalVisibility(int iRowNum, Map<Integer, List<String>> mAtLeastOne) throws Exception  {
		GRITSListDataRow row = getMyTableDataObject().getTableData().get(iRowNum);
		int iNumExps = getMyTableDataObject().getPeakIdCols().size();
		boolean bInvisible = false;
		for( int i = 0; i < iNumExps; i++ ) {		
						
			int iFeatureIdCol = getMyTableDataObject().getFeatureIdCols().get(i);
			String sFeatureId = (String) row.getDataRow().get(iFeatureIdCol);
			if( sFeatureId == null && hideUnAnnotatedRows() ) {
				continue;
			}
					
			if (!mAtLeastOne.containsKey(iRowNum)) {
				// there is no row for this parent scan
				// check if it was previously hidden, if so skip this row	
				if( sFeatureId == null && hideUnAnnotatedRows() ) {
					continue;
				}
				if( sFeatureId != null  &&
						getMyTableDataObject().isHiddenRow(iRowNum, Integer.toString(i), sFeatureId) ) {
					continue;
				}
				bInvisible = true;
			} else {
				List<String> atLeastOne = mAtLeastOne.get(iRowNum);
				if (atLeastOne == null || atLeastOne.size() == 0) {
					// no rows for this parent scan
					// check if this is hidden already
					if( sFeatureId == null && hideUnAnnotatedRows() ) {
						continue;
					}
					if( sFeatureId != null && 
						getMyTableDataObject().isHiddenRow(iRowNum, Integer.toString(i), sFeatureId) ) {
						continue;
					}
					bInvisible = true;
				}
			}									
		}
		return bInvisible;
	}
	
	/**
	 * @param iRowNum
	 * @return 0 if visible and not hidden, 1 if hidden, 2 if invisible
	 */
	@Override
	protected int isVisible( int iRowNum ) {
		GRITSListDataRow row = getMyTableDataObject().getTableData().get(iRowNum);
		int iNumExps = getMyTableDataObject().getPeakIdCols().size();
		int iHiddenCount = 0;
		for( int i = 0; i < iNumExps; i++ ) {			
			int iFeatureIdCol = getMyTableDataObject().getFeatureIdCols().get(i);
			String sFeatureId = (String) row.getDataRow().get(iFeatureIdCol);
			if( sFeatureId == null && hideUnAnnotatedRows() ) {
				continue;
			}
			String iRowId = Integer.toString(i);
			
			if( getMyTableDataObject().isHiddenRow(iRowNum, iRowId, sFeatureId) )  {
				iHiddenCount++;
				continue;
			}
			
			// if we make it here, then the experiment data is visible!
			return 0;
		}
		if( iHiddenCount == iNumExps ) { 
			return 2;
		}
		return 1;
	}

	
	/**
	 * If the user specifies a minimum peak or precursor intensity, this method filters out those peaks
	 * whose intensity is less than the specified threshold by setting their intensity values to 0. For the Merge,
	 * the maximum intensity for each peak across all experiments is determined, and it is this max that is
	 * compared to the specified threshold to determine if it should be filtered out (set to 0).
	 * 
	 * @param iRowNum
	 * @param rowIntensityMap
	 */
	@Override
	protected void applyIntensityFilter(int iRowNum, Map<String, Double> rowIntensityMap) {
		GRITSListDataRow row = getMyTableDataObject().getTableData().get(iRowNum);
		List<Double> lFiltered = new ArrayList<>();
		int iNumExps = getMyTableDataObject().getSequenceCols().size();
		double dMaxInt = 0.0;
		for( int i = 0; i < iNumExps; i++ ) {
			
			Double dPeakIntensity = 0.0;
			if (filterKey != null && filterKey.equals(DMPeak.peak_intensity.getLabel())) {
				Double peakIntensity = (Double)row.getDataRow().get(getTableDataObject().getPeakIntensityCols().get(0));
				if (this.thresholdValue > 0 && peakIntensity >= this.thresholdValue ) { // skip this row since it fails to pass the threshold filter
					dPeakIntensity = peakIntensity;
				}
			} else if (filterKey != null && filterKey.equals(DMPrecursorPeak.precursor_peak_intensity.getLabel())) {
				Double intensity = (Double)row.getDataRow().get(getTableDataObject().getPrecursorIntensityCols().get(0));
				if (this.thresholdValue > 0 && intensity >= this.thresholdValue) { // skip this row since it fails to pass the threshold filter
					dPeakIntensity = intensity;
				}
			}
			if( dPeakIntensity > dMaxInt ) {
				dMaxInt = dPeakIntensity;
			}
		}
		rowIntensityMap.put(Integer.toString(iRowNum), dMaxInt);
		
	}
	
	/**
	 * Because this is a Merged Glycan Annotation table, this method must inspect each glycan and corresponding
	 * sequence from each experiment. This method will call "passesFilter" method to verify if the 
	 * any sequence passes the specified glycan filters (if any).
	 * 
	 * @param dataRow, array of values in the current row
	 * @return true if at least one sequence from all experiments passes any specified glycan filter (or none specified). Returns false otherwise.
	 * @throws Exception
	 * 
	 */
	@Override
	protected Boolean applyGlycanFilters(ArrayList<Object> dataRow) throws Exception {
		int iNumExps = getMyTableDataObject().getSequenceCols().size();
		boolean bPassedOne = false;
		for( int i = 0; i < iNumExps; i++ ) {
			// check if the row passes the filters
			int sequenceCol = getMyTableDataObject().getSequenceCols().get(i);
			String sequence = (String) dataRow.get(sequenceCol);
			boolean bPasses = passesFilters (sequence);
			bPassedOne |= bPasses;
		}
		return bPassedOne;
	}

	public MSGlycanAnnotationReportTableDataObject getMyTableDataObject() {
		return (MSGlycanAnnotationReportTableDataObject) tableDataObject;
	}

	
}
