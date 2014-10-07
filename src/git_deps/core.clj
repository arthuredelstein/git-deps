(ns git-deps.core
  (:use [clojure.java.shell :only [sh with-sh-dir *sh-dir*]]))

(def ^{:dynamic true} *repo-path* "../tor-browser31")

(defn ancestor?
  "Returns true if one commit is the ancestor of another."
  [putative-ancestor-commit putative-descendant-commit]
  (let [exit-status
        (:exit (sh "git" "merge-base" "--is-ancestor"
                   putative-ancestor-commit
                   putative-descendant-commit
                   :dir *repo-path*))]
    ;; the exit status is 0 if true, 1 if false
    :exit
    (condp = exit-status
      1 false
      0 true
      (throw (Exception. (str "Error code: " exit-status))))))

(defn responsible-commits
  "Returns a set of commits responsible for a given file."
  [file-path]
  (let [lines
        (-> (sh "git" "blame" file-path
                :dir *repo-path*)
          :out
          (.split "\n"))]
    (->> lines (map #(first (.split % "\\s"))) set)))

(defn commit-comparator
  "Compares two commits."
  [commit1 commit2]
  (cond (= commit1 commit2) 0
        (ancestor? commit1 commit2) -1
        (ancestor? commit2 commit1) 1
        :else (throw (Exception. "Two commits not comparable."))))

(defn sort-commits
  "Sorts a list of commits from earliest to latest."
  [commits]
  (sort commit-comparator commits))