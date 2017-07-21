(ns dk.salza.liq.logging
  (:require [clojure.string :as str]))

(def logfile (ref nil))

(defn enable
  [logfilepath]
  (dosync (ref-set logfile logfilepath)
    (spit @logfile "")))

(defn log
  [& entries]
  (when @logfile
    (dosync
      (spit @logfile
        (str (.format
               (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm:ss ")
               (new java.util.Date))
          (str/join " " entries) "\n")
        :append true))))
