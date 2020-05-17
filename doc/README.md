# Installation

The next packages are required to make it work.

  - [Emacs](https://www.google.com/search?q=emacs)
  - [Clojure](https://clojure.org/guides/getting_started)
  - [orgmk](https://github.com/fniessen/orgmk)
  - [org-roam](https://org-roam.readthedocs.io/en/latest/)
  - [mkdocs material](https://squidfunk.github.io/mkdocs-material/) and
    this plugin [awesomepages](https://github.com/lukasgeiter/mkdocs-awesome-pages-plugin)

See the pages for the steps of installing each dependencies.

## Restriction in authoring

Because of the export and how mkdocs read markdown files, the first
level should be the title of your cards, and then only keep second level
headlines. A typical file will look as the following

```org
#+TITLE: A good title
#+OPTIONS: toc:nil
#+ROAM_ALIAS: title the-title good-title
#+TAGS: title how-to-write-titles

* A good title

- tags :: use-org-roam-insert

** First heading
```

## Quick start

Make sure you fulfill the requirements. Then

```bash
git clone https ~/Documents/org_roam_clj_files/
cd ~/Documents/org_roam_clj_files/example
```

Set the org-roam-directory in your settings (e.g. `init.el`)

```elisp
(set org-roam-directory "~/Documents/org_roam_clj_files/example")
```

Go into the example folder, edit some org files, run the
`org-roam-db-build-cache` function. Then

```bash
clojure -m org-roam-clj.core -t create-tags
clojure -m org-roam-clj.core -t markdown
```

## Configuration

The [mkdocs.yml](mkdocs.yml) file sets the configuration of mkdocs. See
[mkdocs material](https://squidfunk.github.io/mkdocs-material/) for more
details.

### Emacs settings

Make sure to set the `org-roam` folder to the folder where your
`deps.edn` is located.

```elisp
(set org-roam-directory "path/to/root-org-files")
```

You can overwrite the default template with the following to add
directly the tags and removal of table of content

```elisp
(require 's)
(require 'org-roam)

(setq
 org-roam-capture-templates
 (let ((s (s-join
           "\n"
           '("#+TITLE: ${title}\n#+OPTIONS: toc:nil"
             "#+ROAM_ALIAS: %(s-dashed-words \"${title}\")"
             "#+TAGS: %(s-dashed-words \"${title}\")"
             "#+DATE: %<%Y-%m-%d>"
             ""
             "* ${title}"))))
   (list (list "d" "default" 'plain '(function org-roam-capture--get-point)
               "%?"
               :file-name "cards/%<%Y%m%d%H%M%S>-${slug}"
               :head s
               :unnarrowed t))))
```

## Generate docs

```bash
clojure -m org-roam-clj.core -t markdown
```

Docs will be generated in the `docs-md` folder because github pages only
supports distribution from the `docs` folder (which is a symlink from
site) .

## FAQ

### Why Clojure and not elisp?

Babashka and Clojure were fast enough to perform the task I wanted to
do, and my skills in elisp were not good enough to make the project.

## Possible Extension

We do not really need emacs nor org-roam to make the link work. We just need to
be able to parse the org files and manage the `org-roam.db` files. This would
open the tool for everyone who do not use Emacs to manage their org files. Nor
does it fully depends on org files (we could easily extend it to markdown, with
yaml headers).
