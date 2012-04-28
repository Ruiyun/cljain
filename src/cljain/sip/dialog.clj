(ns ^{:doc "place doc string here"
      :author "ruiyun"}
  cljain.sip.dialog
  (:require [clojure.tools.logging :as log]
            [cljain.sip.transaction :as trans])
  (:import  [javax.sip Dialog]))

(defn dialog?
  "Check the obj is an instance of javax.sip.Dialog"
  {:added "0.2.0"}
  [object]
  (instance? Dialog object))

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
  (log/trace "cljain.sip.dialog/send-request!" \newline "dialog:" dialog \newline "transaction:" transaction \newline
    "request" (trans/request transaction))
  (.sendRequest dialog transaction))

(defn ack
  "Creates an ACK request for an Invite that was responded with 2xx response.
  The cseq number for the invite is supplied to relate the ACK to its original invite request."
  {:added "0.2.0"}
  [dialog seq-num]
  (try
    (.createAck dialog seq-num)))

(defn send-ack!
  "Sends ACK Request to the remote party of this dialog.
  This method implies that the application is functioning as User Agent Client
  hence the underlying SipProvider acts statefully.
  This method does not increment the local sequence number."
  {:added "0.2.0"}
  [dialog ack]
  (log/trace "cljain.sip.dialog/send-ack!" \newline "dialog:" dialog "ack:" ack)
  (.sendAck dialog ack))
