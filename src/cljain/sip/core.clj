(ns cljain.sip.core
  (:use [clojure.string :only [upper-case lower-case capitalize split]])
  (:require [clojure.tools.logging :as log])
  (:import [java.util Properties]
           [javax.sip SipFactory SipStack SipProvider SipListener ListeningPoint Transaction ClientTransaction ServerTransaction Dialog
            ResponseEvent IOExceptionEvent TimeoutEvent Timeout TransactionTerminatedEvent DialogTerminatedEvent]))

(def ^{:doc "The instance of JAIN-SIP SipFactory."
       :tag SipFactory}
  sip-factory (doto (SipFactory/getInstance) (.setPathName "gov.nist")))

(def ^{:doc "Before call any function expect 'sip-provider!' in the cljain.sip.core namespace,
            please binding *sip-provider* with the current provider object first."
       :tag SipProvider
       :dynamic true}
  *sip-provider*)

(def ^{:doc "Set by 'global-start!'"
       :tag SipProvider
       :private true}
  global-sip-provider (atom nil))

(defn global-bind-sip-provider!
  "Bind the sip-provider in global scope."
  [provider]
  (reset! global-sip-provider provider))

(defn global-unbind-sip-provider!
  "Unbind the sip-provider in global scope."
  []
  (reset! global-sip-provider nil))

