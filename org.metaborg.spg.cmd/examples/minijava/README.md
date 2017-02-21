# MiniJava NaBL2

MiniJava with NaBL2. The `tests` directory contains the lab6 and lab8 grading tests from Compiler Construction. For this reason, **do not make this repository public**.

## TODO

- We used to do post-analysis desugaring where we turned `Var(_)` that referenced a field into `Field(_)`. See [here](https://github.com/TUDelft-IN4303/grading/blob/master/lab6/reference/MiniJava/trans/desugar.str#L56). How to accomplish this in NaBL2? Proposed solution: annotate declaration (i.e. as being a field). Retrieve declaration that reference resolves to.

- MiniJava should have an error on overloading, an error on erroneous overriding, and a note on correct overriding.
  - We can give an error on erroneous overriding by adding a reference for every method to a new scope s', two edges from s' to the class scope labeled S & I, using the label ordering to prefer S over I, and requiring S to be followed by I in the WF predicate. Now the return type of the thing that it resolves to must be a subtype, otherwise erroneous overriding error.
  - The above technique does not work for adding a note on correct overriding. It may be that the reference resolves to the method itself (and not a method in a super class), but then this is not overriding.
  - Error on overloading requires more expressiveness in the constraints. E.g. the set of names that the method reference resolves to such that the types are different. If we cannot change the constraint language, we may be able to change the semantics of MiniJava and allow overloading?

- Error on field hiding inherited field. This is possible, but becomes much easier if set difference on namesets is allowed.

- Warning on variable/param hiding local field. This is possible, but becomes much easier if set difference on namesets is allowed.

- Warning on variable/param hiding inherited field. This is possible, but becomes much easier if set difference on namesets is allowed.

- Code generation uses `nabl-prop-site` on `Method` to store an index on parameters and variable declarations. During code generation for `VarRef`, the index is retrieved and used in the bytecode. In NaBL2, I propose adding the indexes during a desugaring, then adding the index as metadata during constraint generation, and retrieving this index using code generation. Should be doable with the change for (1).