(ns dk.salza.liq.apps.commandapp
  (:require [dk.salza.liq.editor :as editor]
            [dk.salza.liq.tools.cshell :as cs]
            [dk.salza.liq.apps.promptapp :as promptapp]
            [dk.salza.liq.apps.textapp :as textapp]
            [dk.salza.liq.apps.typeaheadapp :as typeaheadapp]
            [dk.salza.liq.slider :refer :all]
            [dk.salza.liq.coreutil :refer :all]
            [clojure.string :as str]))


(defn execute
  [hit]
  (cond (= (first hit) :buffer) (editor/switch-to-buffer (second hit))
        (= (first hit) :snippet) (editor/insert (second hit))
        (= (first hit) :file)   (textapp/run (second hit)) ;(editor/find-file (second hit))
        (= (first hit) :command) (editor/eval-safe (second hit))
        (= (first hit) :interactive) (let [fun (second (second hit))
                                           params (nth (second hit) 2)]
                                       (if params
                                         (promptapp/run fun params)
                                         (editor/eval-safe fun)))
        true (editor/previous-buffer)))

(defn simplify
  [fullfun]
  (let [content (editor/get-content)
        [tmp namesp fun] (re-find #"(.*)/(.*)" fullfun)
        [fullmatch aliasmatch] (re-find (re-pattern (str "\\[" namesp " :as ([^]]*)\\]")) content)
        refermatch (re-find (re-pattern (str "\\[" namesp " :refer :all\\]")) content)]
    (cond (= namesp "clojure.core") fun
          aliasmatch (str aliasmatch "/" fun)
          refermatch fun
          :else fullfun)))

(defn function-typeahead
  []
  (editor/previous-buffer)
  (let [funcs (apply concat (for [n (all-ns)] (for [f (keys (ns-publics n))] (str n "/" f))))]
    (typeaheadapp/run funcs str #(editor/insert (simplify %)))))

(defn activate
  [functions]
  (let [filesbelow (fn [path] (filter #(not (re-find #"\.(png|class|jpg|pdf|git/)" %)) (filter cs/file? (cs/lsr path))))]
    (concat
      (map #(vector :buffer %) (filter #(not= "commandmode" %) (editor/buffer-names)))
      (map #(vector :command %) (editor/setting ::editor/commands))
      (map #(vector :interactive %) (editor/setting ::editor/interactive))
      (map #(vector :snippet %) (editor/setting ::editor/snippets))
      (map #(vector :snippet %) functions)
      (map #(vector :file %) (editor/setting ::editor/files))
      (map #(vector :file %) (apply concat (map filesbelow (editor/get-searchpaths)))))))

(defn tostring
  [f]
  (let [label (cond (string? (second f)) (second f)
                        (vector? (second f)) (-> f second first)
                        :else (str (second f)))]
    (str/trim (str (first f) " " (str/replace label #"\n" "\\\\n") "\n"))))

(defn run
  []
  (when (and (= (first (editor/buffer-names)) "-prompt-") (> (count (editor/get-windows)) 1))
    (editor/other-window))
  (let [functions (list)]; (editor/get-available-functions)
    (typeaheadapp/run (activate (list)) tostring execute :keymappings {"C- " function-typeahead})))

