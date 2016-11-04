# Technical documentation of λiquid text editor
This documentation is for those who want to extend the editor, either by modifying the source, creating plugins or do advanced modifications to the .liq file.

## Editor
The editor works almost like an object. It is a huge state, which can be modified by function calls. Eg. (editor/end-of-line) will move the cursor to the end of the line in the active buffer. The thought is that any editor call should be possible at any moment, therefore only functions that leaves the editor in a consistent state should be implemented here.

Most actions in the editor are done to the current buffer.

## Buffer
The buffer is a map with information of e.g buffer content, filename, mode, undo, dirty or not dirty and a set of functions that does something to a buffer and returns a new buffer with the modification.
Most call from the editor takes the current buffer and replaces it with a modified buffer. So (editor/end-of-line) will replace the current buffer with a new buffer as result of the function (buffer/end-of-line buffer). Most of the functions in the buffer namespace takes a buffer as first command, so it is easy to do multiple modification by using the threading macro, like

    (-> buffer (insert "something") (forward-char 3) (insert "more") (beginning-of-line))

The text content of the buffer is stored in a structure which is called a Slider. So most modifications to a buffer can be split into a similar change to a Slider and some household (like changing the dirty flag.).

## Slider
The primary parts of the slider are two lists. One list "before" contains the characters before the cursor in *reverse* order and the list "after" contains the characters after the cursor. So if the content of the slider is "something" and the cursor is place just before "e", like "som|ething", the structure will look like this:

    :before '("m" "o" "s")
    :after  '("e" "t" "h" "i" "n" "g")

Moving the cursor to the left is handled by taking the first element of "before" and moving it to be the first element of "after", like this

    :before '("o" "s")
    :after  '("m" "e" "t" "h" "i" "n" "g")

So basic movements are mostly done by moving elements from the head of one list to the head of the other (which should be a fast action on lists). Actions like getting the current character is just done by returning the head element of "after".

The slider also keeps track of marks, which are just named positions. Each time text is inserted or deleted, the marks are adjusted correspondingly. So if 3 letters are removed strictly before a mark, the mark will be decreased with 3. Marks on and after the current position will not be effected.

The slider should not be extended much. It is considered a very basic structure in the editor and it has to perform well and be highly consistent. Everything relies on this structure. Therefore it has been of highest priority to cover this namespace with unittests.

Like with the buffer most functions takes a Slider and returns a modified Slider.

## Modes
A mode basically consists of a "run" function, a keymapping, some state and extra functions.

In the case where the mode represents some "application" within Liquid the run function will typically create the buffer and set itself as mode for that buffer.
Almost every keypress will then be redirected the the mapping specified in the mode.

### Example
todo:
Rewrite mode concept. From .liq it should be easy to create a mode that just enter "x" when space is pressed and quits the buffer when "q" is pressed. The mode should be "registered" as a command in commandmode.