package com.danil.blackswordsman;

/** Three-axis impulses, Coulomb friction and angular response for mobile rigid bodies. */
public final class ContactSolver {
    private ContactSolver(){}
    private static float inertia(PhysicsWorld.Body b){return b.dynamic?3f*b.invMass/Math.max(.025f,b.hx*b.hx+b.hy*b.hy+b.hz*b.hz):0;}
    public static void solve(PhysicsWorld.Body a,PhysicsWorld.Body b,CollisionMath.Contact c){
        float total=a.invMass+b.invMass;if(total<=0)return;
        float correction=Math.max(0,c.depth-.002f)*.65f/total;
        a.x-=c.nx*correction*a.invMass;a.y-=c.ny*correction*a.invMass;a.z-=c.nz*correction*a.invMass;
        b.x+=c.nx*correction*b.invMass;b.y+=c.ny*correction*b.invMass;b.z+=c.nz*correction*b.invMass;
        float ax=c.x-a.x,ay=c.y-a.y,az=c.z-a.z,bx=c.x-b.x,by=c.y-b.y,bz=c.z-b.z;
        float vx=b.vx+b.avy*bz-b.avz*by-a.vx-a.avy*az+a.avz*ay;
        float vy=b.vy+b.avz*bx-b.avx*bz-a.vy-a.avz*ax+a.avx*az;
        float vz=b.vz+b.avx*by-b.avy*bx-a.vz-a.avx*ay+a.avy*ax;
        float speed=vx*c.nx+vy*c.ny+vz*c.nz;
        if(speed<0){
            float aCross=square(ay*c.nz-az*c.ny)+square(az*c.nx-ax*c.nz)+square(ax*c.ny-ay*c.nx);
            float bCross=square(by*c.nz-bz*c.ny)+square(bz*c.nx-bx*c.nz)+square(bx*c.ny-by*c.nx);
            float denominator=total+inertia(a)*aCross+inertia(b)*bCross;
            float bounce=speed< -1.2f?Math.min(a.restitution,b.restitution):0,j=-(1+bounce)*speed/denominator;
            impulse(a,-c.nx*j,-c.ny*j,-c.nz*j,ax,ay,az);impulse(b,c.nx*j,c.ny*j,c.nz*j,bx,by,bz);
            float tx=vx-c.nx*speed,ty=vy-c.ny*speed,tz=vz-c.nz*speed,t=(float)Math.sqrt(tx*tx+ty*ty+tz*tz);
            if(t>.001f){float f=Math.min(t/denominator,j*(float)Math.sqrt(a.friction*b.friction))/t;impulse(a,tx*f,ty*f,tz*f,ax,ay,az);impulse(b,-tx*f,-ty*f,-tz*f,bx,by,bz);}
        }
        if(c.depth>.01f||Math.abs(speed)>.15f){if(a.dynamic)a.sleeping=false;if(b.dynamic)b.sleeping=false;}
    }
    private static float square(float v){return v*v;}
    public static void impulse(PhysicsWorld.Body b,float x,float y,float z,float rx,float ry,float rz){
        if(!b.dynamic)return;b.vx+=x*b.invMass;b.vy+=y*b.invMass;b.vz+=z*b.invMass;float k=inertia(b);
        b.avx+=(ry*z-rz*y)*k;b.avy+=(rz*x-rx*z)*k;b.avz+=(rx*y-ry*x)*k;
    }
}
