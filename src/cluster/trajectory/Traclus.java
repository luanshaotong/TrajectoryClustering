package cluster.trajectory;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import cluster.trajectory.*;

//import com.stromberglabs.cluster.Cluster;
//import com.stromberglabs.cluster.Clusterable;
import com.stromberglabs.cluster.KMeansClusterer;

import extras.AuxiliaryFunctions;
import extras.GetPropertyValues;
import extras.TimeKeeping;
import fastdtw.com.dtw.DTW;
import graphics.TrajectoryPlotter;


public class Traclus {

	private ArrayList<Trajectory> trajectories;
	private ArrayList<Segment> segmentsCompleteSet;
	private ArrayList<Cluster> clusterOfTrajectories;
	private float eNeighborhoodParameter;
	private int minLins;
	private int cardinalityOfClusters;
	
	private double epsilonDouglasPeucker;
	private int fixedNumOfTrajectoryPartitionsDouglas;
	
	//To select Segmentation Method
	SegmentationMethod segmentationMethod;
	//Parameters
	//Notion of E Neighbourhood
	//Notion of acceptable time distance
	//
	ArrayList<Cluster> setOfClusters;
	
	//Constructor for Douglas Peucker Partition Traclus
	public Traclus(ArrayList<Trajectory> trajectories, float eNeighborhoodParameter, int minLins, int cardinalityOfClusters, 
			double epsilonDouglas, int fixedNumberTrajPartitionDouglas, SegmentationMethod segmentationMethod) {
		this.trajectories = trajectories;
		this.segmentsCompleteSet = new ArrayList<Segment>();
		this.clusterOfTrajectories = new ArrayList<Cluster>();
		this.eNeighborhoodParameter = eNeighborhoodParameter;
		this.minLins = minLins;
		this.cardinalityOfClusters = cardinalityOfClusters;
		this.fixedNumOfTrajectoryPartitionsDouglas = fixedNumberTrajPartitionDouglas;
		this.epsilonDouglasPeucker = epsilonDouglas;
		this.segmentationMethod = segmentationMethod;
	}
	
	//Constructor for Original Partition Method Traclus
	public Traclus(ArrayList<Trajectory> trajectories, float eNeighborhoodParameter, int minLins, int cardinalityOfClusters, SegmentationMethod segmentationMethod) {
		this.trajectories = trajectories;
		this.segmentsCompleteSet = new ArrayList<Segment>();
		this.clusterOfTrajectories = new ArrayList<Cluster>();
		this.eNeighborhoodParameter = eNeighborhoodParameter;
		this.minLins = minLins;
		this.cardinalityOfClusters = cardinalityOfClusters;
		this.segmentationMethod = segmentationMethod;
	}
	
	//New more Generic Constructor usable for all clustering Methods
	/**
	 * Constructor that only holds a copy of the trajectories, only thing we need to cluster in all methods.
	 * This Class no longer represents only Traclus, it should be refactored.
	 * This Generic Constructor should eventually replace the older constructors in later Refactorings
	 * @param trajectories
	 */
	public Traclus(ArrayList<Trajectory> trajectories) {
		this.trajectories = trajectories;
		this.segmentsCompleteSet = new ArrayList<Segment>();
		this.clusterOfTrajectories = new ArrayList<Cluster>();
	}
	
	public ArrayList<Cluster> executeTraclus()
	{
		segmentsCompleteSet = partition(trajectories);
		
		
		long startTime = System.nanoTime();
		
		//for Traclus clustering approach, cluster over segments
		clusterOfTrajectories = clusterSegments(segmentsCompleteSet, eNeighborhoodParameter, minLins, cardinalityOfClusters);
		
		long stopTime = System.nanoTime();
		double finalTimeInSeconds = (stopTime - startTime)/1000000000.0;
		System.out.println("Clustering Execution time in seconds: " + (finalTimeInSeconds));
		testTrajectoryClustering.timesClustering.add(finalTimeInSeconds);
		return clusterOfTrajectories;
	}
	
	/**
	 * To do the density base clustering over the whole trajectory
	 * and using DTW as a distance (similarity metric).
	 * @return
	 */
	public ArrayList<Cluster> executeDensityBasedClusterOverTrajectories()
	{
		ArrayList<Trajectory> workingTrajectories = trajectories;
		
		long startTime = System.nanoTime();
		
		//For new Rao Approach, do clustering over trajectories.
		//Form clustering over DTW
		clusterOfTrajectories = clusterTrajectoriesWithDTW(workingTrajectories, eNeighborhoodParameter, minLins, cardinalityOfClusters);
		

		long stopTime = System.nanoTime();
		double finalTimeInSeconds = (stopTime - startTime)/1000000000.0;
		System.out.println("Clustering Execution time in seconds: " + (finalTimeInSeconds));
		testTrajectoryClustering.timesClustering.add(finalTimeInSeconds);
		return clusterOfTrajectories;
	}
	
	public ArrayList<Cluster> executeKMeansDTW(int k, int minNumElems, boolean calculateSilhouetteCoefficient)
	{
		//ArrayList<Trajectory> simplifiedTrajectories = simplifyTrajectories(trajectories, true, segmentationMethod, fixedNumOfTrajectoryPartitionsDouglas);
		
		ArrayList<Trajectory> workingTrajectories = trajectories;
		long startTime = System.nanoTime();
		
		//Export trajectories to visualize them
		//exportPlotableCoordinatesForAllTrajectories(simplifiedTrajectories);
		
		//For new Rao Approach, do clustering over trajectories.
		//Form clustering over DTW
		clusterOfTrajectories = clusterTrajectoriesKMeansEuclidean(workingTrajectories, k, calculateSilhouetteCoefficient);		
		
		long stopTime = System.nanoTime();
		double finalTimeInSeconds = (stopTime - startTime)/1000000000.0;
		System.out.println("Clustering Execution time in seconds: " + (finalTimeInSeconds));
		testTrajectoryClustering.timesClustering.add(finalTimeInSeconds);
		clusterOfTrajectories = Cluster.keepClustersWithMinElements(clusterOfTrajectories, minNumElems);
		return clusterOfTrajectories;
	}
	
	public ArrayList<Cluster> executeKMedoidsDTW(int k, int minNumElems) {

		ArrayList<Trajectory> workingTrajectories = trajectories;
		
		
		long startTime = System.nanoTime();
		
		//Export trajectories to visualize them
		//exportPlotableCoordinatesForAllTrajectories(simplifiedTrajectories);
		
		//Now we are trying to obtain centroids based in DTW metric and not in Euclidean Distance like K-Means
		clusterOfTrajectories = clusterTrajectoriesKMedoids(workingTrajectories, k, TrajectoryDistance.DTW);		
		
		long stopTime = System.nanoTime();
		double finalTimeInSeconds = (stopTime - startTime)/1000000000.0;
		System.out.println("Clustering Execution time in seconds: " + (finalTimeInSeconds));
		testTrajectoryClustering.timesClustering.add(finalTimeInSeconds);
		clusterOfTrajectories = Cluster.keepClustersWithMinElements(clusterOfTrajectories, minNumElems);
		return clusterOfTrajectories;
	}
	
	public ArrayList<Cluster> executeKmeansDTW(int k, int minNumElems) {
	
		ArrayList<Trajectory> workingTrajectories = trajectories;

		long startTime = System.nanoTime();

		//Export trajectories to visualize them
		//exportPlotableCoordinatesForAllTrajectories(simplifiedTrajectories);
		
		//Now we are trying to obtain centroids based in DTW metric and not in Euclideand Distance like K-Means
		clusterOfTrajectories = clusterTrajectoriesKMeansDTW(workingTrajectories, k);		
		
		long stopTime = System.nanoTime();
		double finalTimeInSeconds = (stopTime - startTime)/1000000000.0;
		System.out.println("Clustering Execution time in seconds: " + (finalTimeInSeconds));
		testTrajectoryClustering.timesClustering.add(finalTimeInSeconds);
		clusterOfTrajectories = Cluster.keepClustersWithMinElements(clusterOfTrajectories, minNumElems);
		return clusterOfTrajectories;
	}
	

