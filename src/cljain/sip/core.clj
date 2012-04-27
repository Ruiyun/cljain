(ns ^{:doc "place doc string here"
      :author "ruiyun"}
  cljain.sip.core
  (:use     cljain.util
            [clojure.string :only [upper-case lower-case capitalize split]])
  (:require [clojure.tools.logging :as log])
  (:import  [java.util Properties]
            [javax.sip SipFactory SipStack SipProvider SipListener
             Transaction ClientTransaction Dialog
             ResponseEvent IOExceptionEvent TimeoutEvent Timeout
             TransactionTerminatedEvent DialogTerminatedEvent]))

(def ^{:doc "The instance of JAIN-SIP SipFactory."
       :added "0.2.0"}
  sip-factory (doto (SipFactory/getInstance) (.setPathName "gov.nist")))

(def ^{:doc "Before call any function expect 'sip-provider!' in the cljain.sip.core namespace,
            please binding *sip-provider* with the current provider object first."
       :added "0.2.0"
       :dynamic true}
  *sip-provider*)

(def ^{:doc "Set by 'global-start!'"
       :added "0.2.0"
       :private true}
  global-sip-provider (atom nil))

(defn global-bind-sip-provider!
  "Bind the sip-provider in global scope."
  {:added "0.2.0"}
  [provider]
  (reset! global-sip-provider provider))

(defn global-unbind-sip-provider!
  "Unbind the sip-provider in global scope."
  {:added "0.2.0"}
  []
  (reset! global-sip-provider nil))

