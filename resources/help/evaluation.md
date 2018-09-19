# Evaluation #

[index.md]

# Reloading current file/namespace (C-e)
To make a raw load/reload of the current namespace type C-e.
This will simply load the current clj file as regular Clojore file.

# Loading the content of the current buffer (E)
To load just the content of the current buffer and print the output to the -prompt- buffer type E (shift+e) in navigation mode (blue cursor).

In this case every "println" will be redirected to the -prompt- buffer.

Compared to the raw reload this is more useful when experimenting with some local code to check the output, while the raw reload is better for loading a complete namespace so it can be used elsewhere..

# Loading snippets (e)
Typeing e in navigation mode will evaluate the selected text if there is any selection, otherwise and attempt will be made to load the current S-expression.

## Example
More cursor to "do" in the S-expression below and type e:

    (do (def myvar "It works") (println myvar))

This will print "It works" in the -prompt- buffer and the defined variable will be useful for further evaluation:

    (str myvar ", still!")