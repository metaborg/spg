package nl.tudelft.fragments

/**
  * Language-dependent generation configuration.
  *
  * @param limit Maximum number of terms to generate.
  * @param fuel Fuel provided to the backtracker.
  * @param sizeLimit Maximum size of a term.
  */
case class Config(limit: Int, fuel: Int, sizeLimit: Int)
