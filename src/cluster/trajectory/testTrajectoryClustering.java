package cluster.trajectory;
import graphics.TrajectoryPlotter;

import java.awt.color.CMMException;
import java.io.*;
import java.security.AlgorithmConstraints;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import javax.lang.model.type.IntersectionType;

import dataset.TrajectoryDatasets;
import extras.AuxiliaryFunctions;


public class testTrajectoryClustering {
	
	public testTrajectoryClustering() {
		// TODO Auto-generated constructor stub
	}


	
	/**
	 * Main method to test all the experiments.
	 * Basically this class glues everything together and allows the easy execution 
	 * of the different trajectory clustering methods and their respective configuration,
	 * the selection of different datasets and the option to plot resulting clusters.
	 * @param args
	 */
	public static void main(String[] args) {
		
		ClusteringMethod method = ClusteringMethod.LSH_EUCLIDEAN_SLIDING_WIN;
		//ClusteringMethod method = ClusteringMethod.KMEANS_EUCLIDEAN;
		//starkeyElk93Experiment(method);
		boolean plotTrajectories = true;
		boolean simplifyTrajectories = true;
		boolean printDetailedClusters = false;
		boolean printOutputZayFile = true;
		boolean printOutputZayToScreen = false;
		boolean printConfusionMatrix = false;
		
		SegmentationMethod simplificationMethod = SegmentationMethod.douglasPeucker;
		TrajectoryDatasets trajectoryDataset = TrajectoryDatasets.LABOMNI;
		int numberOfPartitionsPerTrajectory = 25; //normal value = 8 //9 for tests with zay
		
		//For big data Experiment
		boolean veryBigData = false;
		int numTrajectoryBigDataset = 500;
		
		CVRRExperiment(method, trajectoryDataset, plotTrajectories, simplifyTrajectories, simplificationMethod,numberOfPartitionsPerTrajectory, veryBigData, numTrajectoryBigDataset, printOutputZayFile, printOutputZayToScreen, printConfusionMatrix, printDetailedClusters);
		
		//to evaluate the numbers of buckets produced by different numbers of hashing functions
		/*
		boolean plotHashFuntionsToNumBucketsGraph = true;
		int maxIterations = 20;
		int startingNumHashFunctions = 1;
		
		int totalHashesReached;
		int maxTotalOfHashesToReach = 6;
		do{
		totalHashesReached = evaluateProduceNumOfClusters(method, trajectoryDataset, plotHashFuntionsToNumBucketsGraph, simplifyTrajectories, simplificationMethod, 
				numberOfPartitionsPerTrajectory, maxIterations, startingNumHashFunctions);
		}while(totalHashesReached<maxTotalOfHashesToReach);
		*/
	}

