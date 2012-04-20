(ns ^{:doc "place doc string here"
      :author "ruiyun"}
  cljain.util)

(defn legal-proxy-address?
  "判断地址格式是否合法，如合法，返回非空集合，否则返回nil."
  {:added "0.2.0"}
  [address]
  (let [re #"^\d+\.\d+\.\d+\.\d+(:\d+)?(/(tcp|TCP|udp|UDP))?$"]
    (re-find re address)))

(defn parse-address
  "place doc string here"
  {:added "0.2.0"}
  ([address-str]
    (let [re #"(\d+\.\d+\.\d+\.\d+)(:(\d+))?(/(tcp|TCP|udp|UDP))?$"
          [_ ip _ port _ transport] (re-find re address-str)
          address (transient {:ip ip})]
      (when (nil? ip) (throw (AssertionError. "Incorrect address-str format.")))
      (when port (assoc! address :port (Integer/parseInt port)))
      (when transport (assoc! address :transport (.toUpperCase transport)))
      (persistent! address)))
  ([address-str defaults]
    (let [address (merge defaults (parse-address address-str))
          transport (:transport address)]
      (if transport
        (assoc address :transport (.toUpperCase transport))
        address))))

(defmacro check-nullable
  "检查可选的参数是否能通过`check-fn`校验，如果`arg`为空，则直接通过检查."
  {:added "0.2.0"}
  [arg check-fn & check-fn-args]
  `(or (nil? ~arg) (~check-fn ~arg ~@check-fn-args)))