	public ArrayList<Cluster> executeDBHApproximationOfClusterOverTrajectories(int l, int numBits, int minNumElems, boolean merge, float mergeRatio)
	{
		ArrayList<Trajectory> workingTrajectories = trajectories;
				
		long startTime = System.nanoTime();
		
		//Export trajectories to visualize them
		//exportPlotableCoordinatesForAllTrajectories(simplifiedTrajectories);
		
		//For new Rao Approach, do clustering over trajectories.
		//Form clustering over DTW
		try {
			//Old clustering with t1, t2 precalculated for all functions (wrong way but gave results)
			//clusterOfTrajectories = approximateClustersDBH(simplifiedTrajectories, l, numBits, t1, t2, minNumElems, merge, mergeRatio);
			
			//New corrected way as suggested by zay
			clusterOfTrajectories = approximateClustersDBH(workingTrajectories, l, numBits, minNumElems, merge, mergeRatio);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.err.println("Error, some hash functions have no interval t1-t2 defined.");
			System.err.print(e.getMessage());
			e.printStackTrace();
		}
		
		long stopTime = System.nanoTime();
		double finalTimeInSeconds = (stopTime - startTime)/1000000000.0;
		System.out.println("Clustering Execution time in seconds: " + (finalTimeInSeconds));
		testTrajectoryClustering.timesClustering.add(finalTimeInSeconds);
		clusterOfTrajectories = Cluster.keepClustersWithMinElements(clusterOfTrajectories, minNumElems);
		return clusterOfTrajectories;
	}
	
	public ArrayList<Cluster> executeDBHOverFeatureVectorTrajectories(int numBits, int minNumElems, int k, boolean isBinaryFeatureVector, boolean saveFeatureVectorsToFile, boolean calculateSilhouetteCoefficient,
			boolean normalize) {
		
		ArrayList<Trajectory> workingTrajectories = trajectories;
		
		long startTime = System.nanoTime();
		
		//As suggested by Zay, lets try to do K means over the Feature vectors generated from dbh
		try {
			clusterOfTrajectories = approximateClustersDBHFeatureVector(workingTrajectories, numBits, k, isBinaryFeatureVector, saveFeatureVectorsToFile, calculateSilhouetteCoefficient, normalize);
		} catch (Exception e) {
			System.err.print(e.getMessage());
			e.printStackTrace();
		}
		
		
		/*
		 * Do not print this time because this method has 2 different processes with its own time 
		 * and other intermediate Printing states that should not add up to the total clustering time.
		long stopTime = System.nanoTime();
		double finalTimeInSeconds = (stopTime - startTime)/1000000000.0;
		System.out.println("Clustering Execution time in seconds: " + (finalTimeInSeconds));
		*/
		
		clusterOfTrajectories = Cluster.keepClustersWithMinElements(clusterOfTrajectories, minNumElems);		
		return clusterOfTrajectories;
	}

	/**
	 * This Method calculates approximate clusters using LSH to project Euclidean Distance (L2 Norm).
	 * @return Clusters
	 */
	public ArrayList<Cluster> executeLSHEuclidean(int numHashingFunctions, int windowSize, int minNumElems) {

		ArrayList<Trajectory> workingTrajectories = trajectories;
		
		long startTime = System.nanoTime();
		
		try {

			clusterOfTrajectories = approximateClustersLSHEuclidean(workingTrajectories, numHashingFunctions, windowSize, minNumElems);

		} catch (Exception e) {
			System.err.print(e.getMessage());
			e.printStackTrace();
		}
		
		long stopTime = System.nanoTime();
		double finalTimeInSeconds = (stopTime - startTime - TimeKeeping.wastedTime)/1000000000.0;
		System.out.println("Non clustering time: " + TimeKeeping.wastedTime/1000000000.0);
		System.out.println("Clustering Execution time in seconds: " + (finalTimeInSeconds));
		testTrajectoryClustering.timesClustering.add(finalTimeInSeconds);
		clusterOfTrajectories = Cluster.keepClustersWithMinElements(clusterOfTrajectories, minNumElems);
		return clusterOfTrajectories;
	}
	
	/**
	 * LSH with sliding windows, where each segment from a trajectory inside the window is hashed into a feature vector.
	 * @param numHashingFunctions
	 * @param lshFunctionWindowSize : Parameter W of LSH Functions, not to be confused with the sliding window size.
	 * @param minNumElems
	 * @param slidingWindowSize : Size of the sliding window
	 * @param k 
	 * @return
	 */
	public ArrayList<Cluster> executeLSHEuclideanSlidingWindow(int numHashingFunctions, int lshFunctionWindowSize, int minNumElems, int slidingWindowSize, int k, boolean calculateSilhouetteCoefficient) 
		{

		ArrayList<Trajectory> workingTrajectories = trajectories;
		
		long startTime = System.nanoTime();
		
		try {

			clusterOfTrajectories = approximateClustersLSHEuclideanWindowSize(workingTrajectories, numHashingFunctions, lshFunctionWindowSize, minNumElems, slidingWindowSize, k, calculateSilhouetteCoefficient);

		} catch (Exception e) {
			System.err.print(e.getMessage());
			e.printStackTrace();
		}
		
		long stopTime = System.nanoTime();
		double finalTimeInSeconds = (stopTime - startTime - TimeKeeping.wastedTime)/1000000000.0;
		System.out.println("Non clustering time: " + TimeKeeping.wastedTime/1000000000.0);
		System.out.println("Clustering Execution time in seconds: " + (finalTimeInSeconds));
		testTrajectoryClustering.timesClustering.add(finalTimeInSeconds);
		clusterOfTrajectories = Cluster.keepClustersWithMinElements(clusterOfTrajectories, minNumElems);
	
		return clusterOfTrajectories;

	}

	public ArrayList<Cluster> executeKmeansLCSS(int numBits, int minNumElems, int k)
	{
		ArrayList<Trajectory> workingTrajectories = trajectories;
		
		long startTime = System.nanoTime();
		
		try {
			//TODO Implement Kmeans LCSS
			throw new UnsupportedOperationException("Still need to implement Kmeans over LCSS");
			//clusterOfTrajectories = approximateClustersLSHEuclidean(workingTrajectories, numHashingFunctions, windowSize, minNumElems);

		} catch (Exception e) {
			System.err.print(e.getMessage());
			e.printStackTrace();
		}
		
		long stopTime = System.nanoTime();
		double finalTimeInSeconds = (stopTime - startTime - TimeKeeping.wastedTime)/1000000000.0;
		System.out.println("Non clustering time: " + TimeKeeping.wastedTime/1000000000.0);
		System.out.println("Clustering Execution time in seconds: " + (finalTimeInSeconds));
		testTrajectoryClustering.timesClustering.add(finalTimeInSeconds);
		clusterOfTrajectories = Cluster.keepClustersWithMinElements(clusterOfTrajectories, minNumElems);
		return clusterOfTrajectories;
	}
	
	public ArrayList<Cluster> executeKmedoidsLCSS(int minNumElems, int k) 
	{
		ArrayList<Trajectory> workingTrajectories = trajectories;
		
		long startTime = System.nanoTime();
		
		try {

			clusterOfTrajectories = clusterTrajectoriesKMedoids(workingTrajectories, k, TrajectoryDistance.LCSS);

		} catch (Exception e) {
			System.err.print(e.getMessage());
			e.printStackTrace();
		}
		
		long stopTime = System.nanoTime();
		double finalTimeInSeconds = (stopTime - startTime - TimeKeeping.wastedTime)/1000000000.0;
		System.out.println("Non clustering time: " + TimeKeeping.wastedTime/1000000000.0);
		System.out.println("Clustering Execution time in seconds: " + (finalTimeInSeconds));
		testTrajectoryClustering.timesClustering.add(finalTimeInSeconds);
		clusterOfTrajectories = Cluster.keepClustersWithMinElements(clusterOfTrajectories, minNumElems);
		return clusterOfTrajectories;
	}


