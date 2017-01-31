# Test Cases t000100

## Test Case t000100r001a Remapping a key in the Global Keymap 

### ID
t000100r001a

### Name
Remapping a key in the Global Keymap

### Description

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

## Test Case t000100r001b Remapping existing key in the Global Keymap 

### ID
t000100r001b

### Name
Remapping a key in the Global Keymap

### Description

### Prerequisites
t000100r001a

### Procedure
1. Assume t000100r001a has just been executed and that Liquid is still open
2. Type: (editor/set-global-key :C-z editor/beginning-of-line)
3. Move cursor into the expression above and click e to evaluate
4. Navigate to somewhere not at the beginning of a line
5. Press C-z
6. Observe: The cursor move to the beginning of the line
