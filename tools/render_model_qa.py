from pathlib import Path
import numpy as np,struct
from PIL import Image,ImageDraw,ImageFont
root=Path(__file__).resolve().parents[1]/'build/model-qa'
im=Image.new('RGB',(1280,660),(20,24,30));d=ImageDraw.Draw(im);font=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',19)
cam=np.array([2.,1.2,5.]);cam/=np.linalg.norm(cam);right=np.cross([0,1,0],cam);right/=np.linalg.norm(right);up=np.cross(cam,right);R=np.array([right,up,cam]);light=np.array([-.4,.8,.5]);light/=np.linalg.norm(light)
labels=['Idle skin','Heavy strike','Walking skin','Beast skin']
for i in range(4):
 raw=(root/f'{i}.bin').read_bytes();nv,ni=struct.unpack('>ii',raw[:8]);v=np.frombuffer(raw,dtype='>f4',count=nv,offset=8).reshape(-1,16);ix=np.frombuffer(raw,dtype='>u2',count=ni,offset=8+nv*4).reshape(-1,3)
 pos=v[:,:3]@R.T;norm=v[:,3:6]@R.T;tri=pos[ix];nn=norm[ix].mean(axis=1);col=v[ix,6:9].mean(axis=1);shade=np.clip(nn@light,0,1)*.7+.3
 for k in np.argsort(tri[:,:,2].mean(axis=1)):
  if nn[k,2]<=0:continue
  pts=[(float(x*200+i*320+160),float(500-y*200)) for x,y,z in tri[k]];c=np.clip(col[k]*shade[k]*np.array([75,90,104]),0,255).astype(int);d.polygon(pts,fill=tuple(c))
 d.text((i*320+32,570),labels[i],font=font,fill=(226,228,235))
d.text((32,27),'v6 actual weighted geometry / host preview without armor or game lighting',font=font,fill=(220,224,232));im.save(root/'weighted-models.png')
