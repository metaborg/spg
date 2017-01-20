package nl.tudelft.fragments

/**
  * Language-dependent generation configuration.
  */
abstract class Config {
  /**
    * Limit at which to abandon a term.
    *
    * @return
    */
  def sizeLimit: Int
}

/**
  * A config with some sane defaults
  */
object DefaultConfig extends Config {
  override def sizeLimit: Int =
    60
}
