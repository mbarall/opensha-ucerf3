package org.opensha.sha.earthquake.faultSysSolution.ruptures.util;

import java.awt.Color;
import java.awt.Font;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.dom4j.DocumentException;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.annotations.XYBoxAnnotation;
import org.jfree.chart.annotations.XYPolygonAnnotation;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.TickUnit;
import org.jfree.chart.axis.TickUnits;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.data.Range;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.TextAnchor;
import org.opensha.commons.data.function.ArbitrarilyDiscretizedFunc;
import org.opensha.commons.data.function.DefaultXY_DataSet;
import org.opensha.commons.data.function.DiscretizedFunc;
import org.opensha.commons.data.function.EvenlyDiscretizedFunc;
import org.opensha.commons.data.function.HistogramFunction;
import org.opensha.commons.data.function.XY_DataSet;
import org.opensha.commons.geo.Location;
import org.opensha.commons.geo.LocationList;
import org.opensha.commons.geo.LocationUtils;
import org.opensha.commons.geo.Region;
import org.opensha.commons.gui.plot.HeadlessGraphPanel;
import org.opensha.commons.gui.plot.PlotCurveCharacterstics;
import org.opensha.commons.gui.plot.PlotElement;
import org.opensha.commons.gui.plot.PlotLineType;
import org.opensha.commons.gui.plot.PlotSpec;
import org.opensha.commons.gui.plot.PlotSymbol;
import org.opensha.commons.gui.plot.jfreechart.xyzPlot.XYZGraphPanel;
import org.opensha.commons.mapping.PoliticalBoundariesData;
import org.opensha.commons.mapping.gmt.elements.GMT_CPT_Files;
import org.opensha.commons.util.ClassUtils;
import org.opensha.commons.util.ComparablePairing;
import org.opensha.commons.util.DataUtils.MinMaxAveTracker;
import org.opensha.commons.util.ExceptionUtils;
import org.opensha.commons.util.MarkdownUtils;
import org.opensha.commons.util.MarkdownUtils.TableBuilder;
import org.opensha.commons.util.cpt.CPT;
import org.opensha.sha.earthquake.faultSysSolution.ruptures.ClusterRupture;
import org.opensha.sha.earthquake.faultSysSolution.ruptures.FaultSubsectionCluster;
import org.opensha.sha.earthquake.faultSysSolution.ruptures.Jump;
import org.opensha.sha.earthquake.faultSysSolution.ruptures.plausibility.PlausibilityConfiguration;
import org.opensha.sha.earthquake.faultSysSolution.ruptures.plausibility.PlausibilityFilter;
import org.opensha.sha.earthquake.faultSysSolution.ruptures.plausibility.ScalarCoulombPlausibilityFilter;
import org.opensha.sha.earthquake.faultSysSolution.ruptures.plausibility.ScalarValuePlausibiltyFilter;
import org.opensha.sha.earthquake.faultSysSolution.ruptures.plausibility.impl.CumulativeProbabilityFilter;
import org.opensha.sha.earthquake.faultSysSolution.ruptures.plausibility.impl.GapWithinSectFilter;
import org.opensha.sha.earthquake.faultSysSolution.ruptures.plausibility.impl.JumpAzimuthChangeFilter;
import org.opensha.sha.earthquake.faultSysSolution.ruptures.plausibility.impl.JumpAzimuthChangeFilter.AzimuthCalc;
import org.opensha.sha.earthquake.faultSysSolution.ruptures.plausibility.impl.JumpDistFilter;
import org.opensha.sha.earthquake.faultSysSolution.ruptures.plausibility.impl.MultiDirectionalPlausibilityFilter;
import org.opensha.sha.earthquake.faultSysSolution.ruptures.plausibility.impl.SplayCountFilter;
import org.opensha.sha.earthquake.faultSysSolution.ruptures.plausibility.impl.CumulativeProbabilityFilter.*;
import org.opensha.sha.earthquake.faultSysSolution.ruptures.strategies.ClusterConnectionStrategy;
import org.opensha.sha.earthquake.faultSysSolution.ruptures.strategies.DistCutoffClosestSectClusterConnectionStrategy;
import org.opensha.sha.faultSurface.FaultSection;
import org.opensha.sha.faultSurface.RuptureSurface;
import org.opensha.sha.faultSurface.utils.GriddedSurfaceUtils;
import org.opensha.sha.simulators.stiffness.SubSectStiffnessCalculator;
import org.opensha.sha.simulators.stiffness.SubSectStiffnessCalculator.StiffnessType;
import org.opensha.sha.simulators.utils.RupturePlotGenerator;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;

import scratch.UCERF3.FaultSystemRupSet;
import scratch.UCERF3.FaultSystemSolution;
import scratch.UCERF3.enumTreeBranches.FaultModels;
import scratch.UCERF3.inversion.SectionConnectionStrategy;
import scratch.UCERF3.inversion.laughTest.PlausibilityResult;
import scratch.UCERF3.utils.FaultSystemIO;

public class RupSetDiagnosticsPageGen {

	@SuppressWarnings("unused")
	public static void main(String[] args) throws IOException, DocumentException {
		if (args.length == 1 && args[0].equals("--hardcoded")) {
			// special case to make things easier for Kevin in eclipse
			
			File rupSetsDir = new File("/home/kevin/OpenSHA/UCERF4/rup_sets");

//			String inputName = "5% Sect Fract Increase";
//			File inputFile = new File(rupSetsDir, "fm3_1_cmlAz_cffClusterPathPositive_sectFractPerm0.05.zip");
//			String inputName = "10% Sect Fract Increase";
//			File inputFile = new File(rupSetsDir, "fm3_1_cmlAz_cffClusterPathPositive_sectFractPerm0.1.zip");
//			String inputName = "15% Sect Fract Increase";
//			File inputFile = new File(rupSetsDir, "fm3_1_cmlAz_cffClusterPathPositive_sectFractPerm0.15.zip");
//			String inputName = "CmlAz, CFF Cluster Path Positive";
//			File inputFile = new File(rupSetsDir, "fm3_1_cmlAz_cffClusterPathPositive.zip");
//			String inputName = "UCERF3, 10% Sect Fract Increase";
//			File inputFile = new File(rupSetsDir, "fm3_1_ucerf3_sectFractPerm0.1.zip");
//			String inputName = "10km Jump Dist";
//			File inputFile = new File(rupSetsDir, "fm3_1_10km_cmlAz_cffClusterPathPositive.zip");
//			float probThresh = 0.005f;
//			String inputName = "Cumulative W&B 16-17 P>"+probThresh;
//			File inputFile = new File(rupSetsDir, "fm3_1_cmlProb"+probThresh+"-BW16-17_cffClusterPathPositive.zip");
//			String inputName = "10km, Cumulative W&B 16-17 P>0.005";
//			File inputFile = new File(rupSetsDir, "fm3_1_10km_cmlProb0.005-BW16-17_cffClusterPathPositive.zip");
			
//			String inputName = "Penalty5: Az60, Jump 1, Rake45";
////			File inputFile = new File(rupSetsDir, "fm3_1_cmlPen5_az60_jump0.1km_rake45_cffClusterPathPositive.zip");
//			File inputFile = new File(rupSetsDir, "fm3_1_ucerf3_cmlPen5_jump1km_rake45.zip");
			
			String inputName = "RSQSim 4983, SectArea=0.5";
			File inputFile = new File(rupSetsDir, "rsqsim_4983_stitched_m6.5_skip65000_sectArea0.5.zip");
			
//			String inputName = "UCERF3";
//			File inputFile = new File(rupSetsDir, "fm3_1_ucerf3.zip");
			
			String compName = "UCERF3";
			File compareFile = new File(rupSetsDir, "fm3_1_ucerf3.zip");
//			String compName = null;
//			File compareFile = null;
//			String compName = "CmlAz, CFF Cluster Path Positive";
//			File compareFile = new File(rupSetsDir, "fm3_1_cmlAz_cffClusterPathPositive.zip");
//			String compName = "CmlAz Only";
//			File compareFile = new File(rupSetsDir, "fm3_1_cmlAz.zip");
//			String compName = "CmlAz, CFF Cluster Positive";
//			File compareFile = new File(rupSetsDir, "fm3_1_cmlAz_cffClusterPositive.zip");
//			File altPlausibilityCompareFile = new File(rupSetsDir, "new_coulomb_filters.json");
//			File altPlausibilityCompareFile = new File(rupSetsDir, "new_cumulative_prob_filters.json");
			File altPlausibilityCompareFile = new File(rupSetsDir, "alt_filters.json");
//			File altPlausibilityCompareFile = null;

			List<String> argz = new ArrayList<>();
			argz.add("--reports-dir"); argz.add("/home/kevin/markdown/rupture-sets");
			argz.add("--dist-az-cache");
			argz.add(new File(rupSetsDir, "fm3_1_dist_az_cache.csv").getAbsolutePath());
			argz.add("--coulomb-cache-dir"); argz.add(rupSetsDir.getAbsolutePath());
			argz.add("--rupture-set"); argz.add(inputFile.getAbsolutePath());
			if (inputName != null) {
				argz.add("--name"); argz.add(inputName);
			}
			if (compareFile != null) {
				argz.add("--comp-rupture-set"); argz.add(compareFile.getAbsolutePath());
				if (compName != null)
					argz.add("--comp-name"); argz.add(compName);
			}
			if (altPlausibilityCompareFile != null) {
				argz.add("--alt-plausibility"); argz.add(altPlausibilityCompareFile.getAbsolutePath());
			}
			args = argz.toArray(new String[0]);
		}
		
		System.setProperty("java.awt.headless", "true");
		
		Options options = createOptions();
		
		CommandLineParser parser = new DefaultParser();
		
		CommandLine cmd;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			e.printStackTrace();
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(ClassUtils.getClassNameWithoutPackage(RupSetDiagnosticsPageGen.class),
					options, true );
			System.exit(2);
			return;
		}
		
		File inputFile = new File(cmd.getOptionValue("rupture-set"));
		Preconditions.checkArgument(inputFile.exists(),
				"Rupture set file doesn't exist: %s", inputFile.getAbsolutePath());
		String inputName;
		if (cmd.hasOption("name"))
			inputName = cmd.getOptionValue("name");
		else
			inputName = inputFile.getName().replaceAll(".zip", "");
		
		File compareFile = null;
		String compName = null;
		if (cmd.hasOption("comp-rupture-set")) {
			compareFile = new File(cmd.getOptionValue("comp-rupture-set"));
			Preconditions.checkArgument(compareFile.exists(),
					"Rupture set file doesn't exist: %s", compareFile.getAbsolutePath());
			if (cmd.hasOption("comp-name"))
				compName = cmd.getOptionValue("comp-name");
			else
				compName = compareFile.getName().replaceAll(".zip", "");
		}
		
		File outputDir;
		if (cmd.hasOption("output-dir")) {
			Preconditions.checkArgument(!cmd.hasOption("reports-dir"),
					"Can't supply both --output-dir and --reports-dir");
			outputDir = new File(cmd.getOptionValue("output-dir"));
			Preconditions.checkState(outputDir.exists() || outputDir.mkdir(),
					"Output dir doesn't exist and could not be created: %s", outputDir.getAbsolutePath());
		} else {
			Preconditions.checkArgument(cmd.hasOption("reports-dir"),
					"Must supply either --output-dir or --reports-dir");
			File reportsDir = new File(cmd.getOptionValue("reports-dir"));
			Preconditions.checkState(reportsDir.exists() || reportsDir.mkdir(),
					"Reports dir doesn't exist and could not be created: %s", reportsDir.getAbsolutePath());
			
			outputDir = new File(reportsDir, inputFile.getName().replaceAll(".zip", ""));
			Preconditions.checkState(outputDir.exists() || outputDir.mkdir());
			
			if (compareFile == null)
				outputDir = new File(outputDir, "standalone");
			else
				outputDir = new File(outputDir, "comp_"+compareFile.getName().replaceAll(".zip", ""));
			Preconditions.checkState(outputDir.exists() || outputDir.mkdir());
		}
		
		File altPlausibilityCompareFile = null;
		if (cmd.hasOption("alt-plausibility")) {
			altPlausibilityCompareFile = new File(cmd.getOptionValue("alt-plausibility"));
			Preconditions.checkState(altPlausibilityCompareFile.exists(),
					"Alt-plausibility file doesn't exist: %s", altPlausibilityCompareFile.getAbsolutePath());
		}
		
		File coulombCacheDir = null;
		if (cmd.hasOption("coulomb-cache-dir")) {
			coulombCacheDir = new File(cmd.getOptionValue("coulomb-cache-dir"));
			Preconditions.checkState(coulombCacheDir.exists(),
					"Coulomb cache dir doesn't exist: %s", coulombCacheDir.getAbsolutePath());
		}
		
		File distAzCache = null;
		if (cmd.hasOption("dist-az-cache")) {
			distAzCache = new File(cmd.getOptionValue("dist-az-cache"));
			Preconditions.checkState(distAzCache.exists(),
					"Distance/azimuth cache doesn't exist: %s", distAzCache.getAbsolutePath());
		}
		
		File resourcesDir = new File(outputDir, "resources");
		Preconditions.checkState(resourcesDir.exists() || resourcesDir.mkdir());
		
		FaultSystemRupSet inputRupSet;
		FaultSystemSolution inputSol = null;
		PlausibilityConfiguration inputConfig = null;
		
		FaultSystemRupSet compRupSet = null;
		FaultSystemSolution compSol = null;
		PlausibilityConfiguration compConfig = null;
		
		System.out.println("Loading input");
		if (FaultSystemIO.isSolution(inputFile)) {
			System.out.println("Input is a solution");
			inputSol = FaultSystemIO.loadSol(inputFile);
			inputRupSet = inputSol.getRupSet();
		} else {
			inputRupSet = FaultSystemIO.loadRupSet(inputFile);
		}
		inputConfig = inputRupSet.getPlausibilityConfiguration();
		List<? extends FaultSection> subSects = inputRupSet.getFaultSectionDataList();
		
		SectionDistanceAzimuthCalculator distAzCalc;
		if (inputConfig == null)
			distAzCalc = new SectionDistanceAzimuthCalculator(subSects);
		else
			distAzCalc = inputConfig.getDistAzCalc();
		if (distAzCache != null && distAzCache.exists())
			distAzCalc.loadCacheFile(distAzCache);
		int numAzCached = distAzCalc.getNumCachedAzimuths();
		int numDistCached = distAzCalc.getNumCachedDistances();
		
		if (inputConfig == null) {
			// see if it's UCERF3
			FaultModels fm = getUCERF3FM(inputRupSet);
			if (fm != null) {
				inputConfig = PlausibilityConfiguration.getUCERF3(subSects, distAzCalc, fm);
				inputRupSet.setPlausibilityConfiguration(inputConfig);
			}
		}
		// check load coulomb
		HashMap<String, List<SubSectStiffnessCalculator>> loadedCoulombCaches = new HashMap<>();
		if (inputConfig != null && coulombCacheDir != null)
			checkLoadCoulombCache(inputConfig.getFilters(), coulombCacheDir, loadedCoulombCaches);
		RuptureConnectionSearch inputSearch = new RuptureConnectionSearch(
				inputRupSet, distAzCalc, getSearchMaxJumpDist(inputConfig), false);
		System.out.println("Building input cluster ruptures");
		List<ClusterRupture> inputRups = inputRupSet.getClusterRuptures();
		if (inputRups == null) {
			inputRupSet.buildClusterRups(inputSearch);
			inputRups = inputRupSet.getClusterRuptures();
		}
		HashSet<UniqueRupture> inputUniques = new HashSet<>();
		for (ClusterRupture rup : inputRups)
			inputUniques.add(rup.unique);
		
		// detect region
		MinMaxAveTracker latTrack = new MinMaxAveTracker();
		MinMaxAveTracker lonTrack = new MinMaxAveTracker();
		for (FaultSection sect : inputRupSet.getFaultSectionDataList()) {
			for (Location loc : sect.getFaultTrace()) {
				latTrack.addValue(loc.getLatitude());
				lonTrack.addValue(loc.getLongitude());
			}
		}
		double minLat = Math.floor(latTrack.getMin());
		double maxLat = Math.ceil(latTrack.getMax());
		double minLon = Math.floor(lonTrack.getMin());
		double maxLon = Math.ceil(lonTrack.getMax());
		Region region = new Region(new Location(minLat, minLon), new Location(maxLat, maxLon));

		List<ClusterRupture> compRups = null;
		RuptureConnectionSearch compSearch = null;
		HashSet<UniqueRupture> compUniques = null;
		if (compareFile != null) {
			System.out.println("Loading comparison");
			if (FaultSystemIO.isSolution(compareFile)) {
				System.out.println("comp is a solution");
				compSol = FaultSystemIO.loadSol(compareFile);
				compRupSet = compSol.getRupSet();
			} else {
				compRupSet = FaultSystemIO.loadRupSet(compareFile);
			}
			Preconditions.checkState(compRupSet.getNumSections() == subSects.size(),
					"comp has different sub sect count");
			compConfig = compRupSet.getPlausibilityConfiguration();
			if (compConfig == null) {
				// see if it's UCERF3
				FaultModels fm = getUCERF3FM(compRupSet);
				if (fm != null) {
					compConfig = PlausibilityConfiguration.getUCERF3(subSects, distAzCalc, fm);
					compRupSet.setPlausibilityConfiguration(compConfig);
				}
			}
			if (compConfig != null && coulombCacheDir != null)
				checkLoadCoulombCache(compConfig.getFilters(), coulombCacheDir, loadedCoulombCaches);
			compSearch = new RuptureConnectionSearch(
					compRupSet, distAzCalc, getSearchMaxJumpDist(compConfig), false);
			compRups = compRupSet.getClusterRuptures();
			if (compRups == null) {
				compRupSet.buildClusterRups(compSearch);
				compRups = compRupSet.getClusterRuptures();
			}
			compUniques = new HashSet<>();
			for (ClusterRupture rup : compRups)
				compUniques.add(rup.unique);
		}
		
		List<String> lines = new ArrayList<>();
		lines.add("# Rupture Set Diagnostics: "+inputName);
		lines.add("");
		lines.addAll(getBasicLines(inputRupSet));
		lines.add("");
		int tocIndex = lines.size();
		String topLink = "*[(top)](#table-of-contents)*";
		
		// calculate jumps
		Map<Jump, List<Integer>> inputJumpsToRupsMap = new HashMap<>();
		Map<Jump, Double> inputJumps = getJumps(inputSol, inputRups, inputJumpsToRupsMap);
		Map<Jump, List<Integer>> compJumpsToRupsMap = null;
		Map<Jump, Double> compJumps = null;
		Map<Jump, Double> inputUniqueJumps = null;
		Set<Jump> commonJumps = null;
		Map<Jump, Double> compUniqueJumps = null;
		if (compRups != null) {
			compJumpsToRupsMap = new HashMap<>();
			compJumps = getJumps(compSol, compRups, compJumpsToRupsMap);
			inputUniqueJumps = new HashMap<>(inputJumps);
			commonJumps = new HashSet<>();
			for (Jump jump : compJumps.keySet()) {
				if (inputUniqueJumps.containsKey(jump)) {
					inputUniqueJumps.remove(jump);
					commonJumps.add(jump);
				}
			}
			compUniqueJumps = new HashMap<>(compJumps);
			for (Jump jump : inputJumps.keySet())
				if (compUniqueJumps.containsKey(jump))
					compUniqueJumps.remove(jump);
		}
		
		if (inputConfig != null) {
			lines.add("## Plausibility Configuration");
			lines.add(topLink); lines.add("");
			lines.addAll(getPlausibilityLines(inputConfig, inputJumps));
			lines.add("");
		}
		
		if (compRupSet != null) {
			lines.add("## Comparison Rup Set");
			lines.add(topLink); lines.add("");
			lines.add("Will include comparisons against: "+compName);
			lines.add("");
			lines.addAll(getBasicLines(compRupSet));
			lines.add("");
			
			lines.add("### Rupture Set Overlap");
			lines.add(topLink); lines.add("");
			
			TableBuilder table = MarkdownUtils.tableBuilder();
			table.addLine("", inputName, compName);
			table.addLine("**Total Count**", inputRupSet.getNumRuptures(), compRupSet.getNumRuptures());
			int numInputUnique = 0;
			double rateInputUnique = 0d;
			for (int r=0; r<inputRupSet.getNumRuptures(); r++) {
				if (!compUniques.contains(inputRups.get(r).unique)) {
					numInputUnique++;
					if (inputSol != null)
						rateInputUnique += inputSol.getRateForRup(r);
				}
			}
			int numCompUnique = 0;
			double rateCompUnique = 0d;
			for (int r=0; r<compRupSet.getNumRuptures(); r++) {
				if (!inputUniques.contains(compRups.get(r).unique)) {
					numCompUnique++;
					if (compSol != null)
						rateCompUnique += compSol.getRateForRup(r);
				}
			}
			table.initNewLine();
			table.addColumn("**# Unique**");
			table.addColumn(numInputUnique+" ("+percentDF.format(
					(double)numInputUnique/(double)inputRups.size())+")");
			table.addColumn(numCompUnique+" ("+percentDF.format(
					(double)numCompUnique/(double)compRups.size())+")");
			table.finalizeLine();
			if (inputSol != null || compSol != null) {
				table.addColumn("**Unique Rate**");
				if (inputSol == null)
					table.addColumn("*N/A*");
				else
					table.addColumn((float)rateInputUnique+" ("+percentDF.format(
							rateInputUnique/inputSol.getTotalRateForAllFaultSystemRups())+")");
				if (compSol == null)
					table.addColumn("*N/A*");
				else
					table.addColumn((float)rateCompUnique+" ("+percentDF.format(
							rateCompUnique/compSol.getTotalRateForAllFaultSystemRups())+")");
				table.finalizeLine();
			}
			lines.addAll(table.build());
			lines.add("");
			
			if (compConfig != null) {
				lines.add("### Comp Plausibility Configuration");
				lines.add(topLink); lines.add("");
				lines.addAll(getPlausibilityLines(compConfig, compJumps));
				lines.add("");
			}
		}
		
		// length and magnitude distributions
		
		lines.add("## Rupture Size Histograms");
		lines.add(topLink); lines.add("");

