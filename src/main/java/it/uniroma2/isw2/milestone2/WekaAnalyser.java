package it.uniroma2.isw2.milestone2;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import it.uniroma2.isw2.milestone2.entities.AnalysisProfile;
import it.uniroma2.isw2.milestone2.entities.AnalysisRun;
import it.uniroma2.isw2.milestone2.entities.EvalOutputs;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

public class WekaAnalyser {
	
	private static final Logger LOGGER = Logger.getLogger(WekaAnalyser.class.getName());
	
	public static final int VERSION_NAME_INDEX = 1;
	public static final int FILENAME_INDEX = 2;

	private String projectName;
	private String outputsFolder;
	private Instances dataset;
	private List<String> versions;
	
	public WekaAnalyser(String projectName, String outputsFolder) throws Exception {
		this.projectName = projectName;
		this.outputsFolder = outputsFolder;
		this.dataset = new DataSource(String.format("%s%s_metrics.arff", this.outputsFolder, this.projectName)).getDataSet();
		this.versions = getVersion(this.dataset);
	}
	
	public void analyse() throws Exception {
		File csvDataset = new File(String.format("%s%s_analysis.csv", this.outputsFolder, this.projectName));
		try (FileWriter writer = new FileWriter(csvDataset, false)) {
			writer.append(EvalOutputs.CSV_HEADER);
			for (AnalysisProfile profile : AnalysisProfile.generateAllProfiles())
				this.walkForward(profile, writer);
		}
	}
	
	private void walkForward(AnalysisProfile analysisProfile, FileWriter writer) throws Exception {

		for (int testingIdx = 1; testingIdx < this.versions.size(); ++testingIdx) {
			AnalysisRun actualRun = new AnalysisRun(dataset, analysisProfile, projectName);

			for (Instance instance : dataset) {
				if (versions.indexOf(instance.stringValue(VERSION_NAME_INDEX).toString()) == testingIdx)
					actualRun.addToTesting(instance);

				else if (versions.indexOf(instance.stringValue(VERSION_NAME_INDEX).toString()) < testingIdx)
					actualRun.addToTraining(instance);
			}

			actualRun.removeUnwantedAttributes();
			actualRun.setupClassIndexes();
			actualRun.initializeOutputs(this.dataset.size());
			actualRun.evaluate(analysisProfile);

			LOGGER.log(Level.INFO, "Nuovi risultati aggiunti: {0}", actualRun.getOutputs());
			writer.append(String.format("%s%n", actualRun.getOutputs()));
		}
		
	}
	
	private List<String> getVersion(Instances dataset) {
		List<String> versionNames = new ArrayList<>();
		for (Instance instance : dataset)
			if (!versionNames.contains(instance.stringValue(VERSION_NAME_INDEX))) 
				versionNames.add(instance.stringValue(VERSION_NAME_INDEX));
		return versionNames;
	}
	
}
