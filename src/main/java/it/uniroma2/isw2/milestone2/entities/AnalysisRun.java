package it.uniroma2.isw2.milestone2.entities;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import it.uniroma2.isw2.milestone2.WekaAnalyser;
import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.BestFirst;
import weka.attributeSelection.CfsSubsetEval;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.CostMatrix;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.meta.CostSensitiveClassifier;
import weka.classifiers.trees.RandomForest;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.supervised.instance.Resample;
import weka.filters.supervised.instance.SMOTE;
import weka.filters.supervised.instance.SpreadSubsample;
import weka.filters.unsupervised.attribute.Remove;

public class AnalysisRun {

	private static final double CFP = 1.0;
	private static final double CFN = 10.0;
	
	private Instances trainingSet;
	private Instances testingSet;
	
	private EvalOutputs evalOutputs;
	
	public AnalysisRun(Instances dataset, AnalysisProfile analysisProfile, String projectName) {
		this.trainingSet = new Instances(dataset, 0); // Crea un insieme vuoto di instanze con un header uguale a quello del dataset passato
		this.testingSet = new Instances(dataset, 0);  // Crea un insieme vuoto di instanze con un header uguale a quello del dataset passato
		this.evalOutputs = new EvalOutputs(projectName, analysisProfile);
	}
	
	public void addToTraining(Instance instance) {
		this.trainingSet.add(instance);
	}
	
	public void addToTesting(Instance instance) {
		this.testingSet.add(instance);
	}
	
	public void setupClassIndexes() {
		int numberOfAttributes = this.trainingSet.numAttributes();
		this.trainingSet.setClassIndex(numberOfAttributes - 1);
		this.testingSet.setClassIndex(numberOfAttributes - 1);
	}
	
	public void initializeOutputs(int size) {
		this.evalOutputs.setNumberOfTrainingReleases(this.trainingSet.size());
		this.evalOutputs.setPercentageOfTrainingReleases(((double)trainingSet.size())/size);
		
		int trainingDefects = this.getDefectsInTraining();
		int testingDefects = this.getDefectsInTesting();
		int totalDefects  = trainingDefects + testingDefects;
		
		if (totalDefects > 0) {
			this.evalOutputs.setPercentageOfDefectiveInTraining(totalDefects);
			this.evalOutputs.setPercentageOfDefectiveInTesting(totalDefects);
		}
		else {
			this.evalOutputs.setPercentageOfDefectiveInTraining(0);
			this.evalOutputs.setPercentageOfDefectiveInTesting(0);
		}
	}
	
	private void applyFeatureSelection(String type) throws Exception {
		switch (type) {
			case AnalysisProfile.FEATURE_SELECTION_NO:
				break;
			case AnalysisProfile.FEATURE_SELECTION_BEST_FIRST:
				Remove removeFilter = this.getRemoveFilter(getSelectedIndexes());
				this.trainingSet = Filter.useFilter(this.trainingSet, removeFilter);
				this.testingSet = Filter.useFilter(this.testingSet, removeFilter);
				setupClassIndexes();
				break;
			default:
				throw new IllegalArgumentException(String.format("Feature selection %s non valida", type));
		}
	}
	
	private void applySampling(String type) throws Exception {
		
		List<Integer> countYN = getNumberOfYN();
		int majoritySize = Collections.max(countYN);
		int minoritySize = Collections.min(countYN);
		String[] opts;
		
		switch (type) {
			case AnalysisProfile.SAMPLING_NO:
				break;
			case AnalysisProfile.SAMPLING_UNDERSAMPLING:
				SpreadSubsample spreadSubsample = new SpreadSubsample();
				// Choose uniform distribution for spread
				// (see: https://weka.sourceforge.io/doc.dev/weka/filters/supervised/instance/SpreadSubsample.html)
				opts = new String[] {"-M", "1.0"};
				spreadSubsample.setOptions(opts);
				spreadSubsample.setInputFormat(this.trainingSet);
				this.trainingSet = Filter.useFilter(trainingSet, spreadSubsample);
				break;
			case AnalysisProfile.SAMPLING_OVERSAMPLING:
				Resample resample = new Resample();
				// -B -> Choose uniform distribution
				// (see: https://weka.sourceforge.io/doc.dev/weka/filters/supervised/instance/Resample.html)
				// -Z -> From https://waikato.github.io/weka-blog/posts/2019-01-30-sampling/
				// "where Y/2 is (approximately) the percentage of data that belongs to the majority class"
				String z = Double.toString(2 * ((double) majoritySize / this.trainingSet.size()) * 100);
				opts = new String[] {"-B", "1.0", "-Z", z};
				resample.setOptions(opts);
				resample.setInputFormat(this.trainingSet);
				this.trainingSet = Filter.useFilter(this.trainingSet, resample);
				break;
			case AnalysisProfile.SAMPLING_SMOTE:
				SMOTE smote = new SMOTE();
				// Percentage of SMOTE instances to create
				// (see: https://weka.sourceforge.io/doc.packages/SMOTE/weka/filters/supervised/instance/SMOTE.html)
				String p = (minoritySize > 0) ? Double.toString(100.0 * (majoritySize - minoritySize) / minoritySize) : "100.0";
				opts = new String[] {"-P", p};
				smote.setOptions(opts);
				smote.setInputFormat(this.trainingSet);
				this.trainingSet = Filter.useFilter(this.trainingSet, smote);
				break;
			default:
				throw new IllegalArgumentException(String.format("Metodo di campionamento %s non valido", type));
		}
		
	}
	
