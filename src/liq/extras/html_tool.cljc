(ns liq.extras.html-tool
  (:require [clojure.string :as str]
            [liq.buffer :as buffer]
            [liq.modes.clojure-mode :as clojure-mode]
            [liq.modes.help-mode :as help-mode]
            [liq.highlighter :as highlighter]))

(def default-colors
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

(def default-bgcolors
  {:plain "181818"
   :cursor0 "181818"
   :cursor1 "336633"
   :cursor2 "0000cc"
   :hl "ffff00"
   :selection "ff0000"
   :statusline "000000"
   :default "333333"
   nil "333333"})

(def bw-colors
  {:plain "777777"
   :definition "000000"
   :special "000000"
   :keyword "000000"
   :green "000000"
   :yellow "333333"
   :red "000000"
   :comment "000000"
   :string "888888"
   :stringst "888888"
   :default "333333"
   nil "777777"})

(def bw-bgcolors
  {:plain "ffffff"
   :cursor0 "ffffff"
   :cursor1 "ffffff"
   :cursor2 "ffffff"
   :hl "ffffff"
   :selection "ffffff"
   :statusline "ffffff"
   :default "ffffff"
   nil "ffffff"})

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
  ([buf hl colors bgcolors]
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
  ([buf hl] (to-html buf hl default-colors default-bgcolors))
  ([buf] (to-html buf (clojure-mode/mode :syntax) default-colors default-bgcolors)))

(defn to-html-bw
  [buf]
  (to-html buf (clojure-mode/mode :syntax) bw-colors bw-bgcolors))

(defn txt-to-link
  ([s colors]
   (str/replace s #"((\w|-)+)\.txt" #(str "<a href=\"" (second %1) ".html\" "
                                          "style=\"color: #" (colors :definition) "; text-decoration: none;\">"
                                          (second %1) ".txt</a>")))
  ([s] (txt-to-link s default-colors)))

(comment
  (ns user (:require [liq.extras.html-tool :as h]))
  (spit "/tmp/buf.html" (h/to-html (buffer/buffer "(defn abc\n  [x]\n  (str :test x))") (clojure-mode/mode :syntax)))
  (spit "/tmp/buf.html" (h/to-html-bw (buffer/buffer "(defn abc\n  [x]\n  (str :test x))")))
  (spit "/tmp/buf.html" (h/to-html-bw (editor/get-buffer "scratch"))))
