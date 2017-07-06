(ns dk.salza.liq.clojureutil
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [dk.salza.liq.buffer :as buffer]
            [clojure.repl :as repl]))

;(println (repl/apropos #"tmp"))
;(symbol "clojure.string")

(defn get-file-path
  [classpath]
  (str/replace (str (io/resource (str (str/replace classpath #"\." "/") ".clj")))
    #"^file:" ""))

(defn get-class-path
  [buffer alias]
  (if (re-find #"\." alias)
    alias
    (let [content (buffer/get-content buffer)]
      (re-find (re-pattern (str "(?<=\\[)[-a-z\\.]*(?= :as " alias "\\])")) content)
    )))

(defn get-namespace
  [buffer]
  (let [content (buffer/get-content buffer)]
    (re-find #"(?<=\(ns )[-a-z0-9\\.]+" content)))
; (re-pattern "\\(") ;)