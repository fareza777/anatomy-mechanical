import numpy as np, wave
from pathlib import Path
P=Path(__file__).resolve().parents[1]/'remotion'/'public'
SR=44100;duration=36;n=SR*duration;t=np.arange(n)/SR
rng=np.random.default_rng(1729);music=np.zeros(n)
for start,end,notes in [(0,6,[110,164.81,220,261.63,329.63]),(6,16,[87.31,130.81,174.61,220,261.63]),(16,26,[98,146.83,196,246.94,293.66]),(26,36,[110,164.81,220,261.63,329.63])]:
 a=int(start*SR);b=int(end*SR);tt=np.arange(b-a)/SR;env=np.minimum(tt/1.3,1)*np.minimum((end-start-tt)/1.5,1)
 for f in notes:music[a:b]+=.024*env*(np.sin(2*np.pi*f*tt)+.18*np.sin(2*np.pi*f*2*tt))
for k in range(68):
 a=int((2+k*.5)*SR);length=min(int(.38*SR),n-a)
 if length<=0:continue
 tt=np.arange(length)/SR
 if k%2==0:music[a:a+length]+=.17*np.sin(2*np.pi*(45*tt+45*.05*(1-np.exp(-tt/.05))))*np.exp(-tt*18)
 else:music[a:a+length]+=.019*rng.standard_normal(length)*np.exp(-tt*65)
 f=[440,523.25,659.25,783.99][k%4];music[a:a+length]+=.023*np.sin(2*np.pi*f*tt)*np.exp(-tt*13)
for start in [3,6,11,16,21,26,31]:
 a=int((start-.38)*SR);length=int(.65*SR);air=rng.standard_normal(length);air=np.convolve(air,np.ones(12)/12,mode='same');music[a:a+length]+=.065*air*np.sin(np.pi*np.arange(length)/length)**2
music*=np.minimum(t/1,1)*np.minimum((duration-t)/1.5,1)
stereo=np.tanh(np.stack([music,.97*music+.03*np.roll(music,330)],axis=-1)*1.1)*.82
with wave.open(str(P/'original-score.wav'),'wb') as w:w.setnchannels(2);w.setsampwidth(2);w.setframerate(SR);w.writeframes((stereo*32767).astype('<i2').tobytes())
print('Original 36-second stereo score written.')
