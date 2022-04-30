#Ported from https://github.com/blitz-research/blitzmax/blob/master/mod/brl.mod/random.mod/random.bmx
rnd_state = 0x1234
RND_A=48271
RND_M=2147483647
RND_Q=44488
RND_R=3399

#bbdoc: Generate random float
#returns: A random float in the range 0 (inclusive) to 1 (exclusive)
def RndFloat():
  global rnd_state
  rnd_state=RND_A*(rnd_state%RND_Q)-RND_R*(rnd_state/RND_Q)
  if rnd_state<0:
    rnd_state=rnd_state+RND_M
  return (rnd_state & 0xffffff0) / 268435456
	#divide by 2^28

#bbdoc: Generate random double
#returns: A random double in the range 0 (inclusive) to 1 (exclusive)
def RndDouble():
  global rnd_state
  TWO27 = 134217728.0		#2 ^ 27
  TWO29 = 536870912.0		#2 ^ 29
  rnd_state=RND_A*(rnd_state%RND_Q)-RND_R*(rnd_state/RND_Q)
  if rnd_state<0:
	  rnd_state=rnd_state+RND_M
  r_hi = int(rnd_state) & 0x1ffffffc
  rnd_state=RND_A*(rnd_state%RND_Q)-RND_R*(rnd_state/RND_Q)
  if rnd_state<0:
    rnd_state=rnd_state+RND_M
  r_lo = int(rnd_state) & 0x1ffffff8
  return (r_hi + r_lo/TWO27)/TWO29

#bbdoc: Generate random double
#returns: A random double in the range min (inclusive) to max (exclusive)
#about: 
#The optional parameters allow you to use Rnd in 3 ways:
#[ @Format | @Result
#* &Rnd() | Random double in the range 0 (inclusive) to 1 (exclusive)
#* &Rnd(_x_) | Random double in the range 0 (inclusive) to n (exclusive)
#* &Rnd(_x,y_) | Random double in the range x (inclusive) to y (exclusive)
#]
def Rnd( min_value=1,max_value=0 ):
  if max_value>min_value:
	  return RndDouble()*(max_value-min_value)+min_value
  return RndDouble()*(min_value-max_value)+max_value

#bbdoc: Generate random integer
#returns: A random integer in the range min (inclusive) to max (inclusive)
#about:
#The optional parameter allows you to use #Rand in 2 ways:
#[ @Format | @Result
#* &Rand(x) | Random integer in the range 1 to x (inclusive)
#* &Rand(x,y) | Random integer in the range x to y (inclusive)
#]
def Rand( min_value,max_value=1 ):
  range=max_value-min_value
  if range>0:
    return int( RndDouble()*(1+range) )+min_value
  return int( RndDouble()*(1-range) )+max_value

#bbdoc: Set random number generator seed
def SeedRnd( seed=0 ):
  global rnd_state
  rnd_state=seed & 0x7fffffff
  #enforces rnd_state >= 0
  if rnd_state==0 or rnd_state==RND_M:
    rnd_state=0x1234	#disallow 0 and M

#bbdoc: Get random number generator seed
#returns: The current random number generator seed
#about: Use in conjunction with SeedRnd, RndSeed allows you to reproduce sequences of random
#numbers.
def RndSeed():
  return rnd_state

def choice(list):
  return list[Rand(0,len(list)-1)]
