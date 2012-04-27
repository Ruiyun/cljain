(ns ^{:doc "place doc string here"
      :author "ruiyun"}
  cljain.sip.transaction
  (:import [javax.sip Transaction ClientTransaction ServerTransaction]))

(defn send-request!
  "Sends the Request which created this ClientTransaction."
  {:added "0.2.0"}
  [client-transaction]
  (.sendRequest client-transaction))

(defn send-response!
  "Sends the Response to a Request which is associated with this ServerTransaction.
  When an application wishes to send a Response, it creates a Response using the
  MessageFactory and then passes that Response to this method. The Response message
  gets sent out on the network via the ListeningPoint information that is associated
  with the SipProvider of this ServerTransaction.

  This method implies that the application is functioning as either a UAS or
  a stateful proxy, hence the underlying implementation acts statefully.
  When a UAS sends a 2xx response to an INVITE, the server transaction is
  transitions to the TerminatedState. The implementation may delay physically
  removing ServerTransaction record from memory to catch retransmissions of the INVITE
  in accordance with the reccomendation of http://bugs.sipit.net/show_bug.cgi?id=769 ."
  {:added "0.2.0"}
  [server-transaction response]
  (.sendResponse server-transaction response))

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

(defn branch-id
  "Returns a unique branch identifer that identifies this transaction.
  The branch identifier is used in the ViaHeader. The uniqueness property of the
  branch ID parameter to facilitate its use as a transaction ID, was not part of RFC 2543.
  The branch ID inserted by an element compliant with the RFC3261 specification MUST always
  begin with the characters \"z9hG4bK\". These 7 characters are used as a magic cookie,
  so that servers receiving the request can determine that the branch ID was constructed
  to be globally unique. The precise format of the branch token is implementation-defined.
  This method should always return the same branch identifier for the same transaction."
  {:added "VERSION"}
  [transaction]
  (.getBranchId transaction))

(defn request
  "Returns the request that created this transaction.
  The transaction state machine needs to keep the Request that resulted in the creation
  of this transaction while the transaction is still alive. Applications also need to access
  this information, e.g. a forking proxy server may wish to retrieve the original Invite
  request to cancel branches of a fork when a final Response has been received by one branch."
  {:added "0.2.0"}
  [transaction]
  (.getRequest transaction))
