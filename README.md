## Inspiration

The challenge of creating an efficient Forex market broker stuck with us. While it is a very complex and hard to break-in area, one can learn a lot about the modern economy focusing on creating a profitable strategy, especially in the setup of a hackathon.

## What it does

We devise multiple proposed algorithms for implementing market broker behavior. The execution environment of the algorithm includes a real-time feed of the latest Fiat prices on the market, as well as a set of client requests for the broker to exchange specific currency paris. The challenge is to minimize the amount of times the broker has to trade on the actual Forex market because of the high fees there and instead try to match the requests of its users as well as build a set of internal balances to represent a market on its own.

The first basic implementation of our algorithm includes the goal of keeping a specific balance of fiat locally in the broker in all available currencies at all costs. Should a specific balance reach a lower level than the limit, the broker places a market order to buy back that amount. This basic logic passed the first three difficulty tests of the challenge.

The second (WIP) implementation is a lot more intelligent and comprises of real-time streamed data analysis and market forecasts with neural networks. For each traded currency pair, we do the following:

(1): Given the most recent price, calculate the technical indicators SMA, EMA, MACD and RSI for five different parameter sets. 
(2): Use (1) to perform windowing, normalization, and labeling to create a neural network dataset with the goal of predicting the next movement for that currency pair.
(3): Use (2) to train a classificational neural model


(1) to (3) are done once and for every currency pair. Once we have (3), we can start the real-time trading environment and use the neural predictions to trade on the market. More specifically, given all predictions for the next price movement for all currencies, we are going to place a market order for a specific amount to sell the currency most likely to decrease in value and buy the currency that is most likely to raise. Overtime this will give us a stable portfolio of multiple currencies with which we can process external client orders while still growing and maintaining the portfolio.

The third (WIP) implementation of an even more advanced algorithm represents the creation of a reinforcement learning environment for a Deep-Q-Network to learn to trade the stock market given the current situation of price movements and user trades. This is theoretically the best approach since our market environment actively reacts to our trades and this is the best scenario where a RL algorithm can learn to interact with it to maximize profit given its previous actions and experience. After a significant investment of time in learning how to design the actual algorithm, it turned out that when we started creating the execution environment for it, our DeepLearning library did not have any documentation for creating anything based on RL. They had examples for interfacing with already made environments in OpenAI's Gym, but this was over http and was too latent for us to use it.

Extra cool things that we did include: 
(1): When we want to place market orders, we add them to a queue instead of executing them instantly. On every market price update, we process one order from the queue and execute it in the market. This load balances our interaction with the market and disrupts the economy less, which has seen positive results in our experiments.
(2): Sometimes when we place a market order, we split it on multiple orders before adding it to the queue. The intent is to also disrupt the market less especially if we're buying a large amount at once.
(3): When managing JPY we often see significant biased demand from our users towards it. We've included a bias in our focus on maintaining our local balance of JPY accordingly.

## How we built it

Scala, DeepLearning4j

## Challenges we ran into

The input dataset preparation for our neural network took an exceptionally amount of time. In fact, it took most of the time and it is still not ready. We found too late that the DeepLearning4j library is very inconvenient to use for using datasets which were created in memory, because we have to implement all of the data processing pipelines ourselves. This is very cumbersome and error-prone, especially when you have to perform real-time multi-feature extraction, windowing, normalization, and overall dataset generation. The tools in the Library do include automated pipelines for this but they are mostly suited for datasets stored in CSV files or images, which is unfeasible for our case. The rest of the pipeline, including the trading algorithm and the predictive neural network, should work, but have not been tested because of this.

## Accomplishments that we're proud of

We managed to work the whole 24h without any sleep!

## What we learned

Scala, DeepLearning4j, JVM bytecode tricks, how to extract public fields when someone forgot to protect them, and the fact that we're never using DeepLearning4j ever again. 

## What's next for Hilt

Sleep. Thank you.

# Pre-requisites
* [JDK 8+][jdk]
* [Maven 3+][mvn]
* [IDE][ide]

# First Steps

* Fork / clone the repo

* At `com.swissquote.lauzhack.evolution.sq.team` fill in the right properties (specially "path" and "team")

* As a first test run : `mvn clean install`

* You can then launch `App.main()`

# Links

* [Viewer online][viewer]
* [API Sources][sources]

# Trial configuration

* Profile : RANDOM
* Steps : 5000
* Interval : 1

# Formulas

**Client trades "Q" _EUR/CHF_ :**
* `EUR : -Q`
* `CHF : Q.r.(1+M) + 10`

**Client trades "Q" _CHF/EUR_:**
* `EUR : Q / (r.(1-M))`
* `CHF : -Q + 10`

**Client trades "Q" _EUR/CHF_:**
* `EUR : -Q`
* `CHF : Q.r.(1+M) - 100`

**Client trades "Q" _CHF/EUR_:**
* `EUR : -Q / (r.(1-M))`
* `CHF : Q - 100`

[viewer]: https://astat.github.io/sq-evolution-viewer/
[jdk]: https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
[mvn]: https://maven.apache.org/download.cgi
[ide]: https://www.jetbrains.com/idea/download/
[sources]: https://github.com/Astat/sq-evolution-sources
