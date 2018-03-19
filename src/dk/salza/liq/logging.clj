(ns dk.salza.liq.logging
  (:require [clojure.string :as str]))

(def logfile (atom nil))

(defn enable
  [logfilepath]
  (reset! logfile logfilepath)
    (spit @logfile ""))

(defn log
  [& entries]
  (when @logfile
    (spit @logfile
      (str (.format
             (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm:ss ")
             (new java.util.Date))
        (str/join " " (map eval entries)) "\n")
      :append true)))