(defn already-bound-provider?
  "Check whether the *sip-provider* has been bound in current thread."
  {:added "0.2.0"}
  []
  (and (bound? #'*sip-provider*) (instance? SipProvider *sip-provider*)))

(defn provider-can-be-found?
  "Check where the *sip-provider* has been bound or global sip-provider has been set."
  {:added "0.2.0"}
  []
  (or (already-bound-provider?) (not (nil? @global-sip-provider))))

(defn sip-provider
  "Get the current bound *sip-provider* or global-sip-provider."
  {:added "0.2.0"}
  []
  (if (already-bound-provider?)
    *sip-provider*
    (or @global-sip-provider (throw (RuntimeException. "The 'cljain.sip.core/*sip-provider*' should be bound.")))))

(defn sip-stack
  "Get the SipStack object from a SipProvider object."
  {:added "0.2.0"}
  []
  (.getSipStack (sip-provider)))

(defn stack-name
  "Get the SipStack name from a SipProvider object."
  {:added "0.2.0"}
  []
  (.getStackName (sip-stack)))

(defn- map-listening-point
  "Get the ip, port, and transport from a ListeningPoint object, then pack them as a map."
  {:added "0.2.0"}
  [lp]
  {:ip (.getIPAddress lp) :port (.getPort lp) :transport (.getTransport lp)})

(defn listening-point
  "Get the current bound listening ip, port and transport information."
  {:added "0.2.0"}
  ([] (map-listening-point (.getListeningPoint (sip-provider))))
  ([transport] (map-listening-point (.getListeningPoint (sip-provider) transport))))

(defn sip-provider!
  "Create a new SipProvider with meaningful name, local ip, port, transport and other optional SipStack properties.
  Rember, the name must be unique to make a distinction between other provider.
  To set standard SipStack properties, use the property's lowcase short name as keyword.
  If want to set the nist define property, let property keys lead with 'nist'.

  (sip-provider \"cool-phone\" \"192.168.1.2\" 5060 \"UDP\" :outbound-proxy \"192.168.1.128\")

  More SipStack properties document can be found here:
  http://hudson.jboss.org/hudson/job/jain-sip/lastSuccessfulBuild/artifact/javadoc/index.html
  and
  http://hudson.jboss.org/hudson/job/jain-sip/lastSuccessfulBuild/artifact/javadoc/index.html"
  {:added "0.2.0"}
  [name ip port transport & properties]
  {:pre [(check-optional properties :outbound-proxy #(re-find #"^\d+\.\d+\.\d+\.\d+(:\d+)?(/(tcp|TCP|udp|UDP))?$" %))]}
  (let [more-props (apply array-map properties)
        props (Properties.)]
    (.setProperty props "javax.sip.STACK_NAME" name)
    (doseq [prop more-props]
      (let [prop-name (upper-case (.replace (name (first prop)) \- \_))
            prop-name (if (.startsWith prop-name "nist")
                        (str "gov.nist.javax.sip." prop-name)
                        (str "javax.sip." prop-name))]
        (.setProperty props prop-name (second prop))))
    (let [sip-stack (.createSipStack sip-factory props)
          listening-point (.createListeningPoint sip-stack ip port transport)]
      (.createSipProvider sip-stack listening-point))))

(defn- trans-from-event
  "Get ServerTransaction or ClientTransaction from an event object."
  {:added "0.2.0"}
  [event]
  (if (.isServerTransaction event)
    (.getServerTransaction event)
    (.getClientTransaction event)))

(defmacro trace-call
  "Log the exception from event callback."
  {:added "0.2.0"
   :private true}
  [callback event arg]
  (let [callback-name (str "process" (reduce str (map capitalize (split (str callback) #"-"))))]
    `(try
       (log/trace ~callback-name "has been invoked." (bean ~event))
       (and ~callback (~callback ~arg))
       (catch Exception e#
         (log/error "Exception occurs when calling the event callback" ~callback-name ":" e#)))))

(defn set-listener!
  "Set several event listening function to current bound provider.
  Because JAIN-SIP just allow set listener once, if call 'set-listener' more then one times,
  an exception will be thrown."
  {:added "0.2.0"}
  [& processors]
  {:pre [(even? (count processors))
         (check-optional processors :request fn?)
         (check-optional processors :response fn?)
         (check-optional processors :timeout fn?)
         (check-optional processors :io-exception fn?)
         (check-optional processors :transaction-terminated fn?)
         (check-optional processors :dialog-terminated fn?)]}
  (.addSipListener (sip-provider)
    (let [{:keys [request response timeout io-exception
                  transaction-terminated dialog-terminated]} (apply array-map processors)]
      (reify SipListener
        (processRequest [this event]
          (trace-call request event {:request (.getRequest event)
                                     :server-transaction (.getServerTransaction event)
                                     :dialog (.getDialog event)}))
        (processResponse [this event]
          (trace-call response event {:response (.getResponse event)
                                      :client-transaction (.getClientTransaction event)
                                      :dialog (.getDialog event)}))
        (processIOException [this event]
          (trace-call io-exception event {:host (.getHost event)
                                          :port (.getPort event)
                                          :transport (.getTransport event)}))
        (processTimeout [this event]
          (trace-call timeout event {:transaction (trans-from-event event)
                                     :timeout (.. event (getTimeout) (getValue))}))
        (processTransactionTerminated [this event]
          (trace-call transaction-terminated event (trans-from-event event)))
        (processDialogTerminated [this event]
          (trace-call dialog-terminated event (.getDialog event)))))))

(defn start!
  "Start to run the stack which bound with current bound provider."
  {:added "0.2.0"}
  []
  (.start (sip-stack)))

(defn stop-and-release!
  "Stop the stack wich bound with current bound provider. And release all resource associated the stack.
  Becareful, after called 'stop!' function, all other function include 'start!' will be invalid.
  A new provider need be generated for later call."
  {:added "0.2.0"}
  []
  (.stop (sip-stack)))

(defn gen-call-id-header
  "Generate a new Call-ID header use current bound provider."
  {:added "0.2.0"}
  []
  (.getNewCallId (sip-provider)))

(defn send-request!
  "Send out of dialog SipRequest use current bound provider."
  {:added "0.2.0"}
  [request]
  (.sendRequest (sip-provider) request))

(defn new-server-transaction!
  "An application has the responsibility of deciding to respond to a Request
  that does not match an existing server transaction."
  {:added "0.2.0"}
  [request]
  (.getNewServerTransaction (sip-provider) request))

(defn new-client-transcation!
  "Before an application can send a new request it must first request
  a new client transaction to handle that Request."
  {:added "0.2.0"}
  [request]
  (.getNewClientTransaction (sip-provider) request))

(defn new-dialog!
  "Create a dialog for the given transaction."
  {:added "0.2.0"}
  [transaction]
  (.getNewDialog (sip-provider) transaction))