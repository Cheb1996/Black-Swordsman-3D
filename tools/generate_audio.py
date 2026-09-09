#!/usr/bin/env python3
"""Original deterministic synthesis; no sampled or third-party recordings."""
import math,random,wave,struct
from pathlib import Path
out=Path(__file__).resolve().parents[1]/'app/src/main/assets/audio'
out.mkdir(parents=True,exist_ok=True)
for name,duration in [('slash',.26),('hit',.22),('cannon',.9),('dodge',.24),('rage',1.1),('page',.2),('boss',1.5),('grass',.16),('stone',.12),('wood',.19),('ambience',8.)]:
 rng=random.Random(name);samples=[];sr=22050;last=0
 for i in range(int(sr*duration)):
  t=i/sr;q=t/duration;n=rng.uniform(-1,1);last=.9*last+.1*n
  if name=='ambience':v=(math.sin(t*2*math.pi*55)+.6*math.sin(t*2*math.pi*82.5)+last*4)*.09*math.sin(math.pi*q)**2
  elif name in ('slash','dodge','page'):v=(n-last)*math.sin(math.pi*q)**2*(.35 if name=='slash' else .16)
  elif name in ('stone','grass','wood'):v=(n*.28+math.sin(t*2*math.pi*(260 if name=='wood' else 160))*.35)*math.exp(-q*9)*(.5 if name=='stone' else .32)
  elif name=='cannon':v=(last*3+math.sin(2*math.pi*(80*t-30*t*t))*.5)*math.exp(-q*6)
  elif name=='hit':v=(n*.65+math.sin(2*math.pi*110*t)*.3)*math.exp(-q*12)
  else:v=(math.sin(2*math.pi*(70*t+23*t*t))+.4*math.sin(2*math.pi*104*t)+last)*math.sin(math.pi*q)**2*.23
  samples.append(struct.pack('<h',int(max(-.95,min(.95,v))*32767)))
 with wave.open(str(out/(name+'.wav')),'wb') as f:f.setparams((1,2,sr,0,'NONE','not compressed'));f.writeframes(b''.join(samples))
