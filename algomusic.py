import threading
import time
import sys
import os
import random
import blitzrand
import glob
import struct
import math
import io
import numpy as np
from midiutil import MIDIFile
import wave
try:
  import sound
except:
  import sounddevice as sd
  import soundfile as sf
import nltk
try:
  from nltk.corpus import words
except:
  nltk.download('words')

notes = ["c","d","e","f","g"]
pitchlists = {
  "normal": [1.0,1.10,1.25,1.30,1.45],
  "microtonal": [1.0,1.1,1.2,1.3,1.4],
  "microtonal2": [1.0,1.2,1.4,1.6,1.8]
}
pitches = pitchlists["normal"]
delays = [0.2]
soundsdir = "music"
sounds = [i.replace("\\","/") for i in glob.glob(f"{soundsdir}/*.wav")]
mmlinstr = {
  f"{soundsdir}/bass1.wav": "@4 @n0 @E1,0,5,0,0",
  f"{soundsdir}/bass2.wav": "@4 @n90 @E1,0,5,0,0",
  f"{soundsdir}/bass3.wav": "@4 @n105 @E1,0,50,0,20",
  f"{soundsdir}/click2.wav": "@4 @n0 @E1,0,5,0,20",
  f"{soundsdir}/click3.wav": "@4 @n0 @E1,0,5,0,20",
  f"{soundsdir}/click4.wav": "@4 @n105 @E1,0,5,0,20",
  f"{soundsdir}/snare1.wav": "@4 @n0 @E1,0,2,0,20",
  f"{soundsdir}/snare2.wav": "@4 @n0 @E1,0,15,0,20",
  f"{soundsdir}/snare3.wav": "@4 @n0 @E1,0,15,0,20",
  f"{soundsdir}/sound1.wav": "@4 @n0 @E1,0,5,0,0",
  f"{soundsdir}/sound2.wav": "@4 @n0 @E1,0,5,0,0",
  f"{soundsdir}/sound3.wav": "@4 @n0 @E1,0,5,0,0",
  f"{soundsdir}/sound4.wav": "@4 @n120 @E1,0,50,0,25",
  f"{soundsdir}/taik2.wav": "@4 @n120 @E1,0,50,0,15",
  f"{soundsdir}/01_wooooqq.wav": "@2 @E1,50,50,0,150 <",
  f"{soundsdir}/02_hibuzzbipqq.wav": "@1 @E1,0,100,0,0",
  f"{soundsdir}/04_tik2qq.wav": "@3 @E1,0,5,0,0 l8 <",
  f"{soundsdir}/05_snq.wav": "@4 @n0 @E1,0,50,0,0",
  f"{soundsdir}/05_tribipqq.wav": "@3 @E1,0,5,0,0",
  f"{soundsdir}/06_tangcutqq.wav": "@1 @E1,0,50,0,50",
  f"{soundsdir}/07_tribeepcutqqwav.wav": "@3 @E1,0,50,0,0",
  f"{soundsdir}/celest.wav": "@2 @E1,0,100,0,50 <",
  f"{soundsdir}/churchstring.wav": "@13 @E1,0,100,0,250",
  f"{soundsdir}/glock.wav": "@2 @E1,0,100,0,50",
  f"{soundsdir}/harp.wav": "@1 @E1,20,100,0,150",
  f"{soundsdir}/koto.wav": "@13 @E1,20,100,0,150",
  f"{soundsdir}/mutedguit.wav": "@1 @E1,0,100,0,50",
  f"{soundsdir}/nylonguit.wav": "@13 @E1,0,100,0,50",
  f"{soundsdir}/organ1.wav": "@2 @E1,0,100,0,250",
  f"{soundsdir}/piano1.wav": "@3 @E1,0,100,0,250",
  f"{soundsdir}/piano3.wav": "@3 @E1,0,100,0,250",
  f"{soundsdir}/pizzicato.wav": "@1 @E1,5,10,50,50",
  f"{soundsdir}/tremolo.wav": "@1 @E1,5,100,0,250",
  f"{soundsdir}/violin.wav": "@3 @E1,5,100,0,250"
}
midinstr = {
  f"{soundsdir}/bass1.wav": 118,
  f"{soundsdir}/bass2.wav": 118,
  f"{soundsdir}/bass3.wav": 117,
  f"{soundsdir}/click2.wav": 115,
  f"{soundsdir}/click3.wav": 115,
  f"{soundsdir}/click4.wav": 115,
  f"{soundsdir}/snare1.wav": 118,
  f"{soundsdir}/snare2.wav": 118,
  f"{soundsdir}/snare3.wav": 118,
  f"{soundsdir}/sound1.wav": 115,
  f"{soundsdir}/sound2.wav": 155,
  f"{soundsdir}/sound3.wav": 155,
  f"{soundsdir}/sound4.wav": 155,
  f"{soundsdir}/taik2.wav": 116,
  f"{soundsdir}/01_wooooqq.wav": 73,
  f"{soundsdir}/02_hibuzzbipqq.wav": 81,
  f"{soundsdir}/04_tik2qq.wav": 80,
  f"{soundsdir}/05_snq.wav": 115,
  f"{soundsdir}/05_tribipqq.wav": 82,
  f"{soundsdir}/06_tangcutqq.wav": 81,
  f"{soundsdir}/07_tribeepcutqqwav.wav": 82,
  f"{soundsdir}/bell2.wav": 14,
  f"{soundsdir}/celest.wav": 8,
  f"{soundsdir}/churchstring.wav": 48,
  f"{soundsdir}/glock.wav": 9,
  f"{soundsdir}/harp.wav": 46,
  f"{soundsdir}/koto.wav": 107,
  f"{soundsdir}/mutedguit.wav": 28,
  f"{soundsdir}/nylonguit.wav": 24,
  f"{soundsdir}/organ1.wav": 16,
  f"{soundsdir}/piano1.wav": 0,
  f"{soundsdir}/pickedbass.wav": 34,
  f"{soundsdir}/mutedguit.wav": 28,
  f"{soundsdir}/pizzicato.wav": 45,
  f"{soundsdir}/tremolo.wav": 44,
  f"{soundsdir}/violin.wav": 40
}
midinotes = [72,74,76,77,79]

