(ns dk.salza.liq.apps.helpapp
  (:require [dk.salza.liq.editor :as editor]
            [dk.salza.liq.apps.typeaheadapp :as typeaheadapp]
            [dk.salza.liq.apps.promptapp :as promptapp]
            [dk.salza.liq.coreutil :refer :all]
            [clojure.string :as str]))

(defn help-function
  []
  (typeaheadapp/run (str/split-lines (with-out-str (clojure.repl/dir editor)))
                    str #(editor/prompt-set (with-out-str (clojure.repl/find-doc %)))))
;  (editor/prompt-set (str "Not implemented yet"
;    (with-out-str (clojure.repl/dir editor))
;    (with-out-str (clojure.repl/doc editor/doto-buffer)))))

(defn help-apropos
  []
  (promptapp/run clojure.repl/find-doc '("APROPOS")))

(defn help-key
  []
  (str "Not implemented yet"))
