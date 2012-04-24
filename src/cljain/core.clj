(ns ^{:doc "place doc string here"
      :author "ruiyun"}
  cljain.core
  (:use [clojure.string :only [upper-case lower-case]])
  (:require [clojure.tools.logging :as log])
  (:import [java.util Properties]
           [javax.sip SipFactory SipStack SipProvider SipListener
            Transaction ClientTransaction Dialog
            ResponseEvent IOExceptionEvent TimeoutEvent Timeout
            TransactionTerminatedEvent DialogTerminatedEvent]))

(def ^{:doc "The instance of JAIN-SIP SipFactory."
       :added "0.2.0"}
  sip-factory (doto (SipFactory/getInstance) (.setPathName "gov.nist")))

(defn legal-proxy-address?
  "Check whether the format of outbound proxy address is legal."
  {:added "0.2.0"}
  [address]
  (let [re #"^\d+\.\d+\.\d+\.\d+(:\d+)?(/(tcp|TCP|udp|UDP))?$"]
    (re-find re address)))

(defn legal-content?
  "Check the content is a string or a map with :type, :sub-type, :length and :content keys."
  {:added "0.2.0"}
  [content]
  (or (string? content)
    (and (map? content)
      (= (sort [:type :sub-type :length :content]) (sort (keys content))))))

(defn create-listener
  "place doc string here"
  {:added "0.2.0"}
  [request-evt-handler io-exception-evt-handler]
  (reify SipListener
    (processRequest [this evt]
      (log/trace "processRequest has been invoked.")
      (request-evt-handler evt))
    (processResponse [this evt]
      (log/trace "processResponse has been invoked.")
      )
    (processIOException [this evt]
      (log/trace "processIOException has been invoked.")
      (io-exception-evt-handler evt))
    (processTimeout [this evt]
      (log/trace "processTimeout has been invoked.")
      (if (= (.getTimeout evt) Timeout/TRANSACTION)
        nil
        (log/warn "cljain doesn't support none transaction timeout.")))
    (processTransactionTerminated [this evt]
      (log/trace "processTransactionTerminated has been invoked.")
      )
    (processDialogTerminated [this evt]
      (log/trace "processDialogTerminated has been invoked.")
      )))

(defn dialog?
  "Check the obj is an instance of javax.sip.Dialog"
  {:added "0.2.0"}
  [obj]
  (instance? Dialog obj))

(defn sip-stack
  "Get the SipStack object from a SipProvider object."
  {:added "0.2.0"}
  [provider]
  (.getSipStack provider))

(defn stack-name
  "Get the SipStack name from a SipProvider object."
  {:added "0.2.0"}
  [provider]
  (.getStackName (sip-stack provider)))

(defn- map-listening-point
  "Get the ip, port, and transport from a ListeningPoint object, then pack them as a map."
  {:added "0.2.0"}
  [listening-poing]
  (let [lp (bean listening-poing)]
    {:ip (:IPAddress lp), :port (:port lp), :transport (:transport lp)}))

(defn listening-point
  "Get the current bound listening ip, port and transport information."
  {:added "0.2.0"}
  ([provider] (map-listening-point (.getListeningPoint provider)))
  ([provider transport] (map-listening-point (.getListeningPoint provider transport))))

(defn sip-provider!
  "Create a new SipProvider with stack name, listening point information and other optional SipStack properties.
  To set standard SipStack properties, use the property's lowcase short name as keyword.
  If want to set the nist define property, let property keys lead with 'nist'.

  (sip-provider \"cool-phone\" \"192.168.1.2\" 5060 \"UDP\" :outbound-proxy \"192.168.1.128\")

  More SipStack properties document can be found here:
  http://hudson.jboss.org/hudson/job/jain-sip/lastSuccessfulBuild/artifact/javadoc/index.html
  and
  http://hudson.jboss.org/hudson/job/jain-sip/lastSuccessfulBuild/artifact/javadoc/index.html"
  {:added "0.2.0"}
  [stack-name ip port transport & more-stack-properties]
  (let [props (Properties.)
        more-props (apply hash-map more-stack-properties)
        _ (prn more-props)]
    (.setProperty props "javax.sip.STACK_NAME" stack-name)
    (doseq [prop more-props]
      (let [prop-name (upper-case (.replace (name (first prop)) \- \_))
            prop-name (if (.startsWith prop-name "nist")
                        (str "gov.nist.javax.sip." prop-name)
                        (str "javax.sip." prop-name))]
        (.setProperty props prop-name (second prop))))
    (let [sip-stack (.createSipStack sip-factory props)
          listening-point (.createListeningPoint sip-stack ip port transport)]
      (.createSipProvider sip-stack listening-point))))

(defn add-listener!
  "place doc string here"
  {:added "0.2.0"}
  [provider request-handler io-exception-handler]
  (.addSipListener provider (create-listener request-handler io-exception-handler)))

(defn start!
  "Start to run the stack which bound with a provider."
  {:added "0.2.0"}
  [provider]
  (.start (sip-stack provider)))

(defn stop!
  "Stop the stack wich bound with a provider."
  {:added "0.2.0"}
  [provider]
  (.stop (sip-stack provider)))

(defn gen-call-id-header
  "Generate a new Call-ID header use a provider."
  {:added "0.2.0"}
  [provider]
  (.getNewCallId provider))

(defn send-request!
  "place doc string here"
  {:added "0.2.0"}
  [provider request]
  (.sendRequest provider request))