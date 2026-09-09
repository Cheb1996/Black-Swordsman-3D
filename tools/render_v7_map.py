from pathlib import Path
import struct, re
import numpy as np
from PIL import Image,ImageDraw,ImageFont
root=Path(__file__).resolve().parents[1]
raw=(root/'build/v7-world.bin').read_bytes();n=struct.unpack('>i',raw[:4])[0];a=np.frombuffer(raw,dtype='>f4',offset=4).reshape(n,n,4);h,depth,desert,mountain=a[:,:,0],a[:,:,1],a[:,:,2],a[:,:,3]
color=np.zeros((n,n,3));color[:]=[62,91,63];color=color*(1-desert[:,:,None])+np.array([160,133,82])*desert[:,:,None];color=color*(1-mountain[:,:,None])+np.array([112,119,115])*mountain[:,:,None]
y,x=np.gradient(h);shade=np.clip(1+x*.1-y*.1,.65,1.22);color*=shade[:,:,None]
t=np.clip(depth/12,0,1);wet=np.stack([30-t*14,133-t*64,145-t*47],axis=2);color[depth>.02]=wet[depth>.02]
im=Image.new('RGB',(1120,1050),(18,25,33));im.paste(Image.fromarray(np.uint8(np.clip(color,0,255))).resize((900,900),Image.Resampling.BILINEAR),(30,100));d=ImageDraw.Draw(im);font='/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf';title=ImageFont.truetype(font,24);small=ImageFont.truetype(font,14)
d.text((30,25),'v7 — фактический рельеф и водоёмы',font=title,fill=(235,239,242));d.text((30,63),'898 × 898 м • полоса океана доступна для плавания • вид сверху, без игрового освещения',font=small,fill=(173,190,196))
s=(root/'app/src/main/java/com/danil/blackswordsman/RegionLayout.java').read_text();block=s.split('PLACES=')[1].split(';')[0];places=re.findall(r'\{(-?[\d.]+),(-?[\d.]+),(\d)\}',block);half=142*10**.5+160
colors=[(239,211,137),(199,156,240),(137,218,87),(227,164,113),(151,224,238),(242,134,134)]
for xx,zz,k in places:
 px=30+(float(xx)+half)/(2*half)*900;py=100+(half-float(zz))/(2*half)*900;d.ellipse((px-4,py-4,px+4,py+4),fill=colors[int(k)],outline=(20,25,30))
for i,name in enumerate(['Поселение','Пещера','Оазис','Башня','Причал','Мастерская']):d.rectangle((950,145+i*42,960,155+i*42),fill=colors[i]);d.text((971,140+i*42),name,font=small,fill=(220,227,231))
d.text((35,1013),'Из WorldLayout / WaterField / RegionLayout. Центральный маршрут кампании проходит с юга на север.',font=small,fill=(183,197,202))
im.save(root/'verification/v7-world-map.png')
