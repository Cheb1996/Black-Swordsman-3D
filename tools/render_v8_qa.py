from pathlib import Path
import struct
import numpy as np
from PIL import Image,ImageDraw,ImageFont
root=Path(__file__).resolve().parents[1]
font=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',16)
small=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',12)
cam=np.array([1.4,.65,4.]);cam/=np.linalg.norm(cam);right=np.cross([0,1,0],cam);right/=np.linalg.norm(right);up=np.cross(cam,right);R=np.array([right,up,cam]);light=np.array([-.4,.8,.5]);light/=np.linalg.norm(light)
def mesh(path,stride):
    raw=path.read_bytes();nv,ni=struct.unpack('>ii',raw[:8]);assert len(raw)==8+nv*4+ni*2,(path,len(raw),nv,ni)
    return np.frombuffer(raw,dtype='>f4',count=nv,offset=8).reshape(-1,stride),np.frombuffer(raw,dtype='>u2',count=ni,offset=8+nv*4).reshape(-1,3)
def tile(draw,v,ix,x,y,w,h):
    pos=v[:,:3]@R.T;norm=v[:,3:6]@R.T;lo=pos.min(axis=0);hi=pos.max(axis=0);scale=min((w-25)/max(.1,hi[0]-lo[0]),(h-52)/max(.1,hi[1]-lo[1]));tri=pos[ix];nn=norm[ix].mean(axis=1);col=v[ix,6:9].mean(axis=1);shade=np.clip(nn@light,0,1)*.7+.3
    for k in np.argsort(tri[:,:,2].mean(axis=1)):
        if nn[k,2]<=0:continue
        pts=[(float((xx-(lo[0]+hi[0])*.5)*scale+x+w*.5),float(y+h-36-(yy-lo[1])*scale)) for xx,yy,z in tri[k]]
        c=np.clip(np.power(np.maximum(col[k],0),.65)*shade[k]*225,0,255).astype(int);draw.polygon(pts,fill=tuple(c))
labels=['Гатс: покой','Гатс: шаг','Тяжёлый замах','Полёт','Гончая','Змеиный барон','Граф','Бивневый кабан','Болотный плевун','Шагоход','Посетитель','Страж рощи']
im=Image.new('RGB',(1440,1050),(23,27,34));d=ImageDraw.Draw(im);d.text((25,20),'v8 — проверка скелетных оболочек и поз, без брони и освещения Android',font=font,fill=(230,232,237))
for i,name in enumerate(labels):
    v,ix=mesh(root/f'build/v8-model-qa/{i}.bin',16);x=i%4*360;y=60+i//4*325;tile(d,v,ix,x,y,360,310);d.text((x+20,y+292),name,font=font,fill=(210,215,224))
im.save(root/'verification/v8-models.png')
im=Image.new('RGB',(1440,1200),(23,27,34));d=ImageDraw.Draw(im);d.text((25,18),'v8 — 32 гриба и 16 растений: реальные запечённые сетки; просмотр вне Android',font=font,fill=(230,232,237))
for i in range(48):
    v,ix=mesh(root/f'app/src/main/assets/flora/{i}-1.bin',9);x=i%8*180;y=60+i//8*185;tile(d,v,ix,x,y,180,180);d.text((x+14,y+159),f'{i+1:02} '+('Гриб' if i<24 else 'Способность' if i<32 else 'Растение'),font=small,fill=(212,220,225))
im.save(root/'verification/v8-flora.png')
