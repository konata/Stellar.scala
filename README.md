# Stellar.scala

playgrounds for core static analysis concepts utilizing soot &amp; scala

## Get Started

1. download sbt
2. add your own instrument code inside `src/main/java/playground`
3. edit code in `src/main/scala/app/Application.scala` to specify which method to instrument as entry
4. edit `outputPath` inside `Consts.scala` to specify output path for JSON encoded graphviz source code
5. execute `sbt run` under project
6. you may employ `Stellar.js` project to visualize the analysis process right on browser

## Caveats

1. primitive types are not supported, it's simple to extend at your own effort
2. static fields store / load & array access are not implemented as they can be modeled as a subset of instance field
   stores / loads

## Documentation

see [SLIDES.md](SLIDES.md)









