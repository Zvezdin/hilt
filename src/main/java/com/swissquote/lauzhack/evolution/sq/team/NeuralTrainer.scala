package com.swissquote.lauzhack.evolution.sq.team;


import org.deeplearning4j.datasets.iterator.INDArrayDataSetIterator
import org.nd4j.linalg.activations.Activation
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator
import org.deeplearning4j.datasets.iterator.impl.MnistDataSetIterator
import org.deeplearning4j.optimize.listeners.EvaluativeListener
import org.deeplearning4j.eval.Evaluation
import org.deeplearning4j.nn.api.OptimizationAlgorithm
import org.deeplearning4j.nn.conf.MultiLayerConfiguration
import org.deeplearning4j.nn.conf.NeuralNetConfiguration
import org.deeplearning4j.nn.conf.Updater
import org.deeplearning4j.nn.conf.layers.DenseLayer
import org.deeplearning4j.nn.conf.layers.OutputLayer
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork
import org.deeplearning4j.nn.weights.WeightInit
import org.deeplearning4j.optimize.listeners.ScoreIterationListener
import org.deeplearning4j.util.ModelSerializer
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.dataset.DataSet
import org.nd4j.linalg.learning.config.{Nesterovs, Sgd}
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction
import org.nd4j.nativeblas.Nd4jCuda.NDArray
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.nd4j.linalg.primitives.Pair


class NeuralModel {

  val batchSize = 64

  var model: MultiLayerNetwork

  def NeuralTrainer() {

  }

  def buildModel() {
    //number of rows and columns in the input pictures
    val numRows = 8
    val numColumns = 5
    val outputNum = 2 // number of output classes
    val rngSeed = 123 // random number seed for reproducibility
    val numEpochs = 15 // number of epochs to perform
    val rate = 0.0015 // learning rate


    val conf = new NeuralNetConfiguration.Builder()
      .seed(rngSeed) //include a random seed for reproducibility
      // use stochastic gradient descent as an optimization algorithm

      .activation(Activation.RELU)
      .weightInit(WeightInit.XAVIER)
      .updater(new Nesterovs(rate, 0.98)) //specify the rate of change of the learning rate.
      .l2(rate * 0.005) // regularize learning model
      .list()
      .layer(new DenseLayer.Builder() //create the first input layer.
        .nIn(numRows * numColumns)
        .nOut(500)
        .build())
      .layer(new DenseLayer.Builder() //create the second input layer
        .nIn(500)
        .nOut(100)
        .build())
      .layer(new OutputLayer.Builder(LossFunction.NEGATIVELOGLIKELIHOOD) //create hidden layer
        .activation(Activation.SOFTMAX)
        .nIn(100)
        .nOut(outputNum)
        .build())
      .build()

    this.model = new MultiLayerNetwork(conf)
    this.model.init()
  }

  def saveModel(filepath: String) {
    this.model.save(new File(filepath))
  }

  def loadModel(filepath: String) {
    this.model = ModelSerializer.restoreMultiLayerNetwork(new File(filepath))
  }

  def fit(data: Iterable[Pair[INDArray, INDArray]]) {
    this.model.setListeners(new ScoreIterationListener(5)) //print the score every 5 iterations

    // Cast to nothing is needed because the compiler for some reason expects this type
    val data_it = new INDArrayDataSetIterator(data.asInstanceOf[Nothing], batchSize);

    this.model.fit(data_it)
  }

  def evaluate(data: Iterable[Pair[INDArray, INDArray]]): Evaluation = {
    // Cast to nothing is needed because the compiler for some reason expects this type
    val data_it = new INDArrayDataSetIterator(data.asInstanceOf[Nothing], batchSize);

    val eval: org.nd4j.evaluation.classification.Evaluation = this.model.evaluate(data_it)

    return eval;
  }

  def predict(x: INDArray): Array[Int] = {
    return this.model.predict(x)
  }

}