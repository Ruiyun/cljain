(ns ^{:doc "place doc string here"
      :author "ruiyun"}
  cljain.tools.timer
  (:use cljain.tools.predicate)
  (:import [java.util Timer TimerTask Date]))

(defn timer
  "Create a new java.util.Timer object."
  {:added "0.2.0"}
  ([] (Timer.))
  ([name] (Timer. name)))

(defn deamon-timer
  "Create a new java.util.Timer object with deamon option."
  {:added "0.2.0"}
  []
  (Timer. true))

(defmacro task
  "Create a java.util.TimerTask object with some code."
  {:arglists '([body*])
   :added "0.2.0"}
  [& body]
  `(proxy [TimerTask] []
     (run []
       ~@body)))

(defmulti run!
  "Execute a timer task, then return the timer user passed or new created.
  User must set one of the two options:
    :at <time>
    :delay <milliseconds>

  Optional, user can set
    :period <milliseconds>"
  {:arglists '([timer? task & options])
   :added "0.2.0"}
  (fn [a & more] (class a)))

(defn- run-task!
  "The 'run!' functions internal implement."
  {:added "0.2.0"}
  [timer task options]
  {:pre [(even? (count options))
         (check-optional options :at #(instance? Date %))
         (check-optional options :delay >= 0)
         (check-optional options :period > 0)]
   :post [(instance? Timer %)]}
  (let [{start-time :at, delay :delay, period :period} (apply array-map options)]
    (cond
      (not (nil? start-time)) (if (nil? period)
                                (.schedule timer task start-time)
                                (.schedule timer task start-time period))
      (not (nil? delay)) (if (nil? period)
                           (.schedule timer task delay)
                           (.schedule timer task delay period))
      :else (throw (IllegalArgumentException. "Run a timer task must set :at or :delay option.")))
    timer))

(defmethod run! Timer
  [timer task & options]
  (run-task! timer task options))

(defmethod run! TimerTask
  [task & options]
  (run-task! (timer) task options))

(defn cancel!
  "Terminates a timer, discarding any currently scheduled tasks."
  {:added "0.2.0"}
  [timer]
  (.cancel timer))