		List<Integer> inputIndexes = new ArrayList<>();
		for (int i=0; i<inputRups.size(); i++)
			inputIndexes.add(i);
		File rupHtmlDir = new File(resourcesDir.getParentFile(), "hist_rup_pages");
		Preconditions.checkState(rupHtmlDir.exists() || rupHtmlDir.mkdir());
		List<HistScalarValues> inputScalarVals = new ArrayList<>();
		List<HistScalarValues> compScalarVals = compRups == null ? null : new ArrayList<>();
		for (HistScalar scalar : HistScalar.values()) {
			lines.add("### "+scalar.name);
			lines.add(topLink); lines.add("");
			lines.add(scalar.description);
			lines.add("");
			TableBuilder table = MarkdownUtils.tableBuilder();
			HistScalarValues inputScalars = new HistScalarValues(scalar, inputRupSet, inputSol,
					inputRups, distAzCalc);
			inputScalarVals.add(inputScalars);
			HistScalarValues compScalars = null;
			if (compRupSet != null) {
				table.addLine(inputName, compName);
				compScalars = new HistScalarValues(scalar, compRupSet, compSol,
						compRups, distAzCalc);
				compScalarVals.add(compScalars);
			}
			plotRuptureHistograms(resourcesDir, "hist_"+scalar.name(), table, inputScalars,
					inputUniques, compScalars, compUniques);
			
			lines.addAll(table.build());
			lines.add("");
			
			double[] fractiles = scalar.getExampleRupPlotFractiles();
			if (fractiles == null)
				continue;
			
			int[] fractileIndexes = new int[fractiles.length];
			for (int i=0; i<fractiles.length; i++) {
				double f = fractiles[i];
				if (f == 1d)
					fractileIndexes[i] = inputRups.size()-1;
				else
					fractileIndexes[i] = (int)(f*inputRups.size());
			}
			
			lines.add("");
			lines.add("#### "+scalar.name+" Extremes & Examples");
			lines.add(topLink); lines.add("");
			lines.add("Example "+inputName+" ruptures at varios percentiles of "+scalar.name);
			lines.add("");
			
			List<Double> vals = new ArrayList<>();
			for (int index=0; index<inputRups.size(); index++)
				vals.add(scalar.getValue(index, inputRupSet, inputRups.get(index), distAzCalc));
			List<Integer> sortedIndexes = ComparablePairing.getSortedData(vals, inputIndexes);
			
			table = MarkdownUtils.tableBuilder();
			table.initNewLine();
			for (int i=0; i<fractiles.length; i++) {
				int index = sortedIndexes.get(fractileIndexes[i]);
				double val = vals.get(index);
				double f = fractiles[i];
				String str;
				if (f == 0d)
					str = "Minimum";
				else if (f == 1d)
					str = "Maximum";
				else
					str = "p"+new DecimalFormat("0.#").format(f*100d);
				str += ": ";
				if (val < 0.1)
					str += (float)val;
				else
					str += new DecimalFormat("0.##").format(val);
				table.addColumn("**"+str+"**");
			}
			table.finalizeLine();
			table.initNewLine();
			for (int rawIndex : fractileIndexes) {
				int index = sortedIndexes.get(rawIndex);
				String rupPrefix = "hist_rup_"+index;
				RupCartoonGenerator.plotRupture(resourcesDir, rupPrefix, inputRups.get(index),
						"Rupture "+index, false, true);
				table.addColumn("[<img src=\"" + resourcesDir.getName() + "/" + rupPrefix + ".png\" />]"+
						"("+ generateRuptureInfoPage(inputRupSet, inputRups.get(index),
								index, rupHtmlDir, rupPrefix, null, distAzCalc)+ ")");
			}
			
			lines.addAll(table.wrap(4, 0).build());
			lines.add("");
		}
		
//		System.gc();
//		try {
//			Thread.sleep(100000000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		if ((compRups != null && (inputConfig != null || compConfig != null))
				|| altPlausibilityCompareFile != null) {
			// plausibility comparisons
			
			lines.add("Plausibility Comparisons");
			lines.add(topLink); lines.add("");
			
			if (compConfig != null) {
				lines.add("## Comparisons with "+compName+" filters");
				lines.add(topLink); lines.add("");
				
				List<PlausibilityFilter> filters = new ArrayList<>();
				// add implicit filters
				double jumpDist = compConfig.getConnectionStrategy().getMaxJumpDist();
				if (Double.isFinite(jumpDist))
					filters.add(new JumpDistFilter(jumpDist));
				filters.add(new SplayCountFilter(compConfig.getMaxNumSplays()));
				filters.add(new GapWithinSectFilter());
				filters.addAll(compConfig.getFilters());
				RupSetPlausibilityResult result = testRupSetPlausibility(inputRups, filters, inputConfig, inputSearch);
				File plot = plotRupSetPlausibility(result, resourcesDir, "comp_filter_compare",
						"Comparison with "+compName+" Filters");
				lines.add("![plot](resources/"+plot.getName()+")");
				lines.add("");
				lines.addAll(getRupSetPlausibilityTable(result).build());
				lines.add("");
				lines.add("**Magnitude-filtered comparisons**");
				lines.add("");
				lines.addAll(getMagPlausibilityTable(inputRupSet, result, resourcesDir,
						"comp_filter_mag_comp").wrap(2, 0).build());
				lines.add("");
				lines.addAll(getRupSetPlausibilityDetailLines(result, false, inputRupSet, inputRups, 15,
						resourcesDir, "### "+compName, topLink, inputSearch, inputScalarVals));
				lines.add("");
			}
			
			if (compRups != null && inputConfig != null) {
				lines.add("## "+compName+" comparisons with new filters");
				lines.add(topLink); lines.add("");
				
				List<PlausibilityFilter> filters = new ArrayList<>();
				// add implicit filters
				double jumpDist = inputConfig.getConnectionStrategy().getMaxJumpDist();
				if (Double.isFinite(jumpDist))
					filters.add(new JumpDistFilter(jumpDist));
				filters.add(new SplayCountFilter(inputConfig.getMaxNumSplays()));
				filters.add(new GapWithinSectFilter());
				filters.addAll(inputConfig.getFilters());
				RupSetPlausibilityResult result = testRupSetPlausibility(compRups, filters, compConfig, compSearch);
				File plot = plotRupSetPlausibility(result, resourcesDir, "main_filter_compare",
						"Comparison with "+inputName+" Filters");
				lines.add("![plot](resources/"+plot.getName()+")");
				lines.add("");
				lines.addAll(getRupSetPlausibilityTable(result).build());
				lines.add("");
				lines.add("**Magnitude-filtered comparisons**");
				lines.add("");
				lines.addAll(getMagPlausibilityTable(compRupSet, result, resourcesDir,
						"main_filter_mag_comp").wrap(2, 0).build());
				lines.add("");
				lines.addAll(getRupSetPlausibilityDetailLines(result, true, compRupSet, compRups, 15,
						resourcesDir, "### "+compName+" against", topLink, compSearch, compScalarVals));
				lines.add("");
			}
			
			if (altPlausibilityCompareFile != null) {
				lines.add("## Comparisons with Alternative filters");
				lines.add(topLink); lines.add("");
				
				// load in alternatives
				ClusterConnectionStrategy inputConnStrat = null;
				if (inputConfig == null)
					inputConnStrat = new DistCutoffClosestSectClusterConnectionStrategy(
							subSects, distAzCalc, 100d);
				else
					inputConnStrat = inputConfig.getConnectionStrategy();
				List<PlausibilityFilter> filters = PlausibilityConfiguration.readFiltersJSON(
						altPlausibilityCompareFile, inputConnStrat, distAzCalc);
				if (coulombCacheDir != null)
					checkLoadCoulombCache(filters, coulombCacheDir, loadedCoulombCaches);
				
				RupSetPlausibilityResult result = testRupSetPlausibility(inputRups, filters, inputConfig, inputSearch);
				File plot = plotRupSetPlausibility(result, resourcesDir, "alt_filter_compare",
						"Comparison with Alternative Filters");
				lines.add("![plot](resources/"+plot.getName()+")");
				lines.add("");
				lines.addAll(getRupSetPlausibilityTable(result).build());
				lines.add("");
				lines.add("**Magnitude-filtered comparisons**");
				lines.add("");
				lines.addAll(getMagPlausibilityTable(inputRupSet, result, resourcesDir,
						"alt_filter_mag_comp").wrap(2, 0).build());
				lines.add("");
				lines.addAll(getRupSetPlausibilityDetailLines(result, false, inputRupSet, inputRups, 15,
						resourcesDir, "### Alternative", topLink, inputSearch, inputScalarVals));
				lines.add("");
			}
		}
		
		// connections plots
		System.out.println("Plotting section connections");
		plotConnectivityLines(inputRupSet, resourcesDir, "sect_connectivity",
				inputName+" Connectivity", inputJumps.keySet(), MAIN_COLOR, region, 800);
		plotConnectivityLines(inputRupSet, resourcesDir, "sect_connectivity_hires",
				inputName+" Connectivity", inputJumps.keySet(), MAIN_COLOR, region, 3000);
		if (compRups != null) {
			plotConnectivityLines(compRupSet, resourcesDir, "sect_connectivity_comp",
					compName+" Connectivity", compJumps.keySet(), COMP_COLOR, region, 800);
			plotConnectivityLines(compRupSet, resourcesDir, "sect_connectivity_comp_hires",
					compName+" Connectivity", compJumps.keySet(), COMP_COLOR, region, 3000);
			plotConnectivityLines(inputRupSet, resourcesDir, "sect_connectivity_unique",
					inputName+" Unique Connectivity", inputUniqueJumps.keySet(), MAIN_COLOR, region, 800);
			plotConnectivityLines(inputRupSet, resourcesDir, "sect_connectivity_unique_hires",
					inputName+" Unique Connectivity", inputUniqueJumps.keySet(), MAIN_COLOR, region, 3000);
			plotConnectivityLines(compRupSet, resourcesDir, "sect_connectivity_unique_comp",
					compName+" Unique Connectivity", compUniqueJumps.keySet(), COMP_COLOR, region, 800);
			plotConnectivityLines(compRupSet, resourcesDir, "sect_connectivity_unique_comp_hires",
					compName+" Unique Connectivity", compUniqueJumps.keySet(), COMP_COLOR, region, 3000);
		}
		
		double maxConnDist = 0d;
		for (Jump jump : inputJumps.keySet())
			maxConnDist = Math.max(maxConnDist, jump.distance);
		if (compJumps != null)
			for (Jump jump : compJumps.keySet())
				maxConnDist = Math.max(maxConnDist, jump.distance);
		plotConnectivityHistogram(resourcesDir, "sect_connectivity_hist",
				inputName+" Connectivity", inputJumps, inputUniqueJumps, maxConnDist,
				MAIN_COLOR, false, false);
		if (inputSol != null) {
			plotConnectivityHistogram(resourcesDir, "sect_connectivity_hist_rates",
					inputName+" Connectivity", inputJumps, inputUniqueJumps, maxConnDist,
					MAIN_COLOR, true, false);
			plotConnectivityHistogram(resourcesDir, "sect_connectivity_hist_rates_log",
					inputName+" Connectivity", inputJumps, inputUniqueJumps, maxConnDist,
					MAIN_COLOR, true, true);
		}
		if (compRups != null) {
			plotConnectivityHistogram(resourcesDir, "sect_connectivity_hist_comp",
					compName+" Connectivity", compJumps, compUniqueJumps, maxConnDist,
					COMP_COLOR, false, false);
			if (compSol != null) {
				plotConnectivityHistogram(resourcesDir, "sect_connectivity_hist_rates_comp",
						compName+" Connectivity", compJumps, compUniqueJumps, maxConnDist,
						COMP_COLOR, true, false);
				plotConnectivityHistogram(resourcesDir, "sect_connectivity_hist_rates_comp_log",
						compName+" Connectivity", compJumps, compUniqueJumps, maxConnDist,
						COMP_COLOR, true, true);
			}
		}
		
		lines.add("## Fault Section Connections");
		lines.add(topLink); lines.add("");
		
		if (compRups != null) {
			List<Set<Jump>> connectionsList = new ArrayList<>();
			List<Color> connectedColors = new ArrayList<>();
			List<String> connNames = new ArrayList<>();
			
			connectionsList.add(inputUniqueJumps.keySet());
			connectedColors.add(MAIN_COLOR);
			connNames.add(inputName+" Only");
			
			connectionsList.add(compUniqueJumps.keySet());
			connectedColors.add(COMP_COLOR);
			connNames.add(compName+" Only");
			
			connectionsList.add(commonJumps);
			connectedColors.add(COMMON_COLOR);
			connNames.add("Common Connections");
			
			String combConnPrefix = "sect_connectivity_combined";
			plotConnectivityLines(inputRupSet, resourcesDir, combConnPrefix, "Combined Connectivity",
					connectionsList, connectedColors, connNames, region, 800);
			plotConnectivityLines(inputRupSet, resourcesDir, combConnPrefix+"_hires", "Combined Connectivity",
					connectionsList, connectedColors, connNames, region, 3000);
			lines.add("![Combined]("+resourcesDir.getName()+"/"+combConnPrefix+".png)");
			lines.add("");
			lines.add("[View high resolution]("+resourcesDir.getName()+"/"+combConnPrefix+"_hires.png)");
			lines.add("");
			
			lines.add("### Jump Overlaps");
			lines.add(topLink); lines.add("");
			
			TableBuilder table = MarkdownUtils.tableBuilder();
			table.addLine("", inputName, compName);
			table.addLine("**Total Count**", inputJumps.size(), compJumps.size());
			table.initNewLine();
			table.addColumn("**# Unique Jumps**");
			table.addColumn(inputUniqueJumps.size()+" ("+percentDF.format(
					(double)inputUniqueJumps.size()/(double)inputJumps.size())+")");
			table.addColumn(compUniqueJumps.size()+" ("+percentDF.format(
					(double)compUniqueJumps.size()/(double)compJumps.size())+")");
			table.finalizeLine();
			if (inputSol != null || compSol != null) {
				table.addColumn("**Unique Jump Rate**");
				if (inputSol == null) {
					table.addColumn("*N/A*");
				} else {
					double rateInputUnique = 0d;
					for (Double rate : inputUniqueJumps.values())
						rateInputUnique += rate;
					table.addColumn((float)rateInputUnique+" ("+percentDF.format(
							rateInputUnique/inputSol.getTotalRateForAllFaultSystemRups())+")");
				}
				if (compSol == null) {
					table.addColumn("*N/A*");
				} else {
					double rateCompUnique = 0d;
					for (Double rate : compUniqueJumps.values())
						rateCompUnique += rate;
					table.addColumn((float)rateCompUnique+" ("+percentDF.format(
							rateCompUnique/compSol.getTotalRateForAllFaultSystemRups())+")");
				}
				table.finalizeLine();
			}
			lines.addAll(table.build());
			lines.add("");
		}
		
		TableBuilder table = MarkdownUtils.tableBuilder();
		if (compRups != null)
			table.addLine(inputName, compName);
		File mainPlot = new File(resourcesDir, "sect_connectivity.png");
		File compPlot = new File(resourcesDir, "sect_connectivity_comp.png");
		addTablePlots(table, mainPlot, compPlot, compRups != null);
		if (compRups != null) {
			mainPlot = new File(resourcesDir, "sect_connectivity_unique.png");
			compPlot = new File(resourcesDir, "sect_connectivity_unique_comp.png");
			addTablePlots(table, mainPlot, compPlot, compRups != null);
		}
		mainPlot = new File(resourcesDir, "sect_connectivity_hist.png");
		compPlot = new File(resourcesDir, "sect_connectivity_hist_comp.png");
		addTablePlots(table, mainPlot, compPlot, compRups != null);
		if (inputSol != null || compSol != null) {
			mainPlot = new File(resourcesDir, "sect_connectivity_hist_rates.png");
			compPlot = new File(resourcesDir, "sect_connectivity_hist_rates_comp.png");
			addTablePlots(table, mainPlot, compPlot, compRups != null);
			mainPlot = new File(resourcesDir, "sect_connectivity_hist_rates_log.png");
			compPlot = new File(resourcesDir, "sect_connectivity_hist_rates_comp_log.png");
			addTablePlots(table, mainPlot, compPlot, compRups != null);
		}
		lines.addAll(table.build());
		lines.add("");
		
		if (compRups!= null) {
			System.out.println("Plotting connection examples");
			lines.add("### Unique Connection Example Ruptures");
			lines.add(topLink); lines.add("");
			
			lines.add("**New Ruptures with Unique Connections**");
			int maxRups = 20;
			int maxCols = 5;
			table = plotConnRupExamples(inputSearch, inputRupSet, inputUniqueJumps.keySet(),
					inputJumpsToRupsMap, maxRups, maxCols, resourcesDir, "conn_example");
			lines.add("");
			if (table == null)
				lines.add("*N/A*");
			else
				lines.addAll(table.build());
			lines.add("");
			lines.add("**"+compName+" Ruptures with Unique Connections**");
			table = plotConnRupExamples(compSearch, compRupSet, compUniqueJumps.keySet(),
					compJumpsToRupsMap, maxRups, maxCols, resourcesDir, "comp_conn_example");
			lines.add("");
			if (table == null)
				lines.add("*N/A*");
			else
				lines.addAll(table.build());
			lines.add("");
		}
		
		// now plot section maximum mag/connected lengths
		
		lines.add("## Subsection Maximum Values");
		lines.add(topLink); lines.add("");
		lines.add("These plots show the maximum value of various quantities across all ruptures for which "
				+ "each individual subsection participates. This is useful, for example, to find sections "
				+ "with low maximum magnitudes (due to low or no connectivity).");
		lines.add("");
		for (int i=0; i<inputScalarVals.size(); i++) {
			HistScalarValues inputScalars = inputScalarVals.get(i);
			HistScalarValues compScalars = compScalarVals == null ? null : compScalarVals.get(i);
			HistScalar scalar = inputScalars.scalar;
			if (!(scalar == HistScalar.MAG) && !(scalar == HistScalar.LENGTH)
					&& !(scalar == HistScalar.CLUSTER_COUNT))
				continue;
			lines.add("### Subsection Maximum "+scalar.name);
			lines.add(topLink); lines.add("");
			table = MarkdownUtils.tableBuilder();
			if (compRups != null)
				table.addLine(inputName, compName);
			String prefix = "sect_max_"+scalar.name();
			if (!plotScalarMaxMapView(inputRupSet, resourcesDir, prefix, inputName,
					inputScalars, compScalars, region, MAIN_COLOR, false, false))
				continue;
			table.initNewLine();
			table.addColumn("![map]("+resourcesDir.getName()+"/"+prefix+".png)");
			if (compScalars != null) {
				plotScalarMaxMapView(compRupSet, resourcesDir, prefix+"_comp", compName,
						compScalarVals.get(i), inputScalars, region, COMP_COLOR, false, false);
				table.addColumn("![map]("+resourcesDir.getName()+"/"+prefix+"_comp.png)");
			}
			table.finalizeLine().initNewLine();
			table.addColumn("![map]("+resourcesDir.getName()+"/"+prefix+"_hist.png)");
			if (compScalarVals != null) {
				table.addColumn("![map]("+resourcesDir.getName()+"/"+prefix+"_comp_hist.png)");
			}
			table.finalizeLine();
			if (compRups != null) {
				table.addLine("**Difference**", "**Ratio**");
				table.initNewLine();
				plotScalarMaxMapView(inputRupSet, resourcesDir, prefix+"_diff", "Difference",
						inputScalars, compScalars, region, MAIN_COLOR, true, false);
				table.addColumn("![map]("+resourcesDir.getName()+"/"+prefix+"_diff.png)");
				plotScalarMaxMapView(inputRupSet, resourcesDir, prefix+"_ratio", "Ratio",
						inputScalars, compScalars, region, MAIN_COLOR, false, true);
				table.addColumn("![map]("+resourcesDir.getName()+"/"+prefix+"_ratio.png)");
				table.finalizeLine().initNewLine();
				table.addColumn("![map]("+resourcesDir.getName()+"/"+prefix+"_diff_hist.png)");
				table.addColumn("![map]("+resourcesDir.getName()+"/"+prefix+"_ratio_hist.png)");
				table.finalizeLine();
			}
			lines.addAll(table.build());
			lines.add("");
		}
		
		// now jumps
		
		float[] maxJumpDists = { 0.1f, 1f, 3f };
		boolean hasSols = compSol != null || inputSol != null;
		
		lines.add("## Jump Counts Over Distance");
		lines.add(topLink); lines.add("");
		table = MarkdownUtils.tableBuilder();
		if (hasSols)
			table.addLine("As Discretized", "Rate Weighted");
		for (float jumpDist : maxJumpDists) {
			lines.add("");
			System.out.println("Plotting num jumps");
			table.initNewLine();
			File plotFile = plotFixedJumpDist(inputRupSet, null, inputRups, inputName,
					compRupSet, null, compRups, compName, distAzCalc, 0d, jumpDist, resourcesDir);
			table.addColumn("![Plausibility Filter]("+resourcesDir.getName()+"/"+plotFile.getName()+")");
			if (hasSols) {
				plotFile = plotFixedJumpDist(
						inputSol == null ? null : inputRupSet, inputSol, inputRups, inputName,
						compSol == null ? null : compRupSet, compSol, compRups, compName,
						distAzCalc, 0d, jumpDist, resourcesDir);
				table.addColumn("![Plausibility Filter]("+resourcesDir.getName()+"/"+plotFile.getName()+")");
			}
		}
		lines.addAll(table.build());
		lines.add("");
		
		// now azimuths
		List<RakeType> rakeTypes = new ArrayList<>();
		rakeTypes.add(null);
		for (RakeType type : RakeType.values())
			rakeTypes.add(type);
		lines.add("## Jump Azimuths");
		lines.add(topLink); lines.add("");
		
		Table<RakeType, RakeType, List<Double>> inputRakeAzTable = calcJumpAzimuths(inputRups, distAzCalc);
		Table<RakeType, RakeType, List<Double>> compRakeAzTable = null;
		if (compRups != null)
			compRakeAzTable = calcJumpAzimuths(compRups, distAzCalc);
		
		for (RakeType sourceType : rakeTypes) {
			String prefix, title;
			if (sourceType == null) {
				prefix = "jump_az_any";
				title = "Jumps from Any";
				lines.add("### Jump Azimuths From Any");
			} else {
				prefix = "jump_az_"+sourceType.prefix;
				title = "Jumps from "+sourceType.name;
				lines.add("### Jump Azimuths From "+sourceType.name);
			}
			
			System.out.println("Plotting "+title);

			lines.add(topLink); lines.add("");
			
			table = MarkdownUtils.tableBuilder();
			if (compRups != null)
				table.addLine(inputName, compName);
			
			table.initNewLine();
			File plotFile = plotJumpAzimuths(sourceType, rakeTypes, inputRakeAzTable,
					resourcesDir, prefix, title);
			table.addColumn("!["+title+"](resources/"+plotFile.getName()+")");
			if (compRups != null) {
				plotFile = plotJumpAzimuths(sourceType, rakeTypes, compRakeAzTable,
						resourcesDir, prefix+"_comp", title);
				table.addColumn("!["+title+"](resources/"+plotFile.getName()+")");
			}
			table.finalizeLine();
			lines.addAll(table.build());
			lines.add("");
			
			table = MarkdownUtils.tableBuilder();
			table.initNewLine();
			
			for (RakeType destType : rakeTypes) {
				String myPrefix = prefix+"_";
				String myTitle = title+" to ";
				if (destType == null) {
					myPrefix += "any";
					myTitle += "Any";
				} else {
					myPrefix += destType.prefix;
					myTitle += destType.name;
				}
				
				plotFile = plotJumpAzimuthsRadial(sourceType, destType, inputRakeAzTable,
						resourcesDir, myPrefix, myTitle);
				table.addColumn("!["+title+"](resources/"+plotFile.getName()+")");
			}
			table.finalizeLine();
			lines.addAll(table.wrap(3, 0).build());
			lines.add("");
		}
		
		lines.add("## Biasi & Wesnousky (2016,2017) Comparisons");
		lines.add(topLink); lines.add("");
		
		lines.add("### Jump Distance Comparisons");
		lines.add(topLink); lines.add("");
		String line = "These plots express the chances of taking an available jump of a given distance "
				+ "between two faults of the same type (SS, Normal, Reverse). Passing ratios give the "
				+ "ratio of times a jump was taken to the number of times a rupture ended without taking "
				+ "an available jump of that distance.";
		if (inputSol == null)
			line += " NOTE: Only as-discretized rates are included, as we don't have a fault system solution.";
		else
			line += " Both as-discretized and rate-weighted probabilities are plotted (in separate plots).";
		lines.add(line);
		lines.add("");
		table = MarkdownUtils.tableBuilder();
		ClusterConnectionStrategy connStrat;
		if (inputConfig == null)
			connStrat = new DistCutoffClosestSectClusterConnectionStrategy(
					subSects, distAzCalc, 10d);
		else
			connStrat = inputConfig.getConnectionStrategy();
		System.out.println("Plotting Biasi & Wesnousky (2016) jump distance comparisons");
		for (RakeType type : RakeType.values()) {
			table.addLine("**"+type.name+" Passing Ratio**", "**"+type.name+" Probability**");
			File[] plots = plotBiasiWesnouskyJumpDistComparison(resourcesDir,
					"bw_jump_dist_"+type.prefix+"_discr", type.name, inputRups, connStrat,
					inputSearch, null, type);
			table.addLine("![Passing Ratio](resources/"+plots[0].getName()+")",
					"![Probability](resources/"+plots[1].getName()+")");
			if (inputSol != null) {
				plots = plotBiasiWesnouskyJumpDistComparison(resourcesDir,
						"bw_jump_dist_"+type.prefix, type.name, inputRups, connStrat,
						inputSearch, inputSol, type);
				table.addLine("![Passing Ratio](resources/"+plots[0].getName()+")",
						"![Probability](resources/"+plots[1].getName()+")");
			}
		}
		lines.addAll(table.build());
		
		lines.add("### Jump Azimuth Change Comparisons");
		lines.add(topLink); lines.add("");
		line = "These plots express the chances of taking an available jump of a given azimuth change "
				+ "between two faults of the same type (SS, Normal, Reverse). Passing ratios give the "
				+ "ratio of times a jump was taken to the number of times a rupture ended without taking "
				+ "an available jump of that azimuth change.";
		if (inputSol == null)
			line += " NOTE: Only as-discretized rates are included, as we don't have a fault system solution.";
		else
			line += " Both as-discretized and rate-weighted probabilities are plotted (in separate plots).";
		lines.add(line);
		lines.add("");
		table = MarkdownUtils.tableBuilder();
		System.out.println("Plotting Biasi & Wesnousky (2017) azimuth change comparisons");
		for (RakeType type : RakeType.values()) {
			table.addLine("**"+type.name+" Passing Ratio**", "**"+type.name+" Probability**");
			File[] plots = plotBiasiWesnouskyJumpAzComparison(resourcesDir,
					"bw_jump_az_"+type.prefix+"_discr", type.name, inputRups, connStrat,
					inputSearch, null, type);
			table.addLine("![Passing Ratio](resources/"+plots[0].getName()+")",
					"![Probability](resources/"+plots[1].getName()+")");
			if (inputSol != null) {
				plots = plotBiasiWesnouskyJumpAzComparison(resourcesDir,
						"bw_jump_az_"+type.prefix, type.name, inputRups, connStrat,
						inputSearch, inputSol, type);
				table.addLine("![Passing Ratio](resources/"+plots[0].getName()+")",
						"![Probability](resources/"+plots[1].getName()+")");
			}
		}
		lines.addAll(table.build());
		
		lines.add("### Mechanism Change Comparisons");
		lines.add(topLink); lines.add("");
		line = "These plots express the probability of a rupture changing mechanism (e.g., strike-slip to "
				+ "reverse, or right-lateral to left-lateral) at least once, as a function of magnitude.";
		if (inputSol == null)
			line += " NOTE: Only as-discretized rates are included, as we don't have a fault system solution.";
		else
			line += " Both as-discretized and rate-weighted probabilities are plotted (in separate plots).";
		lines.add(line);
		lines.add("");
		table = MarkdownUtils.tableBuilder();
		if (inputSol != null)
			table.addLine("As Discretized", "Rate-Weighted");
		table.initNewLine();
		System.out.println("Plotting Biasi & Wesnousky (2017) mechanism change comparisons");
		File plot = plotBiasiWesnouskyMechChangeComparison(resourcesDir,
				"bw_mech_change_discr", "Mechanism Change Probability", inputRups, connStrat,
				inputSearch, inputRupSet, null);
		table.addColumn("![As Discretized](resources/"+plot.getName()+")");
		if (inputSol != null) {
			plot = plotBiasiWesnouskyMechChangeComparison(resourcesDir,
					"bw_mech_change", "Mechanism Change Probability", inputRups, connStrat,
					inputSearch, inputRupSet, inputSol);
			table.addColumn("![Rate-Weighted](resources/"+plot.getName()+")");
		}
		table.finalizeLine();
		lines.addAll(table.build());
		
		System.out.println("DONE building, writing markdown and HTML");
		
		// add TOC
		lines.addAll(tocIndex, MarkdownUtils.buildTOC(lines, 2));
		lines.add(tocIndex, "## Table Of Contents");
		
		// write markdown
		MarkdownUtils.writeReadmeAndHTML(lines, outputDir);
		
		if (distAzCache != null && (numAzCached < distAzCalc.getNumCachedAzimuths()
				|| numDistCached < distAzCalc.getNumCachedDistances())) {
			System.out.println("Writing dist/az cache to "+distAzCache.getAbsolutePath());
			distAzCalc.writeCacheFile(distAzCache);
		}
	}
	
	private static Options createOptions() {
		Options ops = new Options();

		Option outDirOption = new Option("od", "output-dir", true,
				"Output directory to write the report. Must supply either this or --reports-dir");
		outDirOption.setRequired(false);
		ops.addOption(outDirOption);

		Option reportsDirOption = new Option("rd", "reports-dir", true,
				"Directory where reports should be written. Individual reports will be placed in "
				+ "subdirectories created using the fault system solution file names. Must supply "
				+ "either this or --output-dir");
		reportsDirOption.setRequired(false);
		ops.addOption(reportsDirOption);

		Option rupSetOption = new Option("rs", "rupture-set", true,
				"Path to the primary rupture set being evaluated");
		rupSetOption.setRequired(true);
		ops.addOption(rupSetOption);
		
		Option nameOption = new Option("n", "name", true,
				"Name of the rupture set, if not supplied then the file name will be used");
		nameOption.setRequired(false);
		ops.addOption(nameOption);

		Option compRupSetOption = new Option("crs", "comp-rupture-set", true,
				"Optional path to an alternative rupture set for comparison");
		compRupSetOption.setRequired(false);
		ops.addOption(compRupSetOption);
		
		Option compNameOption = new Option("cn", "comp-name", true,
				"Name of the comparison rupture set, if not supplied then the file name will be used");
		compNameOption.setRequired(false);
		ops.addOption(compNameOption);
		
		Option altPlausibilityOption = new Option("ap", "alt-plausibility", true,
				"Path to a JSON file with an alternative set of plausibility filters which the rupture "
				+ "set should be tested against");
		altPlausibilityOption.setRequired(false);
		ops.addOption(altPlausibilityOption);
		
		Option distAzCacheOption = new Option("dac", "dist-az-cache", true,
				"Path to a distance/azimuth cache file to speed up plausibility comparisons");
		distAzCacheOption.setRequired(false);
		ops.addOption(distAzCacheOption);
		
		Option coulombCacheOption = new Option("ccd", "coulomb-cache-dir", true,
				"Path to a Coulomb cache file to speed up plausibility comparisons");
		coulombCacheOption.setRequired(false);
		ops.addOption(coulombCacheOption);
		
		return ops;
	}
	
	private static void addTablePlots(TableBuilder table, File mainPlot, File compPlot,
			boolean hasComp) {
		table.initNewLine();
		if (mainPlot.exists())	
			table.addColumn("![plot]("+mainPlot.getParentFile().getName()
					+"/"+mainPlot.getName()+")");
		else
			table.addColumn("*N/A*");
		if (hasComp) {
			if (compPlot.exists())
				table.addColumn("![plot]("+compPlot.getParentFile().getName()
						+"/"+compPlot.getName()+")");
			else
				table.addColumn("*N/A*");
		}
		table.finalizeLine();
	}
	
	private static final Color MAIN_COLOR = Color.RED;
	private static final Color COMP_COLOR = Color.BLUE;
	private static final Color COMMON_COLOR = Color.GREEN;
	private static DecimalFormat twoDigits = new DecimalFormat("0.00");
	private static DecimalFormat thousands = new DecimalFormat("0");
	static {
		thousands.getDecimalFormatSymbols().setGroupingSeparator(',');
	}
	
	private static double getLength(FaultSystemRupSet rupSet, int r) {
		double[] lengths = rupSet.getLengthForAllRups();
		if (lengths == null) {
			// calculate it
			double len = 0d;
			for (FaultSection sect : rupSet.getFaultSectionDataForRupture(r))
				len += sect.getTraceLength();
			return len;
		}
		return lengths[r]*1e-3; // m => km
	}
	
	private static List<String> getBasicLines(FaultSystemRupSet rupSet) {
		List<String> lines = new ArrayList<>();
		MinMaxAveTracker magTrack = new MinMaxAveTracker();
		MinMaxAveTracker lenTrack = new MinMaxAveTracker();
		MinMaxAveTracker sectsTrack = new MinMaxAveTracker();
		for (int r=0; r<rupSet.getNumRuptures(); r++) {
			magTrack.addValue(rupSet.getMagForRup(r));
			lenTrack.addValue(getLength(rupSet, r));
			sectsTrack.addValue(rupSet.getSectionsIndicesForRup(r).size());
		}
		lines.add("* Num ruptures: "+thousands.format(rupSet.getNumRuptures()));
		lines.add("* Rupture mag range: ["+twoDigits.format(magTrack.getMin())
		+","+twoDigits.format(magTrack.getMax())+"]");
		lines.add("* Rupture length range: ["+twoDigits.format(lenTrack.getMin())
		+","+twoDigits.format(lenTrack.getMax())+"] km");
		lines.add("* Rupture sect count range: ["+(int)sectsTrack.getMin()
			+","+(int)sectsTrack.getMax()+"]");
		return lines;
	}
	
	private static List<String> getPlausibilityLines(PlausibilityConfiguration config,
			Map<Jump, Double> jumps) {
		List<String> lines = new ArrayList<>();
		
		ClusterConnectionStrategy connStrat = config.getConnectionStrategy();
		MinMaxAveTracker parentConnTrack = new MinMaxAveTracker();
		HashSet<Integer> parentIDs = new HashSet<>();
		for (FaultSection sect : connStrat.getSubSections())
			parentIDs.add(sect.getParentSectionId());
		int totNumConnections = 0;
		for (int parentID1 : parentIDs) {
			int myConnections = 0;
			for (int parentID2 : parentIDs) {
				if (parentID1 == parentID2)
					continue;
				if (connStrat.areParentSectsConnected(parentID1, parentID2))
					myConnections++;
			}
			parentConnTrack.addValue(myConnections);
			totNumConnections += myConnections;
		}
		int minConns = (int)parentConnTrack.getMin();
		int maxConns = (int)parentConnTrack.getMax();
		String avgConns = twoDigits.format(parentConnTrack.getAverage());
		totNumConnections /= 2; // remove duplicates
		lines.add("* Connection strategy: ");
		lines.add("    * Max jump dist: "+(float)connStrat.getMaxJumpDist()+" km");
		lines.add("    * Allowed parent-section connections:");
		lines.add("        * Total: "+totNumConnections);
		lines.add("        * Each: avg="+avgConns+", range=["+minConns+","+maxConns+"]");
		
		Map<Integer, Integer> actualParentCountsMap = new HashMap<>();
		for (Jump jump : jumps.keySet()) {
			int parent1 = jump.fromCluster.parentSectionID;
			if (actualParentCountsMap.containsKey(parent1))
				actualParentCountsMap.put(parent1, actualParentCountsMap.get(parent1)+1);
			else
				actualParentCountsMap.put(parent1, 1);
			int parent2 = jump.toCluster.parentSectionID;
			if (actualParentCountsMap.containsKey(parent2))
				actualParentCountsMap.put(parent2, actualParentCountsMap.get(parent2)+1);
			else
				actualParentCountsMap.put(parent2, 1);
		}
		MinMaxAveTracker actualTrack = new MinMaxAveTracker();
		for (Integer parentID : actualParentCountsMap.keySet())
			actualTrack.addValue(actualParentCountsMap.get(parentID));
		minConns = (int)actualTrack.getMin();
		maxConns = (int)actualTrack.getMax();
		avgConns = twoDigits.format(parentConnTrack.getAverage());
		lines.add("* Actual connections (after applying filters): ");
		lines.add("    * Total: "+jumps.size());
		lines.add("    * Each: avg="+avgConns+", range=["+minConns+","+maxConns+"]");
		
		lines.add("* Max num splays: "+config.getMaxNumSplays());
		lines.add("* Filters:");
		for (PlausibilityFilter filter : config.getFilters())
			lines.add("    * "+filter.getName());
		
		return lines;
	}
	
	private static FaultModels getUCERF3FM(FaultSystemRupSet rupSet) {
		if (rupSet.getNumRuptures() == 253706)
			return FaultModels.FM3_1;
		if (rupSet.getNumRuptures() == 305709)
			return FaultModels.FM3_2;
		return null;
	}
	
	private static void checkLoadCoulombCache(List<PlausibilityFilter> filters,
			File cacheDir, Map<String, List<SubSectStiffnessCalculator>> loadedCoulombCaches)
					throws IOException {
		for (PlausibilityFilter filter : filters) {
			if (filter instanceof ScalarCoulombPlausibilityFilter) {
				SubSectStiffnessCalculator stiffnessCalc =
						((ScalarCoulombPlausibilityFilter)filter).getStiffnessCalc();
				String cacheName = stiffnessCalc.getCacheFileName(StiffnessType.CFF);
				File cacheFile = new File(cacheDir, cacheName);
				if (!cacheFile.exists())
					continue;
				if (loadedCoulombCaches.containsKey(cacheName)) {
					// copy the cache over to this one, if not already set
					List<SubSectStiffnessCalculator> calcs = loadedCoulombCaches.get(cacheName);
					// it might be shared, so make sure we haven't already loaded that one
					boolean found = false;
					for (SubSectStiffnessCalculator oCalc : calcs) {
						if (oCalc == stiffnessCalc) {
							found = true;
							// it's already been populated
							break;
						}
					}
					if (!found) {
						// need to actually populate this one
						stiffnessCalc.copyCacheFrom(calcs.get(0));
						calcs.add(stiffnessCalc);
					}
				}
				if (!loadedCoulombCaches.containsKey(cacheName) && cacheFile.exists()) {
					stiffnessCalc.loadCacheFile(cacheFile, StiffnessType.CFF);
					List<SubSectStiffnessCalculator> calcs = new ArrayList<>();
					calcs.add(stiffnessCalc);
					loadedCoulombCaches.put(cacheName, calcs);
				}
			}
		}
	}
	
	private static double getSearchMaxJumpDist(PlausibilityConfiguration config) {
		if (config == null)
			return 100d;
		ClusterConnectionStrategy connStrat = config.getConnectionStrategy();
		double maxDist = connStrat.getMaxJumpDist();
		if (Double.isFinite(maxDist))
			return maxDist;
		return 100d;
	}
	
	public static void plotRuptureHistograms(File outputDir, String prefix, TableBuilder table,
			HistScalarValues inputScalars, HashSet<UniqueRupture> inputUniques, HistScalarValues compScalars,
			HashSet<UniqueRupture> compUniques) throws IOException {
		File main = plotRuptureHistogram(outputDir, prefix, inputScalars, compScalars, compUniques,
				MAIN_COLOR, false, false);
		// normal as-discretized hists
		table.initNewLine();
		table.addColumn("![hist]("+outputDir.getName()+"/"+main.getName()+")");
		if (compScalars != null) {
			File comp = plotRuptureHistogram(outputDir, prefix+"_comp", compScalars,
					inputScalars, inputUniques, COMP_COLOR, false, false);
			table.addColumn("![hist]("+outputDir.getName()+"/"+comp.getName()+")");
		}
		table.finalizeLine();
		boolean hasCompSol = compScalars != null && compScalars.sol != null;
		if (inputScalars.sol != null || hasCompSol) {
			// rate-weighted hists
			for (boolean logY : new boolean[] {false, true}) {
				table.initNewLine();
				
				String logAdd = logY ? "_log" : "";
				if (inputScalars.sol != null) {
					main = plotRuptureHistogram(outputDir, prefix+"_rates"+logAdd, inputScalars,
							hasCompSol ? compScalars : null, compUniques, MAIN_COLOR, logY, true);
					table.addColumn("![hist]("+outputDir.getName()+"/"+main.getName()+")");
				} else {
					table.addColumn("*N/A*");
				}
				if (hasCompSol) {
					File comp = plotRuptureHistogram(outputDir, prefix+"_comp_rates"+logAdd, compScalars,
							inputScalars.sol == null ? null : inputScalars, inputUniques,
									COMP_COLOR, logY, true);
					table.addColumn("![hist]("+outputDir.getName()+"/"+comp.getName()+")");
				} else {
					table.addColumn("*N/A*");
				}
				
				table.finalizeLine();
			}
		}
		
		// now cumulatives
		table.initNewLine();
		main = plotRuptureCumulatives(outputDir, prefix+"_cumulative", inputScalars, compScalars, compUniques,
				MAIN_COLOR, false);
		table.addColumn("![hist]("+outputDir.getName()+"/"+main.getName()+")");
		if (compScalars != null) {
			File comp = plotRuptureCumulatives(outputDir, prefix+"_cumulative_comp", compScalars,
					inputScalars, inputUniques, COMP_COLOR, false);
			table.addColumn("![hist]("+outputDir.getName()+"/"+comp.getName()+")");
		}
		table.finalizeLine();
	}
	
	private static class HistScalarValues {
		private final HistScalar scalar;
		private final FaultSystemRupSet rupSet;
		private final FaultSystemSolution sol;
		private final List<ClusterRupture> rups;
		private final List<Double> values;
		
		public HistScalarValues(HistScalar scalar, FaultSystemRupSet rupSet, FaultSystemSolution sol,
				List<ClusterRupture> rups, SectionDistanceAzimuthCalculator distAzCalc) {
			super();
			this.scalar = scalar;
			this.rupSet = rupSet;
			this.sol = sol;
			this.rups = rups;
			
			values = new ArrayList<>();
			System.out.println("Calculating "+scalar.name+" for "+rups.size()+" ruptures");
			for (int r=0; r<rups.size(); r++)
				values.add(scalar.getValue(r, rupSet, rups.get(r), distAzCalc));
		}
	}
	
	private static double[] example_fractiles_default =  { 0d, 0.5, 0.9, 0.95, 0.975, 0.99, 0.999, 1d };
	
	private enum HistScalar {
		LENGTH("Rupture Length", "Length (km)",
				"Total length (km) of the rupture, not including jumps or gaps.") {
			@Override
			public HistogramFunction getHistogram(MinMaxAveTracker scalarTrack) {
				HistogramFunction hist;
				if (scalarTrack.getMax() <= 100d)
					hist = HistogramFunction.getEncompassingHistogram(
							0d, 100d, 10d);
				else
					hist = HistogramFunction.getEncompassingHistogram(
							0d, scalarTrack.getMax(), 50d);
				return hist;
			}

			@Override
			public double getValue(int index, FaultSystemRupSet rupSet, ClusterRupture rup,
					SectionDistanceAzimuthCalculator distAzCalc) {
				return getLength(rupSet, index);
			}

			@Override
			public double[] getExampleRupPlotFractiles() {
				return example_fractiles_default;
			}
		},
		MAG("Rupture Magnitude", "Magnitude", "Magnitude of the rupture.") {
			@Override
			public HistogramFunction getHistogram(MinMaxAveTracker scalarTrack) {
				return HistogramFunction.getEncompassingHistogram(Math.floor(scalarTrack.getMin()),
						Math.ceil(scalarTrack.getMax())-0.1, 0.1d);
			}

			@Override
			public double getValue(int index, FaultSystemRupSet rupSet, ClusterRupture rup,
					SectionDistanceAzimuthCalculator distAzCalc) {
				return rupSet.getMagForRup(index);
			}

			@Override
			public double[] getExampleRupPlotFractiles() {
				return example_fractiles_default;
			}
		},
		SECT_COUNT("Subsection Count", "# Subsections", "Total number of subsections involved in a rupture.") {
			@Override
			public HistogramFunction getHistogram(MinMaxAveTracker scalarTrack) {
				return new HistogramFunction(1, (int)Math.max(10, scalarTrack.getMax()), 1d);
			}

			@Override
			public double getValue(int index, FaultSystemRupSet rupSet, ClusterRupture rup,
					SectionDistanceAzimuthCalculator distAzCalc) {
				return rupSet.getSectionsIndicesForRup(index).size();
			}

			@Override
			public double[] getExampleRupPlotFractiles() {
				return example_fractiles_default;
			}
		},
		CLUSTER_COUNT("Cluster Count", "# Clusters",
				"Total number of clusters (of contiguous subsections on the same parent fault section) "
				+ "a rupture.") {
			@Override
			public HistogramFunction getHistogram(MinMaxAveTracker scalarTrack) {
				return new HistogramFunction(1, (int)Math.max(2, scalarTrack.getMax()), 1d);
			}

			@Override
			public double getValue(int index, FaultSystemRupSet rupSet, ClusterRupture rup,
					SectionDistanceAzimuthCalculator distAzCalc) {
				return rup.getTotalNumClusters();
			}

			@Override
			public double[] getExampleRupPlotFractiles() {
				return example_fractiles_default;
			}
		},
		AREA("Area", "Area (km^2)", "Total area of the rupture (km^2).") {
			@Override
			public HistogramFunction getHistogram(MinMaxAveTracker scalarTrack) {
				HistogramFunction hist = HistogramFunction.getEncompassingHistogram(
							0d, scalarTrack.getMax(), 100d);
				return hist;
			}

			@Override
			public double getValue(int index, FaultSystemRupSet rupSet, ClusterRupture rup,
					SectionDistanceAzimuthCalculator distAzCalc) {
				return rupSet.getAreaForRup(index)*1e-6;
			}

			@Override
			public double[] getExampleRupPlotFractiles() {
				return example_fractiles_default;
			}
		},
		CUM_JUMP_DIST("Cumulative Jump Dist", "Cumulative Jump Distance (km)",
				"The total cumulative jump distance summed over all jumps in the rupture.") {
			@Override
			public HistogramFunction getHistogram(MinMaxAveTracker scalarTrack) {
				double delta;
				if (scalarTrack.getMax() > 20d)
					delta = 2d;
				else
					delta = 1d;
				return HistogramFunction.getEncompassingHistogram(0d, scalarTrack.getMax(), delta);
			}

			@Override
			public double getValue(int index, FaultSystemRupSet rupSet, ClusterRupture rup,
					SectionDistanceAzimuthCalculator distAzCalc) {
				double sum = 0d;
				for (Jump jump : rup.getJumpsIterable())
					sum += jump.distance;
				return sum;
			}

			@Override
			public double[] getExampleRupPlotFractiles() {
				return example_fractiles_default;
			}
		},
		IDEAL_LEN_RATIO("Ideal Length Ratio", "Ideal Length Ratio",
				"The ratio between the total length of this rupture and the 'idealized length,' which we "
				+ "define as the straight line distance between the furthest two subsections.") {
			@Override
			public HistogramFunction getHistogram(MinMaxAveTracker scalarTrack) {
				return new HistogramFunction(0.25, 10, 0.5);
			}

			@Override
			public double getValue(int index, FaultSystemRupSet rupSet, ClusterRupture rup,
					SectionDistanceAzimuthCalculator distAzCalc) {
				double idealLen = calcIdealMinLength(rupSet.getFaultSectionDataForRupture(index), distAzCalc);
				double len = LENGTH.getValue(index, rupSet, rup, distAzCalc);
				return len/idealLen;
			}

			@Override
			public double[] getExampleRupPlotFractiles() {
				return example_fractiles_default;
			}
		},
		RAKE("Rake", "Rake (degrees)",
				"The area-averaged rake for this rupture.") {
			@Override
			public HistogramFunction getHistogram(MinMaxAveTracker scalarTrack) {
				return new HistogramFunction(-175, 36, 10d);
			}

			@Override
			public double getValue(int index, FaultSystemRupSet rupSet, ClusterRupture rup,
					SectionDistanceAzimuthCalculator distAzCalc) {
				return rupSet.getAveRakeForRup(index);
			}

			@Override
			public double[] getExampleRupPlotFractiles() {
				return null;
			}
		},
		MECH_CHANGE("Mechanism Change", "# Mechanism Changes",
				"The number of times a rupture changed mechanisms, e.g., "
				+ "from right-lateral SS to left-lateral or SS to reverse.") {
			@Override
			public HistogramFunction getHistogram(MinMaxAveTracker scalarTrack) {
				int num = 1 + (int)scalarTrack.getMax();
				return new HistogramFunction(0d, num, 1d);
			}

			@Override
			public double getValue(int index, FaultSystemRupSet rupSet, ClusterRupture rup,
					SectionDistanceAzimuthCalculator distAzCalc) {
				if (rup.getTotalNumClusters() == 1)
					return 0;
				int count = 0;
				for (Jump jump : rup.getJumpsIterable())
					if (RakeType.getType(jump.fromSection.getAveRake())
							!= RakeType.getType(jump.toSection.getAveRake()))
						count++;
				return count;
			}

			@Override
			public double[] getExampleRupPlotFractiles() {
				return example_fractiles_default;
			}
		},
		BW_PROB("Biasi & Wesnousky (2016,2017) Prob", "BS '16-'17 Prob",
				"Biasi & Wesnousky (2016,2017) conditional probability of passing through each jump.") {
			@Override
			public HistogramFunction getHistogram(MinMaxAveTracker scalarTrack) {
//				return HistogramFunction.getEncompassingHistogram(0d, 1d, 0.02);
				return HistogramFunction.getEncompassingHistogram(-5, 0, 0.1);
			}
			
			public boolean isLogX() {
				return true;
			}
			
			private CumulativeProbabilityFilter filter = null;

			@Override
			public double getValue(int index, FaultSystemRupSet rupSet, ClusterRupture rup,
					SectionDistanceAzimuthCalculator distAzCalc) {
				synchronized (this) {
					if (filter == null)
						filter = new CumulativeProbabilityFilter(1e-10f,
								CumulativeProbabilityFilter.getPrefferedBWCalcs(distAzCalc));
				}
				return filter.getValue(rup).doubleValue();
			}

			@Override
			public double[] getExampleRupPlotFractiles() {
				return new double[] { 1d, 0.5, 0.1, 0.05, 0.025, 0.01, 0.001, 0 };
			}
		};
		
		private String name;
		private String xAxisLabel;
		private String description;

		private HistScalar(String name, String xAxisLabel, String description) {
			this.name = name;
			this.xAxisLabel = xAxisLabel;
			this.description = description;
		}
		
		public boolean isLogX() {
			return false;
		}
		
		public abstract HistogramFunction getHistogram(MinMaxAveTracker scalarTrack);
		
		public abstract double getValue(int index, FaultSystemRupSet rupSet, ClusterRupture rup,
				SectionDistanceAzimuthCalculator distAzCalc);
		
		public abstract double[] getExampleRupPlotFractiles();
	}
	
	public static File plotRuptureHistogram(File outputDir, String prefix,
			HistScalarValues scalarVals, HistScalarValues compScalarVals, HashSet<UniqueRupture> compUniques, Color color,
			boolean logY, boolean rateWeighted) throws IOException {
		List<Integer> includeIndexes = new ArrayList<>();
		for (int r=0; r<scalarVals.rupSet.getNumRuptures(); r++)
			includeIndexes.add(r);
		return plotRuptureHistogram(outputDir, prefix, scalarVals, includeIndexes,
				compScalarVals, compUniques, color, logY, rateWeighted);
	}
	
	public static File plotRuptureHistogram(File outputDir, String prefix,
			HistScalarValues scalarVals, Collection<Integer> includeIndexes, HistScalarValues compScalarVals,
			HashSet<UniqueRupture> compUniques, Color color, boolean logY, boolean rateWeighted)
					throws IOException {
		List<Integer> indexesList = includeIndexes instanceof List<?> ?
				(List<Integer>)includeIndexes : new ArrayList<>(includeIndexes);
		MinMaxAveTracker track = new MinMaxAveTracker();
		for (int r : indexesList)
			track.addValue(scalarVals.values.get(r)); 
		if (compScalarVals != null) {
			// used only for bounds
			for (double scalar : compScalarVals.values)
				track.addValue(scalar);
		}
		HistScalar histScalar = scalarVals.scalar;
		HistogramFunction hist = histScalar.getHistogram(track);
		boolean logX = histScalar.isLogX();
		HistogramFunction commonHist = null;
		if (compUniques != null)
			commonHist = new HistogramFunction(hist.getMinX(), hist.getMaxX(), hist.size());
		HistogramFunction compHist = null;
		if (compScalarVals != null) {
			compHist = new HistogramFunction(hist.getMinX(), hist.getMaxX(), hist.size());
			for (int i=0; i<compScalarVals.values.size(); i++) {
				double scalar = compScalarVals.values.get(i);
				double y = rateWeighted ? compScalarVals.sol.getRateForRup(i) : 1d;
				int index;
				if (logX)
					index = scalar <= 0 ? 0 : compHist.getClosestXIndex(Math.log10(scalar));
				else
					index = compHist.getClosestXIndex(scalar);
				compHist.add(index, y);
			}
		}
		
		for (int i=0; i<indexesList.size(); i++) {
			int rupIndex = indexesList.get(i);
			double scalar = scalarVals.values.get(i);
			double y = rateWeighted ? scalarVals.sol.getRateForRup(rupIndex) : 1;
			int index;
			if (logX)
				index = scalar <= 0 ? 0 : hist.getClosestXIndex(Math.log10(scalar));
			else
				index = hist.getClosestXIndex(scalar);
			hist.add(index, y);
			if (compUniques != null && compUniques.contains(scalarVals.rups.get(rupIndex).unique))
				commonHist.add(index, y);
		}
		
		List<DiscretizedFunc> funcs = new ArrayList<>();
		List<PlotCurveCharacterstics> chars = new ArrayList<>();
		
		if (logX) {
			ArbitrarilyDiscretizedFunc linearHist = new ArbitrarilyDiscretizedFunc();
			for (Point2D pt : hist)
				linearHist.set(Math.pow(10, pt.getX()), pt.getY());
			
			linearHist.setName("Unique");
			funcs.add(linearHist);
			chars.add(new PlotCurveCharacterstics(PlotLineType.HISTOGRAM, 1f, color));
			
			if (commonHist != null) {
				linearHist = new ArbitrarilyDiscretizedFunc();
				for (Point2D pt : commonHist)
					linearHist.set(Math.pow(10, pt.getX()), pt.getY());
				linearHist.setName("Common To Both");
				funcs.add(linearHist);
				chars.add(new PlotCurveCharacterstics(PlotLineType.HISTOGRAM, 1f, COMMON_COLOR));
			}
		} else {
			hist.setName("Unique");
			funcs.add(hist);
			chars.add(new PlotCurveCharacterstics(PlotLineType.HISTOGRAM, 1f, color));
			
			if (commonHist != null) {
				commonHist.setName("Common To Both");
				funcs.add(commonHist);
				chars.add(new PlotCurveCharacterstics(PlotLineType.HISTOGRAM, 1f, COMMON_COLOR));
			}
		}
		
		String title = histScalar.name+" Histogram";
		String xAxisLabel = histScalar.xAxisLabel;
		String yAxisLabel = rateWeighted ? "Annual Rate" : "Count";
		
		PlotSpec spec = new PlotSpec(funcs, chars, title, xAxisLabel, yAxisLabel);
		spec.setLegendVisible(compUniques != null);
		
		Range xRange;
		if (logX)
			xRange = new Range(Math.pow(10, hist.getMinX() - 0.5*hist.getDelta()),
					Math.pow(10, hist.getMaxX() + 0.5*hist.getDelta()));
		else
			xRange = new Range(hist.getMinX() - 0.5*hist.getDelta(),
					hist.getMaxX() + 0.5*hist.getDelta());
		
		HeadlessGraphPanel gp = new HeadlessGraphPanel();
		gp.setBackgroundColor(Color.WHITE);
		gp.setTickLabelFontSize(18);
		gp.setAxisLabelFontSize(20);
		gp.setPlotLabelFontSize(21);
		gp.setLegendFontSize(22);
		
		Range yRange;
		if (logY) {
			double minY = Double.POSITIVE_INFINITY;
			double maxY = 0d;
			for (DiscretizedFunc func : funcs) {
				for (Point2D pt : func) {
					double y = pt.getY();
					if (y > 0) {
						minY = Math.min(minY, y);
						maxY = Math.max(maxY, y);
					}
				}
			}
			if (compHist != null) {
				for (Point2D pt : compHist) {
					double y = pt.getY();
					if (y > 0) {
						minY = Math.min(minY, y);
						maxY = Math.max(maxY, y);
					}
				}
			}
			yRange = new Range(Math.pow(10, Math.floor(Math.log10(minY))),
					Math.pow(10, Math.ceil(Math.log10(maxY))));
		} else {
			double maxY = hist.getMaxY();
			if (compHist != null)
				maxY = Math.max(maxY, compHist.getMaxY());
			yRange = new Range(0, 1.05*maxY);
		}
		
		gp.drawGraphPanel(spec, logX, logY, xRange, yRange);
		gp.getChartPanel().setSize(800, 600);
		File pngFile = new File(outputDir, prefix+".png");
		File pdfFile = new File(outputDir, prefix+".pdf");
		gp.saveAsPNG(pngFile.getAbsolutePath());
		gp.saveAsPDF(pdfFile.getAbsolutePath());
		
		return pngFile;
	}
	
	private static HistogramFunction getCumulativeFractionalHist(HistogramFunction hist) {
		HistogramFunction ret = new HistogramFunction(hist.getMinX(), hist.getMaxX(), hist.size());
		double sum = 0d;
		for (int i=0; i<hist.size(); i++) {
			sum += hist.getY(i);
			ret.set(i, sum);
		}
		ret.scale(1d/sum);
		return ret;
	}
	
	public static File plotRuptureCumulatives(File outputDir, String prefix,
			HistScalarValues scalarVals, HistScalarValues compScalarVals,
			HashSet<UniqueRupture> compUniques, Color color, boolean logY)
					throws IOException {
		List<Integer> includeIndexes = new ArrayList<>();
		for (int r=0; r<scalarVals.rupSet.getNumRuptures(); r++)
			includeIndexes.add(r);
		return plotRuptureCumulatives(outputDir, prefix, scalarVals, includeIndexes, compScalarVals,
				compUniques, color, logY);
	}
	
	public static File plotRuptureCumulatives(File outputDir, String prefix,
			HistScalarValues scalarVals, Collection<Integer> includeIndexes, HistScalarValues compScalarVals,
			HashSet<UniqueRupture> compUniques, Color color, boolean logY)
					throws IOException {
		List<Integer> indexesList = includeIndexes instanceof List<?> ?
				(List<Integer>)includeIndexes : new ArrayList<>(includeIndexes);
		MinMaxAveTracker track = new MinMaxAveTracker();
		for (int r : indexesList)
			track.addValue(scalarVals.values.get(r)); 
		if (compScalarVals != null) {
			// used only for bounds
			for (double scalar : compScalarVals.values)
				track.addValue(scalar);
		}
		HistScalar histScalar = scalarVals.scalar;
		HistogramFunction hist = histScalar.getHistogram(track);
		// now super-sample it for the cumulative plot
		double origDelta = hist.getDelta();
		double origMin = hist.getMinX();
		double newDelta = origDelta/50d;
		double newMin = origMin - 0.5*origDelta + 0.5*newDelta;
		hist = new HistogramFunction(newMin, hist.size()*50, newDelta);
		boolean logX = histScalar.isLogX();
		HistogramFunction commonHist = null;
		if (compUniques != null)
			commonHist = new HistogramFunction(hist.getMinX(), hist.getMaxX(), hist.size());
		HistogramFunction rateHist = null;
		HistogramFunction commonRateHist = null;
		if (scalarVals.sol != null) {
			rateHist = new HistogramFunction(hist.getMinX(), hist.getMaxX(), hist.size());
			commonRateHist = new HistogramFunction(hist.getMinX(), hist.getMaxX(), hist.size());
		}
		
		for (int i=0; i<indexesList.size(); i++) {
			int rupIndex = indexesList.get(i);
			double scalar = scalarVals.values.get(i);
			int index;
			if (logX)
				index = scalar <= 0 ? 0 : hist.getClosestXIndex(Math.log10(scalar));
			else
				index = hist.getClosestXIndex(scalar);
			hist.add(index, 1d);
			if (rateHist != null)
				rateHist.add(index, scalarVals.sol.getRateForRup(rupIndex));
			if (compUniques != null && compUniques.contains(scalarVals.rups.get(rupIndex).unique)) {
				commonHist.add(index, 1d);
				if (commonRateHist != null)
					commonRateHist.add(index, scalarVals.sol.getRateForRup(rupIndex));
			}
		}
		
		List<DiscretizedFunc> funcs = new ArrayList<>();
		List<PlotCurveCharacterstics> chars = new ArrayList<>();
		
		hist = getCumulativeFractionalHist(hist);
		if (commonHist != null)
			commonHist = getCumulativeFractionalHist(commonHist);
		if (rateHist != null)
			rateHist = getCumulativeFractionalHist(rateHist);
		if (commonRateHist != null)
			commonRateHist = getCumulativeFractionalHist(commonRateHist);
		
		boolean[] rateWeighteds;
		if (rateHist == null)
			rateWeighteds = new boolean[] { false };
		else
			rateWeighteds = new boolean[] { false, true };
		
		for (boolean rateWeighted : rateWeighteds) {
			DiscretizedFunc myHist;
			DiscretizedFunc myCommonHist = null;
			PlotLineType plt;
			if (rateWeighted) {
				myHist = rateHist;
				myHist.setName("Rate-Weighted");
				if (commonRateHist != null) {
					myCommonHist = commonRateHist;
					myCommonHist.setName("Common");
				}
				plt = PlotLineType.DASHED;
			} else {
				myHist = hist;
				myHist.setName("As Discretized");
				if (commonHist != null) {
					myCommonHist = commonHist;
					myCommonHist.setName("Common");
				}
				plt = PlotLineType.SOLID;
			}
			if (logX) {
				ArbitrarilyDiscretizedFunc linearHist = new ArbitrarilyDiscretizedFunc();
				for (Point2D pt : myHist)
					linearHist.set(Math.pow(10, pt.getX()), pt.getY());
				
				funcs.add(linearHist);
				chars.add(new PlotCurveCharacterstics(plt, 3f, color));
				
				if (myCommonHist != null) {
					linearHist = new ArbitrarilyDiscretizedFunc();
					for (Point2D pt : myCommonHist)
						linearHist.set(Math.pow(10, pt.getX()), pt.getY());
					funcs.add(linearHist);
					chars.add(new PlotCurveCharacterstics(plt, 3f, COMMON_COLOR));
				}
			} else {
				funcs.add(myHist);
				chars.add(new PlotCurveCharacterstics(plt, 3f, color));
				
				if (myCommonHist != null) {
					funcs.add(myCommonHist);
					chars.add(new PlotCurveCharacterstics(plt, 3f, COMMON_COLOR));
				}
			}
		}
		
		String title = histScalar.name+" Cumulative Distribution";
		String xAxisLabel = histScalar.xAxisLabel;
		String yAxisLabel = "Cumulative Fraction";
		
		PlotSpec spec = new PlotSpec(funcs, chars, title, xAxisLabel, yAxisLabel);
		spec.setLegendVisible(true);
		
		Range xRange;
		if (logX)
			xRange = new Range(Math.pow(10, hist.getMinX() - 0.5*hist.getDelta()),
					Math.pow(10, hist.getMaxX() + 0.5*hist.getDelta()));
		else
			xRange = new Range(hist.getMinX() - 0.5*hist.getDelta(),
					hist.getMaxX() + 0.5*hist.getDelta());
		
		HeadlessGraphPanel gp = new HeadlessGraphPanel();
		gp.setBackgroundColor(Color.WHITE);
		gp.setTickLabelFontSize(18);
		gp.setAxisLabelFontSize(20);
		gp.setPlotLabelFontSize(21);
		gp.setLegendFontSize(22);
		
		Range yRange;
		if (logY)
			yRange = new Range(1e-8, 1);
		else
			yRange = new Range(0, 1d);
		
		gp.drawGraphPanel(spec, logX, logY, xRange, yRange);
		gp.getChartPanel().setSize(800, 600);
		File pngFile = new File(outputDir, prefix+".png");
		File pdfFile = new File(outputDir, prefix+".pdf");
		gp.saveAsPNG(pngFile.getAbsolutePath());
		gp.saveAsPDF(pdfFile.getAbsolutePath());
		
		return pngFile;
	}
	
	/*
	 * rupture plausibility testing
	 */
	
	public static RupSetPlausibilityResult testRupSetPlausibility(List<ClusterRupture> rups,
			List<PlausibilityFilter> filters, PlausibilityConfiguration config,
			RuptureConnectionSearch connSearch) {
		
		List<PlausibilityFilter> newFilters = new ArrayList<>();
		for (PlausibilityFilter filter : filters) {
			if (filter instanceof JumpAzimuthChangeFilter)
//				filter = new ErrOnCantEvalAzFilter(filter, false);
				((JumpAzimuthChangeFilter)filter).setErrOnCantEvaluate(true);
			if (filter.isDirectional(false) || filter.isDirectional(true)) {
				if (config == null) {
					if (filter instanceof ScalarValuePlausibiltyFilter<?>)
						filter = new MultiDirectionalPlausibilityFilter.Scalar(
								(ScalarValuePlausibiltyFilter<?>)filter, connSearch,
								!filter.isDirectional(false));
					else
						filter = new MultiDirectionalPlausibilityFilter(
								filter, connSearch, !filter.isDirectional(false));
				} else {
					if (filter instanceof ScalarValuePlausibiltyFilter<?>)
						filter = new MultiDirectionalPlausibilityFilter.Scalar(
								(ScalarValuePlausibiltyFilter<?>)filter, config,
								!filter.isDirectional(false));
					else
						filter = new MultiDirectionalPlausibilityFilter(
								filter, config, !filter.isDirectional(false));
				}
			}
			newFilters.add(filter);
		}
		
		List<Future<PlausibilityCalcCallable>> futures = new ArrayList<>();
		ExecutorService exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		for (int r=0; r<rups.size(); r++) {
			ClusterRupture rupture = rups.get(r);
			futures.add(exec.submit(new PlausibilityCalcCallable(newFilters, rupture, r)));
		}
		RupSetPlausibilityResult fullResults = new RupSetPlausibilityResult(filters, rups.size());
		
		System.out.println("Waiting on "+futures.size()+" plausibility calc futures...");
		for (Future<PlausibilityCalcCallable> future : futures) {
			try {
				future.get().merge(fullResults);
			} catch (Exception e) {
				throw ExceptionUtils.asRuntimeException(e);
			}
		}
		exec.shutdown();
		System.out.println("DONE with plausibility");
		
		return fullResults;
	}
	
	private static class PlausibilityCalcCallable implements Callable<PlausibilityCalcCallable> {
		
		// inputs
		private ClusterRupture rupture;
		private List<PlausibilityFilter> filters;
		private int rupIndex;
		
		// outputs
		private PlausibilityResult[] results;
		private Throwable[] exceptions;
		private Double[] scalars;

		public PlausibilityCalcCallable(List<PlausibilityFilter> filters, ClusterRupture rupture,
				int rupIndex) {
			super();
			this.filters = filters;
			this.rupture = rupture;
			this.rupIndex = rupIndex;
		}

		@Override
		public PlausibilityCalcCallable call() throws Exception {
			results = new PlausibilityResult[filters.size()];
			exceptions = new Throwable[filters.size()];
			scalars = new Double[filters.size()];
			for (int t=0; t<filters.size(); t++) {
				PlausibilityFilter filter = filters.get(t);
				try {
					results[t] = filter.apply(rupture, false);
					if (filter instanceof ScalarValuePlausibiltyFilter<?>) {
						Number scalar = ((ScalarValuePlausibiltyFilter<?>)filter)
								.getValue(rupture);
						if (scalar != null)
							scalars[t] = scalar.doubleValue();
					}
				} catch (Exception e) {
					exceptions[t] = e;
				}
			}
			return this;
		}
		
		public void merge(RupSetPlausibilityResult fullResults) {
			fullResults.addResult(results, exceptions, scalars);
		}
		
	}
	
	public static class RupSetPlausibilityResult {
		public final List<PlausibilityFilter> filters;
		public final int numRuptures;
		public int allPassCount;
		
		public final List<List<PlausibilityResult>> filterResults;
		public final List<List<Double>> scalarVals;
		public final List<Boolean> singleFailures;
		public final int[] failCounts;
		public final int[] failCanContinueCounts;
		public final int[] onlyFailCounts;
		public final int[] erredCounts;
		
		private RupSetPlausibilityResult(List<PlausibilityFilter> filters, int numRuptures) {
			this.filters = filters;
			this.numRuptures = numRuptures;
			this.allPassCount = 0;
			this.filterResults = new ArrayList<>();
			this.scalarVals = new ArrayList<>();
			for (PlausibilityFilter filter : filters) {
				filterResults.add(new ArrayList<>());
				if (filter instanceof ScalarValuePlausibiltyFilter<?>)
					scalarVals.add(new ArrayList<>());
				else
					scalarVals.add(null);
			}
			this.singleFailures = new ArrayList<>();
			failCounts = new int[filters.size()];
			failCanContinueCounts = new int[filters.size()];
			onlyFailCounts = new int[filters.size()];
			erredCounts = new int[filters.size()];
		}
		
		public RupSetPlausibilityResult(List<PlausibilityFilter> filters, int numRuptures, int allPassCount,
				List<List<PlausibilityResult>> filterResults, List<List<Double>> scalarVals,
				List<Boolean> singleFailures, int[] failCounts, int[] failCanContinueCounts, int[] onlyFailCounts,
				int[] erredCounts) {
			super();
			this.filters = filters;
			this.numRuptures = numRuptures;
			this.allPassCount = allPassCount;
			this.filterResults = filterResults;
			this.scalarVals = scalarVals;
			this.singleFailures = singleFailures;
			this.failCounts = failCounts;
			this.failCanContinueCounts = failCanContinueCounts;
			this.onlyFailCounts = onlyFailCounts;
			this.erredCounts = erredCounts;
		}
		
		public RupSetPlausibilityResult filterByMag(FaultSystemRupSet rupSet, double minMag) {
			List<Integer> matchingRups = new ArrayList<>();
			for (int r=0; r<rupSet.getNumRuptures(); r++)
				if (rupSet.getMagForRup(r) >= minMag)
					matchingRups.add(r);
			if (matchingRups.isEmpty())
				return null;
			RupSetPlausibilityResult ret = new RupSetPlausibilityResult(filters, matchingRups.size());
			Throwable fakeException = new RuntimeException("Placeholder exception");
			for (int r : matchingRups) {
				PlausibilityResult[] results = new PlausibilityResult[filters.size()];
				Throwable[] exceptions = new Throwable[filters.size()];
				Double[] scalars = new Double[filters.size()];
				for (int f=0; f<filters.size(); f++) {
					results[f] = filterResults.get(f).get(r);
					if (results[f] == null)
						exceptions[f] = fakeException;
					if (scalarVals.get(f) != null)
						scalars[f] = scalarVals.get(f).get(r);
				}
				ret.addResult(results, exceptions, scalars);
			}
			return ret;
		}
		
		private void addResult(PlausibilityResult[] results, Throwable[] exceptions, Double[] scalars) {
			Preconditions.checkState(results.length == filters.size());
			boolean allPass = true;
			int onlyFailureIndex = -1;
			for (int t=0; t<filters.size(); t++) {
				PlausibilityFilter test = filters.get(t);
				PlausibilityResult result = results[t];
				
				boolean subPass;
				if (exceptions[t] != null) {
					if (erredCounts[t] == 0 && (exceptions[t].getMessage() == null
							|| !exceptions[t].getMessage().startsWith("Placeholder"))) {
						System.err.println("First exception for "+test.getName()+":");
						exceptions[t].printStackTrace();
					}
					erredCounts[t]++;
					subPass = true; // do not fail on error
					result = null;
				} else {
					if (result == PlausibilityResult.FAIL_FUTURE_POSSIBLE)
						failCanContinueCounts[t]++;
					subPass = result.isPass();
				}
				if (!subPass && allPass) {
					// this is the first failure
					onlyFailureIndex = t;
				} else if (!subPass) {
					// failed more than 1
					onlyFailureIndex = -1;
				}
				allPass = subPass && allPass;
				if (!subPass)
					failCounts[t]++;
				
				filterResults.get(t).add(result);
				if (scalarVals.get(t) != null)
					scalarVals.get(t).add(scalars[t]);
			}
			if (allPass)
				allPassCount++;
			if (onlyFailureIndex >= 0) {
//				fullResults.onlyFailIndexes.get(onlyFailureIndex).add(rupIndex);
				onlyFailCounts[onlyFailureIndex]++;
				singleFailures.add(true);
			} else {
				singleFailures.add(false);
			}
		}
	}
	
	private static Color[] FILTER_COLORS = { Color.DARK_GRAY, new Color(102, 51, 0), Color.RED, Color.BLUE,
			Color.GREEN.darker(), Color.CYAN, Color.PINK, Color.ORANGE.darker(), Color.MAGENTA };
	
	public static File plotRupSetPlausibility(RupSetPlausibilityResult result, File outputDir,
			String prefix, String title) throws IOException {
		double dx = 1d;
		double buffer = 0.2*dx;
		double deltaEachSide = (dx - buffer)/2d;
		double maxY = 50;
		for (int i=0; i<result.filters.size(); i++) {
			int failCount = result.erredCounts[i]+result.failCounts[i];
			double percent = (100d)*failCount/(double)result.numRuptures;
			while (percent > maxY - 25d)
				maxY += 10;
		}

		Font font = new Font(Font.SANS_SERIF, Font.BOLD, 20);
		Font allFont = new Font(Font.SANS_SERIF, Font.BOLD, 26);
		
		List<PlotElement> funcs = new ArrayList<>();
		List<PlotCurveCharacterstics> chars = new ArrayList<>();
		
		funcs.add(new DefaultXY_DataSet(new double[] {0d, 1d}, new double[] {0d, 0d}));
		chars.add(new PlotCurveCharacterstics(PlotLineType.SOLID, 0f, Color.WHITE));
		
		List<XYAnnotation> anns = new ArrayList<>();
		
		double topRowY = maxY*0.95;
		double secondRowY = maxY*0.91;
		double thirdRowY = maxY*0.85;
		
		for (int i=0; i<result.filters.size(); i++) {
			double x = i*dx + 0.5*dx;
			double percentFailed = 100d*result.failCounts[i]/result.numRuptures;
			double percentOnly = 100d*result.onlyFailCounts[i]/result.numRuptures;
			double percentErred = 100d*result.erredCounts[i]/result.numRuptures;
			
			Color c = FILTER_COLORS[i % FILTER_COLORS.length];
			
			String name = result.filters.get(i).getShortName();
			
			if (percentErred > 0) {
//				funcs.add(vertLine(x, percentFailed, percentFailed + percentErred));
//				chars.add(new PlotCurveCharacterstics(PlotLineType.SOLID, thickness, Color.LIGHT_GRAY));
				anns.add(emptyBox(x-deltaEachSide, 0d, x+deltaEachSide, percentFailed + percentErred,
						PlotLineType.DASHED, Color.LIGHT_GRAY, 2f));
				name += "*";
			}
			
//			funcs.add(vertLine(x, 0, percentFailed));
//			chars.add(new PlotCurveCharacterstics(PlotLineType.SOLID, thickness, c));
			anns.add(filledBox(x-deltaEachSide, 0, x+deltaEachSide, percentFailed, c));
			
			if (percentOnly > 0) {
//				funcs.add(vertLine(x, 0, percentOnly));
//				chars.add(new PlotCurveCharacterstics(PlotLineType.SOLID, thickness, darker(c)));
				anns.add(filledBox(x-deltaEachSide, 0, x+deltaEachSide, percentOnly, darker(c)));
			}
			
			XYTextAnnotation ann = new XYTextAnnotation(name, x, i % 2 == 0 ? secondRowY : thirdRowY);
			ann.setTextAnchor(TextAnchor.TOP_CENTER);
			ann.setPaint(c);
			ann.setFont(font);
			
			anns.add(ann);
			
			ann = new XYTextAnnotation(percentDF.format(percentFailed/100d), x, percentFailed+0.6);
			ann.setTextAnchor(TextAnchor.BOTTOM_CENTER);
			ann.setPaint(Color.BLACK);
			ann.setFont(font);
			
			anns.add(ann);
		}
		
		Range xRange = new Range(-0.30*dx, (result.filters.size()+0.15)*dx + 0.15*dx);
		
		XYTextAnnotation ann = new XYTextAnnotation(
				percentDF.format((double)result.allPassCount/result.numRuptures)+" passed all",
				xRange.getCentralValue(), topRowY);
		ann.setTextAnchor(TextAnchor.CENTER);
		ann.setPaint(Color.BLACK);
		ann.setFont(allFont);
		
		anns.add(ann);
		
		PlotSpec spec = new PlotSpec(funcs, chars, title, " ", "Percent Failed");
		spec.setPlotAnnotations(anns);
		
		HeadlessGraphPanel gp = new HeadlessGraphPanel();
		gp.setBackgroundColor(Color.WHITE);
		gp.setTickLabelFontSize(18);
		gp.setAxisLabelFontSize(20);
		gp.setPlotLabelFontSize(21);
		
		gp.drawGraphPanel(spec, false, false, xRange, new Range(0, maxY));
		gp.getXAxis().setTickLabelsVisible(false);
//		gp.getXAxis().setvisi
		gp.getChartPanel().setSize(1200, 600);
		File pngFile = new File(outputDir, prefix+".png");
		File pdfFile = new File(outputDir, prefix+".pdf");
		gp.saveAsPNG(pngFile.getAbsolutePath());
		gp.saveAsPDF(pdfFile.getAbsolutePath());
		return pngFile;
	}
	
	private static double[] plausibility_min_mags = { 6.5d, 7d, 7.5d, 8d };
	
	private static TableBuilder getMagPlausibilityTable(FaultSystemRupSet rupSet, RupSetPlausibilityResult fullResult,
			File resourcesDir, String prefix) throws IOException {
		TableBuilder table = MarkdownUtils.tableBuilder().initNewLine();
		
		for (double minMag : plausibility_min_mags) {
			RupSetPlausibilityResult result = fullResult.filterByMag(rupSet, minMag);
			if (result == null)
				continue;
			String magPrefix = prefix+"_m"+(float)minMag;
			String title = "M≥"+(float)minMag+" Comparison";
			File file = plotRupSetPlausibility(result, resourcesDir, magPrefix, title);
			table.addColumn("![M>="+(float)minMag+"]("+resourcesDir.getName()+"/"+file.getName()+")");
		}
		table.finalizeLine();
		
		return table;
	}
	
	private static Color darker(Color c) {
		int r = c.getRed();
		int g = c.getGreen();
		int b = c.getBlue();
//		r += (255-r)/2;
//		g += (255-g)/2;
//		b += (255-b)/2;
		r /= 2;
		g /= 2;
		b /= 2;
		return new Color(r, g, b);
	}
	
	private static DefaultXY_DataSet vertLine(double x, double y0, double y1) {
		DefaultXY_DataSet line = new DefaultXY_DataSet();
		line.set(x, y0);
		line.set(x, y1);
		return line;
	}
	
	private static XYBoxAnnotation filledBox(double x0, double y0, double x1, double y1, Color c) {
		XYBoxAnnotation ann = new XYBoxAnnotation(x0, y0, x1, y1, null, null, c);
		return ann;
	}
	
	private static XYBoxAnnotation emptyBox(double x0, double y0, double x1, double y1,
			PlotLineType lineType, Color c, float thickness) {
		Stroke stroke = lineType.buildStroke(thickness);
		XYBoxAnnotation ann = new XYBoxAnnotation(x0, y0, x1, y1, stroke, c, null);
		return ann;
	}
	
	public static TableBuilder getRupSetPlausibilityTable(RupSetPlausibilityResult result) {
		TableBuilder table = MarkdownUtils.tableBuilder();
		table.addLine("Filter", "Failed", "Only Failure", "Erred");
		for (int t=0; t<result.filters.size(); t++) {
			table.initNewLine();
			table.addColumn("**"+result.filters.get(t).getName()+"**");
			table.addColumn(countStats(result.failCounts[t], result.numRuptures));
			table.addColumn(countStats(result.onlyFailCounts[t], result.numRuptures));
			table.addColumn(countStats(result.erredCounts[t], result.numRuptures));
			table.finalizeLine();
		}
		return table;
	}
	
	public static List<String> getRupSetPlausibilityDetailLines(RupSetPlausibilityResult result, boolean compRups,
			FaultSystemRupSet rupSet, List<ClusterRupture> rups, int maxNumToPlot, File resourcesDir,
			String heading, String topLink, RuptureConnectionSearch connSearch, List<HistScalarValues> scalarVals)
					throws IOException {
		List<String> lines = new ArrayList<>();
		
		File rupHtmlDir = new File(resourcesDir.getParentFile(), "rupture_pages");
		Preconditions.checkState(rupHtmlDir.exists() || rupHtmlDir.mkdir());
		
		for (int i = 0; i < result.filters.size(); i++) {
			if (result.failCounts[i] == 0 && result.erredCounts[i] == 0)
				continue;
			PlausibilityFilter filter = result.filters.get(i);
			
			lines.add(heading+" "+filter.getName());
			lines.add(topLink); lines.add("");
			
			String filterPrefix = filter.getShortName().replaceAll("\\W+", "");
			if (compRups)
				filterPrefix += "_compRups";
			Color color = compRups ? COMP_COLOR : MAIN_COLOR;
			
			HashSet<Integer> failIndexes = new HashSet<>();
			HashSet<Integer> errIndexes = new HashSet<>();
			
			for (int r=0; r<result.numRuptures; r++) {
				PlausibilityResult res = result.filterResults.get(i).get(r);
				if (res == null)
					errIndexes.add(r);
				else if (!res.isPass())
					failIndexes.add(r);
			}
			
			HashSet<Integer> combIndexes = new HashSet<>(failIndexes);
			combIndexes.addAll(errIndexes);
			
			if (scalarVals != null) {
				TableBuilder table = MarkdownUtils.tableBuilder();
				table.initNewLine();
				for (HistScalarValues vals : scalarVals) {
					HistScalar scalar = vals.scalar;
					String prefix = "filter_hist_"+filterPrefix+"_"+scalar.name();
					File plot = plotRuptureHistogram(resourcesDir, prefix, vals, combIndexes,
							null, null, color, false, false);
					table.addColumn("!["+scalar.name+"]("+resourcesDir.getName()+"/"+plot.getName()+")");
				}
				table.finalizeLine();
				lines.add("Distributions of ruptures which failed ("+result.failCounts[i]+") or erred ("
						+result.erredCounts[i]+"):");
				lines.add("");
				lines.addAll(table.wrap(5, 0).build());
				lines.add("");
			}
			
			if (result.failCounts[i] > 0 && filter instanceof ScalarValuePlausibiltyFilter<?>) {
				System.out.println(filter.getName()+" is scalar");
				ScalarValuePlausibiltyFilter<?> scaleFilter = (ScalarValuePlausibiltyFilter<?>)filter;
				com.google.common.collect.Range<?> range = scaleFilter.getAcceptableRange();
				Double lower = null;
				Double upper = null;
				if (range != null) {
					if (range.hasLowerBound())
						lower = ((Number)range.lowerEndpoint()).doubleValue();
					if (range.hasUpperBound())
						upper = ((Number)range.upperEndpoint()).doubleValue();
				}
				TableBuilder table = MarkdownUtils.tableBuilder();
				table.addLine("Fail Scalar Distribution", "Pass Scalar Distribution");
				table.initNewLine();
				for (boolean passes : new boolean[] { false, true }) {
					MinMaxAveTracker track = new MinMaxAveTracker();
					List<Double> scalars = new ArrayList<>();
					
					for (int r=0; r<result.numRuptures; r++) {
						PlausibilityResult res = result.filterResults.get(i).get(r);
						if (res == null || res.isPass() != passes)
							continue;
						Double scalar = result.scalarVals.get(i).get(r);
//						if (filter instanceof ClusterPathCoulombCompatibilityFilter && passes
//								&& scalar != null && scalar < 0d) {
//							System.out.println("Debugging weirdness...");
//							ClusterRupture rupture = rups.get(r);
//							System.out.println("Rupture: "+rupture);
//							System.out.println("Stored result: "+res);
//							System.out.println("Calculating now...");
//							System.out.println("Result: "+filter.apply(rupture, true));
//							System.out.println("Multi Result: "+new MultiDirectionalPlausibilityFilter.Scalar<>(
//									(ClusterPathCoulombCompatibilityFilter)filter, connSearch, false).apply(rupture, true));
//							System.out.println("Stored scalar: "+scalar);
//							System.out.println("Now scalar: "
//									+((ClusterPathCoulombCompatibilityFilter)filter).getValue(rupture));
//							System.exit(0);
//						}
//						ClusterRupture rupture = rups.get(r);
//						
//						// TODO
//						Number scalar = scaleFilter.getValue(rupture);
//						boolean D = passes && scalar != null && scalar.doubleValue() < 0
//								&& filter instanceof ClusterPathCoulombCompatibilityFilter;
//						if (D) System.out.println("DEBUG: initial scalar: "+scalar+", result="+res);
//						if (filter.isDirectional(!rupture.splays.isEmpty()) && range != null) {
//							// see if there's a better version available
//							for (ClusterRupture alt : rupture.getInversions(connSearch)) {
//								Number altScalar = scaleFilter.getValue(alt);
//								boolean better = isScalarBetter(lower, upper, altScalar, scalar);
//								if (D) System.out.println("\tDEBUG: alt scalar: "+altScalar+", better ? "+better);
//								if (altScalar != null && better)
//									// this one is better
//									scalar = altScalar;
//							}
//						}
//						if (D) System.out.println("\tDEBUG: final scalar: "+scalar);
						if (scalar != null && Double.isFinite(scalar.doubleValue())) {
							scalars.add(scalar);
							track.addValue(scalar);
						}
					}
					
					System.out.println("Have "+scalars.size()+" scalars, passes="+passes);
					if (scalars.size() > 0) {
						DiscretizedFunc hist;
						boolean xLog;
						boolean xNegFlip;
						Range xRange;
						String xAxisLabel;
						if (scaleFilter.getScalarName() == null) {
							xAxisLabel = "Scalar Value";
						} else {
							xAxisLabel = scaleFilter.getScalarName();
							if (scaleFilter.getScalarUnits() != null)
								xAxisLabel += " ("+scaleFilter.getScalarUnits()+")";
						}
						System.out.println("tracker: "+track);
						
						if (filter instanceof ScalarCoulombPlausibilityFilter
								&& lower != null && lower.floatValue() <= 0f && track.getMax() < 0d) {
							// do it in log spacing, negative
							double logMinNeg = Math.log10(-track.getMax());
							double logMaxNeg = Math.log10(-track.getMin());
							System.out.println("Flipping with track: "+track);
							System.out.println("\tlogMinNeg="+logMinNeg);
							System.out.println("\tlogMaxNeg="+logMaxNeg);
							if (logMinNeg < -8)
								logMinNeg = -8;
							else
								logMinNeg = Math.floor(logMinNeg);
							if (logMaxNeg < -1)
								logMaxNeg = -1;
							else
								logMaxNeg = Math.ceil(logMaxNeg);
							System.out.println("\tlogMinNeg="+logMinNeg);
							System.out.println("\tlogMaxNeg="+logMaxNeg);
							HistogramFunction logHist = HistogramFunction.getEncompassingHistogram(
									logMinNeg, logMaxNeg, 0.1);
							for (double scalar : scalars) {
								scalar = Math.log10(-scalar);
								logHist.add(logHist.getClosestXIndex(scalar), 1d);
							}
							hist = new ArbitrarilyDiscretizedFunc();
							for (Point2D pt : logHist)
								hist.set(Math.pow(10, pt.getX()), pt.getY());
							xLog = true;
							xNegFlip = false;
							xRange = new Range(Math.pow(10, logHist.getMinX()-0.5*logHist.getDelta()),
									Math.pow(10, logHist.getMaxX()+0.5*logHist.getDelta()));
							xAxisLabel = "-"+xAxisLabel;
						} else if (filter instanceof ScalarCoulombPlausibilityFilter
								&& lower != null && lower.floatValue() >= 0f && track.getMin() > 0d) {
							// do it in log spacing
							double logMin = Math.log10(track.getMin());
							double logMax = Math.log10(track.getMax());
							if (logMin < -8)
								logMin = -8;
							else
								logMin = Math.floor(logMin);
							if (logMax < -1)
								logMax = -1;
							else
								logMax = Math.ceil(logMax);
							System.out.println("\tlogMin="+logMin);
							System.out.println("\tlogMax="+logMax);
							HistogramFunction logHist = HistogramFunction.getEncompassingHistogram(
									logMin, logMax, 0.1);
							for (double scalar : scalars) {
								scalar = Math.log10(scalar);
								logHist.add(logHist.getClosestXIndex(scalar), 1d);
							}
							hist = new ArbitrarilyDiscretizedFunc();
							for (Point2D pt : logHist)
								hist.set(Math.pow(10, pt.getX()), pt.getY());
							xLog = true;
							xNegFlip = false;
							xRange = new Range(Math.pow(10, logHist.getMinX()-0.5*logHist.getDelta()),
									Math.pow(10, logHist.getMaxX()+0.5*logHist.getDelta()));
						} else {
							double len = track.getMax() - track.getMin();
							double delta;
							if (len > 1000)
								delta = 50;
							else if (len > 100)
								delta = 10;
							else if (len > 50)
								delta = 5;
							else if (len > 10)
								delta = 2;
							else if (len > 5)
								delta = 1;
							else if (len > 2)
								delta = 0.5;
							else if (len > 1)
								delta = 0.1;
							else if (len > 0.5)
								delta = 0.05;
							else if (len > 0.1)
								delta = 0.01;
							else
								delta = len/10d;
							
							double min = track.getMin();
							double max = track.getMax();
							if (min == max) {
								HistogramFunction myHist = new HistogramFunction(min, max, 1);
								myHist.set(0, scalars.size());
								hist = myHist;
								xLog = false;
								xNegFlip = false;
								xRange = new Range(min-0.5, max+0.5);
							} else {
								HistogramFunction myHist = HistogramFunction.getEncompassingHistogram(
										track.getMin(), track.getMax(), delta);
								for (double scalar : scalars)
									myHist.add(myHist.getClosestXIndex(scalar), 1d);
								hist = myHist;
								xLog = false;
								xNegFlip = false;
								xRange = new Range(myHist.getMinX()-0.5*myHist.getDelta(),
										myHist.getMaxX()+0.5*myHist.getDelta());
							}
							
						}
						
						List<DiscretizedFunc> funcs = new ArrayList<>();
						List<PlotCurveCharacterstics> chars = new ArrayList<>();
						
						funcs.add(hist);
						chars.add(new PlotCurveCharacterstics(PlotLineType.HISTOGRAM, 1f, color));
						
						String title = filter.getName();
						if (passes)
							title += " Passes";
						else
							title += " Failures";
						PlotSpec spec = new PlotSpec(funcs, chars, title, xAxisLabel, "Count");
						
						HeadlessGraphPanel gp = new HeadlessGraphPanel();
						gp.setTickLabelFontSize(18);
						gp.setAxisLabelFontSize(24);
						gp.setPlotLabelFontSize(24);
						gp.setBackgroundColor(Color.WHITE);
						
						gp.drawGraphPanel(spec, xLog, false, xRange, null);
						gp.getPlot().getDomainAxis().setInverted(xNegFlip);

						String prefix = "filter_hist_"+filterPrefix;
						if (passes)
							prefix += "_passed_scalars";
						else
							prefix += "_failed_scalars";
						File file = new File(resourcesDir, prefix);
						gp.getChartPanel().setSize(800, 600);
						gp.saveAsPNG(file.getAbsolutePath()+".png");
						gp.saveAsPDF(file.getAbsolutePath()+".pdf");
						
						table.addColumn("![hist]("+resourcesDir.getName()+"/"+file.getName()+".png)");
					} else {
						table.addColumn("*N/A*");
					}
				}
				table.finalizeLine();
				
				lines.add("Scalar values of ruptures");
				lines.add("");
				lines.addAll(table.build());
				lines.add("");
			}
			
			// now add examples
			for (boolean err : new boolean[] {false, true}) {
				if (!err && result.failCounts[i] == 0 ||
						err && result.erredCounts[i] == 0)
					continue;
				int shortestIndex = -1;
				int shortestNumSects = Integer.MAX_VALUE;
				int longestIndex = -1;
				int longestNumClusters = 0;
				HashSet<Integer> plotIndexes = err ? errIndexes : failIndexes;
				for (int index : plotIndexes) {
					ClusterRupture rup = rups.get(index);
					int sects = rup.getTotalNumSects();
					int clusters = rup.getTotalNumClusters();
					if (sects < shortestNumSects) {
						shortestIndex = index;
						shortestNumSects = sects;
					}
					if (clusters > longestNumClusters) {
						longestIndex = index;
						longestNumClusters = clusters;
					}
				}
				plotIndexes.remove(shortestIndex);
				plotIndexes.remove(longestIndex);
				List<Integer> plotSorted = new ArrayList<>(plotIndexes);
				Collections.sort(plotSorted);
				Collections.shuffle(plotSorted, new Random(plotIndexes.size()));
				if (plotSorted.size() > maxNumToPlot-2)
					plotSorted = plotSorted.subList(0, maxNumToPlot-2);
				for (int r=plotSorted.size(); --r>=0;) {
					int index = plotSorted.get(r);
					if (index == shortestIndex || index == longestIndex)
						plotSorted.remove(r);
				}
				plotSorted.add(0, shortestIndex);
				if (longestIndex != shortestIndex)
					plotSorted.add(longestIndex);
				
				// plot them
				TableBuilder table = MarkdownUtils.tableBuilder();
				table.initNewLine();
				String prefix = "filter_examples_"+filterPrefix;
				boolean plotAzimuths = filter.getName().toLowerCase().contains("azimuth");
				for (int rupIndex : plotSorted) {
					String rupPrefix = prefix+"_"+rupIndex;
//					connSearch.plotConnections(resourcesDir, prefix, rupIndex);
					RupCartoonGenerator.plotRupture(resourcesDir, rupPrefix, rups.get(rupIndex),
							"Rupture "+rupIndex, plotAzimuths, true);
					table.addColumn("[<img src=\"" + resourcesDir.getName() + "/" + rupPrefix + ".png\" />]"+
							"("+ generateRuptureInfoPage(rupSet, rups.get(rupIndex), rupIndex, rupHtmlDir,
									rupPrefix, result, connSearch.getDistAzCalc())+ ")");
				}
				table.finalizeLine();
				if (err)
					lines.add("Example ruptures which erred:");
				else
					lines.add("Example ruptures which failed:");
				lines.add("");
				lines.addAll(table.wrap(5, 0).build());
				lines.add("");
			}
		}
		
		return lines;
	}
	
	private static final DecimalFormat percentDF = new DecimalFormat("0.00%");
	private static String countStats(int count, int tot) {
		return count+"/"+tot+" ("+percentDF.format((double)count/(double)tot)+")";
	}
	
