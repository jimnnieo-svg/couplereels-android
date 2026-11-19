"""Produce public, deterministically redacted copies. Original evidence stays untouched."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT.parent/'p08-evidence'/'galaxy'
OUT=ROOT/'public'/'screens'; OUT.mkdir(parents=True,exist_ok=True)
font=ImageFont.truetype('C:/Windows/Fonts/malgun.ttf',32)
mapping={'library':'82-delivery-library.png','discovery':'75-discovery.png','taste':'80-taste-tab.png','profiles':'74-me-restored.png','settings':'40-partner-direct.png','share':'07-share-all-apps.png','category':'79-gift-filter.png','preview':'45-embed-before-play.png'}
for name,file in mapping.items():
 im=Image.open(SRC/file).convert('RGB'); d=ImageDraw.Draw(im)
 if name in ['library','category']:
  for x,y in [(80,1710),(560,1710),(1045,1710),(80,2406),(560,2406)]:
   d.rectangle((x,y,x+320,y+110),fill='#fceef1');d.text((x+30,y+30),'식별정보 가림',font=font,fill='#765a65')
 if name=='taste':
  for y in [1906,2221,2536]:
   d.rectangle((545,y,1110,y+102),fill='#fff7f8');d.text((560,y+30),'식별정보 가림',font=font,fill='#765a65')
 if name=='settings':
  d.rectangle((50,1978,1350,2665),fill='#f1e8ec');d.text((505,2285),'외부 콘텐츠 가림',font=font,fill='#765a65')
 if name=='preview':
  d.rectangle((50,800,1390,905),fill='#fff7f8');d.text((60,820),'원본 URL 가림',font=font,fill='#765a65')
  d.rectangle((60,1070,1320,2108),fill='#f1e8ec');d.text((430,1550),'외부 원본·계정 정보 가림',font=font,fill='#765a65')
 if name=='share':
  d.rectangle((0,120,35,3000),fill='#202020');d.rectangle((1405,120,1440,3000),fill='#202020')
 im=im.crop((0,120,1440,3000)).resize((600,1200),Image.Resampling.LANCZOS)
 im.save(OUT/(name+'.webp'),quality=88,method=6)
provided=ROOT.parent/'couplereels-web-reference'
for name,index in [('discovery',0),('taste',1),('library',2),('profiles',3),('share',4)]:
 source=provided/(str(index)+'.png')
 if not source.exists():continue
 im=Image.open(source).convert('RGB');d=ImageDraw.Draw(im)
 if name=='discovery':
  d.rectangle((25,480,1023,570),fill='#fff7f8');d.text((35,500),'원본 URL 가림',font=font,fill='#9b667c')
  d.rectangle((30,640,1020,1450),fill='#f3e7ed');d.text((265,1000),'외부 원본·계정 정보 가림',font=font,fill='#9b667c')
 if name=='library':
  for x in [45,395,745]:
   for y in [1155,1650]:
    d.rectangle((x,y,x+260,y+100),fill='#fce7ee');d.text((x+12,y+27),'식별정보 가림',font=font,fill='#9b667c')
 if name=='taste':
  for y in [1265,1490,1720]:
   d.rectangle((390,y,1020,y+130),fill='#fff7f8');d.text((405,y+40),'식별정보 가림',font=font,fill='#9b667c')
 if name=='share':
  d.rectangle((0,0,1003,1000),fill='#222222');d.text((275,450),'Instagram 원본 콘텐츠 가림',font=font,fill='#d8c4cf')
  d.rectangle((170,1260,930,1365),fill='#222222');d.text((195,1290),'원본 링크 가림',font=font,fill='#d8c4cf')
  d.rectangle((0,1390,1003,1715),fill='#222222');d.text((340,1525),'개인 공유 대상 가림',font=font,fill='#d8c4cf')
 im.thumbnail((600,1200),Image.Resampling.LANCZOS)
 frame=Image.new('RGB',(600,1200),'#fff7f8' if name!='share' else '#222222');frame.paste(im,((600-im.width)//2,(1200-im.height)//2));frame.save(OUT/(name+'.webp'),quality=90,method=6)
print('8 screenshots optimized; five user-supplied screens used when present')

# A native social preview: brand typography and the actual redacted library screen.
im=Image.new('RGB',(1200,630),'#fff9fa');d=ImageDraw.Draw(im)
d.rounded_rectangle((815,30,1160,600),radius=140,fill='#fbe5ec')
for x in [828,930]:d.ellipse((x-170,140,x+170,480),outline='#d5b58a',width=2)
d.ellipse((65,55,113,103),outline='#c97794',width=3);d.ellipse((92,55,140,103),outline='#c97794',width=3)
serif=ImageFont.truetype('C:/Windows/Fonts/georgia.ttf',38);korean=ImageFont.truetype('C:/Windows/Fonts/malgun.ttf',52)
d.text((158,56),'CoupleReels',font=serif,fill='#782f4e')
d.text((70,215),'내가 좋아하는 것을,',font=korean,fill='#33292d');d.text((70,300),'네가 발견하도록.',font=korean,fill='#b46583')
d.text((74,445),'Android P0-8 · 이지민의 개인 바이브코딩 프로젝트',font=ImageFont.truetype('C:/Windows/Fonts/malgun.ttf',23),fill='#856575')
screen=Image.open(OUT/'library.webp').resize((240,480));im.paste(screen,(870,75));im.save(ROOT/'public'/'og.png',optimize=True)
