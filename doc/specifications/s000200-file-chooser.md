# Specification s000200

## ID
s000200

## Name
File Chooser

## Description
The file chooser will be called findfilemode

The file chooser should take a function as input, which will be used on the result.

There should be a general concept of hotpaths, that will always be available in the typeahead as supplement to files and folders in the current folder.

Two out of the box function should be created to use the chooser. These functions will be used as input:

1. A function that opens the choosen file
2. A function insert-path that inserts the path at the current point

### States
The file chooser will during execution have the following state attributes

    search: The current text in typeahead
    path: The current path below which search is performed
    hotpaths: List of paths that will always be part of searchlist
    selected: The index of the selected item
    hit: The current hit, which may be the path to the selected item or path + search

### UI
Here is a sample of what the screen might look like:

    /tmp/myfolder
    myf|
    ----------
        [myfolder2]
        myfile.txt
    #>  /tmp/myfolder/someotherfile.txt
        /tmp/myfolder/somethinghot

## Requirements

### r001 Current filepath and subpath as hotpaths
When the file chooser is invoked from file, the file itself and all its subpaths should be included in the hotpaths list.

So, if the file is /tmp/myfolder/myfile.txt, then

    /tmp
    /tmp/myfolder
    /tmp/myfolder/myfile.txt

should be added to hotpaths

### r002 Context as subpath
If the context around the cursor at the time the file chooser is invoked looks like a path it, and all its subpaths should be added to the hotpaths.

The basefolder of the file chooser should be the corresponding folder and typeahead should be prepopulated with the (maybe partial) filename.

So, if the context is /tmp/myfolder/myfi and the filechooser is invoked, the path should be "/tmp/myfolder/" and search should be "myfi".  

### r003 Context replace in insert-path function
If the current context is path like, e.g "/tmp/myfold" and the choosen path is a superpath, like "/tmp/myfolder/myfile.txt" than insert-path should not just insert the path, but also remove the current context. This is to be able to use insert-path to extend existing path.
