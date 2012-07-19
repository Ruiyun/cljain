(ns ^{:author "ruiyun"
      :added "0.2.0"
      :deprecated "0.4.0"}
  cljain.sip.dialog
  (:require [clojure.tools.logging :as log]
            [cljain.sip.transaction :as trans])
  (:import  [javax.sip Dialog]))

(defn dialog?
  "DEPRECATED: Use 'cljain.core/dialog?' instead.
  Check the obj is an instance of javax.sip.Dialog"
  {:added "0.2.0"
   :deprecated "0.4.0"}
  [object]
  (instance? Dialog object))

(defn application-data
  "DEPRECATED: Use Java method 'getApplicationData' directly instead.
  Gets the application specific data specific to this dialog."
  {:added "0.2.0"
   :deprecated "0.4.0"}
  [dialog]
  (.getApplicationData dialog))

(defn set-application-data
  "DEPRECATED: Use Java method 'setApplicationData' directly instead.
  Sets application specific data to this dialog."
  {:added "0.2.0"
   :deprecated "0.4.0"}
  [dialog data]
  (.setApplicationData dialog data))

(defn create-request
  "DEPRECATED: Use Java method 'createRequest' directly instead.
  Creates a new Request message based on the dialog creating request."
  {:added "0.2.0"
   :deprecated "0.4.0"}
  [dialog method]
  (.createRequest dialog method))

(defn send-request!
  "DEPRECATED: Use Java method 'sendRequest' directly instead.
  Sends a Request to the remote party of this dialog."
  {:added "0.2.0"
   :deprecated "0.4.0"}
  [dialog transaction]
  (log/trace "cljain.sip.dialog/send-request!" \newline "dialog:" dialog \newline "transaction:" transaction \newline
    "request" (trans/request transaction))
  (.sendRequest dialog transaction)
  dialog)

(defn ack
  "DEPRECATED: Use Java method 'createAck' directly instead.
  Creates an ACK request for an Invite that was responded with 2xx response.
  The cseq number for the invite is supplied to relate the ACK to its original invite request."
  {:added "0.2.0"
   :deprecated "0.4.0"}
  [dialog seq-num]
  (try
    (.createAck dialog seq-num)))

(defn send-ack!
  "DEPRECATED: Use Java method 'sendAck' directly instead.
  Sends ACK Request to the remote party of this dialog.
  This method implies that the application is functioning as User Agent Client
  hence the underlying SipProvider acts statefully.
  This method does not increment the local sequence number."
  {:added "0.2.0"
   :deprecated "0.4.0"}
  [dialog ack]
  (log/trace "cljain.sip.dialog/send-ack!" \newline "dialog:" dialog "ack:" ack)
  (.sendAck dialog ack)
  dialog)
