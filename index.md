# rbg
**published: 2020-07-06**
**edited: 2020-07-12**

Random Background Generator (rbg) is a very simple Clojure project that produces a randomly generated background using a simple 'tiled lines' approach. The line orientations, line colour, and background colour are random. The user can input the resulting images's width, height, and grid size in pixels. You can see the code in action throughout this post, and can see the rendered result at the bottom of the page. It's even possible to edit the generator call and see new results rendered live.

This is not an important project. The method behind generation is simplistic and the output is not groundbreaking. My purpose for this project is less concerned with the program's usefulness and more focused on the skills required to actually produce the code.

Put simply, the purpose of rbg is to prove to myself that I actually **can** complete code projects. Yes, this is small and simple, but it's a step in the right direction, and I'm proud of that.

Additionally, I am quite happy to have written a program that works in a Clojure environment (on the JVM), in a Clojurescript environment (on node.js via Lumo), and in the browser (with the amazing Klipse plugin). To write one program that works within different environments is exciting.

## design
Even though this is a simple program, design choices are still required. I didn't spend days making architecture diagrams, but I had a few decisions to make as I wrote the program.

### algorithm
I'm not exactly sure this is complex enough to really count as an algorithm, but for lack of a better word, I'll call it one anyway. Besides, the word **algorithm** sounds important and computer sciency, which is nice.

The generated background will use the 'tiled lines' approach, which works as follows:

1.  Create a grid with square cells filling the image size
2.  For Each cell, randomly draw one of the following:
- a line from the bottom left to top right corners
- a line from the top left to bottom right corners

The resulting image does actually look pretty neat. I find it fascinating how simple either/or random selection can give rise to an image that seems complex. Good stuff.

### interface
Creating a GUI for this program is overkill. I'll stick with running the program via the terminal. This keeps design very simple as all I have to do is create a main function that takes the user's arguments and runs the algorithm.

A notable difference of interface is this page you're reading now. The Klipse plugin interprets the clojure(script) code blocks and executes them live in the browser. This is still a text based interface, but it exposes the entire program to the user for manipulation, if they so chose.

### output
I often create html documents with embedded svg diagrams, and I figure that's a nice output format. Rbg outputs an svg string to stdout so that the user can direct it as they please. Most likely a user will want to push the output into an svg file.

Once again, this web page is an exception to the output method. I have written a function at the end of this page that displays the program's output by injecting the svg into a div.

## ns
The only dependency that I need is clojure.string because it has the **replace** function, which I will use to build strings.

{kl}
(ns rbg.core
  (:require [clojure.string :as s]))
{kl}

### test-ns

{kl}
#_(ns rbg.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [rbg.core :as rbg]))

"tests included here for reference. They run best in CLJ on desktop."
{kl}

## colour
Since I'm using svg as the output, I can use CSS to style the background and lines. Colour is fun to randomize and is simple enough to do. You can use random RGB values by selecting an integer between 0 and 256, but I might want to disallow 'ugly' sections of the colour spectrum in the future. I think that using HSL (Hue, Saturation, and Light) colour specifications will let me do that better. I could limit the Saturation and Lightness to be between 50% and 75%, for example, or limit the Hue to only one quadrant of the colour wheel.

Hue is specified as an angle in degrees around the colour wheel, so I can select a random integer between 0 (inclusive) and 360 (exclusive).

Saturation and Lightness both are specified as percent, so select a random integer between 0 and 101 for both saturation and lightness.

{kl}
(defn hsl-str
  [h s l]
  {:pre [(and (<= 0 h 359)
              (<= 0 s 100)
              (<= 0 l 100))]}
  (str "hsl(" h ", " s "%, " l "%)"))

(defn random-colour
  []
  (hsl-str (rand-int 360) (rand-int 101) (rand-int 101)))
{kl}

### colour-tests
My `hsl-str` function uses a precondition to validate the inputs. I simply check that each value is correctly inside the appropriate ranges. I'll write a test to ensure that this validation is indeed working as expected.

{kl}
#_(deftest hsl-str-validation
  (testing "hsl-str validation"
    (testing "throws"
      (are [h s l] (thrown? AssertionError (rbg/hsl-str h s l))
        360 0 0
        0 101 0
        0 0 101))
    (testing "succeeds"
      (are [h s l] (string? (rbg/hsl-str h s l))
        0 0 0
        359 100 100
        237 50 87))))