	/**
	 * @param method 
	 * 
	 */
	private static void starkeyElk93Experiment(ClusteringMethod method) {
		// TODO Auto-generated method stub
		
		ArrayList<Trajectory> testTrajectories;
		//For my dummy trajectories
		//ArrayList<Trajectory> testTrajectoriesDummy = testTraclus.generateTestTrajectories();
		//testTrajectories = testTrajectoriesDummy;
		
		/* 
		//Parameters for Dummy Trajectories
		float eNeighborhoodParameter = (float) 0.01;
		int minLins = 3;
		int cardinalityOfClusters = 3;
		float MLDPrecision = (float) 0.0001;
		*/
		
		//For Microsoft geolife trajectories
		//ArrayList<Trajectory> testTrajectoriesGeolife = testTraclus.generateTestTrajectoriesFromDataSetMicrosoftGeolife(2);
		//testTrajectories = testTrajectoriesGeolife;
		
		/* 
		//Parameters for Geolife
		float eNeighborhoodParameter = (float) 0.01;
		int minLins = 3;
		int cardinalityOfClusters = 3;
		float MLDPrecision = (float) 0.0001;
		*/
		
		//Make sure to initilize this for final version
		Traclus traclus = null;
		
		//Partition Parameters
		SegmentationMethod segmentationMethod = SegmentationMethod.douglasPeucker;
		//SegmentationMethod segmentationMethod = SegmentationMethod.traclus;
		
		//General Parameters, might be overwritten
		float eNeighborhoodParameter = (float) 27;
		int minLins = 8;
		int cardinalityOfClusters = 9;
		float MLDPrecision = (float) 1;
		
		//Generate trajectories from DATA
		//Should have an enum and switch for different cases (generated data, starkey, microsoft data, hurricane data *not available*, soccer Data *not available.
		//For Starkley Animal trajectories to compara with paper
		ArrayList<Trajectory> testTrajectoriesStarkey = InputManagement.generateTestTrajectoriesFromDataSetAnimalsStarkey("E", 1993,MLDPrecision);	
		testTrajectories = testTrajectoriesStarkey;

		
		
		if(segmentationMethod == SegmentationMethod.traclus)
		{
		//Override Parameters for Starkey using traclus
		eNeighborhoodParameter = (float) 27;
		minLins = 8;
		cardinalityOfClusters = 9;
		
		//For Original Traclus Trajectory Partition 		
		traclus = new Traclus(testTrajectories, eNeighborhoodParameter, minLins, cardinalityOfClusters, segmentationMethod);
		//End of trajectory Partition via Original Traclus Partition Phase
		}
		
		if(segmentationMethod == SegmentationMethod.douglasPeucker)
		{
		//For Trajectory Partition using Douglas-Peucker
		double epsilonDouglasPeucker = 0.001;
		int fixNumberPartitionSegment = 32;
		
		//overwriting test parameters
		//eNeighborhoodParameter = (float) 27;
		
		//Parameter for DTW distance
		eNeighborhoodParameter = (float) 520000;
		minLins = 1;
		cardinalityOfClusters = 1;
		//end of douglas peucker ovewriten parameters for test
		traclus = new Traclus(testTrajectories, eNeighborhoodParameter, minLins, cardinalityOfClusters, epsilonDouglasPeucker, fixNumberPartitionSegment, segmentationMethod);
		}
		//End of Douglas Peucker
		
		//For previous Traclus implementation
		//ArrayList<Cluster> testClusters = traclus.executeTraclus();
		
		//Now working over Whole trajectory
		//ArrayList<Cluster> testClusters = traclus.executeDensityBasedClusterOverTrajectories();
		
		//For Kmeans for Whole trajectories
		ArrayList<Cluster> testClusters = traclus.executeKMeansClusterOverTrajectories(13);
		
		//Here print Real cluster data, we do not have those labels yet here

		printSetOfCluster(minLins, testClusters, false);
	}
	
