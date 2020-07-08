# rbg
generates a random background of 'tiled lines'. This is a very simple toy :)

You can interact with the code in your browser if you'd like:
[https://adam-james-v.github.io/rbg/](https://adam-james-v.github.io/rbg/)

![example background](./resources/example.svg)

The colors are randomized as well :)

![a different example background](./resources/example2.svg)

## Usage

Clone this repository.

From the top level of the project (where the deps.edn file is located), run the program with:

    clj -m rbg.core > out.svg

To generate a 1920x1080 svg image with a cell size of 10px. You can pass dimension and cell size options:

    clj -m rbg.core 500x300 10 > out.svg
  
It is also possible to run this program in a self-hosted clojurescript environment. I like using [Lumo](http://lumo-cljs.org/).

    lumo --classpath src -m rbg.core > out.svg
    
