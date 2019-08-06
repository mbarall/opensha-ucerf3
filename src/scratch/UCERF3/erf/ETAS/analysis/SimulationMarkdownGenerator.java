package scratch.UCERF3.erf.ETAS.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.time.StopWatch;
import org.opensha.commons.util.ClassUtils;
import org.opensha.commons.util.ExceptionUtils;
import org.opensha.commons.util.FileNameComparator;
import org.opensha.commons.util.MarkdownUtils;
import org.opensha.commons.util.MarkdownUtils.TableBuilder;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.io.Files;
import com.google.common.primitives.Doubles;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import scratch.UCERF3.FaultSystemSolution;
import scratch.UCERF3.erf.ETAS.ETAS_CatalogIO;
import scratch.UCERF3.erf.ETAS.ETAS_EqkRupture;
import scratch.UCERF3.erf.ETAS.launcher.ETAS_Config;
import scratch.UCERF3.erf.ETAS.launcher.ETAS_Config.BinaryFilteredOutputConfig;
import scratch.UCERF3.erf.ETAS.launcher.ETAS_Launcher;
import scratch.UCERF3.erf.ETAS.launcher.util.ETAS_CatalogIteration;

public class SimulationMarkdownGenerator {
	
	private static Options createOptions() {
		Options ops = new Options();

		Option noMapsOption = new Option("nm", "no-maps", false,
				"Flag to disable map plots (useful it no internet connection or map server down");
		noMapsOption.setRequired(false);
		ops.addOption(noMapsOption);

		Option numCatalogsOption = new Option("num", "num-catalogs", true,
				"Only process the given number of catalogs (optional)");
		numCatalogsOption.setRequired(false);
		ops.addOption(numCatalogsOption);
		
		Option forceUpdateOption = new Option("f", "force-update", false,
				"Force update of all plots, otherwise will only update on new plot versions or if "
				+ "the previous plots were done on an incomplete simulation");
		forceUpdateOption.setRequired(false);
		ops.addOption(forceUpdateOption);

		Option threadsOption = new Option("t", "threads", true,
				"Number of calculation threads. Default is the number of available processors (in this case: "+defaultNumThreads()+")");
		threadsOption.setRequired(false);
		ops.addOption(threadsOption);
		
		return ops;
	}
	
	public static int defaultNumThreads() {
		return Runtime.getRuntime().availableProcessors();
	}

