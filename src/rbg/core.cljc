(ns rbg.core
  (:require [clojure.string :as s]))

#?(:cljs (enable-console-print!))

(defn hsl-str
  "Formats h s l color variables into css rule as a string."
  [h s l]
  (str "hsl(" h ", " s "%, " l "%)"))

(defn random-color
  "Creates an hsl css color string with random H, S, and L."
  []
  (hsl-str (rand-int 360) (rand-int 101) (rand-int 101)))

(def line-attrs
  "Base css attributes for svg line elements."
  {:vector-effect "non-scaling-stroke"
   :stroke-width 2
   :stroke-linecap "round"
   :fill "none"})

(def rect-attrs
  "Base css attributes for svg rect elements."
  {})

(defn key->str
  [key]
  (s/replace-first (str key) #":" ""))

(defn attr->css
  [attr]
  (let [prop (str (key->str (first attr)) ": ")
        val (str (second attr) ";\n")]
    (str prop val)))

(defn hiccup?
  [item]
  (and
   (vector? item)
   (keyword? (first item))))

(defn css-rule?
  [item]
  (and 
   (vector? item)
   (keyword? (first item))
   (map? (second item))))

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

(defn key->tags
  [key]
  (let [tag (key->str key)
        o (str "<" tag ">")
        c (str "</" tag ">\n")]
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
   (str "<![CDATA[\n"
        css-str
        "\n]]>")])

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
     [:rect (assoc rect-attrs :fill bg-col)]]))

(defn gen-bg
  [w h sc]
  (svg [w h sc] (conj
       (gen-bg-data w h sc)
       (svg-style (my-css (gen-css))))))

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
