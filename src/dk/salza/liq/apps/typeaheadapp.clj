(ns dk.salza.liq.apps.typeaheadapp
  "A general app for doing typeahead
  It needs a list to choose from and a to-string
  function to display and filter the list."
  (:require [dk.salza.liq.editor :as editor]
            [dk.salza.liq.keys :as keys]
            [dk.salza.liq.coreutil :refer :all]
            [clojure.string :as str]))

(def state (atom {::tostringfun nil
                  ::callback nil
                  ::items nil
                  ::filtered nil
                  ::oldsearch ""
                  ::search ""
                  ::hit nil
                  ::selected 0}))

(defn update-display
  []
  (editor/clear)
  (editor/insert "\n\n")
  (let [to-string (@state ::tostringfun)
        pat (re-pattern (str "(?i)" (str/replace (@state ::search) #" " ".*")))
        update (> (count (@state ::oldsearch)) (count (@state ::search)))
        res (if update (@state ::items) (@state ::filtered))
        filtered (filter #(re-find pat (to-string %)) res)
        index (@state ::selected)
        hit (when (< index (count filtered)) (nth filtered (@state ::selected)))]
    (swap! state assoc ::hit hit ::filtered filtered ::oldsearch (@state ::search))
    (doseq [e (take 100 filtered)]
      (editor/insert (str (if (= e hit) "#>" "  ") "  " (to-string e) "\n")))
    (editor/beginning-of-buffer)
    (editor/insert (str ">> " "" (@state ::search)))
    (editor/end-of-line)))

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
  (editor/previous-real-buffer)
  (when-let [hit (@state ::hit)]
     ((@state ::callback) hit)))

(defn update-search
  [ch]
  (swap! state update ::search #(str % ch))
  (swap! state assoc ::selected 0)
  (update-display))

(def keymap
  (merge
    {:cursor-color :green
     :C-g editor/previous-buffer
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
  "Items is a list of items.
  tostringfun is a function that takes
  an item and represent it as a string
  for filtering and display.
  The callback takes an item as input.
  The callback will be executed with the
  result."
  [items tostringfun callback]
  (swap! state assoc ::tostringfun tostringfun
                     ::callback callback
                     ::items items
                     ::filtered items
                     ::oldsearch ""
                     ::search ""
                     ::hit nil
                     ::selected 0)
  (editor/new-buffer "-typeaheadapp-")
  (editor/set-keymap keymap)
  (update-display))