	public static void main(String[] args) {
		if (args.length == 1 && args[0].equals("--hardcoded")) {
			//			File simDir = new File("/home/kevin/OpenSHA/UCERF3/etas/simulations/"
			//			+ "2018_08_07-MojaveM7-noSpont-10yr");
			//		configFile = new File(simDir, "config.json");
			//		inputFile = new File(simDir, "results_complete.bin");
			////		inputFile = new File(simDir, "results_complete_partial.bin");

			File simDir = new File("/home/kevin/OpenSHA/UCERF3/etas/simulations/"
//					+ "2019_06_05-Spontaneous-includeSpont-historicalCatalog-full_td-1000yr");
//					+ "2019_06_05-Spontaneous-includeSpont-historicalCatalog-no_ert-1000yr");
//					+ "2019_07_04-SearlesValleyM64-includeSpont-full_td-10yr");
//					+ "2019-06-05_M7.1_SearlesValley_Sequence_UpdatedMw_and_depth");
//					+ "2019_07_06-SearlessValleySequenceFiniteFault-noSpont-full_td-10yr-start-noon");
//					+ "2019_07_06-SearlessValleySequenceFiniteFault-noSpont-full_td-10yr-following-M7.1");
//					+ "2019_07_11-ComCatM7p1_ci38457511_FiniteSurface-noSpont-full_td-scale1.14");
//					+ "2019_07_11-ComCatM7p1_ci38457511_5p9DaysAfter_FiniteSurface-noSpont-full_td-scale1.14");
//					+ "2019_07_11-ComCatM7p1_ci38457511_FiniteSurface_NoFaults-noSpont-poisson-griddedOnly");
//					+ "2019_07_16-ComCatM7p1_ci38457511_11DaysAfter_ShakeMapSurfaces-noSpont-full_td-scale1.14");
					+ "2019_07_16-ComCatM7p1_ci38457511_ShakeMapSurfaces-noSpont-full_td-scale1.14");
//			File configFile = new File(simDir, "config.json");
			File configFile = new File("/home/kevin/git/ucerf3-etas-launcher/tutorial/user_output/"
					+ "comcat-ridgecrest-m7.1-example/config.json");
//			File inputFile = new File(simDir, "results_m5_preserve_chain.bin");
//			args = new String[] { configFile.getAbsolutePath(), inputFile.getAb?olutePath() };
//			args = new String[] { configFile.getAbsolutePath() };
//			String gitHash = getGitHash();
//			System.out.println(gitHash);
//			System.out.println(getGitCommitTime(gitHash));
//			System.exit(0);
			args = new String[] { "--num-catalogs", "10000", configFile.getAbsolutePath() };
		}
		
		// TODO optional second arg
		
		try {
			Options options = createOptions();
			
			CommandLineParser parser = new DefaultParser();
			
			String syntax = ClassUtils.getClassNameWithoutPackage(SimulationMarkdownGenerator.class)
					+" [options] <etas-config.json> [<binary-catalogs-file.bin OR results directory>]";
			
			CommandLine cmd;
			try {
				cmd = parser.parse(options, args);
			} catch (ParseException e) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp(syntax, options, true );
				System.exit(2);
				return;
			}
			
			args = cmd.getArgs();
			
			File configFile;
			File inputFile;
			if (args.length < 1 || args.length > 2) {
				System.err.println("USAGE: "+syntax);
				System.exit(2);
			}
			
			configFile = new File(args[0]);
			
			Preconditions.checkState(configFile.exists(), "ETAS config file doesn't exist: "+configFile.getAbsolutePath());
			ETAS_Config config = ETAS_Config.readJSON(configFile);
			if (args.length == 2) {
				inputFile = new File(args[1]);
			} else {
				System.out.println("Catalogs file/dir not specififed, searching for catalogs...");
				inputFile = locateInputFile(config);
			}
			
			File outputDir = config.getOutputDir();
			if (!outputDir.exists() && !outputDir.mkdir()) {
				System.out.println("Output dir doesn't exist and can't be created, "
						+ "assuming that it was computed remotely: "+outputDir.getAbsolutePath());
				outputDir = inputFile.getParentFile();
				System.out.println("Using parent directory of input file as output dir: "+outputDir.getAbsolutePath());
			}
			
			boolean skipMaps = cmd.hasOption("no-maps");
			
			int maxCatalogs = cmd.hasOption("num-catalogs") ? Integer.parseInt(cmd.getOptionValue("num-catalogs")) : 0;
			int threads = cmd.hasOption("threads") ? Integer.parseInt(cmd.getOptionValue("threads"))
					: defaultNumThreads();
			boolean forcePlot = cmd.hasOption("force-update");
			
			generateMarkdown(configFile, inputFile, config, outputDir, skipMaps, maxCatalogs, threads, forcePlot, true);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(2);
		}
		System.exit(0);
	}

	public static File locateInputFile(ETAS_Config config) {
		List<BinaryFilteredOutputConfig> binaryFilters = config.getBinaryOutputFilters();
		File inputFile = null;
		if (binaryFilters != null) {
			binaryFilters = new ArrayList<>(binaryFilters);
			binaryFilters.sort(binaryOutputComparator); // sort so that the one with the lowest magnitude is used preferentially
			if (config.getDuration() > 200) {
				List<BinaryFilteredOutputConfig> filtered = new ArrayList<>();
				for (BinaryFilteredOutputConfig bin : binaryFilters)
					if (bin.getMinMag() != null && bin.getMinMag() >= 4.9)
						filtered.add(bin);
				if (!filtered.isEmpty()) {
					System.out.println("Skipping binary files below M5 as catalog duration is > 200 years");
					binaryFilters = filtered;
				}
			}
			for (BinaryFilteredOutputConfig bin : binaryFilters) {
				File binFile = new File(config.getOutputDir(), bin.getPrefix()+".bin");
				if (binFile.exists()) {
					inputFile = binFile;
					break;
				}
				// check for gzipped
				binFile = new File(binFile.getParentFile(), binFile.getName()+".gz");
				if (binFile.exists()) {
					inputFile = binFile;
					break;
				}
				// check for partial
				binFile = new File(config.getOutputDir(), bin.getPrefix()+"_partial.bin");
				if (binFile.exists()) {
					inputFile = binFile;
					break;
				}
			}
		}
		if (inputFile != null) {
			System.out.println("Using binary catalogs file: "+inputFile.getAbsolutePath());
		} else {
			inputFile = new File(config.getOutputDir(), "results");
			Preconditions.checkState(inputFile.exists(),
					"Couldn't locate results binary files and results dir doesn't exist: %s", inputFile.getAbsolutePath());
			System.out.println("Using results dir: "+inputFile.getAbsolutePath());
		}
		return inputFile;
	}
	
	private static double seconds(long milliseconds) {
		return milliseconds/1000d;
	}
	
	private static class PlotFinalizeCallable implements Callable<PlotMarkdownBuilder> {
		
		private ETAS_AbstractPlot plot;
		private File plotsDir;
		private FaultSystemSolution fss;
		
		private Stopwatch finalizeSubWatch;

		private ExecutorService exec;
		private List<Future<?>> finalizeFutures;

		public PlotFinalizeCallable(ETAS_AbstractPlot plot, File plotsDir, FaultSystemSolution fss,
				ExecutorService exec) {
			this.plot = plot;
			this.plotsDir = plotsDir;
			this.fss = fss;
			this.exec = exec;
		}

		@Override
		public PlotMarkdownBuilder call() throws Exception {
			System.out.println("Finalizing "+ClassUtils.getClassNameWithoutPackage(plot.getClass()));
			finalizeSubWatch = Stopwatch.createStarted();
			List<? extends Runnable> runnables = plot.finalize(plotsDir, fss);
			if (runnables != null && !runnables.isEmpty()) {
				finalizeFutures = new ArrayList<>();
				for (Runnable runnable : runnables)
					finalizeFutures.add(exec.submit(runnable));
			}
			
			finalizeSubWatch.stop();
			return new PlotMarkdownBuilder(plot, finalizeFutures, finalizeSubWatch);
		}
		
	}
	
	private static class PlotMarkdownBuilder {
		
		private ETAS_AbstractPlot plot;
		private List<Future<?>> finalizeFutures;
		private Stopwatch finalizeSubWatch;

		PlotMarkdownBuilder(ETAS_AbstractPlot plot, List<Future<?>> finalizeFutures,
				Stopwatch finalizeSubWatch) {
			this.plot = plot;
			this.finalizeFutures = finalizeFutures;
			this.finalizeSubWatch = finalizeSubWatch;
		}

		public List<String> buildMarkdown(String relativePathToOutputDir, String topLevelHeading, String topLink) {
			finalizeSubWatch.start();
			try {
				if (finalizeFutures != null) {
					// wait on futures
					for (Future<?> future : finalizeFutures)
						future.get();
				}
				List<String> lines = plot.generateMarkdown(relativePathToOutputDir, topLevelHeading, topLink);
				finalizeSubWatch.stop();
				return lines;
			} catch (Exception e) {
				System.out.flush();
				System.err.println("Exception finalizing plot, it won't be included in the output page");
				e.printStackTrace();
				System.err.flush();
				finalizeSubWatch.stop();
				return null;
			}
		}
		
	}

	public static PlotMetadata generateMarkdown(File configFile, File inputFile, ETAS_Config config, File outputDir,
			boolean skipMaps, int maxCatalogs, int threads, boolean forceUpdateAll, boolean forceUpdateEvaluation) throws IOException {
		long plotStartTime = System.currentTimeMillis();
		Preconditions.checkState(outputDir.exists() || outputDir.mkdir(),
				"Output dir doesn't exist and couldn't be created: %s", outputDir.getAbsolutePath());
		File plotsDir = new File(outputDir, "plots");
		Preconditions.checkState(plotsDir.exists() || plotsDir.mkdir(),
				"Plot dir doesn't exist and couldn't be created: %s", plotsDir.getAbsolutePath());
		
		List<ETAS_AbstractPlot> plots = new ArrayList<>();
		
		ETAS_Launcher launcher = new ETAS_Launcher(config, false);
		
		boolean hasTriggers = config.hasTriggers();
		
		boolean annualizeMFDs = !hasTriggers;
		if (hasTriggers) {
			plots.add(new ETAS_MFD_Plot(config, launcher, "mag_num_cumulative", annualizeMFDs, true));
			plots.add(new ETAS_HazardChangePlot(config, launcher, "hazard_change_100km", 100d));
			plots.add(new ETAS_TriggerRuptureFaultDistancesPlot(config, launcher, 20d));
			List<Double> percentiles = new ArrayList<>();
			percentiles.add(0d);
			percentiles.add(25d);
			percentiles.add(50d);
			percentiles.add(75d);
			if (config.getNumSimulations() > 100) {
				percentiles.add(90d);
				percentiles.add(95d);
			}
			if (config.getNumSimulations() >= 1000) {
				percentiles.add(97.5d);
			}
			if (config.getNumSimulations() >= 10000) {
				percentiles.add(98d);
				percentiles.add(99d);
			}
			if (config.getNumSimulations() >= 100000) {
				percentiles.add(99.5d);
				percentiles.add(99.9d);
			}
			percentiles.add(100d);
			plots.add(new ETAS_SimulatedCatalogPlot(config, launcher, "sim_catalog_map", Doubles.toArray(percentiles)));
			try {
				if (config.getComcatMetadata() != null)
					plots.add(new ETAS_ComcatComparePlot(config, launcher));
			} catch (Exception e) {
				System.err.println("Error building ComCat plot, skipping");
				e.printStackTrace();
			}
		} else {
			plots.add(new ETAS_MFD_Plot(config, launcher, "mfd", annualizeMFDs, true));
			if (config.getDuration() > 50)
				plots.add(new ETAS_LongTermRateVariabilityPlot(config, launcher, "long_term_var"));
		}
		if (!config.isGriddedOnly())
			plots.add(new ETAS_FaultParticipationPlot(config, launcher, "fault_participation", annualizeMFDs, skipMaps));
		if (!skipMaps)
			plots.add(new ETAS_GriddedNucleationPlot(config, launcher, "gridded_nucleation", annualizeMFDs));
		
		Map<ETAS_AbstractPlot, PlotResult> prevResults = new HashMap<>();
		
		File metadataFile = new File(plotsDir, "metadata.json");
		if (!forceUpdateAll && metadataFile.exists()) {
			try {
				PlotMetadata meta = readPlotMetadata(metadataFile);
				if (meta.simulationsProcessed < config.getNumSimulations()) {
					System.out.println("Reprocessing all plots as previous version was on incomplete simulation");
				} else if (meta.plots != null ) {
					Map<String, ETAS_AbstractPlot> plotClassNameMap = new HashMap<>();
					for (ETAS_AbstractPlot plot : plots)
						plotClassNameMap.put(plot.getClass().getName(), plot);
					System.out.println("Looking for plots we can skip (override with --force-replot option)...");
					for (PlotResult result : meta.plots) {
						ETAS_AbstractPlot plot = plotClassNameMap.get(result.className);
						boolean evalUpdate = plot != null && forceUpdateEvaluation && plot.isEvaluationPlot();
						if (plot != null && !evalUpdate && !plot.shouldReplot(result)) {
							System.out.println("\tSkipping plot (already done): "
									+ClassUtils.getClassNameWithoutPackage(plot.getClass()));
							prevResults.put(plot, result);
						} else {
							System.out.println("\tWill update plot: "
									+ClassUtils.getClassNameWithoutPackage(plot.getClass()));
						}
					}
					if (plots.size() == prevResults.size()) {
						System.out.println("No plots left to update.");
						String gitHash;
						long gitTime;
						try {
							// update the git hash and time to make sure that we don't try to replot again on this code version
							gitHash = getGitHash();
							gitTime = getGitCommitTime(gitHash);
						} catch (Exception e) {
							System.err.println("Couldn't locate git hash, won't update metadata file");
							e.printStackTrace();
							gitHash = meta.launcherGitHash;
							gitTime = meta.launcherGitTime;
						}
						if (gitTime > meta.launcherGitTime) {
							System.out.println("Uploading plot metadata file with new git time");
							meta = new PlotMetadata(plotStartTime, System.currentTimeMillis(), meta.simulationsProcessed, meta.dataFile,
								gitHash, gitTime, meta.plots);
							writePlotMetadata(meta, metadataFile);
						}
						return meta;
					}
				}
			} catch (Exception e) {
				System.out.println("Error reading previous plot metadata");
			}
		}
		
		boolean filterSpontaneous = false;
		for (ETAS_AbstractPlot plot : plots)
			filterSpontaneous = filterSpontaneous || plot.isFilterSpontaneous();
		
		final boolean isFilterSpontaneous = filterSpontaneous;
		
		FaultSystemSolution fss = launcher.checkOutFSS();
		
		// process catalogs
		Stopwatch totalProcessWatch = Stopwatch.createStarted();
		int numProcessed = ETAS_CatalogIteration.processCatalogs(inputFile, new ETAS_CatalogIteration.Callback() {
			
			@Override
			public void processCatalog(List<ETAS_EqkRupture> catalog, int index) {
				List<ETAS_EqkRupture> triggeredOnlyCatalog = null;
				if (isFilterSpontaneous)
					triggeredOnlyCatalog = ETAS_Launcher.getFilteredNoSpontaneous(config, catalog);
				for (int i=plots.size(); --i>=0;) {
					ETAS_AbstractPlot plot = plots.get(i);
					try {
						if (!prevResults.containsKey(plot))
							plot.processCatalog(catalog, triggeredOnlyCatalog, fss);
					} catch (Exception e) {
						System.err.println("Error processing catalog with plot "
								+ClassUtils.getClassNameWithoutPackage(plot.getClass())+", disabling plot");
						e.printStackTrace();
						plots.remove(i);
					}
				}
			}
		}, maxCatalogs);
		DecimalFormat timeDF = new DecimalFormat("0.00");
		totalProcessWatch.stop();
		
		double totProcessSeconds = seconds(totalProcessWatch.elapsed(TimeUnit.MILLISECONDS));
		System.out.println("Processed "+numProcessed+" catalogs in "+timeDF.format(totProcessSeconds)+" s");
		
		List<String> lines = new ArrayList<>();
		
		String simName = config.getSimulationName();
		if (simName == null || simName.isEmpty())
			simName = "ETAS Simulation";
		
		lines.add("# "+simName+" Results");
		lines.add("");
		
		List<ETAS_EqkRupture> triggerRups = launcher.getTriggerRuptures();
		List<ETAS_EqkRupture> histRups = launcher.getHistQkList();
		lines.addAll(getCatalogSummarytable(config, numProcessed, triggerRups, histRups));
		lines.add("");
		
		int tocIndex = lines.size();
		String topLink = "*[(top)](#table-of-contents)*";
		lines.add("");
		
		List<Double> finalizeTimes = new ArrayList<>();
		Stopwatch finalizeWatch = Stopwatch.createStarted();
		
		System.out.println("Finalizing plots with "+threads+" threads");
		double sumProcessTimes = 0d;
		Map<ETAS_AbstractPlot, PlotFinalizeCallable> plotCallables = new HashMap<>();
		ExecutorService exec = Executors.newFixedThreadPool(threads);
		Map<ETAS_AbstractPlot, Future<PlotMarkdownBuilder>> futures = new HashMap<>();
		List<PlotResult> plotResults = new ArrayList<>();
		for (ETAS_AbstractPlot plot : plots) {
			if (!prevResults.containsKey(plot)) {
				sumProcessTimes += seconds(plot.getProcessTimeMS());
				PlotFinalizeCallable call = new PlotFinalizeCallable(plot, plotsDir, fss, exec);
				plotCallables.put(plot, call);
				futures.put(plot, exec.submit(call));
			}
		}
		for (ETAS_AbstractPlot plot : plots) {
			if (prevResults.containsKey(plot)) {
				PlotResult result = prevResults.get(plot);
				lines.addAll(result.markdown);
				lines.add("");
				plotResults.add(result);
				finalizeTimes.add(0d);
			} else {
				try {
					PlotMarkdownBuilder builder = futures.get(plot).get();
					if (builder != null) {
						List<String> plotLines = builder.buildMarkdown(plotsDir.getName(), "##", topLink);
						if (plotLines != null && !plotLines.isEmpty()) {
							lines.addAll(plotLines);
							lines.add("");
							
							plotResults.add(new PlotResult(plot.getClass(), plot.getVersion(), plotStartTime, plotLines));
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				finalizeTimes.add(seconds(plotCallables.get(plot).finalizeSubWatch.elapsed(TimeUnit.MILLISECONDS)));
			}
		}
		finalizeWatch.stop();
		exec.shutdown();
		double totFinalizeTime = seconds(finalizeWatch.elapsed(TimeUnit.MILLISECONDS));
		
		System.out.println("Total catalog processing time: "+timeDF.format(totProcessSeconds)+" s");
		DecimalFormat percentDF = new DecimalFormat("0.00 %");
		double overhead = totProcessSeconds - sumProcessTimes;
		System.out.println("\tI/O overhead: "+timeDF.format(overhead)+" s ("+percentDF.format(overhead/totProcessSeconds)+")");
		for (ETAS_AbstractPlot plot : plots) {
			if (prevResults.containsKey(plot))
				continue;
			double plotSecs = seconds(plot.getProcessTimeMS());
			System.out.println("\t"+ClassUtils.getClassNameWithoutPackage(plot.getClass())+" "
					+timeDF.format(plotSecs)+" s ("+percentDF.format(plotSecs/totProcessSeconds)+")");
		}
		System.out.println();
		System.out.println("Total finalize time: "+timeDF.format(totFinalizeTime)+" s");
		for (int i=0; i<plots.size(); i++) {
			if (prevResults.containsKey(plots.get(i)))
				continue;
			double finalizeSecs = finalizeTimes.get(i);
			System.out.println("\t"+ClassUtils.getClassNameWithoutPackage(plots.get(i).getClass())+" "
					+timeDF.format(finalizeSecs)+" s ("+percentDF.format(finalizeSecs/totFinalizeTime)+")");
		}
		
		lines.add("");
		lines.add("## JSON Input File");
		lines.add(topLink); lines.add("");
		lines.add("```");
		for (String line : Files.readLines(configFile, Charset.defaultCharset()))
			lines.add(line);
		lines.add("```");
		lines.add("");
		
		launcher.checkInFSS(fss);
		
		List<String> tocLines = new ArrayList<>();
		tocLines.add("## Table Of Contents");
		tocLines.add("");
		tocLines.addAll(MarkdownUtils.buildTOC(lines, 2));
		
		lines.addAll(tocIndex, tocLines);
		
		System.out.println("Writing markdown and HTML");
		MarkdownUtils.writeReadmeAndHTML(lines, outputDir);
		System.out.println("DONE");
		
		String gitHash = getGitHash();
		Long gitTime = getGitCommitTime(gitHash);
		
		PlotMetadata meta = new PlotMetadata(plotStartTime, System.currentTimeMillis(),
				numProcessed, inputFile, gitHash, gitTime, plotResults);
		writePlotMetadata(meta, metadataFile);
		
		return meta;
	}
	
	public static List<String> getCatalogSummarytable(ETAS_Config config, int numProcessed, List<ETAS_EqkRupture> triggerRups, List<ETAS_EqkRupture> histRups) {
		TableBuilder builder = MarkdownUtils.tableBuilder();
		builder.addLine(" ", config.getSimulationName() == null ? "ETAS Simulation" : config.getSimulationName());
		if (numProcessed < config.getNumSimulations())
			builder.addLine("Num Simulations", numProcessed+" (incomplete)");
		else
			builder.addLine("Num Simulations", numProcessed+"");
		builder.addLine("Start Time", df.format(new Date(config.getSimulationStartTimeMillis())));
		builder.addLine("Start Time Epoch Milliseconds", config.getSimulationStartTimeMillis()+"");
		builder.addLine("Duration", ETAS_AbstractPlot.getTimeLabel(config.getDuration(), true));
		builder.addLine("Includes Spontaneous?", config.isIncludeSpontaneous()+"");
		addTriggerLines(builder, "Trigger Ruptures", triggerRups);
		if (config.isTreatTriggerCatalogAsSpontaneous())
			addTriggerLines(builder, "Historical Ruptures", histRups);
		else
			addTriggerLines(builder, "Trigger Ruptures", histRups);
		return builder.build();
	}
	
	private static void addTriggerLines(TableBuilder builder, String name, List<ETAS_EqkRupture> triggerRups) {
		if (triggerRups == null || triggerRups.isEmpty()) {
			builder.addLine(name, "*(none)*");
		} else {
			if (triggerRups.size() > 10) {
				double firstMag = 0d;
				long firstOT = Long.MAX_VALUE;
				double lastMag = 0d;
				long lastOT = Long.MIN_VALUE;
				long biggestOT = Long.MIN_VALUE;
				double maxMag = 0d;
				for (ETAS_EqkRupture rup : triggerRups) {
					double mag = rup.getMag();
					long ot = rup.getOriginTime();
					
					if (mag > maxMag) {
						maxMag = mag;
						biggestOT = ot;
					}
					
					if (ot < firstOT) {
						firstOT = ot;
						firstMag = mag;
					}
					
					if (ot > lastOT) {
						lastOT = ot;
						lastMag = mag;
					}
				}
				builder.addLine(name, triggerRups.size()+" Trigger Ruptures");
				builder.addLine(" ", "First: M"+ETAS_AbstractPlot.optionalDigitDF.format(firstMag)+" at "+df.format(new Date(firstOT)));
				builder.addLine(" ", "Last: M"+ETAS_AbstractPlot.optionalDigitDF.format(lastMag)+" at "+df.format(new Date(lastOT)));
				builder.addLine(" ", "Largest: M"+ETAS_AbstractPlot.optionalDigitDF.format(maxMag)+" at "+df.format(new Date(biggestOT)));
			}
		}
	}

	public static final SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z");
	static {
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	
	public static final Comparator<BinaryFilteredOutputConfig> binaryOutputComparator =
			new Comparator<ETAS_Config.BinaryFilteredOutputConfig>() {
		
		@Override
		public int compare(BinaryFilteredOutputConfig o1, BinaryFilteredOutputConfig o2) {
			if (o1.isDescendantsOnly() != o2.isDescendantsOnly()) {
				if (o1.isDescendantsOnly())
					return -1;
				return 1;
			}
			Double mag1 = o1.getMinMag();
			Double mag2 = o2.getMinMag();
			if (mag1 == null)
				mag1 = Double.NEGATIVE_INFINITY;
			if (mag2 == null)
				mag2 = Double.NEGATIVE_INFINITY;
			if (mag1 != mag2)
				return Double.compare(mag1, mag2);
			return 0;
		}
	};
	
	public static class PlotMetadata {
		public final long plotStartTime;
		public final long plotEndTime;
		public final int simulationsProcessed;
		public final File dataFile;
		public final String launcherGitHash;
		public final Long launcherGitTime;
		public final List<PlotResult> plots;
		
		public PlotMetadata(long plotStartTime, long plotEndTime, int simulationsProcessed,
				File dataFile, String launcherGitHash, long launcherGitTime, List<PlotResult> plots) {
			this.plotStartTime = plotStartTime;
			this.plotEndTime = plotEndTime;
			this.simulationsProcessed = simulationsProcessed;
			this.dataFile = dataFile;
			this.launcherGitHash = launcherGitHash;
			this.launcherGitTime = launcherGitTime;
			this.plots = plots;
		}
		
		public String toJSON() {
			Gson gson = buildGson();
			return gson.toJson(this);
		}
	}
	
	public static class PlotResult {
		public final String className;
		public final int version;
		public final long time;
		public final List<String> markdown;

		public PlotResult(Class<? extends ETAS_AbstractPlot> clazz, int version, long time, List<String> markdown) {
			this(clazz.getName(), version, time, markdown);
		}
		
		public PlotResult(String className, int version, long time, List<String> markdown) {
			this.className = className;
			this.version = version;
			this.time = time;
			this.markdown = markdown;
		}
	}
	
	private static Gson buildGson() {
		GsonBuilder builder = new GsonBuilder();
		builder.setPrettyPrinting();
		Gson gson = builder.create();
		return gson;
	}
	
	public static void writePlotMetadata(PlotMetadata meta, File outputFile)
			throws IOException {;
		FileWriter fw = new FileWriter(outputFile);
		fw.write(meta.toJSON()+"\n");
		fw.close();
	}
	
	public static PlotMetadata readPlotMetadata(File jsonFile) throws IOException {
		String json = null;
		for (String line : Files.readLines(jsonFile, Charset.defaultCharset())) {
			if (json == null)
				json = line;
			else
				json += "\n"+line;
		}
		return readPlotMetadata(json);
	}
	
	public static PlotMetadata readPlotMetadata(String json) {
		Gson gson = buildGson();
		PlotMetadata conf = gson.fromJson(json, PlotMetadata.class);
		return conf;
	}
	
	public static String getGitHash() {
		String var = System.getenv("ETAS_LAUNCHER");
		if (var != null && var.trim().length() > 0) {
			File dir = new File(var);
			if (dir.exists()) {
				return getGitHash(dir);
			}
		}
		return null;
	}
	
	public static String getGitHash(File launcherDir) {
		String[] command = { "/bin/bash", "-c",
				"cd "+launcherDir.getAbsolutePath()+"; git log -n 1 --pretty='%H' lib/opensha-ucerf3-all.jar" };
		
		try {
			Process p = Runtime.getRuntime().exec(command);
			int exit;
			try {
				exit = p.waitFor();
			} catch (InterruptedException e) {
				e.printStackTrace();
				return null;
			}
			if (exit != 0)
				return null;
			
			BufferedReader b = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			while ((line = b.readLine()) != null)
				if (line.trim().length() > 0)
					return line;
		} catch (Exception e) {
			System.err.println("Exception detecting ETAS_LAUNCHER git hash");
			e.printStackTrace();
		}
		return null;
	}
	
	public static Long getGitCommitTime(String gitHash) {
		String var = System.getenv("ETAS_LAUNCHER");
		if (var != null && var.trim().length() > 0) {
			File dir = new File(var);
			if (dir.exists()) {
				return getGitCommitTime(dir, gitHash);
			}
		}
		return null;
	}
	
	public static Long getGitCommitTime(File launcherDir, String gitHash) {
		String[] command = { "/bin/bash", "-c",
				"cd "+launcherDir.getAbsolutePath()+"; git show --no-patch --no-notes --pretty='%ct' "+gitHash };
		
		try {
			Process p = Runtime.getRuntime().exec(command);
			int exit;
			try {
				exit = p.waitFor();
			} catch (InterruptedException e) {
				e.printStackTrace();
				return null;
			}
			if (exit != 0)
				return null;
			
			BufferedReader b = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			while ((line = b.readLine()) != null)
				if (line.trim().length() > 0)
					return Long.parseLong(line)*1000l; // s to ms
		} catch (Exception e) {
			System.err.println("Exception detecting ETAS_LAUNCHER git hash");
			e.printStackTrace();
		}
		return null;
	}
}
