(ns ^{:doc "Here is a simplest example show how to use it:

        (use 'cljain.dum)
        (require '[cljain.sip.core :as sip]
                 '[cljain.sip.address :as addr])

        (defmethod handle-request :MESSAGE [request transaction _]
          (println \"Received: \" (.getContent request))
          (send-response! 200 :in transaction :pack \"I receive the message from myself.\"))

        (global-set-account :user \"bob\" :domain \"localhost\" :display-name \"Bob\" :password \"thepwd\")
        (sip/global-bind-sip-provider! (sip/sip-provider! \"my-app\" \"localhost\" 5060 \"udp\"))
        (sip/set-listener! (dum-listener))
        (sip/start!)

        (send-request! :MESSAGE :to (addr/address \"sip:bob@localhost\") :pack \"Hello, Bob.\"
                       :on-success (fn [& {:keys [response]}]
                                     (println \"Fine! response: \" (.getContent response)))
                       :on-failure (fn [& {:keys [response]}]
                                     (println \"Oops!\" (.getStatusCode response)))
                       :on-timeout (fn [& _]
                                     (println \"Timeout, try it later.\")))

      Remember, if you want to send REGISTER request, prefer to use the 'register-to!' function, it will
      help you to deal the automatic rigister refresh:

        (register-to! (addr/address \"sip:the-registry\") 3600
                      :on-success (fn [response]
                                    (prn \"Register success.\"))
                      :on-failure (fn [response]
                                    (prn \"Register failed.\"))
                      :on-refreshed (fn [response]
                                      (prn \"Refreshed fine.\"))
                      :on-refresh-failed (fn [response]
                                           (prn \"Refresh failed.\")))

      This version cljain.dum has some limitation that if you want auto-refresh work correctly, you must use
      'global-set-account' to give a root binding with *current-account* like previous."}
    cljain.dum
    (:require [clojure.string :refer [upper-case]]
              [cljain.sip.core :as core]
              [cljain.sip.header :as header]
              [cljain.sip.address :as addr]
              [cljain.sip.message :as msg]
              [cljain.sip.dialog :as dlg]
              [cljain.sip.transaction :as trans]
              [ruiyun.tools.timer :as timer]
              [clojure.tools.logging :as log])
    (:import [javax.sip Transaction SipProvider SipFactory Dialog]
             [javax.sip.message Request Response]
             [javax.sip.address Address]
             [javax.sip.header HeaderAddress]
             [gov.nist.javax.sip.clientauthutils AccountManager UserCredentials AuthenticationHelper]
             [gov.nist.javax.sip SipStackExt]))

(def ^{:doc "A map contain these four fields: :user, :domain, :password and :display-name."
       :dynamic true}
  *current-account*)

(defn global-set-account
  "Give the *current-account* a root binding.
  Although you can use the clojure dynamic binding form, but use this function in this version
  cljian.dum is more recommended.

  account has follow keys:
    :user
    :domain
    :password
    :display-name"
  [account]
  (alter-var-root #'*current-account* (fn [_] account)))

(defmulti handle-request (fn [^Request request, ^Transaction transaction, ^Dialog dialog]
                           (keyword (.getMethod request))))

(defrecord AccountManagerImpl []
  AccountManager
  (getCredentials [this challenged-transaction realm]
    (reify UserCredentials
      (getUserName [_] (*current-account* :user))
      (getPassword [_] (*current-account* :password))
      (getSipDomain [_] (*current-account* :domain)))))

(defn dum-listener
  "Create a dum default event listener.
  You can use it for 'cljain.sip.core/set-listener!' function."
  []
  {:request (fn [request transaction dialog]
              (let [transaction (or transaction (core/new-server-transaction! request))]
                (handle-request request transaction dialog)))
   :response (fn [^Response response, ^Transaction transaction, dialog]
               (when (not (nil? transaction))
                 (let [{process-success :on-success
                        process-failure :on-failure} (.getApplicationData transaction)
                       status-code (.getStatusCode response)
                       lead-number-of-status-code (quot status-code 100)]
                   (cond ; ignore 1xx provisional response
                     (= lead-number-of-status-code 2) ; 2xx means final success response
                     (and process-success (process-success :transaction transaction :dialog dialog :response response))

                     (or (= status-code Response/UNAUTHORIZED)
                       (= status-code Response/PROXY_AUTHENTICATION_REQUIRED)) ; need authentication
                     (let [header-factory (.createHeaderFactory core/sip-factory)
                           sip-stack (.getSipStack (core/sip-provider))
                           auth-helper (.getAuthenticationHelper sip-stack (AccountManagerImpl.) header-factory)
                           client-trans-with-auth (.handleChallenge auth-helper response transaction (core/sip-provider) 5)]
                       (.setApplicationData client-trans-with-auth (.getApplicationData transaction))
                       (trans/send-request! client-trans-with-auth))

                     (> lead-number-of-status-code 3) ; 4xx, 5xx, 6xx means error
                     (and process-failure (process-failure :transaction transaction :dialog dialog :response response))))))
   :timeout (fn [transaction _]
              (let [process-timeout (:on-timeout (.getApplicationData transaction))]
                (and process-timeout (process-timeout :transaction transaction))))})

(defn legal-content?
  "Check the content is a string or a map with :type, :sub-type, :length and :content keys."
  [content]
  (or (string? content)
    (and (map? content)
      (= (sort [:type :sub-type :content]) (sort (keys content))))))

(defn- set-content!
  "Parse the :pack argument from 'send-request!' and 'send-response!',
  then try to set the appropriate Content-Type header and content to the message."
  [message content]
  (when (not (nil? content))
    (let [content-type        (name (or (:type content) "text"))
          content-sub-type    (name (or (:sub-type content) "plain"))
          content-type-header (header/content-type content-type content-sub-type)
          content             (or (:content content) content)]
      (.setContent message content content-type-header)))
  message)

(defn send-request!
  "Fluent style sip message send function.

  The simplest example just send a trivial MESSAGE:

    (send-request! :MESSAGE :to (sip-address \"192.168.1.128\"))
    (send-request! :INFO :in dialog-with-bob)

  More complicate example:

    (send-request! \"MESSAGE\" :pack \"Welcome\" :to (sip-address \"192.168.1.128\" :user \"bob\") :use \"UDP\"
      :on-success #(prn %) :on-failure #(prn %) :on-timeout #(prn %))

  If the pack content is not just a trivial string, provide a well named funciont
  to return a content map like this is recommended:

    {:type \"application\"
     :sub-type \"pidf-diff+xml\"
     :content content-object}"
  [message & {content :pack
              to-address :to
              transport :use
              dialog :in
              more-headers :more-headers
              on-success :on-success
              on-failure :on-failure
              on-timeout :on-timeout}]
  {:pre [(core/provider-can-be-found?)
         (bound? #'*current-account*)
         (or (nil? content) (legal-content? content))
         (or (and (= (upper-case (name message)) "REGISTER") (nil? to-address))
             (addr/address? to-address)
             dialog)
         (or (nil? transport) (#{"UDP" "udp" "TCP" "tcp"} transport))
         (or (nil? dialog) (core/dialog? dialog))
         (or (nil? more-headers) (sequential? more-headers))
         (or (nil? on-success) (fn? on-success))
         (or (nil? on-failure) (fn? on-failure))
         (or (nil? on-timeout) (fn? on-timeout))]}
  (let [{:keys [user domain display-name]} *current-account*
        {:keys [ip port transport]} (if (nil? transport)
                                      (core/listening-point)
                                      (core/listening-point transport))
        domain  (or domain ip)
        method  (upper-case (name message))]
    (if (not (nil? dialog))
      (let [request (-> (dlg/create-request dialog method) (set-content! content))
            ^Transaction transaction (core/new-client-transcation! request)]
        (.setApplicationData transaction {:on-success on-success, :on-failure on-failure, :on-timeout on-timeout})
        (dlg/send-request! dialog transaction)
        request)
      (let [request-uri     (if (nil? to-address)
                              (throw (IllegalArgumentException. "Require the ':to' option for send an out of dialog request."))
                              (.getURI to-address))
            from-address    (addr/sip-address domain :user user :display-name display-name)
            from-header     (header/from from-address (header/gen-tag))
            to-header       (if (= method "REGISTER") ; for REGISTER, To header's uri should equal the From header's.
                              (header/to (.getAddress from-header) nil) ; tag will be auto generated in transaction
                              (header/to to-address nil))
            contact-header  (header/contact (addr/sip-address ip :port port :transport transport :user user))
            call-id-header  (core/gen-call-id-header)
            via-header      (header/via ip port transport nil) ; via branch will be auto generated before message sent.
            request         (msg/request method request-uri from-header call-id-header to-header via-header contact-header more-headers)
            ^Transaction transaction     (core/new-client-transcation! request)]
        (.setApplicationData transaction {:on-success on-success, :on-failure on-failure :on-timeout on-timeout})
        (set-content! request content)
        (trans/send-request! transaction)
        request))))

(defn send-response!
  "Send response with a server transactions."
  [status-code & {^Transaction transaction :in, content :pack, transport :use, more-headers :more-headers}]
  {:pre [(core/provider-can-be-found?)
         (bound? #'*current-account*)
         (core/transaction? transaction)
         (or (nil? content) (legal-content? content))
         (or (nil? transport) (#{"UDP" "udp" "TCP" "tcp"} transport))
         (or (nil? more-headers) (sequential? more-headers))]}
  (let [{:keys [ip port transport]} (if (nil? transport)
                                      (core/listening-point)
                                      (core/listening-point transport))
        user            (*current-account* :user)
        contact-header  (header/contact (addr/sip-address ip :port port :transport transport :user user))
        request         (.getRequest transaction)
        response        (msg/response status-code request contact-header more-headers)]
    (set-content! response content)
    (trans/send-response! transaction response)))

(def ^{:doc "Store contexts for the register auto refresh."
       :private true}
  register-ctx-map (atom {}))

(defn- reg-req-for-refresh [registry-address]
  (when-let [^Request req (get-in @register-ctx-map [registry-address :request])]
    (doto ^Request (.clone req)
      (msg/inc-sequence-number!)
      (.removeHeader "Via"))))

(defn register-to!
  "Send REGISTER sip message to target registry server, and auto refresh register before
  expired.

  Notice: please call 'global-set-account' before you call 'register-to!'. in this version,
  use dynamic binding form to bind *current-account* can not work for auto-refresh."
  [registry-address expires-seconds & {:keys [on-success on-failure on-refreshed on-refresh-failed]}]
  {:pre [(addr/address? registry-address)
         (> expires-seconds 38) ; because a transaction timeout is 32 seconds
         (or (nil? on-success) (fn? on-success))
         (or (nil? on-failure) (fn? on-failure))
         (or (nil? on-refreshed) (fn? on-refreshed))
         (or (nil? on-refresh-failed) (fn? on-refresh-failed))]}
  (let [expires-header              (header/expires expires-seconds)
        safer-interval-milliseconds (* (- expires-seconds 5) 1000)
        register-ctx                (get @register-ctx-map registry-address)]
    (when (nil? register-ctx)
      (send-request! :REGISTER :to registry-address
        :more-headers [expires-header]
        :on-success (fn [& {:keys [transaction response]}]
                      (swap! register-ctx-map assoc registry-address
                        {:timer (timer/run-task!
                                  #(let [request (reg-req-for-refresh registry-address)
                                         transaction (core/new-client-transcation! request)]
                                     (.setApplicationData transaction
                                       {:on-success (fn [& {response :response}] (and on-refreshed (on-refreshed response)))
                                        :on-failure (fn [& {response :response}] (and on-refresh-failed (on-refresh-failed response)))
                                        :on-timeout (fn [& _] (and on-refresh-failed (on-refresh-failed nil)))})
                                     (trans/send-request! transaction)
                                     (swap! register-ctx-map assoc-in [registry-address :request ] request))
                                  :by (timer/timer "Register-Refresh")
                                  :delay safer-interval-milliseconds
                                  :period safer-interval-milliseconds
                                  :on-exception #(log/warn "Register refresh exception: " %))
                         :request (trans/request transaction)})
                      (and on-success (on-success response)))
        :on-failure (fn [& {response :response}] (and on-failure (on-failure response)))
        :on-timeout (fn [& _] (and on-failure (on-failure nil)))))))

(defn unregister-to!
  "Send REGISTER sip message with expires 0 for unregister.
  And the auto-refresh timer will be canceled."
  [registry-address]
  (let [refresh-timer (get-in @register-ctx-map [registry-address :timer])]
    (and refresh-timer (timer/cancel! refresh-timer))
    (let [request (reg-req-for-refresh registry-address)]
      (.setHeader request (header/expires 0))
      (trans/send-request! (core/new-client-transcation! request)))
    (swap! register-ctx-map dissoc registry-address)))
