package com.swissquote.lauzhack.evolution.sq.team;

import com.swissquote.lauzhack.evolution.{ScalaBBook, SwissquoteEvolutionBuilder}
import com.swissquote.lauzhack.evolution.api.MarketProfile
import com.swissquote.lauzhack.evolution.api.SwissquoteEvolution;



object App {

	def time[R](block: => R): R = {
		val t0 = System.currentTimeMillis()
		val result = block    // call-by-name
		val t1 = System.currentTimeMillis()
		println("Elapsed time: " + (t1 - t0) + "ms")
		result
	}

	/**
	 * This is the starter for the application.
	 * You can keep this one, or create your own (using any Framework)
	 * As long as you run a SwissquoteEvolution
	 */
	def main(args: Array[String]) {
		// Instantiate our BBook
		val ourBBook = new ScalaBBook()

		// Create the application runner
		val app = new SwissquoteEvolutionBuilder().
				profile(MarketProfile.UNICORN).
				seed(3).
				team("Hilt").
				bBook(ourBBook).
				filePath(".").
				interval(1).
				steps(5000).
				build()

		// Let's go !
		time {
			app.run()
		}

		// Display the result as JSON in console (also available in the file at "Path")
		println(app.logBook())
	}

}
