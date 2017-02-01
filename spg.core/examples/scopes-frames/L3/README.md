# L3

## Notes

While experimenting, the following things came to light:

- In the original specification, the `WtLFAcc` rule has an unknown type in the subtyping constraint. This cannot be solved (yet?), but also feels wrong, because this allows you to resolve `x` in `new A.x` in "any of A's supertypes". If there are multiple definitions, this gets ambiguous.

- `null` has a polymorphic type. When the type can be inferred, this is not a problem. But in assignments, where the expression being assigned must have a subtype of the thing that it is being assigned to, you get a subtyping constraint with an unknown. This cannot be solved (yet?).

- `override x := e` may resolve to a name in the same class, essentially not overriding anything. This can be overcome by either a) disallowing multiple definitions with the same name or b) resolving references in FieldOverride within the parent class.

- The `WbCDE` rule contains a superfluous `s'` and `s''` (the resolution already captures the associated scope `is''`). A more serious problem is that `s'` is used more than once (the scope associated to `x_i^D` and the scope associated to `x_K^D`).  Moreover, the rule resolves the reference, captures the scope of the declaration, and adds a direct edge to this scope. For this "capturing of the scope", an `Assoc` constraint should be used, not a `GAssoc`. But even then, you could use a named edge as well, which seems to be equivalent?
