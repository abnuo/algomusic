data = []
pointer = 0
def Data(values:list):
  global data
  data = data + values
def Read():
  global pointer
  pointer=(pointer+1)%len(data)
  return data[pointer]
def Restore():
  global pointer
  pointer=0

#C6 pitches *10
hz = [
  10465.0, #C
  11087.3,
  11746.6, # D
  12445.1, # D#
  13185.1, # E
  13969.1, # F
  14799.8,# F#
  15679.8,# G
  16612.2,# G#
  17600.0,# A
  18646.6,# A#
  19755.3,# B
]

# set up constants
ROOT = 0
PER_2ND = 2
MIN_3RD = 3
MAJ_3RD = 4
PER_4TH = 5
DIM_5TH = 6
PER_5TH = 7
PER_6TH = 9
MIN_7TH = 10
MAJ_7th = 11
PER_12TH = 12

DEFAULT_PATH = "music/"

# set up chord structures

MINOR=0
Data([MIN_3RD,PER_5TH,PER_12TH])
MAJOR=1
Data([MAJ_3RD,PER_5TH,PER_12TH])
DIMINISHED=2
Data([MIN_3RD,DIM_5TH,PER_12TH])
SUSPENDED=3
Data([PER_4TH,PER_5TH,PER_12TH])
MINOR_7TH=4
Data([MAJ_3RD,PER_5TH,MIN_7TH])
Data([0,0,0])
