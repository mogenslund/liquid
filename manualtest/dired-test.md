# Dired test

## Exit dired mode
:dired-test/exit-dired-mode
Type ":e ." to enter dired mode.
Typing [Esc] should exit dired mode and return to previous buffer. 

## Search in dired
:dired-test/search-in-dired
Type ":e ." to enter dired mode. Search by type "/<searchfrase>[Enter]"
The cursor should move to the first hit of "<searchfrase>".

## Open folder in dired
:dired-test/open-folder-in-dired
Type ":e ." to enter dired mode. Folders are colered in blue.
Press [Enter] on a folder.
The dired mode should change to view the content of the clicked folder.
