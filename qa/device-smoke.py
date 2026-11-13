#!/usr/bin/env python3
"""Run only on a dedicated test device/emulator. Never clears app data or uninstalls.
Requires a fresh test-account UI; captures local-only MVP scenarios, not Instagram playback.
"""
import argparse, pathlib, subprocess, time, xml.etree.ElementTree as ET, re
p=argparse.ArgumentParser();p.add_argument('--serial',required=True);p.add_argument('--apk',required=True);p.add_argument('--output',required=True);a=p.parse_args()
out=pathlib.Path(a.output);out.mkdir(parents=True,exist_ok=True)
def adb(*args, binary=False):
 return subprocess.check_output(['adb','-s',a.serial,*args],timeout=45) if binary else subprocess.check_output(['adb','-s',a.serial,*args],text=True,timeout=45)
def event(s): print(s,flush=True)
def dump():
 adb('shell','uiautomator','dump','/sdcard/couplereels-smoke.xml')
 return ET.fromstring(adb('shell','cat','/sdcard/couplereels-smoke.xml'))
def tap(node):
 b=list(map(int,re.findall(r'\d+',node.get('bounds'))));adb('shell','input','tap',str((b[0]+b[2])//2),str((b[1]+b[3])//2));time.sleep(2)
def click(text,scroll=True):
 for attempt in range(7):
  tree=dump();nodes=[n for n in tree.iter('node') if n.get('text')==text or n.get('content-desc')==text]
  if nodes:tap(nodes[0]);event('CLICK '+text);return
  if scroll:adb('shell','input','swipe','350','1300','350','450','500')
  time.sleep(2)
 raise AssertionError('UI label not found: '+text)
def shot(name):
 (out/(name+'.png')).write_bytes(adb('exec-out','screencap','-p',binary=True));(out/(name+'.xml')).write_text(ET.tostring(dump(),encoding='unicode'));event('CAPTURE '+name)
def field(index,text):
 fields=[n for n in dump().iter('node') if n.get('class')=='android.widget.EditText'];tap(fields[index]);adb('shell','input','text',text);adb('shell','input','keyevent','KEYCODE_BACK');time.sleep(1)
for _ in range(180):
 try:
  if adb('shell','getprop','sys.boot_completed').strip()=='1':break
 except (subprocess.SubprocessError,ET.ParseError):pass
 time.sleep(2)
else:raise RuntimeError('Emulator did not boot within 6 minutes')
adb('shell','wm','size','720x1600');adb('shell','wm','density','320')
adb('shell','input','keyevent','KEYCODE_WAKEUP');adb('shell','wm','dismiss-keyguard')
event(adb('install','-r',a.apk).strip())
adb('shell','am','start','-n','com.couplereels.mvp/com.couplereels.app.MvpActivity');time.sleep(12)
shot('00-login')
field(0,'p05-a@demo.test');field(1,'demo1234');field(2,'DemoA');click('회원가입');time.sleep(4)
shot('01-onboarding-interests');click('향수');click('다음');shot('02-onboarding-style');click('실버');click('취향 저장하고 시작하기');shot('03-taste')
url='https://www.instagram.com/reel/DUnumljCZlg/'
adb('shell','am','start','-n','com.couplereels.mvp/com.couplereels.app.MvpActivity','-a','android.intent.action.SEND','-t','text/plain','--es','android.intent.extra.TEXT',url+'?igsh=test');time.sleep(3)
shot('04-share-sheet');click('목걸이');click('내 취향에 반영하기');click('보관함',False);shot('05-saved');click('우리',False);shot('06-us-unlinked')
click('새 연결 코드 발급');shot('07-invite')
event('PASS local onboarding/share/save/invite scenarios; no playback or Firebase synchronization assertion')