"tests included here for reference. They run best in CLJ on desktop."
{kl}

## base-attributes
CSS is used to style the lines, and there are a few attributes that I want to set up front.

**vector-effect** forces the stroke of an svg line not to scale. This is important to set because I will be using scale transformations on each line in the grid. If I did not set the scaling stroke, I would have to scale the stroke width up or down according to the scale, and that feels less intuitive.

{kl}
(def line-attrs
  {:vector-effect "non-scaling-stroke"
    :stroke-width 2
    :stroke-linecap "round"})
{kl}

## helpers
Some functions are useful in a few different contexts. For example, both the html and css compilers need a function that converts a key into a string.

{kl}
(defn key->str
  "Turns :key into \"key\"."
  [key]
  (apply str (rest (str key))))
{kl}

### helpers-tests
My `key->str` function has some potentially surprising behaviour. It cuts the ':' off the front of the key to make the string. This is comparable to the `keyword` function which takes a string input and adds the ':' to the front. I wrote a simple test to capture the intent of this conversion.

{kl}
#_(deftest key->str-test
  (is (= (rbg/key->str :adam) "adam")
      "':' char is not included in string result."))

"tests included here for reference. They run best in CLJ on desktop."
{kl}

## css-compiler
In my prototype version of rbg, I had a dependency on garden, a clojure library that compiles hiccup like data structures to css. I thought it would be a useful exercise to eliminate this dependency by writing my own minimal css compiler implementation.

The basic idea is to write a function `my-css` that compiles vectors like this:

`[:tag {:attr val}]`

into valid css strings like this:

`tag {`
`  attr: val;`
`}`

I also want to be able to pass in a list of css rules into the compiler, so I need to write a predicate function that checks for css rules. This is to differentiate between a list of rules and a rule, since they will both be seqable pieces of data.

{kl}
(defn css-rule?
  [item]
  (and 
    (vector? item)
    (keyword? (first item))
    (map? (second item))
    (not (empty? (second item)))))
{kl}

CSS rules will always have a map of attribute value pairs. I can write a function that transforms a single attribute value pair into a proper string. Then I can map the function over the rule's attribute map.

{kl}
(defn attr->css
  [attr]
  (let [prop (str "  " (key->str (first attr)) ": ")
        val (str (second attr) ";\n")]
    (str prop val)))
{kl}

To create a full css rule string is straightforward at this point. Convert the tag key and attribute map with the proper functions and string them together.

The minimal css compiler can now be built by using basic recursion. If the compiler is passed in a single css rule, tranform it with `rule->css`. If a list of rules is passed, map the compiler over the list. 

Since a single rule terminates the recursion, this compiler will work on single rules, flat rule lists, and nested rules lists.

{kl}
(defn rule->css
  [rule]
  {:pre [(css-rule? rule)]}
  (let [tag (str (key->str (first rule)) " {\n")
        attrs (apply str (map attr->css (second rule)))]
    (str tag attrs "}")))

(defn my-css
  [rule]
  (if (css-rule? rule)
    (rule->css rule)
    (apply str (interpose "\n\n" 
                          (concat (map my-css 
                                        (filter css-rule? rule)))))))
{kl}

### css-compiler-tests
For my minimal compiler, I have written a minimal test that simply checks for correct output given a valid css rule. I also have one test confirming that the predicate based precondition correctly throws an AssertionError when an invalid data structure is passed into the compiler.

{kl}
#_(deftest rule->css-validation-test
  (is (thrown? AssertionError (rbg/rule->css [:p]))))

#_(deftest my-css-test
  (is (= (rbg/my-css [:tag {:attr "value"}])
          "tag {\n  attr: value;\n}"))
  (is (= (rbg/my-css (list [:tag1 {:a "val-a"}]
                            [:tag2 {:b "val-b"}]))
          "tag1 {\n  a: val-a;\n}\n\ntag2 {\n  b: val-b;\n}")))

"tests included here for reference. They run best in CLJ on desktop."
{kl}

## html-compiler
My prototype rbg version also used hiccup to compile html snippets, a dependency I worked to eliminate for this small project. The ideas behind my html compiler implementation are very similar to how I build the css compiler, so I begin by creating a predicate to check for valid hiccup data structures, which look like this:

