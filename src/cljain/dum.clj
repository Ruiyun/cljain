(ns ^{:doc "place doc string here"
      :author "ruiyun"}
  cljain.dum
  (:use     cljain.tools.predicate
            [clojure.string :only [upper-case]])
  (:require [cljain.sip.core :as core]
            [cljain.sip.header :as header]
            [cljain.sip.address :as addr]
            [cljain.sip.message :as msg]
            [cljain.sip.dialog :as dlg]
            [cljain.sip.transaction :as trans]
            [cljain.tools.timer :as timer]))

(def ^{:doc "Store current account information, contain :user :domain :display-name"
       :added "0.2.0"
       :dynamic true}
  account-map (atom {}))

(def ^{:doc "Store all the request handlers by 'def-request-handler'"
       :added "0.2.0"
       :private true}
  request-handlers-map (atom {}))

(defn account
  "Get current bound account information."
  {:added "0.2.0"}
  []
  @account-map)

(defn add-handler!
  "Add a sip request received event handler to dum."
  {:added "0.2.0"}
  [method process-fn]
  (let [method (keyword (upper-case (name method)))]
    (swap! request-handlers-map assoc method process-fn)))

(defmacro def-request-handler
  "Define the handler to handle the sip request received from under layer.

  (def-request-handler :MESSAGE [request transaction dialog]
    (do-somethin)
    ...)"
  {:arglists '([method [request transaction dialog] body*])
   :added "0.2.0"}
  [method args & body]
  `(let [f# (fn [~@args] ~@body)]
     (add-handler! ~method f#)))

(defn- request-processor
  "Return a funcion to process request."
  {:added "0.2.0"}
  []
  (fn [{:keys [request server-transaction dialog]}]
    (let [method (keyword (upper-case (name (msg/method request))))
          process-fn (get @request-handlers-map method)
          server-transaction (or server-transaction (core/new-server-transaction! request))]
      (and process-fn (process-fn request server-transaction dialog)))))

(defn- response-processor
  "Return a function to process response."
  {:added "0.2.0"}
  []
  (fn [{:keys [response client-transaction dialog]}]
    (when (not (nil? client-transaction))
      (let [{process-success :on-success
             process-failure :on-failure} (trans/application-data client-transaction)
            lead-number-of-status-code (quot (msg/status-code response) 100)]
        (cond ; ignore 1xx provisional response
          (= lead-number-of-status-code 2) ; 2xx means final success response
            (and process-success (process-success client-transaction dialog response))
          (> lead-number-of-status-code 3) ; 4xx, 5xx, 6xx means error
            (and process-failure (process-failure client-transaction dialog response)))))))

(defn- timeout-processor
  "Return a function to process transaction timeout event."
  {:added "0.2.0"}
  []
  (fn [{:keys [transaction]}]
    (let [process-timeout (:on-timeout (trans/application-data transaction))]
      (and process-timeout (process-timeout transaction)))))

(defn- install-event-handler
  "Install a new dum listener to under layer."
  {:added "0.2.0"}
  []
  (core/set-listener!
    :request (request-processor)
    :response (response-processor)
    :timeout (timeout-processor)))

(defn initialize!
  "Set the default user account information with the current bound provider."
  {:added "0.2.0"}
  [& account-info]
  {:pre [(even? (count account-info))
         (check-optional account-info :user string?)
         (check-optional account-info :domain string?)
         (check-optional account-info :display-name string?)]}
  (let [{:keys [user domain display-name]} (apply array-map account-info)]
    (reset! account-map {:user user, :domain domain, :display-name display-name}))
  (install-event-handler))

(defn finalize!
  "Clean user account information with the current bound provider."
  {:added "0.2.0"}
  []
  {:pre [(core/provider-can-be-found?)]}
  (reset! account-map {})
  (reset! request-handlers-map {}))

(defn legal-content?
  "Check the content is a string or a map with :type, :sub-type, :length and :content keys."
  {:added "0.2.0"}
  [content]
  (or (string? content)
    (and (map? content)
      (= (sort [:type :sub-type :content]) (sort (keys content))))))

(defn- try-set-content!
  "Parse the :pack argument from 'send-request!' and 'send-response!',
  then try to set the appropriate Content-Type header and content to the message."
  {:added "0.2.0"}
  [message content]
  (when (not (nil? content))
    (let [content-type        (name (or (:type content) "text"))
          content-sub-type    (name (or (:sub-type content) "plain"))
          content-type-header (header/content-type content-type content-sub-type)
          content             (or (:content content) content)]
      (msg/set-content! message content-type-header content))))

; TODO Do not user application data in transaction or dialog for event dispatch.

(defn send-request!
  "Fluent style sip message send function.

  The simplest example just send a trivial MESSAGE:
  (send-request! :MESSAGE :to (address (uri \"192.168.1.128\"))
  (send-request! :INFO :in dialog-with-bob)

  More complicate example:
  (let [bob (address (uri \"dream.com\" :user \"bob\") \"Bob\")
        alice (address (uri \"dream.com\" :user \"alice\") \"Alice\")]
    (send-request! \"MESSAGE\" :pack \"Welcome\" :to bob :from alice
      :use \"UDP\" :on-success #(prn %1 %2 %3) :on-failure #(prn %1 %2 %3) :on-timeout #(prn %)))

  If the pack content is not just a trivial string, provide a well named funciont
  to return a content map is recommended.
  {:type \"application\"
   :sub-type \"pidf-diff+xml\"
   :content content-object}"
  {:added "0.2.0"}
  [message & options]
  {:pre [(core/provider-can-be-found?)
         (even? (count options))
         (check-optional options :pack legal-content?)
         (check-optional options :to addr/address?)
         (check-optional options :from addr/address?)
         (check-optional options :use :by upper-case in? ["UDP" "TCP"])
         (check-optional options :in dlg/dialog?)
         (check-optional options :more-headers sequential?)
         (check-optional options :on-success fn?)
         (check-optional options :on-failure fn?)
         (check-optional options :on-timeout fn?)]}
  (let [{content :pack
         to-address :to
         from-address :from
         transport :use
         dialog :in
         more-headers :more-headers
         on-success :on-success
         on-failure :on-failure
         on-timeout :on-timeout} (apply array-map options)
        {:keys [user domain display-name]} (account)
        {:keys [ip port transport]} (if (nil? transport)
                                      (core/listening-point)
                                      (core/listening-point transport))
        domain  (or domain ip)
        method  (upper-case (name message))]
    (if (not (nil? dialog))
      (let [request (dlg/create-request dialog method)
            transaction (core/new-client-transcation! request)]
        (dlg/send-request! dialog transaction)
        request)
      (let [request-uri     (if (nil? to-address)
                              (throw (IllegalArgumentException. "Require the ':to' option for send an out of dialog request."))
                              (addr/uri-from-address to-address))
            from-address    (or from-address (addr/address (addr/sip-uri domain :user user) display-name))
            from-header     (header/from from-address (header/gen-tag))
            to-header       (if (= method "REGISTER") ; for REGISTER, To header's uri should equal the From header's.
                              (header/to (header/get-address from-header) nil) ; tag will be auto generated in transaction
                              (header/to to-address nil))
            contact-header  (header/contact (addr/address (addr/sip-uri ip :port port :transport transport :user user)))
            call-id-header  (core/gen-call-id-header)
            via-header      (header/via ip port transport nil) ; via branch will be auto generated before message sent.
            request         (msg/request method request-uri from-header call-id-header to-header via-header contact-header more-headers)
            transaction     (core/new-client-transcation! request)]
        (trans/set-application-data! transaction {:on-success on-success, :on-failure on-failure :on-timeout on-timeout})
        (try-set-content! request content)
        (trans/send-request! transaction)
        request))))

(defn send-response!
  "Send response with a server transactions."
  {:added "0.2.0"}
  [status-code & options]
  {:pre [(core/provider-can-be-found?)
         (even? (count options))
         (check-required options :in trans/transaction?)
         (check-optional options :pack legal-content?)
         (check-optional options :use :by upper-case in? ["UDP" "TCP"])
         (check-optional options :more-headers vector?)]}
  (let [{transaction :in, content :pack, transport :use, more-headers :more-headers} (apply array-map options)
        {:keys [ip port transport]} (if (nil? transport)
                                      (core/listening-point)
                                      (core/listening-point transport))
        user            (:user (account))
        contact-header  (header/contact (addr/address (addr/sip-uri ip :port port :transport transport :user user)))
        request         (trans/request transaction)
        response        (msg/response status-code request contact-header more-headers)]
    (try-set-content! response content)
    (trans/send-response! transaction response)))

(def ^{:doc "Store contexts for the register auto refresh."
       :added "0.3.0"
       :private true}
  register-ctx-map (atom {}))

(defn register-to
  "Send REGISTER sip message to target registry server, and auto refresh register before
  expired.

  Notice: No matter the first register whether sent successfully, the register auto refresh
  timer will be started. Application can choose to stop it use 'stop-refresh-register', or
  let it auto retry after expires secondes."
  {:added "0.3.0"}
  [registry-address expires-seconds & event-handlers]
  {:pre [(addr/address? registry-address)
         (> expires-seconds 38) ; because a transaction timeout is 32 seconds
         (even? (count event-handlers))
         (check-optional event-handlers :on-success fn?)
         (check-optional event-handlers :on-failure fn?)
         (check-optional event-handlers :on-refreshed fn?)
         (check-optional event-handlers :on-refresh-failed fn?)]}
  (let [{:keys [on-success on-failure on-refreshed on-refresh-failed]} (apply array-map event-handlers)
        expires-header              (header/expires expires-seconds)
        safer-interval-milliseconds (* (- expires-seconds 5) 1000)
        register-ctx                (get @register-ctx-map registry-address)]
    (if (nil? register-ctx)
      (let [register-request  (send-request! :REGISTER :to registry-address
                                :more-headers [expires-header]
                                :on-success (fn [_ _ _] (and on-success (on-success)))
                                :on-failure (fn [_ _ _] (and on-failure (on-failure)))
                                :on-timeout (fn [_] (and on-failure (on-failure))))
            refresh-timer     (timer/timer "Register-Refresh")]
        (timer/run! refresh-timer
          (timer/task
            (let [request (.clone (get-in @register-ctx-map [registry-address :request]))
                  request (msg/inc-sequence-number! request)
                  request (msg/remove-header! request "Via")
                  transaction (core/new-client-transcation! request)]
              (trans/set-application-data! transaction
                {:on-success (fn [_ _ _] (and on-refreshed (on-refreshed)))
                 :on-failure (fn [_ _ _] (and on-refresh-failed (on-refresh-failed)))
                 :on-timeout (fn [_] (and on-refresh-failed (on-refresh-failed)))})
              (trans/send-request! transaction)
              (swap! register-ctx-map assoc-in [registry-address :request] request)))
          :delay  safer-interval-milliseconds
          :period safer-interval-milliseconds)
        (swap! register-ctx-map assoc registry-address {:timer    refresh-timer
                                                        :request  register-request})))))

(defn unregister-to
  "Send REGISTER sip message with expires 0 for unregister."
  {:added "0.3.0"}
  [registry-address]
  (let [refresh-timer (get-in @register-ctx-map [registry-address :timer])]
    (and refresh-timer (timer/cancel! refresh-timer))
    (swap! register-ctx-map dissoc registry-address)))
