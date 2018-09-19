# Command line options #

[index.md]

# Running liquid
To run liquid from git repo on Mac and Linux, goto the folder and execute

    clj -m dk.salza.liq.core <parameters>

Using liq.jar execute (Also works on windows):

    java -jar liq.jar <parameters>

Using Leiningen from git repo

    lein run <parameters>

# Parameter options

    --tty                   Force run in terminal. May be used with parameters that normally would disable tty.
    --server                Starts a server on http://localhost:8520
    --jframe                Runs in JFrame. On linux it needs Inconsolata font and Console font on Windows.
    --rows=50 --columns=80  Sets number of rows and columns.
    --autoupdate            If there are multiple views they should be syncronised.
    --load=<path to file>   Load <path to file> as init file.
    --log=<path to file>    Will write log information to the <path to file>.
    --minimal               To prevent loading some default settings. Useful for fully cusomizing Liquid.
    --fontsize              Specify fontsize for use with --jframe

## Example
Some parameters may be combined, like:

    clj -m dk.salza.liq.core --tty --jframe --server --port=7000 --no-init-file --rows=50 --columns=80 --log=/tmp/liq.log

Starting a server on http://localhost:8000

    --server --port=8000 