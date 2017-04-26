(ns dk.salza.liq.adapters.ghostadapter
  (:require [dk.salza.liq.tools.util :as util]
            [dk.salza.liq.renderer :as renderer]
            [dk.salza.liq.editor :as editor]
            [clojure.string :as str]))

(defn send-input
  [inp]
  (editor/handle-input inp))

(defn get-display
  []
  (renderer/render-screen))
