(ns cljdoc.util.scm
  "Utilities to extract information from SCM urls (GitHub et al)"
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [lambdaisland.uri :as uri]))

(defn owner [scm-url]
  (get (string/split (:path (uri/uri scm-url)) #"/") 1))

(defn repo [scm-url]
  (get (string/split (:path (uri/uri scm-url)) #"/") 2))

(defn coordinate [scm-url]
  (->> (string/split (:path (uri/uri scm-url)) #"/")
       (filter seq)
       (string/join "/")))

(defn provider [scm-url]
  (when-some [host (:host (uri/uri scm-url))]
    (cond
      (.endsWith host "github.com") :github
      (.endsWith host "gitlab.com") :gitlab
      (.endsWith host "sr.ht") :sourcehut)))

(defn http-uri
  "Given a URI pointing to a git remote, normalize that URI to an HTTP one."
  [scm-url]
  (cond
    (.startsWith scm-url "http")
    scm-url

    (or (.startsWith scm-url "git@")
        (.startsWith scm-url "ssh://"))
    (-> scm-url
        (string/replace #":" "/")
        (string/replace #"\.git$" "")
        ;; three slashes because of prior :/ replace
        (string/replace #"^(ssh///)*git@" "http://"))))

(defn fs-uri
  "Normalize a provided `scm-path` to it's canonical path,
  return `nil` if the path does not exist on the filesystem."
  [scm-path]
  (let [f (io/file scm-path)]
    (when (.exists f)
      (.getCanonicalPath f))))

(defn ssh-uri
  "Given a URI pointing to a git remote, normalize that URI to an SSH one."
  [scm-url]
  (cond
    (.startsWith scm-url "git@")
    scm-url

    (.startsWith scm-url "http")
    (let [{:keys [host path]} (uri/uri scm-url)]
      (str "git@" host ":" (subs path 1) ".git"))))

(defn view-uri
  [{:keys [url branch]} source-file]
  (let [blob-path (if (= :sourcehut (provider url))
                    "/tree/"
                    "/blob/")]
    (str url blob-path (or branch "master") "/" source-file)))

(defn normalize-git-url
  "Ensure that the passed string is a git URL and that it's using HTTPS"
  [s]
  (cond-> s
    (.startsWith s "http") (string/replace #"^http://" "https://")
    (.startsWith s "git@github.com:") (string/replace #"^git@github.com:" "https://github.com/")
    (.endsWith s ".git") (string/replace #".git$" "")))
