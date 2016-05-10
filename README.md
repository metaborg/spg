This is an attempt at top-down generation, as previously done, for MiniJava, with constraints.

# Strategy 1

Top-down generation. We ignore solvability, only some limited form of consistency.
 
Observation: lots of "boring" programings. Lists have an expected size of 2, but since non-empty lists have a chance of being rejected the actual expected size is less. This means lots of programs with very few classes and those that have a class have very few methods and those that have a method have very few statements. Since every run is isolated, we start from the same point each time, generating many programs that look the same. Can we do better by starting from a random construct?

# Strategy 2

Inside-out generation.
