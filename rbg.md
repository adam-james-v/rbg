# rbg
The rbg project is a very simple Clojure project. The output of the program is a randomly generated background using a simple 'tiled lines' approach. The line orientations, line colour, and background colour are all random. The user can input the output images's width, height, and grid size in pixels.

This is clearly not an 'important' project. The method behind generation is simplistic and the output is not groundbreaking. My purpose for this project is less concerned with the program's usefulness and more focused on the skills required to actually produce the code.

Put simply, the purpose of rbg was to prove to myself that I actually **can** complete code projects. Yes, this is small and simple, but it's a step in the right direction, and I'm proud of that.

Additionally, I am quite happy to have written a program that works in a Clojure environment (on the JVM), in a Clojurescript environment (on node.js via Lumo), and in the browser (with the amazing Klipse plugin). To write one program that works within different environments is exciting.

## design
Even though this is a simple program, design choices are still required. It's not like I spent days diagramming possible choices, but as I wrote the code I did have to consider a few things.

### algorithm
I'm not exactly sure this is complex enough to really count as an algorithm, but for lack of a better word, I'll call it one anyway. Besides, the word **algorithm** sounds important and computer-sciency, which is nice.

The generated background will use the 'tiled lines' approach, which works as follows:

1.  Create a grid with square cells filling the image size
2.  For Each cell, randomly draw one of the following:
    -   a line from the bottom left to top right corners
    -   a line from the top left to bottom right corners

The resulting image does actually look pretty neat. I find it fascinating how simple either/or random selection can give rise to an image that seems complex. Good stuff.

### interface
Creating a GUI for this program is overkill. I'll stick with running the program via the terminal. This keeps design very simple as all I have to do is create a -main function that takes the user's arguments and runs the algorithm.

A notable difference of interface is this page you're reading now. The Klipse plugin interprets the clojure(script) code blocks and executes them live in the browser. This is still a text-based interface, but it exposes the entire program to the user for manipulation, if they so chose.

### output
I often create html documents with embedded svg diagrams, and I figure that's a nice output format. Rbg outputs an svg string to stdout so that the user can direct it as they please. Most likely a user will want to push the output into an svg file.

Once again, this web page is an exception to the output method. I have written a function at the end of this page that displays the program's output by injecting the svg into a div.

## ns
The only dependency that I need is clojure.string because it has the **replace** function, which I will use to manipulate strings in a few places.

{kl}
(ns rbg.core
  (:require [clojure.string :as s]))
{kl}

## colour
Since I'm using svg as the output, I can use CSS to style the background and lines. Colour is fun to randomize and is simple enough to do. You can use random RGB values by selecting an integer between 0 and 256, but I might want to disallow 'ugly' sections of the colour spectrum in the future. I think that using HSL (Hue, Saturation, and Light) colour specifications will let me do that better. I could limit the Saturation and Lightness to be between 50% and 75%, for example, or limit the Hue to only one quadrant of the colour wheel.

Hue is specified as an angle in degrees around the colour wheel, so I can select a random integer between 0 (inclusive) and 360 (exclusive).

Saturation and Lightness both are specified as percent, so select a random integer between 0 and 101 for both saturation and lightness.

{kl}
(defn hsl-str
  [h s l]
  (str "hsl(" h ", " s "%, " l "%)"))

(defn random-color
  []
  (hsl-str (rand-int 360) (rand-int 101) (rand-int 101)))
{kl}

## base-attributes
CSS is used to style the lines, and there are a few attributes that I want to set up front.

**vector-effect** forces the stroke of an svg line not to scale. This is important to set because I am using scale transformations on each line in the grid. If I did not set the scaling stroke, I would have to scale the stroke width up or down according to the scale, and that feels less intuitive.

This way, the **stroke-width** is set to 2px, which matches visually.

{kl}
(def line-attrs
  {:vector-effect "non-scaling-stroke"
    :stroke-width 2
    :stroke-linecap "round"})
{kl}

## css-compiler
In my prototype version of rbg, I had a dependency on garden, a clojure library that compiles hiccup-like data structures to css. I thought it would be a useful exercise to eliminate this dependency by writing my own minimal css compiler implementation.

{kl}
(defn css-rule?
  [item]
  (and 
    (vector? item)
    (keyword? (first item))
    (map? (second item))))

