package it.uniroma2.isw2.milestone2.entities;

import weka.classifiers.Evaluation;

public class EvalOutputs {

	public static final String CSV_HEADER = "Dataset,#TrainingRelease,%Training,%Defective in training,%Defective in testing,Classifier,Balancing,Feature Selection,Sensitivity,TP,FP,TN,FN,Precision,Recall,AUC,Kappa\n";
	
	private String dataset;
	private int numberOfTrainingReleases;
	private double percentageOfTrainingReleases;
	private double percentageOfDefectiveInTraining;
	private double percentageOfDefectiveInTesting;
	private String classifier;
	private String balancing; 
	private String featureSelection;
	private String sensitivity;
	private double truePositive; 
	private double falsePositive;
	private double trueNegative;
	private double falseNegative;
	private double precision;
	private double recall;
	private double auc;
	private double kappa;
	
	public EvalOutputs(String dataset, AnalysisProfile analysisProfile) {
		this.dataset = dataset;
		this.classifier = analysisProfile.getClassifier();
		this.balancing = analysisProfile.getSamplingTechnique();
		this.featureSelection = analysisProfile.getFeatureSelectionTechnique();
		this.sensitivity = analysisProfile.getCostSensitiveTechnique();
	}
	
	public void setStatistics(Evaluation evaluation) {
		final int CLASS_INDEX = 1;
		this.truePositive = evaluation.truePositiveRate(CLASS_INDEX);
		this.falsePositive = evaluation.falsePositiveRate(CLASS_INDEX);
		this.trueNegative = evaluation.trueNegativeRate(CLASS_INDEX);
		this.falseNegative = evaluation.falseNegativeRate(CLASS_INDEX);
		this.precision = evaluation.precision(CLASS_INDEX);
		this.recall = evaluation.recall(CLASS_INDEX);
		this.auc = evaluation.areaUnderROC(CLASS_INDEX);
		this.kappa = evaluation.kappa();
	}
	
	public void setNumberOfTrainingReleases(int numberOfTrainingReleases) {
		this.numberOfTrainingReleases = numberOfTrainingReleases;
	}
	
	public void setPercentageOfTrainingReleases(double percentageOfTrainingReleases) {
		this.percentageOfTrainingReleases = percentageOfTrainingReleases;
	}
	
	public void setPercentageOfDefectiveInTraining(double percentageOfDefectiveInTraining) {
		this.percentageOfDefectiveInTraining = percentageOfDefectiveInTraining;
	}
	
	public void setPercentageOfDefectiveInTesting(double percentageOfDefectiveInTesting) {
		this.percentageOfDefectiveInTesting = percentageOfDefectiveInTesting;
	}
	
	public String toString() {
		return String.format("%s,%d,%.7f,%.7f,%.7f,%s,%s,%s,%s,%.7f,%.7f,%.7f,%.7f,%.7f,%.7f,%.7f,%.7f", this.dataset,
				this.numberOfTrainingReleases, this.percentageOfTrainingReleases, this.percentageOfDefectiveInTraining,
				this.percentageOfDefectiveInTesting, this.classifier, this.balancing, this.featureSelection,
				this.sensitivity, this.truePositive, this.falsePositive, this.trueNegative, this.falseNegative,
				this.precision, this.recall, this.auc, this.kappa);
	}
	
}