(defn already-bound-provider?
  "Check whether the *sip-provider* has been bound in current thread."
  []
  (and (bound? #'*sip-provider*) (instance? SipProvider *sip-provider*)))

(defn provider-can-be-found?
  "Check where the *sip-provider* has been bound or global sip-provider has been set."
  []
  (or (already-bound-provider?) (not (nil? @global-sip-provider))))

(defn ^SipProvider sip-provider
  "Get the current bound *sip-provider* or global-sip-provider."
  []
  (if (already-bound-provider?)
    *sip-provider*
    (or @global-sip-provider (throw (RuntimeException. "The 'cljain.sip.core/*sip-provider*' should be bound.")))))

(defn ^SipStack sip-stack
  "Get the SipStack object from a SipProvider object."
  []
  (.getSipStack (sip-provider)))

(defn stack-name
  "Get the SipStack name from a SipProvider object."
  []
  (.getStackName (sip-stack)))

(defn- map-listening-point
  "Get the ip, port, and transport from a ListeningPoint object, then pack them as a map."
  [^ListeningPoint lp]
  {:ip (.getIPAddress lp) :port (.getPort lp) :transport (.getTransport lp)})

(defn ^ListeningPoint listening-point
  "Get the current bound listening ip, port and transport information."
  ([] (map-listening-point (.getListeningPoint (sip-provider))))
  ([transport] (map-listening-point (.getListeningPoint (sip-provider) transport))))

(defn ^SipProvider sip-provider!
  "Create a new SipProvider with meaningful name, local ip, port, transport and other optional SipStack properties.
  Rember, the name must be unique to make a distinction between other provider.
  To set standard SipStack properties, use the property's lowcase short name as keyword.
  If want to set the nist define property, let property keys lead with 'nist'.

  (sip-provider! \"cool-phone\" \"192.168.1.2\" 5060 \"UDP\" :outbound-proxy \"192.168.1.128\")

  More SipStack properties document can be found here:
  http://hudson.jboss.org/hudson/job/jain-sip/lastSuccessfulBuild/artifact/javadoc/index.html
  and
  http://hudson.jboss.org/hudson/job/jain-sip/lastSuccessfulBuild/artifact/javadoc/index.html"
  [name ip port transport & properties]
  {:pre [(or (nil? (:outbound-proxy properties))
           (re-find #"^\d+\.\d+\.\d+\.\d+(:\d+)?(/(tcp|TCP|udp|UDP))?$" (:outbound-proxy properties)))]}
  (let [more-props (apply array-map properties)
        props (Properties.)]
    (.setProperty props "javax.sip.STACK_NAME" name)
    (doseq [prop more-props]
      (let [prop-name (upper-case (.replace (clojure.core/name (first prop)) \- \_))
            prop-name (if (.startsWith prop-name "nist")
                        (str "gov.nist.javax.sip." prop-name)
                        (str "javax.sip." prop-name))]
        (.setProperty props prop-name (second prop))))
    (let [sip-stack (.createSipStack sip-factory props)
          listening-point (.createListeningPoint sip-stack ip port transport)]
      (.createSipProvider sip-stack listening-point))))

(defprotocol EventTrans
  (trans [this]))

(extend-protocol EventTrans
  TimeoutEvent
  (trans [this]
    (if (.isServerTransaction this)
      (.getServerTransaction this)
      (.getClientTransaction this)))
  TransactionTerminatedEvent
  (trans [this]
    (if (.isServerTransaction this)
      (.getServerTransaction this)
      (.getClientTransaction this))))

(defmacro ^:private trace-call
  "Log the exception from event callback."
  [callback event & arg]
  (let [callback-name (str "process" (reduce str (map capitalize (split (str callback) #"-"))))]
    `(try
       (log/trace ~callback-name "has been invoked." \newline "event:" (bean ~event))
       (and ~callback (~callback ~@arg))
       (catch Exception e#
         (log/error "Exception occurs when calling the event callback" ~callback-name ":" e#)))))

(defn set-listener!
  "Set several event listening function to current bound provider.
  Because JAIN-SIP just allow set listener once, if call 'set-listener' more then one times,
  an exception will be thrown."
  [{:keys [request response timeout io-exception transaction-terminated dialog-terminated]}]
  {:pre [(or (nil? request) (fn? request))
         (or (nil? response) (fn? response))
         (or (nil? timeout) (fn? timeout))
         (or (nil? io-exception) (fn? io-exception))
         (or (nil? transaction-terminated) (fn? transaction-terminated))
         (or (nil? dialog-terminated) (fn? dialog-terminated))]}
  (.addSipListener (sip-provider)
    (reify SipListener
      (processRequest [this event]
        (trace-call request event (.getRequest event) (.getServerTransaction event) (.getDialog event)))
      (processResponse [this event]
        (trace-call response event (.getResponse event) (.getClientTransaction event) (.getDialog event)))
      (processIOException [this event]
        (trace-call io-exception event (.getHost event) (.getPort event) (.getTransport event)))
      (processTimeout [this event]
        (trace-call timeout event (trans event) (.. event (getTimeout) (getValue))))
      (processTransactionTerminated [this event]
        (trace-call transaction-terminated event (trans event)))
      (processDialogTerminated [this event]
        (trace-call dialog-terminated event (.getDialog event))))))

(defn start!
  "Start to run the stack which bound with current bound provider."
  []
  (.start (sip-stack)))

(defn stop-and-release!
  "Stop the stack wich bound with current bound provider. And release all resource associated the stack.
  Becareful, after called 'stop!' function, all other function include 'start!' will be invalid.
  A new provider need be generated for later call."
  []
  (.stop (sip-stack)))

(defn gen-call-id-header
  "Generate a new Call-ID header use current bound provider."
  []
  (.getNewCallId (sip-provider)))

(defn send-request!
  "Send out of dialog SipRequest use current bound provider."
  [request]
  (.sendRequest (sip-provider) request))

(defn ^ServerTransaction new-server-transaction!
  "An application has the responsibility of deciding to respond to a Request
  that does not match an existing server transaction."
  [request]
  (.getNewServerTransaction (sip-provider) request))

(defn ^ClientTransaction new-client-transcation!
  "Before an application can send a new request it must first request
  a new client transaction to handle that Request."
  [request]
  (.getNewClientTransaction (sip-provider) request))

(defn transaction?
  "Check the obj is an instance of javax.sip.Transaction.
  Both ClientTransaction and ServerTransaction are pass."
  [object]
  (instance? Transaction object))

(defn new-dialog!
  "Create a dialog for the given transaction."
  [transaction]
  (.getNewDialog (sip-provider) transaction))

(defn dialog?
  "Check the obj is an instance of javax.sip.Dialog"
  [object]
  (instance? Dialog object))
