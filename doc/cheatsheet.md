# Cheatsheet


## Notation
Control: C-h f means hold down control while pressing h, then release the control and press f.

Alt/Meta: M-a M-b means hold down the Meta/Alt key while pressing a and b before releasing the Meta key.

Uppercase: A means shift + a

## Switch keymapping
The Tab key switches between navigation mode (blue cursor) and insert mode (green cursor).

## Mappings
### Navigation (blue cursor)
<pre>
      <b>C-space</b> Start command typeahead (See Command Type ahead below)
      <b>C-space C-space</b> Typeahead to insert function from classpath
      <b>l</b>   Right (Arrow Right will also work)
      <b>j</b>   Left (Arrow Left will also work)
      <b>i</b>   Up (Arrow up will also work)
      <b>k</b>   Down (Arrow down will also work)
      <b>space</b> Page down
      <b>m</b>   Switch to previous buffer (Usefull for swithing forth and back between two buffers)
      <b>C-o</b> Other window (Move between main and prompt windows)
      <b>O</b>   Context action (If the cursor is on a filepath Liquid will try to open that file.
          If it is a function, Liquid will try to navigate to the definition.
          If it is a url it will try to open the url in a browser.)
</pre>

### Copy, Delete and Insert (blue cursor)
<pre>
      <b>x</b>   Delete char
      <b>Backspace</b> Delete backward
      <b>y y</b> Copy line or selection
      <b>y c</b> Copy context (Word, filename, url, ...)
      <b>p p</b> Paste on new line
      <b>p h</b> Paste
      <b>d d</b> Delete line or selection
      <b>o</b>   Insert line
      <b>r</b>   Replace with next char pressed
      <b>I</b>   Move line up
      <b>K</b>   Move line down
      <b>v</b>   Begin or cancel selection
</pre>

### Filehandling (blue cursor)
<pre>
      <b>s</b>   Save file
      <b>C-f</b> Open file (See Find File section)
</pre>

### Clojure code
<pre>
      <b>C-e</b> Evaluate current file without capturing output (for (re)-loading internal stuff)
      <b>E</b>   Evaluate current file
      <b>e</b>   Evaluate current s-expression
      <b>1</b>   Highlight boundries of the  current s-expression
      <b>2</b>   Select current s-expression
          (Multiple presses will make the cursor switch between begin and end parenthesis.)
</pre>

### Macro recording
<pre>
      <b>H</b>   Start and stop recording
      <b>h</b>   Play recording
</pre>

### Searching (blue cursor)
<pre>
      <b>C-s</b> Search (Type a search string an press enter)
      <b>n</b>   Next search result
</pre>

### Folding (blue cursor)
<pre>
      <b>+ +</b> Cycle headline/code fold
      <b>+ 1</b> Fold headline level 1 and code
      <b>+ 2</b> Fold headline level 2
      <b>+ 3</b> Fold headline level 3
      <b>+ 4</b> Fold headline level 4
      <b>+ 5</b> Fold headline level 5
      <b>+ 0</b> Expand all
      <b>+ s</b> Fold selection
      <b>+ f</b> Fold s-expression
</pre>

## Help
<pre>
      <b>C-h h</b> Index of Help files
      <b>C-h f</b> (Typeahead help for some functions)
      <b>C-h a</b> Apropos function. Free text search in documentation
</pre>

### Quit
      <b>C-q</b>   Quit editor
      <b>C-M-q</b> Force quit editor (Will quit even though there are unsaved files.)
      
### Command Typeahead
When typing C-space a typeahead screen is shown with the following keybindings:

<pre>
      <b>space</b> Space is considered a "wildcard" in typeahead, so "some g" will match: "Something".
      <b>C-k</b>  Select next result
      <b>C-i</b>  Select previous result
      <b>C-g</b>  Quit Command Typeahead
      <b>enter</b> Choose the selected result. If it is a snippet, the result will be pasted.
            If it is a command it will be executed. If it is a buffer, the buffer will be chosen.
</pre>

### Find File
When typing C-f a typeahead file chooser will be shown with the following keybindings:

<pre>
      <b>space</b> Space is considered a "wildcard" in typeahead, so "my le" will match: "Myfile".
      <b>C-k</b> Select the next file
      <b>C-i</b> Select the previous file
      <b>C-g</b> Quit Find File
      <b>enter</b> If the selection is a file the file will be opened.
            If it is a folder, a new typeahead will be started below that.
</pre>

### Headline and Function Navigation
When typing gh (blue cursor) a typeahead headline navigator will be shown with the following keybindings:

<pre>
      <b>space</b> Space is considered a "wildcard" in typeahead, so "my li" will match: "My headline".
      <b>C-k</b> Select the next headline
      <b>C-i</b> Select the previous headline
      <b>C-g</b> Quit Headline Navigation
      <b>enter</b> Move cursor to the selected headline or function
</pre>
