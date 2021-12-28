## Static Analysis & Pointer Analysis Concepts & Fundamental

1. IR (Intermediate Representation)
2.

## Taxonomy

1.  Heap Abstraction (Allocation-Site) | ...Source Def?
2.  Context Sensitive | Context Insensitive (\*\*)
3.  Flow Sensitive | Flow Insensitive (\*\*)
4.  Whole-Program Analysis | On-Demand Driven

## Pointer Affecting Statements

```scala
// New
val x = new Bar()

// Assign
val y = x

// Store
bar.foo = foo

// Load
val z = bar.foo

// Call (virtual/special/static)
val x = bar.baz(x,y,... )
```

> why don't we explicit process statements like `foo.a = bar.b` and `foo.a.b.c.d = a` ?
>
> => 3AC

## Rules

### New

`val x = new Bar() // at line i`
$$` \frac {} {o_i \in pt(x)} `$$

### Assign

`val x = y`
$$` \frac {o_i \in pt(y)} {o_i \in pt(x)} `$$

### Store

`x.foo = y`

$$` \frac { o_i \in pt(x), o_j \in pt(y)} { o_j \in pt(o_i.foo) } `$$

### Load

`val x = y.foo`
$$` \frac {o_i \in pt(y), o_j \in(o_i.foo)} { o_j \in pt(x)} `$$

### Call

`val foo = bar.baz(...)`
$$` call `$$

## Fixed Point Theorem & Lattice

## k-CFA

## Reference:

1. https://matt.might.net/articles/implementation-of-kcfa-and-0cfa/
2.