def hashstr(s):
  hash=0
  for ch in s:
    hash = (hash*281 ^ ord(ch)*997) & 0xFFFFFFFF
  return hash
def speedx(sound_array,factor):
  indices = np.round(np.arange(0, len(sound_array),factor))
  indices = indices[indices<len(sound_array)].astype(np.int16)
  return sound_array[indices.astype(int)]
def play2(song,instrument,speed,chn=0):
  #Playback is choppy
  for i,v in enumerate(song):
    if not v == "r":
      note = notes.index(v)
      #note = 4
      instr = wave.open(instrument,"rb")
      data = io.BytesIO(instr.readframes(instr.getnframes()))
      #data = [struct.unpack("<h",bytes([v,data[i+1]]))[0] for i,v in enumerate(data[:-1])]
      data,sr = sf.read(data,channels=1,samplerate=44100,dtype="int16",subtype="PCM_16",endian="LITTLE",format="RAW")
      sd.play(data,sr*pitches[note],1)
    time.sleep(random.choice(delays)/speed)
    try:
      pass
    except:
      pass
def play(song,instrument,speed,chn=0):
  for i,v in enumerate(song):
    if not v == "r":
      note = notes.index(v)
      #note = 4
      effect = sound.play_effect(instrument,pitch=pitches[note])
    time.sleep(random.choice(delays)/speed)
    try:
      sound.stop_effect(effect)
    except:
      pass
def generate(seed):
  song = []
  if not type(seed) == int:
    blitzrand.SeedRnd(hashstr(seed))
  else:
    blitzrand.SeedRnd(seed)
  title = seed
  speed = blitzrand.Rnd(0.5,2.0)
  for i in range(4):
    cinst = blitzrand.choice(sounds)
    snotes = []
    for i in range(100):
      rest = blitzrand.Rand(0,1)
      if not rest == 1:
        note = blitzrand.choice(notes)
        snotes.append(note)
      else:
        snotes.append("r")
    song.append((cinst,snotes))
  return (speed,song)
def printinfo(song):
  print(f"Song entitled \"{title}\"")
  print(f"Speed: {song[0]} ({round(song[0]*150)} BPM)")
  for i in song[1]:
    print(i[0])
def playsong(song,speed,func=play):
  threads = []
  for i,v in enumerate(song):
    x = threading.Thread(target=func,args=(v[1],v[0],speed,i))
    threads.append(x)
    x.start()
  return threads
def tomml(song):
  mml = f"t{round(song[0]*150)} l8 "
  for i in song[1]:
    if i[0] in mmlinstr:
      mml += mmlinstr[i[0]]+" "
    mml += "<"+"".join(i[1])+";"
  return mml[:-1]
def tomidi(song):
  midi = MIDIFile(1,adjust_origin=True)
  midi.addTempo(0,0,round(song[0]*65))
  for i,v in enumerate(song[1]):
    if v[0] in midinstr:
      midi.addProgramChange(0,i,0,midinstr[v[0]])
    else:
      midi.addProgramChange(0,i,0,0)
    t = 0
    for x,n in enumerate(v[1]):
      if not n == "r":
        midi.addNote(0,i,midinotes[notes.index(n)],t,0.25,127)
      t += 0.25
  return midi
def gentitle():
  return random.choice(words.words()).capitalize()+" "+random.choice(words.words()).capitalize()

if __name__ == "__main__":
  try:
    title = sys.argv[1]
  except:
    title = gentitle()
  try:
    sound.stop_all_effects()
  except:
    pass
  if sys.argv[0].startswith("/private/"):
    title = input("> ")
    if title == "":
      title = gentitle()
  song = generate(title)
  printinfo(song)
  try:
    sound
    threads = playsong(song[1],song[0])
  except:
    threads = playsong(song[1],song[0],play2)
  print(tomml(song))
  midi = tomidi(song)
  fn = f"{str(int(blitzrand.RndSeed())&0xffff).zfill(5)}.mid"
  save = input("Save? (Y/N)")
  if save.upper() == "Y":
    with open(fn,"wb") as f:
      midi.writeFile(f)
    print("Saved to",fn)
  for x in threads:
    x.join()