	private static void CVRRExperiment(ClusteringMethod method, TrajectoryDatasets trajectoryDataset,
			boolean plotTrajectories, boolean simplifyTrajectories, SegmentationMethod simplificationMethod, 
			int fixNumberPartitionSegment, boolean veryBigData, int numTrajectoryBigDataset, boolean printOutputZayToFile, 
			boolean printOutputZayToScreen, 
			boolean printConfusionMatrix, boolean printDetailedClusters) {

		//Make sure to initilize this for final version
		Traclus traclus = null;
		
		//Partition Parameters
		//Default DP unless stated otherwise
		SegmentationMethod segmentationMethod = (simplificationMethod==null? SegmentationMethod.douglasPeucker : simplificationMethod);
		
		//General Parameters, might be overwritten
		float eNeighborhoodParameter = (float) 27;
		int minLins = 8;
		int cardinalityOfClusters = 9;
		float MLDPrecision = (float) 1;
		boolean strictSimplification = true;
		
		
		String dataset = getDatasetVariable(trajectoryDataset);
		
		
		ArrayList<Cluster> testClusters = new ArrayList<Cluster>();
		
		//Before clustering, lets simplify trajectories if we have to.
		//This have to be done here rather than in the clustering class to have a fair comparison.
		ArrayList<Trajectory> workingTrajectories = getTrajectories(simplifyTrajectories,
				fixNumberPartitionSegment, dataset);
		
		if(veryBigData)
		{
			workingTrajectories = bigDataset(numTrajectoryBigDataset, workingTrajectories); 
		}
			
		if(method == ClusteringMethod.TRACLUS)
		{
			//Override Parameters for Starkey using traclus
			segmentationMethod = SegmentationMethod.traclus;
		
			eNeighborhoodParameter = (float) 27;
			minLins = 8;
			cardinalityOfClusters = 9;
		
			//For Original Traclus Trajectory Partition 		
			traclus = new Traclus(workingTrajectories, eNeighborhoodParameter, minLins, cardinalityOfClusters, segmentationMethod);
			//End of trajectory Partition via Original Traclus Partition Phase
			
			/*
			 * 		//Parameter for DTW distance
			eNeighborhoodParameter = (float) 520000;
			minLins = 1;
			cardinalityOfClusters = 1;
			//end of douglas peucker ovewriten parameters for test
			traclus = new Traclus(workingTrajectories, eNeighborhoodParameter, minLins, cardinalityOfClusters, epsilonDouglasPeucker, fixNumberPartitionSegment, segmentationMethod);
			*/
			
			//For previous Traclus implementation
			testClusters = traclus.executeTraclus();
		}
		
		if(method == ClusteringMethod.DBSCAN_DTW)
		{
		segmentationMethod = SegmentationMethod.douglasPeucker;
		//For Trajectory Partition using Douglas-Peucker
		double epsilonDouglasPeucker = 0.001;
		fixNumberPartitionSegment = 8;
		
		//overwriting test parameters
		//eNeighborhoodParameter = (float) 27;
		
		//Parameter for DTW distance
		eNeighborhoodParameter = (float) 182; //worked for 8 segments minLins 1
		//eNeighborhoodParameter = (float) 86; // produced 15 clusters for 8 segments minLins 3 but some trajectories classified as noise, so errors on comparison output
		//eNeighborhoodParameter = (float) 340;	//for complete trajectories (expensive) and minLins 3
		eNeighborhoodParameter = (float) 3550;	//for complete trajectories (expensive) and minLins 1
		minLins = 1;
		cardinalityOfClusters = 1;
		//end of douglas peucker ovewriten parameters for test
		traclus = new Traclus(workingTrajectories, eNeighborhoodParameter, minLins, cardinalityOfClusters, epsilonDouglasPeucker, fixNumberPartitionSegment, segmentationMethod);
		
		//Now working over Whole trajectory
		testClusters = traclus.executeDensityBasedClusterOverTrajectories();
		}
		//End of Douglas Peucker
		

		if(method == ClusteringMethod.KMEANS_EUCLIDEAN)
		{
		segmentationMethod = SegmentationMethod.douglasPeucker;
		//For Trajectory Partition using Douglas-Peucker
		double epsilonDouglasPeucker = 0.001;
		fixNumberPartitionSegment = 9; //normal value = 8 //9 for tests with zay
		
		//overwriting test parameters
		//eNeighborhoodParameter = (float) 27;
		traclus = new Traclus(workingTrajectories, eNeighborhoodParameter, minLins, cardinalityOfClusters, epsilonDouglasPeucker, fixNumberPartitionSegment, segmentationMethod);
		

		//For Kmeans for Whole trajectories
		testClusters = traclus.executeKMeansClusterOverTrajectories(15);
		}

		if(method == ClusteringMethod.DBH_APPROXIMATION_DTW)
		{
		segmentationMethod = SegmentationMethod.douglasPeucker;
		//For Trajectory Partition using Douglas-Peucker
		double epsilonDouglasPeucker = 0.001;
		fixNumberPartitionSegment = 9;  //normal value = 8 //9 for tests with zay
		simplifyTrajectories = false; //Normally set to true but now false cause we are simplifying before.
		
		//Parameters only for DBH APPROXIMATION
		int minNumElems = 1;
		//float t1 = 0; //Find this parameter
		//float t2 = 1500; //Should be infinity
		int l = 1;
		int numBits = 5; //before was 9, but 10 bits produce crazy good results //Final value for old implementation settle to 12
		float mergeRatio = 1/2;
		boolean merge = false;
		
		traclus = new Traclus(workingTrajectories, eNeighborhoodParameter, minLins, cardinalityOfClusters, epsilonDouglasPeucker, fixNumberPartitionSegment, segmentationMethod);
		
		
		//I need to establish better parameters
		testClusters = traclus.executeDBHApproximationOfClusterOverTrajectories(l, numBits, minNumElems, merge, mergeRatio);
		}
		
		//For K-Medoids
		if(method == ClusteringMethod.KMEDOIDS_DTW)
		{
		//Call KMedoids here
			
			//For Trajectory Partition using Douglas-Peucker
			segmentationMethod = SegmentationMethod.douglasPeucker;
			
			//Parameters for Partition
			double epsilonDouglasPeucker = 0.001;
			fixNumberPartitionSegment = 9;  //normal value = 8 //9 for tests with zay
			simplifyTrajectories = true; //Normally set to true
			
			//Parameters for K-Medoids
			int k = 15;
			
			traclus = new Traclus(workingTrajectories, eNeighborhoodParameter, minLins, cardinalityOfClusters, epsilonDouglasPeucker, fixNumberPartitionSegment, segmentationMethod);
			
			//For Kmeans for Whole trajectories
			testClusters = traclus.executeKMedoidsClusterOverTrajectories(k);
			
		}
		
		//For K-MeansDTW
		if(method == ClusteringMethod.KMEANS_DTW)
		{
		//Call KMedoids here
			
			//For Trajectory Partition using Douglas-Peucker
			segmentationMethod = SegmentationMethod.douglasPeucker;
			
			//Parameters for Partition
			double epsilonDouglasPeucker = 0.001;
			
			//Parameters for K-Medoids
			int k = 15;
			
			traclus = new Traclus(workingTrajectories, eNeighborhoodParameter, minLins, cardinalityOfClusters, epsilonDouglasPeucker, fixNumberPartitionSegment, segmentationMethod);
			

			
			//For Kmeans for Whole trajectories
			testClusters = traclus.executeKmeansDTW(k);
			
		}
		
		//For LSH Using Euclidean distance
		if(method == ClusteringMethod.LSH_EUCLIDEAN)
		{
			//Parameters for Partition
			double epsilonDouglasPeucker = 0.001;
			
			//Parameters for LSH
			int minNumElems = 1;
			int numHashingFunctions = 10;
			int windowSize = 1000;
			
			traclus = new Traclus(workingTrajectories, eNeighborhoodParameter, minLins, cardinalityOfClusters, epsilonDouglasPeucker, fixNumberPartitionSegment, segmentationMethod);
			
			//For LSH EUCLIDEAN
			testClusters = traclus.executeLSHEuclidean(numHashingFunctions, windowSize, minNumElems);
			
		}
		
		//For LSH Using Euclidean distance
		if(method == ClusteringMethod.LSH_EUCLIDEAN_SLIDING_WIN)
		{
			//Parameters for Partition
			double epsilonDouglasPeucker = 0.001;
			
			//Parameters for LSH
			int minNumElems = 1;
			int numHashingFunctions = 5;
			int lshFunctionWindowSize = 100;
			int	slidingWindowSize = 4;
			int k = 15; //At the end we do K-Means over the feature vectors
			
			traclus = new Traclus(workingTrajectories, eNeighborhoodParameter, minLins, cardinalityOfClusters, epsilonDouglasPeucker, fixNumberPartitionSegment, segmentationMethod);
			
			//For LSH EUCLIDEAN
			testClusters = traclus.executeLSHEuclideanSlidingWindow(numHashingFunctions, lshFunctionWindowSize, minNumElems, slidingWindowSize, k);
			
		}
		
		//PrintReal Cluster Data
		ArrayList<Cluster> realClusters = new ArrayList<Cluster>();
		for(Trajectory t:workingTrajectories)
		{
			int clusterID = t.getClusterIdPreLabel();
			if(realClusters!=null && (realClusters.size()<=clusterID )) //|| realClusters.size()==0 
			{
				for(int j = realClusters.size(); j<=clusterID; j++)
				{
					Cluster c = new Cluster(j, "Cluster"+j);
					realClusters.add(c);
				}
			}
			
			
			if(realClusters!=null && realClusters.get(clusterID) != null)
			{
				realClusters.get(clusterID).addElement(t);
			}else{
				Cluster c = new Cluster(clusterID, "Cluster"+clusterID);
				c.addElement(t);
				
				realClusters.add(c);
			}
		}

		if(printDetailedClusters)
		{
			System.out.println("Real Clusters");
			printSetOfCluster(minLins, realClusters, false);
			System.out.println("Calculated Clusters: " + testClusters.size() + " Method: " + method );
			printSetOfCluster(minLins, testClusters, false);
		}
		
		//to Plot clusters
		if(plotTrajectories)
		{
			TrajectoryPlotter.drawAllClusters(testClusters);
			TrajectoryPlotter.drawAllClustersInSameGraph(testClusters);
		}
		//To calculate True negatives we need a HashSet of all trajectories in the initial
		//Dataset
		HashSet<Integer> allConsideredTrajectories = CommonFunctions.getHashSetAllTrajectories(workingTrajectories);

		compareClusters(realClusters, testClusters, allConsideredTrajectories, printConfusionMatrix);
		
		//Print Output for Zay to run other cluster statistics in Phyton
		if(printOutputZayToFile)
		{
			printTrajectoryLabels(testClusters, printOutputZayToScreen);
		}
		
		//System.out.println("Inverted Output");
		//compareClusters(testClusters, realClusters, allTrajectories);
	}

