(ns ^{:doc "place doc string here"
      :author "ruiyun"}
  cljain.sip.transaction
  (:import [javax.sip Transaction ClientTransaction ServerTransaction]))

(defn send-request!
  "Sends the Request which created this ClientTransaction."
  {:added "0.2.0"}
  [client-transaction]
  (.sendRequest client-transaction))

(defn application-data
  "Returns the application data associated with the transaction.
  This specification does not define the format of this application specific data."
  {:added "0.2.0"}
  [transaction]
  (.getApplicationData transaction))

(defn set-application-data!
  "This method allows applications to associate application context with the transaction."
  {:added "0.2.0"}
  [transaction data]
  (.setApplicationData transaction data))
