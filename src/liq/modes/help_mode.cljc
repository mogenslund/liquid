(ns liq.modes.help-mode
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [liq.editor :as editor :refer [apply-to-buffer switch-to-buffer get-buffer]]
            [liq.buffer :as buffer]
            [liq.util :as util]
            [liq.modes.clojure-mode :as clojure-mode]))

(defn load-topic
  [topic]
  (if-let [id (editor/get-buffer-id-by-name (str "*Help - " topic "*"))]
    (switch-to-buffer id)
    (when-let [path (if (re-find #"/" topic)
                      topic
                      (or (io/resource (str "help/" topic))
                          (io/resource (str "help/" topic ".txt"))))]
      (editor/new-buffer (slurp path)
                         {:major-modes (list :help-mode :spacemacs-mode :clojure-mode :fundamental-mode) :name (str "*Help - " topic "*")}))))
 

; (slurp (io/resource (str "help/" topic)))
; (slurp (io/resource "help/index.txt"))

(defn load-topic-at-point
  []
  (let [buf (editor/current-buffer)
        parent (re-find #"/.*/" (buf ::buffer/name))
        topic (str parent (re-find #"[^:\(\)\[\]\{\}]+" (buffer/get-word buf)))]
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
    :init run
    :syntax
      (-> clojure-mode/mode
          :syntax
          (assoc-in [:plain :matchers #"[-a-zA-Z0-9]+\.txt"] :topic)
          (assoc-in [:plain :matchers #"---.*---"] :topic)
          (assoc-in [:plain :matchers #"===.*==="] :topic)
          (assoc :topic {:style :definition
                         :matchers {#".|$" :plain}}))})
     ;{:plain ; Context
     ;  {:style :plain ; style
     ;   :matchers {#"[-a-zA-Z0-9]+\.txt" :topic
     ;              #"---.*---" :topic
     ;              #"===.*===" :topic}}
     ; :topic
     ;  {:style :definition
     ;   :matchers {#".|$" :plain}}}})








