package main;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.encog.ensemble.EnsembleAggregator;
import org.encog.ensemble.EnsembleMLMethodFactory;
import org.encog.ensemble.EnsembleTrainFactory;
import org.encog.ensemble.aggregator.WeightedAveraging.WeightMismatchException;
import org.encog.neural.data.basic.BasicNeuralDataSet;

import techniques.AdaBoostET.RequiresWeightedAggregatorException;
import techniques.EvaluationTechnique;
import helpers.ArgParser;
import helpers.ArgParser.BadArgument;
import helpers.DataLoader;
import helpers.DataMapper;
import helpers.Evaluator;
import helpers.ChainParams;
import helpers.ProblemDescription;

public class TrainingCurves {

	Evaluator ev;
	static DataLoader dataLoader;
	static ProblemDescription problem;
	
	private static int trainingSetSize;
	private static double activationThreshold;
	private static EnsembleTrainFactory etf;
	private static List<EnsembleMLMethodFactory> mlfs;
	private static EnsembleAggregator agg;
	private static String etType;
	private static int maxIterations;
	
	public static void loop() throws WeightMismatchException, RequiresWeightedAggregatorException {
		List<Integer> one = new ArrayList<Integer>();
		one.add(1);
		for(EnsembleMLMethodFactory mlf: mlfs)
		{
			ChainParams labeler = new ChainParams("", "", "", "", "", 0);
			EvaluationTechnique et = null;
			try {
				et = ArgParser.technique(etType,one,trainingSetSize,labeler,mlf,etf,agg,dataLoader,maxIterations);
			} catch (BadArgument e) {
				help();
			}
			DataMapper dataMapper = dataLoader.getMapper();
			BasicNeuralDataSet testSet = dataLoader.getTestSet();
			BasicNeuralDataSet trainingSet = dataLoader.getTrainingSet();
			et.init(dataLoader,1);
			for (int i=0; i < maxIterations; i++) {
				et.trainStep();
				double trainMSE = et.trainError();
				double testMSE = et.testError();
				double trainMisc = et.getMisclassification(testSet, dataMapper);
				double testMisc = et.getMisclassification(trainingSet, dataMapper);
				System.out.println(i + " " + trainMSE + " " + testMSE
									 + " " + trainMisc + " " + testMisc);
			}
		}
	}
	
	public static void main(String[] args) throws WeightMismatchException, RequiresWeightedAggregatorException {
		if (args.length != 8) {
			help();
		} 
		try {
			etType = args[0];
			problem = ArgParser.problem(args[1]);
			trainingSetSize = ArgParser.intSingle(args[2]);
			activationThreshold = ArgParser.doubleSingle(args[3]);
			etf = ArgParser.ETF(args[4]);
			mlfs = ArgParser.MLFS(args[5]);
			agg = ArgParser.AGG(args[7]);
			maxIterations = ArgParser.intSingle(args[6]);
		} catch (BadArgument e) {
			help();
		}
		
		try {
			dataLoader = problem.getDataLoader(activationThreshold,trainingSetSize);
		}
		catch (FileNotFoundException e)
		{
			System.err.println("Could not create dataLoader - data file not found");
		}
		catch (helpers.ProblemDescriptionLoader.BadArgument e) {
			System.err.println("Could not get dataLoader - perhaps the mapper_type property is wrong");
			e.printStackTrace();
		}
		loop();
		System.exit(0);
	}

	private static void help() {
		System.err.println("Usage: TrainingCurves <technique> <problem> <trainingSetSize> <activationThreshold> <training> <membertypes> <maxIterations> <aggregation>");
		System.exit(2);
	}
}
