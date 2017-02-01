# Tiger

 A Spoofax implementation of Tiger, a language originally defined in Andrew Appel's book _Modern Compiler Implementation in Java_ and later published by [LRDE](https://www.lrde.epita.fr/~tiger/tiger.html#Tiger-Language-Reference-Manual).

## Mutations

The branch `generation` contains the correct implementation and several mutants that contain a soundness bug. These mutants were used to evaluate the effectiveness of our random term generator at discovering soundness bugs.

## Known issues

- To parse For/While/If/IfThen expressions without ambiguity, this needs Timothée's new SDF semantics. However, when using the new generator, the string "(1)" becomes ambiguous (either an `Exp.Seq` with a single integer or a parenthesized integer). Currently, there is no way to unambiguously parse Tiger.

- `int[5] of 0 <> int[5] of 0` cannot be parsed (not even ambiguously). Reason: `Exp.Or > Exp.Array`, `Exp.Neq > .. > Exp.Or` and by transitivity `Exp.Neq > Exp.Array` and as such `Array()` is not allowed as a direct child of `Neq()`. Should investigate how to fix.

- According to LRDE, Boolean operators should normalize their result to 0 (false) or 1 (true). This is currently not implemented.

- Functions share the same namespace as variables, but I'm not sure if this is intended. It does not make much sense, since there is no function type, so we cannot pass functions around anyway. The only useful thing is assigning a function to a variable, thereby creating an alias.

- We implemented natives for eq(S/I), lt(S/I), leq(S/I), but we could do with two of these and express one in the others. E.g. leq = lt | eq.

- Our static semantics only allows comparison operators on integers, but the specification also allows this on strings. We should disjunct (type is either INT() or STRING()), but the generator cannot handle disjunction.

- The syntax does not parse all escape characters in strings (e.g. \r).

## References

1. http://www.cs.columbia.edu/~sedwards/classes/2002/w4115/tiger.pdf
2. https://www.lrde.epita.fr/~tiger/tiger.html#Tiger-Language-Reference-Manual
3. http://www.cs.tufts.edu/comp/181/Tiger.pdf
4. https://www.cs.princeton.edu/~appel/modern/java/project.html
