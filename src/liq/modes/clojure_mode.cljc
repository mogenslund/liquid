(ns liq.modes.clojure-mode
  (:require [clojure.string :as str]
            [clojure.repl :as repl]
            [clojure.java.io :as io]
            [liq.modes.fundamental-mode :as fundamental-mode]
            [liq.editor :as editor :refer [apply-to-buffer switch-to-buffer get-buffer]]
            [liq.buffer :as buffer]
            [liq.util :as util]))

(defn get-namespace
  [buf]
  (re-find #"(?<=\x28ns )[-a-zA-Z.]+" (buffer/text buf)))

(defn get-functions
  "List of alias replaced functions available from namespace"
  [buf]
  (let [n (or (get-namespace buf) "user")
        funs (map str (repl/apropos ""))
        al (ns-aliases (symbol n))]
    (map #(str/replace % "clojure.core/" "")
      (reduce (fn [l [short full]]
                  (map #(str/replace % (re-pattern (str "^" full "/")) (str short "/")) l))       
              funs
              al))))

(defn goto-definition
  [buf]
  (let [fun (re-find #"\w.*\w" (-> buf buffer/left buffer/word))
        cpaths (seq (.getURLs (java.lang.ClassLoader/getSystemClassLoader)))]
    (when fun
      (try
        (let [info (load-string (str "(meta #'" fun ")"))
              path (first (filter #(.exists %) (map #(io/file % (info :file)) cpaths)))]
          (when path
            (editor/open-file (str path))
            (editor/apply-to-buffer #(-> %
                                         (buffer/beginning-of-buffer (or (info :line) 1))
                                         (assoc ::buffer/tow {::buffer/row (or (info :line) 1) ::buffer/col 1})))))
        (catch Exception e (str "caught exception: " (.getMessage e)))))))

(defn goto-definition-local
  [buf]
  (let [fun (re-find #"\w.*\w" (-> buf buffer/left buffer/word))
        hit #(-> %
                 buffer/beginning-of-buffer
                 (buffer/search (str "\\x28defn? " fun)))]
    (when (and fun (> (-> (hit buf) ::buffer/cursor ::buffer/row) 1))
      (editor/apply-to-buffer hit))))

(def match
  {:keyword-begin #"(?<=(\s|\(|\[|\{)|^):[\w\#\.\-\_\:\+\=\>\<\/\!\?\*]+(?=(\s|\)|\]|\}|\,|$))"
   :keyword-end #".|$"
   :string-begin #"(?<!\\\\)(\")"
   :string-escape #"(\\\")"
   :string-end "\""
   :comment-begin #"(?<!\\\\);.*$|^#+ .*$"
   :comment-end #"$"
   :special-begin #"(?<=\()(ns |def(n|n-|test|record|protocol|macro)? )"
   :green-begin "✔"
   :red-begin "✘"
   :yellow-begin "➜"
   :bold-begin #"(?<=\*)\w+"
   :bold-end #"\*"
   :definition-begin #"[\w\#\.\-\_\:\+\=\>\<\/\!\?\*]+"
   :definition-end #"."})

(def mode
  {:normal {"g" (assoc ((fundamental-mode/mode :normal) "g")
                       "D" #(goto-definition (editor/current-buffer))
                       "d" #(goto-definition-local (editor/current-buffer)))}
   :syntax
    {:plain ; Context
      {:style :plain1 ; style
       :matchers {(match :string-begin) :string
                  (match :keyword-begin) :keyword
                  (match :comment-begin) :comment
                  (match :green-begin) :green
                  (match :yellow-begin) :yellow
                  (match :red-begin) :red
                  (match :bold-begin) :bold
                  (match :special-begin) :special
                  #"[-a-zA-Z0-9]+\.txt" :topic
                  #"---.*---" :topic
                  #"===.*===" :topic}}
     :string
       {:style :string
        :matchers {(match :string-escape) :string
                   (match :string-end) :string-end}}
     :string-end
       {:style :string
        :matchers {#".|$|^" :plain}}

     :comment
       {:style :comment
        :matchers {(match :comment-end) :plain}}

     :keyword
       {:style :keyword
        :matchers {(match :keyword-end) :plain}}
      
     :special
       {:style :special
        :matchers {(match :definition-begin) :definition}}

     :green
       {:style :green
        :matchers {#".|$|^" :plain}}

     :yellow
       {:style :yellow
        :matchers {#".|$|^" :plain}}

     :red
       {:style :red
        :matchers {#".|$|^" :plain}}

     :bold
       {:style :green
        :matchers {(match :bold-end) :plain}}

     :definition
       {:style :definition
        :matchers {(match :definition-end) :plain}}

     :topic {:style :definition
             :matchers {#".|$" :plain}}}})