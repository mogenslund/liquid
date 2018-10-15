(ns dk.salza.liq.apps.typeaheadapp
  (:require [dk.salza.liq.slider :refer :all]
            [dk.salza.liq.editor :as editor]
            [clojure.string :as str]))

(def ^:private state
  (atom {::tostringfun nil
         ::callback nil
         ::items nil
         ::filtered nil
         ::oldsearch ""
         ::search ""
         ::hit nil
         ::prompt ""
         ::selected 0}))

(defn- update-state
  []
  (let [st @state
        pat (re-pattern (str "(?i)" (str/replace (st ::search) #" " ".*")))
        filtered (take 150 (filter #(re-find pat ((st ::tostringfun)  %)) (st ::items)))
        selected (max (min (st ::selected) (- (count filtered) 1)) 0)
        hit (first (drop selected filtered))]
    (swap! state assoc ::filtered filtered ::selected selected ::hit hit)))

(defn- forward-lines
  [sl amount]
  (loop [sl1 sl n 0]
    (if (= n amount)
      sl1
      (recur (forward-line sl1) (inc n)))))

(defn- update-display
  []
  (let [st (update-state)]
    (-> (create (str (st ::prompt) (st ::search) " "))
        end
        left
        (set-mark "hl0")
        right
        (insert "\n\n")
        (insert (str/join "\n" (map (st ::tostringfun) (st ::filtered))))
        beginning
        (forward-lines (+ (st ::selected) 2))
        end-of-line
        (set-mark "selection")
        beginning-of-line
        editor/set-slider)))

(defn- update-search
  [ch]
  (swap! state update ::search #(str % ch))
  (update-display))

(defn- next-res
  []
  (swap! state update ::selected inc)
  (update-display))

(defn- prev-res
  []
  (swap! state update ::selected #(max (dec %) 0))
  (update-display))

(defn- delete-char
  []
  (when (> (count (@state ::search)) 0)
    (swap! state assoc ::search (subs (@state ::search) 0 (dec (count (@state ::search)))))
    (swap! state assoc ::selected 0)
    (update-display)))

(defn- execute
  []
  (editor/previous-real-buffer-same-window)
  (when-let [hit (@state ::hit)]
     ((@state ::callback) hit)))

(def ^:private keymap
  {:cursor-color :blue
   "C-g" editor/previous-real-buffer-same-window
   "esc" editor/previous-real-buffer-same-window
   "backspace" delete-char
   "C-k" next-res
   "down" next-res
   "\t" prev-res ; tab = C-i in terminal!
   "up" prev-res
   "C-i" prev-res
   "\n" execute
   " " #(update-search " ")
   :selfinsert update-search
   })

(defn run
  "Items is a list of items.
  tostringfun is a function that takes
  an item and represent it as a string
  for filtering and display.
  The callback takes an item as input.
  The callback will be executed with the
  result."
  [items tostringfun callback & {:keys [keymappings prompt]}]
  (swap! state assoc ::tostringfun tostringfun
                     ::callback callback
                     ::items items
                     ::filtered items
                     ::oldsearch ""
                     ::search ""
                     ::hit nil
                     ::prompt (or prompt ">> ")
                     ::selected 0)
  (editor/new-buffer "-typeaheadapp-")
  (editor/set-keymap (merge keymap keymappings))
  (update-display))
