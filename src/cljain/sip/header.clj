(ns cljain.sip.header
  (:refer-clojure :exclude [replace reverse require])
  (:require [clojure.string :refer [replace upper-case capitalize]]
            [cljain.sip.core :refer [sip-factory]])
  (:import [javax.sip SipFactory]
           [javax.sip.header HeaderFactory HeaderAddress Header
            AcceptHeader AcceptEncodingHeader AcceptLanguageHeader AlertInfoHeader AllowHeader AllowEventsHeader
            AuthenticationInfoHeader AuthorizationHeader CallIdHeader CallInfoHeader ContactHeader ContentDispositionHeader
            ContentEncodingHeader ContentLanguageHeader ContentLengthHeader ContentTypeHeader CSeqHeader DateHeader
            ErrorInfoHeader EventHeader ExpiresHeader FromHeader InReplyToHeader MaxForwardsHeader MimeVersionHeader MinExpiresHeader
            OrganizationHeader PriorityHeader ProxyAuthenticateHeader ProxyAuthorizationHeader ProxyRequireHeader RAckHeader
            ReasonHeader RecordRouteHeader ReferToHeader ReplyToHeader RequireHeader RetryAfterHeader RouteHeader RSeqHeader
            ServerHeader SIPETagHeader SIPIfMatchHeader SubjectHeader SubscriptionStateHeader SupportedHeader TimeStampHeader
            ToHeader UnsupportedHeader UserAgentHeader ViaHeader WarningHeader WWWAuthenticateHeader]
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
  {:arglists '([type name [args*]])
   :added "0.2.0"}
  [name args]
  (let [cls-name  (replace (capitalize name) #"-[a-z]" #(upper-case (subs %1 1)))
        cls-name  (str cls-name "Header")
        name      (symbol name)
        method    (symbol (str "create" cls-name))
        m         (-> (meta name)
                      (assoc :doc (str "Create a new " cls-name)
                             :arglists (list 'quote (list args))))]
    `(def ~(with-meta name m) (fn [~@args] (. factory ~method ~@args)))))

(defheader ^AcceptHeader accept [^String content-type, ^String sub-type])
(defheader ^AcceptEncodingHeader accept-encoding [^String encoding])
(defheader ^AcceptLanguageHeader accept-language [^java.util.Locale language])
(defheader ^AlertInfoHeader alert-info [^URI info])
(defheader ^AllowHeader allow [^String method])
(defheader ^AllowEventsHeader allow-events [^String event-type])
(defheader ^AuthenticationInfoHeader authentication-info [^String response])
(defheader ^AuthorizationHeader authorization [^String scheme])
(defheader ^CallIdHeader call-id [^String id])
(defheader ^CallInfoHeader call-info [^URI info])
(defheader ^ContactHeader contact [^Address address])
(defheader ^ContentDispositionHeader content-disposition [^String disposition-type])
(defheader ^ContentEncodingHeader content-encoding [^String encoding])
(defheader ^ContentLanguageHeader content-language [^java.util.Locale language])
(defheader ^ContentLengthHeader content-length [^Integer length])
(defheader ^ContentTypeHeader content-type [^String type, ^String sub-type])
(defheader ^DateHeader date[^java.util.Calendar date])
(defheader ^ErrorInfoHeader error-info[^URI info])
(defheader ^EventHeader event[^String type])
(defheader ^ExpiresHeader expires[^Integer seconds])
(defheader ^FromHeader from[^Address address, ^String tag])
(defheader ^InReplyToHeader in-reply-to [^String call-id])
(defheader ^MaxForwardsHeader max-forwards[^Integer number])
(defheader ^MimeVersionHeader mime-version[^Integer major, ^Integer minor])
(defheader ^MinExpiresHeader min-expires[^Integer seconds])
(defheader ^OrganizationHeader organization[^String value])
(defheader ^PriorityHeader priority[^String value])
(defheader ^ProxyAuthenticateHeader proxy-authenticate[^String scheme])
(defheader ^ProxyAuthorizationHeader proxy-authorization[^String scheme])
(defheader ^ProxyRequireHeader proxy-require[^String option-tag])
(defheader ^RAckHeader r-ack[^Integer r-seq, ^Integer cseq, ^String method])
(defheader ^RSeqHeader r-seq[^Integer number])
(defheader ^ReasonHeader reason[^String protocol ^Integer, cause ^String text])
(defheader ^RecordRouteHeader record-route[^Address address])
(defheader ^ReferToHeader refer-to[^Address address])
(defheader ^ReplyToHeader reply-to[^Address address])
(defheader ^RequireHeader require[^String option-tag])
(defheader ^RetryAfterHeader retry-after[^Integer seconds])
(defheader ^RouteHeader route[^Address address])
(defheader ^ServerHeader server[^java.util.List product])
(defheader ^SubjectHeader subject[^String subject])
(defheader ^SubscriptionStateHeader subscription-state[^String state])
(defheader ^SupportedHeader supported[^String option-tag])
(defheader ^TimeStampHeader time-stamp[^Float time])
(defheader ^ToHeader to[^Address address, ^String tag])
(defheader ^UnsupportedHeader unsupported[^String option-tag])
(defheader ^UserAgentHeader user-agent[^java.util.List product])
(defheader ^ViaHeader via[^String host, ^Integer port, ^String transport, ^String branch])
(defheader ^WarningHeader warning[^String agent, ^Integer code, ^String comment]) ; 3DIGIT code between 99 and 1000

(defn ^CSeqHeader cseq
  "Creates a new CSeqHeader based on the newly supplied sequence number and method values."
  [^long seq-num, ^String method]
  (.createCSeqHeader factory seq-num method))

(defn ^SIPETagHeader sip-etag
  "Creates a new SIP-ETag header with the supplied tag value"
  [^String etag]
  (.createSIPETagHeader factory etag))

(defn ^SIPIfMatchHeader sip-if-match
  "Creates a new SIP-If-Match header with the supplied tag value"
  [^String etag]
  (.createSIPIfMatchHeader factory etag))

(defn ^ContactHeader wildcard-contact
  "Creates a new wildcard ContactHeader.
  This is used in Register requests to indicate to the server that it should remove all locations the at which
  the user is currently available. This implies that the following conditions are met:

  ContactHeader.getAddress.getUserInfo() == *;
  ContactHeader.getAddress.isWildCard() == true;
  ContactHeader.getExpires() == 0;"
  []
  (.createContactHeader factory))

(defn ^WWWAuthenticateHeader www-authenticate
  "Creates a new WWWAuthenticateHeader based on the newly supplied scheme value."
  [^String scheme, ^String realm, ^String nonce & {:keys [algorithm, qop opaque domain stale]}]
  (doto (.createWWWAuthenticateHeader factory scheme)
    (.setRealm realm)
    (.setNonce nonce)
    (#(when algorithm (.setAlgorithm ^WWWAuthenticateHeader %, algorithm)))
    (#(when qop (.setQop ^WWWAuthenticateHeader %, qop)))
    (#(when opaque (.setOpaque ^WWWAuthenticateHeader %, opaque)))
    (#(when domain (.setDomain ^WWWAuthenticateHeader %, domain)))
    (#(when stale (.setStale ^WWWAuthenticateHeader %, stale)))))

(defn ^Header extension
  "Creates a new Header based on the newly supplied name and value values."
  [name value]
  (.createHeader factory name (str value)))


(defn header?
  "Check whether the object is a Header or not"
  [object]
  (instance? Header object))
