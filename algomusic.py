import threading
import time
import sys
import os
import random
import glob
try:
  import sound
except:
  pass
import nltk
try:
  from nltk.corpus import words
except:
  nltk.download('words')

threads = []
song = []
notes = ["c","d","e","f","g"]
pitchlists = {
  "normal": [1.0,1.10,1.25,1.30,1.45],
  "microtonal": [1.0,1.1,1.2,1.3,1.4],
  "microtonal2": [1.0,1.2,1.4,1.6,1.8]
}
pitches = pitchlists["normal"]
delays = [0.2]
soundsdir = "music"
sounds = glob.glob(f"{soundsdir}/*.wav")
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
try:
  title = sys.argv[1]
except:
  title = random.choice(words.words()).capitalize()+" "+random.choice(words.words()).capitalize()
speed = 1

def play(song,instrument,speed):
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
  global title,speed
  random.seed(seed)
  title = seed
  speed = random.uniform(0.5,2.0)
  for i in range(4):
    cinst = random.choice(sounds)
    snotes = []
    for i in range(100):
      rest = random.randint(0,1)
      if not rest == 1:
        note = random.choice(notes)
        snotes.append(note)
      else:
        snotes.append("r")
    song.append((cinst,snotes))
def printinfo():
  print(f"Song entitled \"{title}\"")
  print(f"Speed: {speed} ({round(speed*150)} BPM)")
  for i in song:
    print(i[0])
def playsong():
  for i in song:
    x = threading.Thread(target=play,args=(i[1],i[0],speed))
    threads.append(x)
    x.start()
def tomml(song):
  mml = f"t{round(speed*150)} l8 "
  for i in song:
    if i[0] in mmlinstr:
      mml += mmlinstr[i[0]]+" "
    mml += "<"+"".join(i[1])+";"
  return mml[:-1]

if __name__ == "__main__":
  sound.stop_all_effects()
  generate(title)
  printinfo()
  try:
    playsong()
  except:
    pass
  print(tomml(song))
  for x in threads:
    x.join()