(defn key->str
  [key]
  (s/replace-first (str key) #":" ""))

(defn attr->css
  [attr]
  (let [prop (str (key->str (first attr)) ": ")
        val (str (second attr) ";\n")]
    (str prop val)))

(defn rule->css
  [rule]
  (let [tag (str (key->str (first rule)) " {\n")
        props (apply str (map attr->css (second rule)))]
    (str tag props "}\n")))

(defn my-css
  [rule]
  (if (css-rule? rule)
    (rule->css rule)
    (apply str
            (concat (map my-css rule)))))
{kl}

## html-compiler

{kl}
(defn hiccup?
  [item]
  (and
    (vector? item)
    (keyword? (first item))))

(defn key->tags
  [key]
  (let [tag (key->str key)
        o (str "<" tag ">")
        c (str "<" "/" tag ">\n")]
    [o c]))

(defn insert-props
  [tag props]
  (if (> (count props) 0)
    (s/replace tag #">" (str " " props ">"))
    tag))

(defn attr->html
  [attr]
  (let [prop (str (key->str (first attr)) "='")
        val (str (second attr) "' ")]
    (str prop val)))

(declare my-html)
(defn hiccup->html
  [[k m & content]]
  (let [[o c] (key->tags k)
        [m content] (if (map? m)
                      [m (if content content (list nil))]
                      [{} (conj content m)])
        props (apply str (map attr->html m))
        o (insert-props o props)]
    (cond
      ;; snippet is empty
      (not (first content))
      (str o c)
      ;; snippet contains a string
      (and (string? (first content)) (= (count content) 1))
      (str o (first content) c)
      ;; snippet contains nested snippets
      :else
      (str o (my-html content) c))))

(defn my-html
  [hiccup]
  (if (hiccup? hiccup)
    (hiccup->html hiccup)
    (apply str
            (concat (map my-html hiccup)))))
{kl}

## svg-elements

{kl}
(defn svg
  [[w h sc] content]
  [:svg {:width w
          :height h
          :viewbox (str "0 0 " w " " h)
          :xmlns "http://www.w3.org/2000/svg"}
    [:g {:transform (str "scale(" sc ")")} content]])

(defn svg-style
  "Wraps a css string in CDATA tags for embedding inside svg elements."
  [css-str]
  [:style {:type "text/css"}
    (str "<" "![CDATA[\n"
        css-str
        "\n]]" ">")])

(defn rectangle
  [w h]
  [:rect {:width w
          :height h}])

(defn line
  [a b]
  (let [[x1 y1] a
        [x2 y2] b]
    [:line {:x1 x1
            :y1 y1
            :x2 x2
            :y2 y2}]))

(defn move-line
  [[x y] line]
  (let [[ox1 oy1 ox2 oy2] (map #(get-in line [1 %]) [:x1 :y1 :x2 :y2])
        [nx1 nx2] (map + [ox1 ox2] (repeat x))
        [ny1 ny2] (map + [oy1 oy2] (repeat y))]
    (assoc line 1 {:x1 nx1 :y1 ny1 :x2 nx2 :y2 ny2})))
{kl}

## generator

{kl}
(defn gen-bg-data
  [w h sc]
  (let [lines [(line [0 0] [1 1])
                (line [0 1] [1 0])]]
    (concat
      [(rectangle (/ w sc) (/ h sc))]
      (for [x (range (/ w sc))
            y (range (/ h sc))]
        (move-line [x y] (get lines (rand-int 2)))))))

(defn gen-css
  []
  (let [bg-col (random-color)
        l-col (random-color)]
    [[:line (assoc line-attrs :stroke l-col)]
      [:rect (assoc {} :fill bg-col)]]))

(defn gen-bg
  [w h sc]
  {:pre [(>= sc 5)]}
  (svg [w h sc] (conj
        (gen-bg-data w h sc)
        (svg-style (my-css (gen-css))))))
{kl}

## -main

{kl}
(defn str->int [s]
  #?(:clj  (java.lang.Integer/parseInt s)
      :cljs (js/parseInt s)))

(defn wxh
  [size]
  (map str->int (s/split size #"x")))

(defn -main
  ([]
    (-main "1920x1080" "10"))

  ([size sc]
    (let [[w h] (wxh size)
          sc (str->int sc)]
      (println (my-html (gen-bg w h sc))))))
{kl}

# run

You can run rbg in your terminal using clj:

Navigate to the project's top-level folder (where deps.edn file is) and run: 

`clj -m rbg.core` which prints an svg string to output. 

The default arguments are a size of "1920x1080" and a cell size of 10 pixels.

You can run with custom resolutions and cell sizes by passing arguments:

`clj -m rbg.core 500x300 10`

CAUTION: a small cell size will result in a very large SVG file. Recommmended minimum is a cell size of 10 pixels. 

You can also run this program with a Clojurescript environment. I like lumo:

`lumo --classpath src -m rbg.core`

