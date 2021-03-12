(ns liq.modes.help-mode
  (:require [clojure.string :as str]
            #?(:clj [clojure.java.io :as io])
            [liq.editor :as editor :refer [apply-to-buffer switch-to-buffer get-buffer]]
            [liq.buffer :as buffer]
            [liq.util :as util]))

(defn load-topic
  [topic]
  #?(:clj (if-let [id (editor/get-buffer-id-by-name (str "*Help - " topic "*"))]
            (switch-to-buffer id)
            (when-let [path (if (re-find #"/" topic)
                              topic
                              (or (io/resource (str "help/" topic))
                                  (io/resource (str "help/" topic ".txt"))))]
              (editor/new-buffer (slurp path)
                                 {:major-modes (list :help-mode :spacemacs-mode :clojure-mode :fundamental-mode) :name (str "*Help - " topic "*")})))))
 

; (slurp (io/resource (str "help/" topic)))
; (slurp (io/resource "help/index.txt"))

(defn load-topic-at-point
  []
  (let [buf (editor/current-buffer)
        parent (re-find #"/.*/" (buf ::buffer/name))
        topic (str parent (re-find #"[^:\(\)\[\]\{\}]+" (buffer/word buf)))]
    (when (re-matches #".*\.txt" topic)
      (load-topic topic))))

(defn run
  ([topic] (load-topic topic))
  ([] (run "help.txt")))

(def mode
  {:insert {"esc" (fn [] (apply-to-buffer #(-> % (assoc ::buffer/mode :normal) buffer/left)))}
   :normal {"q" editor/previous-buffer
            "c" {"p" {"p" :eval-sexp-at-point
                      "r" :raw-eval-sexp-at-point
                      "f" :evaluate-file-raw}
                 "i" #(fn [])
                 "a" #(fn [])
                 "c" #(fn [])
                 "$" #(fn [])
                 "e" #(fn [])
                 "E" #(fn [])
                 "w" #(fn [])}
            "i" #(fn [])
            "o" #(fn [])
            "O" #(fn [])
            "r" #(fn [])
            "x" #(fn [])
            "p" #(fn [])
            "P" #(fn [])
            "d" #(fn [])
            "A" #(fn [])
            "D" #(fn [])
            "C" #(fn [])
            "J" #(fn [])
            "\n" load-topic-at-point
            "C-]" load-topic-at-point}
    :init run})
