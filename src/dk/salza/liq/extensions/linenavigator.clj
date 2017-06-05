(ns dk.salza.liq.extensions.linenavigator
  (:require [dk.salza.liq.editor :as editor]
            [dk.salza.liq.apps.typeaheadapp :as typeaheadapp]
            [clojure.string :as str]))

(defn callback
  [item]
  (editor/beginning-of-buffer)
  (editor/find-next item)
  (editor/top-align-page))

(defn run
  []
  (typeaheadapp/run (doall (str/split-lines (editor/get-content)))
                     str ;second
                     callback))

