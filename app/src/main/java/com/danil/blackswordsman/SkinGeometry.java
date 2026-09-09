package com.danil.blackswordsman;
import java.util.ArrayList;
/** Continuous lofted skin with two-bone blend weights, UVs and severable groups. */
public final class SkinGeometry {
 public final float[] vertices;public final short[] indices;public final float[][] bind=new float[16][16];
 private final ArrayList<Float> v=new ArrayList<Float>();private final ArrayList<Short> ix=new ArrayList<Short>();
 public SkinGeometry(GameWorld.Actor actor,boolean detail){
  CharacterSkeleton.Pose p=new CharacterSkeleton.Pose();GameWorld.Actor rest=new GameWorld.Actor();rest.height=actor.height;rest.radius=actor.radius;rest.type=actor.type;rest.speed=1;CharacterSkeleton.sample(rest,0,p);
  for(int b=0;b<16;b++)frame(p,b,0,bind[b]);int sides=detail?24:10;
  if(CharacterSkeleton.isBeast(actor.type)){blob(p.x[0],p.y[0],p.z[0],actor.radius*.67f,actor.height*.29f,actor.radius*.58f,0,actor,sides,detail?13:6);blob(p.x[1],p.y[1],p.z[1],actor.radius*.73f,actor.height*.32f,actor.radius*.80f,1,actor,sides,detail?13:6);blob(p.x[2],p.y[2],p.z[2],actor.radius*.70f,actor.height*.31f,actor.radius*.61f,2,actor,sides,detail?13:6);}else chain(p,actor,new int[]{0,1,2,3},new int[]{0,1,2},sides,detail?7:3,1);
  chain(p,actor,new int[]{3,4},new int[]{3},sides,3,.65f);
  chain(p,actor,new int[]{5,6,7},new int[]{4,5},sides,4,.86f);chain(p,actor,new int[]{8,9,10},new int[]{7,8},sides,4,.86f);
  chain(p,actor,new int[]{11,12,13},new int[]{10,11},sides,4,1);chain(p,actor,new int[]{14,15,16},new int[]{13,14},sides,4,1);
  for(int b:new int[]{3,6,9,12,15})ellipsoid(p,actor,b,sides,detail?13:6);
  orientFaces();vertices=new float[v.size()];for(int i=0;i<v.size();i++)vertices[i]=v.get(i);indices=new short[ix.size()];for(int i=0;i<ix.size();i++)indices[i]=ix.get(i);v.clear();ix.clear();
 }
 private void chain(CharacterSkeleton.Pose p,GameWorld.Actor a,int[] joints,int[] bones,int sides,int rings,float depth){
  int base=v.size()/16,total=bones.length*rings;
  for(int row=0;row<=total;row++){
   int seg=Math.min(bones.length-1,row/rings),j0=joints[seg],j1=joints[seg+1],b=bones[seg];float t=(row-seg*rings)/(float)rings;
   float radius=CharacterSkeleton.radiusFor(a,b)*(b==3?.5f:1)*profile(a,b,t);if(row==0||row==total)radius*=.88f;
   float[] f=bind[b];float cx=p.x[j0]+(p.x[j1]-p.x[j0])*t,cy=p.y[j0]+(p.y[j1]-p.y[j0])*t,cz=p.z[j0]+(p.z[j1]-p.z[j0])*t;
   int other=b;float weight=1;if(t>.65f&&seg+1<bones.length){other=bones[seg+1];weight=1-(t-.65f)/.35f*.5f;}else if(t<.35f&&seg>0){other=bones[seg-1];weight=.5f+t/.35f*.5f;}
   for(int c=0;c<=sides;c++){float theta=c*6.2831853f/sides,nx=(float)Math.cos(theta),nz=(float)Math.sin(theta);float xx=f[0]*nx+f[8]*nz,yy=f[1]*nx+f[9]*nz,zz=f[2]*nx+f[10]*nz;put(cx+radius*(f[0]*nx+f[8]*nz*depth),cy+radius*(f[1]*nx+f[9]*nz*depth),cz+radius*(f[2]*nx+f[10]*nz*depth),xx,yy,zz,1,1,1,c/(float)sides,row/(float)total+SurfaceLibrary.actor(a.type,b)*2,b,other,weight,CharacterSkeleton.GROUP[b]);}
  }
  grid(base,total,sides);
  cap(base,sides,bones[0],a,false);cap(base+total*(sides+1),sides,bones[bones.length-1],a,true);
 }
 private void cap(int ring,int sides,int bone,GameWorld.Actor a,boolean end){float cx=0,cy=0,cz=0;for(int i=0;i<sides;i++){cx+=v.get((ring+i)*16);cy+=v.get((ring+i)*16+1);cz+=v.get((ring+i)*16+2);}cx/=sides;cy/=sides;cz/=sides;float sign=end?1:-1,nx=bind[bone][4]*sign,ny=bind[bone][5]*sign,nz=bind[bone][6]*sign;int base=v.size()/16;put(cx,cy,cz,nx,ny,nz,1,1,1,.5f,.5f+SurfaceLibrary.actor(a.type,bone)*2,bone,bone,1,CharacterSkeleton.GROUP[bone]);for(int i=0;i<=sides;i++){int o=(ring+i)*16;put(v.get(o),v.get(o+1),v.get(o+2),nx,ny,nz,1,1,1,i/(float)sides,.5f+SurfaceLibrary.actor(a.type,bone)*2,bone,bone,1,CharacterSkeleton.GROUP[bone]);}for(int i=0;i<sides;i++){ix.add((short)base);ix.add((short)(base+1+i));ix.add((short)(base+2+i));}}
 private void blob(float x,float y,float z,float sx,float sy,float sz,int bone,GameWorld.Actor a,int sides,int rings){int base=v.size()/16;for(int j=0;j<=rings;j++){float phi=j*(float)Math.PI/rings,ny=(float)Math.cos(phi),rr=(float)Math.sin(phi);for(int i=0;i<=sides;i++){float theta=i*6.2831853f/sides,nx=rr*(float)Math.cos(theta),nz=rr*(float)Math.sin(theta);put(x+nx*sx,y+ny*sy,z+nz*sz,nx/sx,ny/sy,nz/sz,1,1,1,i/(float)sides,j/(float)rings+SurfaceLibrary.actor(a.type,bone)*2,bone,bone,1,CharacterSkeleton.GROUP[bone]);}}grid(base,rings,sides);}
 private void ellipsoid(CharacterSkeleton.Pose p,GameWorld.Actor a,int b,int sides,int rings){int base=v.size()/16,end=CharacterSkeleton.END[b];float radius=CharacterSkeleton.radiusFor(a,b);boolean skin=!CharacterSkeleton.isBeast(a.type)&&(b==3||b==6||b==9);float cr=skin?3.5f:1,cg=skin?2.8f:1,cb=skin?2.3f:1;
  for(int y=0;y<=rings;y++){float phi=y*(float)Math.PI/rings,ny=(float)Math.cos(phi),r=(float)Math.sin(phi);for(int c=0;c<=sides;c++){float theta=c*6.2831853f/sides,nx=r*(float)Math.cos(theta),nz=r*(float)Math.sin(theta);float stretch=b==3?(CharacterSkeleton.isBeast(a.type)?.9f:a.type==GameWorld.ALIEN?1.55f:1.20f):b==12||b==15?.58f:.85f; float jaw=b==3?(.84f+.16f*Math.max(0,ny)):1;put(p.x[end]+nx*radius*jaw,p.y[end]+ny*radius*stretch,p.z[end]+nz*radius*(b==12||b==15?1.6f:b==3&&CharacterSkeleton.isBeast(a.type)?1.85f:1),nx,ny,nz,cr,cg,cb,c/(float)sides,y/(float)rings+SurfaceLibrary.actor(a.type,b)*2,b,b,1,CharacterSkeleton.GROUP[b]);}}
  int first=ix.size();grid(base,rings,sides);for(int i=first;i<ix.size();i+=3){short tmp=ix.get(i+1);ix.set(i+1,ix.get(i+2));ix.set(i+2,tmp);}
 }
 private static float profile(GameWorld.Actor a,int b,float t){float shape=1;if(b==0)shape=1.02f-.25f*t;else if(b==1)shape=.76f+.34f*t;else if(b==2)shape=1.12f-.35f*t;else if(b==4||b==7)shape=.92f+.30f*(float)Math.sin(t*Math.PI)-.23f*t;else if(b==5||b==8)shape=1.02f-.35f*t;else if(b==10||b==13)shape=1.10f-.36f*t+.18f*(float)Math.sin(t*Math.PI);else if(b==11||b==14)shape=.76f+.40f*(float)Math.sin(t*Math.PI)-.18f*t;if(a.type==GameWorld.ALIEN)shape*=b<3?.77f:.68f;if(a.type==GameWorld.EXECUTIONER||a.type==GameWorld.COUNT_APOSTLE)shape*=b<3?1.25f:1.1f;return shape*(1+((a.type+2)%5-2)*.025f);}
 private void orientFaces(){for(int i=0;i<ix.size();i+=3){int a=(ix.get(i)&65535)*16,b=(ix.get(i+1)&65535)*16,c=(ix.get(i+2)&65535)*16;float x=v.get(b)-v.get(a),y=v.get(b+1)-v.get(a+1),z=v.get(b+2)-v.get(a+2),xx=v.get(c)-v.get(a),yy=v.get(c+1)-v.get(a+1),zz=v.get(c+2)-v.get(a+2);float dot=(y*zz-z*yy)*(v.get(a+3)+v.get(b+3)+v.get(c+3))+(z*xx-x*zz)*(v.get(a+4)+v.get(b+4)+v.get(c+4))+(x*yy-y*xx)*(v.get(a+5)+v.get(b+5)+v.get(c+5));if(dot<0){short tmp=ix.get(i+1);ix.set(i+1,ix.get(i+2));ix.set(i+2,tmp);}}}
 private void put(float x,float y,float z,float nx,float ny,float nz,float r,float g,float b,float u,float vv,int b0,int b1,float weight,int group){float[] a={x,y,z,nx,ny,nz,r,g,b,u,vv,b0,b1,weight,1-weight,group};for(float f:a)v.add(f);}
 private void grid(int base,int rows,int cols){for(int r=0;r<rows;r++)for(int c=0;c<cols;c++){int a=base+r*(cols+1)+c,b=a+cols+1;for(int i:new int[]{a,b,a+1,a+1,b,b+1})ix.add((short)i);}}
 public static void frame(CharacterSkeleton.Pose p,int b,float yaw,float[] out){int s=CharacterSkeleton.START[b],e=CharacterSkeleton.END[b];float dx=p.x[e]-p.x[s],dy=p.y[e]-p.y[s],dz=p.z[e]-p.z[s],len=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);if(len<.001f){dx=0;dy=1;dz=0;len=1;}dx/=len;dy/=len;dz/=len;
  float rx=(float)Math.cos(yaw),ry=0,rz=-(float)Math.sin(yaw),dot=rx*dx+rz*dz;rx-=dot*dx;ry-=dot*dy;rz-=dot*dz;float l=(float)Math.sqrt(rx*rx+ry*ry+rz*rz);if(l<.01f){rx=0;ry=dz;rz=-dy;l=(float)Math.sqrt(ry*ry+rz*rz);}rx/=l;ry/=l;rz/=l;
  out[0]=rx;out[1]=ry;out[2]=rz;out[3]=0;out[4]=dx;out[5]=dy;out[6]=dz;out[7]=0;out[8]=ry*dz-rz*dy;out[9]=rz*dx-rx*dz;out[10]=rx*dy-ry*dx;out[11]=0;out[12]=p.x[s];out[13]=p.y[s];out[14]=p.z[s];out[15]=1;
 }
 public void palette(CharacterSkeleton.Pose p,float yaw,float[] out){float[] f=new float[16];for(int b=0;b<16;b++){frame(p,b,yaw,f);float[] rest=bind[b];int o=b*16;for(int c=0;c<3;c++)for(int row=0;row<3;row++)out[o+c*4+row]=f[row]*rest[c]+f[4+row]*rest[4+c]+f[8+row]*rest[8+c];out[o+3]=out[o+7]=out[o+11]=0;for(int row=0;row<3;row++)out[o+12+row]=f[12+row]-out[o+row]*rest[12]-out[o+4+row]*rest[13]-out[o+8+row]*rest[14];out[o+15]=1;}}
 public void paletteBodies(java.util.ArrayList<PhysicsWorld.Body> bodies,GameWorld.Actor actor,float[] out){
  for(PhysicsWorld.Body body:bodies){if(body.owner!=actor||body.bone<0)continue;int bone=body.bone,o=bone*16;float[] rest=bind[bone],axis=body.axis;for(int c=0;c<3;c++)for(int row=0;row<3;row++)out[o+c*4+row]=axis[row]*rest[c]+axis[3+row]*rest[4+c]+axis[6+row]*rest[8+c];out[o+3]=out[o+7]=out[o+11]=0;float[] center={body.x,body.y,body.z};for(int row=0;row<3;row++)out[o+12+row]=center[row]-axis[3+row]*body.skinOffset-out[o+row]*rest[12]-out[o+4+row]*rest[13]-out[o+8+row]*rest[14];out[o+15]=1;}
 }

}
