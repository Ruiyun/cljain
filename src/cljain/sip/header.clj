(ns cljain.sip.header
  (:refer-clojure :exclude [replace reverse require])
  (:require [clojure.string :refer [replace upper-case capitalize]]
            [cljain.sip.core :refer [sip-factory]])
  (:import [javax.sip SipFactory]
           [javax.sip.header HeaderFactory HeaderAddress Header CSeqHeader WWWAuthenticateHeader]
           [javax.sip.address Address URI]
           [gov.nist.javax.sip Utils]))

(def ^{:doc "The factory to create sip headers."
       :tag HeaderFactory
       :private true}
  factory (.createHeaderFactory ^SipFactory sip-factory))

(defn gen-tag
  "Generate a new tag string."
  []
  (.. Utils (getInstance) (generateTag)))

(defn gen-branch
  "Generate a new branch id string."
  []
  (.. Utils (getInstance) (generateBranchId)))

(defmacro defheader
  "Use the macro to define sip headers. More document could be found here
  http://hudson.jboss.org/hudson/job/jain-sip/lastSuccessfulBuild/artifact/javadoc/index.html"
  {:arglists '([name [args*]])
   :added "0.2.0"}
  [name args]
  (let [cls-name  (replace (capitalize name) #"-[a-z]" #(upper-case (subs %1 1)))
        cls-name  (str cls-name "Header")
        name      (symbol name)
        method    (symbol (str "create" cls-name))
        m         {:doc (str "Create a new " cls-name), :arglists (list 'quote (list args))}]
    `(def ~(with-meta name m) (partial (memfn ~method ~@args) factory))))

(defheader accept [^String content-type, ^String sub-type])
(defheader accept-encoding [^String encoding])
(defheader accept-language [^java.util.Locale language])
(defheader alert-info [^URI info])
(defheader allow [^String method])
(defheader allow-events [^String event-type])
(defheader authentication-info [^String response])
(defheader authorization [^String scheme])
(defheader c-seq [^Long number, ^String method])
(defheader call-id [^String id])
(defheader call-info [^URI info])
(defheader contact [^Address address])
(defheader content-disposition [^String disposition-type])
(defheader content-encoding [^String encoding])
(defheader content-language [^java.util.Locale language])
(defheader content-length [^Integer length])
(defheader content-type [^String type, ^String sub-type])
(defheader date [^java.util.Calendar date])
(defheader error-info [^URI info])
(defheader event [^String type])
(defheader expires [^Integer seconds])
(defheader from [^Address address, ^String tag])
(defheader in-reply-to [^String call-id])
(defheader max-forwards [^Integer number])
(defheader mime-version [^Integer major, ^Integer minor])
(defheader min-expires [^Integer seconds])
(defheader organization [^String value])
(defheader priority [^String value])
(defheader proxy-authenticate [^String scheme])
(defheader proxy-authorization [^String scheme])
(defheader proxy-require [^String option-tag])
(defheader r-ack [^Integer r-seq, ^Integer c-seq, ^String method])
(defheader r-seq [^Integer number])
(defheader reason [^String protocol ^Integer, cause ^String text])
(defheader record-route [^Address address])
(defheader refer-to [^Address address])
(defheader reply-to [^Address address])
(defheader require [^String option-tag])
(defheader retry-after [^Integer seconds])
(defheader route [^Address address])
(defheader server [^java.util.List product])
(defheader subject [^String subject])
(defheader subscription-state [^String state])
(defheader supported [^String option-tag])
(defheader time-stamp [^Float time])
(defheader to [^Address address, ^String tag])
(defheader unsupported [^String option-tag])
(defheader user-agent [^java.util.List product])
(defheader via [^String host, ^Integer port, ^String transport, ^String branch])
(defheader warning [^String agent, ^Integer code, ^String comment]) ; 3DIGIT code between 99 and 1000

(defn sip-etag
  "Creates a new SIP-ETag header with the supplied tag value"
  [^String etag]
  (.createSIPETagHeader factory etag))

(defn sip-if-match
  "Creates a new SIP-If-Match header with the supplied tag value"
  [^String etag]
  (.createSIPIfMatchHeader factory etag))

(defn wildcard-contact
  "Creates a new wildcard ContactHeader.
  This is used in Register requests to indicate to the server that it should remove all locations the at which
  the user is currently available. This implies that the following conditions are met:

  ContactHeader.getAddress.getUserInfo() == *;
  ContactHeader.getAddress.isWildCard() == true;
  ContactHeader.getExpires() == 0;"
  []
  (.createContactHeader factory))

(defn www-authenticate
  "Creates a new WWWAuthenticateHeader based on the newly supplied scheme value."
  [^String scheme, ^String realm, ^String nonce & {:keys [algorithm, qop opaque domain stale]}]
  (doto (.createWWWAuthenticateHeader factory scheme)
    (.setRealm realm)
    (.setNonce nonce)
    (#(when algorithm (.setAlgorithm % algorithm)))
    (#(when qop (.setQop % qop)))
    (#(when opaque (.setOpaque % opaque)))
    (#(when domain (.setDomain % domain)))
    (#(when stale (.setStale % stale)))))

(defn extension
  "Creates a new Header based on the newly supplied name and value values."
  [name value]
  (.createHeader factory name (str value)))


(defn header?
  "Check whether the object is a Header or not"
  [object]
  (instance? Header object))
