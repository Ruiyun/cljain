(ns ^{:doc "place doc string here"
      :author "ruiyun"}
  cljain.sip.dialog
  (:import [javax.sip Dialog]))

(defn dialog?
  "Check the obj is an instance of javax.sip.Dialog"
  {:added "0.2.0"}
  [obj]
  (instance? Dialog obj))

(defn application-data
  "Gets the application specific data specific to this dialog."
  {:added "0.2.0"}
  [dialog]
  (.getApplicationData dialog))

(defn set-application-data
  "Sets application specific data to this dialog."
  {:added "0.2.0"}
  [dialog data]
  (.setApplicationData dialog data))

(defn create-request
  "Creates a new Request message based on the dialog creating request."
  {:added "0.2.0"}
  [dialog method]
  (.createRequest dialog method))

(defn send-request!
  "Sends a Request to the remote party of this dialog."
  {:added "0.2.0"}
  [dialog transaction]
  (.sendRequest dialog transaction))
