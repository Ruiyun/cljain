(ns ^{:doc "place doc string here"
      :author "ruiyun"}
  cljain.dialog
  (:import [javax.sip Dialog]))

(defn dialog?
  "Check the obj is an instance of javax.sip.Dialog"
  {:added "0.2.0"}
  [obj]
  (instance? Dialog obj))
