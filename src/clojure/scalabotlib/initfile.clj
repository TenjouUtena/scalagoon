(ns scalabotlib.initfile
  (:require
    [clojure-ini.core :refer [read-ini]]
    ))

(gen-class
  :name scalabotlib.initfile
  :methods [#^{:static true} [getValue [String] String]])

(def ini (atom nil))

(defn getConfig []
  (if @ini
    @ini
    (reset! ini (try (read-ini "bot.ini")
                     (catch java.io.IOException e {})))))

(defn -getValue [val]
  "
  Get a config value
  @param key Key to get from file
  "
  (get (getConfig) val))