# Specification s000100

## ID
s000100

## Name
Global Keymap

## Description
Liquid Text Editor should have a global keymap concept.
Keys in this map should map to a function. If no other keymaps (local keymaps) override a given mapped key the function should be executed when the key combination is used.

## Requirements

### r001 Option to remap keys in runtime
It should be possible to remap a global key in runtime, by evaluating a piece of code.
Like for example by evaluating: (editor/set-global-key :C-z editor/end-of-line)

### r002 Global Keymap if nothing else
If a key combination form the Global Keymap is pressed, and the same combination is defined in the currently active mode, the currently active mode wins and the function to execute is will be taken from that.