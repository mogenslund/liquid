# Contributing to Liquid
Anyone is welcome to submit pull requests for Liquid.

Here are some directions to consider before submitting a pull request:

* Liquid core should not depend on anything else than Clojure (or ClojureScript). If some important features requires dependencies they should be created as extensions.
* Supplying unit tests for new features will increase the likelihood of the pull request to be accepted. Existing unittest should of cause pass or be corrected to pass, if the change calls for unit tests to be adjusted.
* Documentation on functions will also increase the likelihood of the pull request to be accepted.
* The MIT license should not be compromised by the pull request.