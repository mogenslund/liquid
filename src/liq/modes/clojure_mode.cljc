(ns liq.modes.clojure-mode
  (:require [clojure.string :as str]
            [clojure.repl :as repl]
            #?(:clj [clojure.java.io :as io])
            [liq.editor :as editor :refer [apply-to-buffer switch-to-buffer get-buffer]]
            [liq.buffer :as buffer]
            [liq.util :as util])
  (:import #?(:bb []
              :clj [java.net URLClassLoader])))

(defn get-namespace
  [buf]
  (re-find #"(?<=\x28ns )[-a-zA-Z.]+" (buffer/text buf)))

(defn maybe-shorten
  [s full short]
  (if (str/starts-with? s (str full "/"))
    (str short "/" (subs s (inc (count full))))
    s))

(defn get-functions
  "List of alias replaced functions available from namespace"
  [buf]
  (let [n (or (get-namespace buf) "user")
        funs (map str (repl/apropos ""))
        al (ns-aliases (symbol n))]
    (map #(str/replace % "clojure.core/" "")
      (reduce (fn [l [short full]] (map #(maybe-shorten % (str full) short) l))
              funs
              al))))

(defn property-classpath
  []
  (.split (System/getProperty "java.class.path") (System/getProperty "path.separator")))

(defn classloaders-classpath
  []
  #?(:bb (do)
     :clj (->> (.getContextClassLoader (Thread/currentThread)) 
               (iterate #(.getParent ^ClassLoader %))
               (take-while identity)
               (filter #(instance? URLClassLoader %))
               (mapcat #(.getURLs ^URLClassLoader %)))))

(defn classpaths
  []
  (distinct (concat (property-classpath) (classloaders-classpath))))

(defn file-of-var
  [a-var]
  #?(:clj (when-some [path (some-> a-var meta :file)]
            (first (sequence (comp (map #(io/file % path)) (filter #(.exists %))) (classpaths))))))

(let [this-ns *ns*]
  (defn var-at-point
    [buf]
    (some->> (re-find #"\w.*\w\??!?" (-> buf buffer/left buffer/word)) symbol (ns-resolve this-ns))))

(defn goto-var
  [file var]
  (editor/open-file (str file))
  (editor/apply-to-buffer #(let [line (or (-> var meta :line) 1)]
                             (-> %
                                 (buffer/beginning-of-buffer line)
                                 (assoc ::buffer/tow {::buffer/row line ::buffer/col 1})))))

(defn goto-definition
  [buf]
  (try
    (when-some [var (var-at-point buf)]
      (when-some [file (file-of-var var)]
        (goto-var file var)))
    (catch Exception e (str e))))

(defn goto-definition-local
  [buf]
  (let [fun (re-find #"\w.*\w\??!?" (-> buf buffer/left buffer/word))
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
   :special-begin #"(?<=\()(ns |def(n|n-|test|record|protocol|macro|type)? )"
   :green-begin "✔"
   :red-begin "✘"
   :yellow-begin "➜"
   :bold-begin #"(?<=\*)\w+"
   :bold-end #"\*"
   :headline-begin #"^[A-Z0-9][- A-Z0-9ÆØÅ]+$"
   :gherkin-blue #"Feature: |Scenario: |Scenario Outline: |Background: |  Examples:  |  Rule: "
   :gherkin-yellow #"  Given |  When |  Then |  And |  But |  \* "
   ;:gherkin-green #"  Then "
   :definition-begin #"[\w\#\.\-\_\:\+\=\>\<\/\!\?\*]+"
   :definition-end #"."})

(def mode
  {:normal {"g" {"D" #(goto-definition (editor/current-buffer))
                 "d" #(goto-definition-local (editor/current-buffer))}}
   :syntax
    {:plain ; Context
      {:style :plain1 ; style
       :matchers {(match :string-begin) :string
                  (match :keyword-begin) :keyword
                  (match :comment-begin) :comment
                  (match :green-begin) :green
                  ;(match :gherkin-green) :green
                  (match :yellow-begin) :yellow
                  (match :gherkin-yellow) :yellow
                  (match :red-begin) :red
                  (match :bold-begin) :bold
                  (match :special-begin) :special
                  (match :headline-begin) :headline
                  (match :gherkin-blue) :keyword
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

     :headline
      {:style :yellow
       :matchers {#"$" :plain}}

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