//	public static getPlausibility
	
	/*
	 * Rupture connections
	 */
	
	public static void plotConnectivityLines(FaultSystemRupSet rupSet, File outputDir, String prefix, String title,
			Set<Jump> connections, Color connectedColor, Region reg, int width) throws IOException {
		List<Set<Jump>> connectionsList = new ArrayList<>();
		List<Color> connectedColors = new ArrayList<>();
		List<String> connNames = new ArrayList<>();
		
		connectionsList.add(connections);
		connectedColors.add(connectedColor);
		connNames.add("Connections");
		
		plotConnectivityLines(rupSet, outputDir, prefix, title, connectionsList, connectedColors, connNames, reg, width);
	}
	
	public static void plotConnectivityLines(FaultSystemRupSet rupSet, File outputDir, String prefix, String title,
			List<Set<Jump>> connectionsList, List<Color> connectedColors, List<String> connNames,
			Region reg, int width) throws IOException {
		Color faultColor = Color.DARK_GRAY;
		Color faultOutlineColor = Color.LIGHT_GRAY;
		
		List<XY_DataSet> funcs = new ArrayList<>();
		List<PlotCurveCharacterstics> chars = new ArrayList<>();
		
		if (reg.contains(new Location(34, -118))) {
			// add ca outlines
			XY_DataSet[] outlines = PoliticalBoundariesData.loadCAOutlines();
			PlotCurveCharacterstics outlineChar = new PlotCurveCharacterstics(PlotLineType.SOLID, (float)1d, Color.GRAY);
			
			for (XY_DataSet outline : outlines) {
				funcs.add(outline);
				chars.add(outlineChar);
			}
		}
		
		List<Location> middles = new ArrayList<>();
		
		for (int s=0; s<rupSet.getNumSections(); s++) {
			FaultSection sect = rupSet.getFaultSectionData(s);
			RuptureSurface surf = sect.getFaultSurface(1d, false, false);
			
			XY_DataSet trace = new DefaultXY_DataSet();
			for (Location loc : surf.getEvenlyDiscritizedUpperEdge())
				trace.set(loc.getLongitude(), loc.getLatitude());
			
			if (sect.getAveDip() != 90d) {
				XY_DataSet outline = new DefaultXY_DataSet();
				LocationList perimeter = surf.getPerimeter();
				for (Location loc : perimeter)
					outline.set(loc.getLongitude(), loc.getLatitude());
				Location first = perimeter.first();
				outline.set(first.getLongitude(), first.getLatitude());
				
				funcs.add(0, outline);
				chars.add(0, new PlotCurveCharacterstics(PlotLineType.SOLID, 1f, faultOutlineColor));
			}
			
			middles.add(GriddedSurfaceUtils.getSurfaceMiddleLoc(surf));
			
			if (s == 0)
				trace.setName("Fault Sections");
			
			funcs.add(trace);
			chars.add(new PlotCurveCharacterstics(PlotLineType.SOLID, 1f, faultColor));
		}
		
		for (int i=0; i<connectionsList.size(); i++) {
			Set<Jump> connections = connectionsList.get(i);
			Color connectedColor = connectedColors.get(i);
			String connName = connNames.get(i);
			
			boolean first = true;
			for (Jump connection : connections) {
				DefaultXY_DataSet xy = new DefaultXY_DataSet();
				
				if (first) {
					xy.setName(connName);
					first = false;
				}
				
				Location loc1 = middles.get(connection.fromSection.getSectionId());
				Location loc2 = middles.get(connection.toSection.getSectionId());
				
				xy.set(loc1.getLongitude(), loc1.getLatitude());
				xy.set(loc2.getLongitude(), loc2.getLatitude());
				
				funcs.add(xy);
				chars.add(new PlotCurveCharacterstics(PlotLineType.SOLID, 2f, connectedColor));
			}
		}
		
		PlotSpec spec = new PlotSpec(funcs, chars, title, "Longitude", "Latitude");
		spec.setLegendVisible(true);
		
		HeadlessGraphPanel gp = new HeadlessGraphPanel();
		gp.setTickLabelFontSize(18);
		gp.setAxisLabelFontSize(24);
		gp.setPlotLabelFontSize(24);
		gp.setBackgroundColor(Color.WHITE);
		
		Range xRange = new Range(reg.getMinLon(), reg.getMaxLon());
		Range yRange = new Range(reg.getMinLat(), reg.getMaxLat());
		
		gp.drawGraphPanel(spec, false, false, xRange, yRange);
		double tick = 2d;
		TickUnits tus = new TickUnits();
		TickUnit tu = new NumberTickUnit(tick);
		tus.add(tu);
		gp.getXAxis().setStandardTickUnits(tus);
		gp.getYAxis().setStandardTickUnits(tus);
		
		File file = new File(outputDir, prefix);
		double aspectRatio = yRange.getLength() / xRange.getLength();
		gp.getChartPanel().setSize(width, 200 + (int)((width-200d)*aspectRatio));
		gp.saveAsPNG(file.getAbsolutePath()+".png");
		gp.saveAsPDF(file.getAbsolutePath()+".pdf");
	}
	
	public static void plotConnectivityHistogram(File outputDir, String prefix, String title,
			Map<Jump, Double> connections, Map<Jump, Double> uniqueConnections,
			double maxDist, Color connectedColor, boolean rateWeighted, boolean yLog)
					throws IOException {
		double delta = 1d;
//		if (maxDist > 90)
//			delta = 5d;
//		else if (maxDist > 40)
//			delta = 2;
//		else if (maxDist > 20)
//			delta = 1d;
//		else
//			delta = 0.5d;
		
		HistogramFunction hist = HistogramFunction.getEncompassingHistogram(0d, maxDist, delta);
		hist.setName("All Connections");
		HistogramFunction uniqueHist = null;
		if (uniqueConnections != null) {
			uniqueHist = HistogramFunction.getEncompassingHistogram(0d, maxDist, delta);
			uniqueHist.setName("Unique To Model");
		}
		
		double myMax = 0d;
		double mean = 0d;
		double sumWeights = 0d;
		double meanAbove = 0d;
		double sumWeightsAbove = 0d;
		
		for (Jump pair : connections.keySet()) {
			double dist = pair.distance;
			double weight = rateWeighted ? connections.get(pair) : 1d;
			
			myMax = Math.max(myMax, dist);
			mean += dist*weight;
			sumWeights += weight;
			if (dist >= 0.1) {
				meanAbove += dist*weight;
				sumWeightsAbove += weight;
			}
			
			int xIndex = hist.getClosestXIndex(dist);
			hist.add(xIndex, weight);
			if (uniqueConnections != null && uniqueConnections.containsKey(pair))
				uniqueHist.add(xIndex, weight);
		}

		mean /= sumWeights;
		meanAbove /= sumWeightsAbove;
		
		List<XY_DataSet> funcs = new ArrayList<>();
		List<PlotCurveCharacterstics> chars = new ArrayList<>();
		
		Color uniqueColor = new Color(connectedColor.getRed()/4,
				connectedColor.getGreen()/4, connectedColor.getBlue()/4);
		
		funcs.add(hist);
		chars.add(new PlotCurveCharacterstics(PlotLineType.HISTOGRAM, 1f, connectedColor));
		
		if (uniqueConnections != null) {
			funcs.add(uniqueHist);
			chars.add(new PlotCurveCharacterstics(PlotLineType.HISTOGRAM, 1f, uniqueColor));
		}
		
		String yAxisLabel = rateWeighted ? "Annual Rate" : "Count";
		
		PlotSpec spec = new PlotSpec(funcs, chars, title, "Jump Distance (km)", yAxisLabel);
		spec.setLegendVisible(true);
		
		Range xRange = new Range(0d, maxDist);
		Range yRange;
		if (yLog) {
//			double minNonZero = Double.POSITIVE_INFINITY;
//			for (Point2D pt : hist)
//				if (pt.getY() > 0)
//					minNonZero = Math.min(minNonZero, pt.getY());
//			double minY = Math.pow(10, Math.floor(Math.log10(minNonZero)));
//			if (!Double.isFinite(minY) || minY < 1e-8)
//				minY = 1e-8;
//			double maxY = Math.max(1e-1, Math.pow(10, Math.ceil(Math.log10(hist.getMaxY()))));
//			yRange = new Range(minY, maxY);
			yRange = new Range(1e-6, 1e1);
		} else {
			yRange = new Range(0d, 1.05*hist.getMaxY());
		}
		
		DecimalFormat distDF = new DecimalFormat("0.0");
		double annX = 0.975*maxDist;
		Font annFont = new Font(Font.SANS_SERIF, Font.BOLD, 20);
		
		double annYScalar = 0.975;
		double annYDelta = 0.05;
		
		double logMinY = Math.log10(yRange.getLowerBound());
		double logMaxY = Math.log10(yRange.getUpperBound());
		double logDeltaY = logMaxY - logMinY;
		
		double annY;
		if (yLog)
			annY = Math.pow(10, logMinY + logDeltaY*annYScalar);
		else
			annY = annYScalar*yRange.getUpperBound();
		XYTextAnnotation maxAnn = new XYTextAnnotation(
				"Max: "+distDF.format(myMax), annX, annY);
		maxAnn.setFont(annFont);
		maxAnn.setTextAnchor(TextAnchor.TOP_RIGHT);
		spec.addPlotAnnotation(maxAnn);
		
		annYScalar -= annYDelta;
		if (yLog)
			annY = Math.pow(10, logMinY + logDeltaY*annYScalar);
		else
			annY = annYScalar*yRange.getUpperBound();
		XYTextAnnotation meanAnn = new XYTextAnnotation(
				"Mean: "+distDF.format(mean), annX, annY);
		meanAnn.setFont(annFont);
		meanAnn.setTextAnchor(TextAnchor.TOP_RIGHT);
		spec.addPlotAnnotation(meanAnn);
		
		annYScalar -= annYDelta;
		if (yLog)
			annY = Math.pow(10, logMinY + logDeltaY*annYScalar);
		else
			annY = annYScalar*yRange.getUpperBound();
		if (rateWeighted) {
			XYTextAnnotation rateAnn = new XYTextAnnotation(
					"Total Rate: "+distDF.format(sumWeights), annX, annY);
			rateAnn.setFont(annFont);
			rateAnn.setTextAnchor(TextAnchor.TOP_RIGHT);
			spec.addPlotAnnotation(rateAnn);
		} else {
			XYTextAnnotation countAnn = new XYTextAnnotation(
					"Total Count: "+(int)sumWeights, annX, annY);
			countAnn.setFont(annFont);
			countAnn.setTextAnchor(TextAnchor.TOP_RIGHT);
			spec.addPlotAnnotation(countAnn);
		}
		
		annYScalar -= annYDelta;
		if (yLog)
			annY = Math.pow(10, logMinY + logDeltaY*annYScalar);
		else
			annY = annYScalar*yRange.getUpperBound();
		XYTextAnnotation meanAboveAnn = new XYTextAnnotation(
				"Mean >0.1: "+distDF.format(meanAbove), annX, annY);
		meanAboveAnn.setFont(annFont);
		meanAboveAnn.setTextAnchor(TextAnchor.TOP_RIGHT);
		spec.addPlotAnnotation(meanAboveAnn);
		
		annYScalar -= annYDelta;
		if (yLog)
			annY = Math.pow(10, logMinY + logDeltaY*annYScalar);
		else
			annY = annYScalar*yRange.getUpperBound();
		if (rateWeighted) {
			XYTextAnnotation rateAnn = new XYTextAnnotation(
					"Total Rate >0.1: "+distDF.format(sumWeightsAbove), annX, annY);
			rateAnn.setFont(annFont);
			rateAnn.setTextAnchor(TextAnchor.TOP_RIGHT);
			spec.addPlotAnnotation(rateAnn);
		} else {
			XYTextAnnotation countAnn = new XYTextAnnotation(
					"Total Count >0.1: "+(int)sumWeightsAbove, annX, annY);
			countAnn.setFont(annFont);
			countAnn.setTextAnchor(TextAnchor.TOP_RIGHT);
			spec.addPlotAnnotation(countAnn);
		}
		
		HeadlessGraphPanel gp = new HeadlessGraphPanel();
		gp.setTickLabelFontSize(18);
		gp.setAxisLabelFontSize(24);
		gp.setPlotLabelFontSize(24);
		gp.setBackgroundColor(Color.WHITE);
		
		gp.drawGraphPanel(spec, false, yLog, xRange, yRange);
		
		File file = new File(outputDir, prefix);
		gp.getChartPanel().setSize(800, 650);
		gp.saveAsPDF(file.getAbsolutePath()+".pdf");
		gp.saveAsPNG(file.getAbsolutePath()+".png");
	}
	
	public static Map<Jump, Double> getJumps(FaultSystemSolution sol, List<ClusterRupture> ruptures,
			Map<Jump, List<Integer>> jumpToRupsMap) {
		Map<Jump, Double> jumpRateMap = new HashMap<>();
		for (int r=0; r<ruptures.size(); r++) {
			double rate = sol == null ? 1d : sol.getRateForRup(r);
			ClusterRupture rupture = ruptures.get(r);
			for (Jump jump : rupture.getJumpsIterable()) {
				if (jump.fromSection.getSectionId() > jump.toSection.getSectionId())
					jump = jump.reverse();
				Double prevRate = jumpRateMap.get(jump);
				if (prevRate == null)
					prevRate = 0d;
				jumpRateMap.put(jump, prevRate + rate);
				if (jumpToRupsMap != null) {
					List<Integer> prevRups = jumpToRupsMap.get(jump);
					if (prevRups == null) {
						prevRups = new ArrayList<>();
						jumpToRupsMap.put(jump, prevRups);
					}
					prevRups.add(r);
				}
			}
		}
		return jumpRateMap;
	}

	public static TableBuilder plotConnRupExamples(RuptureConnectionSearch search, FaultSystemRupSet rupSet,  Set<Jump> pairings,
			Map<Jump, List<Integer>> pairRupsMap, int maxRups, int maxCols,
			File resourcesDir, String prefix) throws IOException {
		List<Jump> sortedPairings = new ArrayList<>(pairings);
		Collections.sort(sortedPairings, Jump.id_comparator);
		
		Random r = new Random(sortedPairings.size()*maxRups);
		Collections.shuffle(sortedPairings, r);
		
		int possibleRups = 0;
		for (Jump pair : pairings)
			possibleRups += pairRupsMap.get(pair).size();
		if (possibleRups < maxRups)
			maxRups = possibleRups;
		if (maxRups == 0)
			return null;
		
		int indInPairing = 0;
		List<Integer> rupsToPlot = new ArrayList<>();
		while (rupsToPlot.size() < maxRups) {
			for (Jump pair : sortedPairings) {
				List<Integer> rups = pairRupsMap.get(pair);
				if (rups.size() > indInPairing) {
					rupsToPlot.add(rups.get(indInPairing));
					if (rupsToPlot.size() == maxRups)
						break;
				}
			}
			indInPairing++;
		}
		
		File rupHtmlDir = new File(resourcesDir.getParentFile(), "rupture_pages");
		Preconditions.checkState(rupHtmlDir.exists() || rupHtmlDir.mkdir());
		
		System.out.println("Plotting "+rupsToPlot+" ruptures");
		TableBuilder table = MarkdownUtils.tableBuilder();
		table.initNewLine();
		for (int rupIndex : rupsToPlot) {
			String rupPrefix = prefix+"_"+rupIndex;
			search.plotConnections(resourcesDir, rupPrefix, rupIndex, pairings, "Unique Connections");
			table.addColumn("[<img src=\"" + resourcesDir.getName() + "/" + rupPrefix + ".png\" />]"+
					"("+ generateRuptureInfoPage(rupSet, search.buildClusterRupture(rupIndex, true),
							rupIndex, rupHtmlDir, rupPrefix, null, search.getDistAzCalc())+ ")");
		}
		table.finalizeLine();
		return table.wrap(maxCols, 0);
	}
	
	public static File plotFixedJumpDist(FaultSystemRupSet inputRupSet, FaultSystemSolution inputSol,
			List<ClusterRupture> inputClusterRups, String inputName, FaultSystemRupSet compRupSet,
			FaultSystemSolution compSol, List<ClusterRupture> compClusterRups, String compName,
			SectionDistanceAzimuthCalculator distAzCalc, double minMag, float jumpDist, File outputDir)
					throws IOException {
		
		List<DiscretizedFunc> funcs = new ArrayList<>();
		List<PlotCurveCharacterstics> chars = new ArrayList<>();

		if (inputRupSet != null) {
			DiscretizedFunc func = calcJumpDistFunc(inputRupSet, inputSol, inputClusterRups, minMag, jumpDist);
			func.scale(1d/func.calcSumOfY_Vals());
			funcs.add(func);
			
			func.setName(inputName);
			chars.add(new PlotCurveCharacterstics(PlotLineType.SOLID, 3f, MAIN_COLOR));
		}
		
		if (compRupSet != null) {
			DiscretizedFunc compFunc = calcJumpDistFunc(compRupSet, compSol, compClusterRups, minMag, jumpDist);
			compFunc.scale(1d/compFunc.calcSumOfY_Vals());
			compFunc.setName(compName);
			funcs.add(compFunc);
			chars.add(new PlotCurveCharacterstics(PlotLineType.SOLID, 3f, COMP_COLOR));
		}
		
		String title;
		String xAxisLabel = "Num Jumps ≥"+(float)jumpDist+" km";
		String yAxisLabel;
		if (minMag > 0d) {
			title = "M≥"+(float)minMag+" "+(float)jumpDist+" km Jump Comparison";
		} else {
			title = (float)jumpDist+" km Jump Comparison";
		}
		Range yRange = null;
		String prefixAdd;
		if (inputSol != null || compSol != null) {
			yAxisLabel = "Fraction (Rate-Weighted)";
			yRange = new Range(0d, 1d);
			prefixAdd = "_rates";
		} else {
			yAxisLabel = "Count";
			prefixAdd = "_counts";
		}
		PlotSpec spec = new PlotSpec(funcs, chars, title, xAxisLabel, yAxisLabel);
//				"Num Jumps ≥"+(float)jumpDist+"km", "Fraction (Rate-Weighted)");
		spec.setLegendVisible(true);
		
		HeadlessGraphPanel gp = new HeadlessGraphPanel();
		gp.setBackgroundColor(Color.WHITE);
		gp.setTickLabelFontSize(18);
		gp.setAxisLabelFontSize(20);
		gp.setPlotLabelFontSize(21);
		
		String prefix = new File(outputDir, "jumps_"+(float)jumpDist+"km"+prefixAdd).getAbsolutePath();
		
		gp.drawGraphPanel(spec, false, false, null, yRange);
		TickUnits tus = new TickUnits();
		TickUnit tu = new NumberTickUnit(1d);
		tus.add(tu);
		gp.getXAxis().setStandardTickUnits(tus);
		gp.getChartPanel().setSize(1000, 500);
		gp.saveAsPNG(prefix+".png");
		gp.saveAsPDF(prefix+".pdf");
		gp.saveAsTXT(prefix+".txt");
		return new File(prefix+".png");
	}
	
	private static DiscretizedFunc calcJumpDistFunc(FaultSystemRupSet rupSet, FaultSystemSolution sol,
			List<ClusterRupture> clusterRups, double minMag, float jumpDist) {
		EvenlyDiscretizedFunc solFunc = new EvenlyDiscretizedFunc(0d, 5, 1d);

		for (int r=0; r<rupSet.getNumRuptures(); r++) {
			double mag = rupSet.getMagForRup(r);

			if (mag < minMag)
				continue;
			
			ClusterRupture rup = clusterRups.get(r);
			int jumpsOverDist = 0;
			for (Jump jump : rup.getJumpsIterable()) {
				if ((float)jump.distance > jumpDist)
					jumpsOverDist++;
			}

			double rate = sol == null ? 1d : sol.getRateForRup(r);
			
			// indexes are fine to use here since it starts at zero with a delta of one 
			if (jumpsOverDist < solFunc.size())
				solFunc.set(jumpsOverDist, solFunc.getY(jumpsOverDist) + rate);
		}
		
		return solFunc;
	}
	
	public enum RakeType {
		RIGHT_LATERAL("Right-Lateral SS", "rl", Color.RED.darker()) {
			@Override
			public boolean isMatch(double rake) {
				return (float)rake >= -180f && (float)rake <= -170f
						|| (float)rake <= 180f && (float)rake >= 170f;
			}
		},
		LEFT_LATERAL("Left-Lateral SS", "ll", Color.GREEN.darker()) {
			@Override
			public boolean isMatch(double rake) {
				return (float)rake >= -10f && (float)rake <= 10f;
			}
		},
		REVERSE("Reverse", "rev", Color.BLUE.darker()) {
			@Override
			public boolean isMatch(double rake) {
				return (float)rake >= 80f && (float)rake <= 100f;
			}
		},
		NORMAL("Normal", "norm", Color.YELLOW.darker()) {
			@Override
			public boolean isMatch(double rake) {
				return (float)rake >= -100f && (float)rake <= -80f;
			}
		},
		OBLIQUE("Oblique", "oblique", Color.MAGENTA.darker()) {
			@Override
			public boolean isMatch(double rake) {
				for (RakeType type : values())
					if (type != this && type.isMatch(rake))
						return false;
				return true;
			}
		};
		
		public final String name;
		public final String prefix;
		public final Color color;

		private RakeType(String name, String prefix, Color color) {
			this.name = name;
			this.prefix = prefix;
			this.color = color;
		}
		
		public abstract boolean isMatch(double rake);
		
		public static RakeType getType(double rake) {
			for (RakeType type : values())
				if (type != OBLIQUE && type.isMatch(rake))
					return type;
			return OBLIQUE;
		}
	}
	
	public static Table<RakeType, RakeType, List<Double>> calcJumpAzimuths(
			List<ClusterRupture> rups, SectionDistanceAzimuthCalculator distAzCalc) {
		AzimuthCalc azCalc = new JumpAzimuthChangeFilter.SimpleAzimuthCalc(distAzCalc);
		Table<RakeType, RakeType, List<Double>> ret = HashBasedTable.create();
		for (RakeType r1 : RakeType.values())
			for (RakeType r2 : RakeType.values())
				ret.put(r1, r2, new ArrayList<>());
		for (ClusterRupture rup : rups) {
			RuptureTreeNavigator navigator = rup.getTreeNavigator();
			for (Jump jump : rup.getJumpsIterable()) {
				RakeType sourceRake = null, destRake = null;
				for (RakeType type : RakeType.values()) {
					if (type.isMatch(jump.fromSection.getAveRake()))
						sourceRake = type;
					if (type.isMatch(jump.toSection.getAveRake()))
						destRake = type;
				}
				Preconditions.checkNotNull(sourceRake);
				Preconditions.checkNotNull(destRake);
				FaultSection before1 = navigator.getPredecessor(jump.fromSection);
				if (before1 == null)
					continue;
				FaultSection before2 = jump.fromSection;
				double beforeAz = azCalc.calcAzimuth(before1, before2);
				FaultSection after1 = jump.toSection;
				for (FaultSection after2 : navigator.getDescendants(after1)) {
					double afterAz = azCalc.calcAzimuth(after1, after2);
					double rawDiff = JumpAzimuthChangeFilter.getAzimuthDifference(beforeAz, afterAz);
					Preconditions.checkState(rawDiff >= -180 && rawDiff <= 180);
					double[] azDiffs;
					if ((float)before2.getAveDip() == 90f) {
						// strike slip, include both directions
						azDiffs = new double[] { rawDiff, -rawDiff };
					} else {
						// follow the aki & richards convention
						double dipDir = before2.getDipDirection();
						double dipDirDiff = JumpAzimuthChangeFilter.getAzimuthDifference(dipDir, beforeAz);
						if (dipDirDiff < 0)
							// this means that the fault dips to the right of beforeAz, we're good
							azDiffs = new double[] { rawDiff };
						else
							// this means that the fault dips to the left of beforeAz, flip it
							azDiffs = new double[] { -rawDiff };
					}
					for (double azDiff : azDiffs)
						ret.get(sourceRake, destRake).add(azDiff);
				}
			}
		}
		return ret;
	}
	
	private static Map<RakeType, List<Double>> getAzimuthsFrom (RakeType sourceRake,
			Table<RakeType, RakeType, List<Double>> azTable) {
		Map<RakeType, List<Double>> azMap;
		if (sourceRake == null) {
			azMap = new HashMap<>();
			for (RakeType type : RakeType.values())
				azMap.put(type, new ArrayList<>());
			for (RakeType source : RakeType.values()) {
				Map<RakeType, List<Double>> row = azTable.row(source);
				for (RakeType dest : row.keySet()) 
					azMap.get(dest).addAll(row.get(dest));
			}
		} else {
			azMap = azTable.row(sourceRake);
		}
		return azMap;
	}
	
	public static File plotJumpAzimuths(RakeType sourceRake, List<RakeType> destRakes,
			Table<RakeType, RakeType, List<Double>> azTable,
			File outputDir, String prefix, String title) throws IOException {
		Map<RakeType, List<Double>> azMap = getAzimuthsFrom(sourceRake, azTable);
		
		Range xRange = new Range(-180d, 180d);
		List<Range> xRanges = new ArrayList<>();
		xRanges.add(xRange);
		
		List<Range> yRanges = new ArrayList<>();
		List<PlotSpec> specs = new ArrayList<>();
		
		for (int i=0; i<destRakes.size(); i++) {
			RakeType destRake = destRakes.get(i);
			
			HistogramFunction hist = HistogramFunction.getEncompassingHistogram(-179d, 179d, 15d);
			for (RakeType oRake : azMap.keySet()) {
				if (destRake != null && destRake != oRake)
					continue;
				for (double azDiff : azMap.get(oRake)) {
					hist.add(hist.getClosestXIndex(azDiff), 1d);
				}
			}

			Color color;
			String label;
			if (destRake == null) {
				color = Color.DARK_GRAY;
				label = "Any";
			} else {
				color = destRake.color;
				label = destRake.name;
			}
			
			List<XY_DataSet> funcs = new ArrayList<>();
			List<PlotCurveCharacterstics> chars = new ArrayList<>();
			
			funcs.add(hist);
			chars.add(new PlotCurveCharacterstics(PlotLineType.HISTOGRAM, 1f, color));
			
			double maxY = Math.max(1.1*hist.getMaxY(), 1d);
			Range yRange = new Range(0d, maxY);
			
			PlotSpec spec = new PlotSpec(funcs, chars, title, "Azimuthal Difference", "Count");
			
			XYTextAnnotation ann = new XYTextAnnotation("To "+label, 175, maxY*0.975);
			ann.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
			ann.setTextAnchor(TextAnchor.TOP_RIGHT);
			spec.addPlotAnnotation(ann);
			
			specs.add(spec);
			yRanges.add(yRange);
		}
		
		HeadlessGraphPanel gp = new HeadlessGraphPanel();
		gp.setTickLabelFontSize(18);
		gp.setAxisLabelFontSize(24);
		gp.setPlotLabelFontSize(24);
		gp.setBackgroundColor(Color.WHITE);
		
		gp.drawGraphPanel(specs, false, false, xRanges, yRanges);
		
		File file = new File(outputDir, prefix+".png");
		gp.getChartPanel().setSize(700, 1000);
		gp.saveAsPNG(file.getAbsolutePath());
		return file;
	}
	
	private static double azDiffDegreesToAngleRad(double azDiff) {
		// we want zero to be up, 90 to be right, 180 to be down, -90 to be left
		// sin/cos convention is zero at the right, 90 up, 180 left, -90 down
		
		Preconditions.checkState((float)azDiff >= (float)-180f && (float)azDiff <= 180f,
				"Bad azDiff: %s", azDiff);
		// first mirror it
		azDiff *= -1;
		// now rotate 90 degrees
		azDiff += 90d;
		
		return Math.toRadians(azDiff);
	}
	
	public static File plotJumpAzimuthsRadial(RakeType sourceRake, RakeType destRake,
			Table<RakeType, RakeType, List<Double>> azTable,
			File outputDir, String prefix, String title) throws IOException {
		System.out.println("Plotting "+title);
		Map<RakeType, List<Double>> azMap = getAzimuthsFrom(sourceRake, azTable);
		
		List<XY_DataSet> funcs = new ArrayList<>();
		List<PlotCurveCharacterstics> chars = new ArrayList<>();
		
		Map<Float, List<Color>> azColorMap = new HashMap<>();
		
		HistogramFunction hist = HistogramFunction.getEncompassingHistogram(-179d, 179d, 15d);
		long totCount = 0;
		for (RakeType oRake : azMap.keySet()) {
			if (destRake != null && destRake != oRake)
				continue;
			for (double azDiff : azMap.get(oRake)) {
				hist.add(hist.getClosestXIndex(azDiff), 1d);
				
				Float azFloat = (float)azDiff;
				List<Color> colors = azColorMap.get(azFloat);
				if (colors == null) {
					colors = new ArrayList<>();
					azColorMap.put(azFloat, colors);
				}
				colors.add(oRake.color);
				totCount++;
			}
		}
		
		System.out.println("Have "+azColorMap.size()+" unique azimuths, "+totCount+" total");
//		Random r = new Random(azColorMap.keySet().size());
		double alphaEach = 0.025;
		if (totCount > 0)
			alphaEach = Math.max(alphaEach, 1d/totCount);
		for (Float azFloat : azColorMap.keySet()) {
			double sumRed = 0d;
			double sumGreen = 0d;
			double sumBlue = 0d;
			double sumAlpha = 0;
			int count = 0;
			for (Color azColor : azColorMap.get(azFloat)) {
				sumRed += azColor.getRed();
				sumGreen += azColor.getGreen();
				sumBlue += azColor.getBlue();
				if (sumAlpha < 1d)
					sumAlpha += alphaEach;
				count++;
			}
			double red = sumRed/(double)count;
			double green = sumGreen/(double)count;
			double blue = sumBlue/(double)count;
			if (red > 1d)
				red = 1d;
			if (green > 1d)
				green = 1d;
			if (blue > 1d)
				blue = 1d;
			if (sumAlpha > 1d)
				sumAlpha = 1d;
			Color color = new Color((float)red, (float)green, (float)blue, (float)sumAlpha);
//			if (destRake == null) {
//				// multipe types, choose a random color sampled from the actual colors
//				// for this azimuth
//				List<Color> colorList = azColorMap.get(azFloat);
//				color = colorList.get(r.nextInt(colorList.size()));
//			} else {
//				color = destRake.color;
//			}
			
			DefaultXY_DataSet line = new DefaultXY_DataSet();
			line.set(0d, 0d);
			double azRad = azDiffDegreesToAngleRad(azFloat);
			double x = Math.cos(azRad);
			double y = Math.sin(azRad);
			line.set(x, y);
			
			funcs.add(line);
			chars.add(new PlotCurveCharacterstics(PlotLineType.SOLID, 1f, color));
		}
		
		double dip;
		if (sourceRake == RakeType.LEFT_LATERAL || sourceRake == RakeType.RIGHT_LATERAL)
			dip = 90d;
		else if (sourceRake == RakeType.NORMAL || sourceRake == RakeType.REVERSE)
			dip = 60d;
		else
			dip = 75d;
		
		double traceLen = 0.5d;
		double lowerDepth = 0.25d;
		if (dip < 90d) {
			// add surface
			
			double horzWidth = lowerDepth/Math.tan(Math.toRadians(dip));
			DefaultXY_DataSet outline = new DefaultXY_DataSet();
			outline.set(0d, 0d);
			outline.set(horzWidth, 0d);
			outline.set(horzWidth, -traceLen);
			outline.set(0d, -traceLen);
			
			funcs.add(outline);
			chars.add(new PlotCurveCharacterstics(PlotLineType.SOLID, 3f, Color.GRAY));
		}
		
		DefaultXY_DataSet trace = new DefaultXY_DataSet();
		trace.set(0d, 0d);
		trace.set(0d, -traceLen);
		
		funcs.add(trace);
		chars.add(new PlotCurveCharacterstics(PlotLineType.SOLID, 6f, Color.BLACK));
		PlotSpec spec = new PlotSpec(funcs, chars, title, "", " ");
		
		CPT cpt = GMT_CPT_Files.BLACK_RED_YELLOW_UNIFORM.instance().reverse();
		cpt = cpt.rescale(2d*Float.MIN_VALUE, 0.25d);
		cpt.setBelowMinColor(Color.WHITE);
		double halfDelta = 0.5*hist.getDelta();
		double innerMult = 0.95;
		double outerMult = 1.05;
		double sumY = Math.max(1d, hist.calcSumOfY_Vals());
		for (int i=0; i<hist.size(); i++) {
			double centerAz = hist.getX(i);
			double startAz = azDiffDegreesToAngleRad(centerAz-halfDelta);
			double endAz = azDiffDegreesToAngleRad(centerAz+halfDelta);
			
			List<Point2D> points = new ArrayList<>();
			
			double startX = Math.cos(startAz);
			double startY = Math.sin(startAz);
			double endX = Math.cos(endAz);
			double endY = Math.sin(endAz);
			
			points.add(new Point2D.Double(innerMult*startX, innerMult*startY));
			points.add(new Point2D.Double(outerMult*startX, outerMult*startY));
			points.add(new Point2D.Double(outerMult*endX, outerMult*endY));
			points.add(new Point2D.Double(innerMult*endX, innerMult*endY));
			points.add(new Point2D.Double(innerMult*startX, innerMult*startY));
			
			double[] polygon = new double[points.size()*2];
			int cnt = 0;
			for (Point2D pt : points) {
				polygon[cnt++] = pt.getX();
				polygon[cnt++] = pt.getY();
			}
			Color color = cpt.getColor((float)(hist.getY(i)/sumY));
			
			Stroke stroke = PlotLineType.SOLID.buildStroke(2f);
			spec.addPlotAnnotation(new XYPolygonAnnotation(polygon, stroke, Color.DARK_GRAY, color));
		}
		
		PaintScaleLegend cptBar = XYZGraphPanel.getLegendForCPT(cpt, "Fraction",
				24, 18, 0.05d, RectangleEdge.BOTTOM);
		spec.addSubtitle(cptBar);
		
		Range xRange = new Range(-1.1d, 1.1d);
		Range yRange = new Range(-1.1d, 1.1d);
		
		HeadlessGraphPanel gp = new HeadlessGraphPanel();
		gp.setTickLabelFontSize(18);
		gp.setAxisLabelFontSize(24);
		gp.setPlotLabelFontSize(22);
		gp.setBackgroundColor(Color.WHITE);
		
		gp.drawGraphPanel(spec, false, false, xRange, yRange);
		
		gp.getXAxis().setTickLabelsVisible(false);
		gp.getYAxis().setTickLabelsVisible(false);
		
		File file = new File(outputDir, prefix+".png");
		gp.getChartPanel().setSize(800, 800);
		gp.saveAsPNG(file.getAbsolutePath());
		return file;
	}

	private static String generateRuptureInfoPage(FaultSystemRupSet rupSet, ClusterRupture rupture, int rupIndex,
			File outputDir, String fileNamePrefix, RupSetPlausibilityResult plausibiltyResult,
			SectionDistanceAzimuthCalculator distAzCalc) throws IOException {
		DecimalFormat format = new DecimalFormat("###,###.#");

		List<String> lines = new ArrayList<>();
		lines.add("## Rupture " + rupIndex);
		lines.add("");
		lines.add("![Rupture " + rupIndex + "](../resources/" + fileNamePrefix + ".png)");

		HashMap<Integer, Jump> jumps = new HashMap<>();
		for (Jump jump : rupture.getJumpsIterable()) {
			jumps.put(jump.fromSection.getSectionId(), jump);
		}
		
		lines.add("");
		TableBuilder table = MarkdownUtils.tableBuilder().initNewLine();
		HistScalar[] scalars = HistScalar.values();
		for (HistScalar scalar : scalars)
			table.addColumn("**"+scalar.xAxisLabel+"**");
		table.finalizeLine().initNewLine();
		for (HistScalar scalar : scalars) {
			double val = scalar.getValue(rupIndex, rupSet, rupture, distAzCalc);
			if (Math.abs(val) < 1)
				table.addColumn((float)val+"");
			else
				table.addColumn(format.format(val));
		}
		table.finalizeLine();
		lines.addAll(table.wrap(5, 0).build());

		lines.add("");
		lines.add("Text representation:");
		lines.add("");
		lines.add("```");
		lines.add(rupture.toString());
		lines.add("```");
		lines.add("");

		List<FaultSection> sections = rupSet.getFaultSectionDataForRupture(rupIndex);

		Location startLocation = sections.get(0).getFaultTrace().get(0);
		Location lastLocation = sections.get(sections.size() - 1).getFaultTrace().get(sections.get(sections.size() - 1).getFaultTrace().size() - 1);
		String location = null;
		if (startLocation.getLatitude() < lastLocation.getLatitude()) {
			location = " South ";
		} else {
			location = " North ";
		}
		if (startLocation.getLongitude() < lastLocation.getLongitude()) {
			location += " West ";
		} else {
			location += " East ";
		}
		lines.add("");
		lines.add("Fault section list. First section listed is " + location + " relative to the last section.");
		lines.add("");

		int lastParent = Integer.MIN_VALUE;
		List<String> sectionIds = new ArrayList<>();
		for (FaultSection section : sections) {
			if (lastParent != section.getParentSectionId()) {
				if (lastParent != Integer.MIN_VALUE) {
					lines.add("    * " + String.join(", ", sectionIds));
					sectionIds.clear();
				}
				lines.add("* " + section.getParentSectionName());
				lastParent = section.getParentSectionId();
			}
			if (jumps.containsKey(section.getSectionId())) {
				Jump jump = jumps.get(section.getSectionId());
				sectionIds.add("" + section.getSectionId() + " (jump to " + jump.toSection.getSectionId() + ", " + format.format(jump.distance) + "km)");
			} else {
				sectionIds.add("" + section.getSectionId());
			}
		}
		lines.add("    * " + String.join(", ", sectionIds));
		
		if (plausibiltyResult != null) {
			lines.add("");
			lines.add("### Plausibility Filters Results");
			lines.add("");
			table = MarkdownUtils.tableBuilder();
			table.addLine("Name", "Result", "Scalar Value");
			for (int f=0; f<plausibiltyResult.filters.size(); f++) {
				table.initNewLine();
				table.addColumn(plausibiltyResult.filters.get(f).getName());
				PlausibilityResult result = plausibiltyResult.filterResults.get(f).get(rupIndex);
				if (result == null)
					table.addColumn("Erred");
				else
					table.addColumn(result.toString());
				if (plausibiltyResult.scalarVals.get(f) == null) {
					table.addColumn("*N/A*");
				} else {
					Double scalar = plausibiltyResult.scalarVals.get(f).get(rupIndex);
					if (scalar == null) {
						table.addColumn("*N/A*");
					} else {
						String str = scalar.floatValue()+"";
						PlausibilityFilter filter = plausibiltyResult.filters.get(f);
						String units = ((ScalarValuePlausibiltyFilter<?>)filter).getScalarUnits();
						if (units != null)
							str += " ("+units+")";
						table.addColumn(str);
					}
				}
			}
			table.finalizeLine();
			lines.addAll(table.build());
		}
		
		MarkdownUtils.writeHTML(lines, new File(outputDir, fileNamePrefix + ".html"));
		return outputDir.getName()+"/"+fileNamePrefix + ".html";
	}
	
	private static double calcIdealMinLength(List<? extends FaultSection> subSects,
			SectionDistanceAzimuthCalculator distAzCalc) {
//		FaultSection farS1 = null;
//		FaultSection farS2 = null;
//		double maxDist = 0d;
//		for (int i=0; i<subSects.size(); i++) {
//			FaultSection s1 = subSects.get(i);
//			for (int j=i; j<subSects.size(); j++) {
//				FaultSection s2 = subSects.get(j);
//				double dist = distAzCalc.getDistance(s1, s2);
//				if (dist >= maxDist) {
//					maxDist = dist;
//					farS1 = s1;
//					farS2 = s2;
//				}
//			}
//		}
		FaultSection farS1 = subSects.get(0);
		if (subSects.size() == 1)
			return LocationUtils.horzDistance(farS1.getFaultTrace().first(), farS1.getFaultTrace().last());
		FaultSection farS2 = null;
		double maxDist = 0d;
		for (int i=1; i<subSects.size(); i++) {
			FaultSection s2 = subSects.get(i);
			double dist = distAzCalc.getDistance(farS1, s2);
			if (dist >= maxDist) {
				maxDist = dist;
				farS2 = s2;
			}
		}
		if (farS1 == farS2)
			return farS1.getTraceLength();
		maxDist = 0d;
		for (Location l1 : farS1.getFaultTrace())
			for (Location l2 : farS2.getFaultTrace())
				maxDist = Math.max(maxDist, LocationUtils.horzDistanceFast(l1, l2));
		return maxDist;
	}
	
	public static boolean plotScalarMaxMapView(FaultSystemRupSet rupSet, File outputDir, String prefix,
			String title, HistScalarValues scalarVals, HistScalarValues compScalarVals, Region reg,
			Color mainColor, boolean difference, boolean ratio) throws IOException {
		Color faultOutlineColor = Color.LIGHT_GRAY;
		
		List<XY_DataSet> funcs = new ArrayList<>();
		List<PlotCurveCharacterstics> chars = new ArrayList<>();
		
		if (reg.contains(new Location(34, -118))) {
			// add ca outlines
			XY_DataSet[] outlines = PoliticalBoundariesData.loadCAOutlines();
			PlotCurveCharacterstics outlineChar = new PlotCurveCharacterstics(PlotLineType.SOLID, (float)1d, Color.GRAY);
			
			for (XY_DataSet outline : outlines) {
				funcs.add(outline);
				chars.add(outlineChar);
			}
		}
		
		List<Double> values = new ArrayList<>();
		for (int s=0; s<rupSet.getNumSections(); s++)
			values.add(Double.NEGATIVE_INFINITY);
		for (int r=0; r<rupSet.getNumRuptures(); r++) {
			double value = scalarVals.values.get(r);
			for (int s : rupSet.getSectionsIndicesForRup(r)) {
				double prevMax = values.get(s);
				if (value > prevMax)
					values.set(s, value);
			}
		}
		MinMaxAveTracker maxTrack = new MinMaxAveTracker();
		for (double value : values)
			if (Double.isFinite(value))
				maxTrack.addValue(value);
		if (maxTrack.getNum() == 0)
			return false;
		Preconditions.checkState(compScalarVals != null || (!difference && !ratio));
		if (compScalarVals != null) {
			// for consistent ranges
			List<Double> compValues = new ArrayList<>();
			for (int s=0; s<compScalarVals.rupSet.getNumSections(); s++)
				compValues.add(Double.NEGATIVE_INFINITY);
			for (int r=0; r<compScalarVals.rupSet.getNumRuptures(); r++) {
				double value = compScalarVals.values.get(r);
				for (int s : compScalarVals.rupSet.getSectionsIndicesForRup(r)) {
					double prevMax = compValues.get(s);
					if (value > prevMax)
						compValues.set(s, value);
				}
			}
			for (double value : compValues)
				if (Double.isFinite(value))
					maxTrack.addValue(value);
			if (difference) {
				Preconditions.checkState(compValues.size() == values.size());
				for (int i=0; i<values.size(); i++)
					values.set(i, values.get(i) - compValues.get(i));
			} else if (ratio) {
				Preconditions.checkState(compValues.size() == values.size());
				for (int i=0; i<values.size(); i++)
					values.set(i, values.get(i) / compValues.get(i));
			}
		}
		
		HistScalar histScalar = scalarVals.scalar;
		
		HistogramFunction hist;
		CPT cpt;
		if (difference) {
			double maxAbsDiff = 0d;
			for (double value : values)
				if (Double.isFinite(value))
					maxAbsDiff = Math.max(maxAbsDiff, Math.abs(value));
			if (histScalar == HistScalar.MAG)
				maxAbsDiff = Math.min(maxAbsDiff, 2d);
			maxAbsDiff = Math.ceil(maxAbsDiff);
			double delta = histScalar == HistScalar.CLUSTER_COUNT ? 1 : maxAbsDiff / 10d;
			int num = 2*(int)(maxAbsDiff/delta)+1;
			hist = new HistogramFunction(-maxAbsDiff - 0.5*delta, num, delta);
			cpt = new CPT(-maxAbsDiff, maxAbsDiff,
					new Color(0, 0, 140), new Color(0, 60, 200 ), new Color(0, 120, 255),
					Color.WHITE,
					new Color(255, 120, 0), new Color(200, 60, 0), new Color(140, 0, 0));
		} else if (ratio) {
			if (histScalar == HistScalar.MAG)
				hist = new HistogramFunction(0.666667d, 1.5d, 30);
			else
				hist = new HistogramFunction(0.5d, 2d, 30);
			CPT belowCPT = new CPT(hist.getMinX(), 1d,
					new Color(0, 0, 140), new Color(0, 60, 200 ), new Color(0, 120, 255),
					Color.WHITE);
			CPT aboveCPT = new CPT(1d, hist.getMaxX(),
					Color.WHITE,
					new Color(255, 120, 0), new Color(200, 60, 0), new Color(140, 0, 0));
			cpt = new CPT();
			cpt.addAll(belowCPT);
			cpt.addAll(aboveCPT);
		} else {
			hist = histScalar.getHistogram(maxTrack);
			cpt = GMT_CPT_Files.RAINBOW_UNIFORM.instance();
			cpt = cpt.rescale(hist.getMinX() - 0.5*hist.getDelta(), hist.getMaxX() + 0.5*hist.getDelta());
		}
		cpt.setBelowMinColor(cpt.getMinColor());
		cpt.setAboveMaxColor(cpt.getMaxColor());
		
		for (int s=0; s<rupSet.getNumSections(); s++) {
			FaultSection sect = rupSet.getFaultSectionData(s);
			RuptureSurface surf = sect.getFaultSurface(1d, false, false);
			
			XY_DataSet trace = new DefaultXY_DataSet();
			for (Location loc : surf.getEvenlyDiscritizedUpperEdge())
				trace.set(loc.getLongitude(), loc.getLatitude());
			
			if (sect.getAveDip() != 90d) {
				XY_DataSet outline = new DefaultXY_DataSet();
				LocationList perimeter = surf.getPerimeter();
				for (Location loc : perimeter)
					outline.set(loc.getLongitude(), loc.getLatitude());
				Location first = perimeter.first();
				outline.set(first.getLongitude(), first.getLatitude());
				
				funcs.add(0, outline);
				chars.add(0, new PlotCurveCharacterstics(PlotLineType.SOLID, 1f, faultOutlineColor));
			}
			
			if (s == 0)
				trace.setName("Fault Sections");
			double value;
			if (histScalar.isLogX())
				value = Math.log10(values.get(s));
			else
				value = values.get(s);
			Color faultColor = cpt.getColor((float)value);
			int index;
			if (histScalar.isLogX() && value <= 0d)
				index = 0;
			else
				index = hist.getClosestXIndex(value);
			hist.add(index, 1d);
			
			funcs.add(trace);
			chars.add(new PlotCurveCharacterstics(PlotLineType.SOLID, 3f, faultColor));
		}
		
		PlotSpec spec = new PlotSpec(funcs, chars, title, "Longitude", "Latitude");
		spec.setLegendVisible(false);
		
		
		String cptTitle = "Section Max Participating "+histScalar.xAxisLabel;
		if (difference)
			cptTitle += ", Difference";
		if (histScalar.isLogX())
			cptTitle = "Log10 "+cptTitle;
		double cptLen = cpt.getMaxValue() - cpt.getMinValue();
		double cptTick;
		if (cptLen > 5000)
			cptTick = 1000;
		else if (cptLen > 1000)
			cptTick = 500;
		else if (cptLen > 500)
			cptTick = 100;
		else if (cptLen > 100)
			cptTick = 50;
		else if (cptLen > 50)
			cptTick = 10;
		else if (cptLen > 10)
			cptTick = 5;
		else if (cptLen > 5)
			cptTick = 1;
		else if (cptLen > 1)
			cptTick = .5;
		else if (cptLen > .5)
			cptTick = .1;
		else
			cptTick = cptLen / 10d;
		PaintScaleLegend cptBar = XYZGraphPanel.getLegendForCPT(cpt, cptTitle,
				24, 18, cptTick, RectangleEdge.BOTTOM);
		spec.addSubtitle(cptBar);
		
		HeadlessGraphPanel gp = new HeadlessGraphPanel();
		gp.setTickLabelFontSize(18);
		gp.setAxisLabelFontSize(24);
		gp.setPlotLabelFontSize(24);
		gp.setBackgroundColor(Color.WHITE);
		
		Range xRange = new Range(reg.getMinLon(), reg.getMaxLon());
		Range yRange = new Range(reg.getMinLat(), reg.getMaxLat());
		
		gp.drawGraphPanel(spec, false, false, xRange, yRange);
		double tick = 2d;
		TickUnits tus = new TickUnits();
		TickUnit tu = new NumberTickUnit(tick);
		tus.add(tu);
		gp.getXAxis().setStandardTickUnits(tus);
		gp.getYAxis().setStandardTickUnits(tus);
		
		File file = new File(outputDir, prefix);
		double aspectRatio = yRange.getLength() / xRange.getLength();
		gp.getChartPanel().setSize(800, 350 + (int)((800d-200d)*aspectRatio));
		gp.saveAsPNG(file.getAbsolutePath()+".png");
		gp.saveAsPDF(file.getAbsolutePath()+".pdf");
		
		// plot histogram now
		funcs = new ArrayList<>();
		chars = new ArrayList<>();
		
		if (histScalar.isLogX()) {
			ArbitrarilyDiscretizedFunc linearHist = new ArbitrarilyDiscretizedFunc();
			for (Point2D pt : hist)
				linearHist.set(Math.pow(10, pt.getX()), pt.getY());
			
			funcs.add(linearHist);
			chars.add(new PlotCurveCharacterstics(PlotLineType.HISTOGRAM, 1f, mainColor));
		} else {
			funcs.add(hist);
			chars.add(new PlotCurveCharacterstics(PlotLineType.HISTOGRAM, 1f, mainColor));
		}
		
		String xAxisLabel = "Section Max "+histScalar.xAxisLabel;
		if (difference)
			xAxisLabel += ", Difference";
		String yAxisLabel = "Count";
		
		spec = new PlotSpec(funcs, chars, title, xAxisLabel, yAxisLabel);
		
		if (histScalar.isLogX())
			xRange = new Range(Math.pow(10, hist.getMinX() - 0.5*hist.getDelta()),
					Math.pow(10, hist.getMaxX() + 0.5*hist.getDelta()));
		else
			xRange = new Range(hist.getMinX() - 0.5*hist.getDelta(),
					hist.getMaxX() + 0.5*hist.getDelta());
		
		gp = new HeadlessGraphPanel();
		gp.setBackgroundColor(Color.WHITE);
		gp.setTickLabelFontSize(18);
		gp.setAxisLabelFontSize(20);
		gp.setPlotLabelFontSize(21);
		gp.setLegendFontSize(22);
		
		yRange = new Range(0, 1.05*hist.getMaxY());
		
		gp.drawGraphPanel(spec, histScalar.isLogX(), false, xRange, yRange);
		gp.getChartPanel().setSize(800, 600);
		prefix += "_hist";
		File pngFile = new File(outputDir, prefix+".png");
		File pdfFile = new File(outputDir, prefix+".pdf");
		gp.saveAsPNG(pngFile.getAbsolutePath());
		gp.saveAsPDF(pdfFile.getAbsolutePath());
		
		return true;
	}
	
	private static File[] plotBiasiWesnouskyJumpDistComparison(File resourcesDir, String prefix,
			String title, List<ClusterRupture> rups, ClusterConnectionStrategy connStrat,
			RuptureConnectionSearch connSearch, FaultSystemSolution sol, RakeType mech) throws IOException {
		Range xRange = new Range(0d, 10d);
		EvenlyDiscretizedFunc jumpsTakenFunc = new EvenlyDiscretizedFunc(0.5, 10, 1d);
		EvenlyDiscretizedFunc jumpsNotTakenFunc = new EvenlyDiscretizedFunc(0.5, 10, 1d);
		for (int r=0; r<rups.size(); r++) {
			double rate = sol == null ? 1d : sol.getRateForRup(r);
			List<ClusterRupture> versions = Lists.newArrayList(rups.get(r));
			versions.addAll(rups.get(r).getPreferredAltRepresentations(connSearch));
			rate /= versions.size();
			for (ClusterRupture rup : versions) {
				for (Jump jump : rup.getJumpsIterable())
					if (mech.isMatch(jump.fromSection.getAveRake()) && mech.isMatch(jump.toSection.getAveRake()))
						jumpsTakenFunc.add(jumpsTakenFunc.getClosestXIndex(jump.distance), rate);
				HashSet<Integer> rupParents = new HashSet<>();
				for (FaultSubsectionCluster cluster : rup.getClustersIterable())
					rupParents.add(cluster.parentSectionID);
				for (ClusterRupture strand : rup.getStrandsIterable()) {
					List<FaultSection> strandSects = strand.clusters[strand.clusters.length-1].subSects;
					FaultSection lastSect = strandSects.get(strandSects.size()-1);
					if (!mech.isMatch(lastSect.getAveRake()))
						continue;
					double minCandidateDist = Double.POSITIVE_INFINITY;
					for (Jump jump : connStrat.getJumpsFrom(lastSect)) {
						if (!mech.isMatch(jump.toSection.getAveRake()))
							continue;
						if (!rupParents.contains(jump.toSection.getParentSectionId())) {
							// only count if we don't have this jump (or a jump to a the same section)
							minCandidateDist = Math.min(minCandidateDist, jump.distance);
						}
					}
					if (Double.isFinite(minCandidateDist))
						jumpsNotTakenFunc.add(jumpsNotTakenFunc.getClosestXIndex(minCandidateDist), rate);
				}
			}
		}
		EvenlyDiscretizedFunc passingRatio = new EvenlyDiscretizedFunc(jumpsTakenFunc.getMinX(),
				jumpsTakenFunc.getMaxX(), jumpsTakenFunc.size());
		EvenlyDiscretizedFunc prob = new EvenlyDiscretizedFunc(jumpsTakenFunc.getMinX(),
				jumpsTakenFunc.getMaxX(), jumpsTakenFunc.size());
		for (int i=0; i<passingRatio.size(); i++) {
			double pr = jumpsTakenFunc.getY(i)/jumpsNotTakenFunc.getY(i);
			if (Double.isInfinite(pr))
				pr = Double.NaN;
			passingRatio.set(i, pr);
			if (Double.isFinite(pr))
				prob.set(i, CumulativeProbabilityFilter.passingRatioToProb(pr));
			else
				prob.set(i, Double.NaN);
		}
		
		List<DiscretizedFunc> funcs = new ArrayList<>();
		List<PlotCurveCharacterstics> chars = new ArrayList<>();
		
		passingRatio.setName("Rupture Set");
		funcs.add(passingRatio);
		chars.add(new PlotCurveCharacterstics(PlotLineType.SOLID, 4f,
				PlotSymbol.FILLED_CIRCLE, 6f, MAIN_COLOR));
		
		EvenlyDiscretizedFunc bwPR = null;
		if (mech == RakeType.LEFT_LATERAL || mech == RakeType.RIGHT_LATERAL) {
			BiasiWesnousky2016SSJumpProb ssDistProb = new BiasiWesnousky2016SSJumpProb();
			bwPR = new EvenlyDiscretizedFunc(passingRatio.getMinX(),
					passingRatio.getMaxX(), passingRatio.size());
			for (int i=0; i<bwPR.size(); i++)
				bwPR.set(i, ssDistProb.calcPassingRatio(bwPR.getX(i)));
			
			bwPR.setName("B&W (2016), SS");
			funcs.add(bwPR);
			chars.add(new PlotCurveCharacterstics(PlotLineType.SOLID, 2f, Color.BLACK));
		}
		
		double bwDistIndepProb = new BiasiWesnousky2016CombJumpDistProb().getDistanceIndepentProb(mech);
		double bwDistIndepPR = CumulativeProbabilityFilter.probToPassingRatio(bwDistIndepProb);
		DiscretizedFunc bwDistIndepPRFunc = new ArbitrarilyDiscretizedFunc();
		bwDistIndepPRFunc.set(xRange.getLowerBound(), bwDistIndepPR);
		bwDistIndepPRFunc.set(xRange.getUpperBound(), bwDistIndepPR);
		
		bwDistIndepPRFunc.setName("B&W (2016) Dist-Indep. P(>1km)");
		funcs.add(bwDistIndepPRFunc);
		chars.add(new PlotCurveCharacterstics(PlotLineType.DASHED, 2f, Color.BLACK));
		
		String yAxisLabel = "Passing Ratio";
		if (sol == null)
			yAxisLabel += " (as discretized)";
		else
			yAxisLabel += " (rate-weighted)";
		PlotSpec spec = new PlotSpec(funcs, chars, title, "Jump Distance (km)", yAxisLabel);
		spec.setLegendVisible(true);
		
		HeadlessGraphPanel gp = new HeadlessGraphPanel();
		gp.setBackgroundColor(Color.WHITE);
		gp.setTickLabelFontSize(18);
		gp.setAxisLabelFontSize(20);
		gp.setPlotLabelFontSize(21);
		gp.setLegendFontSize(22);
		
		Range yRange = new Range(0, 5d);
		
		gp.drawGraphPanel(spec, false, false, xRange, yRange);
		gp.getChartPanel().setSize(800, 550);
		File pngFile = new File(resourcesDir, prefix+"_ratio.png");
		File pdfFile = new File(resourcesDir, prefix+"_ratio.pdf");
		gp.saveAsPNG(pngFile.getAbsolutePath());
		gp.saveAsPDF(pdfFile.getAbsolutePath());
		
		File[] ret = new File[2];
		ret[0] = pngFile;
		
		funcs = new ArrayList<>();
		chars = new ArrayList<>();
		
		prob.setName("Rupture Set");
		funcs.add(prob);
		chars.add(new PlotCurveCharacterstics(PlotLineType.SOLID, 4f,
				PlotSymbol.FILLED_CIRCLE, 6f, MAIN_COLOR));
		
		if (bwPR != null) {
			EvenlyDiscretizedFunc bwProb = new EvenlyDiscretizedFunc(
					bwPR.getMinX(), bwPR.getMaxX(), bwPR.size());
			for (int i=0; i<bwPR.size(); i++)
				bwProb.set(i, CumulativeProbabilityFilter.passingRatioToProb(bwPR.getY(i)));
			
			bwProb.setName("B&W (2016), SS");
			funcs.add(bwProb);
			chars.add(new PlotCurveCharacterstics(PlotLineType.SOLID, 2f, Color.BLACK));
		}
		
		DiscretizedFunc bwDistIndepProbFunc = new ArbitrarilyDiscretizedFunc();
		bwDistIndepProbFunc.set(xRange.getLowerBound(), bwDistIndepProb);
		bwDistIndepProbFunc.set(xRange.getUpperBound(), bwDistIndepProb);
		
		bwDistIndepProbFunc.setName("B&W (2016) Dist-Indep. P(>1km)");
		funcs.add(bwDistIndepProbFunc);
		chars.add(new PlotCurveCharacterstics(PlotLineType.DASHED, 2f, Color.BLACK));
		
		yAxisLabel = "Jump Probability";
		if (sol == null)
			yAxisLabel += " (as discretized)";
		else
			yAxisLabel += " (rate-weighted)";
		spec = new PlotSpec(funcs, chars, title, "Jump Distance (km)", yAxisLabel);
		spec.setLegendVisible(true);
		
		gp = new HeadlessGraphPanel();
		gp.setBackgroundColor(Color.WHITE);
		gp.setTickLabelFontSize(18);
		gp.setAxisLabelFontSize(20);
		gp.setPlotLabelFontSize(21);
		gp.setLegendFontSize(22);
		
		yRange = new Range(0, 1d);
		
		gp.drawGraphPanel(spec, false, false, xRange, yRange);
		gp.getChartPanel().setSize(800, 550);
		pngFile = new File(resourcesDir, prefix+"_prob.png");
		pdfFile = new File(resourcesDir, prefix+"_prob.pdf");
		gp.saveAsPNG(pngFile.getAbsolutePath());
		gp.saveAsPDF(pdfFile.getAbsolutePath());
		
		ret[1] = pngFile;
		
		return ret;
	}
	
	private static File[] plotBiasiWesnouskyJumpAzComparison(File resourcesDir, String prefix,
			String title, List<ClusterRupture> rups, ClusterConnectionStrategy connStrat,
			RuptureConnectionSearch connSearch, FaultSystemSolution sol, RakeType mech) throws IOException {
		Range xRange = new Range(0d, 180d);
		EvenlyDiscretizedFunc jumpsTakenFunc = new EvenlyDiscretizedFunc(5d, 18, 10d);
		EvenlyDiscretizedFunc jumpsNotTakenFunc = new EvenlyDiscretizedFunc(5d, 18, 10d);
		SectionDistanceAzimuthCalculator distAzCalc = connSearch.getDistAzCalc();
		for (int r=0; r<rups.size(); r++) {
			double rate = sol == null ? 1d : sol.getRateForRup(r);
			List<ClusterRupture> versions = Lists.newArrayList(rups.get(r));
			versions.addAll(rups.get(r).getPreferredAltRepresentations(connSearch));
			rate /= versions.size();
			for (ClusterRupture rup : versions) {
				RuptureTreeNavigator nav = rup.getTreeNavigator();
				for (Jump jump : rup.getJumpsIterable()) {
					if (mech.isMatch(jump.fromSection.getAveRake())
							&& mech.isMatch(jump.toSection.getAveRake())) {
						FaultSection before2 = jump.fromSection;
						FaultSection before1 = nav.getPredecessor(before2);
						if (before1 == null)
							continue;
						double beforeAz = distAzCalc.getAzimuth(before1, before2);
						FaultSection after1 = jump.toSection;
						double minAz = Double.POSITIVE_INFINITY;
						for (FaultSection after2 : nav.getDescendants(after1)) {
							double afterAz = distAzCalc.getAzimuth(after1, after2);
							double diff = Math.abs(
									JumpAzimuthChangeFilter.getAzimuthDifference(beforeAz, afterAz));
							minAz = Math.min(minAz, diff);
						}
						if (Double.isFinite(minAz))
							jumpsTakenFunc.add(jumpsTakenFunc.getClosestXIndex(minAz), rate);
					}
				}
				HashSet<Integer> rupParents = new HashSet<>();
				for (FaultSubsectionCluster cluster : rup.getClustersIterable())
					rupParents.add(cluster.parentSectionID);
				for (ClusterRupture strand : rup.getStrandsIterable()) {
					List<FaultSection> strandSects = strand.clusters[strand.clusters.length-1].subSects;
					FaultSection lastSect = strandSects.get(strandSects.size()-1);
					if (!mech.isMatch(lastSect.getAveRake()))
						continue;
					FaultSection before2 = lastSect;
					FaultSection before1 = nav.getPredecessor(before2);
					if (before1 == null)
						continue;
					double beforeAz = distAzCalc.getAzimuth(before1, before2);
					double minCandidateAz = Double.POSITIVE_INFINITY;
					for (Jump jump : connStrat.getJumpsFrom(lastSect)) {
						if (!mech.isMatch(jump.toSection.getAveRake()))
							continue;
						
						if (!rupParents.contains(jump.toSection.getParentSectionId())) {
							// only count if we don't have this jump (or a jump to a the same section)
							FaultSection after1 = jump.toSection;
							if (jump.toCluster.subSects.size() < 2)
								continue;
							FaultSection after2 = jump.toCluster.subSects.get(1);
							double afterAz = distAzCalc.getAzimuth(after1, after2);
							double diff = Math.abs(
									JumpAzimuthChangeFilter.getAzimuthDifference(beforeAz, afterAz));
							minCandidateAz = Math.min(minCandidateAz, diff);
						}
					}
					if (Double.isFinite(minCandidateAz))
						jumpsNotTakenFunc.add(jumpsNotTakenFunc.getClosestXIndex(minCandidateAz), rate);
				}
			}
		}
		EvenlyDiscretizedFunc passingRatio = new EvenlyDiscretizedFunc(jumpsTakenFunc.getMinX(),
				jumpsTakenFunc.getMaxX(), jumpsTakenFunc.size());
		EvenlyDiscretizedFunc prob = new EvenlyDiscretizedFunc(jumpsTakenFunc.getMinX(),
				jumpsTakenFunc.getMaxX(), jumpsTakenFunc.size());
		for (int i=0; i<passingRatio.size(); i++) {
			double pr = jumpsTakenFunc.getY(i)/jumpsNotTakenFunc.getY(i);
			if (Double.isInfinite(pr))
				pr = Double.NaN;
			passingRatio.set(i, pr);
			if (Double.isFinite(pr))
				prob.set(i, CumulativeProbabilityFilter.passingRatioToProb(pr));
			else
				prob.set(i, Double.NaN);
		}
		
		List<DiscretizedFunc> funcs = new ArrayList<>();
		List<PlotCurveCharacterstics> chars = new ArrayList<>();
		
		passingRatio.setName("Rupture Set");
		funcs.add(passingRatio);
		chars.add(new PlotCurveCharacterstics(PlotLineType.SOLID, 4f,
				PlotSymbol.FILLED_CIRCLE, 6f, MAIN_COLOR));
		
		EvenlyDiscretizedFunc bwPR = null;
		if (mech == RakeType.LEFT_LATERAL || mech == RakeType.RIGHT_LATERAL) {
			bwPR = CumulativeProbabilityFilter.bw2017_ss_passRatio.deepClone();
			
			bwPR.setName("B&W (2017), SS");
			funcs.add(bwPR);
			chars.add(new PlotCurveCharacterstics(PlotLineType.SOLID, 2f, Color.BLACK));
		}
		
		String yAxisLabel = "Passing Ratio";
		if (sol == null)
			yAxisLabel += " (as discretized)";
		else
			yAxisLabel += " (rate-weighted)";
		PlotSpec spec = new PlotSpec(funcs, chars, title, "Jump Azimuth Change (degrees)", yAxisLabel);
		spec.setLegendVisible(true);
		
		HeadlessGraphPanel gp = new HeadlessGraphPanel();
		gp.setBackgroundColor(Color.WHITE);
		gp.setTickLabelFontSize(18);
		gp.setAxisLabelFontSize(20);
		gp.setPlotLabelFontSize(21);
		gp.setLegendFontSize(22);
		
		Range yRange = new Range(0, 5d);
		
		gp.drawGraphPanel(spec, false, false, xRange, yRange);
		gp.getChartPanel().setSize(800, 550);
		File pngFile = new File(resourcesDir, prefix+"_ratio.png");
		File pdfFile = new File(resourcesDir, prefix+"_ratio.pdf");
		gp.saveAsPNG(pngFile.getAbsolutePath());
		gp.saveAsPDF(pdfFile.getAbsolutePath());
		
		File[] ret = new File[2];
		ret[0] = pngFile;
		
		funcs = new ArrayList<>();
		chars = new ArrayList<>();
		
		prob.setName("Rupture Set");
		funcs.add(prob);
		chars.add(new PlotCurveCharacterstics(PlotLineType.SOLID, 4f,
				PlotSymbol.FILLED_CIRCLE, 6f, MAIN_COLOR));
		
		if (bwPR != null) {
			EvenlyDiscretizedFunc bwProb = new EvenlyDiscretizedFunc(
					bwPR.getMinX(), bwPR.getMaxX(), bwPR.size());
			for (int i=0; i<bwPR.size(); i++)
				bwProb.set(i, CumulativeProbabilityFilter.passingRatioToProb(bwPR.getY(i)));
			
			bwProb.setName("B&W (2017), SS");
			funcs.add(bwProb);
			chars.add(new PlotCurveCharacterstics(PlotLineType.SOLID, 2f, Color.BLACK));
		}
		
		yAxisLabel = "Jump Probability";
		if (sol == null)
			yAxisLabel += " (as discretized)";
		else
			yAxisLabel += " (rate-weighted)";
		spec = new PlotSpec(funcs, chars, title, "Jump Azimuth Change (degrees)", yAxisLabel);
		spec.setLegendVisible(true);
		
		gp = new HeadlessGraphPanel();
		gp.setBackgroundColor(Color.WHITE);
		gp.setTickLabelFontSize(18);
		gp.setAxisLabelFontSize(20);
		gp.setPlotLabelFontSize(21);
		gp.setLegendFontSize(22);
		
		yRange = new Range(0, 1d);
		
		gp.drawGraphPanel(spec, false, false, xRange, yRange);
		gp.getChartPanel().setSize(800, 550);
		pngFile = new File(resourcesDir, prefix+"_prob.png");
		pdfFile = new File(resourcesDir, prefix+"_prob.pdf");
		gp.saveAsPNG(pngFile.getAbsolutePath());
		gp.saveAsPDF(pdfFile.getAbsolutePath());
		
		ret[1] = pngFile;
		
		return ret;
	}
	
	private static File plotBiasiWesnouskyMechChangeComparison(File resourcesDir, String prefix,
			String title, List<ClusterRupture> rups, ClusterConnectionStrategy connStrat,
			RuptureConnectionSearch connSearch, FaultSystemRupSet rupSet, FaultSystemSolution sol)
					throws IOException {
		Range xRange = new Range(6d, 8.5d);
		EvenlyDiscretizedFunc mechChangesFunc = new EvenlyDiscretizedFunc(6.25, 5, 0.5d);
		EvenlyDiscretizedFunc rupMagFunc = new EvenlyDiscretizedFunc(6.25, 5, 0.5d);
		for (int r=0; r<rups.size(); r++) {
			double rate = sol == null ? 1d : sol.getRateForRup(r);
			
			List<ClusterRupture> versions = Lists.newArrayList(rups.get(r));
			versions.addAll(rups.get(r).getPreferredAltRepresentations(connSearch));
			int magIndex = rupMagFunc.getClosestXIndex(rupSet.getMagForRup(r));
			rupMagFunc.add(magIndex, rate);
			rate /= versions.size();
			for (ClusterRupture rup : versions) {
				boolean found = false;
				for (Jump jump : rup.getJumpsIterable()) {
					if (RakeType.getType(jump.fromSection.getAveRake())
							!= RakeType.getType(jump.toSection.getAveRake())) {
						found = true;
						break;
					}
				}
				if (found)
					mechChangesFunc.add(magIndex, rate);
			}
		}
		EvenlyDiscretizedFunc prob = new EvenlyDiscretizedFunc(rupMagFunc.getMinX(),
				rupMagFunc.getMaxX(), rupMagFunc.size());
		for (int i=0; i<prob.size(); i++) {
			double magRate = rupMagFunc.getY(i);
			if (magRate == 0d) {
				prob.set(i, Double.NaN);
			} else {
				prob.set(i, mechChangesFunc.getY(i)/magRate);
			}
		}
		
		List<DiscretizedFunc> funcs = new ArrayList<>();
		List<PlotCurveCharacterstics> chars = new ArrayList<>();
		
		prob.setName("Rupture Set");
		funcs.add(prob);
		chars.add(new PlotCurveCharacterstics(PlotLineType.SOLID, 4f, MAIN_COLOR));
		
		DiscretizedFunc bwProb = new ArbitrarilyDiscretizedFunc();
		bwProb.set(xRange.getLowerBound(), CumulativeProbabilityFilter.bw2017_mech_change_prob);
		bwProb.set(xRange.getUpperBound(), CumulativeProbabilityFilter.bw2017_mech_change_prob);
		bwProb.setName("B&W (2017)");
		funcs.add(bwProb);
		chars.add(new PlotCurveCharacterstics(PlotLineType.SOLID, 2f, Color.BLACK));
		
		String yAxisLabel = "Probability";
		if (sol == null)
			yAxisLabel += " (as discretized)";
		else
			yAxisLabel += " (rate-weighted)";
		PlotSpec spec = new PlotSpec(funcs, chars, title, "Magnitude", yAxisLabel);
		spec.setLegendVisible(true);
		
		HeadlessGraphPanel gp = new HeadlessGraphPanel();
		gp.setBackgroundColor(Color.WHITE);
		gp.setTickLabelFontSize(18);
		gp.setAxisLabelFontSize(20);
		gp.setPlotLabelFontSize(21);
		gp.setLegendFontSize(22);
		
		Range yRange = new Range(0, 1d);
		
		gp.drawGraphPanel(spec, false, false, xRange, yRange);
		gp.getChartPanel().setSize(800, 550);
		File pngFile = new File(resourcesDir, prefix+".png");
		File pdfFile = new File(resourcesDir, prefix+".pdf");
		gp.saveAsPNG(pngFile.getAbsolutePath());
		gp.saveAsPDF(pdfFile.getAbsolutePath());
		
		return pngFile;
	}
}