	public void evaluate(AnalysisProfile analysisProfile) throws Exception {
		AbstractClassifier classifier = getBasicClassifier(analysisProfile.getClassifier());
		
		this.applyFeatureSelection(analysisProfile.getFeatureSelectionTechnique());
		this.applySampling(analysisProfile.getSamplingTechnique());
		
		CostSensitiveClassifier costSensitiveClassifier = new CostSensitiveClassifier();
		costSensitiveClassifier.setClassifier(classifier);
		costSensitiveClassifier.setCostMatrix(getCostMatrix());
		
		Evaluation evaluation = null;
		
		switch (analysisProfile.getCostSensitiveTechnique()) {
			case AnalysisProfile.COST_SENSITIVE_CLASSIFIER_NO:
				classifier.buildClassifier(this.trainingSet);
				evaluation = new Evaluation(this.testingSet);
				evaluation.evaluateModel(classifier, this.testingSet);
				break;
			case AnalysisProfile.COST_SENSITIVE_CLASSIFIER_SENSITIVE_THRESHOLD:
			case AnalysisProfile.COST_SENSITIVE_CLASSIFIER_SENSITIVE_LEARNING:
				costSensitiveClassifier.setMinimizeExpectedCost(analysisProfile.getCostSensitiveTechnique().equals(AnalysisProfile.COST_SENSITIVE_CLASSIFIER_SENSITIVE_THRESHOLD));
				costSensitiveClassifier.buildClassifier(trainingSet);
				evaluation = new Evaluation(testingSet, costSensitiveClassifier.getCostMatrix());
				evaluation.evaluateModel(costSensitiveClassifier, testingSet);
				break;
			default:
				throw new IllegalArgumentException(String.format("Tecnica di cost sensitive %s non valida", analysisProfile.getCostSensitiveTechnique()));
		}
		evalOutputs.setStatistics(evaluation);
	}
	
	public EvalOutputs getOutputs() {
		return this.evalOutputs;
	}
	
	private int getDefectsInTraining() {
		int counter = 0;
		for (Instance instance : this.trainingSet)
			if (instance.stringValue(instance.classIndex()).equals("Y"))
				counter++;
		return counter;
	}
	
	private int getDefectsInTesting() {
		int counter = 0;
		for (Instance instance : this.testingSet)
			if (instance.stringValue(instance.classIndex()).equals("Y"))
				counter++;
		return counter;
	}
	
	private List<Integer> getNumberOfYN() {
		int countYes = 0;
		int countNo = 0;
		for (Instance instance : this.trainingSet) {
			if (instance.stringValue(instance.classIndex()).equals("Y"))
				countYes++;
			else
				countNo++;
		}
		return Arrays.asList(countNo, countYes);
	}
	
	private CostMatrix getCostMatrix() {
		CostMatrix costMatrix = new CostMatrix(2);
		costMatrix.setCell(0, 0, 0.0);
		costMatrix.setCell(0, 1, AnalysisRun.CFP);
		costMatrix.setCell(1, 0, AnalysisRun.CFN);
		costMatrix.setCell(1, 1, 0.0);
		return costMatrix;
	}
	
	private AbstractClassifier getBasicClassifier(String type) {
		AbstractClassifier classifier;
		
		switch (type) {
			case AnalysisProfile.CLASSIFIER_RANDOM_FOREST:
				classifier = new RandomForest();
				break;
			case AnalysisProfile.CLASSIFIER_NAIVE_BAYES:
				classifier = new NaiveBayes();
				break;
			case AnalysisProfile.CLASSIFIER_IBK:
				classifier = new IBk();
				break;
			default:
				throw new IllegalArgumentException(String.format("Classificatore %s non valido", type));
		}
		return classifier;
	}
	
	private Remove getRemoveFilter(int []attributesSelected) throws Exception {
		Remove filter = new Remove();
		
		filter.setAttributeIndicesArray(attributesSelected);
		filter.setInvertSelection(true);
		filter.setInputFormat(trainingSet);
		
		return filter;
	}
	
	private int[] getSelectedIndexes() throws Exception {
		AttributeSelection filter = new AttributeSelection();
		CfsSubsetEval eval = new CfsSubsetEval();
		BestFirst search = new BestFirst();

		filter.setEvaluator(eval);
		filter.setSearch(search);
		filter.SelectAttributes(this.trainingSet);
		
		return filter.selectedAttributes();
	}
	
	public void removeUnwantedAttributes() throws Exception {
		int[] indices = new int[] {
				WekaAnalyser.VERSION_NAME_INDEX,
				WekaAnalyser.FILENAME_INDEX
		};
		
		Remove removeFilter = new Remove();
		removeFilter.setAttributeIndicesArray(indices);
		removeFilter.setInvertSelection(false);
		removeFilter.setInputFormat(this.trainingSet);
		
		this.trainingSet = Filter.useFilter(this.trainingSet, removeFilter);
		this.testingSet = Filter.useFilter(this.testingSet, removeFilter);
	}
}
