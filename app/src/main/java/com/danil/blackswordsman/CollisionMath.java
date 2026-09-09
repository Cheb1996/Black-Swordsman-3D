package com.danil.blackswordsman;

/** Shared oriented geometry for contacts, projectiles, camera and navigation. */
public final class CollisionMath {
    private CollisionMath(){}
    public static final class Contact implements java.io.Serializable {
        private static final long serialVersionUID=6L;
        public float nx,ny,nz,depth,x,y,z;
    }
    public static void basis(float rx,float ry,float rz,float[] m){
        double x=rx*Math.PI/180,y=ry*Math.PI/180,z=rz*Math.PI/180;
        float sx=(float)Math.sin(x),cx=(float)Math.cos(x),sy=(float)Math.sin(y),cy=(float)Math.cos(y),sz=(float)Math.sin(z),cz=(float)Math.cos(z);
        m[0]=cy*cz+sy*sx*sz;m[1]=cx*sz;m[2]=-sy*cz+cy*sx*sz;
        m[3]=-cy*sz+sy*sx*cz;m[4]=cx*cz;m[5]=sy*sz+cy*sx*cz;
        m[6]=sy*cx;m[7]=-sx;m[8]=cy*cx;
    }
    public static void refresh(PhysicsWorld.Body b){
        basis(b.rx,b.ry,b.rz,b.axis);
        if(b.shape==PhysicsWorld.SHAPE_SPHERE){b.ex=b.ey=b.ez=b.radius;return;}
        if(b.shape==PhysicsWorld.SHAPE_CAPSULE){float len=Math.max(0,b.hy-b.radius);b.ex=Math.abs(b.axis[3])*len+b.radius;b.ey=Math.abs(b.axis[4])*len+b.radius;b.ez=Math.abs(b.axis[5])*len+b.radius;return;}
        float[] m=b.axis;b.ex=Math.abs(m[0])*b.hx+Math.abs(m[3])*b.hy+Math.abs(m[6])*b.hz;
        b.ey=Math.abs(m[1])*b.hx+Math.abs(m[4])*b.hy+Math.abs(m[7])*b.hz;
        b.ez=Math.abs(m[2])*b.hx+Math.abs(m[5])*b.hy+Math.abs(m[8])*b.hz;
    }
    public static float segmentBox(float ax,float ay,float az,float bx,float by,float bz,PhysicsWorld.Body b,float padding){
        if(b.shape==PhysicsWorld.SHAPE_SPHERE)return segmentSphere(ax,ay,az,bx,by,bz,b.x,b.y,b.z,b.radius+padding);
        float[] m=b.axis;float x=ax-b.x,y=ay-b.y,z=az-b.z,dx=bx-ax,dy=by-ay,dz=bz-az,lo=0,hi=1;
        for(int i=0;i<3;i++){
            int k=i*3;float p=x*m[k]+y*m[k+1]+z*m[k+2],d=dx*m[k]+dy*m[k+1]+dz*m[k+2];
            float extent=(b.shape==PhysicsWorld.SHAPE_SPHERE?b.radius:i==0?b.hx:i==1?b.hy:b.hz)+padding;
            if(Math.abs(d)<1e-7f){if(p< -extent||p>extent)return Float.POSITIVE_INFINITY;continue;}
            float t1=(-extent-p)/d,t2=(extent-p)/d;if(t1>t2){float q=t1;t1=t2;t2=q;}lo=Math.max(lo,t1);hi=Math.min(hi,t2);if(lo>hi)return Float.POSITIVE_INFINITY;
        }
        return lo;
    }
    public static float segmentActor(float ax,float ay,float az,float bx,float by,float bz,GameWorld.Actor a,float radius){
        // Vertical capsule: cylinder and two spherical end caps, swept by the projectile radius.
        float r=a.radius+radius,low=a.y+Math.min(a.radius,a.height*.5f),high=a.y+a.height-Math.min(a.radius,a.height*.5f);
        float px=ax-a.x,pz=az-a.z,dx=bx-ax,dy=by-ay,dz=bz-az;
        float best=Math.min(segmentSphere(ax,ay,az,bx,by,bz,a.x,low,a.z,r),segmentSphere(ax,ay,az,bx,by,bz,a.x,high,a.z,r));
        float aa=dx*dx+dz*dz,bb=2*(px*dx+pz*dz),cc=px*px+pz*pz-r*r;
        if(cc<=0&&ay>=low&&ay<=high)best=0;
        float disc=bb*bb-4*aa*cc;if(aa>1e-9f&&disc>=0){float root=(float)Math.sqrt(disc);for(int i=0;i<2;i++){float t=(-bb+(i==0?-root:root))/(2*aa),y=ay+dy*t;if(t>=0&&t<=1&&y>=low&&y<=high)best=Math.min(best,t);}}
        return best;
    }
    public static float segmentSphere(float ax,float ay,float az,float bx,float by,float bz,float x,float y,float z,float r){
        float px=ax-x,py=ay-y,pz=az-z,dx=bx-ax,dy=by-ay,dz=bz-az,c=px*px+py*py+pz*pz-r*r;
        if(c<=0)return 0;float a=dx*dx+dy*dy+dz*dz,b=px*dx+py*dy+pz*dz,disc=b*b-a*c;
        if(a<1e-10f||disc<0)return Float.POSITIVE_INFINITY;float t=(-b-(float)Math.sqrt(disc))/a;return t>=0&&t<=1?t:Float.POSITIVE_INFINITY;
    }
    private static float extent(PhysicsWorld.Body b,float x,float y,float z){float[] m=b.axis;return Math.abs(x*m[0]+y*m[1]+z*m[2])*b.hx+Math.abs(x*m[3]+y*m[4]+z*m[5])*b.hy+Math.abs(x*m[6]+y*m[7]+z*m[8])*b.hz;}
    public static boolean contact(PhysicsWorld.Body a,PhysicsWorld.Body b,Contact c){
        if(Math.abs(a.x-b.x)>a.ex+b.ex+.005f||Math.abs(a.y-b.y)>a.ey+b.ey+.005f||Math.abs(a.z-b.z)>a.ez+b.ez+.005f)return false;
        if(a.shape==PhysicsWorld.SHAPE_CAPSULE)return capsuleContact(a,b,c,false);if(b.shape==PhysicsWorld.SHAPE_CAPSULE)return capsuleContact(b,a,c,true);
        if(a.shape==PhysicsWorld.SHAPE_SPHERE&&b.shape==PhysicsWorld.SHAPE_SPHERE){float dx=b.x-a.x,dy=b.y-a.y,dz=b.z-a.z,d=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);if(d>=a.radius+b.radius)return false;c.nx=d>.0001f?dx/d:1;c.ny=d>.0001f?dy/d:0;c.nz=d>.0001f?dz/d:0;c.depth=a.radius+b.radius-d;c.x=a.x+c.nx*(a.radius-c.depth*.5f);c.y=a.y+c.ny*(a.radius-c.depth*.5f);c.z=a.z+c.nz*(a.radius-c.depth*.5f);return true;}
        if(a.shape==PhysicsWorld.SHAPE_SPHERE)return sphereBox(a,b,c,false);
        if(b.shape==PhysicsWorld.SHAPE_SPHERE)return sphereBox(b,a,c,true);
        c.depth=Float.MAX_VALUE;float dx=b.x-a.x,dy=b.y-a.y,dz=b.z-a.z;
        for(int n=0;n<15;n++){
            float x,y,z;if(n<6){float[] m=n<3?a.axis:b.axis;int k=(n%3)*3;x=m[k];y=m[k+1];z=m[k+2];}
            else{int u=((n-6)/3)*3,v=((n-6)%3)*3;x=a.axis[u+1]*b.axis[v+2]-a.axis[u+2]*b.axis[v+1];y=a.axis[u+2]*b.axis[v]-a.axis[u]*b.axis[v+2];z=a.axis[u]*b.axis[v+1]-a.axis[u+1]*b.axis[v];}
            float len=(float)Math.sqrt(x*x+y*y+z*z);if(len<.0001f)continue;x/=len;y/=len;z/=len;
            float projection=dx*x+dy*y+dz*z,overlap=extent(a,x,y,z)+extent(b,x,y,z)-Math.abs(projection);if(overlap<0)return false;
            if(overlap<c.depth){float sign=projection<0?-1:1;c.depth=overlap;c.nx=x*sign;c.ny=y*sign;c.nz=z*sign;}
        }
        float ax=support(a,c.nx,c.ny,c.nz,0),ay=support(a,c.nx,c.ny,c.nz,1),az=support(a,c.nx,c.ny,c.nz,2);
        float bx=support(b,-c.nx,-c.ny,-c.nz,0),by=support(b,-c.nx,-c.ny,-c.nz,1),bz=support(b,-c.nx,-c.ny,-c.nz,2);
        c.x=(ax+bx)*.5f;c.y=(ay+by)*.5f;c.z=(az+bz)*.5f;manifoldPoint(a,b,c);return true;
    }
    private static void manifoldPoint(PhysicsWorld.Body a,PhysicsWorld.Body b,Contact c){float sx=0,sy=0,sz=0;int count=0;for(int which=0;which<2;which++){PhysicsWorld.Body p=which==0?a:b,q=which==0?b:a;for(int corner=0;corner<8;corner++){float x=p.x,y=p.y,z=p.z;for(int axis=0;axis<3;axis++){int k=axis*3;float extent=axis==0?p.hx:axis==1?p.hy:p.hz,sign=(corner&(1<<axis))==0?-1:1;x+=p.axis[k]*extent*sign;y+=p.axis[k+1]*extent*sign;z+=p.axis[k+2]*extent*sign;}boolean inside=true;for(int axis=0;axis<3;axis++){int k=axis*3;float d=(x-q.x)*q.axis[k]+(y-q.y)*q.axis[k+1]+(z-q.z)*q.axis[k+2],h=axis==0?q.hx:axis==1?q.hy:q.hz;if(Math.abs(d)>h+.003f){inside=false;break;}}if(inside){sx+=x;sy+=y;sz+=z;count++;}}}if(count>0){c.x=sx/count;c.y=sy/count;c.z=sz/count;}}
    private static final PhysicsWorld.Body capsulePoint=new PhysicsWorld.Body();
    private static boolean capsuleContact(PhysicsWorld.Body a,PhysicsWorld.Body b,Contact c,boolean invert){
        float length=Math.max(0,a.hy-a.radius),low=-length,high=length;
        // Squared distance to a convex primitive is convex along the capsule axis.
        for(int n=0;n<18;n++){float t1=low+(high-low)/3,t2=high-(high-low)/3;float d1=distanceSquared(a.x+a.axis[3]*t1,a.y+a.axis[4]*t1,a.z+a.axis[5]*t1,b),d2=distanceSquared(a.x+a.axis[3]*t2,a.y+a.axis[4]*t2,a.z+a.axis[5]*t2,b);if(d1<d2)high=t2;else low=t1;}
        float t=(low+high)*.5f,x=a.x+a.axis[3]*t,y=a.y+a.axis[4]*t,z=a.z+a.axis[5]*t;
        if(b.shape==PhysicsWorld.SHAPE_SPHERE||b.shape==PhysicsWorld.SHAPE_CAPSULE){float bx=b.x,by=b.y,bz=b.z;if(b.shape==PhysicsWorld.SHAPE_CAPSULE){float bl=Math.max(0,b.hy-b.radius),q=Math.max(-bl,Math.min(bl,(x-bx)*b.axis[3]+(y-by)*b.axis[4]+(z-bz)*b.axis[5]));bx+=b.axis[3]*q;by+=b.axis[4]*q;bz+=b.axis[5]*q;}float dx=bx-x,dy=by-y,dz=bz-z,d=(float)Math.sqrt(dx*dx+dy*dy+dz*dz),radius=a.radius+b.radius;if(d>=radius)return false;c.depth=radius-d;float sign=invert?-1:1;c.nx=(d>.0001f?dx/d:1)*sign;c.ny=(d>.0001f?dy/d:0)*sign;c.nz=(d>.0001f?dz/d:0)*sign;c.x=x+(d>.0001f?dx/d:1)*(a.radius-c.depth*.5f);c.y=y+(d>.0001f?dy/d:0)*(a.radius-c.depth*.5f);c.z=z+(d>.0001f?dz/d:0)*(a.radius-c.depth*.5f);return true;}
        capsulePoint.x=x;capsulePoint.y=y;capsulePoint.z=z;capsulePoint.radius=a.radius;return sphereBox(capsulePoint,b,c,invert);
    }
    private static float distanceSquared(float x,float y,float z,PhysicsWorld.Body b){float dx=x-b.x,dy=y-b.y,dz=z-b.z;if(b.shape==PhysicsWorld.SHAPE_SPHERE)return dx*dx+dy*dy+dz*dz;if(b.shape==PhysicsWorld.SHAPE_CAPSULE){float l=Math.max(0,b.hy-b.radius),q=Math.max(-l,Math.min(l,dx*b.axis[3]+dy*b.axis[4]+dz*b.axis[5]));dx-=b.axis[3]*q;dy-=b.axis[4]*q;dz-=b.axis[5]*q;return dx*dx+dy*dy+dz*dz;}float sum=0;for(int i=0;i<3;i++){int k=i*3;float d=Math.max(0,Math.abs(dx*b.axis[k]+dy*b.axis[k+1]+dz*b.axis[k+2])-(i==0?b.hx:i==1?b.hy:b.hz));sum+=d*d;}return sum;}
    private static float support(PhysicsWorld.Body b,float x,float y,float z,int component){float p=component==0?b.x:component==1?b.y:b.z;for(int i=0;i<3;i++){int k=i*3;float d=x*b.axis[k]+y*b.axis[k+1]+z*b.axis[k+2];float s=Math.abs(d)<.0001f?0:Math.signum(d);p+=b.axis[k+component]*s*(i==0?b.hx:i==1?b.hy:b.hz);}return p;}
    private static boolean sphereBox(PhysicsWorld.Body s,PhysicsWorld.Body b,Contact c,boolean invert){
        float dx=s.x-b.x,dy=s.y-b.y,dz=s.z-b.z,qx=b.x,qy=b.y,qz=b.z,inside=Float.MAX_VALUE,ix=1,iy=0,iz=0;
        for(int i=0;i<3;i++){int k=i*3;float p=dx*b.axis[k]+dy*b.axis[k+1]+dz*b.axis[k+2],h=i==0?b.hx:i==1?b.hy:b.hz,q=Math.max(-h,Math.min(h,p));qx+=q*b.axis[k];qy+=q*b.axis[k+1];qz+=q*b.axis[k+2];if(h-Math.abs(p)<inside){inside=h-Math.abs(p);float sign=p<0?-1:1;ix=b.axis[k]*sign;iy=b.axis[k+1]*sign;iz=b.axis[k+2]*sign;}}
        float x=qx-s.x,y=qy-s.y,z=qz-s.z,d=(float)Math.sqrt(x*x+y*y+z*z);if(d>=s.radius)return false;
        if(d<1e-5f){x=-ix;y=-iy;z=-iz;c.depth=s.radius+inside;}else{x/=d;y/=d;z/=d;c.depth=s.radius-d;}
        float sign=invert?-1:1;c.nx=x*sign;c.ny=y*sign;c.nz=z*sign;c.x=qx;c.y=qy;c.z=qz;return true;
    }
}
