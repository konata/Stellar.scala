1. Static Analysis & Pointer Analysis
2. Taxonomy
    1. Heap Abstraction (Allocation-Site) | ...Source Def?
    2. Context Sensitive | Context Insensitive
    3. Flow Sensitive | Flow Insensitive
    4. Whole-Program Analysis | On-Demand Driven
3. Pointer Affecting Statements
   ```scala
   // Alloc
   val x = new Bar()
   // Assign
   val y = x
   // store
   bar.foo = foo
   // load
   val z = bar.foo
   // call (virtual/special/static)
   val x = bar.baz(x,y,... )
   ```
4. Rules
   `val x = new Bar() // at line i` 
   [Alloc](art/alloc.png)

   `val x = y`
   [Assign](art/assign.png)

   `x.foo = y`
   [Store](art/store.png)

   `val x = y.foo`
   [Load](art/load.png)

   Call:
   [Call](art/call.png)

5. K-CFA


6. Lattice & fixed point




7. Reference:
   1. https://matt.might.net/articles/implementation-of-kcfa-and-0cfa/
   2. 




