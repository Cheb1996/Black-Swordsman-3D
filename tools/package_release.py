#!/usr/bin/env python3
"""Package the verified v8 release with atomic final-file writes."""
from pathlib import Path
import argparse,hashlib,json,os,re,shutil,subprocess,zipfile
root=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser(description='Package a verified release without private signing files.')
parser.add_argument('--test-log',type=Path,default=root/'build/v8-final-tests.log')
parser.add_argument('--build-log',type=Path,default=root/'build/v8-final-build.log')
parser.add_argument('--report-dir',type=Path,default=root/'build/verification')
args=parser.parse_args()
output=root/'dist';output.mkdir(exist_ok=True)
log=args.test_log.read_text()
assert len(re.findall(r'^\w+Smoke OK:',log,re.M))==19 and 'AssertionError' not in log
report=json.loads((args.report_dir/'package.json').read_text())
assert report['build_mode']=='release' and report['signature_compatible_with_release']
apk=root/'dist/BlackSwordsman3D-v8.0.apk'
assert hashlib.sha256(apk.read_bytes()).hexdigest()==report['sha256']
def copy_atomic(source,target):
    tmp=target.with_name(target.name+'.part');shutil.copy2(source,tmp);os.replace(tmp,target)
copy_atomic(args.test_log,args.report_dir/'tests.txt')
copy_atomic(args.build_log,args.report_dir/'build.txt')
install=output/'BlackSwordsman3D-v8.0-install.zip'
tmp=install.with_name(install.name+'.part')
with zipfile.ZipFile(tmp,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
    z.write(apk,apk.name)
    for name in ('INSTALL_RU.txt','RELEASE_NOTES_RU.md'):z.write(root/name,name)
    z.writestr('SHA256.txt',report['sha256']+'  '+apk.name+'\n')
os.replace(tmp,install)
source=output/'BlackSwordsman3D-source-v8.0.zip';tmp=source.with_name(source.name+'.part')
with zipfile.ZipFile(tmp,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
    tracked=subprocess.check_output(['git','ls-files','-z'],cwd=root).decode().split('\0')
    for name in sorted(filter(None,tracked)):
        p=root/name
        if p.suffix in ('.apk','.zip','.keystore','.jks','.pem','.p12','.key') or p.name=='.signing.local':continue
        if Path(name).parts[0] in ('keystore','build','dist','.git'):continue
        z.write(p,'Black-Swordsman-3D/'+name)
os.replace(tmp,source)
for p in (source,install):
    with zipfile.ZipFile(p) as z:assert z.testzip() is None
with zipfile.ZipFile(install) as z:assert z.read(apk.name)==apk.read_bytes()
items=[]
for p in (output/apk.name,source,install):
    items.append({'path':str(p),'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()})
(root/'build/v8-deliverables.json').write_text(json.dumps(items,indent=2)+'\n')
print(json.dumps(items,ensure_ascii=False))
