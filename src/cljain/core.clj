(ns ^{:doc "place doc string here"
      :author "ruiyun"}
  cljain.core
  (:use cljain.util
        [clojure.string :only [upper-case]])
  (:require [clojure.tools.logging :as log])
  (:import [java.util Properties]
           [javax.sip SipFactory SipStack SipProvider SipListener
            Transaction ClientTransaction Dialog
            ResponseEvent IOExceptionEvent TimeoutEvent Timeout
            TransactionTerminatedEvent DialogTerminatedEvent]))

(def ^{:doc "用于存放当前可用的上下文.
也许一个更好的设计是，借助stm，利用clojure的集合提供命令队列（但需要权衡读写比率），然后
采用agent访问作为外部资源的jain api，执行命令."
       :added "0.2.0"
       :private true}
  ctx-map (ref {}))

(defn store-ctx!
  "###保存cljain上下文对象.
  将上下文保存到用于存放当前可用上下文的集合中."
  {:added "0.2.0"}
  [name ctx]
  (dosync
    (commute ctx-map assoc name ctx)
    name))

(defn get-ctx
  "###根据名称获取指定的cljain上下文对象."
  {:added "0.2.0"}
  [name]
  (get @ctx-map name))

(defn pick-ctx!
  "###根据名称提取指定的cljain上下文对象.
  与`get-ctx`不同，`pick-ctx`**提取**上下文对象后，将该对象从集合中移除."
  {:added "0.2.0"}
  [name]
  (dosync
    (let [ctx (get-ctx name)]
      (alter ctx-map dissoc name)
      ctx)))

(defn get-ctx-content
  "place doc string here"
  {:added "0.2.0"}
  [name key]
  (key (get-ctx name)))

(defn alter-ctx-content!
  "place doc string here"
  {:added "0.2.0"}
  [name key f & more-args]
  {:pre [(contains? (set (keys (get-ctx name))) key)
         (not (contains? #{:stack :provider :listening-point} key))
         (fn? f)]}
  (dosync
    (let [contents (key (get-ctx name))]
      (let [contents (f contents more-args)]
        (alter ctx-map assoc-in [name key] contents)))))

(def ^{:doc "place doc string here"
       :added "0.2.0"
       :private false}
  sip-factory (doto (SipFactory/getInstance) (.setPathName "gov.nist")))

(defn- call-evt-fn
  "place doc string here"
  {:added "0.2.0"}
  [ctx src-type id evt-key args]
  {:pre [(not (nil? ctx))
         (contains? #{:transactions :dialogs} src-type)
         (string? id)
         (keyword? evt-key)]}
  (let [evt-fn (get-in ctx [src-type id :evt-handler evt-key])]
    (and evt-fn (evt-fn (first args)))))

(defn- call-trans-evt-fn
  "###调用指定的事务事件处理函数."
  {:added "0.2.0"}
  [ctx trans evt-key & args]
  {:pre [(instance? Transaction trans)]}
  (let [tid (.getBranchId trans)]
    (call-evt-fn ctx :transactions tid evt-key args)))

(defn- call-dlg-evt-fn
  "place doc string here"
  {:added "0.2.0"}
  [ctx dlg evt-key & args]
  {:pre [(instance? Dialog dlg)]}
  (let [did (.getDialogId dlg)]
    (call-evt-fn ctx :dialogs did evt-key args)))

(defn- get-trans-from-evt
  "event中包含 Transaction 信息，但不确定是 ServerTranscation 还是
  ClientTranscation."
  {:added "0.2.0"}
  [evt]
  {:pre [(contains? #{TimeoutEvent TransactionTerminatedEvent} (class evt))]}
  (or (.getServerTransaction evt) (.getClientTransaction evt)))

(defn create-listener
  "place doc string here"
  {:added "0.2.0"}
  [ctx-name]
  (reify SipListener
    (processRequest [this evt]
      (log/trace "processRequest has been invoked.")
      ; TODO 暂未处理会话内的请求事件
      (let [evt-fn (get (get-ctx ctx-name) :request-handler)]
        (and evt-fn (evt-fn evt))))
    (processResponse [this evt]
      (log/trace "processResponse has been invoked.")
      (let [trans (.getClientTransaction evt)]
        (call-trans-evt-fn (get-ctx ctx-name) trans :response evt)))
    (processIOException [this evt]
      (log/trace "processIOException has been invoked.")
      (let [src-obj (.getSource evt)
            ctx (get-ctx ctx-name)]
        (cond
          (instance? Transaction src-obj)
          (let [trans (cast Transaction src-obj)]
            (call-trans-evt-fn ctx trans :io-exception evt))
          (instance? Dialog src-obj)
          (let [dlg (cast Dialog src-obj)]
            (call-dlg-evt-fn ctx dlg :io-exception evt))
          :else (log/warn "IOException doesn't trigger from transaction or
                       dialog. dst is" (str (.getHost evt) ":"
                                       (.getPort evt) "/"
                                       (.getTransport evt))))))
    (processTimeout [this evt]
      (log/trace "processTimeout has been invoked.")
      (if (= (.getTimeout evt) Timeout/TRANSACTION)
        (call-trans-evt-fn (get-ctx ctx-name) (get-trans-from-evt evt)
          :timeout evt)
        (log/warn "cljain doesn't support none transaction timeout.")))
    (processTransactionTerminated [this evt]
      (log/trace "processTransactionTerminated has been invoked.")
      (let [trans (get-trans-from-evt evt)
            trans-id (.getBranchId trans)]
        (call-trans-evt-fn (get-ctx ctx-name) trans :trans-terminated evt)
        (alter-ctx-content! ctx-name :transactions
          #(dissoc %1 (first %2)) trans-id)))
    (processDialogTerminated [this evt]
      (log/trace "processDialogTerminated has been invoked.")
      (let [dlg (.getDialog evt)]
        (call-dlg-evt-fn (get-ctx ctx-name) dlg :dlg-terminated evt)))))

(defn create-ctx
  "###创建cljain上下文对象.
  此处定义的cljain上下文，用于表示一组`jain-sip`运行时对象，包括 `SipStack`,
  `SipProvider`, 和上层提供的UAS事件处理器，以及运行期事务和会话集合等。典型的，
  一个cljain上下文对象， 在上层UA调用 `cljain.core/start` 后被创建并托管，
  最终在上层UA调用 `cljain.core/stop` 后被销毁."
  {:added "0.2.0"}
  [name host {:keys [port transport user domain display-name request-handler
                     out-proxy]}]
  {:pre [(check-nullable port > 0)
         (check-nullable transport #(contains? #{"TCP" "UDP"} (upper-case %)))
         (check-nullable user string?)
         (check-nullable domain string?)
         (check-nullable display-name string?)
         (check-nullable request-handler fn?)
         (check-nullable out-proxy legal-proxy-address?)]}
  (let [properties (doto (Properties.)
                         (.setProperty "javax.sip.STACK_NAME" name)
                         (#(when out-proxy (.setProperty %
                                             "javax.sip.OUTBOUND_PROXY"
                                             out-proxy))))
        stack (.createSipStack sip-factory properties)
        port (or port 5060)
        transport (or transport "UDP")
        listening-point (.createListeningPoint stack host port transport)
        provider (doto (.createSipProvider stack listening-point)
                       (.addSipListener (create-listener name)))]
      {:stack stack
       :provider provider
       :listening-point {:host host :port port :transport transport}
       :request-handler request-handler
       :user (or user name)
       :domain (or domain host)
       :display-name display-name
       :transactions {}
       :dialogs {}}))
