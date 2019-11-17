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
import org.nd4j.linalg.learning.config.{Nesterovs, Sgd}
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.nd4j.linalg.primitives.Pair
import java.io.File

import org.nd4j.linalg.factory.Nd4j

class NeuralModel {

  val batchSize = 64

  var model: MultiLayerNetwork = _

  def NeuralTrainer() {

  }

  def buildModel() {
    //number of rows and columns in the input pictures
    val numRows = 8
    val numColumns = 5
    val outputNum = 2 // number of output classes
    val rngSeed = 123 // random number seed for reproducibility
    val numEpochs = 15 // number of epochs to perform
    val rate = 0.00005 // learning rate


    val conf = new NeuralNetConfiguration.Builder()
      .seed(rngSeed) //include a random seed for reproducibility
      // use stochastic gradient descent as an optimization algorithm

      .activation(Activation.RELU)
      .weightInit(WeightInit.XAVIER)
      .updater(new Nesterovs(rate, 0.98)) //specify the rate of change of the learning rate.
      .l2(rate) // regularize learning model
      .list()
      .layer(new DenseLayer.Builder() //create the first input layer.
        .nIn(numRows * numColumns)
        .nOut(500)
        .build()
      )
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
    this.model.setListeners(new ScoreIterationListener(5)) //print the score every 5 iterations
  }

  def saveModel(filepath: String) {
    this.model.save(new File(filepath))
  }

  def loadModel(filepath: String) {
    this.model = ModelSerializer.restoreMultiLayerNetwork(new File(filepath))
  }

  def fit(data: java.lang.Iterable[Pair[INDArray, INDArray]]) {
      val data_it = new INDArrayDataSetIterator(data, batchSize)
      this.model.fit(data_it)
  }

  def evaluate(data: java.lang.Iterable[Pair[INDArray, INDArray]]): Evaluation = {
    this.model.evaluate(new INDArrayDataSetIterator(data, batchSize))
  }

  def predict(x: INDArray): Array[Int] = {
    this.model.predict(x.toDense())
  }

}