	/**
	 * Print for each cluster, the truth and predicted cluster label
	 * @param testClusters
	 */
	private static void printTrajectoryLabels(ArrayList<Cluster> testClusters, boolean printZayOutputToScreen) {
		System.out.println();
		String zayOutput = "****OUTPUT FOR ZAY, PYTHON CLUSTER QUALITY METRICS****\n";
		zayOutput = zayOutput + "Trajectory, True Cluster, Predicted Cluster\n";
		for(Cluster c: testClusters)
		{
			for(Trajectory t: c.getElementsAsTrajectoryObjects())
			{
				zayOutput = zayOutput + t.getTrajectoryId() + ", " + t.getClusterIdPreLabel() + ", " + c.clusterID + "\n";
			}
		}
		if(printZayOutputToScreen) System.out.println(zayOutput);
		
		String path;
		try {
			path = extras.GetPropertyValues.getPropValues("Zay_Output_Exported");
			AuxiliaryFunctions.printStringToFile(zayOutput, "OutputForZay.txt", path);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}



	/**
	 * @param minLins
	 * @param setOfClusterToPrint
	 */
	private static void printSetOfCluster(int minLins,
			ArrayList<Cluster> setOfClusterToPrint, boolean isTraclus) {
		//To print Clusters - Refactor this
		int totalTrajectories = 0;
		
		if(setOfClusterToPrint.isEmpty())
		{
			System.out.println("No clusters meet the parameters criteria");
		}
		else{
			for(Cluster c: setOfClusterToPrint)
			{
				if(isTraclus)
				c.calculateRepresentativeTrajectory(minLins, 0.00005);
				//System.out.println("Cluster: " + c.getClusterID());
				//System.out.println("Representative trajectory: " + c.getRepresentativeTrajectory().toString());
				System.out.println("Cluster: " + c.toString());
				totalTrajectories = totalTrajectories + c.elements.size();
			}

			System.out.println("Total Trajectories in Set of Clusters: " + totalTrajectories);
			System.out.println("*************************************************");
		}
	}

	/**
	 * This function compares 2 sets of clusters, one with labels considered baseline or gold standard
	 * and another generated by our trajectory clustering techniques.
	 * @param baselineSet
	 * @param testSet
	 * @param allTrajectories
	 * @param printConfusionMatrix 
	 */
	private static void compareClusters(ArrayList<Cluster> baselineSet, ArrayList<Cluster> testSet, HashSet<Integer> allTrajectories, boolean printConfusionMatrix)
	{
		//For Whole Method Statistics
		float methodPurity = 0;
		float methodCoverage = 0;
		float methodAccuracy = 0;
		float methodFMeasure = 0;
		float baselineCardinality = 0;
		float realClusterCardinality = 0;

		//Mutual Information
		float mutualInfo = 0;
		float methodNormalizedMutualInfo = 0;
		float entropyBaselineSet = 0;
		boolean testSetEntropyCalculated = false;
		float entropyTestSet = 0;
		
		System.out.println("Considered trajectories: " + allTrajectories.size());
		
		int numClustersNotProducedByMethod = 0;
		
		//Confusion matrix
		String confusionMatrix = "";
		
		//For confusion Matrix
		ArrayList<ArrayList<Integer>> confusionMatrixList = new ArrayList<ArrayList<Integer>>();
		
		for(Cluster cb: baselineSet)
		{
			
			int equivalentIndex=-1;
			float commonElements=0;
			float falsePositives=0;
			float falseNegatives=0;
			float trueNegatives=0;
		
			//For Entropy in Normalized Mutual Information
			float tempCBInfo = (float) cb.elements.size()/(float) allTrajectories.size();
			entropyBaselineSet -= tempCBInfo * Math.log10(tempCBInfo)/Math.log10(2);
			
			realClusterCardinality = cb.getElements().size();
			//true negatives(tni), i.e., the number of trajectories that do not belong to ci and they were
			//correctly assigned to a cluster different from ci
			
			//cb.calculateCardinality();
			
			//Just for testing purposes
			Cluster debugTestCluster = null;
			Cluster debugRealCluster = null;
			HashSet<Integer> commonDebug = new HashSet<Integer>();
			HashSet<Integer> notInAnySetDebug = new HashSet<Integer>();
			float totalElements = 0;
			
			//Row of confusion Matrix
			ArrayList<Integer> lineConfusionMatrix = new ArrayList<Integer>();
			lineConfusionMatrix.add(cb.getClusterID());
			
			for(Cluster ct: testSet)
			{
				ct.calculateCardinality();
				cb.calculateCardinality();
				HashSet<Integer> common = cb.getParentTrajectories();
				common.retainAll(ct.getParentTrajectories());
				
				//For Mutual Information
				if(common.size()>0)
				{
					double tempLogEntropy = Math.log10((allTrajectories.size()*common.size())/((double)(cb.getElements().size()*ct.cardinality)))/Math.log10(2);
					mutualInfo += ((double) common.size()/(double) allTrajectories.size())*tempLogEntropy;
				}
				
				//For Entropy in Normalized Mutual Information
				if(!testSetEntropyCalculated)
				{
					float tempCTInfo = (float) (ct.elements.size()/(float) allTrajectories.size());
					entropyTestSet -= tempCTInfo * Math.log10(tempCTInfo)/Math.log10(2);
				}
				
				//For Confusion Matrix
				int intersection = 0;
				
				if(commonElements<common.size())
				{
					commonElements = common.size();
					equivalentIndex = ct.getClusterID();
					cb.calculateCardinality();
					falsePositives = ct.cardinality - commonElements;
					falseNegatives = cb.cardinality - commonElements;
					
					//For trueNegatives
					HashSet<Integer> notInAnySet = new HashSet<Integer>();
					notInAnySet.addAll(allTrajectories);
					notInAnySet.removeAll(cb.getParentTrajectories());
					notInAnySet.removeAll(ct.getParentTrajectories());

					trueNegatives = notInAnySet.size();
					
					//For confusion Matrix
					intersection = (int) commonElements;
					
					//*****************************
					//For Debugging
					totalElements = commonElements + falseNegatives + falsePositives + trueNegatives;
					//if(totalElements>allTrajectories.size())
					if(falseNegatives>0)
					{
						cb.calculateCardinality();
					//debugging variable
					debugTestCluster = ct;
					debugRealCluster = cb;
					commonDebug.addAll(common);
					notInAnySetDebug.addAll(notInAnySet);
					}
					//***************************
				}
				//Also for Confusion Matrix
				lineConfusionMatrix.add((int) (intersection));
			}
			//For Normalized Mutual Information
			testSetEntropyCalculated = true;
			
			//For confusion Matrix
			//Save label of test Cluster
			if(cb.getClusterID()==0)
			{
				confusionMatrix = confusionMatrix + "Test Clusters->";
				for(Cluster tempTestCluster:testSet)
				{
					confusionMatrix = confusionMatrix + "\t|" + tempTestCluster.getClusterID();
				}
			}
			
			//For confusion Matrix
			confusionMatrixList.add(lineConfusionMatrix);
			
			System.out.println("\n");
			
			totalElements = commonElements + falseNegatives + falsePositives + trueNegatives;
			
			if(totalElements>allTrajectories.size())
			{
				System.err.println("Bug is here, check this out.");
				System.err.println("Total Elements: " + totalElements);
				System.err.println("Real cluster trajectories number: " + allTrajectories.size());
				System.err.println("Test Cluster: " + debugTestCluster.toStringComplete());
				System.err.println("Real Cluster: " + debugRealCluster.toStringComplete());
				System.err.println("Common Set: " + commonDebug);
				System.err.println("Not in any Set: " + notInAnySetDebug);
			}
				
			if(equivalentIndex>-1)
			{
			System.out.println("Real Cluster: " + cb.getClusterID() 
					+ " Equivalent test Cluster: " + equivalentIndex
					+ " Common Elements (TP): " + commonElements
					+ " False Positives (FP): " + falsePositives
					+ " False Negatives (FN): " + falseNegatives
					+ " True Negatives  (TN): " + trueNegatives
					+ " Total Elements : " + totalElements);
			
			float purity = commonElements/(commonElements + falsePositives);
			float coverage = commonElements/(commonElements + falseNegatives);
			float accuracy = (commonElements+trueNegatives)/(commonElements + trueNegatives + falsePositives + falseNegatives);
			float fMeasure = 10*(purity * coverage)/( 9* purity + coverage);
			System.out.println("Per Cluster Stats for Cluster " +  cb.getClusterID());
			System.out.println("Purity: 	" +  purity);
			System.out.println("Coverage: 	" +  coverage);
			System.out.println("Accuracy: 	" +  accuracy);
			System.out.println("F-Measure:	" +  fMeasure);
			
			/*
			methodPurity += purity;
			methodCoverage += coverage;
			methodAccuracy += accuracy;
			methodFMeasure += fMeasure;*/
			
			//New weighted score
			methodPurity += (purity  * realClusterCardinality);
			methodCoverage += (coverage * realClusterCardinality);
			methodAccuracy += (accuracy * realClusterCardinality);
			methodFMeasure += (fMeasure * realClusterCardinality);
			baselineCardinality += realClusterCardinality;
			
			}else{
				System.out.println("Real Cluster: " + cb.getClusterID() 
						+ " Equivalent test Cluster: NONE");
				System.out.println("Not produced by the clustering method, no intersects!!!");
				numClustersNotProducedByMethod++;
			}
		}
		
		int sizeNormalizer = baselineSet.size() - numClustersNotProducedByMethod;
		
		/*
		methodPurity = methodPurity/sizeNormalizer;
		methodCoverage = methodCoverage/sizeNormalizer;
		methodAccuracy = methodAccuracy/sizeNormalizer;
		methodFMeasure = methodFMeasure/sizeNormalizer;
		*/
		
		//For weighted score
		methodPurity = methodPurity/baselineCardinality;
		methodCoverage = methodCoverage/baselineCardinality;
		methodAccuracy = methodAccuracy/baselineCardinality;
		methodFMeasure = methodFMeasure/baselineCardinality;
		
		methodNormalizedMutualInfo = mutualInfo/((entropyBaselineSet+entropyTestSet)/2);
		
		System.out.println("\n");
		System.out.println("Per Method Statistics: ");
		System.out.println("Purity: 	" +  methodPurity);
		System.out.println("Coverage: 	" +  methodCoverage);
		System.out.println("Accuracy: 	" +  methodAccuracy);
		System.out.println("F-Measure: 	" +  methodFMeasure);
		System.out.println("Normalized Mutual Information (NMI): " +  methodNormalizedMutualInfo);
		
		//Now print Confusion Matrix
		int lenghtOfRowLine = confusionMatrix.length();
		confusionMatrix = confusionMatrix + "\n";
		for(int j=0; j<lenghtOfRowLine; j++)
		{
			confusionMatrix = confusionMatrix + "--";
		}
		confusionMatrix = confusionMatrix + "-----\n";
		char[] realClusterLegend = " Real Clusters".toCharArray();
		
		int legendIndex = 0;
		for(ArrayList<Integer> RowConfMatrix:confusionMatrixList)
		{
			char tempLegend = ' ';
			if(realClusterLegend.length>legendIndex)
			{
			tempLegend = realClusterLegend[legendIndex];
			}
			
			confusionMatrix = confusionMatrix + tempLegend + "\t";
			for(Integer i:RowConfMatrix)
			{
				confusionMatrix = confusionMatrix + i + "\t|";
			}
			confusionMatrix = confusionMatrix +"\n";
			legendIndex++;
		}
		if(printConfusionMatrix)
		{
			System.out.println("\n");
			System.out.println("**************Confusion Matrix***********");
			System.out.println("Rows: \t\tReal Clusters");
			System.out.println("Columns: \tTest Clusters");
			System.out.println("Cells represent common elements between clusters.");
			System.out.println("");		
			System.out.println(confusionMatrix);
		}
	}
	
	//New experiments Rao
	
	/**
	 * Method to evaluate how hashing functions perform and when the produced clusters decay.
	 * @param method : The Clustering Method. Has to be a Hashing method (LSH or DBH)
	 * @param trajectoryDataset
	 * @param plotGraph
	 * @param simplifyTrajectories
	 * @param simplificationMethod
	 * @param fixNumberPartitionSegment
	 */
	public static int evaluateProduceNumOfClusters(ClusteringMethod method, TrajectoryDatasets trajectoryDataset,
			boolean plotGraph, boolean simplifyTrajectories, SegmentationMethod simplificationMethod, int fixNumberPartitionSegment, int maxIterations, int startingNumHashFunctions)
	{	
		int numOfNonZeroClusters = -1;
		int previousNumOfNonZeroClusters = -2;
		int numIterations=0;
		int numHashFunctions = startingNumHashFunctions;
		
		HashMap<Integer, Integer> hashesPerIteration = new HashMap<Integer, Integer>(); 
		HashMap<Integer, Integer> minNumElemNon0ClusterPerIteration = new HashMap<Integer, Integer>(); 
		
		SegmentationMethod segmentationMethod = (simplificationMethod==null? SegmentationMethod.douglasPeucker : simplificationMethod);
		String dataset = getDatasetVariable(trajectoryDataset);
		ArrayList<Trajectory> workingTrajectories = getTrajectories(simplifyTrajectories, fixNumberPartitionSegment, dataset);

		do{
		previousNumOfNonZeroClusters = numOfNonZeroClusters;
		//Partition Parameters
		//Default DP unless stated otherwise
	
		ArrayList<Cluster> testClusters = new ArrayList<Cluster>();
				
		//Before clustering, lets simplify trajectories if we have to.
		//This have to be done here rather than in the clustering class to have a fair comparison.
		TrajectoryClustering tc = new TrajectoryClustering(workingTrajectories);
		int minNumElems = 1;
		
			if(method == ClusteringMethod.DBH_APPROXIMATION_DTW)
			{
				//Parameters only for DBH APPROXIMATION
				int l = 1;
				float mergeRatio = 1/2;
				boolean merge = false;
				
				
				testClusters = tc.executeDBHApproximationOfClusterOverTrajectories(l, numHashFunctions, minNumElems, merge, mergeRatio);
				
			}
			
			if(method == ClusteringMethod.LSH_EUCLIDEAN)
			{
				//Parameters for LSH
				int windowSize = 500;

				testClusters = tc.executeLSHEuclidean(numHashFunctions, windowSize, minNumElems);
			}
			
			//For Minimun Number of Elements in a Cluster from a set of Clusters per Number of hash Functions
			int minNumElementsCluster = Integer.MAX_VALUE;
			for(Cluster c:testClusters)
			{
				if(c.elements.size()<minNumElementsCluster)
				{
					minNumElementsCluster = c.elements.size();
				}
			}
			minNumElemNon0ClusterPerIteration.put(numHashFunctions, minNumElementsCluster);
			//*********************
			
			numOfNonZeroClusters = testClusters.size();
			//For total Number of Clusters in set of Clusters per Number of hash Functions
			hashesPerIteration.put(numHashFunctions, numOfNonZeroClusters);
			numHashFunctions++;
			numIterations++;
		}while(previousNumOfNonZeroClusters<numOfNonZeroClusters && numIterations<maxIterations);
		
		System.out.println("\n");
		System.out.println("Hashing Functions -> Number of Non-Zero Clusters");
		System.out.println(hashesPerIteration);
		
		System.out.println("\n");
		System.out.println("Hashing Functions -> Minimun Number of Elements in a Cluster (Non-Zero Cluster)");
		System.out.println(minNumElemNon0ClusterPerIteration);
		
		return hashesPerIteration.size();
	}

	/**
	 * Gets the trajectories, as an ArrayList of trajectories.
	 * @param simplifyTrajectories : Determine if trajectories are simplified
	 * @param fixNumberPartitionSegment : Determines the number of partitions on each of the return trajectories
	 * @param dataset : The dataset to work on
	 * @return ArrayList of Trajectories.
	 */
	private static ArrayList<Trajectory> getTrajectories(boolean simplifyTrajectories,
			int fixNumberPartitionSegment, String dataset) {
		ArrayList<Trajectory> workingTrajectories;
		if(simplifyTrajectories)
		{
			String path = System.getProperty("user.dir") + "\\Simplified points\\";
			OutputManagement.ExportReducedTrajectories(path, dataset, fixNumberPartitionSegment);
			String exported = "CVRR_Dataset_Exported";
			workingTrajectories = InputManagement.generateTestTrajectoriesFromDataSetCVRR(exported, simplifyTrajectories, dataset);
			
		}else{
			workingTrajectories = InputManagement.generateTestTrajectoriesFromDataSetCVRR(dataset, simplifyTrajectories, null);
		}
		return workingTrajectories;
	}
	
	private static ArrayList<Trajectory> bigDataset(int numberOfTrajectories, ArrayList<Trajectory> previousTrajectories)
	{
		ArrayList<Trajectory> workingTrajectories = new ArrayList<Trajectory>();
		workingTrajectories.addAll(previousTrajectories);
		int generatedTrajectoryId = previousTrajectories.size();
		while(generatedTrajectoryId<numberOfTrajectories)
		{
			for(Trajectory t: previousTrajectories)
			{
				Trajectory temp = (Trajectory) t.clone();
				temp.setTrajectoryId(generatedTrajectoryId);
				workingTrajectories.add(temp);
				generatedTrajectoryId++;
				if(generatedTrajectoryId == numberOfTrajectories)
				{
					break;
				}
			}
		}
		
		//For debugging Only
		/*
		System.out.println("**********BIG DATASET*********");
		System.out.println(workingTrajectories);
		*/
		
		return workingTrajectories;
	}

	/**
	 * This method returns the name of the Property value to lookup for the dataset path.
	 * @param trajectoryDataset
	 * @param dataset
	 * @return
	 */
	private static String getDatasetVariable(TrajectoryDatasets trajectoryDataset) 
	{
		String dataset = "";
		if(trajectoryDataset == TrajectoryDatasets.LABOMNI)
		{
			dataset = "CVRR_Dataset_Labomni_Path";
		}
		
		if(trajectoryDataset == TrajectoryDatasets.CROSS)
		{
			dataset = "CVRR_Dataset_Cross_Path";
		}
		return dataset;
	}
}