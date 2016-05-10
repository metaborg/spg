package nl.tudelft.fragments

// Name provider
case class NameProvider(var c: Int) {
  def next = {
    c = c + 1
    c
  }
}