	//3 clear stages
	//Partition Phase
	//Input Set of Trajectories
	public ArrayList<Segment> partition(ArrayList<Trajectory> trajectories)
	{
		System.out.println("start segmentation...");
		ArrayList<Segment> setOfSegments = new ArrayList<Segment>();
		
		for(Trajectory t:trajectories)
		{
			
			//For Traclus Processing of Partition of trajectory into segments
			if(segmentationMethod == SegmentationMethod.traclus)
			setOfSegments.addAll(t.divideTrajectoryInSegmentsTraclus());
			
			if(segmentationMethod == SegmentationMethod.douglasPeucker)
			//For Douglas-Peucker Partition of trajectory into segments
			setOfSegments.addAll(t.divideTrajectoryInSegmentsDouglasPeucker(epsilonDouglasPeucker, fixedNumOfTrajectoryPartitionsDouglas));
		}

		System.out.println("end segmentation.");
		return setOfSegments;
	}
	

	
	//Only to stop the flow of information in console
	private void interrupt(int time)
	{
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void exportPlotableCoordinatesForAllTrajectories(ArrayList<Trajectory> trajectories)
	{
		try {
			String filename = "exportedTrajectoriesToPlot.txt";
			File file = new File(filename);
			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			
			for(Trajectory t:trajectories)
			{
					//*****************Just to print the simplified trajectories****************
					//System.out.println(simplifiedTrajectory.printLocation());
					bw.write(t.printToPlotWithOtherTrajectories());
					//Just to print the simplified trajectories
					//interrupt();
					//*****************End of Print trajectories***************
			}
			bw.close();
			//System.out.println("Done");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//********************HASHING STAGE*********************************
	
	public double maxDTW(Trajectory t, ArrayList<Trajectory> simplifiedTrajectories)
	{
		
		double maxDTW = 0;
		for(Trajectory t0:simplifiedTrajectories)
		{
			 double tempDist = Trajectory.calculateDTWDistance(t,t0);
			 if(maxDTW<tempDist)
			 {
				 maxDTW = tempDist;
			 }
		}
		
		System.out.println("Maximun DTW distance from 0" + maxDTW);
		return maxDTW;
		
	}
	
	//DBH HASHING
	public ArrayList<Cluster> approximateClustersDBH(ArrayList<Trajectory> simplifiedTrajectories, int l, int kBits, int minNumElems, boolean merge, float mergeRatio) throws Exception
	{
		

		//Verify the interval???
		
		//Let X be a non metric space and D a distance function defined in X (X,D).
		//Let H be a family of hash functions h:X->Z, where Z is the set of integers
		//First pick integers k and l.
		//Then construct l hash functions g1,g2,...,gl as concatenations of k functions
		//chosen randomly from the family H.
		// gi(x) = (hi1(x),hi2(x),...,hik(x))
		// each db object is stored in each of the l hash tables defined by the functions of gi.
		
		//Given a Query object Q that belongs to space X
		//retrival process first identifies all db objects that fall in the same bucket as Q
		//in at least one of the l hash tables and then EXACT distances are measure between the query and the objects
		
		//If the measure is not assumed to be an Euclidean one, then we have to treat it like a blackbox.
		//We cannot assume anything about the geometrical properties in that space, hence the LSH properties dont hold.
		
		//SO
		//Propose a family of HASH Functions defined only by the distances between the objects
		
		//we need to define a function that maps an arbitrary space (X,D) into real number R.
		//an example is the line projection function like:
		
		/*
		double ds1s2 = Trajectory.calculateDTWDistance(s1,s2);
		double dts1 = Trajectory.calculateDTWDistance(t,s1);
		double dts2 = Trajectory.calculateDTWDistance(t,s2);
		
		double hash = (Math.pow(dts1, 2) + Math.pow(ds1s2, 2) - Math.pow(dts2, 2))/(2*ds1s2);
		*/
		
		//If (X,D) was an euclidean space, the function defined by hash should had computed the projection of point X on the lines dfined by X1 and X2
		//If X is non euclidean space, then there is no geometrical interpretation of function Hash
		
		//But as long as there is a distance measure D, such as DTW, Hash can still be defined and can project the X space into the space Real numbers R
		//The function defined by Hash is a really rich family, any pair of objects can define a different function.
		//With n objects we can define n^2/2 functions
		
		//Since function H provides real numbers and we need discrete numbers, we need to set thresholds t1,t2 that belong to R
		
		//We want binary so:
		//Hash(t1,t2,X) = 0 if hash(x) belongs to interval [t1,t2]
		//Hash(t1,t2,X) = 1 otherwise
		
		//Now the problem is to choose t1 and t2, such that half of the objects are in 0 and half are in 1.
		//We want to have balanced hash tables so that is why we want to map hash values into half and half.
		//Formally, there should be a set V(x1,x2) of intervals [t1,t2], such that every pair of values x1,x2 that belongs to space X
		//so  that hash(t1,t2,X1,X2,X) splits the space in half.
		
		//Almost for every t there exist a t' such that H(X1,X2) maps half of the objects of X to either [t,t'] or to [t',t]
		//For a set of N objects thers are n/2 ways of spliting the set int 2 equal-sized subsets based in the choice of [t1,t2] that belong to V(x1,x2),
		//One alternative is the interval [t1,infinite] such that Hash(x1,x2,X0) is less than t1 for half the objects X that belong to X.
		//The set V(x1,x2) contains all the intervasl to split X into 2 equal subsets.
		
		//Now we an define a family HDBH of hash functions for an arbitrary space (X,D):
		
		//HDBH : {F(x1,x2,t1,t2) for each x1,x2 that belongs to X space, [t1,t2] belong to V(X1,X2)}
		
		//We need to use binary hash functions h form HDBH to define k-bit hash functions gi. gi(X) = (hi1(x),hi2(x),...,hik(x))
		
		//so, to index or retrieve we need to::
		//1.Choosing parameters k and l.
		//2.Constructing l k-bit hash tables, and storing pointers to each database object at the appropriate l buckets.
		//3. Comparing the query object with the database objects found in the l hash table buckets that the query is mapped to.
		
		//Choose randomly 10% of objects to try (so like 29 for LABOMNI)
		//To create the DBH families
		//Pick randonly 29 elements to crate a small subset 
		//For each pair of objects X1,X2 from that subset, create a binary hash function
		//Choosing randomly an interval [t1,t2] of V(X1,X2)
		//Approximately C(n,r) = C(29,2) = 406 functions
		
		//For me K and L are:
		//L 15 cause of the number of clusters in final dataset (cheating??)
		//K is 8+1 cause that is the number of partitions for a given trajectoru
		
		//Create family of functions H
		ArrayList<HashingFunction> hashingFamily = new ArrayList<HashingFunction>();
	

		//to select random trajectory elements
		Random r = new Random();
		
		
		//Create L functions composed of DBH hash functions of Kbits functions each
		ArrayList<ConcatenatedHashingFuntions> lkBitFunctions = new ArrayList<ConcatenatedHashingFuntions>(l);
		for(int i=0; i<l; i++)
		{
			ConcatenatedHashingFuntions chf = createKConcatenatedHashFunctionsDBH(simplifiedTrajectories, kBits);
			
			lkBitFunctions.add(chf); 
		}
		
		ArrayList<HashTable> hashTables = generateHashTablesFromDBHHashing(simplifiedTrajectories, lkBitFunctions, kBits);
			
		ArrayList<Cluster> finalListClusterRepresentation = createClustersFromHashTables(simplifiedTrajectories, 
				minNumElems, merge, mergeRatio,	hashTables);
		
		//At the end, here, Clustering with DBScan DTW should be done inside each cluster
		//and taking the biggest one (more elements) as the real one. This is only to get rid of noise (false positives)
		
		return finalListClusterRepresentation;
	}

	/**
	 * From a set of HashTables this method creates a set of clusters from a List of HashTables generated from L Kbit concatenated DBH Hash functions
	 * @param trajectories
	 * @param minNumElems : Minimum number of elements to preserve, this is used to get rid of empty buckets that might be present in hash tables.
	 * @param merge : If true, this calls a method to merge buckets from different hash tables. Not in use.
	 * @param mergeRatio : When merge is true, this parameter defines how to merge the different hash tables.
	 * @param r
	 * @param hashTables
	 * @return
	 */
	private ArrayList<Cluster> createClustersFromHashTables(ArrayList<Trajectory> trajectories, int minNumElems,
			boolean merge, float mergeRatio, ArrayList<HashTable> hashTables) 
	{
		//Now create the clusters, this seems infeasible
		ArrayList<ApproximatedSetOfCluster> listApproximatedSetClusters = new ArrayList<ApproximatedSetOfCluster>();
		
		//This random is just to pick a random HashTable when we dont want to merge
		Random r = new Random();
		
		//For each hash table bring me only K top buckets with more elements

		for(HashTable ht: hashTables)
		{
			ApproximatedSetOfCluster approxSetCluster = new ApproximatedSetOfCluster();
			
			for(HashBucket hb: ht.buckets)
			{
				if(hb!=null)
				{
					if(hb.bucketElements.size()>=minNumElems)
					{
						approxSetCluster.possibleClusters.add(hb);
					}
				}
			}
			listApproximatedSetClusters.add(approxSetCluster);
		}
		
		//Definitive set of clusters
		ApproximatedSetOfCluster finalCluster = new ApproximatedSetOfCluster();
		
		//First Prune Them
		//Purging the clusters means just getting the top L from each hash table(top = more members).
		/*for(ApproximatedSetOfCluster approxSetCluster: listApproximatedSetClusters)
		{
			//approxSetCluster.pruneApproximatedSetOfClusters(l);
		}
		*/
		if(merge)
		{
			finalCluster = mergeClustersFromDifferentHashTablesDBH(minNumElems, mergeRatio, listApproximatedSetClusters);
		}else{
			//Just because its faster, get an Approx Cluster at random with no merge
			//Get a random set of clusters from the hash table
			finalCluster = listApproximatedSetClusters.get(r.nextInt(listApproximatedSetClusters.size()));
		}
			
		//My common representation of set of Clusters
		ArrayList<Cluster> finalListClusterRepresentation = new ArrayList<Cluster>();
		//Now transform to the common representation
		int v = 0;
		for(HashBucket hb:finalCluster.possibleClusters)
		{
			Cluster ct = new Cluster(v, "DBH Address " + hb.getBucketAddressAsString());
		
			for(Integer id:hb.bucketElements)
			{
				ct.addElement(trajectories.get(id));
			}
			
			finalListClusterRepresentation.add(ct);
			v++;
		}
		return finalListClusterRepresentation;
	}

		/**
		 * This function Generates L hash tables from DBH hashing with Kbits on a Given List of Trajectories
		 * @param trajectories : List of trajectories to Hash in order to obtain the hash tables.
		 * @param l : Number of Hash Tables needed
		 * @param k : Number of Bits that map to a Bucket, that is the number of hash functions to concatenate for DBH
		 * @return : ArrayList of L HashTables generated from DBH of the Trajectories with Kbits.
		 */
		private ArrayList<HashTable> generateHashTablesFromDBHHashing(ArrayList<Trajectory> trajectories, ArrayList<ConcatenatedHashingFuntions> lkBitFunctions, int kBits) 
		{
			
			//now create hash tables and hash
			ArrayList<HashTable> hashTables = new ArrayList<HashTable>();
			
			//Initialize hash tables
			for(int w=0; w<lkBitFunctions.size();w++)
			{
				HashTable ht = new HashTable(w, kBits, true);
				hashTables.add(ht);
			}
			
			//now hash all trajectories
			//Seems like a extremely expensive process
			/*
			 * This code works but needs recalculating clusters
			for(Trajectory t0:simplifiedTrajectories)
			{
				for(int w=0; w<lkBitFunctions.size();w++)
				{
					ConcatenatedHashingFuntions tempCHF = lkBitFunctions.get(w);
					//now hash and index all in once
					
					//This requires recalculating the hashes
					hashTables.get(w).addToBucket(t0.getTrajectoryId(), tempCHF.execute(t0));
				}
			}
			*/
			

			for(int w=0; w<lkBitFunctions.size();w++)
			{
				ConcatenatedHashingFuntions tempCHF = lkBitFunctions.get(w);
				//now hash and index all in once
					
				//This requires recalculating the hashes
				//hashTables.get(w).addToBucket(t0.getTrajectoryId(), tempCHF.execute(t0));
					
				hashTables.get(w).addAllToBucketBooleanHash(trajectories, tempCHF.execute(trajectories));
			}
			return hashTables;
		}
		
		/**
		 * This method creates Feature Vectors from DBH Hashing for all the trajectories
		 * @param trajectories
		 * @param l
		 * @param kBits
		 * @param isBinaryFeatureVector: Determines wether we get a single real value in the feature vector or a binary vector of k features
		 * @param r
		 * @return
		 */
		private ArrayList<FeatureVector> generateFeatureVectorsFromDBHHashing(ArrayList<Trajectory> trajectories, 
				ConcatenatedHashingFuntions chf, int kBits, boolean isBinaryFeatureVector) 		
		{
			ArrayList<FeatureVector> featureVectors = chf.executeHashForFeatureVectors(trajectories, isBinaryFeatureVector, kBits);
			return featureVectors;
		}

		/**
		 * This method creates and concatenates K functions of the DBH type of Hashing functions.
		 * This concatenated functions can produce a Kbit address for the buckets of hash table. It can also produce signatures for 
		 * each trajectory element. Also used to produce Feature Vectors
		 * @param trajectories
		 * @param kBits : This is the number of DBH hash functions to concatenate to produce a KBit boolean addess - Signature for each trajectory
		 * @return A concatenated DBH HashFunction composed of K independent DBH HashFunctions
		 */
		private ConcatenatedHashingFuntions createKConcatenatedHashFunctionsDBH(
				ArrayList<Trajectory> trajectories, int kBits) {
			Random r = new Random();
			ConcatenatedHashingFuntions chf = new ConcatenatedHashingFuntions(kBits);
			
			//TODO Delete this, cause it is only for Raos Experiment
			//Just to try Raos Experiment
			ArrayList<Integer> listOfPredefinedTrajectoriesToTest = new ArrayList<Integer>();
			/*
			listOfPredefinedTrajectoriesToTest.add(2);
			listOfPredefinedTrajectoriesToTest.add(22);
			listOfPredefinedTrajectoriesToTest.add(3);
			listOfPredefinedTrajectoriesToTest.add(70);
			listOfPredefinedTrajectoriesToTest.add(146);
			listOfPredefinedTrajectoriesToTest.add(184);
			listOfPredefinedTrajectoriesToTest.add(181);
			listOfPredefinedTrajectoriesToTest.add(1);
			listOfPredefinedTrajectoriesToTest.add(0);
			listOfPredefinedTrajectoriesToTest.add(100);
			listOfPredefinedTrajectoriesToTest.add(21);
			listOfPredefinedTrajectoriesToTest.add(66);
			listOfPredefinedTrajectoriesToTest.add(4);
			listOfPredefinedTrajectoriesToTest.add(65);
			*/
			
			/*
			listOfPredefinedTrajectoriesToTest.add(0);
			listOfPredefinedTrajectoriesToTest.add(12);
			int testIndex = 0;
			*/
			//Lets just make a set of trajectories from the clusters we actually want
			//ArrayList<Trajectory> filteredTrajectories = new ArrayList<Trajectory>();
			//filteredTrajectories.add(trajectories.get(181));
			
					
			//End of Rao Test for Cluster Verification
			
			while(!chf.isConcatenationComplete())
			{
				//First get 2 random members (trajectories)
				Trajectory s1 = trajectories.get(r.nextInt(trajectories.size()));
				Trajectory s2 = trajectories.get(r.nextInt(trajectories.size()));
				
				//****************BEGIN TEST***************************************
				//TODO DELETE This, it is only to test RaoMethods
				//For test Purposes Only
				/*
				 s1 = trajectories.get(listOfPredefinedTrajectoriesToTest.get(testIndex));
				 testIndex++;
				 s2 = trajectories.get(listOfPredefinedTrajectoriesToTest.get(testIndex));
				 testIndex++;
				 */
				 //*****************End Of TEST******************
				 
				//Create the hashing function and calculate the interval t1-t2
				HashingFunction newHF = new HashingFunction(s1,s2);
				newHF.findT1T2(trajectories);
				
				chf.concatenate(newHF);
			}
			return chf;
		}
		

	/**
	 * @param minNumElems
	 * @param mergeRatio
	 * @param listApproximatedSetClusters
	 * @return
	 */
	private ApproximatedSetOfCluster mergeClustersFromDifferentHashTablesDBH(int minNumElems, 
			float mergeRatio, ArrayList<ApproximatedSetOfCluster> listApproximatedSetClusters) 
	{
		
		ApproximatedSetOfCluster finalCluster = new ApproximatedSetOfCluster();
		//Merging clusters between the different hash sets (clusters-bucket) from the
		//different hash tables. If we merge we obtain more members in the clusters
		//But is more expensive and imprecise

			//Just validate and get the first Set of clusters (first hash table), this is for the merge
			if(listApproximatedSetClusters.size()>0)
			{
				finalCluster = new ApproximatedSetOfCluster(listApproximatedSetClusters.get(0));
			}

			//Now intersect and merge
			for(int w=1; w<listApproximatedSetClusters.size();w++)
			{
				finalCluster = ApproximatedSetOfCluster.mergeApproximatedSetCluster(finalCluster, listApproximatedSetClusters.get(w), minNumElems, mergeRatio);
			}

		return finalCluster;
	}
			
	//LSH HASHING
	/**
	 * Approximation of clusters using LSH with Euclidean distance E2LSH
	 * @param workingTrajectories
	 * @param numHashingFunctions
	 * @param windowSize
	 * @param minNumElems
	 * @return
	 */
	private ArrayList<Cluster> approximateClustersLSHEuclidean(ArrayList<Trajectory> workingTrajectories,
			int numHashingFunctions, int windowSize, int minNumElems) {

		ArrayList<LocalitySensitiveHashing> allHashFunctions = new ArrayList<LocalitySensitiveHashing>();
		ArrayList<String> trajectoryHashesLSH = new ArrayList<String>();
		//Number of dimensions equal to double the number of points.
		int dimensions = 2 * workingTrajectories.get(0).getPoints().size();	//obtain this from trajectory data
		LocalitySensitiveHashing lsh = new LocalitySensitiveHashing(dimensions, numHashingFunctions, windowSize);
		lsh.createHashFunctions();
		
		//Create HashMap(to represent a Hashtable).
		HashMap<String, ArrayList<Trajectory>> allBuckets = new HashMap<String, ArrayList<Trajectory>>();

		for(Trajectory t:workingTrajectories)
		{
			//Somehow Convert Trajectory to Double array, All of them need to have same lenght
			int[] hash = lsh.generateHashes(t.getLocationDouble());
			allHashFunctions.add(lsh);
			
			String bucketAddressInString = "";
			for(int i=0; i<hash.length; i++)
			{
				bucketAddressInString = bucketAddressInString + Integer.toString(hash[i]);
			}
			
			//Now Store in HashMap (to represent a Hashtable).
			if(allBuckets.containsKey(bucketAddressInString))
			{
				allBuckets.get(bucketAddressInString).add(t);
			}else{
				ArrayList<Trajectory> bucket = new ArrayList<Trajectory>();
				bucket.add(t);
				allBuckets.put(bucketAddressInString, bucket);
			}
			
			trajectoryHashesLSH.add(bucketAddressInString);
		}
		
		//For debugging only, print Map
		/*
		System.err.println("*****************");
		System.err.println("Print All Buckets");
		AuxiliaryFunctions.printMap(allBuckets);
		System.err.println("*****************");
		*/

		//My common representation of set of Clusters
		ArrayList<Cluster> finalListClusterRepresentation = new ArrayList<Cluster>();
		//Now transform to the common representation
		int v = 0;
		for(ArrayList<Trajectory> bucket:allBuckets.values())
		{
			Cluster ct = new Cluster(v, "Cluster"+v);
			
			for(Trajectory t: bucket)
			{
				ct.addElement(t);
			}
			
			finalListClusterRepresentation.add(ct);
			v++;
		}
		
		return finalListClusterRepresentation;

	}

	private ArrayList<Cluster> approximateClustersLSHEuclideanWindowSize(ArrayList<Trajectory> workingTrajectories, int numHashingFunctions,
			int lshFunctionWindowSize, int minNumElems, int slidingWindowSize, int k, boolean calculateSilhouetteCoefficient) 
		{
		
		ArrayList<LocalitySensitiveHashing> allHashFunctions = new ArrayList<LocalitySensitiveHashing>();
		ArrayList<String> trajectoryHashesLSH = new ArrayList<String>();
		//Number of dimensions equal to double the number of points.
		//int dimensions = 2 * workingTrajectories.get(0).getPoints().size();	//obtain this from trajectory data
		//int totalSlidingWindowsPerTrajectory = dimensions - slidingWindowSize + 1;
		//slidingWindowEnhancedDimensions = slidingWindowEnhancedDimensions * slidingWindowSize
		LocalitySensitiveHashing lsh = new LocalitySensitiveHashing(slidingWindowSize, numHashingFunctions, lshFunctionWindowSize);
		lsh.createHashFunctions();
		
		//Create HashMap(to represent a Hashtable).
		HashMap<String, ArrayList<Trajectory>> allBuckets = new HashMap<String, ArrayList<Trajectory>>();

		for(Trajectory t:workingTrajectories)
		{
			//Convert trajectory in Vector
			double[] trajectoryVector = t.getLocationDouble();
			int totalSlidingWindowsPerTrajectory = trajectoryVector.length - slidingWindowSize + 1;
			
			ArrayList<String> hashBucketsPerTrajectory = new ArrayList<String>(); 
			//Now the sliding window hashing
			for(int i = 0; i < totalSlidingWindowsPerTrajectory; i++)
			{
				double[] windowVector = Arrays.copyOfRange(trajectoryVector, i, i+slidingWindowSize);
				//**************This needs to be stored******************
				int[] tempHash =  lsh.generateHashes(windowVector);
				
				String bucketAddressInString = "";
				for(int j=0; j<tempHash.length; j++)
				{
					bucketAddressInString = bucketAddressInString + Integer.toString(tempHash[j]);
					hashBucketsPerTrajectory.add(bucketAddressInString);
				}
				
				if(allBuckets.containsKey(bucketAddressInString))
				{
					allBuckets.get(bucketAddressInString).add(t);
				}else{
					ArrayList<Trajectory> bucket = new ArrayList<Trajectory>();
					bucket.add(t);
					allBuckets.put(bucketAddressInString, bucket);
				}
			}
		}
		
		HashMap<String, FeatureVector> listOfFeatureVectorsPerTrajectory = new HashMap<String, FeatureVector>();

		ArrayList<double[]> featureVectors = new ArrayList<double[]>();
		
		//TODO Print this only in debugging mode and debug logs
		/*
		//Print All buckets produced by Sliding Window
		System.out.println("*******Hash Buckets in Sliding Windows*********");
		//extras.AuxiliaryFunctions.printMap(allBuckets);
		//interrupt(15000);
		System.out.println("Number of buckets: " + allBuckets.size());
		System.out.println("*******End Hash Buckets in Sliding Windows*********");
		*/
		
		//Now create feature vectors
		int o = 0;
		for(String feature:allBuckets.keySet())
		{
			ArrayList<Trajectory> trajectoriesInBucket = allBuckets.get(feature);
			
			for(Trajectory t: workingTrajectories)
			{
					if(listOfFeatureVectorsPerTrajectory.containsKey(Integer.toString(t.getTrajectoryId())))
					{
						if(trajectoriesInBucket.contains(t))
						{
							listOfFeatureVectorsPerTrajectory.get(Integer.toString(t.getTrajectoryId())).features.add((float) 1);
						}else{
							listOfFeatureVectorsPerTrajectory.get(Integer.toString(t.getTrajectoryId())).features.add((float) 0);
						}
					}else{
						FeatureVector fv = new FeatureVector(t.getTrajectoryId());
						if(trajectoriesInBucket.contains(t))
						{
							fv.features.add((float) 1);
						}else{
							fv.features.add((float) 0);
						}
						listOfFeatureVectorsPerTrajectory.put(Integer.toString(t.getTrajectoryId()), fv);
					}
			}
			//TODO Print this only in debugging mode and debug logs
			//System.out.println("Bucket #:" + o + " Bucket key (feature): " + feature);
			o++;
		}
		
		//TODO Print this only in debugging mode and debug logs
		//Print All buckets
		/*
		System.out.println("*******FeatureVectors in Sliding Windows*********");
		extras.AuxiliaryFunctions.printMap(listOfFeatureVectorsPerTrajectory);
		//interrupt(15000);
		System.out.println("*******End FeatureVectors in Sliding Windows*********");
		*/
		
		//Now cluster
		
		ArrayList<FeatureVector> AllFeatureVectors = new ArrayList<FeatureVector>(listOfFeatureVectorsPerTrajectory.values()); 
		
		//Use KMeans
		ArrayList<Cluster> kmeansClusters = null;
		try {
			kmeansClusters = Kmeans.executeVectorKmeans(AllFeatureVectors, workingTrajectories, k, calculateSilhouetteCoefficient);
		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
		return kmeansClusters;
		
		/*
		//For debugging only, print Map
		System.err.println("*****************");
		System.err.println("Print All Buckets");
		AuxiliaryFunctions.printMap(allBuckets);
		System.err.println("*****************");
		*/		
	}
	
	/**
	 * This function calls other methods to crete feature vectors for all trajectories using DBH Hashing techniques and
	 * then hash those feature vectores using Kmeans.
	 * @param workingTrajectories
	 * @param numBits : Number of bits to produce with DBH hashing for each trajectory signature or vector. 
	 * This is also the same number of concatenated DBH hash fucntions to use with the DBH process.
	 * @param k : Number of clusters to produce with K-Means.
	 * @param calculateSilhouetteCoefficient 
	 * @return
	 * @throws Exception : When Normalization Fails
	 */
	private ArrayList<Cluster> approximateClustersDBHFeatureVector(ArrayList<Trajectory> workingTrajectories, int numBits, int k, 
			boolean binary, boolean saveFeatureVectorsToFile, boolean calculateSilhouetteCoefficient, boolean normalize) throws Exception 
	{
		long startHashFunctionTime = System.nanoTime();
		
		ConcatenatedHashingFuntions chf = createKConcatenatedHashFunctionsDBH(trajectories, numBits);
		long stopHashFunctionTime = System.nanoTime();
		long hashTotalTime = stopHashFunctionTime - startHashFunctionTime;
		System.out.println("Time spent just in Hashing (in seconds): " + hashTotalTime/1000000000.0);
		
		long startDBHClusteringProcessing = System.nanoTime();
		
		if(binary)
		{
			//TODO Refactor this cause this should not be in this method, but I put it here cause Zay needs it
			//By separation of concerns this should not be inside this method since it is not needed to generate the Feature Vectors
			ArrayList<ConcatenatedHashingFuntions> lkBitFunctions = new ArrayList<ConcatenatedHashingFuntions>();
			lkBitFunctions.add(chf);
			
			//Only for the use of previous methods
			ArrayList<HashTable> hashTables = generateHashTablesFromDBHHashing(workingTrajectories, lkBitFunctions, numBits);
			int minNumElems = 1;
			boolean merge = false;
			int mergeRatio = 1;
			ArrayList<Cluster> listOfDBHApproxClusters = createClustersFromHashTables(workingTrajectories, minNumElems, merge, mergeRatio, hashTables);
			
			long stopDBHClusteringProcessing = System.nanoTime();
			double DBHClusteringTotalProcessing = (stopDBHClusteringProcessing - startDBHClusteringProcessing + hashTotalTime)/1000000000.0;
			System.out.println("Total DBH Clustering Execution time in seconds: " + (DBHClusteringTotalProcessing));
			
			//TODO ENABLE this only in Debug logs
			/*
			for(Cluster c: listOfDBHApproxClusters)
			{
				System.out.println("Pre-Plotted Cluster: " + c.getClusterName());
			}*/
			
			//Now Plot these clusters
			TrajectoryPlotter.drawAllClusters(listOfDBHApproxClusters, true, false);
			ArrayList<Cluster> realClusters = testTrajectoryClustering.getTrueClustersFromTrajectories(workingTrajectories);
			HashSet<Integer> allConsideredTrajectories = CommonFunctions.getHashSetAllTrajectories(workingTrajectories);
			boolean printConfusionMatrix = false;
			System.out.println("***** Clusters Produced directly from DBH Methods *****");
			testTrajectoryClustering.compareClusters(realClusters, listOfDBHApproxClusters, allConsideredTrajectories, printConfusionMatrix);
			System.out.println("***** END of Output from Clusters Produced directly from DBH Methods *****");
			System.out.println("");
			System.out.println("");
		}
		
		
		

		System.out.println("***** Clusters Produced from K-MEANS OVER DBH Methods *****");
		
		long startDBHFeatureVectorGenerationTime = System.nanoTime();
		ArrayList<FeatureVector> AllFeatureVectors = generateFeatureVectorsFromDBHHashing(workingTrajectories, chf, numBits, binary);
		long stopDBHFeatureVectorGenerationTime = System.nanoTime();
		double DBHFeatureVectorGenerationTimeTotalProcessing = stopDBHFeatureVectorGenerationTime - startDBHFeatureVectorGenerationTime;
	
		System.out.println("Total DBH Feature Vector Generation time in seconds: " + (DBHFeatureVectorGenerationTimeTotalProcessing/1000000000.0));
	
		//*******************************Normalization**********************************
		//Normalize only for real values
		double DBHFeatureVectorNormalizationTimeTotalProcessing = 0;
		if(!binary && normalize)
		{
			long startDBHFeatureVectorNormalizationTime = System.nanoTime();
			//Create reference feature vectors for normalization
			ArrayList<FeatureVector> referenceFeatureVectors =  FeatureVector.createReferenceVectors(AllFeatureVectors);
			FeatureVector maxRefFV = referenceFeatureVectors.get(0);
			FeatureVector minRefFV = referenceFeatureVectors.get(1);
			AllFeatureVectors = FeatureVector.normalizeAll(AllFeatureVectors, maxRefFV, minRefFV, 0, 1); 
			long stopDBHFeatureVectorNormalizationTime = System.nanoTime();
			DBHFeatureVectorNormalizationTimeTotalProcessing = stopDBHFeatureVectorNormalizationTime - startDBHFeatureVectorNormalizationTime;
		}
		
		//**************************End of Normalization**********************************
		
		
		String allVectorsString = "";
		if(saveFeatureVectorsToFile)
		{
			for(FeatureVector fv: AllFeatureVectors)
			{
				String thisFeatureVectorString = "Trajectory ID: " + fv.getId() + " Trajectory Feature Vector size: " + fv.features.size() + " vector: " + fv.toString() +"\n";
				//TODO Move next line to debug log only
				//System.out.print(thisFeatureVectorString);
				allVectorsString = allVectorsString.concat(thisFeatureVectorString);
			}
			String path;
			try {
				path = GetPropertyValues.getPropValues("Feature_Vector_File");
				AuxiliaryFunctions.printStringToFile(allVectorsString, "FeatureVectors.txt", path);
			} catch (IOException e) {
				System.err.println(e.getMessage());
				e.printStackTrace();
			}
		}
		
			
		//Use KMeans to custer
		long startDBHFeatureVectorClusteringTime = System.nanoTime();
		ArrayList<Cluster> kmeansClusters = null;
		try {
			kmeansClusters = Kmeans.executeVectorKmeans(AllFeatureVectors, workingTrajectories, k, calculateSilhouetteCoefficient);
		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
		long stopDBHFeatureVectorClusteringTime = System.nanoTime();
		double DBHFeatureVectorClusteringTotalProcessing = (stopDBHFeatureVectorClusteringTime - startDBHFeatureVectorClusteringTime + DBHFeatureVectorGenerationTimeTotalProcessing + DBHFeatureVectorNormalizationTimeTotalProcessing + hashTotalTime)/1000000000.0;
		System.out.println("Total DBH Feature Vector Clustering Execution time in seconds: " + (DBHFeatureVectorClusteringTotalProcessing));
		testTrajectoryClustering.timesClustering.add(DBHFeatureVectorClusteringTotalProcessing);
		return kmeansClusters;

	}
	
	//Clustering Phase
	public ArrayList<Cluster> clusterSegments(ArrayList<Segment> setOfSegments, float eNeighborhoodParameter, int minLins, int cardinalityOfClusters)
	{
		setOfClusters = new ArrayList<Cluster>();
		
		int clusterId = 0;
		
		for(Segment s: setOfSegments)
		{
			System.out.println("now id:"+clusterId);
			if(!s.isClassified())
			{
				//To compute eNeighboorhood is a hard part
				//Cost n^2 unless we used index like R-Tree which reduces complexity to (n log n)
				//where n is the number of segments in the database.
				//Find Out how to index this.
				//MOre info at the end of page 7 (599) of paper
				ArrayList<Segment> neighborSegments = computeENeighborhoodOfSegment(s, eNeighborhoodParameter, setOfSegments);
				
				//Plus 1 cause my definition of neighborhood does not include core line itself
				if(neighborSegments.size()+1>= minLins)
				{
					ClusterSegments c = new ClusterSegments(clusterId, "Cluster " + clusterId);
					setOfClusters.add(c);
					
					//Before adding to cluster,
					//Should I set all segments of the neighborhood as classified??
					int i = 0;
					for(Segment s1:neighborSegments)
					{
						
						
						s1.setClassified(true);
						s1.setNoise(false);
						//neighborSegments.set(i, s1);
						c.addSegment(s1);
						i++;
					}
					
					//Add neighbours to cluster
					//My definition of neighborhood does not include the core line itself
					//This line is extra since I am already iterating, might be little more efficient?
					//c.setSegments(neighborSegments);
					
					//Add this core line to this cluster
					s.setNoise(false);
					s.setClassified(true);
					c.addSegment(s);
					
					//Insert Neighboors into a Queue (check this part)
					ArrayList<Segment> queue = new ArrayList<Segment>();
					queue.addAll(neighborSegments);
					
					//Now Expand Cluster
					expandClusterSegments(queue, clusterId, eNeighborhoodParameter, minLins, setOfSegments);
					clusterId++;
				}else{
					s.setClassified(true);
					s.setNoise(true);
				}
			}
		}
		
		//Step 3
		//Clusters are already allocated in my code, this differs from paper description of Traclus.
		//Check cardinality

		ArrayList<Cluster> filteredSetOfClusters = filterClustersByCardinality(cardinalityOfClusters, setOfClusters);
		
		//Cluster using Density-based method
		//Need a notion of distance and a notion of time difference
		
		return filteredSetOfClusters;
		
	}

	/**
	 * Can be refactored to use memory in a more efficient way
	 * @param cardinalityOfClusters
	 * @param setOfClusters2 
	 * @return 
	 */
	private ArrayList<Cluster> filterClustersByCardinality(int cardinalityOfClusters, ArrayList<Cluster> setOfClusters2) {
		ArrayList<Cluster> clustersWithDesiredCardinality = new ArrayList<Cluster>();
		for(Cluster c:setOfClusters2)
		{
			if(c.calculateCardinality()>=cardinalityOfClusters)
			{
				clustersWithDesiredCardinality.add(c);
			}
		}
		
		 return clustersWithDesiredCardinality;
	}

	public ArrayList<Cluster> clusterTrajectoriesWithDTW(ArrayList<Trajectory> simplifiedTrajectories, float eNeighborhoodParameter, int minLins, int cardinalityOfClusters)
	{
		setOfClusters = new ArrayList<Cluster>();
		
		int clusterId = 0;
		
		for(Trajectory t: simplifiedTrajectories)
		{
			if(!t.isClassified())
			{
				//This is the Traclus density based algorithm modified to 
				//cluster over whole trajectories, I assume this is similar to DBScan.
				
				ArrayList<Trajectory> neighborTrajectories = computeENeighborhoodOfTrajectory(t, eNeighborhoodParameter, simplifiedTrajectories);
				
				//Plus 1 cause my definition of neighborhood does not include core line itself
				//MinLins is in this case the minimun number of trajectories in the cluster
				if(neighborTrajectories.size()+1>= minLins)
				{
					ClusterTrajectories c = new ClusterTrajectories(clusterId, "Cluster " + clusterId);
					
					setOfClusters.add(c);
					
					//Before adding to cluster,
					//Should I set all segments of the neighborhood as classified??
					for(Trajectory t1:neighborTrajectories)
					{
						if(!t1.isClassified())
						{	
						t1.setClassified(true);
						t1.setNoise(false);
						//neighborSegments.set(i, s1);
						c.addTrajectory(t1);
						}
					}
					
					//Add neighbors to cluster
					//My definition of neighborhood does not include the core line itself
					//This line is extra since I am already iterating, might be little more efficient?
					//c.setSegments(neighborSegments);
					
					//Add this core line to this cluster
					t.setNoise(false);
					t.setClassified(true);
					c.addTrajectory(t);
					
					//Insert Neighboors into a Queue (check this part)
					ArrayList<Trajectory> queue = new ArrayList<Trajectory>();
					queue.addAll(neighborTrajectories);
					
					//Now Expand Cluster
					expandClusterTrajectories(queue, clusterId, eNeighborhoodParameter, minLins, simplifiedTrajectories);
					c.calculateCardinality();
					clusterId++;
				}else{
					t.setClassified(true);
					t.setNoise(true);
				}
			}
		}
		
		//For cluster cardinality
		//not so needed like in segments but I can do it,
		//For now comment it for tests
		ArrayList<Cluster> filteredSetOfClusters = filterClustersByCardinality(cardinalityOfClusters, setOfClusters);
		//ArrayList<Cluster> filteredSetOfClusters = setOfClusters;
		//Cluster using Density-based method
		//Need a notion of distance and a notion of time difference
		
		return filteredSetOfClusters;
	}

	private ArrayList<Segment> computeENeighborhoodOfSegment(Segment s, float eParameter, ArrayList<Segment> allSegments) {
		// TODO Auto-generated method stub'
		ArrayList<Segment> neighborSegments = new ArrayList<Segment>();
		
		for(Segment possibleNeighborSegment:allSegments)
		{
			if(Segment.calculateDistance(s, possibleNeighborSegment)<=eParameter)
			{
				if(s!=possibleNeighborSegment)
				{
				neighborSegments.add(possibleNeighborSegment);
				}
			}
		}
		return neighborSegments;
	}
	
	/**
	 * Could add parameter (int maxNumIterations) to make space for other iterations 
	 * @param simplifiedTrajectories
	 * @param k
	 * @return
	 */
	private  ArrayList<Cluster> clusterTrajectoriesKMeansEuclidean(ArrayList<Trajectory> simplifiedTrajectories, int k, boolean calculateSilhouetteCoefficient)
	{
		
		ArrayList<Cluster> kmeansClusters = null;
		try {
			kmeansClusters = Kmeans.executeVectorKmeans(simplifiedTrajectories, simplifiedTrajectories, k, calculateSilhouetteCoefficient);
		} catch (Exception e) {
			System.err.println(e.getMessage());
			System.err.println("Try Simplifying trajectories so clusterables are of same length.");
			e.printStackTrace();
		}
		return kmeansClusters;
	}

	/**
	 * This method uses DTW distance to calculate new point that will be assumed as the center of the cluster
	 * similar to kmeans but we do not need to calculate a centroid, instead we choose points (trajectories) as
	 * the new center of the cluster until it converges 
	 * @param simplifiedTrajectories
	 * @param k
	 * @param distance 
	 * @return
	 */
	private ArrayList<Cluster> clusterTrajectoriesKMedoids(
			ArrayList<Trajectory> simplifiedTrajectories, int k, TrajectoryDistance distance) {

		ArrayList<Cluster> kmedoidsClusters = new ArrayList<Cluster>();
		
		//here call kmedoids
		kmedoidsClusters = Kmedoids.execute(simplifiedTrajectories, k, distance);
		
		return kmedoidsClusters;
	}
	
	/**
	 * This method uses DTW distance to calculate new point that will be assumed as the center of the cluster
	 * in the real Kmeans way 
	 * @param simplifiedTrajectories
	 * @param k
	 * @return
	 */
	private ArrayList<Cluster> clusterTrajectoriesKMeansDTW(
			ArrayList<Trajectory> simplifiedTrajectories, int k) {

		ArrayList<Cluster> kmeansDTWClusters = new ArrayList<Cluster>();
		
		//here call kmedoids
		kmeansDTWClusters = KmeansDTW.execute(simplifiedTrajectories, k);
		
		return kmeansDTWClusters;
	}

	//Step 2 - Clustering phase
	//To compute a density-connected set
	private void expandClusterSegments(ArrayList<Segment> queue, int clusterId,
			float eNeighborhoodParameter, int minLins, ArrayList<Segment> segmentsCompleteSet) {
		// TODO Auto-generated method stub
		while(!queue.isEmpty())
		{
			ArrayList<Segment> neighborhood = computeENeighborhoodOfSegment(queue.get(0), eNeighborhoodParameter, segmentsCompleteSet);
			
			if(Math.abs(neighborhood.size())>= minLins)
			{
				for(Segment s: neighborhood)
				{
					if(!s.isClassified() || s.isNoise())
					{
						//Cluster c = new Cluster(clusterId, "Cluster " + clusterId);
						ClusterSegments cTemp =  (ClusterSegments) setOfClusters.get(clusterId);
						s.setClassified(true);
						
						//This was before refactoring
						cTemp.addSegment(s);
						
						//Now it should be more generalized
						
						//setOfClusters.add(cTemp);
						setOfClusters.set(clusterId, cTemp);
					}
						//Check this part
					if(!s.isClassified())
					{
						//if element not in queue
						if(!queue.contains(s))
						{
						queue.add(s);
						}
					}
				}
			}
			queue.remove(0);
		}
	}
	
	//Step 2 - Clustering phase
		//To compute a density-connected set of trajectories
	//This should be refactored
		private void expandClusterTrajectories(ArrayList<Trajectory> queue, int clusterId,
				float eNeighborhoodParameter, int minLins, ArrayList<Trajectory> trajectoriesCompleteSet) {
			// TODO Auto-generated method stub
			while(!queue.isEmpty())
			{
				ArrayList<Trajectory> neighborhood = computeENeighborhoodOfTrajectory(queue.get(0), eNeighborhoodParameter, trajectoriesCompleteSet);
				
				if(Math.abs(neighborhood.size())>= minLins)
				{
					for(Trajectory t: neighborhood)
					{
						if(t.getTrajectoryId()==182)
						{
							//if element not in queue
							if(!queue.contains(t))
							{
							//queue.add(t);
							}
						}
						
						
						
						if(!t.isClassified() || t.isNoise())
						{
							//Cluster c = new Cluster(clusterId, "Cluster " + clusterId);
							ClusterTrajectories cTemp =  (ClusterTrajectories) setOfClusters.get(clusterId);
							t.setClassified(true);
							
							//This was before refactoring
							cTemp.addTrajectory(t);
							
							//Now it should be more generalized
							
							//setOfClusters.add(cTemp);
							setOfClusters.set(clusterId, cTemp);
						}
						
						if(t.getTrajectoryId()==182)
						{
							//if element not in queue
							if(!queue.contains(t))
							{
							//queue.add(t);
							}
						}
						
						
						//According to my analysis this code is useless
						/*
						//Check this part
						if(!t.isClassified())
						{
							//if element not in queue
							if(!queue.contains(t))
							{
							queue.add(t);
							}
						}
						*/
					}
				}
				queue.remove(0);
			}
		}


		/**
		 * This method should be taking a trajectory and calculating the neighborhood based in dtw
		 * @param trajectory
		 * @param eNeighborhoodParameter2 = the epsilon, in this case given by DTW
		 * @param trajectoriesCompleteSet = this is actually the set of reduced trajectorues
		 * @return
		 */
	private ArrayList<Trajectory> computeENeighborhoodOfTrajectory(Trajectory t, float eParameter, ArrayList<Trajectory> trajectoriesCompleteSet) 
	{

		ArrayList<Double> dtwValues = new ArrayList<Double>();
		ArrayList<Trajectory> neighborTrajectories = new ArrayList<Trajectory>();
		
		for(Trajectory possibleNeighborTrajectory:trajectoriesCompleteSet)
		{
			double tempDtwDistance = Trajectory.calculateDTWDistance(t, possibleNeighborTrajectory);
			dtwValues.add(tempDtwDistance);
			if(tempDtwDistance <=eParameter)
			{
				if(t!=possibleNeighborTrajectory)
				{
				neighborTrajectories.add(possibleNeighborTrajectory);
				}
			}
		}
		
		Double dtwAverage = calculateMeanDTW(dtwValues);
		//System.out.println(dtwAverage);
		t.setDtwAverage(dtwAverage);
		return neighborTrajectories;
	}

	private Double calculateMeanDTW(ArrayList<Double> dtwValues) {
			// TODO Auto-generated method stub
		double tempDTW = 0;
		for(double d: dtwValues)
		{
			tempDTW = tempDTW + d;
		}
		if(dtwValues!=null && dtwValues.size()>0)
		tempDTW = tempDTW/dtwValues.size();
			
		return tempDTW;
		}

	public double getEpsilonDouglasPeucker() {
		return epsilonDouglasPeucker;
	}

	public void setEpsilonDouglasPeucker(double epsilonDouglasPeucker) {
		this.epsilonDouglasPeucker = epsilonDouglasPeucker;
	}

	public int getFixedNumOfTrajectoryPartitionsDouglas() {
		return fixedNumOfTrajectoryPartitionsDouglas;
	}

	public void setFixedNumOfTrajectoryPartitionsDouglas(
			int fixedNumOfTrajectoryPartitionsDouglas) {
		this.fixedNumOfTrajectoryPartitionsDouglas = fixedNumOfTrajectoryPartitionsDouglas;
	}


	//Calculate Representative Trajectories.
	//This step is done in each individual cluster, so it is in the cluster class.
	
}
