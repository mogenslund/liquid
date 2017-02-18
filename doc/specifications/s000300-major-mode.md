# Specification s000300

## ID
s000300

## Name
Major Mode

## Description
Major mode is a map with keys:
::key-handler Which is a function that takes a key keyword, like :C-s is input and returns a function (an action), another key-handler or nil.
Notice that a map is also a function, so a map with submaps will be usefull in many situations while a regular function will be suiteable in other.
::syntax-highlighter (TODO: Define interface, maybe define a "reduced slider". Maybe a map with regex for "line-comment", "function-name", etc.)

### Brainstorm
Autoindent rules?
Execute file action (Maybe just part of key-handler)
Cursor color?
::doc


## Requirements

### r001 ???

### r002 ???