`[:tag {:optional map} "inner"]`

The compiled html string is:

`&lt;tag optional='map'&gt;`
`  inner`
`&lt;/tag&gt;`

{kl}
(defn hiccup?
  [item]
  (and
    (vector? item)
    (keyword? (first item))))
{kl}

The key in a hiccup vector corresponds to the html tag, and the optional attributes map can show up as properties inside the opening tag. I created three functions to handle the string creation for tags, attributes, and tags containing properties.

{kl}
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
{kl}

The css compiler did not have to consider nested CSS rules, so simply mapping the compiler over a list of rules was sufficient to build the desired structure. The html compiler must handle the more complicated case of having both lists of hiccup items as well as nested hiccup items. So, the `hiccup->html` function contains a cond to handle the various possibilities.

The compiler is built in a simple manner by either compiling the single hiccup item or calling the compiler recursively over a list of hiccup items.

{kl}
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
Now that I have a css and html compiler, I can create functions that build up the hiccup structure for my background. I will use svg to contain the image and can embed the css in a CDATA tag inside the svg, which I can also wrap up into a function.

The background colour can be controlled easily by drawing a rectangle element over the entire svg, so I need a rectangle function.

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
        "]]" ">")])

(defn rectangle
  [w h]
  [:rect {:width w
          :height h}])
{kl}

When I created my prototype, I handled line moves by wrapping every line element in a group which had a translate transformation applied to it. This worked ok, but creates very large svg files since every line element has both a <g> tag and the line tag. To keep the data created smaller, I changed my approach to line movement by creating a`move-line` function.

The `move-line` function works by adding the x and y translation directly to the line element's properties. This elminates the need for group tags in the final SVG, keeping the size down.

{kl}
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
All of the pieces are built so I can now wire them together with some generators. First, I generate the background data by creating a grid according to the width, height, and cell size values. Each cell will either have a forward slash line (/) or backslash line (\\), randomly selected. This is handled by the for construction in the `gen-data` function. 

Clojure's vectors are like a hash map mapping indices to values, so you can randomly select either line with `(get lines (rand-int 2))`.

{kl}
(defn gen-data
  [w h sc]
  (let [lines [(line [0 0] [1 1])
                (line [0 1] [1 0])]]
    (concat
      [(rectangle (/ w sc) (/ h sc))]
      (for [x (range (/ w sc))
            y (range (/ h sc))]
        (move-line [x y] (get lines (rand-int 2)))))))
{kl}

The css is just randomly selected colours for the lines and the background. To create the final background data structure, all I have to do is wrap the data and css with the `svg-style` and svg functions defined earlier.

{kl}
(defn gen-css
  []
  (let [bg-col (random-colour)
        l-col (random-colour)]
    [[:line (assoc line-attrs :stroke l-col)]
      [:rect (assoc {} :fill bg-col)]]))

(defn gen-bg
  [w h sc]
  {:pre [(>= sc 5)]}
  (svg [w h sc] (conj
        (gen-data w h sc)
        (svg-style (my-css (gen-css))))))
{kl}

### generator-tests
I placed a restriction on the cell scale size to prevent the user from creating single pixel cells. Since the program generates line elements for every cell, the total amount of data generated can grow rapidly when you specify a large image size and small scale. 5px is the minimum size enforced by a precondition check. This test confirms that the precondition validation is working correctly.

{kl}
#_(deftest gen-bg-validation-test
  (testing "gen-bg input validation"
    (is (thrown? AssertionError (rbg/gen-bg 10 10 1)) 
        "fails with sc < 5")
    (is (rbg/hiccup? (rbg/gen-bg 5 5 5))
        "succeeds with sc >= 5")))

"tests included here for reference. They run best in CLJ on desktop."
{kl}

## -main-klipse
The `-main` function in the context of this post is built to inject the svg directly into the DOM. On the desktop, this following snippet is not included in the source code as it has no meaning in that context.

This code selects the canvas div and injects the svg.

{kl}
(def bg (gen-bg 300 200 10))
(defn -main []
  (let [canvas (js/document.getElementById "canvas")]
    (set! (.-innerHTML canvas) (my-html bg))))

(do (-main) nil)
{kl}

The result of all that hard work:

{ex}
[:div#canvas]
{ex}

I'm quite proud of that. :)

