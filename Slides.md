## Static Analysis & Pointer Analysis Concepts & Fundamental

1. IR (Intermediate Representation)
2. Accuracy & Efficiency
3. Pointers
   1. Variables likes `a`
   2. Fields like `foo.bar.baz`
   3. model `array-access & static-field` as a trivial-case of fields(above)

## Taxonomy

1.  Heap Abstraction (Allocation-Site) | ...Source Definition?
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

// Call (virtual / special / static / interface / dynamic)
val x = bar.baz(x,y,... )
```

> why don't we explicit process statements like `foo.a = bar.b` and `foo.a.b.c = a` ?
>
> => 3AC

## Rules

### New

`val x = new Bar() // at line i`

```math
\frac {} {o_i \in pt(x)}
```

$$ \frac {} {o_i \in pt(x)} $$

### Assign

`val x = y`

```math
\frac {o_i \in pt(y)} {o_i \in pt(x)}
```

$$ \frac {o_i \in pt(y)} {o_i \in pt(x)} $$

### Store

`x.foo = y`

```math
\frac { o_i \in pt(x), o_j \in pt(y)} { o_j \in pt(o_i.foo) }
```

$$ \frac { o_i \in pt(x), o_j \in pt(y)} { o_j \in pt(o_i.foo) } $$

### Load

`val x = y.foo`

```math
\frac {o_i \in pt(y), o_j \in(o_i.foo)} { o_j \in pt(x)}
```

$$ \frac {o_i \in pt(y), o_j \in(o_i.foo)} { o_j \in pt(x)} $$

### Call

`val foo = bar.baz(...)`
$$ call $$

> P.s logic style formula just like CodeQL

## Fixed Point Theorem & Lattice

### iterative algorithm

$$ f(x) = y $$
$$ f^2(x) = f(f(x)) $$
$$ ... $$
$$ f^n(x) = f(f^{n-1}(x))$$
when
$$ f^k(x) = f^{k-1}(x) $$
we say f reach its fixed-point when
$$ x\_{k-1} = f^{k-1}(x) $$

## k-CFA

## Reference:

1. https://matt.might.net/articles/implementation-of-kcfa-and-0cfa/
2.
