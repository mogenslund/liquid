# Data types
This document describes some of the structures used in Liquid. They are usually just general maps, but with some expected content.

## Character
A charater can be a string containing a char, a map containing a char and information about foreground and background colors or it can be a slider, which is handled like a character, like collapsed content.

    "a"
    {:char "a" :face :string :bgface :selection}
    <slider>

## Slider
A slider is an immutable datastructure to handle editable text, like moving the cursor, selecting, inserting and deleting text.
These operation usually take a slider and returns another slider with the operation applied.

    {::before (list <character>)
     ::after (list <character>)
     ::point 8
     ::linenumber 1
     ::totallines 10
     ::dirty false
     ::marks {:hl1 10 :hl2 23 :selection 2 :somehting 8}}

## Keymap
A keymap is a mapping from a string corresponding to a key on the keyboard to a function.
If the function takes no parameters it will just be executed.
If the function takes one parameter it will be assumed the parameter is a <slider> and the function will be applied to the slider in the current buffer.

    {"i" editor/backward-line
     "f6" #(-> % slider/right slider/down)}

## Buffer
The buffer contains the editor behavior in a given context.

Data: Usually a slider or something that outputs to a slider.
Actions: Keymaps and function to operate on the data.
View: Function to handle syntax highlight.

Besides that some meta data like filename and functionality to handle undo on the data.

    {::name "Some name"
     ::slider <slider>
     ::slider-undo (list <slider>)  
     ::slider-stack (list <slider>)
     ::filename "/tmp/tmp.clj"
     ::modified true
     ::mem-col 10
     ::highlighter <function>
     ::keymap <keymap>}

## Line
A line is a position and a list of content of the characters to draw.

    {:row 10 :column 20 :line (list <character>)}

## Lines list
Lines lists are used as material to draw on the screen. The renderer extract current content from the sliders in the editor and produces a list of lines which should be drawn to the screen by the adapters.

    (list <line>)

## Window
A window is a specification of an area on the screen. The window contains the name of the buffer, which content is to be printed in the given area.

    {::name "Some name"
     ::top 30
     ::left 100
     ::rows 40
     ::columns 80
     ::buffername "Some buffer"})


## Editor
The editor contains the list of buffer and shared information, like windows, search paths.

    {::buffers (list <buffer>)
     ::windows (list <window>)
     ::global-keymap {"C-o" editor/other-window}}
     ::file-eval {"py" #(cshell/cmd "python" %)}
     ::frame-dimensions {::rows 40 ::columns 140}
     ::settings {::default-keymap <keymap>
                 ::key-info false
                 ::default-highlighter <function>
                 ::default-app <function>
                 ::searchstring ""
                 ::searchpaths ("/tmp")
                 ::rootfolders ("/home/foo")
                 ::files ("/home/foo/used-a-lot.clj")
                 ::snippets ("Foo bar")
                 ::commands (editor/find-file editor/save-file)
                 ::interactive (["apropos" clojure.repl/find-doc "APROPOS"])}}


## Font
Font is just the name of the font and the size. It is only used when JFrame is used as display.

    {:font "Arial" :size 18}