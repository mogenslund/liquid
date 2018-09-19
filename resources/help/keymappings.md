# Keymappings #

[index.md]

# Modes

  tab:        Switch between navigation mode (blue cursor) and insert mode (green cursor)

# Navigation (blue cursor)

  C-spc:      Start command typeahead (See Command Type ahead below)
  C-spc-spc:  (C-spc twice) Typeahead to insert function from classpath
  l:          Right (Arrow Right will also work)
  j:          Left (Arrow Left will also work)
  i:          Up (Arrow up will also work)
  k:          Down (Arrow down will also work)
  space:      Page down
  m:          Switch to previous buffer (Usefull for swithing forth and back between two buffers)
  C-o:        Other window (Move between main and prompt windows)
  O:          Context action (If the cursor is on a filepath Liquid will try to open that file.
                If it is a function, Liquid will try to navigate to the definition.
                If it is a url it will try to open the url in a browser.)

# Copy, Delete and Insert (blue cursor)

  x:          Delete char
  Backspace:  Delete backward
  yy:         Copy line or selection
  yc:         Copy context (Word, filename, url, ...)
  pp:         Paste on new line
  ph:         Paste
  dd:         Delete line or selection
  o:          Insert line
  r:          Replace with next char pressed
  I:          Move line up
  K:          Move line down
  v:          Begin or cancel selection

  
# Filehandling (blue cursor)

  s:          Save file
  C-f:        Open file (See Find File section)

# Clojure code (blue cursor)
[evaluation.md]

  C-e:        Evaluate current file without capturing output (for (re)-loading internal stuff)
  E:          Evaluate current file
  e:          Evaluate current s-expression
  1:          Highlight boundries of the  current s-expression
  2:          Select current s-expression
              (Multiple presses will make the cursor switch between begin and end parenthesis.)

# Macro recording (blue cursor)

  H:          Start and stop recording
  h:          Play recording

# Searching (blue cursor)

  C-s:        Search (Type a search string an press enter)
  n:          Next search result

# Folding and collapsing (blue cursor)

   ++:        Cycle collapse level
   +0:        Expand all
   +1:        Collapse level 1 and function definitions
   +2:        Collapse level 2
   +3:        Collapse level 3
   +4:        Collapse level 4
   +5:        Collapse level 5
   +s:        Collapse selection or expand if collapsed already
   +f:        Collapse function definition

# Help

  C-h h       Index of Help files
  C-h f       (Typeahead help for some functions)
  C-h a       Apropos function. Free text search in documentation

# Quit

  C-q         Quit editor
  C-M-q       Force quit editor (Will quit even though there are unsaved files.)

 
# Command Typeahead
When typing C-space a typeahead screen is shown with the following keybindings: 

  space:      Space is considered a "wildcard" in typeahead, so "some g" will match: "Something".
  C-k:        Select next result
  C-i:        Select previous result
  C-g:        Quit Command Typeahead
  enter:      Choose the selected result. If it is a snippet, the result will be pasted.
              If it is a command it will be executed. If it is a buffer, the buffer will be chosen.

# Find File
When typing C-f a typeahead file chooser will be shown with the following keybindings: 

  space:      Space is considered a "wildcard" in typeahead, so "my le" will match: "Myfile".
  C-k:        Select the next file
  C-i:        Select the previous file
  C-g:        Quit Find File
  enter:      If the selection is a file the file will be opened.
              If it is a folder, a new typeahead will be started below that.

# Headline and Function Navigation
When typing gh (blue cursor) a typeahead headline navigator will be shown with the following keybindings: 

  space       Space is considered a "wildcard" in typeahead, so "my li" will match: "My headline".
  C-k:        Select the next headline
  C-i:        Select the previous headline
  C-g:        Quit Headline Navigation
  enter:      Move cursor to the selected headline or function
