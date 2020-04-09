(ns liq.extras.html-tool
  (:require [clojure.string :as str]
            [liq.buffer :as buffer]
            [liq.modes.clojure-mode :as clojure-mode]
            [liq.modes.help-mode :as help-mode]
            [liq.highlighter :as highlighter]))

(def colors
  {:plain "e4e4ef"
   :definition "95a99f"
   :special "ffdd33"
   :keyword "ffdd33"
   :green "73c936"
   :yellow "ffdd33"
   :red "ff0000"
   :comment "cc8c3c"
   :string "73c936"
   :stringst "73c936"
   :default "aaaaaa"
   nil "e4e4ef"})

(def bgcolors
  {:plain "181818"
   :cursor0 "181818"
   :cursor1 "336633"
   :cursor2 "0000cc"
   :hl "ffff00"
   :selection "ff0000"
   :statusline "000000"
   :default "333333"
   nil "333333"})

(defn delta-style
  [onerow]
  (map #(if (or (nil? (%1 ::buffer/style)) (= (%1 ::buffer/style) (%2 ::buffer/style)))
            %1
            (assoc %1 :delta-style (%1 ::buffer/style)))
     onerow (conj onerow {})))

(defn to-html
  "Use syntax to generate a html version
  of the buffer with monospace and highlight.
  It should be selfcontained. No external
  css file."
  [buf hl]
  (let [hl-buf (highlighter/highlight buf hl)
        onerow (reduce #(concat %1 [{::buffer/char \newline
                                     ::buffer/style ((or (last %1) {::buffer/style :plain}) ::buffer/style)}] %2)
                       (hl-buf ::buffer/lines))]
    (str "<html><body bgcolor=\"" (bgcolors :plain) "\">"
         "<pre><span style=\"color: #" (colors :plain) "; background-color: #" (bgcolors :plain) ";\">"
         (str/join (map #(if-let [style (% :delta-style)]
                           (let [color (or (colors style) (colors :plain))]
                             (str "</span><span style=\"color: #" color ";\">" (% ::buffer/char)))
                           (% ::buffer/char))
                     (delta-style onerow)))
         "</span></pre></body></html>")))

(defn txt-to-link
  [s]
  (str/replace s #"((\w|-)+)\.txt" #(str "<a href=\"" (second %1) ".html\" "
                                         "style=\"color: #" (colors :definition) "; text-decoration: none;\">"
                                         (second %1) ".txt</a>")))

(comment (spit "/tmp/buf.html" (to-html (buffer/buffer "(defn abc\n  [x]\n  (str :test x))") (clojure-mode/mode :syntax))))
