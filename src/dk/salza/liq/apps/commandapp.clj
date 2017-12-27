(ns dk.salza.liq.apps.commandapp
  (:require [dk.salza.liq.editor :as editor]
            [dk.salza.liq.keys :as keys]
            [dk.salza.liq.tools.cshell :as cs]
            [dk.salza.liq.apps.promptapp :as promptapp]
            [dk.salza.liq.apps.textapp :as textapp]
            [dk.salza.liq.apps.typeaheadapp :as typeaheadapp]
            [dk.salza.liq.coreutil :refer :all]
            [clojure.string :as str]))


(def state (atom {::everything nil
                  ::filtered nil
                  ::oldsearch ""
                  ::search ""
                  ::hit nil
                  ::selected 0}))

(defn update-display
  []
  (editor/clear)
  (editor/insert "\n\n")
  (let [escaped (-> (@state ::search)
                    (str/replace  #"\(" "\\\\(")
                    (str/replace  #"\n" "\\\\n"))
        pat (re-pattern (str "(?i)" (str/replace escaped #" " ".*")))
        update (> (count (@state ::oldsearch)) (count (@state ::search)))
        res (if update (@state ::everything) (@state ::filtered))
        filtered (filter #(re-find pat (str (second %))) res)
        index (@state ::selected)
        hit (when (< index (count filtered)) (nth filtered (@state ::selected)))]
    (swap! state assoc ::hit hit ::filtered filtered ::oldsearch (@state ::search))
    (doseq [e (take 100 filtered)]
      (let [pre (if (= (str e) (str hit)) "#>  " "    ")
            label (cond (string? (second e)) (second e)
                        (vector? (second e)) (-> e second first)
                        :else (str (second e)))]
        (editor/insert (str pre (first e) " " (str/replace label #"\n" "\\\\n") "\n"))))
    (editor/beginning-of-buffer)
    (editor/insert (str ">> " "" (@state ::search))))
    (editor/end-of-line))

(defn delete-char
  []
  (when (> (count (@state ::search)) 0)
    (swap! state assoc ::search (subs (@state ::search) 0 (dec (count (@state ::search)))))
    (swap! state assoc ::selected 0)
    (update-display)))

(defn next-res
  []
  (swap! state update ::selected inc)
  (update-display))

(defn prev-res
  []
  (swap! state update ::selected #(max (dec %) 0))
  (update-display))

(defn execute
  []
  (if-let [hit (@state ::hit)]
    (cond (= (first hit) :buffer) (editor/switch-to-buffer (second hit))
          (= (first hit) :snippet) (do (editor/previous-buffer) (editor/insert (second hit)))
          (= (first hit) :file)   (textapp/run (second hit)) ;(editor/find-file (second hit))
          (= (first hit) :command) (do (editor/previous-buffer) (editor/eval-safe (second hit)))
          (= (first hit) :interactive) (let [fun (second (second hit))
                                             params (nth (second hit) 2)]
                                         (editor/previous-buffer)
                                         (if params
                                           (promptapp/run fun params)
                                           (editor/eval-safe fun))))
                                         ;(nth (second hit) 2))))
          ;(= (first hit) :folder) (ed/find-file (second hit)))
    (editor/previous-buffer)))

(defn simplify
  [fullfun]
  (let [content (editor/get-content)
        [tmp namesp fun] (re-find #"(.*)/(.*)" fullfun)
        [fullmatch aliasmatch] (re-find (re-pattern (str "\\[" namesp " :as ([^]]*)\\]")) content)]
    (cond (= namesp "clojure.core") fun
          aliasmatch (str aliasmatch "/" fun)
          :else fullfun)))

(defn function-typeahead
  []
  (editor/previous-buffer)
  (let [funcs (apply concat (for [n (all-ns)] (for [f (keys (ns-publics n))] (str n "/" f))))]
    (typeaheadapp/run funcs str #(editor/insert (simplify %)))))

(defn update-search
  [ch]
  (swap! state update ::search #(str % ch))
  (swap! state assoc ::selected 0)
  (update-display))

(defn activate
  [functions]
  (let [filesbelow (fn [path] (filter #(not (re-find #"\.(png|class|jpg|pdf|git/)" %)) (filter cs/file? (cs/lsr path))))
        everything 
        (concat
          (map #(vector :buffer %) (filter #(not= "commandmode" %) (editor/buffer-names)))
          (map #(vector :command %) (editor/setting ::editor/commands))
          (map #(vector :interactive %) (editor/setting ::editor/interactive))
          (map #(vector :snippet %) (editor/setting ::editor/snippets))
          (map #(vector :snippet %) functions)
          (map #(vector :file %) (editor/setting ::editor/files))
          (map #(vector :file %) (apply concat (map filesbelow (editor/setting ::editor/searchpaths)))))]
    (swap! state assoc ::previous (editor/get-name))
    (swap! state assoc ::search "")
    (swap! state assoc ::oldsearch "")
    (swap! state assoc ::selected 0)
    (swap! state assoc ::everything everything)
    (swap! state assoc ::filtered everything))
  (update-display))

(def keymap
  (merge
    {:cursor-color :green
     :C-g editor/previous-buffer
     :C-space function-typeahead
     :esc editor/previous-buffer
     :backspace delete-char
     :C-k next-res
     :down next-res
     :tab prev-res ; tab = C-i in termainal!
     :up prev-res
     :enter execute
     :space #(update-search " ")
    }
    (keys/alphanum-mapping update-search)
    (keys/symbols-mapping update-search)))

(defn run
  []
  (let [functions (list); (editor/get-available-functions)
      ]
    (editor/new-buffer "-commandapp-")
    (editor/set-keymap keymap)
    (activate functions)))