# Test Cases t000100

## Test Case t000100r001a Remapping a key in the Global Keymap 

### ID
t000100r001a

### Description
Remapping a key in the Global Keymap

### Procedure
1. Open Liquid Text Editor
2. Navigate to somewhere not at the end of a line
3. Press C-z
4. Observe: Nothing happens
5. Type: (editor/set-global-key :C-z editor/end-of-line)
6. Move cursor into the expression above and click e to evaluate
7. Navigate to somewhere not at the end of a line
8. Press C-z
9. Observe: The cursor move to the end of the line
