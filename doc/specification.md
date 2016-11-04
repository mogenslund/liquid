Specifications for xed

# Names
Liq (as in liquid, spelled with lambda)

# Tasks
## [ ] The default actionmappings should be in world and setters and getters should be created
## [ ] Eval s-exp does not work when it is at end of buffer
## [ ] Try with build in clojure server and see how far it gets.
## [ ] Some error when cutting and pasting lines. Wrong line is copied
## [ ] Context. See me2. If starts-with "(" then it is a function. Try to locate the file containing the function.
## [ ] Create command function that word starts with "(" sends cljutil res to commandsapp
## [ ] Clean up framer and screen. Make framer independent of screen implementation.
## [ ] Real window implementation. See me2.
## [ ] Make atomic function private and isolated. Public functions should always leave world in consistent state
## [ ] Create editor.clj file with functions emacs like to access from others
## [ ] Prober handling of .xed file and remove own stuff from core
## [ ] jFrame implementation of screen
## [ ] Folding. Maybe maintain a list of fold positions ([23 78] [25 53] [100 110])
## [ ] Tag (/¤¤color:green,bg:yellow¤¤/) syntax hightligt first, then handle replacement in screen implementation.
## [ ] replace-letter. Keyboard :r
## [ ] Space to do page down
## [ ] show-matching command for () [] {}
## [ ] Undo functionality
## [ ] end-of-buffer command performance
## [ ] Implement main editor as an app, reducing the core.
# Layers
Calls should not be made past a layer!

Layer 0:       world
Layer 1:     subeditor --> screen
Layer 2:       editor  --> keys
Layer 3:     core apps
# Main loop
1. Get input from inputadapter (stdin, webservice, jFrame) : It is a keyword like :k, :C-j, :enter
2. Send input to editor
3. Request rendered windows from editor/renderer
4. Send rendered display to in seperate thread screenadapter (tty, browser, jFrame)
5. Wait for input
# Syntax highlighting
http://programmers.stackexchange.com/questions/294065/what-is-an-efficient-data-structure-for-syntax-highlighting-in-text-editors