#!/usr/bin/env python3
from pathlib import Path
import argparse,os,subprocess,zipfile,json,hashlib,re,sys
root=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser(description='Verify an APK built from this checkout.')
parser.add_argument('--debug',action='store_true',help='Verify a development build with its separate signing key.')
parser.add_argument('--apk',type=Path)
parser.add_argument('--previous-apk',type=Path,help='Optional additional update-signature comparison.')
parser.add_argument('--report-dir',type=Path,default=root/'build/verification')
args=parser.parse_args()
report=args.report_dir;report.mkdir(parents=True,exist_ok=True)
kit=Path(os.environ.get('BS_ANDROID_TOOLCHAIN',root/'toolchain'))
env=os.environ.copy();env['LD_LIBRARY_PATH']=':'.join(str(kit/p) for p in ['usr/lib/x86_64-linux-gnu/android','usr/lib/x86_64-linux-gnu','usr/lib'])
apk=args.apk or root/('dist/BlackSwordsman3D-v8.0-debug.apk' if args.debug else 'dist/BlackSwordsman3D-v8.0.apk')
old=args.previous_apk
def sign(path):return subprocess.check_output(['java','-Xmx256m','-jar',str(kit/'usr/share/java/apksigner.jar'),'verify','--verbose','--print-certs',str(path)],env=env,text=True)
sig=sign(apk);(report/'apk-signature.txt').write_text(sig)
badging=subprocess.check_output([str(kit/'usr/bin/aapt'),'dump','badging',str(apk)],env=env,text=True);(report/'apk-manifest.txt').write_text(badging)
assert "versionCode='8'" in badging and "versionName='8.0.0'" in badging and "targetSdkVersion:'35'" in badging and "sdkVersion:'24'" in badging and "0x30002" in badging
align=subprocess.check_output([str(kit/'usr/bin/zipalign'),'-c','-v','4',str(apk)],env=env,text=True);(report/'apk-alignment.txt').write_text(align)
getcert=lambda text:re.search(r'certificate SHA-256 digest: ([0-9a-f]+)',text).group(1)
release_certificate=json.loads((root/'releases/v8.0/package.json').read_text())['signer_sha256']
compatible=getcert(sig)==release_certificate
assert compatible != args.debug, 'Unexpected APK signing certificate for the requested mode'
if old is not None:assert getcert(sig)==getcert(sign(old)), 'Previous APK signature mismatch'
with zipfile.ZipFile(apk) as z:
 assert z.testzip() is None
 assert z.read('classes.dex')==(root/'build/classes.dex').read_bytes()
 meshes=[n for n in z.namelist() if n.startswith('assets/meshes/') and n.endswith('.bin')]
 sounds=[n for n in z.namelist() if n.startswith('assets/audio/') and n.endswith('.wav')]
 assert len(meshes)==500 and len(sounds)==11
 flora=[n for n in z.namelist() if n.startswith("assets/flora/") and n.endswith(".bin")]
 maps=[n for n in z.namelist() if n.startswith("assets/materials/") and n.endswith(".rgba")]
 assert len(flora)==96 and len(maps)==24
 assert all(z.getinfo(n).file_size==512*512*4 for n in maps)
 assert all(z.getinfo(n).compress_type==zipfile.ZIP_STORED for n in sounds)
 assert len([n for n in z.read('assets/items.tsv').decode().splitlines() if n and not n.startswith('#')])==240
 assert len([n for n in z.read('assets/enemies.tsv').decode().splitlines() if n and not n.startswith('#')])==33
 assert not any(n.startswith('lib/') for n in z.namelist())
 d={'apk':apk.name,'bytes':apk.stat().st_size,'sha256':hashlib.sha256(apk.read_bytes()).hexdigest(),'package':'com.danil.blackswordsman','version_code':8,'min_sdk':24,'target_sdk':35,'gles':'3.2','build_mode':'debug' if args.debug else 'release','signature_compatible_with_release':compatible,'signer_sha256':getcert(sig),'baked_meshes':len(meshes),'audio_assets':len(sounds),'flora_meshes':len(flora),'material_maps':len(maps),'material_resolution':512,'zipalign':True,'zip_integrity':True,'dex_matches_compiled_source':True,'physical_device_test':False}
(report/'package.json').write_text(json.dumps(d,indent=2)+'\n');print(json.dumps(d,ensure_ascii=False))
